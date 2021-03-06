/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.data;

import gov.nasa.worldwind.Keys;
import gov.nasa.worldwind.avlist.*;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.util.*;

import java.awt.*;
import java.awt.geom.*;
import java.util.Map;

/**
 * @author Lado Garakanidze
 * @version $Id: AbstractDataRaster.java 1171 2013-02-11 21:45:02Z dcollins $
 */
public abstract class AbstractDataRaster extends KVMap implements DataRaster {
    protected int width;
    protected int height;

    protected AbstractDataRaster() {
        super();
    }

    protected AbstractDataRaster(int width, int height, Sector sector) throws IllegalArgumentException {
        super();

        if (width < 0) {
            String message = Logging.getMessage("generic.InvalidWidth", width);
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (height < 0) {
            String message = Logging.getMessage("generic.InvalidHeight", height);
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

//        if (sector == null) {
//            String message = Logging.getMessage("nullValue.SectorIsNull");
//            Logging.logger().finest(message);
//        }

        // for performance reasons we are "caching" these parameters in addition to AVList
        this.width = width;
        this.height = height;

        if (null != sector) {
            this.set(Keys.SECTOR, sector);
        }

        this.set(Keys.WIDTH, width);
        this.set(Keys.HEIGHT, height);
    }

    protected AbstractDataRaster(int width, int height, Sector sector, KV list) throws IllegalArgumentException {
        this(width, height, sector);

        if (null != list) {
            for (Map.Entry<String, Object> entry : list.getEntries()) {
                this.set(entry.getKey(), entry.getValue());
            }
        }
    }

    public int getWidth() {
        return this.width;
    }

    public int getHeight() {
        return this.height;
    }

    public Sector getSector() {
        if (this.hasKey(Keys.SECTOR)) {
            return (Sector) this.get(Keys.SECTOR);
        }
        return null;
    }

    @Override
    public Object set(String key, Object value) {
        if (null == key) {
            String message = Logging.getMessage("nullValue.KeyIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        // Do not allow to change existing WIDTH or HEIGHT

        if (this.hasKey(key)) {
            if (Keys.WIDTH.equals(key) && this.getWidth() != (Integer) value) {
                String message = Logging.getMessage("generic.AttemptToChangeReadOnlyProperty", key);
                Logging.logger().finest(message);
                // relax restriction, just log and continue
                return this;
            } else if (Keys.HEIGHT.equals(key) && this.getHeight() != (Integer) value) {
                String message = Logging.getMessage("generic.AttemptToChangeReadOnlyProperty", key);
                Logging.logger().finest(message);
                // relax restriction, just log and continue
                return this;
            }
        }
        return super.set(key, value);
    }

    protected Rectangle computeClipRect(Sector clipSector, DataRaster clippedRaster) {
        AffineTransform geographicToRaster = this.computeGeographicToRasterTransform(
            clippedRaster.getWidth(), clippedRaster.getHeight(), clippedRaster.getSector());

        Point2D geoPoint = new Point2D.Double();
        Point2D ul = new Point2D.Double();
        Point2D lr = new Point2D.Double();

        geoPoint.setLocation(clipSector.lonMin, clipSector.latMax);
        geographicToRaster.transform(geoPoint, ul);

        geoPoint.setLocation(clipSector.lonMax, clipSector.latMin);
        geographicToRaster.transform(geoPoint, lr);

        int x = (int) Math.floor(ul.getX());
        int y = (int) Math.floor(ul.getY());
        int width = (int) Math.ceil(lr.getX() - ul.getX());
        int height = (int) Math.ceil(lr.getY() - ul.getY());

        return new Rectangle(x, y, width, height);
    }

    protected AffineTransform computeSourceToDestTransform(
        int sourceWidth, int sourceHeight, Sector sourceSector,
        int destWidth, int destHeight, Sector destSector) {
        // Compute the the transform from source to destination coordinates. In this computation a pixel is assumed
        // to cover a finite area.

        double ty = destHeight * -(sourceSector.latMax - destSector.latMax)
            / destSector.latDelta;
        double tx = destWidth * (sourceSector.lonMin - destSector.lonMin)
            / destSector.lonDelta;

        double sy = ((double) destHeight / sourceHeight)
            * (sourceSector.latDelta / destSector.latDelta);
        double sx = ((double) destWidth / sourceWidth)
            * (sourceSector.lonDelta / destSector.lonDelta);

        AffineTransform transform = new AffineTransform();
        transform.translate(tx, ty);
        transform.scale(sx, sy);
        return transform;
    }

    protected AffineTransform computeGeographicToRasterTransform(int width, int height, Sector sector) {
        // Compute the the transform from geographic to raster coordinates. In this computation a pixel is assumed
        // to cover a finite area.

        double ty = -sector.latMax;
        double tx = -sector.lonMin;

        double sy = -(height / sector.latDelta);
        double sx = (width / sector.lonDelta);

        AffineTransform transform = new AffineTransform();
        transform.scale(sx, sy);
        transform.translate(tx, ty);
        return transform;
    }

    public DataRaster getSubRaster(int width, int height, Sector sector, KV params) {
        params = (null == params) ? new KVMap() : params;

        // copy parent raster keys/values; only those key/value will be copied that do exist in the parent raster
        // AND does NOT exist in the requested raster
        String[] keysToCopy = {
            Keys.DATA_TYPE, Keys.MISSING_DATA_SIGNAL, Keys.BYTE_ORDER, Keys.PIXEL_FORMAT, Keys.ELEVATION_UNIT
        };
        WWUtil.copyValues(this, params, keysToCopy, false);

        params.set(Keys.WIDTH, width);
        params.set(Keys.HEIGHT, height);
        params.set(Keys.SECTOR, sector);

        return this.getSubRaster(params);
    }

    /**
     * Reads the specified region of interest (ROI) with given extent, width, and height, and type
     *
     * @param params Required parameters are:
     *               <p>
     *               AVKey.HEIGHT as Integer, specifies a height of the desired ROI AVKey.WIDTH as Integer, specifies a
     *               width of the desired ROI AVKey.SECTOR as Sector, specifies an extent of the desired ROI
     *               <p>
     *               Optional parameters are:
     *               <p>
     *               AVKey.BAND_ORDER as array of integers, examples: for RGBA image: new int[] { 0, 1, 2, 3 }, or for
     *               ARGB image: new int[] { 3, 0, 1, 2 } or if you want only RGB bands of the RGBA image: new int[] {
     *               0, 1, 2 } or only Intensity (4th) band of the specific aerial image: new int[] { 3 }
     * @return DataRaster (BufferedImageRaster for imagery or ByteBufferDataRaster for elevations)
     */
    public DataRaster getSubRaster(KV params) {
        if (null == params) {
            String message = Logging.getMessage("nullValue.ParamsIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (!params.hasKey(Keys.WIDTH)) {
            String message = Logging.getMessage("generic.MissingRequiredParameter", Keys.WIDTH);
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        int roiWidth = (Integer) params.get(Keys.WIDTH);
        if (roiWidth <= 0) {
            String message = Logging.getMessage("generic.InvalidWidth", roiWidth);
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (!params.hasKey(Keys.HEIGHT)) {
            String message = Logging.getMessage("generic.MissingRequiredParameter", Keys.HEIGHT);
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        int roiHeight = (Integer) params.get(Keys.HEIGHT);
        if (roiHeight <= 0) {
            String message = Logging.getMessage("generic.InvalidHeight", roiHeight);
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (!params.hasKey(Keys.SECTOR)) {
            String message = Logging.getMessage("generic.MissingRequiredParameter", Keys.SECTOR);
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        Sector roiSector = (Sector) params.get(Keys.SECTOR);

        if (Sector.EMPTY_SECTOR.equals(roiSector)) {
            String message = Logging.getMessage("nullValue.SectorGeometryIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        // copy parent raster keys/values; only those key/value will be copied that do exist in the parent raster
        // AND does NOT exist in the requested raster
        String[] keysToCopy = {
            Keys.DATA_TYPE, Keys.MISSING_DATA_SIGNAL, Keys.BYTE_ORDER, Keys.PIXEL_FORMAT, Keys.ELEVATION_UNIT
        };
        WWUtil.copyValues(this, params, keysToCopy, false);

        return this.doGetSubRaster(roiWidth, roiHeight, roiSector, params);
    }

    abstract DataRaster doGetSubRaster(int roiWidth, int roiHeight, Sector roiSector, KV roiParams);
}