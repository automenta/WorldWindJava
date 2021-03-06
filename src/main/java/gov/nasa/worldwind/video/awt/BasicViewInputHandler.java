/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.video.awt;

import gov.nasa.worldwind.View;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.view.ViewUtil;

import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.util.List;

/**
 * @author dcollins
 * @version $Id: BasicViewInputHandler.java 2251 2014-08-21 21:17:46Z dcollins $
 */
public abstract class BasicViewInputHandler extends AbstractViewInputHandler {
    public BasicViewInputHandler() {

        ViewInputAttributes.ActionAttributes actionAttrs;
        // Get action maps for mouse and keyboard
        // ----------------------------------Key Rotation --------------------------------------------
        RotateActionListener rotateActionListener = new RotateActionListener();
        this.getAttributes().setActionListener(
            ViewInputAttributes.DEVICE_KEYBOARD, ViewInputAttributes.VIEW_ROTATE_KEYS, rotateActionListener);
        this.getAttributes().setActionListener(
            ViewInputAttributes.DEVICE_KEYBOARD, ViewInputAttributes.VIEW_ROTATE_SLOW, rotateActionListener);
        this.getAttributes().setActionListener(
            ViewInputAttributes.DEVICE_KEYBOARD, ViewInputAttributes.VIEW_ROTATE_KEYS_SHIFT, rotateActionListener);
        this.getAttributes().setActionListener(
            ViewInputAttributes.DEVICE_KEYBOARD, ViewInputAttributes.VIEW_ROTATE_KEYS_SHIFT_SLOW, rotateActionListener);

        // ----------------------------------Key Vertical Translation --------------------------------
        KeyInputActionHandler vertActionsListener = new VerticalTransActionListener();
        actionAttrs =
            this.getAttributes().getActionMap(ViewInputAttributes.DEVICE_KEYBOARD).getActionAttributes(
                ViewInputAttributes.VIEW_VERTICAL_TRANS_KEYS_CTRL);
        actionAttrs.setActionListener(vertActionsListener);
        actionAttrs =
            this.getAttributes().getActionMap(ViewInputAttributes.DEVICE_KEYBOARD).getActionAttributes(
                ViewInputAttributes.VIEW_VERTICAL_TRANS_KEYS_SLOW_CTRL);
        actionAttrs.setActionListener(vertActionsListener);
        actionAttrs =
            this.getAttributes().getActionMap(ViewInputAttributes.DEVICE_KEYBOARD).getActionAttributes(
                ViewInputAttributes.VIEW_VERTICAL_TRANS_KEYS);
        actionAttrs.setActionListener(vertActionsListener);
        actionAttrs =
            this.getAttributes().getActionMap(ViewInputAttributes.DEVICE_KEYBOARD).getActionAttributes(
                ViewInputAttributes.VIEW_VERTICAL_TRANS_KEYS_SLOW);
        actionAttrs.setActionListener(vertActionsListener);

        // ----------------------------------Key Horizontal Translation ------------------------------
        KeyInputActionHandler horizTransActionListener = new HorizontalTransActionListener();
        actionAttrs =
            this.getAttributes().getActionMap(ViewInputAttributes.DEVICE_KEYBOARD).getActionAttributes(
                ViewInputAttributes.VIEW_HORIZONTAL_TRANS_KEYS);
        actionAttrs.setActionListener(horizTransActionListener);
        actionAttrs =
            this.getAttributes().getActionMap(ViewInputAttributes.DEVICE_KEYBOARD).getActionAttributes(
                ViewInputAttributes.VIEW_HORIZONTAL_TRANSLATE_SLOW);
        actionAttrs.setActionListener(horizTransActionListener);

        // -------------------------------- Mouse Rotation -------------------------------------------
        MouseInputActionHandler rotateMouseListener = new RotateMouseActionListener();
        actionAttrs =
            this.getAttributes().getActionMap(ViewInputAttributes.DEVICE_MOUSE).getActionAttributes(
                ViewInputAttributes.VIEW_ROTATE);
        actionAttrs.setMouseActionListener(rotateMouseListener);
        actionAttrs =
            this.getAttributes().getActionMap(ViewInputAttributes.DEVICE_MOUSE).getActionAttributes(
                ViewInputAttributes.VIEW_ROTATE_SHIFT);
        actionAttrs.setMouseActionListener(rotateMouseListener);

        // ----------------------------- Mouse Horizontal Translation --------------------------------
        actionAttrs =
            this.getAttributes().getActionMap(ViewInputAttributes.DEVICE_MOUSE).getActionAttributes(
                ViewInputAttributes.VIEW_HORIZONTAL_TRANSLATE);
        actionAttrs.setMouseActionListener(new HorizTransMouseActionListener());

        // ----------------------------- Mouse Vertical Translation ----------------------------------
        MouseInputActionHandler vertTransListener = new VertTransMouseActionListener();
        actionAttrs =
            this.getAttributes().getActionMap(ViewInputAttributes.DEVICE_MOUSE).getActionAttributes(
                ViewInputAttributes.VIEW_VERTICAL_TRANSLATE);
        actionAttrs.setMouseActionListener(vertTransListener);
        actionAttrs =
            this.getAttributes().getActionMap(ViewInputAttributes.DEVICE_MOUSE).getActionAttributes(
                ViewInputAttributes.VIEW_VERTICAL_TRANSLATE_CTRL);
        actionAttrs.setMouseActionListener(vertTransListener);

        // Move to mouse
        actionAttrs = this.getAttributes().getActionMap(ViewInputAttributes.DEVICE_MOUSE).getActionAttributes(
            ViewInputAttributes.VIEW_MOVE_TO);
        actionAttrs.setMouseActionListener(new MoveToMouseActionListener());

        // Reset Heading
        actionAttrs =
            this.getAttributes().getActionMap(ViewInputAttributes.DEVICE_KEYBOARD).getActionAttributes(
                ViewInputAttributes.VIEW_RESET_HEADING);
        actionAttrs.setActionListener(new ResetHeadingActionListener());

        // Reset Heading and Pitch
        actionAttrs =
            this.getAttributes().getActionMap(ViewInputAttributes.DEVICE_KEYBOARD).getActionAttributes(
                ViewInputAttributes.VIEW_RESET_HEADING_PITCH_ROLL);
        actionAttrs.setActionListener(new ResetHeadingPitchActionListener());

        // Stop View
        actionAttrs =
            this.getAttributes().getActionMap(ViewInputAttributes.DEVICE_KEYBOARD).getActionAttributes(
                ViewInputAttributes.VIEW_STOP_VIEW);
        actionAttrs.setActionListener(new StopViewActionListener());

        // Mouse Wheel vertical translate
        actionAttrs = this.getAttributes().getActionMap(ViewInputAttributes.DEVICE_MOUSE_WHEEL).getActionAttributes(
            ViewInputAttributes.VIEW_VERTICAL_TRANSLATE);
        actionAttrs.setMouseActionListener(new VertTransMouseWheelActionListener());
    }

    protected abstract void onMoveTo(Position focalPosition,
        ViewInputAttributes.DeviceAttributes deviceAttributes,
        ViewInputAttributes.ActionAttributes actionAttribs);

    protected abstract void onHorizontalTranslateRel(double forwardInput, double sideInput,
        double sideInputFromMouseDown, double forwardInputFromMouseDown,
        ViewInputAttributes.DeviceAttributes deviceAttributes,
        ViewInputAttributes.ActionAttributes actionAttributes);

    protected abstract void onVerticalTranslate(double translateChange, double totalTranslateChange,
        ViewInputAttributes.DeviceAttributes deviceAttributes,
        ViewInputAttributes.ActionAttributes actionAttributes);

    protected abstract void onRotateView(double headingInput, double pitchInput,
        double totalHeadingInput, double totalPitchInput,
        ViewInputAttributes.DeviceAttributes deviceAttributes,
        ViewInputAttributes.ActionAttributes actionAttributes);

    protected abstract void onResetHeading(ViewInputAttributes.ActionAttributes actionAttribs);

    protected abstract void onResetHeadingPitchRoll(ViewInputAttributes.ActionAttributes actionAttribs);

    //**************************************************************//
    //********************  Key Events  ****************************//
    //**************************************************************//
    protected void handleKeyPressed(KeyEvent e) {

        boolean eventHandled = false;

        int modifier = e.getModifiersEx();
        for (int i = 0; i < AbstractViewInputHandler.NUM_MODIFIERS; i++) {
            if ((((modifier & this.modifierList[i]) == this.modifierList[i]))) {
                Iterable<ViewInputAttributes.ActionAttributes> actionList = (ViewInputAttributes.ActionAttributesList)
                    keyModsActionMap.get(this.modifierList[i]);
                eventHandled = callActionListListeners(e,
                    ViewInputAttributes.ActionAttributes.ActionTrigger.ON_PRESS, actionList);
            }
        }

        if (!eventHandled) {
            super.handleKeyPressed(e);
        }
    }

    //**************************************************************//
    //********************  Mouse Events  **************************//
    //**************************************************************//
    protected void handleMouseClicked(MouseEvent e) {

        boolean eventHandled = false;

        int modifier = e.getModifiersEx();
        for (int i = 0; i < AbstractViewInputHandler.NUM_MODIFIERS; i++) {
            if ((((modifier & this.modifierList[i]) == this.modifierList[i]))) {
                Iterable<ViewInputAttributes.ActionAttributes> actionList = (ViewInputAttributes.ActionAttributesList)
                    mouseModsActionMap.get(this.modifierList[i]);
                eventHandled = callMouseActionListListeners(e,
                    ViewInputAttributes.ActionAttributes.ActionTrigger.ON_PRESS, actionList);
            }
        }

        if (!eventHandled) {
            super.handleMouseClicked(e);
        }
    }

    protected void handleMouseWheelMoved(MouseWheelEvent e) {
        boolean eventHandled = false;

        // TODO : Make this conditional look like handleMouseDragged
        int modifier = e.getModifiersEx();
        for (int i = 0; i < AbstractViewInputHandler.NUM_MODIFIERS; i++) {
            if ((((modifier & this.modifierList[i]) == this.modifierList[i]))) {
                Iterable<ViewInputAttributes.ActionAttributes> actionList = (ViewInputAttributes.ActionAttributesList)
                    mouseWheelModsActionMap.get(this.modifierList[i]);
                eventHandled = callMouseWheelActionListListeners(e,
                    ViewInputAttributes.ActionAttributes.ActionTrigger.ON_DRAG, actionList);
            }
        }

        if (!eventHandled) {
            super.handleMouseWheelMoved(e);
        }
    }

    //**************************************************************//
    //********************  Mouse Motion Events  *******************//
    //**************************************************************//
    protected void handleMouseDragged(MouseEvent e) {

        int modifier = e.getModifiersEx();

        for (int i = 0; i < AbstractViewInputHandler.NUM_MODIFIERS; i++) {
            if ((((modifier & this.modifierList[i]) == this.modifierList[i]))) {
                Iterable<ViewInputAttributes.ActionAttributes> actionList = (ViewInputAttributes.ActionAttributesList)
                    mouseModsActionMap.get(this.modifierList[i]);
                if (callMouseActionListListeners(e,
                    ViewInputAttributes.ActionAttributes.ActionTrigger.ON_DRAG, actionList)) {
                    return;
                }
            }
        }
    }

    // Interpret the current key state according to the specified target. If the target is KEY_POLL_GENERATE_EVENTS,
    // then the the key state will generate any appropriate view change events. If the target is KEY_POLL_QUERY_EVENTS,
    // then the key state will not generate events, and this will return whether or not any view change events would
    // have been generated.
    protected boolean handlePerFrameKeyState(KeyEventState keys, String target) {

        if (keys.getNumKeysDown() == 0)
            return false;

        boolean isKeyEventTrigger = false;

        int modifier = keys.getModifiersEx();
        for (int i = 0; i < AbstractViewInputHandler.NUM_MODIFIERS; i++) {
            if (((modifier & this.modifierList[i]) == this.modifierList[i])) {

                Iterable<ViewInputAttributes.ActionAttributes> actionList = (ViewInputAttributes.ActionAttributesList)
                    keyModsActionMap.get(this.modifierList[i]);
                isKeyEventTrigger = callActionListListeners(keys, target,
                    ViewInputAttributes.ActionAttributes.ActionTrigger.ON_KEY_DOWN, actionList);
                break;
            }
        }

        return isKeyEventTrigger;
    }

    protected boolean handlePerFrameMouseState(KeyEventState keys, String target) {

        if (keys.getNumButtonsDown() == 0) {
            return false;
        }

        int modifier = keys.getModifiersEx();

        for (int i = 0; i < AbstractViewInputHandler.NUM_MODIFIERS; i++) {
            if (((modifier & this.modifierList[i]) == this.modifierList[i])) {

                Iterable<ViewInputAttributes.ActionAttributes> actionList = (ViewInputAttributes.ActionAttributesList)
                    mouseModsActionMap.get(this.modifierList[i]);
                if (callActionListListeners(keys, target,
                    ViewInputAttributes.ActionAttributes.ActionTrigger.ON_KEY_DOWN, actionList)) {

                    return true;
                }
            }
        }

        return false;
    }

    private boolean callMouseActionListListeners(MouseEvent e,
        ViewInputAttributes.ActionAttributes.ActionTrigger trigger,
        Iterable<ViewInputAttributes.ActionAttributes> actionList) {
        boolean eventHandled = false;
        if (actionList != null) {
            for (ViewInputAttributes.ActionAttributes actionAttr : actionList) {
                final MouseInputActionHandler l = actionAttr.getMouseActionListener();
                if (l != null && actionAttr.getActionTrigger() == trigger) {
                    if (l.inputActionPerformed(this, e, actionAttr)) {
                        eventHandled = true;
                    }
                }
            }
        }
        return eventHandled;
    }

    private boolean callMouseWheelActionListListeners(MouseWheelEvent e,
        ViewInputAttributes.ActionAttributes.ActionTrigger trigger,
        Iterable<ViewInputAttributes.ActionAttributes> actionList) {
        boolean eventHandled = false;
        if (actionList != null) {
            for (ViewInputAttributes.ActionAttributes actionAttr : actionList) {
                if (actionAttr.getMouseActionListener() != null &&
                    actionAttr.getActionTrigger() == trigger) {
                    if (actionAttr.getMouseActionListener().inputActionPerformed(this, e, actionAttr)) {
                        eventHandled = true;
                    }
                }
            }
        }
        return eventHandled;
    }

    private boolean callActionListListeners(KeyEvent e,
        ViewInputAttributes.ActionAttributes.ActionTrigger trigger,
        Iterable<ViewInputAttributes.ActionAttributes> actionList) {
        boolean eventHandled = false;
        if (actionList != null) {
            for (ViewInputAttributes.ActionAttributes actionAttr : actionList) {
                if (actionAttr.getActionListener() != null &&
                    actionAttr.getActionTrigger() == trigger) {
                    if (actionAttr.getActionListener().inputActionPerformed(this, e, actionAttr)) {
                        eventHandled = true;
                    }
                }
            }
        }
        return eventHandled;
    }

    private boolean callActionListListeners(KeyEventState keys, String target,
        ViewInputAttributes.ActionAttributes.ActionTrigger trigger,
        Iterable<ViewInputAttributes.ActionAttributes> actionList) {
        boolean eventHandled = false;
        if (actionList != null) {
            for (ViewInputAttributes.ActionAttributes actionAttr : actionList) {
                if (actionAttr.getActionTrigger() == trigger) {
                    if (callActionListener(keys, target, actionAttr)) {
                        eventHandled = true;
                    }
                }
            }
        }
        return eventHandled;
    }

    private boolean callActionListener(KeyEventState keys, String target,
        ViewInputAttributes.ActionAttributes action) {

        if (action.getActionListener() != null) {
            return (action.getActionListener().inputActionPerformed(this, keys, target, action));
        }
        if (action.getMouseActionListener() != null) {
            return (action.getMouseActionListener().inputActionPerformed(keys, target, action));
        }
        return false;
    }

    protected void handlePropertyChange(PropertyChangeEvent e) {
        super.handlePropertyChange(e);

        //noinspection StringEquality
        if (e.getPropertyName() == View.VIEW_STOPPED) {
            this.handleViewStopped();
        }
    }

    protected void handleViewStopped() {

    }

    public class RotateActionListener extends ViewInputActionHandler {
        public boolean inputActionPerformed(AbstractViewInputHandler inputHandler, KeyEventState keys, String target,
            ViewInputAttributes.ActionAttributes viewAction) {
            List keyList = viewAction.getKeyActions();
            double headingInput = 0;
            double pitchInput = 0;
            for (Object k : keyList) {
                ViewInputAttributes.ActionAttributes.KeyAction keyAction =
                    (ViewInputAttributes.ActionAttributes.KeyAction) k;
                if (keys.isKeyDown(keyAction.keyCode)) {
                    if (keyAction.direction == ViewInputAttributes.ActionAttributes.KeyAction.KA_DIR_X) {
                        headingInput += keyAction.sign;
                    } else {
                        pitchInput += keyAction.sign;
                    }
                }
            }

            if (headingInput == 0 && pitchInput == 0) {
                return false;
            }

            //noinspection StringEquality
            if (target == AbstractViewInputHandler.GENERATE_EVENTS) {

                ViewInputAttributes.DeviceAttributes deviceAttributes =
                    inputHandler.getAttributes().getDeviceAttributes(ViewInputAttributes.DEVICE_KEYBOARD);

                onRotateView(headingInput, pitchInput, headingInput, pitchInput, deviceAttributes, viewAction);
            }
            return true;
        }
    }

    public class HorizontalTransActionListener extends ViewInputActionHandler {
        public boolean inputActionPerformed(AbstractViewInputHandler inputHandler, KeyEventState keys, String target,
            ViewInputAttributes.ActionAttributes viewAction) {
            double forwardInput = 0;
            double sideInput = 0;

            List keyList = viewAction.getKeyActions();
            for (Object k : keyList) {
                ViewInputAttributes.ActionAttributes.KeyAction keyAction =
                    (ViewInputAttributes.ActionAttributes.KeyAction) k;
                if (keys.isKeyDown(keyAction.keyCode)) {
                    if (keyAction.direction == ViewInputAttributes.ActionAttributes.KeyAction.KA_DIR_X) {
                        sideInput += keyAction.sign;
                    } else {
                        forwardInput += keyAction.sign;
                    }
                }
            }

            if (forwardInput == 0 && sideInput == 0) {
                return false;
            }

            //noinspection StringEquality
            if (target == AbstractViewInputHandler.GENERATE_EVENTS) {
                onHorizontalTranslateRel(forwardInput, sideInput, forwardInput, sideInput,
                    getAttributes().getDeviceAttributes(ViewInputAttributes.DEVICE_KEYBOARD), viewAction);
            }

            return true;
        }
    }

    //**************************************************************//
    //********************  Rendering Events  **********************//
    //**************************************************************//

    public class VerticalTransActionListener extends ViewInputActionHandler {
        public boolean inputActionPerformed(AbstractViewInputHandler inputHandler, KeyEventState keys, String target,
            ViewInputAttributes.ActionAttributes viewAction) {
            double transInput = 0;
            List keyList = viewAction.getKeyActions();
            for (Object k : keyList) {
                ViewInputAttributes.ActionAttributes.KeyAction keyAction =
                    (ViewInputAttributes.ActionAttributes.KeyAction) k;

                if (keys.isKeyDown(keyAction.keyCode))
                    transInput += keyAction.sign;
            }

            if (transInput == 0) {
                return false;
            }

            //noinspection StringEquality
            if (target == AbstractViewInputHandler.GENERATE_EVENTS) {

                ViewInputAttributes.DeviceAttributes deviceAttributes =
                    getAttributes().getDeviceAttributes(ViewInputAttributes.DEVICE_KEYBOARD);

                onVerticalTranslate(transInput, transInput, deviceAttributes, viewAction);
            }

            return true;
        }
    }

    public class RotateMouseActionListener extends ViewInputActionHandler {
        public boolean inputActionPerformed(KeyEventState keys, String target,
            ViewInputAttributes.ActionAttributes viewAction) {

            boolean handleThisEvent = false;
            List buttonList = viewAction.getMouseActions();
            for (Object b : buttonList) {
                ViewInputAttributes.ActionAttributes.MouseAction buttonAction =
                    (ViewInputAttributes.ActionAttributes.MouseAction) b;
                if ((keys.getMouseModifiersEx() & buttonAction.mouseButton) != 0) {
                    handleThisEvent = true;
                    break;
                }
            }
            if (!handleThisEvent) {
                return false;
            }

            Point point = AbstractViewInputHandler.constrainToSourceBounds(getMousePoint(), wwd());
            Point lastPoint = AbstractViewInputHandler.constrainToSourceBounds(getLastMousePoint(), wwd());
            Point mouseDownPoint = AbstractViewInputHandler.constrainToSourceBounds(getMouseDownPoint(), wwd());
            if (point == null || lastPoint == null) {
                return false;
            }

            Point movement = ViewUtil.subtract(point, lastPoint);
            int headingInput = movement.x;
            int pitchInput = movement.y;
            Point totalMovement = ViewUtil.subtract(point, mouseDownPoint);
            int totalHeadingInput = totalMovement.x;
            int totalPitchInput = totalMovement.y;

            ViewInputAttributes.DeviceAttributes deviceAttributes =
                getAttributes().getDeviceAttributes(ViewInputAttributes.DEVICE_MOUSE);

            onRotateView(headingInput, pitchInput, totalHeadingInput, totalPitchInput,
                deviceAttributes, viewAction);
            return true;
        }

        public boolean inputActionPerformed(AbstractViewInputHandler inputHandler,
            MouseEvent mouseEvent, ViewInputAttributes.ActionAttributes viewAction) {
            boolean handleThisEvent = false;
            List buttonList = viewAction.getMouseActions();
            for (Object b : buttonList) {
                ViewInputAttributes.ActionAttributes.MouseAction buttonAction =
                    (ViewInputAttributes.ActionAttributes.MouseAction) b;
                if ((mouseEvent.getModifiersEx() & buttonAction.mouseButton) != 0) {
                    handleThisEvent = true;
                }
            }
            if (!handleThisEvent) {
                return false;
            }
            Point point = AbstractViewInputHandler.constrainToSourceBounds(getMousePoint(), wwd());
            Point lastPoint = AbstractViewInputHandler.constrainToSourceBounds(getLastMousePoint(), wwd());
            Point mouseDownPoint = AbstractViewInputHandler.constrainToSourceBounds(getMouseDownPoint(), wwd());
            if (point == null || lastPoint == null) {
                return false;
            }

            Point movement = ViewUtil.subtract(point, lastPoint);
            int headingInput = movement.x;
            int pitchInput = movement.y;
            if (mouseDownPoint == null)
                mouseDownPoint = lastPoint;
            Point totalMovement = ViewUtil.subtract(point, mouseDownPoint);
            int totalHeadingInput = totalMovement.x;
            int totalPitchInput = totalMovement.y;

            ViewInputAttributes.DeviceAttributes deviceAttributes =
                getAttributes().getDeviceAttributes(ViewInputAttributes.DEVICE_MOUSE);

            onRotateView(headingInput, pitchInput, totalHeadingInput, totalPitchInput,
                deviceAttributes, viewAction);
            return true;
        }
    }

    public class HorizTransMouseActionListener extends ViewInputActionHandler {
        public boolean inputActionPerformed(
            KeyEventState keys, String target, ViewInputAttributes.ActionAttributes viewAction) {

            boolean handleThisEvent = false;
            List buttonList = viewAction.getMouseActions();
            for (Object b : buttonList) {
                ViewInputAttributes.ActionAttributes.MouseAction buttonAction =
                    (ViewInputAttributes.ActionAttributes.MouseAction) b;
                if ((keys.getMouseModifiersEx() & buttonAction.mouseButton) != 0) {
                    handleThisEvent = true;
                    break;
                }
            }
            if (!handleThisEvent) {
                return false;
            }
            if (target == AbstractViewInputHandler.GENERATE_EVENTS) {
                Point point = AbstractViewInputHandler.constrainToSourceBounds(getMousePoint(), wwd());
                Point lastPoint = AbstractViewInputHandler.constrainToSourceBounds(getLastMousePoint(), wwd());
                Point mouseDownPoint = AbstractViewInputHandler.constrainToSourceBounds(getMouseDownPoint(), wwd());

                Point movement = ViewUtil.subtract(point, lastPoint);
                if (point == null || lastPoint == null)
                    return false;
                int forwardInput = movement.y;
                int sideInput = -movement.x;

                Point totalMovement = ViewUtil.subtract(point, mouseDownPoint);
                int totalForward = totalMovement.y;
                int totalSide = -totalMovement.x;

                ViewInputAttributes.DeviceAttributes deviceAttributes =
                    getAttributes().getDeviceAttributes(ViewInputAttributes.DEVICE_MOUSE);

                onHorizontalTranslateRel(forwardInput, sideInput, totalForward, totalSide, deviceAttributes,
                    viewAction);
            }

            return (true);
        }

        public boolean inputActionPerformed(AbstractViewInputHandler inputHandler,
            MouseEvent mouseEvent, ViewInputAttributes.ActionAttributes viewAction) {
            boolean handleThisEvent = false;
            List buttonList = viewAction.getMouseActions();
            for (Object b : buttonList) {
                ViewInputAttributes.ActionAttributes.MouseAction buttonAction =
                    (ViewInputAttributes.ActionAttributes.MouseAction) b;
                if ((mouseEvent.getModifiersEx() & buttonAction.mouseButton) != 0) {
                    handleThisEvent = true;
                }
            }
            if (!handleThisEvent) {
                return false;
            }
            Point point = AbstractViewInputHandler.constrainToSourceBounds(getMousePoint(), wwd());
            Point lastPoint = AbstractViewInputHandler.constrainToSourceBounds(getLastMousePoint(), wwd());
            Point mouseDownPoint = AbstractViewInputHandler.constrainToSourceBounds(getMouseDownPoint(), wwd());
            if (point == null || lastPoint == null || mouseDownPoint == null) {
                return (false);
            }
            Point movement = ViewUtil.subtract(point, lastPoint);
            int forwardInput = movement.y;
            int sideInput = -movement.x;

            Point totalMovement = ViewUtil.subtract(point, mouseDownPoint);
            int totalForward = totalMovement.y;
            int totalSide = -totalMovement.x;

            ViewInputAttributes.DeviceAttributes deviceAttributes =
                getAttributes().getDeviceAttributes(ViewInputAttributes.DEVICE_MOUSE);

            onHorizontalTranslateRel(forwardInput, sideInput, totalForward, totalSide, deviceAttributes,
                viewAction);

            return (true);
        }
    }

    public class VertTransMouseActionListener extends ViewInputActionHandler {
        public boolean inputActionPerformed(AbstractViewInputHandler inputHandler,
            MouseEvent mouseEvent, ViewInputAttributes.ActionAttributes viewAction) {
            boolean handleThisEvent = false;
            List buttonList = viewAction.getMouseActions();
            for (Object b : buttonList) {
                ViewInputAttributes.ActionAttributes.MouseAction buttonAction =
                    (ViewInputAttributes.ActionAttributes.MouseAction) b;
                if ((mouseEvent.getModifiersEx() & buttonAction.mouseButton) != 0) {
                    handleThisEvent = true;
                }
            }
            if (!handleThisEvent) {
                return false;
            }

            Point point = AbstractViewInputHandler.constrainToSourceBounds(getMousePoint(), wwd());
            Point lastPoint = AbstractViewInputHandler.constrainToSourceBounds(getLastMousePoint(), wwd());
            Point mouseDownPoint = AbstractViewInputHandler.constrainToSourceBounds(getMouseDownPoint(), wwd());
            if (point == null || lastPoint == null || mouseDownPoint == null) {
                return false;
            }

            Point movement = ViewUtil.subtract(point, lastPoint);
            int translationInput = movement.y;
            Point totalMovement = ViewUtil.subtract(point, mouseDownPoint);
            int totalTranslationInput = totalMovement.y;

            ViewInputAttributes.DeviceAttributes deviceAttributes =
                getAttributes().getDeviceAttributes(ViewInputAttributes.DEVICE_MOUSE);
            onVerticalTranslate(translationInput, totalTranslationInput, deviceAttributes, viewAction);

            return true;
        }

        public boolean inputActionPerformed(
            KeyEventState keys, String target, ViewInputAttributes.ActionAttributes viewAction) {
            boolean handleThisEvent = false;
            List buttonList = viewAction.getMouseActions();
            for (Object b : buttonList) {
                ViewInputAttributes.ActionAttributes.MouseAction buttonAction =
                    (ViewInputAttributes.ActionAttributes.MouseAction) b;
                if ((keys.getMouseModifiersEx() & buttonAction.mouseButton) != 0) {
                    handleThisEvent = true;
                    break;
                }
            }
            if (!handleThisEvent) {
                return false;
            }

            Point point = AbstractViewInputHandler.constrainToSourceBounds(getMousePoint(), wwd());
            Point lastPoint = AbstractViewInputHandler.constrainToSourceBounds(getLastMousePoint(), wwd());
            Point mouseDownPoint = AbstractViewInputHandler.constrainToSourceBounds(getMouseDownPoint(), wwd());
            if (point == null || lastPoint == null) {
                return false;
            }

            Point movement = ViewUtil.subtract(point, lastPoint);
            int translationInput = movement.y;
            Point totalMovement = ViewUtil.subtract(point, mouseDownPoint);
            int totalTranslationInput = totalMovement.y;

            ViewInputAttributes.DeviceAttributes deviceAttributes =
                getAttributes().getDeviceAttributes(ViewInputAttributes.DEVICE_MOUSE);
            onVerticalTranslate(translationInput, totalTranslationInput, deviceAttributes, viewAction);

            return true;
        }
    }

    public class MoveToMouseActionListener extends ViewInputActionHandler {
        public boolean inputActionPerformed(AbstractViewInputHandler inputHandler,
            MouseEvent mouseEvent, ViewInputAttributes.ActionAttributes viewAction) {
            boolean handleThisEvent = false;
            List buttonList = viewAction.getMouseActions();
            for (Object b : buttonList) {
                ViewInputAttributes.ActionAttributes.MouseAction buttonAction =
                    (ViewInputAttributes.ActionAttributes.MouseAction) b;
                if ((mouseEvent.getButton() == buttonAction.mouseButton)) {
                    handleThisEvent = true;
                }
            }
            if (!handleThisEvent) {
                return false;
            }
            Position pos = computeSelectedPosition();
            if (pos == null) {
                return false;
            }

            onMoveTo(pos, getAttributes().getDeviceAttributes(ViewInputAttributes.DEVICE_MOUSE), viewAction);
            return true;
        }
    }

    public class ResetHeadingActionListener extends ViewInputActionHandler {
        public boolean inputActionPerformed(AbstractViewInputHandler inputHandler, KeyEvent event,
            ViewInputAttributes.ActionAttributes viewAction) {
            List keyList = viewAction.getKeyActions();
            for (Object k : keyList) {
                ViewInputAttributes.ActionAttributes.KeyAction keyAction =
                    (ViewInputAttributes.ActionAttributes.KeyAction) k;
                if (event.getKeyCode() == keyAction.keyCode) {
                    onResetHeading(viewAction);
                    return true;
                }
            }
            return false;
        }
    }

    public class ResetHeadingPitchActionListener extends ViewInputActionHandler {
        public boolean inputActionPerformed(AbstractViewInputHandler inputHandler, KeyEvent event,
            ViewInputAttributes.ActionAttributes viewAction) {
            List keyList = viewAction.getKeyActions();
            for (Object k : keyList) {
                ViewInputAttributes.ActionAttributes.KeyAction keyAction =
                    (ViewInputAttributes.ActionAttributes.KeyAction) k;
                if (event.getKeyCode() == keyAction.keyCode) {
                    onResetHeadingPitchRoll(viewAction);
                    return true;
                }
            }
            return false;
        }
    }

    //**************************************************************//
    //********************  Property Change Events  ****************//
    //**************************************************************//

    public class StopViewActionListener extends ViewInputActionHandler {
        public boolean inputActionPerformed(AbstractViewInputHandler inputHandler, KeyEvent event,
            ViewInputAttributes.ActionAttributes viewAction) {
            List keyList = viewAction.getKeyActions();
            for (Object k : keyList) {
                ViewInputAttributes.ActionAttributes.KeyAction keyAction =
                    (ViewInputAttributes.ActionAttributes.KeyAction) k;
                if (event.getKeyCode() == keyAction.keyCode) {
                    onStopView();
                    return true;
                }
            }
            return false;
        }
    }

    public class VertTransMouseWheelActionListener extends ViewInputActionHandler {
        public boolean inputActionPerformed(AbstractViewInputHandler inputHandler,
            MouseWheelEvent mouseWheelEvent, ViewInputAttributes.ActionAttributes viewAction) {

            double zoomInput = mouseWheelEvent.getWheelRotation();

            ViewInputAttributes.DeviceAttributes deviceAttributes =
                getAttributes().getDeviceAttributes(ViewInputAttributes.DEVICE_MOUSE_WHEEL);

            onVerticalTranslate(zoomInput, zoomInput, deviceAttributes, viewAction);
            return true;
        }
    }
}
