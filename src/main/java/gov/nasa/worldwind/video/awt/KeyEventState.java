/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.video.awt;

import gov.nasa.worldwind.util.Logging;

import java.awt.event.*;
import java.util.*;

/**
 * @author dcollins
 * @version $Id: KeyEventState.java 2193 2014-08-01 23:33:16Z dcollins $
 */
public class KeyEventState implements KeyListener, MouseListener {

    private final Map<Object, InputState> keyStateMap = new HashMap<>();
    private int modifiersEx;
    private int mouseModifiersEx;

    public KeyEventState() {
    }

    private static long getTimeStamp(InputEvent e, int eventType, InputState currentState) {
        // If the current state for this input event type exists and is not null, then keep the current timestamp.
        if (currentState != null && currentState.eventType == eventType) {
            return currentState.timestamp;
        } else
            return e.getWhen(); // Otherwise return the InputEvent's timestamp.
    }

    public boolean isKeyDown(int keyCode) {
        InputState state = this.getKeyState(keyCode);
        return state != null && state.eventType == KeyEvent.KEY_PRESSED;
    }

    public int keyState(int keyCode) {
        InputState state = this.getKeyState(keyCode);
        return state != null && state.eventType == KeyEvent.KEY_PRESSED ? 1 : 0;
    }

    public int getNumKeysDown() {
        if (keyStateMap.isEmpty()) {
            return (0);
        }
        int numKeys = 0;
        for (InputState is : this.keyStateMap.values()) {
            //Integer key = (KeyEvent) o;
            if (is.eventType == KeyEvent.KEY_PRESSED) {
                numKeys++;
            }
        }
        return (numKeys);
    }

    public int getNumButtonsDown() {
        if (keyStateMap.isEmpty()) {
            return (0);
        }
        int numKeys = 0;
        for (InputState is : this.keyStateMap.values()) {
            if (is.eventType == MouseEvent.MOUSE_PRESSED) {
                numKeys++;
            }
        }
        return (numKeys);
    }

    /**
     * @return The same value as {@link #getModifiersEx()}.
     * @deprecated Use {@link #getModifiersEx()} instead
     */
    @Deprecated
    public int getModifiers() {
        String msg = Logging.getMessage("generic.OperationDeprecatedAndChanged", "getModifiers", "getModifiersEx");
        Logging.logger().severe(msg);
        return this.modifiersEx;
    }

    /**
     * @param modifiers Unused.
     * @deprecated Use {@link #setModifiersEx(int)} instead
     */
    @Deprecated
    protected static void setModifiers(int modifiers) {
        String msg = Logging.getMessage("generic.OperationDeprecatedAndChanged", "setModifiers", "setModifiersEx");
        Logging.logger().severe(msg);
    }

    /**
     * @return The extended event modifiers.
     */
    public int getModifiersEx() {
        return this.modifiersEx;
    }

    private void setModifiersEx(int modifiersEx) {
        this.modifiersEx = modifiersEx;
    }

    /**
     * @return The same value as {@link #getMouseModifiersEx()}.
     * @deprecated Use {@link #getMouseModifiersEx()} instead
     */
    @Deprecated
    public int getMouseModifiers() {
        String msg = Logging.getMessage("generic.OperationDeprecatedAndChanged", "getMouseModifiers",
            "getMouseModifiersEx");
        Logging.logger().severe(msg);
        return this.mouseModifiersEx;
    }

    /**
     * @param modifiers Unused.
     * @deprecated Use {@link #setMouseModifiersEx(int)} instead
     */
    @Deprecated
    protected static void setMouseModifiers(int modifiers) {
        String msg = Logging.getMessage("generic.OperationDeprecatedAndChanged", "setMouseModifiers",
            "setMouseModifiersEx");
        Logging.logger().severe(msg);
    }

    /**
     * @return The extended mouse event modifiers.
     */
    public int getMouseModifiersEx() {
        return this.mouseModifiersEx;
    }

    private void setMouseModifiersEx(int modifiersEx) {
        this.mouseModifiersEx = modifiersEx;
    }

    public void clearKeyState() {
        this.keyStateMap.clear();
        this.modifiersEx = 0;
        this.mouseModifiersEx = 0;
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    @Override
    public void keyPressed(KeyEvent e) {
        this.onKeyEvent(e, KeyEvent.KEY_PRESSED);
    }

    @Override
    public void keyReleased(KeyEvent e) {
        this.removeKeyState(e);
    }

    private void onKeyEvent(KeyEvent e, int eventType) {
        if (e == null) {
            return;
        }

        long timestamp = KeyEventState.getTimeStamp(e, eventType, this.keyStateMap.get(e.getKeyCode()));
        this.setKeyState(e.getKeyCode(), new InputState(eventType, e.getKeyCode(), timestamp));
        this.setModifiersEx(e.getModifiersEx());
    }

    @Override
    public void mouseClicked(MouseEvent mouseEvent) {
    }

    @Override
    public void mousePressed(MouseEvent e) {
        long timestamp = KeyEventState.getTimeStamp(e, MouseEvent.MOUSE_PRESSED,
            this.keyStateMap.get(e.getModifiersEx()));
        this.setKeyState(e.getButton(), new InputState(MouseEvent.MOUSE_PRESSED, e.getButton(), timestamp));
        this.setMouseModifiersEx(e.getModifiersEx());
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        this.keyStateMap.remove(e.getButton());
        this.setMouseModifiersEx(0);
    }

    @Override
    public void mouseEntered(MouseEvent mouseEvent) {

    }

    @Override
    public void mouseExited(MouseEvent mouseEvent) {

    }

    private InputState getKeyState(int keyCode) {
        return this.keyStateMap.get(keyCode);
    }

    private void setKeyState(int keyCode, InputState state) {
        this.keyStateMap.put(keyCode, state);
    }

    private void removeKeyState(KeyEvent e) {
        this.keyStateMap.remove(e.getKeyCode());
        this.setModifiersEx(e.getModifiersEx());
    }

    protected static class InputState {

        final int eventType;
        final int keyOrButtonCode;
        final long timestamp;

        InputState(int eventType, int keyOrButtonCode, long timestamp) {
            this.eventType = eventType;
            this.keyOrButtonCode = keyOrButtonCode;
            this.timestamp = timestamp;
        }
    }
}
