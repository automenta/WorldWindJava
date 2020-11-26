/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */

package gov.nasa.worldwind.formats.rpf;

import gov.nasa.worldwind.formats.nitfs.*;

import java.nio.ByteBuffer;
import java.util.*;

/**
 * @author Lado Garakanidze
 * @version $Id: RPFFrameFileIndexSection.java 1171 2013-02-11 21:45:02Z dcollins $
 */
public class RPFFrameFileIndexSection {
    // [ frame file index section subheader ]
    private final String highestSecurityClassification;
    private final long frameFileIndexTableOffset;
    private final long numOfFrameFileIndexRecords;
    private final int numOfPathnameRecords;
    private final int frameFileIndexRecordLength;

    // [ frame file index subsection ]

    //      [ frame file index table ]
    private final List<RPFFrameFileIndexRecord> frameFileIndexTable = new ArrayList<>();
    //      [ pathname table ]
    // private ArrayList<String> pathnameTable = new ArrayList<String>();

    public RPFFrameFileIndexSection(ByteBuffer buffer) {
        // [ frame file index section subheader ]
        this.highestSecurityClassification = NITFSUtil.getString(buffer, 1);
        this.frameFileIndexTableOffset = NITFSUtil.getUInt(buffer);
        this.numOfFrameFileIndexRecords = NITFSUtil.getUInt(buffer);
        this.numOfPathnameRecords = NITFSUtil.getUShort(buffer);
        this.frameFileIndexRecordLength = NITFSUtil.getUShort(buffer);

        this.parseFrameFileIndexAndPathnameTables(buffer);
    }

    public String getHighestSecurityClassification() {
        return highestSecurityClassification;
    }

    public long getFrameFileIndexTableOffset() {
        return frameFileIndexTableOffset;
    }

    public long getNumOfFrameFileIndexRecords() {
        return numOfFrameFileIndexRecords;
    }

    public int getNumOfPathnameRecords() {
        return numOfPathnameRecords;
    }

    public int getFrameFileIndexRecordLength() {
        return frameFileIndexRecordLength;
    }

//    public ArrayList<String> getPathnameTable()
//    {
//        return pathnameTable;
//    }

    public List<RPFFrameFileIndexRecord> getFrameFileIndexTable() {
        return frameFileIndexTable;
    }

    private void parseFrameFileIndexAndPathnameTables(ByteBuffer buffer) {
        int theSectionOffset = buffer.position();
        Hashtable<Integer, String> pathnames = new Hashtable<>();

        for (int i = 0; i < this.numOfFrameFileIndexRecords; i++) {
            this.frameFileIndexTable.add(new RPFFrameFileIndexRecord(buffer));
        }

        for (int i = 0; i < this.numOfPathnameRecords; i++) {
            int relOffset = buffer.position() - theSectionOffset;
            int len = NITFSUtil.getUShort(buffer);
            pathnames.put(relOffset, NITFSUtil.getString(buffer, len));
        }

        if (!this.frameFileIndexTable.isEmpty()
            && !pathnames.isEmpty()) { // update pathname field in every RPFFrameFileIndexRecord
            for (RPFFrameFileIndexRecord rec : this.frameFileIndexTable) {
                int offset = (int) rec.getPathnameRecordOffset();
                if (pathnames.containsKey(offset))
                    rec.setPathname(pathnames.get(offset));
                else
                    throw new NITFSRuntimeException("NITFSReader.CorrespondingPathnameWasNotFound");
            }
        }
    }

    public static class RPFFrameFileIndexRecord {
        private final int boundaryRectangleRecordNumber;
        private final int frameLocationRowNumber;
        private final int frameLocationColumnNumber;
        private final long pathnameRecordOffset;
        private final String frameFileName;
        private final String geoLocation;
        private final String securityClass;
        private final String securityCountryCode;
        private final String securityReleaseMark;
        private String pathname;   // this field is not part of the NITFS spec

        public RPFFrameFileIndexRecord(ByteBuffer buffer) {
            this.boundaryRectangleRecordNumber = NITFSUtil.getUShort(buffer);
            this.frameLocationRowNumber = NITFSUtil.getUShort(buffer);
            this.frameLocationColumnNumber = NITFSUtil.getUShort(buffer);
            this.pathnameRecordOffset = NITFSUtil.getUInt(buffer);
            this.frameFileName = NITFSUtil.getString(buffer, 12);
            this.geoLocation = NITFSUtil.getString(buffer, 6);
            this.securityClass = NITFSUtil.getString(buffer, 1);
            this.securityCountryCode = NITFSUtil.getString(buffer, 2);
            this.securityReleaseMark = NITFSUtil.getString(buffer, 2);
            this.pathname = "";
        }

        public int getBoundaryRectangleRecordNumber() {
            return boundaryRectangleRecordNumber;
        }

        public int getFrameLocationRowNumber() {
            return frameLocationRowNumber;
        }

        public int getFrameLocationColumnNumber() {
            return frameLocationColumnNumber;
        }

        public String getFrameFileName() {
            return frameFileName;
        }

        public String getGeoLocation() {
            return geoLocation;
        }

        public String getSecurityClass() {
            return securityClass;
        }

        public String getSecurityCountryCode() {
            return securityCountryCode;
        }

        public String getSecurityReleaseMark() {
            return securityReleaseMark;
        }

        public long getPathnameRecordOffset() {
            return pathnameRecordOffset;
        }

        public String getPathname() {
            return pathname;
        }

        public void setPathname(String pathname) {
            this.pathname = pathname;
        }
    }
}
