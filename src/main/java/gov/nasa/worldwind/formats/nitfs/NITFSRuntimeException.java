/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */

package gov.nasa.worldwind.formats.nitfs;

import gov.nasa.worldwind.util.Logging;

/**
 * @author Lado Garakanidze
 * @version $Id: NITFSRuntimeException.java 1171 2013-02-11 21:45:02Z dcollins $
 */
public final class NITFSRuntimeException extends RuntimeException {
    public NITFSRuntimeException() {
        super();
    }

    public NITFSRuntimeException(String messageID) {
        super(Logging.getMessage(messageID));
        NITFSRuntimeException.log(this.getMessage());
    }

    public NITFSRuntimeException(String messageID, String params) {
        super(Logging.getMessage(messageID) + params);
        NITFSRuntimeException.log(this.getMessage());
    }

    public NITFSRuntimeException(Throwable throwable) {
        super(throwable);
        NITFSRuntimeException.log(this.getMessage());
    }

    public NITFSRuntimeException(String messageID, Throwable throwable) {
        super(Logging.getMessage(messageID), throwable);
        NITFSRuntimeException.log(this.getMessage());
    }

    public NITFSRuntimeException(String messageID, String params, Throwable throwable) {
        super(Logging.getMessage(messageID) + params, throwable);
        NITFSRuntimeException.log(this.getMessage());
    }

    // TODO: Calling the logger from here causes the wrong method to be listed in the log record. Must call the
    // logger from the site with the problem and generating the exception.
    private static void log(String s) {
        Logging.logger().fine(s);
    }
}