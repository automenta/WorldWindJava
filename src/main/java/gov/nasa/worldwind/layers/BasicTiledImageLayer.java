/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.layers;

import com.jogamp.opengl.util.texture.TextureData;
import gov.nasa.worldwind.*;
import gov.nasa.worldwind.avlist.*;
import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.layers.ogc.wms.WMSCapabilities;
import gov.nasa.worldwind.render.*;
import gov.nasa.worldwind.retrieve.*;
import gov.nasa.worldwind.util.*;
import org.w3c.dom.*;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.function.Function;
import java.util.logging.Level;

/**
 * @author tag
 * @version $Id: BasicTiledImageLayer.java 2684 2015-01-26 18:31:22Z tgaskins $
 */
public class BasicTiledImageLayer extends TiledImageLayer implements BulkRetrievable {
    // Layer resource properties.
    protected static final int RESOURCE_ID_OGC_CAPABILITIES = 1;
    protected final Object fileLock = new Object();

    public BasicTiledImageLayer(LevelSet levelSet) {
        super(levelSet);
    }

    public BasicTiledImageLayer(KV params) {
        this(new LevelSet(params));

        String s = params.getStringValue(Keys.DISPLAY_NAME);
        if (s != null)
            this.setName(s);

        String[] strings = (String[]) params.get(Keys.AVAILABLE_IMAGE_FORMATS);
        if (strings != null && strings.length > 0)
            this.setAvailableImageFormats(strings);

        s = params.getStringValue(Keys.TEXTURE_FORMAT);
        if (s != null)
            this.setTextureFormat(s);

        Double d = (Double) params.get(Keys.OPACITY);
        if (d != null)
            this.setOpacity(d);

        d = (Double) params.get(Keys.MAX_ACTIVE_ALTITUDE);
        if (d != null)
            this.setMaxActiveAltitude(d);

        d = (Double) params.get(Keys.MIN_ACTIVE_ALTITUDE);
        if (d != null)
            this.setMinActiveAltitude(d);

        d = (Double) params.get(Keys.MAP_SCALE);
        if (d != null)
            this.set(Keys.MAP_SCALE, d);

        d = (Double) params.get(Keys.DETAIL_HINT);
        if (d != null)
            this.setDetailHint(d);

        Boolean b = (Boolean) params.get(Keys.RETAIN_LEVEL_ZERO_TILES);
        if (b != null)
            this.setRetainLevelZeroTiles(b);

        b = (Boolean) params.get(Keys.NETWORK_RETRIEVAL_ENABLED);
        if (b != null)
            this.setNetworkRetrievalEnabled(b);

        b = (Boolean) params.get(Keys.USE_MIP_MAPS);
        if (b != null)
            this.setUseMipMaps(b);

        b = (Boolean) params.get(Keys.USE_TRANSPARENT_TEXTURES);
        if (b != null)
            this.setUseTransparentTextures(b);

        Object o = params.get(Keys.URL_CONNECT_TIMEOUT);
        if (o != null)
            this.set(Keys.URL_CONNECT_TIMEOUT, o);

        o = params.get(Keys.URL_READ_TIMEOUT);
        if (o != null)
            this.set(Keys.URL_READ_TIMEOUT, o);

        o = params.get(Keys.RETRIEVAL_QUEUE_STALE_REQUEST_LIMIT);
        if (o != null)
            this.set(Keys.RETRIEVAL_QUEUE_STALE_REQUEST_LIMIT, o);

        ScreenCredit sc = (ScreenCredit) params.get(Keys.SCREEN_CREDIT);
        if (sc != null)
            this.setScreenCredit(sc);

        if (params.get(Keys.TRANSPARENCY_COLORS) != null)
            this.set(Keys.TRANSPARENCY_COLORS, params.get(Keys.TRANSPARENCY_COLORS));

        b = (Boolean) params.get(Keys.DELETE_CACHE_ON_EXIT);
        if (b != null)
            this.set(Keys.DELETE_CACHE_ON_EXIT, true);

        this.set(Keys.CONSTRUCTION_PARAMETERS, params.copy());

        // If any resources should be retrieved for this Layer, start a task to retrieve those resources, and initialize
        // this Layer once those resources are retrieved.
        if (this.isRetrieveResources()) {
            this.startResourceRetrieval();
        }
    }

    public BasicTiledImageLayer(Document dom, KV params) {
        this(dom.getDocumentElement(), params);
    }

    public BasicTiledImageLayer(Element domElement, KV params) {
        this(BasicTiledImageLayer.getParamsFromDocument(domElement, params));
    }

    public BasicTiledImageLayer(String restorableStateInXml) {
        this(BasicTiledImageLayer.restorableStateToParams(restorableStateInXml));

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

    protected static KV getParamsFromDocument(Element domElement, KV params) {
        if (domElement == null) {
            String message = Logging.getMessage("nullValue.DocumentIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (params == null)
            params = new KVMap();

        TiledImageLayer.getTiledImageLayerConfigParams(domElement, params);
        BasicTiledImageLayer.setFallbacks(params);

        return params;
    }

    protected static void setFallbacks(KV params) {
        if (params.get(Keys.LEVEL_ZERO_TILE_DELTA) == null) {
            Angle delta = new Angle(36);
            params.set(Keys.LEVEL_ZERO_TILE_DELTA, new LatLon(delta, delta));
        }

        if (params.get(Keys.TILE_WIDTH) == null)
            params.set(Keys.TILE_WIDTH, 512);

        if (params.get(Keys.TILE_HEIGHT) == null)
            params.set(Keys.TILE_HEIGHT, 512);

        if (params.get(Keys.FORMAT_SUFFIX) == null)
            params.set(Keys.FORMAT_SUFFIX, ".dds");

        if (params.get(Keys.NUM_LEVELS) == null)
            params.set(Keys.NUM_LEVELS, 19); // approximately 0.1 meters per pixel

        if (params.get(Keys.NUM_EMPTY_LEVELS) == null)
            params.set(Keys.NUM_EMPTY_LEVELS, 0);
    }

    protected static KV restorableStateToParams(String stateInXml) {
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

        KV params = new KVMap();
        BasicTiledImageLayer.restoreStateForParams(rs, null, params);
        return params;
    }

    protected static void restoreStateForParams(RestorableSupport rs, RestorableSupport.StateObject context,
        KV params) {
        String s = rs.getStateValueAsString(context, Keys.DATA_CACHE_NAME);
        if (s != null)
            params.set(Keys.DATA_CACHE_NAME, s);

        s = rs.getStateValueAsString(context, Keys.SERVICE);
        if (s != null)
            params.set(Keys.SERVICE, s);

        s = rs.getStateValueAsString(context, Keys.DATASET_NAME);
        if (s != null)
            params.set(Keys.DATASET_NAME, s);

        s = rs.getStateValueAsString(context, Keys.FORMAT_SUFFIX);
        if (s != null)
            params.set(Keys.FORMAT_SUFFIX, s);

        Integer i = rs.getStateValueAsInteger(context, Keys.NUM_EMPTY_LEVELS);
        if (i != null)
            params.set(Keys.NUM_EMPTY_LEVELS, i);

        i = rs.getStateValueAsInteger(context, Keys.NUM_LEVELS);
        if (i != null)
            params.set(Keys.NUM_LEVELS, i);

        i = rs.getStateValueAsInteger(context, Keys.TILE_WIDTH);
        if (i != null)
            params.set(Keys.TILE_WIDTH, i);

        i = rs.getStateValueAsInteger(context, Keys.TILE_HEIGHT);
        if (i != null)
            params.set(Keys.TILE_HEIGHT, i);

        Long lo = rs.getStateValueAsLong(context, Keys.EXPIRY_TIME);
        if (lo != null)
            params.set(Keys.EXPIRY_TIME, lo);

        LatLon ll = rs.getStateValueAsLatLon(context, Keys.LEVEL_ZERO_TILE_DELTA);
        if (ll != null)
            params.set(Keys.LEVEL_ZERO_TILE_DELTA, ll);

        ll = rs.getStateValueAsLatLon(context, Keys.TILE_ORIGIN);
        if (ll != null)
            params.set(Keys.TILE_ORIGIN, ll);

        Sector sector = rs.getStateValueAsSector(context, Keys.SECTOR);
        if (sector != null)
            params.set(Keys.SECTOR, sector);
    }

    protected void requestTexture(DrawContext dc, TextureTile tile) {
        Vec4 centroid = tile.getCentroidPoint(dc.getGlobe());
        Vec4 referencePoint = TiledImageLayer.getReferencePoint(dc);
        if (referencePoint != null)
            tile.setPriorityDistance(centroid.distanceTo3(referencePoint));

        var r = new RequestTask(tile, this);
        requestQ.add(r);
    }

    // *** Bulk download ***
    // *** Bulk download ***
    // *** Bulk download ***

//    @Nullable
//    protected TextureData loadTexture(URL url) {
//        TextureData textureData;
//
//        synchronized (this.fileLock) {
//            TextureData result;
//            boolean useMipMaps1 = this.isUseMipMaps();
//            try {
//                // If the caller has enabled texture compression, and the texture data is not a DDS file, then use read the
//                // texture data and convert it to DDS.
//                if ("image/dds".equalsIgnoreCase(this.getTextureFormat()) && !url.toString().toLowerCase().endsWith("dds")) {
//                    // Configure a DDS compressor to generate mipmaps based according to the 'useMipMaps' parameter, and
//                    // convert the image URL to a compressed DDS format.
//                    DXTCompressionAttributes attributes = DDSCompressor.getDefaultCompressionAttributes();
//                    attributes.setBuildMipmaps(useMipMaps1);
//                    ByteBuffer buffer = DDSCompressor.compressImageURL(url, attributes);
//
//                    result = OGLUtil.newTextureData(Configuration.getMaxCompatibleGLProfile(),
//                        WWIO.getInputStreamFromByteBuffer(buffer), useMipMaps1);
//                } else {
//                    // If the caller has disabled texture compression, or if the texture data is already a DDS file, then read
//                    // the texture data without converting it.
//                    result = OGLUtil.newTextureData(url, useMipMaps1, Configuration.getMaxCompatibleGLProfile());
//                }
//            }
//            catch (Exception e) {
//                String msg = Logging.getMessage("layers.TextureLayer.ExceptionAttemptingToReadTextureFile", url);
//                Logging.logger().log(Level.SEVERE, msg, e);
//                result = null;
//            }
//            textureData = result;
//        }
//        return textureData;
//    }

//    /**
//     * Get the estimated size in bytes of the imagery not in the WorldWind file cache for the given sector and
//     * resolution.
//     * <p>
//     * Note that the target resolution must be provided in radians of latitude per texel, which is the resolution in
//     * meters divided by the globe radius.
//     *
//     * @param sector     the sector to estimate.
//     * @param resolution the target resolution, provided in radians of latitude per texel.
//     * @return the estimated size in bytes of the missing imagery.
//     * @throws IllegalArgumentException if the sector is null or the resolution is less than zero.
//     */
//    public long getEstimatedMissingDataSize(Sector sector, double resolution) {
//        return this.getEstimatedMissingDataSize(sector, resolution, null);
//    }

    // *** Tile download ***
    // *** Tile download ***
    // *** Tile download ***

    protected void retrieveTexture(TextureTile tile, DownloadPostProcessor postProcessor) {
        if (this.get(Keys.RETRIEVER_FACTORY_LOCAL) != null)
            this.retrieveLocalTexture(tile, postProcessor);
        else
            this.retrieveRemoteTexture(tile, postProcessor);
    }

    protected void retrieveLocalTexture(TextureTile tile, Function<Retriever, ByteBuffer> postProcessor) {
        if (!WorldWind.retrieveLocal().isAvailable())
            return;

        RetrieverFactory retrieverFactory = (RetrieverFactory) this.get(Keys.RETRIEVER_FACTORY_LOCAL);
        if (retrieverFactory == null)
            return;

        KV avList = new KVMap();
        avList.set(Keys.SECTOR, tile.sector);
        avList.set(Keys.WIDTH, tile.getWidth());
        avList.set(Keys.HEIGHT, tile.getHeight());
        avList.set(Keys.FILE_NAME, tile.getPath());

        Retriever retriever = retrieverFactory.retriever(avList, postProcessor);

        WorldWind.retrieveLocal().run(retriever, tile.getPriority());
    }
    protected void retrieveRemoteTexture(TextureTile tile, DownloadPostProcessor postProcessor) {
        if (!this.isNetworkRetrievalEnabled()) {
            levels.miss(tile);
            return;
        }

        if (!WorldWind.retrieveRemote().isAvailable())
            return;

        URL url;
        try {
            url = tile.getResourceURL();

            if (WorldWind.getNetworkStatus().isHostUnavailable(url)) {
                levels.miss(tile);
                return;
            }
        }
        catch (MalformedURLException e) {
            Logging.logger().log(Level.SEVERE,
                Logging.getMessage("layers.TextureLayer.ExceptionCreatingTextureUrl", tile), e);
            return;
        }

        Retriever retriever;

        if (postProcessor == null)
            postProcessor = this.createDownloadPostProcessor(tile);
        retriever = URLRetriever.createRetriever(url, postProcessor);
        retriever.set(URLRetriever.EXTRACT_ZIP_ENTRY, "true"); // supports legacy layers

        // Apply any overridden timeouts.
        Integer cto = KVMap.getIntegerValue(this, Keys.URL_CONNECT_TIMEOUT);
        if (cto != null && cto > 0)
            retriever.setConnectTimeout(cto);
        Integer cro = KVMap.getIntegerValue(this, Keys.URL_READ_TIMEOUT);
        if (cro != null && cro > 0)
            retriever.setReadTimeout(cro);
        Integer srl = KVMap.getIntegerValue(this, Keys.RETRIEVAL_QUEUE_STALE_REQUEST_LIMIT);
        if (srl != null && srl > 0)
            retriever.setStaleRequestLimit(srl);

        WorldWind.retrieveRemote().run(retriever, tile.getPriority());
    }

    protected DownloadPostProcessor createDownloadPostProcessor(TextureTile tile) {
        return new DownloadPostProcessor(tile);
    }

    //**************************************************************//
    //********************  Non-Tile Resource Retrieval  ***********//
    //**************************************************************//

    /**
     * Retrieves any non-tile resources associated with this Layer, either online or in the local filesystem, and
     * initializes properties of this Layer using those resources. This returns a key indicating the retrieval state:
     * {@link Keys#RETRIEVAL_STATE_SUCCESSFUL} indicates the retrieval succeeded, {@link Keys#RETRIEVAL_STATE_ERROR}
     * indicates the retrieval failed with errors, and
     * <code>null</code> indicates the retrieval state is unknown. This method may invoke blocking I/O operations, and
     * therefore should not be executed from the rendering thread.
     *
     * @return {@link Keys#RETRIEVAL_STATE_SUCCESSFUL} if the retrieval succeeded, {@link Keys#RETRIEVAL_STATE_ERROR}
     * if the retrieval failed with errors, and <code>null</code> if the retrieval state is unknown.
     */
    protected String retrieveResources() {
        // This Layer has no construction parameters, so there is no description of what to retrieve. Return a key
        // indicating the resources have been successfully retrieved, though there is nothing to retrieve.
        KV params = (KV) this.get(Keys.CONSTRUCTION_PARAMETERS);
//        if (params == null) {
//            String message = Logging.getMessage("nullValue.ConstructionParametersIsNull");
//            Logging.logger().warning(message);
//            return AVKey.RETRIEVAL_STATE_SUCCESSFUL;
//        }

        // This Layer has no OGC Capabilities URL in its construction parameters. Return a key indicating the resources
        // have been successfully retrieved, though there is nothing to retrieve.
        URL url = DataConfigurationUtils.getOGCGetCapabilitiesURL(params);
//        if (url == null) {
//            String message = Logging.getMessage("nullValue.CapabilitiesURLIsNull");
//            Logging.logger().warning(message);
//            return AVKey.RETRIEVAL_STATE_SUCCESSFUL;
//        }

        // Get the service's OGC Capabilities resource from the session cache, or initiate a retrieval to fetch it.
        // SessionCacheUtils.getOrRetrieveSessionCapabilities() returns null if it initiated a
        // retrieval, or if the OGC Capabilities URL is unavailable.
        //
        // Note that we use the URL's String representation as the cache key. We cannot use the URL itself, because
        // the cache invokes the methods Object.hashCode() and Object.equals() on the cache key. URL's implementations
        // of hashCode() and equals() perform blocking IO calls. WorldWind does not perform blocking calls during
        // rendering, and this method is likely to be called from the rendering thread.
        WMSCapabilities caps;
        if (this.isNetworkRetrievalEnabled())
            caps = SessionCacheUtils.getOrRetrieveSessionCapabilities(url, WorldWind.getSessionCache(),
                url.toString(), null, BasicTiledImageLayer.RESOURCE_ID_OGC_CAPABILITIES, null, null);
        else
            caps = SessionCacheUtils.getSessionCapabilities(WorldWind.getSessionCache(), url.toString(),
                url.toString());

        // The OGC Capabilities resource retrieval is either currently running in another thread, or has failed. In
        // either case, return null indicating that that the retrieval was not successful, and we should try again
        // later. 
        if (caps == null)
            return null;

        // We have successfully retrieved this Layer's OGC Capabilities resource. Initialize this Layer using the
        // Capabilities document, and return a key indicating the retrieval has succeeded.
        this.initFromOGCCapabilitiesResource(caps, params);

        return Keys.RETRIEVAL_STATE_SUCCESSFUL;
    }

    /**
     * Initializes this Layer's expiry time property from the specified WMS Capabilities document and parameter list
     * describing the WMS layer names associated with this Layer. This method is thread safe; it synchronizes changes to
     * this Layer by wrapping the appropriate method calls in {@link SwingUtilities#invokeLater(Runnable)}.
     *
     * @param caps   the WMS Capabilities document retrieved from this Layer's WMS server.
     * @param params the parameter list describing the WMS layer names associated with this Layer.
     * @throws IllegalArgumentException if either the Capabilities or the parameter list is null.
     */
    protected void initFromOGCCapabilitiesResource(WMSCapabilities caps, KV params) {

        String[] names = DataConfigurationUtils.getOGCLayerNames(params);
        if (names == null || names.length == 0)
            return;

        final Long expiryTime = caps.getLayerLatestLastUpdateTime(names);
        if (expiryTime == null)
            return;

        // Synchronize changes to this Layer with the Event Dispatch Thread.
        //SwingUtilities.invokeLater(() -> {
        BasicTiledImageLayer.this.setExpiryTime(expiryTime);
        BasicTiledImageLayer.this.emit(Keys.LAYER, null, BasicTiledImageLayer.this);
        //});
    }

    /**
     * Returns a boolean value indicating if this Layer should retrieve any non-tile resources, either online or in the
     * local filesystem, and initialize itself using those resources.
     *
     * @return <code>true</code> if this Layer should retrieve any non-tile resources, and <code>false</code> otherwise.
     */
    protected boolean isRetrieveResources() {
        KV params = (KV) this.get(Keys.CONSTRUCTION_PARAMETERS);
        if (params == null)
            return false;

        Boolean b = (Boolean) params.get(Keys.RETRIEVE_PROPERTIES_FROM_SERVICE);
        return b != null && b;
    }

    /**
     * Starts retrieving non-tile resources associated with this Layer in a non-rendering thread.
     */
    protected void startResourceRetrieval() {
        Thread t = new Thread(this::retrieveResources);
        t.setName("Capabilities retrieval for " + this.name());
        t.start();
    }

    protected Document createConfigurationDocument(KV params) {
        return TiledImageLayer.createTiledImageLayerConfigDocument(params);
    }

    //**************************************************************//
    //********************  Restorable Support  ********************//
    //**************************************************************//

    public String getRestorableState() {
        // We only create a restorable state XML if this elevation model was constructed with an AVList.
        KV constructionParams = (KV) this.get(Keys.CONSTRUCTION_PARAMETERS);
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

    protected void doGetRestorableState(RestorableSupport rs, RestorableSupport.StateObject context) {
        KV constructionParams = (KV) this.get(Keys.CONSTRUCTION_PARAMETERS);
        if (constructionParams != null) {
            for (Map.Entry<String, Object> avp : constructionParams.getEntries()) {
                this.getRestorableStateForAVPair(avp.getKey(), avp.getValue(), rs, context);
            }
        }

        rs.addStateValueAsBoolean(context, "Layer.Enabled", this.isEnabled());
        rs.addStateValueAsDouble(context, "Layer.Opacity", this.getOpacity());
        rs.addStateValueAsDouble(context, "Layer.MinActiveAltitude", this.getMinActiveAltitude());
        rs.addStateValueAsDouble(context, "Layer.MaxActiveAltitude", this.getMaxActiveAltitude());
        rs.addStateValueAsBoolean(context, "Layer.NetworkRetrievalEnabled", this.isNetworkRetrievalEnabled());
        rs.addStateValueAsString(context, "Layer.Name", this.name());
        rs.addStateValueAsBoolean(context, "TiledImageLayer.UseMipMaps", this.isUseMipMaps());
        rs.addStateValueAsBoolean(context, "TiledImageLayer.UseTransparentTextures", this.isUseTransparentTextures());

        RestorableSupport.StateObject so = rs.addStateObject(context, "avlist");
        for (Map.Entry<String, Object> avp : this.getEntries()) {
            this.getRestorableStateForAVPair(avp.getKey(), avp.getValue(), rs, so);
        }
    }

    public void getRestorableStateForAVPair(String key, Object value,
        RestorableSupport rs, RestorableSupport.StateObject context) {
        if (value == null)
            return;

        if (key.equals(Keys.CONSTRUCTION_PARAMETERS))
            return;

        if (key.equals(Keys.FRAME_TIMESTAMP))
            return; // frame timestamp is a runtime property and must not be saved/restored

        if (value instanceof LatLon) {
            rs.addStateValueAsLatLon(context, key, (LatLon) value);
        } else if (value instanceof Sector) {
            rs.addStateValueAsSector(context, key, (Sector) value);
        } else if (value instanceof Color) {
            rs.addStateValueAsColor(context, key, (Color) value);
        } else {
            super.getRestorableStateForAVPair(key, value, rs, context);
        }
    }

    protected void doRestoreState(RestorableSupport rs, RestorableSupport.StateObject context) {
        Boolean b = rs.getStateValueAsBoolean(context, "Layer.Enabled");
        if (b != null)
            this.setEnabled(b);

        Double d = rs.getStateValueAsDouble(context, "Layer.Opacity");
        if (d != null)
            this.setOpacity(d);

        d = rs.getStateValueAsDouble(context, "Layer.MinActiveAltitude");
        if (d != null)
            this.setMinActiveAltitude(d);

        d = rs.getStateValueAsDouble(context, "Layer.MaxActiveAltitude");
        if (d != null)
            this.setMaxActiveAltitude(d);

        b = rs.getStateValueAsBoolean(context, "Layer.NetworkRetrievalEnabled");
        if (b != null)
            this.setNetworkRetrievalEnabled(b);

        String s = rs.getStateValueAsString(context, "Layer.Name");
        if (s != null)
            this.setName(s);

        b = rs.getStateValueAsBoolean(context, "TiledImageLayer.UseMipMaps");
        if (b != null)
            this.setUseMipMaps(b);

        b = rs.getStateValueAsBoolean(context, "TiledImageLayer.UseTransparentTextures");
        if (b != null)
            this.setUseTransparentTextures(b);

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

        if (so.getName().equals(Keys.FRAME_TIMESTAMP))
            return; // frame timestamp is a runtime property and must not be saved/restored

        this.set(so.getName(), so.getValue());
    }

    protected static class RequestTask implements Runnable, Comparable<RequestTask> {
        protected final BasicTiledImageLayer layer;
        protected final TextureTile tile;

        protected RequestTask(TextureTile tile, BasicTiledImageLayer layer) {
            this.layer = layer;
            this.tile = tile;
            assert(Double.isFinite(tile.getPriority()));
        }

        public void run() {

            if (tile.getTextureData()!=null)
                return; //got already

//            final FileStore store = this.layer.getDataFileStore();

            //TODO this ends up caching the texture twice in 2 different places: OK HTTP and the old file storage.  fix this

            this.layer.retrieveTexture(this.tile, this.layer.createDownloadPostProcessor(this.tile));
        }



        /**
         * @param that the task to compare
         * @return -1 if <code>this</code> less than <code>that</code>, 1 if greater than, 0 if equal
         * @throws IllegalArgumentException if <code>that</code> is null
         */
        @Override
        public int compareTo(RequestTask that) {
            if (this == that)
                return 0;
            return Double.compare(this.tile.getPriority(), that.tile.getPriority());
        }

        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;

            final RequestTask that = (RequestTask) o;

            // Don't include layer in comparison so that requests are shared among layers
            return Objects.equals(tile, that.tile);
        }

        public int hashCode() {
            return (tile != null ? tile.hashCode() : 0);
        }

        public String toString() {
            return this.tile.toString();
        }
    }

    private final class DownloadPostProcessor extends AbstractRetrievalPostProcessor {
        protected final TextureTile tile;

        DownloadPostProcessor(TextureTile tile) {
            //noinspection RedundantCast
            super((KV) BasicTiledImageLayer.this);

            this.tile = tile;
        }

        @Override
        protected void markResourceAbsent() {
            levels.miss(this.tile);
        }

        @Override
        protected ByteBuffer handleSuccessfulRetrieval() {
            ByteBuffer buffer = super.handleSuccessfulRetrieval();

            if (buffer != null) {
                // We've successfully cached data. Check if there's a configuration file for this layer, create one if there's not.
//                writeConfigurationFile(this.getFileStore());

                // Fire a property change to denote that the layer's backing data has changed.
                emit(Keys.LAYER, null, this);

                loadTexture(buffer.array());
            }

            return buffer;
        }

        private boolean loadTexture(byte[] textureBytes) {
            TextureData td;
            try {
                td = OGLUtil.newTextureData(textureBytes, isUseMipMaps(), Configuration.getMaxCompatibleGLProfile());
            }
            catch (IOException e) {
                e.printStackTrace();
                return false;
            }
            if (td!=null) {
                //if (this.layer.loadTexture(tile, textureURL)) {
                tile.setTextureData(td);

                if (tile.getLevelNumber() != 0 || !isRetainLevelZeroTiles())
                    TextureTile.cache.add(tile.key, tile);

                levels.has(tile);
                emit(Keys.LAYER, null, this);
                return true;
            } else {
                // Assume that something is wrong with the file and delete it.
                levels.miss(tile);
                return false;
            }

        }
        @Override
        protected ByteBuffer handleTextContent() throws IOException {
            this.markResourceAbsent();

            return super.handleTextContent();
        }
    }
}