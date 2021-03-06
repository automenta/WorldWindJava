/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */

package gov.nasa.worldwind.formats.dds;

import gov.nasa.worldwind.Keys;
import gov.nasa.worldwind.avlist.KV;
import gov.nasa.worldwind.data.*;
import gov.nasa.worldwind.exception.WWRuntimeException;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.util.*;

import java.awt.image.*;
import java.io.*;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;

/**
 * @author Lado Garakanidze
 * @version $Id: DDSDecompressor.java 1171 2013-02-11 21:45:02Z dcollins $
 */

public class DDSDecompressor {
    public DDSDecompressor() {

    }

    /**
     * Reconstructs image raster from a DDS source. The source type may be one of the following: <ul><li>{@link
     * java.net.URL}</li> <li>{@link java.net.URI}</li> <li>{@link File}</li> <li>{@link String} containing a valid URL
     * description, a valid URI description, or a valid path to a local file.</li> </ul>
     *
     * @param source the source to convert to local file path.
     * @param params The AVList is a required parameter, Cannot be null. Requires AVK.Sector to be present.
     * @return MipMappedBufferedImageRaster if the DDS source contains mipmaps, otherwise returns a BufferedImageRaster
     * @throws Exception when source or params is null
     */
    public static DataRaster decompress(Object source, KV params) throws Exception {
        return DDSDecompressor.doDecompress(source, params);
    }

    protected static DataRaster doDecompress(Object source, KV params) throws Exception {
        if (null == params || !params.hasKey(Keys.SECTOR)) {
            String message = Logging.getMessage("generic.MissingRequiredParameter", Keys.SECTOR);
            Logging.logger().severe(message);
            throw new WWRuntimeException(message);
        }

        File file = WWIO.getFileForLocalAddress(source);
        if (null == file) {
            String message = Logging.getMessage("generic.UnrecognizedSourceType", source.getClass().getName());
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (!file.exists()) {
            String message = Logging.getMessage("generic.FileNotFound", file.getAbsolutePath());
            Logging.logger().severe(message);
            throw new FileNotFoundException(message);
        }

        if (!file.canRead()) {
            String message = Logging.getMessage("generic.FileNoReadPermission", file.getAbsolutePath());
            Logging.logger().severe(message);
            throw new IOException(message);
        }

        RandomAccessFile raf = null;
        FileChannel channel = null;
        DataRaster raster = null;

        try {
            raf = new RandomAccessFile(file, "r");
            channel = raf.getChannel();

            MappedByteBuffer buffer = DDSDecompressor.mapFile(channel, 0, channel.size());

            buffer.position(0);
            DDSHeader header = DDSHeader.readFrom(source);

            int width = header.getWidth();
            int height = header.getHeight();

            if (!WWMath.isPowerOfTwo(width) || !WWMath.isPowerOfTwo(height)) {
                String message = Logging.getMessage("generic.InvalidImageSize", width, height);
                Logging.logger().severe(message);
                throw new WWRuntimeException(message);
            }

            int mipMapCount = header.getMipMapCount();

            DDSPixelFormat pixelFormat = header.getPixelFormat();
            if (null == pixelFormat) {
                String reason = Logging.getMessage("generic.MissingRequiredParameter", "DDSD_PIXELFORMAT");
                String message = Logging.getMessage("generic.InvalidImageFormat", reason);
                Logging.logger().severe(message);
                throw new WWRuntimeException(message);
            }

            DXTDecompressor decompressor = null;

            int dxtFormat = pixelFormat.getFourCC();
            if (dxtFormat == DDSConstants.D3DFMT_DXT3) {
                decompressor = new DXT3Decompressor();
            } else if (dxtFormat == DDSConstants.D3DFMT_DXT1) {
                decompressor = new DXT1Decompressor();
            }

            if (null == decompressor) {
                String message = Logging.getMessage("generic.UnsupportedCodec", dxtFormat);
                Logging.logger().severe(message);
                throw new WWRuntimeException(message);
            }

            Sector sector = (Sector) params.get(Keys.SECTOR);
            params.set(Keys.PIXEL_FORMAT, Keys.IMAGE);

            if (mipMapCount == 0) {
                // read max resolution raster
                buffer.position(DDSConstants.DDS_DATA_OFFSET);
                BufferedImage image = decompressor.decompress(buffer, header.getWidth(), header.getHeight());
                raster = new BufferedImageRaster(sector, image, params);
            } else if (mipMapCount > 0) {
                ArrayList<BufferedImage> list = new ArrayList<>();

                int mmLength = header.getLinearSize();
                int mmOffset = DDSConstants.DDS_DATA_OFFSET;

                for (int i = 0; i < mipMapCount; i++) {
                    int zoomOut = (int) Math.pow(2.0d, i);

                    int mmWidth = header.getWidth() / zoomOut;
                    int mmHeight = header.getHeight() / zoomOut;

                    if (mmWidth < 4 || mmHeight < 4) {
                        break;
                    }

                    buffer.position(mmOffset);
                    BufferedImage image = decompressor.decompress(buffer, mmWidth, mmHeight);
                    list.add(image);

                    mmOffset += mmLength;
                    mmLength /= 4;
                }

                BufferedImage[] images = new BufferedImage[list.size()];
                images = list.toArray(images);

                raster = new MipMappedBufferedImageRaster(sector, images);
            }

            return raster;
        }
        finally {
            String name = file.getAbsolutePath();
            WWIO.closeStream(channel, name);
            WWIO.closeStream(raf, name);
        }
    }

    protected static MappedByteBuffer mapFile(FileChannel channel, long offset, long length)
        throws IllegalArgumentException, IOException {
        if (null == channel || !channel.isOpen()) {
            String message = Logging.getMessage("nullValue.ChannelIsNull");
            Logging.logger().fine(message);
            throw new IllegalArgumentException(message);
        }

        if (channel.size() < (offset + length)) {
            String reason = channel.size() + " < " + (offset + length);
            String message = Logging.getMessage("generic.LengthIsInvalid", reason);
            Logging.logger().severe(message);
            throw new IOException(message);
        }

        return channel.map(FileChannel.MapMode.READ_ONLY, offset, length);
    }
}