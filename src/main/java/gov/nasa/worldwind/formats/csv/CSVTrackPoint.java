/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.formats.csv;

import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.tracks.TrackPoint;
import gov.nasa.worldwind.util.Logging;

/**
 * @author tag
 * @version $Id: CSVTrackPoint.java 1171 2013-02-11 21:45:02Z dcollins $
 */
public class CSVTrackPoint implements TrackPoint {
    String time = "";
    private double latitude;
    private double longitude;
    private double altitude;

    /**
     * @param words The point coordinate values.
     * @throws IllegalArgumentException if <code>words</code> is null or has length less than 1
     */
    public CSVTrackPoint(String[] words) {
        if (words == null) {
            String msg = Logging.getMessage("nullValue.ArrayIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }
        if (words.length < 2) {
            String msg = Logging.getMessage("generic.ArrayInvalidLength", words.length);
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        this.doValues(words);
    }

    private void doValues(String[] words) {
        this.latitude = CSVTrackPoint.parseLatitude(words[1]);
        this.longitude = CSVTrackPoint.parseLongitude(words[2]);
        if (words.length > 3)
            this.altitude = parseElevation(words[3], "M");
    }

    private static double parseLatitude(String angle) {
        return angle.isEmpty() ? 0 : Double.parseDouble(angle);
    }

    private static double parseLongitude(String angle) {
        return angle.isEmpty() ? 0 : Double.parseDouble(angle);
    }

    private static double parseElevation(String alt, String units) {
        return alt.isEmpty() ? 0 : Double.parseDouble(alt) * unitsToMeters(units);
    }

    private static double unitsToMeters(String units) {
        // meters
// feet
        // fathoms

        return switch (units) {
            case "f" -> 3.2808399;
            case "F" -> 0.5468066528;
            default -> 1.0d;
        };
    }

    public double getLatitude() {
        return latitude;
    }

    /**
     * @param latitude The latitude value.
     * @throws IllegalArgumentException if <code>latitude</code> is less than -90 or greater than 90
     */
    public void setLatitude(double latitude) {
        if (latitude > 90 || latitude < -90) {
            String msg = Logging.getMessage("generic.AngleOutOfRange", latitude);
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    /**
     * @param longitude The new longitude value.
     * @throws IllegalArgumentException if <code>longitude</code> is less than -180 or greater than 180
     */
    public void setLongitude(double longitude) {
        if (longitude > 180 || longitude < -180) {
            String msg = Logging.getMessage("generic.AngleOutOfRange", longitude);
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        this.longitude = longitude;
    }

    public Position getPosition() {
        return Position.fromDegrees(this.latitude, this.longitude, this.altitude);
    }

    public void setPosition(Position position) {
        if (position == null) {
            String msg = Logging.getMessage("nullValue.PositionIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        this.latitude = position.getLatitude().getDegrees();
        this.longitude = position.getLongitude().getDegrees();
        this.altitude = position.getElevation();
    }

    public double getElevation() {
        return this.altitude;
    }

    public void setElevation(double elevation) {
        this.altitude = elevation;
    }

    public String getTime() {
        return null;
    }

    public void setTime(String time) {
        this.time = time;
    }

    @Override
    public String toString() {
        return String.format("(%10.8f\u00B0, %11.8f\u00B0, %10.4g m, %s)", this.latitude, this.longitude,
            this.altitude, this.time);
    }
}
