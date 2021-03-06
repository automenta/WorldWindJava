/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.examples;

import gov.nasa.worldwind.*;
import gov.nasa.worldwind.animation.BasicAnimator;
import gov.nasa.worldwind.avlist.KV;
import gov.nasa.worldwind.event.*;
import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.globes.Globe;
import gov.nasa.worldwind.layers.*;
import gov.nasa.worldwind.render.*;
import gov.nasa.worldwind.render.airspaces.*;
import gov.nasa.worldwind.util.*;
import gov.nasa.worldwind.view.orbit.OrbitView;

import javax.swing.Box;
import javax.swing.Timer;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;
import java.util.*;

/**
 * KeepingObjectsInView demonstrates keeping a set of scene elements visible by using the utility class {@link
 * ExtentVisibilitySupport}. To run this demonstration, execute this class' main
 * method, then follow the on-screen instructions.
 * <p>
 * The key functionality demonstrated by KeepingObjectsVisible is found in the internal classes {@link
 * KeepingObjectsInView.ViewController} and {@link KeepingObjectsInView.ViewAnimator}.
 *
 * @author dcollins
 * @version $Id: KeepingObjectsInView.java 2109 2014-06-30 16:52:38Z tgaskins $
 */
public class KeepingObjectsInView extends ApplicationTemplate {

    public static Iterable<?> createObjectsToTrack() {
        List<Object> objects = new ArrayList<>();
        Sector sector = Sector.fromDegrees(35, 45, -110, -100);

        for (int i = 0; i < 3; i++) {
            LatLon randLocation1, randLocation2;

            // Add a UserFacingIcon.
            randLocation1 = randomLocation(sector);
            WWIcon icon = new UserFacingIcon("gov/nasa/worldwind/examples/images/antenna.png",
                new Position(randLocation1, 0));
            icon.setSize(new Dimension(64, 64));
            icon.set(Keys.FEEDBACK_ENABLED, Boolean.TRUE);
            objects.add(icon);

            // Add a SphereAirspace.
            randLocation1 = randomLocation(sector);
            Airspace airspace = new SphereAirspace(randLocation1, 50000.0d);
            airspace.setAltitude(0.0d);
            airspace.setTerrainConforming(true);
            airspace.setAttributes(new BasicAirspaceAttributes(Material.GREEN, 1.0d));
            objects.add(airspace);

            // Add a Path.
            randLocation1 = randomLocation(sector);
            randLocation2 = randomLocation(sector);
            Path path = new Path(Arrays.asList(randLocation1, randLocation2), 0.0d);
            path.setSurfacePath(true);
            var attrs = new BasicShapeAttributes();
            attrs.setOutlineWidth(3);
            attrs.setOutlineMaterial(new Material(Color.RED));
            path.setAttributes(attrs);
            objects.add(path);

            // Add a SurfaceCircle.
            randLocation1 = randomLocation(sector);
            attrs = new BasicShapeAttributes();
            attrs.setInteriorMaterial(Material.BLUE);
            attrs.setOutlineMaterial(new Material(WWUtil.makeColorBrighter(Color.BLUE)));
            attrs.setInteriorOpacity(0.5);
            SurfaceCircle circle = new SurfaceCircle(attrs, randLocation1, 50000.0d);
            objects.add(circle);
        }

        return objects;
    }

    protected static LatLon randomLocation(Sector sector) {
        return new LatLon(
            Angle.mix(Math.random(), sector.latMin(), sector.latMax()),
            Angle.mix(Math.random(), sector.lonMin(), sector.lonMax()));
    }

    public static Annotation createHelpAnnotation(WorldWindow wwd) {
        String text = "The view tracks the antenna icons,"
            + " the <font color=\"#DD0000\">red</font> lines,"
            + " the <font color=\"#00DD00\">green</font> spheres,"
            + " and the <font color=\"#0000DD\">blue</font> circles."
            + " Drag any object out of the window to track it.";
        Rectangle viewport = ((Component) wwd).getBounds();
        Point screenPoint = new Point(viewport.width / 2, viewport.height / 3);

        AnnotationAttributes attr = new AnnotationAttributes();
        attr.setAdjustWidthToText(Keys.SIZE_FIT_TEXT);
        attr.setFont(Font.decode("Arial-Bold-16"));
        attr.setTextAlign(Keys.CENTER);
        attr.setTextColor(Color.WHITE);
        attr.setEffect(Keys.TEXT_EFFECT_OUTLINE);
        attr.setBackgroundColor(new Color(0, 0, 0, 127)); // 50% transparent black
        attr.setBorderColor(Color.LIGHT_GRAY);
        attr.setLeader(Keys.SHAPE_NONE);
        attr.setCornerRadius(0);
        attr.setSize(new Dimension(350, 0));

        return new ScreenAnnotation(text, screenPoint, attr);
    }

    public static void main(String[] args) {
        ApplicationTemplate.start("Keeping Objects In View", AppFrame.class);
    }

    public static class AppFrame extends ApplicationTemplate.AppFrame {

        protected final Iterable<?> objectsToTrack;
        protected final ViewController viewController;
        protected RenderableLayer helpLayer;

        public AppFrame() {
            // Create an iterable of the objects we want to keep in view.
            this.objectsToTrack = createObjectsToTrack();
            // Set up a view controller to keep the objects in view.
            this.viewController = new ViewController(this.wwd());
            this.viewController.setObjectsToTrack(this.objectsToTrack);
            // Set up a layer to render the objects we're tracking.
            this.addObjectsToWorldWindow(this.objectsToTrack);
            // Set up swing components to toggle the view controller's behavior.
            this.initSwingComponents();

            // Set up a one-shot timer to zoom to the objects once the app launches.
            Timer timer = new Timer(1000, (ActionEvent e) -> {
                enableHelpAnnotation();
                viewController.gotoScene();
            });
            timer.setRepeats(false);
            timer.start();
        }

        protected void enableHelpAnnotation() {
            if (this.helpLayer != null) {
                return;
            }

            this.helpLayer = new RenderableLayer();
            this.helpLayer.add(createHelpAnnotation(wwd()));
            WorldWindow.insertBeforePlacenames(this.wwd(), this.helpLayer);
        }

        protected void disableHelpAnnotation() {
            if (this.helpLayer == null) {
                return;
            }

            this.wwd().model().layers().remove(this.helpLayer);
            this.helpLayer.clear();
            this.helpLayer = null;
        }

        protected void addObjectsToWorldWindow(Iterable<?> objectsToTrack) {
            // Set up a layer to render the icons. Disable WWIcon view clipping, since view tracking works best when an
            // icon's screen rectangle is known even when the icon is outside the view frustum.
            IconLayer iconLayer = new IconLayer();
            iconLayer.setViewClippingEnabled(false);
            iconLayer.setName("Icons To Track");
            WorldWindow.insertBeforePlacenames(this.wwd(), iconLayer);

            // Set up a layer to render the markers.
            RenderableLayer shapesLayer = new RenderableLayer();
            shapesLayer.setName("Shapes to Track");
            WorldWindow.insertBeforePlacenames(this.wwd(), shapesLayer);

            // Add the objects to track to the layers.
            for (Object o : objectsToTrack) {
                if (o instanceof WWIcon) {
                    iconLayer.addIcon((WWIcon) o);
                }
                else if (o instanceof Renderable) {
                    shapesLayer.add((Renderable) o);
                }
            }

            // Set up a SelectListener to drag the spheres.
            this.wwd().addSelectListener(new SelectListener() {
                protected final SelectListener dragger = new BasicDragger(wwd());

                @Override
                public void accept(SelectEvent event) {
                    // Delegate dragging computations to a dragger.
                    this.dragger.accept(event);

                    if (event.getEventAction().equals(SelectEvent.DRAG)) {
                        disableHelpAnnotation();
                        viewController.sceneChanged();
                    }
                }
            });
        }

        protected void initSwingComponents() {
            // Create a checkbox to enable/disable the view controller.
            JCheckBox checkBox = new JCheckBox("Enable view tracking", true);
            checkBox.setAlignmentX(Component.LEFT_ALIGNMENT);
            checkBox.addActionListener((ActionEvent event) -> {
                boolean selected = ((AbstractButton) event.getSource()).isSelected();
                viewController.setEnabled(selected);
            });
            JButton button = new JButton("Go to objects");
            button.setAlignmentX(Component.LEFT_ALIGNMENT);
            button.addActionListener((ActionEvent event) -> viewController.gotoScene());
            Box box = Box.createVerticalBox();
            box.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30)); // top, left, bottom, right
            box.add(checkBox);
            box.add(Box.createVerticalStrut(5));
            box.add(button);

            this.getControlPanel().add(box, BorderLayout.SOUTH);
        }
    }

    //**************************************************************//
    //********************  View Controller  ***********************//
    //**************************************************************//
    public static class ViewController {

        protected static final double SMOOTHING_FACTOR = 0.96;
        protected final WorldWindow wwd;
        protected boolean enabled = true;
        protected ViewAnimator animator;
        protected Iterable<?> objectsToTrack;

        public ViewController(WorldWindow wwd) {
            this.wwd = wwd;
        }

        public boolean isEnabled() {
            return this.enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;

            if (this.animator != null) {
                this.animator.stop();
                this.animator = null;
            }
        }

        public Iterable<?> getObjectsToTrack() {
            return this.objectsToTrack;
        }

        public void setObjectsToTrack(Iterable<?> iterable) {
            this.objectsToTrack = iterable;
        }

        public boolean isSceneContained(View view) {
            ExtentVisibilitySupport vs = new ExtentVisibilitySupport();
            this.addExtents(vs);

            return vs.areExtentsContained(view);
        }

        public Vec4[] computeViewLookAtForScene(View view) {
            Globe globe = this.wwd.model().globe();
            double ve = this.wwd.sceneControl().getVerticalExaggeration();

            ExtentVisibilitySupport vs = new ExtentVisibilitySupport();
            this.addExtents(vs);

            return vs.computeViewLookAtContainingExtents(globe, ve, view);
        }

        public Position computePositionFromPoint(Vec4 point) {
            return this.wwd.model().globe().computePositionFromPoint(point);
        }

        public void gotoScene() {
            Vec4[] lookAtPoints = this.computeViewLookAtForScene(this.wwd.view());
            if (lookAtPoints == null || lookAtPoints.length != 3) {
                return;
            }

            Position centerPos = this.wwd.model().globe().computePositionFromPoint(lookAtPoints[1]);
            double zoom = lookAtPoints[0].distanceTo3(lookAtPoints[1]);

            this.wwd.view().stopAnimations();
            this.wwd.view().goTo(centerPos, zoom);
        }

        public void sceneChanged() {
            OrbitView view = (OrbitView) this.wwd.view();

            if (!this.isEnabled()) {
                return;
            }

            if (this.isSceneContained(view)) {
                return;
            }

            if (this.animator == null || !this.animator.hasNext()) {
                this.animator = new ViewAnimator(SMOOTHING_FACTOR, view, this);
                this.animator.start();
                view.stopAnimations();
                view.addAnimator(this.animator);
                view.emit(Keys.VIEW, null, view);
            }
        }

        protected void addExtents(ExtentVisibilitySupport vs) {
            // Compute screen extents for WWIcons which have feedback information from their IconRenderer.
            Iterable<?> iterable = this.getObjectsToTrack();
            if (iterable == null) {
                return;
            }

            List<ExtentHolder> extentHolders = new ArrayList<>();
            List<ExtentVisibilitySupport.ScreenExtent> screenExtents
                = new ArrayList<>();

            for (Object o : iterable) {
                if (o == null) {
                    continue;
                }

                if (o instanceof ExtentHolder) {
                    extentHolders.add((ExtentHolder) o);
                }
                else if (o instanceof KV) {
                    KV avl = (KV) o;

                    Object b = avl.get(Keys.FEEDBACK_ENABLED);
                    if (b == null || !Boolean.TRUE.equals(b)) {
                        continue;
                    }

                    if (avl.get(Keys.FEEDBACK_REFERENCE_POINT) != null) {
                        screenExtents.add(new ExtentVisibilitySupport.ScreenExtent(
                            (Vec4) avl.get(Keys.FEEDBACK_REFERENCE_POINT),
                            (Rectangle) avl.get(Keys.FEEDBACK_SCREEN_BOUNDS)));
                    }
                }
            }

            if (!extentHolders.isEmpty()) {
                Globe globe = this.wwd.model().globe();
                double ve = this.wwd.sceneControl().getVerticalExaggeration();
                vs.setExtents(ExtentVisibilitySupport.extentsFromExtentHolders(extentHolders, globe, ve));
            }

            if (!screenExtents.isEmpty()) {
                vs.setScreenExtents(screenExtents);
            }
        }
    }

    //**************************************************************//
    //********************  View Animator  *************************//
    //**************************************************************//
    public static class ViewAnimator extends BasicAnimator {

        protected static final double LOCATION_EPSILON = 1.0e-9;
        protected static final double ALTITUDE_EPSILON = 0.1;

        protected final OrbitView view;
        protected final ViewController viewController;
        protected boolean haveTargets;
        protected Position centerPosition;
        protected double zoom;

        public ViewAnimator(final double smoothing, OrbitView view, ViewController viewController) {
            super(() -> 1.0d - smoothing);

            this.view = view;
            this.viewController = viewController;
        }

        @Override
        public void stop() {
            super.stop();
            this.haveTargets = false;
        }

        @Override
        protected void setImpl(double interpolant) {
            this.updateTargetValues();

            if (!this.haveTargets) {
                this.stop();
                return;
            }

            if (this.valuesMeetCriteria(this.centerPosition, this.zoom)) {
                this.view.setCenterPosition(this.centerPosition);
                this.view.setZoom(this.zoom);
                this.stop();
            }
            else {
                Position newCenterPos = Position.interpolateGreatCircle(this.view.getCenterPosition(),
                    this.centerPosition, interpolant
                );
                double newZoom = WWMath.mix(interpolant, this.view.getZoom(), this.zoom);
                this.view.setCenterPosition(newCenterPos);
                this.view.setZoom(newZoom);
            }

            this.view.emit(Keys.VIEW, null, this);
        }

        protected void updateTargetValues() {
            if (this.viewController.isSceneContained(this.view)) {
                return;
            }

            Vec4[] lookAtPoints = this.viewController.computeViewLookAtForScene(this.view);
            if (lookAtPoints == null || lookAtPoints.length != 3) {
                return;
            }

            this.centerPosition = this.viewController.computePositionFromPoint(lookAtPoints[1]);
            this.zoom = lookAtPoints[0].distanceTo3(lookAtPoints[1]);
            if (this.zoom < view.getZoom()) {
                this.zoom = view.getZoom();
            }

            this.haveTargets = true;
        }

        protected boolean valuesMeetCriteria(Position centerPos, double zoom) {
            Angle cd = LatLon.greatCircleDistance(this.view.getCenterPosition(), centerPos);
            double ed = Math.abs(this.view.getCenterPosition().getElevation() - centerPos.getElevation());
            double zd = Math.abs(this.view.getZoom() - zoom);

            return cd.degrees < LOCATION_EPSILON
                && ed < ALTITUDE_EPSILON
                && zd < ALTITUDE_EPSILON;
        }
    }
}