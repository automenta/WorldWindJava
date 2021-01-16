/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.util;

import gov.nasa.worldwind.cache.Cacheable;
import gov.nasa.worldwind.geom.*;

import java.net.*;
import java.util.Objects;

/**
 * Large images and most imagery and elevation-data sets are subdivided in order to display visible portions quickly and
 * without excessive memory usage. Each subdivision is called a tile, and a collections of adjacent tiles corresponding
 * to a common spatial resolution is typically maintained in a {@link Level}. A collection of levels of progressive
 * resolutions are maintained in a {@link LevelSet}. The <code>Tile</code> class represents a single tile of a
 * subdivided image or elevation raster.
 * <p>
 * Individual tiles are identified by the level, row and column of the tile within its containing level set.
 *
 * @author tag
 * @version $Id: Tile.java 1171 2013-02-11 21:45:02Z dcollins $
 */
public class Tile implements Comparable<Tile>, Cacheable {
    public final Sector sector;
    public final Level level;
    public final int row;
    public final int col;
    /**
     * An optional cache name. Overrides the Level's cache name when non-null.
     */
    public final String cacheName;
    public final TileKey key;
    private double priority = Double.NaN;
    // The following is late bound because it's only selectively needed and costly to create
    private String path;

    /**
     * Constructs a tile for a given sector, level, row and column of the tile's containing tile set.
     *
     * @param sector the sector corresponding with the tile.
     * @param level  the tile's level within a containing level set.
     * @param row    the row index (0 origin) of the tile within the indicated level.
     * @param col    the column index (0 origin) of the tile within the indicated level.
     * @throws IllegalArgumentException if <code>sector</code> or <code>level</code> is null.
     */
    public Tile(Sector sector, Level level, int row, int col) {

        this.sector = sector;
        this.level = level;
        this.row = row;
        this.col = col;
        this.cacheName = null;
        this.key = new TileKey(this);
        this.path = null;
    }

    /**
     * Constructs a tile for a given sector, level, row and column of the tile's containing tile set. If the cache name
     * is non-null, it overrides the level's cache name and is returned by {@link #getCacheName()}. Otherwise, the
     * level's cache name is used.
     *
     * @param sector    the sector corresponding with the tile.
     * @param level     the tile's level within a containing level set.
     * @param row       the row index (0 origin) of the tile within the indicated level.
     * @param col       the column index (0 origin) of the tile within the indicated level.
     * @param cacheName optional cache name to override the Level's cache name. May be null.
     * @throws IllegalArgumentException if <code>sector</code> or <code>level</code> is null.
     */
    public Tile(Sector sector, Level level, int row, int col, String cacheName) {

        this.sector = sector;
        this.level = level;
        this.row = row;
        this.col = col;
        this.cacheName = cacheName;
        this.key = new TileKey(this);
        this.path = null;
    }

    /**
     * Constructs a texture tile for a given sector and level, and with a default row and column.
     *
     * @param sector the sector to create the tile for.
     * @param level  the level to associate the tile with
     * @throws IllegalArgumentException if sector or level are null.
     */
    public Tile(Sector sector, Level level) {

        this.sector = sector;
        this.level = level;
        this.row = Tile.computeRow(sector.latDelta(), sector.latMin(), Angle.NEG90);
        this.col = Tile.computeColumn(sector.lonDelta(), sector.lonMin(), Angle.NEG180);
        this.cacheName = null;
        this.key = new TileKey(this);
        this.path = null;
    }

    /**
     * Computes the row index of a latitude in the global tile grid corresponding to a specified grid interval.
     *
     * @param delta    the grid interval
     * @param latitude the latitude for which to compute the row index
     * @param origin   the origin of the grid
     * @return the row index of the row containing the specified latitude
     * @throws IllegalArgumentException if <code>delta</code> is null or non-positive, or <code>latitude</code> is null,
     *                                  greater than positive 90 degrees, or less than  negative 90 degrees
     */
    @Deprecated
    public static int computeRow(Angle delta, Angle latitude, Angle origin) {
        return Tile.computeRow(delta.degrees, latitude.degrees, origin.degrees);
    }

    /**
     * in degrees
     */
    public static int computeRow(double delta, double latitude, double origin) {

        Tile.assertPositiveDelta(delta);

        if (latitude < -90.0d || latitude > 90.0d) {
            String message = Logging.getMessage("generic.AngleOutOfRange", latitude);
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        int row = (int) ((latitude - origin) / delta);
        // Latitude is at the end of the grid. Subtract 1 from the computed row to return the last row.
        if ((latitude - origin) == 180.0d)
            row = row - 1;

        return row;
    }

    /**
     * Computes the column index of a longitude in the global tile grid corresponding to a specified grid interval.
     *
     * @param delta     the grid interval
     * @param longitude the longitude for which to compute the column index
     * @param origin    the origin of the grid
     * @return the column index of the column containing the specified latitude
     * @throws IllegalArgumentException if <code>delta</code> is null or non-positive, or <code>longitude</code> is
     *                                  null, greater than positive 180 degrees, or less than  negative 180 degrees
     */
    @Deprecated
    public static int computeColumn(Angle delta, Angle longitude, Angle origin) {
        return Tile.computeColumn(delta.degrees, longitude.degrees, origin.degrees);
    }

    /**
     * in degrees
     */
    public static int computeColumn(double delta, double longitude, double origin) {

        Tile.assertPositiveDelta(delta);

        if (longitude < -180.0d || longitude > 180.0d) {
            String message = Logging.getMessage("generic.AngleOutOfRange", longitude);
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        // Compute the longitude relative to the grid. The grid provides 360 degrees of longitude from the grid origin.
        // We wrap grid longitude values so that the grid begins and ends at the origin.
        double gridLongitude = longitude - origin;
        if (gridLongitude < 0.0)
            gridLongitude = 360.0d + gridLongitude;

        int col = (int) (gridLongitude / delta);
        // Longitude is at the end of the grid. Subtract 1 from the computed column to return the last column.
        if ((longitude - origin) == 360.0d)
            col = col - 1;

        return col;
    }

    /**
     * Determines the minimum latitude of a row in the global tile grid corresponding to a specified grid interval.
     *
     * @param row    the row index of the row in question
     * @param delta  the grid interval
     * @param origin the origin of the grid
     * @return the minimum latitude of the tile corresponding to the specified row
     * @throws IllegalArgumentException if the grid interval (<code>delta</code>) is null or zero or the row index is
     *                                  negative.
     */
    public static Angle rowLat(int row, Angle delta, Angle origin) {
        Tile.assertPositiveRow(row);

        Tile.assertPositiveDelta(delta);

        double latDegrees = origin.degrees + (row * delta.degrees);
        return new Angle(latDegrees);
    }

    private static void assertPositiveRow(int row) {
        if (row < 0) {
            String msg = Logging.getMessage("generic.RowIndexOutOfRange", row);
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }
    }

    /**
     * Determines the minimum longitude of a column in the global tile grid corresponding to a specified grid interval.
     *
     * @param column the row index of the row in question
     * @param delta  the grid interval
     * @param origin the origin of the grid
     * @return the minimum longitude of the tile corresponding to the specified column
     * @throws IllegalArgumentException if the grid interval (<code>delta</code>) is null or zero or the column index is
     *                                  negative.
     */
    public static Angle columnLon(int column, Angle delta, Angle origin) {
        if (column < 0) {
            String msg = Logging.getMessage("generic.ColumnIndexOutOfRange", column);
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        Tile.assertPositiveDelta(delta);

        double lonDegrees = origin.degrees + (column * delta.degrees);
        return new Angle(lonDegrees);
    }

    @Deprecated
    private static void assertPositiveDelta(Angle delta) {
        Tile.assertPositiveDelta(delta.degrees);
    }

    private static void assertPositiveDelta(double deltaDegrees) {
        if (deltaDegrees <= 0.0d) {
            String message = Logging.getMessage("generic.DeltaAngleOutOfRange", deltaDegrees);
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
    }

    public long getSizeInBytes() {
        // Return just an approximate size
        long size = 0;

        if (this.sector != null)
            size += this.sector.getSizeInBytes();

        if (this.path != null)
            size += this.getPath().length();

        size += 32; // to account for the references and the TileKey size

        return size;
    }

    public String getPath() {
        if (this.path == null) {
            this.path = this.level.getPath() + '/' + this.row + '/' + this.row + '_' + this.col;
            if (!this.level.isEmpty())
                path += this.level.getFormatSuffix();
        }

        return this.path;
    }

    public String getPathBase() {
        String path = this.getPath();

        final int period = path.lastIndexOf('.');
        return period == -1 ? path : path.substring(0, period);
    }

    public final int getLevelNumber() {
        return this.level != null ? this.level.getLevelNumber() : 0;
    }

    public final String getLevelName() {
        return this.level != null ? this.level.getLevelName() : "";
    }

    /**
     * Returns the tile's cache name. If a non-null cache name was specified at construction, that name is returned.
     * Otherwise this returns the level's cache name.
     *
     * @return the tile's cache name.
     */
    public final String getCacheName() {
        if (this.cacheName != null)
            return this.cacheName;

        return this.level != null ? this.level.getCacheName() : null;
    }

    public final String getFormatSuffix() {
        return this.level != null ? this.level.getFormatSuffix() : null;
    }

    public URL getResourceURL() throws MalformedURLException {
        return this.level != null ? this.level.getTileResourceURL(this, null) : null;
    }

    public URL getResourceURL(String imageFormat) throws MalformedURLException {
        return this.level != null ? this.level.getTileResourceURL(this, imageFormat) : null;
    }

    public String getLabel() {
        StringBuilder sb = new StringBuilder(64);
        sb.append(getLevelNumber());
        sb.append('(');
        sb.append(getLevelName());
        sb.append("), ").append(row);
        sb.append(", ").append(col);
        return sb.toString();
    }

    public int getWidth() {
        return level.getTileWidth();
    }

    public int getHeight() {
        return level.getTileHeight();
    }

    public int compareTo(Tile tile) {
        if (this == tile)
            return 0;
//        if (tile == null) {
//            String msg = Logging.getMessage("nullValue.TileIsNull");
//            Logging.logger().severe(msg);
//            throw new IllegalArgumentException(msg);
//        }

        // No need to compare Sectors or path because they are redundant with row and column
        final int thisLevel = this.getLevelNumber();
        final int otherLevel = tile.getLevelNumber();

        if (thisLevel < otherLevel) // Lower-res levels compare lower than higher-res
            return -1;
        if (thisLevel > otherLevel)
            return 1;

        if (this.row < tile.row)
            return -1;
        if (this.row > tile.row)
            return 1;

        return Integer.compare(this.col, tile.col);
    }

    @Override
    public boolean equals(Object o) {
        // Equality based only on the tile key
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        final Tile tile = (Tile) o;

        return Objects.equals(key, tile.key);
    }

    @Override
    public int hashCode() {
        return (key != null ? key.hashCode() : 0);
    }

    @Override
    public String toString() {
        return this.getPath();
    }

    public double getPriority() {
        return priority;
    }

    public void setPriority(double priority) {
        this.priority = priority;
    }

    public void setPriorityDistance(double dist) {
        //setPriority(1.0 / (1.0 + dist));
        setPriority(dist);
    }
}
