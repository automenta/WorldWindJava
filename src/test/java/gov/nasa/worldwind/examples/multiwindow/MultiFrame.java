/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */

package gov.nasa.worldwind.examples.multiwindow;

import gov.nasa.worldwind.*;
import gov.nasa.worldwind.globes.*;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.layers.earth.*;
import gov.nasa.worldwind.layers.sky.StarsLayer;
import gov.nasa.worldwind.layers.tool.*;
import gov.nasa.worldwind.util.StatusBar;
import gov.nasa.worldwind.video.LayerList;
import gov.nasa.worldwind.video.awt.WorldWindowGLCanvas;

import javax.swing.*;
import java.awt.*;

/**
 * This example shows how to create two WorldWindows, each in its own JFrame. The WorldWindows share a globe and some
 * layers.
 * <p>
 * Applications using multiple WorldWind windows simultaneously should instruct WorldWind to share OpenGL and other
 * resources among those windows. Most WorldWind classes are designed to be shared across {@link WorldWindow} objects
 * and are shared automatically. But OpenGL resources are not automatically shared. To share them, a reference to a
 * previously created WorldWindow must be specified as a constructor argument for subsequently created WorldWindows.
 * <p>
 * Most WorldWind {@link Globe} and {@link Layer} objects can be
 * shared among WorldWindows. Those that cannot be shared have an operational dependency on the WorldWindow they're
 * associated with. An example is the {@link ViewControlsLayer} layer for on-screen
 * navigation. Because this layer responds to input events within a specific WorldWindow, it is not sharable. Refer to
 * the WorldWind Overview page for a list of layers that cannot be shared. // TODO: include the reference to
 * overview.html.
 *
 * @author tag
 * @version $Id: MultiFrame.java 1171 2013-02-11 21:45:02Z dcollins $
 */
public class MultiFrame {
    public static void main(String[] args) {
        try {
            // Create a Model for each window, starting with the Globe they share.
            Globe earth = new Earth();

            // Create layers that both WorldWindows can share.
            Layer[] layers = new Layer[]
                {
                    new StarsLayer(),
                    new CompassLayer(),
                    new BMNGWMSLayer(),
                    new LandsatI3WMSLayer(),
                };

            // Create two models and pass them the shared layers.
            Model modelForWindowA = new BasicModel();
            modelForWindowA.setGlobe(earth);
            modelForWindowA.setLayers(new LayerList(layers));

            Model modelForWindowB = new BasicModel();
            modelForWindowB.setGlobe(earth);
            modelForWindowB.setLayers(new LayerList(layers));

            // Create two frames and give each their own model.
            CanvasFrame frameA = new CanvasFrame(null, modelForWindowA, "left");
            frameA.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frameA.setTitle("Frame A");
            frameA.wwp.wwd.setModel(modelForWindowA);
            frameA.setVisible(true);

            // When creating the second frame, specify resource sharing with the first one.
            CanvasFrame frameB = new CanvasFrame(frameA.wwp.wwd, modelForWindowB, "right");
            frameB.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frameB.setTitle("Frame B");
            frameB.wwp.wwd.setModel(modelForWindowB);
            frameB.setVisible(true);

            // Add view control layers, which the WorldWindows cannnot share.
            ViewControlsLayer viewControlsA = new ViewControlsLayer();
            frameA.wwp.wwd.model().getLayers().add(viewControlsA);
            frameA.wwp.wwd.addSelectListener(new ViewControlsLayer.ViewControlsSelectListener(frameA.wwp.wwd, viewControlsA));

            ViewControlsLayer viewControlsB = new ViewControlsLayer();
            frameB.wwp.wwd.model().getLayers().add(viewControlsB);
            frameB.wwp.wwd.addSelectListener(new ViewControlsLayer.ViewControlsSelectListener(frameB.wwp.wwd, viewControlsB));
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    // A panel to hold a WorldWindow and status bar.
    private static class WWPanel extends JPanel {
        private final WorldWindowGLCanvas wwd;

        public WWPanel(WorldWindowGLCanvas shareWith, int width, int height, Model model) {
            // To share resources among WorldWindows, pass the first WorldWindow to the constructor of the other
            // WorldWindows.
            this.wwd = shareWith != null ? new WorldWindowGLCanvas(shareWith) : new WorldWindowGLCanvas();
            this.wwd.setSize(new Dimension(width, height));
            this.wwd.setModel(model);

            this.setLayout(new BorderLayout(5, 5));
            this.add(this.wwd, BorderLayout.CENTER);

            StatusBar statusBar = new StatusBar();
            statusBar.setEventSource(wwd);
            this.add(statusBar, BorderLayout.SOUTH);
        }
    }

    // A JFrame to hold one WorldWindow panel. Multiple of these are created in main below.
    private static class CanvasFrame extends JFrame {
        private final WWPanel wwp;

        public CanvasFrame(WorldWindow shareWith, Model model, String side) {
            this.getContentPane().setLayout(new BorderLayout(5, 5));

            this.wwp = new WWPanel((WorldWindowGLCanvas) shareWith, 500, 500, model);
            this.getContentPane().add(wwp, BorderLayout.CENTER);

            this.pack();

            Dimension wwSize = this.getPreferredSize();
            wwSize.setSize(wwSize.getWidth(), 1.1 * wwSize.getHeight());
            this.setSize(wwSize);

            // Position the windows side-by-side.
            Dimension parentSize;
            Point parentLocation = new Point(0, 0);
            parentSize = Toolkit.getDefaultToolkit().getScreenSize();
            int x = parentLocation.x + (parentSize.width / 2 + (side.equals("left") ? -wwSize.width : 20));
            int y = parentLocation.y + (parentSize.height - wwSize.height) / 2;
            this.setLocation(x, y);
            this.setResizable(true);
        }
    }
}
