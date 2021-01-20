/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.util;

import com.jogamp.opengl.GL2;
import gov.nasa.worldwind.Keys;
import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.render.*;

import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;

/**
 * @author dcollins
 * @version $Id: DialogAnnotation.java 1171 2013-02-11 21:45:02Z dcollins $
 */
public abstract class DialogAnnotation extends GlobeAnnotation implements ActionListener {
    protected static final String CLOSE_IMAGE_PATH = "gov/nasa/worldwind/examples/images/16x16-button-cancel.png";
    protected static final String BUSY_IMAGE_PATH = "images/indicator-16.gif";
    protected static final String DEPRESSED_MASK_PATH
        = "gov/nasa/worldwind/examples/images/16x16-button-depressed-mask.png";

    protected static final String CLOSE_TOOLTIP_TEXT = "Close window";
    protected final EventListenerList listenerList = new EventListenerList();
    protected boolean busy;
    protected ButtonAnnotation closeButton;
    protected ImageAnnotation busyImage;

    protected DialogAnnotation(Position position) {
        super("", position);
        this.initComponents();
        this.layoutComponents();
        this.setBusy(false);
    }

    public boolean isBusy() {
        return this.busy;
    }

    public void setBusy(boolean busy) {
        this.busy = busy;
        this.getBusyImage().getAttributes().setVisible(busy);
    }

    public ButtonAnnotation getCloseButton() {
        return this.closeButton;
    }

    public ImageAnnotation getBusyImage() {
        return this.busyImage;
    }

    public ActionListener[] getActionListeners() {
        return this.listenerList.getListeners(ActionListener.class);
    }

    public void addActionListener(ActionListener listener) {
        this.listenerList.add(ActionListener.class, listener);
    }

    public void removeActionListener(ActionListener listener) {
        this.listenerList.remove(ActionListener.class, listener);
    }

    //**************************************************************//
    //********************  Action Listener  ***********************//
    //**************************************************************//

    public void actionPerformed(ActionEvent e) {
        // Notify my listeners of the event.
        this.fireActionPerformed(e);
    }

    protected void fireActionPerformed(ActionEvent e) {
        // Guaranteed to return a non-null array
        Object[] listeners = this.listenerList.getListenerList();
        // Process the listeners last to first, notifying
        // those that are interested in this event
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == ActionListener.class) {
                ((ActionListener) listeners[i + 1]).actionPerformed(e);
            }
        }
    }

    //**************************************************************//
    //********************  Annotation Components  *****************//
    //**************************************************************//

    protected void initComponents() {
        this.closeButton = new ButtonAnnotation(CLOSE_IMAGE_PATH, DEPRESSED_MASK_PATH);
        this.closeButton.setActionCommand(Keys.CLOSE);
        this.closeButton.addActionListener(this);
        this.closeButton.setToolTipText(CLOSE_TOOLTIP_TEXT);

        this.busyImage = new BusyImage(BUSY_IMAGE_PATH);
    }

    protected void layoutComponents() {
        AnnotationNullLayout layout = new AnnotationNullLayout();
        this.setLayout(layout);
        this.addChild(this.busyImage);
        this.addChild(this.closeButton);
        layout.setConstraint(this.busyImage, Keys.NORTHWEST);
        layout.setConstraint(this.closeButton, Keys.NORTHEAST);
    }

    protected void setupContainer(Annotation annotation) {
        AnnotationAttributes defaultAttribs = new AnnotationAttributes();
        DialogAnnotation.setupDefaultAttributes(defaultAttribs);
        defaultAttribs.setAdjustWidthToText(Keys.SIZE_FIXED);
        defaultAttribs.setSize(new Dimension(0, 0));

        annotation.setPickEnabled(false);
        annotation.getAttributes().setDefaults(defaultAttribs);
    }

    protected void setupLabel(Annotation annotation) {
        AnnotationAttributes defaultAttribs = new AnnotationAttributes();
        DialogAnnotation.setupDefaultAttributes(defaultAttribs);
        defaultAttribs.setAdjustWidthToText(Keys.SIZE_FIT_TEXT);

        annotation.setPickEnabled(false);
        annotation.getAttributes().setDefaults(defaultAttribs);
    }

    protected static void setupDefaultAttributes(AnnotationAttributes attributes) {
        Color transparentBlack = new Color(0, 0, 0, 0);

        attributes.setBackgroundColor(transparentBlack);
        attributes.setBorderColor(transparentBlack);
        attributes.setBorderWidth(0);
        attributes.setCornerRadius(0);
        attributes.setDrawOffset(new Point(0, 0));
        attributes.setHighlightScale(1);
        attributes.setInsets(new Insets(0, 0, 0, 0));
        attributes.setLeader(Keys.SHAPE_NONE);
    }

    //**************************************************************//
    //********************  Busy Image  ****************************//
    //**************************************************************//

    protected static class BusyImage extends ImageAnnotation {
        protected Angle angle;
        protected Angle increment;
        protected long lastFrameTime;

        public BusyImage(Object imageSource) {
            super(imageSource);
            this.setUseMipmaps(false);

            this.angle = Angle.ZERO;
            this.increment = new Angle(300);
        }

        public Angle getAngle() {
            return this.angle;
        }

        public void setAngle(Angle angle) {
            if (angle == null) {
                String message = Logging.getMessage("nullValue.AngleIsNull");
                Logging.logger().severe(message);
                throw new IllegalArgumentException(message);
            }

            double a = angle.degrees % 360;
            a = (a > 180) ? (a - 360) : (a < -180 ? 360 + a : a);
            this.angle = new Angle(a);
        }

        public Angle getIncrement() {
            return this.increment;
        }

        public void setIncrement(Angle angle) {
            if (angle == null) {
                String message = Logging.getMessage("nullValue.AngleIsNull");
                Logging.logger().severe(message);
                throw new IllegalArgumentException(message);
            }

            this.increment = angle;
        }

        public void drawContent(DrawContext dc, int width, int height, double opacity, Position pickPosition) {
            super.drawContent(dc, width, height, opacity, pickPosition);
            this.updateState(dc);
        }

        protected void transformBackgroundImageCoordsToAnnotationCoords(DrawContext dc, int width, int height,
            WWTexture texture) {
            GL2 gl = dc.getGL2(); // GL initialization checks for GL2 compatibility.

            // Rotate around an axis originating from the center of the image and coming out of the screen.
            double hw = texture.getWidth(dc) / 2.0d;
            double hh = texture.getHeight(dc) / 2.0d;
            gl.glTranslated(hw, hh, 0);
            gl.glRotated(-this.getAngle().degrees, 0, 0, 1);
            gl.glTranslated(-hw, -hh, 0);

            super.transformBackgroundImageCoordsToAnnotationCoords(dc, width, height, texture);
        }

        protected void updateState(DrawContext dc) {
            // Increment the angle by a fixed increment each frame.
            Angle increment = this.getIncrement();
            increment = this.adjustAngleIncrement(dc, increment);
            this.setAngle(this.getAngle().add(increment));

            // Fire a property change to force a repaint.
            dc.getView().firePropertyChange(Keys.VIEW, null, dc.getView());

            // Update the frame time stamp.
            this.lastFrameTime = dc.getFrameTimeStamp();
        }

        protected Angle adjustAngleIncrement(DrawContext dc, Angle unitsPerSecond) {
            long millis = dc.getFrameTimeStamp() - this.lastFrameTime;
            double seconds = millis / 1000.0;
            double degrees = seconds * unitsPerSecond.degrees;

            return new Angle(degrees);
        }
    }
}