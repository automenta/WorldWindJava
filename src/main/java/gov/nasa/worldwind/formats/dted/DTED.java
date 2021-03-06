/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */

package gov.nasa.worldwind.formats.dted;

import gov.nasa.worldwind.Keys;
import gov.nasa.worldwind.avlist.*;
import gov.nasa.worldwind.data.*;
import gov.nasa.worldwind.formats.tiff.GeoTiff;
import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.util.Logging;

import java.io.*;
import java.nio.*;
import java.nio.channels.*;

/**
 * @author Lado Garakanidze
 * @version $Id: DTED.java 3037 2015-04-17 23:08:47Z tgaskins $
 */

public class DTED {
    protected static final int REC_HEADER_SIZE = 8; // 8 bytes
    protected static final int REC_CHKSUM_SIZE = Integer.SIZE / Byte.SIZE; // 4 bytes (32bit integer)

    protected static final int DTED_UHL_SIZE = 80;
    protected static final int DTED_DSI_SIZE = 648;
    protected static final int DTED_ACC_SIZE = 2700;

    protected static final long DTED_UHL_OFFSET = 0L;
    protected static final long DTED_DSI_OFFSET = DTED.DTED_UHL_OFFSET + DTED.DTED_UHL_SIZE;
    protected static final long DTED_ACC_OFFSET = DTED.DTED_DSI_OFFSET + DTED.DTED_DSI_SIZE;
    protected static final long DTED_DATA_OFFSET = DTED.DTED_ACC_OFFSET + DTED.DTED_ACC_SIZE;

    protected static final int DTED_NODATA_VALUE = -32767;
    protected static final int DTED_MIN_VALUE = -12000;
    protected static final int DTED_MAX_VALUE = 9000;

    protected DTED() {
    }

    protected static RandomAccessFile open(File file) throws IOException, IllegalArgumentException {
        if (null == file) {
            String message = Logging.getMessage("nullValue.FileIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (!file.exists()) {
            String message = Logging.getMessage("generic.FileNotFound", file.getAbsolutePath());
            Logging.logger().severe(message);
            throw new IOException(message);
        }

        if (!file.canRead()) {
            String message = Logging.getMessage("generic.FileNoReadPermission", file.getAbsolutePath());
            Logging.logger().severe(message);
            throw new IOException(message);
        }

        return new RandomAccessFile(file, "r");
    }

    protected static void close(Closeable file) {
        if (null != file) {
            try {
                file.close();
            }
            catch (Exception ex) {
                Logging.logger().finest(ex.getMessage());
            }
        }
    }

    public static KV readMetadata(File file) throws IOException {
        KV metadata = null;
        RandomAccessFile sourceFile = null;

        try {
            sourceFile = DTED.open(file);

            FileChannel channel = sourceFile.getChannel();

            metadata = new KVMap();

            DTED.readUHL(channel, DTED.DTED_UHL_OFFSET, metadata);
            DTED.readDSI(channel, DTED.DTED_DSI_OFFSET, metadata);
            DTED.readACC(channel, DTED.DTED_ACC_OFFSET, metadata);
        }
        finally {
            DTED.close(sourceFile);
        }

        return metadata;
    }

    public static DataRaster read(File file, KV metadata) throws IOException {
        DataRaster raster = null;
        RandomAccessFile sourceFile = null;

        try {
            sourceFile = DTED.open(file);

            FileChannel channel = sourceFile.getChannel();

            DTED.readUHL(channel, DTED.DTED_UHL_OFFSET, metadata);
            DTED.readDSI(channel, DTED.DTED_DSI_OFFSET, metadata);
            DTED.readACC(channel, DTED.DTED_ACC_OFFSET, metadata);

            raster = DTED.readElevations(channel, DTED.DTED_DATA_OFFSET, metadata);
        }
        finally {
            DTED.close(sourceFile);
        }

        return raster;
    }

    protected static DataRaster readElevations(SeekableByteChannel theChannel, long offset, KV metadata)
        throws IOException {
        if (null == theChannel)
            return null;

        ByteBufferRaster raster = (ByteBufferRaster) ByteBufferRaster.createGeoreferencedRaster(metadata);

        theChannel.position(offset);

        int width = (Integer) metadata.get(Keys.WIDTH);
        int height = (Integer) metadata.get(Keys.HEIGHT);

        int recordSize = DTED.REC_HEADER_SIZE + height * Short.SIZE / Byte.SIZE + DTED.REC_CHKSUM_SIZE;

        double min = +Double.POSITIVE_INFINITY;
        double max = -Double.POSITIVE_INFINITY;

        ByteBuffer bb = ByteBuffer.allocate(recordSize).order(ByteOrder.BIG_ENDIAN);
        for (int x = 0; x < width; x++) {
            theChannel.read(bb);
            bb.flip();

            int dataChkSum = 0;
            for (int i = 0; i < recordSize - DTED.REC_CHKSUM_SIZE;
                i++) // include header and elevations, exclude checksum itself
            {
                dataChkSum += 0xFF & bb.get(i);
            }

            ShortBuffer data = bb.asShortBuffer();
            for (int i = 0; i < height; i++) {
                double elev = data.get(i + 4); // skip 4 shorts of header
                int y = height - i - 1;

                if (elev != DTED.DTED_NODATA_VALUE && elev >= DTED.DTED_MIN_VALUE && elev <= DTED.DTED_MAX_VALUE) {
                    raster.setDoubleAtPosition(y, x, elev);
                    min = Math.min(elev, min);
                    max = Math.max(elev, max);
                } else {
                    // Interpret null DTED values and values outside the practical range of [-12000,+9000] as missing
                    // data. See MIL-PRF-89020B sections 3.11.2 and 3.11.3.
                    raster.setDoubleAtPosition(y, x, DTED.DTED_NODATA_VALUE);
                }
            }

            short hi = data.get(height + DTED.REC_CHKSUM_SIZE);
            short lo = data.get(height + DTED.REC_CHKSUM_SIZE + 1);

            int expectedChkSum = (0xFFFF & hi) << 16 | (0xFFFF & lo);

            if (expectedChkSum != dataChkSum) {
                String message = Logging.getMessage("DTED.DataRecordChecksumError", expectedChkSum, dataChkSum);
                Logging.logger().severe(message);
                throw new IOException(message);
            }
        }

        raster.set(Keys.ELEVATION_MIN, min);
        raster.set(Keys.ELEVATION_MAX, max);

        return raster;
    }

    protected static Angle readAngle(String angle) throws IOException {
        if (null == angle) {
            String message = Logging.getMessage("nullValue.AngleIsNull");
            Logging.logger().severe(message);
            throw new IOException(message);
        }

        // let's separate DDDMMSSH with spaces and use a standard Angle.fromDMS() method
        StringBuilder sb = new StringBuilder(angle.trim());
        int length = sb.length();
        // 0123456789
        // DD MM SS H
        // 01234567890
        // DDD MM SS H
        // 012345678901
        // DD MM SS.S H
        // 0123456789012
        // DDD MM SS.S H
        switch (length) {
            case 7 -> {
                sb.insert(2, ' ').insert(5, ' ').insert(8, ' ');
                return Angle.fromDMS(sb.toString());
            }
            case 8 -> {
                sb.insert(3, ' ').insert(6, ' ').insert(9, ' ');
                return Angle.fromDMS(sb.toString());
            }
            case 9 -> {
                sb.insert(2, ' ').insert(5, ' ').insert(10, ' ');
                sb.delete(8, 10);  // remove ".S" part, DTED spec not uses it anyway
                return Angle.fromDMS(sb.toString());
            }
            case 10 -> {
                sb.insert(3, ' ').insert(6, ' ').insert(11, ' ');
                sb.delete(9, 11);   // remove ".S" part, DTED spec not uses it anyway
                return Angle.fromDMS(sb.toString());
            }
        }

        return null;
    }

    protected static Integer readLevel(String dtedLevel) {
        if (null == dtedLevel)
            return null;

        dtedLevel = dtedLevel.trim();

        if (!dtedLevel.startsWith("DTED") || dtedLevel.length() != 5)
            return null;
        return (dtedLevel.charAt(4) - '0');
    }

    protected static String readClassLevel(String code) {
        if (null != code) {
            code = code.trim();
            if ("U".equalsIgnoreCase(code))
                return Keys.CLASS_LEVEL_UNCLASSIFIED;
            else if ("R".equalsIgnoreCase(code))
                return Keys.CLASS_LEVEL_RESTRICTED;
            else if ("C".equalsIgnoreCase(code))
                return Keys.CLASS_LEVEL_CONFIDENTIAL;
            else if ("S".equalsIgnoreCase(code))
                return Keys.CLASS_LEVEL_SECRET;
        }
        return null;
    }

    protected static void readACC(SeekableByteChannel theChannel, long offset, KV metadata) throws IOException {
        if (null == theChannel)
            return;

        theChannel.position(offset);

        byte[] acc = new byte[DTED.DTED_ACC_SIZE];
        ByteBuffer bb = ByteBuffer.wrap(acc).order(ByteOrder.BIG_ENDIAN);
        theChannel.read(bb);
        bb.flip();

        String id = new String(acc, 0, 3);
        if (!"ACC".equalsIgnoreCase(id)) {
            String reason = Logging.getMessage("DTED.UnexpectedRecordId", id, "ACC");
            String message = Logging.getMessage("DTED.BadFileFormat", reason);
            Logging.logger().severe(message);
            throw new IOException(message);
        }
    }

    protected static void readUHL(SeekableByteChannel theChannel, long offset, KV metadata) throws IOException {
        if (null == theChannel)
            return;

        theChannel.position(offset);

        byte[] uhl = new byte[DTED.DTED_UHL_SIZE];
        ByteBuffer bb = ByteBuffer.wrap(uhl).order(ByteOrder.BIG_ENDIAN);
        theChannel.read(bb);
        bb.flip();

        String id = new String(uhl, 0, 3);
        if (!"UHL".equalsIgnoreCase(id)) {
            String reason = Logging.getMessage("DTED.UnexpectedRecordId", id, "UHL");
            String message = Logging.getMessage("DTED.BadFileFormat", reason);
            Logging.logger().severe(message);
            throw new IOException(message);
        }

        metadata.set(Keys.BYTE_ORDER, Keys.BIG_ENDIAN);

        // DTED is always WGS84
        metadata.set(Keys.COORDINATE_SYSTEM, Keys.COORDINATE_SYSTEM_GEOGRAPHIC);
        metadata.set(Keys.PROJECTION_EPSG_CODE, GeoTiff.GCS.WGS_84);

        // DTED is elevation and always Int16
        metadata.set(Keys.PIXEL_FORMAT, Keys.ELEVATION);
        metadata.set(Keys.DATA_TYPE, Keys.INT16);
        metadata.set(Keys.ELEVATION_UNIT, Keys.UNIT_METER);
        metadata.set(Keys.MISSING_DATA_SIGNAL, (double) DTED.DTED_NODATA_VALUE);

        metadata.set(Keys.RASTER_PIXEL, Keys.RASTER_PIXEL_IS_POINT);

        //  Number of longitude lines
        int width = Integer.parseInt(new String(uhl, 47, 4));
        metadata.set(Keys.WIDTH, width);
        // Number of latitude points
        int height = Integer.parseInt(new String(uhl, 51, 4));
        metadata.set(Keys.HEIGHT, height);

        double pixelWidth = 1.0d / (width - 1);
        metadata.set(Keys.PIXEL_WIDTH, pixelWidth);

        double pixelHeight = 1.0d / (height - 1);
        metadata.set(Keys.PIXEL_HEIGHT, pixelHeight);

        // Longitude of origin (lower left corner) as DDDMMSSH
        Angle lon = DTED.readAngle(new String(uhl, 4, 8));

        // Latitude of origin (lower left corner) as DDDMMSSH
        Angle lat = DTED.readAngle(new String(uhl, 12, 8));

        // in DTED the original is always lower left (South-West) corner
        // and each file always contains 1" x 1" degrees tile
        // also, we should account 1 pixel overlap and half pixel shift

        Sector sector = Sector.fromDegrees(lat.degrees, lat.degrees + 1.0d, lon.degrees, lon.degrees + 1.0d);
        metadata.set(Keys.SECTOR, sector);

        // WW uses Upper Left corner as an Origin, let's calculate a new origin
        LatLon wwOrigin = LatLon.fromDegrees(sector.latMax, sector.lonMin);
        metadata.set(Keys.ORIGIN, wwOrigin);

        String classLevel = DTED.readClassLevel(new String(uhl, 32, 3));
        if (null != classLevel)
            metadata.set(Keys.CLASS_LEVEL, classLevel);
    }

    protected static void readDSI(SeekableByteChannel theChannel, long offset, KV metadata) throws IOException {
        if (null == theChannel)
            return;

        theChannel.position(offset);
        theChannel.position(offset);

        byte[] dsi = new byte[DTED.DTED_DSI_SIZE];
        ByteBuffer bb = ByteBuffer.wrap(dsi).order(ByteOrder.BIG_ENDIAN);
        theChannel.read(bb);
        bb.flip();

        String id = new String(dsi, 0, 3);
        if (!"DSI".equalsIgnoreCase(id)) {
            String reason = Logging.getMessage("DTED.UnexpectedRecordId", id, "DSI");
            String message = Logging.getMessage("DTED.BadFileFormat", reason);
            Logging.logger().severe(message);
            throw new IOException(message);
        }

        if (!metadata.hasKey(Keys.CLASS_LEVEL)) {
            String classLevel = DTED.readClassLevel(new String(dsi, 3, 1));
            if (null != classLevel)
                metadata.set(Keys.CLASS_LEVEL, classLevel);
        }

        Integer level = DTED.readLevel(new String(dsi, 59, 5));
        if (null != level)
            metadata.set(Keys.DTED_LEVEL, level);

        // Technically, there is no need to read next parameters because:
        // they are redundant (same data we get from UHL), and WW has no use for them 

    }
}