/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.retrieve;

import gov.nasa.worldwind.cache.FileStore;
import gov.nasa.worldwind.geom.Sector;

/**
 * Interface for classes whose data may be retrieved in bulk from its remote source. When used, will copy the requested
 * data to either the local WorldWind cache or a specified filestore. Data already contained in the specified location
 * is not recopied.
 *
 * @author Patrick Murris
 * @version $Id: BulkRetrievable.java 1171 2013-02-11 21:45:02Z dcollins $
 */
@Deprecated public interface BulkRetrievable {
//
//    /**
//     * Estimates the amount of data, in bytes, that must be retrieved to the WorldWind data cache for a specified sector
//     * and resolution.
//     *
//     * @param sector     the sector for which to retrieve the data.
//     * @param resolution the resolution desired. All data within the specified sector up to and including this
//     *                   resolution is downloaded.
//     * @return the estimated data size, in bytes.
//     */
//    long getEstimatedMissingDataSize(Sector sector, double resolution);

//    /**
//     * Estimates the amount of data, in bytes, that must be retrieved to a specified filestore for a specified sector
//     * and resolution.
//     *
//     * @param sector     the sector for which to retrieve the data.
//     * @param resolution the resolution desired. All data within the specified sector up to and including this
//     *                   resolution is downloaded.
//     * @param fileStore  the location to place the data. If null, the current WorldWind cache is used.
//     * @return the estimated data size, in bytes.
//     */
//    long getEstimatedMissingDataSize(Sector sector, double resolution, FileStore fileStore);

    String name();
}
