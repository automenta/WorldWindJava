/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.layers.rpf.wizard;

/**
 * @author dcollins
 * @version $Id: SizeFormatter.java 1171 2013-02-11 21:45:02Z dcollins $
 */
public enum SizeFormatter {
    ;
    private static final long ONE_GIGABYTE = 1L << 30L;
    private static final long ONE_MEGABYTE = 1L << 20L;
    private static final long ONE_KILOBYTE = 1L << 10L;

    private static long[] bytesToGbMbKbB(long bytes) {
        return new long[] {
            (long) (Math.floor(bytes / (double) SizeFormatter.ONE_GIGABYTE) % 1024.0d),
            (long) (Math.floor(bytes / (double) SizeFormatter.ONE_MEGABYTE) % 1024.0d),
            (long) (Math.floor(bytes / (double) SizeFormatter.ONE_KILOBYTE) % 1024.0d),
            (long) (bytes % 1024.0d)};
    }

    public static String formatPrecise(long bytes) {
        long[] GbMbKbB = SizeFormatter.bytesToGbMbKbB(bytes);
        return String.format("%dGB %dMB %dKB %dbytes", GbMbKbB[0], GbMbKbB[1], GbMbKbB[2], GbMbKbB[3]);
    }

    public static String formatEstimate(long bytes) {
        String result;

        double Gb = bytes / (double) SizeFormatter.ONE_GIGABYTE;
        double Mb = bytes / (double) SizeFormatter.ONE_MEGABYTE;
        double Kb = bytes / (double) SizeFormatter.ONE_KILOBYTE;
        // Size in Giga-bytes.
        if (Gb >= 1) {
            result = String.format("%.2f GB", Gb);
        }
        // Size in Mega-bytes.
        else if (Mb >= 1) {
            result = String.format("%.0f MB", Mb);
        }
        // Size in Kilo-bytes.
        else if (Kb >= 1) {
            result = String.format("%.0f KB", Kb);
        }
        // Size in bytes.
        else {
            result = String.format("%d bytes", bytes);
        }

        return result;
    }
}
