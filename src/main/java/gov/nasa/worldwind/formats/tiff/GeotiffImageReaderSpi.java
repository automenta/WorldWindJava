/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */

package gov.nasa.worldwind.formats.tiff;

import gov.nasa.worldwind.Version;

import javax.imageio.ImageReader;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
import java.util.Locale;

/**
 * GeotiffImageReaderSpi is a singleton class. Multiply registering it should be harmless.
 *
 * @author brownrigg
 * @version $Id: GeotiffImageReaderSpi.java 1171 2013-02-11 21:45:02Z dcollins $
 */
public class GeotiffImageReaderSpi extends ImageReaderSpi {

    private static final String vendorName = Version.getVersionName();
    private static final String version = Version.getVersionNumber();
    private static final String[] names = {"tiff", "GTiff", "geotiff"};
    private static final String[] suffixes = {"tif", "tiff", "gtif"};
    private static final String[] mimeTypes = {"image/tiff", "image/geotiff"};
    private static final String readerClassname = "gov.nasa.worldwind.servers.wms.utilities.TiffImageReader";
    private static GeotiffImageReaderSpi theInstance;

    private GeotiffImageReaderSpi() {
        super(GeotiffImageReaderSpi.vendorName, GeotiffImageReaderSpi.version, GeotiffImageReaderSpi.names, GeotiffImageReaderSpi.suffixes, GeotiffImageReaderSpi.mimeTypes,
            GeotiffImageReaderSpi.readerClassname, new Class[] {ImageInputStream.class},
            null, false, null, null, null, null,
            false, null, null, null, null);
    }

    public static GeotiffImageReaderSpi inst() {
        if (GeotiffImageReaderSpi.theInstance == null)
            GeotiffImageReaderSpi.theInstance = new GeotiffImageReaderSpi();
        return GeotiffImageReaderSpi.theInstance;
    }

    @Override
    public boolean canDecodeInput(Object source) {
        if (!(source instanceof ImageInputStream))
            return false;

        ImageInputStream inp = (ImageInputStream) source;
        byte[] ifh = new byte[8];  // Tiff image-file header
        try {
            inp.mark();
            inp.readFully(ifh);
            inp.reset();
        }
        catch (IOException ex) {
            return false;
        }

        return (ifh[0] == 0x4D && ifh[1] == 0x4D && ifh[2] == 0x00 && ifh[3] == 0x2A) ||  // big-endian
            (ifh[0] == 0x49 && ifh[1] == 0x49 && ifh[2] == 0x2A && ifh[3] == 0x00);    // little-endian
    }

    @Override
    public ImageReader createReaderInstance(Object extension) {
        return new GeotiffImageReader(this);
    }

    @Override
    public String getDescription(Locale locale) {
        return "NASA WorldWind Geotiff Image Reader";
    }
}
