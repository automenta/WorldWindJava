/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.layers;

import com.jogamp.opengl.*;
import gov.nasa.worldwind.*;
import gov.nasa.worldwind.avlist.*;
import gov.nasa.worldwind.cache.GpuResourceCache;
import gov.nasa.worldwind.geom.Box;
import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.globes.*;
import gov.nasa.worldwind.render.*;
import gov.nasa.worldwind.retrieve.*;
import gov.nasa.worldwind.util.*;
import org.w3c.dom.*;

import javax.imageio.ImageIO;
import javax.xml.xpath.XPath;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.*;
import java.io.*;
import java.net.*;
import java.util.List;
import java.util.*;
import java.util.concurrent.PriorityBlockingQueue;

/**
 * @author tag
 * @version $Id: TiledImageLayer.java 2922 2015-03-24 23:56:58Z tgaskins $
 */
public abstract class TiledImageLayer extends AbstractLayer {
    protected static final double detailHintOrigin = 2.8;
    protected static final Comparator<TextureTile> levelComparator = (ta, tb) -> {
        if (ta == tb)
            return 0;

        final TextureTile taf = ta.getFallbackTile();
        final TextureTile tbf = tb.getFallbackTile();
        var la = taf == null ? ta : taf;
        var lb = tbf == null ? tb : tbf;

        int l = Integer.compare(la.getLevelNumber(), lb.getLevelNumber());
        if (l != 0)
            return l;
        else
            return Integer.compare(System.identityHashCode(ta), System.identityHashCode(tb));
    };
    private static final int QUEUE_SIZE = 2048;
    protected final LevelSet levels;
    protected final ArrayList<String> supportedImageFormats = new ArrayList<>();
    // Stuff computed each frame
    protected final ArrayList<TextureTile> currentTiles = new ArrayList<>();
    protected final PriorityBlockingQueue<Runnable> requestQ = new PriorityBlockingQueue<>(TiledImageLayer.QUEUE_SIZE);
    protected ArrayList<TextureTile> topLevels;
//    protected boolean forceLevelZeroLoads;
    protected boolean levelZeroLoaded;
    protected boolean retainLevelZeroTiles;
    protected String tileCountName;
    protected double detailHint;
    protected boolean useMipMaps = true;
    protected boolean useTransparentTextures;
    protected String textureFormat;
    // Diagnostic flags
    protected boolean drawTileBoundaries;
    protected boolean drawBoundingVolumes;
    protected TextureTile currentResourceTile;
    protected boolean atMaxResolution;
    private boolean drawTileIDs;

    public TiledImageLayer(LevelSet levelSet) {

        this.levels = new LevelSet(levelSet); // the caller's levelSet may change internally, so we copy it.
        this.set(AVKey.SECTOR, this.levels.getSector());

        this.setPickEnabled(false); // textures are assumed to be terrain unless specifically indicated otherwise.
        this.tileCountName = this.name() + " Tiles";
    }

    /**
     * Creates a configuration document for a TiledImageLayer described by the specified params. The returned document
     * may be used as a construction parameter to {@link BasicTiledImageLayer}.
     *
     * @param params parameters describing the TiledImageLayer.
     * @return a configuration document for the TiledImageLayer.
     */
    public static Document createTiledImageLayerConfigDocument(AVList params) {
        Document doc = WWXML.createDocumentBuilder(true).newDocument();

        Element root = WWXML.setDocumentElement(doc, "Layer");
        WWXML.setIntegerAttribute(root, "version", 1);
        WWXML.setTextAttribute(root, "layerType", "TiledImageLayer");

        TiledImageLayer.createTiledImageLayerConfigElements(params, root);

        return doc;
    }

    /**
     * Appends TiledImageLayer configuration parameters as elements to the specified context. This appends elements for
     * the following parameters: <table> <caption style="font-weight: bold;">Parameters</caption>
     * <tr><th>Parameter</th><th>Element Path</th><th>Type</th></tr> <tr><td>{@link
     * AVKey#SERVICE_NAME}</td><td>Service/@serviceName</td><td>String</td></tr> <tr><td>{@link
     * AVKey#IMAGE_FORMAT}</td><td>ImageFormat</td><td>String</td></tr> <tr><td>{@link
     * AVKey#AVAILABLE_IMAGE_FORMATS}</td><td>AvailableImageFormats/ImageFormat</td><td>String array</td></tr>
     * <tr><td>{@link AVKey#FORCE_LEVEL_ZERO_LOADS}</td><td>ForceLevelZeroLoads</td><td>Boolean</td></tr>
     * <tr><td>{@link
     * AVKey#RETAIN_LEVEL_ZERO_TILES}</td><td>RetainLevelZeroTiles</td><td>Boolean</td></tr> <tr><td>{@link
     * AVKey#TEXTURE_FORMAT}</td><td>TextureFormat</td><td>String</td></tr> <tr><td>{@link
     * AVKey#USE_MIP_MAPS}</td><td>UseMipMaps</td><td>Boolean</td></tr> <tr><td>{@link
     * AVKey#USE_TRANSPARENT_TEXTURES}</td><td>UseTransparentTextures</td><td>Boolean</td></tr> <tr><td>{@link
     * AVKey#URL_CONNECT_TIMEOUT}</td><td>RetrievalTimeouts/ConnectTimeout/Time</td><td>Integer milliseconds</td></tr>
     * <tr><td>{@link AVKey#URL_READ_TIMEOUT}</td><td>RetrievalTimeouts/ReadTimeout/Time</td><td>Integer
     * milliseconds</td></tr> <tr><td>{@link AVKey#RETRIEVAL_QUEUE_STALE_REQUEST_LIMIT}</td>
     * <td>RetrievalTimeouts/StaleRequestLimit/Time</td><td>Integer milliseconds</td></tr> </table> This also writes
     * common layer and LevelSet configuration parameters by invoking {@link AbstractLayer#createLayerConfigElements(AVList,
     * Element)} and {@link DataConfigurationUtils#createLevelSetConfigElements(AVList, Element)}.
     *
     * @param params  the key-value pairs which define the TiledImageLayer configuration parameters.
     * @param context the XML document root on which to append TiledImageLayer configuration elements.
     * @return a reference to context.
     * @throws IllegalArgumentException if either the parameters or the context are null.
     */
    public static Element createTiledImageLayerConfigElements(AVList params, Element context) {

        XPath xpath = WWXML.makeXPath();

        // Common layer properties.
        AbstractLayer.createLayerConfigElements(params, context);

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
        WWXML.checkAndAppendTextElement(params, AVKey.TEXTURE_FORMAT, context, "TextureFormat");

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

        // Optional behavior properties.
        WWXML.checkAndAppendBooleanElement(params, AVKey.FORCE_LEVEL_ZERO_LOADS, context, "ForceLevelZeroLoads");
        WWXML.checkAndAppendBooleanElement(params, AVKey.RETAIN_LEVEL_ZERO_TILES, context, "RetainLevelZeroTiles");
        WWXML.checkAndAppendBooleanElement(params, AVKey.USE_MIP_MAPS, context, "UseMipMaps");
        WWXML.checkAndAppendBooleanElement(params, AVKey.USE_TRANSPARENT_TEXTURES, context, "UseTransparentTextures");
        WWXML.checkAndAppendDoubleElement(params, AVKey.DETAIL_HINT, context, "DetailHint");

        // Retrieval properties.
        if (params.get(AVKey.URL_CONNECT_TIMEOUT) != null ||
            params.get(AVKey.URL_READ_TIMEOUT) != null ||
            params.get(AVKey.RETRIEVAL_QUEUE_STALE_REQUEST_LIMIT) != null) {
            Element el = WWXML.getElement(context, "RetrievalTimeouts", xpath);
            if (el == null)
                el = WWXML.appendElementPath(context, "RetrievalTimeouts");

            WWXML.checkAndAppendTimeElement(params, AVKey.URL_CONNECT_TIMEOUT, el, "ConnectTimeout/Time");
            WWXML.checkAndAppendTimeElement(params, AVKey.URL_READ_TIMEOUT, el, "ReadTimeout/Time");
            WWXML.checkAndAppendTimeElement(params, AVKey.RETRIEVAL_QUEUE_STALE_REQUEST_LIMIT, el,
                "StaleRequestLimit/Time");
        }

        return context;
    }

    /**
     * Parses TiledImageLayer configuration parameters from the specified DOM document. This writes output as key-value
     * pairs to params. If a parameter from the XML document already exists in params, that parameter is ignored.
     * Supported key and parameter names are: <table> <caption style="font-weight: bold;">Supported Names</caption>
     * <tr><th>Parameter</th><th>Element Path</th><th>Type</th></tr>
     * <tr><td>{@link AVKey#SERVICE_NAME}</td><td>Service/@serviceName</td><td>String</td></tr> <tr><td>{@link
     * AVKey#IMAGE_FORMAT}</td><td>ImageFormat</td><td>String</td></tr> <tr><td>{@link
     * AVKey#AVAILABLE_IMAGE_FORMATS}</td><td>AvailableImageFormats/ImageFormat</td><td>String array</td></tr>
     * <tr><td>{@link AVKey#FORCE_LEVEL_ZERO_LOADS}</td><td>ForceLevelZeroLoads</td><td>Boolean</td></tr>
     * <tr><td>{@link
     * AVKey#RETAIN_LEVEL_ZERO_TILES}</td><td>RetainLevelZeroTiles</td><td>Boolean</td></tr> <tr><td>{@link
     * AVKey#TEXTURE_FORMAT}</td><td>TextureFormat</td><td>Boolean</td></tr> <tr><td>{@link
     * AVKey#USE_MIP_MAPS}</td><td>UseMipMaps</td><td>Boolean</td></tr> <tr><td>{@link
     * AVKey#USE_TRANSPARENT_TEXTURES}</td><td>UseTransparentTextures</td><td>Boolean</td></tr> <tr><td>{@link
     * AVKey#URL_CONNECT_TIMEOUT}</td><td>RetrievalTimeouts/ConnectTimeout/Time</td><td>Integer milliseconds</td></tr>
     * <tr><td>{@link AVKey#URL_READ_TIMEOUT}</td><td>RetrievalTimeouts/ReadTimeout/Time</td><td>Integer
     * milliseconds</td></tr> <tr><td>{@link AVKey#RETRIEVAL_QUEUE_STALE_REQUEST_LIMIT}</td>
     * <td>RetrievalTimeouts/StaleRequestLimit/Time</td><td>Integer milliseconds</td></tr> </table> This also parses
     * common layer and LevelSet configuration parameters by invoking {@link AbstractLayer#getLayerConfigParams(Element,
     * AVList)} and {@link DataConfigurationUtils#getLevelSetConfigParams(Element, AVList)}.
     *
     * @param domElement the XML document root to parse for TiledImageLayer configuration parameters.
     * @param params     the output key-value pairs which recieve the TiledImageLayer configuration parameters. A null
     *                   reference is permitted.
     * @return a reference to params, or a new AVList if params is null.
     * @throws IllegalArgumentException if the document is null.
     */
    public static AVList getTiledImageLayerConfigParams(Element domElement, AVList params) {

        if (params == null)
            params = new AVListImpl();

        XPath xpath = WWXML.makeXPath();

        // Common layer properties.
        AbstractLayer.getLayerConfigParams(domElement, params);

        // LevelSet properties.
        DataConfigurationUtils.getLevelSetConfigParams(domElement, params);

        // Service properties.
        WWXML.checkAndSetStringParam(domElement, params, AVKey.SERVICE_NAME, "Service/@serviceName", xpath);
        WWXML.checkAndSetBooleanParam(domElement, params, AVKey.RETRIEVE_PROPERTIES_FROM_SERVICE,
            "RetrievePropertiesFromService", xpath);

        // Image format properties.
        WWXML.checkAndSetStringParam(domElement, params, AVKey.IMAGE_FORMAT, "ImageFormat", xpath);
        WWXML.checkAndSetStringParam(domElement, params, AVKey.TEXTURE_FORMAT, "TextureFormat", xpath);
        WWXML.checkAndSetUniqueStringsParam(domElement, params, AVKey.AVAILABLE_IMAGE_FORMATS,
            "AvailableImageFormats/ImageFormat", xpath);

        // Optional behavior properties.
        WWXML.checkAndSetBooleanParam(domElement, params, AVKey.FORCE_LEVEL_ZERO_LOADS, "ForceLevelZeroLoads", xpath);
        WWXML.checkAndSetBooleanParam(domElement, params, AVKey.RETAIN_LEVEL_ZERO_TILES, "RetainLevelZeroTiles", xpath);
        WWXML.checkAndSetBooleanParam(domElement, params, AVKey.USE_MIP_MAPS, "UseMipMaps", xpath);
        WWXML.checkAndSetBooleanParam(domElement, params, AVKey.USE_TRANSPARENT_TEXTURES, "UseTransparentTextures",
            xpath);
        WWXML.checkAndSetDoubleParam(domElement, params, AVKey.DETAIL_HINT, "DetailHint", xpath);
        WWXML.checkAndSetColorArrayParam(domElement, params, AVKey.TRANSPARENCY_COLORS, "TransparencyColors/Color",
            xpath);

        // Retrieval properties. Convert the Long time values to Integers, because BasicTiledImageLayer is expecting
        // Integer values.
        WWXML.checkAndSetTimeParamAsInteger(domElement, params, AVKey.URL_CONNECT_TIMEOUT,
            "RetrievalTimeouts/ConnectTimeout/Time", xpath);
        WWXML.checkAndSetTimeParamAsInteger(domElement, params, AVKey.URL_READ_TIMEOUT,
            "RetrievalTimeouts/ReadTimeout/Time", xpath);
        WWXML.checkAndSetTimeParamAsInteger(domElement, params, AVKey.RETRIEVAL_QUEUE_STALE_REQUEST_LIMIT,
            "RetrievalTimeouts/StaleRequestLimit/Time", xpath);

        // Parse the legacy configuration parameters. This enables TiledImageLayer to recognize elements from previous
        // versions of configuration documents.
        TiledImageLayer.getLegacyTiledImageLayerConfigParams(domElement, params);

        return params;
    }

    /**
     * Parses TiledImageLayer configuration parameters from previous versions of configuration documents. This writes
     * output as key-value pairs to params. If a parameter from the XML document already exists in params, that
     * parameter is ignored. Supported key and parameter names are: <table> <caption style="font-weight:
     * bold;">Supported Names</caption>
     * <tr><th>Parameter</th><th>Element
     * Path</th><th>Type</th></tr> <tr><td>{@link AVKey#TEXTURE_FORMAT}</td><td>CompressTextures</td><td>"image/dds" if
     * CompressTextures is "true"; null otherwise</td></tr> </table>
     *
     * @param domElement the XML document root to parse for legacy TiledImageLayer configuration parameters.
     * @param params     the output key-value pairs which recieve the TiledImageLayer configuration parameters. A null
     *                   reference is permitted.
     * @return a reference to params, or a new AVList if params is null.
     * @throws IllegalArgumentException if the document is null.
     */
    protected static AVList getLegacyTiledImageLayerConfigParams(Element domElement, AVList params) {

        if (params == null)
            params = new AVListImpl();

        XPath xpath = WWXML.makeXPath();

        Object o = params.get(AVKey.TEXTURE_FORMAT);
        if (o == null) {
            Boolean b = WWXML.getBoolean(domElement, "CompressTextures", xpath);
            if (b != null && b)
                params.set(AVKey.TEXTURE_FORMAT, "image/dds");
        }

        return params;
    }

    protected static boolean isTileVisible(DrawContext dc, TextureTile tile) {
        final Frustum viewFrust = dc.getView().getFrustumInModelCoordinates();
        if (tile.getExtent(dc).intersects(viewFrust)) {
            final Sector visibleSector = dc.getVisibleSector();
            return (visibleSector == null || visibleSector.intersects(tile.sector));
        }
        return false;
    }

    protected static Vec4 computeReferencePoint(DrawContext dc) {
        final Globe globe = dc.getGlobe();
        if (dc.getViewportCenterPosition() != null)
            return globe.computePointFromPosition(dc.getViewportCenterPosition());

        final View view = dc.getView();
        Rectangle2D viewport = view.getViewport();
        int x = (int) viewport.getWidth() / 2;
        for (int y = (int) (0.5 * viewport.getHeight()); y >= 0; y--) {
            Position pos = view.computePositionFromScreenPoint(x, y);
            if (pos != null)
                return globe.computePointFromPosition(pos.getLatitude(), pos.getLongitude(), 0.0d);
        }

        return null;
    }

    protected static Vec4 getReferencePoint(DrawContext dc) {
        return TiledImageLayer.computeReferencePoint(dc);
    }

    protected static void drawTileIDs(DrawContext dc, Iterable<TextureTile> tiles) {
        Rectangle viewport = dc.getView().getViewport();
        TextRenderer textRenderer = OGLTextRenderer.getOrCreateTextRenderer(dc.getTextRendererCache(),
            Font.decode("Arial-Plain-13"));

        GL gl = dc.getGL();
        gl.glDisable(GL.GL_DEPTH_TEST);
        gl.glDisable(GL.GL_BLEND);
        gl.glDisable(GL.GL_TEXTURE_2D);

        textRenderer.beginRendering(viewport.width, viewport.height);
        textRenderer.setColor(Color.YELLOW);
        for (TextureTile tile : tiles) {
            String tileLabel = tile.getLabel();

            if (tile.getFallbackTile() != null)
                tileLabel += '/' + tile.getFallbackTile().getLabel();

            LatLon ll = tile.sector.getCentroid();
            Vec4 pt = dc.getGlobe().computePointFromPosition(ll.getLatitude(), ll.getLongitude(),
                dc.getGlobe().getElevation(ll.getLatitude(), ll.getLongitude()));
            pt = dc.getView().project(pt);
            textRenderer.draw(tileLabel, (int) pt.x, (int) pt.y);
        }
        textRenderer.setColor(Color.WHITE);
        textRenderer.endRendering();
    }

    abstract protected void requestTexture(DrawContext dc, TextureTile tile);

//    abstract protected void forceTextureLoad(TextureTile tile);

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

    @Override
    public void setName(String name) {
        super.setName(name);
        this.tileCountName = this.name() + " Tiles";
    }

    public boolean isRetainLevelZeroTiles() {
        return retainLevelZeroTiles;
    }

    public void setRetainLevelZeroTiles(boolean retainLevelZeroTiles) {
        this.retainLevelZeroTiles = retainLevelZeroTiles;
    }

    public boolean isDrawTileIDs() {
        return drawTileIDs;
    }

    public void setDrawTileIDs(boolean drawTileIDs) {
        this.drawTileIDs = drawTileIDs;
    }

    public boolean isDrawTileBoundaries() {
        return drawTileBoundaries;
    }

    public void setDrawTileBoundaries(boolean drawTileBoundaries) {
        this.drawTileBoundaries = drawTileBoundaries;
    }

    public boolean isDrawBoundingVolumes() {
        return drawBoundingVolumes;
    }

    public void setDrawBoundingVolumes(boolean drawBoundingVolumes) {
        this.drawBoundingVolumes = drawBoundingVolumes;
    }

    /**
     * Indicates the layer's detail hint, which is described in {@link #setDetailHint(double)}.
     *
     * @return the detail hint
     * @see #setDetailHint(double)
     */
    public double getDetailHint() {
        return this.detailHint;
    }

    /**
     * Modifies the default relationship of image resolution to screen resolution as the viewing altitude changes.
     * Values greater than 0 cause imagery to appear at higher resolution at greater altitudes than normal, but at an
     * increased performance cost. Values less than 0 decrease the default resolution at any given altitude. The default
     * value is 0. Values typically range between -0.5 and 0.5.
     * <p>
     * Note: The resolution-to-height relationship is defined by a scale factor that specifies the approximate size of
     * discernible lengths in the image relative to eye distance. The scale is specified as a power of 10. A value of 3,
     * for example, specifies that 1 meter on the surface should be distinguishable from an altitude of 10^3 meters
     * (1000 meters). The default scale is 1/10^2.8, (1 over 10 raised to the power 2.8). The detail hint specifies
     * deviations from that default. A detail hint of 0.2 specifies a scale of 1/1000, i.e., 1/10^(2.8 + .2) = 1/10^3.
     * Scales much larger than 3 typically cause the applied resolution to be higher than discernible for the altitude.
     * Such scales significantly decrease performance.
     *
     * @param detailHint the degree to modify the default relationship of image resolution to screen resolution with
     *                   changing view altitudes. Values greater than 1 increase the resolution. Values less than zero
     *                   decrease the resolution. The default value is 0.
     */
    public void setDetailHint(double detailHint) {
        this.detailHint = detailHint;
    }

    public LevelSet getLevels() {
        return levels;
    }

    @Override
    public boolean isMultiResolution() {
        return this.getLevels() != null && this.getLevels().getNumLevels() > 1;
    }

    @Override
    public boolean isAtMaxResolution() {
        return this.atMaxResolution;
    }

    /**
     * Returns the format used to store images in texture memory, or null if images are stored in their native format.
     *
     * @return the texture image format; null if images are stored in their native format.
     * @see #setTextureFormat(String)
     */
    public String getTextureFormat() {
        return this.textureFormat;
    }

    // ============== Tile Assembly ======================= //
    // ============== Tile Assembly ======================= //
    // ============== Tile Assembly ======================= //

    /**
     * Specifies the format used to store images in texture memory, or null to store images in their native format.
     * Supported texture formats are as follows: <ul> <li><code>image/dds</code> - Stores images in the compressed DDS
     * format. If the image is already in DDS format it's stored as-is.</li> </ul>
     *
     * @param textureFormat the texture image format; null to store images in their native format.
     */
    public void setTextureFormat(String textureFormat) {
        this.textureFormat = textureFormat;
    }

    public boolean isUseMipMaps() {
        return useMipMaps;
    }

    public void setUseMipMaps(boolean useMipMaps) {
        this.useMipMaps = useMipMaps;
    }

    public boolean isUseTransparentTextures() {
        return this.useTransparentTextures;
    }

    public void setUseTransparentTextures(boolean useTransparentTextures) {
        this.useTransparentTextures = useTransparentTextures;
    }

    /**
     * Specifies the time of the layer's most recent dataset update, beyond which cached data is invalid. If greater
     * than zero, the layer ignores and eliminates any in-memory or on-disk cached data older than the time specified,
     * and requests new information from the data source. If zero, the default, the layer applies any expiry times
     * associated with its individual levels, but only for on-disk cached data. In-memory cached data is expired only
     * when the expiry time is specified with this method and is greater than zero. This method also overwrites the
     * expiry times of the layer's individual levels if the value specified to the method is greater than zero.
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

    public List<TextureTile> getTopLevels() {
        if (this.topLevels == null)
            this.createTopLevelTiles();

        return topLevels;
    }

    protected void createTopLevelTiles() {
        Sector sector = this.levels.getSector();

        Level level = levels.getFirstLevel();
        double dLat = level.getTileDelta().latitude;
        double dLon = level.getTileDelta().longitude;
        double latOrigin = this.levels.getTileOrigin().latitude;
        double lonOrigin = this.levels.getTileOrigin().longitude;

        // Determine the row and column offset from the common WorldWind global tiling origin.
        int firstRow = Tile.computeRow(dLat, sector.latMin, latOrigin);
        int firstCol = Tile.computeColumn(dLon, sector.lonMin, lonOrigin);
        int lastRow = Tile.computeRow(dLat, sector.latMax, latOrigin);
        int lastCol = Tile.computeColumn(dLon, sector.lonMax, lonOrigin);

        int nLatTiles = lastRow - firstRow + 1;
        int nLonTiles = lastCol - firstCol + 1;

        this.topLevels = new ArrayList<>(nLatTiles * nLonTiles);

        final Angle LAT_ORIGIN = Angle.fromDegrees(latOrigin);
        final Angle LON_ORIGIN = Angle.fromDegrees(lonOrigin);
        final Angle DLON = Angle.fromDegrees(dLon);
        final Angle DLAT = Angle.fromDegrees(dLat);
        Angle p1 = Tile.computeRowLatitude(firstRow, Angle.fromDegrees(dLat), LAT_ORIGIN);
        for (int row = firstRow; row <= lastRow; row++) {
            Angle p2 = p1.add(DLAT);

            Angle t1 = Tile.computeColumnLongitude(firstCol, DLON, LON_ORIGIN);
            for (int col = firstCol; col <= lastCol; col++) {
                Angle t2;
                t2 = t1.add(DLON);

                this.topLevels.add(new TextureTile(new Sector(p1, p2, t1, t2), level, row, col));
                t1 = t2;
            }
            p1 = p2;
        }
    }

    protected void assembleTiles(DrawContext dc) {
        this.currentTiles.clear();

        for (TextureTile tile : this.getTopLevels()) {
            if (TiledImageLayer.isTileVisible(dc, tile)) {
                this.currentResourceTile = null;
                this.addTileOrDescendants(dc, tile);
            }
        }
    }

    protected void addTileOrDescendants(DrawContext dc, TextureTile tile) {
        if (this.meetsRenderCriteria(dc, tile)) {
            this.addTile(dc, tile);
            return;
        }

        // The incoming tile does not meet the rendering criteria, so it must be subdivided and those
        // subdivisions tested against the criteria.

        // All tiles that meet the selection criteria are drawn, but some of those tiles will not have
        // textures associated with them either because their texture isn't loaded yet or because they
        // are finer grain than the layer has textures for. In these cases the tiles use the texture of
        // the closest ancestor that has a texture loaded. This ancestor is called the currentResourceTile.
        // A texture transform is applied during rendering to align the sector's texture coordinates with the
        // appropriate region of the ancestor's texture.

        TextureTile ancestorResource = null;

        try {
            // TODO: Revise this to reflect that the parent layer is only requested while the algorithm continues
            // to search for the layer matching the criteria.
            // At this point the tile does not meet the render criteria but it may have its texture in memory.
            // If so, register this tile as the resource tile. If not, then this tile will be the next level
            // below a tile with texture in memory. So to provide progressive resolution increase, add this tile
            // to the draw list. That will cause the tile to be drawn using its parent tile's texture, and it will
            // cause it's texture to be requested. At some future call to this method the tile's texture will be in
            // memory, it will not meet the render criteria, but will serve as the parent to a tile that goes
            // through this same process as this method recurses. The result of all this is that a tile isn't rendered
            // with its own texture unless all its parents have their textures loaded. In addition to causing
            // progressive resolution increase, this ensures that the parents are available as the user zooms out, and
            // therefore the layer remains visible until the user is zoomed out to the point the layer is no longer
            // active.
            if (tile.isTextureInMemory(dc.getTextureCache()) || tile.getLevelNumber() == 0) {
                ancestorResource = this.currentResourceTile;
                this.currentResourceTile = tile;
            } else if (!tile.level.isEmpty()) {
//                this.addTile(dc, tile);
//                return;

                // Issue a request for the parent before descending to the children.
            }

            final Sector sector = this.getLevels().getSector();
            TextureTile[] subTiles = tile.createSubTiles(this.levels.getLevel(tile.getLevelNumber() + 1));
            for (TextureTile child : subTiles) {
                if (sector.intersects(child.sector) && TiledImageLayer.isTileVisible(dc, child))
                    this.addTileOrDescendants(dc, child);
            }
        }
        finally {
            if (ancestorResource != null) // Pop this tile as the currentResource ancestor
                this.currentResourceTile = ancestorResource;
        }
    }

    // ============== Rendering ======================= //
    // ============== Rendering ======================= //
    // ============== Rendering ======================= //

    protected void addTile(DrawContext dc, TextureTile tile) {
        //tile.setFallbackTile(null);

        final GpuResourceCache textureCache = dc.getTextureCache();
        if (tile.isTextureInMemory(textureCache)) {
            this.addTileToCurrent(tile);
            return;
        }

//        // Level 0 loads may be forced
//        if (tile.getLevelNumber() == 0 && this.forceLevelZeroLoads && !tile.isTextureInMemory(textureCache)) {
//            this.forceTextureLoad(tile);
//            if (tile.isTextureInMemory(textureCache)) {
//                this.addTileToCurrent(tile);
//                return;
//            }
//        }

        // Tile's texture isn't available, so request it
        if (tile.getLevelNumber() < this.levels.getNumLevels()) {
            // Request only tiles with data associated at this level
            if (!this.levels.missing(tile))
                this.requestTexture(dc, tile);
        }

        // Set up to use the currentResource tile's texture
        if (this.currentResourceTile != null) {

            if (this.currentResourceTile.isTextureInMemory(textureCache)) {
                tile.setFallbackTile(currentResourceTile);
                this.addTileToCurrent(tile);
            }
        }
    }

    protected void addTileToCurrent(TextureTile tile) {
        this.currentTiles.add(tile);
    }

    protected boolean meetsRenderCriteria(DrawContext dc, TextureTile tile) {
        return this.levels.isFinalLevel(tile.getLevelNumber()) || !needToSplit(dc, tile.sector, tile.level);
    }

    protected double getDetailFactor() {
        return TiledImageLayer.detailHintOrigin + this.getDetailHint();
    }

    protected boolean needToSplit(DrawContext dc, Sector sector, Level level) {
        // Compute the height in meters of a texel from the specified level. Take care to convert from the radians to
        // meters by multiplying by the globe's radius, not the length of a Cartesian point. Using the length of a
        // Cartesian point is incorrect when the globe is flat.
        double texelSizeRadians = level.getTexelSize();
        double texelSizeMeters = dc.getGlobe().getRadius() * texelSizeRadians;

        // Compute the level of detail scale and the field of view scale. These scales are multiplied by the eye
        // distance to derive a scaled distance that is then compared to the texel size. The level of detail scale is
        // specified as a power of 10. For example, a detail factor of 3 means split when the cell size becomes more
        // than one thousandth of the eye distance. The field of view scale is specified as a ratio between the current
        // field of view and a the default field of view. In a perspective projection, decreasing the field of view by
        // 50% has the same effect on object size as decreasing the distance between the eye and the object by 50%.
        // The detail hint is reduced for tiles above 75 degrees north and below 75 degrees south.
        double s = this.getDetailFactor();
        if (sector.latMin >= 75 || sector.latMax <= -75)
            s *= 0.9;
        double detailScale = Math.pow(10, -s);
        double fieldOfViewScale = dc.getView().getFieldOfView().tanHalfAngle() / Angle.fromDegrees(45).tanHalfAngle();
        fieldOfViewScale = WWMath.clamp(fieldOfViewScale, 0, 1);

        // Compute the distance between the eye point and the sector in meters, and compute a fraction of that distance
        // by multiplying the actual distance by the level of detail scale and the field of view scale.
        double eyeDistanceMeters = sector.distanceTo(dc, dc.getView().getEyePoint());
        double scaledEyeDistanceMeters = eyeDistanceMeters * detailScale * fieldOfViewScale;

        // Split when the texel size in meters becomes greater than the specified fraction of the eye distance, also in
        // meters. Another way to say it is, use the current tile if its texel size is less than the specified fraction
        // of the eye distance.
        //
        // NOTE: It's tempting to instead compare a screen pixel size to the texel size, but that calculation is
        // window-size dependent and results in selecting an excessive number of tiles when the window is large.
        return texelSizeMeters > scaledEyeDistanceMeters;
    }

    public Double getMinEffectiveAltitude(Double radius) {
        if (radius == null)
            radius = Earth.WGS84_EQUATORIAL_RADIUS;

        // Get the texel size in meters for the highest-resolution level.
        double texelSizeRadians = this.getLevels().getLastLevel().getTexelSize();
        double texelSizeMeters = radius * texelSizeRadians;

        // Compute altitude associated with the texel size at which it would switch if it had higher-res levels.
        return texelSizeMeters * Math.pow(10, this.getDetailFactor());
    }

    public Double getMaxEffectiveAltitude(Double radius) {
        if (radius == null)
            radius = Earth.WGS84_EQUATORIAL_RADIUS;

        // Find first non-empty level. Compute altitude at which it comes into effect.
        final int n = this.getLevels().getLastLevel().getLevelNumber();
        for (int i = 0; i < n; i++) {
            if (this.levels.isLevelEmpty(i))
                continue;

            // Compute altitude associated with the texel size at which it would switch if it had a lower-res level.
            // That texel size is twice that of the current lowest-res level.
            double texelSizeRadians = this.levels.getLevel(i).getTexelSize();
            double texelSizeMeters = 2 * radius * texelSizeRadians;

            return texelSizeMeters * Math.pow(10, this.getDetailFactor());
        }

        return null;
    }

    protected boolean atMaxLevel(DrawContext dc) {
        Position vpc = dc.getViewportCenterPosition();
        if (dc.getView() == null || this.getLevels() == null || vpc == null)
            return false;

        if (!this.getLevels().getSector().contains(vpc))
            return true;

        Level nextToLast = this.getLevels().getNextToLastLevel();
        if (nextToLast == null)
            return true;

        Sector centerSector = nextToLast.computeSectorForPosition(vpc.getLatitude(), vpc.getLongitude(),
            this.levels.getTileOrigin());

        return this.needToSplit(dc, centerSector, nextToLast);
    }

    @Override
    public void render(DrawContext dc) {
        this.atMaxResolution = this.atMaxLevel(dc);
        super.render(dc);
    }

    @Override
    protected final void doRender(DrawContext dc) {
        if (dc.getSurfaceGeometry() == null || dc.getSurfaceGeometry().size() < 1)
            return;

        dc.getGeographicSurfaceTileRenderer().setShowImageTileOutlines(this.isDrawTileBoundaries());

        draw(dc);
    }

    protected void draw(DrawContext dc) {
        this.assembleTiles(dc); // Determine the tiles to draw.

        if (this.currentTiles.size() >= 1) {
            // Indicate that this layer rendered something this frame.
            this.set(AVKey.FRAME_TIMESTAMP, dc.getFrameTimeStamp());

            if (this.getScreenCredit() != null) {
                dc.addScreenCredit(this.getScreenCredit());
            }

            TextureTile[] sortedTiles = new TextureTile[this.currentTiles.size()];
            sortedTiles = this.currentTiles.toArray(sortedTiles);
            Arrays.sort(sortedTiles, TiledImageLayer.levelComparator);

            GL2 gl = dc.getGL2(); // GL initialization checks for GL2 compatibility.

            if (this.isUseTransparentTextures() || this.getOpacity() < 1) {
                gl.glPushAttrib(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_POLYGON_BIT | GL2.GL_CURRENT_BIT);
                this.setBlendingFunction(dc);
            } else {
                gl.glPushAttrib(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_POLYGON_BIT);
            }

            gl.glPolygonMode(GL2.GL_FRONT, GL2.GL_FILL);
            gl.glEnable(GL.GL_CULL_FACE);
            gl.glCullFace(GL.GL_BACK);

            dc.setPerFrameStatistic(PerformanceStatistic.IMAGE_TILE_COUNT, this.tileCountName,
                this.currentTiles.size());
            dc.getGeographicSurfaceTileRenderer().renderTiles(dc, this.currentTiles);

            gl.glPopAttrib();

            if (this.drawTileIDs)
                TiledImageLayer.drawTileIDs(dc, this.currentTiles);

            if (this.drawBoundingVolumes)
                this.drawBoundingVolumes(dc, this.currentTiles);

            // Check texture expiration. Memory-cached textures are checked for expiration only when an explicit,
            // non-zero expiry time has been set for the layer. If none has been set, the expiry times of the layer's
            // individual levels are used, but only for images in the local file cache, not textures in memory. This is
            // to avoid incurring the overhead of checking expiration of in-memory textures, a very rarely used feature.
            if (this.getExpiryTime() > 0 && this.getExpiryTime() <= System.currentTimeMillis())
                this.checkTextureExpiration(dc, this.currentTiles);

            this.currentTiles.clear();
        }

        WorldWind.tasks().drain(requestQ);
    }

    //**************************************************************//
    //********************  Configuration  *************************//
    //**************************************************************//

    protected void checkTextureExpiration(DrawContext dc, Iterable<TextureTile> tiles) {
        for (TextureTile tile : tiles) {
            if (tile.isTextureExpired())
                this.requestTexture(dc, tile);
        }
    }

    protected void setBlendingFunction(DrawContext dc) {
        // Set up a premultiplied-alpha blending function. Any texture read by JOGL will have alpha-premultiplied color
        // components, as will any DDS file created by WorldWind or the WorldWind WMS. We'll also set up the base
        // color as a premultiplied color, so that any incoming premultiplied color will be properly combined with the
        // base color.

        GL2 gl = dc.getGL2(); // GL initialization checks for GL2 compatibility.

        double alpha = this.getOpacity();
        gl.glColor4d(alpha, alpha, alpha, alpha);
        gl.glEnable(GL.GL_BLEND);
        gl.glBlendFunc(GL.GL_ONE, GL.GL_ONE_MINUS_SRC_ALPHA);
    }

    public boolean isLayerInView(DrawContext dc) {

        final Sector visibleSector = dc.getVisibleSector();
        return !(visibleSector != null && !this.levels.getSector().intersects(visibleSector));
    }

    protected void drawBoundingVolumes(DrawContext dc, Iterable<TextureTile> tiles) {
        GL2 gl = dc.getGL2(); // GL initialization checks for GL2 compatibility.

        float[] previousColor = new float[4];
        gl.glGetFloatv(GL2.GL_CURRENT_COLOR, previousColor, 0);
        gl.glColor3d(0, 1, 0);

        for (TextureTile tile : tiles) {
            if (tile.getExtent(dc) instanceof Renderable)
                ((Renderable) tile.getExtent(dc)).render(dc);
        }

        Box c = Sector.computeBoundingBox(dc.getGlobe(), dc.getVerticalExaggeration(), this.levels.getSector());
        gl.glColor3d(1, 1, 0);
        c.render(dc);

        gl.glColor4fv(previousColor, 0);
    }

    // ============== Image Composition ======================= //
    // ============== Image Composition ======================= //
    // ============== Image Composition ======================= //

    public List<String> getAvailableImageFormats() {
        return new ArrayList<>(this.supportedImageFormats);
    }

    protected void setAvailableImageFormats(String[] formats) {
        this.supportedImageFormats.clear();

        if (formats != null)
            this.supportedImageFormats.addAll(Arrays.asList(formats));
    }

    public boolean isImageFormatAvailable(String imageFormat) {
        return imageFormat != null && this.supportedImageFormats.contains(imageFormat);
    }

    public String getDefaultImageFormat() {
        return this.supportedImageFormats.isEmpty() ? null : this.supportedImageFormats.get(0);
    }

    protected BufferedImage requestImage(TextureTile tile, String mimeType)
        throws IOException, MalformedURLException {

        InputStream in;
        String path = tile.getPathBase() + WWIO.mimeSuffix(mimeType);
        File f = new File(path);

        if (f.isAbsolute() && f.exists())
            in = f.toURI().toURL().openStream();
        else
            in = WWIO.stream(path);

        try {
//        if (in == null) // image is not local
//            return null;
//
//        if (WWIO.isFileOutOfDate(in, tile.level.getExpiryTime())) {
//            // The file has expired. Delete it.
//            this.levels.miss(tile);
//            this.getDataFileStore().removeFile(in);
//            String message = Logging.getMessage("generic.DataFileExpired", in);
//            Logging.logger().fine(message);
//        } else {
            BufferedImage image = ImageIO.read(in);
            if (image != null) {
                this.levels.has(tile);
                return image;
            } else {
                throw new IOException(Logging.getMessage("generic.ImageReadFailed", in));
            }

        } catch (Exception e) {
            this.levels.miss(tile);
            throw e;
        } finally {
            in.close();
        }

//            catch (InterruptedIOException e) {
//                this.levels.miss(tile);
//                throw e;
//            }
//            catch (IOException e) {
//                // Assume that something's wrong with the file and delete it.
//                this.getDataFileStore().removeFile(url);
//                this.levels.miss(tile);
//                Logging.logger().info(Logging.getMessage("generic.DeletedCorruptDataFile", url));
//            }
//        }

//        return null;
    }

    protected void downloadImage(final TextureTile tile, String mimeType, int timeout) throws Exception {
        if (this.get(AVKey.RETRIEVER_FACTORY_LOCAL) != null)
            this.retrieveLocalImage(tile, mimeType, timeout);
        else
            // Assume it's remote.
            this.retrieveRemoteImage(tile, mimeType, timeout);
    }

    protected void retrieveRemoteImage(final TextureTile tile, String mimeType, int timeout) throws Exception,
        MalformedURLException, RuntimeException {
        // TODO: apply retriever-factory pattern for remote retrieval case.
        final URL resourceURL = tile.getResourceURL(mimeType);
        if (resourceURL == null)
            return;

        Retriever retriever;

        String protocol = resourceURL.getProtocol();

        if ("http".equalsIgnoreCase(protocol) || "https".equalsIgnoreCase(protocol)) {
            retriever = new HTTPRetriever(resourceURL, new CompositionRetrievalPostProcessor(tile));
            retriever.set(URLRetriever.EXTRACT_ZIP_ENTRY, "true"); // supports legacy layers
        } else {
            String message = Logging.getMessage("layers.TextureLayer.UnknownRetrievalProtocol", resourceURL);
            throw new RuntimeException(message);
        }

        Logging.logger().log(java.util.logging.Level.FINE, "Retrieving " + resourceURL.toString());
        retriever.setConnectTimeout(10000);
        retriever.setReadTimeout(timeout);
        retriever.call();
    }

    protected void retrieveLocalImage(TextureTile tile, String mimeType, int timeout) throws Exception {
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
        avList.set(AVKey.IMAGE_FORMAT, mimeType);

        Retriever retriever = retrieverFactory.retriever(avList, new CompositionRetrievalPostProcessor(tile));

        Logging.logger().log(java.util.logging.Level.FINE, "Locally retrieving " + tile.getPath());
        retriever.setReadTimeout(timeout);
        retriever.call();
    }

    public int computeLevelForResolution(Sector sector, double resolution) {
//        if (sector == null) {
//            String message = Logging.getMessage("nullValue.SectorIsNull");
//            Logging.logger().severe(message);
//            throw new IllegalStateException(message);
//        }

        // Find the first level exceeding the desired resolution
        double texelSize;
        Level targetLevel = this.levels.getLastLevel();
        for (int i = 0; i < this.getLevels().getLastLevel().getLevelNumber(); i++) {
            if (this.levels.isLevelEmpty(i))
                continue;

            texelSize = this.levels.getLevel(i).getTexelSize();
            if (texelSize > resolution)
                continue;

            targetLevel = this.levels.getLevel(i);
            break;
        }

        // Choose the level closest to the resolution desired
        if (targetLevel.getLevelNumber() != 0 && !this.levels.isLevelEmpty(targetLevel.getLevelNumber() - 1)) {
            Level nextLowerLevel = this.levels.getLevel(targetLevel.getLevelNumber() - 1);
            double dless = Math.abs(nextLowerLevel.getTexelSize() - resolution);
            double dmore = Math.abs(targetLevel.getTexelSize() - resolution);
            if (dless < dmore)
                targetLevel = nextLowerLevel;
        }

        Logging.logger().fine(Logging.getMessage("layers.TiledImageLayer.LevelSelection",
            targetLevel.getLevelNumber(), Double.toString(targetLevel.getTexelSize())));
        return targetLevel.getLevelNumber();
    }

    /**
     * Create an image for the portion of this layer lying within a specified sector.The image is created at a specified
     * aspect ratio within a canvas of a specified size. This returns the specified image if this layer has no content
     * in the specified sector.
     *
     * @param sector       the sector of interest.
     * @param canvasWidth  the width of the canvas.
     * @param canvasHeight the height of the canvas.
     * @param aspectRatio  the aspect ratio, width/height, of the window. If the aspect ratio is greater or equal to
     *                     one, the full width of the canvas is used for the image; the height used is proportional to
     *                     the inverse of the aspect ratio. If the aspect ratio is less than one, the full height of the
     *                     canvas is used, and the width used is proportional to the aspect ratio.
     * @param levelNumber  the target level of the tiled image layer.
     * @param mimeType     the type of image to create, e.g., "png" and "jpg".
     * @param abortOnError indicates whether to stop assembling the image if an error occurs. If false, processing
     *                     continues until all portions of the layer that intersect the specified sector have been added
     *                     to the image. Portions for which an error occurs will be blank.
     * @param image        if non-null, a {@link BufferedImage} in which to place the image. If null, a new buffered
     *                     image is created. The image must be the width and height specified in the
     *                     <code>canvasWidth</code> and <code>canvasHeight</code> arguments.
     * @param timeout      The amount of time to allow for reading the image from the server.
     * @return image        the assembled image, of size indicated by the <code>canvasWidth</code> and
     * <code>canvasHeight</code>. If the specified aspect ratio is one, all pixels contain values. If the aspect ratio
     * is greater than one, a full-width segment along the top of the canvas is blank. If the aspect ratio is less than
     * one, a full-height segment along the right side of the canvase is blank. If the <code>image</code> argument was
     * non-null, that buffered image is returned.
     * @throws IllegalArgumentException if <code>sector</code> is null.
     * @throws Exception                Other errors.
     * @see ImageUtil#mergeImage(Sector, Sector, double, BufferedImage, BufferedImage)  ;
     */
    public BufferedImage composeImageForSector(Sector sector, int canvasWidth, int canvasHeight, double aspectRatio,
        int levelNumber, String mimeType, boolean abortOnError, BufferedImage image, int timeout) throws Exception {

        if (!this.levels.getSector().intersects(sector)) {
            Logging.logger().severe(Logging.getMessage("generic.SectorRequestedOutsideCoverageArea", sector,
                this.levels.getSector()));
            return image;
        }

        Sector intersection = this.levels.getSector().intersection(sector);

        if (levelNumber < 0) {
            levelNumber = this.levels.getLastLevel().getLevelNumber();
        } else if (levelNumber > this.levels.getLastLevel().getLevelNumber()) {
            Logging.logger().warning(Logging.getMessage("generic.LevelRequestedGreaterThanMaxLevel",
                levelNumber, this.levels.getLastLevel().getLevelNumber()));
            levelNumber = this.levels.getLastLevel().getLevelNumber();
        }

        int numTiles = 0;
        TextureTile[][] tiles = this.getTilesInSector(intersection, levelNumber);
        for (TextureTile[] row : tiles) {
            numTiles += row.length;
        }

        if (tiles.length == 0 || tiles[0].length == 0) {
            Logging.logger().severe(Logging.getMessage("layers.TiledImageLayer.NoImagesAvailable"));
            return image;
        }

        if (image == null)
            image = new BufferedImage(canvasWidth, canvasHeight, BufferedImage.TYPE_INT_RGB);

        double tileCount = 0;
        for (TextureTile[] row : tiles) {
            for (TextureTile tile : row) {
                if (tile == null)
                    continue;

                BufferedImage tileImage;
                try {
                    tileImage = this.getImage(tile, mimeType, timeout);
                    Thread.sleep(1); // generates InterruptedException if thread has been interupted

                    if (tileImage != null)
                        ImageUtil.mergeImage(sector, tile.sector, aspectRatio, tileImage, image);

                    this.firePropertyChange(AVKey.PROGRESS, tileCount / numTiles, ++tileCount / numTiles);
                }
                catch (InterruptedException | InterruptedIOException e) {
                    throw e;
                }
                catch (Exception e) {
                    if (abortOnError)
                        throw e;

                    String message = Logging.getMessage("generic.ExceptionWhileRequestingImage", tile.getPath());
                    Logging.logger().log(java.util.logging.Level.WARNING, message, e);
                }
            }
        }

        return image;
    }

    public long countImagesInSector(Sector sector) {
        long count = 0;
        for (int i = 0; i <= this.getLevels().getLastLevel().getLevelNumber(); i++) {
            if (!this.levels.isLevelEmpty(i))
                count += countImagesInSector(sector, i);
        }
        return count;
    }

    public long countImagesInSector(Sector sector, int levelNumber) {
        if (sector == null) {
            String msg = Logging.getMessage("nullValue.SectorIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        Level targetLevel = this.levels.getLastLevel();
        if (levelNumber >= 0) {
            for (int i = levelNumber; i < this.getLevels().getLastLevel().getLevelNumber(); i++) {
                if (this.levels.isLevelEmpty(i))
                    continue;

                targetLevel = this.levels.getLevel(i);
                break;
            }
        }

        // Collect all the tiles intersecting the input sector.
        LatLon delta = targetLevel.getTileDelta();
        LatLon origin = this.levels.getTileOrigin();
        final int nwRow = Tile.computeRow(delta.getLatitude(), sector.latMax(), origin.getLatitude());
        final int nwCol = Tile.computeColumn(delta.getLongitude(), sector.lonMin(), origin.getLongitude());
        final int seRow = Tile.computeRow(delta.getLatitude(), sector.latMin(), origin.getLatitude());
        final int seCol = Tile.computeColumn(delta.getLongitude(), sector.lonMax(), origin.getLongitude());

        long numRows = nwRow - seRow + 1;
        long numCols = seCol - nwCol + 1;

        return numRows * numCols;
    }

    public TextureTile[][] getTilesInSector(Sector sector, int levelNumber) {
        if (sector == null) {
            String msg = Logging.getMessage("nullValue.SectorIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        Level targetLevel = this.levels.getLastLevel();
        if (levelNumber >= 0) {
            for (int i = levelNumber; i < this.getLevels().getLastLevel().getLevelNumber(); i++) {
                if (this.levels.isLevelEmpty(i))
                    continue;

                targetLevel = this.levels.getLevel(i);
                break;
            }
        }

        // Collect all the tiles intersecting the input sector.
        LatLon delta = targetLevel.getTileDelta();
        LatLon origin = this.levels.getTileOrigin();
        final int nwRow = Tile.computeRow(delta.getLatitude(), sector.latMax(), origin.getLatitude());
        final int nwCol = Tile.computeColumn(delta.getLongitude(), sector.lonMin(), origin.getLongitude());
        final int seRow = Tile.computeRow(delta.getLatitude(), sector.latMin(), origin.getLatitude());
        final int seCol = Tile.computeColumn(delta.getLongitude(), sector.lonMax(), origin.getLongitude());

        int numRows = nwRow - seRow + 1;
        int numCols = seCol - nwCol + 1;
        TextureTile[][] sectorTiles = new TextureTile[numRows][numCols];

        for (int row = nwRow; row >= seRow; row--) {
            for (int col = nwCol; col <= seCol; col++) {
                TileKey key = new TileKey(targetLevel.getLevelNumber(), row, col, targetLevel.getCacheName());
                Sector tileSector = this.levels.computeSectorForKey(key);
                sectorTiles[nwRow - row][col - nwCol] = new TextureTile(tileSector, targetLevel, row, col);
            }
        }

        return sectorTiles;
    }

    protected BufferedImage getImage(TextureTile tile, String mimeType, int timeout) throws Exception,
        URISyntaxException, IOException, RuntimeException {
        // Read the image from disk.
        BufferedImage image = this.requestImage(tile, mimeType);
        //Thread.sleep(1); // generates InterruptedException if thread has been interrupted
        if (image != null)
            return image;

        // Retrieve it from the net since it's not on disk.
        this.downloadImage(tile, mimeType, timeout);

        // Try to read from disk again after retrieving it from the net.
        image = this.requestImage(tile, mimeType);
        //Thread.sleep(1); // generates InterruptedException if thread has been interupted
        if (image == null) {
            throw new RuntimeException(Logging.getMessage("layers.TiledImageLayer.ImageUnavailable", tile.getPath()));
        }

        return image;
    }

    protected class CompositionRetrievalPostProcessor extends AbstractRetrievalPostProcessor {
        protected final TextureTile tile;

        public CompositionRetrievalPostProcessor(TextureTile tile) {
            this.tile = tile;
        }

        protected File doGetOutputFile() {
            String suffix = WWIO.mimeSuffix(this.getRetriever().getContentType());

            String path = this.tile.getPathBase();
            path += suffix;

            File f = new File(path);

            return f.isAbsolute() ? f : getDataFileStore().newFile(path);
        }

        @Override
        protected boolean isDeleteOnExit(File outFile) {
            return outFile.getPath().contains(WWIO.DELETE_ON_EXIT_PREFIX);
        }

        @Override
        protected boolean overwriteExistingFile() {
            return true;
        }

        protected void markResourceAbsent() {
            TiledImageLayer.this.levels.miss(tile);
        }

        protected void handleUnsuccessfulRetrieval() {
            // Don't mark the tile as absent because the caller may want to try again.
        }
    }
}
