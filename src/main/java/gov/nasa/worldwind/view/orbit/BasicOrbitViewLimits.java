/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.view.orbit;

import gov.nasa.worldwind.View;
import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.util.*;
import gov.nasa.worldwind.view.BasicViewPropertyLimits;

/**
 * BasicOrbitViewLimits provides an implementation of OrbitViewLimits.
 *
 * @author dcollins
 * @version $Id: BasicOrbitViewLimits.java 2253 2014-08-22 16:33:46Z dcollins $
 */
public class BasicOrbitViewLimits extends BasicViewPropertyLimits implements OrbitViewLimits {
    protected Sector centerLocationLimits;
    protected double minCenterElevation;
    protected double maxCenterElevation;
    protected double minZoom;
    protected double maxZoom;

    /**
     * Creates a new BasicOrbitViewLimits with default limits.
     */
    public BasicOrbitViewLimits() {
        this.reset();
    }

    /**
     * Applies the orbit view property limits to the specified view.
     *
     * @param view       the view that receives the property limits.
     * @param viewLimits defines the view property limits.
     * @throws IllegalArgumentException if any argument is null.
     * @deprecated Use methods that limit individual view properties directly: {@link #limitCenterPosition(View,
     * Position)}, {@link #limitHeading(View,
     * Angle)}, etc.
     */
    @Deprecated
    public static void applyLimits(OrbitView view, OrbitViewLimits viewLimits) {
        if (view == null) {
            String message = Logging.getMessage("nullValue.ViewIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        if (viewLimits == null) {
            String message = Logging.getMessage("nullValue.ViewLimitsIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        view.setCenterPosition(limitCenterPosition(view.getCenterPosition(), viewLimits));
        view.setHeading(limitHeading(view.getHeading(), viewLimits));
        view.setPitch(limitPitch(view.getPitch(), viewLimits));
        view.setZoom(limitZoom(view.getZoom(), viewLimits));
    }

    /**
     * Clamp center location angles and elevation to the range specified in a limit object.
     *
     * @param position   position to clamp to the allowed range.
     * @param viewLimits defines the center location and elevation limits.
     * @return The clamped position.
     * @throws IllegalArgumentException if any argument is null.
     * @deprecated Use {@link #limitCenterPosition(View, Position)} instead.
     */
    @Deprecated
    public static Position limitCenterPosition(Position position, OrbitViewLimits viewLimits) {
        if (position == null) {
            String message = Logging.getMessage("nullValue.PositionIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        if (viewLimits == null) {
            String message = Logging.getMessage("nullValue.ViewLimitsIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        return new Position(
            limitCenterLocation(position.getLatitude(), position.getLongitude(), viewLimits),
            limitCenterElevation(position.getElevation(), viewLimits));
    }

    /**
     * Clamp center location angles to the range specified in a limit object.
     *
     * @param latitude   latitude angle to clamp to the allowed range.
     * @param longitude  longitude angle to clamp to the allowed range.
     * @param viewLimits defines the center location limits.
     * @return The clamped location.
     * @throws IllegalArgumentException if any argument is null.
     * @deprecated Use {@link #limitCenterPosition(View, Position)} instead.
     */
    @Deprecated
    public static LatLon limitCenterLocation(Angle latitude, Angle longitude, OrbitViewLimits viewLimits) {
        if (latitude == null || longitude == null) {
            String message = Logging.getMessage("nullValue.LatitudeOrLongitudeIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        if (viewLimits == null) {
            String message = Logging.getMessage("nullValue.ViewLimitsIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        Sector limits = viewLimits.getCenterLocationLimits();
        Angle newLatitude = latitude;
        Angle newLongitude = longitude;

        if (latitude.compareTo(limits.latMin()) < 0) {
            newLatitude = limits.latMin();
        }
        else if (latitude.compareTo(limits.latMax()) > 0) {
            newLatitude = limits.latMax();
        }

        if (longitude.compareTo(limits.lonMin()) < 0) {
            newLongitude = limits.lonMin();
        }
        else if (longitude.compareTo(limits.lonMax()) > 0) {
            newLongitude = limits.lonMax();
        }

        return new LatLon(newLatitude, newLongitude);
    }

    /**
     * Clamp an center elevation to the range specified in a limit object.
     *
     * @param value      elevation to clamp to the allowed range.
     * @param viewLimits defines the center elevation limits.
     * @return The clamped elevation.
     * @throws IllegalArgumentException if any argument is null.
     * @deprecated Use {@link #limitCenterPosition(View, Position)} instead.
     */
    @Deprecated
    public static double limitCenterElevation(double value, OrbitViewLimits viewLimits) {
        if (viewLimits == null) {
            String message = Logging.getMessage("nullValue.ViewLimitsIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        double[] limits = viewLimits.getCenterElevationLimits();
        double newValue = value;

        if (value < limits[0]) {
            newValue = limits[0];
        }
        else if (value > limits[1]) {
            newValue = limits[1];
        }

        return newValue;
    }

    /**
     * Clamp an zoom distance to the range specified in a limit object.
     *
     * @param value      distance to clamp to the allowed range.
     * @param viewLimits defines the zoom distance limits.
     * @return The clamped zoom.
     * @throws IllegalArgumentException if any argument is null.
     * @deprecated Use {@link #limitZoom(View, double)} instead.
     */
    @Deprecated
    public static double limitZoom(double value, OrbitViewLimits viewLimits) {
        if (viewLimits == null) {
            String message = Logging.getMessage("nullValue.ViewLimitsIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        double[] limits = viewLimits.getZoomLimits();
        double newValue = value;

        if (value < limits[0]) {
            newValue = limits[0];
        }
        else if (value > limits[1]) {
            newValue = limits[1];
        }

        return newValue;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Sector getCenterLocationLimits() {
        return this.centerLocationLimits;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setCenterLocationLimits(Sector sector) {
        if (sector == null) {
            String message = Logging.getMessage("nullValue.SectorIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.centerLocationLimits = sector;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double[] getCenterElevationLimits() {
        return new double[] {this.minCenterElevation, this.maxCenterElevation};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setCenterElevationLimits(double minValue, double maxValue) {
        this.minCenterElevation = minValue;
        this.maxCenterElevation = maxValue;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double[] getZoomLimits() {
        return new double[] {this.minZoom, this.maxZoom};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setZoomLimits(double minValue, double maxValue) {
        this.minZoom = minValue;
        this.maxZoom = maxValue;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void reset() {
        super.reset();

        this.centerLocationLimits = Sector.FULL_SPHERE;
        this.minCenterElevation = -Double.MAX_VALUE;
        this.maxCenterElevation = Double.MAX_VALUE;
        this.minZoom = 0;
        this.maxZoom = Double.MAX_VALUE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Position limitCenterPosition(View view, Position position) {
        if (view == null) {
            String message = Logging.getMessage("nullValue.ViewIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (position == null) {
            String message = Logging.getMessage("nullValue.PositionIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        Sector sector = this.centerLocationLimits;
        Angle lat = Angle.clamp(position.latitude, sector.latMin(), sector.latMax());
        Angle lon = Angle.clamp(position.longitude, sector.lonMin(), sector.lonMax());
        double alt = WWMath.clamp(position.elevation, this.minCenterElevation, this.maxCenterElevation);

        return new Position(lat, lon, alt);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double limitZoom(View view, double value) {
        if (view == null) {
            String message = Logging.getMessage("nullValue.ViewIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        double minZoom = this.minZoom;
        double maxZoom = this.maxZoom;

        if (BasicViewPropertyLimits.is2DGlobe(view.getGlobe())) // limit zoom to ~360 degrees of visible longitude on 2D globes
        {
            double max2DZoom = Math.PI * view.getGlobe().getEquatorialRadius() / view.getFieldOfView().tanHalfAngle();
            if (minZoom > max2DZoom)
                minZoom = max2DZoom;
            if (maxZoom > max2DZoom)
                maxZoom = max2DZoom;
        }

        return WWMath.clamp(value, minZoom, maxZoom);
    }

    //**************************************************************//
    //******************** Restorable State  ***********************//
    //**************************************************************//

    @Override
    public void getRestorableState(RestorableSupport rs, RestorableSupport.StateObject context) {
        super.getRestorableState(rs, context);

        rs.addStateValueAsSector(context, "centerLocationLimits", this.centerLocationLimits);
        rs.addStateValueAsDouble(context, "minCenterElevation", this.minCenterElevation);
        rs.addStateValueAsDouble(context, "maxCenterElevation", this.maxCenterElevation);
        rs.addStateValueAsDouble(context, "minZoom", this.minZoom);
        rs.addStateValueAsDouble(context, "maxZoom", this.maxZoom);
    }

    @Override
    public void restoreState(RestorableSupport rs, RestorableSupport.StateObject context) {
        super.restoreState(rs, context);

        Sector sector = rs.getStateValueAsSector(context, "centerLocationLimits");
        if (sector != null)
            this.setCenterLocationLimits(sector);

        // Min and max center elevation.
        double[] minAndMaxValue = this.getCenterElevationLimits();
        Double min = rs.getStateValueAsDouble(context, "minCenterElevation");
        if (min != null)
            minAndMaxValue[0] = min;

        Double max = rs.getStateValueAsDouble(context, "maxCenterElevation");
        if (max != null)
            minAndMaxValue[1] = max;

        if (min != null || max != null)
            this.setCenterElevationLimits(minAndMaxValue[0], minAndMaxValue[1]);

        // Min and max zoom value.        
        minAndMaxValue = this.getZoomLimits();
        min = rs.getStateValueAsDouble(context, "minZoom");
        if (min != null)
            minAndMaxValue[0] = min;

        max = rs.getStateValueAsDouble(context, "maxZoom");
        if (max != null)
            minAndMaxValue[1] = max;

        if (min != null || max != null)
            this.setZoomLimits(minAndMaxValue[0], minAndMaxValue[1]);
    }
}
