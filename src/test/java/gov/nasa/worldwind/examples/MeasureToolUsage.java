/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.examples;

import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.layers.TerrainProfileLayer;
import gov.nasa.worldwind.util.measure.*;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.beans.*;
import java.util.ArrayList;

/**
 * Example usage of MeasureTool to draw a shape on the globe and measure length, area, etc. Click the "New" button, and
 * then click and drag on the globe to define a shape. The panel on the left shows the shape's measurement.
 *
 * @author Patrick Murris
 * @version $Id: MeasureToolUsage.java 2117 2014-07-01 20:36:49Z tgaskins $
 * @see MeasureTool
 * @see MeasureToolController
 * @see MeasureToolPanel
 */
public class MeasureToolUsage extends ApplicationTemplate {

    public static void main(String[] args) {
        ApplicationTemplate.start("WorldWind Measure Tool", MeasureToolUsage.AppFrame.class);
    }

    public static class AppFrame extends ApplicationTemplate.AppFrame {

        private final JTabbedPane tabbedPane = new JTabbedPane();
        private final TerrainProfileLayer profile = new TerrainProfileLayer();
        private final PropertyChangeListener measureToolListener = new MeasureToolListener();
        private int lastTabIndex = -1;

        public AppFrame() {
            super(true, true, false); // no layer or statistics panel

            // Add terrain profile layer
            profile.setEventSource(wwd());
            profile.setFollow(TerrainProfileLayer.FOLLOW_PATH);
            profile.setShowProfileLine(false);
            WorldWindow.insertBeforePlacenames(wwd(), profile);

            // Add + tab
            tabbedPane.add(new JPanel());
            tabbedPane.setTitleAt(0, "+");
            tabbedPane.addChangeListener((ChangeEvent changeEvent) -> {
                if (tabbedPane.getSelectedIndex() == 0) {
                    // Add new measure tool in a tab when '+' selected
                    MeasureTool measureTool = new MeasureTool(wwd());
                    measureTool.setController(new MeasureToolController());
                    tabbedPane.add(new MeasureToolPanel(wwd(), measureTool));
                    tabbedPane.setTitleAt(tabbedPane.getTabCount() - 1, String.valueOf(tabbedPane.getTabCount() - 1));
                    tabbedPane.setSelectedIndex(tabbedPane.getTabCount() - 1);
                    switchMeasureTool();
                }
                else {
                    switchMeasureTool();
                }
            });

            // Add measure tool control panel to tabbed pane
            MeasureTool measureTool = new MeasureTool(this.wwd());
            measureTool.setController(new MeasureToolController());
            tabbedPane.add(new MeasureToolPanel(this.wwd(), measureTool));
            tabbedPane.setTitleAt(1, "1");
            tabbedPane.setSelectedIndex(1);
            switchMeasureTool();

            this.getControlPanel().add(tabbedPane, BorderLayout.EAST);
            this.pack();
        }

        private void switchMeasureTool() {
            // Disarm last measure tool when changing tab and switching tool
            if (lastTabIndex != -1) {
                MeasureTool mt = ((MeasureToolPanel) tabbedPane.getComponentAt(lastTabIndex)).getMeasureTool();
                mt.setArmed(false);
                mt.removePropertyChangeListener(measureToolListener);
            }
            // Update terrain profile from current measure tool
            lastTabIndex = tabbedPane.getSelectedIndex();
            MeasureTool mt = ((MeasureToolPanel) tabbedPane.getComponentAt(lastTabIndex)).getMeasureTool();
            mt.addPropertyChangeListener(measureToolListener);
            updateProfile(mt);
        }

        private void updateProfile(MeasureTool mt) {
            ArrayList<? extends LatLon> positions = mt.getPositions();
            if (positions != null && positions.size() > 1) {
                profile.setPathPositions(positions);
                profile.setEnabled(true);
            }
            else {
                profile.setEnabled(false);
            }

            wwd().redraw();
        }

        private class MeasureToolListener implements PropertyChangeListener {

            @Override
            public void propertyChange(PropertyChangeEvent event) {
                // Measure shape position list changed - update terrain profile
                if (event.getPropertyName().equals(MeasureTool.EVENT_POSITION_ADD)
                    || event.getPropertyName().equals(MeasureTool.EVENT_POSITION_REMOVE)
                    || event.getPropertyName().equals(MeasureTool.EVENT_POSITION_REPLACE)) {
                    updateProfile(((MeasureTool) event.getSource()));
                }
            }
        }
    }
}
