/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.util;

import com.jogamp.opengl.*;
import gov.nasa.worldwind.Keys;

/**
 * This program returns the version and implementation information for the Java Bindings for OpenGL (R) implementation
 * found in the CLASSPATH. This information is also found in the manifest for jogl-all.jar, and this program uses the
 * java.lang.Package class to retrieve it programmatically.
 *
 * @version $Id: JOGLVersionInfo.java 1171 2013-02-11 21:45:02Z dcollins $
 */
public class JOGLVersionInfo {

    private static final JOGLVersionInfo svi = new JOGLVersionInfo();
    /**
     * Returns the highest OpenGL profile available on the current graphics device that is compatible with WorldWind.
     * The returned profile favors hardware acceleration over software acceleration. With JOGL version 2.0, this returns
     * the highest available profile from the following list:
     * <ul> <li>OpenGL compatibility profile 4.x</li> <li>OpenGL compatibility profile 3.x</li> <li>OpenGL profile 1.x
     * up to 3.0</li> </ul>
     *
     * @return the highest compatible OpenGL profile.
     */
    public final static GLProfile maxCompatible = GLProfile.getMaxFixedFunc(true); // Favor a hardware rasterizer
    private final Package p;

    private JOGLVersionInfo() {
        ClassLoader classLoader = getClass().getClassLoader();
        this.p = JOGLVersionInfo.pkgInfo(classLoader, "com.jogamp.opengl", "GL");
    }

    private static Package pkgInfo(ClassLoader classLoader, String pkgName, String className) {
        Package p = null;

        try {
            classLoader.loadClass(pkgName + '.' + className);

            // TODO: message logging
            p = classLoader.getDefinedPackage(pkgName);
            if (p == null) {
                System.out.println("WARNING: Package.getPackage(" + pkgName + ") is null");
            }
        }
        catch (ClassNotFoundException e) {
            System.out.println("Unable to load " + pkgName);
        }

        return p;
    }

    public static Package getPackage() {
        return JOGLVersionInfo.svi.p;
    }

    public static boolean isCompatibleWith(String version) {
        return JOGLVersionInfo.svi.p != null && JOGLVersionInfo.svi.p.isCompatibleWith(version);
    }

    public static String getSpecificationTitle() {
        return JOGLVersionInfo.svi.p != null ? JOGLVersionInfo.svi.p.getSpecificationTitle() : null;
    }

    public static String getSpecificationVendor() {
        return JOGLVersionInfo.svi.p != null ? JOGLVersionInfo.svi.p.getSpecificationVendor() : null;
    }

    public static String getSpecificationVersion() {
        return JOGLVersionInfo.svi.p != null ? JOGLVersionInfo.svi.p.getSpecificationVersion() : null;
    }

    public static String getImplementationTitle() {
        return JOGLVersionInfo.svi.p != null ? JOGLVersionInfo.svi.p.getImplementationTitle() : null;
    }

    public static String getImplementationVersion() {
        return JOGLVersionInfo.svi.p != null ? JOGLVersionInfo.svi.p.getImplementationVersion() : null;
    }

    public static void main(String[] args) {
        System.out.println(JOGLVersionInfo.getPackage());
        System.out.println(JOGLVersionInfo.getSpecificationTitle());
        System.out.println(JOGLVersionInfo.getSpecificationVendor());
        System.out.println(JOGLVersionInfo.getSpecificationVersion());
        System.out.println(JOGLVersionInfo.getImplementationTitle());
        System.out.println(JOGLVersionInfo.getImplementationVersion());
        System.out.println(JOGLVersionInfo.isCompatibleWith("1.0"));
        System.out.println(JOGLVersionInfo.isCompatibleWith("1.1.1"));
        System.out.println(JOGLVersionInfo.isCompatibleWith("1.2.1"));
        System.out.println(
            JOGLVersionInfo.getImplementationVersion().compareToIgnoreCase("1.1.1-pre-20070511-02:12:11"));
        System.out.println(
            JOGLVersionInfo.getImplementationVersion().compareToIgnoreCase("1.1.1-pre-20070512-02:12:11"));
        System.out.println(JOGLVersionInfo.getImplementationVersion().compareToIgnoreCase("1.1.1"));
    }

    public static GLProfile getMaxCompatibleGLProfile() {
        return maxCompatible;
    }

    /**
     * Returns a {@link GLCapabilities} identifying graphics features required by WorldWind. The capabilities instance
     * returned requests the maximum OpenGL profile supporting GL fixed function operations, a frame buffer with 8 bits
     * each of red, green, blue and alpha, a 24-bit depth buffer, double buffering, and if the Java property
     * "gov.nasa.worldwind.stereo.mode" is set to "device", device supported stereo.
     *
     * @return a new capabilities instance identifying required graphics features.
     */
    public static GLCapabilities getRequiredGLCapabilities() {
        GLCapabilities caps = new GLCapabilities(getMaxCompatibleGLProfile());

        caps.setAlphaBits(8);
        caps.setRedBits(8);
        caps.setGreenBits(8);
        caps.setBlueBits(8);
        caps.setDepthBits(24);
        caps.setDoubleBuffered(true);

        // Determine whether we should request a stereo canvas
        String stereo = System.getProperty(Keys.STEREO_MODE);
        if ("device".equals(stereo))
            caps.setStereo(true);

        return caps;
    }
}