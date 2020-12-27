/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.formats.vpf;

////.*;

import gov.nasa.worldwind.util.*;

import java.util.*;

/**
 * @author dcollins
 * @version $Id: VPFPrimitiveData.java 1171 2013-02-11 21:45:02Z dcollins $
 */
public class VPFPrimitiveData {
    protected final Map<String, PrimitiveInfo[]> primitiveInfo;
    protected final Map<String, VecBufferSequence> primitiveCoords;
    protected final Map<String, CompoundStringBuilder> primitiveStrings;

    public VPFPrimitiveData() {
        this.primitiveInfo = new HashMap<>();
        this.primitiveCoords = new HashMap<>();
        this.primitiveStrings = new HashMap<>();
    }

    public PrimitiveInfo[] getPrimitiveInfo(String name) {
        return this.primitiveInfo.get(name);
    }

    public void setPrimitiveInfo(String name, PrimitiveInfo[] info) {
        this.primitiveInfo.put(name, info);
    }

    public PrimitiveInfo getPrimitiveInfo(String name, int id) {
        return this.primitiveInfo.get(name)[VPFBufferedRecordData.indexFromId(id)];
    }

    public VecBufferSequence getPrimitiveCoords(String name) {
        return this.primitiveCoords.get(name);
    }

    public void setPrimitiveCoords(String name, VecBufferSequence coords) {
        this.primitiveCoords.put(name, coords);
    }

    public CompoundStringBuilder getPrimitiveStrings(String name) {
        return this.primitiveStrings.get(name);
    }

    public void setPrimitiveStrings(String name, CompoundStringBuilder strings) {
        this.primitiveStrings.put(name, strings);
    }

    public interface PrimitiveInfo {
        VPFBoundingBox getBounds();
    }

    public static class BasicPrimitiveInfo implements PrimitiveInfo {
        protected final VPFBoundingBox bounds;

        public BasicPrimitiveInfo(VPFBoundingBox bounds) {
            this.bounds = bounds;
        }

        public VPFBoundingBox getBounds() {
            return this.bounds;
        }
    }

    public static class EdgeInfo extends BasicPrimitiveInfo {
        protected final int edgeType;
        protected final int startNode;
        protected final int endNode;
        protected final int leftFace;
        protected final int rightFace;
        protected final int leftEdge;
        protected final int rightEdge;
        protected final boolean isOnTileBoundary;

        public EdgeInfo(int edgeType, int startNode, int endNode, int leftFace, int rightFace, int leftEdge,
            int rightEdge, boolean isOnTileBoundary, VPFBoundingBox bounds) {
            super(bounds);
            this.edgeType = edgeType;
            this.startNode = startNode;
            this.endNode = endNode;
            this.leftFace = leftFace;
            this.rightFace = rightFace;
            this.leftEdge = leftEdge;
            this.rightEdge = rightEdge;
            this.isOnTileBoundary = isOnTileBoundary;
        }

        public int getEdgeType() {
            return this.edgeType;
        }

        public int getStartNode() {
            return this.startNode;
        }

        public int getEndNode() {
            return this.endNode;
        }

        public int getLeftFace() {
            return this.leftFace;
        }

        public int getRightFace() {
            return this.rightFace;
        }

        public int getLeftEdge() {
            return this.leftEdge;
        }

        public int getRightEdge() {
            return this.rightEdge;
        }

        public boolean isOnTileBoundary() {
            return this.isOnTileBoundary;
        }
    }

    public static class FaceInfo extends BasicPrimitiveInfo {
        protected final Ring outerRing;
        protected final Ring[] innerRings;
        protected VPFBoundingBox bounds;

        public FaceInfo(Ring outerRing, Ring[] innerRings, VPFBoundingBox bounds) {
            super(bounds);
            this.outerRing = outerRing;
            this.innerRings = innerRings;
        }

        public Ring getOuterRing() {
            return this.outerRing;
        }

        public Ring[] getInnerRings() {
            return this.innerRings;
        }
    }

    public static class Ring {
        protected final int numEdges;
        protected final int[] edgeId;
        protected final int[] edgeOrientation;

        public Ring(int numEdges, int[] edgeId, int[] edgeOrientation) {
            this.numEdges = numEdges;
            this.edgeId = edgeId;
            this.edgeOrientation = edgeOrientation;
        }

        public int getNumEdges() {
            return this.numEdges;
        }

        public int getEdgeId(int index) {
            return this.edgeId[index];
        }

        public int getEdgeOrientation(int index) {
            return this.edgeOrientation[index];
        }
    }
}
