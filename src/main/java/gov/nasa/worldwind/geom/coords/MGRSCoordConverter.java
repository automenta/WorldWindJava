/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.geom.coords;

import gov.nasa.worldwind.Keys;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.globes.Globe;

/**
 * Converter used to translate MGRS coordinate strings to and from geodetic latitude and longitude.
 *
 * @author Patrick Murris
 * @version $Id: MGRSCoordConverter.java 1171 2013-02-11 21:45:02Z dcollins $
 * @see gov.nasa.worldwind.geom.coords.MGRSCoord
 */

/**
 * Ported to Java from the NGA GeoTrans mgrs.c and mgrs.h code. Contains routines to convert from Geodetic to MGRS and
 * the other direction.
 *
 * @author Garrett Headley, Patrick Murris
 */
class MGRSCoordConverter {
    public static final int MGRS_NO_ERROR = 0;
    public static final int MGRS_STRING_ERROR = 0x0004;
    public static final double DEG_TO_RAD = 0.017453292519943295;   // PI/180
    private static final int MGRS_LAT_ERROR = 0x0001;
    private static final int MGRS_LON_ERROR = 0x0002;
    private static final int MGRS_PRECISION_ERROR = 0x0008;
    private static final int MGRS_A_ERROR = 0x0010;
    private static final int MGRS_INV_F_ERROR = 0x0020;
    private static final int MGRS_EASTING_ERROR = 0x0040;
    private static final int MGRS_NORTHING_ERROR = 0x0080;
    private static final int MGRS_ZONE_ERROR = 0x0100;
    private static final int MGRS_HEMISPHERE_ERROR = 0x0200;
    private static final int MGRS_LAT_WARNING = 0x0400;
    private static final int MGRS_NOZONE_WARNING = 0x0800;
    private static final int MGRS_UTM_ERROR = 0x1000;
    private static final int MGRS_UPS_ERROR = 0x2000;
    private static final double PI = 3.14159265358979323;
    private static final double PI_OVER_2 = (MGRSCoordConverter.PI / 2.0e0);
    private static final int MAX_PRECISION = 5;
    private static final double MIN_UTM_LAT = (-80 * MGRSCoordConverter.PI) / 180.0;    // -80 degrees in radians
    private static final double MAX_UTM_LAT = (84 * MGRSCoordConverter.PI) / 180.0;     // 84 degrees in radians
    private static final double RAD_TO_DEG = 57.29577951308232087;   // 180/PI

    private static final double MIN_EAST_NORTH = 0;
    private static final double MAX_EAST_NORTH = 4000000;
    private static final double TWOMIL = 2000000;
    private static final double ONEHT = 100000;

    private static final String CLARKE_1866 = "CC";
    private static final String CLARKE_1880 = "CD";
    private static final String BESSEL_1841 = "BR";
    private static final String BESSEL_1841_NAMIBIA = "BN";
    private static final int LETTER_A = 0;   /* ARRAY INDEX FOR LETTER A               */
    private static final int LETTER_B = 1;   /* ARRAY INDEX FOR LETTER B               */
    private static final int LETTER_C = 2;   /* ARRAY INDEX FOR LETTER C               */
    private static final int LETTER_D = 3;   /* ARRAY INDEX FOR LETTER D               */
    private static final int LETTER_E = 4;   /* ARRAY INDEX FOR LETTER E               */
    private static final int LETTER_F = 5;   /* ARRAY INDEX FOR LETTER E               */
    private static final int LETTER_G = 6;   /* ARRAY INDEX FOR LETTER H               */
    private static final int LETTER_H = 7;   /* ARRAY INDEX FOR LETTER H               */
    private static final int LETTER_I = 8;   /* ARRAY INDEX FOR LETTER I               */
    private static final int LETTER_J = 9;   /* ARRAY INDEX FOR LETTER J               */
    private static final int LETTER_K = 10;   /* ARRAY INDEX FOR LETTER J               */
    private static final int LETTER_L = 11;   /* ARRAY INDEX FOR LETTER L               */
    private static final int LETTER_M = 12;   /* ARRAY INDEX FOR LETTER M               */
    private static final int LETTER_N = 13;   /* ARRAY INDEX FOR LETTER N               */
    private static final int LETTER_O = 14;   /* ARRAY INDEX FOR LETTER O               */
    private static final int LETTER_P = 15;   /* ARRAY INDEX FOR LETTER P               */
    private static final int LETTER_Q = 16;   /* ARRAY INDEX FOR LETTER Q               */
    private static final int LETTER_R = 17;   /* ARRAY INDEX FOR LETTER R               */
    private static final int LETTER_S = 18;   /* ARRAY INDEX FOR LETTER S               */
    private static final int LETTER_T = 19;   /* ARRAY INDEX FOR LETTER S               */
    private static final int LETTER_U = 20;   /* ARRAY INDEX FOR LETTER U               */
    private static final int LETTER_V = 21;   /* ARRAY INDEX FOR LETTER V               */
    private static final int LETTER_W = 22;   /* ARRAY INDEX FOR LETTER W               */
    private static final int LETTER_X = 23;   /* ARRAY INDEX FOR LETTER X               */
    private static final int LETTER_Y = 24;   /* ARRAY INDEX FOR LETTER Y               */
    private static final int LETTER_Z = 25;   /* ARRAY INDEX FOR LETTER Z               */
    private static final int MGRS_LETTERS = 3;  /* NUMBER OF LETTERS IN MGRS              */
    private static final String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    // UPS Constants are in the following order:
    private static final long[][] upsConstants = {
        {MGRSCoordConverter.LETTER_A, MGRSCoordConverter.LETTER_J, MGRSCoordConverter.LETTER_Z, MGRSCoordConverter.LETTER_Z, 800000, 800000},
        {MGRSCoordConverter.LETTER_B, MGRSCoordConverter.LETTER_A, MGRSCoordConverter.LETTER_R, MGRSCoordConverter.LETTER_Z, 2000000, 800000},
        {MGRSCoordConverter.LETTER_Y, MGRSCoordConverter.LETTER_J, MGRSCoordConverter.LETTER_Z, MGRSCoordConverter.LETTER_P, 800000, 1300000},
        {MGRSCoordConverter.LETTER_Z, MGRSCoordConverter.LETTER_A, MGRSCoordConverter.LETTER_J, MGRSCoordConverter.LETTER_P, 2000000, 1300000}};
    // Latitude Band Constants are in the following order:
    private static final double[][] latitudeBandConstants = {
        {MGRSCoordConverter.LETTER_C, 1100000.0, -72.0, -80.5, 0.0},
        {MGRSCoordConverter.LETTER_D, 2000000.0, -64.0, -72.0, 2000000.0},
        {MGRSCoordConverter.LETTER_E, 2800000.0, -56.0, -64.0, 2000000.0},
        {MGRSCoordConverter.LETTER_F, 3700000.0, -48.0, -56.0, 2000000.0},
        {MGRSCoordConverter.LETTER_G, 4600000.0, -40.0, -48.0, 4000000.0},
        {MGRSCoordConverter.LETTER_H, 5500000.0, -32.0, -40.0, 4000000.0},   //smithjl last column to table
        {MGRSCoordConverter.LETTER_J, 6400000.0, -24.0, -32.0, 6000000.0},
        {MGRSCoordConverter.LETTER_K, 7300000.0, -16.0, -24.0, 6000000.0},
        {MGRSCoordConverter.LETTER_L, 8200000.0, -8.0, -16.0, 8000000.0},
        {MGRSCoordConverter.LETTER_M, 9100000.0, 0.0, -8.0, 8000000.0},
        {MGRSCoordConverter.LETTER_N, 0.0, 8.0, 0.0, 0.0},
        {MGRSCoordConverter.LETTER_P, 800000.0, 16.0, 8.0, 0.0},
        {MGRSCoordConverter.LETTER_Q, 1700000.0, 24.0, 16.0, 0.0},
        {MGRSCoordConverter.LETTER_R, 2600000.0, 32.0, 24.0, 2000000.0},
        {MGRSCoordConverter.LETTER_S, 3500000.0, 40.0, 32.0, 2000000.0},
        {MGRSCoordConverter.LETTER_T, 4400000.0, 48.0, 40.0, 4000000.0},
        {MGRSCoordConverter.LETTER_U, 5300000.0, 56.0, 48.0, 4000000.0},
        {MGRSCoordConverter.LETTER_V, 6200000.0, 64.0, 56.0, 6000000.0},
        {MGRSCoordConverter.LETTER_W, 7000000.0, 72.0, 64.0, 6000000.0},
        {MGRSCoordConverter.LETTER_X, 7900000.0, 84.5, 72.0, 6000000.0}};
    private final Globe globe;
    // Ellipsoid parameters, default to WGS 84
    private double MGRS_a = 6378137.0;          // Semi-major axis of ellipsoid in meters
    private double MGRS_f = 1 / 298.257223563;  // Flattening of ellipsoid
    private String MGRS_Ellipsoid_Code = "WE";
    private String MGRSString = "";
    private long ltr2_low_value;
    private long ltr2_high_value;       // this is only used for doing MGRS to xxx conversions.
    private double false_northing;
    private long lastLetter;
    private long last_error = MGRSCoordConverter.MGRS_NO_ERROR;
    private double north, south, min_northing, northing_offset;  //smithjl added north_offset
    private double latitude;
    private double longitude;

    MGRSCoordConverter(Globe globe) {
        this.globe = globe;
        if (globe != null) {
            double a = globe.getEquatorialRadius();
            double f = (globe.getEquatorialRadius() - globe.getPolarRadius()) / globe.getEquatorialRadius();
            setMGRSParameters(a, f, MGRS_Ellipsoid_Code);
        }
    }

    /**
     * The function Check_Zone receives an MGRS coordinate string. If a zone is given, MGRS_NO_ERROR is returned.
     * Otherwise, MGRS_NOZONE_WARNING. is returned.
     *
     * @param MGRSString the MGRS coordinate string.
     * @return the error code.
     */
    private static long checkZone(CharSequence MGRSString) {
        int i = 0;
        int j = 0;
        int num_digits = 0;
        long error_code = MGRSCoordConverter.MGRS_NO_ERROR;

        /* skip any leading blanks */
        while (i < MGRSString.length() && MGRSString.charAt(i) == ' ') {
            i++;
        }
        j = i;
        while (i < MGRSString.length() && Character.isDigit(MGRSString.charAt(i))) {
            i++;
        }
        num_digits = i - j;
        if (num_digits > 2)
            error_code |= MGRSCoordConverter.MGRS_STRING_ERROR;
        else if (num_digits <= 0)
            error_code |= MGRSCoordConverter.MGRS_NOZONE_WARNING;

        return error_code;
    }

    /**
     * The function Round_MGRS rounds the input value to the nearest integer, using the standard engineering rule. The
     * rounded integer value is then returned.
     *
     * @param value Value to be rounded
     * @return rounded double value
     */
    private static double roundMGRS(double value) {
        double ivalue = Math.floor(value);
        long ival;
        double fraction = value - ivalue;
        // double fraction = modf (value, &ivalue);

        ival = (long) (ivalue);
        if ((fraction > 0.5) || ((fraction == 0.5) && (ival % 2 == 1)))
            ival++;
        return ival;
    }

    /**
     * The function setMGRSParameters receives the ellipsoid parameters and sets the corresponding state variables. If
     * any errors occur, the error code(s) are returned by the function, otherwise MGRS_NO_ERROR is returned.
     *
     * @param mgrs_a        Semi-major axis of ellipsoid in meters
     * @param mgrs_f        Flattening of ellipsoid
     * @param ellipsoidCode 2-letter code for ellipsoid
     * @return error code
     */
    public long setMGRSParameters(double mgrs_a, double mgrs_f, String ellipsoidCode) {
        if (mgrs_a <= 0.0)
            return MGRSCoordConverter.MGRS_A_ERROR;

        if (mgrs_f == 0.0)
            return MGRSCoordConverter.MGRS_INV_F_ERROR;
        double inv_f = 1 / mgrs_f;
        if (inv_f < 250 || inv_f > 350)
            return MGRSCoordConverter.MGRS_INV_F_ERROR;

        MGRS_a = mgrs_a;
        MGRS_f = mgrs_f;
        MGRS_Ellipsoid_Code = ellipsoidCode;

        return MGRSCoordConverter.MGRS_NO_ERROR;
    }

    /**
     * @return Flattening of ellipsoid
     */
    public double getMGRS_f() {
        return MGRS_f;
    }

    /**
     * @return Semi-major axis of ellipsoid in meters
     */
    public double getMGRS_a() {
        return MGRS_a;
    }

    /**
     * @return Latitude band letter
     */
    private long getLastLetter() {
        return lastLetter;
    }

    /**
     * @return 2-letter code for ellipsoid
     */
    public String getMGRS_Ellipsoid_Code() {
        return MGRS_Ellipsoid_Code;
    }

    /**
     * The function ConvertMGRSToGeodetic converts an MGRS coordinate string to Geodetic (latitude and longitude)
     * coordinates according to the current ellipsoid parameters.  If any errors occur, the error code(s) are returned
     * by the function, otherwise UTM_NO_ERROR is returned.
     *
     * @param MGRSString MGRS coordinate string.
     * @return the error code.
     */
    public long convertMGRSToGeodetic(String MGRSString) {
        latitude = 0;
        longitude = 0;
        long error_code = MGRSCoordConverter.checkZone(MGRSString);
        if (error_code == MGRSCoordConverter.MGRS_NO_ERROR) {
            UTMCoord UTM = convertMGRSToUTM(MGRSString);
            if (UTM != null) {
                latitude = UTM.getLatitude().radians();
                longitude = UTM.getLongitude().radians();
            } else
                error_code = MGRSCoordConverter.MGRS_UTM_ERROR;
        } else if (error_code == MGRSCoordConverter.MGRS_NOZONE_WARNING) {
            // TODO: polar conversion
            UPSCoord UPS = convertMGRSToUPS(MGRSString);
            if (UPS != null) {
                latitude = UPS.getLatitude().radians();
                longitude = UPS.getLongitude().radians();
            } else
                error_code = MGRSCoordConverter.MGRS_UPS_ERROR;
        }
        return (error_code);
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    /**
     * The function Break_MGRS_String breaks down an MGRS coordinate string into its component parts. Updates
     * last_error.
     *
     * @param MGRSString the MGRS coordinate string
     * @return the corresponding <code>MGRSComponents</code> or <code>null</code>.
     */
    private MGRSComponents breakMGRSString(String MGRSString) {
        int num_digits;
        int num_letters;
        int i = 0;
        int j = 0;
        long error_code = MGRSCoordConverter.MGRS_NO_ERROR;

        int zone = 0;
        int[] letters = new int[3];
        long easting = 0;
        long northing = 0;
        int precision = 0;

        while (i < MGRSString.length() && MGRSString.charAt(i) == ' ') {
            i++;  /* skip any leading blanks */
        }
        j = i;
        while (i < MGRSString.length() && Character.isDigit(MGRSString.charAt(i))) {
            i++;
        }
        num_digits = i - j;
        if (num_digits <= 2)
            if (num_digits > 0) {
                /* get zone */
                zone = Integer.parseInt(MGRSString.substring(j, i));
                if ((zone < 1) || (zone > 60))
                    error_code |= MGRSCoordConverter.MGRS_STRING_ERROR;
            } else
                error_code |= MGRSCoordConverter.MGRS_STRING_ERROR;
        j = i;

        while (i < MGRSString.length() && Character.isLetter(MGRSString.charAt(i))) {
            i++;
        }
        num_letters = i - j;
        if (num_letters == 3) {
            /* get letters */
            letters[0] = MGRSCoordConverter.alphabet.indexOf(Character.toUpperCase(MGRSString.charAt(j)));
            if ((letters[0] == MGRSCoordConverter.LETTER_I) || (letters[0] == MGRSCoordConverter.LETTER_O))
                error_code |= MGRSCoordConverter.MGRS_STRING_ERROR;
            letters[1] = MGRSCoordConverter.alphabet.indexOf(Character.toUpperCase(MGRSString.charAt(j + 1)));
            if ((letters[1] == MGRSCoordConverter.LETTER_I) || (letters[1] == MGRSCoordConverter.LETTER_O))
                error_code |= MGRSCoordConverter.MGRS_STRING_ERROR;
            letters[2] = MGRSCoordConverter.alphabet.indexOf(Character.toUpperCase(MGRSString.charAt(j + 2)));
            if ((letters[2] == MGRSCoordConverter.LETTER_I) || (letters[2] == MGRSCoordConverter.LETTER_O))
                error_code |= MGRSCoordConverter.MGRS_STRING_ERROR;
        } else
            error_code |= MGRSCoordConverter.MGRS_STRING_ERROR;
        j = i;
        while (i < MGRSString.length() && Character.isDigit(MGRSString.charAt(i))) {
            i++;
        }
        num_digits = i - j;
        if ((num_digits <= 10) && (num_digits % 2 == 0)) {
            /* get easting, northing and precision */
            int n;
            double multiplier;
            /* get easting & northing */
            n = num_digits / 2;
            precision = n;
            if (n > 0) {
                easting = Integer.parseInt(MGRSString.substring(j, j + n));
                northing = Integer.parseInt(MGRSString.substring(j + n, j + n + n));
                multiplier = Math.pow(10.0, 5 - n);
                easting *= multiplier;
                northing *= multiplier;
            } else {
                easting = 0;
                northing = 0;
            }
        } else
            error_code |= MGRSCoordConverter.MGRS_STRING_ERROR;

        last_error = error_code;
        if (error_code == MGRSCoordConverter.MGRS_NO_ERROR)
            return new MGRSComponents(zone, letters[0], letters[1], letters[2], easting, northing, precision);

        return null;
    }

    /**
     * The function Get_Latitude_Band_Min_Northing receives a latitude band letter and uses the Latitude_Band_Table to
     * determine the minimum northing for that latitude band letter. Updates min_northing.
     *
     * @param letter Latitude band letter.
     * @return the error code.
     */
    private long getLatitudeBandMinNorthing(int letter) {
        long error_code = MGRSCoordConverter.MGRS_NO_ERROR;

        if ((letter >= MGRSCoordConverter.LETTER_C) && (letter <= MGRSCoordConverter.LETTER_H)) {
            min_northing = MGRSCoordConverter.latitudeBandConstants[letter - 2][1];
            northing_offset = MGRSCoordConverter.latitudeBandConstants[letter - 2][4];        //smithjl
        } else if ((letter >= MGRSCoordConverter.LETTER_J) && (letter <= MGRSCoordConverter.LETTER_N)) {
            min_northing = MGRSCoordConverter.latitudeBandConstants[letter - 3][1];
            northing_offset = MGRSCoordConverter.latitudeBandConstants[letter - 3][4];        //smithjl
        } else if ((letter >= MGRSCoordConverter.LETTER_P) && (letter <= MGRSCoordConverter.LETTER_X)) {
            min_northing = MGRSCoordConverter.latitudeBandConstants[letter - 4][1];
            northing_offset = MGRSCoordConverter.latitudeBandConstants[letter - 4][4];        //smithjl
        } else
            error_code |= MGRSCoordConverter.MGRS_STRING_ERROR;
        return error_code;
    }

    /**
     * The function Get_Latitude_Range receives a latitude band letter and uses the Latitude_Band_Table to determine the
     * latitude band boundaries for that latitude band letter. Updates north and south.
     *
     * @param letter the Latitude band letter
     * @return the error code.
     */
    private long getLatitudeRange(int letter) {
        long error_code = MGRSCoordConverter.MGRS_NO_ERROR;

        if ((letter >= MGRSCoordConverter.LETTER_C) && (letter <= MGRSCoordConverter.LETTER_H)) {
            north = MGRSCoordConverter.latitudeBandConstants[letter - 2][2] * MGRSCoordConverter.DEG_TO_RAD;
            south = MGRSCoordConverter.latitudeBandConstants[letter - 2][3] * MGRSCoordConverter.DEG_TO_RAD;
        } else if ((letter >= MGRSCoordConverter.LETTER_J) && (letter <= MGRSCoordConverter.LETTER_N)) {
            north = MGRSCoordConverter.latitudeBandConstants[letter - 3][2] * MGRSCoordConverter.DEG_TO_RAD;
            south = MGRSCoordConverter.latitudeBandConstants[letter - 3][3] * MGRSCoordConverter.DEG_TO_RAD;
        } else if ((letter >= MGRSCoordConverter.LETTER_P) && (letter <= MGRSCoordConverter.LETTER_X)) {
            north = MGRSCoordConverter.latitudeBandConstants[letter - 4][2] * MGRSCoordConverter.DEG_TO_RAD;
            south = MGRSCoordConverter.latitudeBandConstants[letter - 4][3] * MGRSCoordConverter.DEG_TO_RAD;
        } else
            error_code |= MGRSCoordConverter.MGRS_STRING_ERROR;

        return error_code;
    }

    /**
     * The function convertMGRSToUTM converts an MGRS coordinate string to UTM projection (zone, hemisphere, easting and
     * northing) coordinates according to the current ellipsoid parameters.  Updates last_error if any errors occured.
     *
     * @param MGRSString the MGRS coordinate string
     * @return the corresponding <code>UTMComponents</code> or <code>null</code>.
     */
    private UTMCoord convertMGRSToUTM(String MGRSString) {
        double scaled_min_northing;
        double grid_easting;        /* Easting for 100,000 meter grid square      */
        double grid_northing;       /* Northing for 100,000 meter grid square     */
        double temp_grid_northing = 0.0;
        double fabs_grid_northing = 0.0;
        double latitude = 0.0;
        double longitude = 0.0;
        double divisor = 1.0;
        long error_code = MGRSCoordConverter.MGRS_NO_ERROR;

        String hemisphere = Keys.NORTH;
        double easting = 0;
        double northing = 0;
        UTMCoord UTM = null;

        MGRSComponents MGRS = breakMGRSString(MGRSString);
        if (MGRS == null)
            error_code |= MGRSCoordConverter.MGRS_STRING_ERROR;
        else {
            if ((MGRS.latitudeBand == MGRSCoordConverter.LETTER_X) && ((MGRS.zone == 32) || (MGRS.zone == 34) || (MGRS.zone == 36)))
                error_code |= MGRSCoordConverter.MGRS_STRING_ERROR;
            else {
                if (MGRS.latitudeBand < MGRSCoordConverter.LETTER_N)
                    hemisphere = Keys.SOUTH;
                else
                    hemisphere = Keys.NORTH;

                getGridValues(MGRS.zone);

                // Check that the second letter of the MGRS string is within
                // the range of valid second letter values
                // Also check that the third letter is valid
                if ((MGRS.squareLetter1 < ltr2_low_value) || (MGRS.squareLetter1 > ltr2_high_value) ||
                    (MGRS.squareLetter2 > MGRSCoordConverter.LETTER_V))
                    error_code |= MGRSCoordConverter.MGRS_STRING_ERROR;

                if (error_code == MGRSCoordConverter.MGRS_NO_ERROR) {
                    grid_northing =
                        (MGRS.squareLetter2) * MGRSCoordConverter.ONEHT;  //   smithjl  commented out + false_northing;
                    grid_easting = ((MGRS.squareLetter1) - ltr2_low_value + 1) * MGRSCoordConverter.ONEHT;
                    if ((ltr2_low_value == MGRSCoordConverter.LETTER_J) && (MGRS.squareLetter1 > MGRSCoordConverter.LETTER_O))
                        grid_easting = grid_easting - MGRSCoordConverter.ONEHT;

                    if (MGRS.squareLetter2 > MGRSCoordConverter.LETTER_O)
                        grid_northing = grid_northing - MGRSCoordConverter.ONEHT;

                    if (MGRS.squareLetter2 > MGRSCoordConverter.LETTER_I)
                        grid_northing = grid_northing - MGRSCoordConverter.ONEHT;

                    if (grid_northing >= MGRSCoordConverter.TWOMIL)
                        grid_northing = grid_northing - MGRSCoordConverter.TWOMIL;

                    error_code = getLatitudeBandMinNorthing(MGRS.latitudeBand);
                    if (error_code == MGRSCoordConverter.MGRS_NO_ERROR) {
                        /*smithjl Deleted code here and added this*/
                        grid_northing = grid_northing - false_northing;

                        if (grid_northing < 0.0)
                            grid_northing += MGRSCoordConverter.TWOMIL;

                        grid_northing += northing_offset;

                        if (grid_northing < min_northing)
                            grid_northing += MGRSCoordConverter.TWOMIL;

                        /* smithjl End of added code */

                        easting = grid_easting + MGRS.easting;
                        northing = grid_northing + MGRS.northing;

                        try {
                            UTM = UTMCoord.fromUTM(MGRS.zone, hemisphere, easting, northing, globe);
                            latitude = UTM.getLatitude().radians();
                            divisor = Math.pow(10.0, MGRS.precision);
                            error_code = getLatitudeRange(MGRS.latitudeBand);
                            if (error_code == MGRSCoordConverter.MGRS_NO_ERROR) {
                                if (!(((south - MGRSCoordConverter.DEG_TO_RAD / divisor) <= latitude)
                                    && (latitude <= (north + MGRSCoordConverter.DEG_TO_RAD / divisor))))
                                    error_code |= MGRSCoordConverter.MGRS_LAT_WARNING;
                            }
                        }
                        catch (RuntimeException e) {
                            error_code = MGRSCoordConverter.MGRS_UTM_ERROR;
                        }
                    }
                }
            }
        }

        last_error = error_code;
        if (error_code == MGRSCoordConverter.MGRS_NO_ERROR || error_code == MGRSCoordConverter.MGRS_LAT_WARNING)
            return UTM;

        return null;
    } /* Convert_MGRS_To_UTM */

    /**
     * The function convertGeodeticToMGRS converts Geodetic (latitude and longitude) coordinates to an MGRS coordinate
     * string, according to the current ellipsoid parameters.  If any errors occur, the error code(s) are returned by
     * the function, otherwise MGRS_NO_ERROR is returned.
     *
     * @param latitude  Latitude in radians
     * @param longitude Longitude in radian
     * @param precision Precision level of MGRS string
     * @return error code
     */
    public long convertGeodeticToMGRS(double latitude, double longitude, int precision) {
        String Hemisphere = Keys.NORTH;
        double Easting = 0.0;
        double Northing = 0.0;

        MGRSString = "";

        long error_code = MGRSCoordConverter.MGRS_NO_ERROR;
        if ((latitude < -MGRSCoordConverter.PI_OVER_2) || (latitude > MGRSCoordConverter.PI_OVER_2)) { /* Latitude out of range */
            error_code = MGRSCoordConverter.MGRS_LAT_ERROR;
        }

        if ((longitude < -MGRSCoordConverter.PI) || (longitude > (2 * MGRSCoordConverter.PI))) { /* Longitude out of range */
            error_code = MGRSCoordConverter.MGRS_LON_ERROR;
        }

        if ((precision < 0) || (precision > MGRSCoordConverter.MAX_PRECISION))
            error_code = MGRSCoordConverter.MGRS_PRECISION_ERROR;

        if (error_code == MGRSCoordConverter.MGRS_NO_ERROR) {
            if ((latitude < MGRSCoordConverter.MIN_UTM_LAT) || (latitude > MGRSCoordConverter.MAX_UTM_LAT)) {
                // TODO: polar
                try {
                    UPSCoord UPS =
                        UPSCoord.fromLatLon(Angle.fromRadians(latitude), Angle.fromRadians(longitude), globe);
                    error_code |= convertUPSToMGRS(UPS.getHemisphere(), UPS.getEasting(),
                        UPS.getNorthing(), precision);
                }
                catch (RuntimeException e) {
                    error_code = MGRSCoordConverter.MGRS_UPS_ERROR;
                }
            } else {
                try {
                    UTMCoord UTM =
                        UTMCoord.fromLatLon(Angle.fromRadians(latitude), Angle.fromRadians(longitude), globe);
                    error_code |= convertUTMToMGRS(UTM.getZone(), latitude, UTM.getEasting(),
                        UTM.getNorthing(), precision);
                }
                catch (RuntimeException e) {
                    error_code = MGRSCoordConverter.MGRS_UTM_ERROR;
                }
            }
        }

        return error_code;
    }

    /**
     * @return converted MGRS string
     */
    public String getMGRSString() {
        return MGRSString;
    }

    /**
     * The function Convert_UPS_To_MGRS converts UPS (hemisphere, easting, and northing) coordinates to an MGRS
     * coordinate string according to the current ellipsoid parameters.  If any errors occur, the error code(s) are
     * returned by the function, otherwise MGRS_NO_ERROR is returned.
     *
     * @param Hemisphere Hemisphere either, {@link Keys#NORTH} or {@link Keys#SOUTH}.
     * @param Easting    Easting/X in meters
     * @param Northing   Northing/Y in meters
     * @param Precision  Precision level of MGRS string
     * @return error value
     */
    private long convertUPSToMGRS(String Hemisphere, Double Easting, Double Northing, long Precision) {
        double false_easting;       /* False easting for 2nd letter                 */
        double false_northing;      /* False northing for 3rd letter                */
        double grid_easting;        /* Easting used to derive 2nd letter of MGRS    */
        double grid_northing;       /* Northing used to derive 3rd letter of MGRS   */
        int ltr2_low_value;        /* 2nd letter range - low number                */
        long[] letters = new long[MGRSCoordConverter.MGRS_LETTERS];  /* Number location of 3 letters in alphabet     */
        double divisor;
        int index;
        long error_code = MGRSCoordConverter.MGRS_NO_ERROR;

        if (!Keys.NORTH.equals(Hemisphere) && !Keys.SOUTH.equals(Hemisphere))
            error_code |= MGRSCoordConverter.MGRS_HEMISPHERE_ERROR;
        if ((Easting < MGRSCoordConverter.MIN_EAST_NORTH) || (Easting > MGRSCoordConverter.MAX_EAST_NORTH))
            error_code |= MGRSCoordConverter.MGRS_EASTING_ERROR;
        if ((Northing < MGRSCoordConverter.MIN_EAST_NORTH) || (Northing > MGRSCoordConverter.MAX_EAST_NORTH))
            error_code |= MGRSCoordConverter.MGRS_NORTHING_ERROR;
        if ((Precision < 0) || (Precision > MGRSCoordConverter.MAX_PRECISION))
            error_code |= MGRSCoordConverter.MGRS_PRECISION_ERROR;

        if (error_code == MGRSCoordConverter.MGRS_NO_ERROR) {
            divisor = Math.pow(10.0, (5 - Precision));
            Easting = MGRSCoordConverter.roundMGRS(Easting / divisor) * divisor;
            Northing = MGRSCoordConverter.roundMGRS(Northing / divisor) * divisor;

            if (Keys.NORTH.equals(Hemisphere)) {
                if (Easting >= MGRSCoordConverter.TWOMIL)
                    letters[0] = MGRSCoordConverter.LETTER_Z;
                else
                    letters[0] = MGRSCoordConverter.LETTER_Y;

                index = (int) letters[0] - 22;
                ltr2_low_value = (int) MGRSCoordConverter.upsConstants[index][1];
                false_easting = MGRSCoordConverter.upsConstants[index][4];
                false_northing = MGRSCoordConverter.upsConstants[index][5];
            } else // AVKey.SOUTH.equals(Hemisphere)
            {
                if (Easting >= MGRSCoordConverter.TWOMIL)
                    letters[0] = MGRSCoordConverter.LETTER_B;
                else
                    letters[0] = MGRSCoordConverter.LETTER_A;

                ltr2_low_value = (int) MGRSCoordConverter.upsConstants[(int) letters[0]][1];
                false_easting = MGRSCoordConverter.upsConstants[(int) letters[0]][4];
                false_northing = MGRSCoordConverter.upsConstants[(int) letters[0]][5];
            }

            grid_northing = Northing;
            grid_northing = grid_northing - false_northing;
            letters[2] = (int) (grid_northing / MGRSCoordConverter.ONEHT);

            if (letters[2] > MGRSCoordConverter.LETTER_H)
                letters[2] = letters[2] + 1;

            if (letters[2] > MGRSCoordConverter.LETTER_N)
                letters[2] = letters[2] + 1;

            grid_easting = Easting;
            grid_easting = grid_easting - false_easting;
            letters[1] = ltr2_low_value + ((int) (grid_easting / MGRSCoordConverter.ONEHT));

            if (Easting < MGRSCoordConverter.TWOMIL) {
                if (letters[1] > MGRSCoordConverter.LETTER_L)
                    letters[1] = letters[1] + 3;

                if (letters[1] > MGRSCoordConverter.LETTER_U)
                    letters[1] = letters[1] + 2;
            } else {
                if (letters[1] > MGRSCoordConverter.LETTER_C)
                    letters[1] = letters[1] + 2;

                if (letters[1] > MGRSCoordConverter.LETTER_H)
                    letters[1] = letters[1] + 1;

                if (letters[1] > MGRSCoordConverter.LETTER_L)
                    letters[1] = letters[1] + 3;
            }

            makeMGRSString(0, letters, Easting, Northing, Precision);
        }
        return (error_code);
    }

    /**
     * The function UTM_To_MGRS calculates an MGRS coordinate string based on the zone, latitude, easting and northing.
     *
     * @param Zone      Zone number
     * @param Latitude  Latitude in radians
     * @param Easting   Easting
     * @param Northing  Northing
     * @param Precision Precision
     * @return error code
     */
    private long convertUTMToMGRS(long Zone, double Latitude, double Easting, double Northing, long Precision) {
        double grid_easting;        /* Easting used to derive 2nd letter of MGRS   */
        double grid_northing;       /* Northing used to derive 3rd letter of MGRS  */
        long[] letters = new long[MGRSCoordConverter.MGRS_LETTERS];  /* Number location of 3 letters in alphabet    */
        double divisor;
        long error_code;

        /* Round easting and northing values */
        divisor = Math.pow(10.0, (5 - Precision));
        Easting = MGRSCoordConverter.roundMGRS(Easting / divisor) * divisor;
        Northing = MGRSCoordConverter.roundMGRS(Northing / divisor) * divisor;

        getGridValues(Zone);

        error_code = getLatitudeLetter(Latitude);
        letters[0] = getLastLetter();

        if (error_code == MGRSCoordConverter.MGRS_NO_ERROR) {
            grid_northing = Northing;
            if (grid_northing == 1.0e7)
                grid_northing = grid_northing - 1.0;

            while (grid_northing >= MGRSCoordConverter.TWOMIL) {
                grid_northing = grid_northing - MGRSCoordConverter.TWOMIL;
            }
            grid_northing = grid_northing + false_northing;   //smithjl

            if (grid_northing >= MGRSCoordConverter.TWOMIL)                     //smithjl
                grid_northing = grid_northing - MGRSCoordConverter.TWOMIL;        //smithjl

            letters[2] = (long) (grid_northing / MGRSCoordConverter.ONEHT);
            if (letters[2] > MGRSCoordConverter.LETTER_H)
                letters[2] = letters[2] + 1;

            if (letters[2] > MGRSCoordConverter.LETTER_N)
                letters[2] = letters[2] + 1;

            grid_easting = Easting;
            if (((letters[0] == MGRSCoordConverter.LETTER_V) && (Zone == 31)) && (grid_easting == 500000.0))
                grid_easting = grid_easting - 1.0; /* SUBTRACT 1 METER */

            letters[1] = ltr2_low_value + ((long) (grid_easting / MGRSCoordConverter.ONEHT) - 1);
            if ((ltr2_low_value == MGRSCoordConverter.LETTER_J) && (letters[1] > MGRSCoordConverter.LETTER_N))
                letters[1] = letters[1] + 1;

            makeMGRSString(Zone, letters, Easting, Northing, Precision);
        }
        return error_code;
    }

    /**
     * The function Get_Grid_Values sets the letter range used for the 2nd letter in the MGRS coordinate string, based
     * on the set number of the utm zone. It also sets the false northing using a value of A for the second letter of
     * the grid square, based on the grid pattern and set number of the utm zone.
     * <p>
     * Key values that are set in this function include:  ltr2_low_value, ltr2_high_value, and false_northing.
     *
     * @param zone Zone number
     */
    private void getGridValues(long zone) {
        long set_number;    /* Set number (1-6) based on UTM zone number */
        long aa_pattern;    /* Pattern based on ellipsoid code */

        set_number = zone % 6;

        if (set_number == 0)
            set_number = 6;

        if (MGRS_Ellipsoid_Code.compareTo(MGRSCoordConverter.CLARKE_1866) == 0 || MGRS_Ellipsoid_Code.compareTo(
            MGRSCoordConverter.CLARKE_1880) == 0 ||
            MGRS_Ellipsoid_Code.compareTo(MGRSCoordConverter.BESSEL_1841) == 0 || MGRS_Ellipsoid_Code.compareTo(
            MGRSCoordConverter.BESSEL_1841_NAMIBIA) == 0)
            aa_pattern = 0L;
        else
            aa_pattern = 1L;

        if ((set_number == 1) || (set_number == 4)) {
            ltr2_low_value = MGRSCoordConverter.LETTER_A;
            ltr2_high_value = MGRSCoordConverter.LETTER_H;
        } else if ((set_number == 2) || (set_number == 5)) {
            ltr2_low_value = MGRSCoordConverter.LETTER_J;
            ltr2_high_value = MGRSCoordConverter.LETTER_R;
        } else if ((set_number == 3) || (set_number == 6)) {
            ltr2_low_value = MGRSCoordConverter.LETTER_S;
            ltr2_high_value = MGRSCoordConverter.LETTER_Z;
        }

        /* False northing at A for second letter of grid square */
        if (aa_pattern == 1L) {
            if ((set_number % 2) == 0)
                false_northing = 500000.0;             //smithjl was 1500000
            else
                false_northing = 0.0;
        } else {
            if ((set_number % 2) == 0)
                false_northing = 1500000.0;            //smithjl was 500000
            else
                false_northing = 1000000.00;
        }
    }

    /**
     * The function Get_Latitude_Letter receives a latitude value and uses the Latitude_Band_Table to determine the
     * latitude band letter for that latitude.
     *
     * @param latitude latitude to turn into code
     * @return error code
     */
    private long getLatitudeLetter(double latitude) {
        double temp;
        long error_code = MGRSCoordConverter.MGRS_NO_ERROR;
        double lat_deg = latitude * MGRSCoordConverter.RAD_TO_DEG;

        if (lat_deg >= 72 && lat_deg < 84.5)
            lastLetter = MGRSCoordConverter.LETTER_X;
        else if (lat_deg > -80.5 && lat_deg < 72) {
            temp = ((latitude + (80.0 * MGRSCoordConverter.DEG_TO_RAD)) / (8.0 * MGRSCoordConverter.DEG_TO_RAD)) + 1.0e-12;
            lastLetter = (long) MGRSCoordConverter.latitudeBandConstants[(int) temp][0];
        } else
            error_code |= MGRSCoordConverter.MGRS_LAT_ERROR;

        return error_code;
    }

    /**
     * The function Make_MGRS_String constructs an MGRS string from its component parts.
     *
     * @param Zone      UTM Zone
     * @param Letters   MGRS coordinate string letters
     * @param Easting   Easting value
     * @param Northing  Northing value
     * @param Precision Precision level of MGRS string
     * @return error code
     */
    private long makeMGRSString(long Zone, long[] Letters, double Easting, double Northing, long Precision) {
        int j;
        double divisor;
        long east;
        long north;

        if (Zone != 0)
            MGRSString = String.format("%02d", Zone);
        else
            MGRSString = "  ";

        for (j = 0; j < 3; j++) {

            if (Letters[j] < 0 || Letters[j] > 26)
                return MGRSCoordConverter.MGRS_ZONE_ERROR;  // TODO: Find out why this happens
            MGRSString = MGRSString + MGRSCoordConverter.alphabet.charAt((int) Letters[j]);
        }

        divisor = Math.pow(10.0, (5 - Precision));
        Easting = Easting % 100000.0;
        if (Easting >= 99999.5)
            Easting = 99999.0;
        east = (long) (Easting / divisor);

        // Here we need to only use the number requesting in the precision
        int iEast = (int) east;
        String sEast = Integer.toString(iEast);
        if (sEast.length() > Precision)
            sEast = sEast.substring(0, (int) Precision - 1);
        else {
            int i;
            int length = sEast.length();
            for (i = 0; i < Precision - length; i++) {
                sEast = '0' + sEast;
            }
        }
        MGRSString = MGRSString + ' ' + sEast;

        Northing = Northing % 100000.0;
        if (Northing >= 99999.5)
            Northing = 99999.0;
        north = (long) (Northing / divisor);

        int iNorth = (int) north;
        String sNorth = Integer.toString(iNorth);
        if (sNorth.length() > Precision)
            sNorth = sNorth.substring(0, (int) Precision - 1);
        else {
            int i;
            int length = sNorth.length();
            for (i = 0; i < Precision - length; i++) {
                sNorth = '0' + sNorth;
            }
        }
        MGRSString = MGRSString + ' ' + sNorth;

        return MGRSCoordConverter.MGRS_NO_ERROR;
    }

    /**
     * Get the last error code.
     *
     * @return the last error code.
     */
    public long getError() {
        return last_error;
    }

    /**
     * The function Convert_MGRS_To_UPS converts an MGRS coordinate string to UPS (hemisphere, easting, and northing)
     * coordinates, according to the current ellipsoid parameters. If any errors occur, the error code(s) are returned
     * by the function, otherwide UPS_NO_ERROR is returned.
     *
     * @param MGRS the MGRS coordinate string.
     * @return a corresponding {@link UPSCoord} instance.
     */
    private UPSCoord convertMGRSToUPS(String MGRS) {
        long ltr2_high_value;       /* 2nd letter range - high number             */
        long ltr3_high_value;       /* 3rd letter range - high number (UPS)       */
        long ltr2_low_value;        /* 2nd letter range - low number              */
        double false_easting;       /* False easting for 2nd letter               */
        double false_northing;      /* False northing for 3rd letter              */
        double grid_easting;        /* easting for 100,000 meter grid square      */
        double grid_northing;       /* northing for 100,000 meter grid square     */
        int index = 0;
        long error_code = MGRSCoordConverter.MGRS_NO_ERROR;

        String hemisphere;
        double easting, northing;

        MGRSComponents mgrs = breakMGRSString(MGRS);
        if (mgrs == null)
            error_code = this.last_error;

        if (mgrs != null && mgrs.zone > 0)
            error_code |= MGRSCoordConverter.MGRS_STRING_ERROR;

        if (error_code == MGRSCoordConverter.MGRS_NO_ERROR) {
            easting = mgrs.easting;
            northing = mgrs.northing;

            if (mgrs.latitudeBand >= MGRSCoordConverter.LETTER_Y) {
                hemisphere = Keys.NORTH;

                index = mgrs.latitudeBand - 22;
                ltr2_low_value = MGRSCoordConverter.upsConstants[index][1]; //.ltr2_low_value;
                ltr2_high_value = MGRSCoordConverter.upsConstants[index][2]; //.ltr2_high_value;
                ltr3_high_value = MGRSCoordConverter.upsConstants[index][3]; //.ltr3_high_value;
                false_easting = MGRSCoordConverter.upsConstants[index][4]; //.false_easting;
                false_northing = MGRSCoordConverter.upsConstants[index][5]; //.false_northing;
            } else {
                hemisphere = Keys.SOUTH;

                ltr2_low_value = MGRSCoordConverter.upsConstants[mgrs.latitudeBand][12]; //.ltr2_low_value;
                ltr2_high_value = MGRSCoordConverter.upsConstants[mgrs.latitudeBand][2]; //.ltr2_high_value;
                ltr3_high_value = MGRSCoordConverter.upsConstants[mgrs.latitudeBand][3]; //.ltr3_high_value;
                false_easting = MGRSCoordConverter.upsConstants[mgrs.latitudeBand][4]; //.false_easting;
                false_northing = MGRSCoordConverter.upsConstants[mgrs.latitudeBand][5]; //.false_northing;
            }

            // Check that the second letter of the MGRS string is within
            // the range of valid second letter values
            // Also check that the third letter is valid
            if ((mgrs.squareLetter1 < ltr2_low_value) || (mgrs.squareLetter1 > ltr2_high_value) ||
                ((mgrs.squareLetter1 == MGRSCoordConverter.LETTER_D) || (mgrs.squareLetter1 == MGRSCoordConverter.LETTER_E) ||
                    (mgrs.squareLetter1 == MGRSCoordConverter.LETTER_M) || (mgrs.squareLetter1 == MGRSCoordConverter.LETTER_N) ||
                    (mgrs.squareLetter1 == MGRSCoordConverter.LETTER_V) || (mgrs.squareLetter1 == MGRSCoordConverter.LETTER_W)) ||
                (mgrs.squareLetter2 > ltr3_high_value))
                error_code = MGRSCoordConverter.MGRS_STRING_ERROR;

            if (error_code == MGRSCoordConverter.MGRS_NO_ERROR) {
                grid_northing = mgrs.squareLetter2 * MGRSCoordConverter.ONEHT + false_northing;
                if (mgrs.squareLetter2 > MGRSCoordConverter.LETTER_I)
                    grid_northing = grid_northing - MGRSCoordConverter.ONEHT;

                if (mgrs.squareLetter2 > MGRSCoordConverter.LETTER_O)
                    grid_northing = grid_northing - MGRSCoordConverter.ONEHT;

                grid_easting = ((mgrs.squareLetter1) - ltr2_low_value) * MGRSCoordConverter.ONEHT + false_easting;
                if (ltr2_low_value != MGRSCoordConverter.LETTER_A) {
                    if (mgrs.squareLetter1 > MGRSCoordConverter.LETTER_L)
                        grid_easting = grid_easting - 300000.0;

                    if (mgrs.squareLetter1 > MGRSCoordConverter.LETTER_U)
                        grid_easting = grid_easting - 200000.0;
                } else {
                    if (mgrs.squareLetter1 > MGRSCoordConverter.LETTER_C)
                        grid_easting = grid_easting - 200000.0;

                    if (mgrs.squareLetter1 > MGRSCoordConverter.LETTER_I)
                        grid_easting = grid_easting - MGRSCoordConverter.ONEHT;

                    if (mgrs.squareLetter1 > MGRSCoordConverter.LETTER_L)
                        grid_easting = grid_easting - 300000.0;
                }

                easting = grid_easting + easting;
                northing = grid_northing + northing;
                return UPSCoord.fromUPS(hemisphere, easting, northing, globe);
            }
        }

        return null;
    }

    private static class MGRSComponents {
        private final int zone;
        private final int latitudeBand;
        private final int squareLetter1;
        private final int squareLetter2;
        private final double easting;
        private final double northing;
        private final int precision;

        public MGRSComponents(int zone, int latitudeBand, int squareLetter1, int squareLetter2,
            double easting, double northing, int precision) {
            this.zone = zone;
            this.latitudeBand = latitudeBand;
            this.squareLetter1 = squareLetter1;
            this.squareLetter2 = squareLetter2;
            this.easting = easting;
            this.northing = northing;
            this.precision = precision;
        }

        public String toString() {
            return "MGRS: " + zone + ' ' +
                MGRSCoordConverter.alphabet.charAt(latitudeBand) + ' ' +
                MGRSCoordConverter.alphabet.charAt(squareLetter1) + MGRSCoordConverter.alphabet.charAt(squareLetter2) + ' ' +
                easting + ' ' +
                northing + ' ' +
                '(' + precision + ')';
        }
    }
}