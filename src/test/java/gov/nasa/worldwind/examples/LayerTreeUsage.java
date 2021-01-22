/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */

package gov.nasa.worldwind.examples;

import gov.nasa.worldwind.*;
import gov.nasa.worldwind.layers.RenderableLayer;
import gov.nasa.worldwind.ui.HotSpotController;
import gov.nasa.worldwind.ui.tree.BasicTree;
import gov.nasa.worldwind.ui.tree.layer.LayerTree;
import gov.nasa.worldwind.video.newt.WorldWindowNEWT;
import spacegraph.video.JoglWindow;

/**
 * Example of using {@link BasicTree} to display a list of layers.
 *
 * @author pabercrombie
 * @version $Id: LayerTreeUsage.java 1171 2013-02-11 21:45:02Z dcollins $
 */
public class LayerTreeUsage extends ApplicationTemplate {
    public static void main(String[] args) {
        final BasicModel m = new BasicModel();

        JoglWindow W = new JoglWindow(1024, 800);

        WorldWindowNEWT w = new WorldWindowNEWT(m);
        w.setWindow(W);

        W.runLater(()->{
            //HACK
            LayerTree layerTree = new LayerTree();
            layerTree.getModel().refresh(m.layers);


            // Set up a layer to display the on-screen layer tree in the WorldWindow.
            RenderableLayer ui = new RenderableLayer();
            ui.set(Keys.HIDDEN, true);
            ui.add(layerTree);

            // Add a controller to handle input events on the layer tree.
            HotSpotController controller = new HotSpotController(w.wwd());

            m.layers.add(ui);
        });
    }

}