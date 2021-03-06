/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.examples;

import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.layers.RenderableLayer;
import gov.nasa.worldwind.render.*;
import gov.nasa.worldwind.util.*;

import java.awt.*;
import java.util.Arrays;

/**
 * Illustrates how to enable shape dragging in WorldWind by using a <code>{@link BasicDragger}</code>.
 * This creates multiple shapes on the surface terrain that can be dragged to a new location on the terrain. The shapes
 * retain their form when dragged.
 *
 * @author dcollins
 * @version $Id: DraggingShapes.java 2109 2014-06-30 16:52:38Z tgaskins $
 */
public class DraggingShapes extends ApplicationTemplate {
    public static void main(String[] args) {
        ApplicationTemplate.start("WorldWind Dragging Shapes", AppFrame.class);
    }

    public static class AppFrame extends ApplicationTemplate.AppFrame {
        protected static final String SURFACE_POLYGON_IMAGE_PATH = "gov/nasa/worldwind/examples/images/georss.png";

        public AppFrame() {
            // Add a basic dragger to the WorldWindow's select listeners to enable shape dragging.
            this.wwd().addSelectListener(new BasicDragger(this.wwd()));

            // Create a layer of shapes to drag.
            this.makeShapes();
        }

        protected void makeShapes() {
            RenderableLayer layer = new RenderableLayer();
            layer.setName("Surface Shapes");

            // Surface polygon over Florida.
            ShapeAttributes attrs = new BasicShapeAttributes();
            attrs.setOutlineMaterial(Material.WHITE);
            attrs.setInteriorOpacity(0.5);
            attrs.setOutlineOpacity(0.8);
            attrs.setOutlineWidth(3);
            attrs.setImageSource(SURFACE_POLYGON_IMAGE_PATH);
            attrs.setImageScale(0.5);

            double originLat = 28;
            double originLon = -82;
            Iterable<LatLon> locations = Arrays.asList(
                LatLon.fromDegrees(originLat + 5.0, originLon + 2.5),
                LatLon.fromDegrees(originLat + 5.0, originLon - 2.5),
                LatLon.fromDegrees(originLat + 2.5, originLon - 5.0),
                LatLon.fromDegrees(originLat - 2.5, originLon - 5.0),
                LatLon.fromDegrees(originLat - 5.0, originLon - 2.5),
                LatLon.fromDegrees(originLat - 5.0, originLon + 2.5),
                LatLon.fromDegrees(originLat - 2.5, originLon + 5.0),
                LatLon.fromDegrees(originLat + 2.5, originLon + 5.0),
                LatLon.fromDegrees(originLat + 5.0, originLon + 2.5));
            SurfaceShape shape = new SurfacePolygon(locations);
            shape.setAttributes(attrs);
            layer.add(shape);

            // Surface polygon spanning the international dateline.
            attrs = new BasicShapeAttributes();
            attrs.setInteriorMaterial(Material.RED);
            attrs.setOutlineMaterial(new Material(WWUtil.makeColorBrighter(Color.RED)));
            attrs.setInteriorOpacity(0.5);
            attrs.setOutlineOpacity(0.8);
            attrs.setOutlineWidth(3);

            locations = Arrays.asList(
                LatLon.fromDegrees(20, -170),
                LatLon.fromDegrees(15, 170),
                LatLon.fromDegrees(10, -175),
                LatLon.fromDegrees(5, 170),
                LatLon.fromDegrees(0, -170),
                LatLon.fromDegrees(20, -170));
            shape = new SurfacePolygon(locations);
            shape.setAttributes(attrs);
            layer.add(shape);

            // Surface ellipse over the center of the United States.
            attrs = new BasicShapeAttributes();
            attrs.setInteriorMaterial(Material.CYAN);
            attrs.setOutlineMaterial(new Material(WWUtil.makeColorBrighter(Color.CYAN)));
            attrs.setInteriorOpacity(0.5);
            attrs.setOutlineOpacity(0.8);
            attrs.setOutlineWidth(3);

            shape = new SurfaceEllipse(LatLon.fromDegrees(38, -104), 1.5e5, 1.0e5, new Angle(15));
            shape.setAttributes(attrs);
            layer.add(shape);

            // Surface circle over the center of the United states.
            attrs = new BasicShapeAttributes();
            attrs.setInteriorMaterial(Material.GREEN);
            attrs.setOutlineMaterial(new Material(WWUtil.makeColorBrighter(Color.GREEN)));
            attrs.setInteriorOpacity(0.5);
            attrs.setOutlineOpacity(0.8);
            attrs.setOutlineWidth(3);

            shape = new SurfaceCircle(LatLon.fromDegrees(36, -104), 1.0e5);
            shape.setAttributes(attrs);
            layer.add(shape);

            // Surface circle over the North Pole
            shape = new SurfaceCircle(LatLon.fromDegrees(90, 0), 1.0e6);
            shape.setAttributes(attrs);
            layer.add(shape);

            // Surface quadrilateral over the center of the United States.
            attrs = new BasicShapeAttributes();
            attrs.setInteriorMaterial(Material.ORANGE);
            attrs.setOutlineMaterial(new Material(WWUtil.makeColorBrighter(Color.ORANGE)));
            attrs.setInteriorOpacity(0.5);
            attrs.setOutlineOpacity(0.8);
            attrs.setOutlineWidth(3);

            shape = new SurfaceQuad(LatLon.fromDegrees(42, -104), 1.0e5, 1.3e5, new Angle(20));
            shape.setAttributes(attrs);
            layer.add(shape);

            // Surface square over the center of the United states.
            attrs = new BasicShapeAttributes();
            attrs.setInteriorMaterial(Material.MAGENTA);
            attrs.setOutlineMaterial(new Material(WWUtil.makeColorBrighter(Color.MAGENTA)));
            attrs.setInteriorOpacity(0.5);
            attrs.setOutlineOpacity(0.8);
            attrs.setOutlineWidth(3);

            shape = new SurfaceSquare(LatLon.fromDegrees(45, -104), 1.0e5);
            shape.setAttributes(attrs);
            layer.add(shape);

            // Surface sector over Mt. Shasta.
            attrs = new BasicShapeAttributes();
            attrs.setInteriorMaterial(Material.YELLOW);
            attrs.setOutlineMaterial(new Material(WWUtil.makeColorBrighter(Color.YELLOW)));
            attrs.setInteriorOpacity(0.5);
            attrs.setOutlineOpacity(0.8);
            attrs.setOutlineWidth(3);

            shape = new SurfaceSector(new Sector(
                new Angle(41.0), new Angle(41.6),
                new Angle(-122.5), new Angle(-121.7)));
            shape.setAttributes(attrs);
            layer.add(shape);

            // Surface sector over Lake Tahoe.
            attrs = new BasicShapeAttributes();
            attrs.setInteriorMaterial(Material.CYAN);
            attrs.setOutlineMaterial(new Material(WWUtil.makeColorBrighter(Color.CYAN)));
            attrs.setInteriorOpacity(0.5);
            attrs.setOutlineOpacity(0.8);
            attrs.setOutlineWidth(3);

            shape = new SurfaceSector(new Sector(
                new Angle(38.9), new Angle(39.3),
                new Angle(-120.2), new Angle(-119.9)));
            shape.setAttributes(attrs);
            layer.add(shape);

            // Surface polyline spanning the international dateline.
            attrs = new BasicShapeAttributes();
            attrs.setOutlineMaterial(Material.RED);
            attrs.setOutlineWidth(3);
            attrs.setOutlineStippleFactor(2);

            locations = Arrays.asList(
                LatLon.fromDegrees(-10, 170),
                LatLon.fromDegrees(-10, -170));
            shape = new SurfacePolyline(locations);
            shape.setAttributes(attrs);
            ((SurfacePolyline) shape).setClosed(false);
            layer.add(shape);

            // Add the layer to the model and update the layer panel.
            WorldWindow.insertBeforeCompass(this.wwd(), layer);
        }
    }
}
