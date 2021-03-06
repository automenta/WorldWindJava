/*
 * Copyright (C) 2013 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */

package gov.nasa.worldwind.examples;

import com.jogamp.opengl.*;
import com.jogamp.opengl.awt.GLCanvas;

import java.awt.*;

/**
 * Determines whether a device supports the OpenGL features necessary for WorldWind.
 *
 * @author tag
 * @version $Id: OpenGLSupportTest.java 1675 2013-10-18 00:32:15Z tgaskins $
 */
public class OpenGLSupportTest implements GLEventListener {
    public static void main(String[] args) {
        Frame frame = new Frame("OpenGL Support Test");
        frame.setSize(200, 200);
        frame.setLayout(new BorderLayout());

        GLCapabilities caps = new GLCapabilities(GLProfile.getMaxFixedFunc(true));

        caps.setAlphaBits(8);
        caps.setRedBits(8);
        caps.setGreenBits(8);
        caps.setBlueBits(8);
        caps.setDepthBits(24);
        caps.setDoubleBuffered(true);
        GLCanvas canvas = new GLCanvas(caps);

        GLEventListener testClass = new OpenGLSupportTest();
        canvas.addGLEventListener(testClass);

        frame.add(canvas, BorderLayout.CENTER);
        frame.validate();
        frame.setVisible(true);
    }

    @Override
    public void init(GLAutoDrawable glAutoDrawable) {
        int status = 0;

        for (String funcName : OpenGLSupportTest.getRequiredOglFunctions()) {
            if (!glAutoDrawable.getGL().isFunctionAvailable(funcName)) {
                System.out.println("OpenGL function " + funcName + " is not available.");
                status = 1;
            }
        }

        for (String extName : OpenGLSupportTest.getRequiredOglExtensions()) {
            if (!glAutoDrawable.getGL().isExtensionAvailable(extName)) {
                System.out.println("OpenGL extension " + extName + " is not available.");
                status = 2;
            }
        }

        GLCapabilitiesImmutable caps = glAutoDrawable.getChosenGLCapabilities();
        if (caps.getAlphaBits() != 8 || caps.getRedBits() != 8 || caps.getGreenBits() != 8 || caps.getBlueBits() != 8) {
            System.out.println("Device canvas color depth is inadequate.");
            status = 3;
        }

        if (caps.getDepthBits() < 16) {
            System.out.println("Device canvas depth buffer depth is inadequate.");
            status = 4;
        }

        if (!caps.getDoubleBuffered()) {
            System.out.println("Device canvas is not double buffered.");
            status = 5;
        }

        System.exit(status);
    }

    @Override
    public void dispose(GLAutoDrawable glAutoDrawable) {
    }

    @Override
    public void display(GLAutoDrawable glAutoDrawable) {
    }

    @Override
    public void reshape(GLAutoDrawable glAutoDrawable, int i, int i1, int i2, int i3) {
    }

    protected static String[] getRequiredOglFunctions() {
        return new String[] {"glActiveTexture", "glClientActiveTexture"};
    }

    protected static String[] getRequiredOglExtensions() {
        return new String[] {"GL_EXT_texture_compression_s3tc"};
    }
}
