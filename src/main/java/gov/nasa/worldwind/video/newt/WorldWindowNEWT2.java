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
import spacegraph.video.JoglWindow;

import java.beans.PropertyChangeListener;


public class WorldWindowNEWT2 extends JoglWindow implements WorldWindow, GLEventListener {

    private final WorldWindowGLAutoDrawable wwd;

    public WorldWindowNEWT2(Model model, int width, int height) {
        this(model);
        //size(width, height);
        showInit(width,height);
        window.setVisible(true,true); //HACK
    }

    private WorldWindowNEWT2(Model model) {
        super();

        this.wwd = ((WorldWindowGLAutoDrawable) WorldWind.createConfigurationComponent(AVKey.WORLD_WINDOW_CLASS_NAME));
        setModel(model);
    }

    @Override
    protected void showInit(GL2 g) {

        final WorldWindowGLDrawable w = this.wwd();

        w.initDrawable(window, this);
        createView();
        WorldWindow.configureIdentityPixelScale(window);


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
    protected void render(long l, float v) {

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
