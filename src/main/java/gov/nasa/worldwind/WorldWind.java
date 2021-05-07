/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */

package gov.nasa.worldwind;

import com.jogamp.opengl.GL;
import gov.nasa.worldwind.cache.*;
import gov.nasa.worldwind.exception.WWRuntimeException;
import gov.nasa.worldwind.formats.tiff.GeotiffImageReaderSpi;
import gov.nasa.worldwind.retrieve.RetrievalService;
import gov.nasa.worldwind.util.*;

import javax.imageio.spi.IIORegistry;
import java.beans.PropertyChangeListener;
import java.util.logging.Level;

/**
 * @author Tom Gaskins
 * @version $Id: WorldWind.java 1171 2013-02-11 21:45:02Z dcollins $
 */
public final class WorldWind {

    public static final String SHUTDOWN_EVENT = "gov.nasa.worldwind.ShutDown";

    // Altitude modes
    public static final int ABSOLUTE = 0;
    public static final int CLAMP_TO_GROUND = 1;
    public static final int RELATIVE_TO_GROUND = 2;
    public static final int CONSTANT = 3;

    // Path types (Don't use these. Use the AVKey versions instead. Only Polyline still uses these.)
    public final static int GREAT_CIRCLE = 0;
    public final static int LINEAR = 1;
    public final static int RHUMB_LINE = 2;

    // Anti-alias hints
    public final static int ANTIALIAS_DONT_CARE = GL.GL_DONT_CARE;
    public final static int ANTIALIAS_FASTEST = GL.GL_FASTEST;
    public final static int ANTIALIAS_NICEST = GL.GL_NICEST;

    private static final WorldWind the = new WorldWind();

    private final WWObjectImpl wwo;
    private final MemoryCacheSet memoryCacheSet;
    private final RetrievalService remoteRetrievalService;
    private final RetrievalService localRetrievalService;
    private final TaskService taskService;
    private final ScheduledTaskService scheduledTaskService;
    private final NetworkStatus networkStatus;
    private final SessionCache sessionCache;

    private WorldWind() // Singleton, prevent public instantiation.
    {
        this.wwo = new WWObjectImpl();
        this.remoteRetrievalService =
//            (RetrievalService) WorldWind.createConfigurationComponent(Keys.RETRIEVAL_SERVICE_CLASS_NAME);
        this.localRetrievalService =
            (RetrievalService) WorldWind.createConfigurationComponent(Keys.RETRIEVAL_SERVICE_CLASS_NAME);
        this.taskService = (TaskService) WorldWind.createConfigurationComponent(Keys.TASK_SERVICE_CLASS_NAME);

        this.memoryCacheSet = (MemoryCacheSet) WorldWind.createConfigurationComponent(Keys.MEMORY_CACHE_SET_CLASS_NAME);
        this.networkStatus = (NetworkStatus) WorldWind.createConfigurationComponent(Keys.NETWORK_STATUS_CLASS_NAME);
        this.sessionCache = (SessionCache) WorldWind.createConfigurationComponent(Keys.SESSION_CACHE_CLASS_NAME);
        this.scheduledTaskService = new ScheduledTaskService();

        // Seems like an unlikely place to load the tiff reader, but do it here nonetheless.
        IIORegistry.getDefaultInstance().registerServiceProvider(GeotiffImageReaderSpi.inst());
    }

    /**
     * Reinitialize WorldWind to its initial ready state. Shut down and restart all WorldWind services and clear all
     * WorldWind memory caches. Cache memory will be released at the next JVM garbage collection.
     * <p>
     * Call this method to reduce WorldWind's current resource usage to its initial, empty state.
     * <p>
     * The state of any open {@link WorldWindow} objects is indeterminate subsequent to invocation of this method. The
     * core WorldWindow objects attempt to shut themselves down cleanly during the call, but their resulting window
     * state is undefined.
     * <p>
     * WorldWind can continue to be used after calling this method.
     */
    public static synchronized void shutDown() {
        WorldWind.the.wwo.emit(WorldWind.SHUTDOWN_EVENT, null, -1);
        WorldWind.the.dispose();
    }

    public static MemoryCacheSet getMemoryCacheSet() {
        return WorldWind.the.memoryCacheSet;
    }

    public static MemoryCache cache(String key) {
        return WorldWind.the.memoryCacheSet.getCache(key);
    }



    public static RetrievalService retrieveRemote() {
        return WorldWind.the.remoteRetrievalService;
    }

    public static RetrievalService retrieveLocal() {
        return WorldWind.the.localRetrievalService;
    }

    public static TaskService tasks() {
        return WorldWind.the.taskService;
    }

    /**
     * Get the scheduled task service. This service can be used to scheduled tasks that execute after a delay, or
     * execute repeatedly.
     *
     * @return the scheduled task service.
     */
    public static ScheduledTaskService scheduler() {
        return WorldWind.the.scheduledTaskService;
    }

    public static NetworkStatus getNetworkStatus() {
        return WorldWind.the.networkStatus;
    }

    public static SessionCache getSessionCache() {
        return WorldWind.the.sessionCache;
    }

    /**
     * Indicates whether WorldWind will attempt to connect to the network to retrieve data or for other reasons.
     *
     * @return <code>true</code> if WorldWind is in off-line mode, <code>false</code> if not.
     * @see NetworkStatus
     */
    public static boolean isOfflineMode() {
        return WorldWind.getNetworkStatus().isOfflineMode();
    }

    /**
     * Indicate whether WorldWind should attempt to connect to the network to retrieve data or for other reasons. The
     * default value for this attribute is <code>false</code>, indicating that the network should be used.
     *
     * @param offlineMode <code>true</code> if WorldWind should use the network, <code>false</code> otherwise
     * @see NetworkStatus
     */
    public static void setOfflineMode(boolean offlineMode) {
        WorldWind.getNetworkStatus().setOfflineMode(offlineMode);
    }

    /**
     * @param className the full name, including package names, of the component to create
     * @return the new component
     * @throws WWRuntimeException       if the <code>Object</code> could not be created
     * @throws IllegalArgumentException if <code>className</code> is null or zero length
     */
    public static Object create(String className) throws WWRuntimeException {
        try {
            return Class.forName(className.trim()).getConstructor().newInstance();
        } catch (Throwable t) {
            Logging.logger().log(Level.SEVERE, "WorldWind.ErrorCreatingComponent", className);
            throw new WWRuntimeException(Logging.getMessage("WorldWind.ErrorCreatingComponent", className), t);
        }
    }

    /**
     * @param classNameKey the key identifying the component
     * @return the new component
     * @throws IllegalStateException    if no name could be found which corresponds to <code>classNameKey</code>
     * @throws IllegalArgumentException if <code>classNameKey</code> is null
     * @throws WWRuntimeException       if the component could not be created
     */
    public static Object createConfigurationComponent(String classNameKey)
        throws IllegalStateException, IllegalArgumentException {
        if (classNameKey.isEmpty()) {
            Logging.logger().severe("nullValue.ClassNameKeyNullZero");
            throw new IllegalArgumentException(Logging.getMessage("nullValue.ClassNameKeyNullZero"));
        }

        String name = Configuration.getStringValue(classNameKey);
        if (name == null) {
            Logging.logger().log(Level.SEVERE, "WorldWind.NoClassNameInConfigurationForKey", classNameKey);
            throw new WWRuntimeException(
                Logging.getMessage("WorldWind.NoClassNameInConfigurationForKey", classNameKey));
        }

        return WorldWind.create(name.trim());

    }

    public static void setValue(String key, Object value) {
        WorldWind.the.wwo.set(key, value);
    }

    public static void setValue(String key, String value) {
        WorldWind.the.wwo.set(key, value);
    }

    public static Object getValue(String key) {
        return WorldWind.the.wwo.get(key);
    }

    public static String getStringValue(String key) {
        return WorldWind.the.wwo.getStringValue(key);
    }

    public static boolean hasKey(String key) {
        return WorldWind.the.wwo.hasKey(key);
    }

    public static void removeKey(String key) {
        WorldWind.the.wwo.removeKey(key);
    }

    public static void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        WorldWind.the.wwo.addPropertyChangeListener(propertyName, listener);
    }

    public static void removePropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        WorldWind.the.wwo.removePropertyChangeListener(propertyName, listener);
    }

    public static void addPropertyChangeListener(PropertyChangeListener listener) {
        WorldWind.the.wwo.addPropertyChangeListener(listener);
    }

    public static void removePropertyChangeListener(PropertyChangeListener listener) {
        WorldWind.the.wwo.removePropertyChangeListener(listener);
    }

    private void dispose() {
        if (this.taskService != null)
            this.taskService.shutdown(true);
        if (this.remoteRetrievalService != null)
            this.remoteRetrievalService.shutdown(true);
        if (this.localRetrievalService != null)
            this.localRetrievalService.shutdown(true);
        if (this.memoryCacheSet != null)
            this.memoryCacheSet.clear();
        if (this.sessionCache != null)
            this.sessionCache.clear();
        if (this.scheduledTaskService != null)
            this.scheduledTaskService.shutdown(true);
    }
}