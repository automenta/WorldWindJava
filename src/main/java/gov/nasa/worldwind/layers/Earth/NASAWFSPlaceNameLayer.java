/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */

package gov.nasa.worldwind.layers.Earth;

import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.layers.placename.*;
import gov.nasa.worldwind.util.Logging;

import java.awt.Color;
import java.awt.Font;
import java.util.*;

/**
 * @version $Id: NASAWFSPlaceNameLayer.java 1171 2013-02-11 21:45:02Z dcollins $
 */
public class NASAWFSPlaceNameLayer extends PlaceNameLayer {

    //String constants for name sets
    public static final String OCEANS = "topp:wpl_oceans";
    public static final String CONTINENTS = "topp:wpl_continents";
    public static final String WATERBODIES = "topp:wpl_waterbodies";
    public static final String TRENCHESRIDGES = "topp:wpl_trenchesridges";
    public static final String DESERTSPLAINS = "topp:wpl_desertsplains";
    public static final String LAKESRIVERS = "topp:wpl_lakesrivers";
    public static final String MOUNTAINSVALLEYS = "topp:wpl_mountainsvalleys";
    public static final String COUNTRIES = "topp:wpl_countries";
    public static final String GEONET_P_PPC = "topp:wpl_geonet_p_pplc";
    public static final String CITIESOVER500K = "topp:citiesover500k";
    public static final String CITIESOVER100K = "topp:citiesover100k";
    public static final String CITIESOVER50K = "topp:citiesover50k";
    public static final String CITIESOVER10K = "topp:citiesover10k";
    public static final String CITIESOVER1K = "topp:citiesover1k";
    public static final String USCITIESOVER0 = "topp:wpl_uscitiesover0";
    public static final String USCITIES0 = "topp:wpl_uscities0";
    public static final String US_ANTHROPOGENIC = "topp:wpl_us_anthropogenic";
    public static final String US_WATER = "topp:wpl_us_water";
    public static final String US_TERRAIN = "topp:wpl_us_terrain";
    public static final String GEONET_A_ADM1 = "topp:wpl_geonet_a_adm1";
    public static final String GEONET_A_ADM2 = "topp:wpl_geonet_a_adm2";
    public static final String GEONET_P_PPLA = "topp:wpl_geonet_p_ppla";
    public static final String GEONET_P_PPL = "topp:wpl_geonet_p_ppl";
    public static final String GEONET_P_PPLC = "topp:wpl_geonet_p_pplC";

    private static final String[] allNameSets = {OCEANS, CONTINENTS, WATERBODIES, TRENCHESRIDGES, DESERTSPLAINS,
        LAKESRIVERS,
        MOUNTAINSVALLEYS, COUNTRIES, GEONET_P_PPC, CITIESOVER500K, CITIESOVER100K,
        CITIESOVER50K, CITIESOVER10K, CITIESOVER1K, USCITIESOVER0, USCITIES0,
        US_ANTHROPOGENIC, US_WATER, US_TERRAIN, GEONET_A_ADM1, GEONET_A_ADM2,
        GEONET_P_PPLA, GEONET_P_PPL};

    private static List activeNamesList = Arrays.asList(allNameSets);

    public NASAWFSPlaceNameLayer() {
        super(makePlaceNameServiceSet());
    }

    private static PlaceNameServiceSet makePlaceNameServiceSet() {
        final String service = "https://worldwind22.arc.nasa.gov/geoserver/wfs";
        final String fileCachePath = "Earth/PlaceNames/WFSPlaceNamesVersion1.0";
        PlaceNameServiceSet placeNameServiceSet = new PlaceNameServiceSet();
        placeNameServiceSet.setExpiryTime(new GregorianCalendar(2008, 1, 11).getTimeInMillis());
        PlaceNameService placeNameService;
        final boolean addVersionTag = true;  //true if pointing to a new wfs server
        // Oceans
        if (activeNamesList.contains(OCEANS)) {
            placeNameService = new PlaceNameService(service, "topp:wpl_oceans", fileCachePath, Sector.FULL_SPHERE,
                GRID_1x1,
                Font.decode("Arial-BOLDITALIC-12"), addVersionTag);
            placeNameService.setColor(new Color(200, 200, 200));
            placeNameService.setMinDisplayDistance(0.0d);
            placeNameService.setMaxDisplayDistance(LEVEL_A);
            placeNameServiceSet.addService(placeNameService, false);
        }
        // Continents
        if (activeNamesList.contains(CONTINENTS)) {
            placeNameService = new PlaceNameService(service, "topp:wpl_continents", fileCachePath, Sector.FULL_SPHERE,
                GRID_1x1, Font.decode("Arial-BOLD-12"), addVersionTag);
            placeNameService.setColor(new Color(255, 255, 240));
            placeNameService.setMinDisplayDistance(LEVEL_G);
            placeNameService.setMaxDisplayDistance(LEVEL_A);
            placeNameServiceSet.addService(placeNameService, false);
        }

        // Water Bodies
        if (activeNamesList.contains(WATERBODIES)) {
            placeNameService = new PlaceNameService(service, "topp:wpl_waterbodies", fileCachePath, Sector.FULL_SPHERE,
                GRID_4x8, Font.decode("Arial-ITALIC-10"), addVersionTag);
            placeNameService.setColor(Color.cyan);
            placeNameService.setMinDisplayDistance(0.0d);
            placeNameService.setMaxDisplayDistance(LEVEL_B);
            placeNameServiceSet.addService(placeNameService, false);
        }
        // Trenches & Ridges
        if (activeNamesList.contains(TRENCHESRIDGES)) {
            placeNameService = new PlaceNameService(service, "topp:wpl_trenchesridges", fileCachePath,
                Sector.FULL_SPHERE,
                GRID_4x8, Font.decode("Arial-BOLDITALIC-10"), addVersionTag);
            placeNameService.setColor(Color.cyan);
            placeNameService.setMinDisplayDistance(0.0d);
            placeNameService.setMaxDisplayDistance(LEVEL_B);
            placeNameServiceSet.addService(placeNameService, false);
        }
        // Deserts & Plains
        if (activeNamesList.contains(DESERTSPLAINS)) {
            placeNameService = new PlaceNameService(service, "topp:wpl_desertsplains", fileCachePath,
                Sector.FULL_SPHERE,
                GRID_4x8, Font.decode("Arial-BOLDITALIC-10"), addVersionTag);
            placeNameService.setColor(Color.orange);
            placeNameService.setMinDisplayDistance(0.0d);
            placeNameService.setMaxDisplayDistance(LEVEL_B);
            placeNameServiceSet.addService(placeNameService, false);
        }
        // Lakes & Rivers
        if (activeNamesList.contains(LAKESRIVERS)) {
            placeNameService = new PlaceNameService(service, "topp:wpl_lakesrivers", fileCachePath, Sector.FULL_SPHERE,
                GRID_8x16, Font.decode("Arial-ITALIC-10"), addVersionTag);
            placeNameService.setColor(Color.cyan);
            placeNameService.setMinDisplayDistance(0.0d);
            placeNameService.setMaxDisplayDistance(LEVEL_C);
            placeNameServiceSet.addService(placeNameService, false);
        }
        // Mountains & Valleys
        if (activeNamesList.contains(MOUNTAINSVALLEYS)) {
            placeNameService = new PlaceNameService(service, "topp:wpl_mountainsvalleys", fileCachePath,
                Sector.FULL_SPHERE,
                GRID_8x16, Font.decode("Arial-BOLDITALIC-10"), addVersionTag);
            placeNameService.setColor(Color.orange);
            placeNameService.setMinDisplayDistance(0.0d);
            placeNameService.setMaxDisplayDistance(LEVEL_C);
            placeNameServiceSet.addService(placeNameService, false);
        }
        // Countries
        if (activeNamesList.contains(COUNTRIES)) {
            placeNameService = new PlaceNameService(service, "topp:countries", fileCachePath, Sector.FULL_SPHERE,
                GRID_4x8,
                Font.decode("Arial-BOLD-10"), addVersionTag);
            placeNameService.setColor(Color.white);
            placeNameService.setMinDisplayDistance(LEVEL_G);
            placeNameService.setMaxDisplayDistance(LEVEL_D);
            placeNameServiceSet.addService(placeNameService, false);
        }
        // GeoNet World Capitals
        if (activeNamesList.contains(GEONET_P_PPLC)) {
            placeNameService = new PlaceNameService(service, "topp:wpl_geonet_p_pplc", fileCachePath,
                Sector.FULL_SPHERE,
                GRID_16x32, Font.decode("Arial-BOLD-10"), addVersionTag);
            placeNameService.setColor(Color.yellow);
            placeNameService.setMinDisplayDistance(0.0d);
            placeNameService.setMaxDisplayDistance(LEVEL_D);
            placeNameServiceSet.addService(placeNameService, false);
        }
        // World Cities >= 500k
        if (activeNamesList.contains(CITIESOVER500K)) {
            placeNameService = new PlaceNameService(service, "topp:citiesover500k", fileCachePath, Sector.FULL_SPHERE,
                GRID_8x16, Font.decode("Arial-BOLD-10"), addVersionTag);
            placeNameService.setColor(Color.yellow);
            placeNameService.setMinDisplayDistance(0);
            placeNameService.setMaxDisplayDistance(LEVEL_D);
            placeNameServiceSet.addService(placeNameService, false);
        }
        // World Cities >= 100k
        if (activeNamesList.contains(CITIESOVER100K)) {
            placeNameService = new PlaceNameService(service, "topp:citiesover100k", fileCachePath, Sector.FULL_SPHERE,
                GRID_16x32, Font.decode("Arial-PLAIN-10"), addVersionTag);
            placeNameService.setColor(Color.yellow);
            placeNameService.setMinDisplayDistance(LEVEL_N);
            placeNameService.setMaxDisplayDistance(LEVEL_F);
            placeNameServiceSet.addService(placeNameService, false);
        }
        // World Cities >= 50k and <100k
        if (activeNamesList.contains(CITIESOVER50K)) {
            placeNameService = new PlaceNameService(service, "topp:citiesover50k", fileCachePath, Sector.FULL_SPHERE,
                GRID_16x32, Font.decode("Arial-PLAIN-10"), addVersionTag);
            placeNameService.setColor(Color.yellow);
            placeNameService.setMinDisplayDistance(LEVEL_N);
            placeNameService.setMaxDisplayDistance(LEVEL_H);
            placeNameServiceSet.addService(placeNameService, false);
        }

        // World Cities >= 10k and <50k
        if (activeNamesList.contains(CITIESOVER10K)) {
            placeNameService = new PlaceNameService(service, "topp:citiesover10k", fileCachePath, Sector.FULL_SPHERE,
                GRID_36x72, Font.decode("Arial-PLAIN-10"), addVersionTag);
            placeNameService.setColor(Color.yellow);
            placeNameService.setMinDisplayDistance(0.0d);
            placeNameService.setMaxDisplayDistance(LEVEL_I);
            placeNameServiceSet.addService(placeNameService, false);
        }

        // World Cities >= 1k and <10k
        if (activeNamesList.contains(CITIESOVER1K)) {
            placeNameService = new PlaceNameService(service, "topp:citiesover1k", fileCachePath, Sector.FULL_SPHERE,
                GRID_36x72, Font.decode("Arial-PLAIN-10"), addVersionTag);
            placeNameService.setColor(Color.yellow);
            placeNameService.setMinDisplayDistance(0.0d);
            placeNameService.setMaxDisplayDistance(LEVEL_K);
            placeNameServiceSet.addService(placeNameService, false);
        }
        // US Cities (Population Over 0)
        if (activeNamesList.contains(USCITIESOVER0)) {
            //values for masking sector pulled from wfs capabilities request
            Sector maskingSector = new Sector(Angle.fromDegrees(18.0), Angle.fromDegrees(70.7),
                Angle.fromDegrees(-176.66), Angle.fromDegrees(-66.0));
            placeNameService = new PlaceNameService(service, "topp:wpl_uscitiesover0", fileCachePath, maskingSector,
                GRID_36x72, Font.decode("Arial-PLAIN-10"), addVersionTag);
            placeNameService.setColor(Color.yellow);
            placeNameService.setMinDisplayDistance(0.0d);
            placeNameService.setMaxDisplayDistance(LEVEL_N);
            placeNameServiceSet.addService(placeNameService, false);
        }
        // US Cities (No Population)
        if (activeNamesList.contains(USCITIES0)) {
            //values for masking sector pulled from wfs capabilities request
            Sector maskingSector = new Sector(Angle.fromDegrees(-14.4), Angle.fromDegrees(71.3),
                Angle.fromDegrees(-176.66), Angle.fromDegrees(178.88));
            placeNameService = new PlaceNameService(service, "topp:wpl_uscities0", fileCachePath, maskingSector,
                GRID_288x576, Font.decode("Arial-PLAIN-10"), addVersionTag);
            placeNameService.setColor(Color.orange);
            placeNameService.setMinDisplayDistance(0.0d);
            placeNameService.setMaxDisplayDistance(LEVEL_N);//M);
            placeNameServiceSet.addService(placeNameService, false);
        }

        // US Anthropogenic Features
        if (activeNamesList.contains(US_ANTHROPOGENIC)) {
            placeNameService = new PlaceNameService(service, "topp:wpl_us_anthropogenic", fileCachePath,
                Sector.FULL_SPHERE, GRID_288x576,
                Font.decode("Arial-PLAIN-10"), addVersionTag);
            placeNameService.setColor(Color.orange);
            placeNameService.setMinDisplayDistance(0.0d);
            placeNameService.setMaxDisplayDistance(LEVEL_P);
            placeNameServiceSet.addService(placeNameService, false);
        }

        // US Water Features
        if (activeNamesList.contains(US_WATER)) {
            placeNameService = new PlaceNameService(service, "topp:wpl_us_water", fileCachePath, Sector.FULL_SPHERE,
                GRID_144x288,
                Font.decode("Arial-PLAIN-10"), addVersionTag);
            placeNameService.setColor(Color.cyan);
            placeNameService.setMinDisplayDistance(0.0d);
            placeNameService.setMaxDisplayDistance(LEVEL_M);
            placeNameServiceSet.addService(placeNameService, false);
        }
        // US Terrain Features
        if (activeNamesList.contains(US_TERRAIN)) {
            placeNameService = new PlaceNameService(service, "topp:wpl_us_terrain", fileCachePath, Sector.FULL_SPHERE,
                GRID_72x144,
                Font.decode("Arial-PLAIN-10"), addVersionTag);
            placeNameService.setColor(Color.orange);
            placeNameService.setMinDisplayDistance(0.0d);
            placeNameService.setMaxDisplayDistance(LEVEL_O);
            placeNameServiceSet.addService(placeNameService, false);
        }
        // GeoNET Administrative 1st Order
        if (activeNamesList.contains(GEONET_A_ADM1)) {
            placeNameService = new PlaceNameService(service, "topp:wpl_geonet_a_adm1", fileCachePath,
                Sector.FULL_SPHERE, GRID_36x72,
                Font.decode("Arial-BOLD-10"), addVersionTag);
            placeNameService.setColor(Color.yellow);
            placeNameService.setMinDisplayDistance(0.0d);
            placeNameService.setMaxDisplayDistance(LEVEL_N);
            placeNameServiceSet.addService(placeNameService, false);
        }
        // GeoNET Administrative 2nd Order
        if (activeNamesList.contains(GEONET_A_ADM2)) {
            placeNameService = new PlaceNameService(service, "topp:wpl_geonet_a_adm2", fileCachePath,
                Sector.FULL_SPHERE, GRID_36x72,
                Font.decode("Arial-BOLD-10"), addVersionTag);
            placeNameService.setColor(Color.yellow);
            placeNameService.setMinDisplayDistance(0.0d);
            placeNameService.setMaxDisplayDistance(LEVEL_N);
            placeNameServiceSet.addService(placeNameService, false);
        }
        // GeoNET Populated Place Administrative
        if (activeNamesList.contains(GEONET_P_PPLA)) {
            placeNameService = new PlaceNameService(service, "topp:wpl_geonet_p_ppla", fileCachePath,
                Sector.FULL_SPHERE, GRID_36x72,
                Font.decode("Arial-BOLD-10"), addVersionTag);
            placeNameService.setColor(Color.pink);
            placeNameService.setMinDisplayDistance(0.0d);
            placeNameService.setMaxDisplayDistance(LEVEL_N);
            placeNameServiceSet.addService(placeNameService, false);
        }
        // GeoNET Populated Place
        if (activeNamesList.contains(GEONET_P_PPL)) {
            placeNameService = new PlaceNameService(service, "topp:wpl_geonet_p_ppl", fileCachePath, Sector.FULL_SPHERE,
                GRID_36x72,
                Font.decode("Arial-PLAIN-10"), addVersionTag);
            placeNameService.setColor(Color.pink);
            placeNameService.setMinDisplayDistance(0.0d);
            placeNameService.setMaxDisplayDistance(LEVEL_O);
            placeNameServiceSet.addService(placeNameService, false);
        }

        return placeNameServiceSet;
    }

    public void setPlaceNameSetsVisible(List names) {
        activeNamesList = names;
        makePlaceNameServiceSet();
    }

    @Override
    public String toString() {
        return Logging.getMessage("layers.Earth.PlaceName.Name");
    }
}
