/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.data;

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
public abstract class AbstractDataRaster extends AVListImpl implements DataRaster {
    protected int width = 0;
    protected int height = 0;

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

        if (sector == null) {
            String message = Logging.getMessage("nullValue.SectorIsNull");
            Logging.logger().finest(message);
//            throw new IllegalArgumentException(message);
        }

        // for performance reasons we are "caching" these parameters in addition to AVList
        this.width = width;
        this.height = height;

        if (null != sector) {
            this.set(AVKey.SECTOR, sector);
        }

        this.set(AVKey.WIDTH, width);
        this.set(AVKey.HEIGHT, height);
    }

    protected AbstractDataRaster(int width, int height, Sector sector, AVList list) throws IllegalArgumentException {
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
        if (this.hasKey(AVKey.SECTOR)) {
            return (Sector) this.get(AVKey.SECTOR);
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
            if (AVKey.WIDTH.equals(key) && this.getWidth() != (Integer) value) {
                String message = Logging.getMessage("generic.AttemptToChangeReadOnlyProperty", key);
                Logging.logger().finest(message);
                // relax restriction, just log and continue
//                throw new IllegalArgumentException(message);
                return this;
            }
            else if (AVKey.HEIGHT.equals(key) && this.getHeight() != (Integer) value) {
                String message = Logging.getMessage("generic.AttemptToChangeReadOnlyProperty", key);
                Logging.logger().finest(message);
                // relax restriction, just log and continue
//                throw new IllegalArgumentException(message);
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

        geoPoint.setLocation(clipSector.lonMin().degrees, clipSector.latMax().degrees);
        geographicToRaster.transform(geoPoint, ul);

        geoPoint.setLocation(clipSector.lonMax().degrees, clipSector.latMin().degrees);
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

        double ty = destHeight * -(sourceSector.latMax().degrees - destSector.latMax().degrees)
            / destSector.getDeltaLatDegrees();
        double tx = destWidth * (sourceSector.lonMin().degrees - destSector.lonMin().degrees)
            / destSector.getDeltaLonDegrees();

        double sy = ((double) destHeight / sourceHeight)
            * (sourceSector.getDeltaLatDegrees() / destSector.getDeltaLatDegrees());
        double sx = ((double) destWidth / sourceWidth)
            * (sourceSector.getDeltaLonDegrees() / destSector.getDeltaLonDegrees());

        AffineTransform transform = new AffineTransform();
        transform.translate(tx, ty);
        transform.scale(sx, sy);
        return transform;
    }

    protected AffineTransform computeGeographicToRasterTransform(int width, int height, Sector sector) {
        // Compute the the transform from geographic to raster coordinates. In this computation a pixel is assumed
        // to cover a finite area.

        double ty = -sector.latMax().degrees;
        double tx = -sector.lonMin().degrees;

        double sy = -(height / sector.getDeltaLatDegrees());
        double sx = (width / sector.getDeltaLonDegrees());

        AffineTransform transform = new AffineTransform();
        transform.scale(sx, sy);
        transform.translate(tx, ty);
        return transform;
    }

    public DataRaster getSubRaster(int width, int height, Sector sector, AVList params) {
        params = (null == params) ? new AVListImpl() : params;

        // copy parent raster keys/values; only those key/value will be copied that do exist in the parent raster
        // AND does NOT exist in the requested raster
        String[] keysToCopy = new String[] {
            AVKey.DATA_TYPE, AVKey.MISSING_DATA_SIGNAL, AVKey.BYTE_ORDER, AVKey.PIXEL_FORMAT, AVKey.ELEVATION_UNIT
        };
        WWUtil.copyValues(this, params, keysToCopy, false);

        params.set(AVKey.WIDTH, width);
        params.set(AVKey.HEIGHT, height);
        params.set(AVKey.SECTOR, sector);

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
    public DataRaster getSubRaster(AVList params) {
        if (null == params) {
            String message = Logging.getMessage("nullValue.ParamsIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (!params.hasKey(AVKey.WIDTH)) {
            String message = Logging.getMessage("generic.MissingRequiredParameter", AVKey.WIDTH);
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        int roiWidth = (Integer) params.get(AVKey.WIDTH);
        if (roiWidth <= 0) {
            String message = Logging.getMessage("generic.InvalidWidth", roiWidth);
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (!params.hasKey(AVKey.HEIGHT)) {
            String message = Logging.getMessage("generic.MissingRequiredParameter", AVKey.HEIGHT);
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        int roiHeight = (Integer) params.get(AVKey.HEIGHT);
        if (roiHeight <= 0) {
            String message = Logging.getMessage("generic.InvalidHeight", roiHeight);
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (!params.hasKey(AVKey.SECTOR)) {
            String message = Logging.getMessage("generic.MissingRequiredParameter", AVKey.SECTOR);
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        Sector roiSector = (Sector) params.get(AVKey.SECTOR);
        if (null == roiSector || Sector.EMPTY_SECTOR.equals(roiSector)) {
            String message = Logging.getMessage("nullValue.SectorIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (Sector.EMPTY_SECTOR.equals(roiSector)) {
            String message = Logging.getMessage("nullValue.SectorGeometryIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        // copy parent raster keys/values; only those key/value will be copied that do exist in the parent raster
        // AND does NOT exist in the requested raster
        String[] keysToCopy = new String[] {
            AVKey.DATA_TYPE, AVKey.MISSING_DATA_SIGNAL, AVKey.BYTE_ORDER, AVKey.PIXEL_FORMAT, AVKey.ELEVATION_UNIT
        };
        WWUtil.copyValues(this, params, keysToCopy, false);

        return this.doGetSubRaster(roiWidth, roiHeight, roiSector, params);
    }

    abstract DataRaster doGetSubRaster(int roiWidth, int roiHeight, Sector roiSector, AVList roiParams);
}
