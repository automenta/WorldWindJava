/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */

package gov.nasa.worldwind.ui;

import gov.nasa.worldwind.Keys;
import gov.nasa.worldwind.event.SelectEvent;

import java.awt.*;
import java.awt.event.*;

/**
 * A HotSpot for resizing a frame or window. This class handles the resize input events, but does does not actually draw
 * the resize controls. The HotSpot is defined by a direction, for example, {@link Keys#NORTH} indicates that the
 * HotSpot resizes the frame vertically from the north edge (the user clicks the top edge of the frame and drags
 * vertically).
 * <p>
 * An instance of this class should be added to the picked object when the edge or corner of the frame is picked.
 * <p>
 * Subclasses must the implement {#getSize}, {#setSize}, {#getScreenPoint}, and {#setScreenPoint} to manipulate the
 * frame that they want to resize.
 *
 * @author pabercrombie
 * @version $Id: AbstractResizeHotSpot.java 1171 2013-02-11 21:45:02Z dcollins $
 */
public abstract class AbstractResizeHotSpot extends AbstractHotSpot {
    protected static final int NORTH = 1;
    protected static final int SOUTH = 2;
    protected static final int EAST = 4;
    protected static final int WEST = 8;
    protected static final int NORTHWEST = AbstractResizeHotSpot.NORTH + AbstractResizeHotSpot.WEST;
    protected static final int NORTHEAST = AbstractResizeHotSpot.NORTH + AbstractResizeHotSpot.EAST;
    protected static final int SOUTHWEST = AbstractResizeHotSpot.SOUTH + AbstractResizeHotSpot.WEST;
    protected static final int SOUTHEAST = AbstractResizeHotSpot.SOUTH + AbstractResizeHotSpot.EAST;

    protected boolean dragging;

    protected Point dragRefPoint;
    protected Dimension refSize;
    protected Point refLocation;

    protected boolean allowVerticalResize = true;
    protected boolean allowHorizontalResize = true;

    /**
     * True if the window needs to be moved in the X direction as it is resized. For example, if the upper left corner
     * is being dragged, the window should move to keep that corner under the cursor.
     */
    protected boolean adjustLocationX;
    /**
     * True if the window needs to be moved in the Y direction as it is resized.
     */
    protected boolean adjustLocationY;

    protected int xSign = 1;
    protected int ySign = 1;

    protected int cursor;

    protected void setDirection(String direction) {
        int dir = 0;
        if (Keys.NORTH.equals(direction))
            dir = AbstractResizeHotSpot.NORTH;
        else if (Keys.SOUTH.equals(direction))
            dir = AbstractResizeHotSpot.SOUTH;
        else if (Keys.EAST.equals(direction))
            dir = AbstractResizeHotSpot.EAST;
        else if (Keys.WEST.equals(direction))
            dir = AbstractResizeHotSpot.WEST;
        else if (Keys.NORTHEAST.equals(direction))
            dir = AbstractResizeHotSpot.NORTHEAST;
        else if (Keys.NORTHWEST.equals(direction))
            dir = AbstractResizeHotSpot.NORTHWEST;
        else if (Keys.SOUTHEAST.equals(direction))
            dir = AbstractResizeHotSpot.SOUTHEAST;
        else if (Keys.SOUTHWEST.equals(direction))
            dir = AbstractResizeHotSpot.SOUTHWEST;

        this.setDirection(dir);
    }

    protected void setDirection(int direction) {
        this.adjustLocationX =
            AbstractResizeHotSpot.NORTH == direction
                || AbstractResizeHotSpot.WEST == direction
                || AbstractResizeHotSpot.SOUTHWEST == direction
                || AbstractResizeHotSpot.NORTHWEST == direction;
        this.adjustLocationY =
            AbstractResizeHotSpot.NORTH == direction
                || AbstractResizeHotSpot.WEST == direction
                || AbstractResizeHotSpot.NORTHWEST == direction
                || AbstractResizeHotSpot.NORTHEAST == direction;

        if (AbstractResizeHotSpot.NORTH == direction || AbstractResizeHotSpot.SOUTH == direction) {
            this.allowVerticalResize = true;
            this.allowHorizontalResize = false;
        } else if (AbstractResizeHotSpot.EAST == direction || AbstractResizeHotSpot.WEST == direction) {
            this.allowVerticalResize = false;
            this.allowHorizontalResize = true;
        } else {
            this.allowVerticalResize = true;
            this.allowHorizontalResize = true;
        }

        if (AbstractResizeHotSpot.WEST == direction || AbstractResizeHotSpot.SOUTHWEST == direction || AbstractResizeHotSpot.NORTHWEST == direction)
            this.xSign = -1;
        else
            this.xSign = 1;

        if (AbstractResizeHotSpot.NORTH == direction || AbstractResizeHotSpot.NORTHEAST == direction || AbstractResizeHotSpot.NORTHWEST == direction)
            this.ySign = -1;
        else
            this.ySign = 1;

        if (AbstractResizeHotSpot.NORTH == direction)
            this.cursor = Cursor.N_RESIZE_CURSOR;
        else if (AbstractResizeHotSpot.SOUTH == direction)
            this.cursor = Cursor.S_RESIZE_CURSOR;
        else if (AbstractResizeHotSpot.EAST == direction)
            this.cursor = Cursor.E_RESIZE_CURSOR;
        else if (AbstractResizeHotSpot.WEST == direction)
            this.cursor = Cursor.W_RESIZE_CURSOR;
        else if (AbstractResizeHotSpot.NORTHEAST == direction)
            this.cursor = Cursor.NE_RESIZE_CURSOR;
        else if (AbstractResizeHotSpot.SOUTHEAST == direction)
            this.cursor = Cursor.SE_RESIZE_CURSOR;
        else if (AbstractResizeHotSpot.SOUTHWEST == direction)
            this.cursor = Cursor.SW_RESIZE_CURSOR;
        else if (AbstractResizeHotSpot.NORTHWEST == direction)
            this.cursor = Cursor.NW_RESIZE_CURSOR;
    }

    /**
     * Set the resize direction based on which point on the frame was picked (if a point on the left of the frame is
     * picked, the resize direction is west, if a point on the top edge is picked the resize direction is north, etc).
     *
     * @param pickPoint The point on the frame that was picked.
     */
    protected void setDirectionFromPoint(Point pickPoint) {
        Point topLeft = this.getScreenPoint();
        Dimension size = this.getSize();

        if (topLeft == null || size == null)
            return;

        // Find the center of the frame
        Point center = new Point(topLeft.x + size.width / 2, topLeft.y + size.height / 2);

        // Find horizontal and vertical distance from pick point to center point
        int dy = center.y - pickPoint.y;
        int dx = pickPoint.x - center.x;

        // Use the sign of dx and dy to determine if we are resizing up or down, left or right
        int vdir = (dy > 0) ? AbstractResizeHotSpot.NORTH : AbstractResizeHotSpot.SOUTH;
        int hdir = (dx > 0) ? AbstractResizeHotSpot.EAST : AbstractResizeHotSpot.WEST;

        // Compare the aspect ratio of the frame to the aspect ratio of the rectangle formed by the pick point and the
        // center point. If the aspect ratios are close to equal, resize both horizontally and vertically. Otherwise,
        // resize only horizontally or only vertically.
        double frameAspectRatio = (double) size.width / size.height;
        double pickAspectRatio = Math.abs((double) dx / dy);

        int dir;

        double tolerance = frameAspectRatio * 0.1;
        if (Math.abs(pickAspectRatio - frameAspectRatio) < tolerance)
            dir = hdir + vdir;
        else if (pickAspectRatio < frameAspectRatio)
            dir = vdir;
        else
            dir = hdir;

        this.setDirection(dir);
    }

    /**
     * Is the control currently dragging?
     *
     * @return True if the control is dragging.
     */
    public boolean isDragging() {
        return this.dragging;
    }

    /**
     * Handle a {@link SelectEvent} and call {@link #beginDrag}, {@link #drag}, {@link #endDrag} as appropriate.
     * Subclasses may override this method if they need to handle events other than drag events.
     *
     * @param event Select event.
     */
    @Override
    public void accept(SelectEvent event) {
        if (event == null || AbstractHotSpot.isConsumed(event))
            return;

        Point pickPoint = event.pickPoint;
        if (pickPoint != null) {
            if (event.isDrag()) {
                if (!this.isDragging()) {
                    this.dragging = true;
                    this.beginDrag(pickPoint);
                }

                this.drag(pickPoint);

                event.consume();
            }
        }

        if (event.isDragEnd()) {
            this.dragging = false;
            this.endDrag();

            event.consume();
        }
    }

    /**
     * Update the resize cursor when the mouse moves.
     *
     * @param e Mouse event.
     */
    @Override
    public void mouseMoved(MouseEvent e) {
        if (e == null || e.isConsumed())
            return;

        this.setDirectionFromPoint(e.getPoint());
    }

    protected void beginDrag(Point point) {
        this.dragRefPoint = point;
        this.refSize = this.getSize();
        this.refLocation = this.getScreenPoint();
    }

    public void drag(Point point) {
        int deltaX = 0;
        int deltaY = 0;

        if (this.refLocation == null || this.refSize == null)
            return;

        if (this.allowHorizontalResize)
            deltaX = (point.x - this.dragRefPoint.x) * this.xSign;
        if (this.allowVerticalResize)
            deltaY = (point.y - this.dragRefPoint.y) * this.ySign;

        int width = this.refSize.width + deltaX;
        int height = this.refSize.height + deltaY;

        if (this.isValidSize(width, height)) {
            this.setSize(new Dimension(width, height));

            if (this.adjustLocationX || this.adjustLocationY) {
                double x = this.refLocation.x - (this.adjustLocationX ? deltaX : 0);
                double y = this.refLocation.y - (this.adjustLocationY ? deltaY : 0);
                this.setScreenPoint(new Point((int) x, (int) y));
            }
        }
    }

    /**
     * Called when a drag action ends. This implementation sets {@link #dragRefPoint} to null.
     */
    protected void endDrag() {
        this.dragRefPoint = null;
    }

    /**
     * Get a cursor for the type of resize that this hotspot handles.
     *
     * @return New cursor.
     */
    @Override
    public Cursor getCursor() {
        return Cursor.getPredefinedCursor(this.cursor);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Overridden to reset state when the mouse leaves the resize area.
     *
     * @param active {@code true} if the HotSpot is being activated, {@code false} if it is being deactivated.
     */
    @Override
    public void setActive(boolean active) {
        // If the resize area is being deactivated, reset the cursor so that the next time the HotSpot becomes active
        // we won't show the wrong cursor.
        if (!active)
            this.cursor = Cursor.DEFAULT_CURSOR;
        super.setActive(active);
    }

    /**
     * Is a frame size valid? This method is called before attempting to resize the frame. If this method returns false,
     * the resize operation is not attempted. This implementation ensures that the proposed frame size is greater than
     * or equal to the minimum frame size.
     *
     * @param width  Frame width.
     * @param height Frame height.
     * @return True if this frame size is valid.
     * @see #getMinimumSize()
     */
    protected boolean isValidSize(int width, int height) {
        Dimension minSize = this.getMinimumSize();
        return width >= minSize.width && height >= minSize.height;
    }

    /**
     * Get the minimum size of the frame. The user is not allowed to resize the frame to be smaller than this size. This
     * implementation returns 0, 0.
     *
     * @return Minimum frame size.
     * @see #isValidSize(int, int)
     */
    protected Dimension getMinimumSize() {
        return new Dimension(0, 0);
    }

    /**
     * Get the size of the frame.
     *
     * @return Frame size in pixels.
     */
    protected abstract Dimension getSize();

    /**
     * Set the size of the frame.
     *
     * @param newSize New frame size in pixels.
     */
    protected abstract void setSize(Dimension newSize);

    /**
     * Get the screen point of the upper left corner of the frame.
     *
     * @return Screen point measured from upper left corner of the screen (AWT coordinates).
     */
    protected abstract Point getScreenPoint();

    /**
     * Set the screen point of the upper left corner of the frame.
     *
     * @param newPoint New screen point measured from upper left corner of the screen (AWT coordinates).
     */
    protected abstract void setScreenPoint(Point newPoint);
}