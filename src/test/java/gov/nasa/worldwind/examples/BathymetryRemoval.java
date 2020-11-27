/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.examples;

import gov.nasa.worldwind.globes.ElevationModel;
import gov.nasa.worldwind.terrain.BathymetryFilterElevationModel;

/**
 * Illustrates how to suppress the WorldWind <code>{@link gov.nasa.worldwind.globes.Globe}'s</code> bathymetry
 * (elevations below mean sea level) by using a <code>{@link BathymetryFilterElevationModel}</code>.
 *
 * @author tag
 * @version $Id: BathymetryRemoval.java 1171 2013-02-11 21:45:02Z dcollins $
 */
public class BathymetryRemoval extends ApplicationTemplate {
    public static void main(String[] args) {
        ApplicationTemplate.start("WorldWind Bathymetry Removal", AppFrame.class);
    }

    public static class AppFrame extends ApplicationTemplate.AppFrame {
        public AppFrame() {
            // Get the current elevation model.
            ElevationModel currentElevationModel = this.getWwd().model().getGlobe().getElevationModel();

            // Wrap it with the no-bathymetry elevation model.
            ElevationModel noDepthModel = new BathymetryFilterElevationModel(currentElevationModel);

            // Have the globe use the no-bathymetry elevation model.
            this.getWwd().model().getGlobe().setElevationModel(noDepthModel);

            // Increase vertical exaggeration to make it clear that bathymetry is suppressed.
            this.getWwd().getSceneController().setVerticalExaggeration(5.0d);
        }
    }
}