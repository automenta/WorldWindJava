/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */

package gov.nasa.worldwind.layers.ogc.kml.gx;

import gov.nasa.worldwind.layers.ogc.kml.*;
import gov.nasa.worldwind.util.xml.*;

import javax.xml.namespace.QName;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author tag
 * @version $Id: GXParserContext.java 1171 2013-02-11 21:45:02Z dcollins $
 */
public class GXParserContext extends BasicXMLEventParserContext {
    protected static final String[] StringFields = {
        "altitudeMode",
        "description",
        "flyToMode",
        "playMode",
    };

    protected static final String[] DoubleFields = {
        "duration",
    };

    protected static final String[] BooleanFields = {
        "balloonVisibility",
    };

    public static Map<QName, XMLEventParser> getDefaultParsers() {
        Map<QName, XMLEventParser> parsers = new ConcurrentHashMap<>();

        String ns = GXConstants.GX_NAMESPACE;
        parsers.put(new QName(ns, "AnimatedUpdate"), new GXAnimatedUpdate(ns));
        parsers.put(new QName(ns, "FlyTo"), new GXFlyTo(ns));
        parsers.put(new QName(ns, "LatLonQuad"), new GXLatLongQuad(ns));
        parsers.put(new QName(ns, "Playlist"), new GXPlaylist(ns));
        parsers.put(new QName(ns, "SoundCue"), new GXSoundCue(ns));
        parsers.put(new QName(ns, "TimeSpan"), new KMLTimeSpan(ns));
        parsers.put(new QName(ns, "TimeStamp"), new KMLTimeStamp(ns));
        parsers.put(new QName(ns, "Tour"), new GXTour(ns));
        parsers.put(new QName(ns, "TourControl"), new GXTourControl(ns));
        parsers.put(new QName(ns, "Wait"), new GXWait(ns));

        XMLEventParser stringParser = new StringXMLEventParser();
        for (String s : GXParserContext.StringFields) {
            parsers.put(new QName(ns, s), stringParser);
        }

        XMLEventParser doubleParser = new DoubleXMLEventParser();
        for (String s : GXParserContext.DoubleFields) {
            parsers.put(new QName(ns, s), doubleParser);
        }

        XMLEventParser booleanParser = new BooleanXMLEventParser();
        for (String s : GXParserContext.BooleanFields) {
            parsers.put(new QName(ns, s), booleanParser);
        }

        return parsers;
    }
}
