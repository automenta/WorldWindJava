/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */

package gov.nasa.worldwind.geom;

import java.util.Iterator;

/**
 * @author tag
 * @version $Id: GeoQuad.java 1171 2013-02-11 21:45:02Z dcollins $
 */
public class GeoQuad {
    public static final int NORTH = 1;
    public static final int SOUTH = 2;
    public static final int EAST = 4;
    public static final int WEST = 8;
    public static final int NORTHWEST = GeoQuad.NORTH + GeoQuad.WEST;
    public static final int NORTHEAST = GeoQuad.NORTH + GeoQuad.EAST;
    public static final int SOUTHWEST = GeoQuad.SOUTH + GeoQuad.WEST;
    public static final int SOUTHEAST = GeoQuad.SOUTH + GeoQuad.EAST;

    private final LatLon sw, se, ne, nw;
    private final Line northEdge, southEdge, eastEdge, westEdge;

    public GeoQuad(Iterable<? extends LatLon> corners) {
//        if (corners == null) {
//            String message = Logging.getMessage("nullValue.LocationsListIsNull");
//            Logging.logger().severe(message);
//            throw new IllegalArgumentException(message);
//        }

        // Count the corners and check for nulls
        Iterator<? extends LatLon> iter = corners.iterator();

        this.sw = iter.next();
        this.se = iter.next();
        this.ne = iter.next();
        this.nw = iter.next();

        this.northEdge = Line.fromSegment(
            new Vec4(this.nw.getLongitude().degrees, this.nw.getLatitude().degrees, 0),
            new Vec4(this.ne.getLongitude().degrees, this.ne.getLatitude().degrees, 0));
        this.southEdge = Line.fromSegment(
            new Vec4(this.sw.getLongitude().degrees, this.sw.getLatitude().degrees, 0),
            new Vec4(this.se.getLongitude().degrees, this.se.getLatitude().degrees, 0));
        this.eastEdge = Line.fromSegment(
            new Vec4(this.se.getLongitude().degrees, this.se.getLatitude().degrees, 0),
            new Vec4(this.ne.getLongitude().degrees, this.ne.getLatitude().degrees, 0));
        this.westEdge = Line.fromSegment(
            new Vec4(this.sw.getLongitude().degrees, this.sw.getLatitude().degrees, 0),
            new Vec4(this.nw.getLongitude().degrees, this.nw.getLatitude().degrees, 0));
    }

    public LatLon getSw() {
        return sw;
    }

    public LatLon getSe() {
        return se;
    }

    public LatLon getNe() {
        return ne;
    }

    public LatLon getNw() {
        return nw;
    }

    public Angle distanceToNW(LatLon p) {
        return LatLon.rhumbDistance(this.nw, p);
    }

    public Angle distanceToNE(LatLon p) {
        return LatLon.rhumbDistance(this.ne, p);
    }

    public Angle distanceToSW(LatLon p) {
        return LatLon.rhumbDistance(this.sw, p);
    }

    public Angle distanceToSE(LatLon p) {
        return LatLon.rhumbDistance(this.se, p);
    }

    public Angle distanceToNorthEdge(LatLon p) {
        return new Angle(this.northEdge.distanceTo(new Vec4(p.getLongitude().degrees, p.getLatitude().degrees, 0)));
    }

    public Angle distanceToSouthEdge(LatLon p) {
        return new Angle(this.southEdge.distanceTo(new Vec4(p.getLongitude().degrees, p.getLatitude().degrees, 0)));
    }

    public Angle distanceToEastEdge(LatLon p) {
        return new Angle(this.eastEdge.distanceTo(new Vec4(p.getLongitude().degrees, p.getLatitude().degrees, 0)));
    }

    public Angle distanceToWestEdge(LatLon p) {
        return new Angle(this.westEdge.distanceTo(new Vec4(p.getLongitude().degrees, p.getLatitude().degrees, 0)));
    }

    public LatLon interpolate(double t, double s) {
        Line topToBot = Line.fromSegment(
            this.southEdge.getPointAt(s),
            this.northEdge.getPointAt(s));

        Vec4 point = topToBot.getPointAt(t);
        return LatLon.fromDegrees(point.y, point.x);
    }
}
