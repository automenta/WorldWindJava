/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.util;

import gov.nasa.worldwind.Keys;
import gov.nasa.worldwind.avlist.*;
import gov.nasa.worldwind.geom.*;

import java.net.*;
import java.util.Objects;

/**
 * @author tag
 * @version $Id: Level.java 1171 2013-02-11 21:45:02Z dcollins $
 */
public class Level extends KVMap implements Comparable<Level> {
    static final int DEFAULT_MAX_ABSENT_TILE_ATTEMPTS = 2;
    static final int DEFAULT_MIN_ABSENT_TILE_CHECK_INTERVAL = 10000; // milliseconds
    protected KV params;
    protected int levelNumber;
    protected String levelName; // null or empty level name signifies no data resources associated with this level
    protected LatLon tileDelta;
    protected int tileWidth;
    protected int tileHeight;
    protected String cacheName;
    protected String service;
    protected String dataset;
    protected String formatSuffix;
    protected double texelSize;
    protected String path;
    protected TileUrlBuilder urlBuilder;
    protected long expiryTime;
    protected boolean active = true;
    // Absent tiles: A tile is deemed absent if a specified maximum number of attempts have been made to retrieve it.
    // Retrieval attempts are governed by a minimum time interval between successive attempts. If an attempt is made
    // within this interval, the tile is still deemed to be absent until the interval expires.
    protected AbsentResourceList absentTiles;

    public Level(KV params) {
//        if (params == null) {
//            String message = Logging.getMessage("nullValue.LevelConfigParams");
//            Logging.logger().severe(message);
//            throw new IllegalArgumentException(message);
//        }

        this.params = params.copy(); // Private copy to insulate from subsequent changes by the app
        String message = Level.validate(params);
        if (message != null) {
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        String ln = this.params.getStringValue(Keys.LEVEL_NAME);
        this.levelName = ln != null ? ln : "";

        this.levelNumber = (Integer) this.params.get(Keys.LEVEL_NUMBER);
        this.tileDelta = (LatLon) this.params.get(Keys.TILE_DELTA);
        this.tileWidth = (Integer) this.params.get(Keys.TILE_WIDTH);
        this.tileHeight = (Integer) this.params.get(Keys.TILE_HEIGHT);
        this.cacheName = this.params.getStringValue(Keys.DATA_CACHE_NAME);
        this.service = this.params.getStringValue(Keys.SERVICE);
        this.dataset = this.params.getStringValue(Keys.DATASET_NAME);
        this.formatSuffix = this.params.getStringValue(Keys.FORMAT_SUFFIX);
        this.urlBuilder = (TileUrlBuilder) this.params.get(Keys.TILE_URL_BUILDER);
        this.expiryTime = KVMap.getLongValue(params, Keys.EXPIRY_TIME, 0L);

        this.texelSize = this.tileDelta.getLatitude().radians() / this.tileHeight;

        this.path = this.cacheName + '/' + this.levelName;

        Integer maxAbsentTileAttempts = (Integer) this.params.get(Keys.MAX_ABSENT_TILE_ATTEMPTS);
        if (maxAbsentTileAttempts == null)
            maxAbsentTileAttempts = Level.DEFAULT_MAX_ABSENT_TILE_ATTEMPTS;

        Integer minAbsentTileCheckInterval = (Integer) this.params.get(Keys.MIN_ABSENT_TILE_CHECK_INTERVAL);
        if (minAbsentTileCheckInterval == null)
            minAbsentTileCheckInterval = Level.DEFAULT_MIN_ABSENT_TILE_CHECK_INTERVAL;

        this.absentTiles = new AbsentResourceList(maxAbsentTileAttempts, minAbsentTileCheckInterval);
    }

    /**
     * Determines whether the constructor arguments are valid.
     *
     * @param params the list of parameters to validate.
     * @return null if valid, otherwise a <code>String</code> containing a description of why it's invalid.
     */
    protected static String validate(KV params) {
        StringBuilder sb = new StringBuilder();

        Object o = params.get(Keys.LEVEL_NUMBER);
        if (!(o instanceof Integer) || ((Integer) o) < 0)
            sb.append(Logging.getMessage("term.levelNumber")).append(' ');

        o = params.get(Keys.LEVEL_NAME);
        if (!(o instanceof String))
            sb.append(Logging.getMessage("term.levelName")).append(' ');

        o = params.get(Keys.TILE_WIDTH);
        if (!(o instanceof Integer) || ((Integer) o) < 0)
            sb.append(Logging.getMessage("term.tileWidth")).append(' ');

        o = params.get(Keys.TILE_HEIGHT);
        if (!(o instanceof Integer) || ((Integer) o) < 0)
            sb.append(Logging.getMessage("term.tileHeight")).append(' ');

        o = params.get(Keys.TILE_DELTA);
        if (!(o instanceof LatLon))
            sb.append(Logging.getMessage("term.tileDelta")).append(' ');

        o = params.get(Keys.DATA_CACHE_NAME);
        if (!(o instanceof String) || ((CharSequence) o).length() < 1)
            sb.append(Logging.getMessage("term.fileStoreFolder")).append(' ');

        o = params.get(Keys.TILE_URL_BUILDER);
        if (!(o instanceof TileUrlBuilder))
            sb.append(Logging.getMessage("term.tileURLBuilder")).append(' ');

        o = params.get(Keys.EXPIRY_TIME);
        if (o != null && (!(o instanceof Long) || ((Long) o) < 1))
            sb.append(Logging.getMessage("term.expiryTime")).append(' ');

        if (!params.getStringValue(Keys.LEVEL_NAME).isEmpty()) {
            o = params.get(Keys.DATASET_NAME);
            if (!(o instanceof String) || ((CharSequence) o).length() < 1)
                sb.append(Logging.getMessage("term.datasetName")).append(' ');

            o = params.get(Keys.FORMAT_SUFFIX);
            if (!(o instanceof String) || ((CharSequence) o).length() < 1)
                sb.append(Logging.getMessage("term.formatSuffix")).append(' ');
        }

        if (sb.isEmpty())
            return null;

        return Logging.getMessage("layers.LevelSet.InvalidLevelDescriptorFields", sb.toString());
    }

    public String getPath() {
        return this.path;
    }

    public int getLevelNumber() {
        return this.levelNumber;
    }

    public String getLevelName() {
        return this.levelName;
    }

    public LatLon getTileDelta() {
        return this.tileDelta;
    }

    public int getTileWidth() {
        return this.tileWidth;
    }

    public int getTileHeight() {
        return this.tileHeight;
    }

    public String getFormatSuffix() {
        return this.formatSuffix;
    }

    public String getService() {
        return this.service;
    }

    public String getDataset() {
        return this.dataset;
    }

    public String getCacheName() {
        return this.cacheName;
    }

    public double getTexelSize() {
        return this.texelSize;
    }

    public boolean isEmpty() {
        return this.levelName == null || this.levelName.isEmpty() || !this.active;
    }

    public void markResourceAbsent(long tileNumber) {
        if (tileNumber >= 0)
            this.absentTiles.markResourceAbsent(tileNumber);
    }

    public boolean isResourceAbsent(long tileNumber) {
        return this.absentTiles.isResourceAbsent(tileNumber);
    }

    public void unmarkResourceAbsent(long tileNumber) {
        if (tileNumber >= 0)
            this.absentTiles.unmarkResourceAbsent(tileNumber);
    }

    public long getExpiryTime() {
        return this.expiryTime;
    }

    public void setExpiryTime(long expTime) {
        this.expiryTime = expTime;
    }

    public boolean isActive() {
        return this.active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public Object set(String key, Object value) {
        if (key != null && key.equals(Keys.MAX_ABSENT_TILE_ATTEMPTS) && value instanceof Integer)
            this.absentTiles.setMaxTries((Integer) value);
        else if (key != null && key.equals(Keys.MIN_ABSENT_TILE_CHECK_INTERVAL) && value instanceof Integer)
            this.absentTiles.setMinCheckInterval((Integer) value);

        return super.set(key, value);
    }

    @Override
    public Object get(String key) {
        if (key != null && key.equals(Keys.MAX_ABSENT_TILE_ATTEMPTS))
            return this.absentTiles.getMaxTries();
        else if (key != null && key.equals(Keys.MIN_ABSENT_TILE_CHECK_INTERVAL))
            return this.absentTiles.getMinCheckInterval();

        return super.get(key);
    }

    /**
     * Returns the URL necessary to retrieve the specified tile.
     *
     * @param tile        the tile who's resources will be retrieved.
     * @param imageFormat a string identifying the mime type of the desired image format
     * @return the resource URL.
     * @throws MalformedURLException    if the URL cannot be formed from the tile's parameters.
     * @throws IllegalArgumentException if <code>tile</code> is null.
     */
    public URL getTileResourceURL(Tile tile, String imageFormat) throws MalformedURLException {

        return this.urlBuilder.getURL(tile, imageFormat);
    }

    public Sector computeSectorForPosition(Angle latitude, Angle longitude, LatLon tileOrigin) {
//        if (latitude == null || longitude == null) {
//            String message = Logging.getMessage("nullValue.LatLonIsNull");
//            Logging.logger().severe(message);
//            throw new IllegalArgumentException(message);
//        }
//        if (tileOrigin == null) {
//            String message = Logging.getMessage("nullValue.TileOriginIsNull");
//            Logging.logger().severe(message);
//            throw new IllegalArgumentException(message);
//        }

        // Compute the tile's SW lat/lon based on its row/col in the level's data set.
        Angle dLat = this.getTileDelta().getLatitude();
        Angle dLon = this.getTileDelta().getLongitude();
        Angle latOrigin = tileOrigin.getLatitude();
        Angle lonOrigin = tileOrigin.getLongitude();

        int row = Tile.computeRow(dLat, latitude, latOrigin);
        int col = Tile.computeColumn(dLon, longitude, lonOrigin);
        Angle minLatitude = Tile.rowLat(row, dLat, latOrigin);
        Angle minLongitude = Tile.columnLon(col, dLon, lonOrigin);

        return new Sector(minLatitude, minLatitude.add(dLat), minLongitude, minLongitude.add(dLon));
    }

    public int compareTo(Level that) {
        if (this == that)
            return 0;
        int l = Integer.compare(this.levelNumber, that.levelNumber);
        if (l == 0)
            return path.compareTo(that.path);
        else
            return l;
    }

    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        final Level level = (Level) o;

        if (levelNumber != level.levelNumber)
            return false;
        if (tileHeight != level.tileHeight)
            return false;
        if (tileWidth != level.tileWidth)
            return false;
        if (!Objects.equals(cacheName, level.cacheName))
            return false;
        if (!Objects.equals(dataset, level.dataset))
            return false;
        if (!Objects.equals(formatSuffix, level.formatSuffix))
            return false;
        if (!Objects.equals(levelName, level.levelName))
            return false;
        if (!Objects.equals(service, level.service))
            return false;
        //noinspection RedundantIfStatement
        if (!Objects.equals(tileDelta, level.tileDelta))
            return false;

        return true;
    }

    public int hashCode() {
        int result;
        result = levelNumber;
        result = 29 * result + (levelName != null ? levelName.hashCode() : 0);
        result = 29 * result + (tileDelta != null ? tileDelta.hashCode() : 0);
        result = 29 * result + tileWidth;
        result = 29 * result + tileHeight;
        result = 29 * result + (formatSuffix != null ? formatSuffix.hashCode() : 0);
        result = 29 * result + (service != null ? service.hashCode() : 0);
        result = 29 * result + (dataset != null ? dataset.hashCode() : 0);
        result = 29 * result + (cacheName != null ? cacheName.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return this.path;
    }
}