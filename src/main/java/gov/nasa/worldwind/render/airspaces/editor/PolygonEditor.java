/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.render.airspaces.editor;

import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.render.airspaces.Polygon;
import gov.nasa.worldwind.render.airspaces.*;

import java.awt.*;
import java.util.List;
import java.util.*;

/**
 * @author dcollins
 * @version $Id: PolygonEditor.java 1171 2013-02-11 21:45:02Z dcollins $
 */
public class PolygonEditor extends AbstractAirspaceEditor {
    private static final double DEFAULT_POLYGON_HEIGHT = 10.0;
    private gov.nasa.worldwind.render.airspaces.Polygon polygon; // Can be null

    public PolygonEditor(AirspaceControlPointRenderer renderer) {
        super(renderer);
    }

    public PolygonEditor() {
    }

    public Airspace getAirspace() {
        return this.getPolygon();
    }

    public gov.nasa.worldwind.render.airspaces.Polygon getPolygon() {
        return this.polygon;
    }

    public void setPolygon(Polygon polygon) {
        this.polygon = polygon;
    }

    //**************************************************************//
    //********************  Control Point Assembly  ****************//
    //**************************************************************//

    protected void assembleControlPoints(DrawContext dc) {
        if (this.getPolygon() == null)
            return;

        int numLocations = this.getPolygon().getLocations().size();
        boolean isCollapsed = this.getPolygon().isAirspaceCollapsed();

        for (int locationIndex = 0; locationIndex < numLocations; locationIndex++) {
            // If the polygon is not collapsed, then add the lower altitude control points.
            if (!isCollapsed) {
                this.addPolygonControlPoint(dc, locationIndex, AbstractAirspaceEditor.LOWER_ALTITUDE);
            }

            // Add the upper altitude control points.
            this.addPolygonControlPoint(dc, locationIndex, AbstractAirspaceEditor.UPPER_ALTITUDE);
        }
    }

    protected void addPolygonControlPoint(DrawContext dc, int locationIndex, int altitudeIndex) {
        LatLon location = this.getPolygon().getLocations().get(locationIndex);
        double altitude = this.getPolygon().getAltitudes()[altitudeIndex];
        boolean terrainConforming = this.getPolygon().isTerrainConforming()[altitudeIndex];

        // Apply the vertical exaggeration to the polygon's altitude when computing the Cartesian point of the polygon's
        // vertex. We do this to match the logic in Polygon, which applies vertical exaggeration to the altitude of its
        // vertices.
        Vec4 point = this.getPolygon().computePointFromPosition(dc, location.getLat(), location.getLon(),
            dc.getVerticalExaggeration() * altitude, terrainConforming);

        AirspaceControlPoint controlPoint =
            new BasicAirspaceControlPoint(this, this.getPolygon(), locationIndex, altitudeIndex, point);

        this.addControlPoint(dc, controlPoint);
    }

    //**************************************************************//
    //********************  Control Point Events  ******************//
    //**************************************************************//

    protected AirspaceControlPoint doAddControlPoint(WorldWindow wwd, Airspace airspace,
        Point mousePoint) {
        if (this.getPolygon().getLocations().isEmpty()) {
            return this.doAddFirstLocation(wwd, mousePoint);
        } else {
            return this.doAddNextLocation(wwd, mousePoint);
        }
    }

    protected AirspaceControlPoint doAddFirstLocation(WorldWindow wwd, Point mousePoint) {
        // Adding the first location is unique in two ways:
        //
        // First, the airspace has no existing locations, so the only reference we have to interpret the user's intent
        // is the terrain. We will not modify the terrain conformance property. However we will modify the altitude
        // property to ensure the shape appears correctly on the terrain.
        //
        // Second, the app may want rubber band creation of the first two points. Therefore we add two points and 
        // return a handle to the second point. If rubber banding is enabled, then we return a control point
        // referencing to the second location. Otherwise we return a control point referencing the first location.

        Line ray = wwd.view().computeRayFromScreenPoint(mousePoint.getX(), mousePoint.getY());
        double surfaceElevation = AirspaceEditorUtil.surfaceElevationAt(wwd, ray);

        Vec4 newPoint = AirspaceEditorUtil.intersectGlobeAt(wwd, surfaceElevation, ray);
        if (newPoint == null) {
            return null;
        }

        Position newPosition = wwd.model().globe().computePositionFromPoint(newPoint);

        boolean[] terrainConformance = this.getPolygon().isTerrainConforming();
        double[] altitudes = new double[2];
        altitudes[AbstractAirspaceEditor.LOWER_ALTITUDE] = terrainConformance[AbstractAirspaceEditor.LOWER_ALTITUDE] ? 0.0 : newPosition.getElevation();
        altitudes[AbstractAirspaceEditor.UPPER_ALTITUDE] = terrainConformance[AbstractAirspaceEditor.UPPER_ALTITUDE] ? 0.0
            : newPosition.getElevation() + PolygonEditor.DEFAULT_POLYGON_HEIGHT;
        this.getPolygon().setAltitudes(altitudes[AbstractAirspaceEditor.LOWER_ALTITUDE], altitudes[AbstractAirspaceEditor.UPPER_ALTITUDE]);

        Collection<LatLon> locationList = new ArrayList<>();
        locationList.add(new LatLon(newPosition));

        // If rubber banding is enabled, add a second entry at the same location.
        if (this.isUseRubberBand()) {
            locationList.add(new LatLon(newPosition));
        }

        this.getPolygon().setLocations(locationList);

        AirspaceControlPoint controlPoint =
            new BasicAirspaceControlPoint(this, this.getPolygon(), 0, AbstractAirspaceEditor.LOWER_ALTITUDE, newPoint);
        this.fireControlPointAdded(new AirspaceEditEvent(wwd, this.getAirspace(), this, controlPoint));

        // If rubber banding is enabled, fire a second add event, and return a reference to the second location.
        if (this.isUseRubberBand()) {
            controlPoint = new BasicAirspaceControlPoint(this, this.getPolygon(), 1,
                AbstractAirspaceEditor.LOWER_ALTITUDE, newPoint);
            this.fireControlPointAdded(new AirspaceEditEvent(wwd, this.getAirspace(), this, controlPoint));
        }

        return controlPoint;
    }

    protected AirspaceControlPoint doAddNextLocation(WorldWindow wwd, Point mousePoint) {
        // Try to find the edge that is closest to a ray passing through the screen point. We're trying to determine
        // the user's intent as to which edge a new two control points should be added to. We create a list of all
        // potentiall control point edges, then find the best match. We compute the new location by intersecting the
        // geoid with the screen ray, then create a new control point by inserting that point into the location list
        // based on the points orientaton relative to the edge.

        List<AirspaceEditorUtil.EdgeInfo> edgeInfoList = AirspaceEditorUtil.computeEdgeInfoFor(
            this.getPolygon().getLocations().size(), this.getCurrentControlPoints());

        if (edgeInfoList.isEmpty()) {
            return null;
        }

        Line ray = wwd.view().computeRayFromScreenPoint(mousePoint.getX(), mousePoint.getY());
        AirspaceEditorUtil.EdgeInfo bestMatch = AirspaceEditorUtil.selectBestEdgeMatch(
            wwd, ray, this.getAirspace(), edgeInfoList);

        if (bestMatch == null) {
            return null;
        }

        AirspaceControlPoint controlPoint = AirspaceEditorUtil.createControlPointFor(
            wwd, ray, this, this.getAirspace(), bestMatch);

        Vec4 newPoint = controlPoint.getPoint();
        LatLon newLocation = new LatLon(wwd.model().globe().computePositionFromPoint(newPoint));

        List<LatLon> locationList = new ArrayList<>(this.getPolygon().getLocations());
        locationList.add(controlPoint.getLocationIndex(), newLocation);
        this.getPolygon().setLocations(locationList);

        this.fireControlPointAdded(new AirspaceEditEvent(wwd, this.getAirspace(), this, controlPoint));

        return controlPoint;
    }

    protected void doRemoveControlPoint(WorldWindow wwd, AirspaceControlPoint controlPoint) {
        int index = controlPoint.getLocationIndex();
        List<LatLon> newLocationList = new ArrayList<>(this.getPolygon().getLocations());
        newLocationList.remove(index);
        this.getPolygon().setLocations(newLocationList);

        this.fireControlPointRemoved(new AirspaceEditEvent(wwd, controlPoint.getAirspace(), this, controlPoint));
    }

    protected void doMoveControlPoint(WorldWindow wwd, AirspaceControlPoint controlPoint,
        Point mousePoint, Point previousMousePoint) {
        // Intersect a ray throuh each mouse point, with a geoid passing through the selected control point. Since
        // most airspace control points follow a fixed altitude, this will track close to the intended mouse position.
        // If either ray fails to intersect the geoid, then ignore this event. Use the difference between the two
        // intersected positions to move the control point's location.

        Position controlPointPos = wwd.model().globe().computePositionFromPoint(controlPoint.getPoint());

        Line ray = wwd.view().computeRayFromScreenPoint(mousePoint.getX(), mousePoint.getY());
        Line previousRay = wwd.view().computeRayFromScreenPoint(previousMousePoint.getX(),
            previousMousePoint.getY());

        Vec4 vec = AirspaceEditorUtil.intersectGlobeAt(wwd, controlPointPos.getElevation(), ray);
        Vec4 previousVec = AirspaceEditorUtil.intersectGlobeAt(wwd, controlPointPos.getElevation(), previousRay);

        if (vec == null || previousVec == null) {
            return;
        }

        Position pos = wwd.model().globe().computePositionFromPoint(vec);
        Position previousPos = wwd.model().globe().computePositionFromPoint(previousVec);
        LatLon change = pos.subtract(previousPos);

        int index = controlPoint.getLocationIndex();
        List<LatLon> newLocationList = new ArrayList<>(this.getPolygon().getLocations());
        LatLon newLatLon = newLocationList.get(index).add(change);
        newLocationList.set(index, newLatLon);
        this.getPolygon().setLocations(newLocationList);

        this.fireControlPointChanged(new AirspaceEditEvent(wwd, controlPoint.getAirspace(), this, controlPoint));
    }

    protected void doResizeAtControlPoint(WorldWindow wwd, AirspaceControlPoint controlPoint,
        Point mousePoint, Point previousMousePoint) {
        // Find the closest points between the rays through each screen point, and the ray from the control point
        // and in the direction of the globe's surface normal. Compute the elevation difference between these two
        // points, and use that as the change in airspace altitude.
        //
        // When the airspace is collapsed, override the
        // selected control point altitude. This will typically be the case when the airspace is new. If the user drags
        // up, then adjust the upper altiutde. If the user drags down, then adjust the lower altitude.
        //
        // If the state keepControlPointsAboveTerrain is set, we prevent the control point from passing any lower than
        // the terrain elevation beneath it.

        Vec4 surfaceNormal = wwd.model().globe().computeSurfaceNormalAtPoint(controlPoint.getPoint());
        Line verticalRay = new Line(controlPoint.getPoint(), surfaceNormal);
        Line screenRay = wwd.view().computeRayFromScreenPoint(previousMousePoint.getX(), previousMousePoint.getY());
        Line previousScreenRay = wwd.view().computeRayFromScreenPoint(mousePoint.getX(), mousePoint.getY());

        Vec4 pointOnLine = AirspaceEditorUtil.nearestPointOnLine(verticalRay, screenRay);
        Vec4 previousPointOnLine = AirspaceEditorUtil.nearestPointOnLine(verticalRay, previousScreenRay);

        Position pos = wwd.model().globe().computePositionFromPoint(pointOnLine);
        Position previousPos = wwd.model().globe().computePositionFromPoint(previousPointOnLine);
        double elevationChange = previousPos.getElevation() - pos.getElevation();

        int index;
        if (this.getPolygon().isAirspaceCollapsed()) {
            index = (elevationChange < 0) ? AbstractAirspaceEditor.LOWER_ALTITUDE : AbstractAirspaceEditor.UPPER_ALTITUDE;
        } else {
            index = controlPoint.getAltitudeIndex();
        }

        double[] altitudes = controlPoint.getAirspace().getAltitudes();
        boolean[] terrainConformance = controlPoint.getAirspace().isTerrainConforming();

        if (this.isKeepControlPointsAboveTerrain()) {
            if (terrainConformance[index]) {
                if (altitudes[index] + elevationChange < 0.0)
                    elevationChange = -altitudes[index];
            } else {
                double height = AirspaceEditorUtil.computeLowestHeightAboveSurface(
                    wwd, this.getCurrentControlPoints(), index);
                if (elevationChange <= -height)
                    elevationChange = -height;
            }
        }

        double d = AirspaceEditorUtil.computeMinimumDistanceBetweenAltitudes(this.getPolygon().getLocations().size(),
            this.getCurrentControlPoints());
        if (index == AbstractAirspaceEditor.LOWER_ALTITUDE) {
            if (elevationChange > d)
                elevationChange = d;
        } else if (index == AbstractAirspaceEditor.UPPER_ALTITUDE) {
            if (elevationChange < -d)
                elevationChange = -d;
        }

        altitudes[index] += elevationChange;
        controlPoint.getAirspace().setAltitudes(altitudes[AbstractAirspaceEditor.LOWER_ALTITUDE], altitudes[AbstractAirspaceEditor.UPPER_ALTITUDE]);

        AirspaceEditEvent editEvent = new AirspaceEditEvent(wwd, controlPoint.getAirspace(), this, controlPoint);
        this.fireControlPointChanged(editEvent);
        this.fireAirspaceResized(editEvent);
    }
}