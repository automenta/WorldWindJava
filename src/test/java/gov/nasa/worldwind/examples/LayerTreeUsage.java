/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */

package gov.nasa.worldwind.examples;

import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.layers.RenderableLayer;
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
    public static void main(String[] args) {
        ApplicationTemplate.start("WorldWind Layer Tree", AppFrame.class);
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
            this.getWwd().model().getLayers().add(this.hiddenLayer);

            // Mark the layer as hidden to prevent it being included in the layer tree's model. Including the layer in
            // the tree would enable the user to hide the layer tree display with no way of bringing it back.
            this.hiddenLayer.setValue(AVKey.HIDDEN, true);

            // Refresh the tree model with the WorldWindow's current layer list.
            this.layerTree.getModel().refresh(this.getWwd().model().getLayers());

            // Add a controller to handle input events on the layer tree.
            this.controller = new HotSpotController(this.getWwd());

            // Size the WorldWindow to take up the space typically used by the layer panel. This illustrates the
            // screen space gained by using the on-screen layer tree.
            Dimension size = new Dimension(1000, 600);
            this.setPreferredSize(size);
            this.pack();
            WWUtil.alignComponent(null, this, AVKey.CENTER);
        }
    }
}