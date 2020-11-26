/*
 * Copyright (C) 2014 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */

package gov.nasa.worldwind.examples;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.*;
import gov.nasa.worldwind.*;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.examples.render.*;
import gov.nasa.worldwind.examples.render.markers.*;
import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.globes.Globe;
import gov.nasa.worldwind.layers.*;
import gov.nasa.worldwind.terrain.*;
import gov.nasa.worldwind.util.*;

import javax.swing.*;
import javax.xml.stream.XMLStreamWriter;
import java.awt.*;
import java.nio.*;
import java.util.List;
import java.util.*;

/**
 * Shows how to compute a radar volume that considers terrain intersection and how to use the {@link
 * RadarVolume} shape to display the computed volume.
 *
 * @author tag
 * @version $Id: RadarVolumeExample.java 3233 2015-06-22 17:06:51Z tgaskins $
 */
public class RadarVolumeExample extends ApplicationTemplate {
    public static void main(String[] args) {
        Configuration.setValue(AVKey.INITIAL_LATITUDE, 36.8378);
        Configuration.setValue(AVKey.INITIAL_LONGITUDE, -118.8743);
        Configuration.setValue(AVKey.INITIAL_ALTITUDE, 2000.0e3);
        ApplicationTemplate.start("Terrain Shadow Prototype", AppFrame.class);
    }

    public static class AppFrame extends ApplicationTemplate.AppFrame {
        protected static final boolean CONE_VOLUME = true;

        // Use the HighResolutionTerrain class to get accurate terrain for computing the intersections.
        protected final HighResolutionTerrain terrain;
        protected final double innerRange = 100;
        protected final double outerRange = 30.0e3;
        protected final int numAz = 25; // number of azimuth samplings
        protected final int numEl = 25; // number of elevation samplings
        protected final Angle minimumElevation = Angle.fromDegrees(0);

        public AppFrame() {
            super(true, true, false);

            Position center = Position.fromDegrees(36.8378, -118.8743, 100.0e2); // radar location
            Angle startAzimuth = Angle.fromDegrees(140);
            Angle endAzimuth = Angle.fromDegrees(270);
            Angle startElevation = Angle.fromDegrees(-50);
            Angle endElevation = Angle.fromDegrees(50);
            Angle coneFieldOfView = Angle.fromDegrees(100);
            Angle coneElevation = Angle.fromDegrees(20);
            Angle coneAzimuth = Angle.fromDegrees(205);

            // Initialize the high-resolution terrain class. Construct it to use 50 meter resolution elevations.
            this.terrain = new HighResolutionTerrain(this.getWwd().getModel().getGlobe(), 50.0d);

            // Compute a near and far grid of positions that will serve as ray endpoints for computing terrain
            // intersections.
            List<Vec4> vertices;

            if (CONE_VOLUME) {
                vertices = this.computeSphereVertices(center, coneFieldOfView, coneAzimuth, coneElevation, innerRange,
                    outerRange, numAz, numEl);
            }
            else {
                vertices = this.computeGridVertices(center, startAzimuth, endAzimuth, startElevation,
                    endElevation, innerRange, outerRange, numAz, numEl);
            }

            // Create geographic positions from the computed Cartesian vertices. The terrain intersector works with
            // geographic positions.
            final List<Position> positions = this.makePositions(vertices);
//            this.showPositionsAndRays(positions, null);

            // Intersect the rays defined by the radar center and the computed positions with the terrain. Since
            // this is potentially a long-running operation, perform it in a separate thread.
            Thread thread = new Thread(() -> {
                long start = System.currentTimeMillis(); // keep track of how long the intersection operation takes
                final int[] obstructionFlags = intersectTerrain(positions);
                long end = System.currentTimeMillis();
                System.out.println("Intersection calculations took " + (end - start) + " ms");

                // The computed positions define the radar volume. Set up to show that on the event dispatch thread.
                SwingUtilities.invokeLater(() -> {
                    try {
                        showRadarVolume(positions, obstructionFlags, numAz, numEl);
//                                showPositionsAndRays(positions, obstructionFlags);
                        getWwd().redraw();
                    }
                    finally {
                        ((Component) getWwd()).setCursor(Cursor.getDefaultCursor());
                    }
                });
            });
            ((Component) this.getWwd()).setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            thread.start();

            // Show the radar source as a marker.
            MarkerLayer markerLayer = new MarkerLayer();
            markerLayer.setKeepSeparated(false);
            MarkerAttributes markerAttributes = new BasicMarkerAttributes();
            List<Marker> markers = new ArrayList<>();
            markerLayer.setMarkers(markers);
            markers.add(new BasicMarker(positions.get(0), markerAttributes));
            insertAfterPlacenames(getWwd(), markerLayer);
        }

        List<Vec4> computeGridVertices(Position center, Angle leftAzimuth, Angle rightAzimuth, Angle lowerElevation,
            Angle upperElevation, double innerRange, double outerRange, int numAzimuths, int numElevations) {
            // Compute the vertices at the Cartesian origin then transform them to the radar position and
            // orientation. The grid is formed as though we're looking at the back of it -- the side the radar is on.
            // The radar volume shape is expecting this orientation and requires it to make the orientation of the
            // triangle faces consistent with the normal vectors that are parallel to the emanating radar rays.

            List<Vec4> vertices = new ArrayList<>();
            vertices.add(Vec4.ZERO); // the first vertex is the radar position.

            double dAz = (rightAzimuth.radians - leftAzimuth.radians) / (numAzimuths - 1);
            double dEl = (upperElevation.radians - lowerElevation.radians) / (numElevations - 1);

            // Compute the grid for the inner range.
            for (int iel = 0; iel < numElevations; iel++) {
                double elevation = lowerElevation.radians + iel * dEl;

                for (int iaz = 0; iaz < numAzimuths; iaz++) {
                    double azimuth = leftAzimuth.radians + iaz * dAz;

                    double x = innerRange * Math.sin(azimuth) * Math.cos(elevation);
                    double y = innerRange * Math.cos(azimuth) * Math.cos(elevation);
                    double z = innerRange * Math.sin(elevation);

                    vertices.add(new Vec4(x, y, z));
                }
            }

            // Compute the grid for the outer range.
            for (int iel = 0; iel < numElevations; iel++) {
                double elevation = lowerElevation.radians + iel * dEl;

                for (int iaz = 0; iaz < numAzimuths; iaz++) {
                    double azimuth = leftAzimuth.radians + iaz * dAz;

                    double x = outerRange * Math.sin(azimuth) * Math.cos(elevation);
                    double y = outerRange * Math.cos(azimuth) * Math.cos(elevation);
                    double z = outerRange * Math.sin(elevation);

                    vertices.add(new Vec4(x, y, z));
                }
            }

            // The vertices are computed relative to the origin. Transform them to the radar position and orientation.
            return this.transformVerticesToPosition(center, vertices);
        }

        List<Vec4> computeConeVertices(Position apex, Angle fov, Angle azimuth, Angle elevation, double innerRange,
            double outerRange, int width, int height) {
            List<Vec4> vertices = new ArrayList<>();
            vertices.add(Vec4.ZERO); // the first vertex is the radar position.

            // Rotate both grids around the Y axis to the specified elevation.
            Matrix elevationMatrix = Matrix.fromAxisAngle(Angle.NEG90.subtract(elevation), 0, 1, 0);

            // Rotate both grids around the Z axis to the specified azimuth.
            Matrix azimuthMatrix = Matrix.fromAxisAngle(Angle.POS90.subtract(azimuth), 0, 0, 1);

            // Combine the rotations and build the full vertex list.
            Matrix combined = azimuthMatrix.multiply(elevationMatrix);

            double x, y;

            double dTheta = 2 * Math.PI / (width - 1);

            // Compute the near grid.
            double innerWidth = innerRange * fov.divide(2).sin(); // half width of chord
            double R = innerRange; // radius of sphere
            double dRadius = innerWidth / (height - 1);

            // Compute rings of vertices to define the grid.
            for (int j = 0; j < height; j++) {
                double radius = innerWidth - j * dRadius;

                for (int i = 0; i < width; i++) {
                    double theta = i * dTheta;

                    x = radius * Math.cos(theta);
                    y = radius * Math.sin(theta);

                    // Compute Z on the sphere of inner range radius.
                    double w = Math.sqrt(x * x + y * y); // perpendicular distance from centerline to point on sphere
                    double z = Math.sqrt(Math.max(R * R - w * w, 0));

                    Vec4 v = new Vec4(x, y, -z);
                    vertices.add(v.transformBy3(combined));
                }
            }

            // Compute the far grid.
            double outerWidth = outerRange * fov.divide(2).sin();
            R = outerRange;
            dRadius = outerWidth / (height - 1);

            for (int j = 0; j < height; j++) {
                double radius = outerWidth - j * dRadius;

                for (int i = 0; i < width; i++) {
                    double theta = i * dTheta;

                    x = radius * Math.cos(theta);
                    y = radius * Math.sin(theta);

                    // Compute Z on the sphere of outer range radius.
                    double w = Math.sqrt(x * x + y * y); // perpendicular distance from centerline to point on sphere
                    double z = Math.sqrt(Math.max(R * R - w * w, 0));

                    Vec4 v = new Vec4(x, y, -z);
                    vertices.add(v.transformBy3(combined));
                }
            }

            // The vertices are computed relative to the origin. Transform them to the radar position and orientation.
            return this.transformVerticesToPosition(apex, vertices);
        }

        List<Vec4> computeSphereVertices(Position apex, Angle fov, Angle azimuth, Angle elevation, double innerRange,
            double outerRange, int width, int height) {
            List<Vec4> vertices = new ArrayList<>();
            vertices.add(Vec4.ZERO); // the first vertex is the radar position.

            // Rotate both grids around the Y axis to the specified elevation.
            Matrix elevationMatrix = Matrix.fromAxisAngle(Angle.NEG90.subtract(elevation), 0, 1, 0);

            // Rotate both grids around the Z axis to the specified azimuth.
            Matrix azimuthMatrix = Matrix.fromAxisAngle(Angle.POS90.subtract(azimuth), 0, 0, 1);

            // Combine the rotations and build the full vertex list.
            Matrix combined = azimuthMatrix.multiply(elevationMatrix);

            double x, y, z;

            double dTheta = 2 * Math.PI / (width - 1);

            // Compute the near grid.
            double phi;
            double dPhi = fov.divide(2).radians / (height - 1);

            for (int j = 0; j < height; j++) {
                phi = fov.divide(2).radians - j * dPhi;

                for (int i = 0; i < width; i++) {
                    double theta = i * dTheta;

                    x = innerRange * Math.cos(theta) * Math.sin(phi);
                    y = innerRange * Math.sin(theta) * Math.sin(phi);
                    z = innerRange * Math.cos(phi);

                    Vec4 v = new Vec4(x, y, -z);
                    vertices.add(v.transformBy3(combined));
                }
            }

            // Compute the far grid.
            for (int j = 0; j < height; j++) {
                phi = fov.divide(2).radians - j * dPhi;

                for (int i = 0; i < width; i++) {
                    double theta = i * dTheta;

                    x = outerRange * Math.cos(theta) * Math.sin(phi);
                    y = outerRange * Math.sin(theta) * Math.sin(phi);
                    z = outerRange * Math.cos(phi);

                    Vec4 v = new Vec4(x, y, -z);
                    vertices.add(v.transformBy3(combined));
                }
            }

            // The vertices are computed relative to the origin. Transform them to the radar position and orientation.
            return this.transformVerticesToPosition(apex, vertices);
        }

        List<Vec4> transformVerticesToPosition(Position position, Collection<Vec4> vertices) {
            // Transforms the incoming origin-centered vertices to the radar position and orientation.

            List<Vec4> transformedVertices = new ArrayList<>(vertices.size());

            // Create the transformation matrix that performs the transform.
            Matrix transform = this.getWwd().getModel().getGlobe().computeEllipsoidalOrientationAtPosition(
                position.getLatitude(), position.getLongitude(),
                this.terrain.getElevation(position) + position.getAltitude());

            for (Vec4 vertex : vertices) {
                transformedVertices.add(vertex.transformBy4(transform));
            }

            return transformedVertices;
        }

        int[] intersectTerrain(List<Position> positions) {
            int[] obstructionFlags = new int[positions.size() - 1];

            int gridSize = (positions.size() - 1) / 2;
            Globe globe = this.terrain.getGlobe();

            // Perform the intersection tests with the terrain and keep track of which rays intersect.

            Position origin = positions.get(0); // this is the radar position
            Vec4 originPoint = globe.computeEllipsoidalPointFromPosition(origin);

            Collection<Integer> intersectionIndices = new ArrayList<>();

            for (int i = 1; i < positions.size(); i++) {
                Position position = positions.get(i);

                // Mark the position as obstructed if it's below the minimum elevation.
                if (this.isBelowMinimumElevation(position, originPoint)) {
                    obstructionFlags[i - 1] = RadarVolume.EXTERNAL_OBSTRUCTION;
                    continue;
                }

                // If it's obstructed at the near grid it's obstructed at the far grid.
                if (i > gridSize && obstructionFlags[i - 1 - gridSize] == RadarVolume.EXTERNAL_OBSTRUCTION) {
                    obstructionFlags[i - 1] = RadarVolume.EXTERNAL_OBSTRUCTION;
                    continue;
                }

                // Compute the intersection with the terrain of a ray to this position.
                //
                // No need to perform the intersection test if the ray to the position just below this one is
                // unobstructed because no obstruction will occur above an unobstructed position. Cannot perform this
                // optimization on cone volumes because their orientation varies around the cone.
                if (!CONE_VOLUME // can't perform this optimization on cones because their orientation is not constant
                    && ((i > this.numAz && i <= gridSize) // near grid above the first row of elevations
                    || (i > gridSize + this.numAz))) // far grid above the first row of elevations
                {
                    if (obstructionFlags[i - 1 - numAz] == RadarVolume.NO_OBSTRUCTION) {
                        obstructionFlags[i - 1] = RadarVolume.NO_OBSTRUCTION;
                        continue;
                    }
                }

                // Perform the intersection test.
                Intersection[] intersections = this.terrain.intersect(origin, position, WorldWind.ABSOLUTE);

                if (intersections == null || intersections.length == 0) {
                    // No intersection so use the grid position.
                    obstructionFlags[i - 1] = RadarVolume.NO_OBSTRUCTION;
                }
                else {
                    // An intersection with the terrain occurred. If it's a far grid position and beyond the near grid,
                    // set the grid position to be the intersection position.

                    // First see if the intersection is beyond the far grid, in which case the ray is considered
                    // unobstructed.
                    Vec4 intersectionPoint = intersections[0].getIntersectionPoint();
                    double distance = intersectionPoint.distanceTo3(originPoint);
                    if (distance > this.outerRange) {
                        // No intersection so use the grid position.
                        obstructionFlags[i - 1] = RadarVolume.NO_OBSTRUCTION;
                        continue;
                    }

                    if (i > gridSize) // if this is a far grid position
                    {
                        // The obstruction occurs beyond the near grid.
                        obstructionFlags[i - 1] = RadarVolume.INTERNAL_OBSTRUCTION;
                        Position pos = globe.computePositionFromEllipsoidalPoint(intersectionPoint);
                        double elevation = this.terrain.getElevation(pos);
                        positions.set(i, new Position(pos, elevation));
                        intersectionIndices.add(i);
                    }
                    else // it's a near grid position
                    {
                        if (distance < this.innerRange)
                            obstructionFlags[i - 1] = RadarVolume.EXTERNAL_OBSTRUCTION;
                        else
                            obstructionFlags[i - 1] = RadarVolume.NO_OBSTRUCTION;
                    }
                }
            }

            // Raise the internal intersection positions to the next elevation level above their original one. This
            // provides more clearance between the volume and the terrain.
            for (Integer i : intersectionIndices) {
                if (i < positions.size() - numAz) {
                    Position position = positions.get(i);
                    Position upper = positions.get(i + this.numAz);
                    Vec4 positionVec = globe.computeEllipsoidalPointFromPosition(position).subtract3(originPoint);
                    Vec4 upperVec = globe.computeEllipsoidalPointFromPosition(upper).subtract3(originPoint);
                    upperVec = upperVec.add3(positionVec).divide3(2);
                    double t = positionVec.getLength3() / upperVec.getLength3();
                    Vec4 newPoint = upperVec.multiply3(t).add3(originPoint);
                    Position newPosition = globe.computePositionFromEllipsoidalPoint(newPoint);
                    positions.set(i, newPosition);
                }
            }

            return obstructionFlags;
        }

        protected boolean isBelowMinimumElevation(Position position, Vec4 cartesianOrigin) {
            Globe globe = this.getWwd().getModel().getGlobe();

            Vec4 cartesianPosition = globe.computeEllipsoidalPointFromPosition(position);
            Angle angle = cartesianOrigin.angleBetween3(cartesianPosition.subtract3(cartesianOrigin));

            return angle.radians > (Math.PI / 2 - this.minimumElevation.radians);
        }

        List<Position> makePositions(Collection<Vec4> vertices) {
            // Convert the Cartesian vertices to geographic positions.

            List<Position> positions = new ArrayList<>(vertices.size());

            Globe globe = this.getWwd().getModel().getGlobe();

            for (Vec4 vertex : vertices) {
                positions.add(globe.computePositionFromEllipsoidalPoint(vertex));
            }

            return positions;
        }

        void showRadarVolume(List<Position> positions, int[] obstructionFlags, int numAz, int numEl) {
            RenderableLayer layer = new RenderableLayer();

            // Set the volume's attributes.
            ShapeAttributes attributes = new BasicShapeAttributes();
            attributes.setDrawInterior(true);
            attributes.setInteriorMaterial(Material.WHITE);
            attributes.setEnableLighting(true);
//            attributes.setInteriorOpacity(0.8);

            // Create the volume and add it to the model.
            RadarVolume volume = new RadarVolume(positions.subList(1, positions.size()), obstructionFlags, numAz,
                numEl);
            volume.setAttributes(attributes);
            volume.setEnableSides(!CONE_VOLUME);
            layer.add(volume);

            // Create two paths to show their interaction with the radar volume. The first path goes through most
            // of the volume. The second path goes mostly under the volume.

            Path path = new Path(Position.fromDegrees(36.9843, -119.4464, 20.0e3),
                Position.fromDegrees(36.4630, -118.3595, 20.0e3));
            ShapeAttributes pathAttributes = new BasicShapeAttributes();
            pathAttributes.setOutlineMaterial(Material.RED);
            path.setAttributes(pathAttributes);
            layer.add(path);

            path = new Path(Position.fromDegrees(36.9843, -119.4464, 5.0e3),
                Position.fromDegrees(36.4630, -118.3595, 5.0e3));
            path.setAttributes(pathAttributes);
            layer.add(path);

            insertAfterPlacenames(getWwd(), layer);
        }

        void showPositionsAndRays(List<Position> positions, int[] obstructionFlags) {
            MarkerLayer markerLayer = new MarkerLayer();
            markerLayer.setKeepSeparated(false);
            MarkerAttributes attributes = new BasicMarkerAttributes();
            List<Marker> markers = new ArrayList<>();

            RenderableLayer lineLayer = new RenderableLayer();
            ShapeAttributes lineAttributes = new BasicShapeAttributes();
            lineAttributes.setOutlineMaterial(Material.RED);

            for (Position position : positions) {
                {
                    Marker marker = new BasicMarker(position, attributes);
                    markers.add(marker);
                }
            }
            markerLayer.setMarkers(markers);

            int gridSize = positions.size() / 2;
            for (int i = 1; i < gridSize; i++) {
                Path path = new Path(positions.get(0), positions.get(i + gridSize));
                path.setAttributes(lineAttributes);
                path.setAltitudeMode(WorldWind.ABSOLUTE);
                if (obstructionFlags != null) {
                    int obstructionFlag = obstructionFlags[i + gridSize - 1];
                    String msg = obstructionFlag == RadarVolume.NO_OBSTRUCTION ? "None"
                        : obstructionFlag == RadarVolume.EXTERNAL_OBSTRUCTION ? "External"
                            : obstructionFlag == RadarVolume.INTERNAL_OBSTRUCTION ? "Internal" : "UNKNOWN";
                    path.setValue(AVKey.DISPLAY_NAME, msg);
                }
                lineLayer.add(path);
            }

            insertAfterPlacenames(getWwd(), markerLayer);
            insertAfterPlacenames(getWwd(), lineLayer);
        }
    }

    /**
     * Displays a volume defined by a near and far grid of positions. This shape is meant to represent a radar volume, with
     * the radar having a minimum and maximum range.
     *
     * @author tag
     * @version $Id: RadarVolume.java 2438 2014-11-18 02:11:29Z tgaskins $
     */
    public static class RadarVolume extends AbstractShape {
        public static final int NO_OBSTRUCTION = 0;
        public static final int EXTERNAL_OBSTRUCTION = 1;
        public static final int INTERNAL_OBSTRUCTION = 2;

        protected static final int VERTEX_NORMAL = 0;
        protected static final int TRIANGLE_NORMAL = 1;

        protected List<Position> positions; // the grid positions, near grid first, followed by far grid
        protected int[] obstructionFlags; // flags indicating where obstructions occur
        protected int width; // the number of horizontal positions in the grid.
        protected int height; // the number of vertical positions in the grid.
        protected IntBuffer sideIndices; // OpenGL indices defining the sides of the area between the grids.
        protected boolean enableSides = true; // sides show up inside conical volumes to enable the app to turn them off

        /**
         * Constructs a radar volume.
         *
         * @param positions        the volume's positions, organized as two grids. The near grid is held in the first width
         *                         x height entries, the far grid is held in the next width x height entries. This list is
         *                         retained as-is and is not copied.
         * @param obstructionFlags flags indicating the obstruction state of the specified positions. This array is retained
         *                         as-is and is not copied. Recognized values are <code>NO_OBSTRUCTION</code> indicating
         *                         that the specified position is unobstructed, <code>INTERNAL_OBSTRUCTION</code> indicating
         *                         that the position is obstructed beyond the near grid but before the far grid,
         *                         <code>EXTERNAL_OBSTRUCTION</code> indicating that the position is obstructed before the
         *                         near grid.
         * @param width            the horizontal dimension of the grid.
         * @param height           the vertical dimension of the grid.
         * @throws IllegalArgumentException if the positions list or inclusion flags array is null, the size of
         *                                            the inclusion flags array is less than the number of grid positions,
         *                                            the positions list is less than the specified size, or the width or
         *                                            height are less than 2.
         */
        public RadarVolume(List<Position> positions, int[] obstructionFlags, int width, int height) {
            if (positions == null || obstructionFlags == null) {
                String message = Logging.getMessage("nullValue.ArrayIsNull");
                Logging.logger().severe(message);
                throw new IllegalArgumentException(message);
            }

            if (width < 2) {
                String message = Logging.getMessage("generic.InvalidWidth", width);
                Logging.logger().severe(message);
                throw new IllegalArgumentException(message);
            }

            if (height < 2) {
                String message = Logging.getMessage("generic.InvalidHeight", height);
                Logging.logger().severe(message);
                throw new IllegalArgumentException(message);
            }

            if (positions.size() < 2 * (width * height)) {
                String message = Logging.getMessage("generic.ListLengthInsufficient", positions.size());
                Logging.logger().severe(message);
                throw new IllegalArgumentException(message);
            }

            if (obstructionFlags.length < positions.size()) {
                String message = Logging.getMessage("generic.ListLengthInsufficient", obstructionFlags.length);
                Logging.logger().severe(message);
                throw new IllegalArgumentException(message);
            }

            this.positions = positions;
            this.obstructionFlags = obstructionFlags;
            this.width = width;
            this.height = height;
        }

        protected static FloatBuffer trimBuffer(FloatBuffer buffer) {
            FloatBuffer outputBuffer = Buffers.newDirectFloatBuffer(buffer.limit());

            buffer.rewind();
            while (buffer.hasRemaining()) {
                outputBuffer.put(buffer.get());
            }

            return outputBuffer;
        }

        public boolean isEnableSides() {
            return enableSides;
        }

        public void setEnableSides(boolean enableSides) {
            this.enableSides = enableSides;
        }

        @Override
        protected void initialize() {
            // Nothing unique to initialize.
        }

        @Override
        protected AbstractShapeData createCacheEntry(DrawContext dc) {
            return new ShapeData(dc, this);
        }

        /**
         * Returns the current shape data cache entry.
         *
         * @return the current data cache entry.
         */
        protected ShapeData getCurrent() {
            return (ShapeData) this.getCurrentData();
        }

        /**
         * Returns the grid positions as specified to this object's constructor.
         *
         * @return this object's grid positions.
         */
        public List<Position> getPositions() {
            return positions;
        }

        /**
         * Returns the inclusion flags as specified to this object's constructor.
         *
         * @return this object's inclusion flags.
         */
        public int[] getObstructionFlags() {
            return this.obstructionFlags;
        }

        /**
         * Indicates the grid width.
         *
         * @return the grid width.
         */
        public int getWidth() {
            return width;
        }

        /**
         * Indicates the grid height.
         *
         * @return the grid height.
         */
        public int getHeight() {
            return height;
        }

        @Override
        protected boolean mustApplyTexture(DrawContext dc) {
            return false;
        }

        @Override
        protected boolean shouldUseVBOs(DrawContext dc) {
            return false;
        }

        @Override
        protected boolean isOrderedRenderableValid(DrawContext dc) {
            ShapeData shapeData = this.getCurrent();

            return shapeData.triangleVertices != null && shapeData.triangleVertices.capacity() > 0;
        }

        @Override
        protected boolean doMakeOrderedRenderable(DrawContext dc) {
            if (!this.intersectsFrustum(dc)) {
                return false;
            }

            ShapeData shapeData = this.getCurrent();

            if (shapeData.triangleVertices == null) {
                this.makeGridVertices(dc);
                this.computeCenterPoint();
                this.makeGridNormals();
                this.makeGridTriangles();
                this.makeSides();

                // No longer need the grid vertices or normals
                shapeData.gridVertices = null;
                shapeData.gridNormals = null;
            }

            shapeData.setEyeDistance(dc.getView().getEyePoint().distanceTo3(shapeData.centerPoint));

            return true;
        }

        @Override
        protected void doDrawOutline(DrawContext dc) {
            // The shape does not have an outline
        }

        @Override
        protected void doDrawInterior(DrawContext dc) {
            this.drawModel(dc, GL2.GL_FILL);
        }

        protected void drawModel(DrawContext dc, int displayMode) {
            ShapeData shapeData = this.getCurrent();
            GL2 gl = dc.getGL().getGL2();

            gl.glPolygonMode(GL2.GL_FRONT_AND_BACK, displayMode);

            // Draw the volume's near and far grids and floor.
            gl.glVertexPointer(3, GL.GL_FLOAT, 0, shapeData.triangleVertices.rewind());
            gl.glNormalPointer(GL.GL_FLOAT, 0, shapeData.triangleNormals.rewind());
            gl.glDrawArrays(GL.GL_TRIANGLES, 0, shapeData.triangleVertices.limit() / 3);

            if (this.isEnableSides()) {
                // Draw the volume's sides.
                gl.glVertexPointer(3, GL.GL_FLOAT, 0, shapeData.sideVertices.rewind());
                gl.glNormalPointer(GL.GL_FLOAT, 0, shapeData.sideNormals.rewind());
                gl.glDrawElements(GL.GL_TRIANGLE_STRIP, this.sideIndices.limit(), GL.GL_UNSIGNED_INT,
                    this.sideIndices.rewind());
            }
        }

        protected void makeGridVertices(DrawContext dc) {
            // The Cartesian coordinates of the grid are computed but only used to construct the displayed volume. They are
            // not themselves rendered, and are cleared once construction is done.

            // The grid consists of independent triangles. A tri-strip can't be used because not all positions in the
            // input grids participate in triangle formation because they may be obstructed.

            // Get the current shape data.
            ShapeData shapeData = this.getCurrent();

            // Set the reference point to the grid's origin.
            Vec4 refPt = dc.getGlobe().computePointFromPosition(this.positions.get(0));
            shapeData.setReferencePoint(refPt);

            // Allocate the grid vertices.
            shapeData.gridVertices = Buffers.newDirectFloatBuffer(3 * this.positions.size());

            // Compute the grid vertices.
            for (Position position : this.positions) {
                Vec4 point = dc.getGlobe().computePointFromPosition(position).subtract3(refPt);
                shapeData.gridVertices.put((float) point.x).put((float) point.y).put((float) point.z);
            }
        }

        protected void makeGridNormals() {
            // Like the grid vertices, the grid normals are computed only for construction of the volume and determination
            // of its normals. The grid normals are not used otherwise and are cleared once construction is done.

            // The grid normals are defined by a vector from each position in the near grid to the corresponding
            // position in the far grid.

            ShapeData shapeData = this.getCurrent();
            FloatBuffer vertices = shapeData.gridVertices;

            shapeData.gridNormals = Buffers.newDirectFloatBuffer(shapeData.gridVertices.limit());
            int gridSize = this.getWidth() * this.getHeight();
            int separation = 3 * gridSize;
            for (int i = 0; i < gridSize; i++) {
                int k = i * 3;
                double nx = vertices.get(k + separation) - vertices.get(k);
                double ny = vertices.get(k + separation + 1) - vertices.get(k + 1);
                double nz = vertices.get(k + separation + 2) - vertices.get(k + 2);

                double length = Math.sqrt(nx * nx + ny * ny + nz * nz);
                if (length > 0) {
                    nx /= length;
                    ny /= length;
                    nz /= length;
                }

                shapeData.gridNormals.put((float) nx).put((float) ny).put((float) nz);
                shapeData.gridNormals.put(k + separation, (float) nx);
                shapeData.gridNormals.put(k + separation + 1, (float) ny);
                shapeData.gridNormals.put(k + separation + 2, (float) nz);
            }
        }

        protected void computeCenterPoint() {
            ShapeData shapeData = this.getCurrent();

            int gridSize = this.width * this.height;
            int k = 3 * gridSize / 2;

            double xNear = shapeData.gridVertices.get(k);
            double yNear = shapeData.gridVertices.get(k + 1);
            double zNear = shapeData.gridVertices.get(k + 2);

            k += 3 * gridSize;

            double xFar = shapeData.gridVertices.get(k);
            double yFar = shapeData.gridVertices.get(k + 1);
            double zFar = shapeData.gridVertices.get(k + 2);

            Vec4 pNear = (new Vec4(xNear, yNear, zNear)).add3(shapeData.getReferencePoint());
            Vec4 pFar = (new Vec4(xFar, yFar, zFar)).add3(shapeData.getReferencePoint());

            shapeData.centerPoint = pNear.add3(pFar).multiply3(0.5);
        }

        /**
         * Forms the volume's front, back and bottom vertices and computes appropriate normals.
         */
        protected void makeGridTriangles() {
            // This method first computes the triangles that form the near and far grid surfaces, then it computes the
            // floor connecting those surface to either each other or the terrain intersections within the volume. For
            // the grid face there are five relevant cases, each described in their implementation below. For the floor
            // there are 8 relevant cases, also described in their implementation below.

            ShapeData shapeData = this.getCurrent();
            FloatBuffer vs = shapeData.gridVertices;

            // Allocate the most we'll need because we don't yet know exactly how much we'll use. We  need at most room
            // for 9 floats per triangle, 4 triangles per grid cell and 2 sets of grid cells (near and far).
            int maxSize = 9 * 4 * 2 * ((this.width - 1) * (this.height - 1));
            shapeData.triangleVertices = Buffers.newDirectFloatBuffer(maxSize);
            shapeData.triangleNormals = Buffers.newDirectFloatBuffer(maxSize);

            FloatBuffer triVerts = shapeData.triangleVertices;

            int[] triFlags = new int[3];
            int[] triIndices = new int[3];

            for (int n = 0; n < 2; n++) // once for near grid, then again for far grid
            {
                int base = n * this.width * this.height;

                for (int j = 0; j < this.height - 1; j++) {
                    for (int i = 0; i < this.width - 1; i++) {
                        // k identifies the grid index of the lower left position in each cell
                        int k = base + j * this.width + i;
                        boolean ll, lr, ul, ur;

                        // Determine the status of the four grid positions.
                        if (n == 0) // near grid
                        {
                            ll = this.obstructionFlags[k] == NO_OBSTRUCTION;
                            lr = this.obstructionFlags[k + 1] == NO_OBSTRUCTION;
                            ul = this.obstructionFlags[k + this.width] == NO_OBSTRUCTION;
                            ur = this.obstructionFlags[k + this.width + 1] == NO_OBSTRUCTION;
                        }
                        else // far grid
                        {
                            ll = this.obstructionFlags[k] != EXTERNAL_OBSTRUCTION;
                            lr = this.obstructionFlags[k + 1] != EXTERNAL_OBSTRUCTION;
                            ul = this.obstructionFlags[k + this.width] != EXTERNAL_OBSTRUCTION;
                            ur = this.obstructionFlags[k + this.width + 1] != EXTERNAL_OBSTRUCTION;
                        }
                        int gridSize = this.width * this.height;

                        int llv = k; // index of lower left cell position
                        int lrv = k + 1;
                        int ulv = (k + width);
                        int urv = k + width + 1;

                        int kk; // index into the grid vertices buffer

                        if (ul && ur && ll && lr) // case 6
                        {
                            // Show both triangles.

                            // It matters how we decompose the cell into triangles. The order in these two clauses
                            // ensures that the correct half cells are drawn when one of the lower positions has an
                            // internal obstruction -- is not on the face of the grid.

                            if (this.obstructionFlags[llv] == INTERNAL_OBSTRUCTION) {
                                kk = llv * 3;
                                triVerts.put(vs.get(kk)).put(vs.get(kk + 1)).put(vs.get(kk + 2));
                                triFlags[0] = this.obstructionFlags[kk / 3];
                                triIndices[0] = kk;

                                kk = ulv * 3;
                                triVerts.put(vs.get(kk)).put(vs.get(kk + 1)).put(vs.get(kk + 2));
                                triFlags[1] = this.obstructionFlags[kk / 3];
                                triIndices[1] = kk;

                                kk = lrv * 3;
                                triVerts.put(vs.get(kk)).put(vs.get(kk + 1)).put(vs.get(kk + 2));
                                triFlags[2] = this.obstructionFlags[kk / 3];
                                triIndices[2] = kk;

                                this.setTriangleNormals(triFlags, triIndices);

                                kk = lrv * 3;
                            }
                            else {
                                kk = llv * 3;
                                triVerts.put(vs.get(kk)).put(vs.get(kk + 1)).put(vs.get(kk + 2));
                                triFlags[0] = this.obstructionFlags[kk / 3];
                                triIndices[0] = kk;

                                kk = urv * 3;
                                triVerts.put(vs.get(kk)).put(vs.get(kk + 1)).put(vs.get(kk + 2));
                                triFlags[1] = this.obstructionFlags[kk / 3];
                                triIndices[1] = kk;

                                kk = lrv * 3;
                                triVerts.put(vs.get(kk)).put(vs.get(kk + 1)).put(vs.get(kk + 2));
                                triFlags[2] = this.obstructionFlags[kk / 3];
                                triIndices[2] = kk;

                                this.setTriangleNormals(triFlags, triIndices);

                                kk = llv * 3;
                            }
                            triVerts.put(vs.get(kk)).put(vs.get(kk + 1)).put(vs.get(kk + 2));
                            triFlags[0] = this.obstructionFlags[kk / 3];
                            triIndices[0] = kk;
                            kk = ulv * 3;
                            triVerts.put(vs.get(kk)).put(vs.get(kk + 1)).put(vs.get(kk + 2));
                            triFlags[1] = this.obstructionFlags[kk / 3];
                            triIndices[1] = kk;
                            kk = urv * 3;
                            triVerts.put(vs.get(kk)).put(vs.get(kk + 1)).put(vs.get(kk + 2));
                            triFlags[2] = this.obstructionFlags[kk / 3];
                            triIndices[2] = kk;
                            this.setTriangleNormals(triFlags, triIndices);
                        }
                        else if (ul && !ur && ll && lr) // case 5
                        {
                            // Show the lower left triangle

                            kk = ulv * 3;
                            triVerts.put(vs.get(kk)).put(vs.get(kk + 1)).put(vs.get(kk + 2));
                            triFlags[0] = this.obstructionFlags[kk / 3];
                            triIndices[0] = kk;

                            kk = lrv * 3;
                            triVerts.put(vs.get(kk)).put(vs.get(kk + 1)).put(vs.get(kk + 2));
                            triFlags[1] = this.obstructionFlags[kk / 3];
                            triIndices[1] = kk;

                            kk = llv * 3;
                            triVerts.put(vs.get(kk)).put(vs.get(kk + 1)).put(vs.get(kk + 2));
                            triFlags[2] = this.obstructionFlags[kk / 3];
                            triIndices[2] = kk;

                            this.setTriangleNormals(triFlags, triIndices);
                        }
                        else if (ul && ur && ll && !lr) // case 7
                        {
                            // Show the upper left triangle.

                            kk = llv * 3;
                            triVerts.put(vs.get(kk)).put(vs.get(kk + 1)).put(vs.get(kk + 2));
                            triFlags[0] = this.obstructionFlags[kk / 3];
                            triIndices[0] = kk;

                            kk = ulv * 3;
                            triVerts.put(vs.get(kk)).put(vs.get(kk + 1)).put(vs.get(kk + 2));
                            triFlags[1] = this.obstructionFlags[kk / 3];
                            triIndices[1] = kk;

                            kk = urv * 3;
                            triVerts.put(vs.get(kk)).put(vs.get(kk + 1)).put(vs.get(kk + 2));
                            triFlags[2] = this.obstructionFlags[kk / 3];
                            triIndices[2] = kk;

                            this.setTriangleNormals(triFlags, triIndices);
                        }
                        else if (!ul && ur && ll && lr) // case 8
                        {
                            // Show the lower right triangle

                            kk = llv * 3;
                            triVerts.put(vs.get(kk)).put(vs.get(kk + 1)).put(vs.get(kk + 2));
                            triFlags[0] = this.obstructionFlags[kk / 3];
                            triIndices[0] = kk;

                            kk = urv * 3;
                            triVerts.put(vs.get(kk)).put(vs.get(kk + 1)).put(vs.get(kk + 2));
                            triFlags[1] = this.obstructionFlags[kk / 3];
                            triIndices[1] = kk;

                            kk = lrv * 3;
                            triVerts.put(vs.get(kk)).put(vs.get(kk + 1)).put(vs.get(kk + 2));
                            triFlags[2] = this.obstructionFlags[kk / 3];
                            triIndices[2] = kk;

                            this.setTriangleNormals(triFlags, triIndices);
                        }
                        else if (ul && ur && !ll && lr) // case 11
                        {
                            // Show the right triangle.

                            kk = lrv * 3;
                            triVerts.put(vs.get(kk)).put(vs.get(kk + 1)).put(vs.get(kk + 2));
                            triFlags[0] = this.obstructionFlags[kk / 3];
                            triIndices[0] = kk;

                            kk = ulv * 3;
                            triVerts.put(vs.get(kk)).put(vs.get(kk + 1)).put(vs.get(kk + 2));
                            triFlags[1] = this.obstructionFlags[kk / 3];
                            triIndices[1] = kk;

                            kk = urv * 3;
                            triVerts.put(vs.get(kk)).put(vs.get(kk + 1)).put(vs.get(kk + 2));
                            triFlags[2] = this.obstructionFlags[kk / 3];
                            triIndices[2] = kk;

                            this.setTriangleNormals(triFlags, triIndices);
                        }

                        if (n == 0) // no need to calculate floor for the near grid
                            continue;

                        // Form the cell's "floor".

                        if (!ul && !ur && ll && lr) // case 2
                        {
                            // Draw the cell bottom.

                            if (this.obstructionFlags[llv] == INTERNAL_OBSTRUCTION
                                || this.obstructionFlags[lrv] == INTERNAL_OBSTRUCTION) {
                                kk = llv * 3;
                                triVerts.put(vs.get(kk)).put(vs.get(kk + 1)).put(vs.get(kk + 2));
                                triIndices[0] = kk;

                                kk = lrv * 3;
                                triVerts.put(vs.get(kk)).put(vs.get(kk + 1)).put(vs.get(kk + 2));
                                triIndices[1] = kk;

                                kk = (llv - gridSize) * 3;
                                triVerts.put(vs.get(kk)).put(vs.get(kk + 1)).put(vs.get(kk + 2));
                                triIndices[2] = kk;

                                this.setTriangleNormals(null, triIndices);

                                kk = lrv * 3;
                                triVerts.put(vs.get(kk)).put(vs.get(kk + 1)).put(vs.get(kk + 2));
                                triIndices[0] = kk;

                                kk = (lrv - gridSize) * 3;
                                triVerts.put(vs.get(kk)).put(vs.get(kk + 1)).put(vs.get(kk + 2));
                                triIndices[1] = kk;

                                kk = (llv - gridSize) * 3;
                            }
                            else {
                                kk = llv * 3;
                                triVerts.put(vs.get(kk)).put(vs.get(kk + 1)).put(vs.get(kk + 2));
                                triIndices[0] = kk;

                                kk = (llv - gridSize) * 3;
                                triVerts.put(vs.get(kk)).put(vs.get(kk + 1)).put(vs.get(kk + 2));
                                triIndices[1] = kk;

                                kk = (lrv - gridSize) * 3;
                                triVerts.put(vs.get(kk)).put(vs.get(kk + 1)).put(vs.get(kk + 2));
                                triIndices[2] = kk;

                                this.setTriangleNormals(null, triIndices);

                                kk = llv * 3;
                                triVerts.put(vs.get(kk)).put(vs.get(kk + 1)).put(vs.get(kk + 2));
                                triIndices[0] = kk;

                                kk = lrv * 3;
                                triVerts.put(vs.get(kk)).put(vs.get(kk + 1)).put(vs.get(kk + 2));
                                triIndices[1] = kk;

                                kk = (lrv - gridSize) * 3;
                            }
                            triVerts.put(vs.get(kk)).put(vs.get(kk + 1)).put(vs.get(kk + 2));
                            triIndices[2] = kk;
                            this.setTriangleNormals(null, triIndices);
                        }
                        else if (ul && !ur && ll && !lr) // case 3
                        {
                            // Draw the left side of the cell.

                            kk = llv * 3;
                            triVerts.put(vs.get(kk)).put(vs.get(kk + 1)).put(vs.get(kk + 2));
                            triIndices[0] = kk;

                            kk = (llv - gridSize) * 3;
                            triVerts.put(vs.get(kk)).put(vs.get(kk + 1)).put(vs.get(kk + 2));
                            triIndices[1] = kk;

                            kk = ulv * 3;
                            triVerts.put(vs.get(kk)).put(vs.get(kk + 1)).put(vs.get(kk + 2));
                            triIndices[2] = kk;

                            this.setTriangleNormals(null, triIndices);

                            kk = ulv * 3;
                            triVerts.put(vs.get(kk)).put(vs.get(kk + 1)).put(vs.get(kk + 2));
                            triIndices[0] = kk;

                            kk = (llv - gridSize) * 3;
                            triVerts.put(vs.get(kk)).put(vs.get(kk + 1)).put(vs.get(kk + 2));
                            triIndices[1] = kk;

                            kk = (ulv - gridSize) * 3;
                            triVerts.put(vs.get(kk)).put(vs.get(kk + 1)).put(vs.get(kk + 2));
                            triIndices[2] = kk;

                            this.setTriangleNormals(null, triIndices);
                        }
                        else if (ul && !ur && ll && lr) // case 5
                        {
                            // Draw the ul to lr diagonal of the cell.

                            kk = ulv * 3;
                            triVerts.put(vs.get(kk)).put(vs.get(kk + 1)).put(vs.get(kk + 2));
                            triIndices[0] = kk;

                            kk = (ulv - gridSize) * 3;
                            triVerts.put(vs.get(kk)).put(vs.get(kk + 1)).put(vs.get(kk + 2));
                            triIndices[1] = kk;

                            kk = (lrv - gridSize) * 3;
                            triVerts.put(vs.get(kk)).put(vs.get(kk + 1)).put(vs.get(kk + 2));
                            triIndices[2] = kk;

                            this.setTriangleNormals(null, triIndices);

                            kk = (lrv - gridSize) * 3;
                            triVerts.put(vs.get(kk)).put(vs.get(kk + 1)).put(vs.get(kk + 2));
                            triIndices[0] = kk;

                            kk = lrv * 3;
                            triVerts.put(vs.get(kk)).put(vs.get(kk + 1)).put(vs.get(kk + 2));
                            triIndices[1] = kk;

                            kk = ulv * 3;
                            triVerts.put(vs.get(kk)).put(vs.get(kk + 1)).put(vs.get(kk + 2));
                            triIndices[2] = kk;

                            this.setTriangleNormals(null, triIndices);
                        }
                        else if (ul && ur && ll && !lr) // case 7
                        {
                            // Draw the ur to ll diagonal of the cell.

                            kk = urv * 3;
                            triVerts.put(vs.get(kk)).put(vs.get(kk + 1)).put(vs.get(kk + 2));
                            triIndices[0] = kk;

                            kk = (urv - gridSize) * 3;
                            triVerts.put(vs.get(kk)).put(vs.get(kk + 1)).put(vs.get(kk + 2));
                            triIndices[1] = kk;

                            kk = (llv - gridSize) * 3;
                            triVerts.put(vs.get(kk)).put(vs.get(kk + 1)).put(vs.get(kk + 2));
                            triIndices[2] = kk;

                            this.setTriangleNormals(null, triIndices);

                            kk = (llv - gridSize) * 3;
                            triVerts.put(vs.get(kk)).put(vs.get(kk + 1)).put(vs.get(kk + 2));
                            triIndices[0] = kk;

                            kk = llv * 3;
                            triVerts.put(vs.get(kk)).put(vs.get(kk + 1)).put(vs.get(kk + 2));
                            triIndices[1] = kk;

                            kk = urv * 3;
                            triVerts.put(vs.get(kk)).put(vs.get(kk + 1)).put(vs.get(kk + 2));
                            triIndices[2] = kk;

                            this.setTriangleNormals(null, triIndices);
                        }
                        else if (!ul && ur && ll && lr) // case 8
                        {
                            // Draw the ll to ur diagonal of the cell.

                            kk = urv * 3;
                            triVerts.put(vs.get(kk)).put(vs.get(kk + 1)).put(vs.get(kk + 2));
                            triIndices[0] = kk;

                            kk = (urv - gridSize) * 3;
                            triVerts.put(vs.get(kk)).put(vs.get(kk + 1)).put(vs.get(kk + 2));
                            triIndices[1] = kk;

                            kk = (llv - gridSize) * 3;
                            triVerts.put(vs.get(kk)).put(vs.get(kk + 1)).put(vs.get(kk + 2));
                            triIndices[2] = kk;

                            this.setTriangleNormals(null, triIndices);

                            kk = (llv - gridSize) * 3;
                            triVerts.put(vs.get(kk)).put(vs.get(kk + 1)).put(vs.get(kk + 2));
                            triIndices[0] = kk;

                            kk = llv * 3;
                            triVerts.put(vs.get(kk)).put(vs.get(kk + 1)).put(vs.get(kk + 2));
                            triIndices[1] = kk;

                            kk = urv * 3;
                            triVerts.put(vs.get(kk)).put(vs.get(kk + 1)).put(vs.get(kk + 2));
                            triIndices[2] = kk;

                            this.setTriangleNormals(null, triIndices);
                        }
                        else if (!ul && ur && !ll && lr) // case 10
                        {
                            // Draw the right side of the cell.

                            kk = urv * 3;
                            triVerts.put(vs.get(kk)).put(vs.get(kk + 1)).put(vs.get(kk + 2));
                            triIndices[0] = kk;

                            kk = (urv - gridSize) * 3;
                            triVerts.put(vs.get(kk)).put(vs.get(kk + 1)).put(vs.get(kk + 2));
                            triIndices[1] = kk;

                            kk = (lrv - gridSize) * 3;
                            triVerts.put(vs.get(kk)).put(vs.get(kk + 1)).put(vs.get(kk + 2));
                            triIndices[2] = kk;

                            this.setTriangleNormals(null, triIndices);

                            kk = (lrv - gridSize) * 3;
                            triVerts.put(vs.get(kk)).put(vs.get(kk + 1)).put(vs.get(kk + 2));
                            triIndices[0] = kk;

                            kk = lrv * 3;
                            triVerts.put(vs.get(kk)).put(vs.get(kk + 1)).put(vs.get(kk + 2));
                            triIndices[1] = kk;

                            kk = urv * 3;
                            triVerts.put(vs.get(kk)).put(vs.get(kk + 1)).put(vs.get(kk + 2));
                            triIndices[2] = kk;

                            this.setTriangleNormals(null, triIndices);
                        }
                        else if (ul && ur && !ll && lr) // case 11
                        {
                            // Draw the ul to lr diagonal of the cell.

                            kk = ulv * 3;
                            triVerts.put(vs.get(kk)).put(vs.get(kk + 1)).put(vs.get(kk + 2));
                            triIndices[0] = kk;

                            kk = (ulv - gridSize) * 3;
                            triVerts.put(vs.get(kk)).put(vs.get(kk + 1)).put(vs.get(kk + 2));
                            triIndices[1] = kk;

                            kk = (lrv - gridSize) * 3;
                            triVerts.put(vs.get(kk)).put(vs.get(kk + 1)).put(vs.get(kk + 2));
                            triIndices[2] = kk;

                            this.setTriangleNormals(null, triIndices);

                            kk = (lrv - gridSize) * 3;
                            triVerts.put(vs.get(kk)).put(vs.get(kk + 1)).put(vs.get(kk + 2));
                            triIndices[0] = kk;

                            kk = lrv * 3;
                            triVerts.put(vs.get(kk)).put(vs.get(kk + 1)).put(vs.get(kk + 2));
                            triIndices[1] = kk;

                            kk = ulv * 3;
                            triVerts.put(vs.get(kk)).put(vs.get(kk + 1)).put(vs.get(kk + 2));
                            triIndices[2] = kk;

                            this.setTriangleNormals(null, triIndices);
                        }
                        else if (ul && ur && !ll && !lr) // case 13
                        {
                            // Draw the cell top.

                            llv = ulv - gridSize;
                            lrv = urv - gridSize;

                            // Draw the floor.
                            kk = llv * 3;
                            triVerts.put(vs.get(kk)).put(vs.get(kk + 1)).put(vs.get(kk + 2));
                            triIndices[0] = kk;

                            kk = urv * 3;
                            triVerts.put(vs.get(kk)).put(vs.get(kk + 1)).put(vs.get(kk + 2));
                            triIndices[1] = kk;

                            kk = lrv * 3;
                            triVerts.put(vs.get(kk)).put(vs.get(kk + 1)).put(vs.get(kk + 2));
                            triIndices[2] = kk;

                            this.setTriangleNormals(null, triIndices);

                            kk = llv * 3;
                            triVerts.put(vs.get(kk)).put(vs.get(kk + 1)).put(vs.get(kk + 2));
                            triIndices[0] = kk;

                            kk = ulv * 3;
                            triVerts.put(vs.get(kk)).put(vs.get(kk + 1)).put(vs.get(kk + 2));
                            triIndices[1] = kk;

                            kk = urv * 3;
                            triVerts.put(vs.get(kk)).put(vs.get(kk + 1)).put(vs.get(kk + 2));
                            triIndices[2] = kk;

                            this.setTriangleNormals(null, triIndices);
                        }

                        // If this is the bottom row of cells, then we may need to draw the floor connecting
                        // the far grid to the near grid along the edge.

                        if (j == 0 && ll && lr) {
                            if (this.obstructionFlags[llv] == INTERNAL_OBSTRUCTION
                                || this.obstructionFlags[lrv] == INTERNAL_OBSTRUCTION) {
                                // Draw the floor.
                                kk = llv * 3;
                                triVerts.put(vs.get(kk)).put(vs.get(kk + 1)).put(vs.get(kk + 2));
                                triIndices[0] = kk;

                                kk = lrv * 3;
                                triVerts.put(vs.get(kk)).put(vs.get(kk + 1)).put(vs.get(kk + 2));
                                triIndices[1] = kk;

                                kk = (llv - gridSize) * 3;
                                triVerts.put(vs.get(kk)).put(vs.get(kk + 1)).put(vs.get(kk + 2));
                                triIndices[2] = kk;

                                this.setTriangleNormals(null, triIndices);

                                kk = lrv * 3;
                                triVerts.put(vs.get(kk)).put(vs.get(kk + 1)).put(vs.get(kk + 2));
                                triIndices[0] = kk;

                                kk = (lrv - gridSize) * 3;
                                triVerts.put(vs.get(kk)).put(vs.get(kk + 1)).put(vs.get(kk + 2));
                                triIndices[1] = kk;

                                kk = (llv - gridSize) * 3;
                            }
                            else {
                                // Draw the floor.
                                kk = llv * 3;
                                triVerts.put(vs.get(kk)).put(vs.get(kk + 1)).put(vs.get(kk + 2));
                                triIndices[0] = kk;

                                kk = (llv - gridSize) * 3;
                                triVerts.put(vs.get(kk)).put(vs.get(kk + 1)).put(vs.get(kk + 2));
                                triIndices[1] = kk;

                                kk = (lrv - gridSize) * 3;
                                triVerts.put(vs.get(kk)).put(vs.get(kk + 1)).put(vs.get(kk + 2));
                                triIndices[2] = kk;

                                this.setTriangleNormals(null, triIndices);

                                kk = llv * 3;
                                triVerts.put(vs.get(kk)).put(vs.get(kk + 1)).put(vs.get(kk + 2));
                                triIndices[0] = kk;

                                kk = lrv * 3;
                                triVerts.put(vs.get(kk)).put(vs.get(kk + 1)).put(vs.get(kk + 2));
                                triIndices[1] = kk;

                                kk = (lrv - gridSize) * 3;
                            }
                            triVerts.put(vs.get(kk)).put(vs.get(kk + 1)).put(vs.get(kk + 2));
                            triIndices[2] = kk;
                            this.setTriangleNormals(null, triIndices);
                        }
                    }
                }
            }

            shapeData.triangleVertices.flip(); // capture the currently used buffer size as the limit.
            shapeData.triangleVertices = trimBuffer(shapeData.triangleVertices);

            shapeData.triangleNormals.flip();
            shapeData.triangleNormals = trimBuffer(shapeData.triangleNormals);
        }

        protected void setTriangleNormals(int[] flags, int[] indices) {
            ShapeData shapeData = this.getCurrent();

            // We want to use the actual normals -- the rays from the radar position to the grid positions -- when the
            // triangle is fully outward facing and not part of the floor. This prevents faceting of the volume's surface.

            if (flags != null && flags[0] == flags[1] && flags[1] == flags[2] && flags[2] == NO_OBSTRUCTION) {
                // Use the actual normal of each position.
                shapeData.triangleNormals.put(shapeData.gridNormals.get(indices[0]));
                shapeData.triangleNormals.put(shapeData.gridNormals.get(indices[0] + 1));
                shapeData.triangleNormals.put(shapeData.gridNormals.get(indices[0] + 2));
                shapeData.triangleNormals.put(shapeData.gridNormals.get(indices[1]));
                shapeData.triangleNormals.put(shapeData.gridNormals.get(indices[1] + 1));
                shapeData.triangleNormals.put(shapeData.gridNormals.get(indices[1] + 2));
                shapeData.triangleNormals.put(shapeData.gridNormals.get(indices[2]));
                shapeData.triangleNormals.put(shapeData.gridNormals.get(indices[2] + 1));
                shapeData.triangleNormals.put(shapeData.gridNormals.get(indices[2] + 2));
            }
            else {
                // Compute a single normal for the triangle and assign it to all three vertices.
                double x0 = shapeData.gridVertices.get(indices[0]);
                double y0 = shapeData.gridVertices.get(indices[0] + 1);
                double z0 = shapeData.gridVertices.get(indices[0] + 2);
                double x1 = shapeData.gridVertices.get(indices[1]);
                double y1 = shapeData.gridVertices.get(indices[1] + 1);
                double z1 = shapeData.gridVertices.get(indices[1] + 2);
                double x2 = shapeData.gridVertices.get(indices[2]);
                double y2 = shapeData.gridVertices.get(indices[2] + 1);
                double z2 = shapeData.gridVertices.get(indices[2] + 2);

                double ux = x1 - x0;
                double uy = y1 - y0;
                double uz = z1 - z0;

                double vx = x2 - x0;
                double vy = y2 - y0;
                double vz = z2 - z0;

                double nx = uy * vz - uz * vy;
                double ny = uz * vx - ux * vz;
                double nz = ux * vy - uy * vx;

                double length = Math.sqrt(nx * nx + ny * ny + nz * nz);
                if (length > 0) {
                    nx /= length;
                    ny /= length;
                    nz /= length;
                }

                shapeData.triangleNormals.put((float) nx).put((float) ny).put((float) nz);
                shapeData.triangleNormals.put((float) nx).put((float) ny).put((float) nz);
                shapeData.triangleNormals.put((float) nx).put((float) ny).put((float) nz);
            }
        }

        protected void makeSides() {
            // The sides consist of a single triangle strip going around the left, top and right sides of the volume.
            // Obscured positions on the sides are skipped.

            ShapeData shapeData = this.getCurrent();

            int numSideVertices = 2 * (2 * this.getHeight() + this.getWidth() - 2);

            shapeData.sideVertices = Buffers.newDirectFloatBuffer(3 * numSideVertices);
            int gridSize = this.getWidth() * this.getHeight();

            // Left side vertices.
            for (int i = 0; i < this.getHeight(); i++) {
                int k = gridSize + i * this.getWidth();
                if (this.obstructionFlags[k] != EXTERNAL_OBSTRUCTION) {
                    shapeData.sideVertices.put(shapeData.gridVertices.get(3 * k));
                    shapeData.sideVertices.put(shapeData.gridVertices.get(3 * k + 1));
                    shapeData.sideVertices.put(shapeData.gridVertices.get(3 * k + 2));

                    k -= gridSize;
                    shapeData.sideVertices.put(shapeData.gridVertices.get(3 * k));
                    shapeData.sideVertices.put(shapeData.gridVertices.get(3 * k + 1));
                    shapeData.sideVertices.put(shapeData.gridVertices.get(3 * k + 2));
                }
            }

            // Top vertices.
            for (int i = 1; i < this.getWidth(); i++) {
                int k = 2 * gridSize - this.getWidth() + i;
                if (this.obstructionFlags[k] != EXTERNAL_OBSTRUCTION) {
                    shapeData.sideVertices.put(shapeData.gridVertices.get(3 * k));
                    shapeData.sideVertices.put(shapeData.gridVertices.get(3 * k + 1));
                    shapeData.sideVertices.put(shapeData.gridVertices.get(3 * k + 2));

                    k -= gridSize;
                    shapeData.sideVertices.put(shapeData.gridVertices.get(3 * k));
                    shapeData.sideVertices.put(shapeData.gridVertices.get(3 * k + 1));
                    shapeData.sideVertices.put(shapeData.gridVertices.get(3 * k + 2));
                }
            }

            // Right side vertices.
            for (int i = 1; i < this.getHeight(); i++) {
                int k = 2 * gridSize - 1 - i * this.getWidth();
                if (this.obstructionFlags[k] != EXTERNAL_OBSTRUCTION) {
                    shapeData.sideVertices.put(shapeData.gridVertices.get(3 * k));
                    shapeData.sideVertices.put(shapeData.gridVertices.get(3 * k + 1));
                    shapeData.sideVertices.put(shapeData.gridVertices.get(3 * k + 2));

                    k -= gridSize;
                    shapeData.sideVertices.put(shapeData.gridVertices.get(3 * k));
                    shapeData.sideVertices.put(shapeData.gridVertices.get(3 * k + 1));
                    shapeData.sideVertices.put(shapeData.gridVertices.get(3 * k + 2));
                }
            }

            shapeData.sideVertices.flip();

            // Create the side indices.
            this.sideIndices = Buffers.newDirectIntBuffer(shapeData.sideVertices.limit() / 3);
            for (int i = 0; i < this.sideIndices.limit(); i++) {
                this.sideIndices.put(i);
            }

            // Allocate and zero a buffer for the side normals then generate the side normals.
            shapeData.sideNormals = Buffers.newDirectFloatBuffer(shapeData.sideVertices.limit());
            while (shapeData.sideNormals.position() < shapeData.sideNormals.limit()) {
                shapeData.sideNormals.put(0);
            }
            WWUtil.generateTriStripNormals(shapeData.sideVertices, this.sideIndices, shapeData.sideNormals);
        }

        @Override
        protected void fillVBO(DrawContext dc) {
            // Not using VBOs.
        }

        public Extent getExtent(Globe globe, double verticalExaggeration) {
            // See if we've cached an extent associated with the globe.
            Extent extent = super.getExtent(globe, verticalExaggeration);
            if (extent != null) {
                return extent;
            }

            this.getCurrent().setExtent(super.computeExtentFromPositions(globe, verticalExaggeration, this.positions));

            return this.getCurrent().getExtent();
        }

        @Override
        public Sector getSector() {
            if (this.sector != null) {
                return this.sector;
            }

            this.sector = Sector.boundingSector(this.positions);

            return this.sector;
        }

        @Override
        public Position getReferencePosition() {
            return this.positions.get(0);
        }

        @Override
        public void moveTo(Position position) {
            // Not supported
        }

        @Override
        public List<Intersection> intersect(Line line, Terrain terrain) {
            return null;
        }

        @Override
        public String isExportFormatSupported(String mimeType) {
            return Exportable.FORMAT_NOT_SUPPORTED;
        }

        @Override
        protected void doExportAsKML(XMLStreamWriter xmlWriter) {
            throw new UnsupportedOperationException("KML output not supported for RadarVolume");
        }

        /**
         * This class holds globe-specific data for this shape. It's managed via the shape-data cache in {@link
         * AbstractShapeData}.
         */
        protected static class ShapeData extends AbstractShapeData {
            // The grid vertices and grid normals below are used only during volume creation and are cleared afterwards.
            protected FloatBuffer gridVertices; // Cartesian versions of the grid vertices, referenced only, not displayed
            protected FloatBuffer gridNormals; // the normals for the gridVertices buffer
            protected FloatBuffer triangleVertices; // vertices of the grid and floor triangles
            protected FloatBuffer triangleNormals; // normals of the grid and floor triangles
            protected FloatBuffer sideVertices; // vertices of the volume's sides -- all but the grids and the floor
            protected FloatBuffer sideNormals; // normals of the side vertices
            protected Vec4 centerPoint; // the volume's approximate center; used to determine eye distance

            /**
             * Construct a cache entry using the boundaries of this shape.
             *
             * @param dc    the current draw context.
             * @param shape this shape.
             */
            public ShapeData(DrawContext dc, RadarVolume shape) {
                super(dc, shape.minExpiryTime, shape.maxExpiryTime);
            }

            @Override
            public boolean isValid(DrawContext dc) {
                return super.isValid(dc) && this.gridVertices != null;// && this.normals != null;
            }

            @Override
            public boolean isExpired(DrawContext dc) {
                return false; // the computed data is terrain independent and therevore never expired
            }
        }
    }
}
