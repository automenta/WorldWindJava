/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.terrain;

import com.jogamp.opengl.*;
import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.pick.*;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.util.Logging;

import java.awt.*;
import java.util.List;
import java.util.*;

/**
 * This class is a container for terrain geometry.
 *
 * @author tag
 * @version $Id: SectorGeometryList.java 1537 2013-08-07 19:58:01Z dcollins $
 */
public class SectorGeometryList extends ArrayList<SectorGeometry> {
    protected final PickSupport pickSupport = new PickSupport();
    protected final HashMap<SectorGeometry, ArrayList<Point>> pickSectors = new HashMap<>();
    /**
     * The spanning sector of all sector geometries contained in this list.
     */
    protected Sector sector;

    /**
     * Constructs an empty sector geometry list.
     */
    public SectorGeometryList() {
    }

    /**
     * Constructs a sector geometry list that contains a specified list of sector geometries.
     *
     * @param list the secter geometries to place in the list.
     */
    public SectorGeometryList(Collection<SectorGeometry> list) {
        super(list);
    }

    /**
     * Indicates that this list's sectors are about to be rendered. When rendering is complete, the {@link
     * #endRendering(DrawContext)} must be called.
     *
     * @param dc the  current draw context.
     */
    public static void beginRendering(DrawContext dc) {
        if (dc == null) {
            String message = Logging.getMessage("nullValue.DrawContextIsNull");
            Logging.logger().severe(message);
            throw new IllegalStateException(message);
        }

        // TODO: add the beginRendering interface to Tessellator in order to eliminate this type test
        if (dc.getGlobe().getTessellator() instanceof RectangularTessellator)
            RectangularTessellator.beginRendering(dc);
    }

    /**
     * Restores state established by {@link #beginRendering(DrawContext)}.
     *
     * @param dc the current draw context.
     */
    public static void endRendering(DrawContext dc) {
        if (dc == null) {
            String message = Logging.getMessage("nullValue.DrawContextIsNull");
            Logging.logger().severe(message);
            throw new IllegalStateException(message);
        }

        if (dc.getGlobe().getTessellator() instanceof RectangularTessellator)
            RectangularTessellator.endRendering(dc);
    }

    /**
     * Indicates that sector geometry picking is about to be performed. Configures the state necessary to correctly draw
     * sector geometry in a second pass using unique per-triangle colors. When picking is complete, {@link
     * #endSectorGeometryPicking(DrawContext)} must be called.
     *
     * @param dc the current draw context.
     */
    protected static void beginSectorGeometryPicking(DrawContext dc) {
        GL gl = dc.getGL();

        gl.glDepthFunc(GL.GL_LEQUAL);

        // When the OpenGL implementation is provided by the VMware SVGA 3D graphics driver, move the per-triangle
        // color geometry's depth values toward the eye and disable depth buffer writes. This works around an issue
        // where the VMware driver breaks OpenGL's invariance requirement when per-vertex colors are enabled.
        // See WWJ-425.
        if (dc.getGLRuntimeCapabilities().isVMwareSVGA3D()) {
            gl.glDepthMask(false);
            gl.glEnable(GL.GL_POLYGON_OFFSET_FILL);
            gl.glPolygonOffset(-1.0f, -1.0f);
        }
    }

    /**
     * Restores state established by {@link #beginSectorGeometryPicking(DrawContext)}.
     *
     * @param dc the current draw context.
     */
    protected static void endSectorGeometryPicking(DrawContext dc) {
        GL gl = dc.getGL();

        gl.glDepthFunc(GL.GL_LESS); // restore to default explicitly to avoid more expensive pushAttrib

        if (dc.getGLRuntimeCapabilities().isVMwareSVGA3D()) {
            gl.glDepthMask(true);
            gl.glDisable(GL.GL_POLYGON_OFFSET_FILL);
            gl.glPolygonOffset(0.0f, 0.0f);
        }
    }

    /**
     * Indicates the spanning sector of all sector geometries in this list.
     *
     * @return a sector that is the union of all sectors of entries in this list.
     */
    public Sector getSector() {
        return sector;
    }

    /**
     * Specifies the sector this list spans.
     *
     * @param sector the sector spanned by this list.
     */
    public void setSector(Sector sector) {
        this.sector = sector;
    }

    /**
     * Detects the locations of the sector geometries in this list that intersect a specified screen point.
     * <p>
     * Note: Prior to calling this method, {@link #beginRendering(DrawContext)} must be called.
     *
     * @param dc        the current draw context.
     * @param pickPoint the screen point to test.
     */
    public void pick(DrawContext dc, Point pickPoint) {
        if (dc == null) {
            String message = Logging.getMessage("nullValue.DrawContextIsNull");
            Logging.logger().severe(message);
            throw new IllegalStateException(message);
        }

        if (pickPoint == null)
            return;

        this.pickSupport.clearPickList();
        PickSupport.beginPicking(dc);

        GL2 gl = dc.getGL2(); // GL initialization checks for GL2 compatibility.
        gl.glShadeModel(GL2.GL_FLAT);

        try {
            // render each sector in unique color
            SectorGeometryList.beginRendering(dc);
            for (SectorGeometry sector : this) {
                Color color = dc.getUniquePickColor();
                gl.glColor3ub((byte) color.getRed(), (byte) color.getGreen(), (byte) color.getBlue());
                sector.render(dc);
                // lat/lon/elevation not used in this case
                this.pickSupport.addPickableObject(color.getRGB(), sector, Position.ZERO, true);
            }

            PickedObject pickedSector = this.pickSupport.getTopObject(dc, pickPoint);
            if (pickedSector == null || pickedSector.get() == null)
                return; // no sector picked

            SectorGeometryList.beginSectorGeometryPicking(dc);
            SectorGeometry sector = (SectorGeometry) pickedSector.get();
            sector.pick(dc, pickPoint);
        }
        finally {
            SectorGeometryList.endSectorGeometryPicking(dc);
            SectorGeometryList.endRendering(dc);
            gl.glShadeModel(GL2.GL_SMOOTH); // restore to default explicitly to avoid more expensive pushAttrib

            PickSupport.endPicking(dc);
            this.pickSupport.clearPickList();
        }
    }

    /**
     * Detects the locations of the sector geometries in this list that intersect any of the points in a specified list
     * of screen points.
     * <p>
     * Note: Prior to calling this method, {@link #beginRendering(DrawContext)} must be called.
     *
     * @param dc         the current draw context.
     * @param pickPoints the points to test.
     * @return an array of picked objects that intersect one or more of the specified screen points.
     */
    public List<PickedObject> pick(DrawContext dc, Collection<Point> pickPoints) {

        if (pickPoints == null || pickPoints.size() < 1)
            return null;

        this.pickSupport.clearPickList();
        PickSupport.beginPicking(dc);

        GL2 gl = dc.getGL2(); // GL initialization checks for GL2 compatibility.
        gl.glShadeModel(GL2.GL_FLAT);

        try {
            // render each sector in a unique color
            SectorGeometryList.beginRendering(dc);
            for (SectorGeometry sector : this) {
                Color color = dc.getUniquePickColor();
                gl.glColor3ub((byte) color.getRed(), (byte) color.getGreen(), (byte) color.getBlue());
                sector.render(dc);
                // lat/lon/elevation not used in this case
                this.pickSupport.addPickableObject(color.getRGB(), sector, Position.ZERO, true);
            }

            // Determine the sectors underneath the pick points. Assemble a pick-points per sector map.
            // Several pick points might intersect the same sector.
            this.pickSectors.clear();
            for (Point pickPoint : pickPoints) {
                PickedObject pickedSector = this.pickSupport.getTopObject(dc, pickPoint);
                if (pickedSector == null || pickedSector.get() == null)
                    continue;

                SectorGeometry sector = (SectorGeometry) pickedSector.get();
                ArrayList<Point> sectorPickPoints;
                if (!this.pickSectors.containsKey(sector)) {
                    sectorPickPoints = new ArrayList<>();
                    this.pickSectors.put(sector, sectorPickPoints);
                } else {
                    sectorPickPoints = this.pickSectors.get(sector);
                }
                sectorPickPoints.add(pickPoint);
            }

            if (this.pickSectors.size() < 1)
                return null;

            // Now have each sector determine the pick position for each intersecting pick point.
            SectorGeometryList.beginSectorGeometryPicking(dc);
            List<PickedObject> pickedObjects = new ArrayList<>();
            for (Map.Entry<SectorGeometry, ArrayList<Point>> sector : this.pickSectors.entrySet()) {
                ArrayList<Point> sectorPickPoints = sector.getValue();
                PickedObject[] pos = sector.getKey().pick(dc, sectorPickPoints);
                if (pos == null)
                    continue;

                for (PickedObject po : pos) {
                    if (po != null)
                        pickedObjects.add(po);
                }
            }

            return pickedObjects;
        }
        finally {
            SectorGeometryList.endSectorGeometryPicking(dc);
            SectorGeometryList.endRendering(dc);
            gl.glShadeModel(GL2.GL_SMOOTH); // restore to default explicitly to avoid more expensive pushAttrib

            PickSupport.endPicking(dc);
            this.pickSupport.clearPickList();
        }
    }

    /**
     * Computes a Cartesian point at a specified latitude, longitude and altitude above the terrain.
     *
     * @param position the position to compute the Cartesian point for. The altitude element of the position is
     *                 considered to be distance above the terrain at the position's latitude and longitude.
     * @return the Cartesian point, in meters, relative to an origin of (0, 0, 0). Will be null if there is no sector
     * geometry in this list for the specifed latitude and longitude.
     */
    public Vec4 getSurfacePoint(Position position) {
        return this.getSurfacePoint(position.getLat(), position.getLon(), position.getElevation());
    }

    /**
     * Computes a Cartesian point at a specified location on the terrain.
     *
     * @param latLon the location of the point to compute.
     * @return the Cartesian point, in meters, relative to an origin of (0, 0, 0). Will be null if there is no sector
     * geometry in this list for the specifed latitude and longitude.
     */
    public Vec4 getSurfacePoint(LatLon latLon) {
        return this.getSurfacePoint(latLon.getLat(), latLon.getLon(), 0.0d);
    }

    /**
     * Computes a Cartesian point at a specified location on the terrain.
     *
     * @param latitude  the latitude of the point to compute.
     * @param longitude the longitude of the point to compute.
     * @return the Cartesian point, in meters, relative to an origin of (0, 0, 0). Will be null if there is no sector
     * geometry in this list for the specifed latitude and longitude.
     */
    public Vec4 getSurfacePoint(Angle latitude, Angle longitude) {
        return this.getSurfacePoint(latitude, longitude, 0.0d);
    }

    /**
     * Computes a Cartesian point at a specified latitude, longitude and altitude above the terrain.
     *
     * @param latitude     the latitude of the point to compute.
     * @param longitude    the longitude of the point to compute.
     * @param metersOffset the distance above the terrain of the point to compute.
     * @return the Cartesian point, in meters, relative to an origin of (0, 0, 0). Will be null if there is no sector
     * geometry in this list for the specifed latitude and longitude.
     */
    public Vec4 getSurfacePoint(Angle lat, Angle lon, double metersOffset) {

        double latitude = lat.degrees, longitude = lon.degrees;
        for (SectorGeometry sg : this) {
            if (sg.getSector().contains(latitude, longitude)) {
                Vec4 point = sg.getSurfacePoint(latitude, longitude, metersOffset);
                if (point != null)
                    return point;
            }
        }

        return null;
    }

    /**
     * Determines if and where a ray intersects the geometry.
     *
     * @param line the <code>Line</code> for which an intersection is to be found.
     * @return the &lt;Vec4&gt; point closest to the ray origin where an intersection has been found or null if no
     * intersection was found.
     */
    public Intersection[] intersect(Line line) {
        if (line == null) {
            String msg = Logging.getMessage("nullValue.LineIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        Iterable<SectorGeometry> sglist = new ArrayList<>(this);

        Intersection[] hits;
        Collection<Intersection> list = new ArrayList<>();
        for (SectorGeometry sg : sglist) {
            if (sg.getExtent().intersects(line))
                if ((hits = sg.intersect(line)) != null)
                    list.addAll(Arrays.asList(hits));
        }

        int numHits = list.size();
        if (numHits == 0)
            return null;

        hits = new Intersection[numHits];
        list.toArray(hits);

        final Vec4 origin = line.origin;
        Arrays.sort(hits, (i1, i2) -> {
            if (i1 == null && i2 == null)
                return 0;
            if (i2 == null)
                return -1;
            if (i1 == null)
                return 1;

            Vec4 v1 = i1.getIntersectionPoint();
            Vec4 v2 = i2.getIntersectionPoint();
            double d1 = origin.distanceTo3(v1);
            double d2 = origin.distanceTo3(v2);
            return Double.compare(d1, d2);
        });
        return hits;
    }

    /**
     * Determines if and where the geometry intersects the ellipsoid at a given elevation.
     * <p>
     * The returned array of <code>Intersection</code> describes a list of individual segments - two
     * <code>Intersection</code> for each, corresponding to each geometry triangle that intersects the given elevation.
     * <p>
     * Note that the provided bounding <code>Sector</code> only serves as a 'hint' to avoid processing unnecessary
     * geometry tiles. The returned intersection list may contain segments outside that sector.
     *
     * @param elevation the elevation for which intersections are to be found.
     * @param sector    the sector inside which intersections are to be found.
     * @return a list of <code>Intersection</code> pairs/segments describing a contour line at the given elevation.
     */
    public Intersection[] intersect(double elevation, Sector sector) {
        if (sector == null) {
            String message = Logging.getMessage("nullValue.SectorIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        Iterable<SectorGeometry> sglist = new ArrayList<>(this);

        Intersection[] hits;
        Collection<Intersection> list = new ArrayList<>();
        for (SectorGeometry sg : sglist) {
            if (sector.intersects(sg.getSector()))
                if ((hits = sg.intersect(elevation)) != null)
                    list.addAll(Arrays.asList(hits));
        }

        int numHits = list.size();
        if (numHits == 0)
            return null;

        hits = new Intersection[numHits];
        list.toArray(hits);

        return hits;
    }
}