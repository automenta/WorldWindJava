/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.event;

import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.avlist.KV;

import java.awt.event.*;
import java.beans.*;
import java.util.*;

/**
 * @author tag
 * @version $Id: InputHandler.java 1171 2013-02-11 21:45:02Z dcollins $
 */
public interface InputHandler
    extends KV, PropertyChangeListener, com.jogamp.newt.event.KeyListener, com.jogamp.newt.event.MouseListener, com.jogamp.newt.event.WindowListener {
    WorldWindow getEventSource();

    void setEventSource(WorldWindow newWorldWindow);

    int getHoverDelay();

    void setHoverDelay(int delay);

    void addSelectListener(SelectListener listener);

    void removeSelectListener(SelectListener listener);

    void addKeyListener(KeyListener listener);

    void removeKeyListener(KeyListener listener);

    void addMouseListener(MouseListener listener);

    void removeMouseListener(MouseListener listener);

    void addMouseMotionListener(MouseMotionListener listener);

    void removeMouseMotionListener(MouseMotionListener listener);

    void addMouseWheelListener(MouseWheelListener listener);

    void removeMouseWheelListener(MouseWheelListener listener);

    void dispose();

    /**
     * Indicates whether a redraw is forced when the a mouse button is pressed. Touch screen devices require this so
     * that the current position and selection are updated when the button is pressed. The update occurs naturally on
     * non-touch screen devices because the motion of the mouse prior to the press causes the current position and
     * selection to be updated.
     *
     * @return true if a redraw is forced when a button is pressed, otherwise false.
     */
    boolean isForceRedrawOnMousePressed();

    /**
     * Specifies whether a redraw is forced when the a mouse button is pressed. Touch screen devices require this so
     * that the current position and selection are updated when the button is pressed. The update occurs naturally on
     * non-touch screen devices because the motion of the mouse prior to the press causes the current position and
     * selection to be updated.
     *
     * @param forceRedrawOnMousePressed true to force a redraw on button press, otherwise false, the default.
     */
    void setForceRedrawOnMousePressed(boolean forceRedrawOnMousePressed);

    @Override
    void mouseClicked(com.jogamp.newt.event.MouseEvent e);

    @Override
    void mouseEntered(com.jogamp.newt.event.MouseEvent e);

    @Override
    void mouseExited(com.jogamp.newt.event.MouseEvent e);

    @Override
    void mousePressed(com.jogamp.newt.event.MouseEvent e);

    @Override
    void mouseReleased(com.jogamp.newt.event.MouseEvent e);

    @Override
    void mouseMoved(com.jogamp.newt.event.MouseEvent e);

    @Override
    void mouseDragged(com.jogamp.newt.event.MouseEvent e);

    @Override
    void mouseWheelMoved(com.jogamp.newt.event.MouseEvent e);

    @Override
    Object set(String key, Object value);

    @Override
    Object get(String key);

    @Override
    Iterable<Object> getValues();

    @Override
    KV setValues(KV avList);

    @Override
    String getStringValue(String key);

    @Override
    Set<Map.Entry<String, Object>> getEntries();

    @Override
    boolean hasKey(String key);

    @Override
    Object removeKey(String key);

    @Override
    void addPropertyChangeListener(String propertyName, PropertyChangeListener listener);

    @Override
    void removePropertyChangeListener(String propertyName, PropertyChangeListener listener);

    @Override
    void addPropertyChangeListener(PropertyChangeListener listener);

    @Override
    void removePropertyChangeListener(PropertyChangeListener listener);

    @Override
    void firePropertyChange(String propertyName, Object oldValue, Object newValue);

    @Override
    void firePropertyChange(PropertyChangeEvent propertyChangeEvent);

    @Override
    KV copy();

    @Override
    KV clearList();

    @Override
    void propertyChange(PropertyChangeEvent evt);
}