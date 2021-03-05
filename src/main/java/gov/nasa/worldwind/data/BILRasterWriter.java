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
import java.nio.*;

/**
 * @author dcollins
 * @version $Id: BILRasterWriter.java 1514 2013-07-22 23:17:23Z dcollins $
 */
public class BILRasterWriter extends AbstractDataRasterWriter {
    protected static final String[] bilMimeTypes = {"image/bil"};
    protected static final String[] bilSuffixes = {"bil"};

    protected boolean writeGeoreferenceFiles;

    public BILRasterWriter(boolean writeGeoreferenceFiles) {
        super(BILRasterWriter.bilMimeTypes, BILRasterWriter.bilSuffixes);

        this.writeGeoreferenceFiles = writeGeoreferenceFiles;
    }

    public BILRasterWriter() {
        this(true); // Enable writing georeference files by default.
    }

    private static Object getDataType(BufferWrapper buffer) {
        Object dataType = null;
        if (buffer instanceof BufferWrapper.ByteBufferWrapper)
            dataType = Keys.INT8;
        else if (buffer instanceof BufferWrapper.ShortBufferWrapper)
            dataType = Keys.INT16;
        else if (buffer instanceof BufferWrapper.IntBufferWrapper)
            dataType = Keys.INT32;
        else if (buffer instanceof BufferWrapper.FloatBufferWrapper)
            dataType = Keys.FLOAT32;

        return dataType;
    }

    private static Object getByteOrder(ByteBuffer byteBuffer) {
        return ByteOrder.LITTLE_ENDIAN.equals(byteBuffer.order()) ? Keys.LITTLE_ENDIAN : Keys.BIG_ENDIAN;
    }

    protected static void writeRaster(DataRaster raster, File file) throws IOException {
        ByteBufferRaster byteBufferRaster = (ByteBufferRaster) raster;
        ByteBuffer byteBuffer = byteBufferRaster.getByteBuffer();

        // Do not force changes to the underlying storage device.
        boolean forceFilesystemWrite = false;
        WWIO.saveBuffer(byteBuffer, file, false);
    }

    protected static void writeWorldFile(KV values, File file) throws FileNotFoundException {
        Sector sector = (Sector) values.get(Keys.SECTOR);
        int[] size = (int[]) values.get(WorldFile.WORLD_FILE_IMAGE_SIZE);

        double xPixelSize = sector.lonDelta / (size[0] - 1);
        double yPixelSize = -sector.latDelta / (size[1] - 1);
        double xCoeff = 0.0;
        double yCoeff = 0.0;
        double xLocation = sector.lonMin;
        double yLocation = sector.latMax;

        try (PrintWriter out = new PrintWriter(file)) {
            out.println(xPixelSize);
            out.println(xCoeff);
            //noinspection SuspiciousNameCombination
            out.println(yCoeff);
            //noinspection SuspiciousNameCombination
            out.println(yPixelSize);
            out.println(xLocation);
            //noinspection SuspiciousNameCombination
            out.println(yLocation);
        }
    }

    protected static void writeHdrFile(KV values, File file) throws FileNotFoundException {
        int[] size = (int[]) values.get(WorldFile.WORLD_FILE_IMAGE_SIZE);
        Object byteOrder = values.get(Keys.BYTE_ORDER);
        Object dataType = values.get(Keys.DATA_TYPE);

        int nBits = 0;
        if (Keys.INT8.equals(dataType))
            nBits = 8;
        else if (Keys.INT16.equals(dataType))
            nBits = 16;
        else if (Keys.INT32.equals(dataType) || Keys.FLOAT32.equals(dataType))
            nBits = 32;

        int rowBytes = size[0] * (nBits / 8);

        try (PrintWriter out = new PrintWriter(file)) {
            out.append("BYTEORDER      ").println(Keys.BIG_ENDIAN.equals(byteOrder) ? "M" : "I");
            out.append("LAYOUT         ").println("BIL");
            out.append("NROWS          ").println(size[1]);
            out.append("NCOLS          ").println(size[0]);
            out.append("NBANDS         ").println(1);
            out.append("NBITS          ").println(nBits);
            out.append("BANDROWBYTES   ").println(rowBytes);
            out.append("TOTALROWBYTES  ").println(rowBytes);
            out.append("BANDGAPBYTES   ").println(0);

            // This code expects the string "gov.nasa.worldwind.avkey.MissingDataValue", which now corresponds to the
            // key MISSING_DATA_REPLACEMENT.
            Object o = values.get(Keys.MISSING_DATA_REPLACEMENT);
            if (o != null)
                out.append("NODATA         ").println(o);
        }
    }

    protected static void initWorldFileParams(DataRaster raster, KV worldFileParams) {
        ByteBufferRaster byteBufferRaster = (ByteBufferRaster) raster;

        int[] size = new int[2];
        size[0] = raster.getWidth();
        size[1] = raster.getHeight();
        worldFileParams.set(WorldFile.WORLD_FILE_IMAGE_SIZE, size);

        Sector sector = raster.getSector();
        worldFileParams.set(Keys.SECTOR, sector);

        worldFileParams.set(Keys.BYTE_ORDER, BILRasterWriter.getByteOrder(byteBufferRaster.getByteBuffer()));
        worldFileParams.set(Keys.PIXEL_FORMAT, Keys.ELEVATION);
        worldFileParams.set(Keys.DATA_TYPE, BILRasterWriter.getDataType(byteBufferRaster.getBuffer()));

        double d = byteBufferRaster.getTransparentValue();
        if (d != Double.POSITIVE_INFINITY)
            worldFileParams.set(Keys.MISSING_DATA_REPLACEMENT, d);
    }

    protected static String validate(KV worldFileParams, Object dataSource) {
        StringBuilder sb = new StringBuilder();

        Object o = worldFileParams.get(WorldFile.WORLD_FILE_IMAGE_SIZE);
        if (!(o instanceof int[]))
            sb.append(sb.isEmpty() ? "" : ", ").append(Logging.getMessage("WorldFile.NoSizeSpecified", dataSource));

        o = worldFileParams.get(Keys.SECTOR);
        if (!(o instanceof Sector))
            sb.append(sb.isEmpty() ? "" : ", ").append(
                Logging.getMessage("WorldFile.NoSectorSpecified", dataSource));

        o = worldFileParams.get(Keys.BYTE_ORDER);
        if (!(o instanceof String))
            sb.append(sb.isEmpty() ? "" : ", ").append(
                Logging.getMessage("WorldFile.NoByteOrderSpecified", dataSource));

        o = worldFileParams.get(Keys.PIXEL_FORMAT);
        if (o == null)
            sb.append(sb.isEmpty() ? "" : ", ").append(
                Logging.getMessage("WorldFile.NoPixelFormatSpecified", dataSource));
        else if (!Keys.ELEVATION.equals(o))
            sb.append(sb.isEmpty() ? "" : ", ").append(
                Logging.getMessage("WorldFile.InvalidPixelFormat", dataSource));

        o = worldFileParams.get(Keys.DATA_TYPE);
        if (o == null)
            sb.append(sb.isEmpty() ? "" : ", ").append(
                Logging.getMessage("WorldFile.NoDataTypeSpecified", dataSource));

        if (sb.isEmpty())
            return null;

        return sb.toString();
    }

    public boolean isWriteGeoreferenceFiles() {
        return this.writeGeoreferenceFiles;
    }

    public void setWriteGeoreferenceFiles(boolean writeGeoreferenceFiles) {
        this.writeGeoreferenceFiles = writeGeoreferenceFiles;
    }

    protected boolean doCanWrite(DataRaster raster, String formatSuffix, File file) {
        return (raster instanceof ByteBufferRaster);
    }

    protected void doWrite(DataRaster raster, String formatSuffix, File file) throws IOException {
        BILRasterWriter.writeRaster(raster, file);

        if (this.isWriteGeoreferenceFiles()) {
            KV worldFileParams = new KVMap();
            BILRasterWriter.initWorldFileParams(raster, worldFileParams);

            String message = BILRasterWriter.validate(worldFileParams, raster);
            if (message != null) {
                Logging.logger().severe(message);
                throw new IOException(message);
            }

            File dir = file.getParentFile();
            String base = WWIO.replaceSuffix(file.getName(), "");

            BILRasterWriter.writeWorldFile(worldFileParams, new File(dir, base + ".blw"));
            BILRasterWriter.writeHdrFile(worldFileParams, new File(dir, base + ".hdr"));
        }
    }
}