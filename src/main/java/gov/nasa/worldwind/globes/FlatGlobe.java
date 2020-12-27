/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.globes;

import gov.nasa.worldwind.*;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.globes.projections.*;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.util.Logging;

import java.util.Objects;

/**
 * Defines a globe represented as a projection onto a plane. The projection type is modifiable. The default projection
 * is Mercator. New projections may be added by extending this class and overriding {@link #geodeticToCartesian(Angle,
 * Angle, double) geodeticToCartesian} {@link #cartesianToGeodetic(Vec4) cartesianToGeodetic}.
 * <p>
 * This globe uses a Cartesian coordinate system in the world plane is located at the origin and has UNIT-Z as normal.
 * The Y axis points to the north pole. The Z axis points up. The X axis completes a right-handed coordinate system, and
 * points east. Latitude and longitude zero are at the origin on y and x respectively. Sea level is at z = zero.
 *
 * @author Patrick Murris
 * @version $Id: FlatGlobe.java 2277 2014-08-28 21:19:37Z dcollins $
 */
public class FlatGlobe extends EllipsoidalGlobe implements Globe2D {
    /**
     * <a href="http://en.wikipedia.org/wiki/Plate_carr%C3%A9e_projection" target="_blank">Latitude/Longitude</a>
     * projection. Also known as the geographic projection, the equirectangular projection, or the Plate Carree
     * projection.
     */
    public final static String PROJECTION_LAT_LON = "gov.nasa.worldwind.globes.projectionLatLon";
    /**
     * <a href="http://en.wikipedia.org/wiki/Mercator_projection" target="_blank">Mercator</a> projection.
     */
    public final static String PROJECTION_MERCATOR = "gov.nasa.worldwind.globes.projectionMercator";
    /**
     * <a href="http://en.wikipedia.org/wiki/Sinusoidal_projection" target="_blank">Sinusoidal</a> projection.
     */
    public final static String PROJECTION_SINUSOIDAL = "gov.nasa.worldwind.globes.projectionSinusoidal";
    public final static String PROJECTION_MODIFIED_SINUSOIDAL =
        "gov.nasa.worldwind.globes.projectionModifiedSinusoidal";
    private final GlobeStateKey stateKey = new FlatStateKey();
    protected GeographicProjection projection = (GeographicProjection) WorldWind.createComponent(
        Configuration.getStringValue(AVKey.GEOGRAPHIC_PROJECTION_CLASS_NAME,
            "gov.nasa.worldwind.globes.projections.ProjectionEquirectangular"));
    protected boolean continuous;
    protected int offset;
    protected Vec4 offsetVector = Vec4.ZERO;

    /**
     * Create a new globe. The globe will use the Mercator projection. The projection can be changed using {@link
     * #setProjection(GeographicProjection)}.
     *
     * @param equatorialRadius Radius of the globe at the equator.
     * @param polarRadius      Radius of the globe at the poles.
     * @param es               Square of the globe's eccentricity.
     * @param em               Elevation model. May be null.
     */
    public FlatGlobe(double equatorialRadius, double polarRadius, double es, ElevationModel em) {
        super(equatorialRadius, polarRadius, es, em);
    }

    public Object getStateKey(DrawContext dc) {
        return this.getGlobeStateKey(dc);
    }

    public GlobeStateKey getGlobeStateKey(DrawContext dc) {
        return new FlatStateKey(dc);
    }

    public final GlobeStateKey getGlobeStateKey() {
        return stateKey;
    }

    /**
     * Indicates the projection used to project the globe onto a plane. The default projection is Mercator.
     *
     * @return The active projection.
     * @see #setProjection
     */
    public GeographicProjection getProjection() {
        return this.projection;
    }

    /**
     * Set the projection used to project the globe onto a plane.
     *
     * @param projection New projection. One of {@link #PROJECTION_LAT_LON}, {@link #PROJECTION_MERCATOR}, {@link
     *                   #PROJECTION_SINUSOIDAL}, or {@link #PROJECTION_MODIFIED_SINUSOIDAL}.
     * @deprecated Use {@link #setProjection(GeographicProjection)}.
     */
    @Deprecated
    public void setProjection(String projection) {
        if (projection == null) {
            String message = Logging.getMessage("nullValue.GeographicProjectionIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        switch (projection) {
            case FlatGlobe.PROJECTION_MERCATOR -> this.setProjection(new ProjectionMercator());
            case FlatGlobe.PROJECTION_SINUSOIDAL -> this.setProjection(new ProjectionSinusoidal());
            case FlatGlobe.PROJECTION_MODIFIED_SINUSOIDAL -> this.setProjection(new ProjectionModifiedSinusoidal());
            default -> this.setProjection(new ProjectionEquirectangular());
        }
    }

    public void setProjection(GeographicProjection projection) {
        if (projection == null) {
            String message = Logging.getMessage("nullValue.GeographicProjectionIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.projection = projection;
    }

    @Override
    public boolean isContinuous() {
        return this.continuous || (this.projection != null && this.projection.isContinuous());
    }

    @Override
    public void setContinuous(boolean continuous) {
        this.continuous = continuous;
    }

    public int getOffset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
        this.offsetVector = new Vec4(2.0 * Math.PI * this.equatorialRadius * this.offset, 0, 0);
    }

    public boolean intersects(Frustum frustum) {
        if (frustum == null) {
            String message = Logging.getMessage("nullValue.FrustumIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        return Sector.computeBoundingBox(this, 1.0, Sector.FULL_SPHERE, this.getMinElevation(),
            this.getMaxElevation()).intersects(frustum);
    }

    @Override
    protected Intersection[] intersect(Line line, double equRadius, double polarRadius) {
        // Flat World Note: plane/line intersection point (OK)
        // Flat World Note: extract altitude from equRadius by subtracting this.equatorialRadius (OK)
        if (line == null) {
            String message = Logging.getMessage("nullValue.LineIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        // Intersection with world plane
        Plane plane = new Plane(0, 0, 1, -(equRadius - this.equatorialRadius));   // Flat globe plane
        Vec4 p = plane.intersect(line);
        if (p == null)
            return null;
        // Check if we are in the world boundaries
        Position pos = this.computePositionFromPoint(p);
        if (pos == null)
            return null;
        if (pos.getLatitude().degrees < -90 || pos.getLatitude().degrees > 90)
            return null;
        if (!this.isContinuous() && (pos.getLongitude().degrees < -180 || pos.getLongitude().degrees > 180))
            return null;

        return new Intersection[] {new Intersection(p, false)};
    }

    @Override
    public boolean intersects(Line line) {
        // Flat World Note: plane/line intersection test (OK)
        if (line == null) {
            String msg = Logging.getMessage("nullValue.LineIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        return this.intersect(line) != null;
    }

    @Override
    public boolean intersects(Plane plane) {
        // Flat World Note: plane/plane intersection test (OK)
        if (plane == null) {
            String msg = Logging.getMessage("nullValue.PlaneIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        Vec4 n = plane.getNormal();
        return !(n.x == 0 && n.y == 0 && n.z == 1);
    }

    @Override
    public Vec4 computeSurfaceNormalAtLocation(Angle latitude, Angle longitude) {
        // Flat World Note: return constant (OK)
        if (latitude == null || longitude == null) {
            String message = Logging.getMessage("nullValue.LatitudeOrLongitudeIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        return Vec4.UNIT_Z;
    }

    @Override
    public Vec4 computeSurfaceNormalAtPoint(Vec4 point) {
        // Flat World Note: return constant (OK)
        if (point == null) {
            String msg = Logging.getMessage("nullValue.PointIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        return Vec4.UNIT_Z;
    }

    @Override
    public Vec4 computeNorthPointingTangentAtLocation(Angle latitude, Angle longitude) {
        if (latitude == null || longitude == null) {
            String message = Logging.getMessage("nullValue.LatitudeOrLongitudeIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        return this.projection.northPointingTangent(this, latitude, longitude);
    }

    @Override
    public Matrix computeSurfaceOrientationAtPosition(Angle latitude, Angle longitude, double metersElevation) {
        if (latitude == null || longitude == null) {
            String message = Logging.getMessage("nullValue.LatitudeOrLongitudeIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        // Compute the origin as the cartesian coordinate at (latitude, longitude, metersElevation).
        Vec4 origin = this.geodeticToCartesian(latitude, longitude, metersElevation);

        // Compute the the local xyz coordinate axes at (latitude, longitude, metersElevation) as follows:
        Vec4 z = this.computeSurfaceNormalAtLocation(latitude, longitude);
        Vec4 y = this.computeNorthPointingTangentAtLocation(latitude, longitude);
        Vec4 x = y.cross3(z); // east pointing tangent
        Vec4[] axes = {x, y, z};

        return Matrix.fromLocalOrientation(origin, axes);
    }

    @Override
    public double getElevation(Angle latitude, Angle longitude) {
        if (latitude == null || longitude == null) {
            String message = Logging.getMessage("nullValue.LatitudeOrLongitudeIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        // Flat World Note: return zero if outside the lat/lon normal boundaries (OK)
        if (latitude.degrees < -90 || latitude.degrees > 90 || longitude.degrees < -180 || longitude.degrees > 180)
            return 0.0d;

        return super.getElevation(latitude, longitude);
    }

    /**
     * Maps a position to a flat world Cartesian coordinates. The world plane is located at the origin and has UNIT-Z as
     * normal. The Y axis points to the north pole. The Z axis points up. The X axis completes a right-handed coordinate
     * system, and points east. Latitude and longitude zero are at the origin on y and x respectively. Sea level is at z
     * = zero.
     *
     * @param latitude        the latitude of the position.
     * @param longitude       the longitude of the position.
     * @param metersElevation the number of meters above or below mean sea level.
     * @return The Cartesian point corresponding to the input position.
     */
    @Override
    protected Vec4 geodeticToCartesian(Angle latitude, Angle longitude, double metersElevation) {
        if (latitude == null || longitude == null) {
            String message = Logging.getMessage("nullValue.LatitudeOrLongitudeIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        return this.projection.geographicToCartesian(this, latitude, longitude, metersElevation,
            this.offsetVector);
    }

    @Override
    protected void geodeticToCartesian(Sector sector, int numLat, int numLon, double[] metersElevation, Vec4[] out) {
        this.projection.geographicToCartesian(this, sector, numLat, numLon, metersElevation, this.offsetVector, out);
    }

    @Override
    protected Position cartesianToGeodetic(Vec4 cart) {
        if (cart == null) {
            String message = Logging.getMessage("nullValue.PointIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        Position pos = this.projection.cartesianToGeographic(this, cart, this.offsetVector);
        if (this.isContinuous()) {
            // Wrap if the globe is continuous.
            if (pos.getLongitude().degrees < -180)
                pos = Position.fromDegrees(pos.getLatitude().degrees, pos.getLongitude().degrees + 360,
                    pos.getAltitude());
            else if (pos.getLongitude().degrees > 180)
                pos = Position.fromDegrees(pos.getLatitude().degrees, pos.getLongitude().degrees - 360,
                    pos.getAltitude());
        }

        return pos;
    }

    /**
     * Determines whether a point is above a given elevation
     *
     * @param point     the <code>Vec4</code> point to test.
     * @param elevation the elevation to test for.
     * @return true if the given point is above the given elevation.
     */
    public boolean isPointAboveElevation(Vec4 point, double elevation) {
        //noinspection SimplifiableIfStatement
        if (point == null)
            return false;

        return point.z > elevation;
    }

    private class FlatStateKey extends StateKey {
        protected final GeographicProjection projection;
        protected final int offset;
        protected double verticalExaggeration;

        public FlatStateKey(DrawContext dc) {
            super(dc);
            this.projection = FlatGlobe.this.getProjection();
            this.offset = FlatGlobe.this.offset;
        }

        public FlatStateKey() {
            super();
            this.projection = FlatGlobe.this.getProjection();
            this.offset = FlatGlobe.this.offset;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (!super.equals(o))
                return false;

            FlatStateKey that = (FlatStateKey) o;

            if (offset != that.offset)
                return false;
            if (Double.compare(that.verticalExaggeration, verticalExaggeration) != 0)
                return false;
            //noinspection RedundantIfStatement
            if (!Objects.equals(projection, that.projection))
                return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = super.hashCode();
            long temp;
            result = 31 * result + (projection != null ? projection.hashCode() : 0);
            temp = verticalExaggeration == +0.0d ? 0L : Double.doubleToLongBits(verticalExaggeration);
            result = 31 * result + (int) (temp ^ (temp >>> 32));
            result = 31 * result + offset;
            return result;
        }
    }
}
