/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */

package gov.nasa.worldwind.retrieve;

import gov.nasa.worldwind.avlist.KV;

import java.nio.ByteBuffer;
import java.util.function.Function;

/**
 * @author tag
 * @version $Id: RetrieverFactory.java 1171 2013-02-11 21:45:02Z dcollins $
 */
public interface RetrieverFactory {
    Retriever retriever(KV params, Function<Retriever, ByteBuffer> postProcessor);
}