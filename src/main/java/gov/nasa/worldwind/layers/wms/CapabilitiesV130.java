/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.layers.wms;

import org.w3c.dom.*;

import javax.xml.xpath.XPath;
import java.util.*;

/**
 * Version-dependent class for gathering information from a wms capabilities document.
 *
 * @author Tom Gaskins
 * @version $Id: CapabilitiesV130.java 1171 2013-02-11 21:45:02Z dcollins $
 */

public class CapabilitiesV130 extends Capabilities {
    public CapabilitiesV130(Document doc, XPath xpath) {
        super(doc, xpath);
    }

    @Override
    public BoundingBox getLayerGeographicBoundingBox(Element layer) {
        Element e = this.getElement(layer, "ancestor-or-self::wms:Layer/wms:EX_GeographicBoundingBox");

        return e == null ? null : BoundingBox.createFromStrings("CRS:84",
            this.getWestBoundLongitude(e), this.getEastBoundLongitude(e),
            this.getSouthBoundLatitude(e), this.getNorthBoundLatitude(e),
            null, null);
    }

    public BoundingBox[] getLayerBoundingBoxes(Element layer) {
        Element[] es = this.getElements(layer, "ancestor-or-self::wms:Layer/wms:BoundingBox");
        if (es == null)
            return null;

        ArrayList<BoundingBox> bboxes = new ArrayList<>();
        Collection<String> crses = new ArrayList<>();

        for (Element e : es) {
            if (e == null)
                continue;

            BoundingBox bb = BoundingBox.createFromStrings(this.getBoundingBoxCRS(e),
                this.getBoundingBoxMinx(e), this.getBoundingBoxMaxx(e),
                this.getBoundingBoxMiny(e), this.getBoundingBoxMaxy(e),
                this.getBoundingBoxResx(e), this.getBoundingBoxResy(e));

            // Add the bbox only if the ancestor's crs is not one of those in the node's crs.
            if (bb.getCrs() != null && !crses.contains(bb.getCrs())) {
                crses.add(bb.getCrs());
                bboxes.add(bb);
            }
        }

        return bboxes.isEmpty() ? null : bboxes.toArray(new BoundingBox[0]);
    }

    @Override
    public String getLayerMaxScaleDenominator(Element layer) {
        return this.getText(layer, "ancestor-or-self::wms:Layer/wms:MaxScaleDenominator");
    }

    @Override
    public String getLayerMinScaleDenominator(Element layer) {
        return this.getText(layer, "ancestor-or-self::wms:Layer/wms:MinScaleDenominator");
    }
}
