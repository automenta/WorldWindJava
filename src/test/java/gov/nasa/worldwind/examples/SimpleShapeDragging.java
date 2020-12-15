/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */

package gov.nasa.worldwind.examples;

import gov.nasa.worldwind.*;
import gov.nasa.worldwind.event.*;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.layers.*;
import gov.nasa.worldwind.layers.placename.PlaceNameLayer;
import gov.nasa.worldwind.render.*;
import gov.nasa.worldwind.util.BasicDragger;
import gov.nasa.worldwind.video.LayerList;
import gov.nasa.worldwind.video.awt.WorldWindowGLCanvas;

import javax.swing.*;
import java.awt.*;

/**
 * This example demonstrates the use of the {@link BasicDragger} class for dragging a shape
 * across the globe.
 *
 * @version $Id: SimpleShapeDragging.java 1171 2013-02-11 21:45:02Z dcollins $
 */
public class SimpleShapeDragging extends JFrame {
    public SimpleShapeDragging() {
        final WorldWindowGLCanvas wwd = new WorldWindowGLCanvas();
        wwd.setPreferredSize(new Dimension(1000, 800));
        this.getContentPane().add(wwd, BorderLayout.CENTER);
        wwd.setModel(new BasicModel());

        // Add a layer containing an image
        Renderable si = new SurfaceImage("images/400x230-splash-nww.png", Sector.fromDegrees(35, 45, -115, -95));
        RenderableLayer layer = new RenderableLayer();
        layer.add(si);
        insertBeforePlacenames(wwd, layer);

        // Set up to drag
        wwd.addSelectListener(new SelectListener() {
            private final SelectListener dragger = new BasicDragger(wwd);

            public void accept(SelectEvent event) {
                // Delegate dragging computations to a dragger.
                this.dragger.accept(event);
            }
        });
    }

    public static void main(String[] args) {
        EventQueue.invokeLater(() -> {
            JFrame frame = new SimpleShapeDragging();
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.pack();
            frame.setVisible(true);
        });
    }

    public static void insertBeforePlacenames(WorldWindow wwd, Layer layer) {
        // Insert the layer into the layer list just before the placenames.
        int compassPosition = 0;
        LayerList layers = wwd.model().getLayers();
        for (Layer l : layers) {
            if (l instanceof PlaceNameLayer)
                compassPosition = layers.indexOf(l);
        }
        layers.add(compassPosition, layer);
    }
}
