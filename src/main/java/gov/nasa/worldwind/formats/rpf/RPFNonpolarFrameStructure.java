/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.formats.rpf;

import gov.nasa.worldwind.util.Logging;

/**
 * @author dcollins
 * @version $Id: RPFNonpolarFrameStructure.java 1171 2013-02-11 21:45:02Z dcollins $
 */
class RPFNonpolarFrameStructure extends RPFFrameStructure {
    private final int northSouthPixelConstant;
    private final int eastWestPixelConstant;
    private final double polewardExtent;
    private final double equatorwardExtent;
    private final int latitudinalFrames;
    private final int longitudinalFrames;

    private RPFNonpolarFrameStructure(
        int northSouthPixelConstant, int eastWestPixelConstant,
        double polewardExtent, double equatorwardExtent,
        int latitudinalFrames, int longitudinalFrames) {
        this.northSouthPixelConstant = northSouthPixelConstant;
        this.eastWestPixelConstant = eastWestPixelConstant;
        this.polewardExtent = polewardExtent;
        this.equatorwardExtent = equatorwardExtent;
        this.latitudinalFrames = latitudinalFrames;
        this.longitudinalFrames = longitudinalFrames;
    }

    public static RPFNonpolarFrameStructure computeStructure(char zoneCode, String rpfDataType, double resolution) {
        if (!RPFZone.isZoneCode(zoneCode)) {
            String message = Logging.getMessage("RPFZone.UnknownZoneCode", zoneCode);
            Logging.logger().fine(message);
            throw new IllegalArgumentException(message);
        }
        if (rpfDataType == null || !RPFDataSeries.isRPFDataType(rpfDataType)) {
            String message = Logging.getMessage("RPFDataSeries.UnkownDataType", rpfDataType);
            Logging.logger().fine(message);
            throw new IllegalArgumentException(message);
        }
        if (resolution < 0) {
            String message = Logging.getMessage("generic.ArgumentOutOfRange", rpfDataType);
            Logging.logger().fine(message);
            throw new IllegalArgumentException(message);
        }

        // Constant zone properties.

        int ewPixelSpacingConst = RPFFrameStructure.eastWestPixelSpacingConstant(zoneCode);
        int nsPixelSpacingConst = RPFFrameStructure.northSouthPixelSpacingConstant();
        int equatorwardNominalBound = RPFFrameStructure.equatorwardNominalBoundary(zoneCode);
        int polewardNominalBound = RPFFrameStructure.polewardNominalBoundary(zoneCode);

        // Scale/GSD specific zone properties.

        int nsPixelConst, ewPixelConst;
        if (RPFDataSeries.isCADRGDataType(rpfDataType)) {
            nsPixelConst = RPFNonpolarFrameStructure.northSouthPixelConstant_CADRG(nsPixelSpacingConst, resolution);
            ewPixelConst = RPFNonpolarFrameStructure.eastWestPixelConstant_CADRG(ewPixelSpacingConst, resolution);
        } else if (RPFDataSeries.isCIBDataType(rpfDataType)) {
            nsPixelConst = RPFNonpolarFrameStructure.northSouthPixelConstant_CIB(nsPixelSpacingConst, resolution);
            ewPixelConst = RPFNonpolarFrameStructure.eastWestPixelConstant_CIB(ewPixelSpacingConst, resolution);
        } else {
            nsPixelConst = -1;
            ewPixelConst = -1;
        }

        double polewardExtent = RPFNonpolarFrameStructure.polewardExtent(polewardNominalBound, nsPixelConst,
            RPFFrameStructure.PIXEL_ROWS_PER_FRAME);
        double equatorwardExtent = RPFNonpolarFrameStructure.equatorwardExtent(equatorwardNominalBound, nsPixelConst,
            RPFFrameStructure.PIXEL_ROWS_PER_FRAME);

        int latFrames = RPFNonpolarFrameStructure.latitudinalFrames(polewardExtent, equatorwardExtent, nsPixelConst,
            RPFFrameStructure.PIXEL_ROWS_PER_FRAME);
        int lonFrames = RPFNonpolarFrameStructure.longitudinalFrames(ewPixelConst, RPFFrameStructure.PIXEL_ROWS_PER_FRAME);

        return new RPFNonpolarFrameStructure(
            nsPixelConst, ewPixelConst,
            polewardExtent, equatorwardExtent,
            latFrames, lonFrames);
    }

    /* [Section 60.1.1, MIL-C-89038] */
    private static int northSouthPixelConstant_CADRG(double northSouthPixelConstant, double scale) {
        double S = 1000000.0d / scale;
        double tmp = northSouthPixelConstant * S;
        tmp = 512.0d * (int) Math.ceil(tmp / 512.0d);
        tmp /= 4.0d;
        tmp /= (150.0d / 100.0d);
        return 256 * (int) Math.round(tmp / 256.0d);
    }

    /* [Section 60.1.2, MIL-C-89038] */
    private static int eastWestPixelConstant_CADRG(double eastWestPixelSpacingConstant, double scale) {
        double S = 1000000.0d / scale;
        double tmp = eastWestPixelSpacingConstant * S;
        tmp = 512.0d * (int) Math.ceil(tmp / 512.0d);
        tmp /= (150.0d / 100.0d);
        return 256 * (int) Math.round(tmp / 256.0d);
    }

    /* [Section A.5.1.1, MIL-PRF-89041A] */
    private static int northSouthPixelConstant_CIB(double northSouthPixelSpacingConstant,
        double groundSampleDistance) {
        double S = 100.0d / groundSampleDistance;
        double tmp = northSouthPixelSpacingConstant * S;
        tmp = 512.0d * (int) Math.ceil(tmp / 512.0d);
        tmp /= 4.0d;
        return 256 * (int) Math.round(tmp / 256.0d);
    }

    /* [Section A.5.1.1, MIL-PRF-89041A] */
    private static int eastWestPixelConstant_CIB(double eastWestPixelSpacingConstant,
        double groundSampleDistance) {
        double S = 100.0d / groundSampleDistance;
        double tmp = eastWestPixelSpacingConstant * S;
        return 512 * (int) Math.ceil(tmp / 512.0d);
    }

    /* [Section 60.1.5.b, MIL-C-89038] */
    /* [Section A.5.1.2.b, MIL-PRF-89041A] */
    private static double polewardExtent(double polewardNominalBoundary, double northSouthPixelConstant,
        double pixelRowsPerFrame) {
        double nsPixelsPerDegree = northSouthPixelConstant / 90.0d;
        return Math.signum(polewardNominalBoundary)
            * RPFNonpolarFrameStructure.clamp(Math.ceil(nsPixelsPerDegree * Math.abs(polewardNominalBoundary) / pixelRowsPerFrame)
            * pixelRowsPerFrame / nsPixelsPerDegree, 0, 90);
    }

    /* [Section 60.1.5.c, MIL-C-89038] */
    /* [Section A.5.1.2.c, MIL-PRF-89041A] */
    private static double equatorwardExtent(double equatorwardNominalBoundary, double northSouthPixelConstant,
        double pixelRowsPerFrame) {
        double nsPixelsPerDegree = northSouthPixelConstant / 90.0d;
        return Math.signum(equatorwardNominalBoundary)
            * RPFNonpolarFrameStructure.clamp((int) (nsPixelsPerDegree * Math.abs(equatorwardNominalBoundary) / pixelRowsPerFrame)
            * pixelRowsPerFrame / nsPixelsPerDegree, 0, 90);
    }

    /* [Section 60.1.6, MIL-PRF-89038] */
    /* [Section A.5.1.3, MIL-PRF-89041A] */
    private static int latitudinalFrames(double polewardExtentDegrees, double equatorwardExtentDegrees,
        double northSouthPixelConstant, double pixelRowsPerFrame) {
        double nsPixelsPerDegree = northSouthPixelConstant / 90.0d;
        double extent = Math.abs(polewardExtentDegrees - equatorwardExtentDegrees);
        return (int) Math.round(extent * nsPixelsPerDegree / pixelRowsPerFrame);
    }

    /* [Section 60.1.7, MIL-C-89038] */
    /* [Section A.5.1.4, MIL-PRF-89041A] */
    private static int longitudinalFrames(double eastWestPixelConstant, double pixelRowsPerFrame) {
        return (int) Math.ceil(eastWestPixelConstant / pixelRowsPerFrame);
    }

    private static double clamp(double x, double min, double max) {
        return (x < min) ? min : (Math.min(x, max));
    }

    public final int getNorthSouthPixelConstant() {
        return this.northSouthPixelConstant;
    }

    public final int getEastWestPixelConstant() {
        return this.eastWestPixelConstant;
    }

    public final double getPolewardExtent() {
        return this.polewardExtent;
    }

    public final double getEquatorwardExtent() {
        return this.equatorwardExtent;
    }

    public final int getLatitudinalFrames() {
        return this.latitudinalFrames;
    }

    public final int getLongitudinalFrames() {
        return this.longitudinalFrames;
    }
}
