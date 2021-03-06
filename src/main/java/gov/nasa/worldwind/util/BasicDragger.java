/*
 * Copyright (C) 2016 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */

package gov.nasa.worldwind.util;

import gov.nasa.worldwind.*;
import gov.nasa.worldwind.drag.*;
import gov.nasa.worldwind.event.*;
import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.globes.Globe;

import java.awt.*;

/**
 * Interprets mouse input via the {@link DragSelectEvent} for notifying picked objects implementing the {@link
 * Draggable} interface. This version uses the {@link Draggable} interface for dragging but retains the original
 * behavior of the BasicDragger when the {@link gov.nasa.worldwind.pick.PickedObject} implements either the {@link
 * Movable} or {@link Movable2} interface.
 * <p>
 * For objects not yet implementing the {@link Draggable} interface the legacy dragging functionality will be used.
 */
public class BasicDragger implements SelectListener {
    /**
     * The {@link WorldWindow} this dragger will utilize for the {@link Globe}, {@link View}, and {@link
     * SceneController} objects.
     */
    protected final WorldWindow wwd;
    /**
     * Indicates if the dragger is currently dragging.
     */
    protected boolean dragging;
    /**
     * The {@link DragContext} for dragging operations. Initialized on {@link Keys#DRAG_BEGIN}.
     */
    protected DragContext dragContext;
    //////////////////////////////////////////////////////////
    // Legacy Properties
    //////////////////////////////////////////////////////////
    protected Vec4 dragRefObjectPoint;
    protected Point dragRefCursorPoint;
    protected double dragRefAltitude;

    /**
     * Creates a dragging controller which converts {@link SelectEvent}s to the {@link Draggable} interface.
     *
     * @param wwd the {@link WorldWindow} this drag controller should be associated with.
     * @throws IllegalArgumentException if the provided {@link WorldWindow} is null.
     */
    public BasicDragger(WorldWindow wwd) {
        this.wwd = wwd;
    }



    /**
     * Returns if the dragger is currently executing a dragging operation.
     *
     * @return <code>true</code> if a drag operation is executing.
     */
    public boolean isDragging() {
        return this.dragging;
    }

    @Override
    public void accept(SelectEvent event) {

        if (event.getEventAction().equals(SelectEvent.DRAG_END)) {
            this.dragContext.setDragState(Keys.DRAG_ENDED);
            this.fireDrag((DragSelectEvent) event);
            this.dragContext = null;
            this.dragging = false;
        } else if (event.getEventAction().equals(SelectEvent.DRAG)) {

            if (this.dragContext == null)
                this.dragContext = new DragContext();

            this.dragContext.setPoint(event.pickPoint);
            this.dragContext.setPreviousPoint(((DragSelectEvent) event).getPreviousPickPoint());
            this.dragContext.setView(this.wwd.view());
            this.dragContext.setGlobe(this.wwd.model().globe());
            this.dragContext.setSceneController(this.wwd.sceneControl());

            if (this.dragging) {
                this.dragContext.setDragState(Keys.DRAG_CHANGE);
            } else {
                this.dragContext.setDragState(Keys.DRAG_BEGIN);
                this.dragContext.setInitialPoint(((DragSelectEvent) event).getPreviousPickPoint());
                this.dragging = true;
            }
            this.fireDrag((DragSelectEvent) event);
        }

        event.consume();
    }

    /**
     * Propagates the {@link DragContext} to the picked object if it implements {@link Draggable}, {@link Movable}, or
     * {@link Movable2}.
     *
     * @param dragEvent the {@link DragContext} to deliver to the selected object.
     * @throws IllegalArgumentException if the {@link DragContext} is null.
     */
    protected void fireDrag(DragSelectEvent dragEvent) {

        Object dragObject = dragEvent.getTopObject();

        if (dragObject instanceof Draggable) {
            ((Draggable) dragObject).drag(this.dragContext);
        }

        if ((dragObject instanceof Movable2) || (dragObject instanceof Movable)) {
            // Utilize the existing behavior
            this.dragLegacy(dragEvent);
        }
    }

    /**
     * Legacy drag approach, provided for objects not yet implementing the {@link Draggable} interface.
     *
     * @param event the current {@link SelectEvent}.
     */
    @Deprecated
    protected void dragLegacy(SelectEvent event) {

        DragSelectEvent dragEvent = (DragSelectEvent) event;
        Object dragObject = dragEvent.getTopObject();
        if (dragObject == null)
            return;

        View view = wwd.view();
        Globe globe = wwd.model().globe();

        // Compute dragged object ref-point in model coordinates.
        // Use the Icon and Annotation logic of elevation as offset above ground when below max elevation.
        Position refPos = null;
        if (dragObject instanceof Movable2)
            refPos = ((Movable2) dragObject).getReferencePosition();
        else if (dragObject instanceof Movable)
            refPos = ((Movable) dragObject).getReferencePosition();
        if (refPos == null)
            return;

        Vec4 refPoint = globe.computePointFromPosition(refPos);

        if (this.dragContext.getDragState().equals(Keys.DRAG_BEGIN))   // Dragging started
        {
            // Save initial reference points for object and cursor in screen coordinates
            // Note: y is inverted for the object point.
            this.dragRefObjectPoint = view.project(refPoint);
            // Save cursor position
            this.dragRefCursorPoint = dragEvent.getPreviousPickPoint();
            // Save start altitude
            this.dragRefAltitude = globe.computePositionFromPoint(refPoint).getElevation();
        }

        // Compute screen-coord delta since drag started.
        if (dragEvent.pickPoint==null)
            return;

        int dx = dragEvent.pickPoint.x - this.dragRefCursorPoint.x;
        int dy = dragEvent.pickPoint.y - this.dragRefCursorPoint.y;

        // Find intersection of screen coord (refObjectPoint + delta) with globe.
        double x = this.dragRefObjectPoint.x + dx;
        double y = event.mouseEvent.getComponent().getSize().height - this.dragRefObjectPoint.y + dy - 1;
        Line ray = view.computeRayFromScreenPoint(x, y);
        Position pickPos = null;
        // Use intersection with sphere at reference altitude.
        Intersection[] inters = globe.intersect(ray, this.dragRefAltitude);
        if (inters != null)
            pickPos = globe.computePositionFromPoint(inters[0].getIntersectionPoint());

        if (pickPos != null) {
            // Intersection with globe. Move reference point to the intersection point,
            // but maintain current altitude.
            Position p = new Position(pickPos, refPos.getElevation());
            if (dragObject instanceof Movable2)
                ((Movable2) dragObject).moveTo(globe, p);
            else
                ((Movable) dragObject).moveTo(p);
        }
    }
}