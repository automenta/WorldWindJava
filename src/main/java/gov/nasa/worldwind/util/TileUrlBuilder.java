/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */

package gov.nasa.worldwind.util;

import java.net.*;

/**
 * @author lado
 * @version $Id: TileUrlBuilder.java 1171 2013-02-11 21:45:02Z dcollins $
 */
public interface TileUrlBuilder {
    URL getURL(Tile tile, String imageFormat) throws MalformedURLException;
}
