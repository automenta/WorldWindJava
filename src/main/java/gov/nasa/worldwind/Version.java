/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind;

/**
 * @author tag
 * @version $Id: Version.java 1171 2013-02-11 21:45:02Z dcollins $
 */
public class Version {

    private static final String MAJOR_VALUE = "2";
    private static final String MINOR_VALUE = "2";
    private static final String DOT_VALUE = "0";
    private static final String versionNumber = 'v' + Version.MAJOR_VALUE + '.' + Version.MINOR_VALUE + '.' + Version.DOT_VALUE;
    private static final String versionName = "NASA WorldWind Java";

    public static String getVersion() {
        return Version.versionName + ' ' + Version.versionNumber;
    }

    public static String getVersionName() {
        return Version.versionName;
    }

    public static String getVersionNumber() {
        return Version.versionNumber;
    }

    public static String getVersionMajorNumber() {
        return Version.MAJOR_VALUE;
    }

    public static String getVersionMinorNumber() {
        return Version.MINOR_VALUE;
    }

    public static String getVersionDotNumber() {
        return Version.DOT_VALUE;
    }
}
