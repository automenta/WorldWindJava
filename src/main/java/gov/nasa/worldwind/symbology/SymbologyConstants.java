/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */

package gov.nasa.worldwind.symbology;

import java.util.*;

/**
 * Defines constants used by the WorldWind symbology classes, including symbolic constants and modifier keys for
 * MIL-STD-2525 tactical symbols and tactical graphics.
 *
 * @author dcollins
 * @version $Id: SymbologyConstants.java 1171 2013-02-11 21:45:02Z dcollins $
 */
public interface SymbologyConstants {
    /**
     * The MIL-STD-2525 Additional Information modifier field ID.  The meaning of this field is implementation specific.
     * See MIL-STD-2525 section 5.3.4.10 (page 29), table IV (pages 22-24) and table XIV (pages 46-47). When used as a
     * key, the corresponding value must be a string containing up to 20 characters.
     */
    String ADDITIONAL_INFORMATION = "H";

    /**
     * The MIL-STD-2525 Altitude/Depth modifier field ID. Indicates either altitude flight level, depth for submerged
     * objects, or height of equipment or structures on the ground. See MIL-STD-2525 section 5.5.2.5 (page 40-41), table
     * IV (pages 22-24) and table XIV (pages 46-47). When used as a key, the corresponding value must be a string
     * containing up to 14 characters.
     */
    String ALTITUDE_DEPTH = "X";

    /**
     * The MIL-STD-2525 Auxiliary Equipment Indicator modifier field ID. Indicates the auxiliary equipment code
     * associated with a MIL-STD-2525 symbol (SIDC positions 11-12). A symbol's auxiliary equipment is currently used to
     * define towed sonar arrays. See MIL-STD-2525C section 5.3.4.4 (page 27) and table VII (page 28). When used as a
     * key, the corresponding value must be one of the following:
     * <ul> <li>AUXILIARY_EQUIPMENT_TOWED_SONAR_ARRAY_SHORT</li> <li>AUXILIARY_EQUIPMENT_TOWED_SONAR_ARRAY_LONG</li>
     * </ul>
     * <p>
     * The auxiliary equipment codes are the same for all symbology schemes that use them, and are defined in each
     * appendix of the MIL-STD-2525C specification:
     * <ul> <li>Warfighting - section A.5.2.1.f (page 51) and table A-II (pages 52-54)</li> </ul>
     */
    String AUXILIARY_EQUIPMENT = "AG";
    /**
     * The MIL-STD-2525 Towed Sonar Array (Long) auxiliary equipment code. See MIL-STD-2525C section 5.3.4.4 (page 27)
     * and table VII (page 28).
     */
    String AUXILIARY_EQUIPMENT_TOWED_SONAR_ARRAY_LONG = "NL";
    /**
     * The MIL-STD-2525 Towed Sonar Array (Short) auxiliary equipment code. See MIL-STD-2525C section 5.3.4.4 (page 27)
     * and table VII (page 28).
     */
    String AUXILIARY_EQUIPMENT_TOWED_SONAR_ARRAY_SHORT = "NS";
    /**
     * List containing all recognized MIL-STD-2525 auxiliary equipment codes.
     */
    List<String> AUXILIARY_EQUIPMENT_ALL = Arrays.asList(
        SymbologyConstants.AUXILIARY_EQUIPMENT_TOWED_SONAR_ARRAY_SHORT,
        SymbologyConstants.AUXILIARY_EQUIPMENT_TOWED_SONAR_ARRAY_LONG
    );

    /**
     * The MIL-STD-2525 Azimuth modifier field ID. Indicates a distance in meters (radius, length, width, etc). See
     * MIL-STD-2525 section 5.5.2 (page 38), table XI (pages 38-39) and table XIV (pages 46-47). When used as a key, the
     * corresponding value must be an {@link gov.nasa.worldwind.geom.Angle} indicating an angle relative to true north.
     */
    String AZIMUTH = "AN";

    /**
     * Indicates the battle dimension code associated with a MIL-STD-2525 symbol (SIDC position 3). A symbol's battle
     * dimension defines the primary mission area for the object being represented. See MIL-STD-2525C section 5.3.1.3
     * (page 17), table I (page 15) and table II (page 16). When used as a key, the corresponding value must be one of
     * the following:
     * <ul> <li>BATTLE_DIMENSION_SPACE</li> <li>BATTLE_DIMENSION_AIR</li> <li>BATTLE_DIMENSION_GROUND</li>
     * <li>BATTLE_DIMENSION_SEA_SURFACE</li> <li>BATTLE_DIMENSION_SEA_SUBSURFACE</li> <li>BATTLE_DIMENSION_SOF</li>
     * <li>BATTLE_DIMENSION_OTHER</li> </ul>
     * <p>
     * The battle dimension codes are the same for all symbology schemes that use them, and are defined in each appendix
     * of the MIL-STD-2525C specification:
     * <ul> <li>Warfighting - section A.5.2.1.c (page 51) and table A-I (page 51)</li> <li>Signals Intelligence -
     * section D.5.2.1.c (page 963) and table D-I (page 964)</li> </ul>
     */
    String BATTLE_DIMENSION = "gov.nasa.worldwind.symbology.BattleDimension";
    /**
     * The MIL-STD-2525 Air battle dimension code. Indicates a symbol who's mission area is between the surface of the
     * Earth and the space dimension. See MIL-STD-2525C section 5.3.1.3 (page 17).
     */
    String BATTLE_DIMENSION_AIR = "A";
    /**
     * The MIL-STD-2525 Ground battle dimension code. Indicates a symbol who's mission area is on the land surface. See
     * MIL-STD-2525C section 5.3.1.3 (page 17).
     */
    String BATTLE_DIMENSION_GROUND = "G";
    /**
     * The MIL-STD-2525 Other battle dimension code. Indicates a symbol who's mission area is not one of the pre-defined
     * areas: space, air, ground, sea surface, sea subsurface, or special operations forces. See MIL-STD-2525C section
     * 5.3.1.3 (page 17).
     */
    String BATTLE_DIMENSION_OTHER = "X";
    /**
     * The MIL-STD-2525 Sea Subsurface battle dimension code. Indicates a symbol who's mission area is below the sea
     * surface. See MIL-STD-2525C section 5.3.1.3 (page 17).
     */
    String BATTLE_DIMENSION_SEA_SUBSURFACE = "U";
    /**
     * The MIL-STD-2525 Sea Surface battle dimension code. Indicates a symbol who's mission area is on the sea surface.
     * See MIL-STD-2525C section 5.3.1.3 (page 17).
     */
    String BATTLE_DIMENSION_SEA_SURFACE = "S";
    /**
     * The MIL-STD-2525 Special Operations Forces (SOF) battle dimension code. See MIL-STD-2525C section 5.3.1.3 (page
     * 17).
     */
    String BATTLE_DIMENSION_SOF = "F";
    /**
     * The MIL-STD-2525 Space battle dimension code. Indicates a symbol who's mission area is at the lower boundary of
     * the Earth's ionosphere or above. See MIL-STD-2525C section 5.3.1.3 (page 17).
     */
    String BATTLE_DIMENSION_SPACE = "P";
    /**
     * The MIL-STD-2525 Unknown battle dimension code. Indicates a symbol who's battle dimension either has not been
     * determined or cannot be determined. See MIL-STD-2525C section 5.3.1.3 (page 17).
     */
    String BATTLE_DIMENSION_UNKNOWN = "Z";
    /**
     * List containing all recognized MIL-STD-2525 battle dimension codes.
     */
    List<String> BATTLE_DIMENSION_ALL = Arrays.asList(
        SymbologyConstants.BATTLE_DIMENSION_UNKNOWN,
        SymbologyConstants.BATTLE_DIMENSION_SPACE,
        SymbologyConstants.BATTLE_DIMENSION_AIR,
        SymbologyConstants.BATTLE_DIMENSION_GROUND,
        SymbologyConstants.BATTLE_DIMENSION_SEA_SURFACE,
        SymbologyConstants.BATTLE_DIMENSION_SEA_SUBSURFACE,
        SymbologyConstants.BATTLE_DIMENSION_SOF,
        SymbologyConstants.BATTLE_DIMENSION_OTHER
    );
    /**
     * List containing all recognized MIL-STD-2525 battle dimension codes for the Signals Intelligence scheme.
     */
    List<String> BATTLE_DIMENSION_ALL_INTELLIGENCE = Arrays.asList(
        SymbologyConstants.BATTLE_DIMENSION_UNKNOWN,
        SymbologyConstants.BATTLE_DIMENSION_SPACE,
        SymbologyConstants.BATTLE_DIMENSION_AIR,
        SymbologyConstants.BATTLE_DIMENSION_GROUND,
        SymbologyConstants.BATTLE_DIMENSION_SEA_SURFACE,
        SymbologyConstants.BATTLE_DIMENSION_SEA_SUBSURFACE,
        SymbologyConstants.BATTLE_DIMENSION_OTHER
    );

    /**
     * Indicates the category code associated with a MIL-STD-2525 symbol (SIDC position 3). The meaning of a symbol's
     * category and the recognized values depend on the specific MIL-STD-2525 symbology scheme the symbol belongs to,
     * and are defined in each appendix of the MIL-STD-2525C specification:
     * <p>
     * <strong>Tactical Graphics</strong> <br> See MIL-STD-2525C section B5.2.1.c (page 304) and table B-I (page 305).
     * <ul> <li>CATEGORY_TASKS</li> <li>CATEGORY_COMMAND_CONTROL_GENERAL_MANEUVER</li>
     * <li>CATEGORY_MOBILITY_SURVIVABILITY</li> <li>CATEGORY_FIRE_SUPPORT</li> <li>CATEGORY_COMBAT_SERVICE_SUPPORT</li>
     * <li>CATEGORY_OTHER</li> </ul>
     * <p>
     * <strong>Stability Operations</strong> <br> See MIL-STD-2525C section E5.2.1.c (page 991) and table E-I (page
     * 991).
     * <ul> <li>CATEGORY_VIOLENT_ACTIVITIES</li> <li>CATEGORY_LOCATIONS</li> <li>CATEGORY_OPERATIONS</li>
     * <li>CATEGORY_ITEMS</li> <li>CATEGORY_INDIVIDUAL</li> <li>CATEGORY_NONMILITARY_GROUP_ORGANIZATION</li>
     * <li>CATEGORY_RAPE</li> </ul>
     * <p>
     * <strong>Emergency Management</strong> <br> See MIL-STD-2525C table G-I (page 1032).
     * <ul> <li>CATEGORY_INCIDENT</li> <li>CATEGORY_NATURAL_EVENTS</li> <li>CATEGORY_OPERATIONS</li>
     * <li>CATEGORY_INFRASTRUCTURE</li> </ul>
     */
    String CATEGORY = "gov.nasa.worldwind.symbology.Category";
    /**
     * The MIL-STD-2525 Command and Control General Maneuver category code, used by symbols belonging to the Tactical
     * Graphics scheme.
     */
    String CATEGORY_COMMAND_CONTROL_GENERAL_MANEUVER = "G";
    /**
     * The MIL-STD-2525 Combat Service Support category code, used by symbols belonging to the Tactical Graphics
     * scheme.
     */
    String CATEGORY_COMBAT_SERVICE_SUPPORT = "S";
    /**
     * The MIL-STD-2525 Fire Support category code, used by symbols belonging to the Tactical Graphics scheme.
     */
    String CATEGORY_FIRE_SUPPORT = "F";
    /**
     * The MIL-STD-2525 Incident category code, used by symbols belonging to the Emergency Management scheme.
     */
    String CATEGORY_INCIDENT = "I";
    /**
     * The MIL-STD-2525 Individual category code, used by symbols belonging to the Stability Operations scheme.
     */
    String CATEGORY_INDIVIDUAL = "P";
    /**
     * The MIL-STD-2525 Infrastructure category code, used by symbols belonging to the Emergency Management scheme.
     */
    String CATEGORY_INFRASTRUCTURE = "F";
    /**
     * The MIL-STD-2525 Items category code, used by symbols belonging to the Stability Operations scheme.
     */
    String CATEGORY_ITEMS = "I";
    /**
     * The MIL-STD-2525 Locations category code, used by symbols belonging to the Stability Operations scheme.
     */
    String CATEGORY_LOCATIONS = "L";
    /**
     * The MIL-STD-2525 Mobility/Survivability category code, used by symbols belonging to the Tactical Graphics
     * scheme.
     */
    String CATEGORY_MOBILITY_SURVIVABILITY = "M";
    /**
     * The MIL-STD-2525 Natural Events category code, used by symbols belonging to the Emergency Management scheme.
     */
    String CATEGORY_NATURAL_EVENTS = "N";
    /**
     * The MIL-STD-2525 Non-Military Group or Organization category code, used by symbols belonging to the Stability
     * Operations scheme.
     */
    String CATEGORY_NONMILITARY_GROUP_ORGANIZATION = "G";
    /**
     * The MIL-STD-2525 Operations category code, used by symbols belonging to the Stability Operations and Emergency
     * Management schemes.
     */
    String CATEGORY_OPERATIONS = "O";
    /**
     * The MIL-STD-2525 Other category code, used by symbols belonging to the Tactical Graphics scheme.
     */
    String CATEGORY_OTHER = "O";
    /**
     * The MIL-STD-2525 Rape category code, used by symbols belonging to the Stability Operations scheme.
     */
    String CATEGORY_RAPE = "R";
    /**
     * The MIL-STD-2525 Tasks category code, used by symbols belonging to the Tactical Graphics scheme.
     */
    String CATEGORY_TASKS = "T";
    /**
     * The MIL-STD-2525 Violent Activities category code, used by symbols belonging to the Stability Operations scheme.
     */
    String CATEGORY_VIOLENT_ACTIVITIES = "V";

    /**
     * The MIL-STD-2525 Atmospheric category code, used by symbols belonging to the METOC scheme.
     */
    String CATEGORY_ATMOSPHERIC = "A";
    /**
     * The MIL-STD-2525 Oceanic category code, used by symbols belonging to the METOC scheme.
     */
    String CATEGORY_OCEANIC = "O";
    /**
     * The MIL-STD-2525 Space category code, used by symbols belonging to the METOC scheme.
     */
    String CATEGORY_SPACE = "S";

    /**
     * List containing all recognized MIL-STD-2525 category codes.
     */
    List<String> CATEGORY_ALL = Arrays.asList(
        // Tactical Graphics category codes.
        SymbologyConstants.CATEGORY_TASKS,
        SymbologyConstants.CATEGORY_COMMAND_CONTROL_GENERAL_MANEUVER,
        SymbologyConstants.CATEGORY_MOBILITY_SURVIVABILITY,
        SymbologyConstants.CATEGORY_FIRE_SUPPORT,
        SymbologyConstants.CATEGORY_COMBAT_SERVICE_SUPPORT,
        SymbologyConstants.CATEGORY_OTHER,
        // Stability Operations category codes.
        SymbologyConstants.CATEGORY_VIOLENT_ACTIVITIES,
        SymbologyConstants.CATEGORY_LOCATIONS,
        SymbologyConstants.CATEGORY_OPERATIONS,
        SymbologyConstants.CATEGORY_ITEMS,
        SymbologyConstants.CATEGORY_INDIVIDUAL,
        SymbologyConstants.CATEGORY_NONMILITARY_GROUP_ORGANIZATION,
        SymbologyConstants.CATEGORY_RAPE,
        // Emergency Management category codes (CATEGORY_OPERATIONS already included from Tactical Graphics).
        SymbologyConstants.CATEGORY_INCIDENT,
        SymbologyConstants.CATEGORY_NATURAL_EVENTS,
        SymbologyConstants.CATEGORY_INFRASTRUCTURE,
        // METOC category codes
        SymbologyConstants.CATEGORY_ATMOSPHERIC,
        SymbologyConstants.CATEGORY_OCEANIC,
        SymbologyConstants.CATEGORY_SPACE
    );
    /**
     * List containing all recognized MIL-STD-2525 category codes for the Tactical Graphics scheme.
     */
    List<String> CATEGORY_ALL_TACTICAL_GRAPHICS = Arrays.asList(
        SymbologyConstants.CATEGORY_TASKS,
        SymbologyConstants.CATEGORY_COMMAND_CONTROL_GENERAL_MANEUVER,
        SymbologyConstants.CATEGORY_MOBILITY_SURVIVABILITY,
        SymbologyConstants.CATEGORY_FIRE_SUPPORT,
        SymbologyConstants.CATEGORY_COMBAT_SERVICE_SUPPORT,
        SymbologyConstants.CATEGORY_OTHER
    );
    /**
     * List containing all recognized MIL-STD-2525 category codes for the Stability Operations scheme.
     */
    List<String> CATEGORY_ALL_STABILITY_OPERATIONS = Arrays.asList(
        SymbologyConstants.CATEGORY_VIOLENT_ACTIVITIES,
        SymbologyConstants.CATEGORY_LOCATIONS,
        SymbologyConstants.CATEGORY_OPERATIONS,
        SymbologyConstants.CATEGORY_ITEMS,
        SymbologyConstants.CATEGORY_INDIVIDUAL,
        SymbologyConstants.CATEGORY_NONMILITARY_GROUP_ORGANIZATION,
        SymbologyConstants.CATEGORY_RAPE
    );
    /**
     * List containing all recognized MIL-STD-2525 category codes for the Emergency Management scheme.
     */
    List<String> CATEGORY_ALL_EMERGENCY_MANAGEMENT = Arrays.asList(
        SymbologyConstants.CATEGORY_INCIDENT,
        SymbologyConstants.CATEGORY_NATURAL_EVENTS,
        SymbologyConstants.CATEGORY_OPERATIONS,
        SymbologyConstants.CATEGORY_INFRASTRUCTURE
    );
    /**
     * List containing all recognized MIL-STD-2525 category codes for the Meteorological and Oceanographic scheme.
     */
    List<String> CATEGORY_ALL_METOC = Arrays.asList(
        SymbologyConstants.CATEGORY_ATMOSPHERIC,
        SymbologyConstants.CATEGORY_OCEANIC,
        SymbologyConstants.CATEGORY_SPACE
    );

    /**
     * The MIL-STD-2525 Combat Effectiveness modifier field ID. Indicates a unit's effectiveness or an installation's
     * capability. See MIL-STD-2525 section 5.3.4.10 (page 29), table IV (pages 22-24) and table XIV (pages 46-47). When
     * used as a key, the corresponding value must be a string containing up to 5 characters.
     */
    String COMBAT_EFFECTIVENESS = "K";

    /**
     * Indicates the country code associated with a MIL-STD-2525 symbol (SIDC positions 13-14). See <a
     * href="http://www.iso.org/iso/country_codes.htm" target="_blank">ISO 3166-1</a> for a definition of valid country
     * codes. The country codes are the same for all symbology schemes that use them:
     * <ul> <li>Warfighting - section A.5.2.1.g and table A-I (page 51)</li> <li>Tactical Graphics - section B.5.2.1.g
     * (page 304) and table B-I (page 305)</li> <li>Signals Intelligence - section D.5.2.1.g and table D-I (page
     * 964)</li> <li>Stability Operations - section E.5.2.1.g and table E-I (page 991)</li> <li>Emergency Management -
     * table G-I (page 1032)</li> </ul>
     */
    String COUNTRY_CODE = "gov.nasa.worldwind.symbology.CountryCode";

    /**
     * The MIL-STD-2525 Date Time Group (DTG) modifier field ID. Displays a time in the DTG format "DDHHMMSSZMONYYYY" or
     * "O/O" for on order. See MIL-STD-2525 section 5.5.2.6 (page 41-42), table IV (pages 22-24) and table XIV (pages
     * 46-47). When used as a key, the corresponding value must be a string containing up to 16 characters.
     */
    String DATE_TIME_GROUP = "W";
    /**
     * The MIL-STD-2525 Direction of Movement Indicator modifier field ID. Indicates the direction of movement or
     * intended movement of an object. See MIL-STD-2525 section 5.3.4.1 (page 25), table IV (pages 22-24) and table XIV
     * (pages 46-47). When used as a key, the corresponding value must be an {@link gov.nasa.worldwind.geom.Angle}
     * indicating the object's heading relative to true north.
     */
    String DIRECTION_OF_MOVEMENT = "Q";

    /**
     * The MIL-STD-2525 Distance modifier field ID. Indicates a distance in meters (radius, length, width, etc). See
     * MIL-STD-2525 section 5.5.2 (page 38), table XI (pages 38-39) and table XIV (pages 46-47). When used as a key, the
     * corresponding value must be a Double.
     */
    String DISTANCE = "AM";

    /**
     * The MIL-STD-2525 Echelon modifier field ID. Indicates the echelon code associated with a MIL-STD-2525 symbol
     * (SIDC position 12). A symbol's echelon defines the command level of a unit represented by the symbol. See
     * MIL-STD-2525 section 5.3.4.2 (page 25), section 5.5.2.2 (page 40), and table V (pages 25-26). When used as a key,
     * the corresponding value must be one of the following:
     * <ul> <li>ECHELON_TEAM_CREW</li> <li>ECHELON_SQUAD</li> <li>ECHELON_SECTION</li>
     * <li>ECHELON_PLATOON_DETACHMENT</li> <li>ECHELON_COMPANY_BATTERY_TROOP</li> <li>ECHELON_BATTALION_SQUADRON</li>
     * <li>ECHELON_REGIMENT_GROUP</li> <li>ECHELON_BRIGADE</li> <li>ECHELON_DIVISION</li> <li>ECHELON_CORPS</li>
     * <li>ECHELON_ARMY</li> <li>ECHELON_ARMY_GROUP_FRONT</li> <li>ECHELON_REGION</li> <li>ECHELON_COMMAND</li> </ul>
     * <p>
     * The echelon codes are the same for all symbology schemes that use them, and are defined in each appendix of the
     * MIL-STD-2525C specification:
     * <ul> <li>Warfighting - section A.5.2.1.f (page 51) and table A-II (pages 52-54)</li> <li>Tactical Graphics -
     * section B.5.2.1.f (page 304) and table B-II (page 305)</li> <li>Stability Operations - section E.5.2.1.f (page
     * 991) and table E-II (pages 992-994)</li> </ul>
     */
    String ECHELON = "B";
    /**
     * The MIL-STD-2525 Army echelon code. See MIL-STD-2525 section 5.3.4.2 (page 25), section 5.5.2.2 (page 40), and
     * table V (pages 25-26).
     */
    String ECHELON_ARMY = "K";
    /**
     * The MIL-STD-2525 Army Group/Front echelon code. See MIL-STD-2525 section 5.3.4.2 (page 25), section 5.5.2.2 (page
     * 40), and table V (pages 25-26).
     */
    String ECHELON_ARMY_GROUP_FRONT = "L";
    /**
     * The MIL-STD-2525 Battalion/Squadron echelon code. See MIL-STD-2525 section 5.3.4.2 (page 25), section 5.5.2.2
     * (page 40), and table V (pages 25-26).
     */
    String ECHELON_BATTALION_SQUADRON = "F";
    /**
     * The MIL-STD-2525 Brigade echelon code. See MIL-STD-2525 section 5.3.4.2 (page 25), section 5.5.2.2 (page 40), and
     * table V (pages 25-26).
     */
    String ECHELON_BRIGADE = "H";
    /**
     * The MIL-STD-2525 Command echelon code. See MIL-STD-2525 section 5.3.4.2 (page 25), section 5.5.2.2 (page 40), and
     * table V (pages 25-26).
     */
    String ECHELON_COMMAND = "N";
    /**
     * The MIL-STD-2525 Company/Battery/Troop echelon code. See MIL-STD-2525 section 5.3.4.2 (page 25), section 5.5.2.2
     * (page 40), and table V (pages 25-26).
     */
    String ECHELON_COMPANY_BATTERY_TROOP = "E";
    /**
     * The MIL-STD-2525 Corps echelon code. See MIL-STD-2525 section 5.3.4.2 (page 25), section 5.5.2.2 (page 40), and
     * table V (pages 25-26).
     */
    String ECHELON_CORPS = "J";
    /**
     * The MIL-STD-2525 Division echelon code. See MIL-STD-2525 section 5.3.4.2 (page 25), section 5.5.2.2 (page 40),
     * and table V (pages 25-26).
     */
    String ECHELON_DIVISION = "I";
    /**
     * The MIL-STD-2525 Platoon/Detachment echelon code. See MIL-STD-2525 section 5.3.4.2 (page 25), section 5.5.2.2
     * (page 40), and table V (pages 25-26).
     */
    String ECHELON_PLATOON_DETACHMENT = "D";
    /**
     * The MIL-STD-2525 Section echelon code. See MIL-STD-2525 section 5.3.4.2 (page 25), section 5.5.2.2 (page 40), and
     * table V (pages 25-26).
     */
    String ECHELON_SECTION = "C";
    /**
     * The MIL-STD-2525 Squad echelon code. See MIL-STD-2525 section 5.3.4.2 (page 25), section 5.5.2.2 (page 40), and
     * table V (pages 25-26).
     */
    String ECHELON_SQUAD = "B";
    /**
     * The MIL-STD-2525 Team/Crew echelon code. See MIL-STD-2525 section 5.3.4.2 (page 25), section 5.5.2.2 (page 40),
     * and table V (pages 25-26).
     */
    String ECHELON_TEAM_CREW = "A";
    /**
     * The MIL-STD-2525 Regiment/Group echelon code. See MIL-STD-2525 section 5.3.4.2 (page 25), section 5.5.2.2 (page
     * 40), and table V (pages 25-26).
     */
    String ECHELON_REGIMENT_GROUP = "G";
    /**
     * The MIL-STD-2525 Region echelon code. See MIL-STD-2525 section 5.3.4.2 (page 25), section 5.5.2.2 (page 40), and
     * table V (pages 25-26).
     */
    String ECHELON_REGION = "M";
    /**
     * List containing all recognized MIL-STD-2525 echelon codes.
     */
    List<String> ECHELON_ALL = Arrays.asList(
        SymbologyConstants.ECHELON_TEAM_CREW,
        SymbologyConstants.ECHELON_SQUAD,
        SymbologyConstants.ECHELON_SECTION,
        SymbologyConstants.ECHELON_PLATOON_DETACHMENT,
        SymbologyConstants.ECHELON_COMPANY_BATTERY_TROOP,
        SymbologyConstants.ECHELON_BATTALION_SQUADRON,
        SymbologyConstants.ECHELON_REGIMENT_GROUP,
        SymbologyConstants.ECHELON_BRIGADE,
        SymbologyConstants.ECHELON_DIVISION,
        SymbologyConstants.ECHELON_CORPS,
        SymbologyConstants.ECHELON_ARMY,
        SymbologyConstants.ECHELON_ARMY_GROUP_FRONT,
        SymbologyConstants.ECHELON_REGION,
        SymbologyConstants.ECHELON_COMMAND
    );

    /**
     * The MIL-STD-2525 Evaluation Rating modifier field ID. Indicates the reliability and credibility of a unit,
     * equipment, or installation. When used as a key, the corresponding value must be a string containing two
     * characters.
     * <p>
     * The first character indicates the reliability rating, and must be one of the following: <ul> <li>"A" - completely
     * reliable</li> <li>"B" - usually reliable</li> <li>"C" - fairly reliable</li> <li>"D" - not usually reliable</li>
     * <li>"E" - unreliable</li> <li>"F" - reliability cannot be judged</li> </ul>
     * <p>
     * The second character indicates the credibility rating, and must be one of the following: <ul> <li>"1" - confirmed
     * by other sources</li> <li>"2" - probably true</li> <li>"3" - possibly true</li> <li>"4" - doubtfully true</li>
     * <li>"5" - improbable</li> <li>"6" - truth cannot be judged</li> </ul>
     * <p>
     * See FM 34-3, Intelligence Analysis, March 1990, pages 2-13 through 2-17 for complete definitions of evaluation
     * ratings.
     */
    String EVALUATION_RATING = "J";

    /**
     * The MIL-STD-2525 Feint/Dummy Indicator modifier field ID. Indicates whether a MIL-STD-2525 symbol's represented
     * object is a feint/dummy. A feint/dummy symbol indicates a unit, equipment, or installation designed to draw the
     * enemy's attention away from the area of the main attack. When a marked as a feint/dummy, a symbol's graphic is
     * changed to include a dashed inverted "V" above its frame. See MIL-STD-2525 section 5.3.4.7 (page 28). When used
     * as a key, the corresponding value must be a boolean value. The value is <code>true</code> if the symbol's
     * represented object is a feint/dummy, and <code>false</code> otherwise.
     * <p>
     * The following symbology schemes support the feint/dummy modifier:
     * <ul> <li>Warfighting</li> <li>Stability Operations</li> </ul>
     */
    String FEINT_DUMMY = "AB";

    /**
     * The MIL-STD-2525 Frame Shape modifier field ID. Indicates standard identity, battle dimension, or exercise
     * amplifying descriptors of an object. See MIL-STD-2525 table XI (pages 38-39) and table XIV (pages 46-47).When
     * used as a key, the corresponding value must be a string of any length.
     */
    String FRAME_SHAPE = "E";

    String FRAME_SHAPE_EXERCISE = "X";

    String FRAME_SHAPE_JOKER = "J";

    String FRAME_SHAPE_FAKER = "K";

    /**
     * Indicates the function ID associated with a MIL-STD-2525 symbol (SIDC positions 5-10). The function IDs are
     * unique to each symbology schemes that uses them, and are defined in each appendix of the MIL-STD-2525C
     * specification:
     * <ul> <li>Warfighting - section A.5.2.1.e (page 51) and table A-I (page 51)</li> <li>Tactical Graphics - section
     * B.5.2.1.e (page 304) and table B-I (page 305)</li> <li>Meteorological and Oceanographic - section C.5.2.1.d (page
     * 763) and table C-I (page 763)</li> <li>Signals Intelligence - section D.5.2.1.e (page 964) and table D-I (page
     * 964)</li> <li>Stability Operations - section E.5.2.1.e (page 991) and table E-I (page 991)</li> <li>Emergency
     * Management - table G-I (page 1032)</li> </ul>
     */
    String FUNCTION_ID = "gov.nasa.worldwind.symbology.FunctionId";

    /**
     * Indicates the type of a graphic in the Meteorological and Oceanographic scheme (SIDC positions 11-13). When used
     * as a key, the corresponding value must be one of the following:
     * <ul> <li>GRAPHIC_TYPE_POINT</li> <li>GRAPHIC_TYPE_LINE</li> <li>GRAPHIC_TYPE_AREA</li> </ul>
     */
    String GRAPHIC_TYPE = "gov.nasa.worldwind.symbology.GraphicType";
    /**
     * The MIL-STD-2525 Point type, used by symbols belonging to the METOC scheme.
     */
    String GRAPHIC_TYPE_POINT = "P--";
    /**
     * The MIL-STD-2525 Line type, used by symbols belonging to the METOC scheme.
     */
    String GRAPHIC_TYPE_LINE = "-L-";
    /**
     * The MIL-STD-2525 Area type, used by symbols belonging to the METOC scheme.
     */
    String GRAPHIC_TYPE_AREA = "--A";
    List<String> GRAPHIC_TYPE_ALL = Arrays.asList(
        SymbologyConstants.GRAPHIC_TYPE_POINT,
        SymbologyConstants.GRAPHIC_TYPE_LINE,
        SymbologyConstants.GRAPHIC_TYPE_AREA
    );

    /**
     * The MIL-STD-2525 Headquarters modifier field ID. Indicates whether a MIL-STD-2525 symbol's represented object is
     * a headquarters. A headquarters symbol indicates the headquarters associated with a unit, equipment, or
     * installation. When a marked as an headquarters, a symbol's graphic is changed to include a line extending
     * downward from the left side of its frame. See MIL-STD-2525 section 5.3.4.8 (page 29). When used as a key, the
     * corresponding value must be a boolean value. The value is <code>true</code> if the symbol's represented object is
     * a headquarters, and <code>false</code> otherwise.
     * <p>
     * The following symbology schemes support the headquarters modifier:
     * <ul> <li>Warfighting</li> <li>Stability Operations</li> </ul>
     */
    String HEADQUARTERS = "S";

    /**
     * The MIL-STD-2525 Higher Formation modifier field ID. Indicates the number or title of higher echelon command. See
     * MIL-STD-2525 section 5.3.4.10 (page 29), table IV (pages 22-24) and table XIV (pages 46-47). When used as a key,
     * the corresponding value must be the string containing up to 21 characters.
     */
    String HIGHER_FORMATION = "M";

    /**
     * String (ENY) that is displayed as part of MIL-STD-2525 graphics that depict hostile entities. See MIL-STD-2525
     * section 5.5.1.1 (page 37). This modifier is displayed automatically on graphics that represent hostile entities
     * and include Hostile Enemy modifier in the MIL-STD-2525C graphic template. Use the accessors on TacticalGraphic to
     * enable or disable display of this modifier for a particular graphic.
     *
     * @see TacticalGraphic#isShowHostileIndicator()
     * @see TacticalGraphic#setShowHostileIndicator(boolean)
     */
    String HOSTILE_ENEMY = "ENY";

    /**
     * The MIL-STD-2525 IFF/SIF modifier field ID. Indicates an IFF/SIF identification mode or code. See MIL-STD-2525
     * section 5.3.4.10 (page 29), table IV (pages 22-24) and table XIV (pages 46-47). When used as a key, the
     * corresponding value must be a string containing up to 5 characters.
     */
    String IFF_SIF = "P";

    /**
     * The MIL-STD-2525 Installation modifier field ID. Indicates the installation code associated with a MIL-STD-2525
     * symbol (SIDC positions 11-12). When a marked as an installation, a symbol's represented object is a military camp
     * or base. See MIL-STD-2525 section 5.3.4.5 (page 28). When used as a key, the corresponding value must be one of
     * the following:
     * <ul> <li>INSTALLATION_NORMAL</li> <li>INSTALLATION_FEINT_DUMMY</li>  </ul>
     * <p>
     * The installation codes are the same for all symbology schemes that use them, and are defined in each appendix of
     * the MIL-STD-2525C specification:
     * <ul> <li>Warfighting - section A.5.2.1.f (page 51) and table A-II (pages 52-54)</li> <li>Stability Operations -
     * section E.5.2.1.f (page 991) and table E-II (pages 992-994)</li> <li>Emergency Management - section G.5.5.5 (page
     * 1030) and table G-II (page 1032)</li> </ul>
     */
    String INSTALLATION = "AC";
    /**
     * The MIL-STD-2525 Normal (as opposed to Feint/Dummy) installation code. See MIL-STD-2525 section 5.3.4.5 (page
     * 28).
     */
    String INSTALLATION_NORMAL = "H-";
    /**
     * The MIL-STD-2525 Feint/Dummy installation code. See MIL-STD-2525 section 5.3.4.5 (page 28).
     */
    String INSTALLATION_FEINT_DUMMY = "HB";
    /**
     * List containing all recognized MIL-STD-2525 installation codes.
     */
    List<String> INSTALLATION_ALL = Arrays.asList(
        SymbologyConstants.INSTALLATION_NORMAL,
        SymbologyConstants.INSTALLATION_FEINT_DUMMY
    );

    /**
     * The MIL-STD-2525 Location modifier field ID. Indicates a symbol's location in any desired display format. See
     * MIL-STD-2525 section 5.3.4.10 (page 29), table IV (pages 22-24) and table XIV (pages 46-47). When used as a key,
     * the corresponding value must be a string containing up to 19 characters.
     */
    String LOCATION = "Y";

    /**
     * The MIL-STD-2525 Mobility Indicator modifier field ID. Indicates the equipment mobility code associated with a
     * MIL-STD-2525 symbol (SIDC positions 11-12). A symbol's mobility defines the mobility feature of the represented
     * object, other than mobility intrinsic to the represented object. Mobility codes are currently used only to
     * describe mobility features of equipment. See MIL-STD-2525 section 5.3.4.3 (page 26) and table VI (pages 26-27).
     * When used as a key, the corresponding value must be one of the following:
     * <ul> <li>MOBILITY_WHEELED</li> <li>MOBILITY_CROSS_COUNTRY</li> <li>MOBILITY_TRACKED</li>
     * <li>MOBILITY_WHEELED_TRACKED_COMBINATION</li> <li>MOBILITY_TOWED</li> <li>MOBILITY_RAIL</li>
     * <li>MOBILITY_OVER_THE_SNOW</li> <li>MOBILITY_SLED</li> <li>MOBILITY_PACK_ANIMALS</li> <li>MOBILITY_BARGE</li>
     * <li>MOBILITY_AMPHIBIOUS</li> </ul>
     * <p>
     * The mobility codes are the same for all symbology schemes that use them, and are defined in each appendix of the
     * MIL-STD-2525C specification:
     * <ul> <li>Warfighting - section A.5.2.1.f (page 51) and table A-II (pages 52-54)</li> <li>Emergency Management -
     * section G.5.5.5 (page 1030) and table G-II (page 1032)</li> </ul>
     */
    String MOBILITY = "R";
    /**
     * The MIL-STD-2525 Amphibious Mobility mobility code. See MIL-STD-2525 section 5.3.4.3 (page 26) and table VI
     * (pages 26-27).
     */
    String MOBILITY_AMPHIBIOUS = "MY";
    /**
     * The MIL-STD-2525 Barge mobility code. See MIL-STD-2525 section 5.3.4.3 (page 26) and table VI (pages 26-27).
     */
    String MOBILITY_BARGE = "MX";
    /**
     * The MIL-STD-2525 Cross Country mobility code. See MIL-STD-2525 section 5.3.4.3 (page 26) and table VI (pages
     * 26-27).
     */
    String MOBILITY_CROSS_COUNTRY = "MP";
    /**
     * The MIL-STD-2525 Over The Snow mobility code. See MIL-STD-2525 section 5.3.4.3 (page 26) and table VI (pages
     * 26-27).
     */
    String MOBILITY_OVER_THE_SNOW = "MU";
    /**
     * The MIL-STD-2525 Pack Animals mobility code. See MIL-STD-2525 section 5.3.4.3 (page 26) and table VI (pages
     * 26-27).
     */
    String MOBILITY_PACK_ANIMALS = "MW";
    /**
     * The MIL-STD-2525 Rail mobility code. See MIL-STD-2525 section 5.3.4.3 (page 26) and table VI (pages 26-27).
     */
    String MOBILITY_RAIL = "MT";
    /**
     * The MIL-STD-2525 Sled mobility code. See MIL-STD-2525 section 5.3.4.3 (page 26) and table VI (pages 26-27).
     */
    String MOBILITY_SLED = "MV";
    /**
     * The MIL-STD-2525 Towed mobility code. See MIL-STD-2525 section 5.3.4.3 (page 26) and table VI (pages 26-27).
     */
    String MOBILITY_TOWED = "MS";
    /**
     * The MIL-STD-2525 Tracked mobility code. See MIL-STD-2525 section 5.3.4.3 (page 26) and table VI (pages 26-27).
     */
    String MOBILITY_TRACKED = "MQ";
    /**
     * The MIL-STD-2525 Wheeled/Limited Cross Country mobility code. See MIL-STD-2525 section 5.3.4.3 (page 26) and
     * table VI (pages 26-27).
     */
    String MOBILITY_WHEELED = "MO";
    /**
     * The MIL-STD-2525 Wheeled And Tracked Combination mobility code. See MIL-STD-2525 section 5.3.4.3 (page 26) and
     * table VI (pages 26-27).
     */
    String MOBILITY_WHEELED_TRACKED_COMBINATION = "MR";
    /**
     * List containing all recognized MIL-STD-2525 mobility codes.
     */
    List<String> MOBILITY_ALL = Arrays.asList(
        SymbologyConstants.MOBILITY_WHEELED,
        SymbologyConstants.MOBILITY_CROSS_COUNTRY,
        SymbologyConstants.MOBILITY_TRACKED,
        SymbologyConstants.MOBILITY_WHEELED_TRACKED_COMBINATION,
        SymbologyConstants.MOBILITY_TOWED,
        SymbologyConstants.MOBILITY_RAIL,
        SymbologyConstants.MOBILITY_OVER_THE_SNOW,
        SymbologyConstants.MOBILITY_SLED,
        SymbologyConstants.MOBILITY_PACK_ANIMALS,
        SymbologyConstants.MOBILITY_BARGE,
        SymbologyConstants.MOBILITY_AMPHIBIOUS
    );

    /**
     * The MIL-STD-2525 feint/dummy units and equipment symbol modifier code. Indicates a symbol that is a feint/dummy.
     * Appears in SIDC position 11. See {@link #FEINT_DUMMY}.
     */
    String MODIFIER_CODE_FEINT_DUMMY = "F";
    /**
     * The MIL-STD-2525 feint/dummy headquarters units and equipment symbol modifier code. Indicates a symbol that is a
     * feint/dummy and a headquarters. Appears in SIDC position 11. See {@link #FEINT_DUMMY} and {@link #HEADQUARTERS}.
     */
    String MODIFIER_CODE_FEINT_DUMMY_HEADQUARTERS = "C";
    /**
     * The MIL-STD-2525 feint/dummy task force units and equipment symbol modifier code. Indicates a symbol that is a
     * feint/dummy and a task force. Appears in SIDC position 11. See {@link #FEINT_DUMMY} and {@link #TASK_FORCE}.
     */
    String MODIFIER_CODE_FEINT_DUMMY_TASK_FORCE = "G";
    /**
     * The MIL-STD-2525 feint/dummy task force headquarters units and equipment symbol modifier code. Indicates a symbol
     * that is a feint/dummy, a task force, and a headquarters. Appears in SIDC position 11. See {@link #FEINT_DUMMY},
     * {@link #TASK_FORCE}, and {@link #HEADQUARTERS}.
     */
    String MODIFIER_CODE_FEINT_DUMMY_TASK_FORCE_HEADQUARTERS = "D";
    /**
     * The MIL-STD-2525 headquarters units and equipment symbol modifier code. Indicates a symbol that is a
     * headquarters. Appears in SIDC position 11. See {@link #HEADQUARTERS}.
     */
    String MODIFIER_CODE_HEADQUARTERS = "A";
    /**
     * The MIL-STD-2525 task force units and equipment symbol modifier code. Indicates a symbol that is a task force.
     * Appears in SIDC position 11. See {@link #TASK_FORCE}.
     */
    String MODIFIER_CODE_TASK_FORCE = "E";
    /**
     * The MIL-STD-2525 task force headquarters units and equipment symbol modifier code. Indicates a symbol that is a
     * task force and a headquarters. Appears in SIDC position 11. See {@link #TASK_FORCE} and {@link #HEADQUARTERS}.
     */
    String MODIFIER_CODE_TASK_FORCE_HEADQUARTERS = "B";
    /**
     * List containing all recognized MIL-STD-2525 units and equipment symbol modifier codes.
     */
    List<String> MODIFIER_CODE_ALL_UEI = Arrays.asList(
        SymbologyConstants.MODIFIER_CODE_HEADQUARTERS,
        SymbologyConstants.MODIFIER_CODE_TASK_FORCE_HEADQUARTERS,
        SymbologyConstants.MODIFIER_CODE_FEINT_DUMMY_HEADQUARTERS,
        SymbologyConstants.MODIFIER_CODE_FEINT_DUMMY_TASK_FORCE_HEADQUARTERS,
        SymbologyConstants.MODIFIER_CODE_TASK_FORCE,
        SymbologyConstants.MODIFIER_CODE_FEINT_DUMMY,
        SymbologyConstants.MODIFIER_CODE_FEINT_DUMMY_TASK_FORCE
    );
    /**
     * List containing all recognized MIL-STD-2525 units and equipment symbol modifier codes that indicate a
     * feint/dummy.
     */
    List<String> MODIFIER_CODE_ALL_FEINT_DUMMY = Arrays.asList(
        SymbologyConstants.MODIFIER_CODE_FEINT_DUMMY_HEADQUARTERS,
        SymbologyConstants.MODIFIER_CODE_FEINT_DUMMY_TASK_FORCE_HEADQUARTERS,
        SymbologyConstants.MODIFIER_CODE_FEINT_DUMMY,
        SymbologyConstants.MODIFIER_CODE_FEINT_DUMMY_TASK_FORCE
    );
    /**
     * List containing all recognized MIL-STD-2525 units and equipment symbol modifier codes that indicate a
     * headquarters.
     */
    List<String> MODIFIER_CODE_ALL_HEADQUARTERS = Arrays.asList(
        SymbologyConstants.MODIFIER_CODE_HEADQUARTERS,
        SymbologyConstants.MODIFIER_CODE_TASK_FORCE_HEADQUARTERS,
        SymbologyConstants.MODIFIER_CODE_FEINT_DUMMY_HEADQUARTERS,
        SymbologyConstants.MODIFIER_CODE_FEINT_DUMMY_TASK_FORCE_HEADQUARTERS
    );
    /**
     * List containing all recognized MIL-STD-2525 units and equipment symbol modifier codes that indicate a task
     * force.
     */
    List<String> MODIFIER_CODE_ALL_TASK_FORCE = Arrays.asList(
        SymbologyConstants.MODIFIER_CODE_TASK_FORCE_HEADQUARTERS,
        SymbologyConstants.MODIFIER_CODE_FEINT_DUMMY_TASK_FORCE_HEADQUARTERS,
        SymbologyConstants.MODIFIER_CODE_TASK_FORCE,
        SymbologyConstants.MODIFIER_CODE_FEINT_DUMMY_TASK_FORCE
    );

    String OPERATIONAL_CONDITION = "gov.nasa.worldwind.symbology.OperationalCondition";
    String OPERATIONAL_CONDITION_DAMAGED = "OD";
    String OPERATIONAL_CONDITION_DESTROYED = "OX";
    List<String> OPERATIONAL_CONDITION_ALL = Arrays.asList(
        SymbologyConstants.OPERATIONAL_CONDITION_DAMAGED,
        SymbologyConstants.OPERATIONAL_CONDITION_DESTROYED
    );

    String OPERATIONAL_CONDITION_ALTERNATE = "gov.nasa.worldwind.symbology.OperationalConditionAlternate";
    String OPERATIONAL_CONDITION_ALTERNATE_FULLY_CAPABLE = "PC";
    String OPERATIONAL_CONDITION_ALTERNATE_DAMAGED = "PD";
    String OPERATIONAL_CONDITION_ALTERNATE_DESTROYED = "PX";
    String OPERATIONAL_CONDITION_ALTERNATE_FULL_TO_CAPACITY = "PF";
    List<String> OPERATIONAL_CONDITION_ALTERNATE_ALL = Arrays.asList(
        SymbologyConstants.OPERATIONAL_CONDITION_ALTERNATE_FULLY_CAPABLE,
        SymbologyConstants.OPERATIONAL_CONDITION_ALTERNATE_DAMAGED,
        SymbologyConstants.OPERATIONAL_CONDITION_ALTERNATE_DESTROYED,
        SymbologyConstants.OPERATIONAL_CONDITION_ALTERNATE_FULL_TO_CAPACITY
    );

    /**
     * Indicates the order of battle code associated with a MIL-STD-2525 symbol (SIDC position 15). A symbol's order of
     * battle provides additional information about the symbol in the operational environment. The recognized values
     * depend on the specific MIL-STD-2525 symbology scheme the symbol belongs to, and are defined in each appendix of
     * the MIL-STD-2525C specification:
     * <p>
     * <strong>Warfighting, Signals Intelligence, Stability Operations, Emergency Management</strong> <br> See
     * MIL-STD-2525C section A.5.2.1.h (page 51), table A-I (page 51), section D.5.2.1.h (page 964), table D-I (page
     * 964), section E.5.2.1.h (page 991), table E-I (page 991), and table G-I (page 1032).
     * <ul> <li>ORDER_OF_BATTLE_AIR</li> <li>ORDER_OF_BATTLE_ELECTRONIC</li> <li>ORDER_OF_BATTLE_CIVILIAN</li>
     * <li>ORDER_OF_BATTLE_GROUND</li> <li>ORDER_OF_BATTLE_MARITIME</li> <li>ORDER_OF_BATTLE_STRATEGIC_FORCE_RELATED</li>
     * </ul>
     * <p>
     * <strong>Tactical Graphics</strong> <br> See MIL-STD-2525C section B5.2.1.h (page 304) and table B-I (page 305).
     * <ul> <li>ORDER_OF_BATTLE_CONTROL_MARKINGS</li> </ul>
     */
    String ORDER_OF_BATTLE = "gov.nasa.worldwind.symbology.OrderOfBattle";
    /**
     * The MIL-STD-2525 Air order of battle code.
     */
    String ORDER_OF_BATTLE_AIR = "A";
    /**
     * The MIL-STD-2525 Civilian order of battle code.
     */
    String ORDER_OF_BATTLE_CIVILIAN = "C";
    /**
     * The MIL-STD-2525 Control Markings order of battle code.
     */
    String ORDER_OF_BATTLE_CONTROL_MARKINGS = "X";
    /**
     * The MIL-STD-2525 Electronic order of battle code.
     */
    String ORDER_OF_BATTLE_ELECTRONIC = "E";
    /**
     * The MIL-STD-2525 Ground order of battle code.
     */
    String ORDER_OF_BATTLE_GROUND = "G";
    /**
     * The MIL-STD-2525 Maritime order of battle code.
     */
    String ORDER_OF_BATTLE_MARITIME = "N";
    /**
     * The MIL-STD-2525 Strategic Force Related order of battle code.
     */
    String ORDER_OF_BATTLE_STRATEGIC_FORCE_RELATED = "S";
    /**
     * List containing all recognized MIL-STD-2525 order of battle codes.
     */
    List<String> ORDER_OF_BATTLE_ALL = Arrays.asList(
        SymbologyConstants.ORDER_OF_BATTLE_AIR,
        SymbologyConstants.ORDER_OF_BATTLE_CIVILIAN,
        SymbologyConstants.ORDER_OF_BATTLE_CONTROL_MARKINGS,
        SymbologyConstants.ORDER_OF_BATTLE_ELECTRONIC,
        SymbologyConstants.ORDER_OF_BATTLE_GROUND,
        SymbologyConstants.ORDER_OF_BATTLE_MARITIME,
        SymbologyConstants.ORDER_OF_BATTLE_STRATEGIC_FORCE_RELATED
    );
    /**
     * List containing all recognized MIL-STD-2525 order of battle codes for the Warfighting (UEI), Signals Intelligence
     * (SIGINT), Stability Operations (SO), and Emergency Management (EM) schemes.
     */
    List<String> ORDER_OF_BATTLE_ALL_UEI_SIGINT_SO_EM = Arrays.asList(
        SymbologyConstants.ORDER_OF_BATTLE_AIR,
        SymbologyConstants.ORDER_OF_BATTLE_ELECTRONIC,
        SymbologyConstants.ORDER_OF_BATTLE_CIVILIAN,
        SymbologyConstants.ORDER_OF_BATTLE_GROUND,
        SymbologyConstants.ORDER_OF_BATTLE_MARITIME,
        SymbologyConstants.ORDER_OF_BATTLE_STRATEGIC_FORCE_RELATED
    );
    /**
     * List containing all recognized MIL-STD-2525 order of battle codes for the Tactical Graphics scheme.
     */
    List<String> ORDER_OF_BATTLE_ALL_TACTICAL_GRAPHICS = Collections.singletonList(
        SymbologyConstants.ORDER_OF_BATTLE_CONTROL_MARKINGS
    );

    /**
     * The MIL-STD-2525 Quantity modifier field ID. Indicates the number of items associated with a MIL-STD-2525 symbol.
     * See MIL-STD-2525 section 5.3.4.10 (page 29), table IV (pages 22-24) and table XIV (pages 46-47). When used as a
     * key, the corresponding value must be a numeric value indicating the number of items present.
     */
    String QUANTITY = "C";

    /**
     * The MIL-STD-2525 Reinforced or Reduced modifier field ID. Indicates whether a unit is reinforced or reduced, or
     * both. When used as a key, the corresponding value must be one of the following values:
     * <ul> <li>REINFORCED to indicate that the unit is reinforced</li> <li>REDUCED to indicate that the unit is
     * reduced</li> <li>REINFORCED_AND_REDUCED to indicate that the unit is reinforced and reduced</li> </ul>
     */
    String REINFORCED_REDUCED = "F";

    String REINFORCED = "R";

    String REDUCED = "D";

    String REINFORCED_AND_REDUCED = "RD";

    /**
     * Indicates the scheme code associated with a MIL-STD-2525 symbol (SIDC position 1). A symbol's scheme defines the
     * specific MIL-STD-2525 symbology set that it belongs to. The scheme codes are defined in each appendix of the
     * MIL-STD-2525 specification. When used as a key, the corresponding value must be one of the following:
     * <ul> <li>SCHEME_WARFIGHTING</li> <li>SCHEME_TACTICAL_GRAPHICS</li> <li>SCHEME_METOC</li>
     * <li>SCHEME_INTELLIGENCE</li> <li>SCHEME_STABILITY_OPERATIONS</li> <li>SCHEME_EMERGENCY_MANAGEMENT</li> </ul>
     */
    String SCHEME = "gov.nasa.worldwind.symbology.Scheme";
    /**
     * The MIL-STD-2525 Emergency Management (EM) scheme code. See MIL-STD-2525C table G-I (page 1032).
     */
    String SCHEME_EMERGENCY_MANAGEMENT = "E";
    /**
     * The MIL-STD-2525 Signals Intelligence (SIGINT) scheme code. See MIL-STD-2525C table D-I (page 964).
     */
    String SCHEME_INTELLIGENCE = "I";
    /**
     * The MIL-STD-2525 Meteorological and Oceanographic (METOC) scheme code. See MIL-STD-2525C table C-I (page 763).
     */
    String SCHEME_METOC = "W";
    /**
     * The MIL-STD-2525 Stability Operations (SO) scheme code. See MIL-STD-2525C table E-I (page 991).
     */
    String SCHEME_STABILITY_OPERATIONS = "O";
    /**
     * The MIL-STD-2525 Tactical Graphics scheme code. See MIL-STD-2525C table B-I (page 305).
     */
    String SCHEME_TACTICAL_GRAPHICS = "G";
    /**
     * The MIL-STD-2525 Warfighting scheme code. This scheme is also referred to as Units, Equipment, and Installations
     * (UEI). See MIL-STD-2525C table A-I (page 51).
     */
    String SCHEME_WARFIGHTING = "S";
    /**
     * List containing all recognized MIL-STD-2525 scheme codes.
     */
    List<String> SCHEME_ALL = Arrays.asList(
        SymbologyConstants.SCHEME_WARFIGHTING,
        SymbologyConstants.SCHEME_TACTICAL_GRAPHICS,
        SymbologyConstants.SCHEME_METOC,
        SymbologyConstants.SCHEME_INTELLIGENCE,
        SymbologyConstants.SCHEME_STABILITY_OPERATIONS,
        SymbologyConstants.SCHEME_EMERGENCY_MANAGEMENT
    );

    /**
     * Indicates whether to display a MIL-STD-2525 tactical symbol's fill color. See MIL-STD-2525 section 5.4.5 (page
     * 24) and table IX (page 35). When used as a key, the corresponding value must be a boolean value. The value is
     * <code>true</code> if the symbol's fill color should be displayed, and <code>false</code> otherwise.
     */
    String SHOW_FILL = "gov.nasa.worldwind.symbology.ShowFill";
    /**
     * Indicates whether to display a MIL-STD-2525 tactical symbol's frame. See MIL-STD-2525 section 5.4.5 (page 24) and
     * table IX (page 35). When used as a key, the corresponding value must be a boolean value. The value is
     * <code>true</code> if the symbol's frame should be displayed, and <code>false</code> otherwise.
     */
    String SHOW_FRAME = "gov.nasa.worldwind.symbology.ShowFrame";
    /**
     * Indicates whether to display a MIL-STD-2525 tactical symbol's icon. See MIL-STD-2525 section 5.4.5 (page 24) and
     * table IX (page 35). When used as a key, the corresponding value must be a boolean value. The value is
     * <code>true</code> if the symbol's icon should be displayed, and <code>false</code> otherwise.
     */
    String SHOW_ICON = "gov.nasa.worldwind.symbology.ShowIcon";

    /**
     * @deprecated Use {@link TacticalSymbol#setShowLocation(boolean)} to control the visibility of the location
     * modifier.
     */
    @Deprecated
    String SHOW_LOCATION = "gov.nasa.worldwind.symbology.ShowLocation";

    /**
     * The MIL-STD-2525 Signature Equipment modifier field ID. Indicates detectable electronic signatures from hostile
     * equipment. See MIL-STD-2525 section 5.3.4.10 (page 29), table IV (pages 22-24) and table XIV (pages 46-47). When
     * used as a key, the corresponding value must be the string "!".
     */
    String SIGNATURE_EQUIPMENT = "L";

    /**
     * The MIL-STD-2525 Special C2 Headquarters modifier field ID. Indicates a the name of a special Command and Control
     * Headquarters. The name is displayed inside the symbol's frame. See MIL-STD-2525 section 5.3.4.10 (page 29), table
     * IV (pages 22-24) and table XIV (pages 46-47). When used as a key, the corresponding value must be a string
     * containing up to 9 characters.
     */
    String SPECIAL_C2_HEADQUARTERS = "AA";

    /**
     * The MIL-STD-2525 Speed modifier field ID. Indicates a symbol's velocity as defined in MIL-STD-6040. See
     * MIL-STD-2525 section 5.3.4.10 (page 29), table IV (pages 22-24) and table XIV (pages 46-47). When used as a key,
     * the corresponding value must be a string containing up to 8 characters.
     */
    String SPEED = "Z";

    /**
     * The MIL-STD-2525 Speed Leader modifier field ID. Indicates the speed and direction of movement or intended
     * movement of an object. See MIL-STD-2525 section 5.3.4.11.3 (page 30-31), table IV (pages 22-24) and table XIV
     * (pages 46-47). When used as a key, the corresponding value must be a numeric value indicating an amount to scale
     * the Direction of Movement modifier line as a ratio of the line's original length. The specified scale is a
     * floating point number greater than 0.0: values less than 1.0 make the line shorter, while values greater than 1.0
     * make the line longer.
     */
    String SPEED_LEADER_SCALE = "AJ";

    /**
     * The MIL-STD-2525 Staff Comments modifier field ID. The meaning of this field is implementation specific. See
     * MIL-STD-2525 section 5.3.4.10 (page 29), table IV (pages 22-24) and table XIV (pages 46-47). When used as a key,
     * the corresponding value must be a string containing up to 20 characters.
     */
    String STAFF_COMMENTS = "G";

    /**
     * Indicates the standard identity code associated with a MIL-STD-2525 symbol (SIDC position 2). A symbol's standard
     * identity defines the threat posed by the object being represented. See MIL-STD-2525C section 3.2.39 (page 10),
     * section 5.3.1.1 (page 17), table I (page 15), and table II (page 16). When used as a key, the corresponding value
     * must be one of the following:
     * <ul> <li>STANDARD_IDENTITY_PENDING</li> <li>STANDARD_IDENTITY_UNKNOWN</li> <li>STANDARD_IDENTITY_ASSUMED_FRIEND</li>
     * <li>STANDARD_IDENTITY_FRIEND</li> <li>STANDARD_IDENTITY_NEUTRAL</li> <li>STANDARD_IDENTITY_SUSPECT</li>
     * <li>STANDARD_IDENTITY_HOSTILE</li> <li>STANDARD_IDENTITY_EXERCISE_PENDING</li>
     * <li>STANDARD_IDENTITY_EXERCISE_UNKNOWN</li> <li>STANDARD_IDENTITY_EXERCISE_ASSUMED_FRIEND</li>
     * <li>STANDARD_IDENTITY_EXERCISE_FRIEND</li> <li>STANDARD_IDENTITY_EXERCISE_NEUTRAL</li>
     * <li>STANDARD_IDENTITY_JOKER</li> <li>STANDARD_IDENTITY_FAKER</li> </ul>
     * <p>
     * The standard identity codes are the same for all symbology schemes that use them, and are defined in each
     * appendix of the MIL-STD-2525 specification:
     * <ul> <li>Warfighting - table A-I (page 51)</li> <li>Tactical Graphics - table B-I (page 305)</li> <li>Signals
     * Intelligence - table D-I (page 964)</li> <li>Stability Operations - table E-I (page 991)</li> <li>Emergency
     * Management - table G-I (page 1032)</li> </ul>
     */
    String STANDARD_IDENTITY = "gov.nasa.worldwind.symbology.StandardIdentity";
    /**
     * The MIL-STD-2525 Assumed Friend standard identity code. Indicates a symbol assumed to be a friend because of its
     * characteristics or origin. See MIL-STD-2525C section 3.2.2 (page 7).
     */
    String STANDARD_IDENTITY_ASSUMED_FRIEND = "A";
    /**
     * The MIL-STD-2525 Exercise Assumed Friend standard identity code. Indicates a symbol acting as an assumed friend
     * for exercise purposes.
     */
    String STANDARD_IDENTITY_EXERCISE_ASSUMED_FRIEND = "M";
    /**
     * The MIL-STD-2525 Exercise Friend standard identity code. Indicates a symbol acting as a friend for exercise
     * purposes.
     */
    String STANDARD_IDENTITY_EXERCISE_FRIEND = "D";
    /**
     * The MIL-STD-2525 Exercise Neutral standard identity code. Indicates a symbol acting as neutral for exercise
     * purposes.
     */
    String STANDARD_IDENTITY_EXERCISE_NEUTRAL = "L";
    /**
     * The MIL-STD-2525 Exercise Pending standard identity code. Indicates a symbol which has not been subject to the
     * identification process for exercise purposes.
     */
    String STANDARD_IDENTITY_EXERCISE_PENDING = "G";
    /**
     * The MIL-STD-2525 Exercise Unknown standard identity code. Indicates a symbol that as been evaluated but not
     * identified  for exercise purposes.
     */
    String STANDARD_IDENTITY_EXERCISE_UNKNOWN = "W";
    /**
     * The MIL-STD-2525 Faker standard identity code.  Indicates a friendly symbol acting as hostile for exercise
     * purposes. See MIL-STD-2525C section 3.2.12 (page 8).
     */
    String STANDARD_IDENTITY_FAKER = "K";
    /**
     * The MIL-STD-2525 Friend standard identity code. Indicates a symbol belonging to a declared friendly nation. See
     * MIL-STD-2525C section 3.2.16 (page 8).
     */
    String STANDARD_IDENTITY_FRIEND = "F";
    /**
     * The MIL-STD-2525 Hostile standard identity code. Indicates a symbol belonging to any opposing nation or entity.
     * See MIL-STD-2525C section 3.2.19 (page 8).
     */
    String STANDARD_IDENTITY_HOSTILE = "H";
    /**
     * The MIL-STD-2525 Joker standard identity code. Indicates a friendly symbol acting as suspect for exercise
     * purposes. See MIL-STD-2525C section 3.2.24 (page 8).
     */
    String STANDARD_IDENTITY_JOKER = "J";
    /**
     * The MIL-STD-2525 Neutral standard identity code. Indicates a symbol who's characteristics or nationality
     * indicates that it is neither supporting nor opposing friendly forces. See MIL-STD-2525C section 3.2.29 (page 9).
     */
    String STANDARD_IDENTITY_NEUTRAL = "N";
    /**
     * The MIL-STD-2525 Pending standard identity code. Indicates a symbol which has not been subject to the
     * identification process. See MIL-STD-2525C section 3.2.32 (page 9).
     */
    String STANDARD_IDENTITY_PENDING = "P";
    /**
     * The MIL-STD-2525 Suspect standard identity code. Indicates a symbol which is potentially hostile because of its
     * characteristics or nationality. See MIL-STD-2525C section 3.2.42 (page 10).
     */
    String STANDARD_IDENTITY_SUSPECT = "S";
    /**
     * The MIL-STD-2525 Unknown standard identity code. Indicates a symbol that as been evaluated but not identified.
     * See MIL-STD-2525C section 3.2.49 (page 10).
     */
    String STANDARD_IDENTITY_UNKNOWN = "U";
    /**
     * List containing all recognized MIL-STD-2525 standard identity codes.
     */
    List<String> STANDARD_IDENTITY_ALL = Arrays.asList(
        SymbologyConstants.STANDARD_IDENTITY_PENDING,
        SymbologyConstants.STANDARD_IDENTITY_UNKNOWN,
        SymbologyConstants.STANDARD_IDENTITY_FRIEND,
        SymbologyConstants.STANDARD_IDENTITY_NEUTRAL,
        SymbologyConstants.STANDARD_IDENTITY_HOSTILE,
        SymbologyConstants.STANDARD_IDENTITY_ASSUMED_FRIEND,
        SymbologyConstants.STANDARD_IDENTITY_SUSPECT,
        SymbologyConstants.STANDARD_IDENTITY_EXERCISE_PENDING,
        SymbologyConstants.STANDARD_IDENTITY_EXERCISE_UNKNOWN,
        SymbologyConstants.STANDARD_IDENTITY_EXERCISE_FRIEND,
        SymbologyConstants.STANDARD_IDENTITY_EXERCISE_NEUTRAL,
        SymbologyConstants.STANDARD_IDENTITY_EXERCISE_ASSUMED_FRIEND,
        SymbologyConstants.STANDARD_IDENTITY_JOKER,
        SymbologyConstants.STANDARD_IDENTITY_FAKER
    );

    /**
     * Indicates if a graphic in the Meteorological and Oceanographic scheme is static or dynamic (SIDC positions 3 and
     * 4). When used as a key, the corresponding value must be one of the following:
     * <ul> <li>STATIC</li> <li>DYNAMIC</li> </ul>
     */
    String STATIC_DYNAMIC = "gov.nasa.worldwind.symbology.StaticDynamic";
    /**
     * The MIL-STD-2525 Static, used by symbols belonging to the METOC scheme.
     */
    String STATIC = "S-";
    /**
     * The MIL-STD-2525 Dynamic, used by symbols belonging to the METOC scheme.
     */
    String DYNAMIC = "-D";
    List<String> STATIC_DYNAMIC_ALL = Arrays.asList(
        SymbologyConstants.STATIC,
        SymbologyConstants.DYNAMIC
    );

    /**
     * The MIL-STD-2525 Status / Operational Condition modifier field ID. Indicates the status code associated with a
     * MIL-STD-2525 symbol (SIDC position 4). A symbol's status defines whether the represented object exists at the
     * time the symbol was generated, or is anticipated to exist in the future. Additionally, a symbol's status can
     * define its operational condition. See MIL-STD-2525C section 3.2.41 (page 10), section 5.3.1.4 (pages 17-18), and
     * tables III and III-2 (pages 18-17). The recognized values depend on the specific MIL-STD-2525 symbology scheme
     * the symbol belongs to, and are defined in each appendix of the MIL-STD-2525C specification:
     * <p>
     * <strong>Warfighting, Signals Intelligence, Stability Operations</strong> <br> See MIL-STD-2525C section
     * A.5.2.1.d (page 51), table A-I (page 51), section D.5.2.1.d (page 964), table D-I (page 964), section E.5.2.1.d
     * (page 991), and table E-I (page 991).
     * <ul> <li>STATUS_ANTICIPATED</li> <li>STATUS_PRESENT</li> <li>STATUS_PRESENT_FULLY_CAPABLE</li>
     * <li>STATUS_PRESENT_DAMAGED</li> <li>STATUS_PRESENT_DESTROYED</li> <li>STATUS_PRESENT_FULL_TO_CAPACITY</li> </ul>
     * <p>
     * <strong>Tactical Graphics</strong> <br> See MIL-STD-2525C section B5.2.1.d (page 304) and table B-I (page 305).
     * <ul> <li>STATUS_ANTICIPATED</li> <li>STATUS_SUSPECTED</li> <li>STATUS_PRESENT</li> <li>STATUS_KNOWN</li> </ul>
     * <p>
     * <strong>Emergency Management</strong> <br> See MIL-STD-2525C section G.5.2.4 (page 1028) and table G-I (page
     * 1032).
     * <ul> <li>STATUS_ANTICIPATED</li> <li>STATUS_PRESENT</li> </ul>
     */
    String STATUS = "AL";
    /**
     * The MIL-STD-2525 Anticipated/Planned status code. Indicates a symbol who's represented object is anticipated to
     * exist at the symbol's location. See MIL-STD-2525C section 5.3.1.4 (pages 17-18).
     */
    String STATUS_ANTICIPATED = "A";
    /**
     * The MIL-STD-2525 Known status code. See MIL-STD-2525C table B-I (page 305).
     */
    String STATUS_KNOWN = "K";
    /**
     * The MIL-STD-2525 Present/Fully Capable status code. Indicates a symbol who's represented object currently exists
     * at the symbol's location. See MIL-STD-2525C section 5.3.1.4 (pages 17-18).
     */
    String STATUS_PRESENT = "P";
    /**
     * The MIL-STD-2525 Present/Damaged status code. Indicates a symbol who's represented object is damaged, and
     * currently exists at the symbol's location. See MIL-STD-2525C section 5.3.1.4 (pages 17-18).
     */
    String STATUS_DAMAGED = "D";
    /**
     * The MIL-STD-2525 Present/Destroyed status code. Indicates a symbol who's represented object is destroyed, and
     * currently exists at the symbol's location. See MIL-STD-2525C section 5.3.1.4 (pages 17-18).
     */
    String STATUS_DESTROYED = "X";
    /**
     * The MIL-STD-2525 Present/Full To Capacity status code. Indicates a symbol who's represented object's capacity is
     * full, and currently exists at the symbol's location. See MIL-STD-2525C section 5.3.1.4 (pages 17-18).
     */
    String STATUS_FULL_TO_CAPACITY = "F";
    /**
     * The MIL-STD-2525 Present/Fully Capable status code. Indicates a symbol who's represented object is fully capable,
     * and currently exists at the symbol's location. See MIL-STD-2525C section 5.3.1.4 (pages 17-18).
     */
    String STATUS_FULLY_CAPABLE = "C";
    /**
     * The MIL-STD-2525 Suspected status code. Indicates a symbol who's represented object is suspected to exist at the
     * symbol's location. See MIL-STD-2525C section 5.3.1.4 (pages 17-18).
     */
    String STATUS_SUSPECTED = "S";
    /**
     * List containing all recognized MIL-STD-2525 status codes.
     */
    List<String> STATUS_ALL = Arrays.asList(
        // UEI, SIGINT, SO, and EM status codes.
        SymbologyConstants.STATUS_ANTICIPATED,
        SymbologyConstants.STATUS_PRESENT,
        SymbologyConstants.STATUS_FULLY_CAPABLE,
        SymbologyConstants.STATUS_DAMAGED,
        SymbologyConstants.STATUS_DESTROYED,
        SymbologyConstants.STATUS_FULL_TO_CAPACITY,
        // Tactical Graphics and METOC status codes (ANTICIPATED and PRESENT already included).
        SymbologyConstants.STATUS_SUSPECTED,
        SymbologyConstants.STATUS_KNOWN
    );
    /**
     * List containing all recognized MIL-STD-2525 status codes for the Warfighting (UEI), Signals Intelligence
     * (SIGINT), Stability Operations (SO), and Emergency Management schemes. TODO: EM scheme contradicts itself.
     */
    List<String> STATUS_ALL_UEI_SIGINT_SO_EM = Arrays.asList(
        SymbologyConstants.STATUS_ANTICIPATED,
        SymbologyConstants.STATUS_PRESENT,
        SymbologyConstants.STATUS_FULLY_CAPABLE,
        SymbologyConstants.STATUS_DAMAGED,
        SymbologyConstants.STATUS_DESTROYED,
        SymbologyConstants.STATUS_FULL_TO_CAPACITY
    );
    /**
     * List containing all recognized MIL-STD-2525 status codes for the Tactical Graphics and Meteorological and
     * Oceanographic (METOC) scheme.
     */
    List<String> STATUS_ALL_TACTICAL_GRAPHICS_METOC = Arrays.asList(
        SymbologyConstants.STATUS_ANTICIPATED,
        SymbologyConstants.STATUS_SUSPECTED,
        SymbologyConstants.STATUS_PRESENT,
        SymbologyConstants.STATUS_KNOWN
    );

    /**
     * Indicates the symbol modifier code associated with a MIL-STD-2525 symbol (SIDC positions 11-12). The symbol
     * modifier code defines what graphic symbol modifiers should be displayed around the symbol's icon, such as
     * feint/dummy, installation, task force, headquarters staff, equipment mobility, and auxiliary equipment. The
     * recognized values depend on the specific MIL-STD-2525 symbology scheme the symbol belongs to, and are defined in
     * each appendix of the MIL-STD-2525C specification:
     * <ul> <li>Warfighting - section A.5.2.1.f (page 51) and table A-II (pages 52-54)</li> <li>Stability Operations -
     * section E.5.2.1.f (page 991) and table E-II (pages 992-994)</li> <li>Emergency Management - section G.5.5 (page
     * 1029) and table EG-II (page 1032)</li> </ul>
     */
    String SYMBOL_MODIFIER = "gov.nasa.worldwind.symbology.SymbolModifier";

    /**
     * The MIL-STD-2525 Symbol Indicator modifier field ID. Indicates a symbol to included in a TacticalGraphic. See
     * MIL-STD-2525 section 5.5.2 (page 38), table XI (pages 38-39) and table XIV (pages 46-47). When used as a key, the
     * corresponding value must be an {@link TacticalSymbol}.
     */
    String SYMBOL_INDICATOR = "A";

    /**
     * The MIL-STD-2525 Task Force Indicator modifier field ID. Indicates whether a MIL-STD-2525 symbol's represented
     * object is a task force. When a marked as a task force, a symbol's graphic is changed to include a bracket above
     * its echelon. See MIL-STD-2525 section 5.3.4.6 (page 28). When used as a key, the corresponding value must be a
     * boolean value. The value is <code>true</code> if the symbol's represented object is a task force, and
     * <code>false</code> otherwise.
     * <p>
     * The following symbology schemes support the task force modifier:
     * <ul> <li>Warfighting</li> <li>Stability Operations</li> </ul>
     */
    String TASK_FORCE = "D";

    /**
     * The MIL-STD-2525 Type modifier field ID. Indicates types of equipment. See MIL-STD-2525 section 5.3.4.1 (page
     * 25), table IV (pages 22-24) and table XIV (pages 46-47). When used as a key, the corresponding value must be a
     * string containing up to 24 characters.
     */
    String TYPE = "V";

    /**
     * The MIL-STD-2525 Unique Designation modifier field ID. Uniquely identifies a particular symbol or track number.
     * Identifies acquisitions number when used with SIGINT symbology. See MIL-STD-2525 section 5.3.4.10 (page 29),
     * table IV (pages 22-24) and table XIV (pages 46-47). When used as a key, the corresponding value must be a string
     * containing up to 21 characters.
     */
    String UNIQUE_DESIGNATION = "T";
}
