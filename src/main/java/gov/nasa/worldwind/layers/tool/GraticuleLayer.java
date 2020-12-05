/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.layers.tool;

import gov.nasa.worldwind.View;
import gov.nasa.worldwind.avlist.*;
import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.globes.*;
import gov.nasa.worldwind.layers.AbstractLayer;
import gov.nasa.worldwind.render.*;
import gov.nasa.worldwind.util.*;
import gov.nasa.worldwind.view.orbit.OrbitView;

import java.awt.*;
import java.util.*;

/**
 * Displays a graticule.
 *
 * @author Patrick Murris
 * @version $Id: AbstractGraticuleLayer.java 2153 2014-07-17 17:33:13Z tgaskins $
 */
public class GraticuleLayer extends AbstractLayer {
    /**
     * Solid line rendering style. This style specifies that a line will be drawn without any breaks. <br>
     * <pre><code>_________</code></pre>
     * <br> is an example of a solid line.
     */
    public static final String LINE_STYLE_SOLID = GraticuleRenderingParams.VALUE_LINE_STYLE_SOLID;
    /**
     * Dashed line rendering style. This style specifies that a line will be drawn as a series of long strokes, with
     * space in between. <br>
     * <pre><code>- - - - -</code></pre>
     * <br> is an example of a dashed line.
     */
    public static final String LINE_STYLE_DASHED = GraticuleRenderingParams.VALUE_LINE_STYLE_DASHED;
    /**
     * Dotted line rendering style. This style specifies that a line will be drawn as a series of evenly spaced "square"
     * dots. <br>
     * <pre><code>. . . . .</code></pre>
     * is an example of a dotted line.
     */
    public static final String LINE_STYLE_DOTTED = GraticuleRenderingParams.VALUE_LINE_STYLE_DOTTED;
    protected final GraticuleSupport graticuleSupport = new GraticuleSupport();
    protected ArrayList<GridElement> gridElements;
    protected double terrainConformance = 50;
    protected Globe globe;

    // Update reference states
    protected Vec4 lastEyePoint;
    protected double lastViewHeading = 0;
    protected double lastViewPitch = 0;
    protected double lastViewFOV = 0;
    protected double lastVerticalExaggeration = 1;
    protected GeographicProjection lastProjection;
    protected long frameTimeStamp; // used only for 2D continuous globes to determine whether render is in same frame

    public GraticuleLayer() {
    }

    private static void makeRestorableState(GraticuleRenderingParams params, RestorableSupport rs,
        RestorableSupport.StateObject context) {
        if (params != null && rs != null) {
            for (Map.Entry<String, Object> p : params.getEntries()) {
                if (p.getValue() instanceof Color) {
                    rs.addStateValueAsInteger(context, p.getKey() + ".Red", ((Color) p.getValue()).getRed());
                    rs.addStateValueAsInteger(context, p.getKey() + ".Green", ((Color) p.getValue()).getGreen());
                    rs.addStateValueAsInteger(context, p.getKey() + ".Blue", ((Color) p.getValue()).getBlue());
                    rs.addStateValueAsInteger(context, p.getKey() + ".Alpha", ((Color) p.getValue()).getAlpha());
                }
                else if (p.getValue() instanceof Font) {
                    rs.addStateValueAsString(context, p.getKey() + ".Name", ((Font) p.getValue()).getName());
                    rs.addStateValueAsInteger(context, p.getKey() + ".Style", ((Font) p.getValue()).getStyle());
                    rs.addStateValueAsInteger(context, p.getKey() + ".Size", ((Font) p.getValue()).getSize());
                }
                else {
                    params.getRestorableStateForAVPair(p.getKey(), p.getValue(), rs, context);
                }
            }
        }
    }

    private static void restorableStateToParams(AVList params, RestorableSupport rs,
        RestorableSupport.StateObject context) {
        if (params != null && rs != null) {
            Boolean b = rs.getStateValueAsBoolean(context, GraticuleRenderingParams.KEY_DRAW_LINES);
            if (b != null)
                params.set(GraticuleRenderingParams.KEY_DRAW_LINES, b);

            Integer red = rs.getStateValueAsInteger(context, GraticuleRenderingParams.KEY_LINE_COLOR + ".Red");
            Integer green = rs.getStateValueAsInteger(context, GraticuleRenderingParams.KEY_LINE_COLOR + ".Green");
            Integer blue = rs.getStateValueAsInteger(context, GraticuleRenderingParams.KEY_LINE_COLOR + ".Blue");
            Integer alpha = rs.getStateValueAsInteger(context, GraticuleRenderingParams.KEY_LINE_COLOR + ".Alpha");
            if (red != null && green != null && blue != null && alpha != null)
                params.set(GraticuleRenderingParams.KEY_LINE_COLOR, new Color(red, green, blue, alpha));

            Double d = rs.getStateValueAsDouble(context, GraticuleRenderingParams.KEY_LINE_WIDTH);
            if (d != null)
                params.set(GraticuleRenderingParams.KEY_LINE_WIDTH, d);

            String s = rs.getStateValueAsString(context, GraticuleRenderingParams.KEY_LINE_STYLE);
            if (s != null)
                params.set(GraticuleRenderingParams.KEY_LINE_STYLE, s);

            b = rs.getStateValueAsBoolean(context, GraticuleRenderingParams.KEY_DRAW_LABELS);
            if (b != null)
                params.set(GraticuleRenderingParams.KEY_DRAW_LABELS, b);

            red = rs.getStateValueAsInteger(context, GraticuleRenderingParams.KEY_LABEL_COLOR + ".Red");
            green = rs.getStateValueAsInteger(context, GraticuleRenderingParams.KEY_LABEL_COLOR + ".Green");
            blue = rs.getStateValueAsInteger(context, GraticuleRenderingParams.KEY_LABEL_COLOR + ".Blue");
            alpha = rs.getStateValueAsInteger(context, GraticuleRenderingParams.KEY_LABEL_COLOR + ".Alpha");
            if (red != null && green != null && blue != null && alpha != null)
                params.set(GraticuleRenderingParams.KEY_LABEL_COLOR, new Color(red, green, blue, alpha));

            String name = rs.getStateValueAsString(context, GraticuleRenderingParams.KEY_LABEL_FONT + ".Name");
            Integer style = rs.getStateValueAsInteger(context, GraticuleRenderingParams.KEY_LABEL_FONT + ".Style");
            Integer size = rs.getStateValueAsInteger(context, GraticuleRenderingParams.KEY_LABEL_FONT + ".Size");
            if (name != null && style != null && size != null)
                params.set(GraticuleRenderingParams.KEY_LABEL_FONT, new Font(name, style, size));
        }
    }

    /**
     * Returns whether or not graticule lines will be rendered.
     *
     * @param key the rendering parameters key.
     * @return true if graticule lines will be rendered; false otherwise.
     * @throws IllegalArgumentException <code>key</code> is null.
     */
    public boolean isDrawGraticule(String key) {
        if (key == null) {
            String message = Logging.getMessage("nullValue.StringIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        return getRenderingParams(key).isDrawLines();
    }

    /**
     * Sets whether or not graticule lines will be rendered.
     *
     * @param drawGraticule true to render graticule lines; false to disable rendering.
     * @param key           the rendering parameters key.
     * @throws IllegalArgumentException <code>key</code> is null.
     */
    public void setDrawGraticule(boolean drawGraticule, String key) {
        if (key == null) {
            String message = Logging.getMessage("nullValue.StringIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        getRenderingParams(key).setDrawLines(drawGraticule);
    }

    /**
     * Returns the graticule line Color.
     *
     * @param key the rendering parameters key.
     * @return Color used to render graticule lines.
     * @throws IllegalArgumentException <code>key</code> is null.
     */
    public Color getGraticuleLineColor(String key) {
        if (key == null) {
            String message = Logging.getMessage("nullValue.StringIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        return getRenderingParams(key).getLineColor();
    }

    /**
     * Sets the graticule line Color.
     *
     * @param color Color that will be used to render graticule lines.
     * @param key   the rendering parameters key.
     * @throws IllegalArgumentException if <code>color</code> or <code>key</code> is null.
     */
    public void setGraticuleLineColor(Color color, String key) {
        if (color == null) {
            String message = Logging.getMessage("nullValue.ColorIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        if (key == null) {
            String message = Logging.getMessage("nullValue.StringIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        getRenderingParams(key).setLineColor(color);
    }

    /**
     * Returns the graticule line width.
     *
     * @param key the rendering parameters key.
     * @return width of the graticule lines.
     * @throws IllegalArgumentException <code>key</code> is null.
     */
    public double getGraticuleLineWidth(String key) {
        if (key == null) {
            String message = Logging.getMessage("nullValue.StringIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        return getRenderingParams(key).getLineWidth();
    }

    /**
     * Sets the graticule line width.
     *
     * @param lineWidth width of the graticule lines.
     * @param key       the rendering parameters key.
     * @throws IllegalArgumentException <code>key</code> is null.
     */
    public void setGraticuleLineWidth(double lineWidth, String key) {
        if (key == null) {
            String message = Logging.getMessage("nullValue.StringIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        getRenderingParams(key).setLineWidth(lineWidth);
    }

    /**
     * Returns the graticule line rendering style.
     *
     * @param key the rendering parameters key.
     * @return rendering style of the graticule lines.
     * @throws IllegalArgumentException <code>key</code> is null.
     */
    public String getGraticuleLineStyle(String key) {
        if (key == null) {
            String message = Logging.getMessage("nullValue.StringIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        return getRenderingParams(key).getLineStyle();
    }

    /**
     * Sets the graticule line rendering style.
     *
     * @param lineStyle rendering style of the graticule lines. One of LINE_STYLE_PLAIN, LINE_STYLE_DASHED, or
     *                  LINE_STYLE_DOTTED.
     * @param key       the rendering parameters key.
     * @throws IllegalArgumentException if <code>lineStyle</code> or <code>key</code> is null.
     */
    public void setGraticuleLineStyle(String lineStyle, String key) {
        if (lineStyle == null) {
            String message = Logging.getMessage("nullValue.StringIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        if (key == null) {
            String message = Logging.getMessage("nullValue.StringIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        getRenderingParams(key).setLineStyle(lineStyle);
    }

    /**
     * Returns whether or not graticule labels will be rendered.
     *
     * @param key the rendering parameters key.
     * @return true if graticule labels will be rendered; false otherwise.
     * @throws IllegalArgumentException <code>key</code> is null.
     */
    public boolean isDrawLabels(String key) {
        if (key == null) {
            String message = Logging.getMessage("nullValue.StringIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        return getRenderingParams(key).isDrawLabels();
    }

    /**
     * Sets whether or not graticule labels will be rendered.
     *
     * @param drawLabels true to render graticule labels; false to disable rendering.
     * @param key        the rendering parameters key.
     * @throws IllegalArgumentException <code>key</code> is null.
     */
    public void setDrawLabels(boolean drawLabels, String key) {
        if (key == null) {
            String message = Logging.getMessage("nullValue.StringIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        getRenderingParams(key).setDrawLabels(drawLabels);
    }

    /**
     * Returns the graticule label Color.
     *
     * @param key the rendering parameters key.
     * @return Color used to render graticule labels.
     * @throws IllegalArgumentException <code>key</code> is null.
     */
    public Color getLabelColor(String key) {
        if (key == null) {
            String message = Logging.getMessage("nullValue.StringIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        return getRenderingParams(key).getLabelColor();
    }

    /**
     * Sets the graticule label Color.
     *
     * @param color Color that will be used to render graticule labels.
     * @param key   the rendering parameters key.
     * @throws IllegalArgumentException if <code>color</code> or <code>key</code> is null.
     */
    public void setLabelColor(Color color, String key) {
        if (color == null) {
            String message = Logging.getMessage("nullValue.ColorIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        if (key == null) {
            String message = Logging.getMessage("nullValue.StringIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        getRenderingParams(key).setLabelColor(color);
    }

    /**
     * Returns the Font used for graticule labels.
     *
     * @param key the rendering parameters key.
     * @return Font used to render graticule labels.
     * @throws IllegalArgumentException <code>key</code> is null.
     */
    public Font getLabelFont(String key) {
        if (key == null) {
            String message = Logging.getMessage("nullValue.StringIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        return getRenderingParams(key).getLabelFont();
    }

    /**
     * Sets the Font used for graticule labels.
     *
     * @param font Font that will be used to render graticule labels.
     * @param key  the rendering parameters key.
     * @throws IllegalArgumentException if <code>font</code> or <code>key</code> is null.
     */
    public void setLabelFont(Font font, String key) {
        if (font == null) {
            String message = Logging.getMessage("nullValue.FontIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        if (key == null) {
            String message = Logging.getMessage("nullValue.StringIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        getRenderingParams(key).setLabelFont(font);
    }

    public String getRestorableState() {
        RestorableSupport rs = RestorableSupport.newRestorableSupport();
        // Creating a new RestorableSupport failed. RestorableSupport logged the problem, so just return null.

        RestorableSupport.StateObject so = rs.addStateObject("renderingParams");
        for (Map.Entry<String, GraticuleRenderingParams> entry : this.graticuleSupport.getAllRenderingParams()) {
            if (entry.getKey() != null && entry.getValue() != null) {
                RestorableSupport.StateObject eso = rs.addStateObject(so, entry.getKey());
                makeRestorableState(entry.getValue(), rs, eso);
            }
        }

        return rs.getStateAsXml();
    }

    public void restoreState(String stateInXml) {
        if (stateInXml == null) {
            String message = Logging.getMessage("nullValue.StringIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        RestorableSupport rs;
        try {
            rs = RestorableSupport.parse(stateInXml);
        }
        catch (Exception e) {
            // Parsing the document specified by stateInXml failed.
            String message = Logging.getMessage("generic.ExceptionAttemptingToParseStateXml", stateInXml);
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message, e);
        }

        RestorableSupport.StateObject so = rs.getStateObject("renderingParams");
        if (so != null) {
            RestorableSupport.StateObject[] renderParams = rs.getAllStateObjects(so);
            for (RestorableSupport.StateObject rp : renderParams) {
                if (rp != null) {
                    GraticuleRenderingParams params = getRenderingParams(rp.getName());
                    if (params == null)
                        params = new GraticuleRenderingParams();
                    restorableStateToParams(params, rs, rp);
                    setRenderingParams(rp.getName(), params);
                }
            }
        }
    }

    // --- Graticule Rendering --------------------------------------------------------------

    protected GraticuleRenderingParams getRenderingParams(String key) {
        if (key == null) {
            String message = Logging.getMessage("nullValue.KeyIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        return this.graticuleSupport.getRenderingParams(key);
    }

    protected void setRenderingParams(String key, GraticuleRenderingParams renderingParams) {
        if (key == null) {
            String message = Logging.getMessage("nullValue.KeyIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.graticuleSupport.setRenderingParams(key, renderingParams);
    }

    protected void addRenderable(Object renderable, String paramsKey) {
        if (renderable == null) {
            String message = Logging.getMessage("nullValue.ObjectIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.graticuleSupport.addRenderable(renderable, paramsKey);
    }

    protected void removeAllRenderables() {
        this.graticuleSupport.removeAllRenderables();
    }

    public void doPreRender(DrawContext dc) {
        if (dc == null) {
            String message = Logging.getMessage("nullValue.DrawContextIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (dc.isContinuous2DGlobe()) {
            if (this.needsToUpdate(dc)) {
                this.clear(dc);
                this.selectRenderables(dc);
            }

            // If the frame time stamp is the same, then this is the second or third pass of the same frame. We continue
            // selecting renderables in these passes.
            if (dc.getFrameTimeStamp() == this.frameTimeStamp)
                this.selectRenderables(dc);

            this.frameTimeStamp = dc.getFrameTimeStamp();
        }
        else {
            if (this.needsToUpdate(dc)) {
                this.clear(dc);
                this.selectRenderables(dc);
            }
        }
    }

    public void doRender(DrawContext dc) {
        if (dc == null) {
            String message = Logging.getMessage("nullValue.DrawContextIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        // Render
        this.renderGraticule(dc);
    }

    protected void renderGraticule(DrawContext dc) {
        if (dc == null) {
            String message = Logging.getMessage("nullValue.DrawContextIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.graticuleSupport.render(dc, this.getOpacity());
    }

    /**
     * Select the visible grid elements
     *
     * @param dc the current <code>DrawContext</code>.
     */
    protected void selectRenderables(DrawContext dc) {
        if (dc == null) {
            String message = Logging.getMessage("nullValue.DrawContextIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        // This method is intended to be overriden by subclasses

    }

    /**
     * Determines whether the grid should be updated. It returns true if: <ul> <li>the eye has moved more than 1% of its
     * altitude above ground <li>the view FOV, heading or pitch have changed more than 1 degree <li>vertical
     * exaggeration has changed </ul>
     *
     * @param dc the current <code>DrawContext</code>.
     * @return true if the graticule should be updated.
     */
    @SuppressWarnings("RedundantIfStatement")
    protected boolean needsToUpdate(DrawContext dc) {
        if (this.lastEyePoint == null)
            return true;

        View view = dc.getView();
        double altitudeAboveGround = computeAltitudeAboveGround(dc);
        if (view.getEyePoint().distanceTo3(this.lastEyePoint) > altitudeAboveGround / 100)  // 1% of AAG
            return true;

        if (this.lastVerticalExaggeration != dc.getVerticalExaggeration())
            return true;

        if (Math.abs(this.lastViewHeading - view.getHeading().degrees) > 1)
            return true;
        if (Math.abs(this.lastViewPitch - view.getPitch().degrees) > 1)
            return true;

        if (Math.abs(this.lastViewFOV - view.getFieldOfView().degrees) > 1)
            return true;

        // We must test the globe and its projection to see if either changed. We can't simply use the globe state
        // key for this because we don't want a 2D globe offset change to cause an update. Offset changes don't
        // invalidate the current set of renderables.

        if (dc.getGlobe() != this.globe)
            return true;

        if (dc.is2DGlobe()) {
            if (((Globe2D) dc.getGlobe()).getProjection() != this.lastProjection)
                return true;
        }

        return false;
    }

    protected void clear(DrawContext dc) {
        this.removeAllRenderables();
        this.terrainConformance = computeTerrainConformance(dc);
        this.globe = dc.getGlobe();
        this.lastEyePoint = dc.getView().getEyePoint();
        this.lastViewFOV = dc.getView().getFieldOfView().degrees;
        this.lastViewHeading = dc.getView().getHeading().degrees;
        this.lastViewPitch = dc.getView().getPitch().degrees;
        this.lastVerticalExaggeration = dc.getVerticalExaggeration();

        if (dc.is2DGlobe())
            this.lastProjection = ((Globe2D) dc.getGlobe()).getProjection();
    }

    protected static double computeTerrainConformance(DrawContext dc) {
        int value = 100;
        double alt = dc.getView().getEyePosition().getElevation();
        if (alt < 10.0e3)
            value = 20;
        else if (alt < 50.0e3)
            value = 30;
        else if (alt < 100.0e3)
            value = 40;
        else if (alt < 1000.0e3)
            value = 60;

        return value;
    }

    protected static LatLon computeLabelOffset(DrawContext dc) {
        LatLon labelPos;
        // Compute labels offset from view center
        if (dc.getView() instanceof OrbitView) {
            OrbitView view = (OrbitView) dc.getView();
            Position centerPos = view.getCenterPosition();
            double pixelSizeDegrees = Angle.fromRadians(view.computePixelSizeAtDistance(view.getZoom())
                / dc.getGlobe().getEquatorialRadius()).degrees;
            double labelOffsetDegrees = pixelSizeDegrees * view.getViewport().getWidth() / 4;
            labelPos = LatLon.fromDegrees(centerPos.getLatitude().degrees - labelOffsetDegrees,
                centerPos.getLongitude().degrees - labelOffsetDegrees);
            double labelLatDegrees = labelPos.getLatitude().latNorm().degrees;
            labelLatDegrees = Math.min(Math.max(labelLatDegrees, -70), 70);
            labelPos = new LatLon(Angle.fromDegrees(labelLatDegrees), labelPos.getLongitude().lonNorm());
        }
        else
            labelPos = dc.getView().getEyePosition(); // fall back if no orbit view

        return labelPos;
    }

    protected static Object createLineRenderable(Iterable<? extends Position> positions, String pathType) {
        Path path = new Path(positions);
        path.setPathType(pathType);
        path.setSurfacePath(true);
        path.setTerrainConformance(1);
        return path;
    }

    protected static Vec4 getSurfacePoint(DrawContext dc, Angle latitude, Angle longitude) {
        Vec4 surfacePoint = dc.getSurfaceGeometry().getSurfacePoint(latitude, longitude);
        if (surfacePoint == null)
            surfacePoint = dc.getGlobe().computePointFromPosition(new Position(latitude, longitude,
                dc.getGlobe().getElevation(latitude, longitude)));

        return surfacePoint;
    }

    // === Support methods ===

    protected static double computeAltitudeAboveGround(DrawContext dc) {
        View view = dc.getView();
        Position eyePosition = view.getEyePosition();
        Vec4 surfacePoint = getSurfacePoint(dc, eyePosition.getLatitude(), eyePosition.getLongitude());

        return view.getEyePoint().distanceTo3(surfacePoint);
    }

    protected static void computeTruncatedSegment(Position p1, Position p2, Sector sector,
        Collection<Position> positions) {
        if (p1 == null || p2 == null)
            return;

        boolean p1In = sector.contains(p1);
        boolean p2In = sector.contains(p2);
        if (!p1In && !p2In) {
            // whole segment is (likely) outside
            return;
        }
        if (p1In && p2In) {
            // whole segment is (likely) inside
            positions.add(p1);
            positions.add(p2);
        }
        else {
            // segment does cross the boundary
            Position outPoint = !p1In ? p1 : p2;
            Position inPoint = p1In ? p1 : p2;
            for (int i = 1; i <= 2; i++)  // there may be two intersections
            {
                LatLon intersection = null;
                if (outPoint.getLongitude().degrees > sector.lonMax
                    || (sector.lonMax == 180 && outPoint.getLongitude().degrees < 0)) {
                    // intersect with east meridian
                    intersection = greatCircleIntersectionAtLongitude(
                        inPoint, outPoint, sector.lonMax());
                }
                else if (outPoint.getLongitude().degrees < sector.lonMin
                    || (sector.lonMin == -180 && outPoint.getLongitude().degrees > 0)) {
                    // intersect with west meridian
                    intersection = greatCircleIntersectionAtLongitude(
                        inPoint, outPoint, sector.lonMin());
                }
                else if (outPoint.getLatitude().degrees > sector.latMax) {
                    // intersect with top parallel
                    intersection = greatCircleIntersectionAtLatitude(
                        inPoint, outPoint, sector.latMax());
                }
                else if (outPoint.getLatitude().degrees < sector.latMin) {
                    // intersect with bottom parallel
                    intersection = greatCircleIntersectionAtLatitude(
                        inPoint, outPoint, sector.latMin());
                }
                if (intersection != null)
                    outPoint = new Position(intersection, outPoint.getElevation());
                else
                    break;
            }
            positions.add(inPoint);
            positions.add(outPoint);
        }
    }

    /**
     * Computes the intersection point position between a great circle segment and a meridian.
     *
     * @param p1        the great circle segment start position.
     * @param p2        the great circle segment end position.
     * @param longitude the meridian longitude <code>Angle</code>
     * @return the intersection <code>Position</code> or null if there was no intersection found.
     */
    protected static LatLon greatCircleIntersectionAtLongitude(LatLon p1, LatLon p2, Angle longitude) {
        if (p1.getLongitude().degrees == longitude.degrees)
            return p1;
        if (p2.getLongitude().degrees == longitude.degrees)
            return p2;
        LatLon pos = null;
        double deltaLon = getDeltaLongitude(p1, p2.getLongitude()).degrees;
        if (getDeltaLongitude(p1, longitude).degrees < deltaLon
            && getDeltaLongitude(p2, longitude).degrees < deltaLon) {
            int count = 0;
            double precision = 1.0d / 6378137.0d; // 1m angle in radians
            LatLon a = p1;
            LatLon b = p2;
            LatLon midPoint = greatCircleMidPoint(a, b);
            while (getDeltaLongitude(midPoint, longitude).radians > precision && count <= 20) {
                count++;
                if (getDeltaLongitude(a, longitude).degrees < getDeltaLongitude(b, longitude).degrees)
                    b = midPoint;
                else
                    a = midPoint;
                midPoint = greatCircleMidPoint(a, b);
            }
            pos = midPoint;
        }
        // Adjust final longitude for an exact match
        if (pos != null)
            pos = new LatLon(pos.getLatitude(), longitude);
        return pos;
    }

    /**
     * Computes the intersection point position between a great circle segment and a parallel.
     *
     * @param p1       the great circle segment start position.
     * @param p2       the great circle segment end position.
     * @param latitude the parallel latitude <code>Angle</code>
     * @return the intersection <code>Position</code> or null if there was no intersection found.
     */
    protected static LatLon greatCircleIntersectionAtLatitude(LatLon p1, LatLon p2, Angle latitude) {
        LatLon pos = null;
        if (Math.signum(p1.getLatitude().degrees - latitude.degrees)
            != Math.signum(p2.getLatitude().degrees - latitude.degrees)) {
            int count = 0;
            double precision = 1.0d / 6378137.0d; // 1m angle in radians
            LatLon a = p1;
            LatLon b = p2;
            LatLon midPoint = greatCircleMidPoint(a, b);
            while (Math.abs(midPoint.getLatitude().radians - latitude.radians) > precision && count <= 20) {
                count++;
                if (Math.signum(a.getLatitude().degrees - latitude.degrees)
                    != Math.signum(midPoint.getLatitude().degrees - latitude.degrees))
                    b = midPoint;
                else
                    a = midPoint;
                midPoint = greatCircleMidPoint(a, b);
            }
            pos = midPoint;
        }
        // Adjust final latitude for an exact match
        if (pos != null)
            pos = new LatLon(latitude, pos.getLongitude());
        return pos;
    }

    protected static LatLon greatCircleMidPoint(LatLon p1, LatLon p2) {
        Angle azimuth = LatLon.greatCircleAzimuth(p1, p2);
        Angle distance = LatLon.greatCircleDistance(p1, p2);
        return LatLon.greatCircleEndPosition(p1, azimuth.radians, distance.radians / 2);
    }

    protected static Angle getDeltaLongitude(LatLon p1, Angle longitude) {
        double deltaLon = Math.abs(p1.getLongitude().degrees - longitude.degrees);
        return Angle.fromDegrees(deltaLon < 180 ? deltaLon : 360 - deltaLon);
    }

    protected static class GridElement {
        public final static String TYPE_LINE = "GridElement_Line";
        public final static String TYPE_LINE_NORTH = "GridElement_LineNorth";
        public final static String TYPE_LINE_SOUTH = "GridElement_LineSouth";
        public final static String TYPE_LINE_WEST = "GridElement_LineWest";
        public final static String TYPE_LINE_EAST = "GridElement_LineEast";
        public final static String TYPE_LINE_NORTHING = "GridElement_LineNorthing";
        public final static String TYPE_LINE_EASTING = "GridElement_LineEasting";
        public final static String TYPE_GRIDZONE_LABEL = "GridElement_GridZoneLabel";
        public final static String TYPE_LONGITUDE_LABEL = "GridElement_LongitudeLabel";
        public final static String TYPE_LATITUDE_LABEL = "GridElement_LatitudeLabel";

        public final Sector sector;
        public final Object renderable;
        public final String type;
        public double value;

        public GridElement(Sector sector, Object renderable, String type) {
            if (sector == null) {
                String message = Logging.getMessage("nullValue.SectorIsNull");
                Logging.logger().severe(message);
                throw new IllegalArgumentException(message);
            }
            if (renderable == null) {
                String message = Logging.getMessage("nullValue.ObjectIsNull");
                Logging.logger().severe(message);
                throw new IllegalArgumentException(message);
            }
            if (type == null) {
                String message = Logging.getMessage("nullValue.StringIsNull");
                Logging.logger().severe(message);
                throw new IllegalArgumentException(message);
            }
            this.sector = sector;
            this.renderable = renderable;
            this.type = type;
        }

        public void setValue(double value) {
            this.value = value;
        }

        public boolean isInView(DrawContext dc) {
            if (dc == null) {
                String message = Logging.getMessage("nullValue.DrawContextIsNull");
                Logging.logger().severe(message);
                throw new IllegalArgumentException(message);
            }
            return isInView(dc, dc.getVisibleSector());
        }

        @SuppressWarnings("RedundantIfStatement")
        public boolean isInView(DrawContext dc, Sector vs) {
            if (dc == null) {
                String message = Logging.getMessage("nullValue.DrawContextIsNull");
                Logging.logger().severe(message);
                throw new IllegalArgumentException(message);
            }
            if (vs == null) {
                String message = Logging.getMessage("nullValue.SectorIsNull");
                Logging.logger().severe(message);
                throw new IllegalArgumentException(message);
            }
            if (!this.sector.intersects(vs))
                return false;

            return true;
        }
    }

    /**
     * @author dcollins
     * @version $Id: GraticuleSupport.java 2372 2014-10-10 18:32:15Z tgaskins $
     */
    public static class GraticuleSupport {
        private final Collection<Pair> renderables = new HashSet<>(); // a set to avoid duplicates in multi-pass (2D globes)
        private final Map<String, GraticuleRenderingParams> namedParams = new HashMap<>();
        private final Map<String, ShapeAttributes> namedShapeAttributes = new HashMap<>();
        private final GeographicTextRenderer textRenderer = new GeographicTextRenderer();
        private AVList defaultParams;

        public GraticuleSupport() {
            this.textRenderer.setEffect(AVKey.TEXT_EFFECT_SHADOW);
            // Keep labels separated by at least two pixels
            this.textRenderer.setCullTextEnabled(true);
            this.textRenderer.setCullTextMargin(1);
            // Shrink and blend labels as they get farther away from the eye
            this.textRenderer.setDistanceMinScale(0.5);
            this.textRenderer.setDistanceMinOpacity(0.5);
        }

        public void addRenderable(Object renderable, String paramsKey) {
            if (renderable == null) {
                String message = Logging.getMessage("nullValue.ObjectIsNull");
                Logging.logger().severe(message);
                throw new IllegalArgumentException(message);
            }

            this.renderables.add(new Pair(renderable, paramsKey));
        }

        public void removeAllRenderables() {
            this.renderables.clear();
        }

        public void render(DrawContext dc) {
            this.render(dc, 1);
        }

        public void render(DrawContext dc, double opacity) {
            if (dc == null) {
                String message = Logging.getMessage("nullValue.DrawContextIsNull");
                Logging.logger().severe(message);
                throw new IllegalArgumentException(message);
            }

            this.namedShapeAttributes.clear();

            // Render lines and collect text labels
            Collection<GeographicText> text = new ArrayList<>();
            for (Pair pair : this.renderables) {
                Object renderable = pair.a;
                String paramsKey = (pair.b instanceof String) ? (String) pair.b : null;
                GraticuleRenderingParams renderingParams = paramsKey != null ? this.namedParams.get(paramsKey) : null;

                if (renderable instanceof Path) {
                    if (renderingParams == null || renderingParams.isDrawLines()) {
                        applyRenderingParams(paramsKey, renderingParams, (Attributable) renderable, opacity);
                        ((Renderable) renderable).render(dc);
                    }
                }
                else if (renderable instanceof GeographicText) {
                    if (renderingParams == null || renderingParams.isDrawLabels()) {
                        applyRenderingParams(renderingParams, (GeographicText) renderable, opacity);
                        text.add((GeographicText) renderable);
                    }
                }
            }

            // Render text labels
            this.textRenderer.render(dc, text);
        }

        public GraticuleRenderingParams getRenderingParams(String key) {
            if (key == null) {
                String message = Logging.getMessage("nullValue.KeyIsNull");
                Logging.logger().severe(message);
                throw new IllegalArgumentException(message);
            }

            GraticuleRenderingParams value = this.namedParams.get(key);
            if (value == null) {
                value = new GraticuleRenderingParams();
                initRenderingParams(value);
                if (this.defaultParams != null)
                    value.setValues(this.defaultParams);

                this.namedParams.put(key, value);
            }

            return value;
        }

        public Collection<Map.Entry<String, GraticuleRenderingParams>> getAllRenderingParams() {
            return this.namedParams.entrySet();
        }

        public void setRenderingParams(String key, GraticuleRenderingParams renderingParams) {
            if (key == null) {
                String message = Logging.getMessage("nullValue.KeyIsNull");
                Logging.logger().severe(message);
                throw new IllegalArgumentException(message);
            }

            initRenderingParams(renderingParams);
            this.namedParams.put(key, renderingParams);
        }

        public AVList getDefaultParams() {
            return this.defaultParams;
        }

        public void setDefaultParams(AVList defaultParams) {
            this.defaultParams = defaultParams;
        }

        private static AVList initRenderingParams(AVList params) {
            if (params == null) {
                String message = Logging.getMessage("nullValue.AVListIsNull");
                Logging.logger().severe(message);
                throw new IllegalArgumentException(message);
            }

            if (params.get(GraticuleRenderingParams.KEY_DRAW_LINES) == null)
                params.set(GraticuleRenderingParams.KEY_DRAW_LINES, Boolean.TRUE);

            if (params.get(GraticuleRenderingParams.KEY_LINE_COLOR) == null)
                params.set(GraticuleRenderingParams.KEY_LINE_COLOR, Color.WHITE);

            if (params.get(GraticuleRenderingParams.KEY_LINE_WIDTH) == null)
                //noinspection UnnecessaryBoxing
                params.set(GraticuleRenderingParams.KEY_LINE_WIDTH, Double.valueOf(1));

            if (params.get(GraticuleRenderingParams.KEY_LINE_STYLE) == null)
                params.set(GraticuleRenderingParams.KEY_LINE_STYLE, GraticuleRenderingParams.VALUE_LINE_STYLE_SOLID);

            if (params.get(GraticuleRenderingParams.KEY_DRAW_LABELS) == null)
                params.set(GraticuleRenderingParams.KEY_DRAW_LABELS, Boolean.TRUE);

            if (params.get(GraticuleRenderingParams.KEY_LABEL_COLOR) == null)
                params.set(GraticuleRenderingParams.KEY_LABEL_COLOR, Color.WHITE);

            if (params.get(GraticuleRenderingParams.KEY_LABEL_FONT) == null)
                params.set(GraticuleRenderingParams.KEY_LABEL_FONT, Font.decode("Arial-Bold-12"));

            return params;
        }

        private static void applyRenderingParams(AVList params, GeographicText text, double opacity) {
            if (params != null && text != null) {
                // Apply "label" properties to the GeographicText.
                Object o = params.get(GraticuleRenderingParams.KEY_LABEL_COLOR);
                if (o instanceof Color) {
                    Color color = applyOpacity((Color) o, opacity);
                    float[] compArray = new float[4];
                    Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), compArray);
                    int colorValue = compArray[2] < 0.5f ? 255 : 0;
                    text.setColor(color);
                    text.setBackgroundColor(new Color(colorValue, colorValue, colorValue, color.getAlpha()));
                }

                o = params.get(GraticuleRenderingParams.KEY_LABEL_FONT);
                if (o instanceof Font) {
                    text.setFont((Font) o);
                }
            }
        }

        private void applyRenderingParams(String key, AVList params, Attributable path, double opacity) {
            if (key != null && params != null && path != null) {
                path.setAttributes(this.getLineShapeAttributes(key, params, opacity));
            }
        }

        private ShapeAttributes getLineShapeAttributes(String key, AVList params, double opacity) {
            ShapeAttributes attrs = this.namedShapeAttributes.get(key);
            if (attrs == null) {
                attrs = createLineShapeAttributes(params, opacity);
                this.namedShapeAttributes.put(key, attrs);
            }
            return attrs;
        }

        private static ShapeAttributes createLineShapeAttributes(AVList params, double opacity) {
            ShapeAttributes attrs = new BasicShapeAttributes();
            attrs.setDrawInterior(false);
            attrs.setDrawOutline(true);
            if (params != null) {
                // Apply "line" properties.
                Object o = params.get(GraticuleRenderingParams.KEY_LINE_COLOR);
                if (o instanceof Color) {
                    attrs.setOutlineMaterial(new Material(applyOpacity((Color) o, opacity)));
                    attrs.setOutlineOpacity(opacity);
                }

                Double lineWidth = getDoubleValue(params, GraticuleRenderingParams.KEY_LINE_WIDTH);
                if (lineWidth != null) {
                    attrs.setOutlineWidth(lineWidth);
                }

                String s = params.getStringValue(GraticuleRenderingParams.KEY_LINE_STYLE);
                // Draw a solid line.
                if (GraticuleRenderingParams.VALUE_LINE_STYLE_SOLID.equalsIgnoreCase(s)) {
                    attrs.setOutlineStipplePattern((short) 0xAAAA);
                    attrs.setOutlineStippleFactor(0);
                }
                // Draw the line as longer strokes with space in between.
                else if (GraticuleRenderingParams.VALUE_LINE_STYLE_DASHED.equalsIgnoreCase(s)) {
                    int baseFactor = (int) (lineWidth != null ? Math.round(lineWidth) : 1.0);
                    attrs.setOutlineStipplePattern((short) 0xAAAA);
                    attrs.setOutlineStippleFactor(3 * baseFactor);
                }
                // Draw the line as a evenly spaced "square" dots.
                else if (GraticuleRenderingParams.VALUE_LINE_STYLE_DOTTED.equalsIgnoreCase(s)) {
                    int baseFactor = (int) (lineWidth != null ? Math.round(lineWidth) : 1.0);
                    attrs.setOutlineStipplePattern((short) 0xAAAA);
                    attrs.setOutlineStippleFactor(baseFactor);
                }
            }
            return attrs;
        }

        private static Color applyOpacity(Color color, double opacity) {
            if (opacity >= 1)
                return color;

            float[] compArray = color.getRGBComponents(null);
            return new Color(compArray[0], compArray[1], compArray[2], compArray[3] * (float) opacity);
        }

        private static class Pair {
            final Object a;
            final Object b;

            Pair(Object a, Object b) {
                this.a = a;
                this.b = b;
            }

            @Override
            public boolean equals(Object o) {
                if (this == o)
                    return true;
                if (o == null || getClass() != o.getClass())
                    return false;

                Pair pair = (Pair) o;

                if (!Objects.equals(a, pair.a))
                    return false;
                return Objects.equals(b, pair.b);
            }

            @Override
            public int hashCode() {
                int result = a != null ? a.hashCode() : 0;
                result = 31 * result + (b != null ? b.hashCode() : 0);
                return result;
            }
        }
    }

    /**
     * @author dcollins
     * @version $Id: GraticuleRenderingParams.java 1171 2013-02-11 21:45:02Z dcollins $
     */
    public static class GraticuleRenderingParams extends AVListImpl {
        public static final String KEY_DRAW_LINES = "DrawGraticule";
        public static final String KEY_LINE_COLOR = "GraticuleLineColor";
        public static final String KEY_LINE_WIDTH = "GraticuleLineWidth";
        public static final String KEY_LINE_STYLE = "GraticuleLineStyle";
        public static final String KEY_LINE_CONFORMANCE = "GraticuleLineConformance";
        public static final String KEY_DRAW_LABELS = "DrawLabels";
        public static final String KEY_LABEL_COLOR = "LabelColor";
        public static final String KEY_LABEL_FONT = "LabelFont";
        public static final String VALUE_LINE_STYLE_SOLID = "LineStyleSolid";
        public static final String VALUE_LINE_STYLE_DASHED = "LineStyleDashed";
        public static final String VALUE_LINE_STYLE_DOTTED = "LineStyleDotted";

        public GraticuleRenderingParams() {
        }

        public boolean isDrawLines() {
            Object value = get(KEY_DRAW_LINES);
            return value instanceof Boolean ? (Boolean) value : false;
        }

        public void setDrawLines(boolean drawLines) {
            set(KEY_DRAW_LINES, drawLines);
        }

        public Color getLineColor() {
            Object value = get(KEY_LINE_COLOR);
            return value instanceof Color ? (Color) value : null;
        }

        public void setLineColor(Color color) {
            if (color == null) {
                String message = Logging.getMessage("nullValue.ColorIsNull");
                Logging.logger().severe(message);
                throw new IllegalArgumentException(message);
            }

            set(KEY_LINE_COLOR, color);
        }

        public double getLineWidth() {

            Object value = get(KEY_LINE_WIDTH);
            return value instanceof Double ? (Double) value : 0;
        }

        public void setLineWidth(double lineWidth) {
            set(KEY_LINE_WIDTH, lineWidth);
        }

        public String getLineStyle() {
            Object value = get(KEY_LINE_STYLE);
            return value instanceof String ? (String) value : null;
        }

        public void setLineStyle(String lineStyle) {
            if (lineStyle == null) {
                String message = Logging.getMessage("nullValue.StringIsNull");
                Logging.logger().severe(message);
                throw new IllegalArgumentException(message);
            }

            set(KEY_LINE_STYLE, lineStyle);
        }

        public boolean isDrawLabels() {
            Object value = get(KEY_DRAW_LABELS);
            return value instanceof Boolean ? (Boolean) value : false;
        }

        public void setDrawLabels(boolean drawLabels) {
            set(KEY_DRAW_LABELS, drawLabels);
        }

        public Color getLabelColor() {
            Object value = get(KEY_LABEL_COLOR);
            return value instanceof Color ? (Color) value : null;
        }

        public void setLabelColor(Color color) {
            if (color == null) {
                String message = Logging.getMessage("nullValue.ColorIsNull");
                Logging.logger().severe(message);
                throw new IllegalArgumentException(message);
            }

            set(KEY_LABEL_COLOR, color);
        }

        public Font getLabelFont() {
            Object value = get(KEY_LABEL_FONT);
            return value instanceof Font ? (Font) value : null;
        }

        public void setLabelFont(Font font) {
            if (font == null) {
                String message = Logging.getMessage("nullValue.FontIsNull");
                Logging.logger().severe(message);
                throw new IllegalArgumentException(message);
            }

            set(KEY_LABEL_FONT, font);
        }
    }
}
