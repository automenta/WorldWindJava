/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.avlist;

////.*;
import gov.nasa.worldwind.util.*;

import java.beans.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

import static java.util.Collections.*;

/**
 *
 * An implementation class for the {@link AVList} interface. Classes implementing <code>AVList</code> can subclass or
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
public class AVListImpl implements AVList {
    // Identifies the property change support instance in the avlist
    private static final String PROPERTY_CHANGE_SUPPORT = "avlist.PropertyChangeSupport";

    // To avoid unnecessary overhead, this object's hash map is created only if needed.
    private volatile Map<String, Object> avList;

    /**
     * Creates an empty attribute-value list.
     */
    public AVListImpl() {
    }

    /**
     * Constructor enabling aggregation
     *
     * @param sourceBean The bean to be given as the source for any events.
     */
    public AVListImpl(Object sourceBean) {
        if (sourceBean != null)
            this.setValue(PROPERTY_CHANGE_SUPPORT, new PropertyChangeSupport(sourceBean));
    }

    // Static AVList utilities.
    public static String getStringValue(AVList avList, String key, String defaultValue) {
        String v = getStringValue(avList, key);
        return v != null ? v : defaultValue;
    }

    public static String getStringValue(AVList avList, String key) {
        try {
            return avList.getStringValue(key);
        } catch (Exception e) {
            return null;
        }
    }

    public static Integer getIntegerValue(AVList avList, String key, Integer defaultValue) {
        Integer v = getIntegerValue(avList, key);
        return v != null ? v : defaultValue;
    }

    public static Integer getIntegerValue(AVList avList, String key) {
        Object o = avList.getValue(key);
        if (o == null)
            return null;

        if (o instanceof Integer)
            return (Integer) o;

        String v = getStringValue(avList, key);
        if (v == null)
            return null;

        try {
            return Integer.parseInt(v);
        } catch (NumberFormatException e) {
            Logging.logger().log(Level.SEVERE, "Configuration.ConversionError", v);
            return null;
        }
    }

    public static Long getLongValue(AVList avList, String key, Long defaultValue) {
        Long v = getLongValue(avList, key);
        return v != null ? v : defaultValue;
    }

    public static Long getLongValue(AVList avList, String key) {
        Object o = avList.getValue(key);
        if (o == null)
            return null;

        if (o instanceof Long)
            return (Long) o;

        String v = getStringValue(avList, key);
        if (v == null)
            return null;

        try {
            return Long.parseLong(v);
        } catch (NumberFormatException e) {
            Logging.logger().log(Level.SEVERE, "Configuration.ConversionError", v);
            return null;
        }
    }

    public static Double getDoubleValue(AVList avList, String key, Double defaultValue) {
        Double v = getDoubleValue(avList, key);
        return v != null ? v : defaultValue;
    }

    public static Double getDoubleValue(AVList avList, String key) {
        Object o = avList.getValue(key);
        if (o == null)
            return null;

        if (o instanceof Double)
            return (Double) o;

        String v = getStringValue(avList, key);
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

    public static Boolean getBooleanValue(AVList avList, String key, Boolean defaultValue) {
        Boolean v = getBooleanValue(avList, key);
        return v != null ? v : defaultValue;
    }

    public static Boolean getBooleanValue(AVList avList, String key) {
        Object o = avList.getValue(key);
        if (o == null)
            return null;

        if (o instanceof Boolean)
            return (Boolean) o;

        String v = getStringValue(avList, key);
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

    private Map<String, Object> newAvList() {
        return new ConcurrentHashMap<>(1);
               //Collections.synchronizedMap(new HashMap<>(1));
    }

    @Override
    public boolean isEmpty() {
        Map<String, Object> v = avList;
        return v == null || avList.isEmpty();
    }

    private Map<String, Object> avList(boolean createIfNone) {
        Map<String, Object> l = avList;
        if (createIfNone && l==null)
             l = (this.avList = this.newAvList());

        return l;
    }

    public Object getValue(String key) {
//        if (key == null) {
//            String message = Logging.getMessage("nullValue.AttributeKeyIsNull");
//            Logging.logger().severe(message);
//            throw new IllegalArgumentException(message);
//        }
        Map<String, Object> l = this.avList;
        return l!=null ? o(l.get(key)) : null;
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
//        if (key == null) {
//            String msg = Logging.getMessage("nullValue.AttributeKeyIsNull");
//            Logging.logger().severe(msg);
//            throw new IllegalStateException(msg);
//        }

        Object y = getValue(key);
        return y != null ? y.toString() : null;

//        try {
//            Object value = this.getValue(key);
//            return value != null ? value.toString() : null;
//        }
//        catch (ClassCastException e) {
//            String msg = Logging.getMessage("AVAAccessibleImpl.AttributeValueForKeyIsNotAString", key);
//            Logging.logger().severe(msg);
//            throw new WWRuntimeException(msg, e);
//        }
    }

    private static final Object NULLL = new Object();

    public Object setValue(String key, Object value) {
//        if (key == null) {
//            String message = Logging.getMessage("nullValue.AttributeKeyIsNull");
//            Logging.logger().severe(message);
//            throw new IllegalArgumentException(message);
//        }

        return this.avList(true).put(key, i(value));
    }

    private static Object i(Object value) {
        return value == null ? NULLL : value;
    }

    private static Object o(Object value) {
        return value == NULLL ? null : value;
    }


    public AVList setValues(AVList list) {
//        if (list == null) {
//            String message = Logging.getMessage("nullValue.AttributesIsNull");
//            Logging.logger().severe(message);
//            throw new IllegalArgumentException(message);
//        }

        //synchronized(avList) {
            Set<Map.Entry<String, Object>> entries = list.getEntries();
            for (Map.Entry<String, Object> entry : entries)
                this.setValue(entry.getKey(), o(entry.getValue()));
        //}

        return this;
    }

    public boolean hasKey(String key) {
//        if (key == null) {
//            String message = Logging.getMessage("nullValue.KeyIsNull");
//            Logging.logger().severe(message);
//            throw new IllegalArgumentException(message);
//        }

        Map<String, Object> l = avList;
        return l!=null && l.containsKey(key);
    }

    public Object removeKey(String key) {
//        if (key == null) {
//            String message = Logging.getMessage("nullValue.KeyIsNull");
//            Logging.logger().severe(message);
//            throw new IllegalArgumentException(message);
//        }

        Map<String, Object> l = avList;
        return l!=null ? l.remove(key) : null;
    }

    public AVList copy() {
        AVListImpl clone = new AVListImpl();

        final Map<String, Object> v = this.avList;
        if (v != null)
            clone.avList(true).putAll(v);

        return clone;
    }

    public AVList clearList() {
        Map<String, Object> v = this.avList;
        if (v!=null) v.clear();
        return this;
    }

    protected PropertyChangeSupport getChangeSupport() {
        return (PropertyChangeSupport) avList(true).computeIfAbsent(PROPERTY_CHANGE_SUPPORT, p -> new PropertyChangeSupport(AVListImpl.this));
    }

    public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
//        if (propertyName == null) {
//            String msg = Logging.getMessage("nullValue.PropertyNameIsNull");
//            Logging.logger().severe(msg);
//            throw new IllegalArgumentException(msg);
//        }
//        if (listener == null) {
//            String msg = Logging.getMessage("nullValue.ListenerIsNull");
//            Logging.logger().severe(msg);
//            throw new IllegalArgumentException(msg);
//        }
        this.getChangeSupport().addPropertyChangeListener(propertyName, listener);
    }

    public void removePropertyChangeListener(String propertyName, PropertyChangeListener listener) {
//        if (propertyName == null) {
//            String msg = Logging.getMessage("nullValue.PropertyNameIsNull");
//            Logging.logger().severe(msg);
//            throw new IllegalArgumentException(msg);
//        }
//        if (listener == null) {
//            String msg = Logging.getMessage("nullValue.ListenerIsNull");
//            Logging.logger().severe(msg);
//            throw new IllegalArgumentException(msg);
//        }
        this.getChangeSupport().removePropertyChangeListener(propertyName, listener);
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
//        if (listener == null) {
//            String msg = Logging.getMessage("nullValue.ListenerIsNull");
//            Logging.logger().severe(msg);
//            throw new IllegalArgumentException(msg);
//        }
        this.getChangeSupport().addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
//        if (listener == null) {
//            String msg = Logging.getMessage("nullValue.ListenerIsNull");
//            Logging.logger().severe(msg);
//            throw new IllegalArgumentException(msg);
//        }
        this.getChangeSupport().removePropertyChangeListener(listener);
    }

    public void firePropertyChange(PropertyChangeEvent propertyChangeEvent) {
//        if (propertyChangeEvent == null) {
//            String msg = Logging.getMessage("nullValue.PropertyChangeEventIsNull");
//            Logging.logger().severe(msg);
//            throw new IllegalArgumentException(msg);
//        }
        this.getChangeSupport().firePropertyChange(propertyChangeEvent);
    }

    public void firePropertyChange(String propertyName, Object oldValue, Object newValue) {
//        if (propertyName == null) {
//            String msg = Logging.getMessage("nullValue.PropertyNameIsNull");
//            Logging.logger().severe(msg);
//            throw new IllegalArgumentException(msg);
//        }
        this.getChangeSupport().firePropertyChange(propertyName, oldValue, newValue);
    }

    public void getRestorableStateForAVPair(String key, Object value, RestorableSupport rs,
        RestorableSupport.StateObject context) {
        if (value == null)
            return;

        if (key.equals(PROPERTY_CHANGE_SUPPORT))
            return;

//        if (rs == null) {
//            String message = Logging.getMessage("nullValue.RestorableStateIsNull");
//            Logging.logger().severe(message);
//            throw new IllegalArgumentException(message);
//        }

        rs.addStateValueAsString(context, key, value.toString());
    }
}
