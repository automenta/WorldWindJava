/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.layers.rpf;

import gov.nasa.worldwind.*;
import gov.nasa.worldwind.formats.dds.DDSCompressor;
import gov.nasa.worldwind.retrieve.Retriever;
import gov.nasa.worldwind.util.Logging;

import java.awt.image.*;
import java.io.IOException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.logging.Level;

/**
 * @author dcollins
 * @version $Id: RPFRetriever.java 1171 2013-02-11 21:45:02Z dcollins $
 */
class RPFRetriever extends WWObjectImpl implements Retriever {
    public static final int RESPONSE_CODE_OK = 1;
    public static final int RESPONSE_CODE_NO_CONTENT = 2;
    private final AtomicInteger contentLengthRead = new AtomicInteger(0);
    private final RPFGenerator.RPFServiceInstance service;
    private final URL url;
    private final Function<Retriever, ByteBuffer> postProcessor;
    private volatile ByteBuffer byteBuffer;
    private volatile int contentLength;
    private volatile String state = Retriever.RETRIEVER_STATE_NOT_STARTED;
    private volatile String contentType;
    private long submitTime;
    private long beginTime;
    private long endTime;
    private int connectTimeout = -1;
    private int readTimeout = -1;
    private int staleRequestLimit = -1;
    private int responseCode;

    public RPFRetriever(RPFGenerator.RPFServiceInstance service, URL url, Function<Retriever, ByteBuffer> postProcessor) {

        this.service = service;
        this.url = url;
        this.postProcessor = postProcessor;
    }

    public final ByteBuffer getBuffer() {
        return this.byteBuffer;
    }

    public final int getContentLength() {
        return this.contentLength;
    }

    public final int getContentLengthRead() {
        return this.contentLengthRead.get();
    }

    protected void setContentLengthRead(int length) {
        this.contentLengthRead.set(length);
    }

    public final String getName() {
        return this.url.toString();
    }

    public final String getState() {
        return this.state;
    }

    private void setState(String state) {
        String oldState = this.state;
        this.state = state;
        this.emit(Keys.RETRIEVER_STATE, oldState, this.state);
    }

    public final String getContentType() {
        return this.contentType;
    }

    /**
     * {@inheritDoc}
     *
     * @return Always returns zero (no expiration).
     */
    public long getExpirationTime() {
        return 0;
    }

    public long getSubmitEpoch() {
        return this.submitTime;
    }

    public void setSubmitEpoch(long submitTime) {
        this.submitTime = submitTime;
    }

    public long getBeginTime() {
        return this.beginTime;
    }

    public void setBeginTime(long beginTime) {
        this.beginTime = beginTime;
    }

    public long getEndTime() {
        return this.endTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    public int getConnectTimeout() {
        return this.connectTimeout;
    }

    public void setConnectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public int getReadTimeout() {
        return this.readTimeout;
    }

    public void setReadTimeout(int readTimeout) {
        this.readTimeout = readTimeout;
    }

    public int getStaleRequestLimit() {
        return this.staleRequestLimit;
    }

    public void setStaleRequestLimit(int staleRequestLimit) {
        this.staleRequestLimit = staleRequestLimit;
    }

    public final RPFGenerator.RPFServiceInstance getService() {
        return this.service;
    }

    public final URL getURL() {
        return this.url;
    }

    public final Function<Retriever, ByteBuffer> getPostProcessor() {
        return this.postProcessor;
    }

    public int getResponseCode() {
        return this.responseCode;
    }

    public final Retriever call() {

        try {

            if (interrupted())
                return this;

            setState(Retriever.RETRIEVER_STATE_READING);

            this.byteBuffer = read();

            setState(Retriever.RETRIEVER_STATE_SUCCESSFUL);
        }
        catch (Exception e) {
            setState(Retriever.RETRIEVER_STATE_ERROR);
            Logging.logger().log(Level.SEVERE,
                Logging.getMessage("URLRetriever.ErrorAttemptingToRetrieve", this.url.toString()), e);
        }
        finally {
            end();
        }

        return this;
    }

    private boolean interrupted() {
        if (Thread.currentThread().isInterrupted()) {
            setState(Retriever.RETRIEVER_STATE_INTERRUPTED);
            String message = Logging.getMessage("URLRetriever.RetrievalInterruptedFor", this.url.toString());
            Logging.logger().fine(message);
            return true;
        }
        return false;
    }

    private void end() {
        try {
            if (this.postProcessor != null) {
                this.byteBuffer = this.postProcessor.apply(this);
            }
        }
        catch (RuntimeException e) {
            setState(Retriever.RETRIEVER_STATE_ERROR);
            Logging.logger().log(Level.SEVERE,
                Logging.getMessage("Retriever.ErrorPostProcessing", this.url.toString()), e);
            throw e;
        }
    }

    private ByteBuffer read() throws IOException {
        ByteBuffer buffer = this.doRead(this.service, this.url);
        if (buffer == null)
            this.contentLength = 0;
        return buffer;
    }

    protected ByteBuffer doRead(RPFGenerator.RPFServiceInstance service, URL url) throws IOException {
        ByteBuffer buffer = null;

        BufferedImage bufferedImage = service.serviceRequest(url);
        if (bufferedImage != null) {
            // TODO: format parameter should determine image type
            buffer = DDSCompressor.compressImage(bufferedImage);
            if (buffer != null) {
                int length = buffer.limit();
                this.contentType = "image/dds";
                this.contentLength = length;
                setContentLengthRead(length);
            }
        }

        // TODO: service should provide response code
        this.responseCode = buffer != null ? RPFRetriever.RESPONSE_CODE_OK : RPFRetriever.RESPONSE_CODE_NO_CONTENT;

        return buffer;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        final RPFRetriever that = (RPFRetriever) o;

        // Retrievers are considered identical if they are for the same URL. This convention is used by the
        // retrieval service to filter out duplicate retreival requests.
        return !(url != null ? !url.toString().contentEquals(that.url.toString()) : that.url != null);
    }

    @Override
    public int hashCode() {
        int result;
        result = (url != null ? url.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return this.getName() != null ? this.getName() : super.toString();
    }
}