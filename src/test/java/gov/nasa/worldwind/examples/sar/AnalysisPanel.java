/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */

package gov.nasa.worldwind.examples.sar;

import gov.nasa.worldwind.*;
import gov.nasa.worldwind.event.*;
import gov.nasa.worldwind.examples.sar.render.*;
import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.globes.Globe;
import gov.nasa.worldwind.layers.RenderableLayer;
import gov.nasa.worldwind.layers.tool.CrosshairLayer;
import gov.nasa.worldwind.util.*;
import gov.nasa.worldwind.view.orbit.*;

import javax.swing.*;
import java.awt.*;
import java.beans.*;

/**
 * @author tag
 * @version $Id: AnalysisPanel.java 1171 2013-02-11 21:45:02Z dcollins $
 */
public class AnalysisPanel extends JPanel implements Restorable {
    public static final String ANALYSIS_PANEL_STATE = "AnalysisPanelState";
    private final TerrainProfilePanel terrainProfilePanel;
    private final JFrame terrainProfileFrame;
    private final CloudCeilingPanel cloudCeilingPanel;
    private final JFrame cloudCeilingFrame;
    private final CrosshairLayer crosshairLayer;
    private final RenderableLayer trackRenderables;
    private final PlaneModel planeModel;
    private final TrackSegmentInfo segmentInfo;
    private WorldWindow wwd;
    private TrackController trackController;
    private SARTrack currentTrack;
    private TrackViewPanel trackViewPanel;
    private boolean crosshairNeedsUpdate = false;
    private final RenderingListener renderingListener = event -> {
        if (crosshairNeedsUpdate && event.getStage().equals(RenderingEvent.AFTER_BUFFER_SWAP)) {
            doUpdateCrosshair();
        }
    };
    private String trackInfoState;
    private String lastUpdateViewMode;
    private ViewState examineViewState;
    private final PropertyChangeListener propertyChangeListener = new PropertyChangeListener() {
        @SuppressWarnings("StringEquality")
        public void propertyChange(PropertyChangeEvent propertyChangeEvent) {

            if (propertyChangeEvent.getPropertyName() == TrackViewPanel.VIEW_CHANGE) {
                // When the view mode has changed, update the view parameters gradually.
                updateView(true);
            }
            else if (propertyChangeEvent.getPropertyName() == TrackViewPanel.POSITION_CHANGE) {
                // When the track position has changed, update the view parameters immediately.
                updateView(false);
                cloudCeilingPanel.setTrackCurrentPositionNumber(getCurrentPositionNumber());
            }
            else if (propertyChangeEvent.getPropertyName() == TrackController.TRACK_MODIFY) {
                // When the track has changed, update the view parameters immediately
                updateView(false);
                // Update the terrain profile path and cloud ceiling too.
                terrainProfilePanel.updatePath(currentTrack.getPositions());
                cloudCeilingPanel.setTrack(currentTrack);
                // If track is being extended goto track end
                if (getTrackController().isExtending() && trackViewPanel.isFreeViewMode())
                    gotoTrackEnd();
            }
            else if (propertyChangeEvent.getPropertyName() == Keys.ELEVATION_MODEL
                && trackViewPanel.isExamineViewMode() && !wwd.view().isAnimating()) {
                // When the elevation model changes, and the view is examining the terrain beneath the track
                // (but has not active state iterators), update the view parameters immediately.
                updateView(false);
            }
            else if (propertyChangeEvent.getPropertyName() == SARKey.ELEVATION_UNIT) {
                updateElevationUnit(propertyChangeEvent.getNewValue());
            }
            else if (propertyChangeEvent.getPropertyName() == SARKey.ANGLE_FORMAT) {
                updateAngleFormat(propertyChangeEvent.getNewValue());
            }
            else if (propertyChangeEvent.getPropertyName() == TerrainProfilePanel.TERRAIN_PROFILE_OPEN) {
                terrainProfileFrame.setVisible(true);
            }
            else if (propertyChangeEvent.getPropertyName() == TerrainProfilePanel.TERRAIN_PROFILE_CHANGE) {
                wwd.redraw();
            }
            else if (propertyChangeEvent.getPropertyName() == CloudCeilingPanel.CLOUD_CEILING_OPEN) {
                cloudCeilingFrame.setVisible(true);
            }
            else if (propertyChangeEvent.getPropertyName() == CloudCeilingPanel.CLOUD_CEILING_CHANGE) {
                wwd.redraw();
            }
            else if (propertyChangeEvent.getPropertyName() == TrackViewPanel.VIEW_MODE_CHANGE) {
                trackViewPanel.setViewMode((String) propertyChangeEvent.getNewValue());
            }
            else if (propertyChangeEvent.getPropertyName() == TrackViewPanel.SHOW_TRACK_INFORMATION) {
                trackInfoState = (String) propertyChangeEvent.getNewValue();
                updateShowTrackInformation();
            }

            if ((propertyChangeEvent.getPropertyName() == Keys.VIEW
                || propertyChangeEvent.getPropertyName() == Keys.VIEW_QUIET)
                && trackViewPanel.isFollowViewMode()) {
                doUpdateCrosshair();
            }
        }
    };

    public AnalysisPanel() {
        this.initComponents();
        this.layoutComponents();

        // Init plane/segment info layer.
        this.planeModel = new PlaneModel(100.0d, 100.0d, Color.YELLOW);
        this.planeModel.setShadowScale(0.1);
        this.planeModel.setShadowColor(new Color(255, 255, 0, 192));
        this.segmentInfo = new TrackSegmentInfo();
        this.trackRenderables = new RenderableLayer();
        this.trackRenderables.add(this.planeModel);
        this.trackRenderables.add(this.segmentInfo);
        // Init crosshair layer
        this.crosshairLayer = new CrosshairLayer("images/64x64-crosshair.png");
        this.crosshairLayer.setOpacity(0.4);
        this.crosshairLayer.setEnabled(false);
        // Init terrain profile panel and frame
        this.terrainProfilePanel = new TerrainProfilePanel();
        this.terrainProfileFrame = new JFrame("Terrain Profile");
        this.terrainProfileFrame.setResizable(false);
        this.terrainProfileFrame.setAlwaysOnTop(true);
        this.terrainProfileFrame.add(this.terrainProfilePanel);
        this.terrainProfileFrame.pack();
        SAR2.centerWindowInDesktop(this.terrainProfileFrame);
        // Init cloud ceiling panel and frame
        this.cloudCeilingPanel = new CloudCeilingPanel();
        this.cloudCeilingFrame = new JFrame("Cloud Contour");
        this.cloudCeilingFrame.setResizable(false);
        this.cloudCeilingFrame.setAlwaysOnTop(true);
        this.cloudCeilingFrame.add(this.cloudCeilingPanel);
        this.cloudCeilingFrame.pack();
        SAR2.centerWindowInDesktop(this.cloudCeilingFrame);
        // Listen to track view panel and cloud ceiling panel
        this.trackViewPanel.addPropertyChangeListener(this.propertyChangeListener);
        this.cloudCeilingPanel.addPropertyChangeListener(this.propertyChangeListener);

        this.updateShowTrackInformation();
    }

    public WorldWindow getWwd() {
        return this.wwd;
    }

    public void setWwd(WorldWindow wwd) {
        if (this.wwd != null) {
            this.wwd.removePropertyChangeListener(this.propertyChangeListener);
            this.wwd.view().removePropertyChangeListener(this.propertyChangeListener);
            this.wwd.removeRenderingListener(this.renderingListener);
        }
        this.wwd = wwd;
        this.terrainProfilePanel.setWwd(wwd);
        if (this.wwd != null) {
            this.wwd.addPropertyChangeListener(this.propertyChangeListener);
            this.wwd.view().addPropertyChangeListener(this.propertyChangeListener);
            this.wwd.addRenderingListener(this.renderingListener);
            WorldWindow.insertBeforeCompass(wwd, this.trackRenderables);
            WorldWindow.insertBeforeCompass(wwd, this.crosshairLayer);
            // Init cloud ceiling
            this.cloudCeilingPanel.setCloudCeiling(new CloudCeiling(this.wwd));
        }
    }

    public TrackController getTrackController() {
        return this.trackController;
    }

    public void setTrackController(TrackController trackController) {
        this.trackController = trackController;
    }

    public String getViewMode() {
        return this.trackViewPanel.getViewMode();
    }

    public void setCurrentTrack(SARTrack currentTrack) {
        if (this.currentTrack != null) {
            this.currentTrack.removePropertyChangeListener(this.propertyChangeListener);
            this.currentTrack.set(ANALYSIS_PANEL_STATE, this.getRestorableState());
        }

        this.currentTrack = currentTrack;
        this.segmentInfo.setTrack(currentTrack);
        this.trackViewPanel.setCurrentTrack(currentTrack);

        if (this.currentTrack != null) {
            this.currentTrack.addPropertyChangeListener(this.propertyChangeListener);
            this.terrainProfilePanel.updatePath(currentTrack.getPositions());
            this.cloudCeilingPanel.setTrack(this.currentTrack);
            String stateInXML = (String) this.currentTrack.get(ANALYSIS_PANEL_STATE);
            if (stateInXML != null)
                this.restoreState(stateInXML);
        }
    }

    private void updateElevationUnit(Object newValue) {
        if (newValue != null) {
            this.segmentInfo.setElevationUnit(newValue);
            this.trackViewPanel.setElevationUnit(newValue.toString());
            this.trackViewPanel.updateReadout(this.getPositionAlongSegment());
            this.cloudCeilingPanel.setElevationUnit(newValue.toString());
        }
    }

    private void updateAngleFormat(Object newValue) {
        if (newValue != null) {
            this.segmentInfo.setAngleFormat(newValue);
            this.trackViewPanel.setAngleFormat(newValue.toString());
            this.trackViewPanel.updateReadout(this.getPositionAlongSegment());
        }
    }

    private static Angle getControlHeading() {
        return Angle.ZERO;
    }

    @SuppressWarnings("UnusedDeclaration")
    private static Angle getControlPitch() {
        return new Angle(80);
    }

    private static Angle getControlFOV() {
        return new Angle(45);
    }

    private void updateView(boolean goSmoothly) {
        BasicOrbitView view = (BasicOrbitView) this.wwd.view();
        view.setFieldOfView(AnalysisPanel.getControlFOV());

        Position pos = this.getPositionAlongSegment();
        if (TrackViewPanel.VIEW_MODE_EXAMINE.equals(this.lastUpdateViewMode)
            && !this.trackViewPanel.getViewMode().equals(this.lastUpdateViewMode)) {
            // Save examine mode view orientation when moving out of examine mode
            this.saveExamineViewState();
        }
        this.lastUpdateViewMode = this.trackViewPanel.getViewMode();

        if (pos != null) {
            Angle heading = this.getHeading().add(AnalysisPanel.getControlHeading());

            this.terrainProfilePanel.updatePosition(pos, heading);
            this.planeModel.setPosition(pos);
            this.planeModel.setHeading(heading);

            if (this.trackViewPanel.isExamineViewMode()) {
                this.crosshairLayer.setEnabled(false);  // Turn off crosshair
                this.terrainProfilePanel.setFollowObject();
                // Set the view center point to the current track position on the ground - spheroid.
                // This gets the eye looking at the cross section.
                Position groundPos = getSmoothedGroundPositionAlongSegment();
                if (groundPos == null)
                    groundPos = getGroundPositionAlongSegment();

                if (goSmoothly) {
                    Angle initialPitch = new Angle(Math.min(60, view.getPitch().degrees));
                    Angle initialHeading = view.getHeading();
                    double initialZoom = 10000;
                    if (this.examineViewState != null) {
                        // Use previously saved view orientation values if available
                        initialPitch = this.examineViewState.pitch;
                        initialHeading = this.examineViewState.relativeHeading.add(this.getHeading());
                        initialZoom = this.examineViewState.zoom;
                    }
                    // If the player is active, set initial parameters immediately.
                    // Otherwise, set initial parameters gradually.
                    if (this.trackViewPanel.isPlayerActive()) {
                        view.setCenterPosition(groundPos);
                        view.setZoom(initialZoom);
                        view.setPitch(initialPitch);
                        view.setHeading(initialHeading);
                    }
                    else {
                        // Stop all animations on the view, and start a 'pan to' animation.
                        view.stopAnimations();
                        view.addPanToAnimator(
                            groundPos, initialHeading, initialPitch, initialZoom, true);
                    }
                }
                else {
                    // Send a message to stop all changes to the view's center position.
                    view.stopMovementOnCenter();
                    // Set the view to center on the track position,
                    // while keeping the eye altitude constant.
                    try {
                        Position eyePos = view.getCurrentEyePosition();
                        // New eye lat/lon will follow the ground position.
                        LatLon newEyeLatLon = eyePos.add(groundPos.subtract(view.getCenterPosition()));
                        // Eye elevation will not change unless it is below the ground position elevation.
                        double newEyeElev = Math.max(eyePos.getElevation(), groundPos.getElevation());

                        Position newEyePos = new Position(newEyeLatLon, newEyeElev);
                        view.setOrientation(newEyePos, groundPos);
                    }
                    // Fallback to setting center position.
                    catch (Exception e) {
                        view.setCenterPosition(groundPos);
                        // View/OrbitView will have logged the exception, no need to log it here.
                    }
                }
            }
            else if (this.trackViewPanel.isFollowViewMode()) {
                Angle pitch = Angle.POS90;
                double zoom = 0;
                heading = getSmoothedHeading(0.1); // smooth heading on first and last 10% of segment

                this.terrainProfilePanel.setFollowObject();

                // Place the eye at the track current lat-lon and altitude, with the proper heading
                // and pitch from slider. Intended to simulate the view from the plane.
                if (goSmoothly) {
                    // If the player is active, set initial parameters immediately.
                    // Otherwise, set initial parameters gradually.
                    if (this.trackViewPanel.isPlayerActive()) {
                        view.setCenterPosition(pos);
                        view.setHeading(heading);
                        view.setPitch(pitch);
                        view.setZoom(zoom);
                    }
                    else {
                        // Stop all animations on the view, and start a 'pan to' animation.
                        view.stopAnimations();
                        view.addPanToAnimator(pos, heading, pitch, zoom);
                    }
                }
                else {
                    // Stop all view animations, and send a message to stop all changes to the view.
                    view.stopAnimations();
                    view.stopMovement();
                    // Set the view values to follow the track.
                    view.setCenterPosition(pos);
                    view.setHeading(heading);
                    view.setPitch(pitch);
                    view.setZoom(zoom);
                }
                // Update crosshair position
                this.updateCrosshair();
            }
            else if (this.trackViewPanel.isFreeViewMode()) {
                this.crosshairLayer.setEnabled(false);  // Turn off crosshair
                if (goSmoothly) {
                    // Stop any state iterators, and any view movement.
                    view.stopAnimations();
                    view.stopMovement();
                    // Flag the OrbitView as 'out of focus'. This ensures that the center position will be updated
                    // just before the users next interaction with the view.
                    view.setViewOutOfFocus(true);
                }
            }
        }

        this.updateShowTrackInformation();
        this.segmentInfo.setSegmentIndex(this.getCurrentPositionNumber());
        this.segmentInfo.setSegmentPosition(pos);
        this.trackViewPanel.updateReadout(pos);
        this.wwd.redraw();
    }

    private void saveExamineViewState() {
        this.examineViewState = new ViewState((OrbitView) this.wwd.view(), this.getHeading(),
            this.getPositionAlongSegment());
    }

    private int getCurrentPositionNumber() {
        return this.trackViewPanel.getCurrentPositionNumber();
    }

    private boolean isLastPosition(int n) {
        return n >= this.currentTrack.size() - 1;
    }

    public void gotoTrackEnd() {
        this.trackViewPanel.gotoTrackEnd();
    }

    public Position getCurrentSegmentStartPosition() {
        int n = this.getCurrentPositionNumber();
        return getSegmentStartPosition(n);
    }

    public Position getSegmentStartPosition(int startPositionNumber) {
        if (this.currentTrack == null || this.currentTrack.isEmpty())
            return null;

        Position pos;
        if (isLastPosition(startPositionNumber))
            pos = this.currentTrack.get(this.currentTrack.size() - 1);
        else
            pos = this.currentTrack.get(startPositionNumber);

        return new Position(pos.getLat(), pos.getLon(), pos.getElevation() + this.currentTrack.getOffset());
    }

    public Position getCurrentSegmentEndPosition() {
        int n = this.getCurrentPositionNumber();
        return getSegmentEndPosition(n);
    }

    public Position getSegmentEndPosition(int startPositionNumber) {
        if (this.currentTrack == null || this.currentTrack.isEmpty())
            return null;

        Position pos;
        if (isLastPosition(startPositionNumber + 1))
            pos = this.currentTrack.get(this.currentTrack.size() - 1);
        else
            pos = this.currentTrack.get(startPositionNumber + 1);

        return new Position(pos.getLat(), pos.getLon(), pos.getElevation() + this.currentTrack.getOffset());
    }

    public double getSegmentLength(int startPositionNumber) {
        Vec4 start = wwd.model().globe().computePointFromPosition(getSegmentStartPosition(startPositionNumber));
        Vec4 end = wwd.model().globe().computePointFromPosition(getSegmentEndPosition(startPositionNumber));
        return start.distanceTo3(end);
    }

    public Position getPositionAlongSegment() {
        double t = this.trackViewPanel.getPositionDelta();
        return this.getPositionAlongSegment(t);
    }

    private Position getPositionAlongSegment(double t) {
        Position pa = this.getCurrentSegmentStartPosition();
        if (pa == null)
            return null;
        Position pb = this.getCurrentSegmentEndPosition();
        if (pb == null)
            return pa;

        return interpolateTrackPosition(t, pa, pb);
    }

    public Angle getHeading() {
        return getHeading(this.getCurrentPositionNumber());
    }

    public Angle getHeading(int cpn) {
        Position pA;
        Position pB;

        if ((cpn < 1) && (this.isLastPosition(cpn))) //handle first position
            return Angle.ZERO;
        else if (!this.isLastPosition(cpn)) {
            pA = this.currentTrack.get(cpn);
            pB = this.currentTrack.get(cpn + 1);
        }
        else {
            pA = this.currentTrack.get(cpn - 1);
            pB = this.currentTrack.get(cpn);
        }

        return LatLon.greatCircleAzimuth(pA, pB);
    }

    // Interpolate heading at track jonctions
    private Angle getSmoothedHeading(double dt) {
        int cpn = this.getCurrentPositionNumber();
        Angle heading1, heading2;
        double t1, t;
        // Current segment
        heading1 = getHeading(cpn);
        t1 = this.trackViewPanel.getPositionDelta();
        if (t1 <= dt && cpn > 0) {
            heading2 = getHeading(cpn - 1);
            t = (1 - t1 / dt) * 0.5;
        }
        else if (t1 >= (1 - dt) && !this.isLastPosition(cpn)) {
            heading2 = getHeading(cpn + 1);
            t = (1 - (1 - t1) / dt) * 0.5;
        }
        else
            return heading1;

        return Angle.mix(t, heading1, heading2);
    }

    @SuppressWarnings("UnusedDeclaration")
    private Position getGroundPositionAlongSegment() {
        if (this.wwd == null)
            return null;

        Position pos = getPositionAlongSegment();
        if (pos == null)
            return null;

        return getGroundPosition(pos);
    }

    private Position getGroundPosition(LatLon location) {
        double elevation = this.wwd.model().globe().elevation(location.getLat(), location.getLon());
        return new Position(location, elevation);
    }

    // TODO: weighted average should be over actual polyline track points
    private Position getSmoothedGroundPositionAlongSegment() {
        if (this.currentTrack == null || this.currentTrack.isEmpty())
            return null;

        Position start = getCurrentSegmentStartPosition();
        Position end = getCurrentSegmentEndPosition();
        if (start == null || end == null)
            return null;

        Globe globe = this.wwd.model().globe();
        if (globe == null)
            return null;

        int n = this.getCurrentPositionNumber();
        double t = this.trackViewPanel.getPositionDelta();
        // Limit t to 0 if this is the last position.
        if (isLastPosition(n))
            t = 0;

        double tstep = 1 / 100.0;
        int numWeights = 15; // TODO: extract to configurable property

        double elev = 0;
        double sumOfWeights = 0;
        // Compute the moving weighted average of track positions on both sides of the current track position.
        for (int i = 0; i < numWeights; i++) {
            double tt;
            Position pos;

            // Previous ground positions.
            tt = t - i * tstep;
            pos = null;
            if (tt >= 0) // Position is in the current track segment.
                pos = interpolateTrackPosition(tt, start, end);
            else if (tt < 0 && n > 0) // Position is in the previous track segment.
                pos = interpolateTrackPosition(tt + 1, this.currentTrack.get(n - 1), start);
            if (pos != null) {
                double e = globe.elevation(pos.getLat(), pos.getLon());
                elev += (numWeights - i) * e;
                sumOfWeights += (numWeights - i);
            }

            // Next ground positions.
            // We don't want to count the first position twice.
            if (i != 0) {
                tt = t + i * tstep;
                pos = null;
                if (tt <= 1) // Position is in the current track segment.
                    pos = interpolateTrackPosition(tt, start, end);
                else if (tt > 1 && !isLastPosition(n + 1)) // Position is in the next track segment.
                    pos = interpolateTrackPosition(tt - 1, end, this.currentTrack.get(n + 2));
                if (pos != null) {
                    double e = globe.elevation(pos.getLat(), pos.getLon());
                    elev += (numWeights - i) * e;
                    sumOfWeights += (numWeights - i);
                }
            }
        }
        elev /= sumOfWeights;

        Position actualPos = interpolateTrackPosition(t, start, end);
        return new Position(actualPos, elev);
    }

    /**
     * SAR tracks points are connected with lines of constant heading (rhumb lines). In order to compute an interpolated
     * position between two track points, we must use rhumb computations, rather than linearly interpolate the
     * position.
     *
     * @param t     a decimal number between 0 and 1
     * @param begin first position
     * @param end   second position
     * @return Position in between begin and end
     */
    private static Position interpolateTrackPosition(double t, Position begin, Position end) {
        if (begin == null || end == null)
            return null;

        // The track is drawn as a rhumb line, therefore we use rhumb computations to interpolate between the track's
        // geographic positions.
        return Position.interpolateRhumb(begin, end, t);
    }

    private void updateCrosshair() {
        this.crosshairNeedsUpdate = true;
        this.wwd.redraw();
    }

//    /**
//     * Compute crosshair location in viewport for 'follow' - 'fly-it' mode.
//     * <p>
//     * It computes the intersection of the air track with the near clipping plane
//     * and determines the corresponding crosshair position in the viewport.</p>
//     * <p>
//     * This assumes the view is headed in the same direction as the air track,
//     * and the eye is set to look at the aircraft from a distance and angle.</p>
//     *
//     * @param view the current <code>View</code>
//     * @param a view pitch angle relative to the air track (0 degree = horizontal)
//     * @param distance eye distance from the aircraft
//     * @return the crosshair center position in the viewport.
//     */
//    private Vec4 computeCrosshairPosition(View view, Angle a, double distance)
//    {
//        double hfovH = view.getFieldOfView().radians / 2; // half horizontal fov in radians
//        double hw = view.getViewport().width / 2;         // half viewport width
//        double hh = view.getViewport().height / 2;        // half viewport height
//        double d = hw / Math.tan(hfovH);                  // distance to viewport plane in pixels
//        // distance to near plane in meters
//        double dNearMeter = Math.abs(view.getFrustum().getNear().getDistance());
//        // crosshair elevation above viewport center in meter
//        double dyMeter = (dNearMeter - distance) * Math.sin(a.radians);
//        // corresponding vertical fov half angle
//        double ay = Math.atan(dyMeter / dNearMeter);
//        // corresponding viewport crosshair elevation in pixels
//        double dy = Math.tan(ay) * d;
//        // final crosshair viewport position
//        return new Vec4(hw, hh + dy, 0, 0);
//    }

    // Mark crosshair as needing update after next render pass

    // Update crosshair position to follow the air track
    private void doUpdateCrosshair() {
        Vec4 crosshairPos = computeCrosshairPosition();
        if (crosshairPos != null) {
            this.crosshairLayer.setEnabled(true);
            this.crosshairLayer.setLocationCenter(crosshairPos);
        }
        else {
            this.crosshairLayer.setEnabled(false);
        }
        this.crosshairNeedsUpdate = false;
    }

    private void updateShowTrackInformation() {
        this.segmentInfo.setEnabled(
            this.trackInfoState != null && this.trackInfoState.equals(TrackViewPanel.CURRENT_SEGMENT)
                && !this.trackViewPanel.isFollowViewMode());
    }

    // Compute cartesian intersection between the current air track segment and the near plane.
    // Follow rhumb line segments.
    private Vec4 computeCrosshairPosition() {
        Position posA = getCurrentSegmentStartPosition();
        Position posB = getCurrentSegmentEndPosition();
        Angle segmentAzimuth = LatLon.rhumbAzimuth(posA, posB);
        Angle segmentDistance = LatLon.rhumbDistance(posA, posB);
        int numSubsegments = 10;  // TODO: get from track polyline
        double step = 1.0d / numSubsegments;
        // Extend the track segment ends by one subsegment to make sure it will intersect the near plane
        double deltaElevation = posB.getElevation() - posA.getElevation();
        LatLon latLon = LatLon.rhumbEndPosition(posA, segmentAzimuth.addRadians(Math.PI),
            Angle.fromRadians(segmentDistance.radians() / numSubsegments));
        posA = new Position(latLon, posA.getElevation() - deltaElevation / numSubsegments);
        latLon = LatLon.rhumbEndPosition(posB, segmentAzimuth,
            Angle.fromRadians(segmentDistance.radians() / numSubsegments));
        posB = new Position(latLon, posB.getElevation() + deltaElevation / numSubsegments);
        segmentDistance = LatLon.rhumbDistance(posA, posB);
        // Iterate through segments to find intersection
        Globe globe = this.wwd.model().globe();
        Plane near = this.wwd.view().getFrustumInModelCoordinates().near;
        Position p1 = null, p2;
        for (double s = 0; s <= 1; s += step) {
            if (s == 0)
                p2 = posA;
            else if (s >= 1)
                p2 = posB;
            else {
                Angle distance = Angle.fromRadians(s * segmentDistance.radians());
                latLon = LatLon.rhumbEndPosition(posA, segmentAzimuth, distance);
                p2 = new Position(latLon, (1 - s) * posA.getElevation() + s * posB.getElevation());
            }
            if (p1 != null) {
                Vec4 pa = globe.computePointFromPosition(p1);
                Vec4 pb = globe.computePointFromPosition(p2);
                if (pa.distanceTo3(pb) > 0) {
                    Vec4 intersection = near.intersect(pa, pb);
                    if (intersection != null) {
                        return this.wwd.view().project(intersection);
                    }
                }
            }
            p1 = p2;
        }
        return null;
    }

    protected void initComponents() {
        this.trackViewPanel = new TrackViewPanel(this);
    }

    protected void layoutComponents() {
        this.setLayout(new BorderLayout(0, 0)); // hgap vgap

        // Put the panel in the north region to keep its components from expanding vertically.
        this.add(this.trackViewPanel, BorderLayout.NORTH);
    }

    public String getRestorableState() {
        RestorableSupport rs = RestorableSupport.newRestorableSupport();
        this.doGetRestorableState(rs, null);

        return rs.getStateAsXml();
    }

    // *** Restorable interface ***

    public void restoreState(String stateInXml) {
        if (stateInXml == null) {
            String message = Logging.getMessage("nullValue.StringIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        RestorableSupport rs;
        try {
            rs = RestorableSupport.parse(stateInXml);
        }
        catch (Exception e) {
            // Parsing the document specified by stateInXml failed.
            String message = Logging.getMessage("generic.ExceptionAttemptingToParseStateXml", stateInXml);
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message, e);
        }

        this.doRestoreState(rs, null);
    }

    protected void doGetRestorableState(RestorableSupport rs, RestorableSupport.StateObject context) {
        // Save examine view mode state if view is currently in examine mode.
        if (TrackViewPanel.VIEW_MODE_EXAMINE.equals(this.trackViewPanel.getViewMode()))
            this.saveExamineViewState();
        // Add state values
        if (this.examineViewState != null) {
            rs.addStateValueAsDouble(context, "examinePitch", this.examineViewState.pitch.degrees);
            rs.addStateValueAsDouble(context, "examineRelativeHeading",
                this.examineViewState.relativeHeading.degrees);
            rs.addStateValueAsDouble(context, "examineZoom", this.examineViewState.zoom);
            rs.addStateValueAsLatLon(context, "examineCenter", this.examineViewState.relativeCenterLocation);
        }
        if (this.trackViewPanel != null)
            this.trackViewPanel.doGetRestorableState(rs, rs.addStateObject(context, "trackView"));

        if (this.terrainProfilePanel != null)
            this.terrainProfilePanel.doGetRestorableState(rs, rs.addStateObject(context, "terrainProfile"));

        if (this.cloudCeilingPanel != null)
            this.cloudCeilingPanel.doGetRestorableState(rs, rs.addStateObject(context, "cloudCeiling"));
    }

    protected void doRestoreState(RestorableSupport rs, RestorableSupport.StateObject context) {
        // Retrieve state values
        Double examinePitchState = rs.getStateValueAsDouble(context, "examinePitch");
        Double examineRelativeHeadingState = rs.getStateValueAsDouble(context, "examineRelativeHeading");
        Double examineZoomState = rs.getStateValueAsDouble(context, "examineZoom");
        LatLon examineCenterState = rs.getStateValueAsLatLon(context, "examineCenter");
        if (examinePitchState != null && examineRelativeHeadingState != null && examineZoomState != null
            && examineCenterState != null) {
            this.examineViewState = new ViewState(new Angle(examineRelativeHeadingState),
                new Angle(examinePitchState), examineZoomState, examineCenterState);
            // this prevents the restored examine view state from being overwritten at next view update.
            this.lastUpdateViewMode = null;
        }

        RestorableSupport.StateObject trackViewState = rs.getStateObject(context, "trackView");
        if (trackViewState != null && this.trackViewPanel != null)
            this.trackViewPanel.doRestoreState(rs, trackViewState);

        RestorableSupport.StateObject terrainProfileState = rs.getStateObject(context, "terrainProfile");
        if (terrainProfileState != null && this.terrainProfilePanel != null)
            this.terrainProfilePanel.doRestoreState(rs, terrainProfileState);

        if (this.cloudCeilingPanel != null && this.currentTrack != null)
            this.cloudCeilingPanel.setTrackCurrentPositionNumber(this.getCurrentPositionNumber());

        RestorableSupport.StateObject cloudCeilingState = rs.getStateObject(context, "cloudCeiling");
        if (cloudCeilingState != null && this.cloudCeilingPanel != null)
            this.cloudCeilingPanel.doRestoreState(rs, cloudCeilingState);
    }

    private static class ViewState {
        public final Angle pitch;
        public final Angle relativeHeading;
        public final double zoom;
        public final LatLon relativeCenterLocation;

        public ViewState(OrbitView view, Angle referenceHeading, Position referencePosition) {
            this.pitch = view.getPitch();
            this.relativeHeading = view.getHeading().sub(referenceHeading);
            this.zoom = view.getZoom();
            this.relativeCenterLocation = view.getCenterPosition().subtract(referencePosition);
        }

        public ViewState(Angle relativeHeading, Angle pitch, double zoom, LatLon relativeCenterPosition) {
            this.pitch = pitch;
            this.relativeHeading = relativeHeading;
            this.zoom = zoom;
            this.relativeCenterLocation = relativeCenterPosition;
        }
    }
}