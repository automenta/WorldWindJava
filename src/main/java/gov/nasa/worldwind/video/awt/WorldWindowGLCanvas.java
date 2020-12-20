/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.video.awt;

import com.jogamp.opengl.awt.GLCanvas;
import gov.nasa.worldwind.*;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.cache.GpuResourceCache;
import gov.nasa.worldwind.event.InputHandler;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.util.BasicGLCapabilitiesChooser;
import gov.nasa.worldwind.util.dashboard.DashboardController;
import gov.nasa.worldwind.video.*;

import java.beans.PropertyChangeListener;

/**
 * <code>WorldWindowGLCanvas</code> is a heavyweight AWT component for displaying WorldWind {@link Model}s (globe and
 * layers). It's a self-contained component intended to serve as an application's <code>WorldWindow</code>. Construction
 * options exist to specify a specific graphics device and to share graphics resources with another graphics device.
 * <p>
 * Heavyweight AWT components such as instances of this class can be used in conjunction with lightweight Swing
 * components. A discussion of doing so is in the <em>Heavyweight and Lightweight Issues</em> section of the <a
 * href="http://download.java.net/media/jogl/doc/userguide/">"JOGL User's Guide"</a>. All that's typically necessary is
 * to invoke the following methods of the indicated Swing classes: {@link javax.swing.ToolTipManager#setLightWeightPopupEnabled(boolean)},
 * {@link javax.swing.JPopupMenu#setLightWeightPopupEnabled(boolean)} and {@link javax.swing.JPopupMenu#setLightWeightPopupEnabled(boolean)}.
 * These methods should be invoked within a <code>static</code> block within an application's main class.
 * <p>
 * This class is capable of supporting stereo devices. To cause a stereo device to be selected and used, specify the
 * Java VM property "gov.nasa.worldwind.stereo.mode=device" prior to creating an instance of this class. A stereo
 * capable {@link SceneController} such as {@link StereoSceneController} must also be specified in the WorldWind {@link
 * Configuration}. The default configuration specifies a stereo-capable controller. To prevent stereo from being used by
 * subsequently opened {@code WorldWindowGLCanvas}es, set the property to a an empty string, "". If a stereo device
 * cannot be selected and used, this falls back to a non-stereo device that supports WorldWind's minimum requirements.
 * <p>
 * Under certain conditions, JOGL replaces the <code>GLContext</code> associated with instances of this class. This then
 * necessitates that all resources such as textures that have been stored on the graphic devices must be regenerated for
 * the new context. WorldWind does this automatically by clearing the associated {@link GpuResourceCache}. Objects
 * subsequently rendered automatically re-create those resources. If an application creates its own graphics resources,
 * including textures, vertex buffer objects and display lists, it must store them in the <code>GpuResourceCache</code>
 * associated with the current {@link DrawContext} so that they are automatically
 * cleared, and be prepared to re-create them if they do not exist in the <code>DrawContext</code>'s current
 * <code>GpuResourceCache</code> when needed. Examples of doing this can be found by searching for usages of the method
 * {@link GpuResourceCache#get(Object)} and {@link GpuResourceCache#getTexture(Object)}.
 *
 * @author Tom Gaskins
 * @version $Id: WorldWindowGLCanvas.java 2924 2015-03-26 01:32:02Z tgaskins $
 */
@Deprecated public class WorldWindowGLCanvas extends GLCanvas implements WorldWindow, PropertyChangeListener {

    private final WorldWindowGLDrawable wwd
        = ((WorldWindowGLDrawable) WorldWind.createConfigurationComponent(AVKey.WORLD_WINDOW_CLASS_NAME));

    private DashboardController dashboard;

    /**
     * Constructs a new <code>WorldWindowGLCanvas</code> on the default graphics device.
     */
    public WorldWindowGLCanvas() {
        super(Configuration.getRequiredGLCapabilities(), new BasicGLCapabilitiesChooser(), null);

        this.wwd().initDrawable(this, this);

//        initializeCreditsController();
        this.dashboard = new DashboardController(this, this);
    }

//    /**
//     * Constructs a new <code>WorldWindowGLCanvas</code> on the default graphics device and shares graphics resources
//     * with another <code>WorldWindow</code>.
//     *
//     * @param shareWith a <code>WorldWindow</code> with which to share graphics resources.
//     * @see GLCanvas#GLCanvas(GLCapabilitiesImmutable, GLCapabilitiesChooser, GraphicsDevice)
//     */
//    public WorldWindowGLCanvas(WorldWindow shareWith) {
//        super(Configuration.getRequiredGLCapabilities(), new BasicGLCapabilitiesChooser(), null);
//
//        if (shareWith != null)
//            this.setSharedAutoDrawable((GLAutoDrawable) shareWith);
//
//        try {
//            this.wwd = ((WorldWindowGLDrawable) WorldWind.createConfigurationComponent(AVKey.WORLD_WINDOW_CLASS_NAME));
//            this.wwd().initDrawable(this);
//            this.wwd().addPropertyChangeListener(this);
//            if (shareWith != null)
//                this.wwd().initGpuResourceCache(shareWith.resourceCache());
//            else
//                this.wwd().initGpuResourceCache(WorldWindow.createGpuResourceCache());
//            this.createView();
//            this.createDefaultInputHandler();
//            WorldWind.addPropertyChangeListener(WorldWind.SHUTDOWN_EVENT, this);
//            WorldWindow.configureIdentityPixelScale(this);
//            this.wwd().endInitialization();
//        }
//        catch (Exception e) {
//            String message = Logging.getMessage("Awt.WorldWindowGLSurface.UnabletoCreateWindow");
//            Logging.logger().severe(message);
//            throw new WWRuntimeException(message, e);
//        }
//    }

    @Override
    public void startEvents(InputHandler h) {
        DefaultInputHandler hh = (DefaultInputHandler) h;
        addKeyListener(hh);
        addMouseMotionListener(hh);
        addMouseListener(hh);
        addMouseWheelListener(hh);
        addFocusListener(hh);
    }

    @Override
    public void stopEvents(InputHandler h) {
        DefaultInputHandler hh = (DefaultInputHandler) h;
        removeKeyListener(hh);
        removeMouseMotionListener(hh);
        removeMouseListener(hh);
        removeMouseWheelListener(hh);
        removeFocusListener(hh);
    }

//    protected void initializeCreditsController() {
//        new ScreenCreditController(wwd());
//    }

    @Override
    public void shutdown() {
        if (this.dashboard != null)
            this.dashboard.dispose();
    }

    public void redraw() {
        this.repaint();
    }

    @Override
    public WorldWindowGLDrawable wwd() {
        return wwd;
    }

    @Override
    public synchronized void addPropertyChangeListener(PropertyChangeListener listener) {
        super.addPropertyChangeListener(listener);
    }

    @Override
    public synchronized void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        super.addPropertyChangeListener(propertyName, listener);
    }

    @Override
    public synchronized void removePropertyChangeListener(PropertyChangeListener listener) {
        super.removePropertyChangeListener(listener);
    }

    @Override
    public synchronized void removePropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        super.removePropertyChangeListener(propertyName, listener);
    }

    @Override
    public void firePropertyChange(String propertyName, Object oldValue, Object newValue) {
        super.firePropertyChange(propertyName, oldValue, newValue);
    }
}
