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
import gov.nasa.worldwind.render.*;
import gov.nasa.worldwind.terrain.HighResTerrain;

import javax.swing.Timer;
import java.util.*;

/**
 * Shows how to determine and display the intersection of a line with an {@link Polygon}.
 *
 * @author tag
 * @version $Id: PolygonIntersection.java 2109 2014-06-30 16:52:38Z tgaskins $
 */
public class PolygonIntersection extends ApplicationTemplate {
    public static void main(String[] args) {
        // Configure the initial view parameters so that the balloons are immediately visible.
        Configuration.setValue(Keys.INITIAL_LATITUDE, 40.5);
        Configuration.setValue(Keys.INITIAL_LONGITUDE, -120.4);
        Configuration.setValue(Keys.INITIAL_ALTITUDE, 125.0e3);
        Configuration.setValue(Keys.INITIAL_HEADING, 27);
        Configuration.setValue(Keys.INITIAL_PITCH, 30);

        ApplicationTemplate.start("WorldWind Polygon Intersection", AppFrame.class);
    }

    public static class AppFrame extends ApplicationTemplate.AppFrame {
        protected final HighResTerrain terrain; // Use this class to test against high-resolution terrain
        protected final Polygon polygon; // the polygon to intersect
        protected final RenderableLayer layer; // layer to display the polygon and the intersection

        public AppFrame() {
            super(true, true, false);

            // Create the polygon boundary and then the polygon.
            List<Position> positions = new ArrayList<>();
            positions.add(Position.fromDegrees(40.4, -120.6, 10.0e3));
            positions.add(Position.fromDegrees(40.4, -120.4, 10.0e3));
            positions.add(Position.fromDegrees(40.6, -120.4, 10.0e3));
            positions.add(Position.fromDegrees(40.6, -120.6, 10.0e3));

            this.polygon = new Polygon(positions);
            this.polygon.setAltitudeMode(WorldWind.RELATIVE_TO_GROUND);
            this.polygon.setReferencePosition(Position.fromDegrees(40.4, -120.6, 0));

            // Set some of the shape's attributes
            ShapeAttributes attrs = new BasicShapeAttributes();
            attrs.setInteriorMaterial(Material.LIGHT_GRAY);
            this.polygon.setAttributes(attrs);

            // Add the shape to the display layer.
            this.layer = new RenderableLayer();
            this.layer.add(this.polygon);
            WorldWindow.insertBeforeCompass(wwd(), this.layer);

            // Create high-resolution terrain for the intersection calculations
            this.terrain = new HighResTerrain(this.wwd().model().globe(), 20.0d);

            // Perform the intersection test within a timer callback. Intersection calculations would normally be done
            // on a separate, non-EDT thread, however.
            Timer timer = new Timer(3000, actionEvent -> {
                Position pA = Position.fromDegrees(40.5, -120.5, 0);
                Position pB = new Position(pA, 20.0e3);
                drawLine(pA, pB);
                performIntersection();
            });
            timer.start();
        }

        protected void performIntersection() {

            // Create the line to intersect with the shape.
            Position referencePosition = Position.fromDegrees(40.5, -120.5, 0);
            Vec4 referencePoint = terrain.surfacePoint(referencePosition);

            Position targetPosition = new Position(referencePosition, 20.0e3);
            Vec4 targetPoint = terrain.surfacePoint(targetPosition);
            Line line = new Line(targetPoint, referencePoint.subtract3(targetPoint));

            // Perform the intersection.
            List<Intersection> intersections = this.polygon.intersect(line, this.terrain);

            // Get and display the intersections.
            if (intersections != null) {
                for (Intersection intersection : intersections) {
                    drawIntersection(intersection);
                }
            }
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
            this.layer.add(path);

            this.wwd().redraw();
        }

        protected void drawIntersection(Intersection intersection) {
            // Display a point at the intersection.
            PointPlacemark iPoint = new PointPlacemark(intersection.getIntersectionPosition());
            iPoint.setAltitudeMode(WorldWind.RELATIVE_TO_GROUND);
            PointPlacemarkAttributes pointAttributes = new PointPlacemarkAttributes();
            pointAttributes.setLineMaterial(Material.CYAN);
            pointAttributes.setScale(8.0d);
            pointAttributes.setUsePointAsDefaultImage(true);
            iPoint.setAttributes(pointAttributes);
            this.layer.add(iPoint);

            this.wwd().redraw();
        }
    }
}