/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */

package gov.nasa.worldwind.util;

import gov.nasa.worldwind.exception.WWRuntimeException;

/**
 * @author Lado Garakanidze
 * @version $Id: NativeLibraryLoader.java 1171 2013-02-11 21:45:02Z dcollins $
 */

public class NativeLibraryLoader {
    public static void loadLibrary(String libName) throws WWRuntimeException, IllegalArgumentException {
        if (WWUtil.isEmpty(libName)) {
            String message = Logging.getMessage("nullValue.LibraryIsNull");
            throw new IllegalArgumentException(message);
        }

        try {
            System.loadLibrary(libName);
        }
        catch (Throwable ule) {
            String message = Logging.getMessage("generic.LibraryNotLoaded", libName, ule.getMessage());
            throw new WWRuntimeException(message);
        }
    }
}
