/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.formats.vpf;

import gov.nasa.worldwind.render.ShapeAttributes;
import gov.nasa.worldwind.util.*;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.*;
import java.io.*;
import java.util.ArrayList;
import java.util.logging.Level;

/**
 * @author Patrick Murris
 * @version $Id: VPFLegendSupport.java 1171 2013-02-11 21:45:02Z dcollins $
 */
public class VPFLegendSupport {
    protected static void drawLineLegend(VPFSymbolAttributes attr, Graphics2D g2, int width, int height, int margin) {
        g2.setStroke(VPFLegendSupport.getStroke(attr));
        g2.setPaint(attr.getOutlineMaterial().getDiffuse());
        g2.drawLine(margin, height / 2, width - margin, height / 2);
    }

    protected static Stroke getStroke(VPFSymbolAttributes attr) {
        BasicStroke stroke;
        float lineWidth = (float) attr.getOutlineWidth() + 0.5f; // Exagerate a bit line width
        if (attr.getOutlineStippleFactor() > 0) {
            // Dashed line - determine dash array from 16 bit stipple pattern
            ArrayList<Float> dashList = new ArrayList<>();
            short pattern = attr.getOutlineStipplePattern();
            int length = 0;
            boolean dash = true;
            for (int i = 0; i < 16; i++) {
                boolean dashBit = ((pattern << i) & 0x8000) > 0;
                if (dashBit != dash) {
                    dashList.add((float) length);
                    length = attr.getOutlineStippleFactor();
                    dash = dashBit;
                } else
                    length += attr.getOutlineStippleFactor();
            }
            dashList.add((float) length);
            float[] dashArray = new float[dashList.size()];
            for (int i = 0; i < dashList.size(); i++) {
                dashArray[i] = dashList.get(i);
            }

            stroke = new BasicStroke(lineWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                10.0f, dashArray, 0.0f);
        } else {
            // Plain line
            stroke = new BasicStroke(lineWidth);
        }
        return stroke;
    }

    public BufferedImage createLegendImage(VPFSymbolAttributes attr, int width, int height, int margin) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR);
        Graphics2D g2 = image.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        switch (attr.getFeatureType()) {
            case POINT -> this.drawPointLegend(attr, g2, width, height, margin);
            case LINE -> VPFLegendSupport.drawLineLegend(attr, g2, width, height, margin);
            case AREA -> this.drawAreaLegend(attr, g2, width, height, margin);
        }

        return image;
    }

    protected void drawPointLegend(VPFSymbolAttributes attr, Graphics2D g2, int width, int height, int margin) {
        if (attr.getIconImageSource() == null)
            return;

        BufferedImage icon = getImage(attr.getIconImageSource());
        if (icon != null) {
            // icon width / height
            int iw = icon.getWidth();
            int ih = icon.getHeight();
            // draw area width / height
            int dw = width - margin * 2;
            int dh = height - margin * 2;
            // draw scale to fit icon inside draw area
            float sx = iw > dw ? (float) dw / iw : 1.0f;  // shrink only
            float sy = ih > dh ? (float) dh / ih : 1.0f;
            float scale = Math.min(sx, sy);
            iw = (int) (iw * scale);
            ih = (int) (ih * scale);
            // Center image and draw
            int x1 = iw < dw ? margin + (dw - iw) / 2 : margin;
            int y1 = ih < dh ? margin + (dh - ih) / 2 : margin;
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2.drawImage(icon, x1, y1, iw, ih, null);
        }
    }

    protected void drawAreaLegend(VPFSymbolAttributes attr, Graphics2D g2, int width, int height, int margin) {
        // Interior if any
        if (attr.isDrawInterior()) {
            g2.setPaint(this.getFillPaint(attr, width, height));
            g2.fillRect(margin, margin, width - margin * 2, height - margin * 2);
        }
        // Outline if any
        if (attr.isDrawOutline()) {
            g2.setStroke(VPFLegendSupport.getStroke(attr));
            g2.setPaint(attr.getOutlineMaterial().getDiffuse());
            g2.drawRect(margin, margin, width - margin * 2, height - margin * 2);
        }
    }

    @SuppressWarnings("UnusedDeclaration")
    protected Paint getFillPaint(ShapeAttributes attr, int width, int height) {
        if (attr.getImageSource() == null)
            return attr.getInteriorMaterial().getDiffuse();

        BufferedImage pattern = getImage(attr.getImageSource());
        if (pattern != null)
            return new TexturePaint(pattern, new Rectangle(0, 0, pattern.getWidth(), pattern.getHeight()));

        return attr.getInteriorMaterial().getDiffuse();
    }

    protected BufferedImage getImage(Object imageSource) {
        if (imageSource instanceof String) {
            String path = (String) imageSource;

            Object streamOrException = WWIO.getFileOrResourceAsStream(path, this.getClass());
            if (streamOrException == null || streamOrException instanceof Exception) {
                Logging.logger().log(Level.SEVERE, "generic.ExceptionAttemptingToReadImageFile",
                    streamOrException != null ? streamOrException : path);
                return null;
            }

            try {
                return ImageIO.read((InputStream) streamOrException);
            }
            catch (IOException e) {
                e.printStackTrace();
            }
            catch (Exception e) {
                Logging.logger().log(Level.SEVERE, "generic.ExceptionAttemptingToReadImageFile",
                    path);
                return null;
            }
        } else if (imageSource instanceof BufferedImage) {
            return (BufferedImage) imageSource;
        }

        return null;
    }
}
