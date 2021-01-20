/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.examples;

import gov.nasa.worldwind.*;

/**
 * Shows how to turn on stereo, which is requested via a Java VM property. This example sets the property directly, but
 * it's more conventional to set it on the java command line.
 *
 * @author tag
 * @version $Id: Stereo.java 1171 2013-02-11 21:45:02Z dcollins $
 */
public class Stereo extends ApplicationTemplate {
    public static void main(String[] args) {
        // Set the stereo.mode property to request stereo. Request red-blue anaglyph in this case. Can also request
        // "device" if the display device supports stereo directly. To prevent stereo, leave the property unset or set
        // it to an empty string.
        System.setProperty("gov.nasa.worldwind.stereo.mode", "redblue");

        // Configure the initial view parameters so that the balloons are immediately visible.
        Configuration.setValue(Keys.INITIAL_LATITUDE, 46.7045);
        Configuration.setValue(Keys.INITIAL_LONGITUDE, -121.6242);
        Configuration.setValue(Keys.INITIAL_ALTITUDE, 10.0e3);
        Configuration.setValue(Keys.INITIAL_HEADING, 342);
        Configuration.setValue(Keys.INITIAL_PITCH, 80);

        ApplicationTemplate.start("WorldWind Anaglyph Stereo", AppFrame.class);
    }
}