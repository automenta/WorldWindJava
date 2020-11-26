/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.formats.vpf;

/**
 * @author dcollins
 * @version $Id: VPFConstants.java 1171 2013-02-11 21:45:02Z dcollins $
 */
public interface VPFConstants {
    // Column Types
    // DIGEST Part 2, Annex C, Table C-11
    String TEXT = "T";           // Text (US ASCII)
    String TEXT_L1 = "L";        // Level 1 (Latin 1 - ISO 8859) text
    String TEXT_L2 = "N";        // Level 2 (obsolete - retained for backward compatibility)
    String TEXT_L3 = "M";        // Level 3 (Multilingual - ISO 10646) text
    String SHORT_FLOAT = "F";    // Short floating point (4 bytes)
    String LONG_FLOAT = "R";     // Long floating point (8 bytes)
    String SHORT_INT = "S";      // Short integer (2 bytes)
    String LONG_INT = "I";       // Long integer (4 bytes)
    String SHORT_COORD_2F = "C"; // 2-coordinate - short floating point
    String LONG_COORD_2F = "B";  // 2-coordinate - long floating point
    String SHORT_COORD_3F = "Z"; // 3-coordinate - short floating point
    String LONG_COORD_3F = "Y";  // 3-coordinate - long floating point
    String DATE_AND_TIME = "D";  // Date and time
    String NULL = "X";           // Null field
    String TRIPLET_ID = "K";     // Triplet id
    String SHORT_COORD_2I = "G"; // 2-coordinate array - short integer
    String LONG_COORD_2I = "H";  // 2-coordinate array - long integer
    String SHORT_COORD_3I = "V"; // 3-coordinate array - short integer
    String LONG_COORD_3I = "W";  // 3-coordinate array - long integer

    // Column Names
    String ID = "id";

    // Key Types
    // DIGEST Part 2, Annex C, Table C-12
    String PRIMARY_KEY = "P";
    String UNIQUE_KEY = "U";
    String NON_UNIQUE_KEY = "N";

    // Reserved File Names
    // DIGEST Part 2, Annex C, Table C-14
    String COVERAGE_ATTRIBUTE_TABLE = "cat";
    String CONNECTED_NODE_PRIMITIVE_TABLE = "cnd";
    String CONNECTED_NODE_SPATIAL_INDEX = "csi";
    String DATABASE_HEADER_TABLE = "dht";
    String DATA_QUALITY_TABLE = "dqt";
    String EDGE_BOUNDING_RECTANGLE_TABLE = "ebr";
    String EDGE_PRIMITIVE_TABLE = "edg";
    String ENTITY_NODE_PRIMITIVE_TABLE = "end";
    String EDGE_SPATIAL_INDEX = "esi";
    String FACE_PRIMITIVE_TABLE = "fac";
    String FACE_BOUNDING_RECTANGLE_TABLE = "fbr";
    String FEATURE_CLASS_ATTRIBUTE_TABLE = "fca";
    String FEATURE_CLASS_SCHEMA_TABLE = "fcs";
    String FACE_SPATIAL_INDEX = "fsi";
    String GEOGRAPHIC_REFERENCE_TABLE = "grt";
    String LIBRARY_ATTRIBUTE_TABLE = "lat";
    String LIBRARY_HEADER_TABLE = "lht";
    String NODE_PRIMITIVE_TABLE = "nod";
    String NODE_OR_ENTITY_NODE_SPATIAL_INDEX = "nsi";
    String PRIMITIVE_EXPANSION_SCHEMA_TABLE = "pes";
    String RING_TABLE = "rng";
    String TEXT_PRIMITIVE_TABLE = "txt";
    String TEXT_SPATIAL_INDEX = "tsi";
    String CHARACTER_VALUE_DESCRIPTION_TABLE = "char.vdt";
    String INTEGER_VALUE_DESCRIPTION_TABLE = "int.vdt";

    // Reserved Directory Names
    // DIGEST Part 2, Annex C, Table C-15
    String LIBRARY_REFERENCE_COVERAGE = "libref";
    String DATA_QUALITY_COVERAGE = "dq";
    String TILE_REFERENCE_COVERAGE = "tileref";
    String NAMES_REFERENCE_COVERAGE = "gazette";

    // Reserved Table Name Extensions
    // DIGEST Part 2, Annex C, Table C-16
    String AREA_BOUNDING_RECTANGLE_TABLE = ".abr";
    String AREA_FEATURE_TABLE = ".aft";
    String AREA_JOIN_TABLE = ".ajt";
    String AREA_THEMATIC_INDEX = ".ati";
    String COMPLEX_BOUNDING_RECTANGLE_TABLE = ".cbr";
    String COMPLEX_FEATURE_TABLE = ".cft";
    String COMPLEX_JOIN_TABLE = ".cjt";
    String COMPLEX_THEMATIC_INDEX = ".cti";
    String NARRATIVE_TABLE = ".doc";
    String DIAGNOSTIC_POINT_TABLE = ".dpt";
    String FEATURE_INDEX_TABLE = ".fit";
    String FEATURE_RELATIONS_JOIN_TABLE = ".fjt";
    String FEATURE_INDEX_TABLE_THEMATIC_INDEX = ".fti";
    String JOIN_THEMATIC_INDEX = ".jti";
    String LINE_BOUNDING_RECTANGLE_TABLE = ".lbr";
    String LINE_FEATURE_TABLE = ".lft";
    String LINE_JOIN_TABLE = ".ljt";
    String LINE_THEMATIC_INDEX = ".lti";
    String POINT_BOUNDING_RECTANGLE_TABLE = ".pbr";
    String POINT_FEATURE_TABLE = ".pft";
    String POINT_JOIN_TABLE = ".pjt";
    String POINT_THEMATIC_INDEX = ".pti";
    String RELATED_ATTRIBUTE_TABLE = ".rat";
    String REGISTRATION_POINT_TABLE = ".rpt";
    String TEXT_BOUNDING_RECTANGLE_TABLE = ".tbr";
    String TEXT_FEATURE_TABLE = ".tft";
    String TEXT_FEATURE_JOIN_TABLE = ".tjt";
    String TEXT_THEMATIC_INDEX = ".tti";

    // Feature Types
    // DIGEST Part 2, Annex C, Table C-65
    String POINT_FEATURE_TYPE = "P";
    String LINE_FEATURE_TYPE = "L";
    String AREA_FEATURE_TYPE = "A";
    String TEXT_FEATURE_TYPE = "T";
    String COMPLEX_FEATURE_TYPE = "C";

    // Feature Tables
    // DIGEST Part 2, Annex C.2.3.3.1
    String TEXT_FEATURE_COLUMN = ".txt_id";

    // Reserved Feature Table Names
    // DIGEST Part 2, Annex C.2.3.5.4.1
    String TILE_REFERENCE_AREA_FEATURE = "tileref.aft";
}
