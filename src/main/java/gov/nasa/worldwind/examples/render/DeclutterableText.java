/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */

package gov.nasa.worldwind.examples.render;

import gov.nasa.worldwind.geom.Vec4;
import gov.nasa.worldwind.util.Logging;

import java.awt.*;
import java.awt.geom.*;
import java.util.logging.Level;

/**
 * A wrapper around {@link GeographicText} that allows provides participation in global text decluttering.
 *
 * @author tag
 * @version $Id: DeclutterableText.java 704 2012-07-21 03:16:21Z tgaskins $
 */
public class DeclutterableText implements Declutterable {
    protected final GeographicText text;
    protected final Vec4 point;
    protected final double eyeDistance;
    protected final DeclutteringTextRenderer textRenderer;
    protected final boolean enableDecluttering = true;
    protected Rectangle2D textBounds; // cached text bounds
    protected Font boundsFont; // font used by cached text bounds

    /**
     * Construct an object for specified text and position.
     *
     * @param text         the text to display.
     * @param point        the Cartesian location of the text.
     * @param eyeDistance  the distance to consider the text from the eye.
     * @param textRenderer the text renderer to use to draw the text.
     */
    DeclutterableText(GeographicText text, Vec4 point, double eyeDistance, DeclutteringTextRenderer textRenderer) {
        this.text = text;
        this.point = point;
        this.eyeDistance = eyeDistance;
        this.textRenderer = textRenderer;
    }

    /**
     * Indicates whether this text should participate in decluttering.
     *
     * @return true (the default) if it should participate, otherwise false.
     */
    public boolean isEnableDecluttering() {
        return this.enableDecluttering;
    }

    public double getDistanceFromEye() {
        return this.eyeDistance;
    }

    public GeographicText getText() {
        return text;
    }

    public Vec4 getPoint() {
        return point;
    }

    public Rectangle2D getBounds(DrawContext dc) {
        Font font = this.getText().getFont();
        if (font == null)
            font = this.textRenderer.getDefaultFont();

        if (this.textBounds != null && this.boundsFont == font)
            return this.textBounds;

        try {
            this.textBounds = this.textRenderer.computeTextBounds(dc, this);
            this.boundsFont = font;
        }
        catch (Exception e) {
            Logging.logger().log(Level.SEVERE, "generic.ExceptionWhileRenderingText", e);
        }

        return this.textBounds;
    }

    /**
     * {@inheritDoc}
     */
    public void render(DrawContext dc) {
        try {
            if (this.getBounds(dc) == null)
                return;

            this.textRenderer.drawText(dc, this, 1, 1);
        }
        catch (Exception e) {
            Logging.logger().log(Level.SEVERE, "generic.ExceptionWhileRenderingText", e);
        }
    }

    public void pick(DrawContext dc, Point pickPoint) {
        // TODO
    }
}
