/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.formats.rpf;

import gov.nasa.worldwind.formats.nitfs.*;

import java.awt.image.*;
import java.io.*;

/**
 * @author lado
 * @version $Id: RPFImageFile.java 1171 2013-02-11 21:45:02Z dcollins $
 */
public class RPFImageFile extends RPFFile {
    private NITFSImageSegment imageSegment = null;
    private UserDefinedImageSubheader imageSubheader = null;
    private RPFFrameFileComponents rpfFrameFileComponents = null;

    private RPFImageFile(File rpfFile) throws IOException, NITFSRuntimeException {
        super(rpfFile);

        this.imageSegment = (NITFSImageSegment) this.getNITFSSegment(NITFSSegmentType.IMAGE_SEGMENT);
        this.validateRPFImage();

        this.imageSubheader = this.imageSegment.getUserDefinedImageSubheader();
        this.rpfFrameFileComponents = this.imageSubheader.getRPFFrameFileComponents();
    }

    public static RPFImageFile load(File rpfFile) throws IOException, NITFSRuntimeException {
        return new RPFImageFile(rpfFile);
    }

    public RPFFrameFileComponents getRPFFrameFileComponents() {
        return this.rpfFrameFileComponents;
    }

    public UserDefinedImageSubheader getImageSubheader() {
        return this.imageSubheader;
    }

    public NITFSImageSegment getImageSegment() {
        return this.imageSegment;
    }

    private void validateRPFImage() throws NITFSRuntimeException {
        if (null == this.imageSegment)
            throw new NITFSRuntimeException("NITFSReader.ImageSegmentWasNotFound");
        if (null == this.imageSegment.getUserDefinedImageSubheader())
            throw new NITFSRuntimeException("NITFSReader.UserDefinedImageSubheaderWasNotFound");
        if (null == this.imageSegment.getUserDefinedImageSubheader().getRPFFrameFileComponents())
            throw new NITFSRuntimeException(
                "NITFSReader.RPFFrameFileComponentsWereNotFoundInUserDefinedImageSubheader");
    }

    public int[] getImagePixelsAsArray(int[] dest, RPFImageType imageType) {
        this.getImageSegment().getImagePixelsAsArray(dest, imageType);
        return dest;
    }

    public BufferedImage getBufferedImage() {
        if (null == this.imageSegment)
            return null;

        BufferedImage bimage = new BufferedImage(
            this.getImageSegment().numSignificantCols,
            this.getImageSegment().numSignificantRows,
            BufferedImage.TYPE_INT_ARGB);

        WritableRaster raster = bimage.getRaster();
        DataBufferInt dataBuffer = (DataBufferInt) raster.getDataBuffer();

//        IntBuffer buffer = IntBuffer.wrap(dataBuffer.getData());
        int[] buffer = dataBuffer.getData();
        this.getImageSegment().getImagePixelsAsArray(buffer, RPFImageType.IMAGE_TYPE_ALPHA_RGB);
        return bimage;
    }

    public boolean hasTransparentAreas() {
        //noinspection SimplifiableIfStatement
        if (null != this.imageSegment)
            return (this.imageSegment.hasTransparentPixels() || this.imageSegment.hasMaskedSubframes());
        return false;
    }
}
