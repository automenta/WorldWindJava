/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwindx.applications.sar;

import gov.nasa.worldwind.util.Logging;
import org.w3c.dom.*;

import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;

/**
 * @author dcollins
 * @version $Id: SARAnnotationWriter.java 1171 2013-02-11 21:45:02Z dcollins $
 */
public class SARAnnotationWriter {
    private final Document doc;
    private final Result result;

    public SARAnnotationWriter(String path) throws ParserConfigurationException {
        if (path == null) {
            String msg = Logging.getMessage("nullValue.PathIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        this.doc = factory.newDocumentBuilder().newDocument();
        this.result = new StreamResult(new File(path));
        createAnnotationsDocument(this.doc);
    }

    public SARAnnotationWriter(OutputStream stream) throws ParserConfigurationException {
        if (stream == null) {
            String msg = Logging.getMessage("nullValue.InputStreamIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        this.doc = factory.newDocumentBuilder().newDocument();
        this.result = new StreamResult(stream);
        createAnnotationsDocument(this.doc);
    }

    public void writeAnnotation(SARAnnotation sarAnnotation) {
        if (sarAnnotation == null) {
            String msg = "nullValue.SARAnnotationIsNull";
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        doWriteAnnotation(sarAnnotation, this.doc.getDocumentElement());
    }

    public void writeAnnotations(Iterable<SARAnnotation> sarAnnotations)
        throws TransformerException {
        if (sarAnnotations == null) {
            String msg = "nullValue.SARAnnotationIterableIsNull";
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        for (SARAnnotation sa : sarAnnotations) {
            if (sa != null)
                doWriteAnnotation(sa, this.doc.getDocumentElement());
        }
        doFlush();
    }

    public void close() {
        // Intentionally left blank,
        // as a placeholder for future functionality.
    }

    private void createAnnotationsDocument(Document doc) {
        // Create the GPX document root when the document
        // doesn't already have a root element.
        if (doc != null) {
            if (doc.getDocumentElement() != null)
                doc.removeChild(doc.getDocumentElement());

            Element annotations = doc.createElement("sarTrackAnnotations");
            doc.appendChild(annotations);
        }
    }

    private void doWriteAnnotation(SARAnnotation sarAnnotation, Node elem) {
        if (sarAnnotation != null) {
            Element anno = this.doc.createElement("sarAnnotation");

            if (sarAnnotation.getPosition() != null) {
                Element lat = this.doc.createElement("latitude");
                Text latText = this.doc.createTextNode(
                    Double.toString(sarAnnotation.getPosition().getLatitude().degrees));
                lat.appendChild(latText);
                anno.appendChild(lat);

                Element lon = this.doc.createElement("longitude");
                Text lonText = this.doc.createTextNode(
                    Double.toString(sarAnnotation.getPosition().getLongitude().degrees));
                lon.appendChild(lonText);
                anno.appendChild(lon);
            }

            if (sarAnnotation.getId() != null) {
                Element id = this.doc.createElement("id");
                Text idText = this.doc.createTextNode(sarAnnotation.getId());
                id.appendChild(idText);
                anno.appendChild(id);
            }

            if (sarAnnotation.getText() != null) {
                Element text = this.doc.createElement("text");
                CDATASection cdata = this.doc.createCDATASection(sarAnnotation.getText());
                text.appendChild(cdata);
                anno.appendChild(text);
            }

            elem.appendChild(anno);
        }
    }

    private void doFlush() throws TransformerException {
        TransformerFactory factory = TransformerFactory.newInstance();
        Transformer transformer = factory.newTransformer();
        Source source = new DOMSource(this.doc);
        transformer.transform(source, this.result);
    }
}
