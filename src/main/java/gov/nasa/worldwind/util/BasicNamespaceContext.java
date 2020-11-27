/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.util;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import java.util.*;

/**
 * BasicNamespaceContext provides a mutable implementation of the {@link NamespaceContext}
 * interface.
 *
 * @author dcollins
 * @version $Id: BasicNamespaceContext.java 1171 2013-02-11 21:45:02Z dcollins $
 */
public class BasicNamespaceContext implements NamespaceContext {

    public static final String XLINK_NS_PREFIX = "xlink";
    public static final String XLINK_NS_URI = "http://www.w3.org/1999/xlink";

    private final Map<String, String> urisByPrefix = new HashMap<>();
    private final Map<String, Set<String>> prefixesByURI = new HashMap<>();

    /**
     * Sole constructor for BasicNamespaceContext. This configures the following namespaces:
     * <table><caption style="font-weight: bold;">Namespaces</caption>
     * <tr><th>Prefix</th><th>URI</th></tr> <tr><td>xml</td><td>http://www.w3.org/XML/1998/namespace</td></tr>
     * <tr><td>xmlns</td><td>http://www.w3.org/2000/xmlns/</td></tr>
     * <tr><td>xlink</td><td>http://www.w3.org/1999/xlink</td></tr>
     * </table>
     */
    public BasicNamespaceContext() {
        // Configure the default xml and xmlns namespaces according to the documentation of the NamespaceContext
        // interface.
        this.addNamespace(XMLConstants.XML_NS_PREFIX, XMLConstants.XML_NS_URI);
        this.addNamespace(XMLConstants.XMLNS_ATTRIBUTE, XMLConstants.XMLNS_ATTRIBUTE_NS_URI);
        this.addNamespace(XLINK_NS_PREFIX, XLINK_NS_URI);
    }

    /**
     * Adds a namepsace binding to this XML namespace context. The specified URI is bound to the specified prefix.
     *
     * @param prefix       the namespace prefix.
     * @param namespaceURI the namespace URI.
     * @throws IllegalArgumentException if either the prefix or the namepsace URI are null.
     */
    public synchronized void addNamespace(String prefix, String namespaceURI) {
        if (prefix == null) {
            String message = Logging.getMessage("nullValue.PrefixIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (namespaceURI == null) {
            String message = Logging.getMessage("nullValue.NamespaceURIIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.urisByPrefix.put(prefix, namespaceURI);

        if (this.prefixesByURI.containsKey(namespaceURI)) {
            this.prefixesByURI.get(namespaceURI).add(prefix);
        }
        else {
            Set<String> set = new HashSet<>();
            set.add(prefix);
            this.prefixesByURI.put(namespaceURI, set);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getNamespaceURI(String prefix) {
        if (prefix == null) {
            String message = Logging.getMessage("nullValue.PrefixIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        return this.urisByPrefix.getOrDefault(prefix, XMLConstants.NULL_NS_URI);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getPrefix(String namespaceURI) {
        if (namespaceURI == null) {
            String message = Logging.getMessage("nullValue.NamespaceURIIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        return this.getPrefixes(namespaceURI).next();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Iterator<String> getPrefixes(String namespaceURI) {
        if (namespaceURI == null) {
            String message = Logging.getMessage("nullValue.NamespaceURIIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        Set<String> returnSet;
        if (this.prefixesByURI.containsKey(namespaceURI)) {
            returnSet = this.prefixesByURI.get(namespaceURI);
        }
        else {
            returnSet = new HashSet<>();
        }
        return Collections.unmodifiableSet(returnSet).iterator();
    }
}