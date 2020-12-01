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
 * @version $Id: ExtrudedPolygonsEverywhere.java 2109 2014-06-30 16:52:38Z tgaskins $
 */
public class ExtrudedPolygonsEverywhere extends ApplicationTemplate {
    public static void main(String[] args) {
        ApplicationTemplate.start("WorldWind Very Many Extruded Polygons", AppFrame.class);
    }

    public static class AppFrame extends ApplicationTemplate.AppFrame {
        public AppFrame() {
            super(true, true, false);

            makeMany();
        }

        protected void makeMany() {
            int altitudeMode = WorldWind.CONSTANT;

            double minLat = -50, maxLat = 50, minLon = -140, maxLon = -10;
            double delta = 2;
            double intervals = 4;
            double dLat = 1 / intervals;
            double dLon = 1 / intervals;

            ShapeAttributes capAttrs = new BasicShapeAttributes();
            capAttrs.setDrawOutline(true);
            capAttrs.setDrawInterior(true);
            capAttrs.setOutlineMaterial(Material.BLUE);
            capAttrs.setInteriorMaterial(Material.CYAN);
            capAttrs.setEnableLighting(true);

            ShapeAttributes sideAttrs = new BasicShapeAttributes();
            sideAttrs.setOutlineWidth(3);
            sideAttrs.setDrawOutline(true);
            sideAttrs.setDrawInterior(true);
            sideAttrs.setOutlineMaterial(Material.GREEN);
            sideAttrs.setInteriorMaterial(Material.RED);
            sideAttrs.setEnableLighting(true);

            ArrayList<Position> positions = new ArrayList<>();

            RenderableLayer layer = new RenderableLayer();

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

                    ExtrudedPolygon pgon = new ExtrudedPolygon(positions, 5.0e4);
                    pgon.setAltitudeMode(altitudeMode);
                    pgon.setEnableSides(true);
                    pgon.setEnableCap(true);
                    pgon.setCapAttributes(capAttrs);
                    pgon.setSideAttributes(sideAttrs);
                    layer.add(pgon);
                    ++count;
                }
            }
            System.out.printf("%d ExtrudedPolygons, %d positions each, Altitude mode = %s\n", count, positions.size(),
                "ABSOLUTE");

            WorldWindow.insertBeforeCompass(wwd(), layer);
        }
    }
}
