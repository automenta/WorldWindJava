/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */

package gov.nasa.worldwind.render.airspaces;

import com.jogamp.opengl.*;
import gov.nasa.worldwind.geom.Box;
import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.globes.Globe;
import gov.nasa.worldwind.render.*;
import gov.nasa.worldwind.util.*;

import java.util.*;

/**
 * @author tag
 * @version $Id: Orbit.java 2454 2014-11-21 17:52:49Z dcollins $
 */
public class Orbit extends AbstractAirspace {
    protected static final int DEFAULT_ARC_SLICES = 16;
    protected static final int DEFAULT_LENGTH_SLICES = 32;
    protected static final int DEFAULT_STACKS = 1;
    protected static final int DEFAULT_LOOPS = 4;
    protected static final int MINIMAL_GEOMETRY_ARC_SLICES = 4;
    protected static final int MINIMAL_GEOMETRY_LENGTH_SLICES = 8;
    protected static final int MINIMAL_GEOMETRY_LOOPS = 2;
    private LatLon location1 = LatLon.ZERO;
    private LatLon location2 = LatLon.ZERO;
    private String orbitType = OrbitType.CENTER;
    private double width = 1.0;
    private boolean enableCaps = true;
    // Geometry.
    private int arcSlices = Orbit.DEFAULT_ARC_SLICES;
    private int lengthSlices = Orbit.DEFAULT_LENGTH_SLICES;
    private int stacks = Orbit.DEFAULT_STACKS;
    private int loops = Orbit.DEFAULT_LOOPS;

    public Orbit(LatLon location1, LatLon location2, String orbitType, double width) {
        if (location1 == null) {
            String message = "nullValue.Location1IsNull";
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        if (location2 == null) {
            String message = "nullValue.Location2IsNull";
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        if (orbitType == null) {
            String message = "nullValue.OrbitTypeIsNull";
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        if (width < 0.0) {
            String message = Logging.getMessage("generic.ArgumentOutOfRange", "width=" + width);
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.location1 = location1;
        this.location2 = location2;
        this.orbitType = orbitType;
        this.width = width;
        this.makeDefaultDetailLevels();
    }

    public Orbit(Orbit source) {
        super(source);

        this.location1 = source.location1;
        this.location2 = source.location2;
        this.orbitType = source.orbitType;
        this.width = source.width;
        this.enableCaps = source.enableCaps;
        this.arcSlices = source.arcSlices;
        this.lengthSlices = source.lengthSlices;
        this.stacks = source.stacks;
        this.loops = source.loops;

        this.makeDefaultDetailLevels();
    }

    public Orbit(AirspaceAttributes attributes) {
        super(attributes);
        this.makeDefaultDetailLevels();
    }

    public Orbit() {
        this.makeDefaultDetailLevels();
    }

    private void makeDefaultDetailLevels() {
        Collection<DetailLevel> levels = new ArrayList<>();
        double[] ramp = ScreenSizeDetailLevel.computeDefaultScreenSizeRamp(5);

        DetailLevel level;
        level = new ScreenSizeDetailLevel(ramp[0], "Detail-Level-0");
        level.set(AbstractAirspace.ARC_SLICES, 16);
        level.set(AbstractAirspace.LENGTH_SLICES, 32);
        level.set(AbstractAirspace.STACKS, 1);
        level.set(AbstractAirspace.LOOPS, 4);
        level.set(AbstractAirspace.DISABLE_TERRAIN_CONFORMANCE, false);
        levels.add(level);

        level = new ScreenSizeDetailLevel(ramp[1], "Detail-Level-1");
        level.set(AbstractAirspace.ARC_SLICES, 13);
        level.set(AbstractAirspace.LENGTH_SLICES, 25);
        level.set(AbstractAirspace.STACKS, 1);
        level.set(AbstractAirspace.LOOPS, 3);
        level.set(AbstractAirspace.DISABLE_TERRAIN_CONFORMANCE, false);
        levels.add(level);

        level = new ScreenSizeDetailLevel(ramp[2], "Detail-Level-2");
        level.set(AbstractAirspace.ARC_SLICES, 10);
        level.set(AbstractAirspace.LENGTH_SLICES, 18);
        level.set(AbstractAirspace.STACKS, 1);
        level.set(AbstractAirspace.LOOPS, 2);
        level.set(AbstractAirspace.DISABLE_TERRAIN_CONFORMANCE, false);
        levels.add(level);

        level = new ScreenSizeDetailLevel(ramp[3], "Detail-Level-3");
        level.set(AbstractAirspace.ARC_SLICES, 7);
        level.set(AbstractAirspace.LENGTH_SLICES, 11);
        level.set(AbstractAirspace.STACKS, 1);
        level.set(AbstractAirspace.LOOPS, 1);
        level.set(AbstractAirspace.DISABLE_TERRAIN_CONFORMANCE, false);
        levels.add(level);

        level = new ScreenSizeDetailLevel(ramp[4], "Detail-Level-4");
        level.set(AbstractAirspace.ARC_SLICES, 4);
        level.set(AbstractAirspace.LENGTH_SLICES, 4);
        level.set(AbstractAirspace.STACKS, 1);
        level.set(AbstractAirspace.LOOPS, 1);
        level.set(AbstractAirspace.DISABLE_TERRAIN_CONFORMANCE, true);
        levels.add(level);

        this.setDetailLevels(levels);
    }

    public LatLon[] getLocations() {
        LatLon[] array = new LatLon[2];
        array[0] = this.location1;
        array[1] = this.location2;
        return array;
    }

    public void setLocations(LatLon location1, LatLon location2) {
        if (location1 == null) {
            String message = "nullValue.Location1IsNull";
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        if (location2 == null) {
            String message = "nullValue.Location2IsNull";
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.location1 = location1;
        this.location2 = location2;
        this.invalidateAirspaceData();
    }

    protected LatLon[] getAdjustedLocations(Extent globe) {
        LatLon[] locations = this.getLocations();

        if (OrbitType.CENTER.equals(this.getOrbitType())) {
            return locations;
        }

        double az1 = LatLon.greatCircleAzimuth(locations[0], locations[1]).radians();
        double az2 = LatLon.greatCircleAzimuth(locations[1], locations[0]).radians();
        double r = (this.getWidth() / 2) / globe.getRadius();

        if (Orbit.OrbitType.LEFT.equals(this.getOrbitType())) {
            locations[0] = LatLon.greatCircleEndPosition(locations[0], az1 - (Math.PI / 2), r);
            locations[1] = LatLon.greatCircleEndPosition(locations[1], az2 + (Math.PI / 2), r);
        } else if (Orbit.OrbitType.RIGHT.equals(this.getOrbitType())) {
            locations[0] = LatLon.greatCircleEndPosition(locations[0], az1 + (Math.PI / 2), r);
            locations[1] = LatLon.greatCircleEndPosition(locations[1], az2 - (Math.PI / 2), r);
        }

        return locations;
    }

    public String getOrbitType() {
        return this.orbitType;
    }

    public void setOrbitType(String orbitType) {
        if (orbitType == null) {
            String message = "nullValue.OrbitTypeIsNull";
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.orbitType = orbitType;
        this.invalidateAirspaceData();
    }

    public double getWidth() {
        return this.width;
    }

    public void setWidth(double width) {
        if (width < 0.0) {
            String message = Logging.getMessage("generic.ArgumentOutOfRange", "width=" + width);
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.width = width;
        this.invalidateAirspaceData();
    }

    public boolean isEnableCaps() {
        return this.enableCaps;
    }

    public void setEnableCaps(boolean enable) {
        this.enableCaps = enable;
    }

    public Position getReferencePosition() {
        double[] altitudes = this.getAltitudes();
        return new Position(this.location1, altitudes[0]);
    }

    protected Extent computeExtent(Globe globe, double verticalExaggeration) {
        List<Vec4> points = this.computeMinimalGeometry(globe, verticalExaggeration);
        if (points == null || points.isEmpty())
            return null;

        return Box.computeBoundingBox(points);
    }

    @Override
    protected List<Vec4> computeMinimalGeometry(Globe globe, double verticalExaggeration) {
        LatLon[] center = this.getAdjustedLocations(globe);
        double radius = this.getWidth() / 2.0;
        GeometryBuilder gb = this.getGeometryBuilder();
        LatLon[] locations = GeometryBuilder.makeLongDiskLocations(globe, center[0], center[1], 0, radius,
            Orbit.MINIMAL_GEOMETRY_ARC_SLICES, Orbit.MINIMAL_GEOMETRY_LENGTH_SLICES, Orbit.MINIMAL_GEOMETRY_LOOPS);

        List<Vec4> points = new ArrayList<>();
        this.makeExtremePoints(globe, verticalExaggeration, Arrays.asList(locations), points);

        return points;
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

        List<LatLon> newLocations = LatLon.computeShiftedLocations(globe, oldRef, newRef,
            Arrays.asList(this.getLocations()));
        this.setLocations(newLocations.get(0), newLocations.get(1));

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

        LatLon[] locations = this.getLocations();
        int count = locations.length;
        for (int i = 0; i < count; i++) {
            double distance = LatLon.greatCircleDistance(oldRef, locations[i]).radians();
            double azimuth = LatLon.greatCircleAzimuth(oldRef, locations[i]).radians();
            locations[i] = LatLon.greatCircleEndPosition(newRef, azimuth, distance);
        }
        this.setLocations(locations[0], locations[1]);
    }

    @Override
    protected SurfaceShape createSurfaceShape() {
        return new SurfacePolygon();
    }

    @Override
    protected void updateSurfaceShape(DrawContext dc, SurfaceShape shape) {
        super.updateSurfaceShape(dc, shape);

        boolean mustDrawInterior = this.getActiveAttributes().isDrawInterior() && this.isEnableCaps();
        shape.getAttributes().setDrawInterior(mustDrawInterior); // suppress the shape interior when caps are disabled
    }

    @Override
    protected void regenerateSurfaceShape(DrawContext dc, SurfaceShape shape) {
        LatLon[] center = this.getAdjustedLocations(dc.getGlobe());
        double radius = this.getWidth() / 2.0;
        GeometryBuilder gb = this.getGeometryBuilder();
        LatLon[] locations = GeometryBuilder.makeLongCylinderLocations(dc.getGlobe(), center[0], center[1], radius,
            this.arcSlices,
            this.lengthSlices);
        ((SurfacePolygon) shape).setOuterBoundary(Arrays.asList(locations));
    }

    protected int getArcSlices() {
        return this.arcSlices;
    }

    protected void setArcSlices(int arcSlices) {
        if (arcSlices < 0) {
            String message = Logging.getMessage("generic.ArgumentOutOfRange", "arcSlices=" + arcSlices);
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.arcSlices = arcSlices;
    }

    protected int getLengthSlices() {
        return this.lengthSlices;
    }

    protected void setLengthSlices(int lengthSlices) {
        if (lengthSlices < 0) {
            String message = Logging.getMessage("generic.ArgumentOutOfRange", "lengthSlices=" + lengthSlices);
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.lengthSlices = lengthSlices;
    }

    protected int getStacks() {
        return this.stacks;
    }

    protected int getLoops() {
        return this.loops;
    }

    protected void setLoops(int loops) {
        if (loops < 0) {
            String message = Logging.getMessage("generic.ArgumentOutOfRange", "loops=" + loops);
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.loops = loops;
    }

    protected Vec4 computeReferenceCenter(DrawContext dc) {
        if (dc == null) {
            String message = Logging.getMessage("nullValue.DrawContextIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        if (dc.getGlobe() == null) {
            String message = Logging.getMessage("nullValue.DrawingContextGlobeIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        Globe globe = dc.getGlobe();
        double[] altitudes = this.getAltitudes(dc.getVerticalExaggeration());
        Vec4 point1 = globe.computeEllipsoidalPointFromPosition(
            this.location1.getLat(), this.location1.getLon(), altitudes[0]);
        Vec4 point2 = globe.computeEllipsoidalPointFromPosition(
            this.location2.getLat(), this.location2.getLon(), altitudes[0]);
        Vec4 centerPoint = Vec4.mix3(0.5, point1, point2);
        Position centerPos = globe.computePositionFromEllipsoidalPoint(centerPoint);
        return globe.computePointFromPosition(centerPos.lat, centerPos.lon,
            altitudes[0]); // model-coordinate reference center
    }

    //**************************************************************//
    //********************  Geometry Rendering  ********************//
    //**************************************************************//

    protected Matrix computeEllipsoidalTransform(Globe globe, double verticalExaggeration) {

        double[] altitudes = this.getAltitudes(verticalExaggeration);
        double radius = this.width / 2.0;

        Vec4 point1 = globe.computeEllipsoidalPointFromPosition(
            this.location1.getLat(), this.location1.getLon(), altitudes[0]);
        Vec4 point2 = globe.computeEllipsoidalPointFromPosition(
            this.location2.getLat(), this.location2.getLon(), altitudes[0]);
        Vec4 centerPoint = Vec4.mix3(0.5, point1, point2);
        Position centerPos = globe.computePositionFromEllipsoidalPoint(centerPoint);
        Vec4 upVec = globe.computeEllipsoidalNormalAtLocation(centerPos.getLat(), centerPos.getLon());
        Vec4 axis = point2.subtract3(point1);
        axis = axis.normalize3();

        Matrix transform = Matrix.fromModelLookAt(point1, point1.add3(upVec), axis);
        if (OrbitType.LEFT.equals(this.orbitType))
            transform = transform.multiply(Matrix.fromTranslation(-radius, 0.0, 0.0));
        else if (OrbitType.RIGHT.equals(this.orbitType))
            transform = transform.multiply(Matrix.fromTranslation(radius, 0.0, 0.0));

        return transform;
    }

    protected void doRenderGeometry(DrawContext dc, String drawStyle) {

        LatLon[] locations = this.getAdjustedLocations(dc.getGlobe());
        double[] altitudes = this.getAltitudes(dc.getVerticalExaggeration());
        boolean[] terrainConformant = this.isTerrainConforming();
        double[] radii = {0.0, this.width / 2};
        int arcSlices = this.arcSlices;
        int lengthSlices = this.lengthSlices;
        int stacks = this.stacks;
        int loops = this.loops;

        if (this.isEnableLevelOfDetail()) {
            DetailLevel level = this.computeDetailLevel(dc);

            Object o = level.get(AbstractAirspace.ARC_SLICES);
            if (o instanceof Integer)
                arcSlices = (Integer) o;

            o = level.get(AbstractAirspace.LENGTH_SLICES);
            if (o instanceof Integer)
                lengthSlices = (Integer) o;

            o = level.get(AbstractAirspace.STACKS);
            if (o instanceof Integer)
                stacks = (Integer) o;

            o = level.get(AbstractAirspace.LOOPS);
            if (o instanceof Integer)
                loops = (Integer) o;

            o = level.get(AbstractAirspace.DISABLE_TERRAIN_CONFORMANCE);
            if (o instanceof Boolean && ((Boolean) o))
                terrainConformant[0] = terrainConformant[1] = false;
        }

        Vec4 referenceCenter = this.computeReferenceCenter(dc);
        this.setExpiryTime(this.nextExpiryTime(dc, terrainConformant));
        this.clearElevationMap();

        GL2 gl = dc.getGL2(); // GL initialization checks for GL2 compatibility.
        OGLStackHandler ogsh = new OGLStackHandler();
        try {
            dc.view().pushReferenceCenter(dc, referenceCenter);

            if (Airspace.DRAW_STYLE_OUTLINE.equals(drawStyle)) {
                this.drawLongCylinderOutline(dc, locations[0], locations[1], radii[1], altitudes, terrainConformant,
                    arcSlices, lengthSlices, stacks, GeometryBuilder.OUTSIDE, referenceCenter);
            } else if (Airspace.DRAW_STYLE_FILL.equals(drawStyle)) {
                if (this.enableCaps) {
                    ogsh.pushAttrib(gl, GL2.GL_POLYGON_BIT);
                    gl.glEnable(GL.GL_CULL_FACE);
                    gl.glFrontFace(GL.GL_CCW);
                }

                if (this.enableCaps) {
                    // Caps aren't rendered if radii are equal.
                    if (radii[0] != radii[1]) {
                        this.drawLongDisk(dc, locations[0], locations[1], radii, altitudes[1], terrainConformant[1],
                            arcSlices, lengthSlices, loops, GeometryBuilder.OUTSIDE, referenceCenter);
                        // Bottom cap isn't rendered if airspace is collapsed.
                        if (!this.isAirspaceCollapsed()) {
                            this.drawLongDisk(dc, locations[0], locations[1], radii, altitudes[0], terrainConformant[0],
                                arcSlices, lengthSlices, loops, GeometryBuilder.INSIDE, referenceCenter);
                        }
                    }
                }

                // Long cylinder isn't rendered if airspace is collapsed.
                if (!this.isAirspaceCollapsed()) {
                    this.drawLongCylinder(dc, locations[0], locations[1], radii[1], altitudes, terrainConformant,
                        arcSlices, lengthSlices, stacks, GeometryBuilder.OUTSIDE, referenceCenter);
                }
            }
        }
        finally {
            dc.view().popReferenceCenter(dc);
            ogsh.pop(gl);
        }
    }

    private void drawLongCylinder(DrawContext dc, LatLon center1, LatLon center2, double radius, double[] altitudes,
        boolean[] terrainConformant, int arcSlices, int lengthSlices, int stacks, int orientation,
        Vec4 referenceCenter) {
        Geometry vertexGeom = this.createLongCylinderVertexGeometry(dc, center1, center2, radius, altitudes,
            terrainConformant, arcSlices, lengthSlices, stacks, orientation, referenceCenter);

        Object cacheKey = new Geometry.CacheKey(this.getClass(), "LongCylinder.Indices", arcSlices, lengthSlices,
            stacks, orientation);
        Geometry indexGeom = (Geometry) AbstractAirspace.getGeometryCache().getObject(cacheKey);
        if (indexGeom == null) {
            indexGeom = new Geometry();
            this.makeLongCylinderIndices(arcSlices, lengthSlices, stacks, orientation, indexGeom);
            AbstractAirspace.getGeometryCache().add(cacheKey, indexGeom);
        }

        this.drawGeometry(dc, indexGeom, vertexGeom);
    }

    //**************************************************************//
    //********************  Long Cylinder       ********************//
    //**************************************************************//

    private void drawLongCylinderOutline(DrawContext dc, LatLon center1, LatLon center2, double radius,
        double[] altitudes, boolean[] terrainConformant, int arcSlices, int lengthSlices, int stacks, int orientation,
        Vec4 referenceCenter) {
        Geometry vertexGeom = this.createLongCylinderVertexGeometry(dc, center1, center2, radius, altitudes,
            terrainConformant, arcSlices, lengthSlices, stacks, orientation, referenceCenter);

        Object cacheKey = new Geometry.CacheKey(this.getClass(), "LongCylinder.OutlineIndices", arcSlices, lengthSlices,
            stacks, orientation);
        Geometry outlineIndexGeom = (Geometry) AbstractAirspace.getGeometryCache().getObject(cacheKey);
        if (outlineIndexGeom == null) {
            outlineIndexGeom = new Geometry();
            this.makeLongCylinderOutlineIndices(arcSlices, lengthSlices, stacks, orientation, outlineIndexGeom);
            AbstractAirspace.getGeometryCache().add(cacheKey, outlineIndexGeom);
        }

        this.drawGeometry(dc, outlineIndexGeom, vertexGeom);
    }

    private Geometry createLongCylinderVertexGeometry(DrawContext dc, LatLon center1, LatLon center2, double radius,
        double[] altitudes, boolean[] terrainConformant, int arcSlices, int lengthSlices, int stacks, int orientation,
        Vec4 referenceCenter) {
        Object cacheKey = new Geometry.CacheKey(dc.getGlobe(), this.getClass(), "LongCylinder.Vertices",
            center1, center2, radius, altitudes[0], altitudes[1], terrainConformant[0], terrainConformant[1], arcSlices,
            lengthSlices, stacks, orientation, referenceCenter);
        Geometry vertexGeom = (Geometry) AbstractAirspace.getGeometryCache().getObject(cacheKey);
        if (vertexGeom == null || AbstractAirspace.isExpired(dc, vertexGeom)) {
            if (vertexGeom == null)
                vertexGeom = new Geometry();
            this.makeLongCylinder(dc, center1, center2, radius, altitudes, terrainConformant, arcSlices, lengthSlices,
                stacks, orientation, referenceCenter, vertexGeom);
            this.updateExpiryCriteria(dc, vertexGeom);
            AbstractAirspace.getGeometryCache().add(cacheKey, vertexGeom);
        }

        return vertexGeom;
    }

    private void makeLongCylinder(DrawContext dc, LatLon center1, LatLon center2, double radius, double[] altitudes,
        boolean[] terrainConformant, int arcSlices, int lengthSlices, int stacks, int orientation, Vec4 referenceCenter,
        Geometry dest) {
        GeometryBuilder gb = this.getGeometryBuilder();
        gb.setOrientation(orientation);

        int count = GeometryBuilder.getLongCylinderVertexCount(arcSlices, lengthSlices, stacks);
        float[] verts = new float[3 * count];
        float[] norms = new float[3 * count];
        GeometryBuilder.makeLongCylinderVertices(dc.getTerrain(), center1, center2, radius, altitudes,
            terrainConformant, arcSlices,
            lengthSlices, stacks, referenceCenter, verts);
        gb.makeLongCylinderNormals(arcSlices, lengthSlices, stacks, norms);

        dest.setVertexData(count, verts);
        dest.setNormalData(count, norms);
    }

    private void makeLongCylinderIndices(int arcSlices, int lengthSlices, int stacks, int orientation, Geometry dest) {
        GeometryBuilder gb = this.getGeometryBuilder();
        gb.setOrientation(orientation);

        int mode = GeometryBuilder.getLongCylinderDrawMode();
        int count = GeometryBuilder.getLongCylinderIndexCount(arcSlices, lengthSlices, stacks);
        int[] indices = new int[count];
        gb.makeLongCylinderIndices(arcSlices, lengthSlices, stacks, indices);

        dest.setElementData(mode, count, indices);
    }

    private void makeLongCylinderOutlineIndices(int arcSlices, int lengthSlices, int stacks, int orientation,
        Geometry dest) {
        GeometryBuilder gb = this.getGeometryBuilder();
        gb.setOrientation(orientation);

        int mode = GeometryBuilder.getLongCylinderOutlineDrawMode();
        int count = GeometryBuilder.getLongCylinderOutlineIndexCount(arcSlices, lengthSlices, stacks);
        int[] indices = new int[count];
        GeometryBuilder.makeLongCylinderOutlineIndices(arcSlices, lengthSlices, stacks, indices);

        dest.setElementData(mode, count, indices);
    }

    private void drawLongDisk(DrawContext dc, LatLon center1, LatLon center2, double[] radii, double altitude,
        boolean terrainConformant, int arcSlices, int lengthSlices, int loops, int orientation, Vec4 referenceCenter) {
        Object cacheKey = new Geometry.CacheKey(dc.getGlobe(), this.getClass(), "LongDisk.Vertices", center1, center2,
            radii[0], radii[1], altitude, terrainConformant, arcSlices, lengthSlices, loops, orientation,
            referenceCenter);
        Geometry vertexGeom = (Geometry) AbstractAirspace.getGeometryCache().getObject(cacheKey);
        if (vertexGeom == null || AbstractAirspace.isExpired(dc, vertexGeom)) {
            if (vertexGeom == null)
                vertexGeom = new Geometry();
            this.makeLongDisk(dc, center1, center2, radii, altitude, terrainConformant, arcSlices, lengthSlices, loops,
                orientation, referenceCenter, vertexGeom);
            this.updateExpiryCriteria(dc, vertexGeom);
            AbstractAirspace.getGeometryCache().add(cacheKey, vertexGeom);
        }

        cacheKey = new Geometry.CacheKey(this.getClass(), "LongDisk.Indices", arcSlices, lengthSlices, loops,
            orientation);
        Geometry indexGeom = (Geometry) AbstractAirspace.getGeometryCache().getObject(cacheKey);
        if (indexGeom == null) {
            indexGeom = new Geometry();
            this.makeLongDiskIndices(arcSlices, lengthSlices, loops, orientation, indexGeom);
            AbstractAirspace.getGeometryCache().add(cacheKey, indexGeom);
        }

        this.drawGeometry(dc, indexGeom, vertexGeom);
    }

    //**************************************************************//
    //********************  Long Disk           ********************//
    //**************************************************************//

    private void makeLongDisk(DrawContext dc, LatLon center1, LatLon center2, double[] radii, double altitude,
        boolean terrainConformant, int arcSlices, int lengthSlices, int loops, int orientation, Vec4 referenceCenter,
        Geometry dest) {
        GeometryBuilder gb = this.getGeometryBuilder();
        gb.setOrientation(orientation);

        int count = GeometryBuilder.getLongDiskVertexCount(arcSlices, lengthSlices, loops);
        float[] verts = new float[3 * count];
        float[] norms = new float[3 * count];
        GeometryBuilder.makeLongDiskVertices(dc.getTerrain(), center1, center2, radii[0], radii[1], altitude,
            terrainConformant,
            arcSlices, lengthSlices, loops, referenceCenter, verts);
        gb.makeLongDiskVertexNormals((float) radii[0], (float) radii[1], 0, arcSlices, lengthSlices, loops, verts,
            norms);

        dest.setVertexData(count, verts);
        dest.setNormalData(count, norms);
    }

    private void makeLongDiskIndices(int arcSlices, int lengthSlices, int loops, int orientation, Geometry dest) {
        GeometryBuilder gb = this.getGeometryBuilder();
        gb.setOrientation(orientation);

        int mode = GeometryBuilder.getLongDiskDrawMode();
        int count = GeometryBuilder.getLongDiskIndexCount(arcSlices, lengthSlices, loops);
        int[] indices = new int[count];
        gb.makeLongDiskIndices(arcSlices, lengthSlices, loops, indices);

        dest.setElementData(mode, count, indices);
    }

    @Override
    protected void doGetRestorableState(RestorableSupport rs, RestorableSupport.StateObject context) {
        super.doGetRestorableState(rs, context);

        rs.addStateValueAsLatLon(context, "location1", this.location1);
        rs.addStateValueAsLatLon(context, "location2", this.location2);
        rs.addStateValueAsString(context, "orbitType", this.orbitType);
        rs.addStateValueAsDouble(context, "width", this.width);
        rs.addStateValueAsBoolean(context, "enableCaps", this.enableCaps);
    }

    //**************************************************************//
    //********************  END Geometry Rendering  ****************//
    //**************************************************************//

    @Override
    protected void doRestoreState(RestorableSupport rs, RestorableSupport.StateObject context) {
        super.doRestoreState(rs, context);

        LatLon loc1 = rs.getStateValueAsLatLon(context, "location1");
        if (loc1 == null)
            loc1 = this.getLocations()[0];

        LatLon loc2 = rs.getStateValueAsLatLon(context, "location2");
        if (loc2 == null)
            loc2 = this.getLocations()[1];

        this.setLocations(loc1, loc2);

        String s = rs.getStateValueAsString(context, "orbitType");
        if (s != null)
            this.setOrbitType(s);

        Double d = rs.getStateValueAsDouble(context, "width");
        if (d != null)
            this.setWidth(d);

        Boolean booleanState = rs.getStateValueAsBoolean(context, "enableCaps");
        if (booleanState != null)
            this.setEnableCaps(booleanState);
    }

    public interface OrbitType {
        String LEFT = "Left";
        String CENTER = "Center";
        String RIGHT = "Right";
    }
}