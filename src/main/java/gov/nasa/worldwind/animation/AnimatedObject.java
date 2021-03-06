package gov.nasa.worldwind.animation;

import gov.nasa.worldwind.Keys;
import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.globes.Globe;
import gov.nasa.worldwind.render.Path;
import gov.nasa.worldwind.util.PropertyAccessor;
import gov.nasa.worldwind.util.measure.LengthMeasurer;

import java.util.Iterator;

public class AnimatedObject {

    private final Animatable object;
    private final AnimationController animationController;
    private Path route;
    private double velocity;
    private Iterator<? extends Position> positionIterator;
    private Position destination;

    public AnimatedObject(Animatable object) {
        this.animationController = new AnimationController();
        this.object = object;
    }

    public Animatable getObject() {
        return this.object;
    }

    public Path getRoute() {
        return this.route;
    }

    public void setRoute(Path route) {
        this.route = route;
        this.positionIterator = null;
    }

    public double getVelocity() {
        return this.velocity;
    }

    public void setVelocity(double velocity) {
        this.velocity = velocity;
    }

    protected Position getNextPosition() {
        if (this.positionIterator == null || !this.positionIterator.hasNext()) {
            this.positionIterator = this.route.getPositions().iterator();
        }
        if (this.positionIterator.hasNext()) {
            return this.positionIterator.next();
        }

        return null;
    }

    protected void createNextLeg(Globe globe) {
        this.animationController.clear();
        Position curPos = this.object.getPosition();
        this.destination = this.getNextPosition();
        MeasurableLength measurer = new LengthMeasurer(new Position[] {curPos, destination});
        long travelTime = (long) ((measurer.getLength(globe) / this.velocity) * 1000);
        ObjectPropertyAccessor accessor = new ObjectPropertyAccessor();
        Animator posAnimator = new PositionAnimator(new ScheduledInterpolator(travelTime), curPos, destination,
            accessor);
        this.animationController.put(posAnimator, posAnimator);
        Angle startHeading = object.getHeading();
        Angle endHeading = LatLon.greatCircleAzimuth(destination, curPos);
        Animator headingAnimator = new AngleAnimator(new ScheduledInterpolator(1000), startHeading, endHeading,
            accessor);
        this.animationController.put(headingAnimator, headingAnimator);
    }

    public void startAnimation(Globe globe) {
        Position p1 = this.getNextPosition();
        this.object.setPosition(p1);
        createNextLeg(globe);
    }

    public void stepAnimation(Globe globe) {
        if (!this.animationController.stepAnimators()) {
            createNextLeg(globe);
        }
    }

    public String getMetadata() {
        return this.object.getField(Keys.ANIMATION_META_DATA).toString();
    }

    public void setMetadata(String data) {
        this.object.setField(Keys.ANIMATION_META_DATA, data);
    }

    private class ObjectPropertyAccessor implements PropertyAccessor.PositionAccessor, PropertyAccessor.AngleAccessor {

        @Override
        public Position getPosition() {
            return object.getPosition();
        }

        @Override
        public boolean setPosition(Position value) {
            object.setPosition(value);
            return true;
        }

        @Override
        public Angle getAngle() {
            return object.getHeading();
        }

        @Override
        public boolean setAngle(Angle value) {
            object.setHeading(value);
            return true;
        }
    }
}