/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */

package gov.nasa.worldwind.examples.milstd2525.graphics.areas;

import gov.nasa.worldwind.examples.milstd2525.graphics.TacGrpSidc;
import gov.nasa.worldwind.examples.render.ShapeAttributes;

import java.util.*;

/**
 * An area that is filled with a pattern of diagonal lines.
 *
 * @author pabercrombie
 * @version $Id: FilledArea.java 545 2012-04-24 22:29:21Z pabercrombie $
 */
public class FilledArea extends BasicArea {
    /**
     * Path to the image used for the polygon fill pattern.
     */
    protected static final String DIAGONAL_FILL_PATH = "images/diagonal-fill-16x16.png";

    /**
     * Create a new filled area graphic.
     *
     * @param sidc Symbol code that identifies the graphic to create.
     */
    public FilledArea(String sidc) {
        super(sidc);
    }

    /**
     * Indicates the graphics supported by this class.
     *
     * @return List of masked SIDC strings that identify graphics that this class supports.
     */
    public static List<String> getSupportedGraphics() {
        return Arrays.asList(
            TacGrpSidc.MOBSU_CBRN_RADA,
            TacGrpSidc.MOBSU_CBRN_BIOCA,
            TacGrpSidc.MOBSU_CBRN_CMLCA);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void applyDefaultAttributes(ShapeAttributes attributes) {
        super.applyDefaultAttributes(attributes);

        // Enable the polygon interior and set the image source to draw a fill pattern of diagonal lines.
        attributes.setDrawInterior(true);
        attributes.setImageSource(this.getImageSource());
    }

    /**
     * Indicates the source of the image that provides the polygon fill pattern.
     *
     * @return The source of the polygon fill pattern.
     */
    protected Object getImageSource() {
        return DIAGONAL_FILL_PATH;
    }
}