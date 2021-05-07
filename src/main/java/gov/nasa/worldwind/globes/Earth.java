/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.globes;

import gov.nasa.worldwind.Keys;
import gov.nasa.worldwind.avlist.*;
import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.layers.ogc.wms.WMSCapabilities;
import gov.nasa.worldwind.terrain.WMSBasicElevationModel;

import javax.xml.stream.XMLStreamException;
import java.net.*;

/**
 * Defines a model of the Earth, using the <a href="http://en.wikipedia.org/wiki/World_Geodetic_System"
 * target="_blank">World Geodetic System</a> (WGS84).
 *
 * @author Tom Gaskins
 * @version $Id: Earth.java 1958 2014-04-24 19:25:37Z tgaskins $
 */

public class Earth extends EllipsoidalGlobe {
    public static final double WGS84_EQUATORIAL_RADIUS = 6378137.0; // ellipsoid equatorial getRadius, in meters
    public static final double WGS84_POLAR_RADIUS = 6356752.3; // ellipsoid polar getRadius, in meters
    public static final double WGS84_ES = 0.00669437999013; // eccentricity squared, semi-major axis

    public static final double ELEVATION_MIN = -11000.0d; // Depth of Marianas trench
    public static final double ELEVATION_MAX = 8500.0d; // Height of Mt. Everest.

    public Earth() {
        super(Earth.WGS84_EQUATORIAL_RADIUS, Earth.WGS84_POLAR_RADIUS, Earth.WGS84_ES,

//            EllipsoidalGlobe.makeElevationModel(Keys.EARTH_ELEVATION_MODEL_CONFIG_FILE,
//                //"config/Earth/EarthElevations2.xml"
//                //"config/Earth/EarthElevations256.xml"
//                "config/Earth/EarthMergedElevationModel.xml"
//            )
            elevation("http://worldwind26.arc.nasa.gov/elev")

        );
    }

    /**
     *         var EarthElevationModel = function () {
     *             ElevationModel.call(this);
     *
     *             this.addCoverage(new GebcoElevationCoverage());
     *             this.addCoverage(new AsterV2ElevationCoverage());
     *             this.addCoverage(new UsgsNedElevationCoverage());
     *             this.addCoverage(new UsgsNedHiElevationCoverage());
     */
    private static WMSBasicElevationModel elevation(String serverUrl)  {
        URI serverURI;
        try {
            serverURI = new URI(serverUrl);
        }
        catch (URISyntaxException e) {
            e.printStackTrace();
            return null;
        }
        WMSCapabilities caps = WMSCapabilities.retrieve(serverURI);
        try {
            caps.parse();
        } catch (XMLStreamException e) {
            e.printStackTrace();
            return null;
        }
//        List<WMSLayerCapabilities> namedLayerCaps = caps.getNamedLayers();
//        if (namedLayerCaps == null) {
//            System.out.println("bad dynamic layer:" + serverUrl);
//            return null;
//        }

        {
            KV p = new KVMap();
            p.set(Keys.LAYER_NAMES,
                //"GEBCO"
                "GEBCO,aster_v2,USGS-NED"
            );
            final int tileResolution =
                256;
                //128;
            p.set(Keys.TILE_WIDTH, tileResolution);
            p.set(Keys.TILE_HEIGHT, tileResolution);
//            p.set(Keys.NUM_LEVELS, 10);
            Angle delta = new Angle(
                //0.01
                20
            );
//            p.set(Keys.ELEVATION_MIN, -11000.0);
//            p.set(Keys.ELEVATION_MAX, 8850.0);
            p.set(Keys.LEVEL_ZERO_TILE_DELTA, new LatLon(delta, delta));
            WMSBasicElevationModel wmsElevations = new WMSBasicElevationModel(caps,
                p);

            wmsElevations.setDetailHint(0.3);
            //wmsElevations.setExpiryTime(System.currentTimeMillis() - fiveDayMillis);
            return wmsElevations;
        }
    }

    public String toString() {
        return "Earth";
    }

}