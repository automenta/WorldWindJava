/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */

package gov.nasa.worldwind.event;

import com.jogamp.newt.event.WindowUpdateEvent;
import gov.nasa.worldwind.*;

import java.awt.event.*;

/**
 * Provides an input handler that does nothing. Meant to serve as a NULL assignment that can be invoked.
 *
 * @author tag
 * @version $Id: NoOpInputHandler.java 1171 2013-02-11 21:45:02Z dcollins $
 */
public class NoOpInputHandler extends WWObjectImpl implements InputHandler {
    public WorldWindow getEventSource() {
        return null;
    }

    public void setEventSource(WorldWindow newWorldWindow) {
    }

    public int getHoverDelay() {
        return 0;
    }

    public void setHoverDelay(int delay) {
    }

    public void addSelectListener(SelectListener listener) {
    }

    public void removeSelectListener(SelectListener listener) {
    }

    public void addKeyListener(KeyListener listener) {
    }

    public void removeKeyListener(KeyListener listener) {
    }

    public void addMouseListener(MouseListener listener) {
    }

    public void removeMouseListener(MouseListener listener) {
    }

    public void addMouseMotionListener(MouseMotionListener listener) {
    }

    public void removeMouseMotionListener(MouseMotionListener listener) {
    }

    public void addMouseWheelListener(MouseWheelListener listener) {
    }

    public void removeMouseWheelListener(MouseWheelListener listener) {
    }

    public void dispose() {
    }

    public boolean isForceRedrawOnMousePressed() {
        return false;
    }

    public void setForceRedrawOnMousePressed(boolean forceRedrawOnMousePressed) {
    }

    @Override
    public void mouseClicked(com.jogamp.newt.event.MouseEvent e) {

    }

    @Override
    public void mouseEntered(com.jogamp.newt.event.MouseEvent e) {

    }

    @Override
    public void mouseExited(com.jogamp.newt.event.MouseEvent e) {

    }

    @Override
    public void mousePressed(com.jogamp.newt.event.MouseEvent e) {

    }

    @Override
    public void mouseReleased(com.jogamp.newt.event.MouseEvent e) {

    }

    @Override
    public void mouseMoved(com.jogamp.newt.event.MouseEvent e) {

    }

    @Override
    public void mouseDragged(com.jogamp.newt.event.MouseEvent e) {

    }

    @Override
    public void mouseWheelMoved(com.jogamp.newt.event.MouseEvent e) {

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

    }

    @Override
    public void windowLostFocus(com.jogamp.newt.event.WindowEvent e) {

    }

    @Override
    public void windowRepaint(WindowUpdateEvent e) {

    }

    @Override
    public void keyPressed(com.jogamp.newt.event.KeyEvent e) {

    }

    @Override
    public void keyReleased(com.jogamp.newt.event.KeyEvent e) {

    }
}
