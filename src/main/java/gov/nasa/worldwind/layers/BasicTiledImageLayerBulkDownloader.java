/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */

package gov.nasa.worldwind.layers;

import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.cache.FileStore;
import gov.nasa.worldwind.event.*;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.retrieve.*;
import gov.nasa.worldwind.util.*;

import java.io.*;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.*;

/**
 * Downloads imagery not currently available in the WorldWind file cache or a specified file store. The class derives
 * from {@link Thread} and is meant to operate in its own thread.
 * <p>
 * The sector and resolution associated with the downloader are specified during construction and are final.
 *
 * @author tag
 * @version $Id: BasicTiledImageLayerBulkDownloader.java 1171 2013-02-11 21:45:02Z dcollins $
 */
public class BasicTiledImageLayerBulkDownloader extends BulkRetrievalThread {
    protected final static int MAX_TILE_COUNT_PER_REGION = 200;
    protected final static long DEFAULT_AVERAGE_FILE_SIZE = 350000L;

    protected final BasicTiledImageLayer layer;
    protected final int level;
    protected ArrayList<TextureTile> missingTiles;

    /**
     * Constructs a downloader to retrieve imagery not currently available in a specified file store.
     * <p>
     * The thread returned is not started during construction, the caller must start the thread.
     *
     * @param layer      the layer for which to download imagery.
     * @param sector     the sector to download data for. This value is final.
     * @param resolution the target resolution, provided in radians of latitude per texel. This value is final.
     * @param fileStore  the file store in which to place the downloaded elevations.
     * @param listener   an optional retrieval listener. May be null.
     * @throws IllegalArgumentException if either the layer, the sector or file store are null, or the resolution is
     *                                  less than zero.
     */
    public BasicTiledImageLayerBulkDownloader(BasicTiledImageLayer layer, Sector sector, double resolution,
        FileStore fileStore, BulkRetrievalListener listener) {
        // Arguments checked in parent constructor
        super(layer, sector, resolution, fileStore, listener);

        this.layer = layer;
        this.level = this.layer.computeLevelForResolution(sector, resolution);
    }

    protected static Sector[] computeRandomRegions(Sector sector, int div, int numRegions) {
        if (numRegions > div * div)
            return sector.subdivide(div);

        final double dLat = sector.latDelta / div;
        final double dLon = sector.lonDelta / div;
        ArrayList<Sector> regions = new ArrayList<>(numRegions);
        Random rand = new Random();
        while (regions.size() < numRegions) {
            int row = rand.nextInt(div);
            int col = rand.nextInt(div);
            Sector s = Sector.fromDegrees(
                sector.latMin + dLat * row,
                sector.latMin + dLat * row + dLat,
                sector.lonMin + dLon * col,
                sector.lonMin + dLon * col + dLon);
            if (!regions.contains(s))
                regions.add(s);
        }

        return regions.toArray(new Sector[numRegions]);
    }

    protected static Iterator<Sector> getRegionIterator(final Sector sector, final int div) {
        final double dLat = sector.latDelta / div;
        final double dLon = sector.lonDelta / div;

        return new Iterator<>() {
            int row;
            int col;

            public boolean hasNext() {
                return row < div;
            }

            public Sector next() {
                Sector s = Sector.fromDegrees(
                    sector.latMin + dLat * row,
                    sector.latMin + dLat * row + dLat,
                    sector.lonMin + dLon * col,
                    sector.lonMin + dLon * col + dLon);

                col++;
                if (col >= div) {
                    col = 0;
                    row++;
                }
                return s;
            }

            public void remove() {

            }
        };
    }

    protected static long computeAverageTileSize(File dir) {
        long size = 0;
        int count = 0;

        File[] files = dir.listFiles();
        for (File file : files) {
            try {
                FileInputStream fis = new FileInputStream(file);
                size += fis.available();
                fis.close();
                count++;
            }
            catch (IOException e) {
                count += 0;
            }
        }

        return count > 0 ? size / count : 0;
    }

    public void run() {
        try {
            // Init progress with missing tile count estimate
            this.progress.setTotalCount(this.estimateMissingTilesCount(20));
            this.progress.setTotalSize(this.progress.getTotalCount() * estimateAverageTileSize());

            // Determine and request missing tiles by level/region
            for (int levelNumber = 0; levelNumber <= this.level; levelNumber++) {
                if (this.layer.getLevels().isLevelEmpty(levelNumber))
                    continue;

                int div = this.computeRegionDivisions(this.sector, levelNumber,
                    BasicTiledImageLayerBulkDownloader.MAX_TILE_COUNT_PER_REGION);
                Iterator<Sector> regionsIterator = BasicTiledImageLayerBulkDownloader.getRegionIterator(this.sector,
                    div);

                Sector region;
                while (regionsIterator.hasNext()) {
                    region = regionsIterator.next();
                    // Determine missing tiles
                    this.missingTiles = getMissingTilesInSector(region, levelNumber);

                    // Submit missing tiles requests at intervals
                    while (!this.missingTiles.isEmpty()) {
                        submitMissingTilesRequests();
                        if (!this.missingTiles.isEmpty())
                            Thread.sleep(RETRIEVAL_SERVICE_POLL_DELAY);
                    }
                }
            }
            // Set progress to 100%
            this.progress.setTotalCount(this.progress.getCurrentCount());
            this.progress.setTotalSize(this.progress.getCurrentSize());
        }
        catch (InterruptedException e) {
            String message = Logging.getMessage("generic.BulkRetrievalInterrupted", this.layer.name());
            Logging.logger().log(java.util.logging.Level.WARNING, message, e);
        }
        catch (RuntimeException e) {
            String message = Logging.getMessage("generic.ExceptionDuringBulkRetrieval", this.layer.name());
            Logging.logger().severe(message);
            throw new RuntimeException(message);
        }
    }

    protected synchronized void submitMissingTilesRequests() throws InterruptedException {
        RetrievalService rs = WorldWind.retrieveRemote();
        int i = 0;
        while (this.missingTiles.size() > i && rs.isAvailable()) {
            Thread.sleep(1); // generates InterruptedException if thread has been interrupted

            TextureTile tile = this.missingTiles.get(i);

            if (this.layer.getLevels().missing(tile)) {
                removeAbsentTile(tile);  // tile is absent, count it off.
                continue;
            }

            URL url = this.fileStore.findFile(tile.getPath(), false);
            if (url != null) {
                // tile has been retrieved and is local now, count it as retrieved.
                removeRetrievedTile(tile);
                continue;
            }

            this.layer.retrieveRemoteTexture(tile, createBulkDownloadPostProcessor(tile));
            i++;
        }
    }

    protected BasicTiledImageLayer.DownloadPostProcessor createBulkDownloadPostProcessor(TextureTile tile) {
        return new BulkDownloadPostProcessor(tile, this.layer, this.fileStore);
    }

    protected void callRetrievalListeners(Retriever retriever, TextureTile tile) {
        String eventType = (retriever.getState().equals(Retriever.RETRIEVER_STATE_SUCCESSFUL))
            ? BulkRetrievalEvent.RETRIEVAL_SUCCEEDED : BulkRetrievalEvent.RETRIEVAL_FAILED;
        super.callRetrievalListeners(new BulkRetrievalEvent(this.layer, eventType, tile.getPath()));
    }

    protected synchronized void removeRetrievedTile(TextureTile tile) {
        this.missingTiles.remove(tile);
        // Update progress
        this.progress.setCurrentCount(this.progress.getCurrentCount() + 1);
        this.progress.setCurrentSize(this.progress.getCurrentSize() + estimateAverageTileSize());
        this.progress.setLastUpdateTime(System.currentTimeMillis());
        this.normalizeProgress();
    }

    protected synchronized void removeAbsentTile(TextureTile tile) {
        this.missingTiles.remove(tile);
        // Decrease progress expected total count and size
        this.progress.setTotalCount(this.progress.getTotalCount() - 1);
        this.progress.setTotalSize(this.progress.getTotalSize() - estimateAverageTileSize());
        this.progress.setLastUpdateTime(System.currentTimeMillis());
        this.normalizeProgress();
    }

    protected void normalizeProgress() {
        if (this.progress.getTotalCount() < this.progress.getCurrentCount()) {
            this.progress.setTotalCount(this.progress.getCurrentCount());
            this.progress.setTotalSize(this.progress.getCurrentSize());
        }
    }

    /**
     * Get the estimated size in byte of the missing imagery for the object's {@link Sector}, resolution and file store.
     * Note that the target resolution must be provided in radian latitude per texel - which is the resolution in meter
     * divided by the globe radius.
     *
     * @return the estimated size in byte of the missing imagery.
     */
    protected long getEstimatedMissingDataSize() {
        // Get missing tiles count estimate
        long totMissing = estimateMissingTilesCount(6);
        // Get average tile size estimate
        long averageTileSize = estimateAverageTileSize();

        return totMissing * averageTileSize;
    }

    protected long estimateMissingTilesCount(int numSamples) {
        int maxLevel = this.layer.computeLevelForResolution(this.sector, this.resolution);
        // Total expected tiles
        long totCount = 0;
        for (int levelNumber = 0; levelNumber <= maxLevel; levelNumber++) {
            if (!this.layer.getLevels().isLevelEmpty(levelNumber))
                totCount += this.layer.countImagesInSector(sector, levelNumber);
        }
        // Sample random small sized sectors at finest level
        int div = this.computeRegionDivisions(this.sector, maxLevel, 36); // max 6x6 tiles per region
        Sector[] regions = BasicTiledImageLayerBulkDownloader.computeRandomRegions(this.sector, div, numSamples);
        long regionMissing = 0;
        long regionCount = 0;
        try {
            if (regions.length < numSamples) {
                regionCount = this.layer.countImagesInSector(this.sector, maxLevel);
                regionMissing = getMissingTilesInSector(this.sector, maxLevel).size();
            } else {
                for (Sector region : regions) {
                    // Count how many tiles are missing in each sample region
                    regionCount += this.layer.countImagesInSector(region, maxLevel);
                    regionMissing += getMissingTilesInSector(region, maxLevel).size();
                }
            }
        }
        catch (InterruptedException e) {
            return 0;
        }
        catch (RuntimeException e) {
            String message = Logging.getMessage("generic.ExceptionDuringDataSizeEstimate", this.layer.name());
            Logging.logger().severe(message);
            throw new RuntimeException(message);
        }

        // Extrapolate total missing count
        return (long) (totCount * ((double) regionMissing / regionCount));
    }

    protected int computeRegionDivisions(Sector sector, int levelNumber, int maxCount) {
        long tileCount = this.layer.countImagesInSector(sector, levelNumber);

        if (tileCount <= maxCount)
            return 1;

        // Divide sector in regions that will contain no more tiles then maxCount
        return (int) Math.ceil(Math.sqrt((double) tileCount / maxCount));
    }

    protected ArrayList<TextureTile> getMissingTilesInSector(Sector sector, int levelNumber)
        throws InterruptedException {
        ArrayList<TextureTile> tiles = new ArrayList<>();

        TextureTile[][] tileArray = this.layer.getTilesInSector(sector, levelNumber);
        for (TextureTile[] row : tileArray) {
            for (TextureTile tile : row) {
                Thread.sleep(1); // generates InterruptedException if thread has been interrupted

                if (tile == null)
                    continue;

                if (isTileLocalOrAbsent(tile))
                    continue;  // tile is local or absent

                tiles.add(tile);
            }
        }
        return tiles;
    }

    protected boolean isTileLocalOrAbsent(TextureTile tile) {
        if (this.layer.getLevels().missing(tile))
            return true;  // tile is absent

        URL url = this.fileStore.findFile(tile.getPath(), false);

        return url != null && !BasicTiledImageLayer.isTextureFileExpired(tile, url, fileStore);
    }

    protected long estimateAverageTileSize() {
        Long previouslyComputedSize = (Long) this.layer.get(AVKey.AVERAGE_TILE_SIZE);
        if (previouslyComputedSize != null)
            return previouslyComputedSize;

        long size = 0;
        long count = 0;

        // Average cached tile files size in a few directories from first non empty level
        Level targetLevel = this.layer.getLevels().getFirstLevel();
        while (targetLevel.isEmpty() && !targetLevel.equals(this.layer.getLevels().getLastLevel())) {
            targetLevel = this.layer.getLevels().getLevel(targetLevel.getLevelNumber() + 1);
        }

        File cacheRoot = new File(this.fileStore.getWriteLocation(), targetLevel.getPath());
        if (cacheRoot.exists()) {
            File[] rowDirs = cacheRoot.listFiles(File::isDirectory);
            for (File dir : rowDirs) {
                long averageSize = BasicTiledImageLayerBulkDownloader.computeAverageTileSize(dir);
                if (averageSize > 0) {
                    size += averageSize;
                    count++;
                }
                if (count >= 2) // average content from up to 2 cache folders
                    break;
            }
        }

        long averageTileSize = BasicTiledImageLayerBulkDownloader.DEFAULT_AVERAGE_FILE_SIZE;
        if (count > 0 && size > 0) {
            averageTileSize = size / count;
            this.layer.set(AVKey.AVERAGE_TILE_SIZE, averageTileSize);
        }

        return averageTileSize;
    }

    protected class BulkDownloadPostProcessor extends BasicTiledImageLayer.DownloadPostProcessor {
        public BulkDownloadPostProcessor(TextureTile tile, BasicTiledImageLayer layer, FileStore fileStore) {
            super(tile, layer, fileStore);
        }

        public ByteBuffer apply(Retriever retriever) {
            ByteBuffer buffer = super.apply(retriever);

            if (retriever.getState().equals(Retriever.RETRIEVER_STATE_SUCCESSFUL))
                removeRetrievedTile(this.tile);

            if (hasRetrievalListeners())
                callRetrievalListeners(retriever, this.tile);

            return buffer;
        }
    }
}
