/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.formats.nmea;

import gov.nasa.worldwind.tracks.*;
import gov.nasa.worldwind.util.Logging;

import java.io.*;

/**
 * @author dcollins
 * @version $Id: NmeaWriter.java 1171 2013-02-11 21:45:02Z dcollins $
 */
public class NmeaWriter {
    private static final String DEFAULT_ENCODING = "US-ASCII";
    private final PrintStream printStream;
    private final String encoding;
    @SuppressWarnings("UnusedDeclaration")
    private int sentenceNumber;

    public NmeaWriter(String path) throws UnsupportedEncodingException, FileNotFoundException {
        this(path, NmeaWriter.DEFAULT_ENCODING);
    }

    public NmeaWriter(String path, String encoding) throws UnsupportedEncodingException, FileNotFoundException {
        if (path == null) {
            String msg = Logging.getMessage("nullValue.PathIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }
        if (encoding == null) {
            String msg = Logging.getMessage("nullValue.StringIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        this.encoding = encoding;
        this.printStream = new PrintStream(
            new BufferedOutputStream(new FileOutputStream(path)),
            false, // Disable autoflush.
            this.encoding); // Character mapping from 16-bit UTF characters to bytes.
    }

    public NmeaWriter(OutputStream stream) throws UnsupportedEncodingException {
        this(stream, NmeaWriter.DEFAULT_ENCODING);
    }

    public NmeaWriter(OutputStream stream, String encoding) throws UnsupportedEncodingException {
        if (stream == null) {
            String msg = Logging.getMessage("nullValue.InputStreamIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }
        if (encoding == null) {
            String msg = Logging.getMessage("nullValue.StringIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        this.encoding = encoding;
        this.printStream = new PrintStream(
            new BufferedOutputStream(stream),
            false, // Disable autoflush.
            this.encoding); // Character mapping from 16-bit UTF characters to bytes.
    }

    private static String formatTime(String time) {
        // Format time as "HHMMSS"
        return (time != null) ? time : "";
    }

    private static String formatLatitude(double degrees) {
        int d = (int) Math.floor(Math.abs(degrees));
        double m = 60 * (Math.abs(degrees) - d);
        // Format latitude as "DDMM.MMM[N|S]"
        return String.format("%02d%06.3f,%s", d, m, degrees < 0 ? "S" : "N");
    }

    private static String formatLongitude(double degrees) {
        int d = (int) Math.floor(Math.abs(degrees));
        double m = 60 * (Math.abs(degrees) - d);
        // Format longitude as "DDDMM.MMM[N|S]"
        return String.format("%03d%06.3f,%s", d, m, degrees < 0 ? "W" : "E");
    }

    private static String formatElevation(double metersElevation) {
        // Format elevation with 1 digit of precision.
        // This provides decimeter resolution.
        return String.format("%.1f,M", metersElevation);
    }

    private static String formatChecksum(int checksum) {
        return Integer.toHexString(checksum);
    }

    private static int computeChecksum(CharSequence s, int start, int end) {
        int chksum = 0;
        for (int i = start; i < end; i++) {
            int c = 0xFF & s.charAt(i);
            chksum ^= c;
        }
        return chksum;
    }

    public final String getEncoding() {
        return this.encoding;
    }

    public void writeTrack(Track track) {
        if (track == null) {
            String msg = Logging.getMessage("nullValue.TrackIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        doWriteTrack(track, this.printStream);
        doFlush();
    }

    public void close() {
        doFlush();
        this.printStream.close();
    }

    private void doWriteTrack(Track track, PrintStream out) {
        if (track != null && track.getSegments() != null) {
            for (TrackSegment ts : track.getSegments()) {
                doWriteTrackSegment(ts, out);
            }
        }
    }

    private void doWriteTrackSegment(TrackSegment segment, PrintStream out) {
        if (segment != null && segment.getPoints() != null) {
            for (TrackPoint tp : segment.getPoints()) {
                if (tp instanceof NmeaTrackPoint)
                    doWriteNmeaTrackPoint(tp, out);
                else
                    doWriteTrackPoint(tp, out);
            }
        }
    }

    private void doWriteTrackPoint(TrackPoint point, PrintStream out) {
        if (point != null) {
            writeGGASentence(point.getTime(), point.getLatitude(), point.getLongitude(), point.getElevation(), 0, out);
        }
    }

    private void doWriteNmeaTrackPoint(TrackPoint point, PrintStream out) {
        if (point != null) {
            // TODO: separate elevation and geoid-height
            writeGGASentence(point.getTime(), point.getLatitude(), point.getLongitude(), point.getElevation(), 0, out);
        }
    }

    private void writeGGASentence(String time, double lat, double lon, double altitude, double geoidHeight,
        PrintStream out) {
        this.sentenceNumber++;
        // Documentation for NMEA Standard 0183
        // taken from http://www.gpsinformation.org/dale/nmea.htm#GGA
        StringBuilder sb = new StringBuilder();
        sb.append("GP");
        // Global Positioning System Fix Data
        sb.append("GGA");
        sb.append(',');
        // Fix taken at "HHMMSS" UTC
        sb.append(NmeaWriter.formatTime(time));
        sb.append(',');
        // Latitude "DDMM.MMM,[N|S]"
        sb.append(NmeaWriter.formatLatitude(lat));
        sb.append(',');
        // Longitude "DDDMM.MMM,[N|S]"
        sb.append(NmeaWriter.formatLongitude(lon));
        sb.append(',');
        // Fix quality: 0 = invalid
        //              1 = GPS fix (SPS)
        //              2 = DGPS fix
        //              3 = PPS fix
        //              4 = Real Time Kinematic
        //              5 = Float RTK
        //              6 = estimated (dead reckoning) (2.3 feature)
        //              7 = Manual input mode
        //              8 = Simulation mode
        sb.append(',');
        // Number of satellites being tracked
        sb.append(',');
        // Horizontal dilution of position
        sb.append(',');
        // Altitude, Meters, above mean sea level
        sb.append(NmeaWriter.formatElevation(altitude));
        sb.append(',');
        // Height of geoid (mean sea level) above WGS84 ellipsoid
        sb.append(NmeaWriter.formatElevation(geoidHeight));
        sb.append(',');
        // time in seconds since last DGPS update
        sb.append(',');
        // DGPS station ID number
        sb.append(',');
        // the checksum data, always begins with *
        int chksum = NmeaWriter.computeChecksum(sb, 0, sb.length());
        sb.append('*');
        sb.append(NmeaWriter.formatChecksum(chksum));

        out.print("$");
        out.print(sb);
        out.print("\r\n");
        doFlush();
    }

    private void doFlush() {
        this.printStream.flush();
    }
}
