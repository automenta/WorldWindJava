/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */

package gov.nasa.worldwind.examples.worldwindow.features;

import gov.nasa.worldwind.avlist.AVListImpl;
import gov.nasa.worldwind.examples.worldwindow.core.Registry;
import gov.nasa.worldwind.examples.worldwindow.core.layermanager.LayerPath;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.util.Logging;

import java.awt.*;

/**
 * @author tag
 * @version $Id: GraticuleLayer.java 1171 2013-02-11 21:45:02Z dcollins $
 */
public abstract class WWGraticuleLayer extends AbstractOnDemandLayerFeature {
    public WWGraticuleLayer(String name, String featureID, String iconPath, String group, Registry registry) {
        super(name, featureID, iconPath, group, registry);
    }

    protected abstract Layer doCreateLayer();

    @Override
    protected Layer createLayer() {
        Layer layer = this.doCreateLayer();

        layer.setPickEnabled(false);

        return layer;
    }

    @Override
    protected void addLayer(LayerPath path) {
        controller.addInternalActiveLayer(this.layer);
    }

    @Override
    protected void removeLayer() {
        this.controller.getWWPanel().removeLayer(this.layer);
    }
}