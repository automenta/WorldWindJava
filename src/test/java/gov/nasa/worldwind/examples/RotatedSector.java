/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */

package gov.nasa.worldwind.examples;

import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.globes.Globe;
import gov.nasa.worldwind.layers.RenderableLayer;
import gov.nasa.worldwind.render.SurfaceQuad;

import javax.swing.*;

import static java.lang.Math.toRadians;

/**
 * Illustrates rotating a {@link Sector} from standard position. A <code>Sector</code> is
 * created, its width and height computed, and a {@link SurfaceQuad} created from the
 * <code>sector's</code> centroid and the computed width and height. The <code>SurfaceQuad's</code> heading is then set
 * to the desired rotation angle.
 *
 * @author tag
 * @version $Id: RotatedSector.java 2109 2014-06-30 16:52:38Z tgaskins $
 */
public class RotatedSector extends ApplicationTemplate {
    private static final Sector sector = Sector.fromDegrees(45, 47, -123, -122);

    public static void main(String[] args) {
        ApplicationTemplate.start("Rotated Sector", AppFrame.class);
    }

    public static class AppFrame extends ApplicationTemplate.AppFrame {
        public AppFrame() {
            super(true, true, false);

            try {
                // Create the Quad from a Sector
                Globe globe = this.wwd().model().globe();
                double radius = globe.getRadiusAt(sector.getCentroid());
                double quadWidth = toRadians(sector.lonDelta) * radius;
                double quadHeight = toRadians(sector.latDelta) * radius;
                final SurfaceQuad quad = new SurfaceQuad(sector.getCentroid(), quadWidth, quadHeight, Angle.ZERO);

                // Create the layer to hold it
                final RenderableLayer layer = new RenderableLayer();
                layer.setName("Rotating Sector");
                layer.add(quad);

                // Add the layer to the model and update the ApplicationTemplate's layer manager
                WorldWindow.insertBeforeCompass(this.wwd(), layer);

                // Rotate the quad continuously
                Timer timer = new Timer(50, actionEvent -> {
                    // Increment the current heading if the layer is visible
                    if (layer.isEnabled()) {
                        quad.setHeading(new Angle((quad.getHeading().degrees + 1) % 360));
                        wwd().redraw();
                    }
                });
                timer.start();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}