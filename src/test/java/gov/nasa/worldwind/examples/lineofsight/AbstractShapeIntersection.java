/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */

package gov.nasa.worldwind.examples.lineofsight;

import gov.nasa.worldwind.*;
import gov.nasa.worldwind.examples.ApplicationTemplate;
import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.layers.RenderableLayer;
import gov.nasa.worldwind.render.Cylinder;
import gov.nasa.worldwind.render.*;
import gov.nasa.worldwind.terrain.HighResTerrain;

import javax.swing.*;
import java.util.List;

/**
 * Shows how to determine and display the intersection of a line with an {@link Ellipsoid} or
 * other rigid shapes. This example creates a shape on the globe, loads high resolution terrain, and then computes the
 * intersection of the shape and a line through the shape parallel to the globe, and a line through the shape
 * perpendicular to the globe.
 *
 * @author ccrick
 * @version $Id: AbstractShapeIntersection.java 2109 2014-06-30 16:52:38Z tgaskins $
 */
public class AbstractShapeIntersection extends ApplicationTemplate {
    public static void main(String[] args) {
        // Configure the initial view parameters so that the balloons are immediately visible.
        Configuration.setValue(Keys.INITIAL_LATITUDE, 40.5);
        Configuration.setValue(Keys.INITIAL_LONGITUDE, -120.4);
        Configuration.setValue(Keys.INITIAL_ALTITUDE, 125.0e3);
        Configuration.setValue(Keys.INITIAL_HEADING, 27);
        Configuration.setValue(Keys.INITIAL_PITCH, 30);

        ApplicationTemplate.start("WorldWind Extruded Abstract Shape Intersection", AppFrame.class);
    }

    public static class AppFrame extends ApplicationTemplate.AppFrame {
        protected final HighResTerrain terrain; // Use this class to test against high-resolution terrain
        protected final Cylinder shape; // the polygon to intersect
        protected final RenderableLayer resultsLayer; // holds the intersection geometry
        protected final RenderableLayer shapeLayer; // holds the shape

        public AppFrame() {
            super(true, true, false);

            this.shape = new Cylinder(Position.fromDegrees(40.5, -120.5, 4.0e3), 5000, 5000, 5000);
            this.shape.setAltitudeMode(WorldWind.ABSOLUTE);

            // Set some of the shape's attributes
            ShapeAttributes attrs = new BasicShapeAttributes();
            attrs.setInteriorMaterial(Material.LIGHT_GRAY);
            attrs.setInteriorOpacity(0.6);

            this.shape.setAttributes(attrs);

            // Add the shape to its display layer.
            this.shapeLayer = new RenderableLayer();
            this.shapeLayer.add(this.shape);
            WorldWindow.insertBeforeCompass(wwd(), this.shapeLayer);

            // Prepare the results layer.
            this.resultsLayer = new RenderableLayer();
            WorldWindow.insertBeforeCompass(wwd(), this.resultsLayer);

            // Create high-resolution terrain for the intersection calculations
            this.terrain = new HighResTerrain(this.wwd().model().globe(), 20.0d);

            // Perform the intersection test within a timer callback. Intersection calculations would normally be done
            // on a separate, non-EDT thread, however.
            Timer timer = new Timer(5000, actionEvent -> {
                // Intersect the sides.
                Position pA = Position.fromDegrees(40.5, -120.7, 4.0e3);
                Position pB = Position.fromDegrees(40.5, -120.3, 4.0e3);
                drawLine(pA, pB);
                performIntersection(pA, pB);

                // Intersect the top.
                pA = Position.fromDegrees(40.5, -120.5, 0);
                pB = new Position(pA, 20.0e3);
                drawLine(pA, pB);
                performIntersection(pA, pB);

                ((Timer) actionEvent.getSource()).stop();
            });
            timer.start();
        }

        protected void performIntersection(Position pA, Position pB) {
//            try
            {
                // Create the line to intersect with the shape.
                Vec4 refPoint = terrain.surfacePoint(pA);
                Vec4 targetPoint = terrain.surfacePoint(pB);
                Line line = new Line(targetPoint, refPoint.subtract3(targetPoint));

                // Perform the intersection.
                List<Intersection> intersections = this.shape.intersect(line, this.terrain);

                // Get and display the intersections.
                if (intersections != null) {
                    for (Intersection intersection : intersections) {
                        drawIntersection(intersection);
                    }
                }
            }
//            catch (InterruptedException e)
        }

        protected void drawLine(Position pA, Position pB) {
            // Create and display the intersection line.
            Path path = new Path(pA, pB);
            ShapeAttributes pathAttributes = new BasicShapeAttributes();
            path.setAltitudeMode(WorldWind.RELATIVE_TO_GROUND);
            pathAttributes.setOutlineMaterial(Material.GREEN);
            pathAttributes.setOutlineOpacity(0.6);
            pathAttributes.setDrawOutline(true);
            pathAttributes.setDrawInterior(false);
            path.setAttributes(pathAttributes);
            this.resultsLayer.add(path);

            this.wwd().redraw();
        }

        protected void drawIntersection(Intersection intersection) {
            // Display a point at the intersection.
            PointPlacemark iPoint = new PointPlacemark(intersection.getIntersectionPosition());
            iPoint.setAltitudeMode(WorldWind.RELATIVE_TO_GROUND);
            PointPlacemarkAttributes pointAttributes = new PointPlacemarkAttributes();
            pointAttributes.setLineMaterial(Material.RED);
            pointAttributes.setScale(8.0d);
            pointAttributes.setUsePointAsDefaultImage(true);
            iPoint.setAttributes(pointAttributes);
            this.resultsLayer.add(iPoint);

            this.wwd().redraw();
        }
    }
}