/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */

package gov.nasa.worldwind.examples.worldwindow.core;

import gov.nasa.worldwind.examples.worldwindow.util.Util;
import gov.nasa.worldwind.util.WWUtil;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * @author tag
 * @version $Id: Registry.java 1171 2013-02-11 21:45:02Z dcollins $
 */
public class Registry {
    private final Map<String, Object> registeredObjects = new ConcurrentHashMap<>();

    /**
     * @param className the full name, including package names, of the component to create
     * @return the new component
     * @throws RuntimeException         if the <code>Object</code> could not be created
     * @throws IllegalArgumentException if <code>className</code> is null or zero length
     */
    public Object createObject(String className) {
        if (className == null || className.isEmpty()) {
            String msg = "Class name is null or zero length";
            Util.getLogger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        try {
            return Class.forName(className.trim()).getConstructor().newInstance();
        }
        catch (Exception e) {
            String msg = "Exception creating object " + className;
            Util.getLogger().log(Level.SEVERE, msg, e);
            throw new RuntimeException(msg, e);
        }
        catch (Throwable t) {
            String msg = "Error creating object " + className;
            Util.getLogger().log(Level.SEVERE, msg, t);
            throw new RuntimeException(msg, t);
        }
    }

    public Object createRegistryObject(Object classOrName)
        throws ClassNotFoundException {
        if (classOrName == null) {
            String msg = "Class or class name is null";
            Util.getLogger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        if (!(classOrName instanceof Class || classOrName instanceof String)) {
            String msg = "Class or class name is not Class or String type";
            Util.getLogger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        if (classOrName instanceof String && ((String) classOrName).isEmpty()) {
            String msg = "Class name is null or zero length";
            Util.getLogger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        Class<?> c = classOrName instanceof Class ? (Class) classOrName : Class.forName(((String) classOrName).trim());
        String className = c.getName();

        try {
            // Create self-registering object, else non-self-registering object
            return c.getConstructor(this.getClass()).newInstance(this);
        }
        catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException | InstantiationException e) {
            return createObject(className);
        }
        catch (Exception e) {
            String msg = "Exception creating object " + className;
            Util.getLogger().log(Level.SEVERE, msg, className);
            throw new RuntimeException(msg, e);
        }
        catch (Throwable t) {
            String msg = "Error creating object " + className;
            Util.getLogger().log(Level.SEVERE, msg, className);
            throw new RuntimeException(msg, t);
        }
    }

    public Object createAndRegisterObject(String objectID, Object classOrName)
        throws ClassNotFoundException {
        if (WWUtil.isEmpty(objectID)) {
            String msg = String.format("Object ID %s is null or zero length", objectID);
            Util.getLogger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        if (classOrName == null || (classOrName instanceof String && WWUtil.isEmpty(classOrName))) {
            String msg = String.format("Class name %s for feature %s is zero length", classOrName, objectID);
            Util.getLogger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        registerObject(objectID, createRegistryObject(classOrName));

        return getRegisteredObject(objectID);
    }

    public synchronized Object getRegisteredObject(String objectID) {
        return this.registeredObjects.get(objectID);
    }

    public synchronized Object registerObject(String objectID, Object o) {
        if (objectID != null)
            this.registeredObjects.put(objectID, o);

        return o;
    }

    public Collection<Object> getObjects() {
        return this.registeredObjects.values();
    }

    public Object[] getObjectsOfType(String className) {
        List<Object> list = new ArrayList<>();

        try {
            Class classClass = Class.forName(className);
            for (Map.Entry<String, Object> entry : this.registeredObjects.entrySet()) {
                if (entry.getValue() == null)
                    continue;

                if (classClass.isInstance(entry.getValue())) {
                    list.add(entry.getValue());
                }
                else if (entry.getValue() instanceof Class) {
                    // TODO: also check superclasses for instance of type. See java.lang.Class.getEnclosingClass and
                    // search recursively.
                    if (implementsInterface(classClass, (Class) entry.getValue())) {
                        try {
                            list.add(this.createAndRegisterObject(entry.getKey(), entry.getValue()));
                        }
                        catch (Exception e) {
                            // continue
                        }
                    }
                }
            }
        }
        catch (ClassNotFoundException e) {
            Util.getLogger().log(Level.SEVERE,
                "No class found for class name " + (className), e);
        }

        return list.toArray();
    }

    protected boolean implementsInterface(Class interfaceClass, Class compareClass) {
        Class<?>[] interfaces = compareClass.getInterfaces();
        for (Class i : interfaces) {
            if (i == interfaceClass)
                return true;
        }

        return false;
    }
}
