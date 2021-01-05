/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.geom;

import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.cache.Cacheable;
import gov.nasa.worldwind.geom.coords.UTMCoord;
import gov.nasa.worldwind.globes.Globe;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.tracks.TrackPoint;
import gov.nasa.worldwind.util.*;

import java.awt.geom.*;
import java.util.*;

import static java.lang.Math.*;

/**
 * <code>Sector</code> represents a rectangular region of latitude and longitude. The region is defined by four angles:
 * its minimum and maximum latitude, its minimum and maximum longitude. The angles are assumed to be normalized to +/-
 * 90 degrees latitude and +/- 180 degrees longitude. The minimums and maximums are relative to these ranges, e.g., -80
 * is less than 20. Behavior of the class is undefined for angles outside these ranges. Normalization is not performed
 * on the angles by this class, nor is it verified by the class' methods. See {@link Angle} for a description of
 * specifying angles. <p> <code>Sector</code> instances are immutable. </p>
 *
 * @author Tom Gaskins
 * @version $Id: Sector.java 2397 2014-10-28 17:13:04Z dcollins $
 * @see Angle
 */
public class Sector implements Cacheable, Comparable<Sector>, Iterable<LatLon> {
    /**
     * A <code>Sector</code> of latitude [-90 degrees, + 90 degrees] and longitude [-180 degrees, + 180 degrees].
     */
    public static final Sector FULL_SPHERE = new Sector(Angle.NEG90, Angle.POS90, Angle.NEG180, Angle.POS180);
    public static final Sector EMPTY_SECTOR = new Sector(Angle.ZERO, Angle.ZERO, Angle.ZERO, Angle.ZERO);

    /**
     * in angles, degrees
     */
    public final double latMin;
    public final double latMax;
    public final double lonMin;
    public final double lonMax;
    public final double latDelta;
    public final double lonDelta;

    /**
     * Creates a new <code>Sector</code> and initializes it to the specified angles. The angles are assumed to be
     * normalized to +/- 90 degrees latitude and +/- 180 degrees longitude, but this method does not verify that.
     *
     * @param latMin the sector's minimum latitude.
     * @param latMax the sector's maximum latitude.
     * @param lonMin the sector's minimum longitude.
     * @param lonMax the sector's maximum longitude.
     * @throws IllegalArgumentException if any of the angles are null
     */
    @Deprecated
    public Sector(Angle latMin, Angle latMax, Angle lonMin, Angle lonMax) {
        this(latMin.degrees, latMax.degrees, lonMin.degrees, lonMax.degrees);
    }

    public Sector(Sector s) {
        this(s.latMin, s.latMax, s.lonMin, s.lonMax);
    }

    public Sector(double latMin, double latMax, double lonMin, double lonMax) {
        if (latMin > latMax || lonMin > lonMax)
            throw new IllegalArgumentException();

        this.latMin = latMin;
        this.latMax = latMax;
        this.lonMin = lonMin;
        this.lonMax = lonMax;
        this.latDelta = this.latMax - this.latMin;
        this.lonDelta = this.lonMax - this.lonMin;
    }

    /**
     * Creates a new <code>Sector</code> and initializes it to the specified angles. The angles are assumed to be
     * normalized to +/- 90 degrees latitude and +/- 180 degrees longitude, but this method does not verify that.
     *
     * @param minLatitude  the sector's minimum latitude in degrees.
     * @param maxLatitude  the sector's maximum latitude in degrees.
     * @param minLongitude the sector's minimum longitude in degrees.
     * @param maxLongitude the sector's maximum longitude in degrees.
     * @return the new <code>Sector</code>
     */
    public static Sector fromDegrees(double minLatitude, double maxLatitude, double minLongitude, double maxLongitude) {
        //return new Sector(Angle.fromDegrees(minLatitude), Angle.fromDegrees(maxLatitude), Angle.fromDegrees(minLongitude), Angle.fromDegrees(maxLongitude));
        return new Sector(minLatitude, maxLatitude, minLongitude, maxLongitude);
    }

    /**
     * Creates a new <code>Sector</code> and initializes it to the specified angles. The angles are assumed to be
     * normalized to +/- 90 degrees latitude and +/- 180 degrees longitude, but this method does not verify that.
     *
     * @param minLatitude  the sector's minimum latitude in degrees.
     * @param maxLatitude  the sector's maximum latitude in degrees.
     * @param minLongitude the sector's minimum longitude in degrees.
     * @param maxLongitude the sector's maximum longitude in degrees.
     * @return the new <code>Sector</code>
     */
    public static Sector fromDegreesAndClamp(double minLatitude, double maxLatitude, double minLongitude,
        double maxLongitude) {
        if (minLatitude < -90)
            minLatitude = -90;
        if (maxLatitude > 90)
            maxLatitude = 90;
        if (minLongitude < -180)
            minLongitude = -180;
        if (maxLongitude > 180)
            maxLongitude = 180;

        return new Sector(minLatitude, maxLatitude, minLongitude, maxLongitude);
    }

    /**
     * Creates a new <code>Sector</code> and initializes it to angles in the specified array. The array is assumed to
     * hold four elements containing the Sector's angles, and must be ordered as follows: minimum latitude, maximum
     * latitude, minimum longitude, and maximum longitude. Additionally, the angles are assumed to be normalized to +/-
     * 90 degrees latitude and +/- 180 degrees longitude, but this method does not verify that.
     *
     * @param array the array of angles in degrees.
     * @return he new <code>Sector</code>
     * @throws IllegalArgumentException if <code>array</code> is null or if its length is less than 4.
     */
    public static Sector fromDegrees(double[] array) {

        return Sector.fromDegrees(array[0], array[1], array[2], array[3]);
    }

    /**
     * Creates a new <code>Sector</code> and initializes it to the angles resulting from the given {@link Rectangle2D}
     * in degrees lat-lon coordinates where x corresponds to longitude and y to latitude. The resulting geographic
     * angles are assumed to be normalized to +/- 90 degrees latitude and +/- 180 degrees longitude, but this method
     * does not verify that.
     *
     * @param rectangle the sector's rectangle in degrees lat-lon coordinates.
     * @return the new <code>Sector</code>
     */
    public static Sector fromDegrees(Rectangle2D rectangle) {
        return Sector.fromDegrees(rectangle.getY(), rectangle.getMaxY(), rectangle.getX(), rectangle.getMaxX());
    }

    /**
     * Creates a new <code>Sector</code> and initializes it to the specified angles. The angles are assumed to be
     * normalized to +/- \u03c0/2 radians latitude and +/- \u03c0 radians longitude, but this method does not verify
     * that.
     *
     * @param minLatitude  the sector's minimum latitude in radians.
     * @param maxLatitude  the sector's maximum latitude in radians.
     * @param minLongitude the sector's minimum longitude in radians.
     * @param maxLongitude the sector's maximum longitude in radians.
     * @return the new <code>Sector</code>
     */
    public static Sector fromRadians(double minLatitude, double maxLatitude, double minLongitude,
        double maxLongitude) {
        return new Sector(Angle.fromRadians(minLatitude), Angle.fromRadians(maxLatitude), Angle.fromRadians(
            minLongitude), Angle.fromRadians(maxLongitude));
    }

    /**
     * Returns a geographic Sector which bounds the specified UTM rectangle. The UTM rectangle is located in specified
     * UTM zone and hemisphere.
     *
     * @param zone        the UTM zone.
     * @param hemisphere  the UTM hemisphere, either {@link AVKey#NORTH} or {@link AVKey#SOUTH}.
     * @param minEasting  the minimum UTM easting, in meters.
     * @param maxEasting  the maximum UTM easting, in meters.
     * @param minNorthing the minimum UTM northing, in meters.
     * @param maxNorthing the maximum UTM northing, in meters.
     * @return a Sector that bounds the specified UTM rectangle.
     * @throws IllegalArgumentException if <code>zone</code> is outside the range 1-60, if <code>hemisphere</code> is
     *                                  null, or if <code>hemisphere</code> is not one of {@link AVKey#NORTH} or {@link
     *                                  AVKey#SOUTH}.
     */
    public static Sector fromUTMRectangle(int zone, String hemisphere, double minEasting, double maxEasting,
        double minNorthing, double maxNorthing) {
        if (zone < 1 || zone > 60) {
            String message = Logging.getMessage("generic.ZoneIsInvalid", zone);
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (!AVKey.NORTH.equals(hemisphere) && !AVKey.SOUTH.equals(hemisphere)) {
            String message = Logging.getMessage("generic.HemisphereIsInvalid", hemisphere);
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        LatLon ll = UTMCoord.locationFromUTMCoord(zone, hemisphere, minEasting, minNorthing, null);
        LatLon lr = UTMCoord.locationFromUTMCoord(zone, hemisphere, maxEasting, minNorthing, null);
        LatLon ur = UTMCoord.locationFromUTMCoord(zone, hemisphere, maxEasting, maxNorthing, null);
        LatLon ul = UTMCoord.locationFromUTMCoord(zone, hemisphere, minEasting, maxNorthing, null);

        return Sector.boundingSector(Arrays.asList(ll, lr, ur, ul));
    }

    public static Sector boundingSector(Iterator<TrackPoint> positions) {

        if (!positions.hasNext())
            return Sector.EMPTY_SECTOR;

        TrackPoint position = positions.next();
        double minLat = position.getLatitude();
        double minLon = position.getLongitude();
        double maxLat = minLat;
        double maxLon = minLon;

        while (positions.hasNext()) {
            TrackPoint p = positions.next();
            double lat = p.getLatitude();
            if (lat < minLat)
                minLat = lat;
            else if (lat > maxLat)
                maxLat = lat;

            double lon = p.getLongitude();
            if (lon < minLon)
                minLon = lon;
            else if (lon > maxLon)
                maxLon = lon;
        }

        return Sector.fromDegrees(minLat, maxLat, minLon, maxLon);
    }

    public static Sector boundingSector(Iterable<? extends LatLon> locations) {

        if (!locations.iterator().hasNext())
            return Sector.EMPTY_SECTOR; // TODO: should be returning null

        double minLat = Angle.POS90degrees;
        double minLon = Angle.POS180degrees;
        double maxLat = Angle.NEG90degrees;
        double maxLon = Angle.NEG180degrees;

        for (LatLon p : locations) {
            double lat = p.latitude;
            if (lat < minLat)
                minLat = lat;
            if (lat > maxLat)
                maxLat = lat;

            double lon = p.longitude;
            if (lon < minLon)
                minLon = lon;
            if (lon > maxLon)
                maxLon = lon;
        }

        return Sector.fromDegrees(minLat, maxLat, minLon, maxLon);
    }

    public static Sector[] splitBoundingSectors(Iterable<? extends LatLon> locations) {

        double minLat = Angle.POS90degrees;
        double minLon = Angle.POS180degrees;
        double maxLat = Angle.NEG90degrees;
        double maxLon = Angle.NEG180degrees;

        LatLon lastLocation = null;

        for (LatLon ll : locations) {
            double lat = ll.getLatitude().degrees;
            if (lat < minLat)
                minLat = lat;
            if (lat > maxLat)
                maxLat = lat;

            double lon = ll.getLongitude().degrees;
            if (lon >= 0 && lon < minLon)
                minLon = lon;
            if (lon <= 0 && lon > maxLon)
                maxLon = lon;

            if (lastLocation != null) {
                double lastLon = lastLocation.getLongitude().degrees;
                if (signum(lon) != signum(lastLon)) {
                    if (abs(lon - lastLon) < 180) {
                        // Crossing the zero longitude line too
                        maxLon = 0;
                        minLon = 0;
                    }
                }
            }
            lastLocation = ll;
        }

        if (minLat == maxLat && minLon == maxLon)
            return null;

        return new Sector[]
            {
                Sector.fromDegrees(minLat, maxLat, minLon, 180), // Sector on eastern hemisphere.
                Sector.fromDegrees(minLat, maxLat, -180, maxLon) // Sector on western hemisphere.
            };
    }

    public static Sector boundingSector(LatLon pA, LatLon pB) {

        double minLat = pA.latitude;
        double minLon = pA.longitude;
        double maxLat = pA.latitude;
        double maxLon = pA.longitude;

        if (pB.latitude < minLat)
            minLat = pB.latitude;
        else if (pB.latitude > maxLat)
            maxLat = pB.latitude;

        if (pB.longitude < minLon)
            minLon = pB.longitude;
        else if (pB.longitude > maxLon)
            maxLon = pB.longitude;

        return Sector.fromDegrees(minLat, maxLat, minLon, maxLon);
    }

    /**
     * Returns a new <code>Sector</code> encompassing a circle centered at a given position, with a given radius in
     * meter.
     *
     * @param globe  a Globe instance.
     * @param center the circle center position.
     * @param radius the circle radius in meter.
     * @return the circle bounding sector.
     */
    public static Sector boundingSector(Globe globe, LatLon center, double radius) {
        double halfDeltaLatRadians = radius / globe.getRadiusAt(center);
        double halfDeltaLonRadians = PI * 2;
        final Angle lat = center.getLatitude();
        if (lat.cos() > 0)
            halfDeltaLonRadians = halfDeltaLatRadians / lat.cos();

        final double lon = center.getLongitude().radians();
        return new Sector(
            Angle.fromRadiansLatitude(lat.radians() - halfDeltaLatRadians),
            Angle.fromRadiansLatitude(lat.radians() + halfDeltaLatRadians),
            Angle.fromRadiansLongitude(lon - halfDeltaLonRadians),
            Angle.fromRadiansLongitude(lon + halfDeltaLonRadians));
    }

    /**
     * Returns an array of Sectors encompassing a circle centered at a given position, with a given radius in meters. If
     * the geometry defined by the circle and radius spans the international dateline, this will return two sectors, one
     * for each side of the dateline. Otherwise, this will return a single bounding sector. This returns null if the
     * radius is zero.
     *
     * @param globe  a Globe instance.
     * @param center the circle center location.
     * @param radius the circle radius in meters.
     * @return the circle's bounding sectors, or null if the radius is zero.
     * @throws IllegalArgumentException if either the globe or center is null, or if the radius is less than zero.
     */
    public static Sector[] splitBoundingSectors(Globe globe, LatLon center, double radius) {
        if (globe == null) {
            String message = Logging.getMessage("nullValue.GlobeIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (center == null) {
            String message = Logging.getMessage("nullValue.CenterIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (radius < 0) {
            String message = Logging.getMessage("generic.ArgumentOutOfRange", "radius < 0");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        double halfDeltaLatRadians = radius / globe.getRadiusAt(center);

        final double latRad = center.getLatitude().radians();
        double minLat = latRad - halfDeltaLatRadians;
        double maxLat = latRad + halfDeltaLatRadians;

        double minLon;
        double maxLon;

        // If the circle does not cross a pole, then compute the max and min longitude.
        if (minLat >= Angle.NEG90.radians() && maxLat <= Angle.POS90.radians()) {
            // We want to find the maximum and minimum longitude values on the circle. We will start with equation 5-6
            // from "Map Projections - A Working Manual", page 31, and solve for the value of Az that will maximize
            // lon - lon0.
            //
            // Eq. 5-6:
            // lon = lon0 + arctan( h(lat0, c, az) )
            // h(lat0, c, az) = sin(c) sin(az) / (cos(lat0) cos(c) - sin(lat1) sin(c) cos(Az))
            //
            // Where (lat0, lon0) are the starting coordinates, c is the angular distance along the great circle from
            // the starting coordinate, Az is the azimuth, and lon is the end position longitude. All values are in
            // radians.
            //
            // lon - lon0 is maximized when h(lat0, c, Az) is maximized because arctan(x) -> 1 as x -> infinity.
            //
            // Taking the partial derivative of h with respect to Az we get:
            // h'(Az) = (sin(c) cos(c) cos(lat0) cos(Az) - sin^2(c) sin(lat0)) / (cos(lat0) cos(c) - sin(lat0) sin(c) cos(Az))^2
            //
            // Setting h' = 0 to find maxima:
            // 0 = sin(c) cos(c) cos(lat0) cos(Az) - sin^2(c) sin(lat0)
            //
            // And solving for Az:
            // Az = arccos( tan(lat0) tan(c) )
            //
            // +/- Az are bearings from North that will give positions of max and min longitude.

            // If halfDeltaLatRadians == 90 degrees, then tan is undefined. This can happen if the circle radius is one
            // quarter of the globe diameter, and the circle is centered on the equator. tan(center lat) is always
            // defined because the center lat is in the range -90 to 90 exclusive. If it were equal to 90, then the
            // circle would cover a pole.
            // Consider within 1/1000th of a radian to be equal
            double az = abs(Angle.POS90.radians() - halfDeltaLatRadians) > 0.001 ?
                acos(tan(halfDeltaLatRadians) * tan(latRad)) :
                Angle.POS90.radians();

            LatLon east = LatLon.greatCircleEndPosition(center, az, halfDeltaLatRadians);
            LatLon west = LatLon.greatCircleEndPosition(center, -az, halfDeltaLatRadians);

            final double eastLonRad = east.getLongitude().radians();
            final double westLonRad = west.getLongitude().radians();
            minLon = min(eastLonRad, westLonRad);
            maxLon = max(eastLonRad, westLonRad);
        } else {
            // If the circle crosses the pole then it spans the full circle of longitude
            minLon = Angle.NEG180.radians();
            maxLon = Angle.POS180.radians();
        }

        LatLon ll = new LatLon(
            Angle.fromRadiansLatitude(minLat),
            Angle.lonNorm(Angle.fromRadians(minLon)));
        LatLon ur = new LatLon(
            Angle.fromRadiansLatitude(maxLat),
            Angle.lonNorm(Angle.fromRadians(maxLon)));

        Iterable<? extends LatLon> locations = Arrays.asList(ll, ur);

        if (LatLon.locationsCrossDateLine(locations)) {
            return Sector.splitBoundingSectors(locations);
        } else {
            Sector s = Sector.boundingSector(locations);
            return (s != null && !s.equals(Sector.EMPTY_SECTOR)) ? new Sector[] {s} : null;
        }
    }

    @SuppressWarnings("RedundantIfStatement")
    public static boolean isSector(Iterable<? extends LatLon> corners) {

        LatLon[] latlons = new LatLon[5];

        int i = 0;
        for (LatLon ll : corners) {
            if (i > 4 || ll == null)
                return false;

            latlons[i++] = ll;
        }

        if (latlons[0].latitude != latlons[1].latitude)
            return false;
        if (latlons[2].latitude != latlons[3].latitude)
            return false;
        if (latlons[0].longitude != latlons[3].longitude)
            return false;
        if (latlons[1].longitude != latlons[2].longitude)
            return false;

        if (i == 5 && !latlons[4].equals(latlons[0]))
            return false;
        else
            return true;
    }

    /**
     * Returns a sphere that minimally surrounds the sector at a specified vertical exaggeration.
     *
     * @param globe                the globe the sector is associated with
     * @param verticalExaggeration the vertical exaggeration to apply to the globe's elevations when computing the
     *                             sphere.
     * @param sector               the sector to return the bounding sphere for.
     * @return The minimal bounding sphere in Cartesian coordinates.
     * @throws IllegalArgumentException if <code>globe</code> or <code>sector</code> is null
     */
    static public Sphere computeBoundingSphere(Globe globe, double verticalExaggeration, Sector sector) {

        LatLon center = sector.getCentroid();
        double[] minAndMaxElevations = globe.getMinAndMaxElevations(sector);
        double minHeight = minAndMaxElevations[0] * verticalExaggeration;
        double maxHeight = minAndMaxElevations[1] * verticalExaggeration;

        Vec4[] points = new Vec4[9];
        points[0] = globe.computePointFromPosition(center.getLatitude(), center.getLongitude(), maxHeight);
        points[1] = globe.computePointFromPosition(sector.latMax(), sector.lonMin(), maxHeight);
        points[2] = globe.computePointFromPosition(sector.latMin(), sector.lonMax(), maxHeight);
        points[3] = globe.computePointFromPosition(sector.latMin(), sector.lonMin(), maxHeight);
        points[4] = globe.computePointFromPosition(sector.latMax(), sector.lonMax(), maxHeight);
        points[5] = globe.computePointFromPosition(sector.latMax(), sector.lonMin(), minHeight);
        points[6] = globe.computePointFromPosition(sector.latMin(), sector.lonMax(), minHeight);
        points[7] = globe.computePointFromPosition(sector.latMin(), sector.lonMin(), minHeight);
        points[8] = globe.computePointFromPosition(sector.latMax(), sector.lonMax(), minHeight);

        return Sphere.createBoundingSphere(points);
    }

    /**
     * Returns a {@link Box} that bounds the specified sector on the surface of the specified {@link Globe}. The
     * returned box encloses the globe's surface terrain in the sector, according to the specified vertical exaggeration
     * and the globe's minimum and maximum elevations in the sector. If the minimum and maximum elevation are equal,
     * this assumes a maximum elevation of 10 + the minimum. If this fails to compute a box enclosing the sector, this
     * returns a unit box enclosing one of the boxes corners.
     *
     * @param globe                the globe the extent relates to.
     * @param verticalExaggeration the globe's vertical surface exaggeration.
     * @param sector               a sector on the globe's surface to compute a bounding box for.
     * @return a box enclosing the globe's surface on the specified sector.
     * @throws IllegalArgumentException if either the globe or sector is null.
     */
    public static Box computeBoundingBox(Globe globe, double verticalExaggeration, Sector sector) {

        double[] minAndMaxElevations = globe.getMinAndMaxElevations(sector);
        return Sector.computeBoundingBox(globe, verticalExaggeration, sector, minAndMaxElevations[0], minAndMaxElevations[1]);
    }

    /**
     * Returns a {@link Box} that bounds the specified sector on the surface of the specified {@link Globe}. The
     * returned box encloses the globe's surface terrain in the sector, according to the specified vertical
     * exaggeration, minimum elevation, and maximum elevation. If the minimum and maximum elevation are equal, this
     * assumes a maximum elevation of 10 + the minimum. If this fails to compute a box enclosing the sector, this
     * returns a unit box enclosing one of the boxes corners.
     *
     * @param globe                the globe the extent relates to.
     * @param verticalExaggeration the globe's vertical surface exaggeration.
     * @param sector               a sector on the globe's surface to compute a bounding box for.
     * @param minElevation         the globe's minimum elevation in the sector.
     * @param maxElevation         the globe's maximum elevation in the sector.
     * @return a box enclosing the globe's surface on the specified sector.
     * @throws IllegalArgumentException if either the globe or sector is null.
     */
    public static Box computeBoundingBox(Globe globe, double verticalExaggeration, Sector sector,
        double minElevation, double maxElevation) {
//        if (globe == null) {
//            String msg = Logging.getMessage("nullValue.GlobeIsNull");
//            Logging.logger().severe(msg);
//            throw new IllegalArgumentException(msg);
//        }
//
//        if (sector == null) {
//            String msg = Logging.getMessage("nullValue.SectorIsNull");
//            Logging.logger().severe(msg);
//            throw new IllegalArgumentException(msg);
//        }

        // Compute the exaggerated minimum and maximum heights.
        double min = minElevation * verticalExaggeration;
        double max = maxElevation * verticalExaggeration;

        // Ensure the top and bottom heights are not equal.
        if (min == max) {
            max = min + 10;
        }

        // Create an array for a 3x5 grid of elevations. Use min height at the corners and max height everywhere else.
        double[] elevations = {
            min, max, max, max, min,
            max, max, max, max, max,
            min, max, max, max, min};

        // Compute the cartesian points for a 3x5 geographic grid. This grid captures enough detail to bound the sector.
        Vec4[] points = new Vec4[15];
        globe.computePointsFromPositions(sector, 3, 5, elevations, points);

//        try {
            return Box.computeBoundingBox(Arrays.asList(points));
//        }
//        catch (RuntimeException e) {
//            return new Box(points[0]); // unit box around point
//        }
    }

    /**
     * Returns a cylinder that minimally surrounds the specified sector at a specified vertical exaggeration.
     *
     * @param globe                The globe associated with the sector.
     * @param verticalExaggeration the vertical exaggeration to apply to the minimum and maximum elevations when
     *                             computing the cylinder.
     * @param sector               the sector to return the bounding cylinder for.
     * @return The minimal bounding cylinder in Cartesian coordinates.
     * @throws IllegalArgumentException if <code>sector</code> is null
     */
    static public Cylinder computeBoundingCylinder(Globe globe, double verticalExaggeration, Sector sector) {

        double[] minAndMaxElevations = globe.getMinAndMaxElevations(sector);
        return Sector.computeBoundingCylinder(globe, verticalExaggeration, sector,
            minAndMaxElevations[0], minAndMaxElevations[1]);
    }

    /**
     * Returns a cylinder that minimally surrounds the specified sector at a specified vertical exaggeration and minimum
     * and maximum elevations for the sector.
     *
     * @param globe                The globe associated with the sector.
     * @param verticalExaggeration the vertical exaggeration to apply to the minimum and maximum elevations when
     *                             computing the cylinder.
     * @param sector               the sector to return the bounding cylinder for.
     * @param minElevation         the minimum elevation of the bounding cylinder.
     * @param maxElevation         the maximum elevation of the bounding cylinder.
     * @return The minimal bounding cylinder in Cartesian coordinates.
     * @throws IllegalArgumentException if <code>sector</code> is null
     */
    public static Cylinder computeBoundingCylinder(Globe globe, double verticalExaggeration, Sector sector,
        double minElevation, double maxElevation) {
//        if (globe == null) {
//            String msg = Logging.getMessage("nullValue.GlobeIsNull");
//            Logging.logger().severe(msg);
//            throw new IllegalArgumentException(msg);
//        }
//
//        if (sector == null) {
//            String msg = Logging.getMessage("nullValue.SectorIsNull");
//            Logging.logger().severe(msg);
//            throw new IllegalArgumentException(msg);
//        }

        // Compute the exaggerated minimum and maximum heights.
        double minHeight = minElevation * verticalExaggeration;
        double maxHeight = maxElevation * verticalExaggeration;

        if (minHeight == maxHeight)
            maxHeight = minHeight + 1; // ensure the top and bottom of the cylinder won't be coincident

        List<Vec4> points = new ArrayList<>(8 + (sector.lonDelta > 180 ? 4 : 0));
        for (LatLon ll : sector) {
            points.add(globe.computePointFromPosition(ll, minHeight));
            points.add(globe.computePointFromPosition(ll, maxHeight));
        }
        final LatLon sc = sector.getCentroid();
        points.add(globe.computePointFromPosition(sc, maxHeight));

        if (sector.lonDelta > 180) {
            // Need to compute more points to ensure the box encompasses the full sector.
            Angle cLon = sc.getLongitude();
            Angle cLat = sc.getLatitude();

            // centroid latitude, longitude midway between min longitude and centroid longitude
            Angle lon = Angle.midAngle(sector.lonMin(), cLon);
            points.add(globe.computePointFromPosition(cLat, lon, maxHeight));

            // centroid latitude, longitude midway between centroid longitude and max longitude
            lon = Angle.midAngle(cLon, sector.lonMax());
            points.add(globe.computePointFromPosition(cLat, lon, maxHeight));

            // centroid latitude, longitude at min longitude and max longitude
            points.add(globe.computePointFromPosition(cLat, sector.lonMin(), maxHeight));
            points.add(globe.computePointFromPosition(cLat, sector.lonMax(), maxHeight));
        }

        try {
            return Cylinder.computeBoundingCylinder(points);
        }
        catch (RuntimeException e) {
            return new Cylinder(points.get(0), points.get(0).add3(Vec4.UNIT_Y), 1);
        }
    }

    static public Cylinder computeBoundingCylinderOrig(Globe globe, double verticalExaggeration, Sector sector) {
        return Cylinder.computeVerticalBoundingCylinder(globe, verticalExaggeration, sector);
    }

    /**
     * Returns a cylinder that minimally surrounds the specified minimum and maximum elevations in the sector at a
     * specified vertical exaggeration.
     *
     * @param globe                The globe associated with the sector.
     * @param verticalExaggeration the vertical exaggeration to apply to the minimum and maximum elevations when
     *                             computing the cylinder.
     * @param sector               the sector to return the bounding cylinder for.
     * @param minElevation         the minimum elevation of the bounding cylinder.
     * @param maxElevation         the maximum elevation of the bounding cylinder.
     * @return The minimal bounding cylinder in Cartesian coordinates.
     * @throws IllegalArgumentException if <code>sector</code> is null
     */
    public static Cylinder computeBoundingCylinderOrig(Globe globe, double verticalExaggeration, Sector sector,
        double minElevation, double maxElevation) {
        return Cylinder.computeVerticalBoundingCylinder(globe, verticalExaggeration, sector, minElevation,
            maxElevation);
    }

    public static Sector union(Sector[] sectors) {
        assert (sectors.length == 2);
        return Sector.union(sectors[0], sectors[1]);
    }

    public static Sector union(Sector sectorA, Sector sectorB) {
        if (sectorA == null || sectorB == null) {
            if (sectorA == sectorB)
                return sectorA;

            return sectorB == null ? sectorA : sectorB;
        }

        return sectorA.union(sectorB);
    }

    public static Sector union(Iterable<? extends Sector> sectors) {

        double minLat = Angle.POS90degrees;
        double maxLat = Angle.NEG90degrees;
        double minLon = Angle.POS180degrees;
        double maxLon = Angle.NEG180degrees;

        for (Sector s : sectors) {
            if (s == null)
                continue;

            for (LatLon p : s) {
                final double lat = p.latitude;
                if (lat < minLat)
                    minLat = lat;
                if (lat > maxLat)
                    maxLat = lat;

                final double lon = p.longitude;
                if (lon < minLon)
                    minLon = lon;
                if (lon > maxLon)
                    maxLon = lon;
            }
        }

        return new Sector(minLat, maxLat, minLon, maxLon);
    }

    /**
     * Returns the intersection of all sectors in the specified iterable. This returns a non-null sector if the iterable
     * contains at least one non-null entry and all non-null entries intersect. The returned sector represents the
     * geographic region in which all sectors intersect. This returns null if at least one of the sectors does not
     * intersect the others.
     *
     * @param sectors the sectors to intersect.
     * @return the intersection of all sectors in the specified iterable, or null if at least one of the sectors does
     * not intersect the others.
     * @throws IllegalArgumentException if the iterable is null.
     */
    public static Sector intersection(Iterable<? extends Sector> sectors) {

        Sector result = null;

        for (Sector s : sectors) {
            if (s == null)
                continue; // ignore null sectors

            if (result == null)
                result = s; // start with the first non-null sector
            else if ((result = result.intersection(s)) == null)
                break; // at least one of the sectors does not intersect the others
        }

        return result;
    }

    /**
     * Returns the sector's minimum latitude.
     *
     * @return The sector's minimum latitude.
     */
    @Deprecated
    public final Angle latMin() {
        return Angle.fromDegrees(latMin);
    }

    /**
     * Returns the sector's minimum longitude.
     *
     * @return The sector's minimum longitude.
     */
    @Deprecated
    public final Angle lonMin() {
        return Angle.fromDegrees(lonMin);
    }

    /**
     * Returns the sector's maximum latitude.
     *
     * @return The sector's maximum latitude.
     */
    @Deprecated
    public final Angle latMax() {
        return Angle.fromDegrees(latMax);
    }

    /**
     * Returns the sector's maximum longitude.
     *
     * @return The sector's maximum longitude.
     */
    @Deprecated
    public final Angle lonMax() {
        return Angle.fromDegrees(lonMax);
    }

    /**
     * Returns the angular difference between the sector's minimum and maximum latitudes: max - min
     *
     * @return The angular difference between the sector's minimum and maximum latitudes.
     */
    @Deprecated
    public final Angle latDelta() {
        return Angle.fromDegrees(
            this.latDelta);//Angle.fromDegrees(this.maxLatitude.degrees - this.minLatitude.degrees);
    }

    /**
     * Returns the angular difference between the sector's minimum and maximum longitudes: max - min.
     *
     * @return The angular difference between the sector's minimum and maximum longitudes
     */
    @Deprecated
    public final Angle lonDelta() {
        return Angle.fromDegrees(
            this.lonDelta);//Angle.fromDegrees(this.maxLongitude.degrees - this.minLongitude.degrees);
    }

    public boolean isWithinLatLonLimits() {
        return this.latMin >= -90 && this.latMax <= 90
            && this.lonMin >= -180 && this.lonMax <= 180;
    }

    public boolean isSameSector(Iterable<? extends LatLon> corners) {
        return Sector.isSector(corners) && Sector.boundingSector(corners).equals(this);
    }

    /**
     * Returns the latitude and longitude of the sector's angular center: (minimum latitude + maximum latitude) / 2,
     * (minimum longitude + maximum longitude) / 2.
     *
     * @return The latitude and longitude of the sector's angular center
     */
    public LatLon getCentroid() {
        return new LatLon(
            Angle.fromDegrees(0.5 * (this.latMax + this.latMin)),
            Angle.fromDegrees(0.5 * (this.lonMax + this.lonMin))
        );
    }

    /**
     * Computes the Cartesian coordinates of a Sector's center.
     *
     * @param globe        The globe associated with the sector.
     * @param exaggeration The vertical exaggeration to apply.
     * @return the Cartesian coordinates of the sector's center.
     * @throws IllegalArgumentException if <code>globe</code> is null.
     */
    public Vec4 computeCenterPoint(Globe globe, double exaggeration) {

        Angle lat = Angle.fromDegrees((this.latMin + this.latMax) / 2);
        Angle lon = Angle.fromDegrees((this.lonMin + this.lonMax) / 2);
        final double ele = globe.getElevation(lat, lon);
        return globe.computePointFromPosition(lat, lon, exaggeration * ele);
    }

    /**
     * Computes the Cartesian coordinates of a Sector's corners.
     *
     * @param g            The globe associated with the sector.
     * @param exaggeration The vertical exaggeration to apply.
     * @return an array of four Cartesian points.
     * @throws IllegalArgumentException if <code>globe</code> is null.
     */
    public Vec4[] computeCornerPoints(Globe g, double exaggeration) {
        Vec4[] corners = new Vec4[4];

        double minLat = this.latMin;
        double maxLat = this.latMax;
        double minLon = this.lonMin;
        double maxLon = this.lonMax;

        final Angle latMin = latMin();
        final Angle lonMin = lonMin();
        final Angle lonMax = lonMax();
        final Angle latMax = latMax();
        corners[0] = g.computePointFromPosition(minLat, minLon, exaggeration * g.getElevation(latMin, lonMin));
        corners[1] = g.computePointFromPosition(minLat, maxLon, exaggeration * g.getElevation(latMin, lonMax));
        corners[2] = g.computePointFromPosition(maxLat, maxLon, exaggeration * g.getElevation(latMax, lonMax));
        corners[3] = g.computePointFromPosition(maxLat, minLon, exaggeration * g.getElevation(latMax, lonMin));

        return corners;
    }

    @Deprecated
    public final boolean contains(Angle latitude, Angle longitude) {
        return containsDegrees(latitude.degrees, longitude.degrees);
    }

    /**
     * Determines whether a latitude/longitude position is within the sector. The sector's angles are assumed to be
     * normalized to +/- 90 degrees latitude and +/- 180 degrees longitude. The result of the operation is undefined if
     * they are not.
     *
     * @param latLon the position to test, with angles normalized to +/- &#960; latitude and +/- 2&#960; longitude.
     * @return <code>true</code> if the position is within the sector, <code>false</code> otherwise.
     * @throws IllegalArgumentException if <code>latlon</code> is null.
     */
    public final boolean contains(LatLon latLon) {
        return this.containsDegrees(latLon.latitude, latLon.longitude);
    }

    public boolean containsDegrees(double degreesLatitude, double degreesLongitude) {
        return degreesLatitude >= this.latMin && degreesLatitude <= this.latMax
            && degreesLongitude >= this.lonMin && degreesLongitude <= this.lonMax;
    }

    /**
     * Determines whether another sector is fully contained within this one. The sector's angles are assumed to be
     * normalized to +/- 90 degrees latitude and +/- 180 degrees longitude. The result of the operation is undefined if
     * they are not.
     *
     * @param that the sector to test for containment.
     * @return <code>true</code> if this sector fully contains the input sector, otherwise <code>false</code>.
     */
    public boolean contains(Sector that) {
        if (that == null)
            return false;

        // Assumes normalized angles -- [-180, 180], [-90, 90]
        if (that.lonMin < this.lonMin)
            return false;
        if (that.lonMax > this.lonMax)
            return false;
        if (that.latMin < this.latMin)
            return false;
        return !(that.latMax > this.latMax);
    }

    /**
     * Determines whether this sector intersects another sector's range of latitude and longitude. The sector's angles
     * are assumed to be normalized to +/- 90 degrees latitude and +/- 180 degrees longitude. The result of the
     * operation is undefined if they are not.
     *
     * @param that the sector to test for intersection.
     * @return <code>true</code> if the sectors intersect, otherwise <code>false</code>.
     */
    public boolean intersects(Sector that) {
        if (that == null)
            return false;
        if (this == that)
            return true;

        // Assumes normalized angles -- [-180, 180], [-90, 90]
        if (that.lonMax < this.lonMin)
            return false;
        if (that.lonMin > this.lonMax)
            return false;
        if (that.latMax < this.latMin)
            return false;
        return !(that.latMin > this.latMax);
    }

    /**
     * Determines whether the interiors of this sector and another sector intersect. The sector's angles are assumed to
     * be normalized to +/- 90 degrees latitude and +/- 180 degrees longitude. The result of the operation is undefined
     * if they are not.
     *
     * @param that the sector to test for intersection.
     * @return <code>true</code> if the sectors' interiors intersect, otherwise <code>false</code>.
     * @see #intersects(Sector)
     */
    public boolean intersectsInterior(Sector that) {
        if (that == null)
            return false;
        if (this == that)
            return true;

        // Assumes normalized angles -- [-180, 180], [-90, 90]
        if (that.lonMax <= this.lonMin)
            return false;
        if (that.lonMin >= this.lonMax)
            return false;
        if (that.latMax <= this.latMin)
            return false;
        return !(that.latMin >= this.latMax);
    }

    /**
     * Determines whether this sector intersects any one of the sectors in the specified iterable. This returns true if
     * at least one of the sectors is non-null and intersects this sector.
     *
     * @param sectors the sectors to test for intersection.
     * @return true if at least one of the sectors is non-null and intersects this sector, otherwise false.
     * @throws IllegalArgumentException if the iterable is null.
     */
    public boolean intersectsAny(Sector[] sectors) {

        for (Sector s : sectors) {
            if (s != null && s.intersects(this))
                return true;
        }

        return false;
    }

    /**
     * Returns a new sector whose angles are the extremes of the this sector and another. The new sector's minimum
     * latitude and longitude will be the minimum of the two sectors. The new sector's maximum latitude and longitude
     * will be the maximum of the two sectors. The sectors are assumed to be normalized to +/- 90 degrees latitude and
     * +/- 180 degrees longitude. The result of the operation is undefined if they are not.
     *
     * @param that the sector to join with <code>this</code>.
     * @return A new sector formed from the extremes of the two sectors, or <code>this</code> if the incoming sector is
     * <code>null</code>.
     */
    public final Sector union(Sector that) {
        if (that == null)
            return this;

        return new Sector(min(latMin, that.latMin), max(latMax, that.latMax), min(lonMin, that.lonMin),
            max(lonMax, that.lonMax));
    }

    public final Sector union(Angle latitude, Angle longitude) {
        if (latitude == null || longitude == null)
            return this;

        double lat = latitude.degrees, lon = longitude.degrees;
        if (containsDegrees(lat, lon))
            return this;
        else
            return new Sector(min(latMin, lat), max(latMax, lat), min(lonMin, lon), max(lonMax, lon));
    }

    public final Sector intersection(Sector that) {
        if (that == null)
            return this;

        double minLat = max(this.latMin, that.latMin);
        double maxLat = min(this.latMax, that.latMax);
        if (minLat > maxLat)
            return null;

        double minLon = max(this.lonMin, that.lonMin);
        double maxLon = min(this.lonMax, that.lonMax);
        if (minLon > maxLon)
            return null;

        return new Sector(minLat, maxLat, minLon, maxLon);
    }

    public final Sector intersection(Angle latitude, Angle longitude) {
        if (latitude == null || longitude == null)
            return this;

        if (!this.contains(latitude, longitude))
            return null;
        return new Sector(latitude, latitude, longitude, longitude);
    }

    public Sector[] subdivide() {
        double midLat = WWMath.average(this.latMin, this.latMax);
        double midLon = WWMath.average(this.lonMin, this.lonMax);

        Sector[] sectors = new Sector[4];
        sectors[0] = new Sector(this.latMin, midLat, this.lonMin, midLon);
        sectors[1] = new Sector(this.latMin, midLat, midLon, this.lonMax);
        sectors[2] = new Sector(midLat, this.latMax, this.lonMin, midLon);
        sectors[3] = new Sector(midLat, this.latMax, midLon, this.lonMax);

        return sectors;
    }

    public Sector[] subdivide(int div) {
        assert (div > 1);

        double dLat = this.latDelta / div;
        double dLon = this.lonDelta / div;

        Sector[] sectors = new Sector[div * div];
        int i = 0;
        for (int row = 0; row < div; row++) {
            final double rowLatMin = this.latMin + dLat * row;
            final double rowLatMax = rowLatMin + dLat;

            for (int col = 0; col < div; col++) {
                sectors[i++] = Sector.fromDegrees(rowLatMin, rowLatMax,
                    this.lonMin + dLon * col,
                    this.lonMin + dLon * col + dLon);
            }
        }

        return sectors;
    }

    /**
     * Returns an approximation of the distance in model coordinates between the surface geometry defined by this sector
     * and the specified model coordinate point. The returned value represents the shortest distance between the
     * specified point and this sector's corner points or its center point. The draw context defines the globe and the
     * elevations that are used to compute the corner points and the center point.
     *
     * @param dc    The draw context defining the surface geometry.
     * @param point The model coordinate point to compute a distance to.
     * @return The distance between this sector's surface geometry and the specified point, in model coordinates.
     * @throws IllegalArgumentException if any argument is null.
     */
    public double distanceTo(DrawContext dc, Vec4 point) {

        final Globe globe = dc.getGlobe();
        final double vertExag = dc.getVerticalExaggeration();
        Vec4[] corners = this.computeCornerPoints(globe, vertExag);
        Vec4 centerPoint = this.computeCenterPoint(globe, vertExag);

        // Get the distance for each of the sector's corners and its center.
        double d1 = point.distanceTo3(corners[0]);
        double d2 = point.distanceTo3(corners[1]);
        double d3 = point.distanceTo3(corners[2]);
        double d4 = point.distanceTo3(corners[3]);
        double d5 = point.distanceTo3(centerPoint);

        // Find the minimum distance.
        double minDistance = d1;
        if (minDistance > d2)
            minDistance = d2;
        if (minDistance > d3)
            minDistance = d3;
        if (minDistance > d4)
            minDistance = d4;
        if (minDistance > d5)
            minDistance = d5;

        return minDistance;
    }

    /**
     * Returns a four element array containing the Sector's angles in degrees. The returned array is ordered as follows:
     * minimum latitude, maximum latitude, minimum longitude, and maximum longitude.
     *
     * @return four-element array containing the Sector's angles.
     */
    public double[] toArrayDegrees() {
        return new double[] {
            this.latMin, this.latMax,
            this.lonMin, this.lonMax
        };
    }

    /**
     * Returns a string indicating the sector's angles.
     *
     * @return A string indicating the sector's angles.
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append('(');
        sb.append(this.latMin().toString());
        sb.append(", ");
        sb.append(this.lonMin().toString());
        sb.append(')');

        sb.append(", ");

        sb.append('(');
        sb.append(this.latMax().toString());
        sb.append(", ");
        sb.append(this.lonMax().toString());
        sb.append(')');

        return sb.toString();
    }

    /**
     * Retrieve the size of this object in bytes. This implementation returns an exact value of the object's size.
     *
     * @return the size of this object in bytes
     */
    public long getSizeInBytes() {
        return 4 * Angle.getSizeInBytes();  // 4 angles
    }

    /**
     * Compares this sector to a specified sector according to their minimum latitude, minimum longitude, maximum
     * latitude, and maximum longitude, respectively.
     *
     * @param that the <code>Sector</code> to compareTo with <code>this</code>.
     * @return -1 if this sector compares less than that specified, 0 if they're equal, and 1 if it compares greater.
     * @throws IllegalArgumentException if <code>that</code> is null
     */
    public int compareTo(Sector that) {
        if (this == that)
            return 0;
        {
            int a = Double.compare(latMin, that.latMin);
            if (a != 0)
                return a;
        }
        {
            int a = Double.compare(lonMin, that.lonMin);
            if (a != 0)
                return a;
        }
        {
            int a = Double.compare(latMax, that.latMax);
            if (a != 0)
                return a;
        }
        {
            int a = Double.compare(lonMax, that.lonMax);
            return a;
        }
    }

    /**
     * Creates an iterator over the four corners of the sector, starting with the southwest position and continuing
     * counter-clockwise.
     *
     * @return an iterator for the sector.
     */
    public Iterator<LatLon> iterator() {
        return new CornerIterator();
    }

    /**
     * Returns the coordinates of the sector as a list, in the order minLat, maxLat, minLon, maxLon.
     *
     * @return the list of sector coordinates.
     */
    public List<LatLon> asList() {
        List<LatLon> list = new ArrayList<>(4);
        for (LatLon ll : this) {
            list.add(ll);
        }
        return list;
    }

    /**
     * Returns the coordinates of the sector as an array of values in degrees, in the order minLat, maxLat, minLon,
     * maxLon.
     *
     * @return the array of sector coordinates.
     */
    public double[] asDegreesArray() {
        return new double[] {this.latMin, this.latMax, this.lonMin, this.lonMax};
    }

    /**
     * Returns a list of the Lat/Lon coordinates of a Sector's corners.
     *
     * @return an array of the four corner locations, in the order SW, SE, NE, NW
     */
    public LatLon[] getCorners() {
        LatLon[] corners = new LatLon[4];
        corners[0] = LatLon.fromDegrees(this.latMin, this.lonMin);
        corners[1] = LatLon.fromDegrees(this.latMin, this.lonMax);
        corners[2] = LatLon.fromDegrees(this.latMax, this.lonMax);
        corners[3] = LatLon.fromDegrees(this.latMax, this.lonMin);
        return corners;
    }

    /**
     * Tests the equality of the sectors' angles. Sectors are equal if all of their corresponding angles are equal.
     *
     * @param o the sector to compareTo with <code>this</code>.
     * @return <code>true</code> if the four corresponding angles of each sector are equal, <code>false</code>
     * otherwise.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        final Sector sector = (Sector) o;

        return latMax == (sector.latMax) && lonMax == (sector.lonMax) && latMin == (sector.latMin)
            && lonMin == (sector.lonMin);
    }

    /**
     * Computes a hash code from the sector's four angles.
     *
     * @return a hash code incorporating the sector's four angles.
     */
    @Override
    public int hashCode() {
        int result;
        result = Double.hashCode(latMin);
        result = 29 * result + Double.hashCode(latMax);
        result = 29 * result + Double.hashCode(lonMin);
        result = 29 * result + Double.hashCode(lonMax);
        return result;
    }

    private final class CornerIterator implements Iterator<LatLon> {
        private int position;

        public boolean hasNext() {
            return this.position < 4;
        }

        public LatLon next() {
            if (this.position > 3)
                throw new NoSuchElementException();

            return switch (this.position++) {
                case 0 -> LatLon.fromDegrees(latMin, lonMin);
                case 1 -> LatLon.fromDegrees(latMin, lonMax);
                case 2 -> LatLon.fromDegrees(latMax, lonMax);
                case 3 -> LatLon.fromDegrees(latMax, lonMin);
                default -> null;
            };
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
}