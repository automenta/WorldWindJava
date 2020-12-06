/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.render;

import com.jogamp.opengl.*;
import gov.nasa.worldwind.util.*;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.geom.*;

/**
 * @author dcollins
 * @version $Id: ToolTipRenderer.java 2053 2014-06-10 20:16:57Z tgaskins $
 */
public class ToolTipRenderer {
    private boolean useSystemLookAndFeel;
    private Font font;
    private Color textColor;
    private Color interiorColor;
    private Color outlineColor;
    private double opacity;
    private double outlineWidth;
    private Insets insets;

    public ToolTipRenderer(Font font) {
        if (font == null) {
            String message = Logging.getMessage("nullValue.FontIsNull");
            Logging.logger().fine(message);
            throw new IllegalArgumentException(message);
        }

        this.useSystemLookAndFeel = false;
        this.font = font;
        this.textColor = Color.WHITE;
        this.interiorColor = Color.BLACK;
        this.outlineColor = Color.WHITE;
        this.outlineWidth = 1;
        this.opacity = 1;
        this.insets = new Insets(1, 1, 1, 1);
    }

    public ToolTipRenderer(boolean useSystemLookAndFeel) {
        this(Font.decode("Arial-PLAIN-12"));
        this.setUseSystemLookAndFeel(useSystemLookAndFeel);
    }

    public ToolTipRenderer() {
        this(Font.decode("Arial-PLAIN-12"));
    }

    public static Color getContrastingColor(Color color) {
        float[] hsbvals = new float[3];
        Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), hsbvals);
        Color c = Color.getHSBColor(0, 0, (hsbvals[2] + 0.5f) % 1.0f);
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), color.getAlpha());
    }

    public boolean isUseSystemLookAndFeel() {
        return this.useSystemLookAndFeel;
    }

    public void setUseSystemLookAndFeel(boolean useSystemLookAndFeel) {
        this.useSystemLookAndFeel = useSystemLookAndFeel;
    }

    public Font getFont() {
        return this.font;
    }

    public void setFont(Font font) {
        if (font == null) {
            String message = Logging.getMessage("nullValue.FontIsNull");
            Logging.logger().fine(message);
            throw new IllegalArgumentException(message);
        }

        this.font = font;
    }

    public Color getTextColor() {
        return this.textColor;
    }

    public void setTextColor(Color color) {
        if (color == null) {
            String message = Logging.getMessage("nullValue.ColorIsNull");
            Logging.logger().fine(message);
            throw new IllegalArgumentException(message);
        }

        this.textColor = color;
    }

    public Color getInteriorColor() {
        return this.interiorColor;
    }

    public void setInteriorColor(Color color) {
        if (color == null) {
            String message = Logging.getMessage("nullValue.ColorIsNull");
            Logging.logger().fine(message);
            throw new IllegalArgumentException(message);
        }

        this.interiorColor = color;
    }

    public Color getOutlineColor() {
        return this.outlineColor;
    }

    public void setOutlineColor(Color color) {
        if (color == null) {
            String message = Logging.getMessage("nullValue.ColorIsNull");
            Logging.logger().fine(message);
            throw new IllegalArgumentException(message);
        }

        this.outlineColor = color;
    }

    public double getOpacity() {
        return this.opacity;
    }

    public void setOpacity(double opacity) {
        if (opacity < 0 || opacity > 1) {
            String message = Logging.getMessage("generic.ArgumentOutOfRange", "opacity < 0 or opacity > 1");
            Logging.logger().fine(message);
            throw new IllegalArgumentException(message);
        }

        this.opacity = opacity;
    }

    public double getOutlineWidth() {
        return this.outlineWidth;
    }

    public void setOutlineWidth(double width) {
        if (width < 0) {
            String message = Logging.getMessage("generic.ArgumentOutOfRange", "width < 0");
            Logging.logger().fine(message);
            throw new IllegalArgumentException(message);
        }

        this.outlineWidth = width;
    }

    public Insets getInsets() {
        // Class java.awt.Insets is known to override the method Object.clone().
        return (Insets) this.insets.clone();
    }

    public void setInsets(Insets insets) {
        if (insets == null) {
            String message = Logging.getMessage("nullValue.InsetsIsNull");
            Logging.logger().fine(message);
            throw new IllegalArgumentException(message);
        }

        // Class java.awt.Insets is known to override the method Object.clone().
        this.insets = (Insets) insets.clone();
    }

    public void render(DrawContext dc, String text, int x, int y) {
        if (dc == null) {
            String message = Logging.getMessage("nullValue.DrawContextIsNull");
            Logging.logger().fine(message);
            throw new IllegalArgumentException(message);
        }

        if (text == null) {
            String message = Logging.getMessage("nullValue.StringIsNull");
            Logging.logger().fine(message);
            throw new IllegalArgumentException(message);
        }

        this.doRender(dc, text, x, y);
    }

    protected ToolTipAttributes getAttributes() {
        return new ToolTipAttributes(
            this.getFont(),
            this.getTextColor(),
            this.getInteriorColor(),
            this.getOutlineColor(),
            this.getOpacity(),
            this.getOpacity(),
            this.getOpacity(),
            this.getOutlineWidth(),
            this.getInsets());
    }

    protected ToolTipAttributes getSystemLookAndFeelAttributes() {
        Font font = UIManager.getFont("ToolTip.font");
        Color textColor = UIManager.getColor("ToolTip.foreground");
        Color interiorColor = UIManager.getColor("ToolTip.background");
        Color outlineColor = UIManager.getColor("ToolTip.foreground");
        double textOpacity = this.getOpacity();
        double interiorOpacity = this.getOpacity();
        double outlineOpacity = this.getOpacity();
        double outlineWidth = this.getOutlineWidth();
        Insets insets = null;

        Border border = UIManager.getBorder("ToolTip.border");
        if (border instanceof LineBorder) // Implicitly checks for non-null.
        {
            outlineColor = ((LineBorder) border).getLineColor();
            outlineWidth = ((LineBorder) border).getThickness();
        }

        if (border != null)
            insets = border.getBorderInsets(null);

        if (font == null)
            font = this.getFont();

        if (textColor == null)
            textColor = this.getTextColor();

        if (interiorColor == null)
            interiorColor = this.getInteriorColor();

        if (outlineColor == null)
            outlineColor = this.getOutlineColor();

        if (insets == null)
            insets = this.getInsets();

        return new ToolTipAttributes(font, textColor, interiorColor, outlineColor, textOpacity, interiorOpacity,
            outlineOpacity, outlineWidth, insets);
    }

    //**************************************************************//
    //********************  Rendering  *****************************//
    //**************************************************************//

    protected void doRender(DrawContext dc, String text, int x, int y) {
        OGLStackHandler stackHandler = new OGLStackHandler();

        ToolTipRenderer.beginRendering(dc, stackHandler);
        try {
            this.draw(dc, dc.getView().getViewport(), text, x, y);
        }
        finally {
            ToolTipRenderer.endRendering(dc, stackHandler);
        }
    }

    protected void draw(DrawContext dc, Rectangle viewport, String text, int x, int y) {
        ToolTipAttributes attributes = this.isUseSystemLookAndFeel() ?
            this.getSystemLookAndFeelAttributes() : this.getAttributes();

        this.drawToolTip(dc, viewport, text, x, y, attributes);
    }

    protected void drawToolTip(DrawContext dc, Rectangle viewport, String text, int x, int y,
        ToolTipAttributes attributes) {
        Rectangle2D textBounds = ToolTipRenderer.computeTextBounds(dc, text, attributes.getFont());
        Rectangle2D bgBounds = ToolTipRenderer.computeBackgroundBounds(dc,
            textBounds.getWidth(), textBounds.getHeight(), attributes.getInsets());

        Point screenPoint = ToolTipRenderer.adjustDrawPointToViewport(x, y, bgBounds, viewport);
        Point2D textTranslation = ToolTipRenderer.computeTextTranslation(dc, textBounds, attributes.getInsets());

        GL2 gl = dc.getGL2(); // GL initialization checks for GL2 compatibility.
        OGLStackHandler stackHandler = new OGLStackHandler();

        stackHandler.pushModelview(gl);
        try {
            gl.glTranslated(screenPoint.getX() + bgBounds.getX(), screenPoint.getY() + bgBounds.getY(), 0);
            ToolTipRenderer.drawToolTipInterior(dc, bgBounds.getWidth(), bgBounds.getHeight(), attributes);
            this.drawToolTipOutline(dc, bgBounds.getWidth(), bgBounds.getHeight(), attributes);

            gl.glTranslated(textTranslation.getX(), textTranslation.getY(), 0);
            ToolTipRenderer.drawToolTipText(dc, text, 0, 0, attributes);
        }
        finally {
            stackHandler.pop(gl);
        }
    }

    protected static void beginRendering(DrawContext dc, OGLStackHandler stackHandler) {
        if (dc == null) {
            String message = Logging.getMessage("nullValue.DrawContextIsNull");
            Logging.logger().fine(message);
            throw new IllegalArgumentException(message);
        }

        GL2 gl = dc.getGL2(); // GL initialization checks for GL2 compatibility.

        int attribMask = GL2.GL_COLOR_BUFFER_BIT // for alpha test func and ref, blend func
            | GL2.GL_CURRENT_BIT // for current color
            | GL2.GL_ENABLE_BIT // for enable/disable
            | GL2.GL_LINE_BIT // for line width
            | GL2.GL_TRANSFORM_BIT; // for matrix mode
        stackHandler.pushAttrib(gl, attribMask);

        stackHandler.pushTextureIdentity(gl);
        stackHandler.pushProjectionIdentity(gl);
        Rectangle viewport = dc.getView().getViewport();
        gl.glOrtho(viewport.x, viewport.x + viewport.width, viewport.y, viewport.y + viewport.height, -1, 1);
        stackHandler.pushModelviewIdentity(gl);

        // Enable the alpha test.
        gl.glEnable(GL2.GL_ALPHA_TEST);
        gl.glAlphaFunc(GL2.GL_GREATER, 0.0f);

        // Enable blending in premultiplied color mode.
        gl.glEnable(GL.GL_BLEND);
        OGLUtil.applyBlending(gl, true);

        gl.glDisable(GL.GL_CULL_FACE);
        gl.glDisable(GL.GL_DEPTH_TEST);
        gl.glDisable(GL2.GL_LIGHTING);
        gl.glDisable(GL.GL_TEXTURE_2D);
    }

    protected static void endRendering(DrawContext dc, OGLStackHandler stackHandler) {
        if (dc == null) {
            String message = Logging.getMessage("nullValue.DrawContextIsNull");
            Logging.logger().fine(message);
            throw new IllegalArgumentException(message);
        }

        GL2 gl = dc.getGL2(); // GL initialization checks for GL2 compatibility.

        stackHandler.pop(gl);
    }

    //**************************************************************//
    //********************  Background Rendering  ******************//
    //**************************************************************//

    protected static void drawToolTipInterior(DrawContext dc, double width, double height, ToolTipAttributes attributes) {
        GL2 gl = dc.getGL2(); // GL initialization checks for GL2 compatibility.

        ToolTipRenderer.applyColor(dc, attributes.getInteriorColor(), attributes.getInteriorOpacity());

        // Draw a filled rectangle with the background dimensions.
        gl.glRectd(0, 0, width, height);
    }

    protected void drawToolTipOutline(DrawContext dc, double width, double height, ToolTipAttributes attributes) {
        GL2 gl = dc.getGL2(); // GL initialization checks for GL2 compatibility.

        ToolTipRenderer.applyColor(dc, attributes.getOutlineColor(), attributes.getOutlineOpacity());
        gl.glLineWidth((float) getOutlineWidth());

        // Draw a line loop around the background rectangle. Inset the lines slightly to compensate for OpenGL's line
        // rasterization algorithm. We want the line to straddle the rectangle pixels.
        double inset = 0.5;
        gl.glBegin(GL2.GL_LINE_LOOP);
        gl.glVertex2d(inset, inset);
        gl.glVertex2d(width - inset, inset);
        gl.glVertex2d(width - inset, height - inset);
        gl.glVertex2d(inset, height - inset);
        gl.glEnd();
    }

    //**************************************************************//
    //********************  Text Rendering  ************************//
    //**************************************************************//

    protected static void drawToolTipText(DrawContext dc, CharSequence text, int x, int y, ToolTipAttributes attributes) {
        Color textColor = ToolTipRenderer.modulateColorOpacity(attributes.getTextColor(),
            attributes.getTextOpacity());

        TextRenderer textRenderer = ToolTipRenderer.getTextRenderer(dc, attributes.getFont());
        textRenderer.begin3DRendering();
        textRenderer.setColor(textColor);
        textRenderer.draw(text, x, y);
        textRenderer.end3DRendering();
    }

    //**************************************************************//
    //********************  Rendering Utilities  *******************//
    //**************************************************************//

    protected static TextRenderer getTextRenderer(DrawContext dc, Font font) {
        return OGLTextRenderer.getOrCreateTextRenderer(dc.getTextRendererCache(), font);
    }

    protected static void applyColor(DrawContext dc, Color color, double opacity) {
        if (dc.isPickingMode())
            return;

        double finalOpacity = opacity * (color.getAlpha() / 255.0);
        GL2 gl = dc.getGL2(); // GL initialization checks for GL2 compatibility.
        OGLUtil.applyColor(gl, color, finalOpacity, true);
    }

    protected static Color modulateColorOpacity(Color color, double opacity) {
        float[] compArray = new float[4];
        color.getRGBComponents(compArray);
        compArray[3] *= (float) opacity;

        return new Color(compArray[0], compArray[1], compArray[2], compArray[3]);
    }

    //**************************************************************//
    //********************  Bounds Computation  ********************//
    //**************************************************************//

    protected static Rectangle2D computeTextBounds(DrawContext dc, String text, Font font) {
        TextRenderer textRenderer = ToolTipRenderer.getTextRenderer(dc, font);
        return textRenderer.getBounds(text);
    }

    @SuppressWarnings("UnusedDeclaration")
    protected static Point2D computeTextTranslation(DrawContext dc, Rectangle2D textBounds,
        Insets insets) {
        // The text bounds are assumed to come from the return value of a call to TextRenderer.getBounds(). The bounds
        // place the origin in the upper left hand corner, with the y axis increasing downward. The y
        // coordinate in the bounds corresponds to the baseline of the leftmost character.

        return new Point2D.Double(
            insets.left - textBounds.getX(),
            insets.bottom + textBounds.getY() + textBounds.getHeight());
    }

    @SuppressWarnings("UnusedDeclaration")
    protected static Rectangle2D computeBackgroundBounds(DrawContext dc, double width, double height,
        Insets insets) {
        return new Rectangle2D.Double(
            0, 0,
            width + (insets.left + insets.right),
            height + (insets.top + insets.bottom));
    }

    protected static Point adjustDrawPointToViewport(int x, int y, Rectangle2D bounds,
        Rectangle viewport) {
        if (x + bounds.getMaxX() > viewport.getWidth())
            x = (int) (viewport.getWidth() - bounds.getWidth()) - 1;
        else if (x < 0)
            x = 0;

        if (y + bounds.getMaxY() > viewport.getHeight())
            y = (int) (viewport.getHeight() - bounds.getHeight()) - 1;
        else if (y < 0)
            y = 0;

        return new Point(x, y);
    }

    //**************************************************************//
    //********************  ToolTip Attributes  ********************//
    //**************************************************************//

    protected static class ToolTipAttributes {
        protected Font font;
        protected Color textColor;
        protected Color interiorColor;
        protected Color outlineColor;
        protected double textOpacity;
        protected double interiorOpacity;
        protected double outlineOpacity;
        protected double borderWidth;
        protected Insets insets;

        public ToolTipAttributes(Font font, Color textColor,
            Color interiorColor, Color outlineColor,
            double textOpacity, double interiorOpacity, double outlineOpacity,
            double borderWidth, Insets insets) {
            this.font = font;
            this.textColor = textColor;
            this.interiorColor = interiorColor;
            this.outlineColor = outlineColor;
            this.textOpacity = textOpacity;
            this.interiorOpacity = interiorOpacity;
            this.outlineOpacity = outlineOpacity;
            this.borderWidth = borderWidth;
            this.insets = insets;
        }

        public Font getFont() {
            return this.font;
        }

        public void setFont(Font font) {
            this.font = font;
        }

        public Color getTextColor() {
            return this.textColor;
        }

        public void setTextColor(Color color) {
            this.textColor = color;
        }

        public Color getInteriorColor() {
            return this.interiorColor;
        }

        public void setInteriorColor(Color color) {
            this.interiorColor = color;
        }

        public Color getOutlineColor() {
            return this.outlineColor;
        }

        public void setOutlineColor(Color color) {
            this.outlineColor = color;
        }

        public double getTextOpacity() {
            return this.textOpacity;
        }

        public void setTextOpacity(double textOpacity) {
            this.textOpacity = textOpacity;
        }

        public double getInteriorOpacity() {
            return this.interiorOpacity;
        }

        public void setInteriorOpacity(double interiorOpacity) {
            this.interiorOpacity = interiorOpacity;
        }

        public double getOutlineOpacity() {
            return this.outlineOpacity;
        }

        public void setOutlineOpacity(double outlineOpacity) {
            this.outlineOpacity = outlineOpacity;
        }

        public double getBorderWidth() {
            return this.borderWidth;
        }

        public void setBorderWidth(double borderWidth) {
            this.borderWidth = borderWidth;
        }

        public Insets getInsets() {
            return this.insets;
        }

        public void setInsets(Insets insets) {
            this.insets = insets;
        }
    }
}
