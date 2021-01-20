/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.geom.coords;

import gov.nasa.worldwind.Keys;
import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.globes.Globe;

/**
 * Converter used to translate UTM coordinates to and from geodetic latitude and longitude.
 *
 * @author Patrick Murris
 * @version $Id: UTMCoordConverter.java 1171 2013-02-11 21:45:02Z dcollins $
 * @see UTMCoord, TMCoordConverter
 */

/**
 * Ported to Java from the NGA GeoTrans utm.c and utm.h
 *
 * @author Garrett Headley, Patrick Murris
 */
class UTMCoordConverter {
    public final static double CLARKE_A = 6378206.4;
    public final static double CLARKE_B = 6356583.8;
    public final static double CLARKE_F = 1 / 294.9786982;

    public final static double WGS84_A = 6378137;
    public final static double WGS84_F = 1 / 298.257223563;

    public final static int UTM_NO_ERROR = 0x0000;
    public final static int UTM_LAT_ERROR = 0x0001;
    public final static int UTM_LON_ERROR = 0x0002;
    public final static int UTM_EASTING_ERROR = 0x0004;
    public final static int UTM_NORTHING_ERROR = 0x0008;
    public final static int UTM_ZONE_ERROR = 0x0010;
    public final static int UTM_HEMISPHERE_ERROR = 0x0020;
    public final static int UTM_ZONE_OVERRIDE_ERROR = 0x0040;
    public final static int UTM_A_ERROR = 0x0080;
    public final static int UTM_INV_F_ERROR = 0x0100;
    public final static int UTM_TM_ERROR = 0x0200;

    private final static double PI = 3.14159265358979323;
    private final static double MIN_LAT = ((-82 * UTMCoordConverter.PI) / 180.0); /* -82 degrees in radians    */
    private final static double MAX_LAT = ((86 * UTMCoordConverter.PI) / 180.0);  /* 86 degrees in radians     */

    private final static int MIN_EASTING = 100000;
    private final static int MAX_EASTING = 900000;
    private final static int MIN_NORTHING = 0;
    private final static int MAX_NORTHING = 10000000;

    private final Globe globe;
    private double UTM_a = 6378137.0;         /* Semi-major axis of ellipsoid in meters  */
    private double UTM_f = 1 / 298.257223563; /* Flattening of ellipsoid                 */
    private long UTM_Override;          /* Zone override flag                      */

    private double Easting;
    private double Northing;
    private String Hemisphere;
    private int Zone;
    private double Latitude;
    private double Longitude;
    private double Central_Meridian;

    UTMCoordConverter(Globe globe) {
        this.globe = globe;
        if (globe != null) {
            double a = globe.getEquatorialRadius();
            double f = (globe.getEquatorialRadius() - globe.getPolarRadius()) / globe.getEquatorialRadius();
            setUTMParameters(a, f, 0);
        }
    }

    UTMCoordConverter(double a, double f) {
        this.globe = null;
        setUTMParameters(a, f, 0);
    }

    public static LatLon convertWGS84ToNAD27(Angle latWGS, Angle lonWGS) {
        double deltaX = -12.0;
        double deltaY = 130.0;
        double deltaZ = 190.0;
        double difA = UTMCoordConverter.WGS84_A - UTMCoordConverter.CLARKE_A;
        double difF = UTMCoordConverter.WGS84_F - UTMCoordConverter.CLARKE_F;

        double lat = latWGS.radians();
        double lon = lonWGS.radians();

        double f = 1 - UTMCoordConverter.CLARKE_B / UTMCoordConverter.CLARKE_A;
        double e2 = 2 * f - Math.pow(f, 2);
        final double sinLat = Math.sin(lat);
        final double oneMinE2TimesSqrSinLat = 1 - e2 * Math.pow(sinLat, 2.0);
        double Rn = UTMCoordConverter.CLARKE_A * Math.pow(oneMinE2TimesSqrSinLat, -0.5);
        double Rm = (UTMCoordConverter.CLARKE_A * (1 - e2)) / Math.pow(oneMinE2TimesSqrSinLat, 1.5);
        final double cosLon = Math.cos(lon);
        double errLon = (-1 * deltaX * Math.sin(lon) + deltaY * cosLon) / (Rn * Math.cos(lat));
        double errLat = (-1 * deltaX * sinLat * cosLon - deltaY * sinLat * Math.sin(lon)
            + deltaZ * Math.cos(lat)
            + difA * (Rn * e2 * sinLat * Math.cos(lat)) / UTMCoordConverter.CLARKE_A
            + difF * (Rm * UTMCoordConverter.CLARKE_A / UTMCoordConverter.CLARKE_B + Rn * UTMCoordConverter.CLARKE_B / UTMCoordConverter.CLARKE_A) * sinLat * Math.cos(lat)) / Rm;

        return LatLon.fromRadians(lat - errLat, lon - errLon);
    }

    /**
     * The function Set_UTM_Parameters receives the ellipsoid parameters and UTM zone override parameter as inputs, and
     * sets the corresponding state variables.  If any errors occur, the error code(s) are returned by the function,
     * otherwise UTM_NO_ERROR is returned.
     *
     * @param a        Semi-major axis of ellipsoid, in meters
     * @param f        Flattening of ellipsoid
     * @param override UTM override zone, zero indicates no override
     * @return error code
     */
    private long setUTMParameters(double a, double f, long override) {
        double inv_f = 1 / f;
        long Error_Code = UTMCoordConverter.UTM_NO_ERROR;

        if (a <= 0.0) { /* Semi-major axis must be greater than zero */
            Error_Code |= UTMCoordConverter.UTM_A_ERROR;
        }
        if ((inv_f < 250) || (inv_f > 350)) { /* Inverse flattening must be between 250 and 350 */
            Error_Code |= UTMCoordConverter.UTM_INV_F_ERROR;
        }
        if ((override < 0) || (override > 60)) {
            Error_Code |= UTMCoordConverter.UTM_ZONE_OVERRIDE_ERROR;
        }
        if (Error_Code == UTMCoordConverter.UTM_NO_ERROR) { /* no errors */
            UTM_a = a;
            UTM_f = f;
            UTM_Override = override;
        }
        return (Error_Code);
    }

    /**
     * The function Convert_Geodetic_To_UTM converts geodetic (latitude and longitude) coordinates to UTM projection
     * (zone, hemisphere, easting and northing) coordinates according to the current ellipsoid and UTM zone override
     * parameters.  If any errors occur, the error code(s) are returned by the function, otherwise UTM_NO_ERROR is
     * returned.
     *
     * @param Latitude  Latitude in radians
     * @param Longitude Longitude in radians
     * @return error code
     */
    public long convertGeodeticToUTM(double Latitude, double Longitude) {
        long Lat_Degrees;
        long Long_Degrees;
        long temp_zone;
        long Error_Code = UTMCoordConverter.UTM_NO_ERROR;
        double Origin_Latitude = 0;
        double False_Easting = 500000;
        double False_Northing = 0;
        double Scale = 0.9996;

        if ((Latitude < UTMCoordConverter.MIN_LAT) || (Latitude > UTMCoordConverter.MAX_LAT)) { /* Latitude out of range */
            Error_Code |= UTMCoordConverter.UTM_LAT_ERROR;
        }
        if ((Longitude < -UTMCoordConverter.PI) || (Longitude > (2 * UTMCoordConverter.PI))) { /* Longitude out of range */
            Error_Code |= UTMCoordConverter.UTM_LON_ERROR;
        }
        if (Error_Code == UTMCoordConverter.UTM_NO_ERROR) { /* no errors */
            if (Longitude < 0)
                Longitude += (2 * UTMCoordConverter.PI) + 1.0e-10;
            Lat_Degrees = (long) (Latitude * 180.0 / UTMCoordConverter.PI);
            Long_Degrees = (long) (Longitude * 180.0 / UTMCoordConverter.PI);

            if (Longitude < UTMCoordConverter.PI)
                temp_zone = (long) (31 + ((Longitude * 180.0 / UTMCoordConverter.PI) / 6.0));
            else
                temp_zone = (long) (((Longitude * 180.0 / UTMCoordConverter.PI) / 6.0) - 29);
            if (temp_zone > 60)
                temp_zone = 1;
            /* UTM special cases */
            if ((Lat_Degrees > 55) && (Lat_Degrees < 64) && (Long_Degrees > -1) && (Long_Degrees < 3))
                temp_zone = 31;
            if ((Lat_Degrees > 55) && (Lat_Degrees < 64) && (Long_Degrees > 2) && (Long_Degrees < 12))
                temp_zone = 32;
            if ((Lat_Degrees > 71) && (Long_Degrees > -1) && (Long_Degrees < 9))
                temp_zone = 31;
            if ((Lat_Degrees > 71) && (Long_Degrees > 8) && (Long_Degrees < 21))
                temp_zone = 33;
            if ((Lat_Degrees > 71) && (Long_Degrees > 20) && (Long_Degrees < 33))
                temp_zone = 35;
            if ((Lat_Degrees > 71) && (Long_Degrees > 32) && (Long_Degrees < 42))
                temp_zone = 37;

            if (UTM_Override != 0) {
                if ((temp_zone == 1) && (UTM_Override == 60))
                    temp_zone = UTM_Override;
                else if ((temp_zone == 60) && (UTM_Override == 1))
                    temp_zone = UTM_Override;
                else if (((temp_zone - 1) <= UTM_Override) && (UTM_Override <= (temp_zone + 1)))
                    temp_zone = UTM_Override;
                else
                    Error_Code = UTMCoordConverter.UTM_ZONE_OVERRIDE_ERROR;
            }
            if (Error_Code == UTMCoordConverter.UTM_NO_ERROR) {
                if (temp_zone >= 31)
                    Central_Meridian = (6 * temp_zone - 183) * UTMCoordConverter.PI / 180.0;
                else
                    Central_Meridian = (6 * temp_zone + 177) * UTMCoordConverter.PI / 180.0;
                Zone = (int) temp_zone;
                if (Latitude < 0) {
                    False_Northing = 10000000;
                    Hemisphere = Keys.SOUTH;
                } else
                    Hemisphere = Keys.NORTH;

                try {
                    TMCoord TM = TMCoord.fromLatLon(Angle.fromRadians(Latitude), Angle.fromRadians(Longitude),
                        this.globe, this.UTM_a, this.UTM_f, Angle.fromRadians(Origin_Latitude),
                        Angle.fromRadians(Central_Meridian), False_Easting, False_Northing, Scale);
                    Easting = TM.getEasting();
                    Northing = TM.getNorthing();

                    if ((Easting < UTMCoordConverter.MIN_EASTING) || (Easting > UTMCoordConverter.MAX_EASTING))
                        Error_Code = UTMCoordConverter.UTM_EASTING_ERROR;
                    if ((Northing < UTMCoordConverter.MIN_NORTHING) || (Northing > UTMCoordConverter.MAX_NORTHING))
                        Error_Code |= UTMCoordConverter.UTM_NORTHING_ERROR;
                }
                catch (RuntimeException e) {
                    Error_Code = UTMCoordConverter.UTM_TM_ERROR;
                }
            }
        }
        return (Error_Code);
    }

    /**
     * @return Easting (X) in meters
     */
    public double getEasting() {
        return Easting;
    }

    /**
     * @return Northing (Y) in meters
     */
    public double getNorthing() {
        return Northing;
    }

    /**
     * @return The coordinate hemisphere, either {@link Keys#NORTH} or {@link Keys#SOUTH}.
     */
    public String getHemisphere() {
        return Hemisphere;
    }

    /**
     * @return UTM zone
     */
    public int getZone() {
        return Zone;
    }

    /**
     * The function Convert_UTM_To_Geodetic converts UTM projection (zone, hemisphere, easting and northing) coordinates
     * to geodetic(latitude and  longitude) coordinates, according to the current ellipsoid parameters.  If any errors
     * occur, the error code(s) are returned by the function, otherwise UTM_NO_ERROR is returned.
     *
     * @param Zone       UTM zone.
     * @param Hemisphere The coordinate hemisphere, either {@link Keys#NORTH} or {@link Keys#SOUTH}.
     * @param Easting    Easting (X) in meters.
     * @param Northing   Northing (Y) in meters.
     * @return error code.
     */
    public long convertUTMToGeodetic(long Zone, String Hemisphere, double Easting, double Northing) {
        // TODO: arg checking
        long Error_Code = UTMCoordConverter.UTM_NO_ERROR;
        double Origin_Latitude = 0;
        double False_Easting = 500000;
        double False_Northing = 0;
        double Scale = 0.9996;

        if ((Zone < 1) || (Zone > 60))
            Error_Code |= UTMCoordConverter.UTM_ZONE_ERROR;
        if (!Hemisphere.equals(Keys.SOUTH) && !Hemisphere.equals(Keys.NORTH))
            Error_Code |= UTMCoordConverter.UTM_HEMISPHERE_ERROR;
        if ((Northing < UTMCoordConverter.MIN_NORTHING) || (Northing > UTMCoordConverter.MAX_NORTHING))
            Error_Code |= UTMCoordConverter.UTM_NORTHING_ERROR;

        if (Error_Code == UTMCoordConverter.UTM_NO_ERROR) { /* no errors */
            if (Zone >= 31)
                Central_Meridian = ((6 * Zone - 183) * UTMCoordConverter.PI / 180.0 /*+ 0.00000005*/);
            else
                Central_Meridian = ((6 * Zone + 177) * UTMCoordConverter.PI / 180.0 /*+ 0.00000005*/);
            if (Hemisphere.equals(Keys.SOUTH))
                False_Northing = 10000000;
            try {
                TMCoord TM = TMCoord.fromTM(Easting, Northing,
                    this.globe, Angle.fromRadians(Origin_Latitude), Angle.fromRadians(Central_Meridian),
                    False_Easting, False_Northing, Scale);
                Latitude = TM.getLatitude().radians();
                Longitude = TM.getLongitude().radians();

                if ((Latitude < UTMCoordConverter.MIN_LAT) || (Latitude > UTMCoordConverter.MAX_LAT)) { /* Latitude out of range */
                    Error_Code |= UTMCoordConverter.UTM_NORTHING_ERROR;
                }
            }
            catch (RuntimeException e) {
                Error_Code = UTMCoordConverter.UTM_TM_ERROR;
            }
        }
        return (Error_Code);
    }

    /**
     * @return Latitude in radians.
     */
    public double getLatitude() {
        return Latitude;
    }

    /**
     * @return Longitude in radians.
     */
    public double getLongitude() {
        return Longitude;
    }

    /**
     * @return Central_Meridian in radians.
     */
    public double getCentralMeridian() {
        return Central_Meridian;
    }
}