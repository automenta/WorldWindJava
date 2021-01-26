/*
 * Copyright (C) 2014 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */

package gov.nasa.worldwind.terrain;

import gov.nasa.worldwind.Keys;
import gov.nasa.worldwind.avlist.*;
import gov.nasa.worldwind.exception.WWRuntimeException;
import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.layers.ogc.gml.GMLRectifiedGrid;
import gov.nasa.worldwind.layers.ogc.wcs.wcs100.*;
import gov.nasa.worldwind.retrieve.*;
import gov.nasa.worldwind.util.*;
import org.w3c.dom.*;

import java.net.*;
import java.util.List;

/**
 * @author tag
 * @version $Id: WCSElevationModel.java 2154 2014-07-17 21:32:34Z pabercrombie $
 */
public class WCSElevationModel extends BasicElevationModel {
    public WCSElevationModel(Element domElement, KV params) {
        super(WCSElevationModel.wcsGetParamsFromDocument(domElement, params));
    }

    public WCSElevationModel(WCS100Capabilities caps, KV params) {
        super(WCSElevationModel.wcsGetParamsFromCapsDoc(caps, params));
    }

    /**
     * Create a new elevation model from a serialized restorable state string.
     *
     * @param restorableStateInXml XML string in WorldWind restorable state format.
     * @see #getRestorableState()
     */
    public WCSElevationModel(String restorableStateInXml) {
        super(WCSElevationModel.wcsRestorableStateToParams(restorableStateInXml));

        RestorableSupport rs;
//        try {
            rs = RestorableSupport.parse(restorableStateInXml);
//        }
//        catch (RuntimeException e) {
//            // Parsing the document specified by stateInXml failed.
//            String message = Logging.getMessage("generic.ExceptionAttemptingToParseStateXml", restorableStateInXml);
//            Logging.logger().severe(message);
//            throw new IllegalArgumentException(message, e);
//        }

        this.doRestoreState(rs, null);
    }

    protected static KV wcsGetParamsFromDocument(Element domElement, KV params) {

        if (params == null)
            params = new KVMap();

        DataConfigurationUtils.getWCSConfigParams(domElement, params);
        BasicElevationModel.getBasicElevationModelConfigParams(domElement, params);
        WCSElevationModel.wcsSetFallbacks(params);

        params.set(Keys.TILE_URL_BUILDER, new URLBuilder(params.getStringValue(Keys.WCS_VERSION), params));

        return params;
    }

    protected static KV wcsGetParamsFromCapsDoc(WCS100Capabilities caps, KV params) {

        WCS100DescribeCoverage coverage = (WCS100DescribeCoverage) params.get(Keys.DOCUMENT);

        WCSElevationModel.getWCSElevationModelConfigParams(caps, coverage, params);

        WCSElevationModel.wcsSetFallbacks(params);
        WCSElevationModel.determineNumLevels(coverage, params);

        params.set(Keys.TILE_URL_BUILDER, new URLBuilder(caps.getVersion(), params));

        if (params.get(Keys.ELEVATION_EXTREMES_FILE) == null) {
            // Use the default extremes file if there are at least as many levels in this new elevation model as the
            // level of the extremes file, which is level 5.
            int numLevels = (Integer) params.get(Keys.NUM_LEVELS);
            if (numLevels >= 6)
                params.set(Keys.ELEVATION_EXTREMES_FILE, "config/SRTM30Plus_ExtremeElevations_5.bil");
        }

        return params;
    }

    protected static void wcsSetFallbacks(KV params) {
        if (params.get(Keys.LEVEL_ZERO_TILE_DELTA) == null) {
            Angle delta = new Angle(20);
            params.set(Keys.LEVEL_ZERO_TILE_DELTA, new LatLon(delta, delta));
        }

        if (params.get(Keys.TILE_WIDTH) == null)
            params.set(Keys.TILE_WIDTH, 150);

        if (params.get(Keys.TILE_HEIGHT) == null)
            params.set(Keys.TILE_HEIGHT, 150);

        if (params.get(Keys.FORMAT_SUFFIX) == null)
            params.set(Keys.FORMAT_SUFFIX, ".tif");

        if (params.get(Keys.MISSING_DATA_SIGNAL) == null)
            params.set(Keys.MISSING_DATA_SIGNAL, Double.NEGATIVE_INFINITY);

        if (params.get(Keys.NUM_LEVELS) == null)
            params.set(Keys.NUM_LEVELS, 18); // approximately 20 cm per pixel

        if (params.get(Keys.NUM_EMPTY_LEVELS) == null)
            params.set(Keys.NUM_EMPTY_LEVELS, 0);

        if (params.get(Keys.ELEVATION_MIN) == null)
            params.set(Keys.ELEVATION_MIN, -11000.0);

        if (params.get(Keys.ELEVATION_MAX) == null)
            params.set(Keys.ELEVATION_MAX, 8850.0);
    }

    protected static void determineNumLevels(WCS100DescribeCoverage coverage, KV params) {
        List<GMLRectifiedGrid> grids =
            coverage.getCoverageOfferings().get(0).getDomainSet().getSpatialDomain().getRectifiedGrids();
        if (grids.size() < 1 || grids.get(0).getOffsetVectors().size() < 2) {
            params.set(Keys.NUM_LEVELS, 18);
            return;
        }

        double xRes = Math.abs(grids.get(0).getOffsetVectors().get(0).x);
        double yRes = Math.abs(grids.get(0).getOffsetVectors().get(1).y);
        double dataResolution = Math.min(xRes, yRes);

        int tileSize = (Integer) params.get(Keys.TILE_WIDTH);
        LatLon level0Delta = (LatLon) params.get(Keys.LEVEL_ZERO_TILE_DELTA);

        double n = Math.log(level0Delta.getLat().degrees / (dataResolution * tileSize)) / Math.log(2);
        params.set(Keys.NUM_LEVELS, (int) (Math.ceil(n) + 1));
    }

    public static KV getWCSElevationModelConfigParams(WCS100Capabilities caps, WCS100DescribeCoverage coverage,
        KV params) {
        DataConfigurationUtils.getWCSConfigParameters(caps, coverage, params); // checks for null args

        // Ensure that we found all the necessary information.
        if (params.getStringValue(Keys.DATASET_NAME) == null) {
            Logging.logger().warning(Logging.getMessage("WCS.NoCoverageName"));
            throw new WWRuntimeException(Logging.getMessage("WCS.NoCoverageName"));
        }

        if (params.getStringValue(Keys.SERVICE) == null) {
            Logging.logger().warning(Logging.getMessage("WCS.NoGetCoverageURL"));
            throw new WWRuntimeException(Logging.getMessage("WCS.NoGetCoverageURL"));
        }

        if (params.getStringValue(Keys.DATA_CACHE_NAME) == null) {
            Logging.logger().warning(Logging.getMessage("nullValue.DataCacheIsNull"));
            throw new WWRuntimeException(Logging.getMessage("nullValue.DataCacheIsNull"));
        }

        if (params.getStringValue(Keys.IMAGE_FORMAT) == null) {
            Logging.logger().severe("WCS.NoImageFormats");
            throw new WWRuntimeException(Logging.getMessage("WCS.NoImageFormats"));
        }

        if (params.get(Keys.SECTOR) == null) {
            Logging.logger().severe("WCS.NoLonLatEnvelope");
            throw new WWRuntimeException(Logging.getMessage("WCS.NoLonLatEnvelope"));
        }

        if (params.getStringValue(Keys.COORDINATE_SYSTEM) == null) {
            String msg = Logging.getMessage("WCS.RequiredCRSNotSupported", "EPSG:4326");
            Logging.logger().severe(msg);
            throw new WWRuntimeException(msg);
        }

        return params;
    }

    protected static KV wcsRestorableStateToParams(String stateInXml) {

        RestorableSupport rs;
        try {
            rs = RestorableSupport.parse(stateInXml);
        }
        catch (RuntimeException e) {
            // Parsing the document specified by stateInXml failed.
            String message = Logging.getMessage("generic.ExceptionAttemptingToParseStateXml", stateInXml);
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message, e);
        }

        KV params = new KVMap();
        WCSElevationModel.wcsRestoreStateForParams(rs, null, params);
        return params;
    }

    protected static void wcsRestoreStateForParams(RestorableSupport rs, RestorableSupport.StateObject context,
        KV params) {
        // Invoke the BasicElevationModel functionality.
        BasicElevationModel.restoreStateForParams(rs, null, params);

        String s = rs.getStateValueAsString(context, Keys.IMAGE_FORMAT);
        if (s != null)
            params.set(Keys.IMAGE_FORMAT, s);

        s = rs.getStateValueAsString(context, Keys.TITLE);
        if (s != null)
            params.set(Keys.TITLE, s);

        s = rs.getStateValueAsString(context, Keys.DISPLAY_NAME);
        if (s != null)
            params.set(Keys.DISPLAY_NAME, s);

        RestorableSupport.adjustTitleAndDisplayName(params);

        s = rs.getStateValueAsString(context, Keys.COVERAGE_IDENTIFIERS);
        if (s != null)
            params.set(Keys.COVERAGE_IDENTIFIERS, s);

        s = rs.getStateValueAsString(context, Keys.WCS_VERSION);
        if (s != null)
            params.set(Keys.TILE_URL_BUILDER, new URLBuilder(s, params));
    }

    protected static Retriever downloadElevations(WMSBasicElevationModel.ElevationCompositionTile tile) throws Exception {
        URL url = tile.getResourceURL();

        Retriever retriever = new HTTPRetriever(url,
            new WMSBasicElevationModel.CompositionRetrievalPostProcessor(tile.getFile()));
        retriever.setConnectTimeout(10000);
        retriever.setReadTimeout(60000);
        retriever.call();
        return retriever;
    }

    /**
     * Appends WCS elevation model configuration elements to the superclass configuration document.
     *
     * @param params configuration parameters describing this WCS basic elevation model.
     * @return a WCS basic elevation model configuration document.
     */
    protected Document createConfigurationDocument(KV params) {
        Document doc = super.createConfigurationDocument(params);
        if (doc == null || doc.getDocumentElement() == null)
            return doc;

        DataConfigurationUtils.createWCSLayerConfigElements(params, doc.getDocumentElement());

        return doc;
    }

    //**************************************************************//
    //********************  Restorable Support  ********************//
    //**************************************************************//

    public void composeElevations(Sector sector, List<? extends LatLon> latlons, int tileWidth, double[] buffer)
        throws Exception {

        final int n = latlons.size();
        if (buffer.length < n || tileWidth > n) {
            String msg = Logging.getMessage("ElevationModel.ElevationsBufferTooSmall", n);
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        WMSBasicElevationModel.ElevationCompositionTile tile = new WMSBasicElevationModel.ElevationCompositionTile(
            sector, this.getLevels().getLastLevel(),
            tileWidth, n / tileWidth);

        Retriever rr = WCSElevationModel.downloadElevations(tile);
        tile.setElevations(this.readElevations(rr.getBuffer(), tile.getFile().toURI().toURL()), this);

        for (int i = 0; i < n; i++) {
            LatLon ll = latlons.get(i);
            if (ll == null)
                continue;

            double value = this.lookupElevation(ll.getLat(), ll.getLon(), tile);

            // If an elevation at the given location is available, then write that elevation to the destination buffer.
            // Otherwise do nothing.
            if (value != this.getMissingDataSignal())
                buffer[i] = value;
        }
    }

    @Override
    public void getRestorableStateForAVPair(String key, Object value,
        RestorableSupport rs, RestorableSupport.StateObject context) {
        if (value instanceof URLBuilder) {
            rs.addStateValueAsString(context, Keys.WCS_VERSION, ((URLBuilder) value).serviceVersion);
        } else if (!(value instanceof WCS100DescribeCoverage)) {
            // Don't pass DescribeCoverage to superclass. The DescribeCoverage parameters will already be present in the
            // parameter list, so do nothing here.
            super.getRestorableStateForAVPair(key, value, rs, context);
        }
    }

    protected static class URLBuilder implements TileUrlBuilder {
        protected final String layerNames;
        protected final String serviceVersion;
        private final String imageFormat;
        protected String URLTemplate;

        protected URLBuilder(String version, KV params) {
            this.serviceVersion = version;
            this.layerNames = params.getStringValue(Keys.COVERAGE_IDENTIFIERS);
            this.imageFormat = params.getStringValue(Keys.IMAGE_FORMAT);
        }

        public URL getURL(Tile tile, String altImageFormat) throws MalformedURLException {
            StringBuffer sb;
            if (this.URLTemplate == null) {
                sb = new StringBuffer(tile.level.getService());

                if (!sb.toString().toLowerCase().contains("service=wcs"))
                    sb.append("service=WCS");
                sb.append("&request=GetCoverage");
                sb.append("&version=");
                sb.append(this.serviceVersion);
                sb.append("&crs=EPSG:4326");
                sb.append("&coverage=");
                sb.append(this.layerNames);
                sb.append("&format=");
                if (altImageFormat == null)
                    sb.append(this.imageFormat);
                else
                    sb.append(altImageFormat);

                this.URLTemplate = sb.toString();
            } else {
                sb = new StringBuffer(this.URLTemplate);
            }

            sb.append("&width=");
            sb.append(tile.getWidth());
            sb.append("&height=");
            sb.append(tile.getHeight());

            Sector s = tile.sector;
            sb.append("&bbox=");
            sb.append(s.lonMin().degrees);
            sb.append(',');
            sb.append(s.latMin().degrees);
            sb.append(',');
            sb.append(s.lonMax().degrees);
            sb.append(',');
            sb.append(s.latMax().degrees);

            sb.append('&'); // terminate the query string

            return new URL(sb.toString().replace(" ", "%20"));
        }
    }
}