/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */

package gov.nasa.worldwind.examples.worldwindow.core;

import gov.nasa.worldwind.avlist.*;
import gov.nasa.worldwind.layers.ogc.OGCCapabilities;
import gov.nasa.worldwind.layers.ogc.wms.*;

import java.util.*;

/**
 * @author tag
 * @version $Id: WMSLayerInfo.java 1171 2013-02-11 21:45:02Z dcollins $
 */
public class WMSLayerInfo {
    private final OGCCapabilities caps;
    private AVList params = new AVListImpl();

    public WMSLayerInfo(OGCCapabilities caps, WMSLayerCapabilities layerCaps, WMSLayerStyle style) {
        this.caps = caps;
        this.params = new AVListImpl();
        this.params.set(AVKey.LAYER_NAMES, layerCaps.getName());
        if (style != null)
            this.params.set(AVKey.STYLE_NAMES, style.getName());

        String layerTitle = layerCaps.getTitle();
        this.params.set(AVKey.DISPLAY_NAME, layerTitle);
    }

    public static List<WMSLayerInfo> createLayerInfos(OGCCapabilities caps, WMSLayerCapabilities layerCaps) {
        // Create the layer info specified by the layer's capabilities entry and the selected style.
        List<WMSLayerInfo> layerInfos = new ArrayList<>();

        // An individual layer may have independent styles, and each layer/style combination is effectively one
        // visual layer. So here the individual layer/style combinations are formed.
        Set<WMSLayerStyle> styles = layerCaps.getStyles();
        if (styles == null || styles.isEmpty()) {
            layerInfos.add(new WMSLayerInfo(caps, layerCaps, null));
        }
        else {
            for (WMSLayerStyle style : styles) {
                WMSLayerInfo layerInfo = new WMSLayerInfo(caps, layerCaps, style);
                layerInfos.add(layerInfo);
            }
        }

        return layerInfos;
    }

    public String getTitle() {
        return params.getStringValue(AVKey.DISPLAY_NAME);
    }

    public OGCCapabilities getCaps() {
        return caps;
    }

    public AVList getParams() {
        return params;
    }
}
