/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */

package gov.nasa.worldwind.symbology;

import gov.nasa.worldwind.avlist.KV;
import gov.nasa.worldwind.geom.Position;

/**
 * A factory to create {@link TacticalGraphic}s. Each implementation of this interface handles the graphics for a
 * specific symbol set. Each graphic within that set is identified by a string identifier.
 * <p>
 * The factory exposes creation several methods:
 * <ul><li>{@link #createGraphic(String, Iterable, KV) createGraphic} - Creates a graphic
 * from a list of positions and modifiers. This method is the most general, and can create any type of graphic. The
 * other creation methods are provided for convenience.</li> <li>{@link #createPoint(String,
 * Position, KV) createPoint} - Create a graphic positioned by a
 * single control point.</li> <li>{@link #createCircle(String, Position, double,
 * KV) createCircle} - Create a graphic positioned by a center point and a radius.</li>
 * <li>{@link #createQuad(String, Iterable, KV) createQuad} - Create a graphic that has
 * length and width properties.</li> <li>{@link #createRoute(String, Iterable, KV)
 * createRoute} - Create a graphic composed of point graphics connected by lines.</li> </ul>
 *
 * @author pabercrombie
 * @version $Id: TacticalGraphicFactory.java 1171 2013-02-11 21:45:02Z dcollins $
 * @see TacticalGraphic
 */
public interface TacticalGraphicFactory {
    /**
     * Create a tactical graphic positioned by more than one control point. This method is general purpose, and may be
     * used to create any type of graphic. The other creation methods in the factory (for example, {@link
     * #createCircle(String, Position, double, KV) createCircle}) are provided for convenience, and may be used to
     * specific categories of graphics.
     *
     * @param symbolIdentifier Identifier for the symbol within its symbol set.
     * @param positions        Control points to use to place the graphic. How many points are required depends on the
     *                         type of graphic.
     * @param modifiers        Modifiers to apply to the graphic.
     * @return A new TacticalGraphic configured to render at the position indicated, or {@code null} if no graphic can
     * be created for the given symbol identifier.
     */
    TacticalGraphic createGraphic(String symbolIdentifier, Iterable<? extends Position> positions, KV modifiers);

    /**
     * Create a tactical graphic positioned by a single control point.
     *
     * @param symbolIdentifier Identifier for the symbol within its symbol set.
     * @param position         Control point to use to place the graphic.
     * @param modifiers        Modifiers to apply to the graphic.
     * @return A new TacticalGraphic configured to render at the position indicated, or {@code null} if no graphic can
     * be created for the given symbol identifier.
     */
    TacticalPoint createPoint(String symbolIdentifier, Position position, KV modifiers);

    /**
     * Create a circular graphic.
     *
     * @param symbolIdentifier Identifier for the symbol within its symbol set.
     * @param center           The position of the center of the circle.
     * @param radius           The radius of the circle, in meters.
     * @param modifiers        Modifiers to apply to the graphic.
     * @return A new graphic configured to render at the position indicated, or {@code null} if no graphic can be
     * created for the given symbol identifier.
     * @throws IllegalArgumentException if {@code symbolIdentifier} does not describe a circular graphic.
     */
    TacticalCircle createCircle(String symbolIdentifier, Position center, double radius, KV modifiers);

    /**
     * Create a graphic with four sides.
     *
     * @param symbolIdentifier Identifier for the symbol within its symbol set.
     * @param positions        Control points to use to place the graphic. How many points are required depends on the
     *                         type of graphic.
     * @param modifiers        Modifiers to apply to the graphic.
     * @return A new graphic configured to render at the position indicated, or {@code null} if no graphic can be
     * created for the given symbol identifier.
     * @throws IllegalArgumentException if {@code symbolIdentifier} does not describe a quad graphic.
     */
    TacticalQuad createQuad(String symbolIdentifier, Iterable<? extends Position> positions, KV modifiers);

    /**
     * Create a route graphic. A route is composed of point graphics connected by lines.
     *
     * @param symbolIdentifier Identifier for the symbol within its symbol set.
     * @param controlPoints    Graphics to place at the points along the route.
     * @param modifiers        Modifiers to apply to the graphic.
     * @return A new graphic configured to render at the position indicated, or {@code null} if no graphic can be
     * created for the given symbol identifier.
     * @throws IllegalArgumentException if {@code symbolIdentifier} does not describe a route graphic.
     */
    TacticalRoute createRoute(String symbolIdentifier, Iterable<? extends TacticalPoint> controlPoints,
        KV modifiers);

    /**
     * Determines if this factory can create a graphic for a given symbol identifier.
     *
     * @param symbolIdentifier An identifier for a symbol within the symbol set.
     * @return True if this factory can create a graphic for the given symbol id. Returns false if the symbol identifier
     * is not valid, or if the identifier is valid but the factory does not support the graphic.
     */
    boolean isSupported(String symbolIdentifier);
}