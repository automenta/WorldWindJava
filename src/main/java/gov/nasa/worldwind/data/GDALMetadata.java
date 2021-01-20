/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */

package gov.nasa.worldwind.data;

import gov.nasa.worldwind.Keys;
import gov.nasa.worldwind.avlist.*;
import gov.nasa.worldwind.exception.WWRuntimeException;
import gov.nasa.worldwind.util.*;
import org.gdal.gdal.Dataset;
import org.gdal.osr.SpatialReference;

import java.util.Hashtable;
import java.util.logging.Level;

/**
 * @author Lado Garakanidze
 * @version $
 */

public class GDALMetadata {
    protected static final String NITF_ONAME = "NITF_ONAME";
    protected static final String NITF_ISORCE = "NITF_ISORCE";
    protected static final String NITF_IREP = "NITF_IREP";
    protected static final String NITF_ABPP = "NITF_ABPP";
    protected static final String NITF_FBKGC = "NITF_FBKGC";
    protected static final String NITF_DYNAMIC_RANGE = "NITF_USE00A_DYNAMIC_RANGE";

    protected GDALMetadata() {
    }

    public static KV extractExtendedAndFormatSpecificMetadata(Dataset ds, KV extParams, KV params)
        throws IllegalArgumentException, WWRuntimeException {
        if (null == ds) {
            String message = Logging.getMessage("nullValue.DataSetIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (null == extParams) {
            extParams = new KVMap();
        }

        try {
            Hashtable dict = ds.GetMetadata_Dict("");
            if (null != dict) {
                for (Object o : dict.keySet()) {
                    if (o instanceof String) {
                        String key = (String) o;
                        Object value = dict.get(key);
                        if (!WWUtil.isEmpty(value)) {
                            extParams.set(key, value);
                        }
                    }
                }
            }
        }
        catch (Throwable t) {
            Logging.logger().log(Level.FINE, t.getMessage(), t);
        }

        return GDALMetadata.mapExtendedMetadata(ds, extParams, params);
    }

    static protected KV mapExtendedMetadata(Dataset ds, KV extParams, KV params) {
        params = (null == params) ? new KVMap() : params;

        if (null == extParams) {
            return params;
        }

        GDALMetadata.convertToWorldWind(extParams, params);

        String drvName = (null != ds) ? ds.GetDriver().getShortName() : "";

        if ("NITF".equals(drvName)) {
            GDALMetadata.mapNITFMetadata(extParams, params);
        }

        return params;
    }

    protected static void mapNITFMetadata(KV extParams, KV params) {
        if (extParams.hasKey(GDALMetadata.NITF_ONAME)) {
            // values: GeoEye, DigitalGlobe
        }

        if (extParams.hasKey(GDALMetadata.NITF_ISORCE)) {
            // values: GEOEYE1,DigitalGlobe
        }

        if (extParams.hasKey(GDALMetadata.NITF_IREP)) {
            // values: RGB/LUT/MONO/MULTI
        }

        // Extract Actual Bit-Per-Pixel
        if (extParams.hasKey(GDALMetadata.NITF_ABPP)) {
            Object o = extParams.get(GDALMetadata.NITF_ABPP);
            if (!WWUtil.isEmpty(o) && o instanceof String) {
                Integer abpp = WWUtil.convertStringToInteger((String) o);
                if (null != abpp)
                    params.set(Keys.RASTER_BAND_ACTUAL_BITS_PER_PIXEL, abpp);
            }
        }

        if (extParams.hasKey(GDALMetadata.NITF_DYNAMIC_RANGE)) {
            Object o = extParams.get(GDALMetadata.NITF_DYNAMIC_RANGE);
            if (!WWUtil.isEmpty(o) && o instanceof String) {
                Double maxPixelValue = WWUtil.convertStringToDouble((String) o);
                if (null != maxPixelValue)
                    params.set(Keys.RASTER_BAND_MAX_PIXEL_VALUE, maxPixelValue);
            }
        }

        if (extParams.hasKey(GDALMetadata.NITF_FBKGC)) {
            Object o = extParams.get(GDALMetadata.NITF_FBKGC);
            if (!WWUtil.isEmpty(o) && o instanceof String) {
                try {
                    String[] s = ((String) o).split(",");
                }
                catch (RuntimeException e) {
                    String msg = Logging.getMessage("generic.CannotCreateColor", o);
                    Logging.logger().severe(msg);
                }
            }
        }
    }

    public static KV convertToWorldWind(KV extParams, KV destParams) {
        if (null == destParams) {
            destParams = new KVMap();
        }

        if (null == extParams) {
            return destParams;
        }

        String proj = null, zone = null, ellps = null, datum = null, units = null;
        Integer epsg = null;

        if (extParams.hasKey("GEOTIFF_CHAR__ProjectedCSTypeGeoKey")) {
            proj = extParams.getStringValue("GEOTIFF_CHAR__ProjectedCSTypeGeoKey");
            proj = (null != proj) ? proj.toUpperCase() : null;

            int idx = (null != proj) ? proj.indexOf("ZONE_") : -1;
            if (idx != -1) {
                zone = proj.substring(idx + 5);
                zone = zone.toUpperCase();
            }
        }

        if (null == proj && extParams.hasKey("IMG__PROJECTION_NAME")) {
            proj = extParams.getStringValue("IMG__PROJECTION_NAME");
            proj = (null != proj) ? proj.toUpperCase() : null;
        }

        if (null == zone && extParams.hasKey("IMG__PROJECTION_ZONE")) {
            zone = extParams.getStringValue("IMG__PROJECTION_ZONE");
            zone = (null != zone) ? zone.toUpperCase() : null;
        }

        if (null != proj && proj.contains("UTM")) {
            destParams.set(Keys.COORDINATE_SYSTEM, Keys.COORDINATE_SYSTEM_PROJECTED);
            destParams.set(Keys.PROJECTION_NAME, Keys.PROJECTION_UTM);

            if (null != zone) {
                if (!zone.isEmpty() && zone.charAt(zone.length() - 1) == 'N') {
                    destParams.set(Keys.PROJECTION_HEMISPHERE, Keys.NORTH);
                    zone = zone.substring(0, zone.length() - 1);
                } else if (!zone.isEmpty() && zone.charAt(zone.length() - 1) == 'S') {
                    destParams.set(Keys.PROJECTION_HEMISPHERE, Keys.SOUTH);
                    zone = zone.substring(0, zone.length() - 1);
                }

                Integer i = WWUtil.makeInteger(zone.trim());
                if (i != null && i >= 1 && i <= 60) {
                    destParams.set(Keys.PROJECTION_ZONE, i);
                }
            }
        }

        if (extParams.hasKey("IMG__SPHEROID_NAME")) {
            String s = extParams.getStringValue("IMG__SPHEROID_NAME");
            if (s != null) {
                s = s.toUpperCase();
                if (s.contains("WGS") && s.contains("84")) {
                    ellps = datum = "WGS84";
                    destParams.set(Keys.PROJECTION_DATUM, datum);
                }
            }
        }

        if (extParams.hasKey("IMG__HORIZONTAL_UNITS")) {
            String s = extParams.getStringValue("IMG__HORIZONTAL_UNITS");
            if (s != null) {
                s = s.toLowerCase();
                if (s.contains("meter") || s.contains("metre")) {
                    units = Keys.UNIT_METER;
                }
                if (s.contains("feet") || s.contains("foot")) {
                    units = Keys.UNIT_FOOT;
                }

                if (null != units) {
                    destParams.set(Keys.PROJECTION_UNITS, units);
                }
            }
        }

        if (extParams.hasKey("GEOTIFF_NUM__3072__ProjectedCSTypeGeoKey")) {
            String s = extParams.getStringValue("GEOTIFF_NUM__3072__ProjectedCSTypeGeoKey");
            if (s != null) {
                epsg = WWUtil.makeInteger(s.trim());
            }
        }

        if (null == epsg && extParams.hasKey("GEO__ProjectedCSTypeGeoKey")) {
            String s = extParams.getStringValue("GEO__ProjectedCSTypeGeoKey");
            if (s != null) {
                epsg = WWUtil.makeInteger(s.trim());
            }
        }

        if (null != epsg) {
            destParams.set(Keys.PROJECTION_EPSG_CODE, epsg);
        }

        StringBuilder proj4 = new StringBuilder();

        if (Keys.COORDINATE_SYSTEM_PROJECTED.equals(destParams.get(Keys.COORDINATE_SYSTEM))) {
            //        +proj=utm +zone=38 +ellps=WGS84 +datum=WGS84 +units=m

            if (Keys.PROJECTION_UTM.equals(destParams.get(Keys.PROJECTION_NAME))) {
                proj4.append("+proj=utm");
            }

            if (destParams.hasKey(Keys.PROJECTION_ZONE)) {
                proj4.append(" +zone=").append(destParams.get(Keys.PROJECTION_ZONE));
            }

            if (destParams.hasKey(Keys.PROJECTION_DATUM)) {
                proj4.append(" +ellps=").append(destParams.get(Keys.PROJECTION_DATUM));
                proj4.append(" +datum=").append(destParams.get(Keys.PROJECTION_DATUM));
            }

            if (destParams.hasKey(Keys.PROJECTION_UNITS)) {
                proj4.append(" +units=").append(
                    Keys.UNIT_METER.equals(destParams.get(Keys.PROJECTION_UNITS)) ? "m" : "f"
                );
            }

            try {
                SpatialReference srs = new SpatialReference();
                srs.ImportFromProj4(proj4.toString());
                destParams.set(Keys.SPATIAL_REFERENCE_WKT, srs.ExportToWkt());
            }
            catch (Throwable t) {
                Logging.logger().log(Level.FINEST, t.getMessage(), t);
            }
        }

        return destParams;
    }
}