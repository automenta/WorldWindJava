/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */

package gov.nasa.worldwind.examples.worldwindow.util.measuretool;

import gov.nasa.worldwind.Keys;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.layers.RenderableLayer;
import gov.nasa.worldwind.render.*;

import java.awt.*;
import java.util.ArrayList;

/**
 * @author tag
 * @version $Id: WWOMeasureToolControlPoints.java 1171 2013-02-11 21:45:02Z dcollins $
 */
public class WWOMeasureToolControlPoints implements WWOMeasureTool.ControlPointList, Renderable {
    protected final WWOMeasureTool measureTool;
    protected final ArrayList<ControlPoint> points = new ArrayList<>();
    protected final AnnotationAttributes controlPointAttributes;

    public WWOMeasureToolControlPoints(WWOMeasureTool measureTool) {
        this.measureTool = measureTool;

        this.controlPointAttributes = new AnnotationAttributes();
        // Define an 8x8 square centered on the screen point
        this.controlPointAttributes.setFrameShape(Keys.SHAPE_RECTANGLE);
        this.controlPointAttributes.setLeader(Keys.SHAPE_NONE);
        this.controlPointAttributes.setAdjustWidthToText(Keys.SIZE_FIXED);
        this.controlPointAttributes.setSize(new Dimension(8, 8));
        this.controlPointAttributes.setDrawOffset(new Point(0, -4));
        this.controlPointAttributes.setInsets(new Insets(0, 0, 0, 0));
        this.controlPointAttributes.setBorderWidth(0);
        this.controlPointAttributes.setCornerRadius(0);
        this.controlPointAttributes.setBackgroundColor(Color.BLUE);    // Normal color
        this.controlPointAttributes.setTextColor(Color.GREEN);         // Highlighted color
        this.controlPointAttributes.setHighlightScale(1.2);
        this.controlPointAttributes.setDistanceMaxScale(1);            // No distance scaling
        this.controlPointAttributes.setDistanceMinScale(1);
        this.controlPointAttributes.setDistanceMinOpacity(1);
    }

    public void addToLayer(RenderableLayer layer) {
        layer.add(this);
    }

    public void removeFromLayer(RenderableLayer layer) {
        layer.remove(this);
    }

    public int size() {
        return this.points.size();
    }

    public WWOMeasureTool.ControlPoint createControlPoint(Position position) {
        return new ControlPoint(position);
    }

    public WWOMeasureTool.ControlPoint get(int index) {
        return this.points.get(index);
    }

    public void add(WWOMeasureTool.ControlPoint controlPoint) {
        this.points.add((ControlPoint) controlPoint);
    }

    public void remove(WWOMeasureTool.ControlPoint controlPoint) {
        this.points.remove(controlPoint);
    }

    public void remove(int index) {
        this.points.remove(index);
    }

    public void clear() {
        this.points.clear();
    }

    public void render(DrawContext dc) {
        for (ControlPoint cp : this.points) {
            cp.render(dc);
        }
    }

    public class ControlPoint extends GlobeAnnotation implements WWOMeasureTool.ControlPoint {
        public ControlPoint(Position position) {
            super("", position, WWOMeasureToolControlPoints.this.controlPointAttributes);
        }

        public WWOMeasureTool getParent() {
            return WWOMeasureToolControlPoints.this.measureTool;
        }

        public void highlight(boolean tf) {
            this.getAttributes().setHighlighted(tf);
            this.getAttributes().setBackgroundColor(tf ? this.getAttributes().getTextColor() : null);
        }
    }
}