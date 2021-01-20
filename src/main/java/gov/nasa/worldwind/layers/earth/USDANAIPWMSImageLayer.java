/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.layers.earth;

import gov.nasa.worldwind.Keys;
import gov.nasa.worldwind.layers.wms.WMSTiledImageLayer;
import gov.nasa.worldwind.util.WWXML;
import org.w3c.dom.Document;

/**
 * @author garakl
 * @version $Id: USDANAIPWMSImageLayer.java 2257 2014-08-22 18:02:19Z tgaskins $
 */

public class USDANAIPWMSImageLayer extends WMSTiledImageLayer {
    public USDANAIPWMSImageLayer() {
        super(USDANAIPWMSImageLayer.getConfigurationDocument(), null);
    }

    protected static Document getConfigurationDocument() {
        return WWXML.openDocumentFile("config/Earth/USDANAIPWMSImageLayer.xml", null);
    }

    public String toString() {
        String o = this.getStringValue(Keys.DISPLAY_NAME);
        return o != null ? o : "USDA FSA Imagery";
    }
}