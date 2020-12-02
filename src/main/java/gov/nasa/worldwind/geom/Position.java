/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.geom;

import gov.nasa.worldwind.globes.Globe;
import gov.nasa.worldwind.util.*;

import java.util.*;

/**
 * @author tag
 * @version $Id: Position.java 2291 2014-08-30 21:38:47Z tgaskins $
 */
public class Position extends LatLon {

    public static final Position ZERO = new Position(Angle.ZERO, Angle.ZERO, 0.0d);

    public final double elevation;

    public Position(Angle latitude, Angle longitude, double elevation) {
        super(latitude, longitude);
        this.elevation = elevation;
    }

    public Position(LatLon latLon, double elevation) {
        super(latLon);
        this.elevation = elevation;
    }

    public Position(Position that) {
        this(that.latitude, that.longitude, that.elevation);
    }

    public static Position fromRadians(double latitude, double longitude, double elevation) {
        return new Position(Angle.fromRadians(latitude), Angle.fromRadians(longitude), elevation);
    }

    public static Position fromDegrees(double latitude, double longitude, double elevation) {
        return new Position(Angle.fromDegrees(latitude), Angle.fromDegrees(longitude), elevation);
    }

    public static Position fromDegrees(double latitude, double longitude) {
        return new Position(Angle.fromDegrees(latitude), Angle.fromDegrees(longitude), 0);
    }

    /**
     * Returns the linear interpolation of <code>value1</code> and <code>value2</code>, treating the geographic
     * locations as simple 2D coordinate pairs, and treating the elevation values as 1D scalars.
     *
     * @param x the first position.
     * @param y the second position.
     * @param a the interpolation factor
     * @return the linear interpolation of <code>value1</code> and <code>value2</code>.
     * @throws IllegalArgumentException if either position is null.
     */
    public static Position interpolate(Position x, Position y, double a) {

        if (a < 0) {
            return x;
        }else if (a > 1) {
            return y;
        }

        LatLon latLon = LatLon.interpolate(a, x, y);
        // Elevation is independent of geographic interpolation method (i.e. rhumb, great-circle, linear), so we
        // interpolate elevation linearly.

        return new Position(latLon,
            WWMath.mix(a, x.getElevation(), y.getElevation()));
    }

    /**
     * Returns the an interpolated location along the great-arc between <code>value1</code> and <code>value2</code>. The
     * position's elevation components are linearly interpolated as a simple 1D scalar value. The interpolation factor
     * <code>amount</code> defines the weight given to each value, and is clamped to the range [0, 1]. If
     * <code>a</code>
     * is 0 or less, this returns <code>value1</code>. If <code>amount</code> is 1 or more, this returns
     * <code>value2</code>. Otherwise, this returns the position on the great-arc between <code>value1</code> and
     * <code>value2</code> with a linearly interpolated elevation component, and corresponding to the specified
     * interpolation factor.
     *
     * @param x the first position.
     * @param y the second position.
     * @param a the interpolation factor
     * @return an interpolated position along the great-arc between <code>value1</code> and <code>value2</code>, with a
     * linearly interpolated elevation component.
     * @throws IllegalArgumentException if either location is null.
     */
    public static Position interpolateGreatCircle(Position x, Position y, double a) {

        LatLon latLon = LatLon.interpolateGreatCircle(a, x, y);
        // Elevation is independent of geographic interpolation method (i.e. rhumb, great-circle, linear), so we
        // interpolate elevation linearly.
        double elevation = WWMath.mix(a, x.getElevation(), y.getElevation());

        return new Position(latLon, elevation);
    }

    /**
     * Returns the an interpolated location along the rhumb line between <code>value1</code> and <code>value2</code>.
     * The position's elevation components are linearly interpolated as a simple 1D scalar value. The interpolation
     * factor <code>amount</code> defines the weight given to each value, and is clamped to the range [0, 1]. If
     * <code>a</code> is 0 or less, this returns <code>value1</code>. If <code>amount</code> is 1 or more, this returns
     * <code>value2</code>. Otherwise, this returns the position on the rhumb line between <code>value1</code> and
     * <code>value2</code> with a linearly interpolated elevation component, and corresponding to the specified
     * interpolation factor.
     *
     * @param x the first position.
     * @param y the second position.
     * @param a the interpolation factor
     * @return an interpolated position along the great-arc between <code>value1</code> and <code>value2</code>, with a
     * linearly interpolated elevation component.
     * @throws IllegalArgumentException if either location is null.
     */
    public static Position interpolateRhumb(Position x, Position y, double a) {

        LatLon latLon = LatLon.interpolateRhumb(a, x, y);
        // Elevation is independent of geographic interpolation method (i.e. rhumb, great-circle, linear), so we
        // interpolate elevation linearly.
        double elevation = WWMath.mix(a, x.getElevation(), y.getElevation());

        return new Position(latLon, elevation);
    }

    public static boolean positionsCrossDateLine(Iterable<? extends Position> positions) {

        Position pos = null;
        for (Position posNext : positions) {
            if (pos != null) {
                // A segment cross the line if end pos have different longitude signs
                // and are more than 180 degress longitude apart
                if (Math.signum(pos.getLongitude().degrees) != Math.signum(posNext.getLongitude().degrees)) {
                    double delta = Math.abs(pos.getLongitude().degrees - posNext.getLongitude().degrees);
                    if (delta > 180 && delta < 360) {
                        return true;
                    }
                }
            }
            pos = posNext;
        }

        return false;
    }

    /**
     * Computes a new set of positions translated from a specified reference position to a new reference position.
     *
     * @param from the original reference position.
     * @param to the new reference position.
     * @param x   the positions to translate.
     * @return the translated positions, or null if the positions could not be translated.
     * @throws IllegalArgumentException if any argument is null.
     */
    public static List<Position> computeShiftedPositions(Position from, Position to,
        Iterable<? extends Position> x) {
        // TODO: Account for dateline spanning

        List<Position> y = new ArrayList<>(WWUtil.sizeEstimate(x));

        double elevDelta = to.getElevation() - from.getElevation();

        for (Position p : x)
            y.add(new Position(LatLon.greatCircleEndPosition(to,
                LatLon.greatCircleAzimuth(from, p),
                LatLon.greatCircleDistance(from, p)),
                p.getElevation() + elevDelta));

        return y;
    }

    public static List<Position> computeShiftedPositions(Globe globe, Position oldPosition, Position newPosition,
        Iterable<? extends Position> x) {

        double elevDelta = newPosition.getElevation() - oldPosition.getElevation();
        Vec4 oldPoint = globe.computePointFromPosition(oldPosition);
        Vec4 newPoint = globe.computePointFromPosition(newPosition);
        Vec4 delta = newPoint.subtract3(oldPoint);

        List<Position> y = new ArrayList<>(WWUtil.sizeEstimate(x));
        for (Position p : x) {
            Vec4 v = globe.computePointFromPosition(p);
            v = v.add3(delta);
            y.add(new Position(globe.computePositionFromPoint(v), p.getElevation() + elevDelta));
        }

        return y;
    }

    /**
     * Obtains the elevation of this position
     *
     * @return this position's elevation
     */
    public double getElevation() {
        return this.elevation;
    }

    /**
     * Obtains the elevation of this position
     *
     * @return this position's elevation
     */
    public double getAltitude() {
        return this.elevation;
    }

    public Position add(Position that) {
        Angle lat = Angle.latNorm(this.latitude.add(that.latitude));
        Angle lon = Angle.lonNorm(this.longitude.add(that.longitude));

        return new Position(lat, lon, this.elevation + that.elevation);
    }

    public Position subtract(Position that) {
        Angle lat = Angle.latNorm(this.latitude.sub(that.latitude));
        Angle lon = Angle.lonNorm(this.longitude.sub(that.longitude));

        return new Position(lat, lon, this.elevation - that.elevation);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        return super.equals(o) && (((Position) o).elevation == elevation);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Double.hashCode(elevation);
    }

    public String toString() {
        return "(" + this.latitude.toString() + ", " + this.longitude.toString() + ", " + this.elevation + ")";
    }

    // A class that makes it easier to pass around position lists.
    public static class PositionList {

        public final List<? extends Position> list;

        public PositionList(List<? extends Position> list) {
            this.list = list;
        }
    }
}
