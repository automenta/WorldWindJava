/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */

package gov.nasa.worldwind.examples;

import gov.nasa.worldwind.*;
import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.layers.RenderableLayer;
import gov.nasa.worldwind.render.Box;
import gov.nasa.worldwind.render.Cylinder;
import gov.nasa.worldwind.render.*;

import java.util.*;

/**
 * This example demonstrates usage of the various Rigid Shapes: {@link Ellipsoid}, {@link
 * Box}, {@link Cylinder}, {@link Cone},
 * {@link Pyramid} and {@link Wedge}. The Rigid Shapes can be
 * scaled, rotated, skewed and textured. Standard attributes and altitude modes may also be specified.
 *
 * @author ccrick
 * @version $Id: RigidShapes.java 2109 2014-06-30 16:52:38Z tgaskins $
 */
public class RigidShapes extends ApplicationTemplate {
    public static void main(String[] args) {
        ApplicationTemplate.start("WorldWind Rigid Shapes", AppFrame.class);
    }

    public static class AppFrame extends ApplicationTemplate.AppFrame {
        public AppFrame() {
            this.makeShapes();
        }

        protected void makeShapes() {
            RenderableLayer layer = new RenderableLayer();
            layer.setName("Rigid Shapes");

            // Create and set an attribute bundle.
            ShapeAttributes attrs = new BasicShapeAttributes();
            attrs.setInteriorMaterial(Material.YELLOW);
            attrs.setInteriorOpacity(0.7);
            attrs.setEnableLighting(true);
            attrs.setOutlineMaterial(Material.RED);
            attrs.setOutlineWidth(2.0d);
            attrs.setDrawInterior(true);
            attrs.setDrawOutline(false);

            // Create and set a second attribute bundle.
            ShapeAttributes attrs2 = new BasicShapeAttributes();
            attrs2.setInteriorMaterial(Material.PINK);
            attrs2.setInteriorOpacity(1);
            attrs2.setEnableLighting(true);
            attrs2.setOutlineMaterial(Material.WHITE);
            attrs2.setOutlineWidth(2.0d);
            attrs2.setDrawOutline(false);

            // Pyramid with equal axes, ABSOLUTE altitude mode.
            Pyramid pyramid = new Pyramid(Position.fromDegrees(40, -120, 220000), 200000, 200000, 200000);
            pyramid.setAltitudeMode(WorldWind.ABSOLUTE);
            pyramid.setAttributes(attrs);
            pyramid.set(Keys.DISPLAY_NAME, "Pyramid with equal axes, ABSOLUTE altitude mode");
            layer.add(pyramid);

            // Cone with equal axes, RELATIVE_TO_GROUND.
            Cone cone = new Cone(Position.fromDegrees(37.5, -115, 200000), 200000, 200000, 200000);
            cone.setAltitudeMode(WorldWind.RELATIVE_TO_GROUND);
            cone.setAttributes(attrs);
            cone.set(Keys.DISPLAY_NAME, "Cone with equal axes, RELATIVE_TO_GROUND altitude mode");
            layer.add(cone);

            // Wedge with equal axes, CLAMP_TO_GROUND.
            Wedge wedge = new Wedge(Position.fromDegrees(35, -110, 200000), new Angle(225),
                200000, 200000, 200000);
            wedge.setAltitudeMode(WorldWind.CLAMP_TO_GROUND);
            wedge.setAttributes(attrs);
            wedge.set(Keys.DISPLAY_NAME, "Wedge with equal axes, CLAMP_TO_GROUND altitude mode");
            layer.add(wedge);

            // Box with a texture.
            Box box = new Box(Position.fromDegrees(0, -90, 600000), 600000, 600000, 600000);
            box.setAltitudeMode(WorldWind.RELATIVE_TO_GROUND);
            List<Object> imageSources = new ArrayList<>();
            imageSources.add("images/32x32-icon-nasa.png");
            imageSources.add(null);
            imageSources.add("gov/nasa/worldwind/examples/images/500px-Checkerboard_pattern.png");
            imageSources.add(null);
            imageSources.add("images/64x64-crosshair.png");
            imageSources.add(null);
            box.setImageSources(imageSources);
            box.setAttributes(attrs);
            box.set(Keys.DISPLAY_NAME, "Box with a texture");
            layer.add(box);

            // Sphere with a texture.
            Ellipsoid sphere = new Ellipsoid(Position.fromDegrees(0, -110, 600000), 600000, 600000, 600000);
            sphere.setAltitudeMode(WorldWind.RELATIVE_TO_GROUND);
            sphere.setImageSources("gov/nasa/worldwind/examples/images/500px-Checkerboard_pattern.png");
            sphere.setAttributes(attrs);
            sphere.set(Keys.DISPLAY_NAME, "Sphere with a texture");
            layer.add(sphere);

            // Cylinder with a texture.
            Cylinder cylinder = new Cylinder(Position.fromDegrees(0, -130, 600000), 600000, 600000, 600000);
            cylinder.setAltitudeMode(WorldWind.RELATIVE_TO_GROUND);
            cylinder.setImageSources("gov/nasa/worldwind/examples/images/500px-Checkerboard_pattern.png");
            cylinder.setAttributes(attrs);
            cylinder.set(Keys.DISPLAY_NAME, "Cylinder with a texture");
            layer.add(cylinder);

            // Cylinder with default orientation.
            cylinder = new Cylinder(Position.ZERO, 600000, 500000, 300000);
            cylinder.setAltitudeMode(WorldWind.ABSOLUTE);
            cylinder.setAttributes(attrs);
            cylinder.set(Keys.DISPLAY_NAME, "Cylinder with default orientation");
            layer.add(cylinder);

            // Ellipsoid with a pre-set orientation.
            Ellipsoid ellipsoid = new Ellipsoid(Position.fromDegrees(0, 30, 750000), 1000000, 500000, 100000,
                new Angle(90), new Angle(45), new Angle(30));
            ellipsoid.setAltitudeMode(WorldWind.RELATIVE_TO_GROUND);
            ellipsoid.setAttributes(attrs2);
            ellipsoid.set(Keys.DISPLAY_NAME, "Ellipsoid with a pre-set orientation");
            layer.add(ellipsoid);

            // Ellipsoid with a pre-set orientation.
            ellipsoid = new Ellipsoid(Position.fromDegrees(30, 30, 750000), 1000000, 500000, 100000,
                new Angle(90), new Angle(45), new Angle(30));
            ellipsoid.setAltitudeMode(WorldWind.RELATIVE_TO_GROUND);
            ellipsoid.setImageSources("gov/nasa/worldwind/examples/images/500px-Checkerboard_pattern.png");
            ellipsoid.setAttributes(attrs2);
            ellipsoid.set(Keys.DISPLAY_NAME, "Ellipsoid with a pre-set orientation");
            layer.add(ellipsoid);

            // Ellipsoid with a pre-set orientation.
            ellipsoid = new Ellipsoid(Position.fromDegrees(60, 30, 750000), 1000000, 500000, 100000,
                new Angle(90), new Angle(45), new Angle(30));
            ellipsoid.setAltitudeMode(WorldWind.RELATIVE_TO_GROUND);
            ellipsoid.setAttributes(attrs2);
            ellipsoid.set(Keys.DISPLAY_NAME, "Ellipsoid with a pre-set orientation");
            layer.add(ellipsoid);

            // Ellipsoid oriented in 3rd "quadrant" (-X, -Y, -Z).
            ellipsoid = new Ellipsoid(Position.fromDegrees(-45, -180, 750000), 1000000, 500000, 100000,
                new Angle(90), new Angle(45), new Angle(30));
            ellipsoid.setAltitudeMode(WorldWind.RELATIVE_TO_GROUND);
            ellipsoid.setAttributes(attrs2);
            ellipsoid.set(Keys.DISPLAY_NAME, "Ellipsoid oriented in 3rd \"quadrant\" (-X, -Y, -Z)");
            layer.add(ellipsoid);

            // Add the layer to the model and update the layer panel.
            WorldWindow.insertBeforePlacenames(wwd(), layer);
        }
    }
}