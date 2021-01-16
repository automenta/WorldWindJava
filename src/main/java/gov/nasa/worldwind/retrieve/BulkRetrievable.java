/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.retrieve;

/**
 * Interface for classes whose data may be retrieved in bulk from its remote source. When used, will copy the requested
 * data to either the local WorldWind cache or a specified filestore. Data already contained in the specified location
 * is not recopied.
 *
 * @author Patrick Murris
 * @version $Id: BulkRetrievable.java 1171 2013-02-11 21:45:02Z dcollins $
 */
@Deprecated public interface BulkRetrievable {

    String name();
}
