/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.view;

import com.jogamp.opengl.GL2;
import gov.nasa.worldwind.*;
import gov.nasa.worldwind.animation.Animator;
import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.globes.Globe;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.util.*;
import gov.nasa.worldwind.video.awt.ViewInputHandler;

import java.awt.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A base class from which {@link View} implementations can be derived. Currently {@link
 * gov.nasa.worldwind.view.firstperson.BasicFlyView} and {@link gov.nasa.worldwind.view.orbit.BasicOrbitView} are both
 * derived from {@link BasicView} {@link BasicView} models the view in terms of a geographic position, and a pitch,
 * heading, and roll. It provides a mapping from that geocentric view model to a 3D graphics modelview matrix. BasicView
 * also manages the projection matrix via a {@link Frustum}.
 * <p>
 * The view model is based on
 *
 * @author jym
 * @version $Id: BasicView.java 2204 2014-08-07 23:35:03Z dcollins $
 */
public class BasicView extends WWObjectImpl implements View {
    // TODO: make configurable
    protected static final double MINIMUM_NEAR_DISTANCE = 1;
    protected static final double MINIMUM_FAR_DISTANCE = 1000;
    /**
     * The views's default worst-case depth resolution, in meters. May be specified in the WorldWind configuration file
     * as the <code>gov.nasa.worldwind.avkey.DepthResolution</code> property. The default if not specified in the
     * configuration is 3.0 meters.
     */
    protected static final double DEFAULT_DEPTH_RESOLUTION = Configuration.getDoubleValue(Keys.DEPTH_RESOLUTION, 3.0);
    protected static final double COLLISION_THRESHOLD = 10;
    protected static final int COLLISION_NUM_ITERATIONS = 4;
    /**
     * Identifier for the modelview matrix state. This number is incremented when one of the fields that affects the
     * modelview matrix is set.
     */
    protected final AtomicLong viewStateID = new AtomicLong();
    /**
     * The field of view in degrees.
     */
    protected Angle fieldOfView = new Angle(45);
    // Provide reasonable default values for the near and far clip distances. By default, BasicView automatically
    // updates these values each frame based on the current eye position relative to the surface. These default values
    // are provided for two reasons:
    // * The view can provide a reasonable value to the application until the first frame.
    // * Subclass implementations which may override the automatic update of clipping plane distances have reasonable
    //   default values to fall back on.
    protected double nearClipDistance = BasicView.MINIMUM_NEAR_DISTANCE;
    protected double farClipDistance = BasicView.MINIMUM_FAR_DISTANCE;
    protected Matrix modelview = Matrix.IDENTITY;
    protected Matrix modelviewInv = Matrix.IDENTITY;
    protected Matrix projection = Matrix.IDENTITY;
    protected Rectangle viewport = new Rectangle();
    protected Frustum frustum = new Frustum();
    protected Frustum lastFrustumInModelCoords;
    protected ViewPropertyLimits viewLimits;
    protected DrawContext dc;
    protected boolean detectCollisions = true;
    protected boolean hadCollisions;
    protected ViewInputHandler viewInputHandler;
    protected Globe globe;
    protected Position eyePosition = Position.ZERO;
    protected double horizonDistance;
    protected Angle roll = new Angle(0.0);
    protected Angle pitch = new Angle(0.0);
    protected Angle heading = new Angle(0.0);
    protected Position lastEyePosition;
    protected Vec4 lastEyePoint;
    protected Vec4 lastUpVector;
    protected Vec4 lastForwardVector;

    /**
     * Construct a BasicView
     */
    public BasicView() {
    }

    /**
     * Sets the the opengl modelview and projection matrices to the given matrices.
     *
     * @param dc         the drawing context
     * @param modelview  the modelview matrix
     * @param projection the projection matrix
     */
    public static void loadGLViewState(DrawContext dc, Matrix modelview, Matrix projection) {

        double[] matrixArray = new double[16];

        GL2 gl = dc.getGL2(); // GL initialization checks for GL2 compatibility.
        // Store the current matrix-mode state.
        OGLStackHandler ogsh = new OGLStackHandler();

        try {
            ogsh.pushAttrib(gl, GL2.GL_TRANSFORM_BIT);

            // Apply the model-view matrix to the current OpenGL context.
            gl.glMatrixMode(GL2.GL_MODELVIEW);
            if (modelview != null) {
                modelview.toArray(matrixArray, 0, false);
                gl.glLoadMatrixd(matrixArray, 0);
            } else {
                gl.glLoadIdentity();
            }

            // Apply the projection matrix to the current OpenGL context.
            gl.glMatrixMode(GL2.GL_PROJECTION);
            if (projection != null) {
                projection.toArray(matrixArray, 0, false);
                gl.glLoadMatrixd(matrixArray, 0);
            } else {
                gl.glLoadIdentity();
            }
        }
        finally {
            ogsh.pop(gl);
        }
    }

    public Globe getGlobe() {
        return this.globe;
    }

    /**
     * Set the globe associated with this view. Note that the globe is reset each frame.
     *
     * @param globe New globe.
     */
    public void setGlobe(Globe globe) {
        this.globe = globe;
    }

    public DrawContext getDC() {
        return (this.dc);
    }

    public ViewInputHandler input() {
        return viewInputHandler;
    }

    public void setViewInputHandler(ViewInputHandler viewInputHandler) {
        this.viewInputHandler = viewInputHandler;
    }

    public boolean isDetectCollisions() {
        return this.detectCollisions;
    }

    public void setDetectCollisions(boolean detectCollisions) {
        this.detectCollisions = detectCollisions;
    }

    public boolean hadCollisions() {
        boolean result = this.hadCollisions;
        this.hadCollisions = false;
        return result;
    }

    public void copyViewState(View view) {
        this.globe = view.getGlobe();
        Vec4 center = view.getCenterPoint();
        if (center == null) {
            Vec4 eyePoint = view.getCurrentEyePoint();
            center = eyePoint.add3(view.getForwardVector());
        }
        setOrientation(view.getCurrentEyePosition(), globe.computePositionFromPoint(center));
    }

    public void apply(DrawContext dc) {

        if (this.viewInputHandler != null)
            this.viewInputHandler.apply();

        doApply(dc);

        if (this.viewInputHandler != null)
            this.viewInputHandler.viewApplied();
    }

    protected void doApply(DrawContext dc) {
    }

    public void stopMovement() {
        this.emit(View.VIEW_STOPPED, null, this);
    }

    public Rectangle getViewport() {
        // java.awt.Rectangle is mutable, so we defensively copy the viewport.
        return new Rectangle(this.viewport);
    }

    public Frustum getFrustum() {
        return this.frustum;
    }

    public Frustum getFrustumInModelCoordinates() {
        if (this.lastFrustumInModelCoords == null) {
            this.lastFrustumInModelCoords = this.frustum.transformBy(this.modelview.getTranspose());
        }
        return this.lastFrustumInModelCoords;
    }

    public double getNearClipDistance() {
        return this.nearClipDistance;
    }

    protected void setNearClipDistance(double clipDistance) {
        this.nearClipDistance = clipDistance;
    }

    public double getFarClipDistance() {
        return this.farClipDistance;
    }

    protected void setFarClipDistance(double clipDistance) {
        this.farClipDistance = clipDistance;
    }

    public Matrix getModelviewMatrix() {
        return this.modelview;
    }

    /**
     * {@inheritDoc}
     */
    public long getViewStateID() {
        return this.viewStateID.get();
    }

    public Angle getFieldOfView() {
        return this.fieldOfView;
    }

    public void setFieldOfView(Angle fieldOfView) {

        this.fieldOfView = fieldOfView;
    }

    public Vec4 project(Vec4 modelPoint) {

        return this.project(modelPoint, this.modelview, this.projection, this.viewport);
    }

    public Vec4 unProject(Vec4 windowPoint) {

        return unProject(windowPoint, this.modelview, this.projection, this.viewport);
    }

    public Vec4 getEyePoint() {
        if (this.lastEyePoint == null)
            this.lastEyePoint = Vec4.UNIT_W.transformBy4(this.modelviewInv);
        return this.lastEyePoint;
    }

    public Vec4 getCenterPoint() {
        Vec4 eyePoint = this.getEyePoint();
        Intersection[] intersection = this.globe.intersect(new Line(eyePoint, this.getForwardVector()), 0);
        return intersection == null ? null : (intersection[0].getIntersectionPoint());
    }

    public Position getCenterPosition() {
        Vec4 eyePoint = this.getEyePoint();
        Intersection[] intersection = this.globe.intersect(new Line(eyePoint, this.getForwardVector()), 0);
        return (this.globe.computePositionFromPoint(intersection[0].getIntersectionPoint()));
    }

    public Vec4 getCurrentEyePoint() {
        if (this.globe != null) {
            Matrix modelview = ViewUtil.computeTransformMatrix(this.globe, this.eyePosition,
                this.heading, this.pitch, this.roll);
            Matrix modelviewInv = modelview.getInverse();
            if (modelviewInv != null) {
                return Vec4.UNIT_W.transformBy4(modelviewInv);
            }
        }

        return Vec4.ZERO;
    }

    public Position getCurrentEyePosition() {
        // This method is intended to compute the eye position from this view's current parameters. It can be called
        // without having previously applied this view in apply().

        if (this.globe != null) {
            Matrix modelview = ViewUtil.computeTransformMatrix(this.globe, this.eyePosition,
                this.heading, this.pitch, this.roll);
            Matrix modelviewInv = modelview.getInverse();
            if (modelviewInv != null) {
                Vec4 eyePoint = Vec4.UNIT_W.transformBy4(modelviewInv);
                return this.globe.computePositionFromPoint(eyePoint);
            }
        }

        return Position.ZERO;
    }

    public Position getEyePosition() {
        return this.lastEyePosition;
    }

    public void setEyePosition(Position eyePosition) {

        this.eyePosition = eyePosition;
        this.updateModelViewStateID();

        //resolveCollisionsWithCenterPosition();
    }

    public Angle getHeading() {
        return this.heading;
    }

    public void setHeading(Angle heading) {

        this.heading = ViewUtil.normalizedHeading(heading);

        this.updateModelViewStateID();
        //resolveCollisionsWithPitch();
    }

    public Angle getPitch() {
        return this.pitch;
    }

    public void setPitch(Angle pitch) {

        this.pitch = pitch;
        this.updateModelViewStateID();
        //resolveCollisionsWithPitch();
    }

    public Angle getRoll() {
        return this.roll;
    }

    public void setRoll(Angle roll) {

        this.roll = ViewUtil.normalizedRoll(roll);
        this.updateModelViewStateID();
    }

    public Vec4 getUpVector() {
        if (this.lastUpVector == null)
            this.lastUpVector = Vec4.UNIT_Y.transformBy4(this.modelviewInv);
        return this.lastUpVector;
    }

    public Vec4 getForwardVector() {
        if (this.lastForwardVector == null)
            this.lastForwardVector = Vec4.UNIT_NEGATIVE_Z.transformBy4(this.modelviewInv);
        return this.lastForwardVector;
    }

    /**
     * Returns the most up-to-date forward vector. Unlike {@link #getForwardVector()}, this method will return the
     * View's immediate forward vector.
     *
     * @return Vec4 of the forward axis.
     */
    public Vec4 getCurrentForwardVector() {
        if (this.globe != null) {
            Matrix modelview = ViewUtil.computeTransformMatrix(this.globe, this.eyePosition,
                this.heading, this.pitch, this.roll);
            Matrix modelviewInv = modelview.getInverse();
            if (modelviewInv != null) {
                return Vec4.UNIT_NEGATIVE_Z.transformBy4(modelviewInv);
            }
        }

        return null;
    }

    protected void setViewState(ViewUtil.ViewState modelCoords) {
        if (modelCoords != null) {
            if ((modelCoords.position) != null) {
                this.eyePosition = ViewUtil.normalizedEyePosition((modelCoords.position));
            }
            if ((modelCoords.heading) != null) {
                this.heading = ViewUtil.normalizedHeading((modelCoords.heading));
            }
            if ((modelCoords.pitch) != null) {
                this.pitch = ViewUtil.normalizedPitch((modelCoords.pitch));
            }
            if ((modelCoords.roll) != null) {
                this.roll = ViewUtil.normalizedRoll((modelCoords.roll));
            }
        }
    }

    public void setOrientation(Position eyePosition, Position centerPosition) {

        Vec4 newEyePoint = this.globe.computePointFromPosition(eyePosition);
        Vec4 newCenterPoint = this.globe.computePointFromPosition(centerPosition);
//        if (newEyePoint == null || newCenterPoint == null) {
//            String message = Logging.getMessage("View.ErrorSettingOrientation", eyePosition, centerPosition);
//            Logging.logger().severe(message);
//            throw new IllegalArgumentException(message);
//        }

        // If eye lat/lon != center lat/lon, then the surface normal at the center point will be a good value
        // for the up direction.
        Vec4 up = this.globe.computeSurfaceNormalAtPoint(newCenterPoint);

        // Otherwise, estimate the up direction by using the *current* heading with the new center position.
        Vec4 forward = newCenterPoint.subtract3(newEyePoint).normalize3();
        if (forward.cross3(up).getLength3() < 0.001) {
            Matrix modelview = ViewUtil.computeTransformMatrix(this.globe, eyePosition, this.heading, Angle.ZERO,
                Angle.ZERO);
            Matrix modelviewInv = modelview.getInverse();
            if (modelviewInv != null) {
                up = Vec4.UNIT_Y.transformBy4(modelviewInv);
            }
        }

        ViewUtil.ViewState modelCoords = ViewUtil.computeViewState(
            this.globe, newEyePoint, newCenterPoint, up);

        setViewState(modelCoords);

        this.updateModelViewStateID();
    }

    public void stopAnimations() {
        viewInputHandler.stopAnimators();
    }

    public boolean isAnimating() {
        return viewInputHandler.isAnimating();
    }

    public void goTo(Position position, double distance) {
        viewInputHandler.goTo(position, distance);
    }

    public Line computeRayFromScreenPoint(double x, double y) {
        return ViewUtil.computeRayFromScreenPoint(this, x, y,
            this.modelview, this.projection, this.viewport);
    }

    public Position computePositionFromScreenPoint(double x, double y) {
        if (this.globe != null) {
            Line ray = computeRayFromScreenPoint(x, y);
            if (ray != null)
                return this.globe.getIntersectionPosition(ray);
        }

        return null;
    }

    public double computePixelSizeAtDistance(double distance) {
        return ViewUtil.computePixelSizeAtDistance(distance, this.fieldOfView, this.viewport);
    }

    protected Position computeEyePositionFromModelview() {
        if (this.globe != null) {
            Vec4 eyePoint = Vec4.UNIT_W.transformBy4(this.modelviewInv);
            return this.globe.computePositionFromPoint(eyePoint);
        }

        return Position.ZERO;
    }

    public double getHorizonDistance() {
        return this.horizonDistance;
    }

    protected double computeHorizonDistance() {
        return this.computeHorizonDistance(computeEyePositionFromModelview());
    }

    protected double computeHorizonDistance(Position eyePosition) {
        if (this.globe != null && eyePosition != null) {
            double elevation = eyePosition.getElevation();
            double elevationAboveSurface = ViewUtil.computeElevationAboveSurface(this.dc, eyePosition);
            return ViewUtil.computeHorizonDistance(this.globe, Math.max(elevation, elevationAboveSurface));
        }

        return 0;
    }

    public ViewPropertyLimits getViewPropertyLimits() {
        return this.viewLimits;
    }

    protected double computeNearClipDistance() {
        return computeNearDistance(getCurrentEyePosition());
    }

    protected double computeFarClipDistance() {
        return computeFarDistance(getCurrentEyePosition());
    }

    protected double computeNearDistance(Position eyePosition) {
        // Compute the near clip distance in order to achieve a desired depth resolution at the far clip distance. This
        // computed distance is limited such that it does not intersect the terrain when possible and is never less than
        // a predetermined minimum (usually one). The computed near distance automatically scales with the resolution of
        // the OpenGL depth buffer.
        int depthBits = this.dc.getGLRuntimeCapabilities().getDepthBits();
        double nearDistance = ViewUtil.computePerspectiveNearDistance(this.farClipDistance,
            BasicView.DEFAULT_DEPTH_RESOLUTION,
            depthBits);

        // Prevent the near clip plane from intersecting the terrain.
        if (eyePosition != null && this.dc != null) {
            double distanceToSurface = ViewUtil.computeElevationAboveSurface(this.dc, eyePosition);
            if (distanceToSurface > 0) {
                double maxNearDistance = ViewUtil.computePerspectiveNearDistance(this.fieldOfView, distanceToSurface);
                if (nearDistance > maxNearDistance)
                    nearDistance = maxNearDistance;
            } else {
                nearDistance = BasicView.MINIMUM_NEAR_DISTANCE;
            }
        }

        // Prevent the near clip plane from becoming unnecessarily small. A very small clip plane is not useful for
        // rendering the WorldWind scene, and significantly reduces the depth precision in the majority of the scene.
        if (nearDistance < BasicView.MINIMUM_NEAR_DISTANCE)
            nearDistance = BasicView.MINIMUM_NEAR_DISTANCE;

        return nearDistance;
    }

    protected double computeFarDistance(Position eyePosition) {
        double far = 0;
        if (eyePosition != null) {
            far = computeHorizonDistance(eyePosition);
        }

        return Math.max(far, BasicView.MINIMUM_FAR_DISTANCE);
    }

    public Matrix getProjectionMatrix() {
        return this.projection;
    }

    public String getRestorableState() {
        RestorableSupport rs = RestorableSupport.newRestorableSupport();
        // Creating a new RestorableSupport failed. RestorableSupport logged the problem, so just return null.

        this.doGetRestorableState(rs, null);

        return rs.getStateAsXml();
    }

    public void restoreState(String stateInXml) {

        RestorableSupport rs;
        try {
            rs = RestorableSupport.parse(stateInXml);
        }
        catch (RuntimeException e) {
            // Parsing the document specified by stateInXml failed.
            String message = Logging.getMessage("generic.ExceptionAttemptingToParseStateXml", stateInXml);
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message, e);
        }

        this.doRestoreState(rs, null);
    }

    //**************************************************************//
    //******************** Restorable State  ***********************//
    //**************************************************************//

    /**
     * Update the modelview state identifier. This method should be called whenever one of the fields that affects the
     * modelview matrix is changed.
     */
    protected void updateModelViewStateID() {
        this.viewStateID.getAndIncrement();
    }

    protected void doGetRestorableState(RestorableSupport rs, RestorableSupport.StateObject context) {
        this.getViewPropertyLimits().getRestorableState(rs, rs.addStateObject(context, "viewPropertyLimits"));

        rs.addStateValueAsBoolean(context, "detectCollisions", this.isDetectCollisions());

        if (this.getFieldOfView() != null)
            rs.addStateValueAsDouble(context, "fieldOfView", this.getFieldOfView().degrees);

        rs.addStateValueAsDouble(context, "nearClipDistance", this.getNearClipDistance());
        rs.addStateValueAsDouble(context, "farClipDistance", this.getFarClipDistance());

        if (this.getEyePosition() != null)
            rs.addStateValueAsPosition(context, "eyePosition", this.getEyePosition());

        if (this.getHeading() != null)
            rs.addStateValueAsDouble(context, "heading", this.getHeading().degrees);

        if (this.getPitch() != null)
            rs.addStateValueAsDouble(context, "pitch", this.getPitch().degrees);
    }

    protected void doRestoreState(RestorableSupport rs, RestorableSupport.StateObject context) {
        // Restore the property limits and collision detection flags before restoring the view's position and
        // orientation. This has the effect of ensuring that the view's position and orientation are consistent with the
        // current property limits and the current surface collision state.

        RestorableSupport.StateObject so = rs.getStateObject(context, "viewPropertyLimits");
        if (so != null)
            this.getViewPropertyLimits().restoreState(rs, so);

        Boolean b = rs.getStateValueAsBoolean(context, "detectCollisions");
        if (b != null)
            this.setDetectCollisions(b);

        Double d = rs.getStateValueAsDouble(context, "fieldOfView");
        if (d != null)
            this.setFieldOfView(new Angle(d));

        d = rs.getStateValueAsDouble(context, "nearClipDistance");
        if (d != null)
            this.setNearClipDistance(d);

        d = rs.getStateValueAsDouble(context, "farClipDistance");
        if (d != null)
            this.setFarClipDistance(d);

        Position p = rs.getStateValueAsPosition(context, "eyePosition");
        if (p != null)
            this.setEyePosition(p);

        d = rs.getStateValueAsDouble(context, "heading");
        if (d != null)
            this.setHeading(new Angle(d));

        d = rs.getStateValueAsDouble(context, "pitch");
        if (d != null)
            this.setPitch(new Angle(d));
    }

    public Matrix pushReferenceCenter(DrawContext dc, Vec4 referenceCenter) {

        Matrix modelview = getModelviewMatrix();

        // Compute a new model-view matrix with origin at referenceCenter.
        Matrix matrix = null;
        if (modelview != null)
            matrix = modelview.multiply(Matrix.fromTranslation(referenceCenter));

        GL2 gl = dc.getGL2(); // GL initialization checks for GL2 compatibility.

        // Store the current matrix-mode state.
        OGLStackHandler ogsh = new OGLStackHandler();

        try {
            ogsh.pushAttrib(gl, GL2.GL_TRANSFORM_BIT);

            gl.glMatrixMode(GL2.GL_MODELVIEW);

            // Push and load a new model-view matrix to the current OpenGL context held by 'dc'.
            gl.glPushMatrix();
            if (matrix != null) {
                double[] matrixArray = new double[16];
                matrix.toArray(matrixArray, 0, false);
                gl.glLoadMatrixd(matrixArray, 0);
            }
        }
        finally {
            ogsh.pop(gl);
        }

        return matrix;
    }

    public Matrix setReferenceCenter(DrawContext dc, Vec4 referenceCenter) {

        Matrix modelview = getModelviewMatrix();

        // Compute a new model-view matrix with origin at referenceCenter.
        Matrix matrix = null;
        if (modelview != null)
            matrix = modelview.multiply(Matrix.fromTranslation(referenceCenter));
        if (matrix == null)
            return null;

        GL2 gl = dc.getGL2(); // GL initialization checks for GL2 compatibility.

        gl.glMatrixMode(GL2.GL_MODELVIEW);

        double[] matrixArray = new double[16];
        matrix.toArray(matrixArray, 0, false);
        gl.glLoadMatrixd(matrixArray, 0);

        return matrix;
    }

    /**
     * Removes the model-view matrix on top of the matrix stack, and restores the original matrix.
     *
     * @param dc the current WorldWind drawing context on which the original matrix will be restored.
     * @throws IllegalArgumentException if <code>dc</code> is null, or if the <code>Globe</code> or <code>GL</code>
     *                                  instances in <code>dc</code> are null.
     */
    public void popReferenceCenter(DrawContext dc) {

        GL2 gl = dc.getGL2(); // GL initialization checks for GL2 compatibility.

        // Store the current matrix-mode state.
        OGLStackHandler ogsh = new OGLStackHandler();

        try {
            ogsh.pushAttrib(gl, GL2.GL_TRANSFORM_BIT);

            gl.glMatrixMode(GL2.GL_MODELVIEW);

            // Pop the top model-view matrix.
            gl.glPopMatrix();
        }
        finally {
            ogsh.pop(gl);
        }
    }

    /**
     * Transforms the specified object coordinates into window coordinates using the given modelview and projection
     * matrices, and viewport.
     *
     * @param point      The object coordinate to transform
     * @param modelview  The modelview matrix
     * @param projection The projection matrix
     * @param viewport   The viewport
     * @return the transformed coordinates
     */
    public Vec4 project(Vec4 point, Matrix modelview, Matrix projection, Rectangle viewport) {
//        if (point == null) {
//            String message = Logging.getMessage("nullValue.PointIsNull");
//            Logging.logger().severe(message);
//            throw new IllegalArgumentException(message);
//        }
//        if (modelview == null || projection == null) {
//            String message = Logging.getMessage("nullValue.MatrixIsNull");
//            Logging.logger().severe(message);
//            throw new IllegalArgumentException(message);
//        }
//        if (viewport == null) {
//            String message = Logging.getMessage("nullValue.RectangleIsNull");
//            Logging.logger().severe(message);
//            throw new IllegalArgumentException(message);
//        }

        // GLU expects matrices as column-major arrays.
        double[] modelviewArray = new double[16];
        double[] projectionArray = new double[16];
        modelview.toArray(modelviewArray, 0, false);
        projection.toArray(projectionArray, 0, false);
        // GLU expects the viewport as a four-component array.
        int[] viewportArray = {viewport.x, viewport.y, viewport.width, viewport.height};

        double[] result = new double[3];
        if (!this.dc.getGLU().gluProject(
            point.x, point.y, point.z,
            modelviewArray, 0,
            projectionArray, 0,
            viewportArray, 0,
            result, 0)) {
            return null;
        }

        return Vec4.fromArray3(result, 0);
    }

    /**
     * Maps the given window coordinates into model coordinates using the given matrices and viewport.
     *
     * @param windowPoint the window point
     * @param modelview   the modelview matrix
     * @param projection  the projection matrix
     * @param viewport    the window viewport
     * @return the unprojected point
     */
    public Vec4 unProject(Vec4 windowPoint, Matrix modelview, Matrix projection, Rectangle viewport) {
//        if (windowPoint == null) {
//            String message = Logging.getMessage("nullValue.PointIsNull");
//            Logging.logger().severe(message);
//            throw new IllegalArgumentException(message);
//        }
//        if (modelview == null || projection == null) {
//            String message = Logging.getMessage("nullValue.MatrixIsNull");
//            Logging.logger().severe(message);
//            throw new IllegalArgumentException(message);
//        }
//        if (viewport == null) {
//            String message = Logging.getMessage("nullValue.RectangleIsNull");
//            Logging.logger().severe(message);
//            throw new IllegalArgumentException(message);
//        }

        // GLU expects matrices as column-major arrays.
        double[] modelviewArray = new double[16];
        double[] projectionArray = new double[16];
        modelview.toArray(modelviewArray, 0, false);
        projection.toArray(projectionArray, 0, false);
        // GLU expects the viewport as a four-component array.
        int[] viewportArray = {viewport.x, viewport.y, viewport.width, viewport.height};

        double[] result = new double[3];
        if (!this.dc.getGLU().gluUnProject(
            windowPoint.x, windowPoint.y, windowPoint.z,
            modelviewArray, 0,
            projectionArray, 0,
            viewportArray, 0,
            result, 0)) {
            return null;
        }

        return Vec4.fromArray3(result, 0);
    }

    /**
     * Add an animator to the this View.  The View does not start the animator.
     *
     * @param animator the {@link Animator} to be added
     */
    public void addAnimator(Animator animator) {
        viewInputHandler.addAnimator(animator);
    }
}