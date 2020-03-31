/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */

package gov.nasa.worldwind.ogc.collada;

/**
 * Interface for resolving paths relative to a COLLADA document.
 *
 * @author pabercrombie
 * @version $Id: ColladaResourceResolver.java 654 2012-06-25 04:15:52Z pabercrombie $
 * @see ColladaRoot
 */
public interface ColladaResourceResolver
{
    /**
     * Resolve a file path.
     *
     * @param path A file path relative to a COLLADA document.
     *
     * @return An absolute path to the resource, or null if the path cannot be determined.
     *
     */
    String resolveFilePath(String path);
}
