/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.view.orbit;

import gov.nasa.worldwind.animation.MoveToPositionAnimator;
import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.util.PropertyAccessor;

/**
 * A position animator that has the ability to adjust the view to focus on the terrain when it is stopped.
 *
 * @author jym
 * @version $Id: OrbitViewCenterAnimator.java 1171 2013-02-11 21:45:02Z dcollins $
 */
public class OrbitViewCenterAnimator extends MoveToPositionAnimator {
    final boolean endCenterOnSurface;
    private final BasicOrbitView orbitView;

    public OrbitViewCenterAnimator(BasicOrbitView orbitView, Position startPosition, Position endPosition,
        double smoothing, PropertyAccessor.PositionAccessor propertyAccessor, boolean endCenterOnSurface) {
        super(startPosition, endPosition, smoothing, propertyAccessor);
        this.endCenterOnSurface = endCenterOnSurface;
        this.orbitView = orbitView;
    }

    public Position nextPosition(double interpolant) {
        Position nextPosition = this.end;
        Position curCenter = this.propertyAccessor.getPosition();

        double latlonDifference = LatLon.greatCircleDistance(nextPosition, curCenter).degrees;
        double elevDifference = Math.abs(nextPosition.getElevation() - curCenter.getElevation());
        boolean stopMoving = Math.max(latlonDifference, elevDifference) < this.positionMinEpsilon;
        if (!stopMoving) {
            interpolant = 1 - this.smoothing;
            nextPosition = new Position(
                Angle.mix(interpolant, curCenter.getLat(), this.end.getLat()),
                Angle.mix(interpolant, curCenter.getLon(), this.end.getLon()),
                (1 - interpolant) * curCenter.getElevation() + interpolant * this.end.getElevation());
        }
        //TODO: What do we do about collisions?

        // If target is close, cancel future value changes.
        if (stopMoving) {
            this.stop();
            this.propertyAccessor.setPosition(nextPosition);
            if (endCenterOnSurface)
                this.orbitView.setViewOutOfFocus(true);
            return (null);
        }
        return nextPosition;
    }

    protected void setImpl(double interpolant) {
        Position newValue = this.nextPosition(interpolant);
        if (newValue == null)
            return;

        this.propertyAccessor.setPosition(newValue);
        this.orbitView.setViewOutOfFocus(true);
    }
}