/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.formats.geojson;

/**
 * @author dcollins
 * @version $Id: GeoJSONConstants.java 1171 2013-02-11 21:45:02Z dcollins $
 */
public interface GeoJSONConstants
{
    String FIELD_TYPE = "type";
    String FIELD_CRS = "crs";
    String FIELD_BBOX = "bbox";
    String FIELD_COORDINATES = "coordinates";
    String FIELD_GEOMETRIES = "geometries";
    String FIELD_GEOMETRY = "geometry";
    String FIELD_PROPERTIES = "properties";
    String FIELD_FEATURES = "features";

    String TYPE_POINT = "Point";
    String TYPE_MULTI_POINT = "MultiPoint";
    String TYPE_LINE_STRING = "LineString";
    String TYPE_MULTI_LINE_STRING = "MultiLineString";
    String TYPE_POLYGON = "Polygon";
    String TYPE_MULTI_POLYGON = "MultiPolygon";
    String TYPE_GEOMETRY_COLLECTION = "GeometryCollection";
    String TYPE_FEATURE = "Feature";
    String TYPE_FEATURE_COLLECTION = "FeatureCollection";
}
