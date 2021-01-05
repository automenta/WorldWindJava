/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.data;

import gov.nasa.worldwind.avlist.*;
import gov.nasa.worldwind.formats.tiff.GeotiffImageReaderSpi;
import gov.nasa.worldwind.formats.worldfile.WorldFile;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.util.*;

import javax.imageio.*;
import javax.imageio.spi.*;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.*;
import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.logging.Level;

/**
 * @author dcollins
 * @version $Id: ImageIORasterReader.java 1171 2013-02-11 21:45:02Z dcollins $
 */
public class ImageIORasterReader extends AbstractDataRasterReader {
    static {
        IIORegistry.getDefaultInstance().registerServiceProvider(GeotiffImageReaderSpi.inst());
    }

    private boolean generateMipMaps;

    public ImageIORasterReader(boolean generateMipMaps) {
        super(ImageIO.getReaderMIMETypes(), ImageIORasterReader.getImageIOReaderSuffixes());
        this.generateMipMaps = generateMipMaps;
    }

    public ImageIORasterReader() {
        this(false);
    }

    private static ImageInputStream createInputStream(Object source) throws IOException {
        // ImageIO can create an ImageInputStream automatically from a File references or a standard I/O InputStream
        // reference. If the data source is a URL, or a string file path, then we must open an input stream ourselves.

        Object input = source;

        if (source instanceof URL) {
            input = ((URL) source).openStream();
        } else if (source instanceof CharSequence) {
            input = ImageIORasterReader.openInputStream(source.toString());
        }

        return ImageIO.createImageInputStream(input);
    }

    private static InputStream openInputStream(String path) throws IOException {
        Object streamOrException = WWIO.getFileOrResourceAsStream(path, null);
        if (streamOrException == null) {
            return null;
        } else if (streamOrException instanceof IOException) {
            throw (IOException) streamOrException;
        } else if (streamOrException instanceof Exception) {
            String message = Logging.getMessage("generic.ExceptionAttemptingToReadImageFile", path);
            Logging.logger().log(Level.SEVERE, message, streamOrException);
            throw new IOException(message);
        }

        return (InputStream) streamOrException;
    }

    private static ImageReader readerFor(ImageInputStream iis) {
        Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);
        if (!readers.hasNext()) {
            return null;
        }

        return readers.next();
    }

    private static String[] getImageIOReaderSuffixes() {
        Iterator<ImageReaderSpi> iter;
        try {
            iter = IIORegistry.getDefaultInstance().getServiceProviders(
                ImageReaderSpi.class, true);
        }
        catch (RuntimeException e) {
            return new String[0];
        }

        Collection<String> set = new HashSet<>();
        while (iter.hasNext()) {
            ImageReaderSpi spi = iter.next();
            String[] names = spi.getFileSuffixes();
            set.addAll(Arrays.asList(names));
        }

        String[] array = new String[set.size()];
        set.toArray(array);
        return array;
    }

    private static boolean canReadWorldFiles(Object source) {
        if (!(source instanceof File)) {
            return false;
        }

        try {
            File[] worldFiles = WorldFile.getWorldFiles((File) source);
            if (worldFiles == null || worldFiles.length == 0) {
                return false;
            }
        }
        catch (IOException e) {
            // Not interested in logging the exception, we only want to report the failure to read.
            return false;
        }

        return true;
    }

    private static void readImageDimension(Object source, AVList params) throws IOException {
        ImageInputStream iis = ImageIORasterReader.createInputStream(source);
        ImageReader reader = null;
        try (iis) {
            reader = ImageIORasterReader.readerFor(iis);
            if (reader == null) {
                String message = Logging.getMessage("generic.UnrecognizedImageSourceType", source);
                Logging.logger().severe(message);
                throw new IOException(message);
            }

            reader.setInput(iis, true, true);
            int width = reader.getWidth(0);
            int height = reader.getHeight(0);
            params.set(AVKey.WIDTH, width);
            params.set(AVKey.HEIGHT, height);
        }
        finally {
            if (reader != null) {
                reader.dispose();
            }
        }
    }

    private static void readWorldFiles(Object source, AVList params) throws IOException, FileNotFoundException {
        if (!(source instanceof File)) {
            String message = Logging.getMessage("DataRaster.CannotRead", source);
            Logging.logger().severe(message);
            throw new IOException(message);
        }

        // If an image is not specified in the metadata values, then attempt to construct the image size from other
        // parameters.
        Object o = params.get(AVKey.IMAGE);
        if (!(o instanceof BufferedImage)) {
            o = params.get(WorldFile.WORLD_FILE_IMAGE_SIZE);
            if (!(o instanceof int[])) {
                // If the image size is specified in the parameters WIDTH and HEIGHT, then translate them to the
                // WORLD_FILE_IMAGE_SIZE parameter.
                Object width = params.get(AVKey.WIDTH);
                Object height = params.get(AVKey.HEIGHT);
                if (width instanceof Integer && height instanceof Integer) {
                    int[] size = {(Integer) width, (Integer) height};
                    params.set(WorldFile.WORLD_FILE_IMAGE_SIZE, size);
                }
            }
        }

        File[] worldFiles = WorldFile.getWorldFiles((File) source);
        WorldFile.decodeWorldFiles(worldFiles, params);
    }

    public boolean isGenerateMipMaps() {
        return this.generateMipMaps;
    }

    public void setGenerateMipMaps(boolean generateMipMaps) {
        this.generateMipMaps = generateMipMaps;
    }

    protected boolean doCanRead(Object source, AVList params) {
        // Determine whether or not the data source can be read.
        //if (!this.canReadImage(source))
        //    return false;

        // If the data source doesn't already have all the necessary metadata, then we determine whether or not
        // the missing metadata can be read.
        Object o = (params != null) ? params.get(AVKey.SECTOR) : null;
        if (!(o instanceof Sector)) {
            if (!ImageIORasterReader.canReadWorldFiles(source)) {
                return false;
            }
        }

        if (null != params && !params.hasKey(AVKey.PIXEL_FORMAT)) {
            params.set(AVKey.PIXEL_FORMAT, AVKey.IMAGE);
        }

        return true;
    }

    protected DataRaster[] doRead(Object source, AVList params) throws IOException {
        ImageInputStream iis = ImageIORasterReader.createInputStream(source);
        BufferedImage image = ImageIO.read(iis);
        image = ImageUtil.toCompatibleImage(image);

        // If the data source doesn't already have all the necessary metadata, then we attempt to read the metadata.
        Object o = (params != null) ? params.get(AVKey.SECTOR) : null;
        if (!(o instanceof Sector)) {
            AVList values = new AVListImpl();
            values.set(AVKey.IMAGE, image);
            ImageIORasterReader.readWorldFiles(source, values);
            o = values.get(AVKey.SECTOR);
        }

        return new DataRaster[] {this.createRaster((Sector) o, image)};
    }

    protected void doReadMetadata(Object source, AVList params) throws IOException {
        Object width = params.get(AVKey.WIDTH);
        Object height = params.get(AVKey.HEIGHT);
        if (!(width instanceof Integer) || !(height instanceof Integer)) {
            ImageIORasterReader.readImageDimension(source, params);
        }

        Object sector = params.get(AVKey.SECTOR);
        if (!(sector instanceof Sector)) {
            ImageIORasterReader.readWorldFiles(source, params);
        }

        if (!params.hasKey(AVKey.PIXEL_FORMAT)) {
            params.set(AVKey.PIXEL_FORMAT, AVKey.IMAGE);
        }
    }

    protected DataRaster createRaster(Sector sector, BufferedImage image) {
        if (this.isGenerateMipMaps()) {
            return new MipMappedBufferedImageRaster(sector, image);
        } else {
            return new BufferedImageRaster(sector, image);
        }
    }
}
