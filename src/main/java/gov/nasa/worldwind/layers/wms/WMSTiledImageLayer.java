/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.layers.wms;

import gov.nasa.worldwind.Keys;
import gov.nasa.worldwind.avlist.*;
import gov.nasa.worldwind.exception.WWRuntimeException;
import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.layers.BasicTiledImageLayer;
import gov.nasa.worldwind.layers.ogc.wms.WMSCapabilities;
import gov.nasa.worldwind.util.*;
import org.w3c.dom.*;

import java.net.*;
import java.util.logging.Level;

/**
 * @author tag
 * @version $Id: WMSTiledImageLayer.java 1957 2014-04-23 23:32:39Z tgaskins $
 */
public class WMSTiledImageLayer extends BasicTiledImageLayer {
    private static final String[] formatOrderPreference = {
        "image/dds", "image/png", "image/jpeg"
    };

    public WMSTiledImageLayer(KV params) {
        super(params);
    }

    public WMSTiledImageLayer(Document dom, KV params) {
        this(dom.getDocumentElement(), params);
    }

    public WMSTiledImageLayer(Element domElement, KV params) {
        this(WMSTiledImageLayer.wmsGetParamsFromDocument(domElement, params));
    }

    public WMSTiledImageLayer(WMSCapabilities caps, KV params) {
        this(WMSTiledImageLayer.wmsGetParamsFromCapsDoc(caps, params));
    }

    public WMSTiledImageLayer(String stateInXml) {
        this(WMSTiledImageLayer.wmsRestorableStateToParams(stateInXml));

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

        this.doRestoreState(rs, null);
    }

    /**
     * Extracts parameters necessary to configure the layer from an XML DOM element.
     *
     * @param domElement the element to search for parameters.
     * @param params     an attribute-value list in which to place the extracted parameters. May be null, in which case
     *                   a new attribue-value list is created and returned.
     * @return the attribute-value list passed as the second parameter, or the list created if the second parameter is
     * null.
     * @throws IllegalArgumentException if the DOM element is null.
     */
    protected static KV wmsGetParamsFromDocument(Element domElement, KV params) {

        if (params == null)
            params = new KVMap();

        DataConfigurationUtils.getWMSLayerConfigParams(domElement, params);
        BasicTiledImageLayer.getParamsFromDocument(domElement, params);

        params.set(Keys.TILE_URL_BUILDER, new URLBuilder(params));

        return params;
    }

    /**
     * Extracts parameters necessary to configure the layer from a WMS capabilities document.
     *
     * @param caps   the capabilities document.
     * @param params an attribute-value list in which to place the extracted parameters. May be null, in which case a
     *               new attribute-value list is created and returned.
     * @return the attribute-value list passed as the second parameter, or the list created if the second parameter is
     * null.
     * @throws IllegalArgumentException if the capabilities document reference is null.
     */
    public static KV wmsGetParamsFromCapsDoc(WMSCapabilities caps, KV params) {

        if (params == null)
            params = new KVMap();

        try {
            DataConfigurationUtils.getWMSLayerConfigParams(caps, WMSTiledImageLayer.formatOrderPreference, params);
        }
        catch (IllegalArgumentException e) {
            String message = Logging.getMessage("WMS.MissingLayerParameters");
            Logging.logger().log(Level.SEVERE, message, e);
            throw new IllegalArgumentException(message, e);
        }
        catch (WWRuntimeException e) {
            String message = Logging.getMessage("WMS.MissingCapabilityValues");
            Logging.logger().log(Level.SEVERE, message, e);
            throw new IllegalArgumentException(message, e);
        }

        BasicTiledImageLayer.setFallbacks(params);

        // Setup WMS URL builder.
        params.set(Keys.TILE_URL_BUILDER, new URLBuilder(params));
        // Setup default WMS tiled image layer behaviors.
        params.set(Keys.USE_TRANSPARENT_TEXTURES, true);

        return params;
    }

    /**
     * Creates an attribute-value list from an xml document containing restorable state for this layer.
     *
     * @param stateInXml an xml document specified in a {@link String}.
     * @return an attribute-value list containing the parameters in the specified restorable state.
     * @throws IllegalArgumentException if the state reference is null.
     */
    public static KV wmsRestorableStateToParams(String stateInXml) {

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
        WMSTiledImageLayer.wmsRestoreStateToParams(rs, null, params);
        return params;
    }

    protected static void wmsRestoreStateToParams(RestorableSupport rs, RestorableSupport.StateObject context,
        KV params) {
        // Invoke the BasicTiledImageLayer functionality.
        BasicTiledImageLayer.restoreStateForParams(rs, context, params);
        // Parse any legacy WMSTiledImageLayer state values.
        WMSTiledImageLayer.legacyWmsRestoreStateToParams(rs, context, params);

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
        if (s != null)
            params.set(Keys.WMS_VERSION, s);
        params.set(Keys.TILE_URL_BUILDER, new URLBuilder(params));
    }

    protected static void legacyWmsRestoreStateToParams(RestorableSupport rs, RestorableSupport.StateObject context,
        KV params) {
        // WMSTiledImageLayer has historically used a different format for storing LatLon and Sector properties
        // in the restorable state XML documents. Although WMSTiledImageLayer no longer writes these properties,
        // we must provide support for reading them here.
        Double lat = rs.getStateValueAsDouble(context, Keys.LEVEL_ZERO_TILE_DELTA + ".Latitude");
        Double lon = rs.getStateValueAsDouble(context, Keys.LEVEL_ZERO_TILE_DELTA + ".Longitude");
        if (lat != null && lon != null)
            params.set(Keys.LEVEL_ZERO_TILE_DELTA, LatLon.fromDegrees(lat, lon));

        Double minLat = rs.getStateValueAsDouble(context, Keys.SECTOR + ".MinLatitude");
        Double minLon = rs.getStateValueAsDouble(context, Keys.SECTOR + ".MinLongitude");
        Double maxLat = rs.getStateValueAsDouble(context, Keys.SECTOR + ".MaxLatitude");
        Double maxLon = rs.getStateValueAsDouble(context, Keys.SECTOR + ".MaxLongitude");
        if (minLat != null && minLon != null && maxLat != null && maxLon != null)
            params.set(Keys.SECTOR, Sector.fromDegrees(minLat, maxLat, minLon, maxLon));
    }

    //**************************************************************//
    //********************  Configuration  *************************//
    //**************************************************************//

//    @Override
//    public BufferedImage composeImageForSector(Sector sector, int canvasWidth, int canvasHeight, double aspectRatio,
//        int levelNumber, String mimeType, boolean abortOnError, BufferedImage image, int timeout) throws Exception,
//        IOException, InterruptedIOException {
//
//        Level requestedLevel;
//        if ((levelNumber >= 0) && (levelNumber < levels.getNumLevels()))
//            requestedLevel = levels.getLevel(levelNumber);
//        else
//            requestedLevel = levels.getLastLevel();
//        ComposeImageTile tile =
//            new ComposeImageTile(sector, mimeType, requestedLevel, canvasWidth, canvasHeight);
//        try {
//            if (image == null)
//                image = new BufferedImage(canvasWidth, canvasHeight, BufferedImage.TYPE_INT_RGB);
//
//            downloadImage(tile, mimeType, timeout);
////            Thread.sleep(1); // generates InterruptedException if thread has been interupted
//
//            BufferedImage tileImage = ImageIO.read(tile.getFile());
////            Thread.sleep(1); // generates InterruptedException if thread has been interupted
//
//            ImageUtil.mergeImage(sector, tile.sector, aspectRatio, tileImage, image);
////            Thread.sleep(1); // generates InterruptedException if thread has been interupted
//
//            this.firePropertyChange(AVKey.PROGRESS, 0.0d, 1.0d);
//        }
//        catch (InterruptedIOException e) {
//            throw e;
//        }
//        catch (Exception e) {
//            if (abortOnError)
//                throw e;
//
//            String message = Logging.getMessage("generic.ExceptionWhileRequestingImage", tile.getPath());
//            Logging.logger().log(java.util.logging.Level.WARNING, message, e);
//        }
//
//        return image;
//    }

    //**************************************************************//
    //********************  Restorable Support  ********************//
    //**************************************************************//

    /**
     * Appends WMS tiled image layer configuration elements to the superclass configuration document.
     *
     * @param params configuration parameters describing this WMS tiled image layer.
     * @return a WMS tiled image layer configuration document.
     */
    protected Document createConfigurationDocument(KV params) {
        Document doc = super.createConfigurationDocument(params);
        if (doc == null || doc.getDocumentElement() == null)
            return doc;

        DataConfigurationUtils.createWMSLayerConfigElements(params, doc.getDocumentElement());

        return doc;
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

    // TODO: consolidate common code in WMSTiledImageLayer.URLBuilder and WMSBasicElevationModel.URLBuilder
    public static class URLBuilder implements TileUrlBuilder {
        private static final String MAX_VERSION = "1.3.0";

        private final String layerNames;
        private final String styleNames;
        private final String imageFormat;
        private final String wmsVersion;
        private final String crs;
        private final String backgroundColor;
        public String URLTemplate;

        public URLBuilder(KV params) {
            this.layerNames = params.getStringValue(Keys.LAYER_NAMES);
            this.styleNames = params.getStringValue(Keys.STYLE_NAMES);
            this.imageFormat = params.getStringValue(Keys.IMAGE_FORMAT);
            this.backgroundColor = params.getStringValue(Keys.WMS_BACKGROUND_COLOR);
            String version = params.getStringValue(Keys.WMS_VERSION);

            String coordSystemKey;
            String defaultCS;
            if (version == null || WWUtil.compareVersion(version, "1.3.0") >= 0) {
                this.wmsVersion = URLBuilder.MAX_VERSION;
                coordSystemKey = "&crs=";
                defaultCS = "CRS:84"; // would like to do EPSG:4326 but that's incompatible with our old WMS server, see WWJ-474
            } else {
                this.wmsVersion = version;
                coordSystemKey = "&srs=";
                defaultCS = "EPSG:4326";
            }

            String coordinateSystem = params.getStringValue(Keys.COORDINATE_SYSTEM);
            this.crs = coordSystemKey + (coordinateSystem != null ? coordinateSystem : defaultCS);
        }

        public URL getURL(Tile tile, String altImageFormat) throws MalformedURLException {
            StringBuffer sb;
            if (this.URLTemplate == null) {
                sb = new StringBuffer(WWXML.fixGetMapString(tile.level.getService()));

                if (!sb.toString().toLowerCase().contains("service=wms"))
                    sb.append("service=WMS");
                sb.append("&request=GetMap");
                sb.append("&version=").append(this.wmsVersion);
                sb.append(this.crs);
                sb.append("&layers=").append(this.layerNames);
                sb.append("&styles=").append(this.styleNames != null ? this.styleNames : "");
                sb.append("&transparent=TRUE");
                if (this.backgroundColor != null)
                    sb.append("&bgcolor=").append(this.backgroundColor);

                this.URLTemplate = sb.toString();
            } else {
                sb = new StringBuffer(this.URLTemplate);
            }

            String format = (altImageFormat != null) ? altImageFormat : this.imageFormat;
            if (null != format)
                sb.append("&format=").append(format);

            sb.append("&width=").append(tile.getWidth());
            sb.append("&height=").append(tile.getHeight());

            Sector s = tile.sector;
            sb.append("&bbox=");
            // The order of the coordinate specification matters, and it changed with WMS 1.3.0.
            if (WWUtil.compareVersion(this.wmsVersion, "1.1.1") <= 0 || this.crs.contains("CRS:84")) {
                // 1.1.1 and earlier and CRS:84 use lon/lat order
                sb.append(s.lonMin);
                sb.append(',');
                sb.append(s.latMin);
                sb.append(',');
                sb.append(s.lonMax);
                sb.append(',');
                sb.append(s.latMax);
            } else {
                // 1.3.0 uses lat/lon ordering
                sb.append(s.latMin);
                sb.append(',');
                sb.append(s.lonMin);
                sb.append(',');
                sb.append(s.latMax);
                sb.append(',');
                sb.append(s.lonMax);
            }

            return new URL(sb.toString().replace(" ", "%20"));
        }
    }
}