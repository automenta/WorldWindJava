/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.examples;

import gov.nasa.worldwind.Keys;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.layers.tool.*;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;

/**
 * Shows the {@link ViewControlsLayer} and allows you to adjust its size, orientation, and
 * available controls.
 *
 * @author Patrick Murris
 * @version $Id: ViewControls.java 2109 2014-06-30 16:52:38Z tgaskins $
 * @see ViewControlsLayer
 * @see ViewControlsLayer.ViewControlsSelectListener
 * @see CompassLayer
 */
public class ViewControls extends ApplicationTemplate {

    public static void main(String[] args) {
        ApplicationTemplate.start("WorldWind View Controls", AppFrame.class);
    }

    public static class AppFrame extends ApplicationTemplate.AppFrame {

        protected ViewControlsLayer viewControlsLayer;

        public AppFrame() {
            super(true, true, false);

            // Find ViewControls layer and keep reference to it
            for (Layer layer : this.wwd().model().layers()) {
                if (layer instanceof ViewControlsLayer) {
                    viewControlsLayer = (ViewControlsLayer) layer;
                }
            }

            // Add view controls selection panel
            this.getControlPanel().add(makeControlPanel(), BorderLayout.SOUTH);
        }

        private JPanel makeControlPanel() {
            JPanel controlPanel = new JPanel();
            controlPanel.setLayout(new BoxLayout(controlPanel, BoxLayout.Y_AXIS));
            controlPanel.setBorder(
                new CompoundBorder(BorderFactory.createEmptyBorder(9, 9, 9, 9), new TitledBorder("View Controls")));
            controlPanel.setToolTipText("Select active view controls");

            // Radio buttons - layout
            JPanel layoutPanel = new JPanel(new GridLayout(0, 2, 0, 0));
            layoutPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            ButtonGroup group = new ButtonGroup();
            JRadioButton button = new JRadioButton("Horizontal", true);
            group.add(button);
            button.addActionListener((ActionEvent actionEvent) -> {
                viewControlsLayer.setLayout(Keys.HORIZONTAL);
                wwd().redraw();
            });
            layoutPanel.add(button);
            button = new JRadioButton("Vertical", false);
            group.add(button);
            button.addActionListener((ActionEvent actionEvent) -> {
                viewControlsLayer.setLayout(Keys.VERTICAL);
                wwd().redraw();
            });
            layoutPanel.add(button);

            // Scale slider
            JPanel scalePanel = new JPanel(new GridLayout(0, 1, 0, 0));
            scalePanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            scalePanel.add(new JLabel("Scale:"));
            JSlider scaleSlider = new JSlider(1, 20, 10);
            scaleSlider.addChangeListener((ChangeEvent event) -> {
                viewControlsLayer.setScale(((JSlider) event.getSource()).getValue() / 10.0d);
                wwd().redraw();
            });
            scalePanel.add(scaleSlider);

            // Check boxes
            JPanel checkPanel = new JPanel(new GridLayout(0, 2, 0, 0));
            checkPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

            JCheckBox check = new JCheckBox("Pan");
            check.setSelected(true);
            check.addActionListener((ActionEvent actionEvent) -> {
                viewControlsLayer.setShowPanControls(((AbstractButton) actionEvent.getSource()).isSelected());
                wwd().redraw();
            });
            checkPanel.add(check);

            check = new JCheckBox("Look");
            check.setSelected(false);
            check.addActionListener((ActionEvent actionEvent) -> {
                viewControlsLayer.setShowLookControls(((AbstractButton) actionEvent.getSource()).isSelected());
                wwd().redraw();
            });
            checkPanel.add(check);

            check = new JCheckBox("Zoom");
            check.setSelected(true);
            check.addActionListener((ActionEvent actionEvent) -> {
                viewControlsLayer.setShowZoomControls(((AbstractButton) actionEvent.getSource()).isSelected());
                wwd().redraw();
            });
            checkPanel.add(check);

            check = new JCheckBox("Heading");
            check.setSelected(true);
            check.addActionListener((ActionEvent actionEvent) -> {
                viewControlsLayer.setShowHeadingControls(((AbstractButton) actionEvent.getSource()).isSelected());
                wwd().redraw();
            });
            checkPanel.add(check);

            check = new JCheckBox("Pitch");
            check.setSelected(true);
            check.addActionListener((ActionEvent actionEvent) -> {
                viewControlsLayer.setShowPitchControls(((AbstractButton) actionEvent.getSource()).isSelected());
                wwd().redraw();
            });
            checkPanel.add(check);

            check = new JCheckBox("Field of view");
            check.setSelected(false);
            check.addActionListener((ActionEvent actionEvent) -> {
                viewControlsLayer.setShowFovControls(((AbstractButton) actionEvent.getSource()).isSelected());
                wwd().redraw();
            });
            checkPanel.add(check);

            controlPanel.add(layoutPanel);
            controlPanel.add(scalePanel);
            controlPanel.add(checkPanel);
            return controlPanel;
        }
    }
}