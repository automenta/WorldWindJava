/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.animation;

import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.util.*;

/**
 * @author jym
 * @version $Id: PositionAnimator.java 1171 2013-02-11 21:45:02Z dcollins $
 */
public class PositionAnimator extends BasicAnimator {

    protected final PropertyAccessor.PositionAccessor propertyAccessor;
    protected Position begin;
    protected Position end;

    public PositionAnimator(
        Interpolator interpolator,
        Position begin,
        Position end,
        PropertyAccessor.PositionAccessor propertyAccessor) {
        super(interpolator);
        if (interpolator == null) {
            this.interpolator = new ScheduledInterpolator(10000);
        }
        if (begin == null || end == null) {
            String message = Logging.getMessage("nullValue.PositionIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        if (propertyAccessor == null) {
            String message = Logging.getMessage("nullValue.ViewPropertyAccessorIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.begin = begin;
        this.end = end;
        this.propertyAccessor = propertyAccessor;
    }

    public Position getBegin() {
        return this.begin;
    }

    public void setBegin(Position begin) {
        this.begin = begin;
    }

    public Position getEnd() {
        return this.end;
    }

    public void setEnd(Position end) {
        this.end = end;
    }

    public PropertyAccessor.PositionAccessor getPropertyAccessor() {
        return this.propertyAccessor;
    }

    protected void setImpl(double interpolant) {
        Position newValue = this.nextPosition(interpolant);
        if (newValue == null)
            return;

        boolean success = this.propertyAccessor.setPosition(newValue);
        if (!success) {
            this.flagLastStateInvalid();
        }
        if (interpolant >= 1) {
            this.stop();
        }
    }

    protected Position nextPosition(double interpolant) {
        return Position.interpolateGreatCircle(this.begin, this.end, interpolant);
    }
}
