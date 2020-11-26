/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.geom;

import gov.nasa.worldwind.globes.Globe;

/**
 * ExtentHolder provides an interface to query an object's enclosing volume in model coordinates.
 *
 * @author dcollins
 * @version $Id: ExtentHolder.java 1171 2013-02-11 21:45:02Z dcollins $
 * @see Extent
 */
public interface ExtentHolder {
    /**
     * Returns the objects enclosing volume as an {@link Extent} in model coordinates, given a
     * specified {@link Globe} and vertical exaggeration (see {@link
     * gov.nasa.worldwind.SceneController#getVerticalExaggeration()}.
     *
     * @param globe                the Globe the object is related to.
     * @param verticalExaggeration the vertical exaggeration of the scene containing this object.
     * @return the object's Extent in model coordinates.
     * @throws IllegalArgumentException if the Globe is null.
     */
    Extent getExtent(Globe globe, double verticalExaggeration);
}
