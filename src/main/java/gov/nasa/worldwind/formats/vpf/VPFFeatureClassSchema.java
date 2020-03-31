/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.formats.vpf;

import java.util.Objects;

/**
 * @author dcollins
 * @version $Id: VPFFeatureClassSchema.java 1171 2013-02-11 21:45:02Z dcollins $
 */
public class VPFFeatureClassSchema
{
    protected final String className;
    protected final VPFFeatureType type;
    protected final String featureTableName;

    public VPFFeatureClassSchema(String className, VPFFeatureType type, String featureTableName)
    {
        this.className = className;
        this.type = type;
        this.featureTableName = featureTableName;
    }

    public String getClassName()
    {
        return this.className;
    }

    public VPFFeatureType getType()
    {
        return this.type;
    }

    public String getFeatureTableName()
    {
        return this.featureTableName;
    }

    public boolean equals(Object o)
    {
        if (this == o)
            return true;
        if (o == null || this.getClass() != o.getClass())
            return false;

        VPFFeatureClassSchema that = (VPFFeatureClassSchema) o;

        if (!Objects.equals(this.className, that.className))
            return false;
        if (!Objects.equals(this.featureTableName, that.featureTableName))
            return false;
        //noinspection RedundantIfStatement
        if (!Objects.equals(this.type, that.type))
            return false;

        return true;
    }

    public int hashCode()
    {
        int result = this.className != null ? this.className.hashCode() : 0;
        result = 31 * result + (this.type != null ? this.type.hashCode() : 0);
        result = 31 * result + (this.featureTableName != null ? this.featureTableName.hashCode() : 0);
        return result;
    }
}
