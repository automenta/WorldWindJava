/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */

package gov.nasa.worldwind.layers.ogc.collada.io;

import gov.nasa.worldwind.util.Logging;
import gov.nasa.worldwind.util.xml.XMLDoc;

import java.io.InputStream;
import java.net.URI;

/**
 * Represents a COLLADA document read from an input stream.
 *
 * @author pabercrombie
 * @version $Id: ColladaInputStream.java 660 2012-06-26 16:13:11Z pabercrombie $
 */
public class ColladaInputStream extends XMLDoc {
    /**
     * The {@link InputStream} specified to the constructor.
     */
    protected InputStream inputStream;

    /**
     * The URI of this COLLADA document. May be {@code null}.
     */
    protected URI uri;

    /**
     * Construct a <code>ColladaInputStream</code> instance.
     *
     * @param sourceStream the COLLADA stream.
     * @param uri          the URI of this COLLADA document. This URI is used to resolve relative references. May be
     *                     {@code null}.
     * @throws IllegalArgumentException if the specified input stream is null.
     */
    public ColladaInputStream(InputStream sourceStream, URI uri) {
        if (sourceStream == null) {
            String message = Logging.getMessage("nullValue.InputStreamIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.inputStream = sourceStream;
        this.uri = uri;
    }

    /**
     * Returns the input stream reference passed to the constructor.
     *
     * @return the input stream reference passed to the constructor.
     */
    public InputStream getInputStream() {
        return this.inputStream;
    }

    /**
     * {@inheritDoc}
     */
    public String getSupportFilePath(String path) {
        if (path == null) {
            String message = Logging.getMessage("nullValue.FilePathIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (this.uri != null) {
            URI remoteFile = uri.resolve(path);
            return remoteFile.toString();
        }
        return null;
    }

    @Override
    public InputStream getSupportFileStream(String path) {
        return null;
    }
}
