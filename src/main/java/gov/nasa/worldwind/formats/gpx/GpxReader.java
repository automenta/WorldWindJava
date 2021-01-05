/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.formats.gpx;

import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.tracks.*;
import gov.nasa.worldwind.util.Logging;
import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.*;
import java.io.*;
import java.util.*;

/**
 * @author tag
 * @version $Id: GpxReader.java 1171 2013-02-11 21:45:02Z dcollins $
 */
public class GpxReader // TODO: I18N, proper exception handling, remove stack-trace prints
{
    private final SAXParser parser;
    private final List<Track> tracks = new ArrayList<>();

    public GpxReader() throws ParserConfigurationException, SAXException {
        SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setNamespaceAware(true);

        this.parser = factory.newSAXParser();
    }

    /**
     * @param path The file spec to read from.
     * @throws IllegalArgumentException if <code>path</code> is null
     * @throws IOException              if no file exists at the location specified by <code>path</code>
     * @throws SAXException             if a parsing error occurs.
     */
    public void readFile(String path) throws IOException, SAXException, FileNotFoundException {
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

    /**
     * @param stream The stream to read from.
     * @throws IllegalArgumentException if <code>stream</code> is null
     * @throws IOException              if a problem is encountered reading the stream.
     * @throws SAXException             if a parsing error occurs.
     */
    public void readStream(InputStream stream) throws IOException, SAXException {
        if (stream == null) {
            String msg = Logging.getMessage("nullValue.InputStreamIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        this.doRead(stream);
    }

    public List<Track> getTracks() {
        return this.tracks;
    }

    public Iterator<Position> getTrackPositionIterator() {
        return new Iterator<>() {
            private final Iterator<TrackPoint> trackPoints = new TrackPointIteratorImpl(GpxReader.this.tracks);

            public boolean hasNext() {
                return this.trackPoints.hasNext();
            }

            public Position next() {
                return this.trackPoints.next().getPosition();
            }

            public void remove() {
                this.trackPoints.remove();
            }
        };
    }

    private void doRead(InputStream fis) throws IOException, SAXException {
        this.parser.parse(fis, new Handler());
    }

    private class Handler extends DefaultHandler {
        // this is a private class used solely by the containing class, so no validation occurs in it.

        private ElementParser currentElement;
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
                if (!lname.equalsIgnoreCase("gpx"))
                    throw new IllegalArgumentException(Logging.getMessage("formats.notGPX", uri));
                else
                    this.firstElement = false;
            }

            if (this.currentElement != null) {
                this.currentElement.startElement(uri, lname, qname, attributes);
            } else if (lname.equalsIgnoreCase("trk")) {
                GpxTrack track = new GpxTrack(uri, lname, qname, attributes);
                this.currentElement = track;
                GpxReader.this.tracks.add(track);
            } else if (lname.equalsIgnoreCase("rte")) {
                GpxRoute route = new GpxRoute(uri, lname, qname, attributes);
                this.currentElement = route;
                GpxReader.this.tracks.add(route);
            }
        }

        @Override
        public void endElement(String uri, String lname, String qname) {
            if (this.currentElement != null) {
                this.currentElement.endElement(uri, lname, qname);

                if (lname.equalsIgnoreCase(this.currentElement.getElementName()))
                    this.currentElement = null;
            }
        }

        @Override
        public void characters(char[] data, int start, int length) {
            if (this.currentElement != null)
                this.currentElement.characters(data, start, length);
        }
    }
}
