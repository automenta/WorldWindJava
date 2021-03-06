/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */

package gov.nasa.worldwind.render.markers;

import com.jogamp.opengl.*;
import gov.nasa.worldwind.*;
import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.pick.*;
import gov.nasa.worldwind.render.*;

import java.awt.*;
import java.util.List;
import java.util.*;

/**
 * @author tag
 * @version $Id: MarkerRenderer.java 2325 2014-09-17 21:55:48Z tgaskins $
 */
public class MarkerRenderer {
    protected final PickSupport pickSupport = new PickSupport();
    private final ArrayList<Vec4> surfacePoints = new ArrayList<>();
    private double elevation = 10.0d;
    private boolean overrideMarkerElevation;
    private boolean keepSeparated = true;
    private boolean enablePickSizeReturn;
    // Rendering state.
    private long frameTimeStamp;
    private MarkerAttributes previousAttributes; // used only by drawSeparated and drawMarker

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

    protected static boolean intersectsFrustum(DrawContext dc, Vec4 point, double radius) {
        if (dc.isPickingMode())
            return dc.getPickFrustums().intersectsAny(new Sphere(point, radius));

        // TODO: determine if culling markers against center point is intentional.
        return dc.view().getFrustumInModelCoordinates().contains(point);
    }

    protected static double computeMarkerRadius(DrawContext dc, Vec4 point, Marker marker) {
        final View v = dc.view();
        double d = point.distanceTo3(v.getEyePoint());
        final MarkerAttributes a = marker.getAttributes();
        double radius = a.getMarkerPixels() * v.computePixelSizeAtDistance(d);
        if (radius < a.getMinMarkerSize() && a.getMinMarkerSize() > 0)
            radius = a.getMinMarkerSize();
        else if (radius > a.getMaxMarkerSize())
            radius = a.getMaxMarkerSize();

        return radius;
    }

    public double getElevation() {
        return elevation;
    }

    public void setElevation(double elevation) {
        this.elevation = elevation;
    }

    public boolean isOverrideMarkerElevation() {
        return overrideMarkerElevation;
    }

    public void setOverrideMarkerElevation(boolean overrideMarkerElevation) {
        this.overrideMarkerElevation = overrideMarkerElevation;
    }

    public boolean isKeepSeparated() {
        return keepSeparated;
    }

    public void setKeepSeparated(boolean keepSeparated) {
        this.keepSeparated = keepSeparated;
    }

    //**************************************************************//
    //********************  Rendering  *****************************//
    //**************************************************************//

    public boolean isEnablePickSizeReturn() {
        return enablePickSizeReturn;
    }

    public void setEnablePickSizeReturn(boolean enablePickSizeReturn) {
        this.enablePickSizeReturn = enablePickSizeReturn;
    }

    public void render(DrawContext dc, Iterable<Marker> markers) {
//        if (dc == null) {
//            String message = Logging.getMessage("nullValue.DrawContextIsNull");
//            Logging.logger().severe(message);
//            throw new IllegalStateException(message);
//        }
//
//        if (markers == null) {
//            String message = Logging.getMessage("nullValue.MarkerListIsNull");
//            Logging.logger().severe(message);
//            throw new IllegalStateException(message);
//        }

        if (this.isKeepSeparated())
            this.drawSeparated(dc, markers);
        else
            this.drawAll(dc, markers);
    }

    protected void draw(DrawContext dc, Iterable<Marker> markers) {
        if (this.isKeepSeparated())
            this.drawSeparated(dc, markers);
        else
            this.drawAll(dc, markers);
    }

    protected void drawSeparated(DrawContext dc, Iterable<Marker> markers) {
        List<Marker> markerList;
        if (markers instanceof List) {
            markerList = (List<Marker>) markers;
        } else {
            markerList = new ArrayList<>();
            for (Marker m : markers) {
                markerList.add(m);
            }
        }

        if (markerList.isEmpty())
            return;

        Layer parentLayer = dc.getCurrentLayer();
        Vec4 eyePoint = dc.view().getEyePoint();

        Marker m1 = markerList.get(0);
        Vec4 p1 = this.computeSurfacePoint(dc, m1.getPosition());
        double r1 = MarkerRenderer.computeMarkerRadius(dc, p1, m1);

        if (MarkerRenderer.intersectsFrustum(dc, p1, r1))
            dc.addOrderedRenderable(new OrderedMarker(0, m1, p1, r1, parentLayer, eyePoint.distanceTo3(p1)));

        if (markerList.size() < 2)
            return;

        int im2 = markerList.size() - 1;
        Marker m2 = markerList.get(im2);
        Vec4 p2 = this.computeSurfacePoint(dc, m2.getPosition());
        double r2 = MarkerRenderer.computeMarkerRadius(dc, p2, m2);

        if (MarkerRenderer.intersectsFrustum(dc, p2, r2))
            dc.addOrderedRenderable(new OrderedMarker(im2, m2, p2, r2, parentLayer, eyePoint.distanceTo3(p2)));

        if (markerList.size() < 3)
            return;

        this.drawInBetweenMarkers(dc, 0, p1, r1, im2, p2, r2, markerList, parentLayer, eyePoint);
    }

    private void drawInBetweenMarkers(DrawContext dc, int im1, Vec4 p1, double r1, int im2, Vec4 p2, double r2,
        List<Marker> markerList, Layer parentLayer, Vec4 eyePoint) {
        if (im2 == im1 + 1)
            return;

        if (p1.distanceTo3(p2) <= r1 + r2)
            return;

        int im = (im1 + im2) / 2;
        Marker m = markerList.get(im);
        Vec4 p = this.computeSurfacePoint(dc, m.getPosition());
        double r = MarkerRenderer.computeMarkerRadius(dc, p, m);

        boolean b1 = false, b2 = false;
        if (p.distanceTo3(p1) > r + r1) {
            this.drawInBetweenMarkers(dc, im1, p1, r1, im, p, r, markerList, parentLayer, eyePoint);
            b1 = true;
        }

        if (p.distanceTo3(p2) > r + r2) {
            this.drawInBetweenMarkers(dc, im, p, r, im2, p2, r2, markerList, parentLayer, eyePoint);
            b2 = true;
        }

        if (b1 && b2 && MarkerRenderer.intersectsFrustum(dc, p, r))
            dc.addOrderedRenderable(new OrderedMarker(im, m, p, r, parentLayer, eyePoint.distanceTo3(p)));
    }

    private void drawMarker(DrawContext dc, int index, Marker marker, Vec4 point, double radius) {
        // This method is called from OrderedMarker's render and pick methods. We don't perform culling here, because
        // the marker has already been culled against the appropriate frustum prior adding OrderedMarker to the draw
        // context.

        if (dc.isPickingMode()) {
            Color color = dc.getUniquePickColor();
            int colorCode = color.getRGB();
            PickedObject po = new PickedObject(colorCode, marker, marker.getPosition(), false);
            po.set(Keys.PICKED_OBJECT_ID, index);
            if (this.enablePickSizeReturn)
                po.set(Keys.PICKED_OBJECT_SIZE, 2 * radius);
            this.pickSupport.addPickableObject(po);
            GL2 gl = dc.getGL2(); // GL initialization checks for GL2 compatibility.
            gl.glColor3ub((byte) color.getRed(), (byte) color.getGreen(), (byte) color.getBlue());
        }

        MarkerAttributes attrs = marker.getAttributes();
        if (attrs != this.previousAttributes) // equality is intentional to avoid constant equals() calls
        {
            attrs.apply(dc);
            this.previousAttributes = attrs;
        }

        marker.render(dc, point, radius);
    }

    protected void computeSurfacePoints(DrawContext dc, Iterable<? extends Marker> markers) {
        surfacePoints.clear();
        for (Marker marker : markers) {
            // If the marker is null, add a null reference to the surfacePoints cache array so that it is
            // the same size as the marker iterator.
            if (marker == null) {
                surfacePoints.add(null);
                continue;
            }
            // Compute the surface point
            Position pos = marker.getPosition();
            Vec4 point = this.computeSurfacePoint(dc, pos);
            // Check to see that the point is within the frustum.  If it is not, place a null reference in the
            // surfacePoints array.  This will let the drawAll method know not to render it on the 2nd pass. We always
            // cull against the view frustum here, because these points are used during both picking and rendering.
            if (!dc.view().getFrustumInModelCoordinates().contains(point)) {
                surfacePoints.add(null);
                continue;
            }
            // Add the point to the cache array.
            surfacePoints.add(point);
        }
    }

    //**************************************************************//
    //********************  Rendering Utilities  *******************//
    //**************************************************************//

    protected void drawAll(DrawContext dc, Iterable<Marker> markers) {
        Layer parentLayer = dc.getCurrentLayer();
        Vec4 eyePoint = dc.view().getEyePoint();

        // If this is a new frame, recompute surface points.
        if (dc.getFrameTimeStamp() != this.frameTimeStamp || dc.isContinuous2DGlobe()) {
            this.frameTimeStamp = dc.getFrameTimeStamp();
            this.computeSurfacePoints(dc, markers);
        }

        Iterator<Marker> markerIterator = markers.iterator();
        for (int index = 0; markerIterator.hasNext(); index++) {
            Marker marker = markerIterator.next();
            Vec4 point = this.surfacePoints.get(index); // TODO: check performance of this buffer access
            // The surface point is null if the marker in this position is null or if the surface point is not in the
            // view frustum.
            if (point == null)
                continue;

            double radius = MarkerRenderer.computeMarkerRadius(dc, point, marker);

            // If we're in picking mode, cull the marker against the draw context's pick frustums. At this point we've
            // only culled against the viewing frustum.
            if (dc.isPickingMode() && !MarkerRenderer.intersectsFrustum(dc, point, radius))
                continue;

            dc.addOrderedRenderable(new OrderedMarker(index, marker, point, radius, parentLayer,
                eyePoint.distanceTo3(point)));
        }
    }

    protected void begin(DrawContext dc) {
        GL2 gl = dc.getGL2(); // GL initialization checks for GL2 compatibility.
        Vec4 cameraPosition = dc.view().getEyePoint();

        if (dc.isPickingMode()) {
            PickSupport.beginPicking(dc);

            gl.glPushAttrib(GL2.GL_ENABLE_BIT | GL2.GL_CURRENT_BIT | GL2.GL_TRANSFORM_BIT);
            gl.glDisable(GL2.GL_COLOR_MATERIAL);
        } else {
            gl.glPushAttrib(GL2.GL_ENABLE_BIT | GL2.GL_CURRENT_BIT | GL2.GL_LIGHTING_BIT | GL2.GL_TRANSFORM_BIT
                | GL2.GL_COLOR_BUFFER_BIT);

            float[] lightPosition = new float[4];

            if (dc.is2DGlobe()) {
                lightPosition[0] = 0.2f;
                lightPosition[1] = -0.5f;
                lightPosition[2] = 1.0f;
                lightPosition[3] = 0.0f;
            } else {
                lightPosition[0] = (float) cameraPosition.x * 2;
                lightPosition[1] = (float) cameraPosition.y / 2;
                lightPosition[2] = (float) cameraPosition.z;
                lightPosition[3] = 0.0f;
            }

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

            // Set up for opacity, either explictly via attributes or implicitly as alpha in the marker color
            dc.getGL().glEnable(GL.GL_BLEND);
            dc.getGL().glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
        }

        gl.glMatrixMode(GL2.GL_MODELVIEW);
        gl.glPushMatrix();

        // We're beginning a new sequence of marker rendering. Clear the previous attributes to ensure that no rendering
        // code assumes we've already set attribute rendering state.
        this.previousAttributes = null;
    }

    protected Vec4 computeSurfacePoint(DrawContext dc, Position pos) {
        double ve = dc.getVerticalExaggeration();
        if (!this.overrideMarkerElevation)
            return dc.getGlobe().computePointFromPosition(pos, dc.is2DGlobe() ? 0 : pos.getElevation() * ve);

        // Compute points that are at the renderer-specified elevation
        double effectiveElevation = dc.is2DGlobe() ? 0 : this.elevation;
        Vec4 point = dc.getSurfaceGeometry().getSurfacePoint(pos.getLat(), pos.getLon(),
            effectiveElevation * ve);
        if (point != null)
            return point;

        // Point is outside the current sector geometry, so compute it from the globe.
        return dc.getGlobe().computePointFromPosition(pos.getLat(), pos.getLon(), effectiveElevation * ve);
    }

    //**************************************************************//
    //********************  Ordered Renderable  ********************//
    //**************************************************************//

    protected void drawOrderedMarkers(DrawContext dc, OrderedMarker uMarker) {
        this.drawMarker(dc, uMarker.index, uMarker.marker, uMarker.point, uMarker.radius);

        // Draw as many as we can in a batch to save ogl state switching.
        Object next = dc.peekOrderedRenderables();
        while (next instanceof OrderedMarker && ((OrderedMarker) next).getRenderer() == this) {
            dc.pollOrderedRenderables(); // take it off the queue

            OrderedMarker om = (OrderedMarker) next;
            this.drawMarker(dc, om.index, om.marker, om.point, om.radius);

            next = dc.peekOrderedRenderables();
        }
    }

    protected void pickOrderedMarkers(DrawContext dc, OrderedMarker uMarker) {
        this.drawMarker(dc, uMarker.index, uMarker.marker, uMarker.point, uMarker.radius);

        // Draw as many as we can in a batch to save ogl state switching.
        Object next = dc.peekOrderedRenderables();
        while (next instanceof OrderedMarker && ((OrderedMarker) next).getRenderer() == this
            && ((OrderedMarker) next).layer == uMarker.layer) {
            dc.pollOrderedRenderables(); // take it off the queue

            OrderedMarker om = (OrderedMarker) next;
            this.drawMarker(dc, om.index, om.marker, om.point, om.radius);

            next = dc.peekOrderedRenderables();
        }
    }

    protected class OrderedMarker implements OrderedRenderable {
        protected final int index;
        protected final Marker marker;
        protected final Vec4 point;
        protected final double radius;
        protected final Layer layer;
        protected final double eyeDistance;

        public OrderedMarker(int index, Marker marker, Vec4 point, double radius, Layer layer, double eyeDistance) {
            this.index = index;
            this.marker = marker;
            this.point = point;
            this.radius = radius;
            this.layer = layer;
            this.eyeDistance = eyeDistance;
        }

        public MarkerRenderer getRenderer() {
            return MarkerRenderer.this;
        }

        public double getDistanceFromEye() {
            return this.eyeDistance;
        }

        public void pick(DrawContext dc, Point pickPoint) {
            MarkerRenderer.this.begin(dc); // Calls pickSupport.beginPicking when in picking mode.

            MarkerRenderer.this.pickOrderedMarkers(dc, this);

            MarkerRenderer.end(dc); // Calls pickSupport.endPicking when in picking mode.
            MarkerRenderer.this.pickSupport.resolvePick(dc, pickPoint, this.layer); // Also clears the pick list.

        }

        public void render(DrawContext dc) {
            MarkerRenderer.this.begin(dc);
            MarkerRenderer.this.drawOrderedMarkers(dc, this);
            MarkerRenderer.end(dc);
        }
    }
}