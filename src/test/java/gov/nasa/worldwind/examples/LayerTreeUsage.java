/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */

package gov.nasa.worldwind.examples;

import gov.nasa.worldwind.*;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.layers.RenderableLayer;
import gov.nasa.worldwind.ui.HotSpotController;
import gov.nasa.worldwind.ui.tree.BasicTree;
import gov.nasa.worldwind.ui.tree.layer.LayerTree;
import gov.nasa.worldwind.util.WWUtil;
import gov.nasa.worldwind.video.newt.WorldWindowNEWT;

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
        WorldWindowNEWT w = new WorldWindowNEWT(m, 1024, 800);

        LayerTree layerTree = new LayerTree();
        layerTree.getModel().refresh(m.getLayers());


        // Set up a layer to display the on-screen layer tree in the WorldWindow.
        RenderableLayer ui = new RenderableLayer();
        ui.set(AVKey.HIDDEN, true);
        ui.add(layerTree);

        // Add a controller to handle input events on the layer tree.
        HotSpotController controller = new HotSpotController(w.wwd());

        //WorldWindow.insertBeforeCompass(w.wwd(), ui);
        m.getLayers().add(ui);
    }

    public static class AppFrame extends ApplicationTemplate.AppFrame {

        public AppFrame() {
            super(true, false, false); // Don't include the layer panel; we're using the on-screen layer tree.

            final WorldWindow wwd = this.wwd();

            var layerTree = new LayerTree();
            layerTree.getModel().refresh(wwd.model().getLayers());

            // Set up a layer to display the on-screen layer tree in the WorldWindow.
            var ui = new RenderableLayer();
            ui.add(layerTree);
            wwd.model().getLayers().add(ui);

            // Mark the layer as hidden to prevent it being included in the layer tree's model. Including the layer in
            // the tree would enable the user to hide the layer tree display with no way of bringing it back.
            ui.set(AVKey.HIDDEN, true);


            // Add a controller to handle input events on the layer tree.
            var controller = new HotSpotController(wwd);

            // Size the WorldWindow to take up the space typically used by the layer panel. This illustrates the
            // screen space gained by using the on-screen layer tree.
            Dimension size = new Dimension(1000, 600);
            this.setPreferredSize(size);
            this.pack();
            WWUtil.alignComponent(null, this, AVKey.CENTER);
        }
    }
}
