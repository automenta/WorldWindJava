/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */

package gov.nasa.worldwind.formats.nitfs;

import java.nio.ByteBuffer;

/**
 * @author Lado Garakanidze
 * @version $Id: NITFSReservedExtensionSegment.java 1171 2013-02-11 21:45:02Z dcollins $
 */
public class NITFSReservedExtensionSegment extends NITFSSegment {
    public NITFSReservedExtensionSegment(ByteBuffer buffer, int headerStartOffset, int headerLength,
        int dataStartOffset, int dataLength) {
        super(NITFSSegmentType.RESERVED_EXTENSION_SEGMENT, buffer, headerStartOffset, headerLength, dataStartOffset,
            dataLength);

        this.restoreBufferPosition();
    }
}
