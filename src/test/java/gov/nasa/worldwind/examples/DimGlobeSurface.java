/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */

package gov.nasa.worldwind.examples;

import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.layers.RenderableLayer;
import gov.nasa.worldwind.render.SurfaceImage;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.image.*;

/**
 * Shows how to add a layer over the globe's surface imagery to simulate dimming the surface. The technique is very
 * simple: just create a {@link SurfaceImage}, apply it to the full globe, and use its opacity to control the amount of
 * dimming. This example uses a black surface image, but any color could be used.
 * <p>
 * Note that this does not provide a filtering effect -- enhancing or blocking specific colors. For that
 * <code>SurfaceImage</code> would need blending controls, but it doesn't have them.
 *
 * @author tag
 * @version $Id: DimGlobeSurface.java 2109 2014-06-30 16:52:38Z tgaskins $
 */
public class DimGlobeSurface extends ApplicationTemplate {
    public static void main(String[] args) {
        ApplicationTemplate.start("WorldWind Surface Dimming", AppFrame.class);
    }

    public static class AppFrame extends ApplicationTemplate.AppFrame {
        protected final SurfaceImage surfaceImage;
        protected JSlider opacitySlider;

        public AppFrame() {
            super(true, true, false);

            // Create a surface image covering the full globe and set its initial opacity.

            this.surfaceImage = new SurfaceImage(AppFrame.makeFilterImage(), Sector.FULL_SPHERE);
            this.surfaceImage.setOpacity(0.10);

            RenderableLayer layer = new RenderableLayer();
            layer.setName("Surface Dimmer");
            layer.setPickEnabled(false);

            layer.add(surfaceImage);

            WorldWindow.insertBeforePlacenames(this.wwd(), layer);

            // Create an opacity control panel.

            JPanel opacityPanel = new JPanel(new BorderLayout(5, 5));
            opacityPanel.setBorder(new EmptyBorder(5, 10, 10, 5));
            opacityPanel.add(new JLabel("Opacity"), BorderLayout.WEST);
            this.makeOpacitySlider();
            opacityPanel.add(this.opacitySlider, BorderLayout.CENTER);
            this.getControlPanel().add(opacityPanel, BorderLayout.SOUTH);
        }

        protected static BufferedImage makeFilterImage() {
            // A very small image can be used because it's all the same color.
            BufferedImage image = new BufferedImage(4, 4, BufferedImage.TYPE_INT_ARGB);

            Graphics2D g = (Graphics2D) image.getGraphics();

            g.setColor(new Color(0.0f, 0.0f, 0.0f, 1.0f)); // black, but any color could be used
            g.fillRect(0, 0, image.getWidth(), image.getHeight());

            g.dispose();

            return image;
        }

        protected void makeOpacitySlider() {
            this.opacitySlider = new JSlider(JSlider.HORIZONTAL, 0, 100, (int) (this.surfaceImage.getOpacity() * 100));
            this.opacitySlider.setToolTipText("Filter opacity");
            this.opacitySlider.addChangeListener(event -> {
                double value = opacitySlider.getValue();
                surfaceImage.setOpacity(value / 100);
                wwd().redraw();
            });
        }
    }
}
