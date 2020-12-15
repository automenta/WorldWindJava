/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */

package gov.nasa.worldwind.video;

import com.jogamp.nativewindow.ScalableSurface;
import gov.nasa.worldwind.*;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.cache.*;
import gov.nasa.worldwind.event.*;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.pick.*;
import gov.nasa.worldwind.util.*;
import gov.nasa.worldwind.video.awt.AWTInputHandler;

import javax.swing.event.*;
import java.util.*;
import java.util.logging.Level;

/**
 * An implementation class for the {@link WorldWindow} interface. Classes implementing <code>WorldWindow</code> can
 * subclass or aggregate this object to provide default <code>WorldWindow</code> functionality.
 *
 * @author Tom Gaskins
 * @version $Id: WorldWindowImpl.java 1855 2014-02-28 23:01:02Z tgaskins $
 */
public abstract class WorldWindowImpl extends WWObjectImpl implements WorldWindow {
    private static final long FALLBACK_TEXTURE_CACHE_SIZE = 60000000;
    private EventListenerList eventListeners = null;
    protected GpuResourceCache gpuResourceCache;
    protected SceneController sceneController;
    private InputHandler inputHandler;

    protected WorldWindowImpl() {
        this.sceneController = (SceneController) WorldWind.createConfigurationComponent(
            AVKey.SCENE_CONTROLLER_CLASS_NAME);

        // Set up to initiate a repaint whenever a file is retrieved and added to the local file store.
        WorldWind.store().addPropertyChangeListener(this);
    }

    public static GpuResourceCache createGpuResourceCache() {
        long cacheSize = Configuration.getLongValue(AVKey.TEXTURE_CACHE_SIZE, FALLBACK_TEXTURE_CACHE_SIZE);
        return new BasicGpuResourceCache((long) (0.8 * cacheSize), cacheSize);
    }

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
    public static void configureIdentityPixelScale(ScalableSurface surface) {
        if (surface == null) {
            String message = Logging.getMessage("nullValue.SurfaceIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        float[] identityScale = new float[] {ScalableSurface.IDENTITY_PIXELSCALE, ScalableSurface.IDENTITY_PIXELSCALE};
        surface.setSurfaceScale(identityScale);
    }

    /**
     * Causes resources used by the WorldWindow to be freed. The WorldWindow cannot be used once this method is called.
     * An OpenGL context for the window must be current.
     */
    public void shutdown() {
        WorldWind.store().removePropertyChangeListener(this);

        if (this.inputHandler != null) {
            this.inputHandler.dispose();
            this.inputHandler = new NoOpInputHandler();
        }

        // Clear the texture cache
        if (this.resourceCache() != null)
            this.resourceCache().clear();

        // Dispose all the layers //  TODO: Need per-window dispose for layers
        if (this.model() != null && this.model().getLayers() != null) {
            for (Layer layer : this.model().getLayers()) {
                try {
                    layer.dispose();
                }
                catch (Exception e) {
                    Logging.logger().log(Level.SEVERE, Logging.getMessage(
                        "WorldWindowGLCanvas.ExceptionWhileShuttingDownWorldWindow"), e);
                }
            }
        }

        SceneController sc = this.sceneControl();
        if (sc != null)
            sc.dispose();
    }

    public GpuResourceCache resourceCache() {
        return this.gpuResourceCache;
    }

    public void setGpuResourceCache(GpuResourceCache gpuResourceCache) {
        this.gpuResourceCache = gpuResourceCache;
        this.sceneController.setGpuResourceCache(this.gpuResourceCache);
    }

    public Model model() {
        return this.sceneController != null ? this.sceneController.getModel() : null;
    }

    public void setModel(Model model) {
        // model can be null, that's ok - it indicates no model.
        if (this.sceneController != null)
            this.sceneController.setModel(model);
    }

    public void setView(View view) {
        // view can be null, that's ok - it indicates no view.
        if (this.sceneController != null)
            this.sceneController.setView(view);
    }

    public SceneController sceneControl() {
        return this.sceneController;
    }

    public void setSceneControl(SceneController sc) {
        if (sc != null && this.sceneControl() != null) {
            sc.setGpuResourceCache(this.sceneController.getGpuResourceCache());
        }

        this.sceneController = sc;
    }

    public InputHandler input() {
        return this.inputHandler;
    }

    public void setInput(InputHandler inputHandler) {
        this.inputHandler = inputHandler;
        //HACK share eventListeners
        this.eventListeners = (inputHandler instanceof AWTInputHandler ?
            ((AWTInputHandler)inputHandler).eventListeners : new EventListenerList());
    }

    @Override public void redraw() {
    }

    public void setPerFrameStatisticsKeys(Set<String> keys) {
        if (this.sceneController != null)
            this.sceneController.setPerFrameStatisticsKeys(keys);
    }

    public Collection<PerformanceStatistic> getPerFrameStatistics() {
        if (this.sceneController == null || this.sceneController.getPerFrameStatistics() == null)
            return new ArrayList<>(0);

        return this.sceneController.getPerFrameStatistics();
    }

    public PickedObjectList objectsAtPosition() {
        return null;
    }

    public PickedObjectList objectsInSelectionBox() {
        return null;
    }

    public Position position() {
        if (this.sceneController == null)
            return null;

        PickedObjectList pol = this.sceneControl().getPickedObjectList();
        if (pol == null || pol.size() < 1)
            return null;

        Position p = null;
        PickedObject top = pol.getTopPickedObject();
        if (top != null && top.hasPosition())
            p = top.getPosition();
        else if (pol.getTerrainObject() != null)
            p = pol.getTerrainObject().getPosition();

        return p;
    }

    protected PickedObject getCurrentSelection() {
        if (this.sceneController == null)
            return null;

        PickedObjectList pol = this.sceneControl().getPickedObjectList();
        if (pol == null || pol.size() < 1)
            return null;

        PickedObject top = pol.getTopPickedObject();
        return top.isTerrain() ? null : top;
    }

    protected PickedObjectList getCurrentBoxSelection() {
        if (this.sceneController == null)
            return null;

        PickedObjectList pol = this.sceneController.getObjectsInPickRectangle();
        return pol != null && !pol.isEmpty() ? pol : null;
    }

    public void addRenderingListener(RenderingListener listener) {
        this.eventListeners.add(RenderingListener.class, listener);
    }

    public void removeRenderingListener(RenderingListener listener) {
        this.eventListeners.remove(RenderingListener.class, listener);
    }

    protected void callRenderingListeners(RenderingEvent event) {
        for (RenderingListener listener : eventListeners.getListeners(RenderingListener.class))
            listener.stageChanged(event);
    }

    public void addPositionListener(PositionListener listener) {
        this.eventListeners.add(PositionListener.class, listener);
    }

    public void removePositionListener(PositionListener listener) {
        this.eventListeners.remove(PositionListener.class, listener);
    }

    protected void callPositionListeners(final PositionEvent event) {
        //EventQueue.invokeLater(() -> {
            for (PositionListener listener : eventListeners.getListeners(PositionListener.class)) {
                listener.moved(event);
            }
        //});
    }

    public void addSelectListener(SelectListener listener) {
        this.eventListeners.add(SelectListener.class, listener);
    }

    public void removeSelectListener(SelectListener listener) {
        this.eventListeners.remove(SelectListener.class, listener);
    }

    protected void callSelectListeners(final SelectEvent event) {
        //EventQueue.invokeLater(() -> {
            for (SelectListener listener : eventListeners.getListeners(SelectListener.class)) {
                listener.accept(event);
            }
        //});
    }

    public void addRenderingExceptionListener(RenderingExceptionListener listener) {
        this.eventListeners.add(RenderingExceptionListener.class, listener);
    }

    public void removeRenderingExceptionListener(RenderingExceptionListener listener) {
        this.eventListeners.remove(RenderingExceptionListener.class, listener);
    }

    protected void callRenderingExceptionListeners(final Throwable exception) {
        //EventQueue.invokeLater(() -> {
            for (RenderingExceptionListener listener : eventListeners.getListeners(
                RenderingExceptionListener.class)) {
                listener.exceptionThrown(exception);
            }
        //});
    }
}
