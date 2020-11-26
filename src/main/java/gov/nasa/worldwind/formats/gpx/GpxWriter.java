/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.formats.gpx;

import gov.nasa.worldwind.tracks.*;
import gov.nasa.worldwind.util.Logging;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Text;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.OutputStream;

/**
 * @author dcollins
 * @version $Id: GpxWriter.java 1171 2013-02-11 21:45:02Z dcollins $
 */
public class GpxWriter {
    private final Document doc;
    private final Result result;

    public GpxWriter(String path) throws ParserConfigurationException {
        if (path == null) {
            String msg = Logging.getMessage("nullValue.PathIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        this.doc = factory.newDocumentBuilder().newDocument();
        this.result = new StreamResult(new File(path));
        createGpxDocument(this.doc);
    }

    public GpxWriter(OutputStream stream) throws ParserConfigurationException {
        if (stream == null) {
            String msg = Logging.getMessage("nullValue.InputStreamIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        this.doc = factory.newDocumentBuilder().newDocument();
        this.result = new StreamResult(stream);
        createGpxDocument(this.doc);
    }

    public void writeTrack(Track track) throws TransformerException {
        if (track == null) {
            String msg = Logging.getMessage("nullValue.TrackIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        doWriteTrack(track, this.doc.getDocumentElement());
        doFlush();
    }

    public void close() {
        // Intentionally left blank,
        // as a placeholder for future functionality.
    }

    private void createGpxDocument(Document doc) {
        // Create the GPX document root when the document
        // doesn't already have a root element.
        if (doc != null) {
            if (doc.getDocumentElement() != null)
                doc.removeChild(doc.getDocumentElement());

            doc.setXmlStandalone(false);

            Element gpx = doc.createElement("gpx");
            gpx.setAttribute("version", "1.1");
            gpx.setAttribute("creator", "NASA WorldWind");
            doc.appendChild(gpx);
        }
    }

    private void doWriteTrack(Track track, Node elem) {
        if (track != null) {
            Element trk = this.doc.createElement("trk");

            if (track.getName() != null) {
                Element name = this.doc.createElement("name");
                Text nameText = this.doc.createTextNode(track.getName());
                name.appendChild(nameText);
                trk.appendChild(name);
            }

            if (track.getSegments() != null) {
                for (TrackSegment ts : track.getSegments()) {
                    doWriteTrackSegment(ts, trk);
                }
            }

            elem.appendChild(trk);
        }
    }

    private void doWriteTrackSegment(TrackSegment segment, Node elem) {
        if (segment != null) {
            Element trkseg = this.doc.createElement("trkseg");

            if (segment.getPoints() != null) {
                for (TrackPoint tp : segment.getPoints()) {
                    doWriteTrackPoint(tp, trkseg);
                }
            }

            elem.appendChild(trkseg);
        }
    }

    private void doWriteTrackPoint(TrackPoint point, Node elem) {
        if (point != null) {
            Element trkpt = this.doc.createElement("trkpt");
            trkpt.setAttribute("lat", Double.toString(point.getLatitude()));
            trkpt.setAttribute("lon", Double.toString(point.getLongitude()));

            Element ele = this.doc.createElement("ele");
            Text eleText = this.doc.createTextNode(Double.toString(point.getElevation()));
            ele.appendChild(eleText);
            trkpt.appendChild(ele);

            if (point.getTime() != null) {
                Element time = this.doc.createElement("time");
                Text timeText = this.doc.createTextNode(point.getTime());
                time.appendChild(timeText);
                trkpt.appendChild(time);
            }

            elem.appendChild(trkpt);
        }
    }

    private void doFlush() throws TransformerException {
        TransformerFactory factory = TransformerFactory.newInstance();
        Transformer transformer = factory.newTransformer();
        Source source = new DOMSource(this.doc);
        transformer.transform(source, this.result);
    }
}
