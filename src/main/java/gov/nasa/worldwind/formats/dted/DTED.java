/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */

package gov.nasa.worldwind.formats.dted;

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
    protected static final long DTED_DSI_OFFSET = DTED_UHL_OFFSET + DTED_UHL_SIZE;
    protected static final long DTED_ACC_OFFSET = DTED_DSI_OFFSET + DTED_DSI_SIZE;
    protected static final long DTED_DATA_OFFSET = DTED_ACC_OFFSET + DTED_ACC_SIZE;

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

    public static AVList readMetadata(File file) throws IOException {
        AVList metadata = null;
        RandomAccessFile sourceFile = null;

        try {
            sourceFile = open(file);

            FileChannel channel = sourceFile.getChannel();

            metadata = new AVListImpl();

            readUHL(channel, DTED_UHL_OFFSET, metadata);
            readDSI(channel, DTED_DSI_OFFSET, metadata);
            readACC(channel, DTED_ACC_OFFSET, metadata);
        }
        finally {
            close(sourceFile);
        }

        return metadata;
    }

    public static DataRaster read(File file, AVList metadata) throws IOException {
        DataRaster raster = null;
        RandomAccessFile sourceFile = null;

        try {
            sourceFile = open(file);

            FileChannel channel = sourceFile.getChannel();

            readUHL(channel, DTED_UHL_OFFSET, metadata);
            readDSI(channel, DTED_DSI_OFFSET, metadata);
            readACC(channel, DTED_ACC_OFFSET, metadata);

            raster = readElevations(channel, DTED_DATA_OFFSET, metadata);
        }
        finally {
            close(sourceFile);
        }

        return raster;
    }

    protected static DataRaster readElevations(SeekableByteChannel theChannel, long offset, AVList metadata)
        throws IOException {
        if (null == theChannel)
            return null;

        ByteBufferRaster raster = (ByteBufferRaster) ByteBufferRaster.createGeoreferencedRaster(metadata);

        theChannel.position(offset);

        int width = (Integer) metadata.get(AVKey.WIDTH);
        int height = (Integer) metadata.get(AVKey.HEIGHT);

        int recordSize = REC_HEADER_SIZE + height * Short.SIZE / Byte.SIZE + REC_CHKSUM_SIZE;

        double min = +Double.MAX_VALUE;
        double max = -Double.MAX_VALUE;

        ByteBuffer bb = ByteBuffer.allocate(recordSize).order(ByteOrder.BIG_ENDIAN);
        for (int x = 0; x < width; x++) {
            theChannel.read(bb);
            bb.flip();

            int dataChkSum = 0;
            for (int i = 0; i < recordSize - REC_CHKSUM_SIZE;
                i++) // include header and elevations, exclude checksum itself
            {
                dataChkSum += 0xFF & bb.get(i);
            }

            ShortBuffer data = bb.asShortBuffer();
            for (int i = 0; i < height; i++) {
                double elev = data.get(i + 4); // skip 4 shorts of header
                int y = height - i - 1;

                if (elev != DTED_NODATA_VALUE && elev >= DTED_MIN_VALUE && elev <= DTED_MAX_VALUE) {
                    raster.setDoubleAtPosition(y, x, elev);
                    min = Math.min(elev, min);
                    max = Math.max(elev, max);
                }
                else {
                    // Interpret null DTED values and values outside the practical range of [-12000,+9000] as missing
                    // data. See MIL-PRF-89020B sections 3.11.2 and 3.11.3.
                    raster.setDoubleAtPosition(y, x, DTED_NODATA_VALUE);
                }
            }

            short hi = data.get(height + REC_CHKSUM_SIZE);
            short lo = data.get(height + REC_CHKSUM_SIZE + 1);

            int expectedChkSum = (0xFFFF & hi) << 16 | (0xFFFF & lo);

            if (expectedChkSum != dataChkSum) {
                String message = Logging.getMessage("DTED.DataRecordChecksumError", expectedChkSum, dataChkSum);
                Logging.logger().severe(message);
                throw new IOException(message);
            }
        }

        raster.set(AVKey.ELEVATION_MIN, min);
        raster.set(AVKey.ELEVATION_MAX, max);

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
                sb.insert(2, " ").insert(5, " ").insert(8, " ");
                return Angle.fromDMS(sb.toString());
            }
            case 8 -> {
                sb.insert(3, " ").insert(6, " ").insert(9, " ");
                return Angle.fromDMS(sb.toString());
            }
            case 9 -> {
                sb.insert(2, " ").insert(5, " ").insert(10, " ");
                sb.delete(8, 10);  // remove ".S" part, DTED spec not uses it anyway
                return Angle.fromDMS(sb.toString());
            }
            case 10 -> {
                sb.insert(3, " ").insert(6, " ").insert(11, " ");
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
                return AVKey.CLASS_LEVEL_UNCLASSIFIED;
            else if ("R".equalsIgnoreCase(code))
                return AVKey.CLASS_LEVEL_RESTRICTED;
            else if ("C".equalsIgnoreCase(code))
                return AVKey.CLASS_LEVEL_CONFIDENTIAL;
            else if ("S".equalsIgnoreCase(code))
                return AVKey.CLASS_LEVEL_SECRET;
        }
        return null;
    }

    protected static void readACC(SeekableByteChannel theChannel, long offset, AVList metadata) throws IOException {
        if (null == theChannel)
            return;

        theChannel.position(offset);

        byte[] acc = new byte[DTED_ACC_SIZE];
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

    protected static void readUHL(SeekableByteChannel theChannel, long offset, AVList metadata) throws IOException {
        if (null == theChannel)
            return;

        theChannel.position(offset);

        byte[] uhl = new byte[DTED_UHL_SIZE];
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

        metadata.set(AVKey.BYTE_ORDER, AVKey.BIG_ENDIAN);

        // DTED is always WGS84
        metadata.set(AVKey.COORDINATE_SYSTEM, AVKey.COORDINATE_SYSTEM_GEOGRAPHIC);
        metadata.set(AVKey.PROJECTION_EPSG_CODE, GeoTiff.GCS.WGS_84);

        // DTED is elevation and always Int16
        metadata.set(AVKey.PIXEL_FORMAT, AVKey.ELEVATION);
        metadata.set(AVKey.DATA_TYPE, AVKey.INT16);
        metadata.set(AVKey.ELEVATION_UNIT, AVKey.UNIT_METER);
        metadata.set(AVKey.MISSING_DATA_SIGNAL, (double) DTED_NODATA_VALUE);

        metadata.set(AVKey.RASTER_PIXEL, AVKey.RASTER_PIXEL_IS_POINT);

        //  Number of longitude lines
        int width = Integer.parseInt(new String(uhl, 47, 4));
        metadata.set(AVKey.WIDTH, width);
        // Number of latitude points
        int height = Integer.parseInt(new String(uhl, 51, 4));
        metadata.set(AVKey.HEIGHT, height);

        double pixelWidth = 1.0d / (width - 1);
        metadata.set(AVKey.PIXEL_WIDTH, pixelWidth);

        double pixelHeight = 1.0d / (height - 1);
        metadata.set(AVKey.PIXEL_HEIGHT, pixelHeight);

        // Longitude of origin (lower left corner) as DDDMMSSH
        Angle lon = readAngle(new String(uhl, 4, 8));

        // Latitude of origin (lower left corner) as DDDMMSSH
        Angle lat = readAngle(new String(uhl, 12, 8));

        // in DTED the original is always lower left (South-West) corner
        // and each file always contains 1" x 1" degrees tile
        // also, we should account 1 pixel overlap and half pixel shift

        Sector sector = Sector.fromDegrees(lat.degrees, lat.degrees + 1.0d, lon.degrees, lon.degrees + 1.0d);
        metadata.set(AVKey.SECTOR, sector);

        // WW uses Upper Left corner as an Origin, let's calculate a new origin
        LatLon wwOrigin = LatLon.fromDegrees(sector.latMax().degrees, sector.lonMin().degrees);
        metadata.set(AVKey.ORIGIN, wwOrigin);

        String classLevel = readClassLevel(new String(uhl, 32, 3));
        if (null != classLevel)
            metadata.set(AVKey.CLASS_LEVEL, classLevel);
    }

    protected static void readDSI(SeekableByteChannel theChannel, long offset, AVList metadata) throws IOException {
        if (null == theChannel)
            return;

        theChannel.position(offset);
        theChannel.position(offset);

        byte[] dsi = new byte[DTED_DSI_SIZE];
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

        if (!metadata.hasKey(AVKey.CLASS_LEVEL)) {
            String classLevel = readClassLevel(new String(dsi, 3, 1));
            if (null != classLevel)
                metadata.set(AVKey.CLASS_LEVEL, classLevel);
        }

        Integer level = readLevel(new String(dsi, 59, 5));
        if (null != level)
            metadata.set(AVKey.DTED_LEVEL, level);

        // Technically, there is no need to read next parameters because:
        // they are redundant (same data we get from UHL), and WW has no use for them 

    }
}
