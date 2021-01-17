/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.util;

import gov.nasa.worldwind.WWObjectImpl;
import gov.nasa.worldwind.avlist.*;
import gov.nasa.worldwind.geom.*;

import java.net.URL;
import java.util.*;

/**
 * @author tag
 * @version $Id: LevelSet.java 2060 2014-06-18 03:19:17Z tgaskins $
 */
public class LevelSet extends WWObjectImpl {
    public final Sector sector;
    public final LatLon levelZeroTileDelta;
    public final LatLon tileOrigin;
    public final int numLevelZeroColumns;
    private final ArrayList<Level> levels = new ArrayList<>();
    private final SectorResolution[] sectorLevelLimits;

    public LevelSet(AVList params) {
        StringBuilder sb = new StringBuilder();

        Object o = params.get(AVKey.LEVEL_ZERO_TILE_DELTA);
        if (!(o instanceof LatLon))
            sb.append(Logging.getMessage("term.tileDelta")).append(' ');

        o = params.get(AVKey.SECTOR);
        if (!(o instanceof Sector))
            sb.append(Logging.getMessage("term.sector")).append(' ');

        int numLevels = 0;
        o = params.get(AVKey.NUM_LEVELS);
        if (!(o instanceof Integer) || (numLevels = (Integer) o) < 1)
            sb.append(Logging.getMessage("term.numLevels")).append(' ');

        int numEmptyLevels = 0;
        o = params.get(AVKey.NUM_EMPTY_LEVELS);
        if (o instanceof Integer && (Integer) o > 0)
            numEmptyLevels = (Integer) o;

        String[] inactiveLevels = null;
        o = params.get(AVKey.INACTIVE_LEVELS);
        if (o != null && !(o instanceof String))
            sb.append(Logging.getMessage("term.inactiveLevels")).append(' ');
        else if (o != null)
            inactiveLevels = ((String) o).split(",");

        SectorResolution[] sectorLimits = null;
        o = params.get(AVKey.SECTOR_RESOLUTION_LIMITS);
        if (o != null && !(o instanceof SectorResolution[])) {
            sb.append(Logging.getMessage("term.sectorResolutionLimits")).append(' ');
        } else if (o != null) {
            sectorLimits = (SectorResolution[]) o;
            for (SectorResolution sr : sectorLimits) {
                if (sr.levelNumber > numLevels - 1) {
                    String message =
                        Logging.getMessage("LevelSet.sectorResolutionLimitsTooHigh", sr.levelNumber, numLevels - 1);
                    Logging.logger().warning(message);
                    break;
                }
            }
        }
        this.sectorLevelLimits = sectorLimits;

        if (!sb.isEmpty()) {
            String message = Logging.getMessage("layers.LevelSet.InvalidLevelDescriptorFields", sb.toString());
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.sector = (Sector) params.get(AVKey.SECTOR);
        this.levelZeroTileDelta = (LatLon) params.get(AVKey.LEVEL_ZERO_TILE_DELTA);

        o = params.get(AVKey.TILE_ORIGIN);
        if (o instanceof LatLon)
            this.tileOrigin = (LatLon) o;
        else
            this.tileOrigin = new LatLon(Angle.NEG90, Angle.NEG180);

        params = params.copy(); // copy so as not to modify the user's params

        TileUrlBuilder tub = (TileUrlBuilder) params.get(AVKey.TILE_URL_BUILDER);
        if (tub == null) {
            params.set(AVKey.TILE_URL_BUILDER, (TileUrlBuilder) (tile, altImageFormat) -> {
                String service = tile.level.getService();
                if (service == null || service.length() < 1)
                    return null;

                StringBuilder sb1 = new StringBuilder(tile.level.getService());
                if (sb1.lastIndexOf("?") != sb1.length() - 1)
                    sb1.append('?');
                sb1.append("T=");
                sb1.append(tile.level.getDataset());
                sb1.append("&L=");
                sb1.append(tile.level.getLevelName());
                sb1.append("&X=");
                sb1.append(tile.col);
                sb1.append("&Y=");
                sb1.append(tile.row);

                // Convention for NASA WWN tiles is to request them with common dataset name but without dds.
                return new URL(altImageFormat == null ? sb1.toString() : sb1.toString().replace("dds", ""));
            });
        }

        if (this.sectorLevelLimits != null) {
            Arrays.sort(this.sectorLevelLimits, (sra, srb) -> {
                // sort order is deliberately backwards in order to list higher-resolution sectors first
                return Integer.compare(srb.levelNumber, sra.levelNumber);
            });
        }

        // Compute the number of level zero columns. This value is guaranteed to be a nonzero number, since there is
        // generally at least one level zero tile.
        int firstLevelZeroCol = Tile.computeColumn(this.levelZeroTileDelta.getLongitude(),
            this.sector.lonMin(), this.tileOrigin.getLongitude());
        int lastLevelZeroCol = Tile.computeColumn(this.levelZeroTileDelta.getLongitude(), this.sector.lonMax(),
            this.tileOrigin.getLongitude());
        this.numLevelZeroColumns = Math.max(1, lastLevelZeroCol - firstLevelZeroCol + 1);

        for (int i = 0; i < numLevels; i++) {
            params.set(AVKey.LEVEL_NAME, i < numEmptyLevels ? "" : Integer.toString(i - numEmptyLevels));
            params.set(AVKey.LEVEL_NUMBER, i);

            Angle latDelta = this.levelZeroTileDelta.getLatitude().divide(Math.pow(2, i));
            Angle lonDelta = this.levelZeroTileDelta.getLongitude().divide(Math.pow(2, i));
            params.set(AVKey.TILE_DELTA, new LatLon(latDelta, lonDelta));

            this.levels.add(new Level(params));
        }

        if (inactiveLevels != null) {
            for (String s : inactiveLevels) {
                int i = Integer.parseInt(s);
                this.getLevel(i).setActive(false);
            }
        }
    }

    public LevelSet(LevelSet source) {
        if (source == null) {
            String msg = Logging.getMessage("nullValue.LevelSetIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        this.sector = source.sector;
        this.levelZeroTileDelta = source.levelZeroTileDelta;
        this.tileOrigin = source.tileOrigin;
        this.numLevelZeroColumns = source.numLevelZeroColumns;
        this.sectorLevelLimits = source.sectorLevelLimits;

        // Levels are final, so it's safe to copy references.
        this.levels.addAll(source.levels);
    }

    @Override
    public Object set(String key, Object value) {
        // Propogate the setting to all levels
        for (Level level : this.levels) {
            level.set(key, value);
        }

        return super.set(key, value);
    }

    @Override
    public Object get(String key) {
        Object value = super.get(key);

        if (value != null)
            return value;

        // See if any level has it
        for (Level level : this.getLevels()) {
            if (level != null && (value = level.get(key)) != null)
                return value;
        }

        return null;
    }

    public final SectorResolution[] getSectorLevelLimits() {
        if (this.sectorLevelLimits == null)
            return null;

        // The SectorResolution instances themselves are immutable. However the entries in a Java array cannot be made
        // immutable, therefore we create a copy to insulate ourselves from changes by the caller.
        SectorResolution[] copy = new SectorResolution[this.sectorLevelLimits.length];
        System.arraycopy(this.sectorLevelLimits, 0, copy, 0, this.sectorLevelLimits.length);

        return copy;
    }

    public final ArrayList<Level> getLevels() {
        return this.levels;
    }

    public final Level getLevel(int levelNumber) {
        return (levelNumber >= 0 && levelNumber < this.levels.size()) ? this.levels.get(levelNumber) : null;
    }

    public final int getNumLevels() {
        return this.levels.size();
    }

    public final Level getFirstLevel() {
        return this.getLevel(0);
    }

    public final Level getLastLevel() {
        return this.getLevel(this.getNumLevels() - 1);
    }

    public final Level getNextToLastLevel() {
        return this.getLevel(this.getNumLevels() > 1 ? this.getNumLevels() - 2 : 0);
    }

    public final Level getLastLevel(Sector sector) {
        if (sector == null) {
            String msg = Logging.getMessage("nullValue.SectorIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        if (!this.sector.intersects(sector))
            return null;

        Level level = this.getLevel(this.getNumLevels() - 1);

        if (this.sectorLevelLimits != null)
            for (SectorResolution sr : this.sectorLevelLimits) {
                if (sr.sector.intersects(sector) && sr.levelNumber <= level.getLevelNumber()) {
                    level = this.getLevel(sr.levelNumber);
                    break;
                }
            }

        return level;
    }

    public final Level getLastLevel(Angle latitude, Angle longitude) {
        Level level = this.getLevel(this.getNumLevels() - 1);

        if (this.sectorLevelLimits != null)
            for (SectorResolution sr : this.sectorLevelLimits) {
                if (sr.sector.contains(latitude, longitude) && sr.levelNumber <= level.getLevelNumber()) {
                    level = this.getLevel(sr.levelNumber);
                    break;
                }
            }

        return level;
    }

    public final boolean isFinalLevel(int levelNum) {
        return levelNum == this.getNumLevels() - 1;
    }

    public final boolean isLevelEmpty(int levelNumber) {
        return this.levels.get(levelNumber).isEmpty();
    }

    private int numColumnsInLevel(Level level) {
        int levelDelta = level.getLevelNumber() - this.getFirstLevel().getLevelNumber();
        double twoToTheN = Math.pow(2, levelDelta);
        return (int) (twoToTheN * this.numLevelZeroColumns);
    }

    private long getTileNumber(Tile tile) {
        return tile.row < 0 ? -1 : (long) tile.row * this.numColumnsInLevel(tile.level) + tile.col;
    }

    private long getTileNumber(TileKey tileKey) {
        return tileKey.row < 0 ? -1 :
            (long) tileKey.row * this.numColumnsInLevel(this.getLevel(tileKey.level))
                + tileKey.col;
    }

    /**
     * Instructs the level set that a tile is likely to be absent.
     *
     * @param tile The tile to mark as having an absent resource.
     * @throws IllegalArgumentException if <code>tile</code> is null
     */
    public final void miss(Tile tile) {

        tile.level.markResourceAbsent(this.getTileNumber(tile));
    }

    /**
     * Indicates whether a tile has been marked as absent.
     *
     * @param tileKey The key of the tile in question.
     * @return <code>true</code> if the tile is marked absent, otherwise <code>false</code>.
     * @throws IllegalArgumentException if <code>tile</code> is null
     */
    public final boolean missing(TileKey tileKey) {

        Level level = this.getLevel(tileKey.level);
        return level.isEmpty() || level.isResourceAbsent(this.getTileNumber(tileKey));
    }

    /**
     * Indicates whether a tile has been marked as absent.
     *
     * @param tile The tile in question.
     * @return <code>true</code> if the tile is marked absent, otherwise <code>false</code>.
     * @throws IllegalArgumentException if <code>tile</code> is null
     */
    public final boolean missing(Tile tile) {

        return tile.level.isEmpty() || tile.level.isResourceAbsent(this.getTileNumber(tile));
    }

    /**
     * Removes the absent-tile mark associated with a tile, if one is associatied.
     *
     * @param tile The tile to unmark.
     * @throws IllegalArgumentException if <code>tile</code> is null
     */
    public final void has(Tile tile) {

        tile.level.unmarkResourceAbsent(this.getTileNumber(tile));
    }

    // Create the tile corresponding to a specified key.
    public Sector computeSectorForKey(TileKey key) {

        Level level = this.getLevel(key.level);

        // Compute the tile's SW lat/lon based on its row/col in the level's data set.
        Angle dLat = level.getTileDelta().getLatitude();
        Angle dLon = level.getTileDelta().getLongitude();
        Angle latOrigin = this.tileOrigin.getLatitude();
        Angle lonOrigin = this.tileOrigin.getLongitude();

        Angle minLatitude = Tile.rowLat(key.row, dLat, latOrigin);
        Angle minLongitude = Tile.columnLon(key.col, dLon, lonOrigin);

        return new Sector(minLatitude, minLatitude.add(dLat), minLongitude, minLongitude.add(dLon));
    }

    public void setExpiryTime(long expiryTime) {
        for (Level level : this.levels) {
            level.setExpiryTime(expiryTime);
        }
    }

    public static final class SectorResolution {
        private final int levelNumber;
        private final Sector sector;

        public SectorResolution(Sector sector, int levelNumber) {
            this.levelNumber = levelNumber;
            this.sector = sector;
        }

        public final int getLevelNumber() {
            return this.levelNumber;
        }

        public final Sector getSector() {
            return this.sector;
        }
    }
}
