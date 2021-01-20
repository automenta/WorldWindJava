/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.examples.analytics;

import gov.nasa.worldwind.Keys;
import gov.nasa.worldwind.render.*;
import gov.nasa.worldwind.util.*;

import java.awt.*;
import java.awt.geom.*;
import java.awt.image.*;
import java.text.Format;
import java.util.List;
import java.util.*;

/**
 * @author dcollins
 * @version $Id: AnalyticSurfaceLegend.java 2053 2014-06-10 20:16:57Z tgaskins $
 */
public class AnalyticSurfaceLegend implements Renderable {
    protected static final Font DEFAULT_FONT = Font.decode("Arial-PLAIN-12");
    protected static final Color DEFAULT_COLOR = Color.WHITE;
    protected static final int DEFAULT_WIDTH = 32;
    protected static final int DEFAULT_HEIGHT = 256;
    protected boolean visible = true;
    protected ScreenImage screenImage;
    protected Iterable<? extends Renderable> labels;

    protected AnalyticSurfaceLegend() {
    }

    public static AnalyticSurfaceLegend fromColorGradient(int width, int height, double minValue, double maxValue,
        double minHue, double maxHue, Color borderColor, Iterable<? extends LabelAttributes> labels,
        LabelAttributes titleLabel) {
        AnalyticSurfaceLegend legend = new AnalyticSurfaceLegend();
        legend.screenImage = new ScreenImage();
        legend.screenImage.setImageSource(
            AnalyticSurfaceLegend.createColorGradientLegendImage(width, height, minHue, maxHue,
            borderColor));
        legend.labels = legend.createColorGradientLegendLabels(width, height, minValue, maxValue, labels, titleLabel);

        return legend;
    }

    public static AnalyticSurfaceLegend fromColorGradient(double minValue, double maxValue, double minHue,
        double maxHue, Iterable<? extends LabelAttributes> labels, LabelAttributes titleLabel) {
        return fromColorGradient(DEFAULT_WIDTH, DEFAULT_HEIGHT, minValue, maxValue, minHue, maxHue, DEFAULT_COLOR,
            labels,
            titleLabel);
    }

    public static Iterable<? extends AnalyticSurfaceLegend.LabelAttributes> createDefaultColorGradientLabels(
        double minValue, double maxValue, Format format) {
        if (format == null) {
            String message = Logging.getMessage("nullValue.Format");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        List<LabelAttributes> labels
            = new ArrayList<>();

        int numLabels = 5;
        Font font = Font.decode("Arial-BOLD-12");

        for (int i = 0; i < numLabels; i++) {
            double value = WWMath.mix(i / (double) (numLabels - 1), minValue, maxValue);

            String text = format.format(value);
            if (!WWUtil.isEmpty(text)) {
                labels.add(createLegendLabelAttributes(value, text, font, Color.WHITE, 5.0d, 0.0d));
            }
        }

        return labels;
    }

    public static AnalyticSurfaceLegend.LabelAttributes createDefaultTitle(String text) {
        if (text == null) {
            String message = Logging.getMessage("nullValue.StringIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        Font font = Font.decode("Arial-BOLD-16");
        return createLegendLabelAttributes(0.0d, text, font, Color.WHITE, 0.0d, -20.0d);
    }

    public static AnalyticSurfaceLegend.LabelAttributes createLegendLabelAttributes(final double value,
        final String text, final Font font, final Color color, final double xOffset, final double yOffset) {
        return new AnalyticSurfaceLegend.LabelAttributes() {
            public double getValue() {
                return value;
            }

            public String getText() {
                return text;
            }

            public Font getFont() {
                return font;
            }

            public Color getColor() {
                return color;
            }

            public Point2D getOffset() {
                return new Point2D.Double(xOffset, yOffset);
            }
        };
    }

    public boolean isVisible() {
        return this.visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public double getOpacity() {
        return this.screenImage.getOpacity();
    }

    public void setOpacity(double opacity) {
        if (opacity < 0.0d || opacity > 1.0d) {
            String message = Logging.getMessage("generic.OpacityOutOfRange", opacity);
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.screenImage.setOpacity(opacity);
    }

    public Point getScreenLocation(DrawContext dc) {
        return this.screenImage.getScreenLocation(dc);
    }

    public void setScreenLocation(Point point) {
        if (point == null) {
            String message = Logging.getMessage("nullValue.PointIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.screenImage.setScreenLocation(point);
    }

    public int getWidth(DrawContext dc) {
        if (dc == null) {
            String message = Logging.getMessage("nullValue.DrawContextIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        return this.screenImage.getImageWidth(dc);
    }

    //**************************************************************//
    //********************  Legend Utilities  **********************//
    //**************************************************************//

    public int getHeight(DrawContext dc) {
        if (dc == null) {
            String message = Logging.getMessage("nullValue.DrawContextIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        return this.screenImage.getImageHeight(dc);
    }

    public void render(DrawContext dc) {
        if (dc == null) {
            String message = Logging.getMessage("nullValue.DrawContextIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (!this.isVisible())
            return;

        this.doRender(dc);
    }

    protected void doRender(DrawContext dc) {
        this.screenImage.render(dc);

        if (!dc.isPickingMode() && this.labels != null) {
            for (Renderable renderable : this.labels) {
                if (renderable != null)
                    renderable.render(dc);
            }
        }
    }

    //**************************************************************//
    //********************  Legend Rendering  **********************//
    //**************************************************************//

    protected void drawLabel(DrawContext dc, LabelAttributes attr, double x, double y, String halign, String valign) {
        String text = attr.getText();
        if (WWUtil.isEmpty(text))
            return;

        Font font = attr.getFont();
        if (font == null)
            font = DEFAULT_FONT;

        Color color = DEFAULT_COLOR;
        if (attr.getColor() != null)
            color = attr.getColor();

        Point location = this.getScreenLocation(dc);
        if (location != null) {
            x += location.getX() - this.screenImage.getImageWidth(dc) / 2.0;
            y += location.getY() - this.screenImage.getImageHeight(dc) / 2.0;
        }

        Point2D offset = attr.getOffset();
        if (offset != null) {
            x += offset.getX();
            y += offset.getY();
        }

        TextRenderer tr = OGLTextRenderer.getOrCreateTextRenderer(dc.getTextRendererCache(), font);

        Rectangle2D bounds = tr.getBounds(text);
        if (bounds != null) {
            if (Keys.CENTER.equals(halign))
                x -= (bounds.getWidth() / 2.0d);
            if (Keys.RIGHT.equals(halign))
                x -= bounds.getWidth();

            if (Keys.CENTER.equals(valign))
                y += (bounds.getHeight() + bounds.getY());
            if (Keys.TOP.equals(valign))
                y += bounds.getHeight();
        }

        Rectangle viewport = dc.getView().getViewport();
        tr.beginRendering(viewport.width, viewport.height);
        try {
            double yInGLCoords = viewport.height - y - 1;

            // Draw the text outline, in a contrasting color.
            tr.setColor(WWUtil.computeContrastingColor(color));
            tr.draw(text, (int) x - 1, (int) yInGLCoords - 1);
            tr.draw(text, (int) x + 1, (int) yInGLCoords - 1);
            tr.draw(text, (int) x + 1, (int) yInGLCoords + 1);
            tr.draw(text, (int) x - 1, (int) yInGLCoords + 1);

            // Draw the text over its outline, in the specified color.
            tr.setColor(color);
            tr.draw(text, (int) x, (int) yInGLCoords);
        }
        finally {
            tr.endRendering();
        }
    }

    protected static BufferedImage createColorGradientLegendImage(int width, int height, double minHue, double maxHue,
        Color borderColor) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR);
        Graphics2D g2d = image.createGraphics();
        try {
            for (int y = 0; y < height; y++) {
                double hue = WWMath.mix(1.0d - y / (double) (height - 1), minHue, maxHue);
                g2d.setColor(Color.getHSBColor((float) hue, 1.0f, 1.0f));
                g2d.drawLine(0, y, width - 1, y);
            }

            if (borderColor != null) {
                g2d.setColor(borderColor);
                g2d.drawRect(0, 0, width - 1, height - 1);
            }
        }
        finally {
            g2d.dispose();
        }

        return image;
    }

    //**************************************************************//
    //********************  Hue Gradient Legend  *******************//
    //**************************************************************//

    protected Iterable<? extends Renderable> createColorGradientLegendLabels(int width, int height,
        double minValue, double maxValue, Iterable<? extends LabelAttributes> labels, LabelAttributes titleLabel) {
        List<Renderable> list = new ArrayList<>();

        if (labels != null) {
            for (LabelAttributes attr : labels) {
                if (attr == null)
                    continue;

                double factor = WWMath.computeInterpolationFactor(attr.getValue(), minValue, maxValue);
                double y = (1.0d - factor) * (height - 1);
                list.add(new LabelRenderable(this, attr, width, y, Keys.LEFT, Keys.CENTER));
            }
        }

        if (titleLabel != null) {
            list.add(new LabelRenderable(this, titleLabel, width / 2.0d, 0.0d, Keys.CENTER, Keys.BOTTOM));
        }

        return list;
    }

    public interface LabelAttributes {
        double getValue();

        String getText();

        Font getFont();

        Color getColor();

        Point2D getOffset();
    }

    //**************************************************************//
    //********************  Legend Label  **************************//
    //**************************************************************//

    protected static class LabelRenderable implements Renderable {
        protected final OrderedLabel orderedLabel;

        public LabelRenderable(AnalyticSurfaceLegend legend, LabelAttributes attr, double x, double y,
            String halign, String valign) {
            this.orderedLabel = new OrderedLabel(legend, attr, x, y, halign, valign);
        }

        public void render(DrawContext dc) {
            dc.addOrderedRenderable(this.orderedLabel);
        }
    }

    protected static class OrderedLabel implements OrderedRenderable {
        protected final AnalyticSurfaceLegend legend;
        protected final LabelAttributes attr;
        protected final double x;
        protected final double y;
        protected final String halign;
        protected final String valign;

        public OrderedLabel(AnalyticSurfaceLegend legend, LabelAttributes attr, double x, double y,
            String halign, String valign) {
            this.legend = legend;
            this.attr = attr;
            this.x = x;
            this.y = y;
            this.halign = halign;
            this.valign = valign;
        }

        public double getDistanceFromEye() {
            return 0;
        }

        public void render(DrawContext dc) {
            this.legend.drawLabel(dc, this.attr, this.x, this.y, this.halign, this.valign);
        }

        public void pick(DrawContext dc, Point pickPoint) {
            // Intentionally left blank.
        }
    }
}