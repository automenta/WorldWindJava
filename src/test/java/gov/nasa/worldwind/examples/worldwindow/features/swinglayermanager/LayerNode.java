/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */

package gov.nasa.worldwind.examples.worldwindow.features.swinglayermanager;

import gov.nasa.worldwind.examples.worldwindow.core.WMSLayerInfo;
import gov.nasa.worldwind.layers.Layer;

/**
 * @author tag
 * @version $Id: LayerNode.java 1171 2013-02-11 21:45:02Z dcollins $
 */
public interface LayerNode {
    Object getID();

    String getTitle();

    void setTitle(String title);

    Layer getLayer();

    void setLayer(Layer layer);

    boolean isSelected();

    void setSelected(boolean selected);

    WMSLayerInfo getWmsLayerInfo();

    String getToolTipText();

    void setToolTipText(String toolTipText);

    boolean isEnableSelectionBox();

    void setEnableSelectionBox(boolean tf);

    void setAllowsChildren(boolean tf);
}
