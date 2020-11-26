/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.formats.tiff;

/**
 * @author Lado Garakanidze
 * @version $Id: GeoTiff.java 1171 2013-02-11 21:45:02Z dcollins $
 */
public abstract class GeoTiff {
    public static final int Undefined = 0;
    public static final int UserDefined = 32767;

    public enum ProjectedCS {
        Undefined(0, 0, "Undefined");

        private final String name;
        private final int epsg;
        private final int datum;

        ProjectedCS(int epsg, int datum, String name) {
            this.name = name;
            this.epsg = epsg;
            this.datum = datum;
        }

        public int getEPSG() {
            return this.epsg;
        }

        public int getDatum() {
            return this.datum;
        }

        public String getName() {
            return this.name;
        }
    }

    // Geotiff extension tags
    public interface Tag {
        int MODEL_PIXELSCALE = 33550;
        int MODEL_TIEPOINT = 33922;
        int MODEL_TRANSFORMATION = 34264;
        int GEO_KEY_DIRECTORY = 34735;
        int GEO_DOUBLE_PARAMS = 34736;
        int GEO_ASCII_PARAMS = 34737;

        int GDAL_NODATA = 42113;
    }

    public interface GeoKeyHeader {
        int KeyDirectoryVersion = 1;
        int KeyRevision = 1;
        int MinorRevision = 0;
    }

    public interface GeoKey {
        int ModelType = 1024; // see GeoTiff.ModelType values
        int RasterType = 1025; // see GeoTiff.RasterType values

        int GeographicType = 2048; // see GeoTiff.GCS for values or Section 6.3.2.1 Codes
        // GeoKey Requirements for User-Defined geographic CS:
        int GeogCitation = 2049; // ASCII
        int GeogGeodeticDatum = 2050; // SHORT, See section 6.3.2.2 Geodetic Datum Codes
        int GeogPrimeMeridian = 2051; // SHORT, Section 6.3.2.4 Codes
        int GeogLinearUnits = 2052; // Double, See GeoTiff.Unit.Liner or Section 6.3.1.3 Codes
        int GeogLinearUnitSize = 2053; // Double, meters
        int GeogAngularUnits = 2054; // Short, See GeoTiff.Units.Angular or Section 6.3.1.4 Codes
        int GeogAngularUnitSize = 2055; // Double, radians
        int GeogEllipsoid = 2056; // Short, See Section 6.3.2.3 Codes
        int GeogAzimuthUnits = 2060; // Short, Section 6.3.1.4 Codes
        int GeogPrimeMeridianLong = 2061; // DOUBLE, See GeoTiff.Units.Angular

        // 6.2.3 Projected CS Parameter Keys
        int ProjectedCSType = 3072; /*  Section 6.3.3.1 codes */
        int PCSCitation = 3073; /*  documentation */
        int Projection = 3074; /*  Section 6.3.3.2 codes */
        int ProjCoordTrans = 3075; /*  Section 6.3.3.3 codes */
        int ProjLinearUnits = 3076; /*  Section 6.3.1.3 codes */
        int ProjLinearUnitSize = 3077; /*  meters */
        int ProjStdParallel1 = 3078; /*  GeogAngularUnit */
        int ProjStdParallel2 = 3079; /*  GeogAngularUnit */
        int ProjNatOriginLong = 3080; /*  GeogAngularUnit */
        int ProjNatOriginLat = 3081; /*  GeogAngularUnit */
        int ProjFalseEasting = 3082; /*  ProjLinearUnits */
        int ProjFalseNorthing = 3083; /*  ProjLinearUnits */
        int ProjFalseOriginLong = 3084; /*  GeogAngularUnit */
        int ProjFalseOriginLat = 3085; /*  GeogAngularUnit */
        int ProjFalseOriginEasting = 3086; /*  ProjLinearUnits */
        int ProjFalseOriginNorthing = 3087; /*  ProjLinearUnits */
        int ProjCenterLong = 3088; /*  GeogAngularUnit */
        int ProjCenterLat = 3089; /*  GeogAngularUnit */
        int ProjCenterEasting = 3090; /*  ProjLinearUnits */
        int ProjCenterNorthing = 3091; /*  ProjLinearUnits */
        int ProjScaleAtNatOrigin = 3092; /*  ratio */
        int ProjScaleAtCenter = 3093; /*  ratio */
        int ProjAzimuthAngle = 3094; /*  GeogAzimuthUnit */
        int ProjStraightVertPoleLong = 3095; /*  GeogAngularUnit */
        // Aliases:
        int ProjStdParallel = ProjStdParallel1;
        int ProjOriginLong = ProjNatOriginLong;
        int ProjOriginLat = ProjNatOriginLat;
        int ProjScaleAtOrigin = ProjScaleAtNatOrigin;

        // 6.2.4 Vertical CS Keys
        int VerticalCSType = 4096; /* Section 6.3.4.1 codes */
        int VerticalCitation = 4097; /* ASCII */
        int VerticalDatum = 4098; /* Section 6.3.4.2 codes */
        int VerticalUnits = 4099; /* Section 6.3.1.3 codes */
    }

    public interface ModelType {
        int Undefined = 0;
        int Projected = 1;
        int Geographic = 2;
        int Geocentric = 3;
        int UserDefined = 32767;

        int DEFAULT = Geographic;
    }

    public interface RasterType {
        int Undefined = 0; // highly not recomended to use
        int RasterPixelIsArea = 1;
        int RasterPixelIsPoint = 2;
        int UserDefined = 32767; // highly not recomended to use
    }

    public interface Unit {
        int Undefined = 0;
        int UserDefined = 32767;

        //6.3.1.3 Linear Units Codes
        interface Linear {
            int Meter = 9001;
            int Foot = 9002;
            int Foot_US_Survey = 9003;
            int Foot_Modified_American = 9004;
            int Foot_Clarke = 9005;
            int Foot_Indian = 9006;
            int Link = 9007;
            int Link_Benoit = 9008;
            int Link_Sears = 9009;
            int Chain_Benoit = 9010;
            int Chain_Sears = 9011;
            int Yard_Sears = 9012;
            int Yard_Indian = 9013;
            int Fathom = 9014;
            int Mile_International_Nautical = 9015;
        }

        // 6.3.1.4 Angular Units Codes
        // These codes shall be used for any key that requires specification of an angular unit of measurement.
        interface Angular {
            int Angular_Radian = 9101;
            int Angular_Degree = 9102;
            int Angular_Arc_Minute = 9103;
            int Angular_Arc_Second = 9104;
            int Angular_Grad = 9105;
            int Angular_Gon = 9106;
            int Angular_DMS = 9107;
            int Angular_DMS_Hemisphere = 9108;
        }
    }

    // Geogrphic Coordinate System (GCS)
    public interface GCS {
        int Undefined = 0;
        int UserDefined = 32767;

        int NAD_83 = 4269;
        int WGS_72 = 4322;
        int WGS_72BE = 4324;
        int WGS_84 = 4326;

        int DEFAULT = WGS_84;
    }

    // Geogrphic Coordinate System Ellipsoid (GCSE)
    public interface GCSE {
        int WGS_84 = 4030;
    }

    // Projected Coordinate System (PCS)
    public interface PCS {
        int Undefined = 0;
        int UserDefined = 32767;
    }

    // Vertical Coordinate System (VCS)
    public interface VCS {
        // [ 1, 4999] = Reserved
        // [ 5000, 5099] = EPSG Ellipsoid Vertical CS Codes
        // [ 5100, 5199] = EPSG Orthometric Vertical CS Codes
        // [ 5200, 5999] = Reserved EPSG
        // [ 6000, 32766] = Reserved
        // [32768, 65535] = Private User Implementations

        int Undefined = 0;
        int UserDefined = 32767;

        int Airy_1830_ellipsoid = 5001;
        int Airy_Modified_1849_ellipsoid = 5002;
        int ANS_ellipsoid = 5003;
        int Bessel_1841_ellipsoid = 5004;
        int Bessel_Modified_ellipsoid = 5005;
        int Bessel_Namibia_ellipsoid = 5006;
        int Clarke_1858_ellipsoid = 5007;
        int Clarke_1866_ellipsoid = 5008;
        int Clarke_1880_Benoit_ellipsoid = 5010;
        int Clarke_1880_IGN_ellipsoid = 5011;
        int Clarke_1880_RGS_ellipsoid = 5012;
        int Clarke_1880_Arc_ellipsoid = 5013;
        int Clarke_1880_SGA_1922_ellipsoid = 5014;
        int Everest_1830_1937_Adjustment_ellipsoid = 5015;
        int Everest_1830_1967_Definition_ellipsoid = 5016;
        int Everest_1830_1975_Definition_ellipsoid = 5017;
        int Everest_1830_Modified_ellipsoid = 5018;
        int GRS_1980_ellipsoid = 5019;
        int Helmert_1906_ellipsoid = 5020;
        int INS_ellipsoid = 5021;
        int International_1924_ellipsoid = 5022;
        int International_1967_ellipsoid = 5023;
        int Krassowsky_1940_ellipsoid = 5024;
        int NWL_9D_ellipsoid = 5025;
        int NWL_10D_ellipsoid = 5026;
        int Plessis_1817_ellipsoid = 5027;
        int Struve_1860_ellipsoid = 5028;
        int War_Office_ellipsoid = 5029;
        int WGS_84_ellipsoid = 5030;
        int GEM_10C_ellipsoid = 5031;
        int OSU86F_ellipsoid = 5032;
        int OSU91A_ellipsoid = 5033;
        // Orthometric Vertical CS;
        int Newlyn = 5101;
        int North_American_Vertical_Datum_1929 = 5102;
        int North_American_Vertical_Datum_1988 = 5103;
        int Yellow_Sea_1956 = 5104;
        int Baltic_Sea = 5105;
        int Caspian_Sea = 5106;

        int DEFAULT = Undefined;
    }
}

