/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */

package gov.nasa.worldwind.data;

import gov.nasa.worldwind.Keys;
import gov.nasa.worldwind.avlist.KV;
import gov.nasa.worldwind.exception.WWRuntimeException;
import gov.nasa.worldwind.formats.dds.*;
import gov.nasa.worldwind.util.*;

import java.io.IOException;
import java.util.logging.Level;

/**
 * @author Lado Garakanidze
 * @version $Id: DDSRasterReader.java 1171 2013-02-11 21:45:02Z dcollins $
 */

public class DDSRasterReader extends AbstractDataRasterReader {
    protected static final String[] ddsMimeTypes = {"image/dds"};
    protected static final String[] ddsSuffixes = {"dds"};

    public DDSRasterReader() {
        super(DDSRasterReader.ddsMimeTypes, DDSRasterReader.ddsSuffixes);
    }

    @Override
    protected boolean doCanRead(Object source, KV params) {
        try {
            DDSHeader header = DDSHeader.readFrom(source);
            if (header.getWidth() > 0 && header.getHeight() > 0) {
                if (null != params && !params.hasKey(Keys.PIXEL_FORMAT)) {
                    params.set(Keys.PIXEL_FORMAT, Keys.IMAGE);
                }

                return true;
            }
        }
        catch (Exception e) {
            String message = e.getMessage();
            message = (null == message && null != e.getCause()) ? e.getCause().getMessage() : message;
            Logging.logger().log(Level.FINEST, message, e);
        }
        return false;
    }

    @Override
    protected DataRaster[] doRead(Object source, KV params) throws IOException {
        if (null == params || !params.hasKey(Keys.SECTOR)) {
            String message = Logging.getMessage("generic.MissingRequiredParameter", Keys.SECTOR);
            Logging.logger().severe(message);
            throw new WWRuntimeException(message);
        }

        DataRaster raster = null;

        try {
            raster = DDSDecompressor.decompress(source, params);
            if (null != raster) {
                raster.set(Keys.PIXEL_FORMAT, Keys.IMAGE);
            }
        }
        catch (WWRuntimeException wwe) {
            throw new IOException(wwe.getMessage());
        }
        catch (Throwable t) {
            String message = t.getMessage();
            message = (WWUtil.isEmpty(message) && null != t.getCause()) ? t.getCause().getMessage() : message;
            Logging.logger().log(Level.FINEST, message, t);
            throw new IOException(message);
        }

        return (null != raster) ? new DataRaster[] {raster} : null;
    }

    @Override
    protected void doReadMetadata(Object source, KV params) throws IOException {
        try {
            DDSHeader header = DDSHeader.readFrom(source);
            if (null != params) {
                params.set(Keys.WIDTH, header.getWidth());
                params.set(Keys.HEIGHT, header.getHeight());
                params.set(Keys.PIXEL_FORMAT, Keys.IMAGE);
            }
        }
        catch (Exception e) {
            String message = e.getMessage();
            message = (WWUtil.isEmpty(message) && null != e.getCause()) ? e.getCause().getMessage() : message;
            Logging.logger().log(Level.FINEST, message, e);
            throw new IOException(message);
        }
    }
}