/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */

package gov.nasa.worldwind.data;

import gov.nasa.worldwind.Keys;
import gov.nasa.worldwind.avlist.KV;
import gov.nasa.worldwind.exception.WWRuntimeException;
import gov.nasa.worldwind.util.*;
import gov.nasa.worldwind.util.gdal.GDALUtils;

import java.io.File;
import java.util.logging.Level;

/**
 * @author Lado Garakanidze
 * @version $Id: GDALDataRasterReader.java 1171 2013-02-11 21:45:02Z dcollins $
 */

public class GDALDataRasterReader extends AbstractDataRasterReader {
    // Extract list of mime types supported by GDAL
    protected static final String[] mimeTypes = {
        "image/jp2", "image/jpeg2000", "image/jpeg2000-image", "image/x-jpeg2000-image",
        "image/x-mrsid-image",
        "image/jpeg", "image/png", "image/bmp", "image/tif"
    };

    // TODO Extract list of extensions supported by GDAL
    protected static final String[] suffixes = {
        "jp2", "sid", "ntf", "nitf",
        "JP2", "SID", "NTF", "NITF",

        "jpg", "jpe", "jpeg",   /* "image/jpeg" */
        "png",                  /* "image/png" */
        "bmp",                  /* "image/bmp" */
        "TIF", "TIFF", "GTIF", "GTIFF", "tif", "tiff", "gtif", "gtiff",     /* "image/tif" */

        // Elevations

        // DTED
        "dt0", "dt1", "dt2",
        "asc", "adf", "dem"
    };

    public GDALDataRasterReader() {
        super("GDAL-based Data Raster Reader", GDALDataRasterReader.mimeTypes, GDALDataRasterReader.suffixes);
    }

    protected static GDALDataRaster readDataRaster(Object source, boolean quickReadingMode) {
        if (null == source) {
            String message = Logging.getMessage("nullValue.SourceIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        try {

            return new GDALDataRaster(source, quickReadingMode);
        }
        catch (WWRuntimeException wwre) {
            throw wwre;
        }
        catch (Throwable t) {
            String message = Logging.getMessage("generic.CannotOpenFile", GDALUtils.getErrorMessage());
            Logging.logger().log(Level.SEVERE, message, t);
            throw new WWRuntimeException(t);
        }
    }

    @Override
    public boolean canRead(Object source, KV params) {
        // RPF imagery cannot be identified by a small set of suffixes or mime types, so we override the standard
        // suffix comparison behavior here.
        return this.doCanRead(source, params);
    }

    @Override
    protected boolean doCanRead(Object source, KV params) {
        if (WWUtil.isEmpty(source)) {
            return false;
        }

        if (null == params) {
            File file = WWIO.getFileForLocalAddress(source);
            if (null == file) {
                return false;
            }

            return GDALUtils.canOpen(file);
        }

        boolean canOpen = false;
        GDALDataRaster raster = null;
        try {
            raster = new GDALDataRaster(source, true); // read data raster quietly
            params.setValues(raster.getMetadata());
            canOpen = true;
        }
        catch (Throwable t) {
            // we purposely ignore any exception here, this should be a very quiet mode
            canOpen = false;
        }
        finally {
            if (null != raster) {
                raster.dispose();
                raster = null;
            }
        }

        return canOpen;
    }

    @Override
    protected DataRaster[] doRead(Object source, KV params) {
        GDALDataRaster raster = GDALDataRasterReader.readDataRaster(source, false);
        if (null != params) {
            params.setValues(raster.getMetadata());
            WWUtil.copyValues(params, raster, new String[] {Keys.SECTOR}, false);
        }

        return new DataRaster[] {raster};
    }

    @Override
    protected void doReadMetadata(Object source, KV params) {
        GDALDataRaster raster = GDALDataRasterReader.readDataRaster(source, true);
        if (null != params) {
            params.setValues(raster.getMetadata());
            WWUtil.copyValues(params, raster, new String[] {Keys.SECTOR}, false);
            raster.dispose();
        }
    }
}