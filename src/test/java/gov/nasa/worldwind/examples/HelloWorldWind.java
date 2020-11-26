/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.examples;

import gov.nasa.worldwind.*;
import gov.nasa.worldwind.awt.WorldWindowGLCanvas;

import javax.swing.*;
import java.awt.*;

/**
 * This is the most basic WorldWind program.
 *
 * @version $Id: HelloWorldWind.java 1971 2014-04-29 21:31:28Z dcollins $
 */
public class HelloWorldWind {
    // An inner class is used rather than directly subclassing JFrame in the main class so
    // that the main can configure system properties prior to invoking Swing. This is
    // necessary for instance on OS X (Macs) so that the application name can be specified.

    public static void main(String[] args) {
        if (Configuration.isMacOS()) {
            System.setProperty("com.apple.mrj.application.apple.menu.about.name", "Hello WorldWind");
        }

        EventQueue.invokeLater(() -> {
            // Create an AppFrame and immediately make it visible. As per Swing convention, this
            // is done within an invokeLater call so that it executes on an AWT thread.
            new AppFrame().setVisible(true);
        });
    }

    private static class AppFrame extends JFrame {
        public AppFrame() {
            WorldWindowGLCanvas wwd = new WorldWindowGLCanvas();
            wwd.setPreferredSize(new Dimension(1000, 800));
            this.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            this.getContentPane().add(wwd, BorderLayout.CENTER);
            this.pack();

            wwd.setModel(new BasicModel());
        }
    }
}