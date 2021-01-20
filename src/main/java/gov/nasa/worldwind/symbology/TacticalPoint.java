/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */

package gov.nasa.worldwind.symbology;

import gov.nasa.worldwind.avlist.KV;
import gov.nasa.worldwind.geom.Position;

/**
 * An interface for tactical graphics that are positioned by a single point.
 *
 * @author pabercrombie
 * @version $Id: TacticalPoint.java 1171 2013-02-11 21:45:02Z dcollins $
 * @see TacticalGraphicFactory#createPoint(String, Position, KV)
 */
public interface TacticalPoint extends TacticalGraphic {
    /**
     * Indicates the position of the graphic.
     *
     * @return The position of the graphic.
     */
    Position getPosition();

    /**
     * Specifies the position of the graphic.
     *
     * @param position New position.
     */
    void setPosition(Position position);
}