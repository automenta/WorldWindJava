/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.formats.vpf;

import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.globes.Globe;
import gov.nasa.worldwind.util.Logging;

import java.util.Objects;

/**
 * @author dcollins
 * @version $Id: VPFTile.java 1171 2013-02-11 21:45:02Z dcollins $
 */
public class VPFTile implements ExtentHolder
{
    private final int id;
    private final String name;
    private final VPFBoundingBox bounds;

    public VPFTile(int id, String name, VPFBoundingBox bounds)
    {
        if (name == null)
        {
            String message = Logging.getMessage("nullValue.NameIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (bounds == null)
        {
            String message = Logging.getMessage("nullValue.BoundingBoxIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.id = id;
        this.name = name;
        this.bounds = bounds;
    }

    public int getId()
    {
        return this.id;
    }

    public String getName()
    {
        return this.name;
    }

    public VPFBoundingBox getBounds()
    {
        return this.bounds;
    }

    public Extent getExtent(Globe globe, double verticalExaggeration)
    {
        if (globe == null)
        {
            String message = Logging.getMessage("nullValue.GlobeIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        return Sector.computeBoundingCylinder(globe, verticalExaggeration, this.bounds.toSector());
    }

    public boolean equals(Object o)
    {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        VPFTile vpfTile = (VPFTile) o;

        if (id != vpfTile.id)
            return false;
        if (!Objects.equals(bounds, vpfTile.bounds))
            return false;
        //noinspection RedundantIfStatement
        if (!Objects.equals(name, vpfTile.name))
            return false;

        return true;
    }

    public int hashCode()
    {
        int result = id;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (bounds != null ? bounds.hashCode() : 0);
        return result;
    }

    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(this.id);
        sb.append(": ");
        sb.append(this.name);
        return sb.toString();
    }
}
