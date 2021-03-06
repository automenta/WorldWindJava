/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.view;

import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.glu.gl2.GLUgl2;
import gov.nasa.worldwind.View;
import gov.nasa.worldwind.animation.*;
import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.globes.Globe;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.util.Logging;

import java.awt.*;

/**
 * @author jym
 * @version $Id: ViewUtil.java 1933 2014-04-14 22:54:19Z dcollins $
 */
public class ViewUtil {
    /**
     * Create an animator to animate heading.
     *
     * @param view  View to animate
     * @param begin starting heading
     * @param end   final heading
     * @return An Animator to animate heading.
     */
    public static AngleAnimator createHeadingAnimator(View view, Angle begin, Angle end) {

        final long MIN_LENGTH_MILLIS = 500;
        final long MAX_LENGTH_MILLIS = 3000;
        long lengthMillis = AnimationSupport.getScaledTimeMillisecs(
            begin, end, Angle.POS180,
            MIN_LENGTH_MILLIS, MAX_LENGTH_MILLIS);

        return new AngleAnimator(new ScheduledInterpolator(lengthMillis),
            begin, end, new ViewPropertyAccessor.HeadingAccessor(view));
    }

    /**
     * Create an animator to animate pitch.
     *
     * @param view  View to animate
     * @param begin starting pitch
     * @param end   final pitch
     * @return An Animator to animate pitch.
     */
    public static AngleAnimator createPitchAnimator(View view, Angle begin, Angle end) {

        final long MIN_LENGTH_MILLIS = 500;
        final long MAX_LENGTH_MILLIS = 3000;
        long lengthMillis = AnimationSupport.getScaledTimeMillisecs(
            begin, end, Angle.POS180,
            MIN_LENGTH_MILLIS, MAX_LENGTH_MILLIS);

        return new AngleAnimator(new ScheduledInterpolator(lengthMillis),
            begin, end, new ViewPropertyAccessor.PitchAccessor(view));
    }

    /**
     * Create an animator to animate roll.
     *
     * @param view  View to animate
     * @param begin starting roll
     * @param end   final roll
     * @return An Animator to animate roll.
     */
    public static AngleAnimator createRollAnimator(View view, Angle begin, Angle end) {

        final long MIN_LENGTH_MILLIS = 500;
        final long MAX_LENGTH_MILLIS = 3000;
        long lengthMillis = AnimationSupport.getScaledTimeMillisecs(
            begin, end, Angle.POS180,
            MIN_LENGTH_MILLIS, MAX_LENGTH_MILLIS);

        return new AngleAnimator(new ScheduledInterpolator(lengthMillis),
            begin, end, new ViewPropertyAccessor.RollAccessor(view));
    }

    /**
     * Create an animator to animate heading, pitch, and roll.
     *
     * @param view         View to animate
     * @param beginHeading staring heading
     * @param endHeading   final heading
     * @param beginPitch   starting pitch
     * @param endPitch     final pitch
     * @param beginRoll    starting roll
     * @param endRoll      final roll
     * @return A CompoundAnimator to animate heading, pitch, and roll.
     */
    public static CompoundAnimator createHeadingPitchRollAnimator(View view, Angle beginHeading, Angle endHeading,
        Angle beginPitch, Angle endPitch, Angle beginRoll, Angle endRoll) {

        final long MIN_LENGTH_MILLIS = 500;
        final long MAX_LENGTH_MILLIS = 3000;
        long headingLengthMillis = AnimationSupport.getScaledTimeMillisecs(
            beginHeading, endHeading, Angle.POS180,
            MIN_LENGTH_MILLIS, MAX_LENGTH_MILLIS);
        long pitchLengthMillis = AnimationSupport.getScaledTimeMillisecs(
            beginPitch, endPitch, Angle.POS90,
            MIN_LENGTH_MILLIS, MAX_LENGTH_MILLIS / 2L);
        long rollLengthMillis = AnimationSupport.getScaledTimeMillisecs(
            beginRoll, endRoll, Angle.POS90,
            MIN_LENGTH_MILLIS, MAX_LENGTH_MILLIS / 2L);
        long lengthMillis = headingLengthMillis + pitchLengthMillis + rollLengthMillis;

        AngleAnimator headingAnimator = ViewUtil.createHeadingAnimator(view, beginHeading, endHeading);
        AngleAnimator pitchAnimator = ViewUtil.createPitchAnimator(view, beginPitch, endPitch);
        AngleAnimator rollAnimator = ViewUtil.createRollAnimator(view, beginRoll, endRoll);

        return (new CompoundAnimator(new ScheduledInterpolator(lengthMillis),
            headingAnimator, pitchAnimator, rollAnimator));
    }

    public static PositionAnimator createEyePositionAnimator(
        View view, long timeToMove, Position begin, Position end) {
        return new PositionAnimator(new ScheduledInterpolator(timeToMove),
            begin, end, ViewPropertyAccessor.createEyePositionAccessor(view));
    }

    public static Point subtract(Point a, Point b) {
        if (a == null || b == null)
            return null;
        return new Point((int) (a.getX() - b.getX()), (int) (a.getY() - b.getY()));
    }

    public static Matrix computeTransformMatrix(Globe globe, Position position, Angle heading, Angle pitch,
        Angle roll) {
//        if (heading == null) {
//            String message = Logging.getMessage("nullValue.HeadingIsNull");
//            Logging.logger().severe(message);
//            throw new IllegalArgumentException(message);
//        }
//        if (pitch == null) {
//            String message = Logging.getMessage("nullValue.PitchIsNull");
//            Logging.logger().severe(message);
//            throw new IllegalArgumentException(message);
//        }

        // To get a yaw-pitch-roll transform, do the view transform in reverse (i.e. roll-pitch-yaw)
        Matrix transform = Matrix.IDENTITY;
        transform = transform.multiply(Matrix.fromAxisAngle(roll, 0, 0, 1));
        transform = transform.multiply(Matrix.fromAxisAngle(pitch, -1, 0, 0));
        transform = transform.multiply(Matrix.fromAxisAngle(heading, 0, 0, 1));

        transform = transform.multiply(ViewUtil.computePositionTransform(globe, position));

        return transform;
    }

    public static Matrix computePositionTransform(Globe globe, Position center) {
//        if (globe == null) {
//            String message = Logging.getMessage("nullValue.GlobeIsNull");
//            Logging.logger().severe(message);
//            throw new IllegalArgumentException(message);
//        }
//        if (center == null) {
//            String message = Logging.getMessage("nullValue.CenterIsNull");
//            Logging.logger().severe(message);
//            throw new IllegalArgumentException(message);
//        }

        // The view eye position will be the same as the center position.
        // This is only the case without any zoom, heading, and pitch.
        Vec4 eyePoint = globe.computePointFromPosition(center);

        // The view forward direction will be colinear with the
        // geoid surface normal at the center position.
        Vec4 normal = globe.computeSurfaceNormalAtLocation(center.getLat(), center.getLon());
        Vec4 lookAtPoint = eyePoint.subtract3(normal);

        // The up direction will be pointing towards the north pole.
        Vec4 north = globe.computeNorthPointingTangentAtLocation(center.getLat(), center.getLon());

        // Creates a viewing matrix looking from eyePoint towards lookAtPoint,
        // with the given up direction. The forward, right, and up vectors
        // contained in the matrix are guaranteed to be orthogonal. This means
        // that the Matrix's up may not be equivalent to the specified up vector
        // here (though it will point in the same general direction).
        // In this case, the forward direction would not be affected.
        return Matrix.fromViewLookAt(eyePoint, lookAtPoint, north);
    }

    public static Matrix computeModelViewMatrix(Globe globe, Vec4 eyePoint, Vec4 centerPoint, Vec4 up) {

        return (Matrix.fromViewLookAt(eyePoint, centerPoint, up));
    }

    public static Vec4 getUpVector(Globe globe, Vec4 lookAtPoint) {
        return globe.computeSurfaceNormalAtPoint(lookAtPoint);
    }

    public static ViewState computeViewState(Globe globe, Vec4 eyePoint, Vec4 centerPoint, Vec4 up) {

        Matrix modelview = Matrix.fromViewLookAt(eyePoint, centerPoint, up);
        return ViewUtil.computeModelCoordinates(globe, modelview, centerPoint,
            eyePoint);
    }

    public static ViewState computeModelCoordinates(Globe globe, Matrix modelTransform, Vec4 centerPoint,
        Vec4 eyePoint) {
//        if (globe == null) {
//            String message = Logging.getMessage("nullValue.GlobeIsNull");
//            Logging.logger().severe(message);
//            throw new IllegalArgumentException(message);
//        }
//        if (modelTransform == null) {
//            String message = "nullValue.ModelTransformIsNull";
//            Logging.logger().severe(message);
//            throw new IllegalArgumentException(message);
//        }

        // Compute the center position.
        Position centerPos = globe.computePositionFromPoint(centerPoint);
        // Compute the center position transform.
        Matrix centerTransform = ViewUtil.computePositionTransform(globe, centerPos);
        Matrix centerTransformInv = centerTransform.getInverse();
//        if (centerTransformInv == null) {
//            String message = Logging.getMessage("generic.NoninvertibleMatrix");
//            Logging.logger().severe(message);
//            throw new IllegalStateException(message);
//        }

        // Compute the heading-pitch-zoom transform.
        Matrix hpzTransform = modelTransform.multiply(centerTransformInv);
        // Extract the heading, pitch, and zoom values from the transform.
        Angle heading = ViewUtil.computeHeading(hpzTransform);
        Angle pitch = ViewUtil.computePitch(hpzTransform);
        if (heading == null || pitch == null)
            return null;
        Position viewPosition = globe.computePositionFromPoint(eyePoint);
        return new ViewState(viewPosition, heading, pitch, Angle.ZERO);
    }

    public static Angle computeHeading(Matrix headingPitchZoomTransform) {

        return headingPitchZoomTransform.getRotationZ();
    }

    public static Angle computePitch(Matrix transform) {

        Angle a = transform.getRotationX();
        if (a != null)
            a = a.multiply(-1.0);
        return a;
    }

    public static Angle computeRoll(Matrix transform) {

        return transform.getRotationY();
    }

    public static Position computePosition(Globe globe, Matrix transform) {
        Vec4 v = transform.getTranslation();
        Position p = globe.computePositionFromPoint(v);

        return p != null ? p : Position.ZERO;
    }

    public static boolean validateViewState(ViewState viewState) {
        return (viewState != null
            && viewState.position != null
            && viewState.position.getLat().degrees >= -90
            && viewState.position.getLat().degrees <= 90
            && viewState.heading != null
            && viewState.pitch != null
            && viewState.pitch.degrees >= 0
            && viewState.pitch.degrees <= 90);
    }

    public static Position normalizedEyePosition(Position unnormalizedPosition) {

        return new Position(
            Angle.latNorm(unnormalizedPosition.getLat()),
            Angle.lonNorm(unnormalizedPosition.getLon()),
            unnormalizedPosition.getElevation());
    }

    public static Angle normalizedHeading(Angle unnormalizedHeading) {

        double degrees = unnormalizedHeading.degrees;
        double heading = degrees % 360;
        double degrees1 = heading > 180 ? heading - 360 : (heading < -180 ? 360 + heading : heading);
        return new Angle(degrees1);
    }

    public static Angle normalizedPitch(Angle unnormalizedPitch) {
//        if (unnormalizedPitch == null) {
//            String message = Logging.getMessage("nullValue.AngleIsNull");
//            Logging.logger().severe(message);
//            throw new IllegalArgumentException(message);
//        }

        // Normalize pitch to the range [-180, 180].
        double degrees = unnormalizedPitch.degrees;
        double pitch = degrees % 360;
        double degrees1 = pitch > 180 ? pitch - 360 : (pitch < -180 ? 360 + pitch : pitch);
        return new Angle(degrees1);
    }

    public static Angle normalizedRoll(Angle unnormalizedRoll) {

        double degrees = unnormalizedRoll.degrees;
        double roll = degrees % 360;
        double degrees1 = roll > 180 ? roll - 360 : (roll < -180 ? 360 + roll : roll);
        return new Angle(degrees1);
    }

    public static Line computeRayFromScreenPoint(View view, double x, double y,
        Matrix modelview, Matrix projection, Rectangle viewport) {
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

        // Compute a ray originating from the view, and passing through the screen point (x, y).
        //
        // Taken from the "OpenGL Technical FAQ & Troubleshooting Guide",
        // section 20.010 "How can I know which primitive a user has selected with the mouse?"
        //
        // http://www.opengl.org/resources/faq/technical/selection.htm#sele0010

        Matrix modelViewInv = modelview.getInverse();
        if (modelViewInv == null)
            return null;

        Vec4 eye = Vec4.UNIT_W.transformBy4(modelViewInv);

        double yInGLCoords = viewport.height - y - 1;
        Vec4 a = view.unProject(new Vec4(x, yInGLCoords, 0, 0));
        Vec4 b = view.unProject(new Vec4(x, yInGLCoords, 1, 0));
        if (a == null || b == null)
            return null;

        return new Line(eye, b.subtract3(a).normalize3());
    }

    public static double computePixelSizeAtDistance(double distance, Angle fieldOfView, Rectangle viewport) {
//        if (fieldOfView == null) {
//            String message = Logging.getMessage("nullValue.AngleIsNull");
//            Logging.logger().severe(message);
//            throw new IllegalArgumentException(message);
//        }
//        if (viewport == null) {
//            String message = Logging.getMessage("nullValue.RectangleIsNull");
//            Logging.logger().severe(message);
//            throw new IllegalArgumentException(message);
//        }

        // If the viewport width is zero, than replace it with 1, which effectively ignores the viewport width.
        double vw = viewport.getWidth();

        return Math.abs(distance) * (2 * fieldOfView.tanHalfAngle() / (vw <= 0 ? 1 : vw));
    }

    public static double computeHorizonDistance(Globe globe, double elevation) {
//        if (globe == null) {
//            String message = Logging.getMessage("nullValue.GlobeIsNull");
//            Logging.logger().severe(message);
//            throw new IllegalArgumentException(message);
//        }

        if (elevation <= 0)
            return 0;

        return Math.sqrt(elevation * (2 * globe.getMaximumRadius() + elevation));
    }

    /**
     * Computes a View's vertical field-of-view given a View's horizontal field-of-view and the viewport window
     * dimensions.
     *
     * @param horizontalFieldOfView the angle between the view frustum's left and right clipping planes.
     * @param viewport              the viewport dimensions, in window coordinates (screen pixels).
     * @return the angle between the view frustum's bottom and top clipping planes.
     * @throws IllegalArgumentException if the horitontal-field-of-view is null, or if the viewport rectangle is null.
     */
    public static Angle computeVerticalFieldOfView(Angle horizontalFieldOfView, Rectangle viewport) {
//        if (horizontalFieldOfView == null) {
//            String message = Logging.getMessage("nullValue.FOVIsNull");
//            Logging.logger().severe(message);
//            throw new IllegalArgumentException(message);
//        }
//
//        if (viewport == null) {
//            String message = Logging.getMessage("nullValue.ViewportIsNull");
//            Logging.logger().severe(message);
//            throw new IllegalArgumentException(message);
//        }

        // Taken form "Mathematics for 3D Game Programming and Computer Graphics", page 114.

        double aspectRatio = viewport.getHeight() / viewport.getWidth();
        double distanceToNearPlane = 1 / horizontalFieldOfView.tanHalfAngle();
        double verticalFieldOfViewRadians = 2 * Math.atan(aspectRatio / distanceToNearPlane);

        return Angle.fromRadians(verticalFieldOfViewRadians);
    }

    public static double computeElevationAboveSurface(DrawContext dc, Position position) {
        Globe globe = dc.getGlobe();

        Position surfacePosition = null;
        // Look for the surface geometry point at 'position'.
        Vec4 pointOnGlobe = dc.getPointOnTerrain(position.getLat(), position.getLon());
        if (pointOnGlobe != null)
            surfacePosition = globe.computePositionFromPoint(pointOnGlobe);
        // Fallback to using globe elevation values.
        if (surfacePosition == null)
            surfacePosition = new Position(
                position,
                globe.elevationOrZero(position.getLat(), position.getLon()) * dc.getVerticalExaggeration());

        return position.getElevation() - surfacePosition.getElevation();
    }

    /**
     * Computes the maximum near clip distance for a perspective projection that avoids clipping an object at a given
     * distance from the eye point. The given distance should specify the smallest distance between the eye and the
     * object being viewed, but may be an approximation if an exact clip distance is not required.
     *
     * @param fieldOfView      The viewport rectangle, in OpenGL screen coordinates.
     * @param distanceToObject The distance from the perspective eye point to the nearest object, in model coordinates.
     * @return The maximum near clip distance, in model coordinates.
     * @throws IllegalArgumentException if the field of view is null, or if the distance is negative.
     */
    public static double computePerspectiveNearDistance(Angle fieldOfView, double distanceToObject) {
//        if (fieldOfView == null) {
//            String msg = Logging.getMessage("nullValue.FOVIsNull");
//            Logging.logger().severe(msg);
//            throw new IllegalArgumentException(msg);
//        }

        if (distanceToObject < 0) {
            String msg = Logging.getMessage("generic.DistanceLessThanZero");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        double tanHalfFov = fieldOfView.tanHalfAngle();
        return distanceToObject / (2 * Math.sqrt(2 * tanHalfFov * tanHalfFov + 1));
    }

    /**
     * Computes the near clip distance that corresponds to a specified far clip distance and a resolution at the far
     * clip distance. This returns zero if either the distance or the resolution are zero.
     *
     * @param farDistance   The far clip distance, in model coordinates.
     * @param farResolution The depth resolution at the far clip plane, in model coordinates.
     * @param depthBits     The number of bitplanes in the depth buffer. This is typically 16, 24, or 32 for OpenGL
     *                      depth buffers.
     * @return The near clip distance, in model coordinates.
     * @throws IllegalArgumentException if either the distance or the resolution are negative, or if the depthBits is
     *                                  less than one.
     */
    public static double computePerspectiveNearDistance(double farDistance, double farResolution, int depthBits) {
        if (farDistance < 0) {
            String msg = Logging.getMessage("generic.DistanceLessThanZero");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        if (farResolution < 0) {
            String msg = Logging.getMessage("generic.ResolutionLessThanZero");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        if (depthBits < 1) {
            String msg = Logging.getMessage("generic.DepthBitsLessThanOne");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        if (farDistance == 0 || farResolution == 0) {
            return 0;
        }

        double maxDepthValue = (1L << depthBits) - 1L;

        return farDistance / (maxDepthValue / (1 - farResolution / farDistance) - maxDepthValue + 1);
    }

    /**
     * Transforms a point in model coordinates to a point in screen coordinates. The returned x and y coordinates are
     * relative to the screen's lower left hand screen corner, and the returned z coordinate defines the point's depth
     * in screen coordinates (in the range [0, 1]). This returns null if the specified combination of modelview matrix,
     * projection matrix, and viewport cannot produce a transformation.
     *
     * @param modelPoint the point in model coordinates to transform into window coordinates.
     * @param modelview  the modelview matrix.
     * @param projection the projection matrix.
     * @param viewport   the viewport rectangle.
     * @return the point in window coordinates, or null if the point cannot be transformed.
     * @throws IllegalArgumentException if any of the model point, modelview matrix, projection matrix, or viewport
     *                                  rectangle are null.
     */
    public static Vec4 project(Vec4 modelPoint, Matrix modelview, Matrix projection, Rectangle viewport) {
//        if (modelPoint == null) {
//            String message = Logging.getMessage("nullValue.PointIsNull");
//            Logging.logger().severe(message);
//            throw new IllegalArgumentException(message);
//        }
//
//        if (modelview == null) {
//            String message = Logging.getMessage("nullValue.ModelViewIsNull");
//            Logging.logger().severe(message);
//            throw new IllegalArgumentException(message);
//        }
//
//        if (projection == null) {
//            String message = Logging.getMessage("nullValue.ProjectionIsNull");
//            Logging.logger().severe(message);
//            throw new IllegalArgumentException(message);
//        }
//
//        if (viewport == null) {
//            String message = Logging.getMessage("nullValue.ViewportIsNull");
//            Logging.logger().severe(message);
//            throw new IllegalArgumentException(message);
//        }

        GLU glu = new GLUgl2();

        int[] viewportArray = {viewport.x, viewport.y, viewport.width, viewport.height};

        double[] result = new double[3];
        if (!glu.gluProject(
            modelPoint.x, modelPoint.y, modelPoint.z,
            modelview.toGLUArray(), 0,
            projection.toGLUArray(), 0,
            viewportArray, 0,
            result, 0)) {
            return null;
        }

        return Vec4.fromArray3(result, 0);
    }

    /**
     * Transforms a point in screen coordinates to a point in model coordinates. The input x and y are relative to the
     * screen's lower left hand screen corner, while the z-value denotes the point's depth in screen coordinates (in the
     * range [0, 1]). This returns null if the specified combination of modelview matrix, projection matrix, and
     * viewport cannot produce a transformation.
     *
     * @param windowPoint the point in screen coordinates to transform into model coordinates.
     * @param modelview   the modelview matrix.
     * @param projection  the projection matrix.
     * @param viewport    the viewport rectangle.
     * @return the point in model coordinates, or null if the point cannot be transformed.
     * @throws IllegalArgumentException if any of the model point, modelview matrix, projection matrix, or viewport
     *                                  rectangle are null.
     */
    public static Vec4 unProject(Vec4 windowPoint, Matrix modelview, Matrix projection, Rectangle viewport) {
//        if (windowPoint == null) {
//            String message = Logging.getMessage("nullValue.PointIsNull");
//            Logging.logger().severe(message);
//            throw new IllegalArgumentException(message);
//        }
//
//        if (modelview == null) {
//            String message = Logging.getMessage("nullValue.ModelViewIsNull");
//            Logging.logger().severe(message);
//            throw new IllegalArgumentException(message);
//        }
//
//        if (projection == null) {
//            String message = Logging.getMessage("nullValue.ProjectionIsNull");
//            Logging.logger().severe(message);
//            throw new IllegalArgumentException(message);
//        }
//
//        if (viewport == null) {
//            String message = Logging.getMessage("nullValue.ViewportIsNull");
//            Logging.logger().severe(message);
//            throw new IllegalArgumentException(message);
//        }

        GLU glu = new GLUgl2();

        // GLU expects matrices as column-major arrays.
        double[] modelviewArray = modelview.toGLUArray();
        double[] projectionArray = projection.toGLUArray();
        // GLU expects the viewport as a four-component array.
        int[] viewportArray = {viewport.x, viewport.y, viewport.width, viewport.height};

        double[] result = new double[3];

        if (!glu.gluUnProject(
            windowPoint.x, windowPoint.y, windowPoint.z,
            modelviewArray, 0,
            projectionArray, 0,
            viewportArray, 0,
            result, 0)) {
            return null;
        }

        return Vec4.fromArray3(result, 0);
    }

    public static final class ViewState {
        public final Position position;
        public final Angle heading;
        public final Angle pitch;
        public final Angle roll;

        public ViewState(Position position, Angle heading, Angle pitch, Angle roll) {
            this.position = position;
            this.heading = heading;
            this.pitch = pitch;
            this.roll = roll;
        }
    }
}