/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */

package gov.nasa.worldwind.examples;

import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.layers.SurfaceImageLayer;
import gov.nasa.worldwind.terrain.*;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;

/**
 * Open and view arbitrary surface images and elevations that have an accompanying world file.
 * <p>
 * After clicking the open button and selecting the desired image or elevations file, the program status will change to
 * Loading while WorldWind installs the selected data.  Wait until the status changes to Ready. The data will have
 * finished installing and will be ready for viewing.
 * <p>
 * Image and elevation files that you wish to load must be accompanied by a world file, or they will fail to load. The
 * world file can be identified as the file with a file extension consisting of three letters.  The first two of these
 * will be the first and last letters of the image or elevation file type, e.g. tf for a tiff file, or jg for a jpeg
 * file.  The last letter will be a double.
 * <p>
 * For example, a world file accompanying a jpeg file would have the extension .jgw :
 * <p>
 * image.jpg           // image file image.jgw           // accompanying world file
 *
 * @author tag
 * @version $Id: SurfaceImageViewer.java 2109 2014-06-30 16:52:38Z tgaskins $
 */
public class SurfaceImageViewer extends ApplicationTemplate {
    public static void main(String[] args) {
        ApplicationTemplate.start("WorldWind Surface Images", SurfaceImageViewer.AppFrame.class);
    }

    public static class AppFrame extends ApplicationTemplate.AppFrame {
        private final JFileChooser fileChooser = new JFileChooser();
        final Action openElevationsAction = new AbstractAction("Open Elevation File...") {
            public void actionPerformed(ActionEvent e) {
                int status = fileChooser.showOpenDialog(AppFrame.this);
                if (status != JFileChooser.APPROVE_OPTION)
                    return;

                final File imageFile = fileChooser.getSelectedFile();
                if (imageFile == null)
                    return;

                Thread t = new Thread(() -> {
                    try {
                        CompoundElevationModel cem
                            = (CompoundElevationModel) wwd().model().globe().getElevationModel();
                        LocalElevationModel em = new LocalElevationModel();
                        em.addElevations(imageFile.getPath());
                        cem.addElevationModel(em);
                    }
                    catch (IOException e1) {
                        e1.printStackTrace();
                    }
                });
                t.setPriority(Thread.MIN_PRIORITY);
                t.start();
            }
        };
        private final JLabel statusLabel = new JLabel("status: ready");
        private JSlider opacitySlider;
        private SurfaceImageLayer layer;
        final Action openImageAction = new AbstractAction("Open Image File...") {
            public void actionPerformed(ActionEvent actionEvent) {
                int status = fileChooser.showOpenDialog(AppFrame.this);
                if (status != JFileChooser.APPROVE_OPTION)
                    return;

                final File imageFile = fileChooser.getSelectedFile();
                if (imageFile == null)
                    return;

                Thread t = new Thread(() -> {
//                    try
                    {
                        statusLabel.setText("status: Loading image");
                        // TODO: proper threading
                        layer.addImage(imageFile.getAbsolutePath());

                        wwd().redraw();
                        statusLabel.setText("status: ready");
                    }
//                    catch (IOException e)
                });
                t.setPriority(Thread.MIN_PRIORITY);
                t.start();
            }
        };

        public AppFrame() {
            super(true, true, false);

            try {
                this.layer = new SurfaceImageLayer();
                this.layer.setOpacity(1);
                this.layer.setPickEnabled(false);
                this.layer.setName("Surface Images");

                WorldWindow.insertBeforeCompass(this.wwd(), layer);

                this.getControlPanel().add(makeControlPanel(), BorderLayout.SOUTH);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }

        private JPanel makeControlPanel() {
            JPanel controlPanel = new JPanel(new GridLayout(0, 1, 5, 5));
            JButton openImageButton = new JButton(openImageAction);
            controlPanel.add(openImageButton);

            this.opacitySlider = new JSlider();
            this.opacitySlider.setMaximum(100);
            this.opacitySlider.setValue((int) (layer.getOpacity() * 100));
            this.opacitySlider.setEnabled(true);
            this.opacitySlider.addChangeListener(e -> {
                int value = opacitySlider.getValue();
                layer.setOpacity(value / 100.0d);
                wwd().redraw();
            });
            JPanel opacityPanel = new JPanel(new BorderLayout(5, 5));
            opacityPanel.setBorder(new EmptyBorder(0, 10, 0, 0));
            opacityPanel.add(new JLabel("Opacity"), BorderLayout.WEST);
            opacityPanel.add(this.opacitySlider, BorderLayout.CENTER);

            controlPanel.add(opacityPanel);

            JButton openElevationsButton = new JButton(openElevationsAction);
            controlPanel.add(openElevationsButton);

            controlPanel.add(statusLabel);
            controlPanel.setBorder(new EmptyBorder(15, 15, 15, 15));

            return controlPanel;
        }
    }
}