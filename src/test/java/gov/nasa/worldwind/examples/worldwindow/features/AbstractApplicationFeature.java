/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */

package gov.nasa.worldwind.examples.worldwindow.features;

import gov.nasa.worldwind.examples.worldwindow.core.Registry;
import gov.nasa.worldwind.examples.worldwindow.core.layermanager.*;
import gov.nasa.worldwind.examples.worldwindow.util.*;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.video.LayerList;

import javax.swing.*;
import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;
import java.util.logging.Level;

/**
 * @author tag
 * @version $Id: AbstractApplicationFeature.java 1171 2013-02-11 21:45:02Z dcollins $
 */
public abstract class AbstractApplicationFeature extends AbstractFeature implements NetworkActivitySignal.NetworkUser {
    protected boolean on;
    protected boolean autoSelectLayers;
    protected LayerList appLayers;
    protected Thread createLayersThread;

    protected AbstractApplicationFeature(String name, String featureID, String largeIconPath, Registry registry) {
        super(name, featureID, largeIconPath, registry);
    }

    protected abstract String getLayerGroupName();

    protected abstract void doCreateLayers();

    public boolean hasNetworkActivity() {
        return this.createLayersThread != null && this.createLayersThread.isAlive();
    }

    @Override
    public boolean isOn() {
        return this.on;
    }

    protected void setOn(boolean tf) {
        this.on = tf;
    }

    public boolean isAutoSelectLayers() {
        return autoSelectLayers;
    }

    public void setAutoSelectLayers(boolean autoSelectLayers) {
        this.autoSelectLayers = autoSelectLayers;
    }

    public LayerList getAppLayers() {
        return this.appLayers != null ? this.appLayers : new LayerList();
    }

    protected void destroyLayers() {
        this.killPopulateLayerThread();

        if (this.appLayers == null)
            return;

        for (Layer layer : this.appLayers) {
            this.destroyLayer(layer);
        }
        this.appLayers.clear();
        this.appLayers = null;
    }

    protected void destroyLayer(Layer layer) {
        this.controller.getLayerManager().removeLayer(layer);
        this.appLayers.remove(layer);
        layer.dispose();
    }

    protected void killPopulateLayerThread() {
        if (this.createLayersThread != null && this.createLayersThread.isAlive()) {
            this.createLayersThread.interrupt();
            this.controller.getNetworkActivitySignal().removeNetworkUser(this);
            this.createLayersThread = null;
        }
    }

    protected void handleInterrupt() {
        if (Thread.currentThread().isInterrupted() && this.appLayers != null) {
            Util.getLogger().info("Data retrieval cancelled");

            // Clean up so the user can try again later
            this.destroyLayers();
        }
    }

    protected void removeLayers() {
        this.controller.getLayerManager().removeLayers(this.appLayers);
    }

    protected void createLayers() {
        if (this.appLayers == null) {
            this.appLayers = new LayerList();
            this.appLayers.setDisplayName(this.getLayerGroupName());
        }

        this.createLayersThread = new Thread(() -> {
            try {
                doCreateLayers();
            }
            finally {
                handleInterrupt();
                SwingUtilities.invokeLater(() -> {
                    controller.getNetworkActivitySignal().removeNetworkUser(AbstractApplicationFeature.this);
                    createLayersThread = null;
                });
            }
        });
        this.createLayersThread.setPriority(Thread.MIN_PRIORITY);
        this.createLayersThread.start();

        this.controller.getNetworkActivitySignal().addNetworkUser(AbstractApplicationFeature.this);
    }

    protected void addLayer(final Layer layer, final LayerPath path) {
        try {
            // In order to synchronize layer additions, they are added on the EDT.
            if (SwingUtilities.isEventDispatchThread())
                this.doAddLayer(layer, path);
            else
                SwingUtilities.invokeAndWait(() -> doAddLayer(layer, path));
        }
        catch (InterruptedException e) {
            // Don't do anything here because higher level code will detect and report the interrupt.
        }
        catch (InvocationTargetException e) {
            Util.getLogger().log(Level.WARNING, "Invocation target exception", e);
        }
    }

    protected void doAddLayer(final Layer layer, final LayerPath path) {
        LayerManager layerManager = controller.getLayerManager();
        Layer oldLayer = layerManager.getLayerFromPath(path);
        if (oldLayer != null) {
            this.controller.getLayerManager().removeLayer(path);
            this.appLayers.remove(oldLayer);
        }

        // Cause the cache files to be deleted when the JVM exits.

        this.appLayers.add(layer);
        layerManager.addLayer(layer, path.lastButOne());
        layerManager.selectLayer(layer, this.isAutoSelectLayers());
        layerManager.expandPath(path.lastButOne());
    }

    protected void addLayers(LayerList layerList) {
        for (Layer layer : layerList) {
            this.addLayer(layer, new LayerPath(layerList.getDisplayName(), layer.name()));
        }
    }

    protected LayerTree addLayerTree(LayerTree layerTree) {
        LayerPath basePath = new LayerPath(this.getLayerGroupName());

        Iterator<LayerPath> iter = layerTree.getPathIterator(basePath);
        while (iter.hasNext()) {
            LayerPath path = iter.next();
            Layer layer = layerTree.getLayer(path);
            this.addLayer(layer, path);
        }

        return layerTree;
    }
}
