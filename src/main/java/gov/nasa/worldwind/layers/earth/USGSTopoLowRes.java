/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */

package gov.nasa.worldwind.layers.earth;

import gov.nasa.worldwind.layers.wms.WMSTiledImageLayer;
import gov.nasa.worldwind.util.WWXML;
import org.w3c.dom.Document;

/**
 * @author tag
 * @version $Id: USGSTopoLowRes.java 1171 2013-02-11 21:45:02Z dcollins $
 */
public class USGSTopoLowRes extends WMSTiledImageLayer {
    public USGSTopoLowRes() {
        super(USGSTopoLowRes.getConfigurationDocument(), null);
    }

    protected static Document getConfigurationDocument() {
        return WWXML.openDocumentFile("config/Earth/USGSTopoLowResLayer.xml", null);
    }
}
