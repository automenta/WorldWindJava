/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.animation;

import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.util.*;

/**
 * @author jym
 * @version $Id: AngleAnimator.java 1171 2013-02-11 21:45:02Z dcollins $
 */

/**
 * Animates angles, via an interpolator.  {@link #begin} and {@link #end} values can be reset once the animation is
 * already in motion.
 */
public class AngleAnimator extends BasicAnimator {

    /**
     * The @link gov.nasa.worldwind.util.PropertyAccessor used to modify the data value being animated.
     */
    protected final PropertyAccessor.AngleAccessor propertyAccessor;
    /**
     * The angle the animation begins at.
     */
    protected Angle begin;
    /**
     * The angle the animation ends at.
     */
    protected Angle end;

    /**
     * Construct an AngleAnimator
     *
     * @param interpolator     the {@link Interpolator}
     * @param begin            angle the animation begins at
     * @param end              The angle the animation ends at.
     * @param propertyAccessor The {@link PropertyAccessor} used to modify the data value being
     *                         animated.
     */
    public AngleAnimator(Interpolator interpolator,
        Angle begin, Angle end,
        PropertyAccessor.AngleAccessor propertyAccessor) {
        super(interpolator);
        if (interpolator == null) {
            this.interpolator = new ScheduledInterpolator(10000);
        }
        if (begin == null || end == null) {
            String message = Logging.getMessage("nullValue.AngleIsNull");
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

    /**
     * Get the current {@link #begin} value.
     *
     * @return the current {@link #begin} value.
     */
    public Angle getBegin() {
        return this.begin;
    }

    /**
     * Set the {@link #begin} value.
     *
     * @param begin the new {@link #begin} value.
     */
    public void setBegin(Angle begin) {
        this.begin = begin;
    }

    /**
     * Get the current {@link #end} value.
     *
     * @return the current {@link #end} value.
     */
    public Angle getEnd() {
        return this.end;
    }

    /**
     * Set the {@link #end} value.
     *
     * @param end the new {@link #end} value.
     */
    public void setEnd(Angle end) {
        this.end = end;
    }

    /**
     * Get the {@link PropertyAccessor} in use by this animation
     *
     * @return the {@link PropertyAccessor} in use by this animation
     */
    public PropertyAccessor.AngleAccessor getPropertyAccessor() {
        return this.propertyAccessor;
    }

    /**
     * Set the value being animated via the {@link PropertyAccessor} using the passed
     * interpolant. This implementation just does a straight liner interpolation between the {@link #begin} and {@link
     * #end} values.
     *
     * @param interpolant the interpolant used to generate the next value that will be set by the {@link
     *                    PropertyAccessor}
     */
    protected void setImpl(double interpolant) {
        Angle newValue = this.nextAngle(interpolant);
        if (newValue == null)
            return;
        boolean success = this.propertyAccessor.setAngle(newValue);
        if (!success) {
            flagLastStateInvalid();
        }
        if (interpolant >= 1)
            this.stop();
    }

    @SuppressWarnings("UnusedDeclaration")
    private Angle nextAngle(double interpolant) {

        return Angle.mix(
            interpolant,
            this.begin,
            this.end);
    }
}
