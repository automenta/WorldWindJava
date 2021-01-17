package gov.nasa.worldwind.video.newt;

import com.jogamp.opengl.*;
import gov.nasa.worldwind.*;
import gov.nasa.worldwind.avlist.*;
import gov.nasa.worldwind.event.InputHandler;
import gov.nasa.worldwind.geom.ExtentHolder;
import gov.nasa.worldwind.layers.RenderableLayer;
import gov.nasa.worldwind.render.*;
import gov.nasa.worldwind.video.WorldWindowGLAutoDrawable;
import org.eclipse.collections.impl.map.mutable.primitive.ObjectFloatHashMap;
import spacegraph.layer.AbstractLayer;
import tech.tablesaw.util.StringUtils;

import java.beans.PropertyChangeListener;
import java.util.*;
import java.util.function.Predicate;


public class WorldWindowNEWT extends AbstractLayer implements WorldWindow, GLEventListener {

    private final WorldWindowGLAutoDrawable wwd;

    public WorldWindowNEWT(Model model) {
        super();

        this.wwd = ((WorldWindowGLAutoDrawable) WorldWind.createConfigurationComponent(AVKey.WORLD_WINDOW_CLASS_NAME));
        setModel(model);
    }

    @Override
    public void init(GL2 g) {
        wwd().initDrawable(window.window, this);
    }

    /**
     * TODO return false when possible
     */
    @Override
    public boolean changed() {
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
        redraw();
    }

    @Override
    public void redraw() {
        wwd.redraw();
    }

    @Override
    public WorldWindowGLAutoDrawable wwd() {
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
        window.window.addKeyListener(h);
        window.window.addMouseListener(h);
        window.window.addWindowListener(h);
    }

    @Override
    public void stopEvents(InputHandler h) {
        window.window.removeMouseListener(h);
        window.window.removeWindowListener(h);
    }

    private final static Set<String> excludedTags = Set.of("yes", "no", "none", "amenity", "name");

    public static boolean tag(String x) {
        //TODO refine
        return x.length() > 1 && StringUtils.isAlpha(x) && !WorldWindowNEWT.excludedTags.contains(x);
    }

    public ObjectFloatHashMap<String> tagsInView() {

        ObjectFloatHashMap<String> h = new ObjectFloatHashMap<>();

        DrawContext dc = sceneControl().getDrawContext();
        Predicate<ExtentHolder> intersectsFrustrum = dc.intersectsFrustrum();
        for (gov.nasa.worldwind.layers.Layer l : this.model().getLayers()) {
            //if (l.isLayerInView(dc)
            if (l instanceof RenderableLayer) {
                if (l.isEnabled()) {
                    var L = (RenderableLayer) l;
                    for (Renderable r : L.all()) {
                        if (r instanceof AVList) {
                            if (r instanceof ExtentHolder) {
                                if (intersectsFrustrum.test(((ExtentHolder) r))) {
                                    Object m = ((AVList) r).get("_"/*DESCRIPTION*/);
                                    for (Map.Entry<String, String> entry : ((Map<String, String>) m).entrySet()) {
                                        String k = entry.getKey();
                                        if (tag(k))
                                            h.addToValue(k, 1);
                                        String v = entry.getValue();
                                        if (tag(v))
                                            h.addToValue(v, 1);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        return h;
    }
}
