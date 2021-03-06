/*
 * Copyright (C) 2014 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */

package gov.nasa.worldwind.layers.ogc.ows;

import gov.nasa.worldwind.util.xml.*;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;
import java.util.*;

/**
 * @author tag
 * @version $Id: OWSParameter.java 2061 2014-06-19 19:59:40Z tgaskins $
 */
public class OWSParameter extends AbstractXMLEventParser {
    protected final List<OWSAllowedValues> allowedValues = new ArrayList<>(1);

    public OWSParameter(String namespaceURI) {
        super(namespaceURI);
    }

    public String getName() {
        return (String) this.getField("name");
    }

    public List<OWSAllowedValues> getAllowedValues() {
        return this.allowedValues;
    }

    protected void doParseEventContent(XMLEventParserContext ctx, XMLEvent event, Object... args)
        throws XMLStreamException {
        if (ctx.isStartElement(event, "AllowedValues")) {
            XMLEventParser parser = this.allocate(ctx, event);
            if (parser != null) {
                Object o = parser.parse(ctx, event, args);
                if (o instanceof OWSAllowedValues)
                    this.allowedValues.add((OWSAllowedValues) o);
            }
        } else {
            super.doParseEventContent(ctx, event, args);
        }
    }
}
