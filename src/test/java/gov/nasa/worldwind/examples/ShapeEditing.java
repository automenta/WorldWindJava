/*
 * Copyright (C) 2014 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */

package gov.nasa.worldwind.examples;

import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.event.*;
import gov.nasa.worldwind.examples.render.*;
import gov.nasa.worldwind.examples.render.airspaces.Polygon;
import gov.nasa.worldwind.examples.render.airspaces.*;
import gov.nasa.worldwind.examples.render.markers.*;
import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.layers.*;
import gov.nasa.worldwind.pick.PickedObject;
import gov.nasa.worldwind.util.ShapeEditor;

import java.awt.*;
import java.awt.event.*;
import java.util.List;
import java.util.*;

/**
 * @author tag
 * @version $Id: ShapeEditing.java 3423 2015-09-23 20:59:03Z tgaskins $
 */
public class ShapeEditing extends ApplicationTemplate {
    public static void main(String[] args) {
        ApplicationTemplate.start("WorldWind Shape Editing", ShapeEditing.AppFrame.class);
    }

    public static class AppFrame extends ApplicationTemplate.AppFrame implements SelectListener {
        protected ShapeEditor editor;
        protected ShapeAttributes lastAttrs;

        public AppFrame() {
            this.getWwd().addSelectListener(this);

            RenderableLayer layer = new RenderableLayer();

            // Airspaces
            boolean useSurfaceAirspaces = false;

            AirspaceAttributes attrs = new BasicAirspaceAttributes();
            attrs.setDrawInterior(true);
            attrs.setDrawOutline(true);
            attrs.setInteriorMaterial(new Material(Color.WHITE));
            attrs.setOutlineMaterial(new Material(Color.BLACK));
            attrs.setOutlineWidth(2);
            attrs.setEnableAntialiasing(true);

            AirspaceAttributes highlightAttrs = new BasicAirspaceAttributes(attrs);
            highlightAttrs.setOutlineMaterial(new Material(Color.RED));

            java.util.List<LatLon> locations = new ArrayList<>();
            locations.add(LatLon.fromDegrees(40, -121));
            locations.add(LatLon.fromDegrees(40, -120));
            locations.add(LatLon.fromDegrees(41, -120));
            locations.add(LatLon.fromDegrees(41, -121));
            Airspace polygon = new Polygon(locations);
            polygon.setDrawSurfaceShape(useSurfaceAirspaces);
            polygon.setAttributes(attrs);
            polygon.setHighlightAttributes(highlightAttrs);
            polygon.setAltitudes(1.0e4, 2.0e4);
            polygon.setAltitudeDatum(AVKey.ABOVE_GROUND_LEVEL, AVKey.ABOVE_GROUND_LEVEL);
            layer.add(polygon);

            CappedCylinder cylinder = new CappedCylinder(LatLon.fromDegrees(40.5, -118), 5.0e4);
            cylinder.setDrawSurfaceShape(useSurfaceAirspaces);
            cylinder.setAttributes(attrs);
            cylinder.setHighlightAttributes(highlightAttrs);
            cylinder.setAltitudeDatum(AVKey.ABOVE_GROUND_LEVEL, AVKey.ABOVE_GROUND_LEVEL);
            cylinder.setAltitudes(1.0e4, 2.0e4);
            layer.add(cylinder);

            cylinder = new CappedCylinder(LatLon.fromDegrees(40.5, -116), 5.0e4);
            cylinder.setRadii(3.0e4, 5.0e4);
            cylinder.setDrawSurfaceShape(useSurfaceAirspaces);
            cylinder.setAttributes(attrs);
            cylinder.setHighlightAttributes(highlightAttrs);
            cylinder.setAltitudeDatum(AVKey.ABOVE_GROUND_LEVEL, AVKey.ABOVE_GROUND_LEVEL);
            cylinder.setAltitudes(1.0e4, 2.0e4);
            layer.add(cylinder);

            Airspace orbit = new Orbit(LatLon.fromDegrees(40, -114), LatLon.fromDegrees(41, -114),
                Orbit.OrbitType.CENTER,
                4.0e4);
            orbit.setDrawSurfaceShape(useSurfaceAirspaces);
            orbit.setAttributes(attrs);
            orbit.setHighlightAttributes(highlightAttrs);
            orbit.setAltitudeDatum(AVKey.ABOVE_GROUND_LEVEL, AVKey.ABOVE_GROUND_LEVEL);
            orbit.setAltitudes(1.0e4, 2.0e4);
            layer.add(orbit);

            locations = new ArrayList<>();
            locations.add(LatLon.fromDegrees(40, -113));
            locations.add(LatLon.fromDegrees(41, -112));
            locations.add(LatLon.fromDegrees(41, -111));
            locations.add(LatLon.fromDegrees(40, -112));
            Route route = new Route(locations, 4.0e4);
            route.setDrawSurfaceShape(useSurfaceAirspaces);
            route.setAttributes(attrs);
            route.setHighlightAttributes(highlightAttrs);
            route.setAltitudes(1.0e4, 2.0e4);
            route.setAltitudeDatum(AVKey.ABOVE_GROUND_LEVEL, AVKey.ABOVE_GROUND_LEVEL);
            layer.add(route);

            locations = new ArrayList<>();
            locations.add(LatLon.fromDegrees(40, -110));
            locations.add(LatLon.fromDegrees(41, -110));
            locations.add(LatLon.fromDegrees(41, -109));
            locations.add(LatLon.fromDegrees(40, -109));
            Airspace curtain = new Curtain(locations);
            curtain.setDrawSurfaceShape(useSurfaceAirspaces);
            curtain.setAttributes(attrs);
            curtain.setHighlightAttributes(highlightAttrs);
            curtain.setAltitudes(1.0e4, 2.0e4);
            curtain.setAltitudeDatum(AVKey.ABOVE_GROUND_LEVEL, AVKey.ABOVE_GROUND_LEVEL);
            layer.add(curtain);

            Airspace sphere = new SphereAirspace(LatLon.fromDegrees(40.5, -107), 5.0e4);
            sphere.setDrawSurfaceShape(useSurfaceAirspaces);
            sphere.setAttributes(attrs);
            sphere.setHighlightAttributes(highlightAttrs);
            sphere.setAltitudeDatum(AVKey.ABOVE_GROUND_LEVEL, AVKey.ABOVE_GROUND_LEVEL);
            sphere.setAltitude(1.5e4);
            layer.add(sphere);

            PartialCappedCylinder partialCylinder = new PartialCappedCylinder(LatLon.fromDegrees(40.5, -105), 5.0e4);
            partialCylinder.setAzimuths(Angle.fromDegrees(270), Angle.fromDegrees(90));
            partialCylinder.setRadii(3.0e4, 5.0e4);
            partialCylinder.setDrawSurfaceShape(useSurfaceAirspaces);
            partialCylinder.setAttributes(attrs);
            partialCylinder.setHighlightAttributes(highlightAttrs);
            partialCylinder.setAltitudeDatum(AVKey.ABOVE_GROUND_LEVEL, AVKey.ABOVE_GROUND_LEVEL);
            partialCylinder.setAltitudes(1.0e4, 2.0e4);
            layer.add(partialCylinder);

            TrackAirspace track = new TrackAirspace();
            track.addLeg(LatLon.fromDegrees(40, -103), LatLon.fromDegrees(41, -103), 1.0e4, 2.0e4, 2.0e4, 2.0e4);
            track.addLeg(LatLon.fromDegrees(41, -103), LatLon.fromDegrees(41, -102), 1.0e4, 2.0e4, 2.0e4, 2.0e4);
            track.addLeg(LatLon.fromDegrees(41, -102), LatLon.fromDegrees(40, -102), 1.0e4, 2.0e4, 2.0e4, 2.0e4);
//            track.addLeg(LatLon.fromDegrees(39.5, -102), LatLon.fromDegrees(39.5, -103), 1e4, 2e4, 2e4, 2e4);
            track.setDrawSurfaceShape(useSurfaceAirspaces);
            track.setAttributes(attrs);
            track.setHighlightAttributes(highlightAttrs);
            track.setAltitudeDatum(AVKey.ABOVE_GROUND_LEVEL, AVKey.ABOVE_GROUND_LEVEL);
            track.setAltitudes(1.0e4, 2.0e4);
            layer.add(track);

            CappedEllipticalCylinder cec = new CappedEllipticalCylinder(LatLon.fromDegrees(40.5, -100), 5.0e4, 6.0e4,
                Angle.fromDegrees(0));
            cec.setRadii(3.0e4, 4.0e4, 5.0e4, 6.0e4);
            cec.setDrawSurfaceShape(useSurfaceAirspaces);
            cec.setAttributes(attrs);
            cec.setHighlightAttributes(highlightAttrs);
            cec.setAltitudeDatum(AVKey.ABOVE_GROUND_LEVEL, AVKey.ABOVE_GROUND_LEVEL);
            cec.setAltitudes(1.0e4, 2.0e4);
            layer.add(cec);

            // Surface Shapes

            ShapeAttributes shapeAttributes = new BasicShapeAttributes();
            attrs.setDrawInterior(true);
            attrs.setDrawOutline(true);
            attrs.setInteriorMaterial(new Material(Color.WHITE));
            attrs.setOutlineMaterial(new Material(Color.BLACK));
            attrs.setOutlineWidth(2);

            locations = new ArrayList<>();
            locations.add(LatLon.fromDegrees(42, -121));
            locations.add(LatLon.fromDegrees(42, -120));
            locations.add(LatLon.fromDegrees(43, -120));
            locations.add(LatLon.fromDegrees(43, -121));
            SurfaceShape surfacePolygon = new SurfacePolygon(attrs, locations);
            surfacePolygon.setHighlightAttributes(highlightAttrs);
            layer.add(surfacePolygon);

            locations = new ArrayList<>();
            locations.add(LatLon.fromDegrees(42, -119));
            locations.add(LatLon.fromDegrees(42, -118));
            locations.add(LatLon.fromDegrees(43, -118));
            locations.add(LatLon.fromDegrees(43, -119));
            SurfaceShape polyline = new SurfacePolyline(attrs, locations);
            polyline.setHighlightAttributes(highlightAttrs);
            layer.add(polyline);

            SurfaceShape circle = new SurfaceCircle(attrs, LatLon.fromDegrees(42.5, -116), 1.0e5);
            circle.setHighlightAttributes(highlightAttrs);
            layer.add(circle);

            SurfaceSquare square = new SurfaceSquare(attrs, LatLon.fromDegrees(42.5, -113), 1.0e5);
            square.setHeading(Angle.fromDegrees(30));
            square.setHighlightAttributes(highlightAttrs);
            layer.add(square);

            SurfaceQuad quad = new SurfaceQuad(attrs, LatLon.fromDegrees(42.5, -111), 1.0e5, 1.0e5);
            quad.setHeading(Angle.fromDegrees(30));
            quad.setHighlightAttributes(highlightAttrs);
            layer.add(quad);

            SurfaceEllipse ellipse = new SurfaceEllipse(attrs, LatLon.fromDegrees(42.5, -108), 1.0e5, 1.5e5);
            ellipse.setHeading(Angle.fromDegrees(30));
            ellipse.setHighlightAttributes(highlightAttrs);
            layer.add(ellipse);

            Collection<Marker> markers = new ArrayList<>(1);
            markers.add(new BasicMarker(Position.fromDegrees(90, 0), new BasicMarkerAttributes()));
            MarkerLayer markerLayer = new MarkerLayer();
            markerLayer.setMarkers(markers);
            insertBeforeCompass(getWwd(), markerLayer);

            List<Position> positions = new ArrayList<>(2);
            positions.add(Position.fromDegrees(-90, 180));
            positions.add(Position.fromDegrees(90, 180));
            Path antiMeridian = new Path(positions);
            antiMeridian.setSurfacePath(true);
            ShapeAttributes antiMeridianAttributes = new BasicShapeAttributes();
            shapeAttributes.setOutlineMaterial(Material.WHITE);
            antiMeridian.setAttributes(antiMeridianAttributes);
            antiMeridian.setHighlightAttributes(antiMeridianAttributes);
            layer.add(antiMeridian);

            insertBeforePlacenames(getWwd(), layer);
        }

        @Override
        public void selected(SelectEvent event) {
            // This select method identifies the shape to edit.

            PickedObject topObject = event.getTopPickedObject();

            if (event.getEventAction().equals(SelectEvent.LEFT_CLICK)) {
                if (topObject != null && this.isEditableShape(topObject.getObject())) {
                    if (this.editor == null) {
                        // Enable editing of the selected shape.
                        this.editor = new ShapeEditor(getWwd(), (Renderable) topObject.getObject());
                        this.editor.setArmed(true);
                        this.keepShapeHighlighted(true);
                        event.consume();
                    }
                    else if (this.editor.getShape() != event.getTopObject()) {
                        // Switch editor to a different shape.
                        this.keepShapeHighlighted(false);
                        this.editor.setArmed(false);
                        this.editor = new ShapeEditor(getWwd(), (Renderable) topObject.getObject());
                        this.editor.setArmed(true);
                        this.keepShapeHighlighted(true);
                        event.consume();
                    }
                    else if ((event.getMouseEvent().getModifiersEx() & MouseEvent.SHIFT_DOWN_MASK) == 0
                        && (event.getMouseEvent().getModifiersEx() & MouseEvent.ALT_DOWN_MASK) == 0) {
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

        protected boolean isEditableShape(Object object) {
            return object instanceof Airspace || object instanceof SurfaceShape;
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