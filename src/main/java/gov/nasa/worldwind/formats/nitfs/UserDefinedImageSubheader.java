/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */

package gov.nasa.worldwind.formats.nitfs;

import gov.nasa.worldwind.formats.rpf.RPFFrameFileComponents;

import java.nio.ByteBuffer;

/**
 * @author Lado Garakanidze
 * @version $Id: UserDefinedImageSubheader.java 1171 2013-02-11 21:45:02Z dcollins $
 */
public class UserDefinedImageSubheader {
    private final short overflow;
    private final String tag;
    private final int dataLength;
    private RPFFrameFileComponents rpfFrameFileComponents;

    public UserDefinedImageSubheader(ByteBuffer buffer) throws NITFSRuntimeException {
        this.overflow = NITFSUtil.getShortNumeric(buffer, 3);
        this.tag = NITFSUtil.getString(buffer, 6);
        this.dataLength = NITFSUtil.getShortNumeric(buffer, 5);

        if (0 < this.dataLength) {
            if (RPFFrameFileComponents.DATA_TAG.equals(tag))
                this.rpfFrameFileComponents = new RPFFrameFileComponents(buffer);
            else
                throw new NITFSRuntimeException("NITFSReader.UnknownOrUnsupportedUserDefinedImageSubheader");
        }
    }

    public short getOverflow() {
        return this.overflow;
    }

    public String getTag() {
        return this.tag;
    }

    public int getDataLength() {
        return this.dataLength;
    }

    public RPFFrameFileComponents getRPFFrameFileComponents() {
        return this.rpfFrameFileComponents;
    }
}
