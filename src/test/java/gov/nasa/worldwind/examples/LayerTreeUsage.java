/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */

package gov.nasa.worldwind.examples;

import gov.nasa.worldwind.*;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.layers.RenderableLayer;
import gov.nasa.worldwind.ui.newt.WorldWindowNEWT;
import gov.nasa.worldwind.util.*;
import gov.nasa.worldwind.util.layertree.LayerTree;
import gov.nasa.worldwind.util.tree.BasicTree;

import java.awt.*;

/**
 * Example of using {@link BasicTree} to display a list of layers.
 *
 * @author pabercrombie
 * @version $Id: LayerTreeUsage.java 1171 2013-02-11 21:45:02Z dcollins $
 */
public class LayerTreeUsage extends ApplicationTemplate {
    public static void main0(String[] args) {
        ApplicationTemplate.start("WorldWind Layer Tree", AppFrame.class);
    }
    public static void main(String[] args) {
        final BasicModel m = new BasicModel();
        LayerTree layerTree = new LayerTree();

        // Set up a layer to display the on-screen layer tree in the WorldWindow.
        RenderableLayer l = new RenderableLayer();
//        l.set(AVKey.HIDDEN, true);
        l.add(layerTree);


        // Refresh the tree model with the WorldWindow's current layer list.
        layerTree.getModel().refresh(m.getLayers());



        WorldWindowNEWT w = new WorldWindowNEWT(m, 1024, 800);

        WorldWindow.insertBeforeCompass(w.wwd(), l);

        // Add a controller to handle input events on the layer tree.
        HotSpotController controller = new HotSpotController(w.wwd());
    }

    public static class AppFrame extends ApplicationTemplate.AppFrame {
        protected final LayerTree layerTree;
        protected final RenderableLayer hiddenLayer;

        protected final HotSpotController controller;

        public AppFrame() {
            super(true, false, false); // Don't include the layer panel; we're using the on-screen layer tree.

            this.layerTree = new LayerTree();

            // Set up a layer to display the on-screen layer tree in the WorldWindow.
            this.hiddenLayer = new RenderableLayer();
            this.hiddenLayer.add(this.layerTree);
            this.wwd().model().getLayers().add(this.hiddenLayer);

            // Mark the layer as hidden to prevent it being included in the layer tree's model. Including the layer in
            // the tree would enable the user to hide the layer tree display with no way of bringing it back.
            this.hiddenLayer.set(AVKey.HIDDEN, true);

            // Refresh the tree model with the WorldWindow's current layer list.
            this.layerTree.getModel().refresh(this.wwd().model().getLayers());

            // Add a controller to handle input events on the layer tree.
            this.controller = new HotSpotController(this.wwd());

            // Size the WorldWindow to take up the space typically used by the layer panel. This illustrates the
            // screen space gained by using the on-screen layer tree.
            Dimension size = new Dimension(1000, 600);
            this.setPreferredSize(size);
            this.pack();
            WWUtil.alignComponent(null, this, AVKey.CENTER);
        }
    }
}
