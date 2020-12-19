/*
 * Copyright (C) 2014 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.examples;

import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.event.*;
import gov.nasa.worldwind.formats.shapefile.ShapefileLayerFactory;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.layers.RenderableLayer;
import gov.nasa.worldwind.pick.PickedObject;
import gov.nasa.worldwind.render.*;
import gov.nasa.worldwind.util.*;
import gov.nasa.worldwind.util.combine.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.logging.Level;

/**
 * Shows how to use the {@link Combinable} interface and the {@link
 * ShapeCombiner} class to compute the intersection of a WorldWind surface shapes with
 * Earth's land and water.
 * <p>
 * This example provides an editable surface circle indicating a region to clip against either land or water. The land
 * and water are represented by an ESRI shapefile containing polygons of Earth's continents, including major islands.
 * Clipping against land is accomplished by computing the intersection of the surface circle and the shapefile polygons.
 * Clipping against water is accomplished by subtracting the shapefile polygons from the surface circle. The user
 * specifies the location of the surface circle, whether to clip against land or water, and the desired resolution of
 * the resultant shape, in kilometers.
 *
 * @author dcollins
 * @version $Id: ShapeClipping.java 2411 2014-10-30 21:27:00Z dcollins $
 */
public class ShapeClipping extends ApplicationTemplate {
    public static void main(String[] args) {
        start("WorldWind Shape Clipping", AppFrame.class);
    }

    public static class AppFrame extends ApplicationTemplate.AppFrame implements SelectListener {
        protected final ShapeClippingPanel clippingPanel;
        protected ShapeEditor editor;
        protected ShapeAttributes lastAttrs;

        public AppFrame() {
            this.clippingPanel = new ShapeClippingPanel(this.wwd());
            this.getControlPanel().add(this.clippingPanel, BorderLayout.SOUTH);

            this.createLandShape();
            this.createClipShape();
        }

        protected void createLandShape() {
            ShapefileLayerFactory factory = (ShapefileLayerFactory) WorldWind.createConfigurationComponent(
                AVKey.SHAPEFILE_LAYER_FACTORY);

            factory.createFromShapefileSource("shapefiles/ne_10m_land.shp",
                new ShapefileLayerFactory.CompletionCallback() {
                    @Override
                    public void completion(final Object result) {
                        if (!SwingUtilities.isEventDispatchThread()) {
                            SwingUtilities.invokeLater(() -> completion(result));
                            return;
                        }

                        RenderableLayer layer = (RenderableLayer) result;
                        Renderable renderable = layer.all().iterator().next();
                        clippingPanel.setLandShape((Combinable) renderable);
                    }

                    @Override
                    public void exception(Exception e) {
                        Logging.logger().log(Level.SEVERE, e.getMessage(), e);
                    }
                });
        }

        protected void createClipShape() {
            ShapeAttributes attrs = new BasicShapeAttributes();
            attrs.setInteriorOpacity(0.3);
            attrs.setOutlineMaterial(new Material(Color.RED));
            attrs.setOutlineWidth(2);

            ShapeAttributes highlightAttrs = new BasicShapeAttributes(attrs);
            highlightAttrs.setInteriorOpacity(0.6);
            highlightAttrs.setOutlineMaterial(new Material(WWUtil.makeColorBrighter(Color.RED)));
            highlightAttrs.setOutlineWidth(4);

            SurfaceCircle circle = new SurfaceCircle(attrs, LatLon.fromDegrees(42.5, -116), 1.0e6);
            circle.setHighlightAttributes(highlightAttrs);
            this.clippingPanel.setClipShape(circle);

            RenderableLayer shapeLayer = new RenderableLayer();
            shapeLayer.setName("Clipping Shape");
            shapeLayer.add(circle);
            this.wwd().model().getLayers().add(shapeLayer);
            this.wwd().addSelectListener(this);
        }

        @Override
        public void accept(SelectEvent event) {
            // This select method identifies the shape to edit.

            PickedObject topObject = event.getTopPickedObject();

            if (event.getEventAction().equals(SelectEvent.LEFT_CLICK)) {
                if (topObject != null && topObject.get() instanceof Renderable) {
                    if (this.editor == null) {
                        // Enable editing of the selected shape.
                        this.editor = new ShapeEditor(wwd(), (Renderable) topObject.get());
                        this.editor.setArmed(true);
                        this.keepShapeHighlighted(true);
                        event.consume();
                    }
                    else if (this.editor.getShape() != event.getTopObject()) {
                        // Switch editor to a different shape.
                        this.keepShapeHighlighted(false);
                        this.editor.setArmed(false);
                        this.editor = new ShapeEditor(wwd(), (Renderable) topObject.get());
                        this.editor.setArmed(true);
                        this.keepShapeHighlighted(true);
                        event.consume();
                    }
                    else if ((event.mouseEvent.getModifiersEx() & MouseEvent.SHIFT_DOWN_MASK) == 0
                        && (event.mouseEvent.getModifiersEx() & MouseEvent.ALT_DOWN_MASK) == 0) {
                        // Disable editing of the current shape. Shift and Alt are used by the editor, so ignore
                        // events with those buttons down.
                        this.editor.setArmed(false);
                        this.keepShapeHighlighted(false);
                        this.editor = null;
                        event.consume();
                    }
                }
            }
        }

        protected void keepShapeHighlighted(boolean tf) {
            if (tf) {
                this.lastAttrs = ((Attributable) this.editor.getShape()).getAttributes();
                ((Attributable) this.editor.getShape()).setAttributes(
                    ((Attributable) this.editor.getShape()).getHighlightAttributes());
            }
            else {
                ((Attributable) this.editor.getShape()).setAttributes(this.lastAttrs);
            }
        }
    }
}
