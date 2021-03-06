/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */

package gov.nasa.worldwind.ui.tree;

import com.jogamp.opengl.GL2;
import gov.nasa.worldwind.Keys;
import gov.nasa.worldwind.pick.PickSupport;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.util.OGLUtil;

import java.awt.*;

/**
 * Utility methods for drawing tree controls.
 *
 * @author pabercrombie
 * @version $Id: TreeUtil.java 1171 2013-02-11 21:45:02Z dcollins $
 */
public class TreeUtil {
    /**
     * Draw a rectangle in a unique pick color, and associate the color with a pickable object.
     *
     * @param dc           Draw context.
     * @param pickSupport  Pick support.
     * @param pickedObject Object to associate with pickable rectangle.
     * @param bounds       Bounds of the pickable rectangle.
     */
    public static void drawPickableRect(DrawContext dc, PickSupport pickSupport, Object pickedObject,
        Rectangle bounds) {

        GL2 gl = dc.getGL2(); // GL initialization checks for GL2 compatibility.

        Color color = dc.getUniquePickColor();
        int colorCode = color.getRGB();
        pickSupport.addPickableObject(colorCode, pickedObject);
        gl.glColor3ub((byte) color.getRed(), (byte) color.getGreen(), (byte) color.getBlue());

        TreeUtil.drawRect(gl, bounds);
    }

    /**
     * Draw a rectangle.
     *
     * @param gl     GL
     * @param bounds Bounds of the rectangle, in GL coordinates.
     */
    public static void drawRect(GL2 gl, Rectangle bounds) {

        gl.glRecti(bounds.x, bounds.y, bounds.x + bounds.width, bounds.y + bounds.height);
    }

    public static void drawRectWithGradient(GL2 gl, Rectangle bounds, Color color1, Color color2, double opacity,
        String gradientDirection) {

        gl.glBegin(GL2.GL_QUADS);

        if (Keys.HORIZONTAL.equals(gradientDirection)) {
            OGLUtil.applyColor(gl, color1, opacity, false);
            gl.glVertex2d(bounds.getMinX(), bounds.getMaxY());
            gl.glVertex2d(bounds.getMinX(), bounds.getMinY());

            OGLUtil.applyColor(gl, color2, opacity, false);
            gl.glVertex2d(bounds.getMaxX(), bounds.getMinY());
            gl.glVertex2d(bounds.getMaxX(), bounds.getMaxY());
        } else {
            OGLUtil.applyColor(gl, color1, opacity, false);
            gl.glVertex2d(bounds.getMaxX(), bounds.getMaxY());
            gl.glVertex2d(bounds.getMinX(), bounds.getMaxY());

            OGLUtil.applyColor(gl, color2, opacity, false);
            gl.glVertex2d(bounds.getMinX(), bounds.getMinY());
            gl.glVertex2d(bounds.getMaxX(), bounds.getMinY());
        }
        gl.glEnd();
    }
}