/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.util;

import gov.nasa.worldwind.avlist.*;
import gov.nasa.worldwind.geom.LatLon;

import java.awt.geom.*;
import java.beans.*;
import java.util.*;

/**
 * @author dcollins
 * @version $Id: RasterControlPointList.java 1171 2013-02-11 21:45:02Z dcollins $
 */
public class RasterControlPointList extends ArrayList<RasterControlPointList.ControlPoint> implements KV {
    private final KV avList = new KVMap();

    public RasterControlPointList(Collection<? extends ControlPoint> c) {
        super(c);
    }

    public RasterControlPointList() {
    }

    public Object set(String key, Object value) {
        return this.avList.set(key, value);
    }

    public KV setValues(KV avList) {
        return this.avList.setValues(avList);
    }

    public Object get(String key) {
        return this.avList.get(key);
    }

    public Iterable<Object> getValues() {
        return this.avList.getValues();
    }

    public String getStringValue(String key) {
        return this.avList.getStringValue(key);
    }

    public Set<Map.Entry<String, Object>> getEntries() {
        return this.avList.getEntries();
    }

    public boolean hasKey(String key) {
        return this.avList.hasKey(key);
    }

    public Object removeKey(String key) {
        return this.avList.removeKey(key);
    }

    public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        this.avList.addPropertyChangeListener(propertyName, listener);
    }

    public void removePropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        this.avList.removePropertyChangeListener(propertyName, listener);
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        this.avList.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        this.avList.removePropertyChangeListener(listener);
    }

    public void emit(String propertyName, Object oldValue, Object newValue) {
        this.avList.emit(propertyName, oldValue, newValue);
    }

    public void emit(PropertyChangeEvent propertyChangeEvent) {
        this.avList.emit(propertyChangeEvent);
    }

    public KV copy() {
        return this.avList.copy();
    }

    public KV clearList() {
        return this.avList.clearList();
    }

    public static class ControlPoint extends KVMap {
        private final double wx;
        private final double wy;
        private final double rx;
        private final double ry;

        public ControlPoint(double worldX, double worldY, double rasterX, double rasterY) {
            this.wx = worldX;
            this.wy = worldY;
            this.rx = rasterX;
            this.ry = rasterY;
        }

        public Point2D getWorldPoint() {
            return new Point2D.Double(this.wx, this.wy);
        }

        public LatLon getWorldPointAsLatLon() {
            return LatLon.fromDegrees(this.wy, this.wx);
        }

        public Point2D getRasterPoint() {
            return new Point2D.Double(this.rx, this.ry);
        }
    }
}