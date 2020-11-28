/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.util;

import com.jogamp.opengl.*;
import gov.nasa.worldwind.render.*;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.render.*;

import java.awt.*;

/**
 * @author dcollins
 * @version $Id: ProgressAnnotation.java 1171 2013-02-11 21:45:02Z dcollins $
 */
public class ProgressAnnotation extends ScreenAnnotation {
    protected double value;
    protected double min;
    protected double max;
    protected Color outlineColor;
    protected Color interiorColor;
    protected Insets interiorInsets;

    public ProgressAnnotation(double value, double min, double max) {
        super("", new Point());

        this.value = value;
        this.min = min;
        this.max = max;

        this.outlineColor = new Color(60, 60, 60);
        this.interiorColor = new Color(171, 171, 171);
        this.interiorInsets = new Insets(2, 2, 2, 2);
    }

    public ProgressAnnotation() {
        this(0, 0, 1);
    }

    public double getValue() {
        return this.value;
    }

    public void setValue(double value) {
        this.value = value;
    }

    public double getMin() {
        return this.min;
    }

    public void setMin(double min) {
        this.min = min;
    }

    public double getMax() {
        return this.max;
    }

    public void setMax(double max) {
        this.max = max;
    }

    public Color getOutlineColor() {
        return this.outlineColor;
    }

    public void setOutlineColor(Color color) {
        if (color == null) {
            String message = Logging.getMessage("nullValue.ColorIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.outlineColor = color;
    }

    public Color getInteriorColor() {
        return this.interiorColor;
    }

    public void setInteriorColor(Color color) {
        if (color == null) {
            String message = Logging.getMessage("nullValue.ColorIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.interiorColor = color;
    }

    public Insets getInteriorInsets() {
        // Class java.awt.Insets is known to override the method Object.clone().
        return (Insets) this.interiorInsets.clone();
    }

    public void setInteriorInsets(Insets insets) {
        if (insets == null) {
            String message = Logging.getMessage("nullValue.InsetsIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        // Class java.awt.Insets is known to override the method Object.clone().
        this.interiorInsets = (Insets) insets.clone();
    }

    //**************************************************************//
    //********************  Rendering  *****************************//
    //**************************************************************//

    protected void doDraw(DrawContext dc, int width, int height, double opacity, Position pickPosition) {
        super.doDraw(dc, width, height, opacity, pickPosition);
        this.drawProgress(dc, width, height, opacity, pickPosition);
    }

    protected void drawProgress(DrawContext dc, int width, int height, double opacity, Position pickPosition) {
        if (dc.isPickingMode())
            return;

        this.drawProgressContainer(dc, width, height, opacity, pickPosition);
        this.drawProgressBar(dc, width, height, opacity, pickPosition);
    }

    @SuppressWarnings("UnusedDeclaration")
    protected void drawProgressContainer(DrawContext dc, int width, int height, double opacity,
        Position pickPosition) {
        Rectangle bounds = this.computeProgressContainerBounds(width, height);

        GL gl = dc.getGL();
        gl.glEnable(GL.GL_LINE_SMOOTH);
        gl.glHint(GL.GL_LINE_SMOOTH_HINT, GL.GL_NICEST);
        gl.glLineWidth(1);

        this.applyColor(dc, this.getOutlineColor(), opacity, false);
        this.drawCallout(dc, GL.GL_LINE_STRIP, bounds, false);
    }

    @SuppressWarnings("UnusedDeclaration")
    protected void drawProgressBar(DrawContext dc, int width, int height, double opacity,
        Position pickPosition) {
        Rectangle bounds = this.computeProgressBarBounds(width, height);

        this.applyColor(dc, this.getInteriorColor(), opacity, true);
        this.drawCallout(dc, GL.GL_TRIANGLE_FAN, bounds, false);
    }

    protected void drawCallout(DrawContext dc, int mode, Rectangle bounds, boolean useTexCoords) {
        GL2 gl = dc.getGL().getGL2(); // GL initialization checks for GL2 compatibility.

        OGLStackHandler stackHandler = new OGLStackHandler();
        stackHandler.pushModelview(gl);

        gl.glTranslated(bounds.x, bounds.y, 0);
        this.drawCallout(dc, mode, bounds.width, bounds.height, useTexCoords);

        stackHandler.pop(gl);
    }

    //**************************************************************//
    //********************  Bounds Computation  ********************//
    //**************************************************************//

    protected Rectangle computeProgressContainerBounds(int width, int height) {
        return this.computeInsetBounds(width, height);
    }

    protected Rectangle computeProgressBarBounds(int width, int height) {
        Rectangle containerBounds = this.computeProgressContainerBounds(width, height);

        int progressBarWidth = this.computeProgressBarWidth(containerBounds.width)
            - (this.interiorInsets.left + this.interiorInsets.right);
        int progressBarHeight = containerBounds.height - (this.interiorInsets.bottom + this.interiorInsets.top);

        if (progressBarWidth < 0)
            progressBarWidth = 0;
        if (progressBarHeight < 0)
            progressBarHeight = 0;

        return new Rectangle(
            containerBounds.x + this.interiorInsets.left, containerBounds.y + this.interiorInsets.bottom,
            progressBarWidth, progressBarHeight);
    }

    protected int computeProgressBarWidth(int containerWidth) {
        double factor = (this.value - this.min) / (this.max - this.min);
        return (int) (factor * containerWidth);
    }
}
