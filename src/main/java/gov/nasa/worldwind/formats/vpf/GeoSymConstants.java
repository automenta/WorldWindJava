/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.formats.vpf;

/**
 * @author dcollins
 * @version $Id: GeoSymConstants.java 1171 2013-02-11 21:45:02Z dcollins $
 */
public interface GeoSymConstants {
    // Column Types
    // MIL-DTL-89045 3.5.3.1
    String INTEGER = "N";          // integer
    String CHARACTER_STRING = "T"; // character string

    // Reserved path names.
    String ASCII = "ascii";
    String BINARY = "bin";
    String CLEAR_TEXT = "ctext";
    String GEOSYM = "geosym";
    String GRAPHICS = "graphics";
    String SYMBOLOGY_ASSIGNMENT = "symasgn";

    // Reserved attribute file names.
    String ATTRIBUTE_EXPRESSION_FILE = "attexp.txt";
    String CODE_VALUE_DESCRIPTION_FILE = "code.txt";
    String COLOR_ASSIGNMENT_FILE = "color.txt";
    String FULL_SYMBOL_ASSIGNMENT_FILE = "fullsym.txt";
    String SIMPLIFIED_SYMBOL_ASSIGNMENT_FILE = "simpsym.txt";
    String TEXT_ABBREVIATIONS_ASSIGNMENT_FILE = "textabbr.txt";
    String TEXT_LABEL_CHARACTERISTICS_FILE = "textchar.txt";
    String TEXT_LABEL_JOIN_FILE = "textjoin.txt";
    String TEXT_LABEL_LOCATION_FILE = "textloc.txt";
    String LINE_AREA_ATTRIBUTES_FILE = "geosym-line-area-attr.csv";
}
