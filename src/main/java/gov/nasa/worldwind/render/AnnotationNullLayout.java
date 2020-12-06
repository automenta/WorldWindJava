/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.render;

import com.jogamp.opengl.GL2;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.util.*;

import java.awt.*;
import java.util.*;

/**
 * @author dcollins
 * @version $Id: AnnotationNullLayout.java 1171 2013-02-11 21:45:02Z dcollins $
 */
public class AnnotationNullLayout extends AbstractAnnotationLayout {
    protected final Map<Annotation, Object> constraintMap;

    public AnnotationNullLayout() {
        this.constraintMap = new HashMap<>();
    }

    public Object getConstraint(Annotation annotation) {
        if (annotation == null) {
            String message = Logging.getMessage("nullValue.AnnotationIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        return this.constraintMap.get(annotation);
    }

    public void setConstraint(Annotation annotation, Object constraint) {
        if (annotation == null) {
            String message = Logging.getMessage("nullValue.AnnotationIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.constraintMap.put(annotation, constraint);
    }

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

        // Start with an empty bounding rectangle in the lower left hand corner.
        Rectangle annotationBounds = new Rectangle();

        for (Annotation annotation : annotations) {
            Rectangle b = AnnotationNullLayout.getAnnotationBounds(dc, annotation);
            if (b != null) {
                annotationBounds = annotationBounds.union(b);
            }
        }

        return annotationBounds.getSize();
    }

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

        GL2 gl = dc.getGL2(); // GL initialization checks for GL2 compatibility.
        OGLStackHandler stackHandler = new OGLStackHandler();

        for (Annotation annotation : annotations) {
            Rectangle annotationBounds = AnnotationNullLayout.getAnnotationBounds(dc, annotation);
            annotationBounds = this.adjustAnnotationBounds(dc, bounds, annotation, annotationBounds);

            stackHandler.pushModelview(gl);
            gl.glTranslated(bounds.getMinX(), bounds.getMinY(), 0);
            gl.glTranslated(annotationBounds.getMinX(), annotationBounds.getMinY(), 0);

            this.drawAnnotation(dc, annotation, annotationBounds.width, annotationBounds.height, opacity, pickPosition);

            stackHandler.pop(gl);
        }
    }

    protected static Rectangle getAnnotationBounds(DrawContext dc, Annotation annotation) {
        Dimension size = AbstractAnnotationLayout.getAnnotationSize(dc, annotation);
        if (size == null)
            return null;

        Point offset = annotation.getAttributes().getDrawOffset();
        if (offset == null)
            offset = new Point();

        return new Rectangle(offset, size);
    }

    @SuppressWarnings("UnusedDeclaration")
    protected Rectangle adjustAnnotationBounds(DrawContext dc, Rectangle parentBounds,
        Annotation annotation, Rectangle bounds) {
        int x = bounds.x;
        int y = bounds.y;

        Object constraint = this.getConstraint(annotation);

        if (constraint == AVKey.WEST) {
            y += parentBounds.height / 2 - bounds.height / 2;
        }
        else if (constraint == AVKey.NORTHWEST) {
            y += parentBounds.height - bounds.height;
        }
        else if (constraint == AVKey.NORTH) {
            x += parentBounds.width / 2 - bounds.width / 2;
            y += parentBounds.height - bounds.height;
        }
        else if (constraint == AVKey.NORTHEAST) {
            x += parentBounds.width - bounds.width;
            y += parentBounds.height - bounds.height;
        }
        else if (constraint == AVKey.EAST) {
            x += parentBounds.width - bounds.width;
            y += parentBounds.height / 2 - bounds.height / 2;
        }
        else if (constraint == AVKey.SOUTHEAST) {
            x += parentBounds.width - bounds.width;
        }
        else if (constraint == AVKey.SOUTH) {
            x += parentBounds.width / 2 - bounds.width / 2;
        }
        else if (constraint == AVKey.CENTER) {
            x += parentBounds.width / 2 - bounds.width / 2;
            y += parentBounds.height / 2 - bounds.height / 2;
        }
        else // Default to anchoring in the south west corner.
        {
        }

        return new Rectangle(x, y, bounds.width, bounds.height);
    }
}
