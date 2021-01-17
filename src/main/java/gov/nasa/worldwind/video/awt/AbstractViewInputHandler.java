/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.video.awt;

import gov.nasa.worldwind.*;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.pick.PickedObjectList;
import gov.nasa.worldwind.util.Logging;
import gov.nasa.worldwind.view.orbit.OrbitView;

import java.awt.*;
import java.awt.event.*;
import java.beans.*;

/**
 * @author dcollins
 * @version $Id: AbstractViewInputHandler.java 2251 2014-08-21 21:17:46Z dcollins $
 */
public abstract class AbstractViewInputHandler implements ViewInputHandler, PropertyChangeListener {
    // These constants are used by the device input handling routines to determine whether or not to
    // (1) generate view change events based on the current device state, or
    // (2) query whether or not events would be generated from the current device state.
    protected static final String GENERATE_EVENTS = "GenerateEvents";
    protected static final String SCALE_FUNC_EYE_ALTITUDE_EXP = "ScaleFuncEyeAltitudeExp";
    protected static final String SCALE_FUNC_ZOOM_EXP = "ScaleFuncZoomExp";
    static final int NUM_MODIFIERS = 8;
    private static final double DEFAULT_DRAG_SLOPE_FACTOR = 0.002;
    private static final long DEFAULT_PER_FRAME_INPUT_INTERVAL = 35L; // perform per frame input every 35 ms
    private static final String QUERY_EVENTS = "QueryEvents";
    // These constants define scaling functions for transforming raw input into a range of values. The scale functions
    // are interpreted as follows:
    // EYE_ALTITUDE: distance from eye to ground, divided by 3 * globe's radius and clamped to range [0, 1]
    // ZOOM: distance from eye to view center point, divided by 3 * globe's radius and clamped to range [0, 1]
    // EYE_ALTITUDE_EXP or ZOOM_EXP: function placed in an exponential function in the range [0, 1]
    private static final String SCALE_FUNC_EYE_ALTITUDE = "ScaleFuncEyeAltitude";
    private static final String SCALE_FUNC_ZOOM = "ScaleFuncZoom";
    final ViewInputAttributes.DeviceModifierMap keyModsActionMap;
    final ViewInputAttributes.DeviceModifierMap mouseModsActionMap;
    final ViewInputAttributes.DeviceModifierMap mouseWheelModsActionMap;
    final int[] modifierList =
        {
            KeyEvent.ALT_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK,
            KeyEvent.ALT_DOWN_MASK | KeyEvent.CTRL_DOWN_MASK,
            KeyEvent.ALT_DOWN_MASK | KeyEvent.META_DOWN_MASK,
            KeyEvent.SHIFT_DOWN_MASK,
            KeyEvent.CTRL_DOWN_MASK,
            KeyEvent.META_DOWN_MASK,
            KeyEvent.ALT_DOWN_MASK,
            0
        };
    private final ViewInputAttributes.ActionAttributesMap mouseActionMap;
    private final ViewInputAttributes.ActionAttributesMap keyActionMap;
    private final KeyEventState keyEventState = new KeyEventState();
    protected Matrix mouseDownModelview;
    protected Matrix mouseDownProjection;
    protected Rectangle mouseDownViewport;
    private WorldWindow wwd;
    private ViewInputAttributes attributes;
    // Optional behaviors.
    private boolean enableSmoothing;
    private boolean lockHeading;
    private boolean stopOnFocusLost;
    // AWT event support.
    private boolean wwdFocusOwner;
    private Point mouseDownPoint;
    private Point lastMousePoint;
    private Point mousePoint;
    private Position selectedPosition;
    // Input transformation coefficients.
    private double dragSlopeFactor = AbstractViewInputHandler.DEFAULT_DRAG_SLOPE_FACTOR;
    // Per-frame input event timing support.
    private long perFrameInputInterval = AbstractViewInputHandler.DEFAULT_PER_FRAME_INPUT_INTERVAL;
    private long lastPerFrameInputTime;

    AbstractViewInputHandler() {
        this.enableSmoothing = true;
        this.lockHeading = true;
        this.stopOnFocusLost = true;
        this.attributes = new ViewInputAttributes();

        // Actions for the pointer device
        this.mouseActionMap = this.attributes.getActionMap(ViewInputAttributes.DEVICE_MOUSE);
        this.keyActionMap = this.attributes.getActionMap(ViewInputAttributes.DEVICE_KEYBOARD);
        this.keyModsActionMap = this.attributes.getModifierActionMap(ViewInputAttributes.DEVICE_KEYBOARD);
        this.mouseModsActionMap = this.attributes.getModifierActionMap(ViewInputAttributes.DEVICE_MOUSE);
        this.mouseWheelModsActionMap =
            this.attributes.getModifierActionMap(ViewInputAttributes.DEVICE_MOUSE_WHEEL);
    }

    protected static Point constrainToSourceBounds(Point point, Object source) {
        if (!(source instanceof Component) || point == null)
            return point;

        Component c = (Component) source;

        int x = (int) Math.round(point.getX());
        if (x < 0)
            x = 0;
        final int cw = c.getWidth();
        if (x > cw)
            x = cw;

        int y = (int) Math.round(point.getY());
        if (y < 0)
            y = 0;
        final int ch = c.getHeight();
        if (y > ch)
            y = ch;

        return new Point(x, y);
    }

    private static boolean handlePerFrameAnimation(String target) {
        return false;
    }

    protected static double getScaleValue(double minValue, double maxValue,
        double value, double range, boolean isExp) {
        double t = value / range;
        t = t < 0 ? 0 : (t > 1 ? 1 : t);
        if (isExp) {
            t = Math.pow(2.0, t) - 1.0;
        }
        return (minValue * (1.0 - t) + maxValue * t);
    }

    /**
     * Return the <code>WorldWindow</code> this ViewInputHandler is listening to for input events, and will modify in
     * response to those events
     *
     * @return the <code>WorldWindow</code> this ViewInputHandler is listening to, and will modify in response to
     * events.
     */
    public WorldWindow wwd() {
        return this.wwd;
    }

    /**
     * Sets the <code>WorldWindow</code> this ViewInputHandler should listen to for input events, and should modify in
     * response to those events. If the parameter <code>newWorldWindow</code> is null, then this ViewInputHandler will
     * do nothing.
     *
     * @param newWorldWindow the <code>WorldWindow</code> to listen on, and modify in response to events.
     */
    public void setWorldWindow(WorldWindow newWorldWindow) {
        if (newWorldWindow == this.wwd)
            return;

        if (this.wwd != null) {
            this.wwd.sceneControl().removePropertyChangeListener(this);
        }

        this.wwd = newWorldWindow;

        if (this.wwd != null) {
            this.wwd.sceneControl().addPropertyChangeListener(this);
        }
    }

    /**
     * Returns the values that are used to transform raw input events into view movments.
     *
     * @return values that are be used to transform raw input into view movement.
     */
    public ViewInputAttributes getAttributes() {
        return this.attributes;
    }

    /**
     * Sets the values that will be used to transform raw input events into view movements. ViewInputAttributes define a
     * calibration value for each combination of device and action, and a general sensitivity value for each device.
     *
     * @param attributes values that will be used to transform raw input into view movement.
     * @throws IllegalArgumentException if <code>attributes</code> is null.
     * @see ViewInputAttributes
     */
    public void setAttributes(ViewInputAttributes attributes) {
        if (attributes == null) {
            String message = Logging.getMessage("nullValue.AttributesIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.attributes = attributes;
    }

    /**
     * Returns whether the ViewInputHandler will smooth view movements in response to input events.
     *
     * @return true if the view will movements are smoothed; false otherwise.
     */
    public boolean isEnableSmoothing() {
        return this.enableSmoothing;
    }

    /**
     * Sets whether the ViewInputHandler should smooth view movements in response to input events. A value of true will
     * cause the ViewInputHandler to delegate decisions about whether to smooth a certain input event to its {@link
     * ViewInputAttributes}. A value of false will disable all smoothing.
     *
     * @param enable true to smooth view movements; false otherwise.
     */
    public void setEnableSmoothing(boolean enable) {
        this.enableSmoothing = enable;
    }

    /**
     * Returns whether the view's heading should stay the same unless explicitly changed.
     *
     * @return true if the view's heading will stay the same unless explicity changed; false otherwise.
     */
    public boolean isLockHeading() {
        return this.lockHeading;
    }

    /**
     * Sets whether the view's heading should stay the same unless explicitly changed. For example, moving forward along
     * a great arc would suggest a change in position and heading. If the heading had been locked, the ViewInputHandler
     * will move forward in a way that doesn't change the heading.
     *
     * @param lock true if the view's heading should stay the same unless explicity changed; false otherwise.
     */
    public void setLockHeading(boolean lock) {
        this.lockHeading = lock;
    }

    /**
     * Returns whether the view will stop when the WorldWindow looses focus.
     *
     * @return true if the view will stop when the WorldWindow looses focus; false otherwise.
     */
    public boolean isStopOnFocusLost() {
        return this.stopOnFocusLost;
    }

    /**
     * Sets whether the view should stop when the WorldWindow looses focus.
     *
     * @param stop true if the view should stop when the WorldWindow looses focus; false otherwise.
     */
    public void setStopOnFocusLost(boolean stop) {
        this.stopOnFocusLost = stop;
    }

    /**
     * Returns the <code>factor</code> that dampens view movement when the user pans drags the cursor in a way that
     * could cause an abrupt transition.
     *
     * @return factor dampening view movement when a mouse drag event would cause an abrupt transition.
     * @see #setDragSlopeFactor
     */
    public double getDragSlopeFactor() {
        return this.dragSlopeFactor;
    }

    /**
     * Sets the <code>factor</code> that dampens view movement when a mouse drag event would cause an abrupt transition.
     * The drag slope is the ratio of screen pixels to Cartesian distance moved, measured by the previous and current
     * mouse points. As drag slope gets larger, it becomes more difficult to operate the view. This typically happens
     * while dragging over and around the horizon, where movement of a few pixels can cause the view to move many
     * kilometers. This <code>factor</code> is the amount of damping applied to the view movement in such cases.
     * Setting
     * <code>factor</code> to zero will disable this behavior, while setting <code>factor</code> to a positive value
     * may dampen the effects of mouse dragging.
     *
     * @param factor dampening view movement when a mouse drag event would cause an abrupt transition. Must be greater
     *               than or equal to zero.
     * @throws IllegalArgumentException if <code>factor</code> is less than zero.
     */
    public void setDragSlopeFactor(double factor) {
        if (factor < 0) {
            String message = Logging.getMessage("generic.ArgumentOutOfRange", "factor < 0");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.dragSlopeFactor = factor;
    }

    //**************************************************************//
    //********************  AWT Event Support  *********************//
    //**************************************************************//

    private long getPerFrameInputInterval() {
        return this.perFrameInputInterval;
    }

    protected void setPerFrameInputInterval(long milliseconds) {
        this.perFrameInputInterval = milliseconds;
    }

    protected View getView() {
        return (this.wwd != null) ? this.wwd.view() : null;
    }

    private boolean isWorldWindowFocusOwner() {
        return this.wwdFocusOwner;
    }

    private void setWorldWindowFocusOwner(boolean focusOwner) {
        this.wwdFocusOwner = focusOwner;
    }

    protected Point getMousePoint() {
        return this.mousePoint;
    }

    protected Point getLastMousePoint() {
        return this.lastMousePoint;
    }

    private void updateMousePoint(MouseEvent e) {
        this.lastMousePoint = this.mousePoint;
        this.mousePoint = new Point(e.getPoint());
    }

    //**************************************************************//
    //********************  View Change Events  ********************//
    //**************************************************************//

    protected Position getSelectedPosition() {
        return this.selectedPosition;
    }

    //**************************************************************//
    //********************  Key Events  ****************************//
    //**************************************************************//

    protected void setSelectedPosition(Position position) {
        this.selectedPosition = position;
    }

    protected Position computeSelectedPosition() {
        PickedObjectList pickedObjects = this.wwd.objectsAtPosition();
        if (pickedObjects != null) {
            if (pickedObjects.getTerrainObject() != null) {
                return pickedObjects.getTerrainObject().position();
            }
        }
        return null;
    }

    void onStopView() {
        View view = this.getView();
        if (view == null) // include this test to ensure any derived implementation performs it
        {
            return;
        }

        view.stopMovement();
    }

    public void keyTyped(KeyEvent e) {
        if (this.wwd == null) // include this test to ensure any derived implementation performs it
        {
            return;
        }

        if (e == null) // include this test to ensure any derived implementation performs it
        {
            return;
        }

        this.keyEventState.keyTyped(e);
    }

    public void keyPressed(KeyEvent e) {
        if (this.wwd == null) // include this test to ensure any derived implementation performs it
        {
            return;
        }

        if (e == null) // include this test to ensure any derived implementation performs it
        {
            return;
        }

        this.keyEventState.keyPressed(e);
        this.handleKeyPressed(e);
    }

    //**************************************************************//
    //********************  Mouse Events  **************************//
    //**************************************************************//

    public void keyReleased(KeyEvent e) {
        if (this.wwd == null) // include this test to ensure any derived implementation performs it
        {
            return;
        }

        if (e == null) // include this test to ensure any derived implementation performs it
        {
            return;
        }

        this.keyEventState.keyReleased(e);
        this.handleKeyReleased(e);
    }

    @SuppressWarnings("UnusedDeclaration")
    protected void handleKeyPressed(KeyEvent e) {
        // Determine whether or not the current key state would have generated a view change event.
        // If so, issue a repaint event to give the per-frame input a chance to run.
        if (this.handlePerFrameKeyState(this.keyEventState, AbstractViewInputHandler.QUERY_EVENTS)) {
            View view = this.getView();
            if (view != null) {
                view.firePropertyChange(AVKey.VIEW, null, view);
            }
        }
    }

    @SuppressWarnings("UnusedDeclaration")
    protected void handleKeyReleased(KeyEvent e) {

    }

    public void mouseClicked(MouseEvent e) {
        if (this.wwd == null) // include this test to ensure any derived implementation performs it
        {
            return;
        }

        if (e == null) // include this test to ensure any derived implementation performs it
        {
            return;
        }

        this.handleMouseClicked(e);
    }

    public void mousePressed(MouseEvent e) {
        if (this.wwd == null) // include this test to ensure any derived implementation performs it
        {
            return;
        }

        if (e == null) // include this test to ensure any derived implementation performs it
        {
            return;
        }
        this.keyEventState.mousePressed(e);
        this.setMouseDownPoint(e.getPoint());
        this.setSelectedPosition(this.computeSelectedPosition());
        this.setMouseDownView(this.getView());
        this.updateMousePoint(e);
        this.handleMousePressed(e);
    }

    public void mouseReleased(MouseEvent e) {
        if (this.wwd == null) // include this test to ensure any derived implementation performs it
        {
            return;
        }

        if (e == null) // include this test to ensure any derived implementation performs it
        {
            return;
        }
        this.keyEventState.mouseReleased(e);
        this.setMouseDownPoint(null);
        this.setSelectedPosition(null);
        this.setMouseDownView(null);
        this.handleMouseReleased(e);
    }

    public void mouseEntered(MouseEvent e) {
        if (this.wwd == null) // include this test to ensure any derived implementation performs it
        {
            return;
        }

        if (e == null) // include this test to ensure any derived implementation performs it
        {
            //noinspection UnnecessaryReturnStatement
            return;
        }
    }

    public void mouseExited(MouseEvent e) {
        if (this.wwd == null) // include this test to ensure any derived implementation performs it
        {
            return;
        }

        if (e == null) // include this test to ensure any derived implementation performs it
        {
            //noinspection UnnecessaryReturnStatement
            return;
        }
    }

    //**************************************************************//
    //********************  Mouse Motion Events  *******************//
    //**************************************************************//

    @SuppressWarnings("UnusedDeclaration")
    protected void handleMouseClicked(MouseEvent e) {
    }

    @SuppressWarnings("UnusedDeclaration")
    protected void handleMousePressed(MouseEvent e) {
        // Determine whether or not the current key state would have generated a view change event.
        // If so, issue a repaint event to give the per-frame input a chance to run.
        if (this.handlePerFrameMouseState(this.keyEventState, AbstractViewInputHandler.QUERY_EVENTS)) {
            View view = this.getView();
            if (view != null) {
                view.firePropertyChange(AVKey.VIEW, null, view);
            }
        }
    }

    @SuppressWarnings("UnusedDeclaration")
    protected void handleMouseReleased(MouseEvent e) {
    }

    public void mouseDragged(MouseEvent e) {
        if (this.wwd == null) // include this test to ensure any derived implementation performs it
        {
            return;
        }

        if (e == null) // include this test to ensure any derived implementation performs it
        {
            return;
        }

        this.updateMousePoint(e);
        this.handleMouseDragged(e);
    }

    //**************************************************************//
    //********************  Mouse Wheel Events  ********************//
    //**************************************************************//

    public void mouseMoved(MouseEvent e) {
        if (this.wwd == null) // include this test to ensure any derived implementation performs it
        {
            return;
        }

        if (e == null) // include this test to ensure any derived implementation performs it
        {
            return;
        }

        this.updateMousePoint(e);
        this.handleMouseMoved(e);
    }

    @SuppressWarnings("UnusedDeclaration")
    protected void handleMouseDragged(MouseEvent e) {
    }

    //**************************************************************//
    //********************  Focus Events  **************************//
    //**************************************************************//

    @SuppressWarnings("UnusedDeclaration")
    protected void handleMouseMoved(MouseEvent e) {
    }

    public void mouseWheelMoved(MouseWheelEvent e) {
        if (this.wwd == null) // include this test to ensure any derived implementation performs it
        {
            return;
        }

        if (e == null) // include this test to ensure any derived implementation performs it
        {
            return;
        }

        this.handleMouseWheelMoved(e);
    }

    @SuppressWarnings("UnusedDeclaration")
    protected void handleMouseWheelMoved(MouseWheelEvent e) {
    }

    public void focusGained(FocusEvent e) {
        if (this.wwd == null) // include this test to ensure any derived implementation performs it
        {
            return;
        }

        if (e == null) // include this test to ensure any derived implementation performs it
        {
            return;
        }

        this.setWorldWindowFocusOwner(true);
        this.handleFocusGained(e);
    }

    public void focusLost(FocusEvent e) {
        if (this.wwd == null) // include this test to ensure any derived implementation performs it
        {
            return;
        }

        if (e == null) // include this test to ensure any derived implementation performs it
        {
            return;
        }

        this.keyEventState.clearKeyState();
        this.setWorldWindowFocusOwner(false);
        this.handleFocusLost(e);
    }

    @SuppressWarnings("UnusedDeclaration")
    protected void handleFocusGained(FocusEvent e) {

    }

    @SuppressWarnings("UnusedDeclaration")
    protected void handleFocusLost(FocusEvent e) {
        if (this.isStopOnFocusLost())
            this.onStopView();
    }

    public void apply() {
        // Process per-frame input only when the WorldWindow is the focus owner.
        if (!this.isWorldWindowFocusOwner()) {
            return;
        }

        // Throttle the interval at which we process per-frame input, which is usually invoked each frame. This helps
        // balance the input response of high and low framerate applications.
        long now = System.currentTimeMillis();
        long interval = now - this.lastPerFrameInputTime;
        if (interval >= this.getPerFrameInputInterval()) {
            this.handlePerFrameKeyState(this.keyEventState, AbstractViewInputHandler.GENERATE_EVENTS);
            this.handlePerFrameMouseState(this.keyEventState, AbstractViewInputHandler.GENERATE_EVENTS);
            AbstractViewInputHandler.handlePerFrameAnimation(AbstractViewInputHandler.GENERATE_EVENTS);
            this.lastPerFrameInputTime = now;
            this.wwd().redraw();
            return;
        }

        // Determine whether or not the current key state would have generated a view change event. If so, issue
        // a repaint event to give the per-frame input a chance to run again.
        if (this.handlePerFrameKeyState(this.keyEventState, AbstractViewInputHandler.QUERY_EVENTS) ||
            this.handlePerFrameMouseState(this.keyEventState, AbstractViewInputHandler.QUERY_EVENTS) ||
            AbstractViewInputHandler.handlePerFrameAnimation(AbstractViewInputHandler.QUERY_EVENTS)) {
            this.wwd().redraw();
        }
    }

    public void viewApplied() {
    }

    //**************************************************************//
    //********************  Property Change Events  ****************//
    //**************************************************************//

    // Interpret the current key state according to the specified target. If the target is KEY_POLL_GENERATE_EVENTS,
    // then the the key state will generate any appropriate view change events. If the target is KEY_POLL_QUERY_EVENTS,
    // then the key state will not generate events, and this will return whether or not any view change events would
    // have been generated.
    boolean handlePerFrameKeyState(KeyEventState keys, String target) {
        return false;
    }

    boolean handlePerFrameMouseState(KeyEventState keys, String target) {
        return false;
    }

    //**************************************************************//
    //********************  Raw Input Transformation  **************//
    //**************************************************************//

    public void propertyChange(PropertyChangeEvent e) {
        if (this.wwd == null) // include this test to ensure any derived implementation performs it
        {
            return;
        }

        if (e == null) // include this test to ensure any derived implementation performs it
        {
            return;
        }

        this.handlePropertyChange(e);
    }

    @SuppressWarnings("UnusedDeclaration")
    protected void handlePropertyChange(PropertyChangeEvent e) {

    }

    // Translates raw user input into a change in value, according to the specified device and action attributes.
    // The input is scaled by the action attribute range (depending on eye position), then scaled by the device
    // sensitivity.
    protected double rawInputToChangeInValue(double rawInput,
        ViewInputAttributes.DeviceAttributes deviceAttributes, ViewInputAttributes.ActionAttributes actionAttributes,
        String scaleFunc) {
        double value = rawInput;

        double[] range = actionAttributes.getValues();
        value *= this.getScaledValue(range[0], range[1], scaleFunc);
        value *= deviceAttributes.getSensitivity();

        return value;
    }

    private double getScaledValue(double minValue, double maxValue, String scaleFunc) {
        if (scaleFunc == null) {
            return minValue;
        }

        double t = 0.0;
        if (scaleFunc.startsWith(AbstractViewInputHandler.SCALE_FUNC_EYE_ALTITUDE)) {
            t = this.evaluateScaleFuncEyeAltitude();
        } else if (scaleFunc.startsWith(AbstractViewInputHandler.SCALE_FUNC_ZOOM)) {
            t = this.evaluateScaleFuncZoom();
        }

        if (scaleFunc.toLowerCase().endsWith("exp")) {
            t = Math.pow(2.0, t) - 1.0;
        }

        return minValue * (1.0 - t) + maxValue * t;
    }

    private double evaluateScaleFuncEyeAltitude() {
        View view = this.getView();
        if (view == null) {
            return 0.0;
        }

        Position eyePos = view.getEyePosition();
        double radius = this.wwd.model().getGlobe().getRadius();
        double surfaceElevation = this.wwd.model().getGlobe().elevation(eyePos.getLatitude(),
            eyePos.getLongitude());
        double t = (eyePos.getElevation() - surfaceElevation) / (3.0 * radius);
        return (t < 0 ? 0 : (t > 1 ? 1 : t));
    }

    private double evaluateScaleFuncZoom() {
        View view = this.getView();
        if (view == null) {
            return 0.0;
        }

        if (view instanceof OrbitView) {
            double radius = this.wwd.model().getGlobe().getRadius();
            double t = ((OrbitView) view).getZoom() / (3.0 * radius);
            return (t < 0 ? 0 : (t > 1 ? 1 : t));
        }

        return 0.0;
    }

    //**************************************************************//
    //********************  Utility Methods  ***********************//
    //**************************************************************//

    protected double getScaleValueElevation(
        ViewInputAttributes.DeviceAttributes deviceAttributes, ViewInputAttributes.ActionAttributes actionAttributes) {
        View view = this.getView();
        if (view == null) {
            return 0.0;
        }

        double[] range = actionAttributes.getValues();

        Position eyePos = view.getEyePosition();
        double radius = this.wwd.model().getGlobe().getRadius();
        double surfaceElevation = this.wwd.model().getGlobe().elevation(eyePos.getLatitude(),
            eyePos.getLongitude());
        double t = AbstractViewInputHandler.getScaleValue(range[0], range[1],
            eyePos.getElevation() - surfaceElevation, 3.0 * radius, true);
        t *= deviceAttributes.getSensitivity();

        return t;
    }

    protected Vec4 computeSelectedPointAt(Point point) {
        if (this.getSelectedPosition() == null) {
            return null;
        }

        View view = this.getView();
        if (view == null) {
            return null;
        }

        // Reject a selected position if its elevation is above the eye elevation. When that happens, the user is
        // essentially dragging along the inside of a sphere, and the effects of dragging are reversed. To the user
        // this behavior appears unpredictable.
        double elevation = this.getSelectedPosition().getElevation();
        if (view.getEyePosition().getElevation() <= elevation) {
            return null;
        }

        // Intersect with a somewhat larger or smaller Globe which will pass through the selected point, but has the
        // same proportions as the actual Globe. This will simulate dragging the selected position more accurately.
        Line ray = view.computeRayFromScreenPoint(point.getX(), point.getY());
        Intersection[] intersections = this.wwd.model().getGlobe().intersect(ray, elevation);
        if (intersections == null || intersections.length == 0) {
            return null;
        }

        return ray.nearestIntersectionPoint(intersections);
    }

    protected LatLon getChangeInLocation(Point point1, Point point2, Vec4 vec1, Vec4 vec2) {
        // Modify the distance we'll actually travel based on the slope of world distance travelled to screen
        // distance travelled . A large slope means the user made a small change in screen space which resulted
        // in a large change in world space. We want to reduce the impact of that change to something reasonable.

        double dragSlope = this.computeDragSlope(point1, point2, vec1, vec2);
        double dragSlopeFactor = this.getDragSlopeFactor();
        double scale = 1.0 / (1.0 + dragSlopeFactor * dragSlope * dragSlope);

        Position pos1 = this.wwd.model().getGlobe().computePositionFromPoint(vec1);
        Position pos2 = this.wwd.model().getGlobe().computePositionFromPoint(vec2);
        LatLon adjustedLocation = LatLon.interpolateGreatCircle(scale, pos1, pos2);

        // Return the distance to travel in angular degrees.
        return pos1.subtract(adjustedLocation);
    }

    public double computeDragSlope(Point point1, Point point2, Vec4 vec1, Vec4 vec2) {
        View view = this.getView();
        if (view == null) {
            return 0.0;
        }

        // Compute the screen space distance between point1 and point2.
        double dx = point2.getX() - point1.getX();
        double dy = point2.getY() - point1.getY();
        double pixelDistance = Math.sqrt(dx * dx + dy * dy);

        // Determine the distance from the eye to the point on the forward vector closest to vec1 and vec2
        double d = view.getEyePoint().distanceTo3(vec1);
        // Compute the size of a screen pixel at the nearest of the two distances.
        double pixelSize = view.computePixelSizeAtDistance(d);

        // Return the ratio of world distance to screen distance.
        double slope = vec1.distanceTo3(vec2) / (pixelDistance * pixelSize);
        if (slope < 1.0)
            slope = 1.0;

        return slope - 1.0;
    }

    public Point getMouseDownPoint() {
        return mouseDownPoint;
    }

    private void setMouseDownPoint(Point mouseDownPoint) {
        this.mouseDownPoint = mouseDownPoint;
    }

    private void setMouseDownView(View mouseDownView) {
        if (mouseDownView != null) {
            this.mouseDownModelview = mouseDownView.getModelviewMatrix();
            this.mouseDownProjection = mouseDownView.getProjectionMatrix();
            this.mouseDownViewport = mouseDownView.getViewport();
        } else {
            this.mouseDownModelview = null;
            this.mouseDownProjection = null;
            this.mouseDownViewport = null;
        }
    }
}
