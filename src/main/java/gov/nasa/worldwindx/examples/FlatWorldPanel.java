/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwindx.examples;

import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.globes.*;
import gov.nasa.worldwind.globes.projections.*;
import gov.nasa.worldwind.terrain.ZeroElevationModel;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;

/**
 * Panel to control a flat or round world projection. The panel includes a radio button to switch between flat and round
 * globes, and a list box of map projections for the flat globe. The panel is attached to a WorldWindow, and changes the
 * WorldWindow to match the users globe selection.
 *
 * @author Patrick Murris
 * @version $Id: FlatWorldPanel.java 2419 2014-11-08 04:44:55Z tgaskins $
 */
@SuppressWarnings("unchecked")
public class FlatWorldPanel extends JPanel {
    private final WorldWindow wwd;
    private final Globe roundGlobe;
    private final FlatGlobe flatGlobe;
    private JComboBox projectionCombo;

    public FlatWorldPanel(WorldWindow wwd) {
        super(new GridLayout(0, 2, 0, 0));
        this.wwd = wwd;
        if (isFlatGlobe()) {
            this.flatGlobe = (FlatGlobe) wwd.getModel().getGlobe();
            this.roundGlobe = new Earth();
        }
        else {
            this.flatGlobe = new EarthFlat();
            this.roundGlobe = wwd.getModel().getGlobe();
        }
        this.flatGlobe.setElevationModel(new ZeroElevationModel());
        this.makePanel();
    }

    private JPanel makePanel() {
        JPanel controlPanel = this;
        controlPanel.setBorder(
            new CompoundBorder(BorderFactory.createEmptyBorder(9, 9, 9, 9), new TitledBorder("Globe")));
        controlPanel.setToolTipText("Set the current projection");

        // Flat vs round buttons
        JPanel radioButtonPanel = new JPanel(new GridLayout(0, 2, 0, 0));
        JRadioButton roundRadioButton = new JRadioButton("Round");
        roundRadioButton.setSelected(!isFlatGlobe());
        roundRadioButton.addActionListener(event -> {
            projectionCombo.setEnabled(false);
            enableFlatGlobe(false);
        });
        radioButtonPanel.add(roundRadioButton);
        JRadioButton flatRadioButton = new JRadioButton("Flat");
        flatRadioButton.setSelected(isFlatGlobe());
        flatRadioButton.addActionListener(event -> {
            projectionCombo.setEnabled(true);
            enableFlatGlobe(true);
        });
        radioButtonPanel.add(flatRadioButton);
        ButtonGroup group = new ButtonGroup();
        group.add(roundRadioButton);
        group.add(flatRadioButton);

        // Projection combo
        JPanel comboPanel = new JPanel(new GridLayout(0, 1, 0, 0));
        this.projectionCombo = new JComboBox(new String[]
            {"Lat-Lon", "Mercator", "Modified Sin.", "Sinusoidal",
                "Transverse Mercator",
                "North Polar",
                "South Polar",
                "UPS North",
                "UPS South"
            });
        this.projectionCombo.setEnabled(isFlatGlobe());
        this.projectionCombo.addActionListener(actionEvent -> updateProjection());
        comboPanel.add(this.projectionCombo);

        controlPanel.add(radioButtonPanel);
        controlPanel.add(comboPanel);
        return controlPanel;
    }

    // Update flat globe projection
    private void updateProjection() {
        if (!isFlatGlobe())
            return;

        this.flatGlobe.setProjection(this.getProjection());

        this.wwd.redraw();
    }

    private GeographicProjection getProjection() {
        String item = (String) projectionCombo.getSelectedItem();
        return switch (item) {
            case "Mercator" -> new ProjectionMercator();
            case "Sinusoidal" -> new ProjectionSinusoidal();
            case "Modified Sin." -> new ProjectionModifiedSinusoidal();
            case "Transverse Mercator" -> new ProjectionTransverseMercator(
                wwd.getView().getCurrentEyePosition().getLongitude());
            case "North Polar" -> new ProjectionPolarEquidistant(AVKey.NORTH);
            case "South Polar" -> new ProjectionPolarEquidistant(AVKey.SOUTH);
            case "UPS North" -> new ProjectionUPS(AVKey.NORTH);
            case "UPS South" -> new ProjectionUPS(AVKey.SOUTH);
            default -> new ProjectionEquirectangular();
        };
        // Default to lat-lon
    }

    public boolean isFlatGlobe() {
        return wwd.getModel().getGlobe() instanceof FlatGlobe;
    }

    public void enableFlatGlobe(boolean flat) {
        if (isFlatGlobe() == flat)
            return;

        if (!flat) {
            // Switch to round globe
            wwd.getModel().setGlobe(roundGlobe);
            wwd.getView().stopMovement();
        }
        else {
            // Switch to flat globe
            wwd.getModel().setGlobe(flatGlobe);
            wwd.getView().stopMovement();
            this.updateProjection();
        }

        wwd.redraw();
    }
}
