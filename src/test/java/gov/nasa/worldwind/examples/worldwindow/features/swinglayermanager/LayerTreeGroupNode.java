/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */

package gov.nasa.worldwind.examples.worldwindow.features.swinglayermanager;

import gov.nasa.worldwind.examples.worldwindow.core.WMSLayerInfo;

/**
 * @author tag
 * @version $Id: LayerTreeGroupNode.java 1171 2013-02-11 21:45:02Z dcollins $
 */
public class LayerTreeGroupNode extends LayerTreeNode {
    public LayerTreeGroupNode() {
    }

    public LayerTreeGroupNode(String title) {
        super(title);
    }

    public LayerTreeGroupNode(WMSLayerInfo layerInfo) {
        super(layerInfo);
    }

    public LayerTreeGroupNode(LayerTreeGroupNode layerNode) {
        super(layerNode);
    }
}
