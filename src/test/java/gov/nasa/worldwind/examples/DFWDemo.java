/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.examples;

import gov.nasa.worldwind.*;
import gov.nasa.worldwind.avlist.*;
import gov.nasa.worldwind.event.*;
import gov.nasa.worldwind.exception.WWAbsentRequirementException;
import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.layers.ogc.wms.*;
import gov.nasa.worldwind.layers.placename.PlaceNameLayer;
import gov.nasa.worldwind.layers.tool.*;
import gov.nasa.worldwind.layers.wms.WMSTiledImageLayer;
import gov.nasa.worldwind.terrain.WMSBasicElevationModel;
import gov.nasa.worldwind.util.*;
import gov.nasa.worldwind.video.LayerList;
import gov.nasa.worldwind.video.awt.WorldWindowGLCanvas;

import javax.swing.*;
import java.awt.*;
import java.net.URI;
import java.util.List;

public class DFWDemo {

    static {
        System.setProperty("java.net.useSystemProxies", "true");
        if (Configuration.isMacOS()) {
            System.setProperty("apple.laf.useScreenMenuBar", "true");
            System.setProperty("com.apple.mrj.application.apple.menu.about.name", "WorldWind Application");
            System.setProperty("com.apple.mrj.application.growbox.intrudes", "false");
            System.setProperty("apple.awt.brushMetalLook", "true");
        }
        else if (Configuration.isWindowsOS()) {
            System.setProperty("sun.awt.noerasebackground", "true"); // prevents flashing during window resizing
        }
    }

    public static void insertBeforeCompass(WorldWindow wwd, Layer layer) {
        // Insert the layer into the layer list just before the compass.
        int compassPosition = 0;
        LayerList layers = wwd.model().getLayers();
        for (Layer l : layers) {
            if (l instanceof CompassLayer) {
                compassPosition = layers.indexOf(l);
            }
        }
        layers.add(compassPosition, layer);
    }

    public static void insertBeforePlacenames(WorldWindow wwd, Layer layer) {
        // Insert the layer into the layer list just before the placenames.
        int compassPosition = 0;
        LayerList layers = wwd.model().getLayers();
        for (Layer l : layers) {
            if (l instanceof PlaceNameLayer) {
                compassPosition = layers.indexOf(l);
            }
        }
        layers.add(compassPosition, layer);
    }

    public static void insertAfterPlacenames(WorldWindow wwd, Layer layer) {
        // Insert the layer into the layer list just after the placenames.
        int compassPosition = 0;
        LayerList layers = wwd.model().getLayers();
        for (Layer l : layers) {
            if (l instanceof PlaceNameLayer) {
                compassPosition = layers.indexOf(l);
            }
        }
        layers.add(compassPosition + 1, layer);
    }

    public static void insertBeforeLayerName(WorldWindow wwd, Layer layer, CharSequence targetName) {
        // Insert the layer into the layer list just before the target layer.
        int targetPosition = 0;
        LayerList layers = wwd.model().getLayers();
        for (Layer l : layers) {
            if (l.name().contains(targetName)) {
                targetPosition = layers.indexOf(l);
                break;
            }
        }
        layers.add(targetPosition, layer);
    }

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
//        logger = gov.nasa.worldwind.examples.util.Logging.logger();
//
//        // Turn off logging to parent handlers of the World Wind handler.
//        logger.setUseParentHandlers(false);
//
//        try {
//            // Create a console handler (defined below) that we use to write log messages.
//            final ConsoleHandler handler = new ConsoleHandler();
//            //final FileHandler handler=new FileHandler("/Users/mpeterson/foo/wwlog.txt");
//            handler.setFilter(null);
//
//            // Enable all logging levels on both the logger and the handler.
//            logger.setLevel(java.util.logging.Level.ALL);
//            handler.setLevel(java.util.logging.Level.ALL);
//
//            // Add our handler to the logger
//            logger.addHandler(handler);
//        } catch (Exception ex) {
//            ex.printStackTrace();
//        }
        // Call the static start method like this from the main method of your derived class.
        // Substitute your application's name for the first argument.
        DFWDemo.start("WorldWind Application", AppFrame.class);
    }

    public static class DynamicLayerInfo {

        private final WMSCapabilities caps;
        private final WMSLayerCapabilities layerCaps;
        private final KV params;
        private final String serverUrl;
        private WMSTiledImageLayer imageLayer;
        private WMSBasicElevationModel elevationModel;

        public DynamicLayerInfo(String pServerUrl, WMSCapabilities pCaps, WMSLayerCapabilities pLayerCaps) {
            serverUrl = pServerUrl;
            imageLayer = null;
            caps = pCaps;
            params = new KVMap();
            layerCaps = pLayerCaps;
            params.set(Keys.LAYER_NAMES, layerCaps.getName());
            String abs = layerCaps.getLayerAbstract();
            if (!WWUtil.isEmpty(abs)) {
                params.set(Keys.LAYER_ABSTRACT, abs);
            }

            params.set(Keys.DISPLAY_NAME, makeTitle(caps));
        }

        public Sector getSector() {
            return layerCaps.getGeographicBoundingBox();
        }

        public String getServerUrl() {
            return serverUrl;
        }

        public WMSTiledImageLayer getImageLayer() {
            return imageLayer;
        }

        public void setImageLayer(WMSTiledImageLayer layer) {
            imageLayer = layer;
        }

        public WMSBasicElevationModel getElevationModel() {
            return this.elevationModel;
        }

        public void setElevationModel(WMSBasicElevationModel m) {
            this.elevationModel = m;
        }

        private String makeTitle(WMSCapabilities caps) {
            String layerNames = params.getStringValue(Keys.LAYER_NAMES);
            String styleNames = params.getStringValue(Keys.STYLE_NAMES);
            String[] lNames = layerNames.split(",");
            String[] sNames = styleNames != null ? styleNames.split(",") : null;

            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < lNames.length; i++) {
                if (!sb.isEmpty()) {
                    sb.append(", ");
                }

                String layerName = lNames[i];
                WMSLayerCapabilities lc = caps.getLayerByName(layerName);
                String layerTitle = lc.getTitle();
                sb.append(layerTitle != null ? layerTitle : layerName);

                if (sNames == null || sNames.length <= i) {
                    continue;
                }

                String styleName = sNames[i];
                WMSLayerStyle style = lc.getStyleByName(styleName);
                if (style == null) {
                    continue;
                }

                sb.append(" : ");
                String styleTitle = style.getTitle();
                sb.append(styleTitle != null ? styleTitle : styleName);
            }

            return sb.toString();
        }

        public WMSCapabilities getCaps() {
            return caps;
        }

        public KV getParams() {
            return params;
        }
    }

    public static class AppPanel extends JPanel {

        protected final WorldWindow wwd;
        protected StatusBar statusBar;
        protected ToolTipController toolTipController;
        protected HighlightController highlightController;

        public AppPanel(Dimension canvasSize, boolean includeStatusBar) {
            super(new BorderLayout());

            this.wwd = AppPanel.createWorldWindow();
            ((Component) this.wwd).setPreferredSize(canvasSize);

            // Create the default model as described in the current worldwind properties.
            Model m = (Model) WorldWind.createConfigurationComponent(Keys.MODEL_CLASS_NAME);
            this.wwd.setModel(m);

            // Setup a select listener for the worldmap click-and-go feature
            this.wwd.addSelectListener(new ClickAndGoSelectListener(this.getWwd(), WorldMapLayer.class));

            this.add((Component) this.wwd, BorderLayout.CENTER);
            if (includeStatusBar) {
                this.statusBar = new StatusBar();
                this.add(statusBar, BorderLayout.PAGE_END);
                this.statusBar.setEventSource(wwd);
            }

            // Add controllers to manage highlighting and tool tips.
            this.toolTipController = new ToolTipController(this.getWwd(), Keys.DISPLAY_NAME, null);
            this.highlightController = new HighlightController(this.getWwd(), SelectEvent.ROLLOVER);
        }

        protected static WorldWindow createWorldWindow() {
            return new WorldWindowGLCanvas();
        }

        public WorldWindow getWwd() {
            return wwd;
        }

        public StatusBar getStatusBar() {
            return statusBar;
        }
    }
    // LogManager holds loggers by weak reference, so if you don't provide a strong reference to your logger, it gets GC'd

    protected static class AppFrame extends JFrame {

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

        private static DynamicLayerInfo loadDynamicLayer(String serverUrl, boolean imagery) throws Exception {
            URI serverURI = new URI(serverUrl); // throws an exception if server name is not a valid uri.
            WMSCapabilities caps;
            caps = WMSCapabilities.retrieve(serverURI);
            caps.parse();
            List<WMSLayerCapabilities> namedLayerCaps = caps.getNamedLayers();
            if (namedLayerCaps == null) {
                System.out.println("bad dynamic layer:" + serverUrl);
                return null;
            }

            DynamicLayerInfo dynamicLayer = null;
            for (WMSLayerCapabilities lc : namedLayerCaps) {
                String layerName = lc.getName();
                if (layerName.endsWith("_group")) {
                    dynamicLayer = new DynamicLayerInfo(serverUrl, caps, lc);
                }
            }

            if (dynamicLayer != null) {
                KV configParams = dynamicLayer.getParams().copy(); // Copy to insulate changes from the caller.

                // Some wms servers are slow, so increase the timeouts and limits used by world wind's retrievers.
                configParams.set(Keys.URL_CONNECT_TIMEOUT, 30000);
                configParams.set(Keys.URL_READ_TIMEOUT, 30000);
                configParams.set(Keys.RETRIEVAL_QUEUE_STALE_REQUEST_LIMIT, 60000);
                long fiveDayMillis = 5L * 24L * 60L * 60L * 1000L;
                if (imagery) {
                    WMSTiledImageLayer wmsLayer = new WMSTiledImageLayer(dynamicLayer.getCaps(), configParams);

                    wmsLayer.setExpiryTime(System.currentTimeMillis() - fiveDayMillis);
                    dynamicLayer.setImageLayer(wmsLayer);
                }
                else {
                    configParams.set(Keys.TILE_WIDTH, 1024);
                    configParams.set(Keys.TILE_HEIGHT, 1024);
                    configParams.set(Keys.NUM_LEVELS, 9);
                    Angle delta = new Angle(0.01);
                    configParams.set(Keys.LEVEL_ZERO_TILE_DELTA, new LatLon(delta, delta));
                    WMSBasicElevationModel wmsElevations = new WMSBasicElevationModel(dynamicLayer.getCaps(),
                        configParams);
                    wmsElevations.setExpiryTime(System.currentTimeMillis() - fiveDayMillis);
                    dynamicLayer.setElevationModel(wmsElevations);
                }
            }

            return dynamicLayer;
        }

        protected void initialize(boolean includeStatusBar, boolean includeLayerPanel, boolean includeStatsPanel) {
            // Create the WorldWindow.
            this.wwjPanel = AppFrame.createAppPanel(this.canvasSize, includeStatusBar);
            this.wwjPanel.setPreferredSize(canvasSize);

            // Put the pieces together.
            this.getContentPane().add(wwjPanel, BorderLayout.CENTER);
            if (includeLayerPanel) {
                this.controlPanel = new JPanel(new BorderLayout(10, 10));
                this.layerPanel = new LayerPanel(this.getWwd());
                this.controlPanel.add(this.layerPanel, BorderLayout.CENTER);
                this.controlPanel.add(new FlatWorldPanel(this.getWwd()), BorderLayout.NORTH);
                this.getContentPane().add(this.controlPanel, BorderLayout.WEST);
            }

            if (includeStatsPanel || System.getProperty("gov.nasa.worldwind.showStatistics") != null) {
                this.statsPanel = new StatisticsPanel(this.wwjPanel.getWwd(), new Dimension(250, canvasSize.height));
                this.getContentPane().add(this.statsPanel, BorderLayout.EAST);
            }

            // Create and install the view controls layer and register a controller for it with the WorldWindow.
            ViewControlsLayer viewControlsLayer = new ViewControlsLayer();
            insertBeforeCompass(getWwd(), viewControlsLayer);
            this.getWwd().addSelectListener(new ViewControlsLayer.ViewControlsSelectListener(this.getWwd(), viewControlsLayer));

            // Register a rendering exception listener that's notified when exceptions occur during rendering.
            this.wwjPanel.getWwd().addRenderingExceptionListener((Throwable t) -> {
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
            for (Layer layer : this.wwjPanel.getWwd().model().getLayers()) {
                if (layer instanceof SelectListener) {
                    this.getWwd().addSelectListener((SelectListener) layer);
                }
            }

            this.pack();

            // Center the application on the screen.
            WWUtil.alignComponent(null, this, Keys.CENTER);
            this.setResizable(true);
            Position eyePos = new Position(Angle.fromDegreesLatitude(32.897), Angle.fromDegreesLongitude(-97.04),
                500.0); // DFW
            this.wwjPanel.getWwd().view().setEyePosition(eyePos);
        }

        protected static AppPanel createAppPanel(Dimension canvasSize, boolean includeStatusBar) {
            return new AppPanel(canvasSize, includeStatusBar);
        }

        public Dimension getCanvasSize() {
            return canvasSize;
        }

        public AppPanel getWwjPanel() {
            return wwjPanel;
        }

        public WorldWindow getWwd() {
            return this.wwjPanel.getWwd();
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

        public StatisticsPanel getStatsPanel() {
            return statsPanel;
        }

        public void setToolTipController(ToolTipController controller) {
            if (this.wwjPanel.toolTipController != null) {
                this.wwjPanel.toolTipController.dispose();
            }

            this.wwjPanel.toolTipController = controller;
        }

        public void setHighlightController(HighlightController controller) {
            if (this.wwjPanel.highlightController != null) {
                this.wwjPanel.highlightController.dispose();
            }

            this.wwjPanel.highlightController = controller;
        }
    }
}