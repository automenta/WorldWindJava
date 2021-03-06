/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */

package gov.nasa.worldwind.symbology.milstd2525;

import gov.nasa.worldwind.Keys;
import gov.nasa.worldwind.avlist.KV;
import gov.nasa.worldwind.exception.WWRuntimeException;
import gov.nasa.worldwind.symbology.*;
import gov.nasa.worldwind.util.Logging;

import java.awt.*;
import java.awt.image.*;

/**
 * Icon retriever to retriever modifiers (for example, the Feint/Dummy indicator) for the symbols in the MIL-STD-2525C
 * symbol set.
 *
 * @author dcollins
 * @version $Id: MilStd2525ModifierRetriever.java 1171 2013-02-11 21:45:02Z dcollins $
 */
public class MilStd2525ModifierRetriever extends AbstractIconRetriever {
    protected static final Color DEFAULT_COLOR = Color.BLACK;

    protected static final String PATH_PREFIX = "modifiers";
    protected static final String PATH_SUFFIX = ".png";
    protected static final int[] variableWidths = {88, 93, 114, 119};

    /**
     * Create a new retriever that will retrieve icons from the specified location.
     *
     * @param retrieverPath File path or URL to the symbol directory, for example "http://myserver.com/milstd2525/".
     */
    public MilStd2525ModifierRetriever(String retrieverPath) {
        super(retrieverPath);
    }

    protected static String composePath(String symbolModifierCode, KV params) {
        KV modifierParams = SymbolCode.parseSymbolModifierCode(symbolModifierCode, null);
        if (modifierParams == null)
            return null;

        if (params != null)
            modifierParams.setValues(params);

        StringBuilder sb = new StringBuilder();
        sb.append(MilStd2525ModifierRetriever.PATH_PREFIX).append('/');
        sb.append(symbolModifierCode.toLowerCase());

        if (MilStd2525ModifierRetriever.isVariableWidth(modifierParams)) {
            Integer i = MilStd2525ModifierRetriever.chooseBestFittingWidth(modifierParams);
            if (i != null)
                sb.append('_').append(i);
        }

        sb.append(MilStd2525ModifierRetriever.PATH_SUFFIX);
        return sb.toString();
    }

    protected static boolean isVariableWidth(KV params) {
        return params.hasKey(SymbologyConstants.FEINT_DUMMY)
            || params.hasKey(SymbologyConstants.OPERATIONAL_CONDITION_ALTERNATE);
    }

    /**
     * Indicates whether or not color must be applied to the modifier. Color is applied to all modifiers except for
     * alternate Operational Condition modifiers, which have their own color.
     *
     * @param symbolId Modifier id.
     * @return True if color must be applied to the modifier.
     */
    protected static boolean mustApplyColor(String symbolId) {
        return !SymbologyConstants.OPERATIONAL_CONDITION_ALTERNATE_ALL.contains(symbolId.toUpperCase());
    }

    protected static Integer chooseBestFittingWidth(KV params) {
        Object o = params.get(Keys.WIDTH);
        if (!(o instanceof Number))
            return null;

        int value = ((Number) o).intValue();
        int width = MilStd2525ModifierRetriever.variableWidths[0];
        int minDiff = Math.abs(value - width);

        for (int i = 1; i < MilStd2525ModifierRetriever.variableWidths.length; i++) {
            int diff = Math.abs(value - MilStd2525ModifierRetriever.variableWidths[i]);
            if (diff < minDiff) {
                width = MilStd2525ModifierRetriever.variableWidths[i];
                minDiff = diff;
            }
        }

        return width;
    }

    /**
     * Retrieves the value of the AVKey.COLOR parameter.
     *
     * @param params Parameter list.
     * @return The value of the AVKey.COLOR parameter, if such a parameter exists and is of type java.awt.Color. Returns
     * null if the parameter list is null, if there is no value for key AVKey.COLOR, or if the value is not a Color.
     */
    protected static Color getColorFromParams(KV params) {
        if (params == null)
            return null;

        Object o = params.get(Keys.COLOR);
        return (o instanceof Color) ? (Color) o : null;
    }

    /**
     * Create an icon for a symbol modifier.
     *
     * @param symbolId Identifier for the modifier. This identifier is fields 11 and 12 of a MIL-STD-2525C SIDC (see
     *                 MIL-STD-2525C Table A-I, pg. 51).
     * @param params   Parameters that affect icon retrieval. This retriever accepts only one parameter: AVKey.COLOR,
     *                 which determines the color of the modifier (default is black).
     * @return BufferedImage containing the requested modifier, or null if the modifier cannot be retrieved.
     */
    public BufferedImage createIcon(String symbolId, KV params) {
        if (symbolId == null) {
            String msg = Logging.getMessage("nullValue.SymbolCodeIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        // Compose a path from the modifier code and value.
        String path = MilStd2525ModifierRetriever.composePath(symbolId, params);
        if (path == null) {
            String msg = Logging.getMessage("Symbology.SymbolIconNotFound", symbolId);
            Logging.logger().severe(msg);
            throw new WWRuntimeException(msg);
        }

        BufferedImage image = this.readImage(path);
        if (image == null) {
            String msg = Logging.getMessage("Symbology.SymbolIconNotFound", symbolId);
            Logging.logger().severe(msg);
            throw new WWRuntimeException(msg);
        }

        if (MilStd2525ModifierRetriever.mustApplyColor(symbolId)) {
            // Apply the correct color the modifier.
            Color color = MilStd2525ModifierRetriever.getColorFromParams(params);
            if (color == null)
                color = MilStd2525ModifierRetriever.DEFAULT_COLOR;
            AbstractIconRetriever.multiply(image, color);
        }

        return image;
    }
}