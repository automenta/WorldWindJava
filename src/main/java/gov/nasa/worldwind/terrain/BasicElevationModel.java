/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.terrain;

import com.jogamp.common.nio.Buffers;
import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.avlist.*;
import gov.nasa.worldwind.cache.*;
import gov.nasa.worldwind.data.*;
import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.layers.ogc.wms.WMSCapabilities;
import gov.nasa.worldwind.retrieve.*;
import gov.nasa.worldwind.util.*;
import org.w3c.dom.*;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.xml.xpath.XPath;
import java.awt.image.*;
import java.io.*;
import java.net.*;
import java.nio.*;
import java.util.*;

// Implementation notes, not for API doc:
//
// Implements an elevation model based on a quad tree of elevation tiles. Meant to be subclassed by very specific
// classes, e.g. Earth/SRTM. A Descriptor passed in at construction gives the configuration parameters. Eventually
// Descriptor will be replaced by an XML configuration document.
//
// A "tile" corresponds to one tile of the data set, which has a corresponding unique row/column address in the data
// set. An inner class implements Tile. An inner class also implements TileKey, which is used to address the
// corresponding Tile in the memory cache.

// Clients of this class get elevations from it by first getting an Elevations object for a specific Sector, then
// querying that object for the elevation at individual lat/lon positions. The Elevations object captures information
// that is used to compute elevations. See in-line comments for a description.
//
// When an elevation tile is needed but is not in memory, a task is threaded off to find it. If it's in the file cache
// then it's loaded by the task into the memory cache. If it's not in the file cache then a retrieval is initiated by
// the task. The disk is never accessed during a call to getElevations(sector, resolution) because that method is
// likely being called when a frame is being rendered. The details of all this are in-line below.

/**
 * @author Tom Gaskins
 * @version $Id: BasicElevationModel.java 3425 2015-09-30 23:17:35Z dcollins $
 */
public class BasicElevationModel extends AbstractElevationModel implements BulkRetrievable {
    // Model resource properties.
    protected static final int RESOURCE_ID_OGC_CAPABILITIES = 1;
    protected final LevelSet levels;
    protected final double minElevation;
    protected final double maxElevation;
    protected final Object fileLock = new Object();
//    protected final ConcurrentHashMap<TileKey, ElevationTile> levelZeroTiles =
//        new ConcurrentHashMap<>();
    protected final MemoryCache extremesLookupCache = new SoftMemoryCache();
    protected String elevationDataType = AVKey.INT16;
    protected String elevationDataByteOrder = AVKey.LITTLE_ENDIAN;
    protected double detailHint;
    protected MemoryCache memoryCache;
    protected int extremesLevel = -1;
    protected boolean extremesCachingEnabled = true;
    protected BufferWrapper extremes;

    public BasicElevationModel(AVList params) {
//        if (params == null) {
//            String message = Logging.getMessage("nullValue.ElevationModelConfigParams");
//            Logging.logger().severe(message);
//            throw new IllegalArgumentException(message);
//        }

        String s = params.getStringValue(AVKey.BYTE_ORDER);
        if (s != null)
            this.setByteOrder(s);

        Double d = (Double) params.get(AVKey.DETAIL_HINT);
        if (d != null)
            this.setDetailHint(d);

        s = params.getStringValue(AVKey.DISPLAY_NAME);
        if (s != null)
            this.setName(s);

        d = (Double) params.get(AVKey.ELEVATION_MIN);
        this.minElevation = d != null ? d : 0;

        d = (Double) params.get(AVKey.ELEVATION_MAX);
        this.maxElevation = d != null ? d : 0;

        Long lo = (Long) params.get(AVKey.EXPIRY_TIME);
        if (lo != null)
            params.set(AVKey.EXPIRY_TIME, lo);

        d = (Double) params.get(AVKey.MISSING_DATA_SIGNAL);
        if (d != null)
            this.setMissingDataSignal(d);

        d = (Double) params.get(AVKey.MISSING_DATA_REPLACEMENT);
        if (d != null)
            this.setMissingDataReplacement(d);

        Boolean b = (Boolean) params.get(AVKey.NETWORK_RETRIEVAL_ENABLED);
        if (b != null)
            this.setNetworkRetrievalEnabled(b);

        s = params.getStringValue(AVKey.DATA_TYPE);
        if (s != null)
            this.setElevationDataType(s);

        s = params.getStringValue(AVKey.ELEVATION_EXTREMES_FILE);
        if (s != null)
            this.loadExtremeElevations(s);

        b = (Boolean) params.get(AVKey.DELETE_CACHE_ON_EXIT);
        if (b != null)
            this.set(AVKey.DELETE_CACHE_ON_EXIT, true);

        // Set some fallback values if not already set.
        BasicElevationModel.setFallbacks(params);

        this.levels = new LevelSet(params);
        if (this.levels.getSector() != null && this.get(AVKey.SECTOR) == null)
            this.set(AVKey.SECTOR, this.levels.getSector());

        this.memoryCache = BasicElevationModel.createMemoryCache(ElevationTile.class.getName());

        this.set(AVKey.CONSTRUCTION_PARAMETERS, params.copy());

        // If any resources should be retrieved for this ElevationModel, start a task to retrieve those resources, and
        // initialize this ElevationModel once those resources are retrieved.
        if (this.isRetrieveResources()) {
            this.startResourceRetrieval();
        }
    }

    public BasicElevationModel(Document dom, AVList params) {
        this(dom.getDocumentElement(), params);
    }

    public BasicElevationModel(Element domElement, AVList params) {
        this(BasicElevationModel.getBasicElevationModelConfigParams(domElement, params));
    }

    public BasicElevationModel(String restorableStateInXml) {
        this(BasicElevationModel.restorableStateToParams(restorableStateInXml));

        RestorableSupport rs;
        try {
            rs = RestorableSupport.parse(restorableStateInXml);
        }
        catch (RuntimeException e) {
            // Parsing the document specified by stateInXml failed.
            String message = Logging.getMessage("generic.ExceptionAttemptingToParseStateXml", restorableStateInXml);
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message, e);
        }

        this.doRestoreState(rs, null);
    }

    protected static void setFallbacks(AVList params) {
        if (params.get(AVKey.TILE_WIDTH) == null)
            params.set(AVKey.TILE_WIDTH, 150);

        if (params.get(AVKey.TILE_HEIGHT) == null)
            params.set(AVKey.TILE_HEIGHT, 150);

        if (params.get(AVKey.FORMAT_SUFFIX) == null)
            params.set(AVKey.FORMAT_SUFFIX, ".bil");

        if (params.get(AVKey.NUM_LEVELS) == null)
            params.set(AVKey.NUM_LEVELS, 2);

        if (params.get(AVKey.NUM_EMPTY_LEVELS) == null)
            params.set(AVKey.NUM_EMPTY_LEVELS, 0);
    }

    protected static ByteBuffer convertImageToElevations(ByteBuffer buffer, String contentType) throws IOException {
        File tempFile = File.createTempFile("wwj-", WWIO.mimeSuffix(contentType));
        try {
            WWIO.saveBuffer(buffer, tempFile);
            BufferedImage image = ImageIO.read(tempFile);
            ByteBuffer byteBuffer = Buffers.newDirectByteBuffer(image.getWidth() * image.getHeight() * 2);
            byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
            ShortBuffer bilBuffer = byteBuffer.asShortBuffer();

            WritableRaster raster = image.getRaster();
            int[] samples = new int[raster.getWidth() * raster.getHeight()];
            raster.getSamples(0, 0, raster.getWidth(), raster.getHeight(), 0, samples);
            for (int sample : samples) {
                bilBuffer.put((short) sample);
            }

            return byteBuffer;
        }
        finally {
            tempFile.delete();
        }
    }

    /**
     * Creates a configuration document for a BasicElevationModel described by the specified params. The returned
     * document may be used as a construction parameter to {@link BasicElevationModel}.
     *
     * @param params parameters describing a BasicElevationModel.
     * @return a configuration document for the BasicElevationModel.
     */
    public static Document createBasicElevationModelConfigDocument(AVList params) {
        Document doc = WWXML.createDocumentBuilder(true).newDocument();

        Element root = WWXML.setDocumentElement(doc, "ElevationModel");
        // Note: no type attribute denotes the default elevation model, which currently is BasicElevationModel.
        WWXML.setIntegerAttribute(root, "version", 1);

        BasicElevationModel.createBasicElevationModelConfigElements(params, root);

        return doc;
    }

    /**
     * Appends BasicElevationModel configuration parameters as elements to the specified context. This appends elements
     * for the following parameters: <table> <caption style="font-weight: bold;">Parameters</caption>
     * <tr><th>Parameter</th><th>Element Path</th><th>Type</th></tr>
     * <tr><td>{@link AVKey#SERVICE_NAME}</td><td>Service/@serviceName</td><td>String</td></tr> <tr><td>{@link
     * AVKey#IMAGE_FORMAT}</td><td>ImageFormat</td><td>String</td></tr> <tr><td>{@link
     * AVKey#AVAILABLE_IMAGE_FORMATS}</td><td>AvailableImageFormats/ImageFormat</td><td>String array</td></tr>
     * <tr><td>{@link AVKey#DATA_TYPE}</td><td>DataType/@type</td><td>String</td></tr> <tr><td>{@link
     * AVKey#BYTE_ORDER}</td><td>ByteOrder</td><td>DataType/@byteOrder</td></tr> <tr><td>{@link
     * AVKey#ELEVATION_EXTREMES_FILE}</td><td>ExtremeElevations/FileName</td><td>String</td></tr> <tr><td>{@link
     * AVKey#ELEVATION_MAX}</td><td>ExtremeElevations/@max</td><td>Double</td></tr> <tr><td>{@link
     * AVKey#ELEVATION_MIN}</td><td>ExtremeElevations/@min</td><td>Double</td></tr> </table> This also writes common
     * elevation model and LevelSet configuration parameters by invoking {@link AbstractElevationModel#createElevationModelConfigElements(AVList,
     * Element)} and {@link DataConfigurationUtils#createLevelSetConfigElements(AVList, Element)}.
     *
     * @param params  the key-value pairs which define the BasicElevationModel configuration parameters.
     * @param context the XML document root on which to append BasicElevationModel configuration elements.
     * @return a reference to context.
     * @throws IllegalArgumentException if either the parameters or the context are null.
     */
    public static Element createBasicElevationModelConfigElements(AVList params, Element context) {
        if (params == null) {
            String message = Logging.getMessage("nullValue.ParametersIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (context == null) {
            String message = Logging.getMessage("nullValue.ContextIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        XPath xpath = WWXML.makeXPath();

        // Common elevation model properties.
        AbstractElevationModel.createElevationModelConfigElements(params, context);

        // LevelSet properties.
        DataConfigurationUtils.createLevelSetConfigElements(params, context);

        // Service properties.
        // Try to get the SERVICE_NAME property, but default to "WWTileService".
        String s = AVListImpl.getStringValue(params, AVKey.SERVICE_NAME, "WWTileService");
        if (s != null && !s.isEmpty()) {
            // The service element may already exist, in which case we want to append to it.
            Element el = WWXML.getElement(context, "Service", xpath);
            if (el == null)
                el = WWXML.appendElementPath(context, "Service");
            WWXML.setTextAttribute(el, "serviceName", s);
        }

        WWXML.checkAndAppendBooleanElement(params, AVKey.RETRIEVE_PROPERTIES_FROM_SERVICE, context,
            "RetrievePropertiesFromService");

        // Image format properties.
        WWXML.checkAndAppendTextElement(params, AVKey.IMAGE_FORMAT, context, "ImageFormat");

        Object o = params.get(AVKey.AVAILABLE_IMAGE_FORMATS);
        if (o instanceof String[]) {
            String[] strings = (String[]) o;
            if (strings.length > 0) {
                // The available image formats element may already exists, in which case we want to append to it, rather
                // than create entirely separate paths.
                Element el = WWXML.getElement(context, "AvailableImageFormats", xpath);
                if (el == null)
                    el = WWXML.appendElementPath(context, "AvailableImageFormats");
                WWXML.appendTextArray(el, "ImageFormat", strings);
            }
        }

        // Data type properties.
        if (params.get(AVKey.DATA_TYPE) != null || params.get(AVKey.BYTE_ORDER) != null) {
            Element el = WWXML.getElement(context, "DataType", null);
            if (el == null)
                el = WWXML.appendElementPath(context, "DataType");

            s = params.getStringValue(AVKey.DATA_TYPE);
            if (s != null && !s.isEmpty()) {
                s = WWXML.dataTypeAsText(s);
                if (s != null && !s.isEmpty())
                    WWXML.setTextAttribute(el, "type", s);
            }

            s = params.getStringValue(AVKey.BYTE_ORDER);
            if (s != null && !s.isEmpty()) {
                s = WWXML.byteOrderAsText(s);
                if (s != null && !s.isEmpty())
                    WWXML.setTextAttribute(el, "byteOrder", s);
            }
        }

        // Elevation data properties.
        Element el = WWXML.appendElementPath(context, "ExtremeElevations");
        WWXML.checkAndAppendTextElement(params, AVKey.ELEVATION_EXTREMES_FILE, el, "FileName");

        Double d = AVListImpl.getDoubleValue(params, AVKey.ELEVATION_MAX);
        if (d != null)
            WWXML.setDoubleAttribute(el, "max", d);

        d = AVListImpl.getDoubleValue(params, AVKey.ELEVATION_MIN);
        if (d != null)
            WWXML.setDoubleAttribute(el, "min", d);

        return context;
    }

    /**
     * Parses BasicElevationModel parameters from a specified DOM document. This writes output as key-value pairs to
     * params. If a parameter from the XML document already exists in params, that parameter is ignored. Supported key
     * and parameter names are: <table> <caption style="font-weight: bold;">Parameters</caption>
     * <tr><th>Parameter</th><th>Element Path</th><th>Type</th></tr>
     * <tr><td>{@link
     * AVKey#SERVICE_NAME}</td><td>Service/@serviceName</td><td>String</td></tr> <tr><td>{@link
     * AVKey#IMAGE_FORMAT}</td><td>ImageFormat</td><td>String</td></tr> <tr><td>{@link
     * AVKey#AVAILABLE_IMAGE_FORMATS}</td><td>AvailableImageFormats/ImageFormat</td><td>String array</td></tr>
     * <tr><td>{@link AVKey#DATA_TYPE}</td><td>DataType/@type</td><td>String</td></tr> <tr><td>{@link
     * AVKey#BYTE_ORDER}</td><td>DataType/@byteOrder</td><td>String</td></tr> <tr><td>{@link
     * AVKey#ELEVATION_EXTREMES_FILE}</td><td>ExtremeElevations/FileName</td><td>String</td></tr> <tr><td>{@link
     * AVKey#ELEVATION_MAX}</td><td>ExtremeElevations/@max</td><td>Double</td></tr> <tr><td>{@link
     * AVKey#ELEVATION_MIN}</td><td>ExtremeElevations/@min</td><td>Double</td></tr> </table> This also parses common
     * elevation model and LevelSet configuration parameters by invoking {@link AbstractElevationModel#getElevationModelConfigParams(Element,
     * AVList)} and {@link DataConfigurationUtils#getLevelSetConfigParams(Element, AVList)}.
     *
     * @param domElement the XML document root to parse for BasicElevationModel configuration parameters.
     * @param params     the output key-value pairs which recieve the BasicElevationModel configuration parameters. A
     *                   null reference is permitted.
     * @return a reference to params, or a new AVList if params is null.
     * @throws IllegalArgumentException if the document is null.
     */
    public static AVList getBasicElevationModelConfigParams(Element domElement, AVList params) {
        if (domElement == null) {
            String message = Logging.getMessage("nullValue.DocumentIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (params == null)
            params = new AVListImpl();

        XPath xpath = WWXML.makeXPath();

        // Common elevation model properties.
        AbstractElevationModel.getElevationModelConfigParams(domElement, params);

        // LevelSet properties.
        DataConfigurationUtils.getLevelSetConfigParams(domElement, params);

        // Service properties.
        WWXML.checkAndSetStringParam(domElement, params, AVKey.SERVICE_NAME, "Service/@serviceName", xpath);
        WWXML.checkAndSetBooleanParam(domElement, params, AVKey.RETRIEVE_PROPERTIES_FROM_SERVICE,
            "RetrievePropertiesFromService", xpath);

        // Image format properties.
        WWXML.checkAndSetStringParam(domElement, params, AVKey.IMAGE_FORMAT, "ImageFormat", xpath);
        WWXML.checkAndSetUniqueStringsParam(domElement, params, AVKey.AVAILABLE_IMAGE_FORMATS,
            "AvailableImageFormats/ImageFormat", xpath);

        // Data type properties.
        if (params.get(AVKey.DATA_TYPE) == null) {
            String s = WWXML.getText(domElement, "DataType/@type", xpath);
            if (s != null && !s.isEmpty()) {
                s = WWXML.parseDataType(s);
                if (s != null && !s.isEmpty())
                    params.set(AVKey.DATA_TYPE, s);
            }
        }

        if (params.get(AVKey.BYTE_ORDER) == null) {
            String s = WWXML.getText(domElement, "DataType/@byteOrder", xpath);
            if (s != null && !s.isEmpty()) {
                s = WWXML.parseByteOrder(s);
                if (s != null && !s.isEmpty())
                    params.set(AVKey.BYTE_ORDER, s);
            }
        }

        // Elevation data properties.
        WWXML.checkAndSetStringParam(domElement, params, AVKey.ELEVATION_EXTREMES_FILE, "ExtremeElevations/FileName",
            xpath);
        WWXML.checkAndSetDoubleParam(domElement, params, AVKey.ELEVATION_MAX, "ExtremeElevations/@max", xpath);
        WWXML.checkAndSetDoubleParam(domElement, params, AVKey.ELEVATION_MIN, "ExtremeElevations/@min", xpath);

        return params;
    }

    protected static AVList restorableStateToParams(String stateInXml) {
        if (stateInXml == null) {
            String message = Logging.getMessage("nullValue.StringIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

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

        AVList params = new AVListImpl();
        BasicElevationModel.restoreStateForParams(rs, null, params);
        return params;
    }

    protected static void restoreStateForParams(RestorableSupport rs, RestorableSupport.StateObject context,
        AVList params) {
        StringBuilder sb = new StringBuilder();

        String s = rs.getStateValueAsString(context, AVKey.DATA_CACHE_NAME);
        if (s != null)
            params.set(AVKey.DATA_CACHE_NAME, s);

        s = rs.getStateValueAsString(context, AVKey.SERVICE);
        if (s != null)
            params.set(AVKey.SERVICE, s);

        s = rs.getStateValueAsString(context, AVKey.DATASET_NAME);
        if (s != null)
            params.set(AVKey.DATASET_NAME, s);

        s = rs.getStateValueAsString(context, AVKey.FORMAT_SUFFIX);
        if (s != null)
            params.set(AVKey.FORMAT_SUFFIX, s);

        Integer i = rs.getStateValueAsInteger(context, AVKey.NUM_EMPTY_LEVELS);
        if (i != null)
            params.set(AVKey.NUM_EMPTY_LEVELS, i);

        i = rs.getStateValueAsInteger(context, AVKey.NUM_LEVELS);
        if (i != null)
            params.set(AVKey.NUM_LEVELS, i);

        i = rs.getStateValueAsInteger(context, AVKey.TILE_WIDTH);
        if (i != null)
            params.set(AVKey.TILE_WIDTH, i);

        i = rs.getStateValueAsInteger(context, AVKey.TILE_HEIGHT);
        if (i != null)
            params.set(AVKey.TILE_HEIGHT, i);

        Long lo = rs.getStateValueAsLong(context, AVKey.EXPIRY_TIME);
        if (lo != null)
            params.set(AVKey.EXPIRY_TIME, lo);

        LatLon ll = rs.getStateValueAsLatLon(context, AVKey.LEVEL_ZERO_TILE_DELTA);
        if (ll != null)
            params.set(AVKey.LEVEL_ZERO_TILE_DELTA, ll);

        ll = rs.getStateValueAsLatLon(context, AVKey.TILE_ORIGIN);
        if (ll != null)
            params.set(AVKey.TILE_ORIGIN, ll);

        Sector sector = rs.getStateValueAsSector(context, AVKey.SECTOR);
        if (sector != null)
            params.set(AVKey.SECTOR, sector);

        Double d = rs.getStateValueAsDouble("ElevationModel.MinElevation");
        if (d != null) {
            params.set(AVKey.ELEVATION_MIN, d);
        } else {
            if (!sb.isEmpty())
                sb.append(", ");
            sb.append("term.minElevation");
        }

        d = rs.getStateValueAsDouble("ElevationModel.MaxElevation");
        if (d != null) {
            params.set(AVKey.ELEVATION_MAX, d);
        } else {
            if (!sb.isEmpty())
                sb.append(", ");
            sb.append("term.maxElevation");
        }

        if (!sb.isEmpty()) {
            String message = Logging.getMessage("BasicElevationModel.InvalidDescriptorFields", sb.toString());
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
    }

    protected static MemoryCache createMemoryCache(String cacheName) {
        if (WorldWind.getMemoryCacheSet().containsCache(cacheName)) {
            return WorldWind.cache(cacheName);
        } else {
            MemoryCache mc = new SoftMemoryCache();
            mc.setName("Elevation Tiles");
            WorldWind.getMemoryCacheSet().addCache(cacheName, mc);
            return mc;
        }
    }

    protected static boolean isFileExpired(Tile tile, URL fileURL, FileStore fileStore) {
        if (!WWIO.isFileOutOfDate(fileURL, tile.level.getExpiryTime()))
            return false;

        // The file has expired. Delete it.
        fileStore.removeFile(fileURL);
        String message = Logging.getMessage("generic.DataFileExpired", fileURL);
        Logging.logger().fine(message);
        return true;
    }

    @SuppressWarnings("UnusedDeclaration")
    public static ByteBuffer generateExtremeElevations(int levelNumber) {
        return null;
    }

    @Override
    public Object set(String key, Object value) {
        // Offer it to the level set
        if (this.getLevels() != null)
            this.getLevels().set(key, value);

        return super.set(key, value);
    }

    @Override
    public Object get(String key) {
        Object value = super.get(key);

        return value != null ? value : this.getLevels().get(key); // see if the level set has it
    }

    public LevelSet getLevels() {
        return this.levels;
    }

//    protected Map<TileKey, ElevationTile> getLevelZeroTiles() {
//        return levelZeroTiles;
//    }

    protected int getExtremesLevel() {
        return extremesLevel;
    }

    protected BufferWrapper getExtremes() {
        return extremes;
    }

    /**
     * Specifies the time of the elevation models's most recent dataset update, beyond which cached data is invalid. If
     * greater than zero, the model ignores and eliminates any in-memory or on-disk cached data older than the time
     * specified, and requests new information from the data source. If zero, the default, the model applies any expiry
     * times associated with its individual levels, but only for on-disk cached data. In-memory cached data is expired
     * only when the expiry time is specified with this method and is greater than zero. This method also overwrites the
     * expiry times of the model's individual levels if the value specified to the method is greater than zero.
     *
     * @param expiryTime the expiry time of any cached data, expressed as a number of milliseconds beyond the epoch. The
     *                   default expiry time is zero.
     * @see System#currentTimeMillis() for a description of milliseconds beyond the epoch.
     */
    public void setExpiryTime(long expiryTime) // Override this method to use intrinsic level-specific expiry times
    {
        super.setExpiryTime(expiryTime);

        if (expiryTime > 0)
            this.levels.setExpiryTime(expiryTime); // remove this in sub-class to use level-specific expiry times
    }

    public double getMaxElevation() {
        return this.maxElevation;
    }

    public double getMinElevation() {
        return this.minElevation;
    }

    public double getBestResolution(Sector sector) {
        if (sector == null)
            return this.levels.getLastLevel().getTexelSize();

        Level level = this.levels.getLastLevel(sector);
        return level != null ? level.getTexelSize() : Double.MAX_VALUE;
    }

    public double getDetailHint(Sector sector) {
        return this.detailHint;
    }

    public void setDetailHint(double hint) {
        this.detailHint = hint;
    }
//**************************************************************//
    //********************  Elevation Tile Management  *************//
    //**************************************************************//

    // Create the tile corresponding to a specified key.

    public String getElevationDataType() {
        return this.elevationDataType;
    }

    // Thread off a task to determine whether the file is local or remote and then retrieve it either from the file
    // cache or a remote server.

    public void setElevationDataType(String dataType) {
        if (dataType == null) {
            String message = Logging.getMessage("nullValue.DataTypeIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.elevationDataType = dataType;
    }

    public String getElevationDataByteOrder() {
        return this.elevationDataByteOrder;
    }

    public void setByteOrder(String byteOrder) {
        if (byteOrder == null) {
            String message = Logging.getMessage("nullValue.ByteOrderIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.elevationDataByteOrder = byteOrder;
    }

    // Reads a tile's elevations from the file cache and adds the tile to the memory cache.

    public int intersects(Sector sector) {
        if (this.levels.getSector().contains(sector))
            return 0;

        return this.levels.getSector().intersects(sector) ? 1 : -1;
    }

    public boolean contains(Angle latitude, Angle longitude) {
        if (latitude == null || longitude == null) {
            String msg = Logging.getMessage("nullValue.AngleIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        return this.levels.getSector().contains(latitude, longitude);
    }

    @Override
    public boolean isExtremesCachingEnabled() {
        return this.extremesCachingEnabled;
    }

    @Override
    public void setExtremesCachingEnabled(boolean enabled) {
        this.extremesCachingEnabled = enabled;
    }

    // Read elevations from the file cache. Don't be confused by the use of a URL here: it's used so that files can
    // be read using System.getResource(URL), which will draw the data from a jar file in the classpath.

    protected ElevationTile createTile(TileKey key) {
        Level level = this.levels.getLevel(key.level);

        // Compute the tile's SW lat/lon based on its row/col in the level's data set.
        Angle dLat = level.getTileDelta().getLatitude();
        Angle dLon = level.getTileDelta().getLongitude();
        Angle latOrigin = this.levels.getTileOrigin().getLatitude();
        Angle lonOrigin = this.levels.getTileOrigin().getLongitude();

        Angle minLatitude = ElevationTile.computeRowLatitude(key.row, dLat, latOrigin);
        Angle minLongitude = ElevationTile.computeColumnLongitude(key.col, dLon, lonOrigin);

        Sector tileSector = new Sector(minLatitude, minLatitude.add(dLat), minLongitude, minLongitude.add(dLon));

        return new ElevationTile(tileSector, level, key.row, key.col);
    }

    protected void requestTile(TileKey key) {
//        if (WorldWind.tasks().isFull())
//            return;
        if (!this.getLevels().missing(key))
            WorldWind.tasks().addTask(new RequestTask(key, this));
    }

    protected boolean loadElevations(ElevationTile tile, URL url) throws Exception {
        BufferWrapper elevations = this.readElevations(url);
        if (elevations == null || elevations.length() == 0)
            return false;

        tile.setElevations(elevations, this);
        memoryCache.add(tile.tileKey, tile, elevations.getSizeInBytes());
        return true;
    }

    // *** Bulk download ***
    // *** Bulk download ***
    // *** Bulk download ***

    protected boolean areElevationsInMemory(TileKey key) {
        // An elevation tile is considered to be in memory if it:
        // * Exists in the memory cache.
        // * Has non-null elevation data.
        // * Has not exipired.
        ElevationTile tile = this.tileFromMemory(key);
        return (tile != null && tile.getElevations() != null && !tile.isElevationsExpired());
    }

    protected ElevationTile tileFromMemory(TileKey tileKey) {
//        if (tileKey.getLevelNumber() == 0)
//            return this.levelZeroTiles.get(tileKey);
//        else
        return (ElevationTile) memoryCache.getObject(tileKey);
    }

    protected BufferWrapper readElevations(URL url) throws IOException, URISyntaxException {
        //return url.getPath().endsWith("tif") ? this.makeTiffElevations(url) : this.makeBilElevations(url);
        return url.getPath().endsWith("tif") ?
            this.makeTiffElevations(url) :
            this.makeBilElevations(url);
    }

    protected BufferWrapper makeBilElevations(URL url) throws IOException {
        ByteBuffer byteBuffer;
        synchronized (this.fileLock) {
            byteBuffer = WWIO.readURLContentToBuffer(url);
        }

        // Setup parameters to instruct BufferWrapper on how to interpret the ByteBuffer.
        AVList bufferParams = new AVListImpl();
        bufferParams.set(AVKey.DATA_TYPE, this.elevationDataType);
        bufferParams.set(AVKey.BYTE_ORDER, this.elevationDataByteOrder);
        return BufferWrapper.wrap(byteBuffer, bufferParams);
    }

    // *** Tile download ***
    // *** Tile download ***
    // *** Tile download ***

    protected BufferWrapper makeTiffElevations(URL url) throws IOException, URISyntaxException {
        File file = new File(url.toURI());

        // Create a raster reader for the file type.
        DataRasterReaderFactory readerFactory = (DataRasterReaderFactory) WorldWind.createConfigurationComponent(
            AVKey.DATA_RASTER_READER_FACTORY_CLASS_NAME);
        DataRasterReader reader = readerFactory.findReaderFor(file, null);
//        if (reader == null) {
//            String msg = Logging.getMessage("generic.UnknownFileFormatOrMatchingReaderNotFound", file.getPath());
//            Logging.logger().severe(msg);
//            throw new WWRuntimeException(msg);
//        }

        // Read the file into the raster.
        DataRaster[] rasters;
        synchronized (this.fileLock) {
            rasters = reader.read(file, null);
        }

//        if (rasters == null || rasters.length == 0) {
//            String msg = Logging.getMessage("ElevationModel.CannotReadElevations", file.getAbsolutePath());
//            Logging.logger().severe(msg);
//            throw new WWRuntimeException(msg);
//        }

        DataRaster raster = rasters[0];

        // Request a sub-raster that contains the whole file. This step is necessary because only sub-rasters
        // are reprojected (if necessary); primary rasters are not.
        int width = raster.getWidth();
        int height = raster.getHeight();

        // Determine the sector covered by the elevations. This information is in the GeoTIFF file or auxiliary
        // files associated with the elevations file.
        final Sector sector = (Sector) raster.get(AVKey.SECTOR);
//        if (sector == null) {
//            String msg = Logging.getMessage("DataRaster.MissingMetadata", AVKey.SECTOR);
//            Logging.logger().severe(msg);
//            throw new IllegalStateException(msg);
//        }

        DataRaster subRaster = raster.getSubRaster(width, height, sector, raster);

        // Verify that the sub-raster can create a ByteBuffer, then create one.
//        if (!(subRaster instanceof ByteBufferRaster)) {
//            String msg = Logging.getMessage("ElevationModel.CannotCreateElevationBuffer", file.getPath());
//            Logging.logger().severe(msg);
//            throw new WWRuntimeException(msg);
//        }
        ByteBuffer elevations = ((ByteBufferRaster) subRaster).getByteBuffer();

        // The sub-raster can now be disposed. Disposal won't affect the ByteBuffer.
        subRaster.dispose();

        // Setup parameters to instruct BufferWrapper on how to interpret the ByteBuffer.
        AVList bufferParams = new AVListImpl();
        bufferParams.setValues(raster.copy()); // copies params from avlist

        String dataType = bufferParams.getStringValue(AVKey.DATA_TYPE);
        if (WWUtil.isEmpty(dataType)) {
            String msg = Logging.getMessage("DataRaster.MissingMetadata", AVKey.DATA_TYPE);
            Logging.logger().severe(msg);
            throw new IllegalStateException(msg);
        }

        BufferWrapper bufferWrapper = BufferWrapper.wrap(elevations, bufferParams);

        // Tne primary raster can now be disposed.
        raster.dispose();

        return bufferWrapper;
    }

//    /**
//     * Start a new {@link BulkRetrievalThread} that downloads all elevations for a given sector and resolution to the
//     * current WorldWind file cache, without downloading imagery already in the cache.
//     * <p>
//     * This method creates and starts a thread to perform the download. A reference to the thread is returned. To create
//     * a downloader that has not been started, construct a {@link BasicElevationModelBulkDownloader}.
//     * <p>
//     * Note that the target resolution must be provided in radians of latitude per texel, which is the resolution in
//     * meters divided by the globe radius.
//     *
//     * @param sector     the sector to download data for.
//     * @param resolution the target resolution, provided in radians of latitude per texel.
//     * @param listener   an optional retrieval listener. May be null.
//     * @return the {@link BulkRetrievalThread} executing the retrieval or <code>null</code> if the specified sector does
//     * not intersect the elevation model bounding sector.
//     * @throws IllegalArgumentException if the sector is null or the resolution is less than  zero.
//     * @see BasicElevationModelBulkDownloader
//     */
//    public BulkRetrievalThread makeLocal(Sector sector, double resolution, BulkRetrievalListener listener) {
//        return this.makeLocal(sector, resolution, null, listener);
//    }

//    /**
//     * Start a new {@link BulkRetrievalThread} that downloads all elevations for a given sector and resolution to a
//     * specified file store, without downloading imagery already in the file store.
//     * <p>
//     * This method creates and starts a thread to perform the download. A reference to the thread is returned. To create
//     * a downloader that has not been started, construct a {@link BasicElevationModelBulkDownloader}.
//     * <p>
//     * Note that the target resolution must be provided in radians of latitude per texel, which is the resolution in
//     * meters divided by the globe radius.
//     *
//     * @param sector     the sector to download data for.
//     * @param resolution the target resolution, provided in radians of latitude per texel.
//     * @param fileStore  the file store in which to place the downloaded elevations. If null the current WorldWind file
//     *                   cache is used.
//     * @param listener   an optional retrieval listener. May be null.
//     * @return the {@link BulkRetrievalThread} executing the retrieval or <code>null</code> if the specified sector does
//     * not intersect the elevation model bounding sector.
//     * @throws IllegalArgumentException if  the sector is null or the resolution is less than zero.
//     * @see BasicElevationModelBulkDownloader
//     */
//    public BulkRetrievalThread makeLocal(Sector sector, double resolution, FileStore fileStore,
//        BulkRetrievalListener listener) {
//        Sector targetSector = sector != null ? getLevels().getSector().intersection(sector) : null;
//        if (targetSector == null)
//            return null;
//
//        // Args checked in downloader constructor
//        BasicElevationModelBulkDownloader thread =
//            new BasicElevationModelBulkDownloader(this, targetSector, resolution,
//                fileStore != null ? fileStore : this.getDataFileStore(), listener);
//        thread.setDaemon(true);
//        thread.start();
//        return thread;
//    }

    /**
     * Get the estimated size in bytes of the elevations not in the WorldWind file cache for the given sector and
     * resolution.
     * <p>
     * Note that the target resolution must be provided in radians of latitude per texel, which is the resolution in
     * meters divided by the globe radius.
     *
     * @param sector     the sector to estimate.
     * @param resolution the target resolution, provided in radians of latitude per texel.
     * @return the estimated size in bytes of the missing elevations.
     * @throws IllegalArgumentException if the sector is null or the resolution is less than zero.
     */
    public long getEstimatedMissingDataSize(Sector sector, double resolution) {
        return this.getEstimatedMissingDataSize(sector, resolution, null);
    }

    /**
     * Get the estimated size in bytes of the elevations not in a specified file store for the given sector and
     * resolution.
     * <p>
     * Note that the target resolution must be provided in radians of latitude per texel, which is the resolution in
     * meters divided by the globe radius.
     *
     * @param sector     the sector to estimate.
     * @param resolution the target resolution, provided in radians of latitude per texel.
     * @param fileStore  the file store to examine. If null the current WorldWind file cache is used.
     * @return the estimated size in bytes of the missing elevations.
     * @throws IllegalArgumentException if the sector is null or the resolution is less than zero.
     */
    public long getEstimatedMissingDataSize(Sector sector, double resolution, FileStore fileStore) {
        Sector targetSector = sector != null ? getLevels().getSector().intersection(sector) : null;
        if (targetSector == null)
            return 0;

        // Args checked by downloader constructor
        // Need a downloader to compute the missing data size.
        BasicElevationModelBulkDownloader downloader = new BasicElevationModelBulkDownloader(this, targetSector,
            resolution, fileStore != null ? fileStore : this.getDataFileStore(), null);

        return downloader.getEstimatedMissingDataSize();
    }

    protected void downloadElevations(final Tile tile) {
        retrieveElevations(tile, new DownloadPostProcessor(tile, this));
    }

    protected void retrieveElevations(final Tile tile, DownloadPostProcessor postProcessor) {
        if (this.get(AVKey.RETRIEVER_FACTORY_LOCAL) != null)
            this.retrieveLocalElevations(tile, postProcessor);
        else
            // Assume it's remote, which handles the legacy cases.
            this.retrieveRemoteElevations(tile, postProcessor);
    }

    protected void retrieveLocalElevations(Tile tile, RetrievalPostProcessor postProcessor) {
        if (!WorldWind.retrieveLocal().isAvailable())
            return;

        RetrieverFactory retrieverFactory = (RetrieverFactory) this.get(AVKey.RETRIEVER_FACTORY_LOCAL);
        if (retrieverFactory == null)
            return;

        AVList avList = new AVListImpl();
        avList.set(AVKey.SECTOR, tile.sector);
        avList.set(AVKey.WIDTH, tile.getWidth());
        avList.set(AVKey.HEIGHT, tile.getHeight());
        avList.set(AVKey.FILE_NAME, tile.getPath());

        Retriever retriever = retrieverFactory.retriever(avList, postProcessor);

        WorldWind.retrieveLocal().run(retriever, tile.getPriority());
    }

    protected void retrieveRemoteElevations(final Tile tile, DownloadPostProcessor postProcessor) {
        if (!this.isNetworkRetrievalEnabled()) {
            this.getLevels().miss(tile);
            return;
        }

        if (!WorldWind.retrieveRemote().isAvailable())
            return;

        URL url = null;
        try {
            url = tile.getResourceURL();
            if (WorldWind.getNetworkStatus().isHostUnavailable(url)) {
                this.getLevels().miss(tile);
                return;
            }
        }
        catch (MalformedURLException e) {
            this.getLevels().miss(tile);
            Logging.logger().log(java.util.logging.Level.SEVERE,
                Logging.getMessage("TiledElevationModel.ExceptionCreatingElevationsUrl", url), e);
            return;
        }

        if (postProcessor == null)
            postProcessor = new DownloadPostProcessor(tile, this);
        Retriever retriever = new HTTPRetriever(url, postProcessor);
        retriever.set(URLRetriever.EXTRACT_ZIP_ENTRY, "true"); // supports legacy elevation models

        WorldWind.retrieveRemote().run(retriever, 0);
    }

    protected void determineExtremes(double value, double[] extremes) {
        if (value == this.getMissingDataSignal())
            value = this.getMissingDataReplacement();

        if (value < extremes[0])
            extremes[0] = value;

        if (value > extremes[1])
            extremes[1] = value;
    }

    public double getUnmappedElevation(Angle latitude, Angle longitude) {

        if (!this.contains(latitude, longitude))
            return this.getMissingDataSignal();

        Level lastLevel = this.levels.getLastLevel(latitude, longitude);
        final TileKey tileKey = new TileKey(latitude, longitude, this.levels, lastLevel.getLevelNumber());
        ElevationTile tile = this.tileFromMemory(tileKey);

        if (tile == null) {
            int fallbackRow = tileKey.row;
            int fallbackCol = tileKey.col;
            for (int fallbackLevelNum = tileKey.level - 1; fallbackLevelNum >= 0; fallbackLevelNum--) {
                fallbackRow /= 2;
                fallbackCol /= 2;

                if (this.levels.getLevel(fallbackLevelNum).isEmpty()) // everything lower res is empty
                    return this.getExtremeElevations(latitude, longitude)[0];

                TileKey fallbackKey = new TileKey(fallbackLevelNum, fallbackRow, fallbackCol,
                    this.levels.getLevel(fallbackLevelNum).getCacheName());
                tile = this.tileFromMemory(fallbackKey);
                if (tile != null)
                    break;
            }
        }

        if (tile == null && !this.levels.getFirstLevel().isEmpty()) {
            // Request the level-zero tile since it's not in memory
            Level firstLevel = this.levels.getFirstLevel();
            final TileKey zeroKey = new TileKey(latitude, longitude, this.levels, firstLevel.getLevelNumber());
            this.requestTile(zeroKey);

            // Return the best we know about the location's elevation
            return this.getExtremeElevations(latitude, longitude)[0];
        }

        // Check tile expiration. Memory-cached tiles are checked for expiration only when an explicit, non-zero expiry
        // time has been set for the elevation model. If none has been set, the expiry times of the model's individual
        // levels are used, but only for tiles in the local file cache, not tiles in memory. This is to avoid incurring
        // the overhead of checking expiration of in-memory tiles, a very rarely used feature.
        if (this.getExpiryTime() > 0 && this.getExpiryTime() < System.currentTimeMillis()) {
            // Normally getUnmappedElevations() does not request elevation tiles, except for first level tiles. However
            // if the tile is already in memory but has expired, we must issue a request to replace the tile data. This
            // will not fetch new tiles into the cache, but rather will force a refresh of the expired tile's resources
            // in the file cache and the memory cache.
            if (tile != null)
                this.checkElevationExpiration(tile);
        }

        // The containing tile is non-null, so look up the elevation and return.
        return this.lookupElevation(latitude, longitude, tile);
    }

    public double getUnmappedLocalSourceElevation(Angle latitude, Angle longitude) {

        if (!this.contains(latitude, longitude))
            return this.getMissingDataSignal();

        Level lastLevel = this.levels.getLastLevel(latitude, longitude);
        final TileKey tileKey = new TileKey(latitude, longitude, this.levels, lastLevel.getLevelNumber());

        ElevationTile tile = this.tileFromMemory(tileKey);
        if (tile != null)
            return this.lookupElevation(latitude, longitude, tile);


        try {
            tile = this.createTile(tileKey);
            final URL url = this.getDataFileStore().findFile(tile.getPath(), false);
            if (url != null) {
                this.loadElevations(tile, url);
            }
        }
        catch (Exception e) {
            String msg = Logging.getMessage("ElevationModel.ExceptionRequestingElevations",
                tileKey.toString());
            Logging.logger().log(java.util.logging.Level.FINE, msg, e);
        }

        tile = this.tileFromMemory(tileKey);
        return tile != null ? this.lookupElevation(latitude, longitude, tile) : this.getMissingDataSignal();
    }

    public double getElevations(Sector sector, List<? extends LatLon> latlons, double targetResolution,
        double[] buffer) {
        return this.getElevations(sector, latlons, targetResolution, buffer, true);
    }

    public double getUnmappedElevations(Sector sector, List<? extends LatLon> latlons, double targetResolution,
        double[] buffer) {
        return this.getElevations(sector, latlons, targetResolution, buffer, false);
    }

    protected double getElevations(Sector sector, List<? extends LatLon> latlons, double targetResolution,
        double[] buffer, boolean mapMissingData) {

        final int n = latlons.size();

        Level targetLevel = this.getTargetLevel(sector, targetResolution);
        if (targetLevel == null)
            return Double.MAX_VALUE;

        Elevations elevations = this.getElevations(sector, this.levels, targetLevel.getLevelNumber());
        if (elevations == null)
            return Double.MAX_VALUE;

        if (this.intersects(sector) == -1)
            return Double.MAX_VALUE;

        // Mark the model as used this frame.
        this.set(AVKey.FRAME_TIMESTAMP, System.currentTimeMillis());

        for (int i = 0; i < n; i++) {
            LatLon ll = latlons.get(i);
            if (ll == null)
                continue;

            Double value = elevations.getElevation(ll.getLatitude(), ll.getLongitude());

            if (this.isTransparentValue(value))
                continue;

            // If an elevation at the given location is available, write that elevation to the destination buffer.
            // If an elevation is not available but the location is within the elevation model's coverage, write the
            // elevation models extreme elevation at the location. Do nothing if the location is not within the
            // elevation model's coverage.
            if (value != null && value != this.getMissingDataSignal())
                buffer[i] = value;
            else if (this.contains(ll.getLatitude(), ll.getLongitude())) {
                if (value == null)
                    buffer[i] = this.getExtremeElevations(sector)[0];
                else if (mapMissingData && value == this.getMissingDataSignal())
                    buffer[i] = this.getMissingDataReplacement();
            }
        }

        return elevations.achievedResolution;
    }

    protected Level getTargetLevel(Sector sector, double targetSize) {
        Level lastLevel = this.levels.getLastLevel(sector); // finest resolution available
        if (lastLevel == null)
            return null;

        if (lastLevel.getTexelSize() >= targetSize)
            return lastLevel; // can't do any better than this

        for (Level level : this.levels.getLevels()) {
            if (level.getTexelSize() <= targetSize)
                return level.isEmpty() ? null : level;

            if (level == lastLevel)
                break;
        }

        return lastLevel;
    }

    protected double lookupElevation(Angle latitude, Angle longitude, final ElevationTile tile) {
        BufferWrapper elevations = tile.getElevations();
        Sector sector = tile.sector;
        final int tileHeight = tile.getHeight();
        final int tileWidth = tile.getWidth();
        final double sectorDeltaLat = sector.latDelta().radians();
        final double sectorDeltaLon = sector.lonDelta().radians();
        final double dLat = sector.latMax().radians() - latitude.radians();
        final double dLon = longitude.radians() - sector.lonMin().radians();
        final double sLat = dLat / sectorDeltaLat;
        final double sLon = dLon / sectorDeltaLon;

        int j = (int) (((tileHeight - 1) * sLat));
        int i = (int) (((tileWidth - 1) * sLon));
        int k = j * tileWidth + i;

        double missing = this.getMissingDataSignal();

        double eLeft = elevations.getDouble(k);
        if (missing == eLeft)
            return missing;
        double eRight = i < (tileWidth - 1) ? elevations.getDouble(k + 1) : eLeft;
        if (missing == eRight)
            return missing;

        double dw = sectorDeltaLon / (tileWidth - 1);
        double dh = sectorDeltaLat / (tileHeight - 1);
        double ssLon = (dLon - i * dw) / dw;
        double ssLat = (dLat - j * dh) / dh;
        double eTop = eLeft + ssLon * (eRight - eLeft);

        if (j < tileHeight - 1 && i < tileWidth - 1) {
            eLeft = elevations.getDouble(k + tileWidth);
            if (missing == eLeft)
                return missing;

            eRight = elevations.getDouble(k + tileWidth + 1);
            if (missing == eRight)
                return missing;
        }

        double eBot = eLeft + ssLon * (eRight - eLeft);
        return eTop + ssLat * (eBot - eTop);
    }

    public double[] getExtremeElevations(Angle latitude, Angle longitude) {

        if (this.extremesLevel < 0 || this.extremes == null)
            return new double[] {this.getMinElevation(), this.getMaxElevation()};

        LatLon delta = this.levels.getLevel(this.extremesLevel).getTileDelta();
        LatLon origin = this.levels.getTileOrigin();
        final int row = ElevationTile.computeRow(delta.getLatitude(), latitude, origin.getLatitude());
        final int col = ElevationTile.computeColumn(delta.getLongitude(), longitude, origin.getLongitude());

        final int nCols = ElevationTile.computeColumn(delta.getLongitude(), Angle.POS180, Angle.NEG180) + 1;

        int index = 2 * (row * nCols + col);

        final double missing = this.getMissingDataSignal();

        double min = this.extremes.getDouble(index);
        if (min == missing)
            min = this.getMissingDataReplacement();

        double max = this.extremes.getDouble(index + 1);
        if (max == missing)
            max = this.getMissingDataReplacement();

        return new double[] {min, max};
    }

    public double[] getExtremeElevations(Sector sector) {

        double[] extremes = this.extremesCachingEnabled
            ? (double[]) this.extremesLookupCache.getObject(sector) : null;
        if (extremes != null)
            return new double[] {extremes[0], extremes[1]}; // return defensive copy

        if (this.extremesLevel < 0 || this.extremes == null)
            return new double[] {this.getMinElevation(), this.getMaxElevation()};

        // Compute the extremes from the extreme-elevations file.
        extremes = this.computeExtremeElevations(sector);
        if (extremes != null && this.isExtremesCachingEnabled())
            this.extremesLookupCache.add(sector, extremes, 64);

        // Return a defensive copy of the array to prevent the caller from modifying the cache contents.
        return extremes != null ? new double[] {extremes[0], extremes[1]} : null;
    }

    public void loadExtremeElevations(String extremesFileName) {

        InputStream is = null;
        try {
            is = this.getClass().getResourceAsStream('/' + extremesFileName);
            if (is == null) {
                // Look directly in the file system
                File file = new File(extremesFileName);
                if (file.exists())
                    is = new FileInputStream(file);
                else
                    Logging.logger().log(java.util.logging.Level.WARNING, "BasicElevationModel.UnavailableExtremesFile",
                        extremesFileName);
            }

            if (is == null)
                return;

            // The level the extremes were taken from is encoded as the last element in the file name
            String[] tokens = extremesFileName.substring(0, extremesFileName.lastIndexOf('.')).split("_");
            this.extremesLevel = Integer.parseInt(tokens[tokens.length - 1]);
            if (this.extremesLevel < 0) {
                this.extremes = null;
                Logging.logger().log(java.util.logging.Level.WARNING, "BasicElevationModel.UnavailableExtremesLevel",
                    extremesFileName);
                return;
            }

            AVList bufferParams = new AVListImpl();
            bufferParams.set(AVKey.DATA_TYPE, AVKey.INT16);
            bufferParams.set(AVKey.BYTE_ORDER, AVKey.BIG_ENDIAN); // Extremes are always saved in JVM byte order
            this.extremes = BufferWrapper.wrap(WWIO.readStreamToBuffer(is, true),
                bufferParams); // Read extremes to a direct ByteBuffer.
        }
        catch (IOException e) {
            Logging.logger().log(java.util.logging.Level.WARNING,
                Logging.getMessage("BasicElevationModel.ExceptionReadingExtremeElevations", extremesFileName), e);
            this.extremes = null;
            this.extremesLevel = -1;
        }
        finally {
            WWIO.closeStream(is, extremesFileName);

            // Clear the extreme elevations lookup cache.
            this.extremesLookupCache.clear();
        }
    }

    protected double[] computeExtremeElevations(Sector sector) {
        LatLon delta = this.levels.getLevel(this.extremesLevel).getTileDelta();
        LatLon origin = this.levels.getTileOrigin();
        final int nwRow = ElevationTile.computeRow(delta.getLatitude(), sector.latMax(),
            origin.getLatitude());
        final int nwCol = ElevationTile.computeColumn(delta.getLongitude(), sector.lonMin(),
            origin.getLongitude());
        final int seRow = ElevationTile.computeRow(delta.getLatitude(), sector.latMin(),
            origin.getLatitude());
        final int seCol = ElevationTile.computeColumn(delta.getLongitude(), sector.lonMax(),
            origin.getLongitude());

        final int nCols = ElevationTile.computeColumn(delta.getLongitude(), Angle.POS180, Angle.NEG180) + 1;

        double min = Double.MAX_VALUE;
        double max = -Double.MAX_VALUE;

        for (int col = nwCol; col <= seCol; col++) {
            for (int row = seRow; row <= nwRow; row++) {
                int index = 2 * (row * nCols + col);
                double a = this.extremes.getDouble(index);
                double b = this.extremes.getDouble(index + 1);

                if (a == this.getMissingDataSignal())
                    a = this.getMissingDataReplacement();
                if (b == this.getMissingDataSignal())
                    b = this.getMissingDataReplacement();

                if (a > max)
                    max = a;
                if (a < min)
                    min = a;
                if (b > max)
                    max = b;
                if (b < min)
                    min = b;
            }
        }

        // Set to model's limits if for some reason a limit wasn't determined
        if (min == Double.MAX_VALUE)
            min = this.getMinElevation();
        if (max == -Double.MAX_VALUE)
            max = this.getMaxElevation();

        return new double[] {min, max};
    }

    protected Elevations getElevations(Sector requestedSector, LevelSet levelSet, int targetLevelNumber) {
        // Compute the intersection of the requested sector with the LevelSet's sector.
        // The intersection will be used to determine which Tiles in the LevelSet are in the requested sector.
        Sector sector = requestedSector.intersection(levelSet.getSector());

        Level targetLevel = levelSet.getLevel(targetLevelNumber);
        LatLon delta = targetLevel.getTileDelta();
        LatLon origin = levelSet.getTileOrigin();
        final int nwRow = Tile.computeRow(delta.latitude, sector.latMax, origin.latitude);
        final int nwCol = Tile.computeColumn(delta.longitude, sector.lonMin, origin.longitude);
        final int seRow = Tile.computeRow(delta.latitude, sector.latMin, origin.latitude);
        final int seCol = Tile.computeColumn(delta.longitude, sector.lonMax, origin.longitude);
        TreeSet<ElevationTile> tiles = new TreeSet<>((t1, t2) -> {
            if (t1 == t2)
                return 0;

            final int l1 = t1.getLevelNumber();
            final int l2 = t2.getLevelNumber();
            if (l2 == l1) {
                int dr = Integer.compare(t1.row, t2.row);
                if (dr != 0)
                    return dr;
                return Integer.compare(t1.col, t2.col);
            } else
                return l1 > l2 ? -1 : 1;             // Higher-res levels compare lower than lower-res
        });
        Collection<TileKey> requested = new ArrayList<>();

        final int targetLevelNum = targetLevel.getLevelNumber();
        final String targetLevelCacheName = targetLevel.getCacheName();

        boolean missingTargetTiles = false;
        boolean missingLevelZeroTiles = false;
        for (int row = seRow; row <= nwRow; row++) {
            for (int col = nwCol; col <= seCol; col++) {

                TileKey key = new TileKey(targetLevelNum, row, col, targetLevelCacheName);
                ElevationTile tile = this.tileFromMemory(key);
                if (tile != null) {
                    tiles.add(tile);
                    continue;
                }

                missingTargetTiles = true;
                this.requestTile(key);

                // Determine the fallback to use. Simultaneously determine a fallback to request that is
                // the next resolution higher than the fallback chosen, if any. This will progressively
                // refine the display until the desired resolution tile arrives.
                TileKey fallbackToRequest = null;
                TileKey fallbackKey;
                int fallbackRow = row;
                int fallbackCol = col;
                for (int fallbackLevelNum = key.level - 1; fallbackLevelNum >= 0; fallbackLevelNum--) {
                    fallbackRow /= 2;
                    fallbackCol /= 2;
                    fallbackKey = new TileKey(fallbackLevelNum, fallbackRow, fallbackCol,
                        this.levels.getLevel(fallbackLevelNum).getCacheName());

                    tile = this.tileFromMemory(fallbackKey);
                    if (tile != null) {
                        tiles.add(tile);
                        break;
                    } else {
                        if (fallbackLevelNum == 0)
                            missingLevelZeroTiles = true;
                        fallbackToRequest = fallbackKey; // keep track of lowest level to request
                    }
                }

                if (fallbackToRequest != null && requested.add(fallbackToRequest)) { //deduplicate requests
                    this.requestTile(fallbackToRequest);
                }
            }
        }

        Elevations elevations;

        if (missingLevelZeroTiles || tiles.isEmpty()) {
            // Double.MAX_VALUE is a signal for no in-memory tile for a given region of the sector.
            elevations = new Elevations(this, Double.MAX_VALUE);
            elevations.tiles = tiles;
        } else if (missingTargetTiles) {
            // Use the level of the the lowest resolution found to denote the resolution of this elevation set.
            // The list of tiles is sorted first by level, so use the level of the list's last entry.
            elevations = new Elevations(this, tiles.last().level.getTexelSize());
            elevations.tiles = tiles;
        } else {
            elevations = new Elevations(this, tiles.last().level.getTexelSize());

            // Compute the elevation extremes now that the sector is fully resolved
            if (!tiles.isEmpty()) {
                elevations.tiles = tiles;
                double[] extremes = elevations.getTileExtremes();
                if (extremes != null && this.isExtremesCachingEnabled()) {
                    // Cache the newly computed extremes if they're different from the currently cached ones.
                    double[] currentExtremes = (double[]) this.extremesLookupCache.getObject(requestedSector);
                    if (currentExtremes == null || currentExtremes[0] != extremes[0]
                        || currentExtremes[1] != extremes[1])
                        this.extremesLookupCache.add(requestedSector, extremes, 64);
                }
            }
        }

        // Check tile expiration. Memory-cached tiles are checked for expiration only when an explicit, non-zero expiry
        // time has been set for the elevation model. If none has been set, the expiry times of the model's individual
        // levels are used, but only for tiles in the local file cache, not tiles in memory. This is to avoid incurring
        // the overhead of checking expiration of in-memory tiles, a very rarely used feature.
        if (this.getExpiryTime() > 0 && this.getExpiryTime() < System.currentTimeMillis())
            this.checkElevationExpiration(tiles);

        return elevations;
    }

    protected void checkElevationExpiration(ElevationTile tile) {
        if (tile.isElevationsExpired())
            this.requestTile(tile.tileKey);
    }

//    public final int getTileCount(Sector sector, int resolution)
//    {
//        if (sector == null)
//        {
//            String msg = Logging.getMessage("nullValue.SectorIsNull");
//            Logging.logger().severe(msg);
//            throw new IllegalArgumentException(msg);
//        }
//
//        // Collect all the elevation tiles intersecting the input sector. If a desired tile is not curently
//        // available, choose its next lowest resolution parent that is available.
//        final Level targetLevel = this.levels.getLevel(resolution);
//
//        LatLon delta = this.levels.getLevel(resolution).getTileDelta();
//        final int nwRow = Tile.computeRow(delta.getLatitude(), sector.getMaxLatitude());
//        final int nwCol = Tile.computeColumn(delta.getLongitude(), sector.getMinLongitude());
//        final int seRow = Tile.computeRow(delta.getLatitude(), sector.getMinLatitude());
//        final int seCol = Tile.computeColumn(delta.getLongitude(), sector.getMaxLongitude());
//
//        return (1 + (nwRow - seRow) * (1 + seCol - nwCol));
//    }

    //**************************************************************//
    //********************  Non-Tile Resource Retrieval  ***********//
    //**************************************************************//

    protected void checkElevationExpiration(Iterable<? extends ElevationTile> tiles) {
        for (ElevationTile tile : tiles) {
            if (tile.isElevationsExpired())
                this.requestTile(tile.tileKey);
        }
    }

    /**
     * Retrieves any non-tile resources associated with this ElevationModel, either online or in the local filesystem,
     * and initializes properties of this ElevationModel using those resources. This returns a key indicating the
     * retrieval state: {@link AVKey#RETRIEVAL_STATE_SUCCESSFUL} indicates the retrieval succeeded, {@link
     * AVKey#RETRIEVAL_STATE_ERROR} indicates the retrieval failed with errors, and <code>null</code> indicates the
     * retrieval state is unknown. This method may invoke blocking I/O operations, and therefore should not be executed
     * from the rendering thread.
     *
     * @return {@link AVKey#RETRIEVAL_STATE_SUCCESSFUL} if the retrieval succeeded, {@link AVKey#RETRIEVAL_STATE_ERROR}
     * if the retrieval failed with errors, and <code>null</code> if the retrieval state is unknown.
     */
    protected String retrieveResources() {
        // This ElevationModel has no construction parameters, so there is no description of what to retrieve. Return a
        // key indicating the resources have been successfully retrieved, though there is nothing to retrieve.
        AVList params = (AVList) this.get(AVKey.CONSTRUCTION_PARAMETERS);
//        if (params == null) {
//            String message = Logging.getMessage("nullValue.ConstructionParametersIsNull");
//            Logging.logger().warning(message);
//            return AVKey.RETRIEVAL_STATE_SUCCESSFUL;
//        }

        // This ElevationModel has no OGC Capabilities URL in its construction parameters. Return a key indicating the
        // resources have been successfully retrieved, though there is nothing to retrieve.
        URL url = DataConfigurationUtils.getOGCGetCapabilitiesURL(params);
//        if (url == null) {
//            String message = Logging.getMessage("nullValue.CapabilitiesURLIsNull");
//            Logging.logger().warning(message);
//            return AVKey.RETRIEVAL_STATE_SUCCESSFUL;
//        }

        // Get the service's OGC Capabilities resource from the session cache, or initiate a retrieval to fetch it in
        // a separate thread. SessionCacheUtils.getOrRetrieveSessionCapabilities() returns null if it initiated a
        // retrieval, or if the OGC Capabilities URL is unavailable.
        //
        // Note that we use the URL's String representation as the cache key. We cannot use the URL itself, because
        // the cache invokes the methods Object.hashCode() and Object.equals() on the cache key. URL's implementations
        // of hashCode() and equals() perform blocking IO calls. WorldWind does not perform blocking calls during
        // rendering, and this method is likely to be called from the rendering thread.
        WMSCapabilities caps;
        if (this.isNetworkRetrievalEnabled())
            caps = SessionCacheUtils.getOrRetrieveSessionCapabilities(url, WorldWind.getSessionCache(),
                url.toString(), null, BasicElevationModel.RESOURCE_ID_OGC_CAPABILITIES, null, null);
        else
            caps = SessionCacheUtils.getSessionCapabilities(WorldWind.getSessionCache(), url.toString(),
                url.toString());

        // The OGC Capabilities resource retrieval is either currently running in another thread, or has failed. In
        // either case, return null indicating that that the retrieval was not successful, and we should try again
        // later.
        if (caps == null)
            return null;

        // We have successfully retrieved this ElevationModel's OGC Capabilities resource. Initialize this ElevationModel
        // using the Capabilities document, and return a key indicating the retrieval has succeeded.
        this.initFromOGCCapabilitiesResource(caps, params);

        return AVKey.RETRIEVAL_STATE_SUCCESSFUL;
    }

    /**
     * Initializes this ElevationModel's expiry time property from the specified WMS Capabilities document and parameter
     * list describing the WMS layer names associated with this ElevationModel. This method is thread safe; it
     * synchronizes changes to this ElevationModel by wrapping the appropriate method calls in {@link
     * SwingUtilities#invokeLater(Runnable)}.
     *
     * @param caps   the WMS Capabilities document retrieved from this ElevationModel's WMS server.
     * @param params the parameter list describing the WMS layer names associated with this ElevationModel.
     * @throws IllegalArgumentException if either the Capabilities or the parameter list is null.
     */
    protected void initFromOGCCapabilitiesResource(WMSCapabilities caps, AVList params) {

        String[] names = DataConfigurationUtils.getOGCLayerNames(params);
        if (names == null || names.length == 0)
            return;

        final Long expiryTime = caps.getLayerLatestLastUpdateTime(names);
        if (expiryTime == null)
            return;

        // Synchronize changes to this ElevationModel with the Event Dispatch Thread.
//        SwingUtilities.invokeLater(() -> {
        BasicElevationModel.this.setExpiryTime(expiryTime);
        BasicElevationModel.this.firePropertyChange(AVKey.ELEVATION_MODEL, null, BasicElevationModel.this);
//        });
    }

    /**
     * Returns a boolean value indicating if this ElevationModel should retrieve any non-tile resources, either online
     * or in the local filesystem, and initialize itself using those resources.
     *
     * @return <code>true</code> if this ElevationModel should retrieve any non-tile resources, and <code>false</code>
     * otherwise.
     */
    protected boolean isRetrieveResources() {
        AVList params = (AVList) this.get(AVKey.CONSTRUCTION_PARAMETERS);
        if (params == null)
            return false;

        Boolean b = (Boolean) params.get(AVKey.RETRIEVE_PROPERTIES_FROM_SERVICE);
        return b != null && b;
    }

    //**************************************************************//
    //********************  Configuration  *************************//
    //**************************************************************//

    /**
     * Starts retrieving non-tile resources associated with this model in a non-rendering thread.
     */
    protected void startResourceRetrieval() {
        Thread t = new Thread(this::retrieveResources);
        t.setName("Capabilities retrieval for " + this.name());
        t.start();
    }

    protected void writeConfigurationFile(FileStore fileStore) {
        // TODO: configurable max attempts for creating a configuration file.

        AVList configParams = this.getConfigurationParams(null);
        this.writeConfigurationParams(configParams, fileStore);
    }

    protected void writeConfigurationParams(AVList params, FileStore fileStore) {
        // Determine what the configuration file name should be based on the configuration parameters. Assume an XML
        // configuration document type, and append the XML file suffix.
        String fileName = DataConfigurationUtils.getDataConfigFilename(params, ".xml");
//        if (fileName == null) {
//            String message = Logging.getMessage("nullValue.FilePathIsNull");
//            Logging.logger().severe(message);
//            throw new WWRuntimeException(message);
//        }

        // Check if this component needs to write a configuration file. This happens outside of the synchronized block
        // to improve multithreaded performance for the common case: the configuration file already exists, this just
        // need to check that it's there and return. If the file exists but is expired, do not remove it -  this
        // removes the file inside the synchronized block below.
        if (!this.needsConfigurationFile(fileStore, fileName, params, false))
            return;

        synchronized (this.fileLock) {
            // Check again if the component needs to write a configuration file, potentially removing any existing file
            // which has expired. This additional check is necessary because the file could have been created by
            // another thread while we were waiting for the lock.
            if (!this.needsConfigurationFile(fileStore, fileName, params, true))
                return;

            this.doWriteConfigurationParams(fileStore, fileName, params);
        }
    }

    protected void doWriteConfigurationParams(FileStore fileStore, String fileName, AVList params) {
        File file = fileStore.newFile(fileName);

        Document doc = this.createConfigurationDocument(params);
        WWXML.saveDocumentToFile(doc, file.getPath());

        String message = Logging.getMessage("generic.ConfigurationFileCreated", fileName);
        Logging.logger().fine(message);
    }

    protected boolean needsConfigurationFile(FileStore fileStore, String fileName, AVList params,
        boolean removeIfExpired) {
        long expiryTime = this.getExpiryTime();
        if (expiryTime <= 0)
            expiryTime = AVListImpl.getLongValue(params, AVKey.EXPIRY_TIME, 0L);

        return !DataConfigurationUtils.hasDataConfigFile(fileStore, fileName, removeIfExpired, expiryTime);
    }

    protected AVList getConfigurationParams(AVList params) {
        if (params == null)
            params = new AVListImpl();

        // Gather all the construction parameters if they are available.
        AVList constructionParams = (AVList) this.get(AVKey.CONSTRUCTION_PARAMETERS);
        if (constructionParams != null)
            params.setValues(constructionParams);

        // Gather any missing LevelSet parameters from the LevelSet itself.
        DataConfigurationUtils.getLevelSetConfigParams(this.getLevels(), params);

        // Gather any missing parameters about the elevation data. These values must be available for consumers of the
        // model configuration to property interpret the cached elevation files. While the elevation model assumes
        // default values when these properties are missing, a different system does not know what those default values
        // should be, and thus cannot assume anything about the value of these properties.

        if (params.get(AVKey.BYTE_ORDER) == null)
            params.set(AVKey.BYTE_ORDER, this.getElevationDataByteOrder());

        if (params.get(AVKey.DATA_TYPE) == null)
            params.set(AVKey.DATA_TYPE, this.getElevationDataType());

        if (params.get(AVKey.MISSING_DATA_SIGNAL) == null)
            params.set(AVKey.MISSING_DATA_SIGNAL, this.getMissingDataSignal());

        return params;
    }

    protected Document createConfigurationDocument(AVList params) {
        return BasicElevationModel.createBasicElevationModelConfigDocument(params);
    }

    public String getRestorableState() {
        // We only create a restorable state XML if this elevation model was constructed with an AVList.
        AVList constructionParams = (AVList) this.get(AVKey.CONSTRUCTION_PARAMETERS);
        if (constructionParams == null)
            return null;

        RestorableSupport rs = RestorableSupport.newRestorableSupport();
        // Creating a new RestorableSupport failed. RestorableSupport logged the problem, so just return null.

        this.doGetRestorableState(rs, null);
        return rs.getStateAsXml();
    }

    public void restoreState(String stateInXml) {
        String message = Logging.getMessage("RestorableSupport.RestoreRequiresConstructor");
        Logging.logger().severe(message);
        throw new UnsupportedOperationException(message);
    }

    //**************************************************************//
    //********************  Restorable Support  ********************//
    //**************************************************************//

    protected void doGetRestorableState(RestorableSupport rs, RestorableSupport.StateObject context) {
        AVList constructionParams = (AVList) this.get(AVKey.CONSTRUCTION_PARAMETERS);
        if (constructionParams != null) {
            for (Map.Entry<String, Object> avp : constructionParams.getEntries()) {
                this.getRestorableStateForAVPair(avp.getKey(), avp.getValue(), rs, context);
            }
        }

        rs.addStateValueAsString(context, "ElevationModel.Name", this.name());
        rs.addStateValueAsDouble(context, "ElevationModel.MissingDataFlag", this.getMissingDataSignal());
        rs.addStateValueAsDouble(context, "ElevationModel.MissingDataValue", this.getMissingDataReplacement());
        rs.addStateValueAsBoolean(context, "ElevationModel.NetworkRetrievalEnabled", this.isNetworkRetrievalEnabled());
        rs.addStateValueAsDouble(context, "ElevationModel.MinElevation", this.getMinElevation());
        rs.addStateValueAsDouble(context, "ElevationModel.MaxElevation", this.getMaxElevation());
        rs.addStateValueAsString(context, "BasicElevationModel.DataType", this.getElevationDataType());
        rs.addStateValueAsString(context, "BasicElevationModel.DataByteOrder", this.getElevationDataByteOrder());

        // We'll write the detail hint attribute only when it's a nonzero value.
        if (this.detailHint != 0.0)
            rs.addStateValueAsDouble(context, "BasicElevationModel.DetailHint", this.detailHint);

        RestorableSupport.StateObject so = rs.addStateObject(context, "avlist");
        for (Map.Entry<String, Object> avp : this.getEntries()) {
            this.getRestorableStateForAVPair(avp.getKey(), avp.getValue(), rs, so);
        }
    }

    public void getRestorableStateForAVPair(String key, Object value,
        RestorableSupport rs, RestorableSupport.StateObject context) {
        if (value == null)
            return;

        if (key.equals(AVKey.CONSTRUCTION_PARAMETERS))
            return;

        if (value instanceof LatLon) {
            rs.addStateValueAsLatLon(context, key, (LatLon) value);
        } else if (value instanceof Sector) {
            rs.addStateValueAsSector(context, key, (Sector) value);
        } else {
            super.getRestorableStateForAVPair(key, value, rs, context);
        }
    }

    protected void doRestoreState(RestorableSupport rs, RestorableSupport.StateObject context) {
        String s = rs.getStateValueAsString(context, "ElevationModel.Name");
        if (s != null)
            this.setName(s);

        Double d = rs.getStateValueAsDouble(context, "ElevationModel.MissingDataFlag");
        if (d != null)
            this.setMissingDataSignal(d);

        d = rs.getStateValueAsDouble(context, "ElevationModel.MissingDataValue");
        if (d != null)
            this.setMissingDataReplacement(d);

        Boolean b = rs.getStateValueAsBoolean(context, "ElevationModel.NetworkRetrievalEnabled");
        if (b != null)
            this.setNetworkRetrievalEnabled(b);

        // Look for the elevation data type using the current property name "BasicElevationModel.DataType", or the the
        // old property name "BasicElevationModel.DataPixelType" if a property with the current name does not exist.
        s = rs.getStateValueAsString(context, "BasicElevationModel.DataType");
        if (s == null)
            s = rs.getStateValueAsString(context, "BasicElevationModel.DataPixelType");
        if (s != null)
            this.setElevationDataType(s);

        s = rs.getStateValueAsString(context, "BasicElevationModel.DataByteOrder");
        if (s != null)
            this.setByteOrder(s);

        d = rs.getStateValueAsDouble(context, "BasicElevationModel.DetailHint");
        if (d != null)
            this.setDetailHint(d);

        // Intentionally omitting "ElevationModel.MinElevation" and "ElevationModel.MaxElevation" because they are final
        // properties only configurable at construction.

        RestorableSupport.StateObject so = rs.getStateObject(context, "avlist");
        if (so != null) {
            RestorableSupport.StateObject[] avpairs = rs.getAllStateObjects(so, "");
            if (avpairs != null) {
                for (RestorableSupport.StateObject avp : avpairs) {
                    if (avp != null)
                        this.doRestoreStateForObject(rs, avp);
                }
            }
        }
    }

    @SuppressWarnings("UnusedDeclaration")
    protected void doRestoreStateForObject(RestorableSupport rs, RestorableSupport.StateObject so) {
        if (so == null)
            return;

        // Map the old PIXEL_TYPE AVKey constant to the new DATA_TYPE constant.
        if ("gov.nasa.worldwind.avkey.PixelType".equals(so.getName()))
            this.set(AVKey.DATA_TYPE, so.getValue());
        else
            this.set(so.getName(), so.getValue());
    }

    @Override
    public double getLocalDataAvailability(Sector requestedSector, Double targetResolution) {
//        if (requestedSector == null) {
//            String msg = Logging.getMessage("nullValue.SectorIsNull");
//            Logging.logger().severe(msg);
//            throw new IllegalArgumentException(msg);
//        }

        // Compute intersection of the requested sector and the sector covered by the elevation model.
        LevelSet levelSet = this.getLevels();
        Sector sector = requestedSector.intersection(levelSet.getSector());

        // If there is no intersection there is no data to retrieve
        if (sector == null)
            return 1.0d;

        Level targetLevel = targetResolution != null
            ? this.getTargetLevel(sector, targetResolution) : levelSet.getLastLevel();

        // Count all the tiles intersecting the input sector.
        long numLocalTiles = 0;
        long numMissingTiles = 0;
        LatLon delta = targetLevel.getTileDelta();
        LatLon origin = levelSet.getTileOrigin();
        final int nwRow = Tile.computeRow(delta.getLatitude(), sector.latMax(), origin.getLatitude());
        final int nwCol = Tile.computeColumn(delta.getLongitude(), sector.lonMin(), origin.getLongitude());
        final int seRow = Tile.computeRow(delta.getLatitude(), sector.latMin(), origin.getLatitude());
        final int seCol = Tile.computeColumn(delta.getLongitude(), sector.lonMax(), origin.getLongitude());

        for (int row = nwRow; row >= seRow; row--) {
            for (int col = nwCol; col <= seCol; col++) {
                TileKey key = new TileKey(targetLevel.getLevelNumber(), row, col, targetLevel.getCacheName());
                Sector tileSector = levelSet.computeSectorForKey(key);
                Tile tile = new Tile(tileSector, targetLevel, row, col);
                if (!this.isTileLocalOrAbsent(tile))
                    ++numMissingTiles;
                else
                    ++numLocalTiles;
            }
        }

        return numLocalTiles > 0 ? numLocalTiles / (double) (numLocalTiles + numMissingTiles) : 0.0d;
    }

    protected boolean isTileLocalOrAbsent(Tile tile) {
        if (this.getLevels().missing(tile))
            return true;  // tile is absent

        URL url = this.getDataFileStore().findFile(tile.getPath(), false);

        return url != null && !BasicElevationModel.isFileExpired(tile, url, this.getDataFileStore());
    }

    protected static class RequestTask implements Runnable {
        protected final BasicElevationModel elevationModel;
        protected final TileKey tileKey;

        protected RequestTask(TileKey tileKey, BasicElevationModel elevationModel) {
            this.elevationModel = elevationModel;
            this.tileKey = tileKey;
        }

        public final void run() {
//            if (Thread.currentThread().isInterrupted())
//                return; // the task was cancelled because it's a duplicate or for some other reason

            try {
                // check to ensure load is still needed
                if (this.elevationModel.areElevationsInMemory(this.tileKey))
                    return;

                ElevationTile tile = this.elevationModel.createTile(this.tileKey);
                final URL url = this.elevationModel.getDataFileStore().findFile(tile.getPath(), false);
                if (url != null && !BasicElevationModel.isFileExpired(tile, url,
                    this.elevationModel.getDataFileStore())) {
                    if (this.elevationModel.loadElevations(tile, url)) {
                        this.elevationModel.levels.has(tile);
                        this.elevationModel.firePropertyChange(AVKey.ELEVATION_MODEL, null, this);
                        return;
                    } else {
                        // Assume that something's wrong with the file and delete it.
                        this.elevationModel.levels.miss(tile);
                        this.elevationModel.getDataFileStore().removeFile(url);
                        Logging.logger().info(Logging.getMessage("generic.DeletedCorruptDataFile", url));
                    }
                }

                this.elevationModel.downloadElevations(tile);
            }
            catch (Exception e) {
                Logging.logger().log(java.util.logging.Level.FINE, Logging.getMessage("ElevationModel.ExceptionRequestingElevations",
                    this.tileKey.toString()), e);
            }
        }

        public final boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;

            final RequestTask that = (RequestTask) o;

            //noinspection RedundantIfStatement
            return Objects.equals(this.tileKey, that.tileKey);
        }

        public final int hashCode() {
            return (this.tileKey != null ? this.tileKey.hashCode() : 0);
        }

        public final String toString() {
            return this.tileKey.toString();
        }
    }

    protected static class DownloadPostProcessor extends AbstractRetrievalPostProcessor {
        protected final Tile tile;
        protected final BasicElevationModel elevationModel;
        protected final FileStore fileStore;

        public DownloadPostProcessor(Tile tile, BasicElevationModel em) {
            this(tile, em, null);
        }

        public DownloadPostProcessor(Tile tile, BasicElevationModel em, FileStore fileStore) {
            //noinspection RedundantCast
            super((AVList) em);

            this.tile = tile;
            this.elevationModel = em;
            this.fileStore = fileStore;
        }

        protected FileStore getFileStore() {
            return this.fileStore != null ? this.fileStore : this.elevationModel.getDataFileStore();
        }

        @Override
        protected boolean overwriteExistingFile() {
            return true;
        }

        @Override
        protected void markResourceAbsent() {
            this.elevationModel.getLevels().miss(this.tile);
        }

        @Override
        protected Object getFileLock() {
            return this.elevationModel.fileLock;
        }

        @Override
        protected File doGetOutputFile() {
            return this.getFileStore().newFile(this.tile.getPath());
        }

        @Override
        protected ByteBuffer handleSuccessfulRetrieval() {
            ByteBuffer buffer = super.handleSuccessfulRetrieval();

            if (buffer != null) {
                // We've successfully cached data. Check whether there's a configuration file for this elevation model
                // in the cache and create one if there isn't.
                this.elevationModel.writeConfigurationFile(this.getFileStore());

                // Fire a property change to denote that the model's backing data has changed.
                this.elevationModel.firePropertyChange(AVKey.ELEVATION_MODEL, null, this);
            }

            return buffer;
        }

        @Override
        protected ByteBuffer handleTextContent() throws IOException {
            this.markResourceAbsent();

            return super.handleTextContent();
        }
    }

    /**
     * Internal class to hold collections of elevation tiles that provide elevations for a specific sector.
     */
    protected static class Elevations {
        protected final BasicElevationModel elevationModel;
        protected final double achievedResolution;
        protected Set<ElevationTile> tiles;
        protected double[] extremes;

        protected Elevations(BasicElevationModel elevationModel, double achievedResolution) {
            this.elevationModel = elevationModel;
            this.achievedResolution = achievedResolution;
        }

        protected Double getElevation(Angle latitude, Angle longitude) {

            if (this.tiles == null)
                return null;

                for (ElevationTile tile : this.tiles) {
                    if (tile.sector.contains(latitude, longitude))
                        return this.elevationModel.lookupElevation(latitude, longitude, tile);
                }

                // Location is not within this group of tiles, so is outside the coverage of this elevation model.
                return null;

        }

        protected double[] getExtremes(Angle latitude, Angle longitude) {

            if (this.extremes != null)
                return this.extremes;

            if (this.tiles == null || tiles.isEmpty())
                return this.elevationModel.getExtremeElevations(latitude, longitude);

            return this.getExtremes();
        }

        /**
         * Get the extreme values (min/max) of this collection of elevations.
         *
         * @return the extreme elevation values.
         */
        protected double[] getExtremes() {
            if (this.extremes != null)
                return this.extremes;

            if (this.tiles == null || tiles.isEmpty())
                return this.extremes = new double[] {this.elevationModel.getMinElevation(),
                    this.elevationModel.getMaxElevation()};

            this.extremes = WWUtil.defaultMinMix();

            for (ElevationTile tile : this.tiles) {
                BufferWrapper elevations = tile.getElevations();

                int len = elevations.length();
                if (len == 0)
                    return null;

                for (int i = 0; i < len; i++) {
                    this.elevationModel.determineExtremes(elevations.getDouble(i), this.extremes);
                }
            }

            return new double[] {this.extremes[0], this.extremes[1]}; // return a defensive copy
        }

        protected double[] getExtremes(Sector sector) {
            if (this.extremes != null)
                return this.extremes;

            Iterator<ElevationTile> iter = this.tiles.iterator();
            if (!iter.hasNext())
                return this.extremes = new double[] {this.elevationModel.getMinElevation(),
                    this.elevationModel.getMaxElevation()};

            this.extremes = WWUtil.defaultMinMix();

            for (ElevationTile tile : this.tiles) {
                tile.getExtremes(sector, this.elevationModel, this.extremes);
            }

            return this.extremes;
        }

        /**
         * Returns the extreme values among all the tiles in this object.
         *
         * @return the extreme values.
         */
        protected double[] getTileExtremes() {
            if (this.extremes != null)
                return this.extremes;

            Iterator<ElevationTile> iter = this.tiles.iterator();
            if (!iter.hasNext())
                return this.extremes = new double[] {this.elevationModel.getMinElevation(),
                    this.elevationModel.getMaxElevation()};

            this.extremes = WWUtil.defaultMinMix();

            for (ElevationTile tile : this.tiles) {
                // This computes the extremes on a tile granularity rather than an elevation-value cell granularity.
                // The latter is very expensive.
                if (tile.extremes[0] < this.extremes[0])
                    this.extremes[0] = tile.extremes[0];
                if (tile.extremes[1] > this.extremes[1])
                    this.extremes[1] = tile.extremes[1];
            }

            return this.extremes;
        }
    }

    protected static class ElevationTile extends Tile {
        protected BufferWrapper elevations; // the elevations themselves
        protected long updateTime;
        protected double[] extremes = new double[2];

        protected ElevationTile(Sector sector, Level level, int row, int col) {
            super(sector, level, row, col);
        }

        public BufferWrapper getElevations() {
            return this.elevations;
        }

        public void setElevations(BufferWrapper elevations, BasicElevationModel em) {
            this.elevations = elevations;
            this.updateTime = System.currentTimeMillis();

            final int n = this.elevations.length();
            if (n > 0) {
                this.extremes = WWUtil.defaultMinMix();
                for (int i = 0; i < n; i++) {
                    em.determineExtremes(this.elevations.getDouble(i), this.extremes);
                }
            }
        }

        public boolean isElevationsExpired() {
            return this.isElevationsExpired(level.getExpiryTime());
        }

        public boolean isElevationsExpired(long expiryTime) {
            return this.updateTime > 0 && this.updateTime < expiryTime;
        }

        public int computeElevationIndex(LatLon location) {
            Sector sector = this.sector;

            final int tileHeight = this.getHeight();
            final int tileWidth = this.getWidth();

            final double sectorDeltaLat = sector.latDelta().radians();
            final double sectorDeltaLon = sector.lonDelta().radians();

            final double dLat = sector.latMax().radians() - location.getLatitude().radians();
            final double dLon = location.getLongitude().radians() - sector.lonMin().radians();

            final double sLat = dLat / sectorDeltaLat;
            final double sLon = dLon / sectorDeltaLon;

            int j = (int) ((tileHeight - 1) * sLat);
            int i = (int) ((tileWidth - 1) * sLon);

            return j * tileWidth + i;
        }

        public double[] getExtremes(Sector sector, BasicElevationModel em, double[] extremes) {
            Sector intersection = this.sector.intersection(sector);
            if (intersection == null)
                return extremes;

            LatLon[] corners = intersection.getCorners();
            int[] indices = new int[4];
            for (int i = 0; i < 4; i++) {
                int k = this.computeElevationIndex(corners[i]);
                indices[i] = k < 0 ? 0 : Math.min(k, this.elevations.length() - 1);
            }

            int sw = indices[0];
            int se = indices[1];
            int nw = indices[3];

            int nCols = se - sw + 1;

            if (extremes == null)
                extremes = WWUtil.defaultMinMix();

            int width = this.getWidth();
            while (nw <= sw) {
                for (int i = 0; i < nCols; i++) {
                    em.determineExtremes(this.elevations.getDouble(nw + i), extremes);
                }

                nw += width;
            }

            return extremes;
        }
    }
}
