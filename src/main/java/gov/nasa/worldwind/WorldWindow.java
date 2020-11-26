/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */

package gov.nasa.worldwind;

import com.jogamp.nativewindow.ScalableSurface;
import com.jogamp.opengl.GLContext;
import gov.nasa.worldwind.avlist.*;
import gov.nasa.worldwind.cache.*;
import gov.nasa.worldwind.event.*;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.pick.PickedObjectList;
import gov.nasa.worldwind.ui.WorldWindowGLDrawable;
import gov.nasa.worldwind.util.*;

import java.beans.*;
import java.util.*;

/**
 * The top-level interface common to all toolkit-specific WorldWind windows.
 *
 * @author Tom Gaskins
 * @version $Id: WorldWindow.java 2047 2014-06-06 22:48:33Z tgaskins $
 */
public interface WorldWindow extends AVList, PropertyChangeListener {
    long FALLBACK_TEXTURE_CACHE_SIZE = 60000000;

    /**
     * Configures JOGL's surface pixel scaling on the specified
     * <code>ScalableSurface</code> to ensure backward compatibility with
     * WorldWind applications developed prior to JOGL pixel scaling's introduction.This method is used by
     * <code>GLCanvas</code> and
     * <code>GLJPanel</code> to effectively disable JOGL's surface pixel scaling
     * by requesting a 1:1 scale.<p> Since v2.2.0, JOGL defaults to using high-dpi pixel scales where possible. This
     * causes WorldWind screen elements such as placemarks, the compass, the world map, the view controls, and the scale
     * bar (plus many more) to appear smaller than they are intended to on screen. The high-dpi default also has the
     * effect of degrading WorldWind rendering performance.
     *
     * @param surface The surface to configure.
     */
    static void configureIdentityPixelScale(ScalableSurface surface) {
        if (surface == null) {
            String message = Logging.getMessage("nullValue.SurfaceIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        float[] identityScale = new float[] {ScalableSurface.IDENTITY_PIXELSCALE, ScalableSurface.IDENTITY_PIXELSCALE};
        surface.setSurfaceScale(identityScale);
    }

    static GpuResourceCache createGpuResourceCache() {
        long cacheSize = Configuration.getLongValue(AVKey.TEXTURE_CACHE_SIZE, WorldWindow.FALLBACK_TEXTURE_CACHE_SIZE);
        return new BasicGpuResourceCache((long) (0.8 * cacheSize), cacheSize);
    }


    /**
     * Constructs and attaches the {@link View} for this <code>WorldWindow</code>.
     */
    default void createView() {
        this.setView((View) WorldWind.createConfigurationComponent(AVKey.VIEW_CLASS_NAME));
    }

    /**
     * Constructs and attaches the {@link InputHandler} for this <code>WorldWindow</code>.
     */
    default void createDefaultInputHandler() {
        this.setInputHandler((InputHandler) WorldWind.createConfigurationComponent(AVKey.INPUT_HANDLER_CLASS_NAME));
    }

    /**
     * Causes a repaint event to be enqueued with the window system for this WorldWindow. The repaint will occur at the
     * window system's discretion, within the window system toolkit's event loop, and on the thread of that loop. This
     * is the preferred method for requesting a repaint of the WorldWindow.
     */
    void redraw();

    /**
     * Returns the {@link GLContext} associated with this <code>WorldWindow</code>.
     *
     * @return the <code>GLContext</code> associated with this window. May be null.
     */
    GLContext getContext();

    /**
     * Indicates whether the GPU resource cache is reinitialized when this window is reinitialized.
     *
     * @return <code>true</code> if reinitialization is enabled, otherwise <code>false</code>.
     */
    default boolean isEnableGpuCacheReinitialization() {
        return this.wwd().isEnableGpuCacheReinitialization();
    }

    /**
     * Specifies whether to reinitialize the GPU resource cache when this window is reinitialized. A value of
     * <code>true</code> indicates that the GPU resource cache this window is using should be cleared when its init()
     * method is called, typically when re-parented. Set this to <code>false</code> when this window is sharing context
     * with other windows and is likely to be re-parented. It prevents the flashing caused by clearing and re-populating
     * the GPU resource cache during re-parenting. The default value is <code>true</code>.
     *
     * @param enableGpuCacheReinitialization <code>true</code> to enable reinitialization, otherwise
     *                                       <code>false</code>.
     */
    default void setEnableGpuCacheReinitialization(boolean enableGpuCacheReinitialization) {
        this.wwd().setEnableGpuCacheReinitialization(enableGpuCacheReinitialization);
    }

    WorldWindowGLDrawable wwd();

    default void propertyChange(PropertyChangeEvent evt) {
        if (this.wwd() == evt.getSource())
            this.firePropertyChange(evt);

        //noinspection StringEquality
        if (evt.getPropertyName() == WorldWind.SHUTDOWN_EVENT)
            this.shutdown();
    }

    /**
     * Causes resources used by the WorldWindow to be freed. The WorldWindow cannot be used once this method is called.
     */
    default void shutdown() {
        WorldWind.removePropertyChangeListener(WorldWind.SHUTDOWN_EVENT, this);
        this.wwd().shutdown();
    }

    default InputHandler getInputHandler() {
        return this.wwd().getInputHandler();
    }

    /**
     * Sets the input handler to use for this instance.
     *
     * @param inputHandler The input handler to use for this WorldWindow. May by <code>null</code> if <code>null</code>
     *                     is specified, the current input handler, if any, is disassociated with the WorldWindow.
     */
    default void setInputHandler(InputHandler inputHandler) {
        if (this.wwd().getInputHandler() != null)
            this.wwd().getInputHandler().setEventSource(null); // remove this window as a source of events

        this.wwd().setInputHandler(inputHandler != null ? inputHandler : new NoOpInputHandler());
        if (inputHandler != null)
            inputHandler.setEventSource(this);
    }

    default SceneController getSceneController() {
        return this.wwd().getSceneController();
    }

    /**
     * Specifies a new scene controller for the window. The caller is responsible for populating the new scene
     * controller with a {@link View}, {@link Model} and any desired per-frame statistics keys.
     *
     * @param sceneController the new scene controller.
     * @see SceneController#setView(View)
     * @see SceneController#setModel(Model)
     * @see SceneController#setPerFrameStatisticsKeys(Set)
     */
    default void setSceneController(SceneController sceneController) {
        this.wwd().setSceneController(sceneController);
    }

    /**
     * Returns the GPU Resource used by this WorldWindow. This method is for internal use only.
     * <p>
     * Note: Applications do not need to interact with the GPU resource cache. It is self managed. Modifying it in any
     * way will cause significant problems such as excessive memory usage or application crashes. The only reason to use
     * the GPU resource cache is to request management of GPU resources within implementations of shapes or layers. And
     * then access should be only through the draw context only.
     *
     * @return The GPU Resource cache used by this WorldWindow.
     */
    default GpuResourceCache getGpuResourceCache() {
        return this.wwd().getGpuResourceCache();
    }

    default void redrawNow() {
        this.wwd().redrawNow();
    }

    default Model model() {
        return this.wwd().model();
    }

    default View view() {
        return this.wwd().view();
    }

    /**
     * Sets the view to use when displaying this window's model. If <code>null</code> is specified for the view, the
     * current view, if any, is disassociated with the window.
     *
     * @param view the view to use to display this window's model. May be null.
     */
    default void setView(View view) {
        // null views are permissible
        if (view != null)
            this.wwd().setView(view);
    }

    default void setModel(Model model) {
        this.wwd().setModel(model); // null models are permissible
    }

    default void setModelAndView(Model model, View view) {   // null models/views are permissible
        this.setModel(model);
        this.setView(view);
    }

    default void addRenderingListener(RenderingListener listener) {
        this.wwd().addRenderingListener(listener);
    }

    default void removeRenderingListener(RenderingListener listener) {
        this.wwd().removeRenderingListener(listener);
    }

    default void addSelectListener(SelectListener listener) {
        this.wwd().getInputHandler().addSelectListener(listener);
        this.wwd().addSelectListener(listener);
    }

    default void removeSelectListener(SelectListener listener) {
        this.wwd().getInputHandler().removeSelectListener(listener);
        this.wwd().removeSelectListener(listener);
    }

    default void addPositionListener(PositionListener listener) {
        this.wwd().addPositionListener(listener);
    }

    default void removePositionListener(PositionListener listener) {
        this.wwd().removePositionListener(listener);
    }

    /**
     * Adds an exception listener to this WorldWindow. Exception listeners are called when an exception or other
     * critical event occurs during drawable initialization or during rendering.
     *
     * @param listener the The exception listener to add.
     */
    default void addRenderingExceptionListener(RenderingExceptionListener listener) {
        this.wwd().addRenderingExceptionListener(listener);
    }

    default void removeRenderingExceptionListener(RenderingExceptionListener listener) {
        this.wwd().removeRenderingExceptionListener(listener);
    }

    default Position getCurrentPosition() {
        return this.wwd().getCurrentPosition();
    }

    default PickedObjectList getObjectsAtCurrentPosition() {
        return this.wwd().getSceneController() != null ? this.wwd().getSceneController().getPickedObjectList() : null;
    }

    default PickedObjectList getObjectsInSelectionBox() {
        return this.wwd().getSceneController() != null ? this.wwd().getSceneController().getObjectsInPickRectangle()
            : null;
    }

    default Object setValue(String key, Object value) {
        return this.wwd().setValue(key, value);
    }

    default AVList setValues(AVList avList) {
        return this.wwd().setValues(avList);
    }

    default Object getValue(String key) {
        return this.wwd().getValue(key);
    }

    default Iterable<Object> getValues() {
        return this.wwd().getValues();
    }

    default Set<Map.Entry<String, Object>> getEntries() {
        return this.wwd().getEntries();
    }

    default String getStringValue(String key) {
        return this.wwd().getStringValue(key);
    }

    default boolean hasKey(String key) {
        return this.wwd().hasKey(key);
    }

    default Object removeKey(String key) {
        return this.wwd().removeKey(key);
    }



    default void firePropertyChange(PropertyChangeEvent propertyChangeEvent) {
        this.wwd().firePropertyChange(propertyChangeEvent);
    }

    default AVList copy() {
        return this.wwd().copy();
    }

    default AVList clearList() {
        return this.wwd().clearList();
    }

    /**
     * Returns the active per-frame performance statistics such as number of tiles drawn in the most recent frame.
     *
     * @return The keys and values of the active per-frame statistics.
     */
    default Collection<PerformanceStatistic> getPerFrameStatistics() {
        return this.wwd().getPerFrameStatistics();
    }

    default void setPerFrameStatisticsKeys(Set<String> keys) {
        this.wwd().setPerFrameStatisticsKeys(keys);
    }
}
