/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */

package gov.nasa.worldwind.symbology.milstd2525.graphics.areas;

import gov.nasa.worldwind.Keys;
import gov.nasa.worldwind.render.ShapeAttributes;
import gov.nasa.worldwind.symbology.TacticalGraphicLabel;
import gov.nasa.worldwind.symbology.milstd2525.graphics.TacGrpSidc;

import java.util.*;

/**
 * Implementation of the Weapons Free Zone graphic (2.X.2.2.3.5).
 *
 * @author pabercrombie
 * @version $Id: WeaponsFreeZone.java 1171 2013-02-11 21:45:02Z dcollins $
 */
public class WeaponsFreeZone extends AviationZone {
    /**
     * Path to the image used for the polygon fill pattern.
     */
    protected static final String DIAGONAL_FILL_PATH = "images/diagonal-fill-16x16.png";

    public WeaponsFreeZone(String sidc) {
        super(sidc);
    }

    /**
     * Indicates the graphics supported by this class.
     *
     * @return List of masked SIDC strings that identify graphics that this class supports.
     */
    public static List<String> getSupportedGraphics() {
        return Collections.singletonList(TacGrpSidc.C2GM_AVN_ARS_WFZ);
    }

    /**
     * Indicates the source of the image that provides the polygon fill pattern.
     *
     * @return The source of the polygon fill pattern.
     */
    protected static Object getImageSource() {
        return WeaponsFreeZone.DIAGONAL_FILL_PATH;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getGraphicLabel() {
        return "WFZ";
    }

    @Override
    protected void createLabels() {
        TacticalGraphicLabel label = this.addLabel(this.createLabelText());
        label.setTextAlign(Keys.LEFT);
        label.setEffect(Keys.TEXT_EFFECT_NONE);
        label.setDrawInterior(true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void applyDefaultAttributes(ShapeAttributes attributes) {
        super.applyDefaultAttributes(attributes);

        // Enable the polygon interior and set the image source to draw a fill pattern of diagonal lines.
        attributes.setDrawInterior(true);
        attributes.setImageSource(WeaponsFreeZone.getImageSource());
    }

    /**
     * {@inheritDoc} Overridden to not include the altitude modifier in the label. This graphic does not support the
     * altitude modifier.
     */
    @Override
    protected String createLabelText() {
        return this.doCreateLabelText(false);
    }
}