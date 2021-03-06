/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.formats.vpf;

import gov.nasa.worldwind.avlist.KVMap;

import java.util.*;

/**
 * @author dcollins
 * @version $Id: VPFFeatureClass.java 1171 2013-02-11 21:45:02Z dcollins $
 */
public class VPFFeatureClass extends KVMap {
    protected final VPFCoverage coverage;
    protected final VPFFeatureClassSchema schema;
    protected final String joinTableName;
    protected final String primitiveTableName;
    protected VPFRelation[] relations;

    public VPFFeatureClass(VPFCoverage coverage, VPFFeatureClassSchema schema, String joinTableName,
        String primitiveTableName) {
        this.coverage = coverage;
        this.schema = schema;
        this.joinTableName = joinTableName;
        this.primitiveTableName = primitiveTableName;
    }

    public VPFCoverage getCoverage() {
        return this.coverage;
    }

    public VPFFeatureClassSchema getSchema() {
        return this.schema;
    }

    public String getClassName() {
        return this.schema.getClassName();
    }

    public VPFFeatureType getType() {
        return this.schema.getType();
    }

    public String getFeatureTableName() {
        return this.schema.getFeatureTableName();
    }

    public String getJoinTableName() {
        return this.joinTableName;
    }

    public String getPrimitiveTableName() {
        return this.primitiveTableName;
    }

    public VPFRelation[] getRelations() {
        return this.relations;
    }

    public void setRelations(VPFRelation[] relations) {
        this.relations = relations;
    }

    public Collection<? extends VPFFeature> createFeatures(VPFFeatureFactory factory) {
        return null;
    }

    public Collection<? extends VPFSymbol> createFeatureSymbols(VPFSymbolFactory factory) {
        return null;
    }

    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || this.getClass() != o.getClass())
            return false;

        VPFFeatureClass that = (VPFFeatureClass) o;

        if (this.coverage != null ? !this.coverage.getFilePath().equals(that.coverage.getFilePath())
            : that.coverage != null)
            return false;
        if (!Objects.equals(this.schema, that.schema))
            return false;
        if (!Arrays.equals(this.relations, that.relations))
            return false;
        if (!Objects.equals(this.joinTableName, that.joinTableName))
            return false;
        //noinspection RedundantIfStatement
        if (!Objects.equals(this.primitiveTableName, that.primitiveTableName))
            return false;

        return true;
    }

    public int hashCode() {
        int result = this.coverage != null ? this.coverage.hashCode() : 0;
        result = 31 * result + (this.schema != null ? this.schema.hashCode() : 0);
        result = 31 * result + (this.relations != null ? Arrays.hashCode(this.relations) : 0);
        result = 31 * result + (this.joinTableName != null ? this.joinTableName.hashCode() : 0);
        result = 31 * result + (this.primitiveTableName != null ? this.primitiveTableName.hashCode() : 0);
        return result;
    }
}