/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.retrieve;

import gov.nasa.worldwind.WWObject;

import java.nio.ByteBuffer;
import java.util.concurrent.Callable;

import static gov.nasa.worldwind.retrieve.BasicRetrievalService.DEFAULT_TIME_PRIORITY_GRANULARITY;

/**
 * @author Tom Gaskins
 * @version $Id: Retriever.java 1171 2013-02-11 21:45:02Z dcollins $
 */
public interface Retriever extends WWObject, Callable<Retriever> {
    String RETRIEVER_STATE_NOT_STARTED = "gov.nasa.worldwind.RetrieverStatusNotStarted";
    String RETRIEVER_STATE_CONNECTING = "gov.nasa.worldwind.RetrieverStatusConnecting";
    String RETRIEVER_STATE_READING = "gov.nasa.worldwind.RetrieverStatusReading";
    String RETRIEVER_STATE_INTERRUPTED = "gov.nasa.worldwind.RetrieverStatusInterrupted";
    String RETRIEVER_STATE_ERROR = "gov.nasa.worldwind.RetrieverStatusError";
    String RETRIEVER_STATE_SUCCESSFUL = "gov.nasa.worldwind.RetrieverStatusSuccessful";

    ByteBuffer getBuffer();

    String getName();

    String getState();

    String getContentType();

    /**
     * Indicates the expiration time of the resource retrieved by this Retriever.
     *
     * @return The expiration time of the resource, in milliseconds since the Epoch (January 1, 1970, 00:00:00 GMT).
     * Zero indicates that there is no expiration time.
     */
    long getExpirationTime();

    long getSubmitEpoch();

    void setSubmitEpoch(long submitTime);

    int getConnectTimeout();

    void setConnectTimeout(int connectTimeout);

    int getReadTimeout();

    void setReadTimeout(int readTimeout);

    int getStaleRequestLimit();

    void setStaleRequestLimit(int staleRequestLimit);

    default void setSubmitEpochNow() {
        setSubmitEpoch(System.currentTimeMillis() / DEFAULT_TIME_PRIORITY_GRANULARITY);
    }
}
