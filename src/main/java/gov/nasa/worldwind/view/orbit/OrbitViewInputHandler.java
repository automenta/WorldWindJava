/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.view.orbit;

import gov.nasa.worldwind.*;
import gov.nasa.worldwind.animation.*;
import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.globes.*;
import gov.nasa.worldwind.util.*;
import gov.nasa.worldwind.video.awt.*;
import gov.nasa.worldwind.view.*;

import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.util.Date;
import java.util.logging.Level;

/**
 * @author dcollins
 * @version $Id: OrbitViewInputHandler.java 2253 2014-08-22 16:33:46Z dcollins $
 */
public class OrbitViewInputHandler extends BasicViewInputHandler {
    public static final String ORBITVIEW_RESET_ROLL = "gov.nasa.worldwind.ViewResetRoll";
    protected static final String VIEW_ANIM_HEADING = "ViewAnimHeading";
    protected static final String VIEW_ANIM_PITCH = "ViewAnimPitch";
    protected static final String VIEW_ANIM_ROLL = "ViewAnimRoll";
    protected static final String VIEW_ANIM_HEADING_PITCH = "ViewAnimHeadingPitch";
    protected static final String VIEW_ANIM_POSITION = "ViewAnimPosition";
    protected static final String VIEW_ANIM_CENTER = "ViewAnimCenter";
    protected static final String VIEW_ANIM_ZOOM = "ViewAnimZoom";
    protected static final String VIEW_ANIM_PAN = "ViewAnimPan";
    protected static final String VIEW_ANIM_APP = "ViewAnimApp";
    protected static final String VIEW_ANIM_EYE = "ViewAnimEye";
    protected final AnimationController gotoAnimControl = new AnimationController();
    protected final AnimationController uiAnimControl = new AnimationController();

    /**
     * Create a new input handler.
     */
    public OrbitViewInputHandler() {
        this.initializeInputHandlers();
    }

    protected static Position computeNewPosition(OrbitView view, Position position) {
        Angle newLat = new Angle(WWMath.clamp(position.lat, -90, 90));
        Angle newLon = Angle.lonNorm(position.getLon());
        return view.getOrbitViewLimits().limitCenterPosition(view, new Position(newLat, newLon, position.elevation));
    }

    protected static Angle computeNewHeading(OrbitView view, Angle heading) {
        return view.getOrbitViewLimits().limitHeading(view, BasicOrbitView.normalizedHeading(heading));
    }

    protected static Angle computeNewPitch(OrbitView view, Angle pitch) {
        return view.getOrbitViewLimits().limitPitch(view, BasicOrbitView.normalizedPitch(pitch));
    }

    //**************************************************************//
    //********************  View Change Events  ********************//
    //**************************************************************//

    protected static double computeNewZoom(OrbitView view, double curZoom, double change) {
        return view.getOrbitViewLimits().limitZoom(view,
            Math.exp((curZoom != 0 ? Math.log(curZoom) : 0) + change));
    }

    /**
     * Initialize input handlers specific to ObitView.
     */
    protected void initializeInputHandlers() {
        // OrbitView allows application controllers to set the view's roll, but it does not provide user controls to
        // change the roll. Add an input handler that will reset the roll to zero when the user clicks the mouse so that
        // the user can easily get back to normal roll state.

        // Reset roll on mouse click
        ViewInputAttributes.ActionAttributes.MouseAction[] resetRollMouseEvents =
            {
                new ViewInputAttributes.ActionAttributes.MouseAction(MouseEvent.BUTTON1_DOWN_MASK)
            };

        // Set up the input attributes for reset roll
        this.getAttributes().setMouseActionAttributes(
            OrbitViewInputHandler.ORBITVIEW_RESET_ROLL, // Action to map to mouse button
            0, // Modifiers, none in this case
            ViewInputAttributes.ActionAttributes.ActionTrigger.ON_PRESS, // The event that triggers the action
            resetRollMouseEvents, // Input actions to map to the behavior
            ViewInputAttributes.DEFAULT_KEY_ROLL_MIN_VALUE,
            ViewInputAttributes.DEFAULT_KEY_ROLL_MAX_VALUE,
            false, // Disable smoothing
            0.0); // Smoothing value

        // Add the action listener
        ViewInputAttributes.ActionAttributes actionAttrs =
            this.getAttributes().getActionMap(ViewInputAttributes.DEVICE_MOUSE).getActionAttributes(
                OrbitViewInputHandler.ORBITVIEW_RESET_ROLL);
        actionAttrs.setMouseActionListener(new ResetRollActionListener());
    }

    protected boolean isNonContinous2DGlobe() {
        Globe globe = this.wwd().model().globe();
        return globe instanceof Globe2D && !((Globe2D) globe).isContinuous();
    }

    protected void onMoveTo(Position focalPosition, ViewInputAttributes.DeviceAttributes deviceAttributes,
        ViewInputAttributes.ActionAttributes actionAttribs) {
        this.stopAllAnimators();

        View view = this.getView();
        if (view == null) // include this test to ensure any derived implementation performs it
        {
            return;
        }

        if (this.isNonContinous2DGlobe()) {
            this.onMoveTo2D(focalPosition, deviceAttributes, actionAttribs);
            return;
        }

        if (view instanceof OrbitView) {
            // We're treating a speed parameter as smoothing here. A greater speed results in greater smoothing and
            // slower response. Therefore the min speed used at lower altitudes ought to be *greater* than the max
            // speed used at higher altitudes.
            double smoothing = this.getScaleValueZoom(actionAttribs);
            if (!actionAttribs.isEnableSmoothing())
                smoothing = 0.0;

            Animator centerAnimator = new OrbitViewCenterAnimator((BasicOrbitView) this.getView(),
                view.getEyePosition(), focalPosition, smoothing,
                OrbitViewPropertyAccessor.createCenterPositionAccessor((OrbitView) view), true);
            this.gotoAnimControl.put(OrbitViewInputHandler.VIEW_ANIM_CENTER, centerAnimator);
            view.emit(Keys.VIEW, null, view);
        }
    }

    @SuppressWarnings("UnusedParameters")
    protected void onMoveTo2D(Position focalPosition, ViewInputAttributes.DeviceAttributes deviceAttributes,
        ViewInputAttributes.ActionAttributes actionAttribs) {
        View view = this.getView();
        if (!(view instanceof BasicOrbitView)) {
            return;
        }

        Globe globe = this.wwd().model().globe();
        BasicOrbitView orbitView = (BasicOrbitView) view;
        Matrix modelview = OrbitViewInputSupport.computeTransformMatrix(globe, focalPosition, orbitView.getHeading(),
            orbitView.getPitch(), orbitView.getRoll(), orbitView.getZoom());
        Vec4 eyePoint = modelview.extractEyePoint();

        double smoothing = actionAttribs.isEnableSmoothing() ? this.getScaleValueZoom(actionAttribs) : 0;
        this.gotoAnimControl.put(
            OrbitViewInputHandler.VIEW_ANIM_EYE, new OrbitViewEyePointAnimator(globe, orbitView, eyePoint, smoothing));
        view.emit(Keys.VIEW, null, view);
    }

    protected void onHorizontalTranslateAbs(Angle latitudeChange, Angle longitudeChange,
        ViewInputAttributes.ActionAttributes actionAttribs) {
        this.stopGoToAnimators();
        this.stopUserInputAnimators(OrbitViewInputHandler.VIEW_ANIM_HEADING, OrbitViewInputHandler.VIEW_ANIM_PITCH, OrbitViewInputHandler.VIEW_ANIM_ZOOM,
            OrbitViewInputHandler.VIEW_ANIM_EYE);

        View view = this.getView();
        if (view == null) // include this test to ensure any derived implementation performs it
        {
            return;
        }

        if (latitudeChange.equals(Angle.ZERO) && longitudeChange.equals(Angle.ZERO)) {
            return;
        }

        if (view instanceof OrbitView) {
            double latDegrees = latitudeChange.degrees;
            double lonDegrees = longitudeChange.degrees;

            Position centerPos = ((OrbitView) view).getCenterPosition();
            latDegrees = WWMath.clamp(centerPos.lat + latDegrees, -90, 90);
            lonDegrees = Angle.normalizedDegreesLongitude(centerPos.lon + lonDegrees);
            centerPos = Position.fromDegrees(latDegrees, lonDegrees, centerPos.elevation);
            this.setCenterPosition((BasicOrbitView) view, uiAnimControl, centerPos, actionAttribs);
        }
    }

    protected void onHorizontalTranslateRel(double forwardInput, double sideInput,
        double totalForwardInput, double totalSideInput,
        ViewInputAttributes.DeviceAttributes deviceAttributes,
        ViewInputAttributes.ActionAttributes actionAttributes) {
        if (this.isNonContinous2DGlobe()) {
            this.onHorizontalTranslate2D(forwardInput, sideInput, totalForwardInput, totalSideInput, deviceAttributes,
                actionAttributes);
            return;
        }

        this.stopGoToAnimators();
        this.stopUserInputAnimators(OrbitViewInputHandler.VIEW_ANIM_HEADING, OrbitViewInputHandler.VIEW_ANIM_PITCH, OrbitViewInputHandler.VIEW_ANIM_ZOOM,
            OrbitViewInputHandler.VIEW_ANIM_EYE);

        if (actionAttributes.getMouseActions() != null) {
            // Normalize the forward and right magnitudes.
            double length = Math.sqrt(forwardInput * forwardInput + sideInput * sideInput);
            if (length > 0.0) {
                forwardInput /= length;
                sideInput /= length;
            }

            Point point = AbstractViewInputHandler.constrainToSourceBounds(getMousePoint(), wwd());
            Point lastPoint = AbstractViewInputHandler.constrainToSourceBounds(getLastMousePoint(), wwd());
            if (getSelectedPosition() == null) {
                // Compute the current selected position if none exists. This happens if the user starts dragging when
                // the cursor is off the globe, then drags the cursor onto the globe.
                setSelectedPosition(computeSelectedPosition());
            } else if (computeSelectedPosition() == null) {
                // User dragged the cursor off the globe. Clear the selected position to ensure a new one will be
                // computed if the user drags the cursor back to the globe.
                setSelectedPosition(null);
            } else if (computeSelectedPointAt(point) == null || computeSelectedPointAt(lastPoint) == null) {
                // User selected a position that is won't work for dragging. Probably the selected elevation is above the
                // eye elevation, in which case dragging becomes unpredictable. Clear the selected position to ensure
                // a new one will be computed if the user drags the cursor to a valid position.
                setSelectedPosition(null);
            }

            Vec4 vec = computeSelectedPointAt(point);
            Vec4 lastVec = computeSelectedPointAt(lastPoint);

            // Cursor is on the globe, pan between the two positions.
            if (vec != null && lastVec != null) {

                // Compute the change in view location given two screen points and corresponding world vectors.
                LatLon latlon = getChangeInLocation(lastPoint, point, lastVec, vec);
                onHorizontalTranslateAbs(latlon.getLat(), latlon.getLon(), actionAttributes);
                return;
            }

            Point movement = ViewUtil.subtract(point, lastPoint);
            forwardInput = movement.y;
            sideInput = -movement.x;
        }

        // Cursor is off the globe, we potentially want to simulate globe dragging.
        // or this is a keyboard event.
        Angle forwardChange = new Angle(forwardInput * getScaleValueHorizTransRel(deviceAttributes, actionAttributes));
        Angle sideChange = new Angle(sideInput * getScaleValueHorizTransRel(deviceAttributes, actionAttributes));
        onHorizontalTranslateRel(forwardChange, sideChange, actionAttributes);
    }

    protected void onHorizontalTranslateRel(Angle forwardChange, Angle sideChange,
        ViewInputAttributes.ActionAttributes actionAttribs) {
        View view = this.getView();
        if (view == null) // include this test to ensure any derived implementation performs it
        {
            return;
        }

        if (forwardChange.equals(Angle.ZERO) && sideChange.equals(Angle.ZERO)) {
            return;
        }

        if (view instanceof OrbitView) {
            double sinHeading = view.getHeading().sin();
            double cosHeading = view.getHeading().cos();
            double latDegrees = cosHeading * forwardChange.degrees - sinHeading * sideChange.degrees;
            double lonDegrees = sinHeading * forwardChange.degrees + cosHeading * sideChange.degrees;

            Position centerPos = ((OrbitView) view).getCenterPosition();
            latDegrees = WWMath.clamp(centerPos.lat + latDegrees, -90, 90);
            lonDegrees = Angle.normalizedDegreesLongitude(centerPos.lon + lonDegrees);
            centerPos = Position.fromDegrees(latDegrees, lonDegrees, centerPos.elevation);
            this.setCenterPosition((BasicOrbitView) view, this.uiAnimControl, centerPos, actionAttribs);
        }
    }

    @SuppressWarnings("UnusedParameters")
    protected void onHorizontalTranslate2D(double forwardInput, double sideInput,
        double totalForwardInput, double totalSideInput,
        ViewInputAttributes.DeviceAttributes deviceAttributes,
        ViewInputAttributes.ActionAttributes actionAttributes) {
        View view = this.getView();
        if (!(view instanceof BasicOrbitView)) {
            return;
        }

        this.stopAllAnimators();

        if (actionAttributes.getMouseActions() != null) {
            // Compute the model coordinate rays corresponding to the mouse down point and the current mouse point.
            View orbitView = this.getView();
            Point p1 = AbstractViewInputHandler.constrainToSourceBounds(this.getMouseDownPoint(), this.wwd());
            Point p2 = AbstractViewInputHandler.constrainToSourceBounds(this.getMousePoint(), this.wwd());
            Line ray1 = ViewUtil.computeRayFromScreenPoint(orbitView, p1.x, p1.y, this.mouseDownModelview,
                this.mouseDownProjection, this.mouseDownViewport);
            Line ray2 = ViewUtil.computeRayFromScreenPoint(orbitView, p2.x, p2.y, this.mouseDownModelview,
                this.mouseDownProjection, this.mouseDownViewport);

            // Compute a model coordinate plane passing through the position under the cursor when the mouse button was
            // pressed. Fall back to a plane normal to the globe if the cursor was off the globe.
            Globe globe = this.wwd().model().globe();
            Position pos = this.getSelectedPosition();
            Vec4 point = pos != null ? globe.computePointFromPosition(pos) : new Vec4(0, 0, 0);
            Vec4 normal = globe.computeSurfaceNormalAtPoint(point);
            Plane plane = new Plane(normal.x, normal.y, normal.z, -normal.dot3(point));

            // Intersect the model coordinate plane with the two model coordinate rays. The difference is the
            // translation in model coordinates.
            Vec4 point1 = plane.intersect(ray1);
            Vec4 point2 = plane.intersect(ray2);
            Vec4 translation = point2.subtract3(point1);

            // Apply the translation vector to the eye point.
            Matrix modelview = this.mouseDownModelview.multiply(Matrix.fromTranslation(translation));
            Vec4 eyePoint = modelview.extractEyePoint();
            this.setEyePoint(eyePoint, actionAttributes);
        } else {
            // Convert the translation vector from a unitless direction to eye coordinates.
            Globe globe = this.wwd().model().globe();
            double degreesPerUnit = this.getScaleValueHorizTransRel(deviceAttributes, actionAttributes);
            double radiansPerUnit = degreesPerUnit * Math.PI / 180.0;
            double metersPerUnit = radiansPerUnit * globe.getRadius();
            Vec4 translation = new Vec4(-sideInput, -forwardInput, 0);
            translation = translation.multiply3(metersPerUnit);

            // Convert the translation vector from eye coordinates to model coordinates in order to match the view's
            // current orientation relative to the model.
            BasicOrbitView orbitView = (BasicOrbitView) this.getView();
            Matrix matrix = ViewUtil.computeTransformMatrix(globe, orbitView.getCenterPosition(),
                orbitView.getHeading(), Angle.ZERO, orbitView.getRoll());
            translation = translation.transformBy3(matrix.getInverse());

            // Apply the translation vector to the eye point.
            Matrix modelview = this.getView().getModelviewMatrix().multiply(Matrix.fromTranslation(translation));
            Vec4 eyePoint = modelview.extractEyePoint();
            this.setEyePoint(eyePoint, actionAttributes);
        }
    }

    @Override
    protected void onResetHeading(ViewInputAttributes.ActionAttributes actionAttribs) {
        this.stopAllAnimators();

        View view = this.getView();
        if (view == null) // include this test to ensure any derived implementation performs it
        {
            return;
        }
        this.addHeadingAnimator(view.getHeading(), Angle.ZERO);
    }

    /**
     * Called when user input causes the roll to reset.
     *
     * @param actionAttribs input that caused the change.
     */
    @SuppressWarnings("UnusedParameters")
    protected void onResetRoll(ViewInputAttributes.ActionAttributes actionAttribs) {
        View view = this.getView();
        if (view == null) // include this test to ensure any derived implementation performs it
        {
            return;
        }

        if (Angle.ZERO.equals(view.getRoll())) // Don't need to reset if roll is already zero
        {
            return;
        }

        this.addRollAnimator(view.getRoll(), Angle.ZERO);
    }

    @Override
    protected void onResetHeadingPitchRoll(ViewInputAttributes.ActionAttributes actionAttribs) {
        this.stopAllAnimators();

        View view = this.getView();
        if (view == null) // include this test to ensure any derived implementation performs it
        {
            return;
        }

        this.addHeadingPitchRollAnimator(view.getHeading(), Angle.ZERO, view.getPitch(), Angle.ZERO, view.getRoll(),
            Angle.ZERO);
    }

    protected void onRotateView(double headingInput, double pitchInput,
        double totalHeadingInput, double totalPitchInput,
        ViewInputAttributes.DeviceAttributes deviceAttributes,
        ViewInputAttributes.ActionAttributes actionAttributes) {
        this.stopGoToAnimators();
        this.stopUserInputAnimators(OrbitViewInputHandler.VIEW_ANIM_CENTER, OrbitViewInputHandler.VIEW_ANIM_ZOOM, OrbitViewInputHandler.VIEW_ANIM_EYE);

        if (actionAttributes.getMouseActions() != null) {
            // Switch the direction of heading change depending on whether the cursor is above or below
            // the center of the screen.
            if (wwd() instanceof Component) {
                if (getMousePoint().y < ((Component) wwd()).getHeight() / 2) {
                    headingInput = -headingInput;
                }
            }
        } else {
            double length = Math.sqrt(headingInput * headingInput + pitchInput * pitchInput);
            if (length > 0.0) {
                headingInput /= length;
                pitchInput /= length;
            }
        }

        Angle headingChange = new Angle(headingInput * getScaleValueRotate(actionAttributes));
        Angle pitchChange = new Angle(pitchInput * getScaleValueRotate(actionAttributes));

        onRotateView(headingChange, pitchChange, actionAttributes);
    }

    //**************************************************************//
    //********************                    **********************//
    //**************************************************************//

    protected void onRotateView(Angle headingChange, Angle pitchChange,
        ViewInputAttributes.ActionAttributes actionAttribs) {
        View view = this.getView();
        if (view == null) // include this test to ensure any derived implementation performs it
        {
            return;
        }

        if (view instanceof BasicOrbitView) {
            if (!headingChange.equals(Angle.ZERO))
                this.changeHeading((BasicOrbitView) view, uiAnimControl, headingChange, actionAttribs);

            if (!pitchChange.equals(Angle.ZERO))
                this.changePitch((BasicOrbitView) view, uiAnimControl, pitchChange, actionAttribs);
        }
    }

    //**************************************************************//
    //********************  Property Change Events  ****************//
    //**************************************************************//

    protected void onVerticalTranslate(double translateChange, double totalTranslateChange,
        ViewInputAttributes.DeviceAttributes deviceAttributes,
        ViewInputAttributes.ActionAttributes actionAttributes) {
        this.stopGoToAnimators();
        this.stopUserInputAnimators(OrbitViewInputHandler.VIEW_ANIM_CENTER, OrbitViewInputHandler.VIEW_ANIM_HEADING, OrbitViewInputHandler.VIEW_ANIM_PITCH,
            OrbitViewInputHandler.VIEW_ANIM_EYE);

        double zoomChange = translateChange * getScaleValueRotate(actionAttributes);
        onVerticalTranslate(zoomChange, actionAttributes);
    }

    protected void onVerticalTranslate(double translateChange, ViewInputAttributes.ActionAttributes actionAttribs) {
        View view = this.getView();
        if (view == null) // include this test to ensure any derived implementation performs it
        {
            return;
        }

        if (translateChange == 0) {
            return;
        }
        if (view instanceof BasicOrbitView) {
            this.changeZoom((BasicOrbitView) view, uiAnimControl, translateChange, actionAttribs);
        }
    }

    /**
     * Apply the changes prior to rendering a frame. The method will step animators, applying the results of those steps
     * to the View, then if a focus on terrain is required, it will do that as well.
     **/
    @Override
    public void apply() {
        super.apply();

        View view = this.getView();
        if (view == null) {
            return;
        }

        if (this.gotoAnimControl.stepAnimators()) {
            view.emit(Keys.VIEW, null, view);
        } else {
            this.gotoAnimControl.clear();
        }

        if (this.uiAnimControl.stepAnimators()) {
            view.emit(Keys.VIEW, null, view);
        } else {
            this.uiAnimControl.clear();
        }
    }

    protected void handlePropertyChange(PropertyChangeEvent e) {
        super.handlePropertyChange(e);

        //noinspection StringEquality
        if (e.getPropertyName() == OrbitView.CENTER_STOPPED) {
            this.handleOrbitViewCenterStopped();
        }
    }

    protected void stopAllAnimators() {
        // Explicitly stop all animators, then clear the data structure which holds them. If we remove an animator
        // from this data structure without invoking stop(), the animator has no way of knowing it was forcibly stopped.
        // An animator's owner - potentially an object other than this ViewInputHandler - may need to know if an
        // animator has been forcibly stopped in order to react correctly to that event.
        this.uiAnimControl.stopAnimations();
        this.gotoAnimControl.stopAnimations();
        this.uiAnimControl.clear();
        this.gotoAnimControl.clear();

        View view = this.getView();
        if (view == null)
            return;

        if (view instanceof BasicOrbitView) {
            ((BasicOrbitView) view).setViewOutOfFocus(true);
        }
    }

    protected void stopGoToAnimators() {
        // Explicitly stop all 'go to' animators, then clear the data structure which holds them. If we remove an
        // animator from this data structure without invoking stop(), the animator has no way of knowing it was forcibly
        // stopped. An animator's owner - likely an application object other - may need to know if an animator has been
        // forcibly stopped in order to react correctly to that event.
        this.gotoAnimControl.stopAnimations();
        this.gotoAnimControl.clear();
    }

    //**************************************************************//
    //********************  View State Change Utilities  ***********//
    //**************************************************************//

    protected void stopUserInputAnimators(Object... names) {
        for (Object o : names) {
            if (this.uiAnimControl.get(o) != null) {
                // Explicitly stop the 'ui' animator, then clear it from the data structure which holds it. If we remove
                // an animator from this data structure without invoking stop(), the animator has no way of knowing it
                // was forcibly stopped. Though applications cannot access the 'ui' animator data structure, stopping
                // the animators here is the correct action.
                this.uiAnimControl.get(o).stop();
                this.uiAnimControl.remove(o);
            }
        }
    }

    protected void handleViewStopped() {
        this.stopAllAnimators();
    }

    protected void handleOrbitViewCenterStopped() {
        // The "center stopped" message instructs components to stop modifying the OrbitView's center position.
        // Therefore we stop any center position animations started by this view controller.
        this.stopUserInputAnimators(OrbitViewInputHandler.VIEW_ANIM_CENTER, OrbitViewInputHandler.VIEW_ANIM_EYE);
    }

    protected void setEyePoint(Vec4 eyePoint, ViewInputAttributes.ActionAttributes attrib) {
        Globe globe = this.wwd().model().globe();
        BasicOrbitView view = (BasicOrbitView) this.getView();

        double smoothing = (this.isEnableSmoothing() && attrib.isEnableSmoothing()) ? attrib.getSmoothingValue() : 0;
        if (smoothing == 0) {
            OrbitViewEyePointAnimator.setEyePoint(globe, view, eyePoint);
        } else {
            this.uiAnimControl.put(
                OrbitViewInputHandler.VIEW_ANIM_EYE, new OrbitViewEyePointAnimator(globe, view, eyePoint, smoothing));
        }
        this.getView().emit(Keys.VIEW, null, this.getView());
    }

    protected void setCenterPosition(BasicOrbitView view,
        AnimationController animControl,
        Position position, ViewInputAttributes.ActionAttributes attrib) {
        double smoothing = attrib.getSmoothingValue();
        if (!(attrib.isEnableSmoothing() && this.isEnableSmoothing()))
            smoothing = 0.0;

        if (smoothing == 0) {
            if (animControl.get(OrbitViewInputHandler.VIEW_ANIM_CENTER) != null)
                animControl.remove(OrbitViewInputHandler.VIEW_ANIM_CENTER);
            Position newPosition = view.getOrbitViewLimits().limitCenterPosition(view, position);
            view.setCenterPosition(newPosition);
            view.setViewOutOfFocus(true);
        } else {
            OrbitViewCenterAnimator centerAnimator = (OrbitViewCenterAnimator) animControl.get(
                OrbitViewInputHandler.VIEW_ANIM_CENTER);
            Position cur = view.getCenterPosition();

            if (centerAnimator == null || !centerAnimator.hasNext()) {
                Position newPosition = OrbitViewInputHandler.computeNewPosition(view, position);
                centerAnimator = new OrbitViewCenterAnimator((BasicOrbitView) this.getView(),
                    cur, newPosition, smoothing,
                    OrbitViewPropertyAccessor.createCenterPositionAccessor(view), true);
                animControl.put(OrbitViewInputHandler.VIEW_ANIM_CENTER, centerAnimator);
            } else {
                Position newPosition = new Position(
                    centerAnimator.getEnd().getLat().add(
                        position.getLat()).sub(cur.getLat()),
                    centerAnimator.getEnd().getLon().add(
                        position.getLon()).sub(cur.getLon()),
                    centerAnimator.getEnd().getElevation() +
                        position.getElevation() - cur.getElevation());
                newPosition = OrbitViewInputHandler.computeNewPosition(view, newPosition);
                centerAnimator.setEnd(newPosition);
            }

            centerAnimator.start();
        }
        view.emit(Keys.VIEW, null, view);
    }

    protected void changeHeading(BasicOrbitView view,
        AnimationController animControl,
        Angle change, ViewInputAttributes.ActionAttributes attrib) {
        view.computeAndSetViewCenterIfNeeded();

        double smoothing = attrib.getSmoothingValue();
        if (!(attrib.isEnableSmoothing() && this.isEnableSmoothing()))
            smoothing = 0.0;

        if (smoothing == 0) {
            if (animControl.get(OrbitViewInputHandler.VIEW_ANIM_HEADING) != null)
                animControl.remove(OrbitViewInputHandler.VIEW_ANIM_HEADING);
            Angle newHeading = OrbitViewInputHandler.computeNewHeading(view, view.getHeading().add(change));
            view.setHeading(newHeading);
        } else {
            RotateToAngleAnimator angleAnimator = (RotateToAngleAnimator)
                animControl.get(OrbitViewInputHandler.VIEW_ANIM_HEADING);

            if (angleAnimator == null || !angleAnimator.hasNext()) {
                Angle newHeading = OrbitViewInputHandler.computeNewHeading(view, view.getHeading().add(change));
                angleAnimator = new RotateToAngleAnimator(
                    view.getHeading(), newHeading, smoothing,
                    ViewPropertyAccessor.createHeadingAccessor(view));
                animControl.put(OrbitViewInputHandler.VIEW_ANIM_HEADING, angleAnimator);
            } else {
                Angle newHeading = OrbitViewInputHandler.computeNewHeading(view, angleAnimator.getEnd().add(change));
                angleAnimator.setEnd(newHeading);
            }

            angleAnimator.start();
        }

        view.emit(Keys.VIEW, null, view);
    }

    protected void changePitch(BasicOrbitView view,
        AnimationController animControl,
        Angle change, ViewInputAttributes.ActionAttributes attrib) {
        view.computeAndSetViewCenterIfNeeded();

        double smoothing = attrib.getSmoothingValue();
        if (!(attrib.isEnableSmoothing() && this.isEnableSmoothing()))
            smoothing = 0.0;

        if (smoothing == 0.0) {
            if (animControl.get(OrbitViewInputHandler.VIEW_ANIM_PITCH) != null)
                animControl.remove(OrbitViewInputHandler.VIEW_ANIM_PITCH);
            Angle newPitch = OrbitViewInputHandler.computeNewPitch(view, view.getPitch().add(change));
            view.setPitch(newPitch);
        } else {
            RotateToAngleAnimator angleAnimator = (RotateToAngleAnimator) animControl.get(
                OrbitViewInputHandler.VIEW_ANIM_PITCH);

            if (angleAnimator == null || !angleAnimator.hasNext()) {
                // Create an angle animator which tilts the view to the specified new pitch. If this changes causes the
                // view to collide with the surface, this animator is set to stop. We enable this behavior by using a
                // {@link #CollisionAwarePitchAccessor} angle accessor and setting the animator's stopOnInvalidState
                // property to 'true'.
                Angle newPitch = OrbitViewInputHandler.computeNewPitch(view, view.getPitch().add(change));
                angleAnimator = new RotateToAngleAnimator(
                    view.getPitch(), newPitch, smoothing,
                    new CollisionAwarePitchAccessor(view));
                angleAnimator.setStopOnInvalidState(true);
                animControl.put(OrbitViewInputHandler.VIEW_ANIM_PITCH, angleAnimator);
            } else {
                Angle newPitch = OrbitViewInputHandler.computeNewPitch(view, angleAnimator.getEnd().add(change));
                angleAnimator.setEnd(newPitch);
            }

            angleAnimator.start();
        }

        view.emit(Keys.VIEW, null, view);
    }

    protected void changeZoom(BasicOrbitView view,
        AnimationController animControl,
        double change, ViewInputAttributes.ActionAttributes attrib) {
        view.computeAndSetViewCenterIfNeeded();

        double smoothing = attrib.getSmoothingValue();
        if (!(attrib.isEnableSmoothing() && this.isEnableSmoothing()))
            smoothing = 0.0;

        if (smoothing == 0.0) {
            if (animControl.get(OrbitViewInputHandler.VIEW_ANIM_ZOOM) != null)
                animControl.remove(OrbitViewInputHandler.VIEW_ANIM_ZOOM);
            view.setZoom(OrbitViewInputHandler.computeNewZoom(view, view.getZoom(), change));
        } else {
            double newZoom;
            OrbitViewMoveToZoomAnimator zoomAnimator = (OrbitViewMoveToZoomAnimator) animControl.get(
                OrbitViewInputHandler.VIEW_ANIM_ZOOM);

            if (zoomAnimator == null || !zoomAnimator.hasNext()) {
                newZoom = OrbitViewInputHandler.computeNewZoom(view, view.getZoom(), change);
                zoomAnimator = new OrbitViewMoveToZoomAnimator(view, newZoom, smoothing,
                    OrbitViewPropertyAccessor.createZoomAccessor(view), false);
                animControl.put(OrbitViewInputHandler.VIEW_ANIM_ZOOM, zoomAnimator);
            } else {
                newZoom = OrbitViewInputHandler.computeNewZoom(view, zoomAnimator.getEnd(), change);
                zoomAnimator.setEnd(newZoom);
            }

            zoomAnimator.start();
        }
        view.emit(Keys.VIEW, null, view);
    }

    protected double getScaleValueHorizTransRel(
        ViewInputAttributes.DeviceAttributes deviceAttributes, ViewInputAttributes.ActionAttributes actionAttributes) {

        View view = this.getView();
        if (view == null) {
            return 0.0;
        }
        if (view instanceof OrbitView) {
            double[] range = actionAttributes.getValues();
            // If this is an OrbitView, we use the zoom value to set the scale
            double radius = this.wwd().model().globe().getRadius();
            return (AbstractViewInputHandler.getScaleValue(range[0], range[1],
                ((OrbitView) view).getZoom(), 3.0 * radius, true));
        } else {
            // Any other view, use the base class scaling method
            return (super.getScaleValueElevation(deviceAttributes, actionAttributes));
        }
    }

    //**************************************************************//
    //********************  Input Handler Property Accessors  ******//
    //**************************************************************//

    protected double getScaleValueRotate(
        ViewInputAttributes.ActionAttributes actionAttributes) {

        View view = this.getView();
        if (view == null) {
            return 0.0;
        }
        if (view instanceof OrbitView) {
            double[] range = actionAttributes.getValues();
            // If this is an OrbitView, we use the zoom value to set the scale
            double radius = this.wwd().model().globe().getRadius();
            return (AbstractViewInputHandler.getScaleValue(range[0], range[1],
                ((OrbitView) view).getZoom(), 3.0 * radius, false));
        }
        return (1.0);
    }

    //**************************************************************//
    //********************  Scaling Utilities  *********************//
    //**************************************************************//

    protected double getScaleValueZoom(ViewInputAttributes.ActionAttributes actionAttributes) {
        View view = this.getView();
        if (view == null) {
            return 0.0;
        }
        if (view instanceof OrbitView) {
            double[] range = actionAttributes.getValues();
            // If this is an OrbitView, we use the zoom value to set the scale
            double radius = this.wwd().model().globe().getRadius();
            double t = ((OrbitView) view).getZoom() / (3.0 * radius);
            t = (t < 0 ? 0 : (t > 1 ? 1 : t));
            return range[0] * (1.0 - t) + range[1] * t;
        }
        return (1.0);
    }

    public void addPanToAnimator(Position beginCenterPos, Position endCenterPos,
        Angle beginHeading, Angle endHeading,
        Angle beginPitch, Angle endPitch,
        double beginZoom, double endZoom, long timeToMove, boolean endCenterOnSurface) {
        int altitudeMode = endCenterOnSurface ? WorldWind.CLAMP_TO_GROUND : WorldWind.ABSOLUTE;

        OrbitView orbitView = (OrbitView) this.getView();
        FlyToOrbitViewAnimator panAnimator = FlyToOrbitViewAnimator.createFlyToOrbitViewAnimator(orbitView,
            beginCenterPos, endCenterPos, beginHeading, endHeading, beginPitch, endPitch,
            beginZoom, endZoom, timeToMove, altitudeMode);

        this.gotoAnimControl.put(OrbitViewInputHandler.VIEW_ANIM_PAN, panAnimator);
        this.getView().emit(Keys.VIEW, null, this.getView());
    }

    public void addPanToAnimator(Position beginCenterPos, Position endCenterPos,
        Angle beginHeading, Angle endHeading,
        Angle beginPitch, Angle endPitch,
        double beginZoom, double endZoom, boolean endCenterOnSurface, long MIN_LENGTH_MILLIS, long MAX_LENGTH_MILLIS) {
        int altitudeMode = endCenterOnSurface ? WorldWind.CLAMP_TO_GROUND : WorldWind.ABSOLUTE;

        // TODO: scale on mid-altitude?

        long timeToMove = AnimationSupport.getScaledTimeMillisecs(
            beginCenterPos, endCenterPos,
            MIN_LENGTH_MILLIS, MAX_LENGTH_MILLIS);
        OrbitView orbitView = (OrbitView) this.getView();
        FlyToOrbitViewAnimator panAnimator = FlyToOrbitViewAnimator.createFlyToOrbitViewAnimator(orbitView,
            beginCenterPos, endCenterPos, beginHeading, endHeading, beginPitch, endPitch,
            beginZoom, endZoom, timeToMove, altitudeMode);

        this.gotoAnimControl.put(OrbitViewInputHandler.VIEW_ANIM_PAN, panAnimator);
        orbitView.emit(Keys.VIEW, null, orbitView);
    }

    public void addPanToAnimator(Position centerPos, Angle heading, Angle pitch, double zoom,
        long timeToMove, boolean endCenterOnSurface) {
        OrbitView view = (OrbitView) this.getView();
        addPanToAnimator(view.getCenterPosition(), centerPos,
            view.getHeading(), heading,
            view.getPitch(), pitch, view.getZoom(), zoom, timeToMove, endCenterOnSurface);
        this.getView().emit(Keys.VIEW, null, this.getView());
    }

    public void addPanToAnimator(Position centerPos, Angle heading, Angle pitch, double zoom,
        boolean endCenterOnSurface) {
        OrbitView view = (OrbitView) this.getView();
        addPanToAnimator(view.getCenterPosition(), centerPos,
            view.getHeading(), heading,
            view.getPitch(), pitch, view.getZoom(), zoom, endCenterOnSurface,
            1000,
            2000
        );
        this.getView().emit(Keys.VIEW, null, this.getView());
    }

    public void addPanToAnimator(Position centerPos, Angle heading, Angle pitch, double zoom) {
        addPanToAnimator(centerPos, heading, pitch, zoom, false);
    }

    public void addEyePositionAnimator(long timeToIterate, Position beginPosition, Position endPosition) {
        PositionAnimator eyePosAnimator = ViewUtil.createEyePositionAnimator(this.getView(),
            timeToIterate, beginPosition, endPosition);
        this.gotoAnimControl.put(OrbitViewInputHandler.VIEW_ANIM_POSITION, eyePosAnimator);
        this.getView().emit(Keys.VIEW, null, this.getView());
    }

    public void addHeadingAnimator(Angle begin, Angle end) {
        this.gotoAnimControl.remove(OrbitViewInputHandler.VIEW_ANIM_HEADING_PITCH);
        AngleAnimator headingAnimator = ViewUtil.createHeadingAnimator(this.getView(), begin, end);
        this.gotoAnimControl.put(OrbitViewInputHandler.VIEW_ANIM_HEADING, headingAnimator);
        this.getView().emit(Keys.VIEW, null, this.getView());
    }

    public void addPitchAnimator(Angle begin, Angle end) {
        this.gotoAnimControl.remove(OrbitViewInputHandler.VIEW_ANIM_HEADING_PITCH);
        AngleAnimator pitchAnimator = ViewUtil.createPitchAnimator(this.getView(), begin, end);
        this.gotoAnimControl.put(OrbitViewInputHandler.VIEW_ANIM_PITCH, pitchAnimator);
        this.getView().emit(Keys.VIEW, null, this.getView());
    }

    /**
     * Add an animator to animate roll.
     *
     * @param begin starting roll
     * @param end   final roll
     */
    public void addRollAnimator(Angle begin, Angle end) {
        this.gotoAnimControl.remove(OrbitViewInputHandler.VIEW_ANIM_ROLL);
        AngleAnimator rollAnimator = ViewUtil.createRollAnimator(this.getView(), begin, end);
        this.gotoAnimControl.put(OrbitViewInputHandler.VIEW_ANIM_ROLL, rollAnimator);
        this.getView().emit(Keys.VIEW, null, this.getView());
    }

    /**
     * Add an animator to animate heading, pitch, and roll.
     *
     * @param beginHeading starting heading
     * @param endHeading   final heading
     * @param beginPitch   starting pitch
     * @param endPitch     final pitch
     * @param beginRoll    starting roll
     * @param endRoll      final roll
     */
    public void addHeadingPitchRollAnimator(Angle beginHeading, Angle endHeading, Angle beginPitch, Angle endPitch,
        Angle beginRoll, Angle endRoll) {
        this.gotoAnimControl.remove(OrbitViewInputHandler.VIEW_ANIM_PITCH);
        this.gotoAnimControl.remove(OrbitViewInputHandler.VIEW_ANIM_HEADING);
        CompoundAnimator headingPitchAnimator = ViewUtil.createHeadingPitchRollAnimator(this.getView(),
            beginHeading, endHeading, beginPitch, endPitch, beginRoll, endRoll);
        this.gotoAnimControl.put(OrbitViewInputHandler.VIEW_ANIM_HEADING_PITCH, headingPitchAnimator);
        this.getView().emit(Keys.VIEW, null, this.getView());
    }

    public void addZoomAnimator(double zoomStart, double zoomEnd) {
        final long DEFAULT_LENGTH_MILLIS = 4000;
        Animator zoomAnimator = new DoubleAnimator(new ScheduledInterpolator(DEFAULT_LENGTH_MILLIS),
            zoomStart, zoomEnd, OrbitViewPropertyAccessor.createZoomAccessor(((OrbitView) this.getView())));
        this.gotoAnimControl.put(OrbitViewInputHandler.VIEW_ANIM_ZOOM, zoomAnimator);
        this.getView().emit(Keys.VIEW, null, this.getView());
    }

    public void addFlyToZoomAnimator(Angle heading, Angle pitch, double zoom) {
        if (heading == null || pitch == null) {
            String message = Logging.getMessage("nullValue.AngleIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        View view = this.getView();
        if (view instanceof OrbitView) {
            OrbitView orbitView = (OrbitView) view;
            Angle beginHeading = orbitView.getHeading();
            Angle beginPitch = orbitView.getPitch();
            double beginZoom = orbitView.getZoom();
            final long MIN_LENGTH_MILLIS = 1000;
            final long MAX_LENGTH_MILLIS = 8000;
            long lengthMillis = AnimationSupport.getScaledTimeMillisecs(
                beginZoom, zoom,
                MIN_LENGTH_MILLIS, MAX_LENGTH_MILLIS);
            Animator zoomAnimator = new DoubleAnimator(
                new ScheduledInterpolator(lengthMillis), beginZoom, zoom,
                OrbitViewPropertyAccessor.createZoomAccessor(orbitView));
            Animator headingAnimator = new AngleAnimator(new ScheduledInterpolator(lengthMillis),
                beginHeading, heading, ViewPropertyAccessor.createHeadingAccessor(orbitView));
            Animator pitchAnimator = new AngleAnimator(new ScheduledInterpolator(lengthMillis),
                beginPitch, pitch, ViewPropertyAccessor.createPitchAccessor(orbitView));

            this.gotoAnimControl.put(OrbitViewInputHandler.VIEW_ANIM_ZOOM, zoomAnimator);
            this.gotoAnimControl.put(OrbitViewInputHandler.VIEW_ANIM_HEADING, headingAnimator);
            this.gotoAnimControl.put(OrbitViewInputHandler.VIEW_ANIM_PITCH, pitchAnimator);
            orbitView.emit(Keys.VIEW, null, orbitView);
        }
    }

    public void addCenterAnimator(Position begin, Position end, boolean smoothed) {

        View view = this.getView();
        if (view instanceof OrbitView) {
            // TODO: length-scaling factory function
            final long DEFAULT_LENGTH_MILLIS = 4000;
            this.addCenterAnimator(begin, end, DEFAULT_LENGTH_MILLIS, smoothed);
        }
    }

    public void addCenterAnimator(Position begin, Position end, long lengthMillis, boolean smoothed) {

        View view = this.getView();
        if (view instanceof OrbitView) {
            OrbitView orbitView = (OrbitView) view;
            Interpolator interpolator;
            if (smoothed) {
                interpolator = new SmoothInterpolator(lengthMillis);
            } else {
                interpolator = new ScheduledInterpolator(lengthMillis);
            }
            Animator centerAnimator = new PositionAnimator(interpolator,
                begin, end, OrbitViewPropertyAccessor.createCenterPositionAccessor(orbitView));
            this.gotoAnimControl.put(OrbitViewInputHandler.VIEW_ANIM_CENTER, centerAnimator);
            orbitView.emit(Keys.VIEW, null, view);
        }
    }

    public void goTo(Position lookAtPos, double distance) {
        View view = this.getView();
        stopAnimators();
        addPanToAnimator(lookAtPos, view.getHeading(), view.getPitch(), distance, true);
        this.getView().emit(Keys.VIEW, null, this.getView());
    }

    public void stopAnimators() {
        this.uiAnimControl.stopAnimations();
        this.gotoAnimControl.stopAnimations();
    }

    public boolean isAnimating() {
        return (this.uiAnimControl.hasActiveAnimation() || this.gotoAnimControl.hasActiveAnimation());
    }

    public void addAnimator(Animator animator) {
        long date = new Date().getTime();
        this.gotoAnimControl.put(OrbitViewInputHandler.VIEW_ANIM_APP + date, animator);
    }

    /**
     * CollisionAwarePitchAccessor implements an {@link PropertyAccessor.AngleAccessor} interface onto the pitch
     * property of an {@link OrbitView}. In addition to accessing the pitch property, this implementation is aware of
     * view-surface collisions caused by setting the pitch property. If a call to {@link #setAngle(Angle)} causes the
     * view to collide with the surface, then the call returns false indicating to the caller that the set operation was
     * not entirely successful.
     */
    protected static class CollisionAwarePitchAccessor implements PropertyAccessor.AngleAccessor {
        protected OrbitView orbitView;

        /**
         * Creates a new CollisionAwarePitchAccessor with the specified OrbitView, but otherwise does nothing.
         *
         * @param orbitView the OrbitView who's pitch will be accessed.
         * @throws IllegalArgumentException if the orbitView is null.
         */
        public CollisionAwarePitchAccessor(OrbitView orbitView) {
            if (orbitView == null) {
                String message = Logging.getMessage("nullValue.OrbitViewIsNull");
                Logging.logger().severe(message);
                throw new IllegalArgumentException(message);
            }

            this.orbitView = orbitView;
        }

        /**
         * Returns the pitch property value from this accessor's view.
         *
         * @return the pitch from this accessor's view.
         */
        public Angle getAngle() {
            return this.orbitView.getPitch();
        }

        /**
         * Sets the pitch property of this accessor's view to the specified value. If the value is null, setting the
         * view's pitch causes a surface collision, or setting the view's pitch causes an exception, this returns false.
         * Otherwise this returns true.
         *
         * @param value the value to set as this view's pitch property.
         * @return true if the pitch property was successfully set, and false otherwise.
         */
        public boolean setAngle(Angle value) {
            if (value == null)
                return false;

            // If the view supports surface collision detection, then clear the view's collision flag prior to
            // making any property changes.
            if (this.orbitView.isDetectCollisions())
                this.orbitView.hadCollisions();

            try {
                this.orbitView.setPitch(value);
            }
            catch (RuntimeException e) {
                String message = Logging.getMessage("generic.ExceptionWhileChangingView");
                Logging.logger().log(Level.SEVERE, message, e);
                return false;
            }

            // If the view supports surface collision detection, then return false if the collision flag is set,
            // otherwise return true.
            return !(this.orbitView.isDetectCollisions() && this.orbitView.hadCollisions());
        }
    }

    /**
     * Action handler to reset roll.
     */
    public class ResetRollActionListener extends ViewInputActionHandler {
        @Override
        public boolean inputActionPerformed(AbstractViewInputHandler inputHandler,
            MouseEvent mouseEvent, ViewInputAttributes.ActionAttributes viewAction) {
            onResetRoll(viewAction);
            return true;
        }
    }
}