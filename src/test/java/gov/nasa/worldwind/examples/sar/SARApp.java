/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.examples.sar;

import gov.nasa.worldwind.Configuration;

/**
 * @author tag
 * @version $Id: SARApp.java 1171 2013-02-11 21:45:02Z dcollins $
 */
public class SARApp {
    public static final String APP_NAME = "WorldWind Search and Rescue Prototype";
    public static final String APP_VERSION = "(Version 6.2 released 7/15/2010)";
    public static final String APP_NAME_AND_VERSION = APP_NAME + " " + APP_VERSION;

    static {
        System.setProperty("gov.nasa.worldwind.config.file",
            "gov/nasa/worldwind/examples/sar/config/SAR.properties");
        if (Configuration.isMacOS()) {
            System.setProperty("apple.laf.useScreenMenuBar", "true");
            System.setProperty("com.apple.mrj.application.apple.menu.about.name", APP_NAME);
            System.setProperty("com.apple.mrj.application.growbox.intrudes", "false");
        }
    }
}
