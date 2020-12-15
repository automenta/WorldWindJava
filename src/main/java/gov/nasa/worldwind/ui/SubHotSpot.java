/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */

package gov.nasa.worldwind.ui;

import gov.nasa.worldwind.event.SelectEvent;

import java.awt.event.*;

/**
 * An area that can receive select and mouse events. The parent's
 * default behavior is to forward events to its parent HotSpot. Subclasses must override methods for events they can
 * react to, and all other events are handled by the parent.
 *
 * @author pabercrombie
 */
public class SubHotSpot extends AbstractHotSpot {
    /**
     * The parent HotSpot, or null if this has no parent.
     */
    protected final HotSpot parent;

    /**
     * Create a hot spot.
     *
     * @param parent The screen area that contains this hot spot. Input events that cannot be handled by this object
     *               will be passed to the parent. May be null.
     */
    public SubHotSpot(HotSpot parent) {
        this.parent = parent;
    }

    /**
     * Forwards the event to the parent HotSpot if the parent is non-null. Otherwise does nothing. Override this method
     * to handle key released events.
     *
     * @param event The event to handle.
     */
    public void accept(SelectEvent event) {
        if (event == null || AbstractHotSpot.isConsumed(event))
            return;

        if (this.parent != null)
            this.parent.accept(event);
    }

    /**
     * Forwards the event to the parent HotSpot if the parent is non-null. Otherwise does nothing. Override this method
     * to handle mouse click events.
     *
     * @param event The event to handle.
     */
    public void mouseClicked(MouseEvent event) {
        if (event == null || event.isConsumed())
            return;

        if (this.parent != null)
            this.parent.mouseClicked(event);
    }

    /**
     * Forwards the event to the parent HotSpot if the parent is non-null. Otherwise does nothing. Override this method
     * to handle mouse pressed events.
     *
     * @param event The event to handle.
     */
    public void mousePressed(MouseEvent event) {
        if (event == null || event.isConsumed())
            return;

        if (this.parent != null)
            this.parent.mousePressed(event);
    }

    /**
     * Forwards the event to the parent HotSpot if the parent is non-null. Otherwise does nothing. Override this method
     * to handle mouse released events.
     *
     * @param event The event to handle.
     */
    public void mouseReleased(MouseEvent event) {
        if (event == null || event.isConsumed())
            return;

        if (this.parent != null)
            this.parent.mouseReleased(event);
    }

    /**
     * Forwards the event to the parent HotSpot if the parent is non-null. Otherwise does nothing. Override this method
     * to handle mouse wheel events.
     *
     * @param event The event to handle.
     */
    public void mouseWheelMoved(MouseWheelEvent event) {
        if (event == null || event.isConsumed())
            return;

        if (this.parent != null)
            this.parent.mouseWheelMoved(event);
    }
}
