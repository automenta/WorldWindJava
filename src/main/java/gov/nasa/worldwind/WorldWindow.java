/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */

package gov.nasa.worldwind;

import com.jogamp.nativewindow.ScalableSurface;
import gov.nasa.worldwind.avlist.KV;
import gov.nasa.worldwind.cache.GpuResourceCache;
import gov.nasa.worldwind.event.*;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.layers.placename.PlaceNameLayer;
import gov.nasa.worldwind.layers.tool.*;
import gov.nasa.worldwind.pick.PickedObjectList;
import gov.nasa.worldwind.util.*;
import gov.nasa.worldwind.video.*;

import java.beans.*;
import java.util.*;

import static gov.nasa.worldwind.Keys.*;

/**
 * The top-level interface common to all toolkit-specific WorldWind windows.
 *
 * @author Tom Gaskins
 * @version $Id: WorldWindow.java 2047 2014-06-06 22:48:33Z tgaskins $
 */
public interface WorldWindow extends KV, PropertyChangeListener {

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
        surface.setSurfaceScale(new float[] {
            ScalableSurface.IDENTITY_PIXELSCALE, ScalableSurface.IDENTITY_PIXELSCALE
        });
    }

    static void insertBeforeCompass(WorldWindow wwd, Layer layer) {
        // Insert the layer into the layer list just before the compass.
        int compassPosition = 0;
        LayerList layers = wwd.model().layers();
        for (Layer l : layers) {
            if (l instanceof CompassLayer) {
                compassPosition = layers.indexOf(l);
            }
        }
        layers.add(compassPosition, layer);
    }

    static void insertBeforePlacenames(WorldWindow wwd, Layer layer) {
        // Insert the layer into the layer list just before the placenames.
        int compassPosition = 0;
        LayerList layers = wwd.model().layers();
        for (Layer l : layers) {
            if (l instanceof PlaceNameLayer) {
                compassPosition = layers.indexOf(l);
            }
        }
        layers.add(compassPosition, layer);
    }

    static void insertAfterPlacenames(WorldWindow wwd, Layer layer) {
        // Insert the layer into the layer list just after the placenames.
        int compassPosition = 0;
        LayerList layers = wwd.model().layers();
        for (Layer l : layers) {
            if (l instanceof PlaceNameLayer) {
                compassPosition = layers.indexOf(l);
            }
        }
        layers.add(compassPosition + 1, layer);
    }

    /**
     * Constructs and attaches the {@link View} for this <code>WorldWindow</code>.
     */
    static void createView(WorldWindow w) {
        w.setView((View) WorldWind.createConfigurationComponent(VIEW_CLASS_NAME));
        w.setInput((InputHandler) WorldWind.createConfigurationComponent(INPUT_HANDLER_CLASS_NAME));

        WorldWind.addPropertyChangeListener(WorldWind.SHUTDOWN_EVENT, w);

        // Setup a select listener for the worldmap click-and-go feature
        w.addSelectListener(new ClickAndGoSelectListener(w, WorldMapLayer.class));

        // Add controllers to manage highlighting and tool tips.
        var toolTipController = new ToolTipController(w, DISPLAY_NAME, null);
        var highlightController = new HighlightController(w, SelectEvent.ROLLOVER);
    }

    /**
     * Causes a repaint event to be enqueued with the window system for this WorldWindow. The repaint will occur at the
     * window system's discretion, within the window system toolkit's event loop, and on the thread of that loop. This
     * is the preferred method for requesting a repaint of the WorldWindow.
     */
    void redraw();

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

    default InputHandler input() {
        return this.wwd().input();
    }

    /**
     * Sets the input handler to use for this instance.
     *
     * @param inputHandler The input handler to use for this WorldWindow. May by <code>null</code> if <code>null</code>
     *                     is specified, the current input handler, if any, is disassociated with the WorldWindow.
     */
    default void setInput(InputHandler inputHandler) {
        if (this.wwd().input() != null)
            this.wwd().input().setEventSource(null); // remove this window as a source of events

        this.wwd().setInput(inputHandler != null ? inputHandler : new NoOpInputHandler());
        if (inputHandler != null)
            inputHandler.setEventSource(this);
    }

    default SceneController sceneControl() {
        return this.wwd().sceneControl();
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
    default void setSceneControl(SceneController sceneController) {
        this.wwd().setSceneControl(sceneController);
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
    default GpuResourceCache resourceCache() {
        return this.wwd().resourceCache();
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

    default void addRenderingListener(RenderingListener listener) {
        this.wwd().addRenderingListener(listener);
    }

    default void removeRenderingListener(RenderingListener listener) {
        this.wwd().removeRenderingListener(listener);
    }

    default void addSelectListener(SelectListener listener) {
        this.wwd().addSelectListener(listener);
    }

    default void removeSelectListener(SelectListener listener) {
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

    default Position position() {
        return this.wwd().position();
    }

    default PickedObjectList objectsAtPosition() {
        return this.wwd().sceneControl() != null ? this.wwd().sceneControl().getPickedObjectList() : null;
    }

    default PickedObjectList objectsInSelectionBox() {
        return this.wwd().sceneControl() != null ? this.wwd().sceneControl().getObjectsInPickRectangle()
            : null;
    }

    default Object set(String key, Object value) {
        return this.wwd().set(key, value);
    }

    default KV setValues(KV avList) {
        return this.wwd().setValues(avList);
    }

    default Object get(String key) {
        return this.wwd().get(key);
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

    default KV copy() {
        return this.wwd().copy();
    }

    default KV clearList() {
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

    /**
     * start supplying events to the handler
     */
    default void startEvents(InputHandler h) {
    }

    /**
     * stop supplying events to the handler
     */
    default void stopEvents(InputHandler h) {
    }
}