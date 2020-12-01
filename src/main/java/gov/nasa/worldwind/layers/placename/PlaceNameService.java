/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.layers.placename;

import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.util.*;

import java.awt.*;
import java.io.File;
import java.net.*;
import java.util.Objects;

/**
 * @author Paul Collins
 * @version $Id: PlaceNameService.java 1171 2013-02-11 21:45:02Z dcollins $
 */
public class PlaceNameService {
    // Geospatial attributes.
    public static final Sector TILING_SECTOR = Sector.FULL_SPHERE;
    private static final String FORMAT_SUFFIX = ".xml.gz";
    private static final int MAX_ABSENT_TILE_TRIES = 2;
    private static final int MIN_ABSENT_TILE_CHECK_INTERVAL = 10000;
    // Data retrieval and caching attributes.
    private final String service;
    private final String dataset;
    private final String fileCachePath;
    private final LatLon tileDelta;
    // Display attributes.
    private final Font font;
    private final int numColumns;
    private final AbsentResourceList absentTiles = new AbsentResourceList(MAX_ABSENT_TILE_TRIES,
        MIN_ABSENT_TILE_CHECK_INTERVAL);
    private boolean enabled;
    private Color color;
    private Color backgroundColor;
    private double minDisplayDistance;
    private double maxDisplayDistance;
    private boolean addVersionTag = false;
    private Sector maskingSector = null;

    /**
     * PlaceNameService Constructor
     *
     * @param service       server hostong placename data
     * @param dataset       name of the dataset
     * @param fileCachePath location of cache
     * @param sector        sets the masking sector for this service.
     * @param tileDelta     tile size
     * @param font          font for rendering name
     * @param versionTag    dictates if the wfs version tag is added to requests
     * @throws IllegalArgumentException if any parameter is null
     */
    public PlaceNameService(String service, String dataset, String fileCachePath, Sector sector, LatLon tileDelta,
        Font font, boolean versionTag) {
        // Data retrieval and caching attributes.
        this.service = service;
        this.dataset = dataset;
        this.fileCachePath = fileCachePath;
        // Geospatial attributes.
        this.maskingSector = sector;
        this.tileDelta = tileDelta;
        // Display attributes.
        this.font = font;
        this.enabled = true;
        this.color = Color.white;
        this.minDisplayDistance = Double.MIN_VALUE;
        this.maxDisplayDistance = Double.MAX_VALUE;
        this.addVersionTag = versionTag;

        String message = this.validate();
        if (message != null) {
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.numColumns = this.numColumnsInLevel();
    }

    /**
     * @param row    row
     * @param column column
     * @return path of the tile in the cache
     * @throws IllegalArgumentException if either <code>row</code> or <code>column</code> is less than zero
     */
    public String createFileCachePathFromTile(int row, int column) {
        if (row < 0 || column < 0) {
            String message = Logging.getMessage("PlaceNameService.RowOrColumnOutOfRange", row, column);
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        StringBuilder sb = new StringBuilder(this.fileCachePath);
        sb.append(File.separator).append(this.dataset);
        sb.append(File.separator).append(row);
        sb.append(File.separator).append(row).append('_').append(column);
        sb.append(FORMAT_SUFFIX);

        String path = sb.toString();
        return path.replaceAll("[:*?<>|]", "");
    }

    private int numColumnsInLevel() {
        int firstCol = Tile.computeColumn(this.tileDelta.getLongitude(), TILING_SECTOR.lonMin(), Angle.NEG180);
        int lastCol = Tile.computeColumn(this.tileDelta.getLongitude(),
            TILING_SECTOR.lonMax().subtract(this.tileDelta.getLongitude()), Angle.NEG180);

        return lastCol - firstCol + 1;
    }

    public long getTileNumber(int row, int column) {
        return row * this.numColumns + column;
    }

    /**
     * @param sector request bounding box
     * @return wfs request url
     * @throws MalformedURLException thrown if error creating the url
     * @throws IllegalArgumentException       if {@link Sector} is null
     */
    public URL createServiceURLFromSector(Sector sector) throws MalformedURLException {
        if (sector == null) {
            String msg = Logging.getMessage("nullValue.SectorIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        StringBuilder sb = new StringBuilder(this.service);
        if (sb.charAt(sb.length() - 1) != '?')
            sb.append('?');

        if (addVersionTag)
            sb.append("version=1.0.0&TypeName=").append(
                dataset);   //version=1.0.0  is needed when querying a new wfs server
        else
            sb.append("TypeName=").append(dataset);

        sb.append("&Request=GetFeature");
        sb.append("&Service=WFS");
        sb.append("&OUTPUTFORMAT=GML2-GZIP");
        sb.append("&BBOX=");
        sb.append(sector.lonMin().getDegrees()).append(',');
        sb.append(sector.latMin().getDegrees()).append(',');
        sb.append(sector.lonMax().getDegrees()).append(',');
        sb.append(sector.latMax().getDegrees());
        return new URL(sb.toString());
    }

    public synchronized final PlaceNameService deepCopy() {
        PlaceNameService copy = new PlaceNameService(this.service, this.dataset, this.fileCachePath, maskingSector,
            this.tileDelta, this.font, this.addVersionTag);
        copy.enabled = this.enabled;
        copy.color = this.color;
        copy.minDisplayDistance = this.minDisplayDistance;
        copy.maxDisplayDistance = this.maxDisplayDistance;
        return copy;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || this.getClass() != o.getClass())
            return false;

        final PlaceNameService other = (PlaceNameService) o;

        if (!Objects.equals(this.service, other.service))
            return false;
        if (!Objects.equals(this.dataset, other.dataset))
            return false;
        if (!Objects.equals(this.fileCachePath, other.fileCachePath))
            return false;
        if (!Objects.equals(this.maskingSector, other.maskingSector))
            return false;
        if (!Objects.equals(this.tileDelta, other.tileDelta))
            return false;
        if (!Objects.equals(this.font, other.font))
            return false;
        if (!Objects.equals(this.color, other.color))
            return false;
        if (!Objects.equals(this.backgroundColor, other.backgroundColor))
            return false;
        if (this.minDisplayDistance != other.minDisplayDistance)
            return false;
        //noinspection RedundantIfStatement
        if (this.maxDisplayDistance != other.maxDisplayDistance)
            return false;

        return true;
    }

    public synchronized final Color getColor() {
        return this.color;
    }

    /**
     * @param color color of label
     * @throws IllegalArgumentException if {@link Color} is null
     */
    public synchronized final void setColor(Color color) {
        if (color == null) {
            String message = Logging.getMessage("nullValue.ColorIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.color = color;
    }

    public synchronized final Color getBackgroundColor() {
        if (this.backgroundColor == null)
            this.backgroundColor = suggestBackgroundColor(this.color);
        return this.backgroundColor;
    }

    public synchronized final void setBackgroundColor(Color backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    private Color suggestBackgroundColor(Color foreground) {
        float[] compArray = new float[4];
        Color.RGBtoHSB(foreground.getRed(), foreground.getGreen(), foreground.getBlue(), compArray);
        int colorValue = compArray[2] < 0.5f ? 255 : 0;
        int alphaValue = foreground.getAlpha();
        return new Color(colorValue, colorValue, colorValue, alphaValue);
    }

    public final String getDataset() {
        return this.dataset;
    }

    /**
     * @param dc DrawContext
     * @return extent of current drawcontext
     * @throws IllegalArgumentException if {@link DrawContext} is null
     */
    public final Extent getExtent(DrawContext dc) {
        if (dc == null) {
            String message = Logging.getMessage("nullValue.DrawContextIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        return Sector.computeBoundingBox(dc.getGlobe(), dc.getVerticalExaggeration(), this.maskingSector);
    }

    public final String getFileCachePath() {
        return this.fileCachePath;
    }

    public final Font getFont() {
        return this.font;
    }

    public synchronized final double getMaxDisplayDistance() {
        return this.maxDisplayDistance;
    }

    /**
     * @param maxDisplayDistance maximum distance to display labels for this service
     * @throws IllegalArgumentException if <code>maxDisplayDistance</code> is less than the current minimum display
     *                                  distance
     */
    public synchronized final void setMaxDisplayDistance(double maxDisplayDistance) {
        if (maxDisplayDistance < this.minDisplayDistance) {
            String message = Logging.getMessage("PlaceNameService.MaxDisplayDistanceLessThanMinDisplayDistance",
                maxDisplayDistance, this.minDisplayDistance);
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.maxDisplayDistance = maxDisplayDistance;
    }

    public synchronized final double getMinDisplayDistance() {
        return this.minDisplayDistance;
    }

    /**
     * @param minDisplayDistance minimum distance to display labels for this service
     * @throws IllegalArgumentException if <code>minDisplayDistance</code> is less than the current maximum display
     *                                  distance
     */
    public synchronized final void setMinDisplayDistance(double minDisplayDistance) {
        if (minDisplayDistance > this.maxDisplayDistance) {
            String message = Logging.getMessage("PlaceNameService.MinDisplayDistanceGrtrThanMaxDisplayDistance",
                minDisplayDistance, this.maxDisplayDistance);
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.minDisplayDistance = minDisplayDistance;
    }

    public final LatLon getTileDelta() {
        return tileDelta;
    }

    public final Sector getMaskingSector() {
        return this.maskingSector;
    }

    public final String getService() {
        return this.service;
    }

    public boolean isAddVersionTag() {
        return addVersionTag;
    }

    public void setAddVersionTag(boolean addVersionTag) {
        this.addVersionTag = addVersionTag;
    }

    @Override
    public int hashCode() {
        int result;
        result = (service != null ? service.hashCode() : 0);
        result = 29 * result + (this.dataset != null ? this.dataset.hashCode() : 0);
        result = 29 * result + (this.fileCachePath != null ? this.fileCachePath.hashCode() : 0);
        result = 29 * result + (this.maskingSector != null ? this.maskingSector.hashCode() : 0);
        result = 29 * result + (this.tileDelta != null ? this.tileDelta.hashCode() : 0);
        result = 29 * result + (this.font != null ? this.font.hashCode() : 0);
        result = 29 * result + (this.color != null ? this.color.hashCode() : 0);
        result = 29 * result + Double.hashCode(minDisplayDistance);
        result = 29 * result + Double.hashCode(maxDisplayDistance);
        return result;
    }

    public synchronized final boolean isEnabled() {
        return this.enabled;
    }

    public synchronized final void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public synchronized final void markResourceAbsent(long tileNumber) {
        this.absentTiles.markResourceAbsent(tileNumber);
    }

    public synchronized final boolean isResourceAbsent(long resourceNumber) {
        return this.absentTiles.isResourceAbsent(resourceNumber);
    }

    public synchronized final void unmarkResourceAbsent(long tileNumber) {
        this.absentTiles.unmarkResourceAbsent(tileNumber);
    }

    /**
     * Determines if this {@link PlaceNameService} constructor arguments are valid.
     *
     * @return null if valid, otherwise a string message containing a description of why it is invalid.
     */
    public final String validate() {
        String msg = "";
        if (this.service == null) {
            msg += Logging.getMessage("nullValue.ServiceIsNull") + ", ";
        }
        if (this.dataset == null) {
            msg += Logging.getMessage("nullValue.DataSetIsNull") + ", ";
        }
        if (this.fileCachePath == null) {
            msg += Logging.getMessage("nullValue.FileStorePathIsNull") + ", ";
        }
        if (this.maskingSector == null) {
            msg += Logging.getMessage("nullValue.SectorIsNull") + ", ";
        }
        if (this.tileDelta == null) {
            msg += Logging.getMessage("nullValue.TileDeltaIsNull") + ", ";
        }
        if (this.font == null) {
            msg += Logging.getMessage("nullValue.FontIsNull") + ", ";
        }

        if (msg.isEmpty()) {
            return null;
        }

        return msg;
    }
}
