/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.data;

import gov.nasa.worldwind.Keys;
import gov.nasa.worldwind.avlist.*;
import gov.nasa.worldwind.formats.tiff.GeotiffReader;
import gov.nasa.worldwind.formats.worldfile.WorldFile;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.util.*;

import java.io.IOException;

/**
 * @author dcollins
 * @version $Id: GeotiffRasterReader.java 1171 2013-02-11 21:45:02Z dcollins $
 */
public class GeotiffRasterReader extends AbstractDataRasterReader {
    private static final String[] geotiffMimeTypes = {"image/tiff", "image/geotiff"};
    private static final String[] geotiffSuffixes = {"tif", "tiff", "gtif", "tif.zip", "tiff.zip", "tif.gz", "tiff.gz"};

    public GeotiffRasterReader() {
        super(GeotiffRasterReader.geotiffMimeTypes, GeotiffRasterReader.geotiffSuffixes);
    }

    protected boolean doCanRead(Object source, KV params) {
        String path = WWIO.getSourcePath(source);
        if (path == null) {
            return false;
        }

        GeotiffReader reader = null;
        try {
            reader = new GeotiffReader(path);
            boolean isGeoTiff = reader.isGeotiff(0);
            if (!isGeoTiff) {
                isGeoTiff = WorldFile.hasWorldFiles(source);
            }
            return isGeoTiff;
        }
        catch (Exception e) {
            // Intentionally ignoring exceptions.
            return false;
        }
        finally {
            if (reader != null) {
                reader.close();
            }
        }
    }

    protected DataRaster[] doRead(Object source, KV params) throws IOException {
        String path = WWIO.getSourcePath(source);
        if (path == null) {
            String message = Logging.getMessage("DataRaster.CannotRead", source);
            Logging.logger().severe(message);
            throw new IOException(message);
        }

        KV metadata = new KVMap();
        if (null != params)
            metadata.setValues(params);

        GeotiffReader reader = null;
        DataRaster[] rasters = null;
        try {
            this.readMetadata(source, metadata);

            reader = new GeotiffReader(path);
            reader.copyMetadataTo(metadata);

            rasters = reader.readDataRaster();

            if (null != rasters) {
                String[] keysToCopy = {Keys.SECTOR};
                for (DataRaster raster : rasters) {
                    WWUtil.copyValues(metadata, raster, keysToCopy, false);
                }
            }
        }
        finally {
            if (reader != null) {
                reader.close();
            }
        }
        return rasters;
    }

    protected void doReadMetadata(Object source, KV params) throws IOException {
        String path = WWIO.getSourcePath(source);
        if (path == null) {
            String message = Logging.getMessage("nullValue.PathIsNull", source);
            Logging.logger().severe(message);
            throw new IOException(message);
        }

        GeotiffReader reader = null;
        try {
            reader = new GeotiffReader(path);
            reader.copyMetadataTo(params);

            boolean isGeoTiff = reader.isGeotiff(0);
            if (!isGeoTiff && params.hasKey(Keys.WIDTH) && params.hasKey(Keys.HEIGHT)) {
                int[] size = new int[2];

                size[0] = (Integer) params.get(Keys.WIDTH);
                size[1] = (Integer) params.get(Keys.HEIGHT);

                params.set(WorldFile.WORLD_FILE_IMAGE_SIZE, size);

                WorldFile.readWorldFiles(source, params);

                Object o = params.get(Keys.SECTOR);
                if (!(o instanceof Sector)) {
                    ImageUtil.calcBoundingBoxForUTM(params);
                }
            }
        }
        finally {
            if (reader != null) {
                reader.close();
            }
        }
    }
}