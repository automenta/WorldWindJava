/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */

package gov.nasa.worldwind.formats.rpf;

import java.nio.ByteBuffer;

/**
 * @author Lado Garakanidze
 * @version $Id: RPFFileComponents.java 1171 2013-02-11 21:45:02Z dcollins $
 */
public class RPFFileComponents {
    private final ByteBuffer buffer;

    private final RPFHeaderSection headerSection;
    private final RPFLocationSection locationSection;

    public RPFFileComponents(ByteBuffer buffer) {
        this.buffer = buffer;
        this.headerSection = new RPFHeaderSection(buffer);

        buffer.position(this.headerSection.locationSectionLocation);
        this.locationSection = new RPFLocationSection(buffer);
    }

    public RPFHeaderSection getRPFHeaderSection() {
        return this.headerSection;
    }

    public RPFFrameFileIndexSection getRPFFrameFileIndexSection() {
        if (0 < locationSection.getFrameFileIndexSectionSubheaderLength()) {
            this.buffer.position(locationSection.getFrameFileIndexSectionSubheaderLocation());
            return new RPFFrameFileIndexSection(buffer);
        }
        return null;
    }

    public RPFBoundingRectangleSection getRPFBoundingRectangleSection() {
        if (0 < locationSection.getBoundaryRectangleSectionSubheaderLength()) {
            this.buffer.position(locationSection.getBoundaryRectangleSectionSubheaderLocation());
            return new RPFBoundingRectangleSection(buffer);
        }
        return null;
    }
}
