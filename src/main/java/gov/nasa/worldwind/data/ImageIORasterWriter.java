/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.data;

import gov.nasa.worldwind.avlist.*;
import gov.nasa.worldwind.formats.worldfile.WorldFile;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.util.WWIO;

import javax.imageio.ImageIO;
import javax.imageio.spi.*;
import java.awt.image.*;
import java.io.*;
import java.util.*;

/**
 * @author dcollins
 * @version $Id: ImageIORasterWriter.java 1171 2013-02-11 21:45:02Z dcollins $
 */
public class ImageIORasterWriter extends AbstractDataRasterWriter {
    private boolean writeGeoreferenceFiles;

    public ImageIORasterWriter(boolean writeGeoreferenceFiles) {
        super(ImageIO.getWriterMIMETypes(), getImageIOWriterSuffixes());

        this.writeGeoreferenceFiles = writeGeoreferenceFiles;
    }

    public ImageIORasterWriter() {
        this(true); // Enable writing georeference files by default.
    }

    private static String[] getImageIOWriterSuffixes() {
        Iterator<ImageWriterSpi> iter;
        try {
            iter = IIORegistry.getDefaultInstance().getServiceProviders(
                ImageWriterSpi.class, true);
        }
        catch (Exception e) {
            return new String[0];
        }

        Collection<String> set = new HashSet<>();
        while (iter.hasNext()) {
            ImageWriterSpi spi = iter.next();
            String[] names = spi.getFileSuffixes();
            set.addAll(Arrays.asList(names));
        }

        String[] array = new String[set.size()];
        set.toArray(array);
        return array;
    }

    public boolean isWriteGeoreferenceFiles() {
        return this.writeGeoreferenceFiles;
    }

    public void setWriteGeoreferenceFiles(boolean writeGeoreferenceFiles) {
        this.writeGeoreferenceFiles = writeGeoreferenceFiles;
    }

    protected boolean doCanWrite(DataRaster raster, String formatSuffix, File file) {
        return (raster instanceof BufferedImageRaster);
    }

    protected void doWrite(DataRaster raster, String formatSuffix, File file) throws IOException {
        this.writeImage(raster, formatSuffix, file);

        if (this.isWriteGeoreferenceFiles()) {
            AVList worldFileParams = new AVListImpl();
            this.initWorldFileParams(raster, worldFileParams);

            File dir = file.getParentFile();
            String base = WWIO.replaceSuffix(file.getName(), "");
            String suffix = WWIO.getSuffix(file.getName());
            String worldFileSuffix = this.suffixForWorldFile(suffix);

            this.writeImageMetadata(new File(dir, base + "." + worldFileSuffix), worldFileParams);
        }
    }

    protected void writeImage(DataRaster raster, String formatSuffix, File file) throws IOException {
        BufferedImageRaster bufferedImageRaster = (BufferedImageRaster) raster;
        BufferedImage image = bufferedImageRaster.getBufferedImage();
        ImageIO.write(image, formatSuffix, file);
    }

    protected void writeImageMetadata(File file, AVList values) throws IOException {
        Sector sector = (Sector) values.getValue(AVKey.SECTOR);
        int[] size = (int[]) values.getValue(WorldFile.WORLD_FILE_IMAGE_SIZE);

        double xPixelSize = sector.getDeltaLonDegrees() / size[0];
        double yPixelSize = -sector.getDeltaLatDegrees() / size[1];
        double xCoeff = 0.0;
        double yCoeff = 0.0;
        double xLocation = sector.lonMin().degrees + (xPixelSize * 0.5);
        double yLocation = sector.latMax().degrees + (yPixelSize * 0.5);

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

    protected String suffixForWorldFile(CharSequence suffix) {
        int length = suffix.length();
        if (length < 2)
            return "";

        StringBuilder sb = new StringBuilder();
        sb.append(Character.toLowerCase(suffix.charAt(0)));
        sb.append(Character.toLowerCase(suffix.charAt(length - 1)));
        sb.append("w");

        return sb.toString();
    }

    protected void initWorldFileParams(DataRaster raster, AVList worldFileParams) {
        int[] size = new int[2];
        size[0] = raster.getWidth();
        size[1] = raster.getHeight();
        worldFileParams.setValue(WorldFile.WORLD_FILE_IMAGE_SIZE, size);

        Sector sector = raster.getSector();
        worldFileParams.setValue(AVKey.SECTOR, sector);
    }
}