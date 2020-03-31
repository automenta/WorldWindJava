/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */

package gov.nasa.worldwind.ogc.kml;

import gov.nasa.worldwind.ogc.collada.ColladaConstants;

/**
 * Defines constants used by the KML parser classes.
 *
 * @author tag
 * @version $Id: KMLConstants.java 1171 2013-02-11 21:45:02Z dcollins $
 */
public interface KMLConstants
{
    /** The KML 2.2 namespace URI. */
    String KML_2dot0_NAMESPACE = "http://earth.google.com/kml/2.0";
    String KML_2dot1_NAMESPACE = "http://earth.google.com/kml/2.1";
    String KML_2dot2_NAMESPACE = "http://www.opengis.net/kml/2.2";
    String KML_GOOGLE_2dot2_NAMESPACE = "http://earth.google.com/kml/2.2";
    String KML_NAMESPACE = KML_2dot2_NAMESPACE;

    /** List of the versioned KML namespaces. */
    String[] KML_NAMESPACES = {
        KML_2dot2_NAMESPACE,
        KML_GOOGLE_2dot2_NAMESPACE,
        KML_2dot1_NAMESPACE,
        KML_2dot0_NAMESPACE
    };

    /** The mime type for KML documents. */
    String KML_MIME_TYPE = "application/vnd.google-earth.kml+xml";
    /** The mime type for KMZ documents. */
    String KMZ_MIME_TYPE = "application/vnd.google-earth.kmz";

    /** @deprecated Use {@link ColladaConstants#COLLADA_MIME_TYPE}. */
    @Deprecated
    String COLLADA_MIME_TYPE = ColladaConstants.COLLADA_MIME_TYPE;

    /** Most recent version of KML that WorldWind supports. */
    String KML_VERSION = "2.2";

    // Style state enums
    String NORMAL = "normal";
    String HIGHLIGHT = "highlight";
    String STYLE_STATE = "styleState"; // a key for a style state field

    // The key that identifies resolved styles in a parser's field map.
    String BALOON_STYLE_FIELD = "BaloonStyle";
    String ICON_STYLE_FIELD = "IconStyle";
    String LABEL_STYLE_FIELD = "LabelStyle";
    String LINE_STYLE_FIELD = "LineStyle";
    String LIST_STYLE_FIELD = "ListStyle";
    String POLY_STYLE_FIELD = "PolyStyle";
    String STYLE_FIELD = "Style";
    String STYLE_MAP_FIELD = "StyleMap";
    String STYLE_URL_FIELD = "styleUrl";

    /**
     * The KML view refresh mode <code>never</code>. Indicates that a resource referenced by a <code>KMLLink</code>
     * should ignore changes in the geographic view, and ignore the link's <code>viewFormat</code> property.
     */
    String NEVER = "never";
    /**
     * The KML view refresh mode <code>onRequest</code>. Indicates that a resource referenced by a <code>KMLLink</code>
     * should refresh only when the user explicitly requests it.
     */
    String ON_REQUEST = "onRequest";
    /**
     * The KML view refresh mode <code>onStop</code>. Indicates that a resource referenced by a <code>KMLLink</code>
     * should refresh at an elapsed time after view movement stops. The time is specified by the link's
     * <code>viewRefreshTime</code> property.
     */
    String ON_STOP = "onStop";
    /**
     * The KML view refresh mode <code>onRegion</code>. Indicates that a resource referenced by a <code>KMLLink</code>
     * should refresh if a certain <code>KMLRegion</code> becomes active. The <code>KMLRegion</code> is specified by an
     * ancestor of the <code>KMLLink</code>.
     */
    String ON_REGION = "onRegion";
    /**
     * The KML link refresh mode <code>onInterval</code>. Indicates that a resource referenced by a <code>KMLLink</code>
     * should refresh periodically.
     */
    String ON_INTERVAL = "onInterval";
    /**
     * The KML view refresh mode <code>onExpire</code>. Indicates that a resource referenced by a <code>KMLLink</code>
     * should refresh when the resource expires. The expiration time can be set by a HTTP header, or by a {@link
     * KMLNetworkLinkControl} element.
     */
    String ON_EXPIRE = "onExpire";
    /**
     * The KML view refresh mode <code>onChange</code>. Indicates that a resource referenced by a <code>KMLLink</code>
     * should refresh when the file containing the link is loaded, or when the link parameters change.
     */
    String ON_CHANGE = "onChange";
}
