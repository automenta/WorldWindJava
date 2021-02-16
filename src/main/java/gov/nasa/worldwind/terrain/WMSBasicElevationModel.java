/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */

package gov.nasa.worldwind.terrain;

import gov.nasa.worldwind.Keys;
import gov.nasa.worldwind.avlist.*;
import gov.nasa.worldwind.exception.WWRuntimeException;
import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.layers.ogc.wms.WMSCapabilities;
import gov.nasa.worldwind.retrieve.*;
import gov.nasa.worldwind.util.*;
import org.w3c.dom.*;

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.List;

/**
 * @author tag
 * @version $Id: WMSBasicElevationModel.java 2050 2014-06-09 18:52:26Z tgaskins $
 */
public class WMSBasicElevationModel extends BasicElevationModel {
    private static final String[] formatOrderPreference = {
        "application/bil32", "application/bil16", "application/bil", "image/bil", "image/png", "image/tiff"
    };

    public WMSBasicElevationModel(KV params) {
        super(params);
    }

    public WMSBasicElevationModel(Element domElement, KV params) {
        this(WMSBasicElevationModel.wmsGetParamsFromDocument(domElement, params));
    }

    public WMSBasicElevationModel(WMSCapabilities caps, KV params) {
        this(WMSBasicElevationModel.wmsGetParamsFromCapsDoc(caps, params));
    }

    protected static KV wmsGetParamsFromDocument(Element domElement, KV params) {

        if (params == null)
            params = new KVMap();

        DataConfigurationUtils.getWMSLayerConfigParams(domElement, params);
        BasicElevationModel.getBasicElevationModelConfigParams(domElement, params);
        WMSBasicElevationModel.wmsSetFallbacks(params);

        params.set(Keys.TILE_URL_BUILDER, new URLBuilder(params.getStringValue(Keys.WMS_VERSION), params));

        return params;
    }

    protected static KV wmsGetParamsFromCapsDoc(WMSCapabilities caps, KV params) {

        String wmsVersion;
        try {
            wmsVersion = caps.getVersion();
            WMSBasicElevationModel.getWMSElevationModelConfigParams(caps, WMSBasicElevationModel.formatOrderPreference, params);
        }
        catch (IllegalArgumentException e) {
            String message = Logging.getMessage("WMS.MissingLayerParameters");
            Logging.logger().log(java.util.logging.Level.SEVERE, message, e);
            throw new IllegalArgumentException(message, e);
        }
        catch (WWRuntimeException e) {
            String message = Logging.getMessage("WMS.MissingCapabilityValues");
            Logging.logger().log(java.util.logging.Level.SEVERE, message, e);
            throw new IllegalArgumentException(message, e);
        }

        WMSBasicElevationModel.wmsSetFallbacks(params);

        params.set(Keys.TILE_URL_BUILDER, new URLBuilder(wmsVersion, params));

        return params;
    }

    protected static void wmsSetFallbacks(KV params) {
        if (params.get(Keys.LEVEL_ZERO_TILE_DELTA) == null) {
            Angle delta = new Angle(20);
            params.set(Keys.LEVEL_ZERO_TILE_DELTA, new LatLon(delta, delta));
        }

        if (params.get(Keys.TILE_WIDTH) == null)
            params.set(Keys.TILE_WIDTH, 150);

        if (params.get(Keys.TILE_HEIGHT) == null)
            params.set(Keys.TILE_HEIGHT, 150);

        if (params.get(Keys.FORMAT_SUFFIX) == null)
            params.set(Keys.FORMAT_SUFFIX, ".bil");

        if (params.get(Keys.MISSING_DATA_SIGNAL) == null)
            params.set(Keys.MISSING_DATA_SIGNAL, Double.NEGATIVE_INFINITY);

        if (params.get(Keys.NUM_LEVELS) == null)
            params.set(Keys.NUM_LEVELS, 18); // approximately 20 cm per pixel

        if (params.get(Keys.NUM_EMPTY_LEVELS) == null)
            params.set(Keys.NUM_EMPTY_LEVELS, 0);
    }

    /**
     * Parses WMSBasicElevationModel configuration parameters from a specified WMS Capabilities source. This writes
     * output as key-value pairs to params. Supported key and parameter names are: <table><caption style="font-weight:
     * bold;">Parameters</caption>
     * <tr><th>Parameter</th><th>Value</th><th>Type</th></tr>
     * <tr><td>{@link Keys#ELEVATION_MAX}</td><td>WMS layer's
     * maximum extreme elevation</td><td>Double</td></tr> <tr><td>{@link Keys#ELEVATION_MIN}</td><td>WMS layer's
     * minimum extreme elevation</td><td>Double</td></tr> <tr><td>{@link Keys#DATA_TYPE}</td><td>Translate WMS layer's
     * image format to a matching data type</td><td>String</td></tr> </table> This also parses common WMS layer
     * parameters by invoking {@link DataConfigurationUtils#getWMSLayerConfigParams(WMSCapabilities, String[],
     * KV)}.
     *
     * @param caps                  the WMS Capabilities source to parse for WMSBasicElevationModel configuration
     *                              parameters.
     * @param formatOrderPreference an ordered array of preferred image formats, or null to use the default format.
     * @param params                the output key-value pairs which recieve the WMSBasicElevationModel configuration
     *                              parameters.
     * @return a reference to params.
     * @throws IllegalArgumentException if either the document or params are null, or if params does not contain the
     *                                  required key-value pairs.
     * @throws WWRuntimeException       if the Capabilities document does not contain any of the required information.
     */
    public static KV getWMSElevationModelConfigParams(WMSCapabilities caps, String[] formatOrderPreference,
        KV params) {
//        if (caps == null) {
//            String message = Logging.getMessage("nullValue.WMSCapabilities");
//            Logging.logger().severe(message);
//            throw new IllegalArgumentException(message);
//        }
//
//        if (params == null) {
//            String message = Logging.getMessage("nullValue.ElevationModelConfigParams");
//            Logging.logger().severe(message);
//            throw new IllegalArgumentException(message);
//        }

        // Get common WMS layer parameters.
        DataConfigurationUtils.getWMSLayerConfigParams(caps, formatOrderPreference, params);

        // Attempt to extract the WMS layer names from the specified parameters.
        String layerNames = params.getStringValue(Keys.LAYER_NAMES);
        if (layerNames == null || layerNames.isEmpty()) {
            String message = Logging.getMessage("nullValue.WMSLayerNames");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        String[] names = layerNames.split(",");
        if (names.length == 0) {
            String message = Logging.getMessage("nullValue.WMSLayerNames");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        // Get the layer's extreme elevations.
        Double[] extremes = caps.getLayerExtremeElevations(names);

        Double d = (Double) params.get(Keys.ELEVATION_MIN);
        if (d == null && extremes != null && extremes[0] != null)
            params.set(Keys.ELEVATION_MIN, extremes[0]);

        d = (Double) params.get(Keys.ELEVATION_MAX);
        if (d == null && extremes != null && extremes[1] != null)
            params.set(Keys.ELEVATION_MAX, extremes[1]);

        // Compute the internal pixel type from the image format.
        if (params.get(Keys.DATA_TYPE) == null && params.get(Keys.IMAGE_FORMAT) != null) {
            String s = WWIO.makeDataTypeForMimeType(params.get(Keys.IMAGE_FORMAT).toString());
            if (s != null)
                params.set(Keys.DATA_TYPE, s);
        }

        // Use the default data type.
        if (params.get(Keys.DATA_TYPE) == null)
            params.set(Keys.DATA_TYPE, Keys.INT16);

        // Use the default byte order.
        if (params.get(Keys.BYTE_ORDER) == null)
            params.set(Keys.BYTE_ORDER, Keys.LITTLE_ENDIAN);

        return params;
    }

    //**************************************************************//
    //********************  Configuration  *************************//
    //**************************************************************//

    protected static KV wmsRestorableStateToParams(String stateInXml) {

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
        WMSBasicElevationModel.wmsRestoreStateForParams(rs, null, params);
        return params;
    }

    protected static void wmsRestoreStateForParams(RestorableSupport rs, RestorableSupport.StateObject context,
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

        s = rs.getStateValueAsString(context, Keys.LAYER_NAMES);
        if (s != null)
            params.set(Keys.LAYER_NAMES, s);

        s = rs.getStateValueAsString(context, Keys.STYLE_NAMES);
        if (s != null)
            params.set(Keys.STYLE_NAMES, s);

        s = rs.getStateValueAsString(context, "wms.Version");
        params.set(Keys.TILE_URL_BUILDER, new URLBuilder(s, params));
    }

    //**************************************************************//
    //********************  Composition  ***************************//
    //**************************************************************//

    protected static Retriever downloadElevations(ElevationCompositionTile tile) throws Exception {

        Retriever retriever = new HTTPRetriever(tile.getResourceURL(), new CompositionRetrievalPostProcessor(tile.getFile()));
        retriever.setConnectTimeout(10000);
        retriever.setReadTimeout(60000);
        return retriever.call();
    }

    /**
     * Appends WMS basic elevation model configuration elements to the superclass configuration document.
     *
     * @param params configuration parameters describing this WMS basic elevation model.
     * @return a WMS basic elevation model configuration document.
     */
    protected Document createConfigurationDocument(KV params) {
        Document doc = super.createConfigurationDocument(params);
        if (doc == null || doc.getDocumentElement() == null)
            return doc;

        DataConfigurationUtils.createWMSLayerConfigElements(params, doc.getDocumentElement());

        return doc;
    }

    public void composeElevations(Sector sector, List<? extends LatLon> latlons, int tileWidth, double[] buffer)
        throws Exception {

        final int n = latlons.size();
        if (buffer.length < n || tileWidth > n) {
            String msg = Logging.getMessage("ElevationModel.ElevationsBufferTooSmall", n);
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        ElevationCompositionTile tile = new ElevationCompositionTile(sector, this.getLevels().getLastLevel(),
            tileWidth, n / tileWidth);

        ByteBuffer b = WMSBasicElevationModel.downloadElevations(tile).getBuffer();
        if (b!=null)
            tile.setElevations(BufferWrapper.ByteBufferWrapper.wrap(b, Keys.INT8), this);
        else
            return;


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

    public void getRestorableStateForAVPair(String key, Object value,
        RestorableSupport rs, RestorableSupport.StateObject context) {
        if (value instanceof URLBuilder) {
            rs.addStateValueAsString(context, "wms.Version", ((URLBuilder) value).wmsVersion);
            rs.addStateValueAsString(context, "wms.Crs", ((URLBuilder) value).crs);
        } else {
            super.getRestorableStateForAVPair(key, value, rs, context);
        }
    }

    //**************************************************************//
    //********************  Restorable Support  ********************//
    //**************************************************************//

    // TODO: consolidate common code in WMSTiledImageLayer.URLBuilder and WMSBasicElevationModel.URLBuilder
    protected static class URLBuilder implements TileUrlBuilder {
        protected static final String MAX_VERSION = "1.3.0";

        private final String layerNames;
        private final String styleNames;
        private final String imageFormat;
        private final String wmsVersion;
        private final String crs;
        protected String URLTemplate;

        protected URLBuilder(String version, KV params) {

            this.layerNames = params.getStringValue(Keys.LAYER_NAMES);
            this.styleNames = params.getStringValue(Keys.STYLE_NAMES);
            this.imageFormat = params.getStringValue(Keys.IMAGE_FORMAT);

            String coordSystemKey;
            String defaultCS;
            if (version == null || WWUtil.compareVersion(version, "1.3.0") >= 0) // version 1.3.0 or greater
            {
                this.wmsVersion = URLBuilder.MAX_VERSION;
                coordSystemKey = "&crs=";
                defaultCS
                    = "CRS:84"; // would like to do EPSG:4326 but that's incompatible with our old WMS server, see WWJ-474
            } else {
                this.wmsVersion = version;
                coordSystemKey = "&srs=";
                defaultCS = "EPSG:4326";
            }

            String coordinateSystem = params.getStringValue(Keys.COORDINATE_SYSTEM);
            this.crs = coordSystemKey + (coordinateSystem != null ? coordinateSystem : defaultCS);
        }

        public URL getURL(Tile tile, String altImageFormat) throws MalformedURLException {
            StringBuilder sb;
            if (this.URLTemplate == null) {
                sb = new StringBuilder(tile.level.getService());

                if (!sb.toString().toLowerCase().contains("service=wms"))
                    sb.append("service=WMS");
                sb.append("&request=GetMap");
                sb.append("&version=");
                sb.append(this.wmsVersion);
                sb.append(this.crs);
                sb.append("&layers=");
                sb.append(this.layerNames);
                sb.append("&styles=");
                sb.append(this.styleNames != null ? this.styleNames : "");
                sb.append("&format=");
                if (altImageFormat == null)
                    sb.append(this.imageFormat);
                else
                    sb.append(altImageFormat);

                this.URLTemplate = sb.toString();
            } else {
                sb = new StringBuilder(this.URLTemplate);
            }

            sb.append("&width=");
            sb.append(tile.getWidth());
            sb.append("&height=");
            sb.append(tile.getHeight());

            Sector s = tile.sector;
            sb.append("&bbox=");
            // The order of the coordinate specification matters, and it changed with WMS 1.3.0.
            if (WWUtil.compareVersion(this.wmsVersion, "1.1.1") <= 0 || this.crs.contains("CRS:84")) {
                // 1.1.1 and earlier and CRS:84 use lon/lat order
                sb.append(s.lonMin).append(',');
                sb.append(s.latMin).append(',');
                sb.append(s.lonMax).append(',');
                sb.append(s.latMax);
            } else {
                // 1.3.0 uses lat/lon ordering
                sb.append(s.latMin).append(',');
                sb.append(s.lonMin).append(',');
                sb.append(s.latMax).append(',');
                sb.append(s.lonMax);
            }

            return new URL(sb.toString().replace(" ", "%20"));
        }
    }

    protected static class ElevationCompositionTile extends BasicElevationModel.ElevationTile {
        private final int width;
        private final int height;
        private final File file;

        public ElevationCompositionTile(Sector sector, Level level, int width, int height) throws IOException {
            super(sector, level, -1, -1); // row and column aren't used and need to signal that

            this.width = width;
            this.height = height;

            this.file = File.createTempFile(WWIO.DELETE_ON_EXIT_PREFIX, level.getFormatSuffix());
        }

        @Override
        public int getWidth() {
            return this.width;
        }

        @Override
        public int getHeight() {
            return this.height;
        }

        @Override
        public String getPath() {
            return this.file.getPath();
        }

        public File getFile() {
            return this.file;
        }
    }

    protected static class CompositionRetrievalPostProcessor extends AbstractRetrievalPostProcessor {
        // Note: Requested data is never marked as absent because the caller may want to continually re-try retrieval
        protected final File outFile;

        public CompositionRetrievalPostProcessor(File outFile) {
            this.outFile = outFile;
        }
    }
}