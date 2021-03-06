/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */

package gov.nasa.worldwind.examples;

import gov.nasa.worldwind.*;
import gov.nasa.worldwind.event.SelectEvent;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.layers.RenderableLayer;
import gov.nasa.worldwind.pick.PickedObject;
import gov.nasa.worldwind.render.*;
import gov.nasa.worldwind.util.*;

import java.awt.*;

/**
 * Illustrates how to use the {@link PlacemarkClutterFilter} to declutter PointPlacemark labels.
 * To enable this decluttering a filter has to be specified to the scene controller and each PointPlacemark that
 * participates in decluttering must be enabled for decluttering.
 * <p>
 * This example also enables label picking for all PointPlacemarks to illustrate that labels can be picked both when
 * they're not decluttered and when they are.
 *
 * @author tag
 * @version $Id: PlacemarkDecluttering.java 2388 2014-10-15 22:58:36Z tgaskins $
 */
public class PlacemarkDecluttering extends ApplicationTemplate {
    public static void main(String[] args) {
        ApplicationTemplate.start("WorldWind Placemark Decluttering", AppFrame.class);
    }

    public static class AppFrame extends ApplicationTemplate.AppFrame {
        public AppFrame() {
            super(true, true, false);

            // Specify the decluttering filter to the scene controller.
            this.wwd().sceneControl().setClutterFilter(new PlacemarkClutterFilter());

            RenderableLayer layer = new RenderableLayer();

            PointPlacemark pp = new PointPlacemark(Position.fromDegrees(28, -102, 1.0e4));
            pp.setEnableDecluttering(true); // enable the placemark for decluttering
            pp.setEnableLabelPicking(true); // enable the placemark for label picking
            pp.setLabelText("Placemark A");
            pp.set(Keys.DISPLAY_NAME, "Clamp to ground, Label, Semi-transparent, Audio icon");
            pp.setLineEnabled(false);
            pp.setAltitudeMode(WorldWind.CLAMP_TO_GROUND);
            PointPlacemarkAttributes attrs = new PointPlacemarkAttributes();
            attrs.setImageAddress("gov/nasa/worldwind/examples/images/audioicon-64.png");
            attrs.setImageColor(new Color(1.0f, 1.0f, 1.0f, 0.6f));
            attrs.setScale(0.6);
            attrs.setLabelOffset(new Offset(0.9d, 0.6d, Keys.FRACTION, Keys.FRACTION));
            pp.setAttributes(attrs);
            AppFrame.setHighlightAttributes(pp);
            layer.add(pp);

            // Place a default pin placemark at the same location over the previous one.
            pp = new PointPlacemark(pp.getPosition());
            pp.setEnableDecluttering(true);
            pp.set(Keys.DISPLAY_NAME, "Clamp to ground, Default icon over audio icon");
            pp.setAltitudeMode(WorldWind.CLAMP_TO_GROUND);
            layer.add(pp);

            pp = new PointPlacemark(Position.fromDegrees(28, -104, 1.0e4));
            pp.set(Keys.DISPLAY_NAME, "Clamp to ground, Audio icon, Heading 90, Screen relative");
            pp.setLabelText("Placemark B");
            pp.setEnableDecluttering(true);
            pp.setEnableLabelPicking(true);
            pp.setLineEnabled(false);
            pp.setAltitudeMode(WorldWind.CLAMP_TO_GROUND);
            attrs = new PointPlacemarkAttributes(attrs);
            attrs.setHeading(90.0d);
            attrs.setHeadingReference(Keys.RELATIVE_TO_SCREEN);
            attrs.setScale(0.6);
            attrs.setImageOffset(new Offset(19.0d, 8.0d, Keys.PIXELS, Keys.PIXELS));
            attrs.setLabelOffset(new Offset(0.9d, 0.6d, Keys.FRACTION, Keys.FRACTION));
            pp.setAttributes(attrs);
            AppFrame.setHighlightAttributes(pp);
            layer.add(pp);

            // Place a pin placemark at the same location over the previous one.
            pp = new PointPlacemark(pp.getPosition());
            pp.setEnableDecluttering(true);
            pp.set(Keys.DISPLAY_NAME, "Clamp to ground, Default icon over rotated audio icon");
            pp.setAltitudeMode(WorldWind.CLAMP_TO_GROUND);
            AppFrame.setHighlightAttributes(pp);
            layer.add(pp);

            // Use a new attributes instance.
            // Note that a new attributes instance must be created for every unique set of attribute values, although
            // the new attributes can be initialized from an existing attributes instance.
            pp = new PointPlacemark(Position.fromDegrees(29, -104, 2.0e4));
            pp.setEnableDecluttering(true);
            pp.setEnableLabelPicking(true);
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
            AppFrame.setHighlightAttributes(pp);
            layer.add(pp);

            // Create a placemark without a leader line.
            pp = new PointPlacemark(Position.fromDegrees(30, -104.5, 2.0e4));
            pp.setEnableDecluttering(true);
            pp.setEnableLabelPicking(true);
            pp.setLabelText("Placemark D");
            pp.set(Keys.DISPLAY_NAME, "Relative to ground, Label, Teal pin icon, No line");
            pp.setAltitudeMode(WorldWind.RELATIVE_TO_GROUND);
            attrs = new PointPlacemarkAttributes(attrs);
            attrs.setImageAddress("images/pushpins/plain-teal.png");
            pp.setAttributes(attrs);
            AppFrame.setHighlightAttributes(pp);
            layer.add(pp);

            // Create a placemark clamped to ground.
            pp = new PointPlacemark(Position.fromDegrees(28, -104.5, 2.0e4));
            pp.setEnableDecluttering(true);
            pp.setEnableLabelPicking(true);
            pp.setLabelText("Placemark E");
            pp.set(Keys.DISPLAY_NAME, "Clamp to ground, Blue label, White pin icon");
            pp.setAltitudeMode(WorldWind.CLAMP_TO_GROUND);
            attrs = new PointPlacemarkAttributes(attrs);
            attrs.setLabelColor("ffff0000");
            attrs.setImageAddress("images/pushpins/plain-white.png");
            pp.setAttributes(attrs);
            AppFrame.setHighlightAttributes(pp);
            layer.add(pp);

            // Create a placemark that uses all default values.
            pp = new PointPlacemark(Position.fromDegrees(30, -103.5, 2.0e3));
            pp.setEnableDecluttering(true);
            pp.setEnableLabelPicking(true);
            pp.setLabelText("Placemark F");
            pp.set(Keys.DISPLAY_NAME, "All defaults");
            AppFrame.setHighlightAttributes(pp);
            layer.add(pp);

            // Create a placemark without an image.
            pp = new PointPlacemark(Position.fromDegrees(29, -104.5, 2.0e4));
            pp.setEnableDecluttering(true);
            pp.setEnableLabelPicking(true);
            pp.setLabelText("Placemark G");
            pp.set(Keys.DISPLAY_NAME, "Clamp to ground, White label, Red point, Scale 5");
            pp.setAltitudeMode(WorldWind.CLAMP_TO_GROUND);
            attrs = new PointPlacemarkAttributes();
            attrs.setLabelColor("ffffffff");
            attrs.setLineColor("ff0000ff");
            attrs.setUsePointAsDefaultImage(true);
            attrs.setScale(5.0d);
            pp.setAttributes(attrs);
            AppFrame.setHighlightAttributes(pp);
            layer.add(pp);

            // Create a placemark off the surface and with a line.
            pp = new PointPlacemark(Position.fromDegrees(30, -104, 2.0e4));
            pp.setEnableDecluttering(true);
            pp.setEnableLabelPicking(true);
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
            AppFrame.setHighlightAttributes(pp);
            layer.add(pp);

            pp = new PointPlacemark(Position.fromDegrees(28, -103, 1.0e4));
            pp.setEnableDecluttering(true);
            pp.setEnableLabelPicking(true);
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
            AppFrame.setHighlightAttributes(pp);
            layer.add(pp);

            // Add the layer to the model.
            WorldWindow.insertBeforeCompass(wwd(), layer);

            // Add a select listener in order to determine when a label is clicked on.
            this.wwd().addSelectListener(event -> {
                PickedObject po = event.getTopPickedObject();
                if (po != null && po.get() instanceof PointPlacemark) {
                    if (event.getEventAction().equals(SelectEvent.LEFT_CLICK)) {
                        // See if it was the label that was picked. If so, raise an input dialog prompting
                        // for new label text.
                        Object placemarkPiece = po.get(Keys.PICKED_OBJECT_ID);
                        if (placemarkPiece != null && placemarkPiece.equals(Keys.LABEL)) {
                            PointPlacemark placemark = (PointPlacemark) po.get();
                            String labelText = placemark.getLabelText();
                            System.out.println(labelText);
                            event.consume();
                        }
                    }
                }
            });
        }

        protected static void setHighlightAttributes(PointPlacemark pp) {
            // Change the label color to orange when the placemark is selected.
            PointPlacemarkAttributes highlightAttributes = new PointPlacemarkAttributes(pp.getAttributes());
            highlightAttributes.setLabelMaterial(Material.ORANGE);
            pp.setHighlightAttributes(highlightAttributes);
        }
    }
}