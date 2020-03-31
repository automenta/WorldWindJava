/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */

package gov.nasa.worldwind.formats.rpf;

import gov.nasa.worldwind.formats.nitfs.NITFSUtil;

/**
 * @author Lado Garakanidze
 * @version $Id: RPFHeaderSection.java 1171 2013-02-11 21:45:02Z dcollins $
 */
class RPFHeaderSection
{
    public static final String DATA_TAG = "RPFHDR";

    public final boolean  endianIndicator;
    public final short    headerLength;
    public final String   filename;
    public final short    updateIndicator; // new | replacement | update
    public final String   govSpecNumber;
    public final String   govSpecDate;
    public final String   securityClass;
    public final String   securityCountryCode;
    public final String   securityReleaseMark;
    public final int      locationSectionLocation;

    public RPFHeaderSection(java.nio.ByteBuffer buffer)
    {
        this.endianIndicator = ((byte) 0 != buffer.get());         // reads 1 byte, 0 for big endian
        this.headerLength = buffer.getShort();                     // reads 2 bytes
        this.filename = NITFSUtil.getString(buffer, 12);
        this.updateIndicator = NITFSUtil.getByteAsShort(buffer);       // reads 1 byte (short)
        this.govSpecNumber = NITFSUtil.getString(buffer, 15);
        this.govSpecDate = NITFSUtil.getString(buffer, 8);
        this.securityClass = NITFSUtil.getString(buffer, 1);
        this.securityCountryCode = NITFSUtil.getString(buffer, 2);
        this.securityReleaseMark = NITFSUtil.getString(buffer, 2);
        this.locationSectionLocation = buffer.getInt();            // read 4 bytes (int)
    }
}
