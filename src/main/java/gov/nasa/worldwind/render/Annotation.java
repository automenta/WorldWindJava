/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.render;

import com.jogamp.opengl.GL;
import gov.nasa.worldwind.*;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.pick.PickSupport;

import java.awt.*;
import java.util.List;

/**
 * Represent a text label and its rendering attributes.
 *
 * @author Patrick Murris
 * @version $Id: Annotation.java 1171 2013-02-11 21:45:02Z dcollins $
 */
public interface Annotation extends Renderable, Disposable, Restorable {
    /**
     * @deprecated Use {@link Keys#REPEAT_NONE} instead.
     */
    @Deprecated
    String IMAGE_REPEAT_NONE = Keys.REPEAT_NONE;
    /**
     * @deprecated Use {@link Keys#REPEAT_X} instead.
     */
    @Deprecated
    String IMAGE_REPEAT_X = Keys.REPEAT_X;
    /**
     * @deprecated Use {@link Keys#REPEAT_Y} instead.
     */
    @Deprecated
    String IMAGE_REPEAT_Y = Keys.REPEAT_Y;
    /**
     * @deprecated Use {@link Keys#REPEAT_XY} instead.
     */
    @Deprecated
    String IMAGE_REPEAT_XY = Keys.REPEAT_XY;

    int ANTIALIAS_DONT_CARE = GL.GL_DONT_CARE;
    int ANTIALIAS_FASTEST = GL.GL_FASTEST;
    int ANTIALIAS_NICEST = GL.GL_NICEST;

    /**
     * @deprecated Use {@link Keys#SIZE_FIXED} instead.
     */
    @Deprecated
    String SIZE_FIXED = Keys.SIZE_FIXED;
    /**
     * @deprecated Use {@link Keys#SIZE_FIT_TEXT} instead.
     */
    @Deprecated
    String SIZE_FIT_TEXT = Keys.SIZE_FIT_TEXT;

    boolean isAlwaysOnTop();

    void setAlwaysOnTop(boolean alwaysOnTop);

    boolean isPickEnabled();

    void setPickEnabled(boolean enable);

    String getText();

    void setText(String text);

    AnnotationAttributes getAttributes();

    void setAttributes(AnnotationAttributes attrs);

    List<? extends Annotation> getChildren();

    void addChild(Annotation annotation);

    boolean removeChild(Annotation annotation);

    void removeAllChildren();

    AnnotationLayoutManager getLayout();

    void setLayout(AnnotationLayoutManager layoutManager);

    PickSupport getPickSupport();

    void setPickSupport(PickSupport pickSupport);

    Object getDelegateOwner();

    void setDelegateOwner(Object delegateOwner);

    Dimension getPreferredSize(DrawContext dc);

    /**
     * Draws the annotation immediately on the specified DrawContext. Rendering is not be delayed by use of the
     * DrawContext's ordered mechanism, or any other delayed rendering mechanism. This is typically called by an
     * AnnotationRenderer while batch rendering. The GL should have its model view set to the identity matrix.
     *
     * @param dc the current DrawContext.
     * @throws IllegalArgumentException if <code>dc</code> is null.
     */
    void renderNow(DrawContext dc);

    /**
     * Draws the annotation without transforming to its screen position, or applying any scaling. This Annotation is
     * draw with the specified width, height, and opacity. The GL should have its model view set to whatever
     * transformation is desired.
     *
     * @param dc           the current DrawContext.
     * @param width        the width of the Annotation.
     * @param height       the height of the Annotation.
     * @param opacity      the opacity of the Annotation.
     * @param pickPosition the picked Position assigned to the Annotation, if picking is enabled.
     * @throws IllegalArgumentException if <code>dc</code> is null.
     */
    void draw(DrawContext dc, int width, int height, double opacity, Position pickPosition);

    /**
     * Get the annotation bounding {@link Rectangle} using OGL coordinates - bottom-left corner x and y relative to the
     * {@link WorldWindow} bottom-left corner, and the annotation callout width and height.
     * <p>
     * The annotation offset from it's reference point is factored in such that the callout leader shape and reference
     * point are included in the bounding rectangle.
     *
     * @param dc the current DrawContext.
     * @return the annotation bounding {@link Rectangle} using OGL viewport coordinates.
     * @throws IllegalArgumentException if <code>dc</code> is null.
     */
    Rectangle getBounds(DrawContext dc);

    /**
     * Returns the minimum eye altitude, in meters, for which the annotation is displayed.
     *
     * @return the minimum altitude, in meters, for which the annotation is displayed.
     * @see #setMinActiveAltitude(double)
     * @see #getMaxActiveAltitude()
     */
    double getMinActiveAltitude();

    /**
     * Specifies the minimum eye altitude, in meters, for which the annotation is displayed.
     *
     * @param minActiveAltitude the minimum altitude, in meters, for which the annotation is displayed.
     * @see #getMinActiveAltitude()
     * @see #setMaxActiveAltitude(double)
     */
    void setMinActiveAltitude(double minActiveAltitude);

    /**
     * Returns the maximum eye altitude, in meters, for which the annotation is displayed.
     *
     * @return the maximum altitude, in meters, for which the annotation is displayed.
     * @see #setMaxActiveAltitude(double)
     * @see #getMinActiveAltitude()
     */
    double getMaxActiveAltitude();

    /**
     * Specifies the maximum eye altitude, in meters, for which the annotation is displayed.
     *
     * @param maxActiveAltitude the maximum altitude, in meters, for which the annotation is displayed.
     * @see #getMaxActiveAltitude()
     * @see #setMinActiveAltitude(double)
     */
    void setMaxActiveAltitude(double maxActiveAltitude);
}