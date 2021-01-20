/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.formats.geojson;

import gov.nasa.worldwind.avlist.KV;

/**
 * @author dcollins
 * @version $Id: GeoJSONFeatureCollection.java 1171 2013-02-11 21:45:02Z dcollins $
 */
public class GeoJSONFeatureCollection extends GeoJSONObject {
    public GeoJSONFeatureCollection(KV fields) {
        super(fields);
    }

    @Override
    public boolean isFeatureCollection() {
        return true;
    }

    public GeoJSONFeature[] getFeatures() {
        return (GeoJSONFeature[]) this.get(GeoJSONConstants.FIELD_FEATURES);
    }
}