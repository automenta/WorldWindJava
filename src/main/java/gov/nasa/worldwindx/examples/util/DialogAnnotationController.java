/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwindx.examples.util;

import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.event.*;
import gov.nasa.worldwind.render.Annotation;
import gov.nasa.worldwind.util.Logging;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.Point;
import java.awt.event.*;
import java.util.logging.Level;

/**
 * @author dcollins
 * @version $Id: DialogAnnotationController.java 1171 2013-02-11 21:45:02Z dcollins $
 */
public abstract class DialogAnnotationController implements ActionListener, SelectListener {
    private final WorldWindow wwd;
    protected ButtonAnnotation toolTipComponent;
    private boolean enabled;
    private DialogAnnotation annotation;

    public DialogAnnotationController(WorldWindow worldWindow, DialogAnnotation annotation) {
        if (worldWindow == null) {
            String message = Logging.getMessage("nullValue.WorldWindow");
            Logging.logger().log(Level.SEVERE, message);
            throw new IllegalArgumentException(message);
        }

        this.wwd = worldWindow;
        this.setAnnotation(annotation);
    }

    public WorldWindow getWorldWindow() {
        return this.wwd;
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public void setEnabled(boolean enabled) {
        if (!this.enabled && enabled) {
            this.doEnable();
        }
        else if (this.enabled && !enabled) {
            this.doDisable();
        }

        this.enabled = enabled;
    }

    protected void doEnable() {
        this.getWorldWindow().addSelectListener(this);
    }

    protected void doDisable() {
        this.getWorldWindow().removeSelectListener(this);
    }

    public DialogAnnotation getAnnotation() {
        return this.annotation;
    }

    public void setAnnotation(DialogAnnotation annotation) {
        if (this.annotation == annotation)
            return;

        if (this.annotation != null) {
            this.annotation.removeActionListener(this);
        }

        this.annotation = annotation;

        if (this.annotation != null) {
            this.annotation.addActionListener(this);
        }
    }

    //**************************************************************//
    //********************  Action Listener  ***********************//
    //**************************************************************//

    public void actionPerformed(ActionEvent e) {
        if (e == null)
            return;

        this.onActionPerformed(e);
    }

    protected void onActionPerformed(ActionEvent e) {
    }

    //**************************************************************//
    //********************  Select Listener  ***********************//
    //**************************************************************//

    public void selected(SelectEvent e) {
        if (e == null)
            return;

        this.onSelected(e);
    }

    protected void onSelected(SelectEvent e) {
        // Forward this event to any ButtonAnnotations under the main annotation.
        this.forwardToButtonAnnotations(this.getAnnotation(), e);

        // Change the cursor type if a ButtonAnnotation is beneath the cursor.
        this.updateCursor(e);

        // Show a tool tip if an ButtonAnnotation is beneath the cursor.
        this.updateToolTip(e);
    }

    protected void forwardToButtonAnnotations(Annotation annotation, SelectEvent e) {
        if (annotation instanceof ButtonAnnotation) {
            ((ButtonAnnotation) annotation).selected(e);
        }

        for (Annotation child : annotation.getChildren()) {
            this.forwardToButtonAnnotations(child, e);
        }
    }

    protected void updateCursor(SelectEvent e) {
        Object topObject = e.getTopObject();
        if (topObject instanceof ButtonAnnotation) {
            this.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }
        else if (topObject instanceof DialogAnnotation) {
            if (((DialogAnnotation) topObject).isBusy()) {
                this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            }
            else {
                this.setCursor(Cursor.getDefaultCursor());
            }
        }
        else {
            this.setCursor(Cursor.getDefaultCursor());
        }
    }

    protected void setCursor(Cursor cursor) {
        if (this.getWorldWindow() instanceof Component) {
            Component component = (Component) this.getWorldWindow();
            if (!component.getCursor().equals(cursor)) {
                component.setCursor(cursor);
            }
        }
    }

    @SuppressWarnings("StringEquality")
    protected void updateToolTip(SelectEvent e) {
        if (e.getEventAction() != SelectEvent.HOVER)
            return;

        Object topObject = e.getTopObject();
        if (topObject instanceof ButtonAnnotation) {
            this.showToolTip(e, (ButtonAnnotation) topObject);
        }
        else {
            this.showToolTip(e, null);
        }
    }

    protected void showToolTip(SelectEvent e, ButtonAnnotation annotation) {
        if (this.toolTipComponent == annotation)
            return;

        if (this.toolTipComponent != null) {
            this.toolTipComponent.setShowToolTip(false);
            this.toolTipComponent.setToolTipPoint(null);
            this.toolTipComponent = null;
        }

        if (annotation != null) {
            Point point = this.getToolTipPoint(e);
            this.toolTipComponent = annotation;
            this.toolTipComponent.setShowToolTip(true);
            this.toolTipComponent.setToolTipPoint(point);
        }

        this.getWorldWindow().redraw();
    }

    protected Point getToolTipPoint(SelectEvent e) {
        Point pickPoint = e.getPickPoint();

        if (e.getSource() instanceof Component) {
            pickPoint = this.glPointFromAwt((Component) e.getSource(), pickPoint);
        }

        return new Point(pickPoint.x, pickPoint.y - 40);
    }

    protected Point glPointFromAwt(Component c, Point p) {
        return new Point(p.x, c.getHeight() - p.y - 1);
    }
}
