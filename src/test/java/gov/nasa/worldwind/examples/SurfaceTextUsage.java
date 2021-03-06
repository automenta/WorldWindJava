/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */

package gov.nasa.worldwind.examples;

import gov.nasa.worldwind.*;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.layers.RenderableLayer;
import gov.nasa.worldwind.render.*;

/**
 * Example of using the {@link SurfaceText} class. SurfaceText draws text on the surface of the globe.
 *
 * @author pabercrombie
 * @version $Id: SurfaceTextUsage.java 1171 2013-02-11 21:45:02Z dcollins $
 */
public class SurfaceTextUsage extends ApplicationTemplate {
    public static void main(String[] args) {
        Configuration.setValue(Keys.INITIAL_LATITUDE, 38.9345);
        Configuration.setValue(Keys.INITIAL_LONGITUDE, -120.1670);
        Configuration.setValue(Keys.INITIAL_ALTITUDE, 50000);

        ApplicationTemplate.start("WorldWind Surface Text", AppFrame.class);
    }

    public static class AppFrame extends ApplicationTemplate.AppFrame {
        public AppFrame() {
            super(true, true, false);

            RenderableLayer layer = new RenderableLayer();

            Renderable surfaceText = new SurfaceText("Desolation Wilderness",
                Position.fromDegrees(38.9345, -120.1670, 0));
            layer.add(surfaceText);

            this.wwd().model().layers().add(layer);
        }
    }
}