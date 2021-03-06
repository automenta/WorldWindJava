/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.layers.rpf;

import com.jogamp.opengl.util.texture.TextureData;
import gov.nasa.worldwind.*;
import gov.nasa.worldwind.avlist.*;
import gov.nasa.worldwind.cache.FileStore;
import gov.nasa.worldwind.formats.dds.DDSCompressor;
import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.layers.TiledImageLayer;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.retrieve.*;
import gov.nasa.worldwind.util.*;

import java.awt.image.*;
import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.*;

/**
 * @author dcollins
 * @version $Id: RPFTiledImageLayer.java 1171 2013-02-11 21:45:02Z dcollins $
 */
public class RPFTiledImageLayer extends TiledImageLayer {
    public static final String RPF_ROOT_PATH = "rpf.RootPath";
    public static final String RPF_DATA_SERIES_ID = "rpf.DataSeriesId";
    private final KV creationParams;
    private final RPFGenerator rpfGenerator;
    private final Object fileLock = new Object();

    public RPFTiledImageLayer(String stateInXml) {
        this(RPFTiledImageLayer.xmlStateToParams(stateInXml));

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

        Boolean b = rs.getStateValueAsBoolean("rpf.LayerEnabled");
        if (b != null)
            this.setEnabled(b);

        Double d = rs.getStateValueAsDouble("rpf.Opacity");
        if (d != null)
            this.setOpacity(d);

        d = rs.getStateValueAsDouble("rpf.MinActiveAltitude");
        if (d != null)
            this.setMinActiveAltitude(d);

        d = rs.getStateValueAsDouble("rpf.MaxActiveAltitude");
        if (d != null)
            this.setMaxActiveAltitude(d);

        String s = rs.getStateValueAsString("rpf.LayerName");
        if (s != null)
            this.setName(s);

        b = rs.getStateValueAsBoolean("rpf.UseMipMaps");
        if (b != null)
            this.setUseMipMaps(b);

        b = rs.getStateValueAsBoolean("rpf.UseTransparentTextures");
        if (b != null)
            this.setUseTransparentTextures(b);

        RestorableSupport.StateObject so = rs.getStateObject("avlist");
        if (so != null) {
            RestorableSupport.StateObject[] avpairs = rs.getAllStateObjects(so, "");
            if (avpairs != null) {
                for (RestorableSupport.StateObject avp : avpairs) {
                    if (avp != null)
                        this.set(avp.getName(), avp.getValue());
                }
            }
        }
    }

    public RPFTiledImageLayer(KV params) {
        super(new LevelSet(RPFTiledImageLayer.initParams(params)));

        this.initRPFFileIndex(params);
        this.creationParams = params.copy();
        this.rpfGenerator = new RPFGenerator(params);

        this.set(Keys.CONSTRUCTION_PARAMETERS, params);
        this.setUseTransparentTextures(true);
        this.setName(RPFTiledImageLayer.makeTitle(params));
    }

    static Collection<Tile> createTopLevelTiles(KV params) {
        if (params == null) {
            String message = Logging.getMessage("nullValue.LayerConfigParams");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        LevelSet levels = new LevelSet(RPFTiledImageLayer.initParams(params));
        Sector sector = levels.sector;

        Level level = levels.getFirstLevel();
        Angle dLat = level.getTileDelta().getLat();
        Angle dLon = level.getTileDelta().getLon();
        Angle latOrigin = levels.tileOrigin.getLat();
        Angle lonOrigin = levels.tileOrigin.getLon();

        // Determine the row and column offset from the common WorldWind global tiling origin.
        int firstRow = Tile.computeRow(dLat, sector.latMin(), latOrigin);
        int firstCol = Tile.computeColumn(dLon, sector.lonMin(), lonOrigin);
        int lastRow = Tile.computeRow(dLat, sector.latMax(), latOrigin);
        int lastCol = Tile.computeColumn(dLon, sector.lonMax(), lonOrigin);

        int nLatTiles = lastRow - firstRow + 1;
        int nLonTiles = lastCol - firstCol + 1;

        Collection<Tile> topLevels = new ArrayList<>(nLatTiles * nLonTiles);

        Angle p1 = Tile.rowLat(firstRow, dLat, latOrigin);
        for (int row = firstRow; row <= lastRow; row++) {
            Angle p2;
            p2 = p1.add(dLat);

            Angle t1 = Tile.columnLon(firstCol, dLon, lonOrigin);
            for (int col = firstCol; col <= lastCol; col++) {
                Angle t2;
                t2 = t1.add(dLon);

                topLevels.add(new Tile(new Sector(p1, p2, t1, t2), level, row, col));
                t1 = t2;
            }
            p1 = p2;
        }

        return topLevels;
    }

    static String getFileIndexCachePath(String rootPath, String dataSeriesId) {
        String path = null;
        if (rootPath != null && dataSeriesId != null) {
            path = WWIO.formPath(
                rootPath,
                dataSeriesId,
                "rpf_file_index.idx");
        }
        return path;
    }

    public static RPFTiledImageLayer fromRestorableState(String stateInXml) {
        if (stateInXml == null) {
            String message = Logging.getMessage("nullValue.StringIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        return new RPFTiledImageLayer(stateInXml);
    }

    private static KV initParams(KV params) {
        if (params == null) {
            String message = Logging.getMessage("nullValue.LayerConfigParams");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        String rootPath = params.getStringValue(RPFTiledImageLayer.RPF_ROOT_PATH);
        if (rootPath == null) {
            String message = Logging.getMessage("nullValue.RPFRootPath");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        String dataSeriesId = params.getStringValue(RPFTiledImageLayer.RPF_DATA_SERIES_ID);
        if (dataSeriesId == null) {
            String message = Logging.getMessage("nullValue.RPFDataSeriesIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        // Use a dummy value for service.
        if (params.get(Keys.SERVICE) == null)
            params.set(Keys.SERVICE, "file://" + RPFGenerator.class.getName() + '?');

        // Use a dummy value for dataset-name.
        if (params.get(Keys.DATASET_NAME) == null)
            params.set(Keys.DATASET_NAME, dataSeriesId);

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
            params.set(Keys.NUM_LEVELS, 14); // approximately 0.5 meters per pixel
        if (params.get(Keys.NUM_EMPTY_LEVELS) == null)
            params.set(Keys.NUM_EMPTY_LEVELS, 0);

        params.set(Keys.TILE_URL_BUILDER, new URLBuilder());

        // RPFTiledImageLayer is typically constructed either by the {@link RPFTiledImageProcessor}, or from restorable
        // state XML. In the first case, either the sector parameter or the RPFFileIndex parameter are specified by the
        // processor. In the latter case, the sector is restored as part of the state xml.
        Sector sector = (Sector) params.get(Keys.SECTOR);
        if (sector == null) {
            RPFFileIndex fileIndex = (RPFFileIndex) params.get(RPFGenerator.RPF_FILE_INDEX);
            if (fileIndex != null && fileIndex.getIndexProperties() != null)
                sector = fileIndex.getIndexProperties().getBoundingSector();

            if (sector == null) {
                String message = Logging.getMessage("RPFTiledImageLayer.NoGeographicBoundingBox");
                Logging.logger().severe(message);
                throw new IllegalArgumentException(message);
            }

            params.set(Keys.SECTOR, sector);
        }

        if (params.get(Keys.DATA_CACHE_NAME) == null) {
            String cacheName = WWIO.formPath(rootPath, dataSeriesId);
            params.set(Keys.DATA_CACHE_NAME, cacheName);
        }

        return params;
    }

    private static RPFFileIndex initFileIndex(File file) {
        ByteBuffer buffer;
        try {
            buffer = WWIO.mapFile(file);
        }
        catch (Exception e) {
            String message = "Exception while attempting to map file: " + file;
            Logging.logger().log(java.util.logging.Level.SEVERE, message, e);
            buffer = null;
        }

        RPFFileIndex fileIndex = null;
        try {
            if (buffer != null) {
                fileIndex = new RPFFileIndex();
                fileIndex.load(buffer);
            }
        }
        catch (Exception e) {
            String message = "Exception while attempting to load RPFFileIndex: " + file;
            Logging.logger().log(java.util.logging.Level.SEVERE, message, e);
            fileIndex = null;
        }

        return fileIndex;
    }

    private static String makeTitle(KV params) {
        StringBuilder sb = new StringBuilder();

        Object o = params.get(RPFGenerator.RPF_FILE_INDEX);
        if (o instanceof RPFFileIndex) {
            RPFFileIndex fileIndex = (RPFFileIndex) o;
            if (fileIndex.getIndexProperties() != null) {
                if (fileIndex.getIndexProperties().getDescription() != null)
                    sb.append(fileIndex.getIndexProperties().getDescription());
                else
                    sb.append(fileIndex.getIndexProperties().getDataSeriesIdentifier());
            }
        }

        if (sb.isEmpty()) {
            String rootPath = params.getStringValue(RPFTiledImageLayer.RPF_ROOT_PATH);
            String dataSeriesId = params.getStringValue(RPFTiledImageLayer.RPF_DATA_SERIES_ID);
            if (rootPath != null && dataSeriesId != null) {
                sb.append(rootPath).append(':').append(dataSeriesId);
            }
        }

        return sb.toString();
    }

    public static KV xmlStateToParams(String stateInXml) {
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

        String s = rs.getStateValueAsString(RPFTiledImageLayer.RPF_ROOT_PATH);
        if (s != null)
            params.set(RPFTiledImageLayer.RPF_ROOT_PATH, s);

        s = rs.getStateValueAsString(RPFTiledImageLayer.RPF_DATA_SERIES_ID);
        if (s != null)
            params.set(RPFTiledImageLayer.RPF_DATA_SERIES_ID, s);

        s = rs.getStateValueAsString(Keys.IMAGE_FORMAT);
        if (s != null)
            params.set(Keys.IMAGE_FORMAT, s);

        s = rs.getStateValueAsString(Keys.DATA_CACHE_NAME);
        if (s != null)
            params.set(Keys.DATA_CACHE_NAME, s);

        s = rs.getStateValueAsString(Keys.SERVICE);
        if (s != null)
            params.set(Keys.SERVICE, s);

        s = rs.getStateValueAsString(Keys.TITLE);
        if (s != null)
            params.set(Keys.TITLE, s);

        s = rs.getStateValueAsString(Keys.DISPLAY_NAME);
        if (s != null)
            params.set(Keys.DISPLAY_NAME, s);

        RestorableSupport.adjustTitleAndDisplayName(params);

        s = rs.getStateValueAsString(Keys.DATASET_NAME);
        if (s != null)
            params.set(Keys.DATASET_NAME, s);

        s = rs.getStateValueAsString(Keys.FORMAT_SUFFIX);
        if (s != null)
            params.set(Keys.FORMAT_SUFFIX, s);

        s = rs.getStateValueAsString(Keys.LAYER_NAMES);
        if (s != null)
            params.set(Keys.LAYER_NAMES, s);

        s = rs.getStateValueAsString(Keys.STYLE_NAMES);
        if (s != null)
            params.set(Keys.STYLE_NAMES, s);

        Integer i = rs.getStateValueAsInteger(Keys.NUM_EMPTY_LEVELS);
        if (i != null)
            params.set(Keys.NUM_EMPTY_LEVELS, i);

        i = rs.getStateValueAsInteger(Keys.NUM_LEVELS);
        if (i != null)
            params.set(Keys.NUM_LEVELS, i);

        i = rs.getStateValueAsInteger(Keys.TILE_WIDTH);
        if (i != null)
            params.set(Keys.TILE_WIDTH, i);

        i = rs.getStateValueAsInteger(Keys.TILE_HEIGHT);
        if (i != null)
            params.set(Keys.TILE_HEIGHT, i);

        Double lat = rs.getStateValueAsDouble(Keys.LEVEL_ZERO_TILE_DELTA + ".Latitude");
        Double lon = rs.getStateValueAsDouble(Keys.LEVEL_ZERO_TILE_DELTA + ".Longitude");
        if (lat != null && lon != null)
            params.set(Keys.LEVEL_ZERO_TILE_DELTA, LatLon.fromDegrees(lat, lon));

        Double minLat = rs.getStateValueAsDouble(Keys.SECTOR + ".MinLatitude");
        Double minLon = rs.getStateValueAsDouble(Keys.SECTOR + ".MinLongitude");
        Double maxLat = rs.getStateValueAsDouble(Keys.SECTOR + ".MaxLatitude");
        Double maxLon = rs.getStateValueAsDouble(Keys.SECTOR + ".MaxLongitude");
        if (minLat != null && minLon != null && maxLat != null && maxLon != null)
            params.set(Keys.SECTOR, Sector.fromDegrees(minLat, maxLat, minLon, maxLon));

        params.set(Keys.TILE_URL_BUILDER, new URLBuilder());

        return params;
    }

    private static TextureData readTexture(URL url, boolean useMipMaps) {
        try {
            return OGLUtil.newTextureData(url, useMipMaps, JOGLVersionInfo.getMaxCompatibleGLProfile());
        }
        catch (Exception e) {
            String msg = Logging.getMessage("layers.TextureLayer.ExceptionAttemptingToReadTextureFile", url.toString());
            Logging.logger().log(java.util.logging.Level.SEVERE, msg, e);
            return null;
        }
    }

    private static ByteBuffer createImage(RPFGenerator.RPFServiceInstance service, URL url)
        throws IOException {
        ByteBuffer buffer = null;
        BufferedImage bufferedImage = service.serviceRequest(url);
        if (bufferedImage != null) {
            buffer = DDSCompressor.compressImage(bufferedImage);
        }

        return buffer;
    }

    private static void addTileToCache(TextureTile tile) {
        TextureTile.getMemoryCache().add(tile.key, tile);
    }

    protected void initRPFFileIndex(KV params) {
        // Load the RPFFileIndex associated with this RPFTiledImageLayer, and update the layer's expiry time according
        // to the last modified time on the RPFFileIndex.

        FileStore fileStore = Configuration.data;

        // Root path and data series ID parameters should have already been validated in initParams().
        String rootPath = params.getStringValue(RPFTiledImageLayer.RPF_ROOT_PATH);
        String dataSeriesId = params.getStringValue(RPFTiledImageLayer.RPF_DATA_SERIES_ID);
        File file = fileStore.newFile(RPFTiledImageLayer.getFileIndexCachePath(rootPath, dataSeriesId));

        RPFFileIndex fileIndex = (RPFFileIndex) params.get(RPFGenerator.RPF_FILE_INDEX);
        if (fileIndex == null) {
            fileIndex = RPFTiledImageLayer.initFileIndex(file);
            if (fileIndex == null) {
                String message = Logging.getMessage("nullValue.RPFFileIndexIsNull");
                Logging.logger().severe(message);
                throw new IllegalArgumentException(message);
            }
            params.set(RPFGenerator.RPF_FILE_INDEX, fileIndex);
        }

        // Default to expiring data on the date the DDS converter was updated. If the RPFFileIndex's last-modified time
        // is newer than the default expiry time, then use newer of the two. This ensures that layer imagery always
        // reflects whats in the RPFFileIndex. If the layer has been re-imported (data has been added, or data has been
        // removed), then all previously created layer imagery will be expired (but not necessarily the preprocessed
        // data).
        long expiryTime = new GregorianCalendar(2009, Calendar.FEBRUARY, 25).getTimeInMillis();
        if (file != null && file.lastModified() > expiryTime) {
            expiryTime = file.lastModified();
        }
        this.setExpiryTime(expiryTime);
    }

    protected void checkResources() {
        // Intentionally left blank.
    }

    private RestorableSupport makeRestorableState(KV params) {
        RestorableSupport rs = RestorableSupport.newRestorableSupport();
        // Creating a new RestorableSupport failed. RestorableSupport logged the problem, so just return null.

        for (Map.Entry<String, Object> p : params.getEntries()) {
            if (p.getValue() instanceof LatLon) {
                rs.addStateValueAsDouble(p.getKey() + ".Latitude", ((LatLon) p.getValue()).getLat().degrees);
                rs.addStateValueAsDouble(p.getKey() + ".Longitude", ((LatLon) p.getValue()).getLon().degrees);
            } else if (p.getValue() instanceof Sector) {
                rs.addStateValueAsDouble(p.getKey() + ".MinLatitude", ((Sector) p.getValue()).latMin);
                rs.addStateValueAsDouble(p.getKey() + ".MaxLatitude", ((Sector) p.getValue()).latMax);
                rs.addStateValueAsDouble(p.getKey() + ".MinLongitude",
                    ((Sector) p.getValue()).lonMin);
                rs.addStateValueAsDouble(p.getKey() + ".MaxLongitude",
                    ((Sector) p.getValue()).lonMax);
            } else if (p.getValue() instanceof URLBuilder) {
                // Intentionally left blank. URLBuilder will be created from scratch in fromRestorableState().
            } else if (p.getKey().equals(RPFGenerator.RPF_FILE_INDEX)) {
                // Intentionally left blank.
            } else {
                super.getRestorableStateForAVPair(p.getKey(), p.getValue(), rs, null);
            }
        }

        rs.addStateValueAsBoolean("rpf.LayerEnabled", this.isEnabled());
        rs.addStateValueAsDouble("rpf.Opacity", this.getOpacity());
        rs.addStateValueAsDouble("rpf.MinActiveAltitude", this.getMinActiveAltitude());
        rs.addStateValueAsDouble("rpf.MaxActiveAltitude", this.getMaxActiveAltitude());
        rs.addStateValueAsString("rpf.LayerName", this.name());
        rs.addStateValueAsBoolean("rpf.UseMipMaps", this.isUseMipMaps());
        rs.addStateValueAsBoolean("rpf.UseTransparentTextures", this.isUseTransparentTextures());

        RestorableSupport.StateObject so = rs.addStateObject("avlist");
        for (Map.Entry<String, Object> p : this.getEntries()) {
            if (p.getKey().equals(Keys.CONSTRUCTION_PARAMETERS))
                continue;

            super.getRestorableStateForAVPair(p.getKey(), p.getValue(), rs, so);
        }

        return rs;
    }

    public String getRestorableState() {
        return this.makeRestorableState(this.creationParams).getStateAsXml();
    }

    public void restoreState(String stateInXml) {
        String message = Logging.getMessage("RestorableSupport.RestoreRequiresConstructor");
        Logging.logger().severe(message);
        throw new UnsupportedOperationException(message);
    }

    protected void requestTexture(DrawContext dc, TextureTile tile) {
        Vec4 centroid = tile.getCentroidPoint(dc.getGlobe());
        Vec4 referencePoint = TiledImageLayer.getReferencePoint(dc);
        if (referencePoint != null)
            tile.setPriorityDistance(centroid.distanceTo3(referencePoint));

        requestQ.add(new RequestTask(tile, this));
    }

    private boolean loadTexture(TextureTile tile, URL textureURL) {
        if (WWIO.isFileOutOfDate(textureURL, tile.level.getExpiryTime())) {
            // The file has expired. Delete it then request download of newer.
            Configuration.data.removeFile(textureURL);
            String message = Logging.getMessage("generic.DataFileExpired", textureURL);
            Logging.logger().fine(message);
            return false;
        }

        TextureData textureData;

        synchronized (this.fileLock) {
            textureData = RPFTiledImageLayer.readTexture(textureURL, this.isUseMipMaps());
        }

        if (textureData == null)
            return false;

        tile.setTextureData(textureData);
        if (tile.getLevelNumber() != 0 || !this.isRetainLevelZeroTiles())
            RPFTiledImageLayer.addTileToCache(tile);

        return true;
    }

    protected void downloadTexture(final TextureTile tile) {
        RPFGenerator.RPFServiceInstance service = this.rpfGenerator.getServiceInstance();
        if (service == null)
            return;

        URL url;
        try {
            url = tile.getResourceURL();
        }
        catch (MalformedURLException e) {
            Logging.logger().log(java.util.logging.Level.SEVERE,
                Logging.getMessage("layers.TextureLayer.ExceptionCreatingTextureUrl", tile), e);
            return;
        }

        if (WorldWind.retrieveRemote().isAvailable()) {
            Retriever retriever = new RPFRetriever(service, url, new DownloadPostProcessor(tile, this));
            // Apply any overridden timeouts.
            Integer srl = KVMap.getIntegerValue(this, Keys.RETRIEVAL_QUEUE_STALE_REQUEST_LIMIT);
            if (srl != null && srl > 0)
                retriever.setStaleRequestLimit(srl);
            WorldWind.retrieveRemote().run(retriever, tile.getPriority());
        } else {
            requestQ.add(new DownloadTask(service, url, tile, this));
        }
    }

    private void saveBuffer(ByteBuffer buffer, File outFile) throws IOException {
        synchronized (this.fileLock) // sychronized with read of file in RequestTask.run()
        {
            WWIO.saveBuffer(buffer, outFile);
        }
    }

    private static class URLBuilder implements TileUrlBuilder {
        public String URLTemplate;

        private URLBuilder() {
        }

        public URL getURL(Tile tile, String imageFormat) throws MalformedURLException {
            StringBuffer sb;
            if (this.URLTemplate == null) {
                sb = new StringBuffer(tile.level.getService());
                sb.append("dataset=");
                sb.append(tile.level.getDataset());
                sb.append("&width=");
                sb.append(tile.level.getTileWidth());
                sb.append("&height=");
                sb.append(tile.level.getTileHeight());

                this.URLTemplate = sb.toString();
            } else {
                sb = new StringBuffer(this.URLTemplate);
            }

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

    private static class RequestTask extends TileTask {
        private final RPFTiledImageLayer layer;

        private RequestTask(TextureTile tile, RPFTiledImageLayer layer) {
            super(tile);
            this.layer = layer;
        }

        public void run() {
            final TextureTile tile = getTile();

            // TODO: check to ensure load is still needed

            final URL textureURL = Configuration.data.findFile(tile.getPath(), false);
            if (textureURL != null) {
                if (this.layer.loadTexture(tile, textureURL)) {
                    layer.levels.has(tile);
                    this.layer.emit(Keys.LAYER, null, this);
                    return;
                } else {
                    // Assume that something's wrong with the file and delete it.
                    Configuration.data.removeFile(textureURL);
                    layer.levels.miss(tile);
                    String message = Logging.getMessage("generic.DeletedCorruptDataFile", textureURL);
                    Logging.logger().info(message);
                }
            }

            this.layer.downloadTexture(tile);
        }
    }

    private static class DownloadPostProcessor extends AbstractRetrievalPostProcessor {
        private final TextureTile tile;
        private final RPFTiledImageLayer layer;

        public DownloadPostProcessor(TextureTile tile, RPFTiledImageLayer layer) {
            this.tile = tile;
            this.layer = layer;
        }

        @Override
        protected void markResourceAbsent() {
            this.layer.levels.miss(this.tile);
        }

        @Override
        protected ByteBuffer handleSuccessfulRetrieval() {
            ByteBuffer buffer = super.handleSuccessfulRetrieval();

            if (buffer != null) {
                // Fire a property change to denote that the layer's backing data has changed.
                this.layer.emit(Keys.LAYER, null, this);
            }

            return buffer;
        }

        @Override
        protected boolean validateResponseCode() {
            if (this.getRetriever() instanceof RPFRetriever)
                return ((RPFRetriever) this.getRetriever()).getResponseCode() == RPFRetriever.RESPONSE_CODE_OK;
            else
                return super.validateResponseCode();
        }

        @Override
        protected ByteBuffer handleTextContent() throws IOException {
            this.markResourceAbsent();

            return super.handleTextContent();
        }
    }

    private static class DownloadTask extends TileTask {
        private final RPFGenerator.RPFServiceInstance service;
        private final URL url;
        private final RPFTiledImageLayer layer;

        private DownloadTask(RPFGenerator.RPFServiceInstance service, URL url, TextureTile tile,
            RPFTiledImageLayer layer) {
            super(tile);
            this.service = service;
            this.url = url;
            this.layer = layer;
        }

        public void run() {
            final TextureTile tile = getTile();
            try {
                ByteBuffer buffer = RPFTiledImageLayer.createImage(this.service, this.url);
                if (buffer != null) {
                    final File outFile = Configuration.data.newFile(tile.getPath());
                    if (outFile != null) {
                        this.layer.saveBuffer(buffer, outFile);
                    }
                }
            }
            catch (Exception e) {
                Logging.logger().log(
                    java.util.logging.Level.SEVERE, "layers.TextureLayer.ExceptionAttemptingToCreateTileImage", e);
                this.layer.levels.miss(tile);
            }
        }
    }

    private static class TileTask implements Runnable, Comparable<TileTask> {
        private final TextureTile tile;

        private TileTask(TextureTile tile) {
            this.tile = tile;
        }

        public final TextureTile getTile() {
            return this.tile;
        }

        public void run() {
        }

        /**
         * @param that the task to compare
         * @return -1 if <code>this</code> less than <code>that</code>, 1 if greater than, 0 if equal
         * @throws IllegalArgumentException if <code>that</code> is null
         */
        @Override
        public int compareTo(TileTask that) {
            if (this == that)
                return 0;
            return Double.compare(that.tile.getPriority(), this.tile.getPriority());
        }

        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;

            // Don't include layer in comparison so that requests are shared among layers
            return Objects.equals(tile, ((TileTask) o).tile);
        }

        public int hashCode() {
            return (tile != null ? tile.hashCode() : 0);
        }

        public String toString() {
            return this.tile.getPath();
        }
    }
}