/*
 * Copyright (C) 2014 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */

package gov.nasa.worldwind.examples;

import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.layers.RenderableLayer;
import gov.nasa.worldwind.render.*;

/**
 * Tests a GlobeAnnotation near the dateline.
 *
 * @author tag
 * @version $Id: GlobeAnnotationExample.java 2134 2014-07-09 23:26:32Z tgaskins $
 */
public class GlobeAnnotationExample extends ApplicationTemplate {
    public static void main(String[] args) {
        ApplicationTemplate.start("WorldWind Globe Annotation", AppFrame.class);
    }

    protected static class AppFrame extends ApplicationTemplate.AppFrame {

        public AppFrame() {
            RenderableLayer layer = new RenderableLayer();
            layer.setName("Annotation");
            WorldWindow.insertBeforePlacenames(this.wwd(), layer);

            Annotation ga = new GlobeAnnotation("AGL Annotation", Position.fromDegrees(20, -120.9, 1000));
            ga.setAlwaysOnTop(true);
            layer.add(ga);
        }
    }
}
