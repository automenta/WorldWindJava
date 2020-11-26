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
 * @version $Id: GpxTrack.java 1171 2013-02-11 21:45:02Z dcollins $
 */
public class GpxTrack extends ElementParser implements Track {
    private final List<TrackSegment> segments =
        new ArrayList<>();
    private String name;
    private int numPoints = -1;

    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    public GpxTrack(String uri, String lname, String qname, Attributes attributes) {
        super("trk");
        // don't validate uri, lname, qname or attributes - they aren't used.
    }

    public List<TrackSegment> getSegments() {
        return segments;
    }

    public String getName() {
        return name;
    }

    public int getNumPoints() {
        if (this.segments == null)
            return 0;

        if (this.numPoints >= 0)
            return this.numPoints;

        this.numPoints = 0;
        for (TrackSegment segment : this.segments) {
            //noinspection UNUSED_SYMBOL,UnusedDeclaration
            for (TrackPoint point : segment.getPoints()) {
                ++this.numPoints;
            }
        }

        return this.numPoints;
    }

    /**
     * @param uri        The element URI.
     * @param lname      the element lname.
     * @param qname      the element qname.
     * @param attributes the element attributes.
     * @throws IllegalArgumentException if <code>lname</code> is null
     */
    @Override
    public void doStartElement(String uri, String lname, String qname, Attributes attributes) {
        // don't validate uri, qname or attributes - they aren't used
        if (lname == null) {
            String msg = Logging.getMessage("nullValue.LNameIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }
        if (qname == null) {
            String msg = Logging.getMessage("nullValue.QNameIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }
        if (uri == null) {
            String msg = Logging.getMessage("nullValue.URIIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }
        if (attributes == null) {
            String msg = Logging.getMessage("nullValue.AttributesIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        if (lname.equalsIgnoreCase("trkSeg")) {
            this.currentElement = new GpxTrackSegment(uri, lname, qname, attributes);
            this.segments.add((TrackSegment) this.currentElement);
        }
    }

    /**
     * @param uri   The element URI.
     * @param lname the element lname.
     * @param qname the element qname.
     * @throws IllegalArgumentException if <code>lname</code> is null
     */
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
