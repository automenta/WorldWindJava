/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.video;

import com.jogamp.newt.event.WindowUpdateEvent;
import com.jogamp.opengl.awt.GLJPanel;
import gov.nasa.worldwind.*;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.event.*;
import gov.nasa.worldwind.pick.*;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.util.Collection;

import static java.awt.event.InputEvent.*;

/**
 * actually, this handles events for NEWT too for now.
 * @author tag
 * @version $Id: AWTInputHandler.java 2258 2014-08-22 22:08:33Z dcollins $
 */
public class DefaultInputHandler extends WWObjectImpl implements KeyListener, MouseListener, MouseMotionListener, MouseWheelListener, FocusListener, InputHandler, Disposable {
    private WorldWindow wwd = null;
    public final EventListenerList eventListeners = new EventListenerList();
    private Point mousePoint = new Point();
    private PickedObjectList hoverObjects;
    private PickedObjectList objectsAtButtonPress;
    private boolean isHovering = false;
    private boolean isDragging = false;
    private boolean forceRedrawOnMousePressed = Configuration.getBooleanValue(AVKey.REDRAW_ON_MOUSE_PRESSED, false);
    private Timer hoverTimer = new Timer(600, actionEvent -> {
        if (DefaultInputHandler.this.pickMatches(DefaultInputHandler.this.hoverObjects)) {
            DefaultInputHandler.this.isHovering = true;
            DefaultInputHandler.this.callSelectListeners(new SelectEvent(DefaultInputHandler.this.wwd,
                SelectEvent.HOVER, mousePoint, DefaultInputHandler.this.hoverObjects));
            DefaultInputHandler.this.hoverTimer.stop();
        }
    });
    // Delegate handler for View.
    private SelectListener selectListener;

    public DefaultInputHandler() {
    }

    public void dispose() {
        this.hoverTimer.stop();
        this.hoverTimer = null;

        this.setEventSource(null);

        if (this.hoverObjects != null)
            this.hoverObjects.clear();
        this.hoverObjects = null;

        if (this.objectsAtButtonPress != null)
            this.objectsAtButtonPress.clear();
        this.objectsAtButtonPress = null;
    }

    public void removeHoverSelectListener() {
        hoverTimer.stop();
        hoverTimer = null;
        this.wwd.removeSelectListener(selectListener);
    }

    public WorldWindow getEventSource() {
        return this.wwd;
    }

    public void setEventSource(WorldWindow newWorldWindow) {

        if (newWorldWindow == this.wwd)
            return; //same

        //this.eventListeners = new EventListenerList(); // make orphans of listener references

        if (wwd!=null) {
            wwd.stopEvents(this);

            if (this.selectListener != null)
                this.wwd.removeSelectListener(this.selectListener);

            if (this.wwd.sceneControl() != null)
                this.wwd.sceneControl().removePropertyChangeListener(AVKey.VIEW, this);
        }

        this.wwd = newWorldWindow;

        if (this.wwd == null)
            return;

        this.wwd.view().getViewInputHandler().setWorldWindow(this.wwd);

        wwd.startEvents(this);

        this.selectListener = event -> {
            if (event.getEventAction().equals(SelectEvent.ROLLOVER)) {
                doHover(true);
            }
        };
        this.wwd.addSelectListener(this.selectListener);

        if (this.wwd.sceneControl() != null)
            this.wwd.sceneControl().addPropertyChangeListener(AVKey.VIEW, this);
    }

    public int getHoverDelay() {
        return this.hoverTimer.getDelay();
    }

    public void setHoverDelay(int delay) {
        this.hoverTimer.setDelay(delay);
    }

    public boolean isSmoothViewChanges() {
        return this.wwd.view().getViewInputHandler().isEnableSmoothing();
    }

    public void setSmoothViewChanges(boolean smoothViewChanges) {
        this.wwd.view().getViewInputHandler().setEnableSmoothing(smoothViewChanges);
    }

    public boolean isLockViewHeading() {
        return this.wwd.view().getViewInputHandler().isLockHeading();
    }

    public void setLockViewHeading(boolean lockHeading) {
        this.wwd.view().getViewInputHandler().setLockHeading(lockHeading);
    }

    public boolean isStopViewOnFocusLost() {
        return this.wwd.view().getViewInputHandler().isStopOnFocusLost();
    }

    public void setStopViewOnFocusLost(boolean stopView) {
        this.wwd.view().getViewInputHandler().setStopOnFocusLost(stopView);
    }

    private WorldWindow getWorldWindow() {
        return wwd;
    }

    protected Point getMousePoint() {
        return mousePoint;
    }

    protected void setMousePoint(Point mousePoint) {
        this.mousePoint = mousePoint;
    }

    protected boolean isHovering() {
        return isHovering;
    }

    protected void setHovering(boolean hovering) {
        isHovering = hovering;
    }

    protected PickedObjectList getHoverObjects() {
        return hoverObjects;
    }

    protected void setHoverObjects(PickedObjectList hoverObjects) {
        this.hoverObjects = hoverObjects;
    }

    protected PickedObjectList getObjectsAtButtonPress() {
        return objectsAtButtonPress;
    }

    protected void setObjectsAtButtonPress(PickedObjectList objectsAtButtonPress) {
        this.objectsAtButtonPress = objectsAtButtonPress;
    }

    public boolean isForceRedrawOnMousePressed() {
        return forceRedrawOnMousePressed;
    }

    public void setForceRedrawOnMousePressed(boolean forceRedrawOnMousePressed) {
        this.forceRedrawOnMousePressed = forceRedrawOnMousePressed;
    }

    @Override
    public void mouseClicked(com.jogamp.newt.event.MouseEvent e) {
        mouseClicked(mouseEvent(e, MouseEvent.MOUSE_CLICKED));

    }

    private static MouseEvent mouseEvent(com.jogamp.newt.event.MouseEvent e, int mousePressed) {
        return new MouseEvent(dummySource, mousePressed, e.getWhen(),
            awtModifiers(e), e.getX(), e.getY(), e.getClickCount(), false, e.getButton());
    }
    private static MouseWheelEvent mouseWheelEvent(com.jogamp.newt.event.MouseEvent e) {
        return new MouseWheelEvent(dummySource, MouseEvent.MOUSE_WHEEL, e.getWhen(), 0, e.getX(), e.getY(),
            e.getClickCount(), false, MouseWheelEvent.WHEEL_UNIT_SCROLL,
            1, -Math.round(e.getRotation()[1]));
    }



    @Override
    public void mouseEntered(com.jogamp.newt.event.MouseEvent e) {
        mouseEntered(mouseEvent(e, MouseEvent.MOUSE_ENTERED));
    }

    @Override
    public void mouseExited(com.jogamp.newt.event.MouseEvent e) {
        mouseExited(mouseEvent(e, MouseEvent.MOUSE_EXITED));
    }

    @Override
    public void mousePressed(com.jogamp.newt.event.MouseEvent e) {
        mousePressed(mouseEvent(e, MouseEvent.MOUSE_PRESSED));
    }

    @Override
    public void mouseReleased(com.jogamp.newt.event.MouseEvent e) {
        mouseReleased(mouseEvent(e, MouseEvent.MOUSE_RELEASED));
    }

    @Override
    public void mouseMoved(com.jogamp.newt.event.MouseEvent e) {
        mouseMoved(mouseEvent(e, MouseEvent.MOUSE_MOVED));
    }


    @Override
    public void mouseWheelMoved(com.jogamp.newt.event.MouseEvent e) {
        mouseWheelMoved(mouseWheelEvent(e));
    }


    public void keyTyped(KeyEvent keyEvent) {
        if (this.wwd == null) {
            return;
        }

        if (keyEvent == null) {
            return;
        }

        this.callKeyTypedListeners(keyEvent);

        if (!keyEvent.isConsumed()) {
            this.wwd.view().getViewInputHandler().keyTyped(keyEvent);
        }
    }

    public void keyPressed(KeyEvent keyEvent) {
        if (this.wwd == null) {
            return;
        }

        if (keyEvent == null) {
            return;
        }

        this.callKeyPressedListeners(keyEvent);

        if (!keyEvent.isConsumed()) {
            this.wwd.view().getViewInputHandler().keyPressed(keyEvent);
        }
    }

    public void keyReleased(KeyEvent keyEvent) {
        if (this.wwd == null) {
            return;
        }

        if (keyEvent == null) {
            return;
        }

        this.callKeyReleasedListeners(keyEvent);

        if (!keyEvent.isConsumed()) {
            this.wwd.view().getViewInputHandler().keyReleased(keyEvent);
        }
    }

    public void mouseClicked(final MouseEvent e) {
        if (this.wwd == null || this.wwd.view() == null || e == null)
            return;


        this.callMouseClickedListeners(e);

        PickedObjectList pickedObjects = this.wwd.objectsAtPosition();

        if (pickedObjects != null && pickedObjects.getTopPickedObject() != null
            //&& !pickedObjects.getTopPickedObject().isTerrain()
        ) {
            // Something is under the cursor, so it's deemed "selected".
            if (MouseEvent.BUTTON1 == e.getButton()) {
                this.callSelectListeners(
                    new SelectEvent(this.wwd,
                        e.getClickCount() <= 1 ? SelectEvent.LEFT_CLICK : SelectEvent.LEFT_DOUBLE_CLICK,
                        e, pickedObjects
                    )
                );
            } else if (MouseEvent.BUTTON3 == e.getButton()) {
                this.callSelectListeners(new SelectEvent(this.wwd, SelectEvent.RIGHT_CLICK,
                    e, pickedObjects));
            }

            this.wwd.view().firePropertyChange(AVKey.VIEW, null, this.wwd.view());
        } else {
            if (!e.isConsumed()) {
                this.wwd.view().getViewInputHandler().mouseClicked(e);
            }
        }
    }

    public void mousePressed(MouseEvent e) {
        if (this.wwd == null || e == null) {
            return;
        }

        // Determine if the mouse point has changed since the last mouse move event. This can happen if user switches to
        // another window, moves the mouse, and then switches back to the WorldWind window.
        boolean mousePointChanged = !e.getPoint().equals(this.mousePoint);

        this.mousePoint = e.getPoint();
        this.cancelHover();
        this.cancelDrag();

        // If the mouse point has changed then we need to set a new pick point, and redraw the scene because the current
        // picked object list may not reflect the current mouse position.
        if (mousePointChanged && this.wwd.sceneControl() != null)
            this.wwd.sceneControl().setPickPoint(this.mousePoint);

        if (mousePointChanged || this.isForceRedrawOnMousePressed())
            this.wwd.redraw();

        this.objectsAtButtonPress = this.wwd.objectsAtPosition();

        this.callMousePressedListeners(e);

        final int button = e.getButton();
        if (this.objectsAtButtonPress != null && objectsAtButtonPress.getTopPickedObject() != null
            && !this.objectsAtButtonPress.getTopPickedObject().isTerrain()) {
            // Something is under the cursor, so it's deemed "selected".
            if (MouseEvent.BUTTON1 == button) {
                this.callSelectListeners(new SelectEvent(this.wwd, SelectEvent.LEFT_PRESS,
                    e, this.objectsAtButtonPress));
            } else if (MouseEvent.BUTTON3 == button) {
                this.callSelectListeners(new SelectEvent(this.wwd, SelectEvent.RIGHT_PRESS,
                    e, this.objectsAtButtonPress));
            }

            // Initiate a repaint.
            this.wwd.view().firePropertyChange(AVKey.VIEW, null, this.wwd.view());
        }

        if (!e.isConsumed()) {
            this.wwd.view().getViewInputHandler().mousePressed(e);
        }

        // GLJPanel does not take keyboard focus when the user clicks on it, thereby suppressing key events normally
        // sent to the InputHandler. This workaround calls requestFocus on the GLJPanel each time the user presses the
        // mouse on the GLJPanel, causing GLJPanel to take the focus in the same manner as GLCanvas. Note that focus is
        // passed only when the user clicks the primary mouse button. See
        // http://issues.worldwind.arc.nasa.gov/jira/browse/WWJ-272.
        if (MouseEvent.BUTTON1 == button && this.wwd instanceof GLJPanel) {
            ((Component) this.wwd).requestFocusInWindow();
        }
    }

    public void mouseReleased(MouseEvent mouseEvent) {
        if (this.wwd == null) {
            return;
        }

        if (mouseEvent == null) {
            return;
        }

        this.mousePoint = mouseEvent.getPoint();
        this.callMouseReleasedListeners(mouseEvent);
        if (!mouseEvent.isConsumed()) {
            this.wwd.view().getViewInputHandler().mouseReleased(mouseEvent);
        }
        this.doHover(true);
        this.cancelDrag();
    }

    public void mouseEntered(MouseEvent mouseEvent) {
        if (this.wwd == null || mouseEvent == null) {
            return;
        }

        this.callMouseEnteredListeners(mouseEvent);
        this.wwd.view().getViewInputHandler().mouseEntered(mouseEvent);
        this.cancelHover();
        this.cancelDrag();
    }

    public void mouseExited(MouseEvent mouseEvent) {
        if (this.wwd == null || mouseEvent == null) {
            return;
        }

        this.callMouseExitedListeners(mouseEvent);
        this.wwd.view().getViewInputHandler().mouseExited(mouseEvent);

        // Enqueue a redraw to update the current position and selection.
        if (this.wwd.sceneControl() != null) {
            this.wwd.sceneControl().setPickPoint(null);
            this.wwd.redraw();
        }

        this.cancelHover();
        this.cancelDrag();
    }

    public void mouseDragged(MouseEvent mouseEvent) {
        if (mouseEvent == null || this.wwd == null)
            return;

        Point prevMousePoint = this.mousePoint;
        this.mousePoint = mouseEvent.getPoint();
        this.callMouseDraggedListeners(mouseEvent);

        if ((BUTTON1_DOWN_MASK & mouseEvent.getModifiersEx()) != 0) {
            PickedObjectList pickedObjects = this.objectsAtButtonPress;
            if (this.isDragging
                || (pickedObjects != null && pickedObjects.getTopPickedObject() != null
                && !pickedObjects.getTopPickedObject().isTerrain())) {
                this.isDragging = true;
                DragSelectEvent selectEvent = new DragSelectEvent(this.wwd, SelectEvent.DRAG, mouseEvent, pickedObjects, prevMousePoint);
                this.callSelectListeners(selectEvent);

                // If no listener consumed the event, then cancel the drag.
                if (!selectEvent.isConsumed()) this.cancelDrag();
            }
        }

        if (!this.isDragging && !mouseEvent.isConsumed())
            this.wwd.view().getViewInputHandler().mouseDragged(mouseEvent);

        // Redraw to update the current position and selection.
        if (this.wwd.sceneControl() != null) {
            this.wwd.sceneControl().setPickPoint(mouseEvent.getPoint());
            this.wwd.redraw();
        }
    }

    private static final Component dummySource = new JButton() {
        private final Point ZERO = new Point(0,0);

        @Override
        public Point getLocationOnScreen() {
            return ZERO;
        }
    };

    @Override
    public void mouseDragged(com.jogamp.newt.event.MouseEvent e) {
        mouseDragged(mouseEvent(e, MouseEvent.MOUSE_DRAGGED));
    }

    private static int awtModifiers(com.jogamp.newt.event.MouseEvent e) {
        int modifiersEx = 0;
        if (e.isButtonDown(com.jogamp.newt.event.MouseEvent.BUTTON1))
            modifiersEx |= BUTTON1_DOWN_MASK;
        if (e.isButtonDown(com.jogamp.newt.event.MouseEvent.BUTTON2))
            modifiersEx |= BUTTON2_DOWN_MASK;
        if (e.isButtonDown(com.jogamp.newt.event.MouseEvent.BUTTON3))
            modifiersEx |= BUTTON3_DOWN_MASK;
        return modifiersEx;
    }

    public void mouseMoved(MouseEvent mouseEvent) {
        if (this.wwd == null || mouseEvent == null) {
            return;
        }

        this.mousePoint = mouseEvent.getPoint();
        this.callMouseMovedListeners(mouseEvent);

        if (!mouseEvent.isConsumed()) {
            this.wwd.view().getViewInputHandler().mouseMoved(mouseEvent);
        }

        // Redraw to update the current position and selection.
        if (this.wwd.sceneControl() != null) {
            this.wwd.sceneControl().setPickPoint(mouseEvent.getPoint());
            this.wwd.redraw();
        }
    }

    public void mouseWheelMoved(MouseWheelEvent mouseWheelEvent) {
        if (this.wwd == null || mouseWheelEvent == null) {
            return;
        }

        this.callMouseWheelMovedListeners(mouseWheelEvent);

        if (!mouseWheelEvent.isConsumed())
            this.wwd.view().getViewInputHandler().mouseWheelMoved(mouseWheelEvent);
    }

    public void focusGained(FocusEvent focusEvent) {
        if (this.wwd == null || focusEvent == null) {
            return;
        }

        this.wwd.view().getViewInputHandler().focusGained(focusEvent);
    }

    public void focusLost(FocusEvent focusEvent) {
        if (this.wwd == null || focusEvent == null) {
            return;
        }

        this.wwd.view().getViewInputHandler().focusLost(focusEvent);
    }

    private static boolean isPickListEmpty(Collection<PickedObject> pickList) {
        return pickList == null || pickList.size() < 1;
    }

    private void doHover(boolean reset) {
        PickedObjectList pickedObjects = this.wwd.objectsAtPosition();
        if (!(DefaultInputHandler.isPickListEmpty(this.hoverObjects) || DefaultInputHandler.isPickListEmpty(pickedObjects))) {
            PickedObject hover = this.hoverObjects.getTopPickedObject();
            PickedObject last = pickedObjects.getTopPickedObject();

            Object oh = hover == null ? null : hover.get() != null ? hover.get() :
                hover.getParentLayer();
            if (oh!=null) {
                Object ol = last == null ? null : last.get() != null ? last.get() :
                    last.getParentLayer();
                if (oh.equals(ol)) {
                    return; // object picked is the hover object. don't do anything but wait for the timer to expire.
                }
            }
        }

        this.cancelHover();

        if (!reset) {
            return;
        }

        if ((pickedObjects != null)
            && (pickedObjects.getTopObject() != null)
            && pickedObjects.getTopPickedObject().isTerrain()) {
            return;
        }

        this.hoverObjects = pickedObjects;
        this.hoverTimer.restart();
    }

    private void cancelHover() {
        if (this.isHovering) {
            this.callSelectListeners(new SelectEvent(this.wwd, SelectEvent.HOVER, this.mousePoint, null));
        }

        this.isHovering = false;
        this.hoverObjects = null;
        this.hoverTimer.stop();
    }

    private boolean pickMatches(PickedObjectList pickedObjects) {
        if (DefaultInputHandler.isPickListEmpty(this.wwd.objectsAtPosition()) || DefaultInputHandler.isPickListEmpty(pickedObjects)) {
            return false;
        }

        PickedObject lastTop = this.wwd.objectsAtPosition().getTopPickedObject();

        if (null != lastTop && lastTop.isTerrain()) {
            return false;
        }

        PickedObject newTop = pickedObjects.getTopPickedObject();
        //noinspection SimplifiableIfStatement
        if (lastTop == null || newTop == null || lastTop.get() == null || newTop.get() == null) {
            return false;
        }

        return lastTop.get().equals(newTop.get());
    }

    private void cancelDrag() {
        if (this.isDragging) {
            this.callSelectListeners(new DragSelectEvent(this.wwd, SelectEvent.DRAG_END, null,
                this.objectsAtButtonPress, this.mousePoint));
        }

        this.isDragging = false;
    }

    public void addSelectListener(SelectListener listener) {
        this.eventListeners.add(SelectListener.class, listener);
    }

    public void removeSelectListener(SelectListener listener) {
        this.eventListeners.remove(SelectListener.class, listener);
    }

    private void callSelectListeners(SelectEvent event) {
        for (SelectListener listener : this.eventListeners.getListeners(SelectListener.class)) {
            listener.accept(event);
        }
    }

    public void addKeyListener(KeyListener listener) {
        this.eventListeners.add(KeyListener.class, listener);
    }

    public void removeKeyListener(KeyListener listener) {
        this.eventListeners.remove(KeyListener.class, listener);
    }

    public void addMouseListener(MouseListener listener) {
        this.eventListeners.add(MouseListener.class, listener);
    }

    public void removeMouseListener(MouseListener listener) {
        this.eventListeners.remove(MouseListener.class, listener);
    }

    public void addMouseMotionListener(MouseMotionListener listener) {
        this.eventListeners.add(MouseMotionListener.class, listener);
    }

    public void removeMouseMotionListener(MouseMotionListener listener) {
        this.eventListeners.remove(MouseMotionListener.class, listener);
    }

    public void addMouseWheelListener(MouseWheelListener listener) {
        this.eventListeners.add(MouseWheelListener.class, listener);
    }

    public void removeMouseWheelListener(MouseWheelListener listener) {
        this.eventListeners.remove(MouseWheelListener.class, listener);
    }

    private void callKeyPressedListeners(KeyEvent event) {
        for (KeyListener listener : this.eventListeners.getListeners(KeyListener.class)) {
            listener.keyPressed(event);
        }
    }

    private void callKeyReleasedListeners(KeyEvent event) {
        for (KeyListener listener : this.eventListeners.getListeners(KeyListener.class)) {
            listener.keyReleased(event);
        }
    }

    private void callKeyTypedListeners(KeyEvent event) {
        for (KeyListener listener : this.eventListeners.getListeners(KeyListener.class)) {
            listener.keyTyped(event);
        }
    }

    private void callMousePressedListeners(MouseEvent event) {
        for (MouseListener listener : this.eventListeners.getListeners(MouseListener.class)) {
            listener.mousePressed(event);
        }
    }

    private void callMouseReleasedListeners(MouseEvent event) {
        for (MouseListener listener : this.eventListeners.getListeners(MouseListener.class)) {
            listener.mouseReleased(event);
        }
    }

    private void callMouseClickedListeners(MouseEvent event) {
        for (MouseListener listener : this.eventListeners.getListeners(MouseListener.class)) {
            listener.mouseClicked(event);
        }
    }

    private void callMouseDraggedListeners(MouseEvent event) {
        for (MouseMotionListener listener : this.eventListeners.getListeners(MouseMotionListener.class)) {
            listener.mouseDragged(event);
        }
    }

    private void callMouseMovedListeners(MouseEvent event) {
        for (MouseMotionListener listener : this.eventListeners.getListeners(MouseMotionListener.class)) {
            listener.mouseMoved(event);
        }
    }

    private void callMouseWheelMovedListeners(MouseWheelEvent event) {
        for (MouseWheelListener listener : this.eventListeners.getListeners(MouseWheelListener.class)) {
            listener.mouseWheelMoved(event);
        }
    }

    private void callMouseEnteredListeners(MouseEvent event) {
        for (MouseListener listener : this.eventListeners.getListeners(MouseListener.class)) {
            listener.mouseEntered(event);
        }
    }

    private void callMouseExitedListeners(MouseEvent event) {
        for (MouseListener listener : this.eventListeners.getListeners(MouseListener.class)) {
            listener.mouseExited(event);
        }
    }

    public void propertyChange(PropertyChangeEvent event) {
        if (this.wwd == null || this.wwd.view() == null || event == null) {
            return;
        }

        if ((event.getSource() == this.getWorldWindow().sceneControl())
            && event.getPropertyName().equals(AVKey.VIEW)) {
            this.wwd.view().getViewInputHandler().setWorldWindow(this.wwd);
        }
    }

    @Override
    public void windowResized(com.jogamp.newt.event.WindowEvent e) {

    }

    @Override
    public void windowMoved(com.jogamp.newt.event.WindowEvent e) {

    }

    @Override
    public void windowDestroyNotify(com.jogamp.newt.event.WindowEvent e) {

    }

    @Override
    public void windowDestroyed(com.jogamp.newt.event.WindowEvent e) {

    }

    @Override
    public void windowGainedFocus(com.jogamp.newt.event.WindowEvent e) {
        focusGained(new FocusEvent(dummySource, WindowEvent.WINDOW_GAINED_FOCUS));
    }

    @Override
    public void windowLostFocus(com.jogamp.newt.event.WindowEvent e) {
        focusGained(new FocusEvent(dummySource, WindowEvent.WINDOW_LOST_FOCUS));
    }

    @Override
    public void windowRepaint(WindowUpdateEvent e) {
        //new FocusEvent(dummySource, WindowUpdateEvent.EVENT_WINDOW_REPAINT));
    }
}
