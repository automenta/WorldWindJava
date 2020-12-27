/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.retrieve;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;

/**
 * @author Tom Gaskins
 * @version $Id: HTTPRetriever.java 1171 2013-02-11 21:45:02Z dcollins $
 */
public class HTTPRetriever extends URLRetriever {

    static final String userAgent_Default = "Mozilla/5.0 (X11; Linux x86_64; rv:68.0) Gecko/20100101 Firefox/68.0";

    private int responseCode;
    private String responseMessage;

    public HTTPRetriever(URL url, RetrievalPostProcessor postProcessor) {
        super(url, postProcessor);
    }

    @Override
    protected URLConnection connection() throws IOException {
        URLConnection u = super.connection();
        u.setRequestProperty("User-Agent", HTTPRetriever.userAgent_Default);
        return u;
    }

    public int getResponseCode() {
        return this.responseCode;
    }

    public String getResponseMessage() {
        return this.responseMessage;
    }

    @Override
    protected ByteBuffer doRead(URLConnection connection) throws Exception {

        HttpURLConnection htpc = (HttpURLConnection) connection;
        this.responseCode = htpc.getResponseCode();
        this.responseMessage = htpc.getResponseMessage();
//        String contentType = connection.getContentType();

        return this.responseCode == HttpURLConnection.HTTP_OK ? super.doRead(connection) : null;
    }
}
