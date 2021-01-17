/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.retrieve;

import gov.nasa.worldwind.WWObject;

/**
 * @author Tom Gaskins
 * @version $Id: RetrievalService.java 1171 2013-02-11 21:45:02Z dcollins $
 */
public interface RetrievalService extends WWObject {

    RetrievalFuture run(Retriever retriever, double priority);
//
//    int getRetrieverPoolSize();
//
//    void setRetrieverPoolSize(int poolSize);

    boolean hasActiveTasks();

    boolean isAvailable();

    void shutdown(boolean immediately);

//    /**
//     * Indicates the listener to be called when {@link javax.net.ssl.SSLHandshakeException}s are thrown during resource
//     * retrieval.
//     *
//     * @return the exception listener, or null if no listener has been specified.
//     */
//    SSLExceptionListener getSSLExceptionListener();

//    /**
//     * Specifies the listener called when a {@link javax.net.ssl.SSLHandshakeException} is thrown during resource
//     * retrieval.
//     *
//     * @param listener to listener to invoke, or null if no listener is to be invoked.
//     */
//    void setSSLExceptionListener(SSLExceptionListener listener);

//    interface SSLExceptionListener {
//        void onException(Throwable e, String path);
//    }
}
