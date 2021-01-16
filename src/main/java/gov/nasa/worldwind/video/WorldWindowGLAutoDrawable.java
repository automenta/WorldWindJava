/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */

package gov.nasa.worldwind.video;

import com.jogamp.nativewindow.ScalableSurface;
import com.jogamp.opengl.*;
import com.jogamp.opengl.awt.AWTGLAutoDrawable;
import com.jogamp.opengl.util.texture.TextureIO;
import gov.nasa.worldwind.*;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.cache.*;
import gov.nasa.worldwind.event.*;
import gov.nasa.worldwind.exception.WWAbsentRequirementException;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.pick.*;
import gov.nasa.worldwind.util.*;

import javax.swing.event.*;
import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

/**
 * A non-platform specific {@link WorldWindow} class. This class can be aggregated into platform-specific classes to
 * provide the core functionality of WorldWind.
 *
 * @author Tom Gaskins
 * @version $Id: WorldWindowGLAutoDrawable.java 2047 2014-06-06 22:48:33Z tgaskins $
 */
public class WorldWindowGLAutoDrawable extends WWObjectImpl implements WorldWindowGLDrawable, GLEventListener {

    /**
     * Default time in milliseconds that the view must remain unchanged before the {@link View#VIEW_STOPPED} message is
     * sent.
     */
    public static final long DEFAULT_VIEW_STOP_TIME = 1000;
    public final AtomicBoolean redrawNecessary = new AtomicBoolean(true);
    protected final boolean enableGpuCacheReinitialization = true;
    private final Message viewStopMsg = new Message(View.VIEW_STOPPED, WorldWindowGLAutoDrawable.this);
    /**
     * Time in milliseconds that the view must remain unchanged before the {@link View#VIEW_STOPPED} message is sent.
     */
    protected long viewStopTime = WorldWindowGLAutoDrawable.DEFAULT_VIEW_STOP_TIME;
    /**
     * The most recent View modelView ID.
     *
     * @see View#getViewStateID()
     */
    protected long lastViewID;
    private final GpuResourceCache gpuResourceCache = new BasicGpuResourceCache();
    protected SceneController sceneController;
    private final Runnable viewStoppedTask = () -> WorldWindowGLAutoDrawable.this.onMessage(viewStopMsg);
    /**
     * Schedule task to send the {@link View#VIEW_STOPPED} message after the view stop time elapses.
     */
    private ScheduledFuture viewRefreshTask;
    private GLAutoDrawable drawable;
    private boolean shuttingDown;
    private boolean firstInit = true;
    private EventListenerList eventListeners;
    private InputHandler inputHandler;

    /**
     * Construct a new <code>WorldWindowGLCanvas</code> for a specified {@link GLDrawable}.
     */
    public WorldWindowGLAutoDrawable() {
        this.sceneController = (SceneController) WorldWind.createConfigurationComponent(
            AVKey.SCENE_CONTROLLER_CLASS_NAME);
        this.sceneController.setGpuResourceCache(this.gpuResourceCache);

        // Set up to initiate a repaint whenever a file is retrieved and added to the local file store.
        Configuration.data.addPropertyChangeListener(this);
        SceneController sc = this.sceneControl();
        if (sc != null)
            sc.addPropertyChangeListener(this);
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

        float[] identityScale = {ScalableSurface.IDENTITY_PIXELSCALE, ScalableSurface.IDENTITY_PIXELSCALE};
        surface.setSurfaceScale(identityScale);
    }

    protected static boolean isGLContextCompatible(GLContext context) {
        return context != null && context.isGL2();
    }

    protected static String[] getRequiredOglFunctions() {
        return new String[] {"glActiveTexture", "glClientActiveTexture"};
    }

    protected static String[] getRequiredOglExtensions() {
        return new String[] {};
    }

    /**
     * Indicates the amount of time, in milliseconds, that the View must remain unchanged before a {@link
     * View#VIEW_STOPPED} event is triggered.
     *
     * @return Time in milliseconds that the View must must remain unchanged before the view stopped event is triggered.
     */
    public long getViewStopTime() {
        return this.viewStopTime;
    }

    public void initDrawable(GLAutoDrawable g, WorldWindow w) {

        this.drawable = g;

        addPropertyChangeListener(w);



        g.addGLEventListener(this);

        WorldWindow.configureIdentityPixelScale((ScalableSurface) g);

        WorldWindow.createView(w);
    }

    @Override
    public void shutdown() {
        this.shuttingDown = true;
        this.redraw(); // Invokes a repaint, where the rest of the shutdown work is done.
    }

    protected final void doShutdown() {
        try {
            shutdown();
            this.drawable.removeGLEventListener(this);
            if (this.viewRefreshTask != null)
                this.viewRefreshTask.cancel(false);
            this.shuttingDown = false;
        }
        catch (RuntimeException e) {
            Logging.logger().log(Level.SEVERE, Logging.getMessage(
                "WorldWindowGLCanvas.ExceptionWhileShuttingDownWorldWindow"), e);
        }
    }

    @Override
    public void propertyChange(PropertyChangeEvent propertyChangeEvent) {

        this.redraw(); // Queue a JOGL display request.
    }

    public GLContext getContext() {
        return this.drawable.getContext();
    }

    /**
     * See {@link GLEventListener#init(GLAutoDrawable)}.
     *
     * @param glAutoDrawable the drawable
     */
    public void init(GLAutoDrawable glAutoDrawable) {
        if (!WorldWindowGLAutoDrawable.isGLContextCompatible(glAutoDrawable.getContext())) {
            String msg = Logging.getMessage("WorldWindowGLAutoDrawable.IncompatibleGLContext",
                glAutoDrawable.getContext());
            this.callRenderingExceptionListeners(new WWAbsentRequirementException(msg));
        }

        for (String funcName : WorldWindowGLAutoDrawable.getRequiredOglFunctions()) {
            if (!glAutoDrawable.getGL().isFunctionAvailable(funcName)) {
                //noinspection ThrowableInstanceNeverThrown
                this.callRenderingExceptionListeners(new WWAbsentRequirementException(funcName + " not available"));
            }
        }

        for (String extName : WorldWindowGLAutoDrawable.getRequiredOglExtensions()) {
            if (!glAutoDrawable.getGL().isExtensionAvailable(extName)) {
                //noinspection ThrowableInstanceNeverThrown
                this.callRenderingExceptionListeners(new WWAbsentRequirementException(extName + " not available"));
            }
        }

        if (this.firstInit)
            this.firstInit = false;
        else if (this.enableGpuCacheReinitialization)
            this.reinitialize(glAutoDrawable);

        // Disables use of the OpenGL extension GL_ARB_texture_rectangle by JOGL's Texture creation utility.
        //
        // Between version 1.1.1 and version 2.x, JOGL modified its texture creation utility to favor
        // GL_ARB_texture_rectangle over GL_ARB_texture_non_power_of_two on Mac OS X machines with ATI graphics cards. See
        // the following URL for details on the texture rectangle extension: http://www.opengl.org/registry/specs/ARB/texture_rectangle.txt
        //
        // There are two problems with favoring texture rectangle for non power of two textures:
        // 1) As of November 2012, we cannot find any evidence that the GL_ARB_texture_non_power_of_two extension is
        //    problematic on Mac OS X machines with ATI graphics cards. The texture rectangle extension is more limiting
        //    than the NPOT extension, and therefore not preferred.
        // 2) WorldWind assumes that a texture's target is always GL_TEXTURE_2D, and therefore incorrectly displays
        //    textures with the target GL_TEXTURE_RECTANGLE.
        TextureIO.setTexRectEnabled(false);

//        this.drawable.setGL(new DebugGL(this.drawable.getGL())); // uncomment to use the debug drawable
    }

    @SuppressWarnings("UnusedParameters")
    protected void reinitialize(GLAutoDrawable glAutoDrawable) {
        // Clear the gpu resource cache if the window is reinitializing, most likely with a new gl hardware context.
        this.resourceCache().clear();

        this.sceneControl().reinitialize();
    }

    /**
     * See {@link GLEventListener#init(GLAutoDrawable)}.
     * <p>
     * GLEventListener's dispose method indicates that the GL context has been released, and provides the listener an
     * opportunity to clean up any resources. Dispose does not imply that the component's lifecycle has ended or that
     * the application is closing. There are three cases in which dispose may be called:
     * <ul> <li>The WorldWindow is removed from its parent component.</li> <li>The WorldWindow's parent frame is
     * closed.</li> <li>The application calls either GLCanvas.dispose or GLJPanel.dispose.</li> </ul>
     * <p>
     * This implementation is left empty. In the case when a WorldWindow or a WorldWind application has reached its end
     * of life, its resources should be released by calling {@link WorldWindow#shutdown()} or {@link
     * WorldWind#shutDown()}, respectively. In the case when a WorldWindow is removed from its parent
     * frame or that frame is closed without a call to shutdown, it is assumed that the application intends to reuse the
     * WorldWindow. Resources associated with the previous GL context are released in the subsequent call to init.
     *
     * @param glAutoDrawable the drawable
     */
    @Override
    public void dispose(GLAutoDrawable glAutoDrawable) {
    }

    @Override
    public void display(GLAutoDrawable g) {
    }

    public void render(GLAutoDrawable g) {

        redrawNecessary.setOpaque(false);

        this.drawable = g;

        // Determine if the view has changed since the last frame.
        this.checkForViewChange();

        Position positionAtStart = this.position();
        PickedObject selectionAtStart = this.getCurrentSelection();
        PickedObjectList boxSelectionAtStart = this.getCurrentBoxSelection();

        SceneController sc = this.sceneControl();

        sc.repaint();

        this.set(PerformanceStatistic.FRAME_TIME, sc.getFrameTime());

        this.set(PerformanceStatistic.FRAME_RATE, sc.getFramesPerSecond());

        // Dispatch the rendering exceptions accumulated by the SceneController during this frame to our
        // RenderingExceptionListeners.
        Iterable<Throwable> renderingExceptions = sc.getRenderingExceptions();
        if (renderingExceptions != null) {
            for (Throwable t : renderingExceptions) {
                this.callRenderingExceptionListeners(t);
            }
        }

//        this.callRenderingListeners(AFTER_BUFFER_SWAP);

        // Position and selection notification occurs only on triggering conditions, not same-state conditions:
        // start == null, end == null: nothing selected -- don't notify
        // start == null, end != null: something now selected -- notify
        // start != null, end == null: something was selected but no longer is -- notify
        // start != null, end != null, start != end: something new was selected -- notify
        // start != null, end != null, start == end: same thing is selected -- don't notify

        Position positionAtEnd = this.position();
        final Point pickPoint = sc.getPickPoint();
        if (positionAtStart != null || positionAtEnd != null) {
            // call the listener if both are not null or positions are the same
            if (positionAtStart != null && positionAtEnd != null) {
                if (!positionAtStart.equals(positionAtEnd))
                    this.callPositionListeners(new PositionEvent(drawable, pickPoint, positionAtStart, positionAtEnd));
            } else
                this.callPositionListeners(new PositionEvent(drawable, pickPoint, positionAtStart, positionAtEnd));
        }

        PickedObject selectionAtEnd = this.getCurrentSelection();
        if (selectionAtStart != null || selectionAtEnd != null)
            this.callSelectListeners(
                new SelectEvent(drawable, SelectEvent.ROLLOVER, pickPoint, sc.getPickedObjectList()));

        PickedObjectList boxSelectionAtEnd = this.getCurrentBoxSelection();
        if (boxSelectionAtStart != null || boxSelectionAtEnd != null)
            this.callSelectListeners(new SelectEvent(drawable, SelectEvent.BOX_ROLLOVER, sc.getPickRectangle(),
                sc.getObjectsInPickRectangle()));
    }

    /**
     * Determine if the view has changed since the previous frame. If the view has changed, schedule a task that will
     * send a {@link View#VIEW_STOPPED} to the Model if the view does not change for {@link #viewStopTime}
     * milliseconds.
     *
     * @see #getViewStopTime()
     */
    protected void checkForViewChange() {
        long viewId = view().getViewStateID();

        // Determine if the view has changed since the previous frame.
        if (viewId != this.lastViewID) {
            // View has changed, capture the new viewStateID
            this.lastViewID = viewId;

            // Cancel the previous view stop task and schedule a new one because the view has changed.
            this.scheduleViewStopTask(this.getViewStopTime());
        }
    }

    /**
     * See {@link GLEventListener#reshape(GLAutoDrawable, int, int, int, int)}.
     *
     * @param glAutoDrawable the drawable
     */
    public void reshape(GLAutoDrawable glAutoDrawable, int x, int y, int w, int h) {
        if (glAutoDrawable instanceof Component) {
            // This is apparently necessary to enable the WWJ canvas to resize correctly with JSplitPane.
            ((Component) glAutoDrawable).setMinimumSize(new Dimension(0, 0));
        }
    }

    @Override
    public void redraw() {
        redrawNecessary.setOpaque(true);
        if (this.drawable instanceof AWTGLAutoDrawable)
            ((AWTGLAutoDrawable) this.drawable).repaint();
    }

    /**
     * Schedule a task that will send a {@link View#VIEW_STOPPED} message to the Model when the task executes. If the
     * task runs (is not cancelled), then the view is considered stopped. Only one view stop task is scheduled at a
     * time. If this method is called again before the task executes, the task will be cancelled and a new task
     * scheduled.
     *
     * @param delay Delay in milliseconds until the task runs.
     */
    protected synchronized void scheduleViewStopTask(long delay) {

        // Cancel the previous view stop task
        if (this.viewRefreshTask != null) {
            this.viewRefreshTask.cancel(false);
            this.viewRefreshTask = null;
        }

        // Schedule the task for execution in delay milliseconds
        this.viewRefreshTask = WorldWind.scheduler()
            .addScheduledTask(viewStoppedTask, delay, TimeUnit.MILLISECONDS);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Forward the message event to the Model for distribution to the layers.
     *
     * @param msg Message event.
     */
    @Override
    public void onMessage(Message msg) {
        Model model = this.model();
        if (model != null) {
            model.onMessage(msg);
        }
    }

    @Override
    public View view() {
        return this.sceneController != null ? this.sceneController.getView() : null;
    }

    @Override
    public WorldWindowGLDrawable wwd() {
        return this;
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
        this.eventListeners = (inputHandler instanceof DefaultInputHandler ?
            ((DefaultInputHandler) inputHandler).eventListeners : new EventListenerList());
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
            p = top.position();
        else if (pol.getTerrainObject() != null)
            p = pol.getTerrainObject().position();

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
        for (RenderingListener listener : eventListeners.getListeners(RenderingListener.class)) {
            listener.stageChanged(event);
        }
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
