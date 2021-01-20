/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */

package gov.nasa.worldwind.util.dashboard;

import gov.nasa.worldwind.*;
import gov.nasa.worldwind.event.RenderingEvent;
import gov.nasa.worldwind.util.*;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;

/**
 * @author tag
 * @version $Id: DashboardDialog.java 1171 2013-02-11 21:45:02Z dcollins $
 */
public class DashboardDialog extends JDialog {
    private WorldWindow wwd;
    private boolean runContinuously;

    public DashboardDialog(Frame parent, WorldWindow wwd) throws HeadlessException {
        super(parent, "WWJ Dashboard");

        this.wwd = wwd;

        this.getContentPane().setLayout(new BorderLayout());
        this.getContentPane().add(this.createControls(), BorderLayout.CENTER);
        this.pack();

        this.setResizable(false);
        this.setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);

        wwd.addRenderingListener(event -> {
            if (runContinuously && event.getStage().equals(RenderingEvent.AFTER_BUFFER_SWAP)
                && event.getSource() instanceof WorldWindow) {
                ((WorldWindow) event.getSource()).redraw();
            }
        });
    }

    @Override
    public void dispose() {
        super.dispose();

        this.wwd = null;
    }

    public void raiseDialog() {
        makeCurrent();
        WWUtil.alignComponent(this.getParent(), this, Keys.RIGHT);
        this.setVisible(true);
    }

    public void lowerDialog() {
        setVisible(false);
    }

    private void makeCurrent() {
    }

    private JTabbedPane createControls() {
        JTabbedPane tabPane = new JTabbedPane();

        JPanel panel = new JPanel(new BorderLayout(10, 20));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));
        panel.add(makeControlPanel(), BorderLayout.CENTER);
        panel.add(makeOkayCancelPanel(), BorderLayout.SOUTH);
        tabPane.add("Controls", panel);

        tabPane.add("Performance", new StatisticsPanel(this.wwd, new Dimension(250, 500)));

        return tabPane;
    }

    private JPanel makeControlPanel() {
        JPanel panel = new JPanel(new FlowLayout(SwingConstants.VERTICAL));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        panel.add(makeTerrainControlPanel());

        return panel;
    }

    private JPanel makeTerrainControlPanel() {
        JPanel panel = new JPanel(new GridLayout(0, 1, 5, 5));

        panel.setBorder(new CompoundBorder(new TitledBorder("Terrain"), new EmptyBorder(10, 10, 10, 10)));

        final JRadioButton triangleButton = new JRadioButton("Show Triangles w/o Skirts");
        panel.add(triangleButton);

        final JRadioButton skirtsButton = new JRadioButton("Make Triangles w/ Skirts");
        panel.add(skirtsButton);

        JCheckBox tileButton = new JCheckBox("Show Tile Boundaries");
        panel.add(tileButton);

        JCheckBox extentsButton = new JCheckBox("Show Tile Extents");
        panel.add(extentsButton);

        final JCheckBox runContinuouslyButton = new JCheckBox("Run Continuously");
        panel.add(runContinuouslyButton);

        ActionListener listener = e -> {
            boolean tris = triangleButton.isSelected();
            boolean skirts = skirtsButton.isSelected();

            if (tris && e.getSource() == triangleButton) {
                wwd.model().setShowWireframeInterior(true);
                wwd.model().getGlobe().getTessellator().setMakeTileSkirts(false);
                skirtsButton.setSelected(false);
            } else if (skirts && e.getSource() == skirtsButton) {
                wwd.model().setShowWireframeInterior(true);
                wwd.model().getGlobe().getTessellator().setMakeTileSkirts(true);
                triangleButton.setSelected(false);
            } else {
                wwd.model().setShowWireframeInterior(false);
                wwd.model().getGlobe().getTessellator().setMakeTileSkirts(true);
            }

            wwd.redraw();
        };
        triangleButton.addActionListener(listener);
        skirtsButton.addActionListener(listener);

        tileButton.addActionListener(e -> {
            wwd.model().setShowWireframeExterior(!wwd.model().isShowWireframeExterior());
            wwd.redraw();
        });

        extentsButton.addActionListener(e -> {
            wwd.model().setShowTessellationBoundingVolumes(!wwd.model().isShowTessellationBoundingVolumes());
            wwd.redraw();
        });

        runContinuouslyButton.addActionListener(e -> {
            runContinuously = runContinuouslyButton.isSelected();
            wwd.redraw();
        });

        return panel;
    }

    private JPanel makeOkayCancelPanel() {
        JPanel panel = new JPanel(new GridLayout(1, 3, 10, 10));

        panel.add(new JLabel(""));
        panel.add(new JButton(new OkayAction()));

        return panel;
    }

    private class OkayAction extends AbstractAction {
        public OkayAction() {
            super("Okay");
        }

        public void actionPerformed(ActionEvent e) {
            lowerDialog();
        }
    }
}