/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */

package gov.nasa.worldwind.video;

import com.jogamp.opengl.GLAutoDrawable;
import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.cache.GpuResourceCache;

/**
 * @author tag
 * @version $Id: WorldWindowGLDrawable.java 1855 2014-02-28 23:01:02Z tgaskins $
 */
public interface WorldWindowGLDrawable extends WorldWindow {
    void initDrawable(GLAutoDrawable g, WorldWindow w);

}
