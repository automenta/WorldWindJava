/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */

package gov.nasa.worldwind.examples.worldwindow.features;

import gov.nasa.worldwind.examples.worldwindow.core.Registry;
import gov.nasa.worldwind.examples.worldwindow.core.layermanager.LayerPath;
import gov.nasa.worldwind.layers.Layer;

/**
 * @author tag
 * @version $Id: AbstractOnDemandLayerFeature.java 1171 2013-02-11 21:45:02Z dcollins $
 */
public abstract class AbstractOnDemandLayerFeature extends AbstractFeature {
    protected final String group;
    protected Layer layer;
    protected boolean on = false;

    public AbstractOnDemandLayerFeature(String s, String featureID, String iconPath, String group, Registry registry) {
        super(s, featureID, iconPath, registry);

        this.group = group;
    }

    protected abstract Layer createLayer();

    @Override
    public boolean isTwoState() {
        return true;
    }

    @Override
    public boolean isOn() {
        return this.on;
    }

    @Override
    public void turnOn(boolean tf) {
        if (tf == this.on)
            return;

        if (tf && this.layer == null)
            this.layer = this.createLayer();

        if (this.layer == null)
            return;

        if (tf) {
            LayerPath path = new LayerPath(this.group);
            this.addLayer(path);
            this.controller.getLayerManager().selectLayer(this.layer, true);
        }
        else {
            this.removeLayer();
        }

        this.on = tf;
    }

    protected void addLayer(LayerPath path) {
        this.controller.getLayerManager().addLayer(this.layer, path);
    }

    protected void removeLayer() {
        this.controller.getLayerManager().removeLayer(this.layer);
    }
}
