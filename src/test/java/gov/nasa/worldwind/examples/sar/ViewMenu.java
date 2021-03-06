/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.examples.sar;

import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.examples.sar.render.PlaneModel;
import gov.nasa.worldwind.layers.*;
import gov.nasa.worldwind.layers.tool.*;
import gov.nasa.worldwind.render.Renderable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * @author jparsons
 * @version $Id: ViewMenu.java 1171 2013-02-11 21:45:02Z dcollins $
 */
public class ViewMenu extends JMenu {
    private WorldWindow wwd;

    public ViewMenu() {
        super("View");
    }

    public void setWwd(WorldWindow wwdInstance) {
        this.wwd = wwdInstance;

        // Layers
        for (Layer layer : wwd.model().layers()) {
            if (isAbstractLayerMenuItem(layer)) {
                JCheckBoxMenuItem mi = new JCheckBoxMenuItem(new LayerVisibilityAction(wwd, layer));
                mi.setState(layer.isEnabled());
                this.add(mi);
            }
        }

        // Terrain profile
        JMenuItem mi = new JMenuItem("Terrain profile...");
        mi.setMnemonic('T');
        mi.setAccelerator(
            KeyStroke.getKeyStroke(KeyEvent.VK_T, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        mi.addActionListener(event -> wwd.emit(TerrainProfilePanel.TERRAIN_PROFILE_OPEN, null, null));
        this.add(mi);

        // Cloud ceiling contour
        mi = new JMenuItem("Cloud Contour...");
        mi.setMnemonic('C');
        mi.setAccelerator(
            KeyStroke.getKeyStroke(KeyEvent.VK_C, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        mi.addActionListener(event -> wwd.emit(CloudCeilingPanel.CLOUD_CEILING_OPEN, null, null));
        this.add(mi);
    }

    private static boolean isAbstractLayerMenuItem(Layer layer) {
        if (layer instanceof RenderableLayer)  //detect PlaneModel layer
        {
            Iterable<Renderable> iter = ((RenderableLayer) layer).all();
            for (Renderable rend : iter) {
                if (rend instanceof PlaneModel)
                    return true;
            }
        }

        return ((layer instanceof ScalebarLayer
            || layer instanceof CrosshairLayer
            || layer instanceof CompassLayer));
    }

    private static class LayerVisibilityAction extends AbstractAction {
        private final Layer layer;
        private final WorldWindow wwd;

        public LayerVisibilityAction(WorldWindow wwd, Layer layer) {
            super(layer.name());
            this.layer = layer;
            this.wwd = wwd;
        }

        public void actionPerformed(ActionEvent actionEvent) {
            layer.setEnabled(((JCheckBoxMenuItem) actionEvent.getSource()).getState());
            this.wwd.redraw();
        }
    }
}