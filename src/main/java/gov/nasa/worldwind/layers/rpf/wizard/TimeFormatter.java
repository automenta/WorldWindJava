/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.layers.rpf.wizard;

/**
 * @author dcollins
 * @version $Id: TimeFormatter.java 1171 2013-02-11 21:45:02Z dcollins $
 */
public enum TimeFormatter {
    ;
    private static final long ONE_HOUR = 60L * 60L * 1000L;
    private static final long ONE_MINUTE = 60L * 1000L;
    private static final long ONE_SECOND = 1000L;

    private static long[] millisToHMS(long millis) {
        return new long[] {
            (long) (Math.floor(millis / (double) TimeFormatter.ONE_HOUR) % 60.0d),  // hours
            (long) (Math.floor(millis / (double) TimeFormatter.ONE_MINUTE) % 60.0d),  // minutes
            (long) (Math.floor(millis / (double) TimeFormatter.ONE_SECOND) % 60.0d)}; // seconds
    }

    public static String formatPrecise(long millis) {
        long[] hms = TimeFormatter.millisToHMS(millis);
        return String.format("%02d:%02d:%02d", hms[0], hms[1], hms[2]);
    }

    public static String formatEstimate(long millis) {
        String result;

        // Less than a minute.
        if (millis < TimeFormatter.ONE_MINUTE) {
            result = "less than 1 minute";
        }
        // Report time in one-minute increments.
        else if (millis < 10L * TimeFormatter.ONE_MINUTE) {
            millis = TimeFormatter.ONE_MINUTE * Math.round(millis / (double) TimeFormatter.ONE_MINUTE);
            long m = millis / TimeFormatter.ONE_MINUTE;
            result = "about " + m + (m > 1 ? " minutes" : " minute");
        }
        // Report time in ten-minute increments.
        else if (millis < 55L * TimeFormatter.ONE_MINUTE) {
            millis = 10L * TimeFormatter.ONE_MINUTE * Math.round(millis / (10.0d * TimeFormatter.ONE_MINUTE));
            long m = millis / TimeFormatter.ONE_MINUTE;
            result = "about " + m + " minutes";
        }
        // Report time in half-hour increments.
        else {
            millis = 30L * TimeFormatter.ONE_MINUTE * Math.round(millis / (30.0d * TimeFormatter.ONE_MINUTE));
            long h = millis / TimeFormatter.ONE_HOUR;
            result = "about " + h + (h > 1 ? " hours" : " hour");
            long m = (millis / TimeFormatter.ONE_MINUTE) % 60L;
            if (m > 0)
                result += " " + m + " minutes";
        }

        return result;
    }
}
