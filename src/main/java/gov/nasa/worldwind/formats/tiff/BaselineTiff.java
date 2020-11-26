/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */

package gov.nasa.worldwind.formats.tiff;

import gov.nasa.worldwind.util.Logging;

/**
 * This is a package private class that contains methods of reading TIFF structures
 *
 * @author Lado Garakanidze
 * @version $Id: BaselineTiff.java 1171 2013-02-11 21:45:02Z dcollins $
 */
class BaselineTiff {
    public int width = Tiff.Undefined;
    public int height = Tiff.Undefined;
    public int samplesPerPixel = Tiff.Undefined;
    public int photometric = Tiff.Photometric.Undefined;
    public int rowsPerStrip = Tiff.Undefined;
    public int planarConfig = Tiff.Undefined;
    public int minSampleValue;
    public int maxSampleValue;

    public int[] sampleFormat = null;
    public int[] bitsPerSample = null;

    public String displayName = null;
    public String imageDescription = null;
    public String softwareVersion = null;
    public String dateTime = null;

    private BaselineTiff() {
    }

    public static BaselineTiff extract(TiffIFDEntry[] ifd, TIFFReader tiffReader) {
        if (null == ifd || null == tiffReader) {
            return null;
        }

        BaselineTiff tiff = new BaselineTiff();

        for (TiffIFDEntry entry : ifd) {
            try {
                // base TIFF tags
                switch (entry.tag) {
                    case Tiff.Tag.IMAGE_WIDTH -> tiff.width = (int) entry.asLong();
                    case Tiff.Tag.IMAGE_LENGTH -> tiff.height = (int) entry.asLong();
                    case Tiff.Tag.DOCUMENT_NAME -> tiff.displayName = tiffReader.readString(entry);
                    case Tiff.Tag.IMAGE_DESCRIPTION -> tiff.imageDescription = tiffReader.readString(entry);
                    case Tiff.Tag.SOFTWARE_VERSION -> tiff.softwareVersion = tiffReader.readString(entry);
                    case Tiff.Tag.DATE_TIME -> tiff.dateTime = tiffReader.readString(entry);
                    case Tiff.Tag.SAMPLES_PER_PIXEL -> tiff.samplesPerPixel = (int) entry.asLong();
                    case Tiff.Tag.PHOTO_INTERPRETATION -> tiff.photometric = (int) entry.asLong();
                    case Tiff.Tag.ROWS_PER_STRIP -> tiff.rowsPerStrip = (int) entry.asLong();
                    case Tiff.Tag.PLANAR_CONFIGURATION -> tiff.planarConfig = (int) entry.asLong();
                    case Tiff.Tag.SAMPLE_FORMAT -> tiff.sampleFormat = entry.getShortsAsInts();
                    case Tiff.Tag.BITS_PER_SAMPLE -> tiff.bitsPerSample = entry.getShortsAsInts();
                    case Tiff.Tag.MIN_SAMPLE_VALUE -> tiff.minSampleValue = entry.asShort();
                    case Tiff.Tag.MAX_SAMPLE_VALUE -> tiff.maxSampleValue = entry.asShort();
                }
            }
            catch (Exception e) {
                Logging.logger().finest(e.toString());
            }
        }
        return tiff;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("{ ");
        sb.append("width=").append(this.width).append(", ");
        sb.append("height=").append(this.height).append(", ");
        sb.append("samplesPerPixel=").append(this.samplesPerPixel).append(", ");
        sb.append("photometric=").append(this.photometric).append(", ");
        sb.append("rowsPerStrip=").append(this.rowsPerStrip).append(", ");
        sb.append("planarConfig=").append(this.planarConfig).append(", ");

        sb.append("sampleFormat=( ");
        if (null != this.sampleFormat) {
            for (int value : this.sampleFormat) {
                sb.append(value).append(" ");
            }
        }
        else
            sb.append(" NULL ");
        sb.append("), ");

        sb.append("bitsPerSample=( ");
        if (null != this.bitsPerSample) {
            for (int value : this.bitsPerSample) {
                sb.append(value).append(" ");
            }
        }
        else
            sb.append(" NULL ");
        sb.append("), ");

        sb.append("displayName=").append(this.displayName).append(", ");
        sb.append("imageDescription=").append(this.imageDescription).append(", ");
        sb.append("softwareVersion=").append(this.softwareVersion).append(", ");
        sb.append("dateTime=").append(this.dateTime);

        sb.append(" }");

        return sb.toString();
    }
}
