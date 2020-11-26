/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwindx.applications.sar;

import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.util.Logging;
import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.*;
import java.io.*;
import java.util.*;

/**
 * @author dcollins
 * @version $Id: SARAnnotationReader.java 1171 2013-02-11 21:45:02Z dcollins $
 */
public class SARAnnotationReader {
    private final SAXParser parser;
    private final List<SARAnnotation> sarAnnotations = new ArrayList<>();

    public SARAnnotationReader() throws ParserConfigurationException, SAXException {
        SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setNamespaceAware(true);

        this.parser = factory.newSAXParser();
    }

    public void readFile(String path) throws IOException, SAXException {
        if (path == null) {
            String msg = Logging.getMessage("nullValue.PathIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        File file = new File(path);
        if (!file.exists()) {
            String msg = Logging.getMessage("generic.FileNotFound", path);
            Logging.logger().severe(msg);
            throw new FileNotFoundException(path);
        }

        FileInputStream fis = new FileInputStream(file);
        this.doRead(fis);
    }

    public void readStream(InputStream stream) throws IOException, SAXException {
        if (stream == null) {
            String msg = Logging.getMessage("nullValue.InputStreamIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        this.doRead(stream);
    }

    public List<SARAnnotation> getSARAnnotations() {
        return this.sarAnnotations;
    }

    private void doRead(InputStream fis) throws IOException, SAXException {
        this.parser.parse(fis, new Handler());
    }

    private static class SARAnnotationElement extends ElementParser {
        private double latitutde;
        private double longitude;
        private String id;
        private String text;

        public SARAnnotationElement(String uri, String lname, String qname, Attributes attributes) {
            super("sarAnnotation");
            // don't perform validation here - no parameters are actually used
        }

        public SARAnnotation getSARAnnotation() {
            Position pos = Position.fromDegrees(this.latitutde, this.longitude, 0);
            SARAnnotation sa = new SARAnnotation(this.text, pos);
            sa.setId(this.id);
            return sa;
        }

        @Override
        public void doStartElement(String uri, String lname, String qname, Attributes attributes) {
            // don't perform validation here - no parameters are actually used
        }

        /**
         * @param uri
         * @param lname
         * @param qname
         * @throws IllegalArgumentException if <code>lname</code> is null
         */
        @Override
        public void doEndElement(String uri, String lname, String qname) {
            if (lname == null) {
                String msg = Logging.getMessage("nullValue.LNameIsNull");
                Logging.logger().severe(msg);
                throw new IllegalArgumentException(msg);
            }
            // don't validate uri or qname - they aren't used.

            if (lname.equalsIgnoreCase("latitude")) {
                this.latitutde = Double.parseDouble(this.currentCharacters);
            }
            else if (lname.equalsIgnoreCase("longitude")) {
                this.longitude = Double.parseDouble(this.currentCharacters);
            }
            else if (lname.equalsIgnoreCase("id")) {
                this.id = this.currentCharacters.trim();
            }
            else if (lname.equalsIgnoreCase("text")) {
                this.text = this.currentCharacters.trim();
            }
        }
    }

    private class Handler extends DefaultHandler {
        // this is a private class used solely by the containing class, so no validation occurs in it.

        private ElementParser currentElement = null;
        private boolean firstElement = true;

        @Override
        public void warning(SAXParseException saxParseException) throws SAXException {
            saxParseException.printStackTrace();
            super.warning(saxParseException);
        }

        @Override
        public void error(SAXParseException saxParseException) throws SAXException {
            saxParseException.printStackTrace();
            super.error(saxParseException);
        }

        @Override
        public void fatalError(SAXParseException saxParseException) throws SAXException {
            saxParseException.printStackTrace();
            super.fatalError(saxParseException);
        }

        @Override
        public void startElement(String uri, String lname, String qname, Attributes attributes) {
            if (this.firstElement) {
                if (!lname.equalsIgnoreCase("sarTrackAnnotations"))
                    throw new IllegalArgumentException("Not a SAR Track Annotations file");
                else
                    this.firstElement = false;
            }

            if (this.currentElement != null) {
                this.currentElement.startElement(uri, lname, qname, attributes);
            }
            else if (lname.equalsIgnoreCase("sarAnnotation")) {
                this.currentElement = new SARAnnotationElement(uri, lname, qname, attributes);
            }
        }

        @Override
        public void endElement(String uri, String lname, String qname) {
            if (this.currentElement != null) {
                this.currentElement.endElement(uri, lname, qname);

                if (lname.equalsIgnoreCase(this.currentElement.getElementName())) {
                    // Get the SARAnnotation once the element is completely constructed.
                    if (this.currentElement instanceof SARAnnotationElement)
                        SARAnnotationReader.this.sarAnnotations.add(
                            ((SARAnnotationElement) this.currentElement).getSARAnnotation());
                    this.currentElement = null;
                }
            }
        }

        @Override
        public void characters(char[] data, int start, int length) {
            if (this.currentElement != null)
                this.currentElement.characters(data, start, length);
        }
    }
}
