/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.retrieve;

import gov.nasa.worldwind.Configuration;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.function.Function;

/**
 * @author Tom Gaskins
 * @version $Id: HTTPRetriever.java 1171 2013-02-11 21:45:02Z dcollins $
 */
public class HTTPRetriever extends URLRetriever {


    private int responseCode;
    private String responseMessage;

    public HTTPRetriever(URL url, Function<Retriever, ByteBuffer> postProcessor) {
        super(url, postProcessor);
    }

    @Override
    protected URLConnection connection() throws IOException {
        URLConnection u = super.connection();
        u.setRequestProperty("User-Agent", Configuration.userAgent);
        return u;
    }

    public int getResponseCode() {
        return this.responseCode;
    }

    public String getResponseMessage() {
        return this.responseMessage;
    }

    @Override
    protected ByteBuffer doRead(URLConnection connection) throws Exception, IOException {

        HttpURLConnection htpc = (HttpURLConnection) connection;
        this.responseCode = htpc.getResponseCode();
        this.responseMessage = htpc.getResponseMessage();
//        String contentType = connection.getContentType();

        return this.responseCode == HttpURLConnection.HTTP_OK ? super.doRead(connection) : null;
    }
}
