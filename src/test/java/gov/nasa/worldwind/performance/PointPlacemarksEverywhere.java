/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */

package gov.nasa.worldwind.performance;

import gov.nasa.worldwind.*;
import gov.nasa.worldwind.examples.ApplicationTemplate;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.layers.RenderableLayer;
import gov.nasa.worldwind.render.PointPlacemark;

import java.util.ArrayList;

/**
 * @author tag
 * @version $Id: PointPlacemarksEverywhere.java 2109 2014-06-30 16:52:38Z tgaskins $
 */
public class PointPlacemarksEverywhere extends ApplicationTemplate {
    public static void main(String[] args) {
        ApplicationTemplate.start("WorldWind Very Many Point Placemarks", AppFrame.class);
    }

    public static class AppFrame extends ApplicationTemplate.AppFrame {
        public AppFrame() {
            super(true, true, false);

            makeMany();
        }

        protected void makeMany() {
            int altitudeMode = WorldWind.RELATIVE_TO_GROUND;

            double minLat = -50, maxLat = 50, minLon = -140, maxLon = -10;
            double delta = 1.5;
            double intervals = 5;

            ArrayList<Position> positions = new ArrayList<>();

            RenderableLayer layer = new RenderableLayer();

            int count = 0;
            for (double lat = minLat; lat <= maxLat; lat += delta) {
                for (double lon = minLon; lon <= maxLon; lon += delta) {

                    PointPlacemark pm = new PointPlacemark(Position.fromDegrees(lat, lon, 5.0e4));
                    pm.setAltitudeMode(altitudeMode);
                    layer.add(pm);
                    ++count;
                }
            }
            System.out.printf("%d Placemarks, Altitude mode = %s\n", count, positions.size(),
                "RELATIVE_TO_GROUND");

            WorldWindow.insertBeforeCompass(wwd(), layer);
        }
    }
}
