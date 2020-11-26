/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */

package gov.nasa.worldwind.formats.rpf;

import gov.nasa.worldwind.formats.nitfs.*;
import gov.nasa.worldwind.geom.LatLon;

import java.nio.ByteBuffer;

/**
 * @author lado
 * @version $Id: RPFFrameFileComponents.java 1171 2013-02-11 21:45:02Z dcollins $
 */
public class RPFFrameFileComponents {
    public static final String DATA_TAG = "RPFIMG";

    // [ rpf location section ]
    public final RPFLocationSection componentLocationTable;

    // [ rpf coverage section ]
    public LatLon nwUpperLeft, swLowerleft, neUpperRight, seLowerRight;
    public double verticalResolutionNorthSouth;
    public double horizontalResolutionEastWest;
    public double verticalIntervalLatitude;
    public double horizontalIntervalLongitude;

    // [ color / grayscale section ]
    public RPFColorMap[] rpfColorMaps;

    // [ rpf color / grayscale section ]
    public short numOfColorGrayscaleOffsetRecords;
    public short numOfColorConverterOffsetRecords;
    public String externalColorGrayscaleFilename;
    // [ rpf colormap subsection ]
    public long colormapOffsetTableOffset;
    public int colormapGrayscaleOffsetRecordLength;

    // [ rpf color converter subsection ]

    // [ rpf image description subheader ]
    public int numOfSpectralGroups;
    public int numOfSubframeTables;
    public int numOfSpectralBandTables;
    public int numOfSpectralBandLinesPerImageRow;
    public int numOfSubframesInEastWestDirection;
    public int numOfSubframesInNorthSouthDirection;
    public long numOfOutputColumnsPerSubframe;
    public long numOfOutputRowsPerSubframe;
    public long subframeMaskTableOffset;
    public long transparencyMaskTableOffset;

    // [ rpf related images section ]
    public RelatedImagesSection relatedImagesSection = null;

    public RPFFrameFileComponents(ByteBuffer buffer) {
        this.componentLocationTable = new RPFLocationSection(buffer);

        if (0 < this.componentLocationTable.getCoverageSectionSubheaderLength())
            this.parseRPFCoverageSection(buffer);

        if (0 < this.componentLocationTable.getColorGrayscaleSectionSubheaderLength())
            this.parseColorGrayscaleSection(buffer);

        if (0 < this.componentLocationTable.getColormapSubsectionLength())
            this.parseColormapSubSection(buffer);

        if (0 < this.componentLocationTable.getColorConverterSubsectionLength())
            this.parseColorConverterSubsection(buffer);

        if (0 < this.componentLocationTable.getImageDescriptionSubheaderLength()) {
            buffer.position(this.componentLocationTable.getImageDescriptionSubheaderLocation());
            this.parseImageDescriptionSubheader(buffer);
        }
        if (0 < this.componentLocationTable.getRelatedImagesSectionSubheaderLength()) {
            buffer.position(this.componentLocationTable.getRelatedImagesSectionSubheaderLocation());
            this.relatedImagesSection = new RelatedImagesSection(buffer);
        }
    }

    private void parseImageDescriptionSubheader(ByteBuffer buffer) {
        this.numOfSpectralGroups = NITFSUtil.getUShort(buffer);
        this.numOfSubframeTables = NITFSUtil.getUShort(buffer);
        this.numOfSpectralBandTables = NITFSUtil.getUShort(buffer);
        this.numOfSpectralBandLinesPerImageRow = NITFSUtil.getUShort(buffer);
        this.numOfSubframesInEastWestDirection = NITFSUtil.getUShort(buffer);
        this.numOfSubframesInNorthSouthDirection = NITFSUtil.getUShort(buffer);
        this.numOfOutputColumnsPerSubframe = NITFSUtil.getUInt(buffer);
        this.numOfOutputRowsPerSubframe = NITFSUtil.getUInt(buffer);
        this.subframeMaskTableOffset = NITFSUtil.getUInt(buffer);
        this.transparencyMaskTableOffset = NITFSUtil.getUInt(buffer);
    }

    private void parseColorConverterSubsection(ByteBuffer buffer) {
        buffer.position(this.componentLocationTable.getColorConverterSubsectionLocation());
//        if (0 < this.numOfColorConverterOffsetRecords)
//            throw new NITFSRuntimeException("NITFSReader.NotImplemented.ColorConvertorSubsectionReader");
    }

    private void parseColormapSubSection(ByteBuffer buffer) {
        buffer.position(this.componentLocationTable.getColormapSubsectionLocation());

        this.colormapOffsetTableOffset = NITFSUtil.getUInt(buffer);
        this.colormapGrayscaleOffsetRecordLength = NITFSUtil.getUShort(buffer);
        // read color / grayscale AND histogram records; builds a ColorMap (LUT)
        if (0 < this.numOfColorGrayscaleOffsetRecords) {
            rpfColorMaps = new RPFColorMap[this.numOfColorGrayscaleOffsetRecords];
            for (int i = 0; i < this.numOfColorGrayscaleOffsetRecords; i++) {
                rpfColorMaps[i] = new RPFColorMap(buffer, this.componentLocationTable.getColormapSubsectionLocation());
            }
        }
        else
            throw new NITFSRuntimeException("NITFSReader.InvalidNumberOfRPFColorGrayscaleRecords");
    }

    private void parseColorGrayscaleSection(ByteBuffer buffer) {
        buffer.position(this.componentLocationTable.getColorGrayscaleSectionSubheaderLocation());

        this.numOfColorGrayscaleOffsetRecords = NITFSUtil.getByteAsShort(buffer);
        this.numOfColorConverterOffsetRecords = NITFSUtil.getByteAsShort(buffer);
        this.externalColorGrayscaleFilename = NITFSUtil.getString(buffer, 12);
    }

    private void parseRPFCoverageSection(ByteBuffer buffer) {
        buffer.position(this.componentLocationTable.getCoverageSectionSubheaderLocation());

        this.nwUpperLeft = LatLon.fromDegrees(buffer.getDouble(), buffer.getDouble());
        this.swLowerleft = LatLon.fromDegrees(buffer.getDouble(), buffer.getDouble());
        this.neUpperRight = LatLon.fromDegrees(buffer.getDouble(), buffer.getDouble());
        this.seLowerRight = LatLon.fromDegrees(buffer.getDouble(), buffer.getDouble());

        this.verticalResolutionNorthSouth = buffer.getDouble();
        this.horizontalResolutionEastWest = buffer.getDouble();
        this.verticalIntervalLatitude = buffer.getDouble();
        this.horizontalIntervalLongitude = buffer.getDouble();
    }

    public static class RelatedImagesSection {
        // [ rpf related images section subheader ]
        public final long relatedImageDescriptionTableOffset;
        public final int numOfRelatedImageDescriptionRecords;
        public final int relatedImageDescriptionRecordLength;

        public RelatedImagesSection(ByteBuffer buffer) {
            this.relatedImageDescriptionTableOffset = NITFSUtil.getUInt(buffer);
            this.numOfRelatedImageDescriptionRecords = NITFSUtil.getUShort(buffer);
            this.relatedImageDescriptionRecordLength = NITFSUtil.getUShort(buffer);
        }
    }
}
