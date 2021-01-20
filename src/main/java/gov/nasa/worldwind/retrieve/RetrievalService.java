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

    boolean hasActiveTasks();

    boolean isAvailable();

    void shutdown(boolean immediately);
}