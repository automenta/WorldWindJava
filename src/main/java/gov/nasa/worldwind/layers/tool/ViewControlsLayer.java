/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.layers.tool;

import gov.nasa.worldwind.*;
import gov.nasa.worldwind.avlist.*;
import gov.nasa.worldwind.event.*;
import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.globes.Globe;
import gov.nasa.worldwind.layers.*;
import gov.nasa.worldwind.render.*;
import gov.nasa.worldwind.util.Logging;
import gov.nasa.worldwind.view.orbit.OrbitView;

import javax.swing.*;
import java.awt.*;

/**
 * This layer displays onscreen view controls. Controls are available for pan, zoom, heading, pitch, tilt, field-of-view
 * and vertical exaggeration. Each of the controls can be enabled or disabled independently.
 * <p>
 * An instance of this class depends on an instance of {@link ViewControlsSelectListener} to control it. The select
 * listener must be registered as such via {@link gov.nasa.worldwind.WorldWindow#addSelectListener(gov.nasa.worldwind.event.SelectListener)}.
 * <p>
 * <code>ViewControlsLayer</code> instances are not sharable among <code>WorldWindow</code>s.
 *
 * @author Patrick Murris
 * @version $Id: ViewControlsLayer.java 1171 2013-02-11 21:45:02Z dcollins $
 * @see ViewControlsSelectListener
 */
public class ViewControlsLayer extends RenderableLayer {
    // The default images
    protected final static String IMAGE_PAN = "images/view-pan-64x64.png";
    protected final static String IMAGE_LOOK = "images/view-look-64x64.png";
    protected final static String IMAGE_HEADING_LEFT = "images/view-heading-left-32x32.png";
    protected final static String IMAGE_HEADING_RIGHT = "images/view-heading-right-32x32.png";
    protected final static String IMAGE_ZOOM_IN = "images/view-zoom-in-32x32.png";
    protected final static String IMAGE_ZOOM_OUT = "images/view-zoom-out-32x32.png";
    protected final static String IMAGE_PITCH_UP = "images/view-pitch-up-32x32.png";
    protected final static String IMAGE_PITCH_DOWN = "images/view-pitch-down-32x32.png";
    protected final static String IMAGE_FOV_NARROW = "images/view-fov-narrow-32x32.png";
    protected final static String IMAGE_FOV_WIDE = "images/view-fov-wide-32x32.png";
    protected final static String IMAGE_VE_UP = "images/view-elevation-up-32x32.png";
    protected final static String IMAGE_VE_DOWN = "images/view-elevation-down-32x32.png";

    // The annotations used to display the controls.
    protected ScreenAnnotation controlPan;
    protected ScreenAnnotation controlLook;
    protected ScreenAnnotation controlHeadingLeft;
    protected ScreenAnnotation controlHeadingRight;
    protected ScreenAnnotation controlZoomIn;
    protected ScreenAnnotation controlZoomOut;
    protected ScreenAnnotation controlPitchUp;
    protected ScreenAnnotation controlPitchDown;
    protected ScreenAnnotation controlFovNarrow;
    protected ScreenAnnotation controlFovWide;
    protected ScreenAnnotation controlVeUp;
    protected ScreenAnnotation controlVeDown;
    protected ScreenAnnotation currentControl;

    protected String position = AVKey.SOUTHWEST;
    protected String layout = AVKey.HORIZONTAL;
    protected Vec4 locationCenter;
    protected Vec4 locationOffset;
    protected double scale = 1;
    protected int borderWidth = 20;
    protected int buttonSize = 32;
    protected int panSize = 64;
    protected boolean initialized;
    protected Rectangle referenceViewport;

    protected boolean showPanControls = true;
    protected boolean showLookControls;
    protected boolean showZoomControls = true;
    protected boolean showHeadingControls = true;
    protected boolean showPitchControls = true;
    protected boolean showFovControls;
    protected boolean showVeControls = true;

    /**
     * Get a control image source.
     *
     * @param control the control type. Can be one of {@link AVKey#VIEW_PAN}, {@link AVKey#VIEW_LOOK}, {@link
     *                AVKey#VIEW_HEADING_LEFT}, {@link AVKey#VIEW_HEADING_RIGHT}, {@link AVKey#VIEW_ZOOM_IN}, {@link
     *                AVKey#VIEW_ZOOM_OUT}, {@link AVKey#VIEW_PITCH_UP}, {@link AVKey#VIEW_PITCH_DOWN}, {@link
     *                AVKey#VIEW_FOV_NARROW} or {@link AVKey#VIEW_FOV_WIDE}.
     * @return the image source associated with the given control type.
     */
    protected static Object getImageSource(String control) {
        return switch (control) {
            case AVKey.VIEW_PAN -> ViewControlsLayer.IMAGE_PAN;
            case AVKey.VIEW_LOOK -> ViewControlsLayer.IMAGE_LOOK;
            case AVKey.VIEW_HEADING_LEFT -> ViewControlsLayer.IMAGE_HEADING_LEFT;
            case AVKey.VIEW_HEADING_RIGHT -> ViewControlsLayer.IMAGE_HEADING_RIGHT;
            case AVKey.VIEW_ZOOM_IN -> ViewControlsLayer.IMAGE_ZOOM_IN;
            case AVKey.VIEW_ZOOM_OUT -> ViewControlsLayer.IMAGE_ZOOM_OUT;
            case AVKey.VIEW_PITCH_UP -> ViewControlsLayer.IMAGE_PITCH_UP;
            case AVKey.VIEW_PITCH_DOWN -> ViewControlsLayer.IMAGE_PITCH_DOWN;
            case AVKey.VIEW_FOV_WIDE -> ViewControlsLayer.IMAGE_FOV_WIDE;
            case AVKey.VIEW_FOV_NARROW -> ViewControlsLayer.IMAGE_FOV_NARROW;
            case AVKey.VERTICAL_EXAGGERATION_UP -> ViewControlsLayer.IMAGE_VE_UP;
            case AVKey.VERTICAL_EXAGGERATION_DOWN -> ViewControlsLayer.IMAGE_VE_DOWN;
            default -> null;
        };
    }

    public int getBorderWidth() {
        return this.borderWidth;
    }

    /**
     * Sets the view controls offset from the viewport border.
     *
     * @param borderWidth the number of pixels to offset the view controls from the borders indicated by {@link
     *                    #setPosition(String)}.
     */
    public void setBorderWidth(int borderWidth) {
        this.borderWidth = borderWidth;
        clearControls();
    }

    /**
     * Get the controls display scale.
     *
     * @return the controls display scale.
     */
    public double getScale() {
        return this.scale;
    }

    /**
     * Set the controls display scale.
     *
     * @param scale the controls display scale.
     */
    public void setScale(double scale) {
        this.scale = scale;
        clearControls();
    }

    protected int getButtonSize() {
        return buttonSize;
    }

    protected void setButtonSize(int buttonSize) {
        this.buttonSize = buttonSize;
        clearControls();
    }

    protected int getPanSize() {
        return panSize;
    }

    protected void setPanSize(int panSize) {
        this.panSize = panSize;
        clearControls();
    }

    /**
     * Returns the current relative view controls position.
     *
     * @return the current view controls position.
     */
    public String getPosition() {
        return this.position;
    }

    /**
     * Sets the relative viewport location to display the view controls. Can be one of {@link AVKey#NORTHEAST}, {@link
     * AVKey#NORTHWEST}, {@link AVKey#SOUTHEAST}, or {@link AVKey#SOUTHWEST} (the default). These indicate the corner of
     * the viewport to place view controls.
     *
     * @param position the desired view controls position, in screen coordinates.
     */
    public void setPosition(String position) {
        if (position == null) {
            String message = Logging.getMessage("nullValue.PositionIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        this.position = position;
        clearControls();
    }

    /**
     * Returns the current layout. Can be one of {@link AVKey#HORIZONTAL} or {@link AVKey#VERTICAL}.
     *
     * @return the current layout.
     */
    public String getLayout() {
        return this.layout;
    }

    /**
     * Sets the desired layout. Can be one of {@link AVKey#HORIZONTAL} or {@link AVKey#VERTICAL}.
     *
     * @param layout the desired layout.
     */
    public void setLayout(String layout) {
        if (layout == null) {
            String message = Logging.getMessage("nullValue.StringIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        if (!this.layout.equals(layout)) {
            this.layout = layout;
            clearControls();
        }
    }

    /**
     * Returns the layer's opacity value, which is ignored by this layer. Opacity is controlled by the alpha values of
     * the operation images.
     *
     * @return The layer opacity, a value between 0 and 1.
     */
    @Override
    public double getOpacity() {
        return super.getOpacity();
    }

    /**
     * Layer opacity is not applied to layers of this type. Opacity is controlled by the alpha values of the operation
     * images.
     *
     * @param opacity the current opacity value, which is ignored by this layer.
     */
    @Override
    public void setOpacity(double opacity) {
        super.setOpacity(opacity);
    }

    /**
     * Returns the current layer image location.
     *
     * @return the current location center. May be null.
     */
    public Vec4 getLocationCenter() {
        return locationCenter;
    }

    /**
     * Specifies the screen location of the layer, relative to the image's center. May be null. If this value is
     * non-null, it overrides the position specified by {@link #setPosition(String)}. The location is specified in
     * pixels. The origin is the window's lower left corner. Positive X values are to the right of the origin, positive
     * Y values are upwards from the origin. The final image location will be affected by the currently specified
     * location offset if a non-null location offset has been specified (see {@link #setLocationOffset(Vec4)} )}.
     *
     * @param locationCenter the location center. May be null.
     * @see #setPosition(String)
     * @see #setLocationOffset(Vec4)
     */
    public void setLocationCenter(Vec4 locationCenter) {
        this.locationCenter = locationCenter;
        clearControls();
    }

    /**
     * Returns the current location offset. See #setLocationOffset for a description of the offset and its values.
     *
     * @return the location offset. Will be null if no offset has been specified.
     */
    public Vec4 getLocationOffset() {
        return locationOffset;
    }

    /**
     * Specifies a placement offset from the layer position on the screen.
     *
     * @param locationOffset the number of pixels to shift the layer image from its specified screen position. A
     *                       positive X value shifts the image to the right. A positive Y value shifts the image up. If
     *                       null, no offset is applied. The default offset is null.
     * @see #setLocationCenter(Vec4)
     * @see #setPosition(String)
     */
    public void setLocationOffset(Vec4 locationOffset) {
        this.locationOffset = locationOffset;
        clearControls();
    }

    public boolean isShowPanControls() {
        return this.showPanControls;
    }

    public void setShowPanControls(boolean state) {
        if (this.showPanControls != state) {
            this.showPanControls = state;
            clearControls();
        }
    }

    public boolean isShowLookControls() {
        return this.showLookControls;
    }

    public void setShowLookControls(boolean state) {
        if (this.showLookControls != state) {
            this.showLookControls = state;
            clearControls();
        }
    }

    public boolean isShowHeadingControls() {
        return this.showHeadingControls;
    }

    public void setShowHeadingControls(boolean state) {
        if (this.showHeadingControls != state) {
            this.showHeadingControls = state;
            clearControls();
        }
    }

    public boolean isShowZoomControls() {
        return this.showZoomControls;
    }

    public void setShowZoomControls(boolean state) {
        if (this.showZoomControls != state) {
            this.showZoomControls = state;
            clearControls();
        }
    }

    public boolean isShowPitchControls() {
        return this.showPitchControls;
    }

    public void setShowPitchControls(boolean state) {
        if (this.showPitchControls != state) {
            this.showPitchControls = state;
            clearControls();
        }
    }

    public boolean isShowFovControls() {
        return this.showFovControls;
    }

    public void setShowFovControls(boolean state) {
        if (this.showFovControls != state) {
            this.showFovControls = state;
            clearControls();
        }
    }

    public boolean isShowVeControls() {
        return this.showVeControls;
    }

    public void setShowVeControls(boolean state) {
        if (this.showVeControls != state) {
            this.showVeControls = state;
            clearControls();
        }
    }

    /**
     * Get the control type associated with the given object or null if unknown.
     *
     * @param control the control object
     * @return the control type. Can be one of {@link AVKey#VIEW_PAN}, {@link AVKey#VIEW_LOOK}, {@link
     * AVKey#VIEW_HEADING_LEFT}, {@link AVKey#VIEW_HEADING_RIGHT}, {@link AVKey#VIEW_ZOOM_IN}, {@link
     * AVKey#VIEW_ZOOM_OUT}, {@link AVKey#VIEW_PITCH_UP}, {@link AVKey#VIEW_PITCH_DOWN}, {@link AVKey#VIEW_FOV_NARROW}
     * or {@link AVKey#VIEW_FOV_WIDE}. <p> Returns null if the object is not a view control associated with this layer.
     * </p>
     */
    public String getControlType(Object control) {
        if (!(control instanceof ScreenAnnotation))
            return null;

        if (showPanControls && controlPan.equals(control))
            return AVKey.VIEW_PAN;
        else if (showLookControls && controlLook.equals(control))
            return AVKey.VIEW_LOOK;
        else if (showHeadingControls && controlHeadingLeft.equals(control))
            return AVKey.VIEW_HEADING_LEFT;
        else if (showHeadingControls && controlHeadingRight.equals(control))
            return AVKey.VIEW_HEADING_RIGHT;
        else if (showZoomControls && controlZoomIn.equals(control))
            return AVKey.VIEW_ZOOM_IN;
        else if (showZoomControls && controlZoomOut.equals(control))
            return AVKey.VIEW_ZOOM_OUT;
        else if (showPitchControls && controlPitchUp.equals(control))
            return AVKey.VIEW_PITCH_UP;
        else if (showPitchControls && controlPitchDown.equals(control))
            return AVKey.VIEW_PITCH_DOWN;
        else if (showFovControls && controlFovNarrow.equals(control))
            return AVKey.VIEW_FOV_NARROW;
        else if (showFovControls && controlFovWide.equals(control))
            return AVKey.VIEW_FOV_WIDE;
        else if (showVeControls && controlVeUp.equals(control))
            return AVKey.VERTICAL_EXAGGERATION_UP;
        else if (showVeControls && controlVeDown.equals(control))
            return AVKey.VERTICAL_EXAGGERATION_DOWN;

        return null;
    }

    /**
     * Indicates the currently highlighted control, if any.
     *
     * @return the currently highlighted control, or null if no control is highlighted.
     */
    public Object getHighlightedObject() {
        return this.currentControl;
    }

    /**
     * Specifies the control to highlight. Any currently highlighted control is un-highlighted.
     *
     * @param control the control to highlight.
     */
    public void highlight(Object control) {
        // Manage highlighting of controls.
        if (this.currentControl == control)
            return; // same thing selected

        // Turn off highlight if on.
        if (this.currentControl != null) {
            this.currentControl.getAttributes().setImageOpacity(-1); // use default opacity
            this.currentControl = null;
        }

        // Turn on highlight if object selected.
        if (control instanceof ScreenAnnotation) {
            this.currentControl = (ScreenAnnotation) control;
            this.currentControl.getAttributes().setImageOpacity(1);
        }
    }

    @Override
    public void doRender(DrawContext dc) {
        if (!this.initialized)
            initialize(dc);

        if (!this.referenceViewport.equals(dc.getView().getViewport()))
            updatePositions(dc);

        super.doRender(dc);
    }

    protected boolean isInitialized() {
        return initialized;
    }

    protected void initialize(DrawContext dc) {
        if (this.initialized)
            return;

        // Setup user interface - common default attributes
        AnnotationAttributes ca = new AnnotationAttributes();
        ca.setAdjustWidthToText(AVKey.SIZE_FIXED);
        ca.setInsets(new Insets(0, 0, 0, 0));
        ca.setBorderWidth(0);
        ca.setCornerRadius(0);
        ca.setSize(new Dimension(buttonSize, buttonSize));
        ca.setBackgroundColor(new Color(0, 0, 0, 0));
        ca.setImageOpacity(0.5);
        ca.setScale(scale);

        final String NOTEXT = "";
        final Point ORIGIN = new Point(0, 0);
        if (this.showPanControls) {
            // Pan
            controlPan = new ScreenAnnotation(NOTEXT, ORIGIN, ca);
            controlPan.set(AVKey.VIEW_OPERATION, AVKey.VIEW_PAN);
            controlPan.getAttributes().setImageSource(ViewControlsLayer.getImageSource(AVKey.VIEW_PAN));
            controlPan.getAttributes().setSize(new Dimension(panSize, panSize));
            this.add(controlPan);
        }
        if (this.showLookControls) {
            // Look
            controlLook = new ScreenAnnotation(NOTEXT, ORIGIN, ca);
            controlLook.set(AVKey.VIEW_OPERATION, AVKey.VIEW_LOOK);
            controlLook.getAttributes().setImageSource(ViewControlsLayer.getImageSource(AVKey.VIEW_LOOK));
            controlLook.getAttributes().setSize(new Dimension(panSize, panSize));
            this.add(controlLook);
        }
        if (this.showZoomControls) {
            // Zoom
            controlZoomIn = new ScreenAnnotation(NOTEXT, ORIGIN, ca);
            controlZoomIn.set(AVKey.VIEW_OPERATION, AVKey.VIEW_ZOOM_IN);
            controlZoomIn.getAttributes().setImageSource(ViewControlsLayer.getImageSource(AVKey.VIEW_ZOOM_IN));
            this.add(controlZoomIn);
            controlZoomOut = new ScreenAnnotation(NOTEXT, ORIGIN, ca);
            controlZoomOut.set(AVKey.VIEW_OPERATION, AVKey.VIEW_ZOOM_OUT);
            controlZoomOut.getAttributes().setImageSource(ViewControlsLayer.getImageSource(AVKey.VIEW_ZOOM_OUT));
            this.add(controlZoomOut);
        }
        if (this.showHeadingControls) {
            // Heading
            controlHeadingLeft = new ScreenAnnotation(NOTEXT, ORIGIN, ca);
            controlHeadingLeft.set(AVKey.VIEW_OPERATION, AVKey.VIEW_HEADING_LEFT);
            controlHeadingLeft.getAttributes().setImageSource(ViewControlsLayer.getImageSource(AVKey.VIEW_HEADING_LEFT));
            this.add(controlHeadingLeft);
            controlHeadingRight = new ScreenAnnotation(NOTEXT, ORIGIN, ca);
            controlHeadingRight.set(AVKey.VIEW_OPERATION, AVKey.VIEW_HEADING_RIGHT);
            controlHeadingRight.getAttributes().setImageSource(ViewControlsLayer.getImageSource(AVKey.VIEW_HEADING_RIGHT));
            this.add(controlHeadingRight);
        }
        if (this.showPitchControls) {
            // Pitch
            controlPitchUp = new ScreenAnnotation(NOTEXT, ORIGIN, ca);
            controlPitchUp.set(AVKey.VIEW_OPERATION, AVKey.VIEW_PITCH_UP);
            controlPitchUp.getAttributes().setImageSource(ViewControlsLayer.getImageSource(AVKey.VIEW_PITCH_UP));
            this.add(controlPitchUp);
            controlPitchDown = new ScreenAnnotation(NOTEXT, ORIGIN, ca);
            controlPitchDown.set(AVKey.VIEW_OPERATION, AVKey.VIEW_PITCH_DOWN);
            controlPitchDown.getAttributes().setImageSource(ViewControlsLayer.getImageSource(AVKey.VIEW_PITCH_DOWN));
            this.add(controlPitchDown);
        }
        if (this.showFovControls) {
            // Field of view FOV
            controlFovNarrow = new ScreenAnnotation(NOTEXT, ORIGIN, ca);
            controlFovNarrow.set(AVKey.VIEW_OPERATION, AVKey.VIEW_FOV_NARROW);
            controlFovNarrow.getAttributes().setImageSource(ViewControlsLayer.getImageSource(AVKey.VIEW_FOV_NARROW));
            this.add(controlFovNarrow);
            controlFovWide = new ScreenAnnotation(NOTEXT, ORIGIN, ca);
            controlFovWide.set(AVKey.VIEW_OPERATION, AVKey.VIEW_FOV_WIDE);
            controlFovWide.getAttributes().setImageSource(ViewControlsLayer.getImageSource(AVKey.VIEW_FOV_WIDE));
            this.add(controlFovWide);
        }
        if (this.showVeControls) {
            // Vertical Exaggeration
            controlVeUp = new ScreenAnnotation(NOTEXT, ORIGIN, ca);
            controlVeUp.set(AVKey.VIEW_OPERATION, AVKey.VERTICAL_EXAGGERATION_UP);
            controlVeUp.getAttributes().setImageSource(ViewControlsLayer.getImageSource(AVKey.VERTICAL_EXAGGERATION_UP));
            this.add(controlVeUp);
            controlVeDown = new ScreenAnnotation(NOTEXT, ORIGIN, ca);
            controlVeDown.set(AVKey.VIEW_OPERATION, AVKey.VERTICAL_EXAGGERATION_DOWN);
            controlVeDown.getAttributes().setImageSource(ViewControlsLayer.getImageSource(AVKey.VERTICAL_EXAGGERATION_DOWN));
            this.add(controlVeDown);
        }

        // Place controls according to layout and viewport dimension
        updatePositions(dc);

        this.initialized = true;
    }

    // Set controls positions according to layout and viewport dimension
    protected void updatePositions(DrawContext dc) {
        boolean horizontalLayout = this.layout.equals(AVKey.HORIZONTAL);

        // horizontal layout: pan button + look button beside 2 rows of 4 buttons
        int width = (showPanControls ? panSize : 0) +
            (showLookControls ? panSize : 0) +
            (showZoomControls ? buttonSize : 0) +
            (showHeadingControls ? buttonSize : 0) +
            (showPitchControls ? buttonSize : 0) +
            (showFovControls ? buttonSize : 0) +
            (showVeControls ? buttonSize : 0);
        int height = Math.max(panSize, buttonSize * 2);
        width = (int) (width * scale);
        height = (int) (height * scale);
        int xOffset = 0;
        int yOffset = (int) (buttonSize * scale);

        if (!horizontalLayout) {
            // vertical layout: pan button above look button above 4 rows of 2 buttons
            int temp = height;
            //noinspection SuspiciousNameCombination
            height = width;
            width = temp;
            xOffset = (int) (buttonSize * scale);
            yOffset = 0;
        }

        int halfPanSize = (int) (panSize * scale / 2);
        int halfButtonSize = (int) (buttonSize * scale / 2);

        Rectangle controlsRectangle = new Rectangle(width, height);
        Point locationSW = computeLocation(dc.getView().getViewport(), controlsRectangle);

        // Layout start point
        int x = locationSW.x;
        int y = horizontalLayout ? locationSW.y : locationSW.y + height;

        if (this.showPanControls) {
            if (!horizontalLayout)
                y -= (int) (panSize * scale);
            controlPan.setScreenPoint(new Point(x + halfPanSize, y));
            if (horizontalLayout)
                x += (int) (panSize * scale);
        }
        if (this.showLookControls) {
            if (!horizontalLayout)
                y -= (int) (panSize * scale);
            controlLook.setScreenPoint(new Point(x + halfPanSize, y));
            if (horizontalLayout)
                x += (int) (panSize * scale);
        }
        if (this.showZoomControls) {
            if (!horizontalLayout)
                y -= (int) (buttonSize * scale);
            controlZoomIn.setScreenPoint(new Point(x + halfButtonSize + xOffset, y + yOffset));
            controlZoomOut.setScreenPoint(new Point(x + halfButtonSize, y));
            if (horizontalLayout)
                x += (int) (buttonSize * scale);
        }
        if (this.showHeadingControls) {
            if (!horizontalLayout)
                y -= (int) (buttonSize * scale);
            controlHeadingLeft.setScreenPoint(new Point(x + halfButtonSize + xOffset, y + yOffset));
            controlHeadingRight.setScreenPoint(new Point(x + halfButtonSize, y));
            if (horizontalLayout)
                x += (int) (buttonSize * scale);
        }
        if (this.showPitchControls) {
            if (!horizontalLayout)
                y -= (int) (buttonSize * scale);
            controlPitchUp.setScreenPoint(new Point(x + halfButtonSize + xOffset, y + yOffset));
            controlPitchDown.setScreenPoint(new Point(x + halfButtonSize, y));
            if (horizontalLayout)
                x += (int) (buttonSize * scale);
        }
        if (this.showFovControls) {
            if (!horizontalLayout)
                y -= (int) (buttonSize * scale);
            controlFovNarrow.setScreenPoint(new Point(x + halfButtonSize + xOffset, y + yOffset));
            controlFovWide.setScreenPoint(new Point(x + halfButtonSize, y));
            if (horizontalLayout)
                x += (int) (buttonSize * scale);
        }
        if (this.showVeControls) {
            if (!horizontalLayout)
                y -= (int) (buttonSize * scale);
            controlVeUp.setScreenPoint(new Point(x + halfButtonSize + xOffset, y + yOffset));
            controlVeDown.setScreenPoint(new Point(x + halfButtonSize, y));
            if (horizontalLayout)
                x += (int) (buttonSize * scale);
        }

        this.referenceViewport = dc.getView().getViewport();
    }

    /**
     * Compute the screen location of the controls overall rectangle bottom right corner according to either the
     * location center if not null, or the screen position.
     *
     * @param viewport the current viewport rectangle.
     * @param controls the overall controls rectangle
     * @return the screen location of the bottom left corner - south west corner.
     */
    protected Point computeLocation(Rectangle viewport, Rectangle controls) {
        double x;
        double y;

        if (this.locationCenter != null) {
            x = this.locationCenter.x - controls.width / 2.0;
            y = this.locationCenter.y - controls.height / 2.0;
        } else if (this.position.equals(AVKey.NORTHEAST)) {
            x = viewport.getWidth() - controls.width - this.borderWidth;
            y = viewport.getHeight() - controls.height - this.borderWidth;
        } else if (this.position.equals(AVKey.SOUTHEAST)) {
            x = viewport.getWidth() - controls.width - this.borderWidth;
            y = 0.0d + this.borderWidth;
        } else if (this.position.equals(AVKey.NORTHWEST)) {
            x = 0.0d + this.borderWidth;
            y = viewport.getHeight() - controls.height - this.borderWidth;
        } else if (this.position.equals(AVKey.SOUTHWEST)) {
            x = 0.0d + this.borderWidth;
            y = 0.0d + this.borderWidth;
        } else // use North East as default
        {
            x = viewport.getWidth() - controls.width - this.borderWidth;
            y = viewport.getHeight() - controls.height - this.borderWidth;
        }

        if (this.locationOffset != null) {
            x += this.locationOffset.x;
            y += this.locationOffset.y;
        }

        return new Point((int) x, (int) y);
    }

    protected void clearControls() {
        this.clear();

        this.controlPan = null;
        this.controlLook = null;
        this.controlHeadingLeft = null;
        this.controlHeadingRight = null;
        this.controlZoomIn = null;
        this.controlZoomOut = null;
        this.controlPitchUp = null;
        this.controlPitchDown = null;
        this.controlFovNarrow = null;
        this.controlFovWide = null;
        this.controlVeUp = null;
        this.controlVeDown = null;

        this.initialized = false;
    }

    @Override
    public String toString() {
        return Logging.getMessage("layers.ViewControlsLayer.Name");
    }

    /**
     * Controller for onscreen view controls displayed by {@link ViewControlsLayer}.
     *
     * @author Patrick Murris
     * @version $Id: ViewControlsSelectListener.java 1876 2014-03-19 17:13:30Z tgaskins $
     * @see ViewControlsLayer
     */
    public static class ViewControlsSelectListener implements SelectListener {
        protected static final int DEFAULT_TIMER_DELAY = 50;

        protected WorldWindow wwd;
        protected ViewControlsLayer viewControlsLayer;

        protected ScreenAnnotation pressedControl;
        protected String pressedControlType;
        protected Point lastPickPoint;

        protected Timer repeatTimer;
        protected double panStep = 0.6;
        protected double zoomStep = 0.8;
        protected double headingStep = 1;
        protected double pitchStep = 1;
        protected double fovStep = 1.05;
        protected double veStep = 0.1;

        /**
         * Construct a controller for specified <code>WorldWindow</code> and <code>ViewControlsLayer</code>.
         * <p>
         * <code>ViewControlLayer</code>s are not sharable among <code>WorldWindow</code>s. A separate layer and
         * controller
         * must be established for each window that's to have view controls.
         *
         * @param wwd   the <code>WorldWindow</code> the specified layer is associated with.
         * @param layer the layer to control.
         */
        public ViewControlsSelectListener(WorldWindow wwd, ViewControlsLayer layer) {
            if (wwd == null) {
                String msg = Logging.getMessage("nullValue.WorldWindow");
                Logging.logger().severe(msg);
                throw new IllegalArgumentException(msg);
            }
            if (layer == null) {
                String msg = Logging.getMessage("nullValue.LayerIsNull");
                Logging.logger().severe(msg);
                throw new IllegalArgumentException(msg);
            }

            this.wwd = wwd;
            this.viewControlsLayer = layer;

            // Setup repeat timer
            this.repeatTimer = new Timer(ViewControlsSelectListener.DEFAULT_TIMER_DELAY, event -> {
                if (pressedControl != null)
                    updateView(pressedControl, pressedControlType);
            });
            this.repeatTimer.start();
        }

        protected static boolean isPathCrossingAPole(LatLon p1, LatLon p2) {
            return Math.abs(p1.getLongitude().degrees - p2.getLongitude().degrees) > 20
                && Math.abs(p1.getLatitude().degrees - 90 * Math.signum(p1.getLatitude().degrees)) < 10;
        }

        protected static double computeNewZoom(OrbitView view, double amount) {
            double coeff = 0.05;
            double change = coeff * amount;
            double logZoom = view.getZoom() != 0 ? Math.log(view.getZoom()) : 0;
            // Zoom changes are treated as logarithmic values. This accomplishes two things:
            // 1) Zooming is slow near the globe, and fast at great distances.
            // 2) Zooming in then immediately zooming out returns the viewer to the same zoom value.
            return Math.exp(logZoom + change);
        }

        /**
         * Get the repeat timer delay in milliseconds.
         *
         * @return the repeat timer delay in milliseconds.
         */
        public int getRepeatTimerDelay() {
            return this.repeatTimer.getDelay();
        }

        /**
         * Set the repeat timer delay in milliseconds.
         *
         * @param delay the repeat timer delay in milliseconds.
         * @throws IllegalArgumentException if delay is less than or equal to zero.
         */
        public void setRepeatTimerDelay(int delay) {
            if (delay <= 0) {
                String message = Logging.getMessage("generic.ArgumentOutOfRange", delay);
                Logging.logger().severe(message);
                throw new IllegalArgumentException(message);
            }
            this.repeatTimer.setDelay(delay);
        }

        /**
         * Get the panning distance factor.
         *
         * @return the panning distance factor.
         */
        public double getPanIncrement() {
            return this.panStep;
        }

        /**
         * Set the panning distance factor. Doubling this value will double the panning speed. Negating it will reverse
         * the panning direction. Default value is .6.
         *
         * @param value the panning distance factor.
         */
        public void setPanIncrement(double value) {
            this.panStep = value;
        }

        /**
         * Get the zooming distance factor.
         *
         * @return the zooming distance factor.
         */
        public double getZoomIncrement() {
            return this.zoomStep;
        }

        /**
         * Set the zoom distance factor. Doubling this value will double the zooming speed. Negating it will reverse the
         * zooming direction. Default value is .8.
         *
         * @param value the zooming distance factor.
         */
        public void setZoomIncrement(double value) {
            this.zoomStep = value;
        }

        /**
         * Get the heading increment value in decimal degrees.
         *
         * @return the heading increment value in decimal degrees.
         */
        public double getHeadingIncrement() {
            return this.headingStep;
        }

        /**
         * Set the heading increment value in decimal degrees. Doubling this value will double the heading change speed.
         * Negating it will reverse the heading change direction. Default value is 1 degree.
         *
         * @param value the heading increment value in decimal degrees.
         */
        public void setHeadingIncrement(double value) {
            this.headingStep = value;
        }

        /**
         * Get the pitch increment value in decimal degrees.
         *
         * @return the pitch increment value in decimal degrees.
         */
        public double getPitchIncrement() {
            return this.pitchStep;
        }

        /**
         * Set the pitch increment value in decimal degrees. Doubling this value will double the pitch change speed.
         * Must be positive. Default value is 1 degree.
         *
         * @param value the pitch increment value in decimal degrees.
         * @throws IllegalArgumentException if value is &lt; zero.
         */
        public void setPitchIncrement(double value) {
            if (value < 0) {
                String message = Logging.getMessage("generic.ArgumentOutOfRange", value);
                Logging.logger().severe(message);
                throw new IllegalArgumentException(message);
            }
            this.pitchStep = value;
        }

        /**
         * Get the field of view increment factor.
         *
         * @return the field of view increment factor.
         */
        public double getFovIncrement() {
            return this.fovStep;
        }

        /**
         * Set the field of view increment factor. At each iteration the current field of view will be multiplied or
         * divided by this value. Must be greater then or equal to one. Default value is 1.05.
         *
         * @param value the field of view increment factor.
         * @throws IllegalArgumentException if value &lt; 1;
         */
        public void setFovIncrement(double value) {
            if (value < 1) {
                String message = Logging.getMessage("generic.ArgumentOutOfRange", value);
                Logging.logger().severe(message);
                throw new IllegalArgumentException(message);
            }
            this.fovStep = value;
        }

        /**
         * Get the vertical exaggeration increment.
         *
         * @return the vertical exaggeration increment.
         */
        public double getVeIncrement() {
            return this.veStep;
        }

        /**
         * Set the vertical exaggeration increment. At each iteration the current vertical exaggeration will be
         * increased or decreased by this amount. Must be greater than or equal to zero. Default value is 0.1.
         *
         * @param value the vertical exaggeration increment.
         * @throws IllegalArgumentException if value &lt; 0.
         */
        public void setVeIncrement(double value) {
            if (value < 0) {
                String message = Logging.getMessage("generic.ArgumentOutOfRange", value);
                Logging.logger().severe(message);
                throw new IllegalArgumentException(message);
            }
            this.veStep = value;
        }

        public void accept(SelectEvent event) {
            if (this.wwd == null)
                return;

            if (!(this.wwd.view() instanceof OrbitView))
                return;

            OrbitView view = (OrbitView) this.wwd.view();

            if (this.viewControlsLayer.getHighlightedObject() != null) {
                this.viewControlsLayer.highlight(null);
                this.wwd.redraw(); // must redraw so the de-highlight can take effect
            }

            if (event.mouseEvent != null && event.mouseEvent.isConsumed())
                return;

            if (event.getTopObject() == null || event.getTopPickedObject().getParentLayer() != this.getParentLayer()
                || !(event.getTopObject() instanceof AVList))
                return;

            String controlType = ((AVList) event.getTopObject()).getStringValue(AVKey.VIEW_OPERATION);
            if (controlType == null)
                return;

            ScreenAnnotation selectedObject = (ScreenAnnotation) event.getTopObject();

            this.lastPickPoint = event.pickPoint;
            if (event.getEventAction().equals(SelectEvent.ROLLOVER)) {
                // Highlight on rollover
                this.viewControlsLayer.highlight(selectedObject);
                this.wwd.redraw();
            }
            if (event.getEventAction().equals(SelectEvent.DRAG)) {
                // just consume drag events
                event.consume();
            } else if (event.getEventAction().equals(SelectEvent.HOVER)) {
                // Highlight on hover
                this.viewControlsLayer.highlight(selectedObject);
                this.wwd.redraw();
            } else if (event.getEventAction().equals(SelectEvent.LEFT_PRESS) ||
                (event.getEventAction().equals(SelectEvent.DRAG) && controlType.equals(AVKey.VIEW_PAN)) ||
                (event.getEventAction().equals(SelectEvent.DRAG) && controlType.equals(AVKey.VIEW_LOOK))) {
                // Handle left press on controls
                this.pressedControl = selectedObject;
                this.pressedControlType = controlType;

                // Consume drag events, but do not consume left press events. It is not necessary to consume left press
                // events here, and doing so prevents the WorldWindow from gaining focus.
                if (event.getEventAction().equals(SelectEvent.DRAG))
                    event.consume();
            } else if (event.getEventAction().equals(SelectEvent.LEFT_CLICK)
                || event.getEventAction().equals(SelectEvent.LEFT_DOUBLE_CLICK)
                || event.getEventAction().equals(SelectEvent.DRAG_END)) {
                // Release pressed control

                if (pressedControl != null)
                    event.consume();

                this.pressedControl = null;
                resetOrbitView(view);
                view.firePropertyChange(AVKey.VIEW, null, view);
            }

            // Keep pressed control highlighted - overrides rollover non currently pressed controls
            if (this.pressedControl != null) {
                this.viewControlsLayer.highlight(this.pressedControl);
                this.wwd.redraw();
            }
        }

        /**
         * Returns this ViewControlsSelectListener's parent layer. The parent layer is associated with picked objects,
         * and is used to determine which SelectEvents thsi ViewControlsSelectListner responds to.
         *
         * @return this ViewControlsSelectListener's parent layer.
         */
        protected Layer getParentLayer() {
            return this.viewControlsLayer;
        }

        protected void updateView(ScreenAnnotation control, String controlType) {
            if (this.wwd == null)
                return;
            if (!(this.wwd.view() instanceof OrbitView))
                return;

            OrbitView view = (OrbitView) this.wwd.view();
            view.stopAnimations();
            view.stopMovement();

            switch (controlType) {
                case AVKey.VIEW_PAN: {
                    resetOrbitView(view);
                    // Go some distance in the control mouse direction
                    Angle heading = computePanHeading(view, control);
                    Angle distance = computePanAmount(this.wwd.model().getGlobe(), view, control, panStep);
                    LatLon newViewCenter = LatLon.greatCircleEndPosition(view.getCenterPosition(),
                        heading, distance);
                    // Turn around if passing by a pole - TODO: better handling of the pole crossing situation
                    if (ViewControlsSelectListener.isPathCrossingAPole(newViewCenter, view.getCenterPosition()))
                        view.setHeading(Angle.POS180.sub(view.getHeading()));
                    // Set new center pos
                    view.setCenterPosition(new Position(newViewCenter, view.getCenterPosition().getElevation()));
                    break;
                }
                case AVKey.VIEW_LOOK: {
                    setupFirstPersonView(view);
                    Angle heading = computeLookHeading(view, control, headingStep);
                    Angle pitch = computeLookPitch(view, control, pitchStep);
                    // Check whether the view will still point at terrain
                    Vec4 surfacePoint = computeSurfacePoint(view, heading, pitch);
                    if (surfacePoint != null) {
                        // Change view state
                        final Position eyePos = view.getEyePosition();// Save current eye position
                        view.setHeading(heading);
                        view.setPitch(pitch);
                        view.setZoom(0);
                        view.setCenterPosition(eyePos); // Set center at the eye position
                    }
                    break;
                }
                case AVKey.VIEW_ZOOM_IN:
                    resetOrbitView(view);
                    view.setZoom(ViewControlsSelectListener.computeNewZoom(view, -zoomStep));
                    break;
                case AVKey.VIEW_ZOOM_OUT:
                    resetOrbitView(view);
                    view.setZoom(ViewControlsSelectListener.computeNewZoom(view, zoomStep));
                    break;
                case AVKey.VIEW_HEADING_LEFT:
                    resetOrbitView(view);
                    view.setHeading(view.getHeading().addDegrees(headingStep));
                    break;
                case AVKey.VIEW_HEADING_RIGHT:
                    resetOrbitView(view);
                    view.setHeading(view.getHeading().addDegrees(-headingStep));
                    break;
                case AVKey.VIEW_PITCH_UP:
                    resetOrbitView(view);
                    if (view.getPitch().degrees >= pitchStep)
                        view.setPitch(view.getPitch().addDegrees(-pitchStep));
                    break;
                case AVKey.VIEW_PITCH_DOWN:
                    resetOrbitView(view);
                    if (view.getPitch().degrees <= 90 - pitchStep)
                        view.setPitch(view.getPitch().addDegrees(pitchStep));
                    break;
                case AVKey.VIEW_FOV_NARROW:
                    if (view.getFieldOfView().degrees / fovStep >= 4)
                        view.setFieldOfView(view.getFieldOfView().divide(fovStep));
                    break;
                case AVKey.VIEW_FOV_WIDE:
                    if (view.getFieldOfView().degrees * fovStep < 120)
                        view.setFieldOfView(view.getFieldOfView().multiply(fovStep));
                    break;
                case AVKey.VERTICAL_EXAGGERATION_UP: {
                    SceneController sc = this.wwd.sceneControl();
                    sc.setVerticalExaggeration(sc.getVerticalExaggeration() + this.veStep);
                    break;
                }
                case AVKey.VERTICAL_EXAGGERATION_DOWN: {
                    SceneController sc = this.wwd.sceneControl();
                    sc.setVerticalExaggeration(Math.max(1.0d, sc.getVerticalExaggeration() - this.veStep));
                    break;
                }
            }
            view.firePropertyChange(AVKey.VIEW, null, view);
        }

        protected Angle computePanHeading(View view, ScreenAnnotation control) {
            // Compute last pick point 'heading' relative to pan control center
            double size = control.getAttributes().getSize().width * control.getAttributes().getScale();
            Vec4 center = new Vec4(control.getScreenPoint().x, control.getScreenPoint().y + size / 2, 0);
            double px = lastPickPoint.x - center.x;
            double py = view.getViewport().getHeight() - lastPickPoint.y - center.y;
            Angle heading = view.getHeading().add(Angle.fromRadians(Math.atan2(px, py)));
            heading = heading.degrees >= 0 ? heading : heading.addDegrees(360);
            return heading;
        }

        protected Angle computePanAmount(Globe globe, View view, ScreenAnnotation control, double panStep) {
            // Compute last pick point distance relative to pan control center
            double size = control.getAttributes().getSize().width * control.getAttributes().getScale();
            Vec4 center = new Vec4(control.getScreenPoint().x, control.getScreenPoint().y + size / 2, 0);
            double px = lastPickPoint.x - center.x;
            double py = view.getViewport().getHeight() - lastPickPoint.y - center.y;
            double pickDistance = Math.sqrt(px * px + py * py);
            double pickDistanceFactor = Math.min(pickDistance / 10, 5);

            // Compute globe angular distance depending on eye altitude
            Position eyePos = view.getEyePosition();
            double radius = globe.getRadiusAt(eyePos);
            double minValue = 0.5 * (180.0 / (Math.PI * radius)); // Minimum change ~0.5 meters
            double maxValue = 1.0; // Maximum change ~1 degree

            // Compute an interpolated value between minValue and maxValue, using (eye altitude)/(globe radius) as
            // the interpolant. Interpolation is performed on an exponential curve, to keep the value from
            // increasing too quickly as eye altitude increases.
            double a = eyePos.getElevation() / radius;
            a = (a < 0 ? 0 : (a > 1 ? 1 : a));
            double expBase = 2.0; // Exponential curve parameter.
            double value = minValue + (maxValue - minValue) * ((Math.pow(expBase, a) - 1.0) / (expBase - 1.0));

            return Angle.fromDegrees(value * pickDistanceFactor * panStep);
        }

        protected Angle computeLookHeading(View view, ScreenAnnotation control, double headingStep) {
            // Compute last pick point 'heading' relative to look control center on x
            double size = control.getAttributes().getSize().width * control.getAttributes().getScale();
            Vec4 center = new Vec4(control.getScreenPoint().x, control.getScreenPoint().y + size / 2, 0);
            double px = lastPickPoint.x - center.x;
            double pickDistanceFactor = Math.min(Math.abs(px) / 3000, 5) * Math.signum(px);
            // New heading
            Angle heading = view.getHeading().add(Angle.fromRadians(headingStep * pickDistanceFactor));
            heading = heading.degrees >= 0 ? heading : heading.addDegrees(360);
            return heading;
        }

        protected Angle computeLookPitch(View view, ScreenAnnotation control, double pitchStep) {
            // Compute last pick point 'pitch' relative to look control center on y
            double size = control.getAttributes().getSize().width * control.getAttributes().getScale();
            Vec4 center = new Vec4(control.getScreenPoint().x, control.getScreenPoint().y + size / 2, 0);
            double py = view.getViewport().getHeight() - lastPickPoint.y - center.y;
            double pickDistanceFactor = Math.min(Math.abs(py) / 3000, 5) * Math.signum(py);
            // New pitch
            Angle pitch = view.getPitch().add(Angle.fromRadians(pitchStep * pickDistanceFactor));
            pitch = pitch.degrees >= 0 ? (pitch.degrees <= 90 ? pitch : Angle.fromDegrees(90)) : Angle.ZERO;
            return pitch;
        }

        /**
         * Reset the view to an orbit view state if in first person mode (zoom = 0)
         *
         * @param view the orbit view to reset
         */
        protected void resetOrbitView(OrbitView view) {
            if (view.getZoom() > 0)   // already in orbit view mode
                return;

            // Find out where on the terrain the eye is looking at in the viewport center
            // TODO: if no terrain is found in the viewport center, iterate toward viewport bottom until it is found
            Vec4 centerPoint = computeSurfacePoint(view, view.getHeading(), view.getPitch());
            // Reset the orbit view center point heading, pitch and zoom
            if (centerPoint != null) {
                Vec4 eyePoint = view.getEyePoint();
                // Center pos on terrain surface
                Position centerPosition = wwd.model().getGlobe().computePositionFromPoint(centerPoint);
                // Compute pitch and heading relative to center position
                Vec4 normal = wwd.model().getGlobe().computeSurfaceNormalAtLocation(centerPosition.getLatitude(),
                    centerPosition.getLongitude());
                Vec4 north = wwd.model().getGlobe().computeNorthPointingTangentAtLocation(centerPosition.getLatitude(),
                    centerPosition.getLongitude());
                // Pitch
                view.setPitch(Angle.POS180.sub(view.getForwardVector().angleBetween3(normal)));
                // Heading
                Vec4 perpendicular = view.getForwardVector().perpendicularTo3(normal);
                Angle heading = perpendicular.angleBetween3(north);
                double direction = Math.signum(-normal.cross3(north).dot3(perpendicular));
                view.setHeading(heading.multiply(direction));
                // Zoom
                view.setZoom(eyePoint.distanceTo3(centerPoint));
                // Center pos
                view.setCenterPosition(centerPosition);
            }
        }

        /**
         * Setup the view to a first person mode (zoom = 0)
         *
         * @param view the orbit view to set into a first person view.
         */
        protected void setupFirstPersonView(OrbitView view) {
            if (view.getZoom() == 0)  // already in first person mode
                return;

            Vec4 eyePoint = view.getEyePoint();
            // Center pos at eye pos
            Position centerPosition = wwd.model().getGlobe().computePositionFromPoint(eyePoint);
            // Compute pitch and heading relative to center position
            Vec4 normal = wwd.model().getGlobe().computeSurfaceNormalAtLocation(centerPosition.getLatitude(),
                centerPosition.getLongitude());
            Vec4 north = wwd.model().getGlobe().computeNorthPointingTangentAtLocation(centerPosition.getLatitude(),
                centerPosition.getLongitude());
            // Pitch
            view.setPitch(Angle.POS180.sub(view.getForwardVector().angleBetween3(normal)));
            // Heading
            Vec4 perpendicular = view.getForwardVector().perpendicularTo3(normal);
            Angle heading = perpendicular.angleBetween3(north);
            double direction = Math.signum(-normal.cross3(north).dot3(perpendicular));
            view.setHeading(heading.multiply(direction));
            // Zoom
            view.setZoom(0);
            // Center pos
            view.setCenterPosition(centerPosition);
        }

        /**
         * Find out where on the terrain surface the eye would be looking at with the given heading and pitch angles.
         *
         * @param view    the orbit view
         * @param heading heading direction clockwise from north.
         * @param pitch   view pitch angle from the surface normal at the center point.
         * @return the terrain surface point the view would be looking at in the viewport center.
         */
        protected Vec4 computeSurfacePoint(OrbitView view, Angle heading, Angle pitch) {
            Globe globe = wwd.model().getGlobe();
            // Compute transform to be applied to north pointing Y so that it would point in the view direction
            // Move coordinate system to view center point
            Matrix transform = globe.computeSurfaceOrientationAtPosition(view.getCenterPosition());
            // Rotate so that the north pointing axes Y will point in the look at direction
            transform = transform.multiply(Matrix.fromRotationZ(heading.multiply(-1)));
            transform = transform.multiply(Matrix.fromRotationX(Angle.NEG90.add(pitch)));
            // Compute forward vector
            Vec4 forward = Vec4.UNIT_Y.transformBy4(transform);
            // Return intersection with terrain
            Intersection[] intersections = wwd.sceneControl().getTerrain().intersect(
                new Line(view.getEyePoint(), forward));
            return (intersections != null && intersections.length != 0) ? intersections[0].getIntersectionPoint()
                : null;
        }
    }
}
