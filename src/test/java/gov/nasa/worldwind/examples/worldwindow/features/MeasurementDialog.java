/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */

package gov.nasa.worldwind.examples.worldwindow.features;

import gov.nasa.worldwind.event.SelectEvent;
import gov.nasa.worldwind.examples.worldwindow.core.*;
import gov.nasa.worldwind.layers.RenderableLayer;
import gov.nasa.worldwind.render.*;

import javax.swing.*;
import java.awt.*;
import java.awt.image.*;

/**
 * @author tag
 * @version $Id: MeasurementDialog.java 1171 2013-02-11 21:45:02Z dcollins $
 */
public class MeasurementDialog extends AbstractFeatureDialog {
    private static final String LAYER_NAME = "Measurement";
    private static final Color[] colors = new Color[]
        {
            Color.RED, Color.GREEN, Color.BLUE, Color.MAGENTA, Color.CYAN, Color.ORANGE, Color.PINK, Color.YELLOW
        };
    private static int nextColor = 0;
    private JTabbedPane tabbedPane = new JTabbedPane();
    private int labelSequence = 0;
    private RenderableLayer shapeLayer;
    private RenderableLayer controlPointsLayer;

    public MeasurementDialog(Registry registry) {
        super("Measurement", Constants.FEATURE_MEASUREMENT_DIALOG, registry);
    }

    private static Color getNextColor() {
        return colors[nextColor++ % colors.length];
    }

    private static Icon makeColorCircle(Color color) {
        BufferedImage bi = PatternFactory.createPattern(
            PatternFactory.PATTERN_CIRCLE, new Dimension(16, 16), 0.9f, color);

        return new ImageIcon(bi);
    }

    public void initialize(final Controller controller) {
        super.initialize(controller);

        this.shapeLayer = new RenderableLayer();
        this.shapeLayer.setName(LAYER_NAME);
        this.controller.addInternalActiveLayer(shapeLayer);

        this.controlPointsLayer = this.shapeLayer; // use same layer for both in MeasureTool

        this.tabbedPane = new JTabbedPane();
        this.tabbedPane.setOpaque(false);

        this.tabbedPane.add(new JPanel());
        this.tabbedPane.setTitleAt(0, "+");
        this.tabbedPane.setToolTipTextAt(0, "Create measurement");

        this.tabbedPane.addChangeListener(changeEvent -> {
            if (tabbedPane.getSelectedIndex() == 0) {
                addNewPanel(tabbedPane); // Add new panel when '+' is selected
            }
        });

        // Add an initial measure panel to tabbed pane
        this.addNewPanel(this.tabbedPane);
        tabbedPane.setSelectedIndex(1);

        this.setTaskComponent(this.tabbedPane);

        this.setLocation(SwingConstants.WEST, SwingConstants.NORTH);
        this.getJDialog().setResizable(true);

        JButton deleteButton = new JButton(
            new ImageIcon(
                this.getClass().getResource("/gov/nasa/worldwind/examples/worldwindow/images/delete-20x20.png")));
        deleteButton.setToolTipText("Remove current measurement");
        deleteButton.setOpaque(false);
        deleteButton.setBackground(new Color(0, 0, 0, 0));
        deleteButton.setBorderPainted(false);
        deleteButton.addActionListener(e -> {
            deleteCurrentPanel();
            controller.redraw();
        });
        deleteButton.setEnabled(true);
        this.insertLeftDialogComponent(deleteButton);
    }

    @Override
    public void setVisible(boolean tf) {
        // Hide the shape layer if it's empty when the dialog closes. There will be fewer than 3 renderables in that
        // case: the control points and the annotation.
        if (!tf && countRenderables(this.shapeLayer) < 3) {
            this.controller.getActiveLayers().remove(this.shapeLayer);
        }

        // Un-hide the shape layer when the dialog is raised
        if (tf) {
            if (!this.controller.getActiveLayers().contains(this.shapeLayer))
                this.controller.addInternalActiveLayer(this.shapeLayer);
        }

        super.setVisible(tf);
    }

    private static int countRenderables(RenderableLayer layer) {
        int count = 0;

        //noinspection UnusedDeclaration
        for (Renderable r : layer.all()) {
            ++count;
        }

        return count;
    }

    private void deleteCurrentPanel() {
        MeasurementPanel mp = getCurrentPanel();
        if (tabbedPane.getTabCount() > 2) {
            mp.deletePanel();
            tabbedPane.remove(tabbedPane.getSelectedComponent());
        }
        else {
            mp.clearPanel();
        }
    }

    private void addNewPanel(JTabbedPane tabPane) {
        final MeasurementPanel mp = new MeasurementPanel(null);
        mp.initialize(this.controller);
        mp.setLayers(this.shapeLayer, this.controlPointsLayer);

        Color color = getNextColor();
        mp.setLineColor(color);
        mp.setFillColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 32));

        tabPane.addTab(String.valueOf(++this.labelSequence), makeColorCircle(color), mp.getJPanel());
        tabPane.setSelectedIndex(tabPane.getTabCount() - 1);
        tabPane.setToolTipTextAt(tabbedPane.getSelectedIndex(), "Select measurement");

        this.controller.getWWd().addSelectListener(event -> {
            if (event.getEventAction().equals(SelectEvent.LEFT_CLICK)) {
                if (mp.getShape() == null || mp.getShape() != event.getTopObject())
                    return;

                for (Component c : tabbedPane.getComponents()) {
                    if (!(c instanceof JComponent))
                        continue;

                    Object o = ((JComponent) c).getClientProperty(Constants.FEATURE_OWNER_PROPERTY);
                    if (o instanceof MeasurementPanel && o == mp) {
                        tabbedPane.setSelectedComponent(c);
                    }
                }
            }
        });
    }

    private MeasurementPanel getCurrentPanel() {
        JComponent p = (JComponent) tabbedPane.getSelectedComponent();
        return (MeasurementPanel) p.getClientProperty(Constants.FEATURE_OWNER_PROPERTY);
    }
}
