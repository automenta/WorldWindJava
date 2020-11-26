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
 * @version $Id: GpxRoute.java 1171 2013-02-11 21:45:02Z dcollins $
 */
public class GpxRoute extends ElementParser implements Track, TrackSegment {
    private final List<TrackPoint> points = new ArrayList<>();
    private String name;

    @SuppressWarnings("UnusedDeclaration")
    public GpxRoute(String uri, String lname, String qname, Attributes attributes) {
        super("rte");

        // dont' validate uri, lname, qname or attributes as they aren't used.
    }

    public List<TrackSegment> getSegments() {
        return Collections.singletonList(this);
    }

    public String getName() {
        return this.name;
    }

    public int getNumPoints() {
        return this.points.size();
    }

    public List<TrackPoint> getPoints() {
        return this.points;
    }

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

        if (lname.equalsIgnoreCase("rtept")) {
            this.currentElement = new GpxRoutePoint(uri, lname, qname, attributes);
            this.points.add((TrackPoint) this.currentElement);
        }
    }

    @Override
    public void doEndElement(String uri, String lname, String qname) {
        // don't validate uri or qname - they aren't used
        if (lname == null) {
            String msg = Logging.getMessage("nullValue.LNameIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        if (lname.equalsIgnoreCase("name")) {
            this.name = this.currentCharacters;
        }
    }
}
