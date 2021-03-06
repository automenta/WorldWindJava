/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.data;

import gov.nasa.worldwind.Keys;
import gov.nasa.worldwind.avlist.*;
import gov.nasa.worldwind.formats.worldfile.WorldFile;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.util.*;

import java.io.*;
import java.net.URL;
import java.nio.ByteBuffer;

/**
 * @author dcollins
 * @version $Id: BILRasterReader.java 1171 2013-02-11 21:45:02Z dcollins $
 */
public class BILRasterReader extends AbstractDataRasterReader {
    private static final String[] bilMimeTypes = {"image/bil", "application/bil", "application/bil16", "application/bil32"};

    private static final String[] bilSuffixes = {"bil", "bil16", "bil32", "bil.gz", "bil16.gz", "bil32.gz"};

    private boolean mapLargeFiles;
    private long largeFileThreshold = 16777216L; // 16 megabytes

    public BILRasterReader() {
        super(BILRasterReader.bilMimeTypes, BILRasterReader.bilSuffixes);
    }

    public boolean isMapLargeFiles() {
        return this.mapLargeFiles;
    }

    public void setMapLargeFiles(boolean mapLargeFiles) {
        this.mapLargeFiles = mapLargeFiles;
    }

    public long getLargeFileThreshold() {
        return this.largeFileThreshold;
    }

    public void setLargeFileThreshold(long largeFileThreshold) {
        if (largeFileThreshold < 0L) {
            String message = Logging.getMessage("generic.ArgumentOutOfRange", "largeFileThreshold < 0");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.largeFileThreshold = largeFileThreshold;
    }

    protected boolean doCanRead(Object source, KV params) {
        if (!(source instanceof File) && !(source instanceof URL)) {
            return false;
        }

        // If the data source doesn't already have all the necessary metadata, then we determine whether or not
        // the missing metadata can be read.
        String error = this.validateMetadata(source, params);
        if (!WWUtil.isEmpty(error)) {
            if (!WorldFile.hasWorldFiles(source)) {
                Logging.logger().fine(error);
                return false;
            }
        }

        if (null != params) {
            if (!params.hasKey(Keys.PIXEL_FORMAT)) {
                params.set(Keys.PIXEL_FORMAT, Keys.ELEVATION);
            }
        }

        return true;
    }

    protected DataRaster[] doRead(Object source, KV params) throws IOException {
        ByteBuffer byteBuffer = this.readElevations(source);

        // If the parameter list is null, or doesn't already have all the necessary metadata, then we copy the parameter
        // list and attempt to populate the copy with any missing metadata.        
        if (this.validateMetadata(source, params) != null) {
            // Copy the parameter list to insulate changes from the caller.
            params = (params != null) ? params.copy() : new KVMap();
            params.set(Keys.FILE_SIZE, byteBuffer.capacity());
            WorldFile.readWorldFiles(source, params);
        }

        int width = (Integer) params.get(Keys.WIDTH);
        int height = (Integer) params.get(Keys.HEIGHT);
        Sector sector = (Sector) params.get(Keys.SECTOR);

        if (!params.hasKey(Keys.PIXEL_FORMAT)) {
            params.set(Keys.PIXEL_FORMAT, Keys.ELEVATION);
        }

        ByteBufferRaster raster = new ByteBufferRaster(width, height, sector, byteBuffer, params);
        ElevationsUtil.rectify(raster);
        return new DataRaster[] {raster};
    }

    protected void doReadMetadata(Object source, KV params) throws IOException {
        if (this.validateMetadata(source, params) != null) {
            WorldFile.readWorldFiles(source, params);
        }
    }

    protected String validateMetadata(Object source, KV params) {
        StringBuilder sb = new StringBuilder();

        String message = super.validateMetadata(source, params);
        if (message != null) {
            sb.append(message);
        }

        Object o = (params != null) ? params.get(Keys.BYTE_ORDER) : null;
        if (!(o instanceof String)) {
            sb.append(sb.isEmpty() ? "" : ", ").append(Logging.getMessage("WorldFile.NoByteOrderSpecified", source));
        }

        o = (params != null) ? params.get(Keys.PIXEL_FORMAT) : null;
        if (o == null) {
            sb.append(sb.isEmpty() ? "" : ", ").append(
                Logging.getMessage("WorldFile.NoPixelFormatSpecified", source));
        } else if (!Keys.ELEVATION.equals(o)) {
            sb.append(sb.isEmpty() ? "" : ", ").append(Logging.getMessage("WorldFile.InvalidPixelFormat", source));
        }

        o = (params != null) ? params.get(Keys.DATA_TYPE) : null;
        if (o == null) {
            sb.append(sb.isEmpty() ? "" : ", ").append(Logging.getMessage("WorldFile.NoDataTypeSpecified", source));
        }

        if (sb.isEmpty()) {
            return null;
        }

        return sb.toString();
    }

    private ByteBuffer readElevations(Object source) throws IOException {
        if (!(source instanceof File) && !(source instanceof URL)) {
            String message = Logging.getMessage("DataRaster.CannotRead", source);
            Logging.logger().severe(message);
            throw new IOException(message);
        }

        File file = (source instanceof File) ? (File) source : null;
        URL url = (source instanceof URL) ? (URL) source : null;

        if (null == file && "file".equalsIgnoreCase(url.getProtocol())) {
            file = new File(url.getFile());
        }

        if (null != file) {
            // handle .bil.zip, .bil16.zip, and .bil32.gz files
            if (file.getName().toLowerCase().endsWith(".zip")) {
                return WWIO.readZipEntryToBuffer(file, null);
            }
            // handle bil.gz, bil16.gz, and bil32.gz files
            else if (file.getName().toLowerCase().endsWith(".gz")) {
                return WWIO.readGZipFileToBuffer(file);
            } else if (!this.isMapLargeFiles() || (this.getLargeFileThreshold() > file.length())) {
                return WWIO.readFileToBuffer(file);
            } else {
                return WWIO.mapFile(file);
            }
        } else // (source instanceof java.net.URL)
        {
            return WWIO.readURLContentToBuffer(url);
        }
    }
}