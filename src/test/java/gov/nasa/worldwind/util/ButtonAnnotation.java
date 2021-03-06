/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.util;

import com.jogamp.opengl.*;
import gov.nasa.worldwind.event.*;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.render.*;

import javax.swing.event.*;
import java.awt.event.*;

/**
 * @author dcollins
 * @version $Id: ButtonAnnotation.java 1171 2013-02-11 21:45:02Z dcollins $
 */
public class ButtonAnnotation extends ImageAnnotation implements SelectListener {
    // Event listeners.
    protected final EventListenerList listenerList = new EventListenerList();
    protected boolean enabled;
    protected boolean pressed;
    protected String actionCommand;
    protected double disabledOpacity;
    protected WWTexture pressedMaskTexture;

    public ButtonAnnotation(Object imageSource, Object pressedMaskSource) {
        super(imageSource);
        this.setEnableSmoothing(false);
        this.setUseMipmaps(false);
        this.enabled = true;
        this.disabledOpacity = 0.6;
        this.setPressedMaskSource(pressedMaskSource);
    }

    public ButtonAnnotation(Object imageSource) {
        this(imageSource, null);
    }

    public ButtonAnnotation() {
        this(null);
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isPressed() {
        return this.pressed;
    }

    public void setPressed(boolean pressed) {
        this.pressed = pressed;
    }

    public String getActionCommand() {
        return this.actionCommand;
    }

    public void setActionCommand(String actionCommand) {
        this.actionCommand = actionCommand;
    }

    public double getDisabledOpacity() {
        return this.disabledOpacity;
    }

    public void setDisabledOpacity(double opacity) {
        if (opacity < 0 || opacity > 1) {
            String message = Logging.getMessage("generic.ArgumentOutOfRange", "opacity < 0 or opacity > 1");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.disabledOpacity = opacity;
    }

    public Object getPressedMaskSource() {
        return (this.pressedMaskTexture != null) ? this.pressedMaskTexture.getImageSource() : null;
    }

    public void setPressedMaskSource(Object source) {
        this.pressedMaskTexture = null;

        if (source != null) {
            this.pressedMaskTexture = new BasicWWTexture(source, false);
        }
    }

    public WWTexture getPressedMaskTexture() {
        return this.pressedMaskTexture;
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

    protected void setupAnnotationAttributes(Annotation annotation) {
        super.setupAnnotationAttributes(annotation);

        annotation.setPickEnabled(true);
    }

    //**************************************************************//
    //********************  Select Listener  ***********************//
    //**************************************************************//

    @SuppressWarnings("StringEquality")
    public void accept(SelectEvent e) {
        if (e == null)
            return;

        // Ignore hover and rollover events. We're only interested in mouse pressed and mouse clicked events.
        if (e.getEventAction() == SelectEvent.HOVER || e.getEventAction() == SelectEvent.ROLLOVER)
            return;

        if (!this.isEnabled())
            return;

        Object topObject = e.getTopObject();
        if (topObject == this) {
            this.setPressed(ButtonAnnotation.isButtonPressed(e));

            if (ButtonAnnotation.isButtonTrigger(e)) {
                this.onButtonPressed(e);
            }
        }
    }

    @SuppressWarnings("StringEquality")
    protected static boolean isButtonPressed(SelectEvent e) {
        return e.getEventAction() == SelectEvent.LEFT_PRESS;
    }

    @SuppressWarnings("StringEquality")
    protected static boolean isButtonTrigger(SelectEvent e) {
        return e.getEventAction() == SelectEvent.LEFT_CLICK;
    }

    protected void onButtonPressed(SelectEvent e) {
        MouseEvent mouseEvent = e.mouseEvent;
        this.fireActionPerformed(mouseEvent.getID(), mouseEvent.getWhen(), mouseEvent.getModifiersEx());
    }

    //**************************************************************//
    //********************  Action Listener  ***********************//
    //**************************************************************//

    protected void fireActionPerformed(int id, long when, int modifiers) {
        ActionEvent event = null;
        // Guaranteed to return a non-null array
        Object[] listeners = this.listenerList.getListenerList();
        // Process the listeners last to first, notifying
        // those that are interested in this event
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == ActionListener.class) {
                // Lazily create the event:
                if (event == null) {
                    event = new ActionEvent(this, id, this.getActionCommand(), when, modifiers);
                }

                ((ActionListener) listeners[i + 1]).actionPerformed(event);
            }
        }
    }

    //**************************************************************//
    //********************  Rendering  *****************************//
    //**************************************************************//

    public void drawContent(DrawContext dc, int width, int height, double opacity, Position pickPosition) {
        if (!this.isEnabled()) {
            opacity *= this.getDisabledOpacity();
        }

        super.drawContent(dc, width, height, opacity, pickPosition);
        this.drawPressedMask(dc, width, height, opacity, pickPosition);
    }

    protected void drawPressedMask(DrawContext dc, int width, int height, double opacity, Position pickPosition) {
        if (dc.isPickingMode())
            return;

        if (!this.isPressed())
            return;

        this.doDrawPressedMask(dc, width, height, opacity, pickPosition);
    }

    protected void applyBackgroundTextureState(DrawContext dc, int width, int height, double opacity,
        WWTexture texture) {
        super.applyBackgroundTextureState(dc, width, height, opacity, texture);

        // Setup the mask to modulate with the existing fragment color. This will have the effect of multiplying
        // the button depressed mask colors with the button colors.
        if (this.getPressedMaskTexture() == texture) {
            GL2 gl = dc.getGL2(); // GL initialization checks for GL2 compatibility.
            gl.glEnable(GL.GL_BLEND);
            gl.glBlendFunc(GL.GL_ZERO, GL.GL_SRC_COLOR);
            gl.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
        }
    }

    @SuppressWarnings("UnusedDeclaration")
    protected void doDrawPressedMask(DrawContext dc, int width, int height, double opacity, Position pickPosition) {
        WWTexture texture = this.getPressedMaskTexture();
        if (texture == null)
            return;

        // Push state for blend enable, blending function, and current color. We set these OGL states in
        // applyBackgroundTextureState(), which is invoked by doDrawBackgroundTexture().
        GL2 gl = dc.getGL2(); // GL initialization checks for GL2 compatibility.
        OGLStackHandler ogsh = new OGLStackHandler();
        ogsh.pushAttrib(gl, GL2.GL_COLOR_BUFFER_BIT | GL2.GL_CURRENT_BIT);
        try {
            this.doDrawBackgroundTexture(dc, width, height, 1, pickPosition, texture);
        }
        finally {
            ogsh.pop(gl);
        }
    }
}
