/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.layers;

import gov.nasa.worldwind.*;
import gov.nasa.worldwind.avlist.KV;
import gov.nasa.worldwind.exception.WWRuntimeException;
import gov.nasa.worldwind.formats.geojson.*;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.render.*;
import gov.nasa.worldwind.util.*;

import java.util.logging.Level;

/**
 * Utility class to load data from a GeoJSON source into a layer.
 *
 * @author dcollins
 * @version $Id: GeoJSONLoader.java 2326 2014-09-17 22:35:45Z dcollins $
 */
public class GeoJSON {
    protected static final RandomShapeAttributes randomAttrs = new RandomShapeAttributes();
    private final GeoJSONRenderer geoJSONRenderer;

    /**
     * Create a new loader.
     * @param r
     */
    public GeoJSON(GeoJSONRenderer r) {
        geoJSONRenderer = r;
    }

    protected static boolean positionsHaveNonzeroAltitude(Iterable<? extends Position> positions) {
        for (Position pos : positions) {
            if (pos.getAltitude() != 0)
                return true;
        }

        return false;
    }

    protected static void handleUnrecognizedObject(Object o) {
        Logging.logger().warning(Logging.getMessage("generic.UnrecognizedObjectType", o));
    }

    //**************************************************************//
    //********************  Geometry Conversion  *******************//
    //**************************************************************//

    /**
     * Parse a GeoJSON document and add it to a layer.
     *
     * @param docSource GeoJSON document. May be a file path {@link String}, {@link java.io.File}, {@link java.net.URL},
     *                  or {@link java.net.URI}.
     * @param layer     layer to receive the new Renderable.
     */
    public RenderableLayer load(Object docSource, RenderableLayer layer) {
        if (WWUtil.isEmpty(docSource)) {
            String message = Logging.getMessage("nullValue.SourceIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        try {

            final Object root = new GeoJSONDoc(docSource).getRoot();

            if (root instanceof GeoJSONObject) {
                this.addGeoJSONGeometryToLayer((GeoJSONObject) root, layer);
            } else if (root instanceof Object[]) {
                for (Object o : (Object[]) root) {
                    if (o instanceof GeoJSONObject) {
                        this.addGeoJSONGeometryToLayer((GeoJSONObject) o, layer);
                    } else {
                        GeoJSON.handleUnrecognizedObject(o);
                    }
                }
            } else {
                GeoJSON.handleUnrecognizedObject(root);
            }
        } catch (Exception e) {
            String message = Logging.getMessage("generic.ExceptionAttemptingToReadGeoJSON", docSource);
            Logging.logger().log(Level.SEVERE, message, e);
            throw new WWRuntimeException(message, e);
        }
        return layer;
    }

    /**
     * Create a layer from a GeoJSON document.
     *
     * @param object GeoJSON object to be added to the layer.
     * @param layer  layer to receive the new GeoJSON renderable.
     */
    public void addGeoJSONGeometryToLayer(GeoJSONObject object, RenderableLayer layer) {

        if (object.isGeometry())
            geoJSONRenderer.addRenderableForGeometry(object.asGeometry(), layer, null);

        else if (object.isFeature())
            geoJSONRenderer.addRenderableForFeature(object.asFeature(), layer);

        else if (object.isFeatureCollection())
            geoJSONRenderer.addRenderableForFeatureCollection(object.asFeatureCollection(), layer);

        else
            GeoJSON.handleUnrecognizedObject(object);
    }


    /**
     * Create a layer from a GeoJSON object.
     *
     * @param object GeoJSON object to use to create a Renderable, which will be added to the new layer.
     * @return the new layer.
     */
    public Layer layer(GeoJSONObject object) {
        RenderableLayer layer = new RenderableLayer();
        addGeoJSONGeometryToLayer(object, layer);
        return layer;
    }

    //**************************************************************//
    //********************  Attribute Construction  ****************//
    //**************************************************************//


    public static class GeoJSONRenderer {
        public GeoJSONRenderer() {
        }

        @SuppressWarnings("UnusedDeclaration")
        protected static Renderable createPoint(GeoJSONGeometry owner, Position pos, PointPlacemarkAttributes attrs,
            KV properties) {
            PointPlacemark p = new PointPlacemark(pos);
            p.setAttributes(attrs);
            if (pos.getAltitude() != 0) {
                p.setAltitudeMode(WorldWind.ABSOLUTE);
                p.setLineEnabled(true);
            } else {
                p.setAltitudeMode(WorldWind.CLAMP_TO_GROUND);
            }

            if (properties != null)
                p.set(Keys.PROPERTIES, properties);

            return p;
        }

        @SuppressWarnings("UnusedDeclaration")
        protected static Renderable createPolyline(GeoJSONGeometry owner, Iterable<? extends Position> positions,
            ShapeAttributes attrs, KV properties) {
            if (GeoJSON.positionsHaveNonzeroAltitude(positions)) {
                Path p = new Path();
                p.setPositions(positions);
                p.setAltitudeMode(WorldWind.ABSOLUTE);
                p.setAttributes(attrs);

                if (properties != null)
                    p.set(Keys.PROPERTIES, properties);

                return p;
            } else {
                SurfacePolyline sp = new SurfacePolyline(attrs, positions);

                if (properties != null)
                    sp.set(Keys.PROPERTIES, properties);

                return sp;
            }
        }

        @SuppressWarnings("UnusedDeclaration")
        protected static Renderable createPolygon(GeoJSONGeometry owner, Iterable<? extends Position> outerBoundary,
            Iterable<? extends Position>[] innerBoundaries, ShapeAttributes attrs, KV properties) {
            if (GeoJSON.positionsHaveNonzeroAltitude(outerBoundary)) {
                Polygon poly = new Polygon(outerBoundary);
                poly.setAttributes(attrs);

                if (innerBoundaries != null) {
                    for (Iterable<? extends Position> iter : innerBoundaries) {
                        poly.addInnerBoundary(iter);
                    }
                }

                if (properties != null)
                    poly.set(Keys.PROPERTIES, properties);

                return poly;
            } else {
                SurfacePolygon poly = new SurfacePolygon(attrs, outerBoundary);

                if (innerBoundaries != null) {
                    for (Iterable<? extends Position> iter : innerBoundaries) {
                        poly.addInnerBoundary(iter);
                    }
                }

                if (properties != null)
                    poly.set(Keys.PROPERTIES, properties);

                return poly;
            }
        }

        protected void addRenderableForGeometry(GeoJSONGeometry geom, RenderableLayer layer, KV properties) {
            if (geom.isPoint())
                addRenderableForPoint(geom.asPoint(), layer, properties);

            else if (geom.isMultiPoint())
                addRenderableForMultiPoint(geom.asMultiPoint(), layer, properties);

            else if (geom.isLineString())
                addRenderableForLineString(geom.asLineString(), layer, properties);

            else if (geom.isMultiLineString())
                addRenderableForMutiLineString(geom.asMultiLineString(), layer, properties);

            else if (geom.isPolygon())
                addRenderableForPolygon(geom.asPolygon(), layer, properties);

            else if (geom.isMultiPolygon())
                addRenderableForMultiPolygon(geom.asMultiPolygon(), layer, properties);

            else if (geom.isGeometryCollection())
                addRenderableForGeometryCollection(geom.asGeometryCollection(), layer, properties);

            else
                GeoJSON.handleUnrecognizedObject(geom);
        }

        protected void addRenderableForGeometryCollection(GeoJSONGeometryCollection c, RenderableLayer layer,
            KV properties) {
            if (c.getGeometries() == null || c.getGeometries().length == 0)
                return;

            for (GeoJSONGeometry geom : c.getGeometries()) {
                addRenderableForGeometry(geom, layer, properties);
            }
        }

        protected void addRenderableForFeature(GeoJSONFeature feature, RenderableLayer layer) {
            if (feature.getGeometry() == null) {
                Logging.logger().warning(Logging.getMessage("nullValue.GeometryIsNull"));
                return;
            }

            addRenderableForGeometry(feature.getGeometry(), layer, feature.getProperties());
        }

        protected void addRenderableForFeatureCollection(GeoJSONFeatureCollection c, RenderableLayer layer) {
            if (c.getFeatures() != null && c.getFeatures().length == 0)
                return;

            for (GeoJSONFeature feat : c.getFeatures()) {
                addRenderableForFeature(feat, layer);
            }
        }

        protected void addRenderableForPoint(GeoJSONPoint geom, RenderableLayer layer, KV properties) {
            PointPlacemarkAttributes attrs = createPointAttributes(geom, layer);

            layer.add(GeoJSONRenderer.createPoint(geom, geom.getPosition(), attrs, properties));
        }

        protected void addRenderableForMultiPoint(GeoJSONMultiPoint geom, RenderableLayer layer, KV properties) {
            PointPlacemarkAttributes attrs = createPointAttributes(geom, layer);

            for (int i = 0; i < geom.getPointCount(); i++) {
                layer.add(GeoJSONRenderer.createPoint(geom, geom.getPosition(i), attrs, properties));
            }
        }

        protected void addRenderableForLineString(GeoJSONLineString geom, RenderableLayer layer, KV properties) {
            ShapeAttributes attrs = createPolylineAttributes(geom, layer);

            layer.add(GeoJSONRenderer.createPolyline(geom, geom.getCoordinates(), attrs, properties));
        }

        protected void addRenderableForMutiLineString(GeoJSONMultiLineString geom, RenderableLayer layer,
            KV properties) {
            ShapeAttributes attrs = createPolylineAttributes(geom, layer);

            for (GeoJSONPositionArray coords : geom.getCoordinates()) {
                layer.add(GeoJSONRenderer.createPolyline(geom, coords, attrs, properties));
            }
        }

        protected void addRenderableForPolygon(GeoJSONPolygon geom, RenderableLayer layer, KV properties) {
            ShapeAttributes attrs = createPolygonAttributes(geom, layer);

            layer.add(GeoJSONRenderer.createPolygon(geom, geom.getExteriorRing(), geom.getInteriorRings(), attrs,
                properties));
        }

        protected void addRenderableForMultiPolygon(GeoJSONMultiPolygon geom, RenderableLayer layer,
            KV properties) {
            ShapeAttributes attrs = createPolygonAttributes(geom, layer);

            for (int i = 0; i < geom.getPolygonCount(); i++) {
                layer.add(GeoJSONRenderer.createPolygon(geom, geom.getExteriorRing(i), geom.getInteriorRings(i), attrs,
                        properties));
            }
        }

        @SuppressWarnings("UnusedDeclaration")
        protected PointPlacemarkAttributes createPointAttributes(GeoJSONGeometry geom, Layer layer) {
            if (layer == null)
                return GeoJSON.randomAttrs.nextAttributes().asPointAttributes();

            String key = getClass().getName() + ".PointAttributes";
            PointPlacemarkAttributes attrs = (PointPlacemarkAttributes) layer.get(key);
            if (attrs == null) {
                attrs = GeoJSON.randomAttrs.nextAttributes().asPointAttributes();
                layer.set(key, attrs);
            }

            return attrs;
        }

        @SuppressWarnings("UnusedDeclaration")
        protected ShapeAttributes createPolylineAttributes(GeoJSONGeometry geom, Layer layer) {
            if (layer == null)
                return GeoJSON.randomAttrs.nextAttributes().asShapeAttributes();

            String key = getClass().getName() + ".PolylineAttributes";
            ShapeAttributes attrs = (ShapeAttributes) layer.get(key);
            if (attrs == null) {
                attrs = GeoJSON.randomAttrs.nextAttributes().asShapeAttributes();
                layer.set(key, attrs);
            }

            return attrs;
        }

        @SuppressWarnings("UnusedDeclaration")
        protected ShapeAttributes createPolygonAttributes(GeoJSONGeometry geom, Layer layer) {
            if (layer == null)
                return GeoJSON.randomAttrs.nextAttributes().asShapeAttributes();

            String key = getClass().getName() + ".PolygonAttributes";
            ShapeAttributes attrs = (ShapeAttributes) layer.get(key);
            if (attrs == null) {
                attrs = GeoJSON.randomAttrs.nextAttributes().asShapeAttributes();
                layer.set(key, attrs);
            }

            return attrs;
        }
    }
}