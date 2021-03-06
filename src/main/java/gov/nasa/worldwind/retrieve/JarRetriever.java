/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */

package gov.nasa.worldwind.retrieve;

import gov.nasa.worldwind.util.Logging;

import java.net.*;
import java.nio.ByteBuffer;
import java.util.function.Function;
import java.util.logging.Level;

/**
 * Retrieves resources identified by a jar url, which has the form jar:&lt;url&gt;!/{entry}, as in:
 * jar:http://www.foo.com/bar/baz.jar!/COM/foo/Quux.class. See {@link JarURLConnection} for a full description of jar
 * URLs.
 *
 * @author tag
 * @version $Id: JarRetriever.java 1171 2013-02-11 21:45:02Z dcollins $
 */
public class JarRetriever extends URLRetriever {
    private int responseCode;
    private String responseMessage;

    public JarRetriever(URL url, Function<Retriever, ByteBuffer> postProcessor) {
        super(url, postProcessor);
    }

    public int getResponseCode() {
        return this.responseCode;
    }

    public String getResponseMessage() {
        return this.responseMessage;
    }

    protected ByteBuffer doRead(URLConnection connection) throws Exception {

        this.responseCode = connection.getContentLength() >= 0 ? HttpURLConnection.HTTP_OK : -1;
        this.responseMessage = this.responseCode >= 0 ? "OK" : "FAILED";

        String contentType = connection.getContentType();
        Logging.logger().log(Level.FINE, "HTTPRetriever.ResponseInfo", new Object[] {this.responseCode,
            connection.getContentLength(), contentType != null ? contentType : "content type not returned",
            connection.getURL()});

        if (this.responseCode == HttpURLConnection.HTTP_OK) // intentionally re-using HTTP constant
            return super.doRead(connection);

        return null;
    }
}
