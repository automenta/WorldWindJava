/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */

package gov.nasa.worldwind.examples;

import gov.nasa.worldwind.*;
import gov.nasa.worldwind.avlist.*;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.layers.RenderableLayer;
import gov.nasa.worldwind.render.*;
import gov.nasa.worldwind.symbology.IconRetriever;
import gov.nasa.worldwind.symbology.milstd2525.*;
import gov.nasa.worldwind.util.WWUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.image.*;

/**
 * Illustrates how to use {@link PointPlacemark}. Also shows how to use a 2525 tactical symbol
 * as a placemark image.
 *
 * @author tag
 * @version $Id: Placemarks.java 2812 2015-02-17 21:00:43Z tgaskins $
 * @see PlacemarkLabelEditing
 */
public class Placemarks extends ApplicationTemplate {
    public static void createTacticalSymbolPointPlacemark(final RenderableLayer layer) {
        // *** This method is running on thread separate from the EDT. ***

        // Create an icon retriever using the path specified in the config file, or the default path.
        String iconRetrieverPath = Configuration.getStringValue(Keys.MIL_STD_2525_ICON_RETRIEVER_PATH,
            MilStd2525Constants.DEFAULT_ICON_RETRIEVER_PATH);
        IconRetriever iconRetriever = new MilStd2525IconRetriever(iconRetrieverPath);

        // Retrieve the tactical symbol image we'll use for the placemark.
        KV params = new KVMap();
        final BufferedImage symbolImage = iconRetriever.createIcon("SFAPMFQM--GIUSA", params);

        // Create an alternate version of the image that we'll use for highlighting.
        params.set(Keys.COLOR, Color.WHITE);
        final BufferedImage highlightImage = iconRetriever.createIcon("SFAPMFQM--GIUSA", params);

        // Add the placemark to WorldWind on the event dispatch thread.
        SwingUtilities.invokeLater(() -> {
            try {
                // Create the placemark
                PointPlacemark pp = new PointPlacemark(Position.fromDegrees(30, -102, 0));
                pp.setLabelText("Tactical Symbol");
                pp.setLineEnabled(false);
                pp.setAltitudeMode(WorldWind.CLAMP_TO_GROUND);
                pp.setEnableLabelPicking(true);
                pp.setAlwaysOnTop(true); // Set this flag just to show how to force the placemark to the top

                // Create and assign the placemark attributes.
                PointPlacemarkAttributes attrs = new PointPlacemarkAttributes();
                attrs.setImage(symbolImage);
                attrs.setImageColor(new Color(1.0f, 1.0f, 1.0f, 1.0f));
                attrs.setLabelOffset(new Offset(0.9d, 0.6d, Keys.FRACTION, Keys.FRACTION));
                attrs.setScale(0.5);
                pp.setAttributes(attrs);

                // Create and assign the placemark's highlight attributes.
                PointPlacemarkAttributes highlightAttributes = new PointPlacemarkAttributes(attrs);
                highlightAttributes.setImage(highlightImage);
                pp.setHighlightAttributes(highlightAttributes);

                // Add the placemark to the layer.
                layer.add(pp);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public static void main(String[] args) {
        ApplicationTemplate.start("WorldWind Placemarks", AppFrame.class);
    }

    public static class AppFrame extends ApplicationTemplate.AppFrame {
        public AppFrame() {
            super(true, true, false);

            final RenderableLayer layer = new RenderableLayer();

            PointPlacemark pp = new PointPlacemark(Position.fromDegrees(28, -102, 1.0e4));
            pp.setLabelText("Placemark A");
            pp.set(Keys.DISPLAY_NAME, "Clamp to ground, Label, Semi-transparent, Audio icon");
            pp.setLineEnabled(false);
            pp.setAltitudeMode(WorldWind.CLAMP_TO_GROUND);
            pp.setEnableLabelPicking(true); // enable label picking for this placemark
            PointPlacemarkAttributes attrs = new PointPlacemarkAttributes();
            attrs.setImageAddress("gov/nasa/worldwind/examples/images/audioicon-64.png");
            attrs.setImageColor(new Color(1.0f, 1.0f, 1.0f, 0.6f));
            attrs.setScale(0.6);
            attrs.setLabelOffset(new Offset(0.9d, 0.6d, Keys.FRACTION, Keys.FRACTION));
            pp.setAttributes(attrs);
            layer.add(pp);

            // Place a default pin placemark at the same location over the previous one.
            pp = new PointPlacemark(pp.getPosition());
            pp.set(Keys.DISPLAY_NAME, "Clamp to ground, Default icon over audio icon");
            pp.setAltitudeMode(WorldWind.CLAMP_TO_GROUND);
            layer.add(pp);

            pp = new PointPlacemark(Position.fromDegrees(28, -104, 1.0e4));
            pp.set(Keys.DISPLAY_NAME, "Clamp to ground, Audio icon, Heading 90, Screen relative");
            pp.setLabelText("Placemark B");
            pp.setLineEnabled(false);
            pp.setAltitudeMode(WorldWind.CLAMP_TO_GROUND);
            attrs = new PointPlacemarkAttributes(attrs);
            attrs.setHeading(90.0d);
            attrs.setHeadingReference(Keys.RELATIVE_TO_SCREEN);
            attrs.setScale(0.6);
            attrs.setImageOffset(new Offset(19.0d, 8.0d, Keys.PIXELS, Keys.PIXELS));
            attrs.setLabelOffset(new Offset(0.9d, 0.6d, Keys.FRACTION, Keys.FRACTION));
            pp.setAttributes(attrs);
            layer.add(pp);

            // Place a pin placemark at the same location over the previous one.
            pp = new PointPlacemark(pp.getPosition());
            pp.set(Keys.DISPLAY_NAME, "Clamp to ground, Default icon over rotated audio icon");
            pp.setAltitudeMode(WorldWind.CLAMP_TO_GROUND);
            layer.add(pp);

            // Use a new attributes instance.
            // Note that a new attributes instance must be created for every unique set of attribute values, although
            // the new attributes can be initialized from an existing attributes instance.
            pp = new PointPlacemark(Position.fromDegrees(29, -104, 2.0e4));
            pp.setLabelText("Placemark C");
            pp.set(Keys.DISPLAY_NAME, "Absolute, Label, Red pin icon, Line in random color and 2 wide");
            pp.setLineEnabled(true);
            pp.setAltitudeMode(WorldWind.ABSOLUTE);
            attrs = new PointPlacemarkAttributes();
            attrs.setScale(0.6);
            attrs.setImageOffset(new Offset(19.0d, 8.0d, Keys.PIXELS, Keys.PIXELS));
            attrs.setLabelOffset(new Offset(0.9d, 0.6d, Keys.FRACTION, Keys.FRACTION));
            attrs.setLineMaterial(new Material(WWUtil.makeRandomColor(null)));
            attrs.setLineWidth(2.0d);
            attrs.setImageAddress("images/pushpins/plain-red.png");
            pp.setAttributes(attrs);
            layer.add(pp);

            // Create a placemark without a leader line.
            pp = new PointPlacemark(Position.fromDegrees(30, -104.5, 2.0e4));
            pp.setLabelText("Placemark D");
            pp.set(Keys.DISPLAY_NAME, "Relative to ground, Label, Teal pin icon, No line");
            pp.setAltitudeMode(WorldWind.RELATIVE_TO_GROUND);
            attrs = new PointPlacemarkAttributes(attrs);
            attrs.setImageAddress("images/pushpins/plain-teal.png");
            pp.setAttributes(attrs);
            layer.add(pp);

            // Create a placemark clamped to ground.
            pp = new PointPlacemark(Position.fromDegrees(28, -104.5, 2.0e4));
            pp.setLabelText("Placemark E");
            pp.set(Keys.DISPLAY_NAME, "Clamp to ground, Blue label, White pin icon");
            pp.setAltitudeMode(WorldWind.CLAMP_TO_GROUND);
            attrs = new PointPlacemarkAttributes(attrs);
            attrs.setLabelColor("ffff0000");
            attrs.setImageAddress("images/pushpins/plain-white.png");
            pp.setAttributes(attrs);
            layer.add(pp);

            // Create a placemark that uses all default values.
            pp = new PointPlacemark(Position.fromDegrees(30, -103.5, 2.0e3));
            pp.setLabelText("Placemark F");
            pp.set(Keys.DISPLAY_NAME, "All defaults");
            layer.add(pp);

            // Create a placemark without an image.
            pp = new PointPlacemark(Position.fromDegrees(29, -104.5, 2.0e4));
            pp.setLabelText("Placemark G");
            pp.set(Keys.DISPLAY_NAME, "Clamp to ground, White label, Red point, Scale 5");
            pp.setAltitudeMode(WorldWind.CLAMP_TO_GROUND);
            attrs = new PointPlacemarkAttributes();
            attrs.setLabelColor("ffffffff");
            attrs.setLineColor("ff0000ff");
            attrs.setUsePointAsDefaultImage(true);
            attrs.setScale(5.0d);
            pp.setAttributes(attrs);
            layer.add(pp);

            // Create a placemark off the surface and with a line.
            pp = new PointPlacemark(Position.fromDegrees(30, -104, 2.0e4));
            pp.setLabelText("Placemark H");
            pp.set(Keys.DISPLAY_NAME, "Relative to ground, Blue label, Magenta point and line, Scale 10");
            pp.setAltitudeMode(WorldWind.RELATIVE_TO_GROUND);
            pp.setLineEnabled(true);
            attrs = new PointPlacemarkAttributes();
            attrs.setLabelColor("ffff0000");
            attrs.setLineMaterial(Material.MAGENTA);
            attrs.setLineWidth(2.0d);
            attrs.setUsePointAsDefaultImage(true);
            attrs.setScale(10.0d);
            pp.setAttributes(attrs);
            layer.add(pp);

            pp = new PointPlacemark(Position.fromDegrees(28, -103, 1.0e4));
            pp.set(Keys.DISPLAY_NAME, "Clamp to ground, Audio icon, Heading -45, Globe relative");
            pp.setLabelText("Placemark I");
            pp.setLineEnabled(false);
            pp.setAltitudeMode(WorldWind.CLAMP_TO_GROUND);
            attrs = new PointPlacemarkAttributes(attrs);
            attrs.setImageAddress("gov/nasa/worldwind/examples/images/audioicon-64.png");
            attrs.setHeading(-45.0d);
            attrs.setHeadingReference(Keys.RELATIVE_TO_GLOBE);
            attrs.setScale(0.6);
            attrs.setImageOffset(new Offset(19.0d, 8.0d, Keys.PIXELS, Keys.PIXELS));
            attrs.setLabelColor("ffffffff");
            attrs.setLabelOffset(new Offset(0.9d, 0.6d, Keys.FRACTION, Keys.FRACTION));
            pp.setAttributes(attrs);
            layer.add(pp);

            pp = new PointPlacemark(Position.fromDegrees(30, 179.9, 100.0e3));
            pp.set(Keys.DISPLAY_NAME, "Near dateline,  Clamp to ground, NASA icon, Heading -45, Globe relative");
            pp.setLabelText("Placemark J");
            pp.setLineEnabled(false);
            pp.setAltitudeMode(WorldWind.CLAMP_TO_GROUND);
            attrs = new PointPlacemarkAttributes(attrs);
            attrs.setImageAddress("gov/nasa/worldwind/examples/images/georss.png");
            attrs.setHeading(-45.0d);
            attrs.setHeadingReference(Keys.RELATIVE_TO_GLOBE);
            attrs.setScale(0.6);
            attrs.setLabelColor("ffffffff");
            attrs.setLabelOffset(new Offset(0.9d, 0.6d, Keys.FRACTION, Keys.FRACTION));
            pp.setAttributes(attrs);
            layer.add(pp);

            pp = new PointPlacemark(Position.fromDegrees(90, 0, 100.0e3));
            pp.set(Keys.DISPLAY_NAME, "North Pole,  Clamp to ground, NASA icon, Heading -45, Globe relative");
            pp.setLabelText("Placemark K");
            pp.setLineEnabled(false);
            pp.setAltitudeMode(WorldWind.CLAMP_TO_GROUND);
            attrs = new PointPlacemarkAttributes(attrs);
            attrs.setImageAddress("gov/nasa/worldwind/examples/images/georss.png");
            attrs.setHeading(-45.0d);
            attrs.setHeadingReference(Keys.RELATIVE_TO_GLOBE);
            attrs.setScale(0.6);
            attrs.setLabelColor("ffffffff");
            attrs.setLabelOffset(new Offset(0.9d, 0.6d, Keys.FRACTION, Keys.FRACTION));
            pp.setAttributes(attrs);
            layer.add(pp);

            // Create a placemark that uses a 2525C tactical symbol. The symbol is downloaded from the internet on a
            // separate thread.
            WorldWind.tasks().addTask(() -> createTacticalSymbolPointPlacemark(layer));

            // Add the layer to the model.
            WorldWindow.insertBeforeCompass(wwd(), layer);
        }
    }
}