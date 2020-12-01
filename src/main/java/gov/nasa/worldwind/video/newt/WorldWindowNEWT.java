package gov.nasa.worldwind.video.newt;

import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.*;
import com.jogamp.opengl.util.FPSAnimator;
import gov.nasa.worldwind.*;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.event.*;
import gov.nasa.worldwind.layers.WorldMapLayer;
import gov.nasa.worldwind.util.*;
import gov.nasa.worldwind.video.WorldWindowGLAutoDrawable;

import java.beans.PropertyChangeListener;

public class WorldWindowNEWT implements WorldWindow, GLEventListener {

    static final int FPS_DEFAULT = 60;

    private final GLWindow window;
    private final WorldWindowGLAutoDrawable wwd;
    private final FPSAnimator animator;

    public WorldWindowNEWT(Model model, int width, int height) {
        this(model);
        size(width, height);
    }

    public WorldWindowNEWT(Model model) {
        window = GLWindow.create(new GLCapabilities(GLProfile.getDefault()));

        this.wwd = ((WorldWindowGLAutoDrawable) WorldWind.createConfigurationComponent(AVKey.WORLD_WINDOW_CLASS_NAME));
        setModel(model);

        window.addGLEventListener(this);
        window.setVisible(true);

        // Create a animator that drives canvas' display() at the specified FPS.
        animator = new FPSAnimator(window, FPS_DEFAULT, true);
        animator.start();

    }

    public WorldWindowNEWT fps(int fps) {
        animator.setFPS(fps);
        return this;
    }

    public void size(int width, int height) {
        window.setSize(width, height);
    }

    @Override
    public void init(GLAutoDrawable drawable) {

        final WorldWindowGLAutoDrawable w = this.wwd();

        w.initDrawable(drawable);
        w.addPropertyChangeListener(this);
        w.initGpuResourceCache(WorldWindow.createGpuResourceCache());
        this.createView();
        this.createDefaultInputHandler();
        WorldWind.addPropertyChangeListener(WorldWind.SHUTDOWN_EVENT, this);
        WorldWindow.configureIdentityPixelScale(window);
        w.endInitialization();

        // Setup a select listener for the worldmap click-and-go feature
        w.addSelectListener(new ClickAndGoSelectListener(w, WorldMapLayer.class));

        // Add controllers to manage highlighting and tool tips.
        var toolTipController = new ToolTipController(w, AVKey.DISPLAY_NAME, null);
        var highlightController = new HighlightController(w, SelectEvent.ROLLOVER);

    }

    @Override
    public void dispose(GLAutoDrawable drawable) {
        wwd().dispose(drawable);
    }

    @Override
    public void display(GLAutoDrawable drawable) {
        //wwd().display(drawable);
    }

    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
        window.setSize(width, height);
        window.setPosition(x, y);
        redraw();
    }


    @Override
    public void redraw() {
        wwd.redraw();
    }

    @Override
    public GLContext getContext() {
        return wwd().getContext();
    }

    @Override public WorldWindowGLAutoDrawable wwd() {
        return wwd;
    }

    @Override
    public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        //TODO?
    }

    @Override
    public void removePropertyChangeListener(String propertyName, PropertyChangeListener listener) {

    }

    @Override
    public void addPropertyChangeListener(PropertyChangeListener listener) {

    }

    @Override
    public void removePropertyChangeListener(PropertyChangeListener listener) {

    }

    @Override
    public void firePropertyChange(String propertyName, Object oldValue, Object newValue) {

    }

    @Override
    public void startEvents(InputHandler h) {
        window.addMouseListener(h);
        window.addWindowListener(h);
    }

    @Override
    public void stopEvents(InputHandler h) {
        window.removeMouseListener(h);
        window.removeWindowListener(h);
    }

}
