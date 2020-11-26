/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.formats.gpx;

import gov.nasa.worldwind.tracks.*;
import gov.nasa.worldwind.util.Logging;
import org.xml.sax.Attributes;

import java.util.*;

/**
 * @author tag
 * @version $Id: GpxTrackSegment.java 1171 2013-02-11 21:45:02Z dcollins $
 */
public class GpxTrackSegment extends ElementParser
    implements TrackSegment {
    private final List<TrackPoint> points =
        new ArrayList<>();

    public GpxTrackSegment(String uri, String lname, String qname, Attributes attributes) {
        super("trkseg");

        // dont' validate uri, lname, qname or attributes as they aren't used.
    }

    public List<TrackPoint> getPoints() {
        return this.points;
    }

    /**
     * @param uri        The element URI.
     * @param lname      the element lname.
     * @param qname      the element qname.
     * @param attributes The element attributes.
     * @throws IllegalArgumentException if any parameter is null
     */
    @Override
    public void doStartElement(String uri, String lname, String qname, Attributes attributes) {
        if (lname == null) {
            String msg = Logging.getMessage("nullValue.LNameIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        if (uri == null) {
            String msg = Logging.getMessage("nullValue.URIIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }
        if (qname == null) {
            String msg = Logging.getMessage("nullValue.QNameIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }
        if (attributes == null) {
            String msg = Logging.getMessage("nullValue.AttributesIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        if (lname.equalsIgnoreCase("trkpt")) {
            this.currentElement = new GpxTrackPoint(uri, lname, qname, attributes);
            this.points.add((TrackPoint) this.currentElement);
        }
    }
}
