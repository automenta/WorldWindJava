package gov.nasa.worldwind.video.newt;

import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.*;
import com.jogamp.opengl.util.FPSAnimator;
import gov.nasa.worldwind.*;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.event.*;
import gov.nasa.worldwind.layers.tool.WorldMapLayer;
import gov.nasa.worldwind.util.*;
import gov.nasa.worldwind.video.*;

import java.beans.PropertyChangeListener;

public class WorldWindowNEWT implements WorldWindow, GLEventListener {

    static {
        System.setProperty("java.awt.headless", "true");
    }
    static final int FPS_DEFAULT = 60;

    private final GLWindow window;
    private final WorldWindowGLAutoDrawable wwd;
    private final FPSAnimator animator;

    public WorldWindowNEWT(Model model, int width, int height) {
        this(model);
        size(width, height);
    }

    public WorldWindowNEWT(Model model) {
        this(model, glWindow());
    }

    private static GLWindow glWindow() {
        return GLWindow.create(new GLCapabilities(GLProfile.getMaximum(true)));
    }

    public WorldWindowNEWT(Model model, GLWindow window) {
        this.window = window;

        this.wwd = ((WorldWindowGLAutoDrawable) WorldWind.createConfigurationComponent(AVKey.WORLD_WINDOW_CLASS_NAME));
        setModel(model);

        window.addGLEventListener(this);
        window.setVisible(true);

        animator = new FPSAnimator(window, FPS_DEFAULT, false);
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
    public void init(GLAutoDrawable g) {
        wwd().initDrawable(g, this);

    }

    @Override
    public void dispose(GLAutoDrawable drawable) {
        wwd().dispose(drawable);
    }

    @Override
    public void display(GLAutoDrawable drawable) {
    }

    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
//        window.setSize(width, height);
//        window.setPosition(x, y);
        redraw();
    }


    @Override
    public void redraw() {
        wwd.redraw();
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
