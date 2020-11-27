package gov.nasa.worldwind.ui.newt;

import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.*;
import com.jogamp.opengl.util.FPSAnimator;
import gov.nasa.worldwind.*;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.event.InputHandler;
import gov.nasa.worldwind.ui.WorldWindowGLAutoDrawable;

import java.beans.PropertyChangeListener;

public class WorldWindowNEWT implements WorldWindow, GLEventListener {

    static final int FPS = 60;

    private final GLWindow window;
    private final WorldWindowGLAutoDrawable wwd;
    private final FPSAnimator animator;

    public WorldWindowNEWT(Model model, int width, int height) {
        this(model);
        size(width, height);
    }

    public WorldWindowNEWT(Model model) {
        GLProfile glp = GLProfile.getDefault();
        GLCapabilities caps = new GLCapabilities(glp);
        window = GLWindow.create(caps);

        this.wwd = ((WorldWindowGLAutoDrawable) WorldWind.createConfigurationComponent(AVKey.WORLD_WINDOW_CLASS_NAME));
        setModel(model);

        window.addGLEventListener(this);
        window.setVisible(true);

        // Create a animator that drives canvas' display() at the specified FPS.
        animator = new FPSAnimator(window, FPS, true);
        animator.start();

    }

    public void size(int width, int height) {
        window.setSize(width, height);
    }

    @Override
    public void init(GLAutoDrawable drawable) {

        this.wwd().initDrawable(drawable);
        this.wwd().addPropertyChangeListener(this);
        this.wwd().initGpuResourceCache(WorldWindow.createGpuResourceCache());
        this.createView();
        this.createDefaultInputHandler();
        WorldWind.addPropertyChangeListener(WorldWind.SHUTDOWN_EVENT, this);
        WorldWindow.configureIdentityPixelScale(window);
        this.wwd().endInitialization();
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
    }

    @Override
    public void redraw() {
//        wwd().redraw();
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
