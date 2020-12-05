/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */

package gov.nasa.worldwind.examples.worldwindow.features;

import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.examples.worldwindow.core.*;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.layers.tool.ViewControlsLayer;

import java.beans.PropertyChangeEvent;

/**
 * @author tag
 * @version $Id: Navigation.java 1171 2013-02-11 21:45:02Z dcollins $
 */
public class Navigation extends AbstractFeatureLayer {
    public static final String POSITION_PROPERTY
        = "gov.nasa.worldwindowx.applications.features.Navegacion.PostionProperty";
    public static final String ORIENTATION_PROPERTY
        = "gov.nasa.worldwindowx.applications.features.Navegacion.OrientationProperty";
    public static final String SIZE_PROPERTY = "gov.nasa.worldwindowx.applications.features.Navegacion.SizeProperty";
    public static final String OPACITY_PROPERTY
        = "gov.nasa.worldwindowx.applications.features.Navegacion.OpacityProperty";

    public static final String PAN_CONTROLS_PROPERTY
        = "gov.nasa.worldwindowx.applications.features.Navegacion.PanControlS";
    public static final String ZOOM_CONTROLS_PROPERTY
        = "gov.nasa.worldwindowx.applications.features.Navegacion.ZoomControlS";
    public static final String TILT_CONTROLS_PROPERTY
        = "gov.nasa.worldwindowx.applications.features.Navegacion.TiltControlS";
    public static final String HEADING_CONTROLS_PROPERTY
        = "gov.nasa.worldwindowx.applications.features.Navegacion.HeadingControlS";

    public Navigation() {
        this(null);
    }

    public Navigation(Registry registry) {
        super("Navigation", Constants.FEATURE_NAVIGATION,
            "gov/nasa/worldwind/examples/worldwindow/images/navegacion-64x64.png", true, registry);
    }

    public void initialize(Controller controller) {
        super.initialize(controller);
    }

    protected Layer doAddLayer() {
        ViewControlsLayer layer = new ViewControlsLayer();

        layer.set(Constants.SCREEN_LAYER, true);
        layer.set(Constants.INTERNAL_LAYER, true);
        layer.setLayout(AVKey.VERTICAL);

        controller.addInternalLayer(layer);

        ViewControlsLayer.ViewControlsSelectListener listener = new ViewControlsLayer.ViewControlsSelectListener(this.controller.getWWd(), layer);
        listener.setRepeatTimerDelay(30);
        listener.setZoomIncrement(0.5);
        listener.setPanIncrement(0.5);
        this.controller.getWWd().addSelectListener(listener);

        return layer;
    }

    private ViewControlsLayer getLayer() {
        return (ViewControlsLayer) this.layer;
    }

    @Override
    public void doPropertyChange(PropertyChangeEvent event) {
        switch (event.getPropertyName()) {
            case POSITION_PROPERTY:
                if (event.getNewValue() != null && event.getNewValue() instanceof String) {
                    this.getLayer().setPosition((String) event.getNewValue());
                    this.controller.redraw();
                }
                break;
            case ORIENTATION_PROPERTY:
                if (event.getNewValue() != null && event.getNewValue() instanceof String) {
                    this.getLayer().setLayout((String) event.getNewValue());
                    this.controller.redraw();
                }
                break;
            case PAN_CONTROLS_PROPERTY:
                if (event.getNewValue() != null && event.getNewValue() instanceof Boolean) {
                    this.getLayer().setShowPanControls((Boolean) event.getNewValue());
                    this.controller.redraw();
                }
                break;
            case ZOOM_CONTROLS_PROPERTY:
                if (event.getNewValue() != null && event.getNewValue() instanceof Boolean) {
                    this.getLayer().setShowZoomControls((Boolean) event.getNewValue());
                    this.controller.redraw();
                }
                break;
            case HEADING_CONTROLS_PROPERTY:
                if (event.getNewValue() != null && event.getNewValue() instanceof Boolean) {
                    this.getLayer().setShowHeadingControls((Boolean) event.getNewValue());
                    this.controller.redraw();
                }
                break;
            case TILT_CONTROLS_PROPERTY:
                if (event.getNewValue() != null && event.getNewValue() instanceof Boolean) {
                    this.getLayer().setShowPitchControls((Boolean) event.getNewValue());
                    this.controller.redraw();
                }
                break;
        }
    }

    public double getSize() {
        return this.layer.getScale();
    }

    public double getOpacity() {
        return this.layer.getOpacity();
    }

    public String getOrientation() {
        return this.getLayer().getLayout();
    }

    public String getPosition() {
        return this.getLayer().getPosition();
    }

    public boolean isShowPan() {
        return this.getLayer().isShowPanControls();
    }

    public boolean isShowZoom() {
        return this.getLayer().isShowZoomControls();
    }

    public boolean isShowTilt() {
        return this.getLayer().isShowPitchControls();
    }

    public boolean isShowHeading() {
        return this.getLayer().isShowHeadingControls();
    }
}
