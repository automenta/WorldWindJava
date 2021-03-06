/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.util.measure;

import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.globes.Globe;
import gov.nasa.worldwind.render.Polyline;
import gov.nasa.worldwind.util.*;

import java.util.List;

import static java.lang.Math.toRadians;

/**
 * Utility class to compute approximations of projected and surface (terrain following) area on a globe.
 *
 * <p>
 * To properly compute surface area the measurer must be provided with a list of positions that describe a closed path -
 * one which last position is equal to the first.</p>
 *
 * <p>
 * Segments which are longer then the current maxSegmentLength will be subdivided along lines following the current
 * pathType - {@link Polyline#LINEAR}, {@link Polyline#RHUMB_LINE} or {@link Polyline#GREAT_CIRCLE}.</p>
 *
 * <p>
 * Projected or non terrain following area is computed in a sinusoidal projection which is equivalent or equal area.
 * Surface or terrain following area is approximated by sampling the path bounding sector with square cells along a
 * grid. Cells which center is inside the path have their area estimated and summed according to the overall slope at
 * the cell south-west corner.</p>
 *
 * @author Patrick Murris
 * @version $Id: AreaMeasurer.java 1171 2013-02-11 21:45:02Z dcollins $
 * @see MeasureTool
 * @see LengthMeasurer
 */
public class AreaMeasurer extends LengthMeasurer implements MeasurableArea {

    private static final double DEFAULT_AREA_SAMPLING_STEPS = 32; // sampling grid max rows or cols
    protected double surfaceArea = -1;
    protected double projectedArea = -1;
    private List<? extends Position> subdividedPositions;
    private Cell[][] sectorCells;
    private Double[][] sectorElevations;
    private double areaTerrainSamplingSteps = AreaMeasurer.DEFAULT_AREA_SAMPLING_STEPS;

    public AreaMeasurer() {
    }

    public AreaMeasurer(List<? extends Position> positions) {
        super(positions);
    }

    // Compute triangle area in a sinusoidal projection centered at the triangle center.
    // Note sinusoidal projection is equivalent or equal erea.
    protected static double computeTriangleProjectedArea(Globe globe, float[] verts, int a, int b, int c) {
        // http://www.mathopenref.com/coordtrianglearea.html
        double area = Math.abs(verts[a] * (verts[b + 1] - verts[c + 1])
            + verts[b] * (verts[c + 1] - verts[a + 1])
            + verts[c] * (verts[a + 1] - verts[b + 1])) / 2; // square radians
        // Compute triangle center
        double centerLat = (verts[a + 1] + verts[b + 1] + verts[c + 1]) / 3;
        double centerLon = (verts[a] + verts[b] + verts[c]) / 3;
        // Apply globe radius at triangle center and scale down area according to center latitude cosine
        double radius = globe.getRadiusAt(Angle.fromRadians(centerLat), Angle.fromRadians(centerLon));
        area *= Math.cos(centerLat) * radius * radius; // Square meter

        return area;
    }

    @Override
    protected void clearCachedValues() {
        super.clearCachedValues();
        this.subdividedPositions = null;
        this.projectedArea = -1;
        this.surfaceArea = -1;
    }

    @Override
    public void setPositions(List<? extends Position> positions) {
        Sector oldSector = getBoundingSector();
        super.setPositions(positions); // will call clearCachedData()
        Sector newSector = getBoundingSector();

        if (newSector == null || !newSector.equals(oldSector)) {
            this.sectorCells = null;
            this.sectorElevations = null;
        }
    }

    /**
     * Get the sampling grid maximum number of rows or columns for terrain following surface area approximation.
     *
     * @return the sampling grid maximum number of rows or columns.
     */
    public double getAreaTerrainSamplingSteps() {
        return this.areaTerrainSamplingSteps;
    }

    /**
     * Set the sampling grid maximum number of rows or columns for terrain following surface area approximation.
     *
     * @param steps the sampling grid maximum number of rows or columns.
     * @throws IllegalArgumentException if steps is less then one.
     */
    public void setAreaTerrainSamplingSteps(double steps) {
        if (steps < 1) {
            String message = Logging.getMessage("generic.ArgumentOutOfRange", steps);
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (this.areaTerrainSamplingSteps != steps) {
            this.areaTerrainSamplingSteps = steps;
            this.surfaceArea = -1;
            this.projectedArea = -1;
            // Invalidate cached data
            this.sectorCells = null;
            this.sectorElevations = null;
        }
    }

    /**
     * Get the surface area approximation for the current path or shape.
     *
     * <p>
     * If the measurer is set to follow terrain, the computed area will account for terrain deformations. Otherwise the
     * area is that of the path once projected at sea level - elevation zero.</p>
     *
     * @param globe the globe to draw terrain information from.
     * @return the current shape surface area or -1 if the position list does not describe a closed path or is too
     * short.
     * @throws IllegalArgumentException if globe is <code>null</code>.
     */
    @Override
    public double getArea(Globe globe) {
        return this.isFollowTerrain() ? getSurfaceArea(globe) : getProjectedArea(globe);
    }

    public double getSurfaceArea(Globe globe) {

        if (this.surfaceArea < 0) {
            this.surfaceArea = this.computeSurfaceAreaSampling(globe, this.areaTerrainSamplingSteps);
        }

        return this.surfaceArea;
    }

    public double getProjectedArea(Globe globe) {

        if (this.projectedArea < 0) {
            this.projectedArea = this.computeProjectedAreaGeometry(globe);
        }

        return this.projectedArea;
    }

    @Override
    public double getPerimeter(Globe globe) {
        return getLength(globe);
    }

    @Override
    public double getWidth(Globe globe) {

        Sector sector = getBoundingSector();
        if (sector != null) {
            return globe.getRadiusAt(sector.getCentroid()) * sector.lonDelta().radians()
                * Math.cos(sector.getCentroid().getLat().radians());
        }

        return -1;
    }

    @Override
    public double getHeight(Globe globe) {

        Sector sector = getBoundingSector();
        if (sector != null) {
            return globe.getRadiusAt(sector.getCentroid()) * sector.latDelta().radians();
        }

        return -1;
    }

    // *** Projected area ***
    // Tessellate the path in lat-lon space, then sum each triangle area.
    protected double computeProjectedAreaGeometry(Globe globe) {
        Sector sector = getBoundingSector();
        if (sector != null && this.isClosedShape()) {
            // Subdivide long segments if needed
            if (this.subdividedPositions == null) {
                this.subdividedPositions = LengthMeasurer.subdividePositions(globe, getPositions(), getMaxSegmentLength(),
                    isFollowTerrain(), getAVKeyPathType());
            }
            // First: tessellate polygon
            int verticesCount = this.subdividedPositions.size() - 1; // trim last pos which is same as first
            float[] verts = new float[verticesCount * 3];
            // Prepare vertices
            int idx = 0;
            for (int i = 0; i < verticesCount; i++) {
                // Vertices coordinates are x=lon y=lat in radians, z = elevation zero
                final Position I = this.subdividedPositions.get(i);
                verts[idx++] = (float) I.getLon().radians();
                verts[idx++] = (float) I.getLat().radians();
                verts[idx++] = 0;
            }
            // Tessellate
            GeometryBuilder gb = new GeometryBuilder();
            GeometryBuilder.IndexedTriangleArray ita = gb.tessellatePolygon2(0, verticesCount, verts);
            // Second: sum triangles area
            double area = 0;
            int[] indices = ita.getIndices();
            int triangleCount = ita.getIndexCount() / 3;
            for (int i = 0; i < triangleCount; i++) {
                idx = i * 3;
                area += AreaMeasurer.computeTriangleProjectedArea(globe, ita.getVertices(), indices[idx] * 3,
                    indices[idx + 1] * 3, indices[idx + 2] * 3);
            }
            return area;
        }
        return -1;
    }

    // *** Surface area - terrain following ***
    // Sample the path bounding sector with square cells which area are approximated according to the surface normal at
    // the cell south-west corner.
    protected double computeSurfaceAreaSampling(Globe globe, double steps) {
        final Sector sector = getBoundingSector();
        if (sector != null && this.isClosedShape()) {
            // Subdivide long segments if needed
            if (this.subdividedPositions == null) {
                this.subdividedPositions = LengthMeasurer.subdividePositions(globe, getPositions(), getMaxSegmentLength(),
                    true, getAVKeyPathType());
            }

            // Sample the bounding sector with cells about the same length in side - squares
            double stepRadians = Math.max(toRadians(sector.latDelta) / steps, toRadians(sector.lonDelta) / steps);
            int latSteps = (int) Math.round(toRadians(sector.latDelta) / stepRadians);
            final LatLon sectorCentroid = sector.getCentroid();
            int lonSteps = (int) Math.round(toRadians(sector.lonDelta) / stepRadians
                * Math.cos(sectorCentroid.getLat().radians()));
            double latStepRadians = toRadians(sector.latDelta) / latSteps;
            double lonStepRadians = toRadians(sector.lonDelta) / lonSteps;

            if (this.sectorCells == null) {
                this.sectorCells = new Cell[latSteps][lonSteps];
            }
            if (this.sectorElevations == null) {
                this.sectorElevations = new Double[latSteps + 1][lonSteps + 1];
            }

            final double sectorLatMinRadians = sector.latMin().radians();
            final double sectorLonMinRadians = sector.lonMin().radians();
            final Angle sectorCentroidLon = sectorCentroid.getLon();

            double area = 0;
            for (int i = 0; i < latSteps; i++) {
                double lat = sectorLatMinRadians + latStepRadians * i;
                // Compute this latitude row cells area
                double radius = globe.getRadiusAt(Angle.fromRadians(lat + latStepRadians / 2),
                    sectorCentroidLon);
                double cellWidth = lonStepRadians * radius * Math.cos(lat + latStepRadians / 2);
                double cellHeight = latStepRadians * radius;
                double cellArea = cellWidth * cellHeight;

                final Angle latAngle = Angle.fromRadians(lat);
                final Cell[] sectorI = this.sectorCells[i];
                final Double[] sectorEleI = sectorElevations[i];

                for (int j = 0; j < lonSteps; j++) {
                    double lon = sectorLonMinRadians + lonStepRadians * j;
                    Sector cellSector = Sector.fromRadians(lat, lat + latStepRadians, lon, lon + lonStepRadians);
                    // Select cells which center is inside the shape
                    if (WWMath.isLocationInside(cellSector.getCentroid(), this.subdividedPositions)) {
                        Cell cell = sectorI[j];
                        if (cell == null || cell.surfaceArea == -1) {
                            // Compute surface area using terrain normal in SW corner
                            // Corners elevation
                            double eleSW = sectorEleI[j] != null ? sectorEleI[j]
                                : globe.elevation(latAngle, Angle.fromRadians(lon));
                            double eleSE = sectorEleI[j + 1] != null ? sectorEleI[j + 1]
                                : globe.elevation(latAngle, Angle.fromRadians(lon + lonStepRadians));
                            double eleNW = sectorElevations[i + 1][j] != null ? sectorElevations[i + 1][j]
                                : globe.elevation(Angle.fromRadians(lat + latStepRadians), Angle.fromRadians(lon));
                            // Cache elevations
                            sectorEleI[j] = eleSW;
                            sectorEleI[j + 1] = eleSE;
                            sectorElevations[i + 1][j] = eleNW;
                            // Compute normal
                            Vec4 vx = new Vec4(cellWidth, 0, eleSE - eleSW).normalize3();
                            Vec4 vy = new Vec4(0, cellHeight, eleNW - eleSW).normalize3();
                            Vec4 normalSW = vx.cross3(vy).normalize3(); // point toward positive Z
                            // Compute slope factor
                            double tan = Math.tan(Vec4.UNIT_Z.angleBetween3(normalSW).radians());
                            double slopeFactor = Math.sqrt(1 + tan * tan);
                            // Create and cache cell
                            cell = new Cell(cellSector, cellArea, cellArea * slopeFactor);
                            sectorI[j] = cell;
                        }
                        // Add cell area
                        area += cell.surfaceArea;
                    }
                }
            }
            return area;
        }
        return -1;
    }

    // *** Computing area ******************************************************************
    protected static class Cell {

        final Sector sector;
        final double projectedArea;
        final double surfaceArea;

        public Cell(Sector sector, double projected, double surface) {
            this.sector = sector;
            this.projectedArea = projected;
            this.surfaceArea = surface;
        }
    }

// Below code is an attempt at computing the surface area using geometry.
//    private static final double DEFAULT_AREA_CONVERGENCE_PERCENT = 2;   // stop sudividing when increase in area
    // is less then this percent
}