/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.data;

import gov.nasa.worldwind.*;
import gov.nasa.worldwind.avlist.KV;
import gov.nasa.worldwind.formats.tiff.GeoTiff;
import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.util.*;

import java.nio.ByteBuffer;
import java.util.Calendar;

/**
 * @author dcollins
 * @version $Id: ByteBufferRaster.java 1171 2013-02-11 21:45:02Z dcollins $
 */
public class ByteBufferRaster extends BufferWrapperRaster {
    private final ByteBuffer byteBuffer;

    public ByteBufferRaster(int width, int height, Sector sector, ByteBuffer byteBuffer, KV list) {
        super(width, height, sector, BufferWrapper.wrap(byteBuffer, list), list);

        this.byteBuffer = byteBuffer;

        this.validateParameters(list);
    }

    public ByteBufferRaster(int width, int height, Sector sector, KV params) {
        this(width, height, sector, ByteBufferRaster.createCompatibleBuffer(width, height, params), params);
    }

    public static ByteBuffer createCompatibleBuffer(int width, int height, KV params) {
        if (width < 1) {
            String message = Logging.getMessage("generic.ArgumentOutOfRange", "width < 1");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        if (height < 1) {
            String message = Logging.getMessage("generic.ArgumentOutOfRange", "height < 1");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        if (params == null) {
            String message = Logging.getMessage("nullValue.ParamsIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        Object dataType = params.get(Keys.DATA_TYPE);

        int sizeOfDataType = 0;
        if (Keys.INT8.equals(dataType))
            sizeOfDataType = (Byte.SIZE / 8);
        else if (Keys.INT16.equals(dataType))
            sizeOfDataType = (Short.SIZE / 8);
        else if (Keys.INT32.equals(dataType))
            sizeOfDataType = (Integer.SIZE / 8);
        else if (Keys.FLOAT32.equals(dataType))
            sizeOfDataType = (Float.SIZE / 8);

        int sizeInBytes = sizeOfDataType * width * height;
        return ByteBuffer.allocate(sizeInBytes);
    }

    public static DataRaster createGeoreferencedRaster(KV params) {
        if (null == params) {
            String msg = Logging.getMessage("nullValue.AVListIsNull");
            Logging.logger().finest(msg);
            throw new IllegalArgumentException(msg);
        }

        if (!params.hasKey(Keys.WIDTH)) {
            String msg = Logging.getMessage("generic.MissingRequiredParameter", Keys.WIDTH);
            Logging.logger().finest(msg);
            throw new IllegalArgumentException(msg);
        }

        int width = (Integer) params.get(Keys.WIDTH);

        if (!(width > 0)) {
            String msg = Logging.getMessage("generic.InvalidWidth", width);
            Logging.logger().finest(msg);
            throw new IllegalArgumentException(msg);
        }

        if (!params.hasKey(Keys.HEIGHT)) {
            String msg = Logging.getMessage("generic.MissingRequiredParameter", Keys.HEIGHT);
            Logging.logger().finest(msg);
            throw new IllegalArgumentException(msg);
        }

        int height = (Integer) params.get(Keys.HEIGHT);

        if (!(height > 0)) {
            String msg = Logging.getMessage("generic.InvalidWidth", height);
            Logging.logger().finest(msg);
            throw new IllegalArgumentException(msg);
        }

        if (!params.hasKey(Keys.SECTOR)) {
            String msg = Logging.getMessage("generic.MissingRequiredParameter", Keys.SECTOR);
            Logging.logger().finest(msg);
            throw new IllegalArgumentException(msg);
        }

        Sector sector = (Sector) params.get(Keys.SECTOR);
        if (null == sector) {
            String msg = Logging.getMessage("nullValue.SectorIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        if (!params.hasKey(Keys.COORDINATE_SYSTEM)) {
            // assume Geodetic Coordinate System
            params.set(Keys.COORDINATE_SYSTEM, Keys.COORDINATE_SYSTEM_GEOGRAPHIC);
        }

        String cs = params.getStringValue(Keys.COORDINATE_SYSTEM);
        if (!params.hasKey(Keys.PROJECTION_EPSG_CODE)) {
            if (Keys.COORDINATE_SYSTEM_GEOGRAPHIC.equals(cs)) {
                // assume WGS84
                params.set(Keys.PROJECTION_EPSG_CODE, GeoTiff.GCS.WGS_84);
            } else {
                String msg = Logging.getMessage("generic.MissingRequiredParameter", Keys.PROJECTION_EPSG_CODE);
                Logging.logger().finest(msg);
                throw new IllegalArgumentException(msg);
            }
        }

        // if PIXEL_WIDTH is specified, we are not overriding it because UTM images
        // will have different pixel size
        if (!params.hasKey(Keys.PIXEL_WIDTH)) {
            if (Keys.COORDINATE_SYSTEM_GEOGRAPHIC.equals(cs)) {
                double pixelWidth = sector.lonDelta / width;
                params.set(Keys.PIXEL_WIDTH, pixelWidth);
            } else {
                String msg = Logging.getMessage("generic.MissingRequiredParameter", Keys.PIXEL_WIDTH);
                Logging.logger().finest(msg);
                throw new IllegalArgumentException(msg);
            }
        }

        // if PIXEL_HEIGHT is specified, we are not overriding it
        // because UTM images will have different pixel size
        if (!params.hasKey(Keys.PIXEL_HEIGHT)) {
            if (Keys.COORDINATE_SYSTEM_GEOGRAPHIC.equals(cs)) {
                double pixelHeight = sector.latDelta / height;
                params.set(Keys.PIXEL_HEIGHT, pixelHeight);
            } else {
                String msg = Logging.getMessage("generic.MissingRequiredParameter", Keys.PIXEL_HEIGHT);
                Logging.logger().finest(msg);
                throw new IllegalArgumentException(msg);
            }
        }

        if (!params.hasKey(Keys.PIXEL_FORMAT)) {
            String msg = Logging.getMessage("generic.MissingRequiredParameter", Keys.PIXEL_FORMAT);
            Logging.logger().finest(msg);
            throw new IllegalArgumentException(msg);
        } else {
            String pixelFormat = params.getStringValue(Keys.PIXEL_FORMAT);
            if (!Keys.ELEVATION.equals(pixelFormat) && !Keys.IMAGE.equals(pixelFormat)) {
                String msg = Logging.getMessage("generic.UnknownValueForKey", pixelFormat, Keys.PIXEL_FORMAT);
                Logging.logger().severe(msg);
                throw new IllegalArgumentException(msg);
            }
        }

        if (!params.hasKey(Keys.DATA_TYPE)) {
            String msg = Logging.getMessage("generic.MissingRequiredParameter", Keys.DATA_TYPE);
            Logging.logger().finest(msg);
            throw new IllegalArgumentException(msg);
        }

        // validate elevation parameters
        if (Keys.ELEVATION.equals(params.get(Keys.PIXEL_FORMAT))) {
            String type = params.getStringValue(Keys.DATA_TYPE);
            if (!Keys.FLOAT32.equals(type) && !Keys.INT16.equals(type)) {
                String msg = Logging.getMessage("generic.UnknownValueForKey", type, Keys.DATA_TYPE);
                Logging.logger().severe(msg);
                throw new IllegalArgumentException(msg);
            }
        }

        if (!params.hasKey(Keys.ORIGIN) && Keys.COORDINATE_SYSTEM_GEOGRAPHIC.equals(cs)) {
            // set UpperLeft corner as the origin, if not specified
            LatLon origin = new LatLon(sector.latMax(), sector.lonMin());
            params.set(Keys.ORIGIN, origin);
        }

        if (!params.hasKey(Keys.BYTE_ORDER)) {
            String msg = Logging.getMessage("generic.MissingRequiredParameter", Keys.BYTE_ORDER);
            Logging.logger().finest(msg);
            throw new IllegalArgumentException(msg);
        }

        if (!params.hasKey(Keys.DATE_TIME)) {
            // add NUL (\0) termination as required by TIFF v6 spec (20 bytes length)
            String timestamp = String.format("%1$tY:%1$tm:%1$td %tT\0", Calendar.getInstance());
            params.set(Keys.DATE_TIME, timestamp);
        }

        if (!params.hasKey(Keys.VERSION)) {
            params.set(Keys.VERSION, Version.getVersion());
        }

        return new ByteBufferRaster(width, height, sector, params);
    }

    private void validateParameters(KV list) throws IllegalArgumentException {
        this.doValidateParameters(list);
    }

    protected void doValidateParameters(KV list) throws IllegalArgumentException {
    }

    public ByteBuffer getByteBuffer() {
        return this.byteBuffer;
    }
}