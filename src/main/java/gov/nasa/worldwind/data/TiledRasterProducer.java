/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.data;

import gov.nasa.worldwind.*;
import gov.nasa.worldwind.avlist.*;
import gov.nasa.worldwind.cache.*;
import gov.nasa.worldwind.exception.WWRuntimeException;
import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.util.*;
import org.w3c.dom.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * @author dcollins
 * @version $Id: TiledRasterProducer.java 3043 2015-04-22 20:56:26Z tgaskins $
 */
public abstract class TiledRasterProducer extends AbstractDataStoreProducer {
    private static final long DEFAULT_TILED_RASTER_PRODUCER_CACHE_SIZE = 300000000L; // ~300 megabytes
    private static final int DEFAULT_TILED_RASTER_PRODUCER_LARGE_DATASET_THRESHOLD = 3000; // 3000 pixels
    private static final int DEFAULT_WRITE_THREAD_POOL_SIZE = 2;
    private static final int DEFAULT_TILE_WIDTH_AND_HEIGHT = 512;
    private static final int DEFAULT_SINGLE_LEVEL_TILE_WIDTH_AND_HEIGHT = 512;
    private static final double DEFAULT_LEVEL_ZERO_TILE_DELTA = 36.0d;

    // List of source data rasters.
    private final Collection<DataRaster> dataRasterList = new ArrayList<>();
    // Data raster caching.
    private final MemoryCache rasterCache;
    // Concurrent processing helper objects.
    private final ExecutorService tileWriteService;
    private final Semaphore tileWriteSemaphore;
    private final Object fileLock = new Object();
    // Progress counters.
    private int tile;
    private int tileCount;

    private DataRasterReaderFactory readerFactory;

    public TiledRasterProducer(MemoryCache cache, int writeThreadPoolSize) {
//        if (cache == null) {
//            String message = Logging.getMessage("nullValue.CacheIsNull");
//            Logging.logger().severe(message);
//            throw new IllegalArgumentException(message);
//        }
        if (writeThreadPoolSize < 1) {
            String message = Logging.getMessage("generic.ArgumentOutOfRange", "writeThreadPoolSize < 1");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.rasterCache = cache;
        this.tileWriteService = this.createDefaultTileWriteService(writeThreadPoolSize);
        this.tileWriteSemaphore = new Semaphore(writeThreadPoolSize, true);

        try {
            readerFactory = (DataRasterReaderFactory) WorldWind.createConfigurationComponent(
                AVKey.DATA_RASTER_READER_FACTORY_CLASS_NAME);
        }
        catch (Exception e) {
            readerFactory = new BasicDataRasterReaderFactory();
        }
    }

    public TiledRasterProducer() {
        this(createDefaultCache(), DEFAULT_WRITE_THREAD_POOL_SIZE);
    }

    protected static MemoryCache createDefaultCache() {
        long cacheSize = Configuration.getLongValue(AVKey.TILED_RASTER_PRODUCER_CACHE_SIZE,
            DEFAULT_TILED_RASTER_PRODUCER_CACHE_SIZE);
        return new BasicMemoryCache((long) (0.8 * cacheSize), cacheSize);
    }

    public Iterable<DataRaster> getDataRasters() {
        return this.dataRasterList;
    }

    // TODO: this describes the file types the producer will read. Make that more clear in the method name.

    protected DataRasterReaderFactory getReaderFactory() {
        return this.readerFactory;
    }

    public String getDataSourceDescription() {
        DataRasterReader[] readers = this.getDataRasterReaders();
        if (readers == null || readers.length < 1)
            return "";

        // Collect all the unique format suffixes available in all readers. If a reader does not publish any
        // format suffixes, then collect it's description.
        Collection<String> suffixSet = new TreeSet<>();
        Collection<String> descriptionSet = new TreeSet<>();
        for (DataRasterReader reader : readers) {
            String description = reader.getDescription();
            String[] names = reader.getSuffixes();

            if (names != null && names.length > 0)
                suffixSet.addAll(Arrays.asList(names));
            else
                descriptionSet.add(description);
        }

        // Create a string representation of the format suffixes (or description if no suffixes are available) for
        // all readers.
        StringBuilder sb = new StringBuilder();
        for (String suffix : suffixSet) {
            if (!sb.isEmpty())
                sb.append(", ");
            sb.append("*.").append(suffix);
        }
        for (String description : descriptionSet) {
            if (!sb.isEmpty())
                sb.append(", ");
            sb.append(description);
        }
        return sb.toString();
    }

    public void removeProductionState() {
        File installLocation = TiledRasterProducer.installLocationFor(this.getStoreParameters());

        if (installLocation == null || !installLocation.exists()) {
            String message = Logging.getMessage("TiledRasterProducer.NoInstallLocation",
                this.getStoreParameters().get(AVKey.DATASET_NAME));
            Logging.logger().warning(message);
            return;
        }

        try {
            WWIO.deleteDirectory(installLocation);
        }
        catch (Exception e) {
            String message = Logging.getMessage("TiledRasterProducer.ExceptionRemovingProductionState",
                this.getStoreParameters().get(AVKey.DATASET_NAME));
            Logging.logger().log(java.util.logging.Level.SEVERE, message, e);
        }
    }

    protected abstract DataRaster createDataRaster(int width, int height, Sector sector, AVList params);

    protected abstract DataRasterReader[] getDataRasterReaders();

    protected abstract DataRasterWriter[] getDataRasterWriters();

    protected MemoryCache getCache() {
        return this.rasterCache;
    }

    protected ExecutorService getTileWriteService() {
        return this.tileWriteService;
    }

    protected Semaphore getTileWriteSemaphore() {
        return this.tileWriteSemaphore;
    }

    protected void doStartProduction(AVList parameters) throws Exception {
        // Copy production parameters to prevent changes to caller's reference.
        this.productionParams = parameters.copy();
        this.initProductionParameters(this.productionParams);

        // Assemble the source data rasters.
        this.assembleDataRasters();

        // Initialize the level set parameters, and create the level set.
        this.initLevelSetParameters(this.productionParams);
        LevelSet levelSet = new LevelSet(this.productionParams);
        // Install the each tiles of the LevelSet.
        this.installLevelSet(levelSet, this.productionParams);

        // Wait for concurrent tasks to complete.
        this.waitForInstallTileTasks();

        // Clear the raster cache.
        this.getCache().clear();

        // Install the data descriptor for this tiled raster set.
        this.installConfigFile(this.productionParams);

        if (AVKey.SERVICE_NAME_LOCAL_RASTER_SERVER.equals(this.productionParams.get(AVKey.SERVICE_NAME))) {
            this.installRasterServerConfigFile(this.productionParams);
        }
    }

    protected String validateProductionParameters(AVList parameters) {
        StringBuilder sb = new StringBuilder();

        Object o = parameters.get(AVKey.FILE_STORE_LOCATION);
        if (!(o instanceof String) || ((CharSequence) o).length() < 1)
            sb.append((!sb.isEmpty() ? ", " : "")).append(Logging.getMessage("term.fileStoreLocation"));

        o = parameters.get(AVKey.DATA_CACHE_NAME);
        if (!(o instanceof String) || ((CharSequence) o).isEmpty())
            sb.append((!sb.isEmpty() ? ", " : "")).append(Logging.getMessage("term.fileStoreFolder"));

        o = parameters.get(AVKey.DATASET_NAME);
        if (!(o instanceof String) || ((CharSequence) o).length() < 1)
            sb.append((!sb.isEmpty() ? ", " : "")).append(Logging.getMessage("term.datasetName"));

        if (sb.isEmpty())
            return null;

        return Logging.getMessage("DataStoreProducer.InvalidDataStoreParamters", sb.toString());
    }

    //**************************************************************//
    //********************  LevelSet Assembly  *********************//
    //**************************************************************//

    protected static File installLocationFor(AVList params) {
        String fileStoreLocation = params.getStringValue(AVKey.FILE_STORE_LOCATION);
        String dataCacheName = params.getStringValue(AVKey.DATA_CACHE_NAME);
        if (fileStoreLocation == null || dataCacheName == null)
            return null;

        String path = WWIO.appendPathPart(fileStoreLocation, dataCacheName);
        if (path == null || path.isEmpty())
            return null;

        return new File(path);
    }

    protected abstract void initProductionParameters(AVList params);

    protected void initLevelSetParameters(AVList params) {
        int largeThreshold = Configuration.getIntegerValue(AVKey.TILED_RASTER_PRODUCER_LARGE_DATASET_THRESHOLD,
            DEFAULT_TILED_RASTER_PRODUCER_LARGE_DATASET_THRESHOLD);
        boolean isDataSetLarge = this.isDataSetLarge(this.dataRasterList, largeThreshold);

        Sector sector = (Sector) params.get(AVKey.SECTOR);
        if (sector == null) {
            // Compute a sector that bounds the data rasters. Make sure the sector does not exceed the limits of
            // latitude and longitude.
            sector = TiledRasterProducer.computeBoundingSector(this.dataRasterList);
            if (sector != null)
                sector = sector.intersection(Sector.FULL_SPHERE);
            params.set(AVKey.SECTOR, sector);
        }

        Integer tileWidth = (Integer) params.get(AVKey.TILE_WIDTH);
        if (tileWidth == null) {
            tileWidth = isDataSetLarge ? DEFAULT_TILE_WIDTH_AND_HEIGHT : DEFAULT_SINGLE_LEVEL_TILE_WIDTH_AND_HEIGHT;
            params.set(AVKey.TILE_WIDTH, tileWidth);
        }

        Integer tileHeight = (Integer) params.get(AVKey.TILE_HEIGHT);
        if (tileHeight == null) {
            tileHeight = isDataSetLarge ? DEFAULT_TILE_WIDTH_AND_HEIGHT : DEFAULT_SINGLE_LEVEL_TILE_WIDTH_AND_HEIGHT;
            params.set(AVKey.TILE_HEIGHT, tileHeight);
        }

        LatLon rasterTileDelta = this.computeRasterTileDelta(tileWidth, tileHeight, this.dataRasterList);
        LatLon desiredLevelZeroDelta = TiledRasterProducer.computeDesiredTileDelta(sector);

        Integer numLevels = (Integer) params.get(AVKey.NUM_LEVELS);
        if (numLevels == null) {
            // If the data set is large, then use compute a number of levels for the full pyramid. Otherwise use a
            // single level.
            numLevels = isDataSetLarge ? TiledRasterProducer.computeNumLevels(desiredLevelZeroDelta, rasterTileDelta) : 1;
            params.set(AVKey.NUM_LEVELS, numLevels);
        }

        Integer numEmptyLevels = (Integer) params.get(AVKey.NUM_EMPTY_LEVELS);
        if (numEmptyLevels == null) {
            numEmptyLevels = 0;
            params.set(AVKey.NUM_EMPTY_LEVELS, numEmptyLevels);
        }

        LatLon levelZeroTileDelta = (LatLon) params.get(AVKey.LEVEL_ZERO_TILE_DELTA);
        if (levelZeroTileDelta == null) {
            double scale = Math.pow(2.0d, numLevels - 1);
            levelZeroTileDelta = LatLon.fromDegrees(
                scale * rasterTileDelta.getLatitude().degrees,
                scale * rasterTileDelta.getLongitude().degrees);
            params.set(AVKey.LEVEL_ZERO_TILE_DELTA, levelZeroTileDelta);
        }

        LatLon tileOrigin = (LatLon) params.get(AVKey.TILE_ORIGIN);
        if (tileOrigin == null) {
            tileOrigin = new LatLon(sector.latMin(), sector.lonMin());
            params.set(AVKey.TILE_ORIGIN, tileOrigin);
        }

        // If the default or caller-specified values define a level set that does not fit in the limits of latitude
        // and longitude, then we re-define the level set parameters using values known to fit in those limits.
        if (!TiledRasterProducer.isWithinLatLonLimits(sector, levelZeroTileDelta, tileOrigin)) {
            levelZeroTileDelta = TiledRasterProducer.computeIntegralLevelZeroTileDelta(levelZeroTileDelta);
            params.set(AVKey.LEVEL_ZERO_TILE_DELTA, levelZeroTileDelta);

            tileOrigin = new LatLon(Angle.NEG90, Angle.NEG180);
            params.set(AVKey.TILE_ORIGIN, tileOrigin);

            numLevels = TiledRasterProducer.computeNumLevels(levelZeroTileDelta, rasterTileDelta);
            params.set(AVKey.NUM_LEVELS, numLevels);
        }
    }

    protected static LatLon computeIntegralLevelZeroTileDelta(LatLon originalDelta) {
        // Find a level zero tile delta that's an integral factor of each dimension.

        double latDelta = Math.ceil(originalDelta.latitude);
        double lonDelta = Math.ceil(originalDelta.longitude);

        while (180 % latDelta != 0) {
            --latDelta;
        }

        while (360 % lonDelta != 0) {
            --lonDelta;
        }

        return LatLon.fromDegrees(latDelta, lonDelta);
    }

    protected boolean isDataSetLarge(Iterable<? extends DataRaster> rasters, int largeThreshold) {
        Sector sector = TiledRasterProducer.computeBoundingSector(rasters);
        LatLon pixelSize = this.computeSmallestPixelSize(rasters);
        int sectorWidth = (int) Math.ceil(sector.lonDelta / pixelSize.getLongitude().degrees);
        int sectorHeight = (int) Math.ceil(sector.latDelta / pixelSize.getLatitude().degrees);
        return (sectorWidth >= largeThreshold) || (sectorHeight >= largeThreshold);
    }

    protected static boolean isWithinLatLonLimits(Sector sector, LatLon tileDelta, LatLon tileOrigin) {
        double minLat = Math.floor((sector.latMin - tileOrigin.getLatitude().degrees)
            / tileDelta.getLatitude().degrees);
        minLat = tileOrigin.getLatitude().degrees + minLat * tileDelta.getLatitude().degrees;
        double maxLat = Math.ceil((sector.latMax - tileOrigin.getLatitude().degrees)
            / tileDelta.getLatitude().degrees);
        maxLat = tileOrigin.getLatitude().degrees + maxLat * tileDelta.getLatitude().degrees;
        double minLon = Math.floor((sector.lonMin - tileOrigin.getLongitude().degrees)
            / tileDelta.getLongitude().degrees);
        minLon = tileOrigin.getLongitude().degrees + minLon * tileDelta.getLongitude().degrees;
        double maxLon = Math.ceil((sector.lonMax - tileOrigin.getLongitude().degrees)
            / tileDelta.getLongitude().degrees);
        maxLon = tileOrigin.getLongitude().degrees + maxLon * tileDelta.getLongitude().degrees;
        return Sector.fromDegrees(minLat, maxLat, minLon, maxLon).isWithinLatLonLimits();
    }

    protected static Sector computeBoundingSector(Iterable<? extends DataRaster> rasters) {
        Sector sector = null;
        for (DataRaster raster : rasters) {
            sector = (sector != null) ? raster.getSector().union(sector) : raster.getSector();
        }
        return sector;
    }

    protected LatLon computeRasterTileDelta(int tileWidth, int tileHeight, Iterable<? extends DataRaster> rasters) {
        LatLon pixelSize = this.computeSmallestPixelSize(rasters);
        // Compute the tile size in latitude and longitude, given a raster's sector and dimension, and the tile
        // dimensions. In this computation a pixel is assumed to cover a finite area.
        double latDelta = tileHeight * pixelSize.getLatitude().degrees;
        double lonDelta = tileWidth * pixelSize.getLongitude().degrees;
        return LatLon.fromDegrees(latDelta, lonDelta);
    }

    protected static LatLon computeDesiredTileDelta(Sector sector) {
        double levelZeroLat = Math.min(sector.latDelta, DEFAULT_LEVEL_ZERO_TILE_DELTA);
        double levelZeroLon = Math.min(sector.lonDelta, DEFAULT_LEVEL_ZERO_TILE_DELTA);
        return LatLon.fromDegrees(levelZeroLat, levelZeroLon);
    }

    protected LatLon computeRasterPixelSize(DataRaster raster) {
        // Compute the raster's pixel dimension in latitude and longitude. In this computation a pixel is assumed to
        // cover a finite area.
        return LatLon.fromDegrees(
            raster.getSector().latDelta / raster.getHeight(),
            raster.getSector().lonDelta / raster.getWidth());
    }

    protected LatLon computeSmallestPixelSize(Iterable<? extends DataRaster> rasters) {
        // Find the smallest pixel dimensions in the given rasters.
        double smallestLat = Double.MAX_VALUE;
        double smallestLon = Double.MAX_VALUE;
        for (DataRaster raster : rasters) {
            LatLon curSize = this.computeRasterPixelSize(raster);
            if (smallestLat > curSize.getLatitude().degrees)
                smallestLat = curSize.getLatitude().degrees;
            if (smallestLon > curSize.getLongitude().degrees)
                smallestLon = curSize.getLongitude().degrees;
        }
        return LatLon.fromDegrees(smallestLat, smallestLon);
    }

    //**************************************************************//
    //********************  DataRaster Assembly  *******************//
    //**************************************************************//

    protected static int computeNumLevels(LatLon levelZeroDelta, LatLon lastLevelDelta) {
        // Compute the number of levels needed to achieve the given last level tile delta, starting from the given
        // level zero tile delta.
        double numLatLevels = 1 + WWMath.logBase2(levelZeroDelta.getLatitude().getDegrees())
            - WWMath.logBase2(lastLevelDelta.getLatitude().getDegrees());
        double numLonLevels = 1 + WWMath.logBase2(levelZeroDelta.getLongitude().getDegrees())
            - WWMath.logBase2(lastLevelDelta.getLongitude().getDegrees());

        // Compute the maximum number of levels needed, but limit the number of levels to positive integers greater
        // than or equal to one.
        int numLevels = (int) Math.ceil(Math.max(numLatLevels, numLonLevels));
        if (numLevels < 1)
            numLevels = 1;

        return numLevels;
    }

    protected void assembleDataRasters() throws Exception {
        // Exit if the caller has instructed us to stop production.
        if (this.isStopped())
            return;

        for (SourceInfo info : this.getDataSourceList()) {
            // Exit if the caller has instructed us to stop production.
            if (this.isStopped())
                break;

            Thread.sleep(0);

            // Don't validate the data source here. Data sources are validated when they're passed to the producer in
            // offerDataSource() or offerAllDataSources().
            this.assembleDataSource(info.source, info);
        }
    }

    protected void assembleDataSource(Object source, AVList params) throws Exception {
        if (source instanceof DataRaster) {
            this.dataRasterList.add((DataRaster) source);
        }
        else {
            DataRasterReader reader = this.readerFactory.findReaderFor(source, params, this.getDataRasterReaders());
            this.dataRasterList.add(new CachedDataRaster(source, params, reader, this.getCache()));
        }
    }

    //**************************************************************//
    //********************  LevelSet Installation  *****************//
    //**************************************************************//

    protected void installLevelSet(LevelSet levelSet, AVList params) throws IOException {
        // Exit if the caller has instructed us to stop production.
        if (this.isStopped())
            return;

        // Setup the progress parameters.
        this.calculateTileCount(levelSet, params);
        this.startProgress();

        Sector sector = levelSet.getSector();
        Level level = levelSet.getFirstLevel();

        Angle dLat = level.getTileDelta().getLatitude();
        Angle dLon = level.getTileDelta().getLongitude();
        Angle latOrigin = levelSet.getTileOrigin().getLatitude();
        Angle lonOrigin = levelSet.getTileOrigin().getLongitude();
        int firstRow = Tile.computeRow(dLat, sector.latMin(), latOrigin);
        int firstCol = Tile.computeColumn(dLon, sector.lonMin(), lonOrigin);
        int lastRow = Tile.computeRow(dLat, sector.latMax(), latOrigin);
        int lastCol = Tile.computeColumn(dLon, sector.lonMax(), lonOrigin);

        buildLoop:
        {
            Angle p1 = Tile.computeRowLatitude(firstRow, dLat, latOrigin);
            for (int row = firstRow; row <= lastRow; row++) {
                Angle p2 = p1.add(dLat);
                Angle t1 = Tile.computeColumnLongitude(firstCol, dLon, lonOrigin);
                for (int col = firstCol; col <= lastCol; col++) {
                    // Exit if the caller has instructed us to stop production.
                    Thread.yield();
                    if (this.isStopped())
                        break buildLoop;

                    Angle t2 = t1.add(dLon);

                    Tile tile = new Tile(new Sector(p1, p2, t1, t2), level, row, col);
                    DataRaster tileRaster = this.createTileRaster(levelSet, tile, params);
                    // Write the top-level tile raster to disk.
                    if (tileRaster != null)
                        this.installTileRasterLater(levelSet, tile, tileRaster, params);

                    t1 = t2;
                }
                p1 = p2;
            }
        }
    }

    protected DataRaster createTileRaster(LevelSet levelSet, Tile tile, AVList params) throws IOException {
        // Exit if the caller has instructed us to stop production.
        if (this.isStopped())
            return null;

        DataRaster tileRaster;

        // If we have reached the final level, then create a tile raster from the original data sources.
        if (TiledRasterProducer.isFinalLevel(levelSet, tile.getLevelNumber(), params)) {
            tileRaster = this.drawDataSources(levelSet, tile, this.dataRasterList, params);
        }
        // Otherwise, recursively create a tile raster from the next level's tile rasters.
        else {
            tileRaster = this.drawDescendants(levelSet, tile, params);
        }

        this.updateProgress();

        return tileRaster;
    }

    protected DataRaster drawDataSources(LevelSet levelSet, Tile tile, Iterable<DataRaster> dataRasters,
        AVList params) {
        DataRaster tileRaster = null;

        // Find the data sources that intersect this tile and intersect the LevelSet sector.
        Collection<DataRaster> intersectingRasters = new ArrayList<>();
        for (DataRaster raster : dataRasters) {
            if (raster.getSector().intersects(tile.sector) && raster.getSector().intersects(levelSet.getSector()))
                intersectingRasters.add(raster);
        }

        // If any data sources intersect this tile, and the tile's level is not empty, then we attempt to read those
        // sources and render them into this tile.
        if (!intersectingRasters.isEmpty() && !tile.level.isEmpty()) {
            // Create the tile raster to render into.
            tileRaster = this.createDataRaster(tile.level.getTileWidth(), tile.level.getTileHeight(),
                tile.sector, params);
            // Render each data source raster into the tile raster.
            for (DataRaster raster : intersectingRasters) {
                raster.drawOnTo(tileRaster);
            }
        }

        // Make the data rasters available for garbage collection.
        intersectingRasters.clear();
        //noinspection UnusedAssignment
        intersectingRasters = null;

        return tileRaster;
    }

    protected DataRaster drawDescendants(LevelSet levelSet, Tile tile, AVList params) throws IOException {
        DataRaster tileRaster = null;
        boolean hasDescendants = false;

        // Recursively create sub-tile rasters.
        Tile[] subTiles = TiledRasterProducer.createSubTiles(tile, levelSet.getLevel(tile.getLevelNumber() + 1));
        DataRaster[] subRasters = new DataRaster[subTiles.length];
        for (int index = 0; index < subTiles.length; index++) {
            // If the sub-tile does not intersect the level set, then skip that sub-tile.
            if (subTiles[index].sector.intersects(levelSet.getSector())) {
                // Recursively create the sub-tile raster.
                DataRaster subRaster = this.createTileRaster(levelSet, subTiles[index], params);
                // If creating the sub-tile raster fails, then skip that sub-tile.
                if (subRaster != null) {
                    subRasters[index] = subRaster;
                    hasDescendants = true;
                }
            }
        }

        // Exit if the caller has instructed us to stop production.
        if (this.isStopped())
            return null;

        // If any of the sub-tiles successfully created a data raster, then we potentially create this tile's raster,
        // then write the sub-tiles to disk.
        if (hasDescendants) {
            // If this tile's level is not empty, then create and render the tile's raster.
            if (!tile.level.isEmpty()) {
                // Create the tile's raster.
                tileRaster = this.createDataRaster(tile.level.getTileWidth(), tile.level.getTileHeight(),
                    tile.sector, params);

                for (int index = 0; index < subTiles.length; index++) {
                    if (subRasters[index] != null) {
                        // Render the sub-tile raster to this this tile raster.
                        subRasters[index].drawOnTo(tileRaster);
                    }
                }
            }
        }

        // Write the sub-rasters to disk.
        for (int index = 0; index < subTiles.length; index++) {
            if (subRasters[index] != null)
                this.installTileRasterLater(levelSet, subTiles[index], subRasters[index], params);
        }

        return tileRaster;
    }

    protected static Tile[] createSubTiles(Tile tile, Level nextLevel) {
        Angle p0 = tile.sector.latMin();
        Angle p2 = tile.sector.latMax();
        Angle p1 = Angle.midAngle(p0, p2);

        Angle t0 = tile.sector.lonMin();
        Angle t2 = tile.sector.lonMax();
        Angle t1 = Angle.midAngle(t0, t2);

        int row = tile.row;
        int col = tile.col;

        Tile[] subTiles = new Tile[4];
        subTiles[0] = new Tile(new Sector(p0, p1, t0, t1), nextLevel, 2 * row, 2 * col);
        subTiles[1] = new Tile(new Sector(p0, p1, t1, t2), nextLevel, 2 * row, 2 * col + 1);
        subTiles[2] = new Tile(new Sector(p1, p2, t1, t2), nextLevel, 2 * row + 1, 2 * col + 1);
        subTiles[3] = new Tile(new Sector(p1, p2, t0, t1), nextLevel, 2 * row + 1, 2 * col);

        return subTiles;
    }

    protected static boolean isFinalLevel(LevelSet levelSet, int levelNumber, AVList params) {
        if (levelSet.isFinalLevel(levelNumber))
            return true;

        int maxNumOfLevels = levelSet.getLastLevel().getLevelNumber();
        int limit = TiledRasterProducer.extractMaxLevelLimit(params, maxNumOfLevels);
        return (levelNumber >= limit);
    }

    /**
     * Extracts a maximum level limit from the AVList if the AVList contains AVKey.TILED_RASTER_PRODUCER_LIMIT_MAX_LEVEL.
     * This method requires <code>maxNumOfLevels</code> - the actual maximum numbers of levels.
     * <p>
     * The AVKey.TILED_RASTER_PRODUCER_LIMIT_MAX_LEVEL could specify multiple things:
     * <p>
     * If the value of the AVKey.TILED_RASTER_PRODUCER_LIMIT_MAX_LEVEL is "Auto" (as String), the calculated limit of
     * levels will be 70% of the actual maximum numbers of levels <code>maxNumOfLevels</code>.
     * <p>
     * If the type of the value of the AVKey.TILED_RASTER_PRODUCER_LIMIT_MAX_LEVEL is Integer, it should contain an
     * integer number between 0 (for level 0 only) and the actual maximum numbers of levels
     * <code>maxNumOfLevels</code>.
     * <p>
     * It is also possible to specify the limit as percents, in this case the type of the
     * AVKey.TILED_RASTER_PRODUCER_LIMIT_MAX_LEVEL value must be "String", have a numeric value as text and the "%"
     * percent sign in the end. Examples: "100%", "25%", "50%", etc.
     * <p>
     * Value of AVKey.TILED_RASTER_PRODUCER_LIMIT_MAX_LEVEL could be a numeric string (for example, "3"), or Integer.
     * The value will be correctly extracted and compared with the <code>maxNumOfLevels</code>. Valid values must be
     * smaller or equal to <code>maxNumOfLevels</code>.
     *
     * @param params         AVList that may contain AVKey.TILED_RASTER_PRODUCER_LIMIT_MAX_LEVEL property
     * @param maxNumOfLevels The actual maximum numbers of levels
     * @return A limit of numbers of levels that should producer generate.
     */
    protected static int extractMaxLevelLimit(AVList params, int maxNumOfLevels) {
        if (null != params && params.hasKey(AVKey.TILED_RASTER_PRODUCER_LIMIT_MAX_LEVEL)) {
            Object o = params.get(AVKey.TILED_RASTER_PRODUCER_LIMIT_MAX_LEVEL);
            if (o instanceof Integer) {
                int limit = (Integer) o;
                return Math.min(limit, maxNumOfLevels);
            }
            else if (o instanceof String) {
                String strLimit = (String) o;
                if ("Auto".equalsIgnoreCase(strLimit)) {
                    return (int) Math.floor(0.5d * maxNumOfLevels); // 0.5 = half, 0.6 = 60%
                }
                else if (!strLimit.isEmpty() && strLimit.charAt(strLimit.length() - 1) == '%') {
                    try {
                        float percent = Float.parseFloat(strLimit.substring(0, strLimit.length() - 1));
                        int limit = (int) Math.floor(percent * (double) maxNumOfLevels / 100.0d);
                        return Math.min(limit, maxNumOfLevels);
                    }
                    catch (Throwable t) {
                        Logging.logger().finest(WWUtil.extractExceptionReason(t));
                    }
                }
                else {
                    try {
                        int limit = Integer.parseInt(strLimit);
                        return Math.min(limit, maxNumOfLevels);
                    }
                    catch (Throwable t) {
                        Logging.logger().finest(WWUtil.extractExceptionReason(t));
                    }
                }
            }
        }

        return maxNumOfLevels;
    }

    //**************************************************************//
    //********************  Tile Installation  *********************//
    //**************************************************************//

    protected ExecutorService createDefaultTileWriteService(int threadPoolSize) {
        // TODO: comment

        // Create a fixed thread pool, but provide a callback to release a tile write permit when a task completes.
        return new ThreadPoolExecutor(
            // Fixed size thread pool.
            threadPoolSize, threadPoolSize,
            // This value is irrelevant, as threads only terminated when the executor is shutdown.
            0L, TimeUnit.MILLISECONDS,
            // Provide an unbounded work queue.
            new LinkedBlockingQueue<>()) {
            protected void afterExecute(Runnable runnable, Throwable t) {
                // Invoke the superclass routine, then release a tile write permit.
                super.afterExecute(runnable, t);
                TiledRasterProducer.this.installTileRasterComplete();
            }
        };
    }

    protected void installTileRasterLater(final LevelSet levelSet, final Tile tile, final DataRaster tileRaster,
        final AVList params) {
        // TODO: comment
        // Try to acquire a permit from the tile write semaphore.
        this.getTileWriteSemaphore().acquireUninterruptibly();
        // We've acquired the permit, now execute the installTileRaster() routine in a different thread.
        this.getTileWriteService().execute(() -> {
            try {
                installTileRaster(tile, tileRaster, params);
                // Dispose the data raster.
                if (tileRaster != null)
                    tileRaster.dispose();
            }
            catch (Throwable t) {
                String message = Logging.getMessage("generic.ExceptionWhileWriting", tile);
                Logging.logger().log(java.util.logging.Level.SEVERE, message, t);
            }
        });
    }

    protected void installTileRasterComplete() {
        // TODO: comment
        this.getTileWriteSemaphore().release();
    }

    protected void waitForInstallTileTasks() {
        // TODO: comment
        try {
            ExecutorService service = this.getTileWriteService();
            service.shutdown();
            // Block this thread until the executor has completed.
            while (!service.awaitTermination(1000L, TimeUnit.MILLISECONDS)) {
                Thread.sleep(5L);
            }
        }
        catch (InterruptedException e) {
            String msg = Logging.getMessage("generic.interrupted", this.getClass().getName(),
                "waitForInstallTileTasks()");
            Logging.logger().finest(msg);
            // Don't swallow interrupts; instead, restore the interrupted status
            Thread.currentThread().interrupt();
        }
    }

    protected void installTileRaster(Tile tile, DataRaster tileRaster, AVList params) throws IOException {
        File installLocation;

        // Compute the install location of the tile.
        Object result = TiledRasterProducer.installLocationForTile(params, tile);
        if (result instanceof File) {
            installLocation = (File) result;
        }
        else {
            String message = result.toString();
            Logging.logger().severe(message);
            throw new IOException(message);
        }

        synchronized (this.fileLock) {
            File dir = installLocation.getParentFile();
            if (!dir.exists()) {
                if (!dir.mkdirs()) {
                    String message = Logging.getMessage("generic.CannotCreateFile", dir);
                    Logging.logger().warning(message);
                }
            }
        }

        // Write the tile data to the filesystem.
        String formatSuffix = params.getStringValue(AVKey.FORMAT_SUFFIX);
        DataRasterWriter[] writers = this.getDataRasterWriters();

        Object writer = TiledRasterProducer.findWriterFor(tileRaster, formatSuffix, installLocation, writers);
        if (writer instanceof DataRasterWriter) {
            try {
                ((DataRasterWriter) writer).write(tileRaster, formatSuffix, installLocation);
            }
            catch (IOException e) {
                String message = Logging.getMessage("generic.ExceptionWhileWriting", installLocation);
                Logging.logger().log(java.util.logging.Level.SEVERE, message, e);
            }
        }
    }

    protected static Object installLocationForTile(AVList installParams, Tile tile) {
        String path = null;

        String s = installParams.getStringValue(AVKey.FILE_STORE_LOCATION);
        if (s != null)
            path = WWIO.appendPathPart(path, s);

        s = tile.getPath();
        if (s != null)
            path = WWIO.appendPathPart(path, s);

        if (path == null || path.length() < 1)
            return Logging.getMessage("TiledRasterProducer.InvalidTile", tile);

        return new File(path);
    }

    protected static Object findWriterFor(DataRaster raster, String formatSuffix, File destination,
        DataRasterWriter[] writers) {
        for (DataRasterWriter writer : writers) {
            if (writer.canWrite(raster, formatSuffix, destination))
                return writer;
        }

        // No writer maching this DataRaster/formatSuffix.
        return Logging.getMessage("DataRaster.CannotWrite", raster, formatSuffix, destination);
    }

    //**************************************************************//
    //********************  Config File Installation  **************//
    //**************************************************************//

    /**
     * Returns a configuration document which describes the tiled data produced by this TiledRasterProducer. The
     * document's contents are derived from the specified parameter list, and depend on the concrete subclass'
     * implementation. This returns null if the parameter list is null, or if the configuration document cannot be
     * created for any reason.
     *
     * @param params the parameters which describe the configuration document's contents.
     * @return the configuration document, or null if the parameter list is null or does not contain the required
     * parameters.
     */
    protected abstract Document createConfigDoc(AVList params);

    /**
     * Installs the configuration file which describes the tiled data produced by this TiledRasterProducer. The install
     * location, configuration filename, and configuration file contents are derived from the specified parameter list.
     * This throws an exception if the configuration file cannot be installed for any reason.
     * <p>
     * The parameter list must contain <strong>at least</strong> the following keys: <table>
     * <caption style="font-weight: bold;">Required Keys</caption><tr><th>Key</th></tr>
     * <tr><td>{@link AVKey#FILE_STORE_LOCATION}</td><td></td></tr> <tr><td>{@link
     * AVKey#DATA_CACHE_NAME}</td><td></td></tr> <tr><td>{@link
     * AVKey#DATASET_NAME}</td><td></td></tr> </table>
     *
     * @param params the parameters which describe the install location, the configuration filename, and the
     *               configuration file contents.
     */
    protected void installConfigFile(AVList params) {
        if (params == null) {
            String message = Logging.getMessage("nullValue.ParametersIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        // Exit if the caller has instructed us to stop production.
        if (this.isStopped())
            return;

        File configFile = TiledRasterProducer.getConfigFileInstallLocation(params);
        if (configFile == null) {
            String message = Logging.getMessage("TiledRasterProducer.NoConfigFileInstallLocation",
                params.get(AVKey.DATASET_NAME));
            Logging.logger().severe(message);
            throw new WWRuntimeException(message);
        }

        // Synchronize construction of the config file's parent directories. One or more tile installation tasks may be
        // running when this code executes. This synchronizes construction of common parent directories between with the
        // tile installation tasks.
        synchronized (this.fileLock) {
            File dir = configFile.getParentFile();
            if (!dir.exists()) {
                if (!dir.mkdirs()) {
                    String message = Logging.getMessage("generic.CannotCreateFile", dir);
                    Logging.logger().warning(message);
                }
            }
        }

        Document configDoc = this.createConfigDoc(params);
        if (configDoc == null) {
            String message = Logging.getMessage("TiledRasterProducer.CannotCreateConfigDoc",
                params.get(AVKey.DATASET_NAME));
            Logging.logger().severe(message);
            throw new WWRuntimeException(message);
        }

        try {
            WWXML.saveDocumentToFile(configDoc, configFile.getAbsolutePath());
        }
        catch (Exception e) {
            String message = Logging.getMessage("TiledRasterProducer.CannotWriteConfigFile", configFile);
            Logging.logger().severe(message);
            throw new WWRuntimeException(message);
        }

        this.getProductionResultsList().add(configDoc);
    }

    /**
     * Returns the location of the configuration file which describes the tiled data produced by this
     * TiledRasterProducer. The install location is derived from the specified parameter list. This returns null if the
     * parameter list is null, or if it does not contain any of the following keys: <table>
     * <caption style="font-weight: bold;">Required Keys</caption><tr><th>Key</th></tr>
     * <tr><td>{@link AVKey#FILE_STORE_LOCATION}</td><td></td></tr> <tr><td>{@link
     * AVKey#DATA_CACHE_NAME}</td><td></td></tr> <tr><td>{@link
     * AVKey#DATASET_NAME}</td><td></td></tr> </table>
     *
     * @param params the parameters which describe the install location.
     * @return the configuration file install location, or null if the parameter list is null or does not contain the
     * required parameters.
     */
    protected static File getConfigFileInstallLocation(AVList params) {
        if (params == null)
            return null;

        String fileStoreLocation = params.getStringValue(AVKey.FILE_STORE_LOCATION);
        if (fileStoreLocation != null)
            fileStoreLocation = WWIO.stripTrailingSeparator(fileStoreLocation);

        if (WWUtil.isEmpty(fileStoreLocation))
            return null;

        String cacheName = DataConfigurationUtils.getDataConfigFilename(params, ".xml");
        if (cacheName != null)
            cacheName = WWIO.stripLeadingSeparator(cacheName);

        if (WWUtil.isEmpty(cacheName))
            return null;

        return new File(fileStoreLocation + File.separator + cacheName);
    }

    protected void installRasterServerConfigFile(AVList productionParams) {
        File configFile = TiledRasterProducer.getConfigFileInstallLocation(productionParams);
        String configFilePath = configFile.getAbsolutePath().replace(".xml", ".RasterServer.xml");

        Document configDoc = WWXML.createDocumentBuilder(true).newDocument();
        Element root = WWXML.setDocumentElement(configDoc, "RasterServer");
        WWXML.setTextAttribute(root, "version", "1.0");

        Sector extent = null;
        if (productionParams.hasKey(AVKey.SECTOR)) {
            Object o = productionParams.get(AVKey.SECTOR);
            if (o instanceof Sector) {
                extent = (Sector) o;
            }
        }
        if (null != extent) {
            WWXML.appendSector(root, "Sector", extent);
        }
        else {
            String message = Logging.getMessage("generic.MissingRequiredParameter", AVKey.SECTOR);
            Logging.logger().severe(message);
            throw new WWRuntimeException(message);
        }

        Element sources = configDoc.createElementNS(null, "Sources");
        for (DataRaster raster : this.getDataRasters()) {
            if (raster instanceof CachedDataRaster) {
                try {
                    TiledRasterProducer.appendSource(sources, (CachedDataRaster) raster);
                }
                catch (Throwable t) {
                    String reason = WWUtil.extractExceptionReason(t);
                    Logging.logger().warning(reason);
                }
            }
            else {
                String message = Logging.getMessage("TiledRasterProducer.UnrecognizedRasterType",
                    raster.getClass().getName(), raster.getStringValue(AVKey.DATASET_NAME));
                Logging.logger().severe(message);
                throw new WWRuntimeException(message);
            }
        }

        AVList rasterServerProperties = new AVListImpl();

        String[] keysToCopy = new String[] {AVKey.DATA_CACHE_NAME, AVKey.DATASET_NAME, AVKey.DISPLAY_NAME};
        WWUtil.copyValues(productionParams, rasterServerProperties, keysToCopy, false);

        TiledRasterProducer.appendProperties(root, rasterServerProperties);

        // add sources
        root.appendChild(sources);

        WWXML.saveDocumentToFile(configDoc, configFilePath);
    }

    protected static void appendProperties(Element context, AVList properties) {
        if (null == context || properties == null) {
            return;
        }

        StringBuilder sb = new StringBuilder();

        // add properties
        for (Map.Entry<String, Object> entry : properties.getEntries()) {
            sb.setLength(0);
            String key = entry.getKey();
            sb.append(properties.get(key));
            String value = sb.toString();
            if (WWUtil.isEmpty(key) || WWUtil.isEmpty(value)) {
                continue;
            }

            Element property = WWXML.appendElement(context, "Property");
            WWXML.setTextAttribute(property, "name", key);
            WWXML.setTextAttribute(property, "value", value);
        }
    }

    protected static void appendSource(Element sources, CachedDataRaster raster) throws WWRuntimeException {
        Object o = raster.getDataSource();
        if (WWUtil.isEmpty(o)) {
            String message = Logging.getMessage("nullValue.DataSourceIsNull");
            Logging.logger().fine(message);
            throw new WWRuntimeException(message);
        }

        File f = WWIO.getFileForLocalAddress(o);
        if (WWUtil.isEmpty(f)) {
            String message = Logging.getMessage("TiledRasterProducer.UnrecognizedDataSource", o);
            Logging.logger().fine(message);
            throw new WWRuntimeException(message);
        }

        Element source = WWXML.appendElement(sources, "Source");
        WWXML.setTextAttribute(source, "type", "file");
        WWXML.setTextAttribute(source, "path", f.getAbsolutePath());

        AVList params = raster.getParams();
        if (null == params) {
            String message = Logging.getMessage("nullValue.ParamsIsNull");
            Logging.logger().fine(message);
            throw new WWRuntimeException(message);
        }

        Sector sector = raster.getSector();
        if (null == sector && params.hasKey(AVKey.SECTOR)) {
            o = params.get(AVKey.SECTOR);
            if (o instanceof Sector) {
                sector = (Sector) o;
            }
        }

        if (null != sector) {
            WWXML.appendSector(source, "Sector", sector);
        }
    }

    //**************************************************************//
    //********************  Progress  ******************************//
    //**************************************************************//

    protected void calculateTileCount(LevelSet levelSet, AVList params) {
        Sector sector = levelSet.getSector();

        this.tileCount = 0;
        for (Level level : levelSet.getLevels()) {
            Angle dLat = level.getTileDelta().getLatitude();
            Angle dLon = level.getTileDelta().getLongitude();
            Angle latOrigin = levelSet.getTileOrigin().getLatitude();
            Angle lonOrigin = levelSet.getTileOrigin().getLongitude();
            int firstRow = Tile.computeRow(dLat, sector.latMin(), latOrigin);
            int firstCol = Tile.computeColumn(dLon, sector.lonMin(), lonOrigin);
            int lastRow = Tile.computeRow(dLat, sector.latMax(), latOrigin);
            int lastCol = Tile.computeColumn(dLon, sector.lonMax(), lonOrigin);
            this.tileCount += (lastRow - firstRow + 1) * (lastCol - firstCol + 1);

            if (TiledRasterProducer.isFinalLevel(levelSet, level.getLevelNumber(), params))
                break;
        }
    }

    protected void startProgress() {
        this.tile = 0;
        this.firePropertyChange(AVKey.PROGRESS, null, 0.0d);
    }

    protected void updateProgress() {
        double oldProgress = this.tile / (double) this.tileCount;
        double newProgress = ++this.tile / (double) this.tileCount;
        this.firePropertyChange(AVKey.PROGRESS, oldProgress, newProgress);
    }
}
