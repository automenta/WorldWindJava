/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.geom;

import gov.nasa.worldwind.util.Logging;

import java.util.*;

import static gov.nasa.worldwind.util.WWUtil.sizeEstimate;

/**
 * @author Tom Gaskins
 * @version $Id: Intersection.java 1171 2013-02-11 21:45:02Z dcollins $
 */
public final class Intersection // Instances are immutable
{
    protected Vec4 intersectionPoint;
    protected Double intersectionLength;
    protected Position intersectionPosition;
    protected boolean isTangent;
    protected Object object;

    /**
     * Constructs an Intersection from an intersection point and tangency indicator.
     *
     * @param intersectionPoint the intersection point.
     * @param isTangent         true if the intersection is tangent to the object intersected, otherwise false.
     * @throws IllegalArgumentException if <code>intersectionPoint</code> is null
     */
    public Intersection(Vec4 intersectionPoint, boolean isTangent) {
        if (intersectionPoint == null) {
            String message = Logging.getMessage("nullValue.IntersectionPointIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        this.intersectionPoint = intersectionPoint;
        this.isTangent = isTangent;
    }

    /**
     * Constructs an Intersection from an intersection point and tangency indicator.
     *
     * @param intersectionPoint  the intersection point
     * @param intersectionLength the parametric length along the intersection geometry. If the geometry was a line, then
     *                           this value will be the parametric value of the intersection point along the line.
     * @param isTangent          true if the intersection is tangent to the object intersected, otherwise false.
     * @throws IllegalArgumentException if <code>intersectionPoint</code> is null
     */
    public Intersection(Vec4 intersectionPoint, double intersectionLength, boolean isTangent) {
        this(intersectionPoint, isTangent);

        this.intersectionLength = intersectionLength;
    }

    public Intersection(Vec4 intersectionPoint, Position intersectionPosition, boolean isTangent, Object object) {
        if (intersectionPoint == null) {
            String message = Logging.getMessage("nullValue.IntersectionPointIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.intersectionPoint = intersectionPoint;
        this.intersectionPosition = intersectionPosition;
        this.isTangent = isTangent;
        this.object = object;
    }

    /**
     * Merges two lists of intersections into a single list sorted by intersection distance from a specified reference
     * point.
     *
     * @param refPoint the reference point.
     * @param listA    the first list of intersections.
     * @param listB    the second list of intersections.
     * @return the merged list of intersections, sorted by increasing distance from the reference point.
     */
    public static List<Intersection> sort(final Vec4 refPoint, Collection<Intersection> listA, Collection<Intersection> listB) {
        final int s = (listA!=null ? sizeEstimate(listA) : 0) + (listB!=null ? sizeEstimate(listB) : 0);

        List<Intersection> sorted = new ArrayList<>(s);

        if (listA != null)
            sorted.addAll(listA);

        if (listB != null)
            sorted.addAll(listB);

        if (sorted.size() > 1) {
            sorted.sort((losiA, losiB) -> {
                if (losiA.intersectionPoint == null || losiB.intersectionPoint == null)
                    return 0;

                double dA = refPoint.distanceTo3(losiA.intersectionPoint);
                double dB = refPoint.distanceTo3(losiB.intersectionPoint);

                return Double.compare(dA, dB);
            });
        }

        return sorted;
    }

    /**
     * Returns the intersection position if one has been set.
     *
     * @return the intersection position, or null if the position has not been set.
     */
    public Position getIntersectionPosition() {
        return intersectionPosition;
    }

    /**
     * Specifies the intersection position, which should be a position computed from the intersection point.
     *
     * @param intersectionPosition the intersection position. May be null.
     */
    public void setIntersectionPosition(Position intersectionPosition) {
        this.intersectionPosition = intersectionPosition;
    }

    /**
     * Returns the object associated with the intersection.
     *
     * @return the object associated with the intersection, or null if no object is associated.
     */
    public Object getObject() {
        return object;
    }

    /**
     * Specifies the object to associate with the intersection.
     *
     * @param object the object to associate with the intersection. May be null.
     */
    public void setObject(Object object) {
        this.object = object;
    }

    /**
     * Returns the intersection point.
     *
     * @return the intersection point.
     */
    public Vec4 getIntersectionPoint() {
        return intersectionPoint;
    }

    /**
     * Specifies the intersection point.
     *
     * @param intersectionPoint the intersection point. May be null, but typically should not be.
     */
    public void setIntersectionPoint(Vec4 intersectionPoint) {
        this.intersectionPoint = intersectionPoint;
    }

    /**
     * Indicates whether the intersection is tangent to the object intersected.
     *
     * @return true if the intersection is tangent, otherwise false.
     */
    public boolean isTangent() {
        return isTangent;
    }

    /**
     * Specifies whether the intersection is tangent to the object intersected.
     *
     * @param tangent true if the intersection is tangent, otherwise false.
     */
    public void setTangent(boolean tangent) {
        isTangent = tangent;
    }

    /**
     * The parametric length along the intersection geometry. If the geometry involved a line, this value is the
     * parametric distance at which the intersection occurred along the line.
     *
     * @return the intersection length, or null if the length was not calculated.
     */
    public Double getIntersectionLength() {
        return intersectionLength;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        final Intersection that = (Intersection) o;

        if (isTangent != that.isTangent)
            return false;
        //noinspection RedundantIfStatement
        if (!intersectionPoint.equals(that.intersectionPoint))
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result;
        result = intersectionPoint.hashCode();
        result = 29 * result + (isTangent ? 1 : 0);
        return result;
    }

    @Override
    public String toString() {
        String pt = "Intersection Point: " + this.intersectionPoint;
        String tang = this.isTangent ? " is a tangent." : " not a tangent";
        return pt + tang;
    }
}
