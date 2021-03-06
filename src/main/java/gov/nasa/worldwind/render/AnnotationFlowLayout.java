/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.render;

import com.jogamp.opengl.GL2;
import gov.nasa.worldwind.Keys;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.util.*;

import java.awt.*;
import java.util.Iterator;

/**
 * @author dcollins
 * @version $Id: AnnotationFlowLayout.java 1171 2013-02-11 21:45:02Z dcollins $
 */
public class AnnotationFlowLayout extends AbstractAnnotationLayout {
    private String orientation;
    private String alignment;
    private int hgap;
    private int vgap;

    public AnnotationFlowLayout(String orientation, String alignment, int hgap, int vgap) {
        if (orientation == null) {
            String message = Logging.getMessage("nullValue.AlignmentIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        // A null alignment is permitted. This tells the layout to choose the default alignment for the current
        // orientation.

        this.orientation = orientation;
        this.alignment = alignment;
        this.hgap = hgap;
        this.vgap = vgap;
    }

    public AnnotationFlowLayout(String orientation, int hgap, int vgap) {
        this(orientation, null, hgap, vgap);
    }

    public AnnotationFlowLayout(String orientation) {
        this(orientation, 0, 0);
    }

    public AnnotationFlowLayout() {
        this(Keys.HORIZONTAL);
    }

    @SuppressWarnings("StringEquality")
    protected static String getDefaultAlignment(String orientation) {
        if (orientation == null) {
            String message = Logging.getMessage("nullValue.OrientationIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (orientation == Keys.HORIZONTAL) {
            return Keys.BOTTOM;
        } else if (orientation == Keys.VERTICAL) {
            return Keys.LEFT;
        }

        return null;
    }

    @SuppressWarnings("StringEquality")
    protected static void alignHorizontal(DrawContext dc, Rectangle bounds, Dimension size, String align) {
        GL2 gl = dc.getGL2(); // GL initialization checks for GL2 compatibility.

        if (align == Keys.BOTTOM) {
            // This is the default.
        } else if (align == Keys.TOP) {
            int dy = bounds.height - size.height;
            gl.glTranslated(0, dy, 0);
        } else if (align == Keys.CENTER) {
            int dy = (bounds.height / 2) - (size.height / 2);
            gl.glTranslated(0, dy, 0);
        }
    }

    @SuppressWarnings("StringEquality")
    protected static void alignVertical(DrawContext dc, Rectangle bounds, Dimension size, String align) {
        GL2 gl = dc.getGL2(); // GL initialization checks for GL2 compatibility.

        if (align == Keys.LEFT) {
            // This is the default.
        }
        if (align == Keys.RIGHT) {
            int dx = bounds.width - size.width;
            gl.glTranslated(dx, 0, 0);
        } else if (align == Keys.CENTER) {
            int dx = (bounds.width / 2) - (size.width / 2);
            gl.glTranslated(dx, 0, 0);
        }
    }

    protected static void beginHorizontal(DrawContext dc, Rectangle bounds) {
        GL2 gl = dc.getGL2(); // GL initialization checks for GL2 compatibility.
        gl.glTranslated(bounds.getMinX(), bounds.getMinY(), 0);
    }

    protected static void beginVertical(DrawContext dc, Rectangle bounds) {
        GL2 gl = dc.getGL2(); // GL initialization checks for GL2 compatibility.
        gl.glTranslated(bounds.getMinX(), bounds.getMaxY(), 0);
    }

    public String getOrientation() {
        return this.orientation;
    }

    public void setOrientation(String orientation) {
        if (orientation == null) {
            String message = Logging.getMessage("nullValue.OrientationIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.orientation = orientation;
    }

    public String getAlignment() {
        return this.alignment;
    }

    public void setAlignment(String alignment) {
        // A null alignment is permitted. This tells the layout to choose the default alignment for the current
        // orientation.
        this.alignment = alignment;
    }

    public int getHorizontalGap() {
        return this.hgap;
    }

    public void setHorizontalGap(int hgap) {
        this.hgap = hgap;
    }

    public int getVerticalGap() {
        return this.vgap;
    }

    public void setVerticalGap(int vgap) {
        this.vgap = vgap;
    }

    @SuppressWarnings("StringEquality")
    public Dimension getPreferredSize(DrawContext dc, Iterable<? extends Annotation> annotations) {
        if (dc == null) {
            String message = Logging.getMessage("nullValue.DrawContextIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (annotations == null) {
            String message = Logging.getMessage("nullValue.IterableIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (this.orientation == Keys.HORIZONTAL) {
            return this.horizontalPreferredSize(dc, annotations);
        } else if (this.orientation == Keys.VERTICAL) {
            return this.verticalPerferredSize(dc, annotations);
        }

        return null;
    }

    @SuppressWarnings("StringEquality")
    public void drawAnnotations(DrawContext dc, Rectangle bounds,
        Iterable<? extends Annotation> annotations, double opacity, Position pickPosition) {
        if (dc == null) {
            String message = Logging.getMessage("nullValue.DrawContextIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (bounds == null) {
            String message = Logging.getMessage("nullValue.RectangleIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (annotations == null) {
            String message = Logging.getMessage("nullValue.IterableIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (this.orientation == Keys.HORIZONTAL) {
            this.drawHorizontal(dc, bounds, annotations, opacity, pickPosition);
        } else if (this.orientation == Keys.VERTICAL) {
            this.drawVertical(dc, bounds, annotations, opacity, pickPosition);
        }
    }

    @SuppressWarnings("StringEquality")
    public void beginDrawAnnotations(DrawContext dc, Rectangle bounds) {
        if (dc == null) {
            String message = Logging.getMessage("nullValue.DrawContextIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (bounds == null) {
            String message = Logging.getMessage("nullValue.RectangleIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        super.beginDrawAnnotations(dc, bounds);

        if (this.orientation == Keys.HORIZONTAL) {
            AnnotationFlowLayout.beginHorizontal(dc, bounds);
        } else if (this.orientation == Keys.VERTICAL) {
            AnnotationFlowLayout.beginVertical(dc, bounds);
        }
    }

    protected Dimension horizontalPreferredSize(DrawContext dc, Iterable<? extends Annotation> annotations) {
        int preferredWidth = 0;
        int preferredHeight = 0;

        Iterator<? extends Annotation> iter = annotations.iterator();
        if (!iter.hasNext())
            return new Dimension(preferredWidth, preferredHeight);

        while (iter.hasNext()) {
            Annotation annotation = iter.next();
            Dimension size = AbstractAnnotationLayout.getAnnotationSize(dc, annotation);
            if (size != null) {
                preferredWidth += size.width;

                if (preferredHeight < size.height)
                    preferredHeight = size.height;

                if (iter.hasNext())
                    preferredWidth += this.hgap;
            }
        }

        return new Dimension(preferredWidth, preferredHeight);
    }

    protected Dimension verticalPerferredSize(DrawContext dc, Iterable<? extends Annotation> annotations) {
        int preferredWidth = 0;
        int preferredHeight = 0;

        Iterator<? extends Annotation> iter = annotations.iterator();
        if (!iter.hasNext())
            return new Dimension(preferredWidth, preferredHeight);

        while (iter.hasNext()) {
            Annotation annotation = iter.next();
            Dimension size = AbstractAnnotationLayout.getAnnotationSize(dc, annotation);
            if (size != null) {
                preferredHeight += size.height;

                if (preferredWidth < size.width)
                    preferredWidth = size.width;

                if (iter.hasNext())
                    preferredHeight += this.vgap;
            }
        }

        return new Dimension(preferredWidth, preferredHeight);
    }

    protected void drawHorizontal(DrawContext dc, Rectangle bounds,
        Iterable<? extends Annotation> annotations, double opacity, Position pickPosition) {
        String align = this.getAlignment();
        if (align == null) {
            align = AnnotationFlowLayout.getDefaultAlignment(Keys.HORIZONTAL);
        }

        GL2 gl = dc.getGL2(); // GL initialization checks for GL2 compatibility.
        OGLStackHandler stackHandler = new OGLStackHandler();

        for (Annotation annotation : annotations) {
            Dimension size = annotation.getPreferredSize(dc);

            stackHandler.pushModelview(gl);
            AnnotationFlowLayout.alignHorizontal(dc, bounds, size, align);
            this.drawAnnotation(dc, annotation, size.width, size.height, opacity, pickPosition);
            stackHandler.pop(gl);

            gl.glTranslated(size.width, 0, 0);
            gl.glTranslated(this.hgap, 0, 0);
        }
    }

    protected void drawVertical(DrawContext dc, Rectangle bounds,
        Iterable<? extends Annotation> annotations, double opacity, Position pickPosition) {
        String align = this.getAlignment();
        if (align == null) {
            align = AnnotationFlowLayout.getDefaultAlignment(Keys.VERTICAL);
        }

        GL2 gl = dc.getGL2(); // GL initialization checks for GL2 compatibility.
        OGLStackHandler stackHandler = new OGLStackHandler();

        for (Annotation annotation : annotations) {
            Dimension size = annotation.getPreferredSize(dc);
            gl.glTranslated(0, -size.height, 0);

            stackHandler.pushModelview(gl);
            AnnotationFlowLayout.alignVertical(dc, bounds, size, align);
            this.drawAnnotation(dc, annotation, size.width, size.height, opacity, pickPosition);
            stackHandler.pop(gl);

            gl.glTranslated(0, -this.vgap, 0);
        }
    }
}