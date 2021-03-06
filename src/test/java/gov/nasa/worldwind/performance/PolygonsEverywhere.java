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
import gov.nasa.worldwind.render.*;

import java.util.ArrayList;

/**
 * @author tag
 * @version $Id: PolygonsEverywhere.java 2109 2014-06-30 16:52:38Z tgaskins $
 */
public class PolygonsEverywhere extends ApplicationTemplate {
    public static void main(String[] args) {
        ApplicationTemplate.start("WorldWind Very Many Polygons", AppFrame.class);
    }

    public static class AppFrame extends ApplicationTemplate.AppFrame {
        public AppFrame() {
            super(true, true, false);

            makeMany();
        }

        protected void makeMany() {
            int altitudeMode = WorldWind.ABSOLUTE;

            double minLat = -50, maxLat = 50, minLon = -140, maxLon = -10;
            double delta = 1.5;
            double intervals = 5;
            double dLat = 1 / intervals;
            double dLon = 1 / intervals;

            ArrayList<Position> positions = new ArrayList<>();

            RenderableLayer layer = new RenderableLayer();
            layer.setPickEnabled(false);

            int count = 0;
            for (double lat = minLat; lat <= maxLat; lat += delta) {
                for (double lon = minLon; lon <= maxLon; lon += delta) {
                    positions.clear();
                    double innerLat = lat;
                    double innerLon = lon;

                    for (int i = 0; i <= intervals; i++) {
                        innerLon += dLon;
                        positions.add(Position.fromDegrees(innerLat, innerLon, 5.0e4));
                    }

                    for (int i = 0; i <= intervals; i++) {
                        innerLat += dLat;
                        positions.add(Position.fromDegrees(innerLat, innerLon, 5.0e4));
                    }

                    for (int i = 0; i <= intervals; i++) {
                        innerLon -= dLon;
                        positions.add(Position.fromDegrees(innerLat, innerLon, 5.0e4));
                    }

                    for (int i = 0; i <= intervals; i++) {
                        innerLat -= dLat;
                        positions.add(Position.fromDegrees(innerLat, innerLon, 5.0e4));
                    }

                    Polygon pgon = new Polygon(positions);
                    pgon.setAltitudeMode(altitudeMode);
                    ShapeAttributes attrs = new BasicShapeAttributes();
                    attrs.setDrawOutline(false);
                    attrs.setInteriorMaterial(Material.RED);
                    attrs.setEnableLighting(true);
                    pgon.setAttributes(attrs);
                    layer.add(pgon);
                    ++count;
                }
            }
            System.out.printf("%d Polygons, %d positions each, Altitude mode = %s\n", count, positions.size(),
                "ABSOLUTE");

            WorldWindow.insertBeforeCompass(wwd(), layer);
        }
    }
}
