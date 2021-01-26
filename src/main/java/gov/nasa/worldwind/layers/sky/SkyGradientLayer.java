/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.layers.sky;

import com.jogamp.opengl.*;
import gov.nasa.worldwind.View;
import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.layers.AbstractLayer;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.util.*;

import java.awt.*;

/**
 * Renders an atmosphere around the globe and a sky dome at low altitude.
 * <p>
 * Note : based on a spherical globe.<br> Issue : Ellipsoidal globe doesnt match the spherical atmosphere everywhere.
 *
 * @author Patrick Murris
 * @version $Id: SkyGradientLayer.java 2146 2014-07-11 17:37:04Z tgaskins $
 */
public class SkyGradientLayer extends AbstractLayer {
    protected final static int STACKS = 12;
    protected final static int SLICES = 64;
    protected final float[] horizonColor = {0.76f, 0.76f, 0.80f, 1.0f}; // horizon color
    protected final float[] zenithColor = {0.26f, 0.47f, 0.83f, 1.0f}; // zenith color
    // TODO: make configurable
    protected double thickness = 100.0e3; // Atmosphere thickness

    /**
     * Renders an atmosphere around the globe
     */
    public SkyGradientLayer() {
        this.setPickEnabled(false);
    }

    /**
     * Converts position in spherical coordinates (lat/lon/altitude) to cartesian (XYZ) coordinates.
     *
     * @param latitude  Latitude in decimal degrees
     * @param longitude Longitude in decimal degrees
     * @param radius    Radius
     * @return the corresponding Point
     */
    protected static Vec4 SphericalToCartesian(double latitude, double longitude, double radius) {
        latitude *= Math.PI / 180.0f;
        longitude *= Math.PI / 180.0f;

        double radCosLat = radius * Math.cos(latitude);

        return new Vec4(
            radCosLat * Math.sin(longitude),
            radius * Math.sin(latitude),
            radCosLat * Math.cos(longitude));
    }

    /**
     * Converts position in cartesian coordinates (XYZ) to spherical (radius, lat, lon) coordinates.
     *
     * @param x X coordinate
     * @param y Y coordinate
     * @param z Z coordinate
     * @return a <code>Vec4</code> point for the spherical coordinates {radius, lat, lon}
     */
    protected static Vec4 CartesianToSpherical(double x, double y, double z) {
        double rho = Math.sqrt(x * x + y * y + z * z);
        double longitude = Math.atan2(x, z);
        double latitude = Math.asin(y / rho);

        return new Vec4(rho, latitude, longitude);
    }

    protected static void applyDrawTransform(DrawContext dc, OGLStackHandler ogsh) {
        GL2 gl = dc.getGL2(); // GL initialization checks for GL2 compatibility.
        View view = dc.view();
        ogsh.pushModelview(gl);
        // Place sky - TODO: find another ellipsoid friendlier way (the sky dome is not exactly normal...
        // to the ground at higher latitude)
        Vec4 camPoint = view.getEyePoint();
        Vec4 camPosFromPoint = SkyGradientLayer.CartesianToSpherical(camPoint.x, camPoint.y, camPoint.z);
        gl.glRotatef((float) (Angle.fromRadians(camPosFromPoint.z).degrees), 0.0f, 1.0f, 0.0f);
        gl.glRotatef((float) (-Angle.fromRadians(camPosFromPoint.y).degrees + 90), 1.0f, 0.0f, 0.0f);
        gl.glTranslatef(0.0f, (float) (view.getEyePoint().getLength3()), 0.0f);
    }

    protected static void applyDrawProjection(DrawContext dc, OGLStackHandler ogsh) {
        GL2 gl = dc.getGL2(); // GL initialization checks for GL2 compatibility.
        View view = dc.view();
        double viewportWidth = view.getViewport().getWidth();
        double viewportHeight = view.getViewport().getHeight();

        // If either the viewport width or height is zero, then treat the dimension as if it had value 1.
        if (viewportWidth <= 0)
            viewportWidth = 1;
        if (viewportHeight <= 0)
            viewportHeight = 1;

        Matrix projection = Matrix.fromPerspective(view.getFieldOfView(), viewportWidth, viewportHeight,
            100, view.getHorizonDistance() + 10.0e3);
        double[] matrixArray = new double[16];
        projection.toArray(matrixArray, 0, false);
        ogsh.pushProjection(gl);
        gl.glLoadMatrixd(matrixArray, 0);
    }

    /**
     * Get the atmosphere thickness in meter
     *
     * @return the atmosphere thickness in meter
     */
    public double getAtmosphereThickness() {
        return this.thickness;
    }

    /**
     * Set the atmosphere thickness in meter
     *
     * @param thickness the atmosphere thickness in meter
     */
    public void setAtmosphereThickness(double thickness) {
        if (thickness < 0) {
            String msg = Logging.getMessage("generic.ArgumentOutOfRange");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }
        this.thickness = thickness;
    }

    /**
     * Get the horizon color
     *
     * @return the horizon color
     */
    public Color getHorizonColor() {
        return new Color(this.horizonColor[0], this.horizonColor[1], this.horizonColor[2], this.horizonColor[3]);
    }

    /**
     * Set the horizon color
     *
     * @param color the horizon color
     */
    public void setHorizonColor(Color color) {
        if (color == null) {
            String msg = Logging.getMessage("nullValue.ColorIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }
        color.getColorComponents(this.horizonColor);
    }

    /**
     * Get the zenith color
     *
     * @return the zenith color
     */
    public Color getZenithColor() {
        return new Color(this.zenithColor[0], this.zenithColor[1], this.zenithColor[2], this.zenithColor[3]);
    }

    /**
     * Set the zenith color
     *
     * @param color the zenith color
     */
    public void setZenithColor(Color color) {
        if (color == null) {
            String msg = Logging.getMessage("nullValue.ColorIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }
        color.getColorComponents(this.zenithColor);
    }

    @Override
    public void doRender(DrawContext dc) {
        if (dc.is2DGlobe())
            return; // Layer doesn't make sense in 2D

        GL2 gl = dc.getGL2(); // GL initialization checks for GL2 compatibility.
        OGLStackHandler ogsh = new OGLStackHandler();

        try {
            ogsh.pushAttrib(gl, GL2.GL_TRANSFORM_BIT);

            gl.glDisable(GL.GL_DEPTH_TEST);
            gl.glDepthMask(false);
            gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
            gl.glEnable(GL.GL_BLEND);

            SkyGradientLayer.applyDrawProjection(dc, ogsh);
            SkyGradientLayer.applyDrawTransform(dc, ogsh);

            // Draw sky
            this.updateSkyDome(dc);
        }
        finally {
            dc.restoreDefaultDepthTesting();
            dc.restoreDefaultBlending();
            dc.restoreDefaultCurrentColor();
            ogsh.pop(gl);
        }
    }

    /**
     * Draws the positive three axes - x is red, y is green and z is blue
     *
     * @param dc     the current DrawContext
     * @param length the lenght of the axes lines
     */
    protected void updateSkyDome(DrawContext dc) {
        View view = dc.view();

        double tangentialDistance = view.getHorizonDistance();
        double distToCenterOfPlanet = view.getEyePoint().getLength3();
        Position camPos = dc.getGlobe().computePositionFromPoint(view.getEyePoint());
        double worldRadius = dc.getGlobe().getRadiusAt(camPos);
        double camAlt = camPos.getElevation();

        // horizon latitude degrees
        double horizonLat = (-Math.PI / 2 + Math.acos(tangentialDistance / distToCenterOfPlanet))
            * 180 / Math.PI;
        // zenith latitude degrees
        double zenithLat = 90;
        float zenithOpacity = 1.0f;
        float gradientBias = 2.0f;
        if (camAlt >= thickness) {
            // Eye is above atmosphere
            double tangentalDistanceZenith = Math.sqrt(distToCenterOfPlanet * distToCenterOfPlanet
                - (worldRadius + thickness) * (worldRadius + thickness));
            zenithLat = (-Math.PI / 2 + Math.acos(tangentalDistanceZenith / distToCenterOfPlanet)) * 180 / Math.PI;
            zenithOpacity = 0.0f;
            gradientBias = 1.0f;
        }
        if (camAlt < thickness && camAlt > thickness * 0.7) {
            // Eye is entering atmosphere - outer 30%
            double factor = (thickness - camAlt) / (thickness - thickness * 0.7);
            zenithLat = factor * 90;
            zenithOpacity = (float) factor;
            gradientBias = 1.0f + (float) factor;
        }

        this.drawSkyDome(dc, (float) tangentialDistance, horizonLat, zenithLat, SkyGradientLayer.SLICES, SkyGradientLayer.STACKS, zenithOpacity,
            gradientBias);
    }

    /**
     * Draws the sky dome
     *
     * @param dc            the current DrawContext
     * @param radius        the sky dome radius
     * @param startLat      the horizon latitude
     * @param endLat        the zenith latitude
     * @param slices        the number of slices - vertical divisions
     * @param stacks        the nuber os stacks - horizontal divisions
     * @param zenithOpacity the sky opacity at zenith
     * @param gradientBias  determines how fast the sky goes from the horizon color to the zenith color. A value of
     *                      <code>1</code> with produce a balanced gradient, a value greater then <code>1</code> will
     *                      have the zenith color dominate and a value less then <code>1</code> will have the opposite
     *                      effect.
     */
    protected void drawSkyDome(DrawContext dc, float radius, double startLat, double endLat,
        int slices, int stacks, float zenithOpacity, float gradientBias) {
        double latitude, longitude, latitudeTop = endLat;

        // GL setup
        GL2 gl = dc.getGL2(); // GL initialization checks for GL2 compatibility.

        // TODO: Simplify code
        double linear, linearTop, k, kTop, colorFactorZ, colorFactorZTop = 0;
        double colorFactorH, colorFactorHTop = 0;
        double alphaFactor, alphaFactorTop = 0;

        // bottom fade
        latitude = startLat - Math.max((endLat - startLat) / 4, 3);
        gl.glBegin(GL2.GL_QUAD_STRIP);
        for (int slice = 0; slice <= slices; slice++) {
            longitude = 180 - ((float) slice / slices * 360);
            Vec4 v = SkyGradientLayer.SphericalToCartesian(latitude, longitude, radius);
            gl.glColor4d(zenithColor[0], zenithColor[1], zenithColor[2], 0);
            gl.glVertex3d(v.x, v.y, v.z);
            v = SkyGradientLayer.SphericalToCartesian(startLat, longitude, radius);
            gl.glColor4d(horizonColor[0], horizonColor[1], horizonColor[2], horizonColor[3]);
            gl.glVertex3d(v.x, v.y, v.z);
        }
        gl.glEnd();

        // stacks and slices
        for (int stack = 1; stack < stacks - 1; stack++) {
            // bottom vertex
            linear = (stack - 1) / (stacks - 1.0f);
            k = 1 - Math.cos(linear * Math.PI / 2);
            latitude = startLat + k * (endLat - startLat);
            colorFactorZ = Math.min(1.0f, linear * gradientBias);     // coef zenith color
            colorFactorH = 1 - colorFactorZ;                        // coef horizon color
            alphaFactor = 1 - Math.pow(linear, 4) * (1 - zenithOpacity);                  // coef alpha transparency
            // top vertex
            linearTop = (stack) / (stacks - 1.0f);
            kTop = 1 - Math.cos(linearTop * Math.PI / 2);
            latitudeTop = startLat + kTop * (endLat - startLat);
            colorFactorZTop = Math.min(1.0f, linearTop * gradientBias);   // coef zenith color
            colorFactorHTop = 1 - colorFactorZTop;                      // coef horizon color
            alphaFactorTop = 1 - Math.pow(linearTop, 4) * (1 - zenithOpacity);            // coef alpha transparency
            // Draw stack
            gl.glBegin(GL2.GL_QUAD_STRIP);
            for (int slice = 0; slice <= slices; slice++) {
                longitude = 180 - ((float) slice / slices * 360);
                Vec4 v = SkyGradientLayer.SphericalToCartesian(latitude, longitude, radius);
                gl.glColor4d(
                    (horizonColor[0] * colorFactorH + zenithColor[0] * colorFactorZ),
                    (horizonColor[1] * colorFactorH + zenithColor[1] * colorFactorZ),
                    (horizonColor[2] * colorFactorH + zenithColor[2] * colorFactorZ),
                    (horizonColor[3] * colorFactorH + zenithColor[3] * colorFactorZ) * alphaFactor);
                gl.glVertex3d(v.x, v.y, v.z);
                v = SkyGradientLayer.SphericalToCartesian(latitudeTop, longitude, radius);
                gl.glColor4d(
                    (horizonColor[0] * colorFactorHTop + zenithColor[0] * colorFactorZTop),
                    (horizonColor[1] * colorFactorHTop + zenithColor[1] * colorFactorZTop),
                    (horizonColor[2] * colorFactorHTop + zenithColor[2] * colorFactorZTop),
                    (horizonColor[3] * colorFactorHTop + zenithColor[3] * colorFactorZTop) * alphaFactorTop);
                gl.glVertex3d(v.x, v.y, v.z);
            }
            gl.glEnd();
        }

        // Top fade
        gl.glBegin(GL2.GL_QUAD_STRIP);
        for (int slice = 0; slice <= slices; slice++) {
            longitude = 180 - ((float) slice / slices * 360);
            Vec4 v = SkyGradientLayer.SphericalToCartesian(latitudeTop, longitude, radius);
            gl.glColor4d(
                (horizonColor[0] * colorFactorHTop + zenithColor[0] * colorFactorZTop),
                (horizonColor[1] * colorFactorHTop + zenithColor[1] * colorFactorZTop),
                (horizonColor[2] * colorFactorHTop + zenithColor[2] * colorFactorZTop),
                (horizonColor[3] * colorFactorHTop + zenithColor[3] * colorFactorZTop) * alphaFactorTop);
            gl.glVertex3d(v.x, v.y, v.z);
            v = SkyGradientLayer.SphericalToCartesian(endLat, longitude, radius);
            gl.glColor4d(zenithColor[0], zenithColor[1], zenithColor[2], zenithOpacity < 1 ? 0 : zenithColor[3]);
            gl.glVertex3d(v.x, v.y, v.z);
        }
        gl.glColor4d(1.0d, 1.0d, 1.0d, 1.0d); // restore the default OpenGL color
        gl.glEnd();
    }

    @Override
    public String toString() {
        return Logging.getMessage("layers.Earth.SkyGradientLayer.Name");
    }
}