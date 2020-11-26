/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.examples;

/**
 * Illustrates how to configure and display WorldWind <code>{@link gov.nasa.worldwind.examples.render.SurfaceShape}s</code>.
 * Surface shapes are used to visualize flat standard shapes types that follow the terrain. This illustrates how to use
 * all 7 standard surface shape types:
 * <ul> <li><code>{@link gov.nasa.worldwind.examples.render.SurfacePolygon}</code></li> <li><code>{@link
 * gov.nasa.worldwind.examples.render.SurfaceEllipse}</code></li> <li><code>{@link gov.nasa.worldwind.examples.render.SurfaceCircle}</code></li>
 * <li><code>{@link gov.nasa.worldwind.examples.render.SurfaceQuad}</code></li> <li><code>{@link
 * gov.nasa.worldwind.examples.render.SurfaceSquare}</code></li> <li><code>{@link gov.nasa.worldwind.examples.render.SurfaceSector}</code></li>
 * <li><code>{@link gov.nasa.worldwind.examples.render.SurfacePolyline}</code></li> </ul>
 *
 * @author dcollins
 * @version $Id: SurfaceShapes.java 1171 2013-02-11 21:45:02Z dcollins $
 */
public class SurfaceShapes extends DraggingShapes {
    public static void main(String[] args) {
        ApplicationTemplate.start("WorldWind Surface Shapes", AppFrame.class);
    }
}
