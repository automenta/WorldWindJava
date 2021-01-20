/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */

package gov.nasa.worldwind.data;

import gov.nasa.worldwind.Keys;
import gov.nasa.worldwind.avlist.KV;
import gov.nasa.worldwind.util.Logging;

/**
 * Implements a {@link DataRasterReaderFactory} with a default list of readers. The list includes the following
 * readers:
 * <pre>
 *  {@link RPFRasterReader}
 *  {@link DTEDRasterReader}
 *  {@link GDALDataRasterReader}
 *  {@link GeotiffRasterReader}
 *  {@link BILRasterReader}
 *  {@link ImageIORasterReader}
 *
 * </pre>
 * <p>
 * To specify a different factory, set the {@link Keys#DATA_RASTER_READER_FACTORY_CLASS_NAME}
 * value in {@link gov.nasa.worldwind.Configuration}, either directly or via the WorldWind configuration file. To add
 * readers to the default set, create a subclass of this class, override {@link #findReaderFor(Object, KV)}, and
 * specify the new class to the configuration.
 *
 * @author tag
 * @version $Id: BasicDataRasterReaderFactory.java 1511 2013-07-17 17:34:00Z dcollins $
 */
public class BasicDataRasterReaderFactory implements DataRasterReaderFactory {
    /**
     * The default list of readers.
     */
    protected final DataRasterReader[] readers = {
        // NOTE: Update the javadoc above if this list changes.
        new RPFRasterReader(),
        new DTEDRasterReader(),
        new GDALDataRasterReader(),
        new GeotiffRasterReader(),
        new BILRasterReader(),
        new ImageIORasterReader(),
    };

    /**
     * {@inheritDoc}
     */
    public DataRasterReader[] getReaders() {
        return readers;
    }

    /**
     * {@inheritDoc}
     */
    public DataRasterReader findReaderFor(Object source, KV params) {
        if (source == null) {
            String message = Logging.getMessage("nullValue.SourceIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        return findReaderFor(source, params, readers);
    }

    /**
     * {@inheritDoc}
     */
    public DataRasterReader findReaderFor(Object source, KV params, DataRasterReader[] readers) {
        if (source == null) {
            String message = Logging.getMessage("nullValue.SourceIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (readers == null) {
            String message = Logging.getMessage("nullValue.ReaderIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        for (DataRasterReader reader : readers) {
            if (reader != null && reader.canRead(source, params))
                return reader;
        }

        return null;
    }
}