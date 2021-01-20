/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.render;

import com.jogamp.opengl.*;
import com.jogamp.opengl.glu.*;
import com.jogamp.opengl.glu.gl2.GLUgl2;
import gov.nasa.worldwind.*;
import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.pick.*;
import gov.nasa.worldwind.terrain.SectorGeometryList;
import gov.nasa.worldwind.tracks.TrackPoint;
import gov.nasa.worldwind.util.Logging;

import java.awt.*;
import java.util.Iterator;

/**
 * @author tag
 * @version $Id: TrackRenderer.java 1181 2013-02-15 22:27:10Z dcollins $
 */
public class TrackRenderer implements Disposable {
    protected final Shape SPHERE = new Sphere();
    protected final Shape CONE = new Cone();
    protected final Shape CYLINDER = new Cylinder();
    protected final PickSupport pickSupport = new PickSupport();
    protected int lowerLimit;
    protected int upperLimit = Integer.MAX_VALUE;
    private double elevation = 10.0d;
    private boolean overrideMarkerElevation;
    private Object client;
    private double markerPixels = 8.0d; // TODO: these should all be configurable
    private double minMarkerSize = 3.0d;
    private Material material = Material.WHITE;
    private Shape shape = SPHERE;
    private boolean keepSeparated = true;

    public TrackRenderer() {
    }

    protected static void begin(DrawContext dc) {
        GL2 gl = dc.getGL2(); // GL initialization checks for GL2 compatibility.
        Vec4 cameraPosition = dc.getView().getEyePoint();

        if (dc.isPickingMode()) {
            PickSupport.beginPicking(dc);

            gl.glPushAttrib(GL2.GL_ENABLE_BIT | GL2.GL_CURRENT_BIT | GL2.GL_TRANSFORM_BIT);
            gl.glDisable(GL.GL_TEXTURE_2D);
            gl.glDisable(GL2.GL_COLOR_MATERIAL);
        } else {
            gl.glPushAttrib(GL2.GL_ENABLE_BIT | GL2.GL_CURRENT_BIT | GL2.GL_LIGHTING_BIT | GL2.GL_TRANSFORM_BIT);
            gl.glDisable(GL.GL_TEXTURE_2D);

            float[] lightPosition =
                {(float) (cameraPosition.x * 2), (float) (cameraPosition.y / 2), (float) (cameraPosition.z), 0.0f};
            float[] lightDiffuse = {1.0f, 1.0f, 1.0f, 1.0f};
            float[] lightAmbient = {1.0f, 1.0f, 1.0f, 1.0f};
            float[] lightSpecular = {1.0f, 1.0f, 1.0f, 1.0f};

            gl.glDisable(GL2.GL_COLOR_MATERIAL);

            gl.glLightfv(GL2.GL_LIGHT1, GL2.GL_POSITION, lightPosition, 0);
            gl.glLightfv(GL2.GL_LIGHT1, GL2.GL_DIFFUSE, lightDiffuse, 0);
            gl.glLightfv(GL2.GL_LIGHT1, GL2.GL_AMBIENT, lightAmbient, 0);
            gl.glLightfv(GL2.GL_LIGHT1, GL2.GL_SPECULAR, lightSpecular, 0);

            gl.glDisable(GL2.GL_LIGHT0);
            gl.glEnable(GL2.GL_LIGHT1);
            gl.glEnable(GL2.GL_LIGHTING);
            gl.glEnable(GL2.GL_NORMALIZE);
        }

        gl.glMatrixMode(GL2.GL_MODELVIEW);
        gl.glPushMatrix();
    }

    protected static void end(DrawContext dc) {
        GL2 gl = dc.getGL2(); // GL initialization checks for GL2 compatibility.

        gl.glMatrixMode(GL2.GL_MODELVIEW);
        gl.glPopMatrix();

        if (dc.isPickingMode()) {
            PickSupport.endPicking(dc);
        } else {
            gl.glDisable(GL2.GL_LIGHT1);
            gl.glEnable(GL2.GL_LIGHT0);
            gl.glDisable(GL2.GL_LIGHTING);
            gl.glDisable(GL2.GL_NORMALIZE);
        }

        gl.glPopAttrib();
    }

    public void dispose() {
        this.CONE.dispose();
        this.CYLINDER.dispose();
        this.SPHERE.dispose();
    }

    public double getMarkerPixels() {
        return markerPixels;
    }

    public void setMarkerPixels(double markerPixels) {
        this.markerPixels = markerPixels;
    }

    public double getMinMarkerSize() {
        return minMarkerSize;
    }

    public void setMinMarkerSize(double minMarkerSize) {
        this.minMarkerSize = minMarkerSize;
    }

    public Material getMaterial() {
        return material;
    }

    public void setMaterial(Material material) {
        if (material == null) {
            String msg = Logging.getMessage("nullValue.MaterialIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        // don't validate material's colors - material does that.

        this.material = material;
    }

    public void setShapeType(String shapeName) {
        if (shapeName.equalsIgnoreCase("Cone"))
            this.shape = CONE;
        else if (shapeName.equalsIgnoreCase("Cylinder"))
            this.shape = CYLINDER;
        else
            this.shape = SPHERE;
    }

    public boolean isKeepSeparated() {
        return keepSeparated;
    }

    public void setKeepSeparated(boolean keepSeparated) {
        this.keepSeparated = keepSeparated;
    }

    protected Vec4 draw(DrawContext dc, Iterator<TrackPoint> trackPositions) {
        if (dc.getVisibleSector() == null)
            return null;

        SectorGeometryList geos = dc.getSurfaceGeometry();
        if (geos == null)
            return null;

        if (!this.shape.isInitialized)
            this.shape.initialize(dc);

        GL2 gl = dc.getGL2(); // GL initialization checks for GL2 compatibility.
        Vec4 lastPointDrawn = null;

        TrackRenderer.begin(dc);
        {
            if (!dc.isPickingMode())
                this.material.apply(gl, GL2.GL_FRONT);

            Vec4 previousDrawnPoint = null;
            double radius;
            for (int index = 0; trackPositions.hasNext(); index++) {
                TrackPoint tp = trackPositions.next();

                if (index < this.lowerLimit)
                    continue;

                if (index > this.upperLimit)
                    break;

                Vec4 point = this.computeSurfacePoint(dc, tp);
                if (point == null)
                    continue;

                if (dc.isPickingMode()) {
                    Color color = dc.getUniquePickColor();
                    int colorCode = color.getRGB();
                    PickedObject po = new PickedObject(colorCode,
                        this.getClient() != null ? this.getClient() : tp.getPosition(), tp.getPosition(), false);
                    po.set(Keys.PICKED_OBJECT_ID, index);
                    this.pickSupport.addPickableObject(po);
                    gl.glColor3ub((byte) color.getRed(), (byte) color.getGreen(), (byte) color.getBlue());
                }

                radius = this.computeMarkerRadius(dc, point);

                if (previousDrawnPoint == null) {
                    // It's the first point drawn
                    previousDrawnPoint = point;
                    this.shape.render(dc, point, radius);
                    lastPointDrawn = point;
                    continue;
                }

                if (!this.keepSeparated) {
                    previousDrawnPoint = point;
                    this.shape.render(dc, point, radius);
                    lastPointDrawn = point;
                    continue;
                }

                double separation = point.distanceTo3(previousDrawnPoint);
                double minSeparation = 4.0d * radius;
                if (separation > minSeparation) {
                    previousDrawnPoint = point;
                    this.shape.render(dc, point, radius);
                    lastPointDrawn = point;
                }
            }
        }
        TrackRenderer.end(dc);

        return lastPointDrawn;
    }

    private double computeMarkerRadius(DrawContext dc, Vec4 point) {
        double d = point.distanceTo3(dc.getView().getEyePoint());
        double radius = this.markerPixels * dc.getView().computePixelSizeAtDistance(d);
        if (radius < this.minMarkerSize)
            radius = this.minMarkerSize;

        return radius;
    }

    public int getLowerLimit() {
        return this.lowerLimit;
    }

    public void setLowerLimit(int lowerLimit) {
        this.lowerLimit = lowerLimit;
    }

    public int getUpperLimit() {
        return this.upperLimit;
    }

    public void setUpperLimit(int upperLimit) {
        this.upperLimit = upperLimit;
    }

    public double getElevation() {
        return elevation;
    }

    public void setElevation(double elevation) {
        this.elevation = elevation;
    }

    public boolean isOverrideElevation() {
        return overrideMarkerElevation;
    }

    public void setOverrideElevation(boolean overrideMarkerElevation) {
        this.overrideMarkerElevation = overrideMarkerElevation;
    }

    public Object getClient() {
        return client;
    }

    public void setClient(Object client) {
        this.client = client;
    }

    protected Vec4 computeSurfacePoint(DrawContext dc, TrackPoint tp) {
        Position pos = tp.getPosition();

        if (!this.overrideMarkerElevation)
            return dc.getGlobe().computePointFromPosition(pos);

        // Compute points that are at the track-specified elevation
        Vec4 point = dc.getSurfaceGeometry().getSurfacePoint(pos.getLatitude(), pos.getLongitude(), this.elevation);
        if (point != null)
            return point;

        // Point is outside the current sector geometry, so compute it from the globe.
        return dc.getGlobe().computePointFromPosition(pos.getLatitude(), pos.getLongitude(), this.elevation);
    }

    public Vec4 pick(DrawContext dc, Iterator<TrackPoint> trackPositions, Point pickPoint, Layer layer) {
        this.pickSupport.clearPickList();
        Vec4 lastPointDrawn = this.draw(dc, trackPositions);
        this.pickSupport.resolvePick(dc, pickPoint, layer);
        this.pickSupport.clearPickList(); // to ensure entries can be garbage collected

        return lastPointDrawn;
    }

    public Vec4 render(DrawContext dc, Iterator<TrackPoint> trackPositions) {
        return this.draw(dc, trackPositions);
    }

    protected static abstract class Shape {
        protected String name;
        protected int glListId;
        protected GLUquadric quadric;
        protected boolean isInitialized;

        abstract protected void doRender(DrawContext dc, Vec4 point, double radius);

        protected void initialize(DrawContext dc) {
            GL2 gl = dc.getGL2(); // GL initialization checks for GL2 compatibility.
            GLU glu = dc.getGLU();

            this.glListId = gl.glGenLists(1);
            this.quadric = glu.gluNewQuadric();
            glu.gluQuadricDrawStyle(quadric, GLU.GLU_FILL);
            glu.gluQuadricNormals(quadric, GLU.GLU_SMOOTH);
            glu.gluQuadricOrientation(quadric, GLU.GLU_OUTSIDE);
            glu.gluQuadricTexture(quadric, false);
        }

        private void dispose() {
            if (this.isInitialized) {
                GLU glu = new GLUgl2();
                glu.gluDeleteQuadric(this.quadric);
                this.isInitialized = false;

                GLContext glc = GLContext.getCurrent();
                if (glc == null || glc.getGL() == null)
                    return;

                GL2 gl = glc.getGL().getGL2(); // GL initialization checks for GL2 compatibility.
                gl.glDeleteLists(this.glListId, 1);

                this.glListId = -1;
            }
        }

        protected void render(DrawContext dc, Vec4 point, double radius) {
            dc.getView().pushReferenceCenter(dc, point);
            this.doRender(dc, point, radius);
            dc.getView().popReferenceCenter(dc);
        }
    }

    private static class Sphere extends Shape {
        protected void initialize(DrawContext dc) {
            super.initialize(dc);

            this.name = "Sphere";
            double radius = 1;
            int slices = 36;
            int stacks = 18;

            GL2 gl = dc.getGL2(); // GL initialization checks for GL2 compatibility.
            GLU glu = dc.getGLU();

            gl.glNewList(this.glListId, GL2.GL_COMPILE);
            glu.gluSphere(this.quadric, radius, slices, stacks);
            gl.glEndList();

            this.isInitialized = true;
        }

        protected void doRender(DrawContext dc, Vec4 point, double radius) {
            GL2 gl = dc.getGL2(); // GL initialization checks for GL2 compatibility.
            gl.glScaled(radius, radius, radius);
            gl.glCallList(this.glListId);
        }
    }

    private static class Cone extends Shape {
        protected void initialize(DrawContext dc) {
            super.initialize(dc);

            this.name = "Cone";
            int slices = 30;
            int stacks = 30;
            int loops = 2;

            GL2 gl = dc.getGL2(); // GL initialization checks for GL2 compatibility.
            GLU glu = dc.getGLU();

            gl.glNewList(this.glListId, GL2.GL_COMPILE);
            glu.gluQuadricOrientation(quadric, GLU.GLU_OUTSIDE);
            glu.gluCylinder(quadric, 1.0d, 0.0d, 2.0d, slices, (int) (2 * (Math.sqrt(stacks)) + 1));
            glu.gluDisk(quadric, 0.0d, 1.0d, slices, loops);
            gl.glEndList();

            this.isInitialized = true;
        }

        protected void doRender(DrawContext dc, Vec4 point, double size) {
            PolarPoint p = PolarPoint.fromCartesian(point);

            GL2 gl = dc.getGL2(); // GL initialization checks for GL2 compatibility.

            gl.glScaled(size, size, size);
            gl.glRotated(p.getLongitude().degrees, 0, 1, 0);
            gl.glRotated(Math.abs(p.getLatitude().degrees), Math.signum(p.getLatitude().degrees) * -1, 0, 0);
            gl.glCallList(this.glListId);
        }
    }

    protected static class Cylinder extends Shape {
        protected void initialize(DrawContext dc) {
            super.initialize(dc);

            this.name = "Cylinder";
            int slices = 30;
            int stacks = 1;
            int loops = 1;

            GL2 gl = dc.getGL2(); // GL initialization checks for GL2 compatibility.
            GLU glu = dc.getGLU();

            gl.glNewList(this.glListId, GL2.GL_COMPILE);
            glu.gluCylinder(quadric, 1.0d, 1.0d, 2.0d, slices, (int) (2 * (Math.sqrt(stacks)) + 1));
            glu.gluDisk(quadric, 0.0d, 1.0d, slices, loops);
            gl.glTranslated(0, 0, 2);
            glu.gluDisk(quadric, 0.0d, 1.0d, slices, loops);
            gl.glTranslated(0, 0, -2);
            gl.glEndList();

            this.isInitialized = true;
        }

        protected void doRender(DrawContext dc, Vec4 point, double size) {
            PolarPoint p = PolarPoint.fromCartesian(point);

            GL2 gl = dc.getGL2(); // GL initialization checks for GL2 compatibility.

            gl.glScaled(size, size, size);
            gl.glRotated(p.getLongitude().degrees, 0, 1, 0);
            gl.glRotated(Math.abs(p.getLatitude().degrees), Math.signum(p.getLatitude().degrees) * -1, 0, 0);
            gl.glCallList(this.glListId);
        }
    }
}