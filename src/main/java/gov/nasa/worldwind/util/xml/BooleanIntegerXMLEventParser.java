/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */

package gov.nasa.worldwind.util.xml;

import gov.nasa.worldwind.util.WWUtil;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;

/**
 * @author tag
 * @version $Id: BooleanIntegerXMLEventParser.java 1171 2013-02-11 21:45:02Z dcollins $
 */
public class BooleanIntegerXMLEventParser extends AbstractXMLEventParser {
    public BooleanIntegerXMLEventParser() {
    }

    public BooleanIntegerXMLEventParser(String namespaceUri) {
        super(namespaceUri);
    }

    public Object parse(XMLEventParserContext ctx, XMLEvent booleanEvent, Object... args) throws XMLStreamException {
        String s = AbstractXMLEventParser.parseCharacterContent(ctx, booleanEvent);
        if (s == null)
            return false;

        s = s.trim();

        if (s.length() > 1)
            return s.equalsIgnoreCase("true");

        return WWUtil.convertNumericStringToBoolean(s);
    }
}
