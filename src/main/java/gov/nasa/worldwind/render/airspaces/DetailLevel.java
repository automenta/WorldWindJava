/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.render.airspaces;

import gov.nasa.worldwind.avlist.KV;
import gov.nasa.worldwind.render.DrawContext;

/**
 * @author dcollins
 * @version $Id: DetailLevel.java 1171 2013-02-11 21:45:02Z dcollins $
 */
public interface DetailLevel extends Comparable<DetailLevel>, KV {
    boolean meetsCriteria(DrawContext dc, Airspace airspace);

    int compareTo(DetailLevel level);
}