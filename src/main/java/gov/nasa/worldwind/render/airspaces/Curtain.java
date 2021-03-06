/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */

package gov.nasa.worldwind.render.airspaces;

import com.jogamp.opengl.*;
import gov.nasa.worldwind.Keys;
import gov.nasa.worldwind.cache.Cacheable;
import gov.nasa.worldwind.geom.Box;
import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.globes.Globe;
import gov.nasa.worldwind.render.*;
import gov.nasa.worldwind.util.*;

import java.util.*;

/**
 * A curtain is a series of adjacent rectangular planes. The upper edges of the planes are the connecting line segments
 * between the vertices of a polyline. The lower edges of the planes are parallel to the upper edges at a specified
 * altitude.
 *
 * @author tag
 * @version $Id: Curtain.java 2309 2014-09-17 00:04:08Z tgaskins $
 */
public class Curtain extends AbstractAirspace {
    protected final List<LatLon> locations = new ArrayList<>();
    protected String pathType = Keys.GREAT_CIRCLE;
    protected double splitThreshold = 2000.0; // 2 km
    protected boolean applyPositionAltitude;

    public Curtain(Iterable<? extends LatLon> locations) {
        this.addLocations(locations);
        this.makeDefaultDetailLevels();
    }

    public Curtain(AirspaceAttributes attributes) {
        super(attributes);
        this.makeDefaultDetailLevels();
    }

    public Curtain() {
        this.makeDefaultDetailLevels();
    }

    public Curtain(Curtain source) {
        super(source);

        this.addLocations(source.locations);
        this.pathType = source.pathType;
        this.splitThreshold = source.splitThreshold;
        this.applyPositionAltitude = source.applyPositionAltitude;

        this.makeDefaultDetailLevels();
    }

    protected static void makeSectionInfo(DrawContext dc, int count, LatLon[] locations, String pathType,
        double splitThreshold,
        SectionRenderInfo[] ri, int[] counts) {
        int sectionCount = count - 1;

        for (int i = 0; i < sectionCount; i++) {
            ri[i] = new SectionRenderInfo(locations[i], locations[i + 1], pathType);
            ri[i].pillars = Curtain.getSectionPillarCount(dc, ri[i].begin, ri[i].end, ri[i].pathType, splitThreshold);
            ri[i].firstFillIndex = counts[0];
            ri[i].firstOutlineIndex = counts[1];
            ri[i].firstVertex = counts[2];
            ri[i].fillIndexCount = Curtain.getSectionFillIndexCount(ri[i].pillars);
            ri[i].outlineIndexCount = Curtain.getSectionOutlineIndexCount(ri[i].pillars);
            ri[i].vertexCount = Curtain.getSectionVertexCount(ri[i].pillars);
            counts[0] += ri[i].fillIndexCount;
            counts[1] += ri[i].outlineIndexCount;
            counts[2] += ri[i].vertexCount;
        }
    }

    protected static int getSectionPillarCount(DrawContext dc, LatLon begin, LatLon end, String pathType,
        double splitThreshold) {
        Globe globe;
        double arcLength, distance;
        int pillars;

        globe = dc.getGlobe();

        if (Keys.RHUMB_LINE.equalsIgnoreCase(pathType) || Keys.LOXODROME.equalsIgnoreCase(pathType)) {
            arcLength = LatLon.rhumbDistance(begin, end).radians();
        } else // (AVKey.GREAT_CIRCLE.equalsIgnoreCase(pathType)
        {
            arcLength = LatLon.greatCircleDistance(begin, end).radians();
        }

        distance = arcLength * globe.getRadius();
        pillars = (int) Math.ceil(distance / splitThreshold) - 1;
        pillars = Math.max(1, pillars);

        return pillars;
    }

    protected static int getSectionFillDrawMode() {
        return GL.GL_TRIANGLE_STRIP;
    }

    protected static int getSectionOutlineDrawMode() {
        return GL.GL_LINES;
    }

    protected static int getSectionFillIndexCount(int pillars) {
        return 2 * (pillars + 1);
    }

    protected static int getSectionOutlineIndexCount(int pillars) {
        return 4 * (pillars + 1);
    }

    protected static int getSectionVertexCount(int pillars) {
        return 2 * (pillars + 1);
    }

    protected static void makeSectionFillIndices(int pillars, int vertexPos, int indexPos, int[] dest) {
        int p;
        int index, vertex;

        index = indexPos;
        for (p = 0; p <= pillars; p++) {
            vertex = vertexPos + 2 * p;
            dest[index++] = vertex + 1;
            dest[index++] = vertex;
        }
    }

    protected static void makeSectionOutlineIndices(int pillars, int vertexPos, int indexPos, int[] dest) {
        int p;
        int index, vertex;
        index = indexPos;

        vertex = vertexPos;
        dest[index++] = vertex + 1;
        dest[index++] = vertex;

        for (p = 0; p < pillars; p++) {
            vertex = vertexPos + 2 * p;
            dest[index++] = vertex;
            dest[index++] = vertex + 2;
            dest[index++] = vertex + 1;
            dest[index++] = vertex + 3;
        }

        vertex = vertexPos + 2 * pillars;
        dest[index++] = vertex + 1;
        dest[index] = vertex;
    }

    protected void makeDefaultDetailLevels() {
        Collection<DetailLevel> levels = new ArrayList<>();
        double[] ramp = ScreenSizeDetailLevel.computeDefaultScreenSizeRamp(5);

        DetailLevel level;
        level = new ScreenSizeDetailLevel(ramp[0], "Detail-Level-0");
        level.set(AbstractAirspace.SPLIT_THRESHOLD, 1000.0);
        level.set(AbstractAirspace.DISABLE_TERRAIN_CONFORMANCE, false);
        levels.add(level);

        level = new ScreenSizeDetailLevel(ramp[1], "Detail-Level-1");
        level.set(AbstractAirspace.SPLIT_THRESHOLD, 2000.0);
        level.set(AbstractAirspace.DISABLE_TERRAIN_CONFORMANCE, false);
        levels.add(level);

        level = new ScreenSizeDetailLevel(ramp[2], "Detail-Level-2");
        level.set(AbstractAirspace.SPLIT_THRESHOLD, 10000.0);
        level.set(AbstractAirspace.DISABLE_TERRAIN_CONFORMANCE, false);
        levels.add(level);

        level = new ScreenSizeDetailLevel(ramp[3], "Detail-Level-3");
        level.set(AbstractAirspace.SPLIT_THRESHOLD, 100000.0);
        level.set(AbstractAirspace.DISABLE_TERRAIN_CONFORMANCE, false);
        levels.add(level);

        level = new ScreenSizeDetailLevel(ramp[4], "Detail-Level-4");
        level.set(AbstractAirspace.SPLIT_THRESHOLD, 1000000.0);
        level.set(AbstractAirspace.DISABLE_TERRAIN_CONFORMANCE, true);
        levels.add(level);

        this.setDetailLevels(levels);
    }

    /**
     * Returns the curtain's locations.
     *
     * @return the curtain's locations in geographic coordinates.
     */
    public Iterable<LatLon> getLocations() {
        return Collections.unmodifiableList(this.locations);
    }

    /**
     * Sets the curtain's locations, in geographic coordinates.
     *
     * @param locations a list of geographic coordinates (latitude and longitude) specifying the upper edge of the
     *                  shape.
     * @throws IllegalArgumentException if the locations list is null or contains fewer than two points.
     */
    public void setLocations(Iterable<? extends LatLon> locations) {
        this.locations.clear();
        this.addLocations(locations);
    }

    protected void addLocations(Iterable<? extends LatLon> newLocations) {
        if (newLocations != null) {
            for (LatLon ll : newLocations) {
                if (ll != null)
                    this.locations.add(ll);
            }
        }

        this.invalidateAirspaceData();
    }

    public String getPathType() {
        return this.pathType;
    }

    public void setPathType(String pathType) {
        if (pathType == null) {
            String message = "nullValue.PathTypeIsNull";
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.pathType = pathType;
        this.invalidateAirspaceData();
    }

    public boolean isApplyPositionAltitude() {
        return applyPositionAltitude;
    }

    public void setApplyPositionAltitude(boolean applyPositionAltitude) {
        this.applyPositionAltitude = applyPositionAltitude;
    }

    public Position getReferencePosition() {
        return AbstractAirspace.computeReferencePosition(this.locations, this.getAltitudes());
    }

    //**************************************************************//
    //********************  Geometry Rendering  ********************//
    //**************************************************************//

    protected Extent computeExtent(Globe globe, double verticalExaggeration) {
        List<Vec4> points = this.computeMinimalGeometry(globe, verticalExaggeration);
        if (points == null || points.isEmpty())
            return null;

        return Box.computeBoundingBox(points);
    }

    @Override
    protected List<Vec4> computeMinimalGeometry(Globe globe, double verticalExaggeration) {
        Collection<LatLon> tessellatedLocations = new ArrayList<>();
        this.makeTessellatedLocations(globe, tessellatedLocations);

        if (tessellatedLocations.isEmpty())
            return null;

        List<Vec4> points = new ArrayList<>();
        this.makeExtremePoints(globe, verticalExaggeration, tessellatedLocations, points);

        return points;
    }

    //**************************************************************//
    //********************  Curtain             ********************//
    //**************************************************************//

    @Override
    protected SurfaceShape createSurfaceShape() {
        return new SurfacePolyline();
    }

    @Override
    protected void updateSurfaceShape(DrawContext dc, SurfaceShape shape) {
        super.updateSurfaceShape(dc, shape);

        // Display the airspace's interior color when its outline is disabled but its interior is enabled. This causes
        // the surface shape to display the color most similar to the 3D airspace.
        if (!this.getActiveAttributes().isDrawOutline() && this.getActiveAttributes().isDrawInterior()) {
            shape.getAttributes().setDrawOutline(true);
            shape.getAttributes().setOutlineMaterial(this.getActiveAttributes().getInteriorMaterial());
        }
    }

    @Override
    protected void regenerateSurfaceShape(DrawContext dc, SurfaceShape shape) {
        ((SurfacePolyline) this.surfaceShape).setLocations(this.getLocations());
        this.surfaceShape.setPathType(this.getPathType());
    }

    protected void doMoveTo(Globe globe, Position oldRef, Position newRef) {
        if (oldRef == null) {
            String message = "nullValue.OldRefIsNull";
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        if (newRef == null) {
            String message = "nullValue.NewRefIsNull";
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        List<LatLon> newLocations = LatLon.computeShiftedLocations(globe, oldRef, newRef, this.getLocations());
        this.setLocations(newLocations);

        super.doMoveTo(oldRef, newRef);
    }

    protected void doMoveTo(Position oldRef, Position newRef) {
        if (oldRef == null) {
            String message = "nullValue.OldRefIsNull";
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        if (newRef == null) {
            String message = "nullValue.NewRefIsNull";
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        super.doMoveTo(oldRef, newRef);

        int count = this.locations.size();
        LatLon[] newLocations = new LatLon[count];
        for (int i = 0; i < count; i++) {
            LatLon ll = this.locations.get(i);
            double distance = LatLon.greatCircleDistance(oldRef, ll).radians();
            double azimuth = LatLon.greatCircleAzimuth(oldRef, ll).radians();
            newLocations[i] = LatLon.greatCircleEndPosition(newRef, azimuth, distance);
        }

        this.setLocations(Arrays.asList(newLocations));
    }

    //**************************************************************//
    //********************  Section             ********************//
    //**************************************************************//

    protected double getSplitThreshold() {
        return this.splitThreshold;
    }

    protected void setSplitThreshold(double splitThreshold) {
        if (splitThreshold <= 0.0) {
            String message = Logging.getMessage("generic.ArgumentOutOfRange", "splitThreshold=" + splitThreshold);
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.splitThreshold = splitThreshold;
    }

    protected Vec4 computeReferenceCenter(DrawContext dc) {
        Extent extent = this.getExtent(dc);
        return extent != null ? extent.getCenter() : null;
    }

    protected void doRenderGeometry(DrawContext dc, String drawStyle) {
        if (dc == null) {
            String message = Logging.getMessage("nullValue.DrawContextIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        if (dc.getGL() == null) {
            String message = Logging.getMessage("nullValue.DrawingContextGLIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        int count = locations.size();
        LatLon[] locationArray = new LatLon[count];
        this.locations.toArray(locationArray);

        double[] altitudes = this.getAltitudes(dc.getVerticalExaggeration());
        boolean[] terrainConformant = this.isTerrainConforming();
        String pathType = this.getPathType();
        double splitThreshold = this.splitThreshold;

        if (this.isEnableLevelOfDetail()) {
            DetailLevel level = this.computeDetailLevel(dc);

            Object o = level.get(AbstractAirspace.SPLIT_THRESHOLD);
            if (o instanceof Double)
                splitThreshold = (Double) o;

            o = level.get(AbstractAirspace.DISABLE_TERRAIN_CONFORMANCE);
            if (o instanceof Boolean && (Boolean) o)
                terrainConformant[0] = terrainConformant[1] = false;
        }

        Vec4 referenceCenter = this.computeReferenceCenter(dc);
        this.setExpiryTime(this.nextExpiryTime(dc, terrainConformant));
        this.clearElevationMap();

        GL2 gl = dc.getGL2(); // GL initialization checks for GL2 compatibility.
        int[] lightModelTwoSide = new int[1];
        try {
            gl.glGetIntegerv(GL2.GL_LIGHT_MODEL_TWO_SIDE, lightModelTwoSide, 0);
            dc.view().pushReferenceCenter(dc, referenceCenter);

            if (Airspace.DRAW_STYLE_FILL.equals(drawStyle)) {
                gl.glLightModeli(GL2.GL_LIGHT_MODEL_TWO_SIDE, GL2.GL_TRUE);

                this.drawCurtainFill(dc, count, locationArray, pathType, splitThreshold, altitudes, terrainConformant,
                    referenceCenter);
            } else if (Airspace.DRAW_STYLE_OUTLINE.equals(drawStyle)) {
                this.drawCurtainOutline(dc, count, locationArray, pathType, splitThreshold, altitudes,
                    terrainConformant, referenceCenter);
            }
        }
        finally {
            dc.view().popReferenceCenter(dc);
            gl.glLightModeli(GL2.GL_LIGHT_MODEL_TWO_SIDE, lightModelTwoSide[0]);
        }
    }

    protected CurtainGeometry getCurtainGeometry(DrawContext dc, int count, LatLon[] locations, String pathType,
        double splitThreshold,
        double[] altitudes, boolean[] terrainConformant,
        Vec4 referenceCenter) {
        Object cacheKey = new Geometry.CacheKey(dc.getGlobe(), this.getClass(), "Curtain",
            locations, pathType, altitudes[0], altitudes[1], terrainConformant[0], terrainConformant[1],
            splitThreshold, referenceCenter);

        CurtainGeometry geom = (CurtainGeometry) AbstractAirspace.getGeometryCache().getObject(cacheKey);
        if (geom == null || AbstractAirspace.isExpired(dc, geom.getVertexGeometry())) {
            if (geom == null)
                geom = new CurtainGeometry();
            this.makeCurtainGeometry(dc, count, locations, pathType, splitThreshold, altitudes, terrainConformant,
                referenceCenter, geom);
            this.updateExpiryCriteria(dc, geom.getVertexGeometry());
            AbstractAirspace.getGeometryCache().add(cacheKey, geom);
        }

        return geom;
    }

    protected void drawCurtainFill(DrawContext dc, int count, LatLon[] locations, String pathType,
        double splitThreshold,
        double[] altitudes, boolean[] terrainConformant,
        Vec4 referenceCenter) {
        CurtainGeometry geom = this.getCurtainGeometry(dc, count, locations, pathType, splitThreshold,
            altitudes, terrainConformant, referenceCenter);

        this.drawGeometry(dc, geom.getFillIndexGeometry(), geom.getVertexGeometry());
    }

    protected void drawCurtainOutline(DrawContext dc, int count, LatLon[] locations, String pathType,
        double splitThreshold,
        double[] altitudes, boolean[] terrainConformant,
        Vec4 referenceCenter) {
        CurtainGeometry geom = this.getCurtainGeometry(dc, count, locations, pathType, splitThreshold,
            altitudes, terrainConformant, referenceCenter);

        this.drawGeometry(dc, geom.getOutlineIndexGeometry(), geom.getVertexGeometry());
    }

    protected void makeCurtainGeometry(DrawContext dc, int count, LatLon[] locations, String pathType,
        double splitThreshold,
        double[] altitudes, boolean[] terrainConformant,
        Vec4 referenceCenter,
        CurtainGeometry dest) {
        int sections = count - 1;
        int[] counts = new int[3];
        SectionRenderInfo[] ri = new SectionRenderInfo[sections];
        Curtain.makeSectionInfo(dc, count, locations, pathType, splitThreshold, ri, counts);

        int fillDrawMode = Curtain.getSectionFillDrawMode();
        int outlineDrawMode = Curtain.getSectionOutlineDrawMode();

        int[] fillIndices = new int[counts[0]];
        int[] outlineIndices = new int[counts[1]];
        float[] verts = new float[3 * counts[2]];
        float[] norms = new float[3 * counts[2]];

        for (int s = 0; s < sections; s++) {
            Curtain.makeSectionFillIndices(ri[s].pillars, ri[s].firstVertex, ri[s].firstFillIndex, fillIndices);
            Curtain.makeSectionOutlineIndices(ri[s].pillars, ri[s].firstVertex, ri[s].firstOutlineIndex,
                outlineIndices);

            this.makeSectionVertices(dc, ri[s].begin, ri[s].end, ri[s].pathType, altitudes, terrainConformant,
                ri[s].pillars, ri[s].firstVertex, verts, referenceCenter);
            this.getGeometryBuilder().makeIndexedTriangleStripNormals(ri[s].firstFillIndex, ri[s].fillIndexCount,
                fillIndices, ri[s].firstVertex, ri[s].vertexCount, verts, norms);
        }

        dest.getFillIndexGeometry().setElementData(fillDrawMode, counts[0], fillIndices);
        dest.getOutlineIndexGeometry().setElementData(outlineDrawMode, counts[1], outlineIndices);
        dest.getVertexGeometry().setVertexData(counts[2], verts);
        dest.getVertexGeometry().setNormalData(counts[2], norms);
    }

    protected void makeSectionVertices(DrawContext dc, LatLon begin, LatLon end, String pathType,
        double[] altitude, boolean[] terrainConformant,
        int pillars, int vertexPos, float[] dest, Vec4 referenceCenter) {
        Globe globe = dc.getGlobe();
        double arcLength, azimuth;

        if (Keys.RHUMB_LINE.equalsIgnoreCase(pathType) || Keys.LOXODROME.equalsIgnoreCase(pathType)) {
            arcLength = LatLon.rhumbDistance(begin, end).radians();
            azimuth = LatLon.rhumbAzimuth(begin, end).radians();
        } else // (AVKey.GREAT_CIRCLE.equalsIgnoreCase(pathType)
        {
            arcLength = LatLon.greatCircleDistance(begin, end).radians();
            azimuth = LatLon.greatCircleAzimuth(begin, end).radians();
        }

        double dlength = arcLength / pillars;

        // Set up to take altitude from the curtain positions if Positions are specified.
        double alt0 = 0;
        Double dAlt = null;
        if (this.isApplyPositionAltitude() && begin instanceof Position && end instanceof Position) {
            alt0 = ((Position) begin).getAltitude();
            dAlt = (((Position) end).getAltitude() - alt0) / pillars;
        }

        for (int p = 0; p <= pillars; p++) {
            double length = p * dlength;

            LatLon ll;
            if (Keys.RHUMB_LINE.equalsIgnoreCase(pathType) || Keys.LOXODROME.equalsIgnoreCase(pathType))
                ll = LatLon.rhumbEndPosition(begin, azimuth, length);
            else // (AVKey.GREAT_CIRCLE.equalsIgnoreCase(pathType)
                ll = LatLon.greatCircleEndPosition(begin, azimuth, length);

            for (int s = 0; s < 2; s++) {
                int index = s + 2 * p;
                index = 3 * (vertexPos + index);

                // For upper altitude, use the Position's if specified, otherwise the curtain's upper altitude.
                double elevation = (dAlt != null && s == 1) ? alt0 + p * dAlt : altitude[s];
                if (terrainConformant[s])
                    elevation += this.computeElevationAt(dc, ll.getLat(), ll.getLon());

                Vec4 vec = globe.computePointFromPosition(ll.getLat(), ll.getLon(), elevation);
                dest[index] = (float) (vec.x - referenceCenter.x);
                dest[index + 1] = (float) (vec.y - referenceCenter.y);
                dest[index + 2] = (float) (vec.z - referenceCenter.z);
            }
        }
    }

    protected void makeTessellatedLocations(Extent globe, Collection<LatLon> tessellatedLocations) {
        if (this.getLocations() == null)
            return;

        Iterator<LatLon> iter = this.getLocations().iterator();
        if (!iter.hasNext())
            return;

        LatLon locA = iter.next();
        tessellatedLocations.add(locA); // Add the curtain's first location.

        while (iter.hasNext()) {
            LatLon locB = iter.next();
            this.makeSegment(globe, locA, locB, tessellatedLocations);
            locA = locB;
        }
    }

    protected void makeSegment(Extent globe, LatLon locA, LatLon locB, Collection<LatLon> tessellatedLocations) {
        Angle segmentAzimuth;
        Angle segmentDistance;
        boolean isRhumbSegment = Keys.RHUMB_LINE.equalsIgnoreCase(this.getPathType())
            || Keys.LOXODROME.equalsIgnoreCase(this.getPathType());

        if (isRhumbSegment) {
            segmentAzimuth = LatLon.rhumbAzimuth(locA, locB);
            segmentDistance = LatLon.rhumbDistance(locA, locB);
        } else // Default to a great circle segment.
        {
            segmentAzimuth = LatLon.greatCircleAzimuth(locA, locB);
            segmentDistance = LatLon.greatCircleDistance(locA, locB);
        }

        double arcLength = segmentDistance.radians() * globe.getRadius();
        if (arcLength <= this.getSplitThreshold()) {
            tessellatedLocations.add(locB);
            return;
        }

        int numSubsegments = (int) Math.ceil(arcLength / this.getSplitThreshold());
        double segmentIncrement = segmentDistance.radians() / numSubsegments;

        for (double s = 0; s < segmentDistance.radians(); ) {
            // If we've reached or passed the second location, then add the second location and break. We handle this
            // case specially to ensure that the actual second location is added, instead of a computed location very
            // close to it.
            s += segmentIncrement;
            if (s >= segmentDistance.radians()) {
                tessellatedLocations.add(locB);
                break;
            }

            LatLon ll;
            if (isRhumbSegment) {
                ll = LatLon.rhumbEndPosition(locA, segmentAzimuth, Angle.fromRadians(s));
            } else // Default to a great circle segment.
            {
                ll = LatLon.greatCircleEndPosition(locA, segmentAzimuth, Angle.fromRadians(s));
            }

            tessellatedLocations.add(ll);
        }
    }

    @Override
    protected void doGetRestorableState(RestorableSupport rs, RestorableSupport.StateObject context) {
        super.doGetRestorableState(rs, context);

        if (this.locations != null)
            rs.addStateValueAsLatLonList(context, "locations", this.locations);

        rs.addStateValueAsString(context, "pathType", this.getPathType());
    }

    @Override
    protected void doRestoreState(RestorableSupport rs, RestorableSupport.StateObject context) {
        super.doRestoreState(rs, context);

        List<LatLon> locations = rs.getStateValueAsLatLonList(context, "locations");
        if (locations != null)
            this.setLocations(locations);

        String s = rs.getStateValueAsString(context, "pathType");
        if (s != null)
            this.setPathType(s);
    }

    //**************************************************************//
    //********************  END Geometry Rendering  ****************//
    //**************************************************************//

    protected static class CurtainGeometry implements Cacheable {
        private final Geometry fillIndexGeometry;
        private final Geometry outlineIndexGeometry;
        private final Geometry vertexGeometry;

        public CurtainGeometry() {
            this.fillIndexGeometry = new Geometry();
            this.outlineIndexGeometry = new Geometry();
            this.vertexGeometry = new Geometry();
        }

        public Geometry getFillIndexGeometry() {
            return this.fillIndexGeometry;
        }

        public Geometry getOutlineIndexGeometry() {
            return this.outlineIndexGeometry;
        }

        public Geometry getVertexGeometry() {
            return this.vertexGeometry;
        }

        public long getSizeInBytes() {
            long sizeInBytes = 0L;
            sizeInBytes += this.fillIndexGeometry.getSizeInBytes();
            sizeInBytes += this.outlineIndexGeometry.getSizeInBytes();
            sizeInBytes += this.vertexGeometry.getSizeInBytes();

            return sizeInBytes;
        }
    }

    protected static class SectionRenderInfo {
        final LatLon begin;
        final LatLon end;
        final String pathType;
        int pillars;
        int firstVertex, vertexCount;
        int firstFillIndex, fillIndexCount;
        int firstOutlineIndex, outlineIndexCount;

        private SectionRenderInfo(LatLon begin, LatLon end, String pathType) {
            this.begin = begin;
            this.end = end;
            this.pathType = pathType;
        }
    }
}