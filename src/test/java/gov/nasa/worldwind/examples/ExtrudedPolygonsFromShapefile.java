/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */

package gov.nasa.worldwind.examples;

import gov.nasa.worldwind.*;
import gov.nasa.worldwind.formats.shapefile.ShapefileLayerFactory;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.util.*;

import javax.swing.*;
import java.util.logging.Level;

/**
 * Shows how to make extruded shapes from an ESRI Shapefile containing per-shape height attributes.
 */
public class ExtrudedPolygonsFromShapefile extends ApplicationTemplate {
    public static void main(String[] args) {
        Configuration.setValue(Keys.INITIAL_LATITUDE, 37.419833280894515);
        Configuration.setValue(Keys.INITIAL_LONGITUDE, -122.08426559929343);
        Configuration.setValue(Keys.INITIAL_ALTITUDE, 1000.0);
        Configuration.setValue(Keys.INITIAL_PITCH, 60.0);

        ApplicationTemplate.start("Extruded Polygons from Shapefile", AppFrame.class);
    }

    public static class AppFrame extends ApplicationTemplate.AppFrame {
        public AppFrame() {
            // Construct a factory that loads Shapefiles on a background thread.
            ShapefileLayerFactory factory = new ShapefileLayerFactory();

            // Load a Shapefile in the San Francisco bay area containing per-shape height attributes.
            factory.createFromShapefileSource("shapefiles/BayArea.shp",
                new ShapefileLayerFactory.CompletionCallback() {
                    @Override
                    public void completion(Object result) {
                        final Layer layer = (Layer) result; // the result is the layer the factory created
                        layer.setName(WWIO.getFilename(layer.name()));

                        // Add the layer to the WorldWindow's layer list on the Event Dispatch Thread.
                        SwingUtilities.invokeLater(() -> {
                            wwd().model().getLayers().add(layer);
                            wwd().redraw();
                        });
                    }

                    @Override
                    public void exception(Exception e) {
                        Logging.logger().log(Level.SEVERE, e.getMessage(), e);
                    }
                });
        }
    }
}