package gov.nasa.worldwind.video.newt;

import com.jogamp.opengl.*;
import gov.nasa.worldwind.*;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.event.*;
import gov.nasa.worldwind.video.*;
import spacegraph.video.*;

import java.beans.PropertyChangeListener;


public class WorldWindowNEWT2 extends AbstractLayer implements WorldWindow, GLEventListener {

    private final WorldWindowGLAutoDrawable wwd;


    public WorldWindowNEWT2(Model model) {
        super();

        this.wwd = ((WorldWindowGLAutoDrawable) WorldWind.createConfigurationComponent(AVKey.WORLD_WINDOW_CLASS_NAME));
        setModel(model);
    }

    @Override
    public void init(GL2 g) {
        wwd().initDrawable(window.window, this);
    }

    /** TODO return false when possible */
    @Override public boolean changed() {
        return wwd().redrawNecessary.getOpaque();
    }

    @Override
    public void init(GLAutoDrawable drawable) {

    }

    @Override
    public void dispose(GLAutoDrawable drawable) {
        wwd().dispose(drawable);
    }

    @Override
    public void display(GLAutoDrawable drawable) {

    }

    @Override
    protected void renderVolume(float dtS, GL2 gl, float aspect) {
        wwd().render(window.window);
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
        window.window.addMouseListener(h);
        window.window.addWindowListener(h);
    }

    @Override
    public void stopEvents(InputHandler h) {
        window.window.removeMouseListener(h);
        window.window.removeWindowListener(h);
    }

}
