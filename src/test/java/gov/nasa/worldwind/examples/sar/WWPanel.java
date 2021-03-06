/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.examples.sar;

import gov.nasa.worldwind.*;
import gov.nasa.worldwind.layers.*;
import gov.nasa.worldwind.layers.tool.ScalebarLayer;
import gov.nasa.worldwind.util.StatusBar;
import gov.nasa.worldwind.video.awt.WorldWindowGLCanvas;

import javax.swing.*;
import javax.swing.plaf.basic.*;
import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeListener;

/**
 * @author tag
 * @version $Id: WWPanel.java 1171 2013-02-11 21:45:02Z dcollins $
 */
public class WWPanel extends JPanel {
    private final FocusablePanel panel;
    private final WorldWindowGLCanvas wwd;
    private final StatusBar statusBar;
    private final PropertyChangeListener propertyChangeListener = propertyChangeEvent -> {
        if (propertyChangeEvent.getPropertyName() == SARKey.ELEVATION_UNIT)
            updateElevationUnit(propertyChangeEvent.getNewValue());
        if (propertyChangeEvent.getPropertyName() == SARKey.ANGLE_FORMAT)
            updateAngleFormat(propertyChangeEvent.getNewValue());
    };
    private final FocusListener focusListener = new FocusListener() {
        public void focusGained(FocusEvent focusEvent) {
            this.focusChanged(focusEvent);
        }

        public void focusLost(FocusEvent focusEvent) {
            this.focusChanged(focusEvent);
        }

        protected void focusChanged(FocusEvent focusEvent) {
            repaint();
        }
    };

    public WWPanel() {
        super(new BorderLayout(0, 0)); // hgap, vgap

        this.wwd = new WorldWindowGLCanvas();
        this.wwd.setPreferredSize(new Dimension(800, 800));

        // Create the default model as described in the current worldwind properties.
        Model m = (Model) WorldWind.createConfigurationComponent(Keys.MODEL_CLASS_NAME);
        this.wwd.setModel(m);

        this.wwd.addPropertyChangeListener(this.propertyChangeListener);
        this.wwd.addFocusListener(this.focusListener);
        this.wwd.setFocusable(true);

        this.statusBar = new StatusBar();
        this.statusBar.setEventSource(wwd);

        this.panel = new FocusablePanel(new BorderLayout(0, 0), this.wwd); // hgap, vgap
        this.panel.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        this.panel.add(this.wwd, BorderLayout.CENTER);
        this.add(this.panel, BorderLayout.CENTER);
        this.add(this.statusBar, BorderLayout.PAGE_END);
    }

    public WorldWindowGLCanvas getWwd() {
        return wwd;
    }

    public StatusBar getStatusBar() {
        return statusBar;
    }

    private void updateElevationUnit(Object newValue) {
        for (Layer layer : this.wwd.model().layers()) {
            if (layer instanceof ScalebarLayer) {
                // Default to metric units.
                ((ScalebarLayer) layer).setUnit(
                    SAR2.UNIT_IMPERIAL.equals(newValue) ? ScalebarLayer.UNIT_IMPERIAL : ScalebarLayer.UNIT_METRIC);
            }else if (layer instanceof TerrainProfileLayer) {
                // Default to metric units.
                ((TerrainProfileLayer) layer).setUnit(
                    SAR2.UNIT_IMPERIAL.equals(newValue) ? TerrainProfileLayer.UNIT_IMPERIAL
                        : TerrainProfileLayer.UNIT_METRIC);
            }
        }

        if (SAR2.UNIT_IMPERIAL.equals(newValue))
            this.statusBar.setElevationUnit(StatusBar.UNIT_IMPERIAL);
        else // Default to metric units.
            this.statusBar.setElevationUnit(StatusBar.UNIT_METRIC);
    }

    private void updateAngleFormat(Object newValue) {
        this.statusBar.setAngleFormat((String) newValue);
    }

    protected static class FocusablePanel extends JPanel {
        private final Component focusContext;

        public FocusablePanel(LayoutManager layoutManager, Component focusContext) {
            super(layoutManager);
            this.focusContext = focusContext;
        }

        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);

            if (this.focusContext.isFocusOwner()) {
                Rectangle bounds = this.getBounds();
                BasicGraphicsUtils.drawDashedRect(graphics, 0, 0, bounds.width, bounds.height);
            }
        }
    }
}