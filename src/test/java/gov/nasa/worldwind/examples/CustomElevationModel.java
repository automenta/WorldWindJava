/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.examples;

import gov.nasa.worldwind.*;

/**
 * Illustrates how to configure WorldWind with a custom <code>{@link gov.nasa.worldwind.globes.ElevationModel}</code>
 * from a configuration file.
 *
 * @author tag
 * @version $Id: CustomElevationModel.java 1171 2013-02-11 21:45:02Z dcollins $
 */
public class CustomElevationModel extends ApplicationTemplate {
    public static void main(String[] args) {
        // Specify the configuration file for the elevation model prior to starting WorldWind:
        Configuration.setValue(Keys.EARTH_ELEVATION_MODEL_CONFIG_FILE,
            "gov/nasa/worldwind/examples/data/CustomElevationModel.xml");

        ApplicationTemplate.start("WorldWind Custom Elevation Model", AppFrame.class);
    }
}