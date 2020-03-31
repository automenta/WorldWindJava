/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.formats.rpf;

import gov.nasa.worldwind.formats.nitfs.NITFSUtil;

import java.nio.ByteBuffer;
import java.util.*;

/**
 * @author brownrigg
 * @version $Id: RPFBoundingRectangleSection.java 1171 2013-02-11 21:45:02Z dcollins $
 */

public class RPFBoundingRectangleSection {

    public RPFBoundingRectangleSection(ByteBuffer buffer) {
        // [ bounding rectangle section subheader ]
        this.tableOffset = NITFSUtil.getUInt(buffer);
        this.numberOfRecords = NITFSUtil.getUShort(buffer);
        this.recordLength = NITFSUtil.getUShort(buffer);

        parseBoundsRecords(buffer);
    }

    public List<RPFBoundingRectangleRecord> getBoundingRecords() {
        return bndRectRecords;
    }

    private void parseBoundsRecords(ByteBuffer buffer) {
        for (int i=0; i<this.numberOfRecords; i++)
            bndRectRecords.add(new RPFBoundingRectangleRecord(buffer));
    }

    public static class RPFBoundingRectangleRecord {

        public double getMinLon() {
            return Math.min(this.ulLon, this.llLon);
        }

        public double getMinLat() {
            return Math.min(this.llLat, this.lrLat);
        }

        public double getMaxLon() {
            return Math.max(this.urLon, this.lrLon);
        }

        public double getMaxLat() {
            return Math.max(this.ulLat, this.urLat);
        }

        public RPFBoundingRectangleRecord(ByteBuffer buffer) {
            this.dataType = NITFSUtil.getString(buffer, 5);
            this.compressionRatio = NITFSUtil.getString(buffer, 5);
            this.scale = NITFSUtil.getString(buffer, 12);
            this.zone = NITFSUtil.getString(buffer, 1);
            this.producer = NITFSUtil.getString(buffer, 5);
            this.ulLat = buffer.getDouble();
            this.ulLon = buffer.getDouble();
            this.llLat = buffer.getDouble();
            this.llLon = buffer.getDouble();
            this.urLat = buffer.getDouble();
            this.urLon = buffer.getDouble();
            this.lrLat = buffer.getDouble();
            this.lrLon = buffer.getDouble();
            this.nsRes = buffer.getDouble();
            this.ewRes = buffer.getDouble();
            this.latInterval = buffer.getDouble();
            this.lonInterval = buffer.getDouble();
            this.numFramesNS = NITFSUtil.getUInt(buffer);
            this.numFramesEW = NITFSUtil.getUInt(buffer);
        }

        private final String dataType;
        private final String compressionRatio;
        private final String scale;
        private final String zone;
        private final String producer;
        private final double ulLat;
        private final double ulLon;
        private final double llLat;
        private final double llLon;
        private final double urLat;
        private final double urLon;
        private final double lrLat;
        private final double lrLon;
        private final double nsRes;
        private final double ewRes;
        private final double latInterval;
        private final double lonInterval;
        private final long   numFramesNS;
        private final long   numFramesEW;
    }

    private final long tableOffset;
    private final int numberOfRecords;
    private final int recordLength;
    private final ArrayList<RPFBoundingRectangleRecord> bndRectRecords =
        new ArrayList<>();
}
