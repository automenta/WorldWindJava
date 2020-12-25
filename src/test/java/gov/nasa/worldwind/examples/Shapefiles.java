/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.examples;

import gov.nasa.worldwind.formats.shapefile.ShapefileLayerFactory;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.util.*;

import javax.swing.*;
import java.util.logging.Level;

/**
 * Illustrates how to import ESRI Shapefiles into WorldWind. This uses a <code>{@link ShapefileLayerFactory}</code> to
 * parse a Shapefile's contents and convert the shapefile into an equivalent WorldWind shape.
 *
 * @version $Id: Shapefiles.java 3212 2015-06-18 02:45:56Z tgaskins $
 */
public class Shapefiles extends ApplicationTemplate {
    public static void main(String[] args) {
        start("WorldWind Shapefiles", AppFrame.class);
    }

    public static class AppFrame extends ApplicationTemplate.AppFrame {
        public AppFrame() {
            ShapefileLayerFactory factory = new ShapefileLayerFactory();

            // Specify an attribute delegate to assign random attributes to each shapefile record.
            final RandomShapeAttributes randomAttrs = new RandomShapeAttributes();
            factory.setAttributeDelegate(
                (shapefileRecord, renderableRecord) -> renderableRecord.setAttributes(
                    randomAttrs.nextAttributes().asShapeAttributes()));

            // Load the shapefile. Define the completion callback.
            factory.createFromShapefileSource("/shapefiles/TM_WORLD_BORDERS-0.3.shp",
                new ShapefileLayerFactory.CompletionCallback() {
                    @Override
                    public void completion(Object result) {
                        final Layer layer = (Layer) result; // the result is the layer the factory created
                        layer.setName(WWIO.getFilename(layer.name()));

                        // Add the layer to the WorldWindow's layer list on the Event Dispatch Thread.
                        SwingUtilities.invokeLater(() -> AppFrame.this.wwd().model().getLayers().add(layer));
                    }

                    @Override
                    public void exception(Exception e) {
                        Logging.logger().log(Level.SEVERE, e.getMessage(), e);
                    }
                });
        }
    }
}
