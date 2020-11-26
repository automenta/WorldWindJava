/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */

package gov.nasa.worldwind.formats.dds;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;

/**
 * @author Lado Garakanidze
 * @version $Id: DXTDecompressor.java 1171 2013-02-11 21:45:02Z dcollins $
 */

public interface DXTDecompressor {
    /**
     * Decompress DXT1, DXT3 and DXT3A encoded rasters
     *
     * @param buffer The buffer to decompress.
     * @param width  must be a positive and power of two (4, 8, 16, 32, 64, 128, 512, etc )
     * @param height must be a positive and power of two (4, 8, 16, 32, 64, 128, 512, etc )
     * @return java.awt.image.BufferedImage instance
     * @throws IllegalArgumentException if any input parameter is null or invalid
     */
    BufferedImage decompress(ByteBuffer buffer, int width, int height)
        throws IllegalArgumentException;
}
