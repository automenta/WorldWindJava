/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */

package gov.nasa.worldwind.poi;

import gov.nasa.worldwind.Keys;
import gov.nasa.worldwind.exception.*;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.util.Logging;
import org.w3c.dom.*;

import javax.xml.parsers.*;
import javax.xml.xpath.*;
import java.io.ByteArrayInputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.logging.Level;

/**
 * A gazetteer that uses Yahoo's geocoding service to find locations for requested places.
 *
 * @author tag
 * @version $Id: YahooGazetteer.java 1395 2013-06-03 22:59:07Z tgaskins $
 */
public class YahooGazetteer implements Gazetteer {
    protected static final String GEOCODE_SERVICE =
        "https://query.yahooapis.com/v1/public/yql?q=select%20*%20from%20geo.places%20where%20text%3D";

    protected static boolean isNumber(String lookupString) {
        lookupString = lookupString.trim();

        return !lookupString.isEmpty() && lookupString.charAt(0) == '-'
            || !lookupString.isEmpty() && lookupString.charAt(0) == '+' || Character.isDigit(
            lookupString.charAt(0));
    }

    protected static ArrayList<PointOfInterest> parseLocationString(String locationString) throws WWRuntimeException {
        try {
            DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
            docBuilderFactory.setNamespaceAware(false);
            DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
            Document doc = docBuilder.parse(new ByteArrayInputStream(locationString.getBytes(
                StandardCharsets.UTF_8)));

            XPathFactory xpFactory = XPathFactory.newInstance();
            XPath xpath = xpFactory.newXPath();

            NodeList resultNodes =
                (NodeList) xpath.evaluate("/query/results/place", doc, XPathConstants.NODESET);

            ArrayList<PointOfInterest> positions = new ArrayList<>(resultNodes.getLength());

            for (int i = 0; i < resultNodes.getLength(); i++) {
                Node location = resultNodes.item(i);
                String lat = xpath.evaluate("centroid/latitude", location);
                String lon = xpath.evaluate("centroid/longitude", location);
                StringBuilder displayName = new StringBuilder();

                String placeType = xpath.evaluate("placeTypeName", location);
                String name = xpath.evaluate("name", location);
                String locality = xpath.evaluate("locality1", location);
                String admin = xpath.evaluate("admin1", location);

                if (placeType != null && !placeType.isEmpty()) {
                    displayName.append(placeType);
                    displayName.append(": ");
                }
                if (name != null && !name.isEmpty()) {
                    displayName.append(name);
                    displayName.append(". ");
                }
                if (locality != null && !locality.isEmpty()) {
                    displayName.append(locality);
                    displayName.append(", ");
                }
                if (admin != null && !admin.isEmpty()) {
                    displayName.append(admin);
                    displayName.append(", ");
                }
                displayName.append(xpath.evaluate("country", location));

                if (lat != null && lon != null) {
                    LatLon latlon = LatLon.fromDegrees(Double.parseDouble(lat), Double.parseDouble(lon));
                    PointOfInterest loc = new BasicPointOfInterest(latlon);
                    loc.set(Keys.DISPLAY_NAME, displayName.toString());
                    positions.add(loc);
                }
            }

            return positions;
        }
        catch (Exception e) {
            String msg = Logging.getMessage("Gazetteer.URLException", locationString);
            Logging.logger().log(Level.SEVERE, msg);
            throw new WWRuntimeException(msg);
        }
    }

    public List<PointOfInterest> findPlaces(String lookupString) throws NoItemException, ServiceException {
        if (lookupString == null || lookupString.length() < 1) {
            return null;
        }

        String urlString;
        urlString = YahooGazetteer.GEOCODE_SERVICE + "%22" + URLEncoder.encode(lookupString, StandardCharsets.UTF_8) + "%22";

        if (YahooGazetteer.isNumber(lookupString))
            lookupString += "%20and%20gflags%3D%22R%22";

        String locationString = POIUtils.callService(urlString);

        if (locationString == null || locationString.length() < 1) {
            return null;
        }

        return YahooGazetteer.parseLocationString(locationString);
    }
}