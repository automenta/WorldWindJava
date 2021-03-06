/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.examples.sar;

import gov.nasa.worldwind.Configuration;
import gov.nasa.worldwind.avlist.*;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.util.*;
import org.w3c.dom.*;

import javax.xml.xpath.XPath;
import java.io.File;
import java.util.Map;
import java.util.logging.Level;

/**
 * @author dcollins
 * @version $Id: UserPreferenceUtils.java 1171 2013-02-11 21:45:02Z dcollins $
 */
public class UserPreferenceUtils {
    public static Document createUserPreferencesDocument(KV params) {

        Document doc = WWXML.createDocumentBuilder(true).newDocument();

        Element root = doc.createElement("Preferences");
        root.setAttribute("version", Integer.toString(1));
        doc.appendChild(root);

        createUserPreferenceElements(params, root);

        return doc;
    }

    public static void getUserPreferences(Element domElement, KV params) {

        XPath xpath = WWXML.makeXPath();

        Element el = WWXML.getElement(domElement, "PropertyList", xpath);
        if (el != null)
            getPropertyList(el, params, xpath);
    }

    public static void createUserPreferenceElements(KV params, Element domElement) {
        if (params == null) {
            String message = Logging.getMessage("nullValue.ParametersIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (domElement == null) {
            String message = Logging.getMessage("nullValue.DocumentIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        Element el = WWXML.appendElementPath(domElement, "PropertyList");
        createPropertyList(params, el);
    }

    public static String getDefaultUserPreferencesPath() {
        String path = Configuration.getUserHomeDirectory();
        String name = ".sarapp/UserPreferences.xml";

        return path + File.separator + name;
    }

    public static void getDefaultUserPreferences(KV params) {
//        if (params == null) {
//            String message = Logging.getMessage("nullValue.ParametersIsNull");
//            Logging.logger().severe(message);
//            throw new IllegalArgumentException(message);
//        }

        // By default, automatically save tracks every minute.
        params.set(SARKey.ANGLE_FORMAT, Angle.ANGLE_FORMAT_DD);
        params.set(SARKey.AUTO_SAVE_TRACKS, Boolean.toString(true));
        params.set(SARKey.AUTO_SAVE_TRACKS_INTERVAL, Long.toString((long) WWMath.convertMinutesToMillis(1)));
        params.set(SARKey.ELEVATION_UNIT, SAR2.UNIT_IMPERIAL);
    }

    public static boolean getBooleanValue(KV avList, String key) {
        Object o = avList.get(key);
        if (o == null)
            return false;

        if (o instanceof Boolean)
            return (Boolean) o;

        String v = KVMap.getStringValue(avList, key);
        if (v == null)
            return false;

        try {
            return Boolean.parseBoolean(v);
        }
        catch (NumberFormatException e) {
            Logging.logger().log(Level.SEVERE, "Configuration.ConversionError", v);
            return false;
        }
    }

    protected static void getPropertyList(Element domElement, KV params, XPath xpath) {
        Element[] els = WWXML.getElements(domElement, "Property", xpath);
        if (els == null || els.length == 0)
            return;

        for (Element el : els) {
            if (el == null)
                continue;

            getProperty(el, params, xpath);
        }
    }

    protected static void getProperty(Element domElement, KV params, XPath xpath) {
        String key = WWXML.getText(domElement, "@key", xpath);
        String value = WWXML.getText(domElement, "@value", xpath);
        if (key == null || value == null)
            return;

        params.set(key, value);
    }

    protected static void createPropertyList(KV params, Element domElement) {
        for (Map.Entry<String, Object> entry : params.getEntries()) {
            if (entry == null || entry.getKey() == null || entry.getValue() == null)
                continue;

            createProperty(entry, domElement);
        }
    }

    protected static void createProperty(Map.Entry<String, Object> entry, Element domElement) {
        String s = entry.getValue().toString();
        if (s == null)
            return;

        Element el = WWXML.appendElementPath(domElement, "Property");
        el.setAttribute("key", entry.getKey());
        el.setAttribute("value", s);
    }
}