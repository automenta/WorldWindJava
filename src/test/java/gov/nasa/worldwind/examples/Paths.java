/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.examples;

import gov.nasa.worldwind.*;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.layers.*;
import gov.nasa.worldwind.render.*;
import gov.nasa.worldwind.render.markers.*;
import gov.nasa.worldwind.util.BasicDragger;

import java.awt.*;
import java.util.*;

/**
 * Example of {@link Path} usage. A Path is a line or curve between positions. The path may follow terrain, and may be
 * turned into a curtain by extruding the path to the ground.
 *
 * @author tag
 * @version $Id: Paths.java 2292 2014-09-02 21:13:05Z tgaskins $
 */
public class Paths extends ApplicationTemplate {

    public static void main(String[] args) {
        ApplicationTemplate.start("WorldWind Paths", AppFrame.class);
    }

    public static class AppFrame extends ApplicationTemplate.AppFrame {

        public AppFrame() {
            super(true, true, false);

            // Add a dragger to enable shape dragging
            this.wwd().addSelectListener(new BasicDragger(this.wwd()));

            RenderableLayer layer = new RenderableLayer();

            // Create and set an attribute bundle.
            ShapeAttributes attrs = new BasicShapeAttributes();
            attrs.setOutlineMaterial(new Material(Color.YELLOW));
            attrs.setOutlineWidth(2.0d);

            // Create a path, set some of its properties and set its attributes.
            ArrayList<Position> pathPositions = new ArrayList<>();
            pathPositions.add(Position.fromDegrees(28, -102, 1.0e4));
            pathPositions.add(Position.fromDegrees(35, -100, 1.0e4));
            Path path = new Path(pathPositions);
            path.setAttributes(attrs);
            path.setVisible(true);
            path.setAltitudeMode(WorldWind.RELATIVE_TO_GROUND);
            path.setPathType(AVKey.GREAT_CIRCLE);
            layer.add(path);

            // Create a path that follows the terrain
            path = new Path(pathPositions);
            path.setAttributes(attrs);
            path.setVisible(true);
            path.setSurfacePath(true);
            layer.add(path);

            // Create a path that uses all default values.
            pathPositions = new ArrayList<>();
            pathPositions.add(Position.fromDegrees(28, -104, 1.0e4));
            pathPositions.add(Position.fromDegrees(35, -102, 1.0e4));
            path = new Path(pathPositions);
            layer.add(path);

            // Create a path with more than two positions and closed.
            pathPositions = new ArrayList<>();
            pathPositions.add(Position.fromDegrees(28, -106, 4.0e4));
            pathPositions.add(Position.fromDegrees(35, -104, 4.0e4));
            pathPositions.add(Position.fromDegrees(35, -107, 4.0e4));
            pathPositions.add(Position.fromDegrees(28, -107, 4.0e4));
            path = new Path(pathPositions);
            path.setAltitudeMode(WorldWind.ABSOLUTE);
            path.setExtrude(true);
            path.setPathType(AVKey.LINEAR);

            attrs = new BasicShapeAttributes();
            attrs.setOutlineMaterial(new Material(Color.BLUE));
            attrs.setInteriorMaterial(new Material(Color.RED));
            attrs.setOutlineWidth(2);
            path.setAttributes(attrs);

            layer.add(path);

            // Add the layer to the model.
            WorldWindow.insertBeforeCompass(wwd(), layer);

            Collection<Marker> markers = new ArrayList<>(1);
            markers.add(new BasicMarker(Position.fromDegrees(90, 0), new BasicMarkerAttributes()));
            MarkerLayer markerLayer = new MarkerLayer();
            markerLayer.setMarkers(markers);
            WorldWindow.insertBeforeCompass(wwd(), markerLayer);
        }
    }
}
