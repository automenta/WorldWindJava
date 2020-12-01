/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.examples;

import gov.nasa.worldwind.*;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.event.*;
import gov.nasa.worldwind.exception.WWAbsentRequirementException;
import gov.nasa.worldwind.layers.*;
import gov.nasa.worldwind.util.*;
import gov.nasa.worldwind.video.awt.WorldWindowGLCanvas;

import javax.swing.*;
import java.awt.*;

/**
 * Provides a base application framework for simple WorldWind examples. Examine other examples in this package to see
 * how it's used.
 *
 * @version $Id: ApplicationTemplate.java 2115 2014-07-01 17:58:16Z tgaskins $
 */
public class ApplicationTemplate {

    static {
        System.setProperty("java.net.useSystemProxies", "true");
        if (Configuration.isMacOS()) {
            System.setProperty("apple.laf.useScreenMenuBar", "true");
            System.setProperty("com.apple.mrj.application.apple.menu.about.name", "WorldWind Application");
            System.setProperty("com.apple.mrj.application.growbox.intrudes", "false");
            System.setProperty("apple.awt.brushMetalLook", "true");
        }else if (Configuration.isWindowsOS()) {
            System.setProperty("sun.awt.noerasebackground", "true"); // prevents flashing during window resizing
        }
    }

    //    public static void insertBeforeLayerName(WorldWindow wwd, Layer layer, CharSequence targetName) {
//        // Insert the layer into the layer list just before the target layer.
//        int targetPosition = 0;
//        LayerList layers = wwd.getModel().getLayers();
//        for (Layer l : layers) {
//            if (l.getName().contains(targetName)) {
//                targetPosition = layers.indexOf(l);
//                break;
//            }
//        }
//        layers.add(targetPosition, layer);
//    }

    public static AppFrame start(String appName, Class<?> appFrameClass) {
        if (Configuration.isMacOS() && appName != null) {
            System.setProperty("com.apple.mrj.application.apple.menu.about.name", appName);
        }

        try {
            final AppFrame frame = (AppFrame) appFrameClass.getConstructor().newInstance();
            frame.setTitle(appName);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            EventQueue.invokeLater(() -> frame.setVisible(true));
            return frame;
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void main(String[] args) {
        // Call the static start method like this from the main method of your derived class.
        // Substitute your application's name for the first argument.
        ApplicationTemplate.start("WorldWind Application", AppFrame.class);
    }

    public static class AppPanel extends JPanel {

        protected final WorldWindow wwd;
        protected StatusBar statusBar;
        protected ToolTipController toolTipController;
        protected HighlightController highlightController;

        public AppPanel(Dimension canvasSize, boolean includeStatusBar) {
            super(new BorderLayout());

            this.wwd = this.createWorldWindow();
            ((Component) this.wwd).setPreferredSize(canvasSize);

            // Create the default model as described in the current worldwind properties.
            Model m = (Model) WorldWind.createConfigurationComponent(AVKey.MODEL_CLASS_NAME);
            this.wwd.setModel(m);


            this.add((Component) this.wwd, BorderLayout.CENTER);
            if (includeStatusBar) {
                this.statusBar = new StatusBar();
                this.add(statusBar, BorderLayout.PAGE_END);
                this.statusBar.setEventSource(wwd);
            }
            // Setup a select listener for the worldmap click-and-go feature
            this.wwd.addSelectListener(new ClickAndGoSelectListener(this.wwd(), WorldMapLayer.class));

            // Add controllers to manage highlighting and tool tips.
            this.toolTipController = new ToolTipController(this.wwd(), AVKey.DISPLAY_NAME, null);
            this.highlightController = new HighlightController(this.wwd(), SelectEvent.ROLLOVER);
        }

        protected WorldWindow createWorldWindow() {
            return new WorldWindowGLCanvas();
        }

        public WorldWindow wwd() {
            return wwd;
        }

        public StatusBar getStatusBar() {
            return statusBar;
        }
    }

    public static class AppFrame extends JFrame {

        protected AppPanel wwjPanel;
        protected JPanel controlPanel;
        protected LayerPanel layerPanel;
        protected StatisticsPanel statsPanel;
        private Dimension canvasSize = new Dimension(1000, 800);

        public AppFrame() {
            this.initialize(true, true, false);
        }

        public AppFrame(Dimension size) {
            this.canvasSize = size;
            this.initialize(true, true, false);
        }

        public AppFrame(boolean includeStatusBar, boolean includeLayerPanel, boolean includeStatsPanel) {
            this.initialize(includeStatusBar, includeLayerPanel, includeStatsPanel);
        }

        protected void initialize(boolean includeStatusBar, boolean includeLayerPanel, boolean includeStatsPanel) {
            // Create the WorldWindow.
            this.wwjPanel = this.createAppPanel(this.canvasSize, includeStatusBar);
            this.wwjPanel.setPreferredSize(canvasSize);

            // Put the pieces together.
            this.getContentPane().add(wwjPanel, BorderLayout.CENTER);
            if (includeLayerPanel) {
                this.controlPanel = new JPanel(new BorderLayout(10, 10));
                this.layerPanel = new LayerPanel(this.wwd());
                this.controlPanel.add(this.layerPanel, BorderLayout.CENTER);
                this.controlPanel.add(new FlatWorldPanel(this.wwd()), BorderLayout.NORTH);
                this.getContentPane().add(this.controlPanel, BorderLayout.WEST);
            }

            if (includeStatsPanel || System.getProperty("gov.nasa.worldwind.showStatistics") != null) {
                this.statsPanel = new StatisticsPanel(this.wwjPanel.wwd(), new Dimension(250, canvasSize.height));
                this.getContentPane().add(this.statsPanel, BorderLayout.EAST);
            }

            // Create and install the view controls layer and register a controller for it with the WorldWindow.
            ViewControlsLayer viewControlsLayer = new ViewControlsLayer();
            WorldWindow.insertBeforeCompass(wwd(), viewControlsLayer);
            this.wwd().addSelectListener(new ViewControlsSelectListener(this.wwd(), viewControlsLayer));

            // Register a rendering exception listener that's notified when exceptions occur during rendering.
            this.wwjPanel.wwd().addRenderingExceptionListener((Throwable t) -> {
                if (t instanceof WWAbsentRequirementException) {
                    String message = "Computer does not meet minimum graphics requirements.\n";
                    message += "Please install up-to-date graphics driver and try again.\n";
                    message += "Reason: " + t.getMessage() + "\n";
                    message += "This program will end when you press OK.";

                    JOptionPane.showMessageDialog(AppFrame.this, message, "Unable to Start Program",
                        JOptionPane.ERROR_MESSAGE);
                    System.exit(-1);
                }
            });

            // Search the layer list for layers that are also select listeners and register them with the World
            // Window. This enables interactive layers to be included without specific knowledge of them here.
            for (Layer layer : this.wwjPanel.wwd().model().getLayers()) {
                if (layer instanceof SelectListener) {
                    this.wwd().addSelectListener((SelectListener) layer);
                }
            }

            this.pack();

            // Center the application on the screen.
            WWUtil.alignComponent(null, this, AVKey.CENTER);
            this.setResizable(true);
        }

        protected AppPanel createAppPanel(Dimension canvasSize, boolean includeStatusBar) {
            return new AppPanel(canvasSize, includeStatusBar);
        }

        public AppPanel getWwjPanel() {
            return wwjPanel;
        }

        public WorldWindow wwd() {
            return this.wwjPanel.wwd();
        }

        public StatusBar getStatusBar() {
            return this.wwjPanel.getStatusBar();
        }

        /**
         * @return This application's layer panel.
         * @deprecated Use getControlPanel instead.
         */
        @Deprecated
        public LayerPanel getLayerPanel() {
            return this.layerPanel;
        }

        public JPanel getControlPanel() {
            return this.controlPanel;
        }

        public void setToolTipController(ToolTipController controller) {
            if (this.wwjPanel.toolTipController != null) {
                this.wwjPanel.toolTipController.dispose();
            }

            this.wwjPanel.toolTipController = controller;
        }

//        public void setHighlightController(HighlightController controller) {
//            if (this.wwjPanel.highlightController != null) {
//                this.wwjPanel.highlightController.dispose();
//            }
//
//            this.wwjPanel.highlightController = controller;
//        }
    }
}
