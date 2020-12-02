/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.render;

import com.jogamp.opengl.GL2;
import gov.nasa.worldwind.cache.GpuResourceCache;
import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.globes.Globe;
import gov.nasa.worldwind.util.*;

import java.util.*;

/**
 * This class renders fast multiple surface polylines in one pass. It relies on a {@link CompoundVecBuffer}.
 *
 * @author Dave Collins
 * @author Patrick Murris
 * @version $Id: SurfacePolylines.java 2406 2014-10-29 23:39:29Z dcollins $
 */
public class SurfacePolylines extends AbstractSurfaceShape {
    protected final Object outlineDisplayListCacheKey = new Object();
    protected List<Sector> sectors;
    protected CompoundVecBuffer buffer;
    protected boolean needsOutlineTessellation = true;
    protected boolean crossesDateLine = false;

    public SurfacePolylines(CompoundVecBuffer buffer) {

        this.buffer = buffer;
    }

    public SurfacePolylines(Sector sector, CompoundVecBuffer buffer) {

        this.sectors = Collections.singletonList(sector);
        this.buffer = buffer;
    }

    /**
     * Get the underlying {@link CompoundVecBuffer} describing the geometry.
     *
     * @return the underlying {@link CompoundVecBuffer}.
     */
    public CompoundVecBuffer getBuffer() {
        return this.buffer;
    }

    @Override
    public List<Sector> getSectors(DrawContext dc) {
//        if (dc == null) {
//            String message = Logging.getMessage("nullValue.DrawContextIsNull");
//            Logging.logger().severe(message);
//            throw new IllegalArgumentException(message);
//        }

        // SurfacePolylines does not interpolate between caller specified positions, therefore it has no path type.
        if (this.sectors == null)
            this.sectors = WWUtil.arrayList(this.computeSectors(dc));

        return this.sectors;
    }

    public Iterable<? extends LatLon> getLocations(Globe globe) {

        return this.getLocations();
    }

    protected List<List<LatLon>> createGeometry(Globe globe, SurfaceTileDrawContext sdc) {
        // SurfacePolylines does not invoke this method, so return null indicating this method is not supported.
        // We avoid invoking computeGeometry by overriding determineActiveGeometry below.
        return null;
    }

    protected List<List<LatLon>> createGeometry(Globe globe, double edgeIntervalsPerDegree) {
        return null;
    }

    public Iterable<? extends LatLon> getLocations() {
        return this.buffer.getLocations();
    }

    @SuppressWarnings("UnusedDeclaration")
    public static void setLocations(Iterable<? extends LatLon> iterable) {
        throw new UnsupportedOperationException();
    }

    public Position getReferencePosition() {
        return buffer.getReferencePosition();
    }

    /**
     * {@inheritDoc} Overridden to treat the polylines as open paths rather than closed polygons.
     */
    @Override
    protected boolean canContainPole() {
        return false;
    }

    protected void doMoveTo(Position oldReferencePosition, Position newReferencePosition) {
        final int n = this.buffer.size();
        for (int i = 0; i < n; i++) {
            VecBuffer vb = this.buffer.subBuffer(i);

            final int v = vb.getSize();
            for (int pos = 0; pos < v; pos++) {
                LatLon ll = vb.getLocation(pos);
                Angle heading = LatLon.greatCircleAzimuth(oldReferencePosition, ll);
                Angle pathLength = LatLon.greatCircleDistance(oldReferencePosition, ll);
                vb.putLocation(pos, LatLon.greatCircleEndPosition(newReferencePosition, heading, pathLength));
            }
        }

        this.onGeometryChanged();
    }

    protected void doMoveTo(Globe globe, Position oldReferencePosition, Position newReferencePosition) {
        final int n = this.buffer.size();
        for (int i = 0; i < n; i++) {
            VecBuffer vb = this.buffer.subBuffer(i);

            List<LatLon> newLocations = LatLon.computeShiftedLocations(globe, oldReferencePosition,
                newReferencePosition, vb.getLocations());

            final int v = vb.getSize();
            for (int pos = 0; pos < v; pos++) {
                vb.putLocation(pos, newLocations.get(i));
            }
        }

        this.onGeometryChanged();
    }

    protected void onGeometryChanged() {
        this.sectors = null;
        this.needsOutlineTessellation = true;
        super.onShapeChanged();
    }

    protected void determineActiveGeometry(DrawContext dc, SurfaceTileDrawContext sdc) {
        // Intentionally left blank in order to override the superclass behavior with nothing.
    }

    protected void drawInterior(DrawContext dc, SurfaceTileDrawContext sdc) {
        // Intentionally left blank; SurfacePolylines does not render an interior.
    }

    protected void drawOutline(DrawContext dc, SurfaceTileDrawContext sdc) {
        // Exit immediately if the Polyline has no coordinate data.
        if (this.buffer.size() == 0)
            return;

        Position referencePos = this.getReferencePosition();
        if (referencePos == null)
            return;

        int hemisphereSign = (int) Math.signum(sdc.getSector().getCentroid().getLongitude().degrees);

        // Attempt to tessellate the Polyline's outline if the Polyline's outline display list is uninitialized, or if
        // the Polyline is marked as needing tessellation.
        int[] dlResource = (int[]) dc.getGpuResourceCache().get(this.outlineDisplayListCacheKey);
        if (dlResource == null || this.needsOutlineTessellation)
            dlResource = this.tessellateOutline(dc, referencePos);

        // Exit immediately if the Polyline's interior failed to tessellate. The cause has already been logged by
        // tessellateInterior.
        if (dlResource == null)
            return;

        GL2 gl = dc.getGL().getGL2(); // GL initialization checks for GL2 compatibility.
        AbstractSurfaceShape.applyOutlineState(dc, this.getActiveAttributes());
        gl.glCallList(dlResource[0]);

        if (this.crossesDateLine) {
            gl.glPushMatrix();
            try {
                // Apply hemisphere offset and draw again
                gl.glTranslated(360 * hemisphereSign, 0, 0);
                gl.glCallList(dlResource[0]);
            } finally {
                gl.glPopMatrix();
            }
        }
    }

    protected int[] tessellateOutline(DrawContext dc, LatLon referenceLocation) {
        GL2 gl = dc.getGL().getGL2(); // GL initialization checks for GL2 compatibility.
        this.crossesDateLine = false;

        int[] dlResource = new int[] {gl.glGenLists(1), 1};

        gl.glNewList(dlResource[0], GL2.GL_COMPILE);
        final int n = this.buffer.size();
        try {
            // Tessellate each part, note if crossing date line
            for (int i = 0; i < n; i++) {
                VecBuffer subBuffer = this.buffer.subBuffer(i);
                if (SurfacePolylines.tessellatePart(gl, subBuffer, referenceLocation))
                    this.crossesDateLine = true;
            }
        }
        finally {
            gl.glEndList();
        }

        this.needsOutlineTessellation = false;

        int numBytes = n * 3 * 4; // 3 float coords
        dc.getGpuResourceCache().put(this.outlineDisplayListCacheKey, dlResource, GpuResourceCache.DISPLAY_LISTS,
            numBytes);

        return dlResource;
    }

    protected static boolean tessellatePart(GL2 gl, VecBuffer vecBuffer, final LatLon referenceLocation) {
        Iterable<double[]> iterable = vecBuffer.getCoords(3);
        boolean dateLineCrossed = false;

        final Angle rLon = referenceLocation.getLongitude();
        final Angle rLat = referenceLocation.getLatitude();

        gl.glBegin(GL2.GL_LINE_STRIP);
        try {
            int sign = 0; // hemisphere offset direction
            double lonPrev = 0;

            for (double[] coords : iterable) {
                if (Math.abs(lonPrev - coords[0]) > 180) {
                    // Crossing date line, sum departure point longitude sign for hemisphere offset
                    sign += (int) Math.signum(lonPrev);
                    dateLineCrossed = true;
                }

                lonPrev = coords[0];

                double lonDegrees = coords[0] - rLon.degrees;
                double latDegrees = coords[1] - rLat.degrees;
                lonDegrees += sign * 360; // apply hemisphere offset
                gl.glVertex3f((float) lonDegrees, (float) latDegrees, 0);
            }
        }
        finally {
            gl.glEnd();
        }

        return dateLineCrossed;
    }
}
