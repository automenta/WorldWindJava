/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.formats.vpf;

import gov.nasa.worldwind.avlist.KV;
import gov.nasa.worldwind.util.Logging;

import java.io.File;
import java.util.*;

/**
 * @author dcollins
 * @version $Id: VPFBasicFeatureFactory.java 1171 2013-02-11 21:45:02Z dcollins $
 */
public class VPFBasicFeatureFactory implements VPFFeatureFactory {
    private final VPFTile tile;
    private final VPFPrimitiveData primitiveData;

    /**
     * Constructs an instance of a VPFBasicFeatureFactory which will construct feature data for the specified {@link
     * VPFTile} and {@link VPFPrimitiveData}. The primitive data must contain information for at least those features
     * found in the specified tile.
     *
     * @param tile          the tile which defines the geographic region to construct features for.
     * @param primitiveData the primitive data describing feature information for the geographic region defined by the
     *                      tile.
     */
    public VPFBasicFeatureFactory(VPFTile tile, VPFPrimitiveData primitiveData) {
        this.tile = tile;
        this.primitiveData = primitiveData;
    }

    protected static VPFRelation findFirstRelation(String table1, String table2, VPFRelation[] relations) {
        if (relations == null)
            return null;

        for (VPFRelation rel : relations) {
            if (rel.getTable1().equalsIgnoreCase(table1) && rel.getTable2().equalsIgnoreCase(table2))
                return rel;
        }

        return null;
    }

    protected static boolean matchesTile(VPFRecord row, VPFTile tile) {
        Object fk = row.getValue("tile_id");
        return (fk != null) && (tile.getId() == VPFBasicFeatureFactory.asInt(fk));
    }

    protected static int asInt(Object o) {
        if (o instanceof Number)
            return ((Number) o).intValue();

        return -1;
    }

    protected static VPFFeature createFeature(VPFFeatureClass featureClass, VPFRecord featureRow,
        Iterable<String> attributeKeys,
        VPFBoundingBox bounds, int[] primitiveIds) {
        VPFFeature feature = new VPFFeature(featureClass, featureRow.getId(), bounds, primitiveIds);
        VPFBasicFeatureFactory.setFeatureAttributes(featureRow, attributeKeys, feature);

        return feature;
    }

    @SuppressWarnings("UnusedDeclaration")
    protected static Collection<? extends VPFFeature> doCreateComplexFeatures(VPFFeatureClass featureClass) {
        throw new UnsupportedOperationException();
    }

    protected static Iterable<String> getFeatureAttributeKeys(VPFBufferedRecordData table) {
        Collection<String> keys = new ArrayList<>();

        for (String name : table.getRecordParameterNames()) {
            if (name.equalsIgnoreCase("id") ||
                name.equalsIgnoreCase("tile_id") ||
                name.equalsIgnoreCase("from_to") ||
                name.equalsIgnoreCase("nod_id") ||
                name.equalsIgnoreCase("end_id") ||
                name.equalsIgnoreCase("cnd_id") ||
                name.equalsIgnoreCase("edg_id") ||
                name.equalsIgnoreCase("fac_id") ||
                name.equalsIgnoreCase("txt_id")) {
                continue;
            }

            keys.add(name);
        }

        return keys;
    }

    protected static void setFeatureAttributes(VPFRecord row, Iterable<String> attributeKeys, KV params) {
        for (String key : attributeKeys) {
            VPFUtils.checkAndSetValue(row, key, key, params);
        }
    }

    //**************************************************************//
    //********************  Simple Feature Assembly  ***************//
    //**************************************************************//

    protected static VPFBufferedRecordData createFeatureTable(VPFFeatureClass featureClass) {
        StringBuilder sb = new StringBuilder(featureClass.getCoverage().getFilePath());
        sb.append(File.separator);
        sb.append(featureClass.getFeatureTableName());

        return VPFUtils.readTable(new File(sb.toString()));
    }

    protected static VPFBufferedRecordData createJoinTable(VPFFeatureClass featureClass) {
        if (featureClass.getJoinTableName() == null)
            return null;

        StringBuilder sb = new StringBuilder(featureClass.getCoverage().getFilePath());
        sb.append(File.separator);
        sb.append(featureClass.getJoinTableName());

        return VPFUtils.readTable(new File(sb.toString()));
    }

    protected static VPFRelation getFeatureToPrimitiveRelation(VPFFeatureClass featureClass) {
        return VPFBasicFeatureFactory.findFirstRelation(featureClass.getFeatureTableName(), featureClass.getPrimitiveTableName(),
            featureClass.getRelations());
    }

    protected static VPFRelation getFeatureToJoinRelation(VPFFeatureClass featureClass) {
        return VPFBasicFeatureFactory.findFirstRelation(featureClass.getFeatureTableName(), featureClass.getJoinTableName(),
            featureClass.getRelations());
    }

    protected static VPFRelation getJoinToPrimitiveRelation(VPFFeatureClass featureClass) {
        return VPFBasicFeatureFactory.findFirstRelation(featureClass.getJoinTableName(), featureClass.getPrimitiveTableName(),
            featureClass.getRelations());
    }

    //**************************************************************//
    //********************  Complex Feature Assembly  **************//
    //**************************************************************//

    public VPFTile getTile() {
        return this.tile;
    }

    //**************************************************************//
    //********************  Common Feature Assembly  ***************//
    //**************************************************************//

    public VPFPrimitiveData getPrimitiveData() {
        return this.primitiveData;
    }

    public Collection<? extends VPFFeature> createPointFeatures(VPFFeatureClass featureClass) {
        if (featureClass == null) {
            String message = Logging.getMessage("nullValue.FeatureClassIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        return this.doCreateSimpleFeatures(featureClass);
    }

    public Collection<? extends VPFFeature> createLineFeatures(VPFFeatureClass featureClass) {
        if (featureClass == null) {
            String message = Logging.getMessage("nullValue.FeatureClassIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        return this.doCreateSimpleFeatures(featureClass);
    }

    //**************************************************************//
    //********************  Utility Methods  ***********************//
    //**************************************************************//

    public Collection<? extends VPFFeature> createAreaFeatures(VPFFeatureClass featureClass) {
        if (featureClass == null) {
            String message = Logging.getMessage("nullValue.FeatureClassIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        return this.doCreateSimpleFeatures(featureClass);
    }

    public Collection<? extends VPFFeature> createTextFeatures(VPFFeatureClass featureClass) {
        if (featureClass == null) {
            String message = Logging.getMessage("nullValue.FeatureClassIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        return this.doCreateSimpleFeatures(featureClass);
    }

    public Collection<? extends VPFFeature> createComplexFeatures(VPFFeatureClass featureClass) {
        if (featureClass == null) {
            String message = Logging.getMessage("nullValue.FeatureClassIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        return VPFBasicFeatureFactory.doCreateComplexFeatures(featureClass);
    }

    protected Collection<? extends VPFFeature> doCreateSimpleFeatures(VPFFeatureClass featureClass) {
        if (this.primitiveData == null) {
            String message = Logging.getMessage("VPF.NoPrimitiveData");
            Logging.logger().severe(message);
            throw new IllegalStateException(message);
        }

        Collection<VPFFeature> results = new ArrayList<>();

        VPFBufferedRecordData featureTable = VPFBasicFeatureFactory.createFeatureTable(featureClass);
        if (featureTable == null)
            return null;

        VPFBufferedRecordData joinTable = VPFBasicFeatureFactory.createJoinTable(featureClass);
        Iterable<String> attributeKeys = VPFBasicFeatureFactory.getFeatureAttributeKeys(featureTable);

        for (VPFRecord featureRow : featureTable) {
            VPFFeature feature = this.doCreateSimpleFeature(featureClass, featureRow, joinTable, attributeKeys);
            if (feature != null)
                results.add(feature);
        }

        return results;
    }

    protected VPFFeature doCreateSimpleFeature(VPFFeatureClass featureClass, VPFRecord featureRow,
        Iterable<VPFRecord> joinTable, Iterable<String> attributeKeys) {
        if (joinTable != null) {
            return this.createCompoundSimpleFeature(featureClass, featureRow, joinTable, attributeKeys);
        } else {
            return this.createSimpleFeature(featureClass, featureRow, attributeKeys);
        }
    }

    protected VPFFeature createSimpleFeature(VPFFeatureClass featureClass, VPFRecord featureRow,
        Iterable<String> attributeKeys) {
        // Feature has a direct 1:1 relation to the primitive table.

        if (this.tile != null && !VPFBasicFeatureFactory.matchesTile(featureRow, this.tile))
            return null;

        VPFRelation featureToPrimitive = VPFBasicFeatureFactory.getFeatureToPrimitiveRelation(featureClass);
        if (featureToPrimitive == null)
            return null;

        int primitiveId = VPFBasicFeatureFactory.asInt(featureRow.getValue(featureToPrimitive.getTable1Key()));
        VPFPrimitiveData.PrimitiveInfo primitiveInfo = this.primitiveData.getPrimitiveInfo(
            featureToPrimitive.getTable2(), primitiveId);

        return VPFBasicFeatureFactory.createFeature(featureClass, featureRow, attributeKeys, primitiveInfo.getBounds(),
            new int[] {primitiveId});
    }

    protected VPFFeature createCompoundSimpleFeature(VPFFeatureClass featureClass, VPFRecord featureRow,
        Iterable<VPFRecord> joinTable, Iterable<String> attributeKeys) {
        // Feature has a direct 1:* relation to the primitive table through a join table.

        // Query the number of primitives which match the feature.
        Object o = this.getPrimitiveIds(featureClass, featureRow, joinTable, null, true);
        if (!(o instanceof Integer))
            return null;

        int numPrimitives = (Integer) o;
        if (numPrimitives < 1)
            return null;

        // Gather the actual primitive ids matching the feature.
        int[] primitiveIds = new int[numPrimitives];
        VPFBoundingBox bounds = (VPFBoundingBox) this.getPrimitiveIds(featureClass, featureRow, joinTable, primitiveIds,
            false);

        return VPFBasicFeatureFactory.createFeature(featureClass, featureRow, attributeKeys, bounds, primitiveIds);
    }

    protected Object getPrimitiveIds(VPFFeatureClass featureClass, VPFRecord featureRow,
        Iterable<VPFRecord> joinTable, int[] primitiveIds, boolean query) {
        // Although a direct link between feature and primitive(s) is provided by the primitive_id column in the join
        // table, a sequential search of the feature_id column must still be performed to find all primitives associated
        // with a selected feature.

        VPFRelation featureToJoin = VPFBasicFeatureFactory.getFeatureToJoinRelation(featureClass);
        if (featureToJoin == null)
            return null;

        VPFRelation joinToPrimitive = VPFBasicFeatureFactory.getJoinToPrimitiveRelation(featureClass);
        if (joinToPrimitive == null)
            return null;

        int featureId = featureRow.getId();
        String joinFeatureKey = featureToJoin.getTable2Key();
        String joinPrimitiveKey = joinToPrimitive.getTable1Key();
        String primitiveTable = joinToPrimitive.getTable2();

        int numPrimitives = 0;
        VPFBoundingBox bounds = null;

        for (VPFRecord joinRow : joinTable) {
            if (this.tile != null && !VPFBasicFeatureFactory.matchesTile(joinRow, this.tile))
                continue;

            int fId = VPFBasicFeatureFactory.asInt(joinRow.getValue(joinFeatureKey));
            if (featureId != fId)
                continue;

            if (!query) {
                int pId = VPFBasicFeatureFactory.asInt(joinRow.getValue(joinPrimitiveKey));
                primitiveIds[numPrimitives] = pId;

                VPFPrimitiveData.PrimitiveInfo primitiveInfo = this.primitiveData.getPrimitiveInfo(primitiveTable, pId);
                bounds = (bounds != null) ? bounds.union(primitiveInfo.getBounds()) : primitiveInfo.getBounds();
            }

            numPrimitives++;
        }

        return query ? numPrimitives : bounds;
    }
}