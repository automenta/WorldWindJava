/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.formats.vpf;

import gov.nasa.worldwind.util.*;

import java.io.File;
import java.util.*;

/**
 * @author dcollins
 * @version $Id: VPFBasicPrimitiveDataFactory.java 1171 2013-02-11 21:45:02Z dcollins $
 */
public class VPFBasicPrimitiveDataFactory implements VPFPrimitiveDataFactory {
    private final VPFTile tile;

    /**
     * Constructs an instance of a VPFBasicPrimitiveDataFactory which will construct primiitve data for the specified
     * {@link VPFTile}.
     *
     * @param tile the tile which defines the geographic region to construct features for.
     */
    public VPFBasicPrimitiveDataFactory(VPFTile tile) {
        this.tile = tile;
    }

    protected static String getPrimitiveTablePath(VPFCoverage coverage, VPFTile tile, String tableName) {
        // Start with the coverage directory.
        StringBuilder sb = new StringBuilder(coverage.getFilePath());
        sb.append(File.separator);

        // If the tile is non-null then append the tile's path.
        if (tile != null) {
            sb.append(tile.getName());
            sb.append(File.separator);
        }

        // Append the primitive table name.
        sb.append(tableName);

        return sb.toString();
    }

    protected static boolean isEdgeOnTileBoundary(VPFRecord record) {
        VPFTripletId id = null;

        Object o = record.getValue("left_face");
        if (o instanceof VPFTripletId)
            id = (VPFTripletId) o;

        if (id == null) {
            o = record.getValue("right_face");
            if (o instanceof VPFTripletId)
                id = (VPFTripletId) o;
        }

        return id != null && id.getExtId() > 0;
    }

    //**************************************************************//
    //********************  Primitive Assembly  ********************//
    //**************************************************************//

    protected static int getNumber(Object key) {
        if (key instanceof Number)
            return ((Number) key).intValue();

        return -1;
    }

    protected static int getId(Object key) {
        if (key instanceof Number)
            return ((Number) key).intValue();
        else if (key instanceof VPFTripletId)
            return ((VPFTripletId) key).getId();

        return -1;
    }

    protected static void buildNodePrimitives(VPFCoverage coverage, VPFTile tile, VPFPrimitiveData primitiveData) {
        VPFBufferedRecordData nodeTable = VPFBasicPrimitiveDataFactory.createPrimitiveTable(coverage, tile,
            VPFConstants.NODE_PRIMITIVE_TABLE);
        if (nodeTable != null && nodeTable.getNumRecords() > 0)
            VPFBasicPrimitiveDataFactory.buildNodePrimitives(nodeTable, VPFConstants.NODE_PRIMITIVE_TABLE,
                primitiveData);

        VPFBufferedRecordData entityNodeTable = VPFBasicPrimitiveDataFactory.createPrimitiveTable(coverage, tile,
            VPFConstants.ENTITY_NODE_PRIMITIVE_TABLE);
        if (entityNodeTable != null && entityNodeTable.getNumRecords() > 0)
            VPFBasicPrimitiveDataFactory.buildNodePrimitives(entityNodeTable, VPFConstants.ENTITY_NODE_PRIMITIVE_TABLE,
                primitiveData);

        VPFBufferedRecordData connectedNodeTable = VPFBasicPrimitiveDataFactory.createPrimitiveTable(coverage, tile,
            VPFConstants.CONNECTED_NODE_PRIMITIVE_TABLE);
        if (connectedNodeTable != null && connectedNodeTable.getNumRecords() > 0)
            VPFBasicPrimitiveDataFactory.buildNodePrimitives(connectedNodeTable,
                VPFConstants.CONNECTED_NODE_PRIMITIVE_TABLE, primitiveData);
    }

    protected static boolean buildNodePrimitives(VPFBufferedRecordData table, String name,
        VPFPrimitiveData primitiveData) {
        int numNodes = table.getNumRecords();
        VPFPrimitiveData.BasicPrimitiveInfo[] nodeInfo = new VPFPrimitiveData.BasicPrimitiveInfo[numNodes];
        VecBufferSequence coords = (VecBufferSequence) table.getRecordData("coordinate").getBackingData();

        for (VPFRecord row : table) {
            int id = row.getId();

            nodeInfo[VPFBufferedRecordData.indexFromId(id)] = new VPFPrimitiveData.BasicPrimitiveInfo(
                VPFBoundingBox.fromVecBuffer(coords.subBuffer(id)));
        }

        primitiveData.setPrimitiveInfo(name, nodeInfo);
        primitiveData.setPrimitiveCoords(name, coords);
        return true;
    }

    protected static void buildEdgePrimitives(VPFCoverage coverage, VPFTile tile, VPFPrimitiveData primitiveData) {
        VPFBufferedRecordData edgeTable = VPFBasicPrimitiveDataFactory.createPrimitiveTable(coverage, tile,
            VPFConstants.EDGE_PRIMITIVE_TABLE);
        if (edgeTable == null || edgeTable.getNumRecords() == 0)
            return;

        VPFBufferedRecordData mbrTable = VPFBasicPrimitiveDataFactory.createPrimitiveTable(coverage, tile,
            VPFConstants.EDGE_BOUNDING_RECTANGLE_TABLE);
        if (mbrTable == null)
            return;

        int numEdges = edgeTable.getNumRecords();
        VPFPrimitiveData.EdgeInfo[] edgeInfo = new VPFPrimitiveData.EdgeInfo[numEdges];
        VecBufferSequence coords = (VecBufferSequence) edgeTable.getRecordData(
            "coordinates").getBackingData();

        for (VPFRecord row : edgeTable) {
            int id = row.getId();
            VPFRecord mbrRow = mbrTable.getRecord(id);

            edgeInfo[VPFBufferedRecordData.indexFromId(id)] = new VPFPrimitiveData.EdgeInfo(
                VPFBasicPrimitiveDataFactory.getNumber(row.getValue("edge_type")),
                VPFBasicPrimitiveDataFactory.getId(row.getValue("start_node")), VPFBasicPrimitiveDataFactory.getNumber(row.getValue("end_node")),
                VPFBasicPrimitiveDataFactory.getId(row.getValue("left_face")), VPFBasicPrimitiveDataFactory.getId(row.getValue("right_face")),
                VPFBasicPrimitiveDataFactory.getId(row.getValue("left_edge")), VPFBasicPrimitiveDataFactory.getId(row.getValue("right_edge")),
                VPFBasicPrimitiveDataFactory.isEdgeOnTileBoundary(row),
                VPFUtils.getExtent(mbrRow));
        }

        primitiveData.setPrimitiveInfo(VPFConstants.EDGE_PRIMITIVE_TABLE, edgeInfo);
        primitiveData.setPrimitiveCoords(VPFConstants.EDGE_PRIMITIVE_TABLE, coords);
    }

    protected static void buildFacePrimitives(VPFCoverage coverage, VPFTile tile, VPFPrimitiveData primitiveData) {
        VPFBufferedRecordData faceTable = VPFBasicPrimitiveDataFactory.createPrimitiveTable(coverage, tile,
            VPFConstants.FACE_PRIMITIVE_TABLE);
        if (faceTable == null)
            return;

        VPFBufferedRecordData mbrTable = VPFBasicPrimitiveDataFactory.createPrimitiveTable(coverage, tile,
            VPFConstants.FACE_BOUNDING_RECTANGLE_TABLE);
        if (mbrTable == null)
            return;

        VPFBufferedRecordData ringTable = VPFBasicPrimitiveDataFactory.createPrimitiveTable(coverage, tile,
            VPFConstants.RING_TABLE);
        if (ringTable == null)
            return;

        VPFPrimitiveData.PrimitiveInfo[] edgeInfo = primitiveData.getPrimitiveInfo(VPFConstants.EDGE_PRIMITIVE_TABLE);

        int numFaces = faceTable.getNumRecords();
        VPFPrimitiveData.FaceInfo[] faceInfo = new VPFPrimitiveData.FaceInfo[numFaces];

        for (VPFRecord faceRow : faceTable) {
            int faceId = faceRow.getId();
            VPFRecord mbrRow = mbrTable.getRecord(faceId);

            // Face ID 1 is reserved for the "universe face", which does not have any associated geometry.
            if (faceId == 1)
                continue;

            // The first ring primitive associated with the face primitive defines the outer ring. The face primitive must
            // at least contain coordinates for an outer ring.

            int ringId = ((Number) faceRow.getValue("ring_ptr")).intValue();
            VPFRecord ringRow = ringTable.getRecord(ringId);
            VPFPrimitiveData.Ring outerRing = VPFBasicPrimitiveDataFactory.buildRing(ringRow, edgeInfo);

            // The ring table maintains an order relationship for its rows. The first record of a new face id will always
            // be defined as the outer ring. Any repeating records with an identical face value will define inner rings.

            Collection<VPFPrimitiveData.Ring> innerRingList = new ArrayList<>();

            for (ringId = ringId + 1; ringId <= ringTable.getNumRecords(); ringId++) {
                ringRow = ringTable.getRecord(ringId);

                // Break on the first ring primitive row which isn't associated with the face. Because the ring rows
                // maintain an ordering with respect to face id, there will be no other ring rows corresponding to this
                // face.
                if (faceId != VPFBasicPrimitiveDataFactory.getId(ringRow.getValue("face_id")))
                    break;

                VPFPrimitiveData.Ring innerRing = VPFBasicPrimitiveDataFactory.buildRing(ringRow, edgeInfo);
                innerRingList.add(innerRing);
            }

            VPFPrimitiveData.Ring[] innerRings = new VPFPrimitiveData.Ring[innerRingList.size()];
            innerRingList.toArray(innerRings);

            faceInfo[VPFBufferedRecordData.indexFromId(faceId)] = new VPFPrimitiveData.FaceInfo(
                outerRing, innerRings, VPFUtils.getExtent(mbrRow));
        }

        primitiveData.setPrimitiveInfo(VPFConstants.FACE_PRIMITIVE_TABLE, faceInfo);
    }

    //**************************************************************//
    //********************  Winged-Edge Face Construction  *********//
    //**************************************************************//

    protected static void buildTextPrimitives(VPFCoverage coverage, VPFTile tile, VPFPrimitiveData primitiveData) {
        VPFBufferedRecordData textTable = VPFBasicPrimitiveDataFactory.createPrimitiveTable(coverage, tile,
            VPFConstants.TEXT_PRIMITIVE_TABLE);
        if (textTable == null || textTable.getNumRecords() == 0)
            return;

        int numText = textTable.getNumRecords();
        VPFPrimitiveData.BasicPrimitiveInfo[] textInfo = new VPFPrimitiveData.BasicPrimitiveInfo[numText];
        VecBufferSequence coords = (VecBufferSequence) textTable.getRecordData("shape_line").getBackingData();
        CompoundStringBuilder strings = (CompoundStringBuilder) textTable.getRecordData("string").getBackingData();

        for (VPFRecord row : textTable) {
            int id = row.getId();

            textInfo[VPFBufferedRecordData.indexFromId(id)] = new VPFPrimitiveData.BasicPrimitiveInfo(
                VPFBoundingBox.fromVecBuffer(coords.subBuffer(id)));
        }

        primitiveData.setPrimitiveInfo(VPFConstants.TEXT_PRIMITIVE_TABLE, textInfo);
        primitiveData.setPrimitiveCoords(VPFConstants.TEXT_PRIMITIVE_TABLE, coords);
        primitiveData.setPrimitiveStrings(VPFConstants.TEXT_PRIMITIVE_TABLE, strings);
    }

    //**************************************************************//
    //********************  Utility Methods  ***********************//
    //**************************************************************//

    /**
     * Given a row from the ring primitive table, navigate the ring and edge primitive tables to construct a new {@link
     * VPFPrimitiveData.Ring}.
     *
     * @param row           the ring primitive row.
     * @param edgeInfoArray the edge primitive data.
     * @return a new Ring.
     */
    protected static VPFPrimitiveData.Ring buildRing(VPFRecord row, VPFPrimitiveData.PrimitiveInfo[] edgeInfoArray) {
        int faceId = ((Number) row.getValue("face_id")).intValue();
        int startEdgeId = ((Number) row.getValue("start_edge")).intValue();

        // Traverse the ring to collect the number of edges which define the ring.
        final int numEdges = VPFWingedEdgeTraverser.traverseRing(faceId, startEdgeId, edgeInfoArray, null);
        final int[] idArray = new int[numEdges];
        final int[] orientationArray = new int[numEdges];

        // Traverse the ring again, but this time populate an entry for the primitiveID and orientation data stuctures
        // for each edge.
        VPFWingedEdgeTraverser.traverseRing(faceId, startEdgeId, edgeInfoArray,
            (index, primitiveId, reverseCoordinates) -> {
                idArray[index] = primitiveId;
                orientationArray[index] = reverseCoordinates ? -1 : 1;
            });

        return new VPFPrimitiveData.Ring(numEdges, idArray, orientationArray);
    }

    protected static VPFBufferedRecordData createPrimitiveTable(VPFCoverage coverage, VPFTile tile, String tableName) {
        String path = VPFBasicPrimitiveDataFactory.getPrimitiveTablePath(coverage, tile, tableName);

        File file = new File(path);
        if (!file.exists())
            return null;

        return VPFUtils.readTable(file);
    }

    public VPFTile getTile() {
        return this.tile;
    }

    public VPFPrimitiveData createPrimitiveData(VPFCoverage coverage) {
        if (coverage == null) {
            String message = Logging.getMessage("nullValue.CoverageIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        String path = VPFBasicPrimitiveDataFactory.getPrimitiveTablePath(coverage, this.tile, "");
        File file = new File(path);
        if (!file.exists())
            return null;

        return this.doCreatePrimitives(coverage);
    }

    protected VPFPrimitiveData doCreatePrimitives(VPFCoverage coverage) {
        VPFPrimitiveData primitiveData = new VPFPrimitiveData();
        VPFBasicPrimitiveDataFactory.buildNodePrimitives(coverage, this.tile, primitiveData);
        VPFBasicPrimitiveDataFactory.buildEdgePrimitives(coverage, this.tile, primitiveData);
        VPFBasicPrimitiveDataFactory.buildFacePrimitives(coverage, this.tile, primitiveData);
        VPFBasicPrimitiveDataFactory.buildTextPrimitives(coverage, this.tile, primitiveData);

        return primitiveData;
    }
}
