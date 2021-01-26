/*
 * Copyright (C) 2016 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */

package gov.nasa.worldwind.drag;

/**
 * An interface provided by objects that can be dragged. The {@link DragContext} provided in the {@link
 * Draggable#drag(DragContext)} method includes information on the screen coordinates and the state of the {@link
 * gov.nasa.worldwind.WorldWindow}.
 */
public interface Draggable {
    /**
     * Indicates whether the object is enabled for dragging.
     *
     * @return true if the object is enabled, else false.
     */
    boolean isDragEnabled();



    /**
     * Drag the object given the provided {@link DragContext}.
     *
     * @param dragContext the {@link DragContext} of this dragging event.
     */
    void drag(DragContext dragContext);
}