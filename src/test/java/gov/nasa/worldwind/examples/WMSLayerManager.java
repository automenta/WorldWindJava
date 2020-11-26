/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.examples;

import javax.swing.*;
import java.awt.*;
import java.net.URISyntaxException;

/**
 * This example demonstrates the use of multiple WMS layers, as displayed in a WMSLayersPanel.
 *
 * @author tag
 * @version $Id: WMSLayerManager.java 2109 2014-06-30 16:52:38Z tgaskins $
 */
public class WMSLayerManager {
    protected static final String[] servers = new String[]
        {
            "https://neowms.sci.gsfc.nasa.gov/wms/wms",
            "https://sedac.ciesin.columbia.edu/geoserver/wcs"
        };

    public static void main(String[] args) {
        ApplicationTemplate.start("WorldWind WMS Layers", AppFrame.class);
    }

    protected static class AppFrame extends ApplicationTemplate.AppFrame {
        protected final Dimension wmsPanelSize = new Dimension(400, 600);
        protected final JTabbedPane tabbedPane;
        protected int previousTabIndex;

        public AppFrame() {
            this.tabbedPane = new JTabbedPane();

            this.tabbedPane.add(new JPanel());
            this.tabbedPane.setTitleAt(0, "+");
            this.tabbedPane.addChangeListener(changeEvent -> {
                if (tabbedPane.getSelectedIndex() != 0) {
                    previousTabIndex = tabbedPane.getSelectedIndex();
                    return;
                }

                String server = JOptionPane.showInputDialog("Enter wms server URL");
                if (server == null || server.length() < 1) {
                    tabbedPane.setSelectedIndex(previousTabIndex);
                    return;
                }

                // Respond by adding a new WMSLayerPanel to the tabbed pane.
                if (addTab(tabbedPane.getTabCount(), server.trim()) != null)
                    tabbedPane.setSelectedIndex(tabbedPane.getTabCount() - 1);
            });

            // Create a tab for each server and add it to the tabbed panel.
            for (int i = 0; i < servers.length; i++) {
                this.addTab(i + 1, servers[i]); // i+1 to place all server tabs to the right of the Add Server tab
            }

            // Display the first server pane by default.
            this.tabbedPane.setSelectedIndex(this.tabbedPane.getTabCount() > 0 ? 1 : 0);
            this.previousTabIndex = this.tabbedPane.getSelectedIndex();

            // Add the tabbed pane to a frame separate from the WorldWindow.
            JFrame controlFrame = new JFrame();
            controlFrame.getContentPane().add(tabbedPane);
            controlFrame.pack();
            controlFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            controlFrame.setVisible(true);
        }

        protected WMSLayersPanel addTab(int position, String server) {
            // Add a server to the tabbed dialog.
            try {
                WMSLayersPanel layersPanel = new WMSLayersPanel(AppFrame.this.getWwd(), server, wmsPanelSize);
                this.tabbedPane.add(layersPanel, BorderLayout.CENTER);
                String title = layersPanel.getServerDisplayString();
                this.tabbedPane.setTitleAt(position, title != null && !title.isEmpty() ? title : server);

                return layersPanel;
            }
            catch (URISyntaxException e) {
                JOptionPane.showMessageDialog(null, "Server URL is invalid", "Invalid Server URL",
                    JOptionPane.ERROR_MESSAGE);
                tabbedPane.setSelectedIndex(previousTabIndex);
                return null;
            }
        }
    }
}
