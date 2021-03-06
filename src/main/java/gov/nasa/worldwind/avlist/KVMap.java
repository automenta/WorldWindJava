/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.avlist;

import gov.nasa.worldwind.util.*;

import java.beans.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

import static java.util.Collections.*;

/**
 * An implementation class for the {@link KV} interface. Classes implementing <code>AVList</code> can subclass or
 * aggregate this class to provide default <code>AVList</code> functionality. This class maintains a hash table of
 * attribute-value pairs.
 * <p>
 * This class implements a notification mechanism for attribute-value changes. The mechanism provides a means for
 * objects to observe attribute changes or queries for certain keys without explicitly monitoring all keys. See {@link
 * PropertyChangeSupport}.
 *
 * @author Tom Gaskins
 * @version $Id: AVListImpl.java 2255 2014-08-22 17:36:32Z tgaskins $
 */
public class KVMap implements KV {
    // Identifies the property change support instance in the avlist
    private static final String PROPERTY_CHANGE_SUPPORT = "avlist.PropertyChangeSupport";
    private static final Object NULLL = new Object();

    // To avoid unnecessary overhead, this object's hash map is created only if needed.
    private volatile Map<String, Object> avList;

    /**
     * Creates an empty attribute-value list.
     */
    public KVMap() {
    }

    /**
     * Constructor enabling aggregation
     *
     * @param sourceBean The bean to be given as the source for any events.
     */
    public KVMap(Object sourceBean) {
        if (sourceBean != null)
            this.set(KVMap.PROPERTY_CHANGE_SUPPORT, new PropertyChangeSupport(sourceBean));
    }

    // Static AVList utilities.
    public static String getStringValue(KV avList, String key, String defaultValue) {
        String v = KVMap.getStringValue(avList, key);
        return v != null ? v : defaultValue;
    }

    public static String getStringValue(KV avList, String key) {
        try {
            return avList.getStringValue(key);
        }
        catch (RuntimeException e) {
            return null;
        }
    }

    public static Integer getIntegerValue(KV avList, String key, Integer defaultValue) {
        Integer v = KVMap.getIntegerValue(avList, key);
        return v != null ? v : defaultValue;
    }

    public static Integer getIntegerValue(KV avList, String key) {
        Object o = avList.get(key);
        if (o == null)
            return null;

        if (o instanceof Integer)
            return (Integer) o;

        String v = KVMap.getStringValue(avList, key);
        if (v == null)
            return null;

        try {
            return Integer.parseInt(v);
        }
        catch (NumberFormatException e) {
            Logging.logger().log(Level.SEVERE, "Configuration.ConversionError", v);
            return null;
        }
    }

    public static Long getLongValue(KV avList, String key, Long defaultValue) {
        Long v = KVMap.getLongValue(avList, key);
        return v != null ? v : defaultValue;
    }

    public static Long getLongValue(KV avList, String key) {
        Object o = avList.get(key);
        if (o == null)
            return null;

        if (o instanceof Long)
            return (Long) o;

        String v = KVMap.getStringValue(avList, key);
        if (v == null)
            return null;

        try {
            return Long.parseLong(v);
        }
        catch (NumberFormatException e) {
            Logging.logger().log(Level.SEVERE, "Configuration.ConversionError", v);
            return null;
        }
    }

    public static Double getDoubleValue(KV avList, String key, Double defaultValue) {
        Double v = KVMap.getDoubleValue(avList, key);
        return v != null ? v : defaultValue;
    }

    public static Double getDoubleValue(KV avList, String key) {
        Object o = avList.get(key);
        if (o == null)
            return null;

        if (o instanceof Double)
            return (Double) o;

        String v = KVMap.getStringValue(avList, key);
        if (v == null)
            return null;

        try {
            return Double.parseDouble(v);
        }
        catch (NumberFormatException e) {
            Logging.logger().log(Level.SEVERE, "Configuration.ConversionError", v);
            return null;
        }
    }

    public static Boolean getBooleanValue(KV avList, String key, Boolean defaultValue) {
        Boolean v = KVMap.getBooleanValue(avList, key);
        return v != null ? v : defaultValue;
    }

    public static Boolean getBooleanValue(KV avList, String key) {
        Object o = avList.get(key);
        if (o == null)
            return null;

        if (o instanceof Boolean)
            return (Boolean) o;

        String v = KVMap.getStringValue(avList, key);
        if (v == null)
            return null;

        try {
            return Boolean.parseBoolean(v);
        }
        catch (NumberFormatException e) {
            Logging.logger().log(Level.SEVERE, "Configuration.ConversionError", v);
            return null;
        }
    }

    private static Map<String, Object> newAvList() {
        return new ConcurrentHashMap<>(1);
    }

    private static Object i(Object value) {
        return value == null ? KVMap.NULLL : value;
    }

    private static Object o(Object value) {
        return value == KVMap.NULLL ? null : value;
    }

    @Override
    public boolean isEmpty() {
        Map<String, Object> v = avList;
        return v == null || avList.isEmpty();
    }

    private Map<String, Object> avList(boolean createIfNone) {
        Map<String, Object> l = avList;
        if (createIfNone && l == null)
            l = (this.avList = KVMap.newAvList());

        return l;
    }

    public Object get(String key) {
        Map<String, Object> l = this.avList;
        return l != null ? KVMap.o(l.get(key)) : null;
    }

    public Iterable<Object> getValues() {
        final Map<String, Object> l = this.avList;
        return l == null ? EMPTY_LIST : l.values();
    }

    public Set<Map.Entry<String, Object>> getEntries() {
        final Map<String, Object> l = this.avList;
        return l == null ? EMPTY_SET : l.entrySet();
    }

    public String getStringValue(String key) {
        Object y = get(key);
        return y != null ? y.toString() : null;
    }

    public Object set(String key, Object value) {

        return this.avList(true).put(key, KVMap.i(value));
    }

    public KV setValues(KV list) {
//        if (list == null) {
//            String message = Logging.getMessage("nullValue.AttributesIsNull");
//            Logging.logger().severe(message);
//            throw new IllegalArgumentException(message);
//        }

        //synchronized(avList) {
        for (Map.Entry<String, Object> entry : list.getEntries()) {
            this.set(entry.getKey(), KVMap.o(entry.getValue()));
        }
        //}

        return this;
    }

    public boolean hasKey(String key) {

        Map<String, Object> l = avList;
        return l != null && l.containsKey(key);
    }

    public Object removeKey(String key) {

        Map<String, Object> l = avList;
        return l != null ? l.remove(key) : null;
    }

    public KV copy() {
        final Map<String, Object> v = this.avList;

        KVMap clone = new KVMap(v!=null ? v.size() : 0);
        if (v != null && !v.isEmpty())
            clone.avList(true).putAll(v);

        return clone;
    }

    public KV clearList() {
        Map<String, Object> v = this.avList;
        if (v != null)
            v.clear();
        return this;
    }

    protected PropertyChangeSupport getCreatedChangeSupport() {
        final Map<String, Object> h = avList(false);
        return h!=null ? (PropertyChangeSupport) h.get(KVMap.PROPERTY_CHANGE_SUPPORT) : null;
    }
    private PropertyChangeSupport getChangeSupport() {
        return (PropertyChangeSupport) avList(true).computeIfAbsent(KVMap.PROPERTY_CHANGE_SUPPORT,
            p -> new PropertyChangeSupport(KVMap.this));
    }

    public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        this.getChangeSupport().addPropertyChangeListener(propertyName, listener);
    }

    public void removePropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        this.getChangeSupport().removePropertyChangeListener(propertyName, listener);
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        this.getChangeSupport().addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        this.getChangeSupport().removePropertyChangeListener(listener);
    }

    public final void emit(PropertyChangeEvent propertyChangeEvent) {
        final PropertyChangeSupport p = this.getCreatedChangeSupport();
        if (p!=null) p.firePropertyChange(propertyChangeEvent);
    }

    public void emit(String propertyName, Object oldValue, Object newValue) {
        final PropertyChangeSupport p = this.getCreatedChangeSupport();
        if (p!=null) p.firePropertyChange(propertyName, oldValue, newValue);
    }

    public void getRestorableStateForAVPair(String key, Object value, RestorableSupport rs,
        RestorableSupport.StateObject context) {
        if (value!=null && !key.equals(KVMap.PROPERTY_CHANGE_SUPPORT))
            rs.addStateValueAsString(context, key, value.toString());
    }
}