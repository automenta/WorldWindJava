/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.render;

import com.jogamp.opengl.GL2;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.pick.PickSupport;
import gov.nasa.worldwind.util.*;

import java.awt.*;
import java.util.logging.Level;

/**
 * @author dcollins
 * @version $Id: AbstractAnnotationLayout.java 1171 2013-02-11 21:45:02Z dcollins $
 */
public abstract class AbstractAnnotationLayout implements AnnotationLayoutManager {
    protected final OGLStackHandler stackHandler;
    protected PickSupport pickSupport;

    protected AbstractAnnotationLayout() {
        this.stackHandler = new OGLStackHandler();
    }

    protected static Dimension getAnnotationSize(DrawContext dc, Annotation annotation) {
        try {
            return annotation.getPreferredSize(dc);
        }
        catch (RuntimeException e) {
            // Trap and log exceptions thrown by computing an annotation's preferred size. This will prevent one
            // annotation from throwing an exception and preventing all other anotations from reporting their
            // preferred size.
            String message = Logging.getMessage("generic.ExceptionWhileComputingSize", annotation);
            Logging.logger().log(Level.SEVERE, message, e);
        }

        return null;
    }

    public PickSupport getPickSupport() {
        return this.pickSupport;
    }

    public void setPickSupport(PickSupport pickSupport) {
        this.pickSupport = pickSupport;
    }

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

        GL2 gl = dc.getGL2(); // GL initialization checks for GL2 compatibility.
        this.stackHandler.pushModelview(gl);
    }

    public void endDrawAnnotations(DrawContext dc) {
        if (dc == null) {
            String message = Logging.getMessage("nullValue.DrawContextIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        GL2 gl = dc.getGL2(); // GL initialization checks for GL2 compatibility.
        this.stackHandler.pop(gl);
    }

    protected void drawAnnotation(DrawContext dc, Annotation annotation, int width, int height, double opacity,
        Position pickPosition) {
        try {
            if (this.pickSupport != null)
                annotation.setPickSupport(this.pickSupport);

            annotation.draw(dc, width, height, opacity, pickPosition);
        }
        catch (RuntimeException e) {
            // Trap and log exceptions thrown by rendering an annotation. This will prevent one annotation from
            // throwing an exception and preventing all other anotations from rendering.
            String message = Logging.getMessage("generic.ExceptionWhileRenderingAnnotation", annotation);
            Logging.logger().log(Level.SEVERE, message, e);
        }
    }
}
