/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.util.measure;

import gov.nasa.worldwind.*;
import gov.nasa.worldwind.avlist.*;
import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.globes.Globe;
import gov.nasa.worldwind.layers.RenderableLayer;
import gov.nasa.worldwind.render.*;
import gov.nasa.worldwind.util.*;

import java.awt.*;
import java.awt.geom.*;
import java.util.*;

/**
 * A utility class to interactively draw shapes and measure distance and area across the terrain. When armed, the class
 * monitors mouse events to allow the definition of a measure shape that can be one of {@link #SHAPE_LINE}, {@link
 * #SHAPE_PATH}, {@link #SHAPE_POLYGON}, {@link #SHAPE_CIRCLE}, {@link #SHAPE_ELLIPSE}, {@link #SHAPE_SQUARE} or {@link
 * #SHAPE_QUAD}.
 * <p>
 * In order to allow user interaction with the measuring shape, a controller must be set by calling {@link
 * #setController(MeasureToolController)} with a new instance of a <code>MeasureToolController</code>.</p>
 * <p>
 * The interaction sequence for drawing a shape and measuring is as follows: </p>  <ul> <li>Set the measure shape.</li>
 * <li>Arm the <code>MeasureTool</code> object by calling its {@link #setArmed(boolean)} method with an argument of
 * true.</li> <li>Click on the terrain to add points.</li> <li>Disarm the <code>MeasureTool</code> object by calling its
 * {@link #setArmed(boolean)} method with an argument of false. </li> <li>Read the measured length or area by calling
 * the <code>MeasureTool</code> {@link #getLength()} or {@link #getArea()} method. Note that the length and area can be
 * queried at any time during or after the process.</li> </ul><p> While entering points or after the measure tool has
 * been disarmed, dragging the control points allow to change the initial points positions and alter the measure
 * shape.</p>
 * <p>
 * While the <code>MeasureTool</code> is armed, pressing and immediately releasing mouse button one while also pressing
 * the control key (Ctl) removes the last point entered. Once the <code>MeasureTool</code> is disarmed, a measure shape
 * of type SHAPE_POLYGON can be moved by dragging a control point while pressing the alt/option key.</p>
 * <p>
 * Arming and disarming the <code>MeasureTool</code> does not change the contents or attributes of the measure tool's
 * layer. Note that the measure tool will NOT disarm itself after the second point of a line or a regular shape has been
 * entered - the MeasureToolController has that responsibility.</p>
 * <p>
 * <b>Setting the measure shape from the application</b></p>
 * <p>
 * The application can set the measure shape to an arbitrary list of positions using {@link #setPositions(ArrayList)}.
 * If the provided list contains two positions, the measure shape will be set to {@link #SHAPE_LINE}. If more then two
 * positions are provided, the measure shape will be set to {@link #SHAPE_PATH} if the last position differs from the
 * first (open path), or {@link #SHAPE_POLYGON} if the path is closed.</p>
 * <p>
 * The application can also set the measure shape to a predefined regular shape by calling {@link
 * #setMeasureShapeType(String, Position, double, double, Angle)}, providing a shape type (one of {@link #SHAPE_CIRCLE},
 * {@link #SHAPE_ELLIPSE}, {@link #SHAPE_SQUARE} or {@link #SHAPE_QUAD}), a center position, a width, a height (in
 * meters) and a heading angle.</p>
 * <p>
 * Finally, the application can use an existing <code>Path</code> or <code>SurfaceShape</code> by using {@link
 * #setMeasureShape(Path)} or {@link #setMeasureShape(SurfaceShape)}. The surface shape can be one of
 * <code>SurfacePolyline</code>,
 * <code>SurfacePolygon</code>, <code>SurfaceQuad</code>, <code>SurfaceSquare</code>, <code>SurfaceEllipse</code> or
 * <code>SurfaceCircle</code>.
 * <p>
 * <b>Measuring</b></p>
 * <p>
 * The application can read the measured length or area by calling the <code>MeasureTool</code> {@link #getLength()} or
 * {@link #getArea()} method. These methods will return -1 when no value is available.</p>
 * <p>
 * Regular shapes are defined by a center position, a width a height and a heading angle. Those attributes can be
 * accessed by calling the {@link #getCenterPosition()}, {@link #getWidth()}, {@link #getHeight()} and {@link
 * #getOrientation()} methods.</p>
 * <p>
 * The measurements are displayed in units specified in the measure tool's {@link UnitsFormat} object. Access to the
 * units format is via the method {@link #getUnitsFormat()}.
 * <p>
 * <b>Events</b></p>
 * <p>
 * The <code>MeasureTool</code> will send events on several occasions: when the position list has changed - {@link
 * #EVENT_POSITION_ADD}, {@link #EVENT_POSITION_REMOVE} or {@link #EVENT_POSITION_REPLACE}, when metrics has changed
 * {@link #EVENT_METRIC_CHANGED} or when the tool is armed or disarmed {@link #EVENT_ARMED}.</p>
 * <p>
 * Events will also be fired at the start and end of a rubber band operation during shape creation: {@link
 * #EVENT_RUBBERBAND_START} and {@link #EVENT_RUBBERBAND_STOP}.</p>
 * <p>
 * See {@link gov.nasa.worldwind.examples.MeasureToolPanel} for some events usage.</p>
 * <p>
 * Several instances of this class can be used simultaneously. However, each instance should be disposed of after usage
 * by calling the {@link #dispose()} method.</p>
 *
 * @author Patrick Murris
 * @version $Id: MeasureTool.java 3297 2015-07-03 16:21:05Z dcollins $
 * @see MeasureToolController
 */
public class MeasureTool extends KVMap implements Disposable {

    public static final String SHAPE_LINE = "MeasureTool.ShapeLine";
    public static final String SHAPE_PATH = "MeasureTool.ShapePath";
    public static final String SHAPE_POLYGON = "MeasureTool.ShapePolygon";
    public static final String SHAPE_CIRCLE = "MeasureTool.ShapeCircle";
    public static final String SHAPE_ELLIPSE = "MeasureTool.ShapeEllipse";
    public static final String SHAPE_QUAD = "MeasureTool.ShapeQuad";
    public static final String SHAPE_SQUARE = "MeasureTool.ShapeSquare";

    public static final String EVENT_POSITION_ADD = "MeasureTool.AddPosition";
    public static final String EVENT_POSITION_REMOVE = "MeasureTool.RemovePosition";
    public static final String EVENT_POSITION_REPLACE = "MeasureTool.ReplacePosition";
    public static final String EVENT_METRIC_CHANGED = "MeasureTool.MetricChanged";
    public static final String EVENT_ARMED = "MeasureTool.Armed";
    public static final String EVENT_RUBBERBAND_START = "MeasureTool.RubberBandStart";
    public static final String EVENT_RUBBERBAND_STOP = "MeasureTool.RubberBandStop";

    public static final String ANGLE_LABEL = "MeasureTool.AngleLabel";
    public static final String AREA_LABEL = "MeasureTool.AreaLabel";
    public static final String LENGTH_LABEL = "MeasureTool.LengthLabel";
    public static final String PERIMETER_LABEL = "MeasureTool.PerimeterLabel";
    public static final String RADIUS_LABEL = "MeasureTool.RadiusLabel";
    public static final String HEIGHT_LABEL = "MeasureTool.HeightLabel";
    public static final String WIDTH_LABEL = "MeasureTool.WidthLabel";
    public static final String HEADING_LABEL = "MeasureTool.HeadingLabel";
    public static final String CENTER_LATITUDE_LABEL = "MeasureTool.CenterLatitudeLabel";
    public static final String CENTER_LONGITUDE_LABEL = "MeasureTool.CenterLongitudeLabel";
    public static final String LATITUDE_LABEL = "MeasureTool.LatitudeLabel";
    public static final String LONGITUDE_LABEL = "MeasureTool.LongitudeLabel";
    public static final String ACCUMULATED_LABEL = "MeasureTool.AccumulatedLabel";
    public static final String MAJOR_AXIS_LABEL = "MeasureTool.MajorAxisLabel";
    public static final String MINOR_AXIS_LABEL = "MeasureTool.MinorAxisLabel";

    public static final String CONTROL_TYPE_LOCATION_INDEX = "MeasureTool.ControlTypeLocationIndex";
    public static final String CONTROL_TYPE_REGULAR_SHAPE = "MeasureTool.ControlTypeRegularShape";
    public static final String CONTROL_TYPE_LEADER_ORIGIN = "MeasureTool.ControlTypeLeaderOrigin";
    protected static final double SHAPE_MIN_WIDTH_METERS = 0.1;
    protected static final double SHAPE_MIN_HEIGHT_METERS = 0.1;
    protected static final int MAX_SHAPE_MOVE_ITERATIONS = 10;
    protected static final double SHAPE_CONTROL_EPSILON_METERS = 0.01;
    private static final String CENTER = "Center";
    private static final String NORTH = "North";
    private static final String EAST = "East";
    private static final String SOUTH = "South";
    private static final String WEST = "West";
    private static final String NORTHEAST = "NE";
    private static final String SOUTHEAST = "SE";
    private static final String SOUTHWEST = "SW";
    private static final String NORTHWEST = "NW";
    private static final String NORTH_LEADER = "NorthLeader";
    protected final WorldWindow wwd;
    protected final ArrayList<Position> positions = new ArrayList<>();
    protected final ArrayList<Renderable> controlPoints = new ArrayList<>();
    protected final int shapeIntervals = 64;
    protected MeasureToolController controller;
    protected RenderableLayer applicationLayer;
    protected CustomRenderableLayer layer;
    protected CustomRenderableLayer controlPointsLayer;
    protected CustomRenderableLayer shapeLayer;
    protected Path line;
    protected SurfaceShape surfaceShape;
    protected ScreenAnnotation annotation;
    protected Color lineColor = Color.YELLOW;
    protected Color fillColor = new Color(0.6f, 0.6f, 0.4f, 0.5f);
    protected double lineWidth = 2;
    protected String pathType = Keys.GREAT_CIRCLE;
    protected AnnotationAttributes controlPointsAttributes;
    protected AnnotationAttributes controlPointWithLeaderAttributes;
    protected ShapeAttributes leaderAttributes;
    protected AnnotationAttributes annotationAttributes;
    protected String measureShapeType = MeasureTool.SHAPE_LINE;
    protected boolean followTerrain;
    protected boolean showControlPoints = true;
    protected boolean showAnnotation = true;
    protected UnitsFormat unitsFormat = new UnitsFormat();
    // Rectangle enclosed regular shapes attributes
    protected Rectangle2D.Double shapeRectangle;
    protected Position shapeCenterPosition;
    protected Angle shapeOrientation;

    /**
     * Construct a new measure tool drawing events from the specified <code>WorldWindow</code>.
     *
     * @param wwd the <code>WorldWindow</code> to draw events from.
     */
    public MeasureTool(final WorldWindow wwd) {
        this(wwd, null);
    }

    /**
     * Construct a new measure tool drawing events from the specified <code>WorldWindow</code> and using the given
     * <code>RenderableLayer</code>.
     *
     * @param wwd              the <code>WorldWindow</code> to draw events from.
     * @param applicationLayer the <code>RenderableLayer</code> to use. May be null. If specified, the caller is
     *                         responsible for adding the layer to the model and enabling it.
     */
    public MeasureTool(final WorldWindow wwd, RenderableLayer applicationLayer) {
        if (wwd == null) {
            String msg = Logging.getMessage("nullValue.WorldWindow");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }
        this.wwd = wwd;
        this.applicationLayer = applicationLayer; // can be null

        // Set up layers
        this.layer = new CustomRenderableLayer();
        this.shapeLayer = new CustomRenderableLayer();
        this.controlPointsLayer = new CustomRenderableLayer() {
            protected Iterable<Renderable> active() {
                return controlPoints;
            }
        };

        this.shapeLayer.setPickEnabled(false);
        this.layer.setName("Measure Tool");
        this.layer.add(this.shapeLayer);          // add shape layer to render layer
        this.layer.add(this.controlPointsLayer);  // add control points layer to render layer
        this.controlPointsLayer.setEnabled(this.showControlPoints);
        if (this.applicationLayer != null) {
            this.applicationLayer.add(this.layer);    // add render layer to the application provided layer
        } else {
            this.wwd.model().getLayers().add(this.layer);    // add render layer to the globe model
        }
        // Init control points rendering attributes
        this.controlPointsAttributes = new AnnotationAttributes();
        // Define an 8x8 square centered on the screen point
        this.controlPointsAttributes.setFrameShape(Keys.SHAPE_RECTANGLE);
        this.controlPointsAttributes.setLeader(Keys.SHAPE_NONE);
        this.controlPointsAttributes.setAdjustWidthToText(Keys.SIZE_FIXED);
        this.controlPointsAttributes.setSize(new Dimension(8, 8));
        this.controlPointsAttributes.setDrawOffset(new Point(0, -4));
        this.controlPointsAttributes.setInsets(new Insets(0, 0, 0, 0));
        this.controlPointsAttributes.setBorderWidth(0);
        this.controlPointsAttributes.setCornerRadius(0);
        this.controlPointsAttributes.setBackgroundColor(Color.BLUE);    // Normal color
        this.controlPointsAttributes.setTextColor(Color.GREEN);         // Highlighted color
        this.controlPointsAttributes.setHighlightScale(1.2);
        this.controlPointsAttributes.setDistanceMaxScale(1);            // No distance scaling
        this.controlPointsAttributes.setDistanceMinScale(1);
        this.controlPointsAttributes.setDistanceMinOpacity(1);

        // Init control point with leader rendering attributes.
        this.controlPointWithLeaderAttributes = new AnnotationAttributes();
        this.controlPointWithLeaderAttributes.setDefaults(this.controlPointsAttributes);
        this.controlPointWithLeaderAttributes.setFrameShape(Keys.SHAPE_ELLIPSE);
        this.controlPointWithLeaderAttributes.setSize(new Dimension(10, 10));
        this.controlPointWithLeaderAttributes.setDrawOffset(new Point(0, -5));
        this.controlPointWithLeaderAttributes.setBackgroundColor(Color.LIGHT_GRAY);

        this.leaderAttributes = new BasicShapeAttributes();
        this.leaderAttributes.setOutlineMaterial(Material.WHITE);
        this.leaderAttributes.setOutlineOpacity(0.7);
        this.leaderAttributes.setOutlineWidth(3);

        // Annotation attributes
        this.setInitialLabels();
        this.annotationAttributes = new AnnotationAttributes();
        this.annotationAttributes.setFrameShape(Keys.SHAPE_NONE);
        this.annotationAttributes.setInsets(new Insets(0, 0, 0, 0));
        this.annotationAttributes.setDrawOffset(new Point(0, 10));
        this.annotationAttributes.setTextAlign(Keys.CENTER);
        this.annotationAttributes.setEffect(Keys.TEXT_EFFECT_OUTLINE);
        this.annotationAttributes.setFont(Font.decode("Arial-Bold-14"));
        this.annotationAttributes.setTextColor(Color.WHITE);
        this.annotationAttributes.setBackgroundColor(Color.BLACK);
        this.annotationAttributes.setSize(new Dimension(220, 0));
        this.annotation = new ScreenAnnotation("", new Point(0, 0), this.annotationAttributes);
        this.annotation.getAttributes().setVisible(false);
        this.annotation.getAttributes().setDrawOffset(null); // use defaults
        this.shapeLayer.add(this.annotation);
    }

    protected static Angle computeNormalizedHeading(Angle heading) {
        double a = heading.degrees % 360;
        double degrees = a > 360 ? a - 360 : a < 0 ? 360 + a : a;
        return new Angle(degrees);
    }

    protected static String getPathType(Collection<? extends Position> positions) {
        return positions.size() > 2 ? MeasureTool.SHAPE_PATH : MeasureTool.SHAPE_LINE;
    }

    protected static boolean isRegularShape(String shape) {
        return (shape.equals(MeasureTool.SHAPE_CIRCLE)
            || shape.equals(MeasureTool.SHAPE_ELLIPSE)
            || shape.equals(MeasureTool.SHAPE_QUAD)
            || shape.equals(MeasureTool.SHAPE_SQUARE));
    }

    public static boolean isCenterControl(KV controlPoint) {
        String control = controlPoint.getStringValue(MeasureTool.CONTROL_TYPE_REGULAR_SHAPE);
        return control != null && control.equals(MeasureTool.CENTER);
    }

    public static boolean isSideControl(KV controlPoint) {
        String control = controlPoint.getStringValue(MeasureTool.CONTROL_TYPE_REGULAR_SHAPE);
        return control != null && (control.equals(MeasureTool.NORTH) || control.equals(MeasureTool.EAST)
            || control.equals(MeasureTool.SOUTH) || control.equals(MeasureTool.WEST));
    }

    public static boolean isCornerControl(KV controlPoint) {
        String control = controlPoint.getStringValue(MeasureTool.CONTROL_TYPE_REGULAR_SHAPE);
        return control != null && (control.equals(MeasureTool.NORTHEAST) || control.equals(MeasureTool.SOUTHEAST)
            || control.equals(MeasureTool.SOUTHWEST) || control.equals(MeasureTool.NORTHWEST));
    }

    protected static LatLon moveShapeByControlPoint(ControlPoint controlPoint, Globe globe, Angle heading,
        LatLon center,
        double width, double height) {
        double globeRadius = globe.getRadiusAt(center);

        String control = controlPoint.getStringValue(MeasureTool.CONTROL_TYPE_REGULAR_SHAPE);
        if (control == null) {
            return center;
        }

        LatLon newCenterLocation = center;

        // Iteratively move a location on the shape defined by heading, center, width, and height to the specified
        // control point's location. Because shapes are defined by an azimuth, center point, and real world dimensions,
        // we cannot assume that moving the shape's center point moves any of its corners or edges by exactly that
        // amount. However, the center and corners should move in a roughly similar manner, so we can iteratively move
        // a corner until it converges at the desired location.
        for (int i = 0; i < MeasureTool.MAX_SHAPE_MOVE_ITERATIONS; i++) {
            // Compute the control point's corresponding location on the shape.
            LatLon shapeControlLocation = MeasureTool.computeControlPointLocation(control, globe, heading,
                newCenterLocation,
                width, height);
            // Compute a great arc spanning the control point's location, and its corresponding location on the shape.
            Angle azimuth = LatLon.greatCircleAzimuth(shapeControlLocation, controlPoint.getPosition());
            Angle pathLength = LatLon.greatCircleDistance(shapeControlLocation, controlPoint.getPosition());

            // If the great circle distance between the control point's location and its corresponding location on the
            // shape is less than a predefined value, then we're done.
            double pathLengthMeters = pathLength.radians() * globeRadius;
            if (pathLengthMeters < MeasureTool.SHAPE_CONTROL_EPSILON_METERS) {
                break;
            }

            // Move the center to a new location on the great arc starting at the current center location, and with
            // azimuth and arc length equal to the arc spanning the corner location and the control point's location.
            newCenterLocation = LatLon.greatCircleEndPosition(newCenterLocation, azimuth, pathLength);
        }

        return newCenterLocation;
    }

    protected static LatLon computeControlPointLocation(String control, Globe globe, Angle heading, LatLon center,
        double width, double height) {
        Angle azimuth = MeasureTool.computeControlPointAzimuth(control, width, height);
        Angle pathLength = MeasureTool.computeControlPointPathLength(control, width, height, globe.getRadiusAt(center));

        if (control.equals(MeasureTool.CENTER)) {
            return center;
        } else if (azimuth != null && pathLength != null) {
            azimuth = azimuth.add(heading);
            return LatLon.greatCircleEndPosition(center, azimuth, pathLength);
        }

        return null;
    }

    @SuppressWarnings("SuspiciousNameCombination")
    protected static Angle computeControlPointAzimuth(String control, double width, double height) {
        Angle azimuth = null;

        switch (control) {
            case MeasureTool.NORTH:
            case MeasureTool.NORTH_LEADER:
                azimuth = Angle.ZERO;
                break;
            case MeasureTool.EAST:
                azimuth = Angle.POS90;
                break;
            case MeasureTool.SOUTH:
                azimuth = Angle.POS180;
                break;
            case MeasureTool.WEST:
                azimuth = new Angle(270);
                break;
            case MeasureTool.NORTHEAST:
                azimuth = Angle.fromRadians(Math.atan2(width, height));
                break;
            case MeasureTool.SOUTHEAST:
                azimuth = Angle.fromRadians(Math.atan2(width, -height));
                break;
            case MeasureTool.SOUTHWEST:
                azimuth = Angle.fromRadians(Math.atan2(-width, -height));
                break;
            case MeasureTool.NORTHWEST:
                azimuth = Angle.fromRadians(Math.atan2(-width, height));
                break;
            default:
                break;
        }

        return azimuth != null ? MeasureTool.computeNormalizedHeading(azimuth) : null;
    }

    protected static Angle computeControlPointPathLength(String control, double width, double height,
        double globeRadius) {
        Angle pathLength = null;

        switch (control) {
            case MeasureTool.NORTH:
            case MeasureTool.SOUTH:
                pathLength = Angle.fromRadians((height / 2.0d) / globeRadius);
                break;
            case MeasureTool.EAST:
            case MeasureTool.WEST:
                pathLength = Angle.fromRadians((width / 2.0d) / globeRadius);
                break;
            case MeasureTool.NORTHEAST:
            case MeasureTool.SOUTHEAST:
            case MeasureTool.SOUTHWEST:
            case MeasureTool.NORTHWEST:
                double diag = Math.sqrt((width * width) / 4.0d + (height * height) / 4.0d);
                pathLength = Angle.fromRadians(diag / globeRadius);
                break;
            case MeasureTool.NORTH_LEADER:
                pathLength = Angle.fromRadians(3.0d / 4.0d * height / globeRadius);
                break;
            default:
                break;
        }

        return pathLength;
    }

    protected static Angle computeAngleBetween(LatLon a, LatLon b, LatLon c) {
        Vec4 v0 = new Vec4(
            b.getLatitude().radians() - a.getLatitude().radians(),
            b.getLongitude().radians() - a.getLongitude().radians(), 0);

        Vec4 v1 = new Vec4(
            c.getLatitude().radians() - b.getLatitude().radians(),
            c.getLongitude().radians() - b.getLongitude().radians(), 0);

        return v0.angleBetween3(v1);
    }

    protected static boolean lengthsEssentiallyEqual(Double l1, Double l2) {
        return Math.abs(l1 - l2) / l1 < 0.001; // equal to within a milimeter
    }

    protected void setInitialLabels() {
        this.setLabel(MeasureTool.ACCUMULATED_LABEL, Logging.getMessage(MeasureTool.ACCUMULATED_LABEL));
        this.setLabel(MeasureTool.ANGLE_LABEL, Logging.getMessage(MeasureTool.ANGLE_LABEL));
        this.setLabel(MeasureTool.AREA_LABEL, Logging.getMessage(MeasureTool.AREA_LABEL));
        this.setLabel(MeasureTool.CENTER_LATITUDE_LABEL, Logging.getMessage(MeasureTool.CENTER_LATITUDE_LABEL));
        this.setLabel(MeasureTool.CENTER_LONGITUDE_LABEL, Logging.getMessage(MeasureTool.CENTER_LONGITUDE_LABEL));
        this.setLabel(MeasureTool.HEADING_LABEL, Logging.getMessage(MeasureTool.HEADING_LABEL));
        this.setLabel(MeasureTool.HEIGHT_LABEL, Logging.getMessage(MeasureTool.HEIGHT_LABEL));
        this.setLabel(MeasureTool.LATITUDE_LABEL, Logging.getMessage(MeasureTool.LATITUDE_LABEL));
        this.setLabel(MeasureTool.LONGITUDE_LABEL, Logging.getMessage(MeasureTool.LONGITUDE_LABEL));
        this.setLabel(MeasureTool.LENGTH_LABEL, Logging.getMessage(MeasureTool.LENGTH_LABEL));
        this.setLabel(MeasureTool.MAJOR_AXIS_LABEL, Logging.getMessage(MeasureTool.MAJOR_AXIS_LABEL));
        this.setLabel(MeasureTool.MINOR_AXIS_LABEL, Logging.getMessage(MeasureTool.MINOR_AXIS_LABEL));
        this.setLabel(MeasureTool.PERIMETER_LABEL, Logging.getMessage(MeasureTool.PERIMETER_LABEL));
        this.setLabel(MeasureTool.RADIUS_LABEL, Logging.getMessage(MeasureTool.RADIUS_LABEL));
        this.setLabel(MeasureTool.WIDTH_LABEL, Logging.getMessage(MeasureTool.WIDTH_LABEL));
    }

    public WorldWindow getWwd() {
        return this.wwd;
    }

    /**
     * Return the {@link UnitsFormat} instance governing the measurement value display units and format.
     *
     * @return the tool's units format instance.
     */
    public UnitsFormat getUnitsFormat() {
        return this.unitsFormat;
    }

    /**
     * Set the measure tool's @{link UnitsFormat} instance that governs measurement value display units and format.
     *
     * @param unitsFormat the units format instance.
     * @throws IllegalArgumentException if the units format instance is null.
     */
    public void setUnitsFormat(UnitsFormat unitsFormat) {
        if (unitsFormat == null) {
            String msg = Logging.getMessage("nullValue.Format");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        this.unitsFormat = unitsFormat;
    }

    public void setLabel(String labelName, String label) {
        if (labelName != null && !labelName.isEmpty()) {
            this.set(labelName, label);
        }
    }

    public String getLabel(String labelName) {
        if (labelName == null) {
            String msg = Logging.getMessage("nullValue.LabelName");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        String label = this.getStringValue(labelName);

        return label != null ? label : this.unitsFormat.getStringValue(labelName);
    }

    /**
     * Get the <code>MeasureToolController</code> for this measure tool.
     *
     * @return the <code>MeasureToolController</code> for this measure tool.
     */
    public MeasureToolController getController() {
        return this.controller;
    }

    /**
     * Set the controller object for this measure tool - can be null.
     *
     * @param controller the controller object for this measure tool.
     */
    public void setController(MeasureToolController controller) {
        if (this.controller != null) {
            this.wwd.input().removeMouseListener(this.controller);
            this.wwd.input().removeMouseMotionListener(this.controller);
            this.wwd.removePositionListener(this.controller);
            this.wwd.removeSelectListener(this.controller);
            this.wwd.removeRenderingListener(this.controller);
            this.controller = null;
        }
        if (controller != null) {
            this.controller = controller;
            this.controller.setMeasureTool(this);
            this.wwd.input().addMouseListener(this.controller);
            this.wwd.input().addMouseMotionListener(this.controller);
            this.wwd.addPositionListener(this.controller);
            this.wwd.addSelectListener(this.controller);
            this.wwd.addRenderingListener(this.controller);
        }
    }

    /**
     * Identifies whether the measure tool controller is armed.
     *
     * @return true if armed, false if not armed.
     */
    public boolean isArmed() {
        return this.controller != null && this.controller.isArmed();
    }

    /**
     * Arms and disarms the measure tool controller. When armed, the controller monitors user input and builds the shape
     * in response to user actions. When disarmed, the controller ignores all user input.
     *
     * @param state true to arm the controller, false to disarm it.
     */
    public void setArmed(boolean state) {
        if (this.controller != null) {
            this.controller.setArmed(state);
        }
    }

    /**
     * Returns the measure tool layer.
     *
     * @return the layer containing the measure shape and control points.
     */
    public RenderableLayer getLayer() {
        return this.layer;
    }

    /**
     * Returns the application layer passed to the constructor.
     *
     * @return the layer containing the measure shape and control points.
     */
    public RenderableLayer getApplicationLayer() {
        return applicationLayer;
    }

    /**
     * Returns the path currently used to display lines and path.
     *
     * @return the path currently used to display lines and path.
     */
    public Path getLine() {
        return this.line;
    }

    /**
     * Returns the surface shape currently used to display polygons.
     *
     * @return the surface shape currently used to display polygons.
     */
    public SurfaceShape getSurfaceShape() {
        return this.surfaceShape;
    }

    /**
     * Get the list of positions that define the current measure shape.
     *
     * @return the list of positions that define the current measure shape.
     */
    public ArrayList<? extends Position> getPositions() {
        return this.positions;
    }

    /**
     * Set the measure shape to an arbitrary list of positions. If the provided list contains two positions, the measure
     * shape will be set to {@link #SHAPE_LINE}. If more then two positions are provided, the measure shape will be set
     * to {@link #SHAPE_PATH} if the last position differs from the first (open path), or {@link #SHAPE_POLYGON} if the
     * path is closed.
     *
     * @param newPositions the shape position list.
     */
    public void setPositions(ArrayList<? extends Position> newPositions) {
        if (newPositions == null) {
            String msg = Logging.getMessage("nullValue.PositionsListIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        if (newPositions.size() < 2) {
            return;
        }

        this.clear();

        // Setup the proper measure shape
        boolean closedShape = newPositions.get(0).equals(newPositions.get(newPositions.size() - 1));
        if (newPositions.size() > 2 && closedShape) {
            setMeasureShapeType(MeasureTool.SHAPE_POLYGON);
        } else {
            setMeasureShapeType(MeasureTool.getPathType(newPositions));
        }

        // Import positions and create control points
        for (int i = 0; i < newPositions.size(); i++) {
            Position pos = newPositions.get(i);
            this.positions.add(pos);
            if (i < newPositions.size() - 1 || !closedShape) {
                addControlPoint(pos, MeasureTool.CONTROL_TYPE_LOCATION_INDEX, this.positions.size() - 1);
            }
        }

        // Update line heading if needed
        if (this.measureShapeType.equals(MeasureTool.SHAPE_LINE)) {
            this.shapeOrientation = LatLon.greatCircleAzimuth(this.positions.get(0), this.positions.get(1));
        }

        // Update screen shapes
        updateMeasureShape();
        this.firePropertyChange(MeasureTool.EVENT_POSITION_REPLACE, null, null);
        this.wwd.redraw();
    }

    /**
     * Get the list of control points associated with the current measure shape.
     *
     * @return the list of control points associated with the current measure shape.
     */
    public ArrayList<Renderable> getControlPoints() {
        return this.controlPoints;
    }

    /**
     * Get the attributes associated with the control points.
     *
     * @return the attributes associated with the control points.
     */
    public AnnotationAttributes getControlPointsAttributes() {
        return this.controlPointsAttributes;
    }

    /**
     * Get the attributes associated with the tool tip annotation.
     *
     * @return the attributes associated with the tool tip annotation.
     */
    public AnnotationAttributes getAnnotationAttributes() {
        return this.annotationAttributes;
    }

    public Color getLineColor() {
        return this.lineColor;
    }

    public void setLineColor(Color color) {
        if (color == null) {
            String msg = Logging.getMessage("nullValue.ColorIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }
        this.lineColor = color;
        if (this.line != null) {
            if (this.line.getAttributes() == null) {
                this.line.setAttributes(new BasicShapeAttributes());
            }
            this.line.getAttributes().setOutlineMaterial(new Material(color));
        }
        if (this.surfaceShape != null) {
            ShapeAttributes attr = this.surfaceShape.getAttributes();
            if (attr == null) {
                attr = new BasicShapeAttributes();
            }
            attr.setOutlineMaterial(new Material(color));
            attr.setOutlineOpacity(color.getAlpha() / 255.0d);
            this.surfaceShape.setAttributes(attr);
        }
        this.wwd.redraw();
    }

    public Color getFillColor() {
        return this.fillColor;
    }

    public void setFillColor(Color color) {
        if (color == null) {
            String msg = Logging.getMessage("nullValue.ColorIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }
        this.fillColor = color;
        if (this.surfaceShape != null) {
            ShapeAttributes attr = this.surfaceShape.getAttributes();
            if (attr == null) {
                attr = new BasicShapeAttributes();
            }
            attr.setInteriorMaterial(new Material(color));
            attr.setInteriorOpacity(color.getAlpha() / 255.0d);
            this.surfaceShape.setAttributes(attr);
        }
        this.wwd.redraw();
    }

    public double getLineWidth() {
        return this.lineWidth;
    }

    public void setLineWidth(double width) {
        this.lineWidth = width;
        if (this.line != null) {
            if (this.line.getAttributes() == null) {
                this.line.setAttributes(new BasicShapeAttributes());
            }
            this.line.getAttributes().setOutlineWidth(width);
        }
        if (this.surfaceShape != null) {
            ShapeAttributes attr = this.surfaceShape.getAttributes();
            if (attr == null) {
                attr = new BasicShapeAttributes();
            }
            attr.setOutlineWidth(width);
            this.surfaceShape.setAttributes(attr);
        }
        this.wwd.redraw();
    }

    public String getPathType() {
        return this.pathType;
    }

    public void setPathType(String type) {
        this.pathType = type;
        if (this.line != null) {
            this.line.setPathType(type);
        }
        if (this.surfaceShape != null) {
            this.surfaceShape.setPathType(type);
        }
        if (this.isRegularShape()) {
            this.updateShapeControlPoints();
        }
        this.wwd.redraw();
    }

    public boolean isShowControlPoints() {
        return this.showControlPoints;
    }

    public void setShowControlPoints(boolean state) {
        this.showControlPoints = state;
        this.controlPointsLayer.setEnabled(state);
        this.wwd.redraw();
    }

    public boolean isShowAnnotation() {
        return this.showAnnotation;
    }

    public void setShowAnnotation(boolean state) {
        this.showAnnotation = state;
    }

    /**
     * Removes all positions from the shape, clear attributes.
     */
    public void clear() {
        while (!this.positions.isEmpty() || !this.controlPoints.isEmpty()) {
            this.removeControlPoint();
        }

        this.shapeCenterPosition = null;
        this.shapeOrientation = null;
        this.shapeRectangle = null;
    }

    public boolean isMeasureShape(Object o) {
        return o == this.shapeLayer;
    }

    /**
     * Get the measure shape type. can be one of {@link #SHAPE_LINE}, {@link #SHAPE_PATH}, {@link #SHAPE_POLYGON},
     * {@link #SHAPE_CIRCLE}, {@link #SHAPE_ELLIPSE}, {@link #SHAPE_SQUARE} or {@link #SHAPE_QUAD}.
     *
     * @return the measure shape type.
     */
    public String getMeasureShapeType() {
        return this.measureShapeType;
    }

    /**
     * Set the measure shape type. can be one of {@link #SHAPE_LINE}, {@link #SHAPE_PATH}, {@link #SHAPE_POLYGON},
     * {@link #SHAPE_CIRCLE}, {@link #SHAPE_ELLIPSE}, {@link #SHAPE_SQUARE} or {@link #SHAPE_QUAD}. This will reset the
     * measure tool and clear the current measure shape.
     *
     * @param shape the measure shape type.
     */
    public void setMeasureShapeType(String shape) {
        if (shape == null) {
            String msg = Logging.getMessage("nullValue.ShapeType");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        if (!this.measureShapeType.equals(shape)) {
            setArmed(false);
            clear();
            this.measureShapeType = shape;
        }
    }

    /**
     * Set and initialize the measure shape to one of the regular shapes {@link #SHAPE_CIRCLE}, {@link #SHAPE_ELLIPSE},
     * {@link #SHAPE_SQUARE} or {@link #SHAPE_QUAD}.
     *
     * @param shapeType      the shape type.
     * @param centerPosition the shape center position.
     * @param radius         the shape radius of half width/height.
     */
    public void setMeasureShapeType(String shapeType, Position centerPosition, double radius) {
        setMeasureShapeType(shapeType, centerPosition, radius * 2, radius * 2, Angle.ZERO);
    }

    /**
     * Set and initialize the measure shape to one of the regular shapes {@link #SHAPE_CIRCLE}, {@link #SHAPE_ELLIPSE},
     * {@link #SHAPE_SQUARE} or {@link #SHAPE_QUAD}.
     *
     * @param shapeType      the shape type.
     * @param centerPosition the shape center position.
     * @param width          the shape width.
     * @param height         the shape height.
     * @param orientation    the shape orientation or azimuth angle - clockwise from north.
     */
    public void setMeasureShapeType(String shapeType, Position centerPosition, double width, double height,
        Angle orientation) {
        if (shapeType == null) {
            String msg = Logging.getMessage("nullValue.ShapeType");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }
        if (centerPosition == null) {
            String msg = Logging.getMessage("nullValue.PositionIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }
        if (orientation == null) {
            String msg = Logging.getMessage("nullValue.AngleIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        if (MeasureTool.isRegularShape(shapeType)) {
            setArmed(false);
            clear();
            if ((shapeType.equals(MeasureTool.SHAPE_CIRCLE) || shapeType.equals(MeasureTool.SHAPE_SQUARE)) && width != height) {
                width = Math.max(width, height);
                height = Math.max(width, height);
            }
            // Set regular shape properties
            this.measureShapeType = shapeType;
            this.shapeCenterPosition = centerPosition;
            this.shapeRectangle = new Rectangle2D.Double(0, 0, width, height);
            this.shapeOrientation = orientation;
            // Create control points for regular shapes
            updateShapeControlPoints();
            // Update screen shapes
            updateMeasureShape();
            this.firePropertyChange(MeasureTool.EVENT_POSITION_REPLACE, null, null);
            this.wwd.redraw();
        }
    }

    /**
     * Set the measure shape to an existing <code>Path</code>.
     *
     * @param line a <code>Path</code> instance.
     */
    public void setMeasureShape(Path line) {
        if (line == null) {
            String msg = Logging.getMessage("nullValue.Shape");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }
        setArmed(false);
        this.clear();

        // Clear and replace current shape
        if (this.surfaceShape != null) {
            this.shapeLayer.remove(this.surfaceShape);
            this.surfaceShape = null;
        }
        if (this.line != null) {
            this.shapeLayer.remove(this.line);
        }
        this.line = line;
        this.shapeLayer.add(line);
        // Grab some of the line attributes
        setFollowTerrain(line.isFollowTerrain());
        setPathType(line.getPathType());
        // Update position list and create control points
        int i = 0;
        for (Position pos : line.getPositions()) {
            this.positions.add(pos);
            addControlPoint(pos, MeasureTool.CONTROL_TYPE_LOCATION_INDEX, i++);
        }
        // Set proper measure shape type
        this.measureShapeType = MeasureTool.getPathType(this.positions);
        this.firePropertyChange(MeasureTool.EVENT_POSITION_REPLACE, null, null);
        this.wwd.redraw();
    }

    /**
     * Set the measure shape to an existing <code>SurfaceShape</code>. Can be one of <code>SurfacePolygon</code>,
     * <code>SurfaceQuad</code>, <code>SurfaceSquare</code>, <code>SurfaceEllipse</code> or <code>SurfaceCircle</code>.
     *
     * @param surfaceShape a <code>SurfaceShape</code> instance.
     */
    public void setMeasureShape(SurfaceShape surfaceShape) {
        if (surfaceShape == null) {
            String msg = Logging.getMessage("nullValue.Shape");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        setArmed(false);
        this.clear();

        // Clear and replace current surface shape
        if (this.surfaceShape != null) {
            this.shapeLayer.remove(this.surfaceShape);
        }
        if (this.line != null) {
            this.shapeLayer.remove(this.line);
            this.line = null;
        }
        this.surfaceShape = surfaceShape;
        this.shapeLayer.add(surfaceShape);
        this.setPathType(surfaceShape.getPathType());

        if (surfaceShape instanceof SurfaceQuad) {
            // Set measure shape type
            this.measureShapeType = surfaceShape instanceof SurfaceSquare ? MeasureTool.SHAPE_SQUARE : MeasureTool.SHAPE_QUAD;
            // Set regular shape properties
            SurfaceQuad shape = ((SurfaceQuad) surfaceShape);
            this.shapeCenterPosition = new Position(shape.getCenter(), 0);
            this.shapeRectangle = new Rectangle2D.Double(0, 0, shape.getWidth(), shape.getHeight());
            this.shapeOrientation = shape.getHeading();
            // Create control points for regular shapes
            updateShapeControlPoints();
            // Extract positions from shape
            updatePositionsFromShape();
        } else if (surfaceShape instanceof SurfaceEllipse) {
            // Set measure shape type
            this.measureShapeType = surfaceShape instanceof SurfaceCircle ? MeasureTool.SHAPE_CIRCLE : MeasureTool.SHAPE_ELLIPSE;
            // Set regular shape properties
            SurfaceEllipse shape = ((SurfaceEllipse) surfaceShape);
            this.shapeCenterPosition = new Position(shape.getCenter(), 0);
            this.shapeRectangle = new Rectangle2D.Double(0, 0, shape.getMajorRadius() * 2,
                shape.getMinorRadius() * 2);
            this.shapeOrientation = shape.getHeading();
            // Create control points for regular shapes
            updateShapeControlPoints();
            // Extract positions from shape
            updatePositionsFromShape();
        } else // SurfacePolygon, SurfacePolyline, SurfaceSector, or some custom shape
        {
            // Set measure shape type
            this.measureShapeType = MeasureTool.SHAPE_POLYGON;
            // Extract positions from shape
            updatePositionsFromShape();
            // Create control points for each position except the last that is the same as the first
            for (int i = 0; i < this.positions.size() - 1; i++) {
                addControlPoint(this.positions.get(i), MeasureTool.CONTROL_TYPE_LOCATION_INDEX, i);
            }
        }

        this.firePropertyChange(MeasureTool.EVENT_POSITION_REPLACE, null, null);
        this.wwd.redraw();
    }

    public boolean isRegularShape() {
        return MeasureTool.isRegularShape(this.measureShapeType);
    }

    public boolean isFollowTerrain() {
        return this.followTerrain;
    }

    public void setFollowTerrain(boolean followTerrain) {
        this.followTerrain = followTerrain;
        if (this.line != null) {
            this.line.setFollowTerrain(followTerrain);
            if (followTerrain) {
                this.line.setAltitudeMode(WorldWind.CLAMP_TO_GROUND);
            } else {
                this.line.setAltitudeMode(WorldWind.ABSOLUTE);
            }
        }
    }

    // *** Editing shapes ***

    // *** Metric accessors ***
    public double getLength() {
        Globe globe = this.wwd.model().getGlobe();

        if (this.line != null) {
            return this.line.getLength(globe);
        }

        if (this.surfaceShape != null) {
            return this.surfaceShape.getPerimeter(globe);
        }

        return -1;
    }

    public double getArea() {
        Globe globe = this.wwd.model().getGlobe();

        if (this.surfaceShape != null) {
            return this.surfaceShape.getArea(globe, this.followTerrain);
        }

        return -1;
    }

    public double getWidth() {
        if (this.shapeRectangle != null) {
            return this.shapeRectangle.width;
        }
        return -1;
    }

    public double getHeight() {
        if (this.shapeRectangle != null) {
            return this.shapeRectangle.height;
        }
        return -1;
    }

    public Angle getOrientation() {
        return this.shapeOrientation;
    }

    public Position getCenterPosition() {
        return this.shapeCenterPosition;
    }

    /**
     * Add a control point to the current measure shape at the current WorldWindow position.
     *
     * @return The position of the new control point, or null if the control point could not be added.
     */
    public Position addControlPoint() {
        Position curPos = this.wwd.position();
        if (curPos == null) {
            return null;
        }

        if (this.isRegularShape()) {
            // Regular shapes are defined in two steps: 1. center, 2. initial corner or edge.
            if (this.shapeCenterPosition == null) {
                this.shapeCenterPosition = curPos;
                this.shapeOrientation = this.getShapeInitialHeading();
                updateShapeControlPoints();
            } else if (this.shapeRectangle == null) {
                // Compute shape rectangle and heading, curPos being a corner
                String control = this.getShapeInitialControl(curPos);
                updateShapeProperties(control, curPos, null);
                // Update or create control points
                updateShapeControlPoints();
            }
        } else {
            if (!this.measureShapeType.equals(MeasureTool.SHAPE_POLYGON) || this.positions.size() <= 1) {
                // Line, path or polygons with less then two points
                this.positions.add(curPos);
                addControlPoint(this.positions.get(this.positions.size() - 1), MeasureTool.CONTROL_TYPE_LOCATION_INDEX,
                    this.positions.size() - 1);
                if (this.measureShapeType.equals(MeasureTool.SHAPE_POLYGON) && this.positions.size() == 2) {
                    // Once we have two points of a polygon, add an extra position
                    // to loop back to the first position and have a closed shape
                    this.positions.add(this.positions.get(0));
                }
                if (this.measureShapeType.equals(MeasureTool.SHAPE_LINE) && this.positions.size() > 1) {
                    // Two points on a line, update line heading info
                    this.shapeOrientation = LatLon.greatCircleAzimuth(this.positions.get(0), this.positions.get(1));
                }
            } else {
                // For polygons with more then 2 points, the last position is the same as the first, so insert before it
                this.positions.add(positions.size() - 1, curPos);
                addControlPoint(this.positions.get(this.positions.size() - 2), MeasureTool.CONTROL_TYPE_LOCATION_INDEX,
                    this.positions.size() - 2);
            }
        }
        // Update screen shapes
        updateMeasureShape();
        this.firePropertyChange(MeasureTool.EVENT_POSITION_ADD, null, curPos);
        this.wwd.redraw();

        return curPos;
    }

    /**
     * Remove the last control point from the current measure shape.
     */
    public void removeControlPoint() {
        Position currentLastPosition = null;
        if (this.isRegularShape()) {
            if (this.shapeRectangle != null) {
                this.shapeRectangle = null;
                this.shapeOrientation = null;
                this.positions.clear();
                // remove all control points except center which is first
                while (this.controlPoints.size() > 1) {
                    this.controlPoints.remove(1);
                }
            } else if (this.shapeCenterPosition != null) {
                this.shapeCenterPosition = null;
                this.controlPoints.clear();
            }
        } else {
            if (this.positions.isEmpty()) {
                return;
            }

            if (!this.measureShapeType.equals(MeasureTool.SHAPE_POLYGON) || this.positions.size() == 1) {
                currentLastPosition = this.positions.get(this.positions.size() - 1);
                this.positions.remove(this.positions.size() - 1);
            } else {
                // For polygons with more then 2 points, the last position is the same as the first, so remove before it
                currentLastPosition = this.positions.get(this.positions.size() - 2);
                this.positions.remove(this.positions.size() - 2);
                if (positions.size() == 2) {
                    positions.remove(1); // remove last loop position when a polygon shrank to only two (same) positions
                }
            }
            if (!this.controlPoints.isEmpty()) {
                this.controlPoints.remove(this.controlPoints.size() - 1);
            }
        }
//        this.controlPointsLayer.set(this.controlPoints);
        // Update screen shapes
        updateMeasureShape();
        this.firePropertyChange(MeasureTool.EVENT_POSITION_REMOVE, currentLastPosition, null);
        this.wwd.redraw();
    }

    /**
     * Update the current measure shape according to a given control point position.
     *
     * @param point one of the shape control points.
     */
    public void moveControlPoint(ControlPoint point) {
        moveControlPoint(point, null); // use the default mode.
    }

    /**
     * Update the current measure shape according to a given control point position and shape edition mode.
     *
     * @param point one of the shape control points.
     * @param mode  the shape edition mode.
     */
    public void moveControlPoint(ControlPoint point, String mode) {
        if (point.get(MeasureTool.CONTROL_TYPE_REGULAR_SHAPE) != null) {
            // Update shape properties
            updateShapeProperties((String) point.get(MeasureTool.CONTROL_TYPE_REGULAR_SHAPE), point.getPosition(), mode);
            updateShapeControlPoints();
        }

        if (point.get(MeasureTool.CONTROL_TYPE_LOCATION_INDEX) != null) {
            int positionIndex = (Integer) point.get(MeasureTool.CONTROL_TYPE_LOCATION_INDEX);
            // Update positions
            Position surfacePosition = computeSurfacePosition(point.getPosition());
            surfacePosition = new Position(point.getPosition(), surfacePosition.getAltitude());
            positions.set(positionIndex, surfacePosition);
            // Update last pos too if polygon and first pos changed
            if (measureShapeType.equals(MeasureTool.SHAPE_POLYGON) && positions.size() > 2 && positionIndex == 0) {
                positions.set(positions.size() - 1, surfacePosition);
            }
            // Update heading for simple line
            if (measureShapeType.equals(MeasureTool.SHAPE_LINE) && positions.size() > 1) {
                shapeOrientation = LatLon.greatCircleAzimuth(positions.get(0), positions.get(1));
            }
        }

        // Update rendered shapes
        updateMeasureShape();
    }

    /**
     * Move the current measure shape along a great circle arc at a given azimuth <code>Angle</code> for a given
     * distance <code>Angle</code>.
     *
     * @param azimuth  the azimuth <code>Angle</code>.
     * @param distance the distance <code>Angle</code>.
     */
    public void moveMeasureShape(Angle azimuth, Angle distance) {
        if (distance == null) {
            String msg = Logging.getMessage("nullValue.AngleIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }
        if (azimuth == null) {
            String msg = Logging.getMessage("nullValue.AngleIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        if (this.isRegularShape()) {
            // Move regular shape center
            if (!controlPoints.isEmpty()) {
                ControlPoint point = this.getControlPoint(MeasureTool.CENTER);
                point.setPosition(
                    new Position(LatLon.greatCircleEndPosition(point.getPosition(), azimuth, distance), 0));
                moveControlPoint(point);
            }
        } else {
            // Move all positions and control points
            for (int i = 0; i < positions.size(); i++) {
                Position newPos = computeSurfacePosition(
                    LatLon.greatCircleEndPosition(positions.get(i), azimuth, distance));
                positions.set(i, newPos);
                if (!this.measureShapeType.equals(MeasureTool.SHAPE_POLYGON) || i < positions.size() - 1) {
                    ((GlobeAnnotation) controlPoints.get(i)).setPosition(new Position(newPos, 0));
                }
            }
            // Update heading for simple line
            if (measureShapeType.equals(MeasureTool.SHAPE_LINE) && positions.size() > 1) {
                shapeOrientation = LatLon.greatCircleAzimuth(positions.get(0), positions.get(1));
            }
            // Update rendered shapes
            updateMeasureShape();
        }
    }

    protected Position computeSurfacePosition(LatLon latLon) {
        Vec4 surfacePoint = wwd.sceneControl().getTerrain().getSurfacePoint(latLon.getLatitude(),
            latLon.getLongitude());
        if (surfacePoint != null) {
            return wwd.model().getGlobe().computePositionFromPoint(surfacePoint);
        } else {
            return new Position(latLon, wwd.model().getGlobe().elevation(latLon.getLatitude(),
                latLon.getLongitude()));
        }
    }

    public String getShapeInitialControl(Position position) {
        if (this.measureShapeType.equals(MeasureTool.SHAPE_ELLIPSE) || this.measureShapeType.equals(MeasureTool.SHAPE_CIRCLE)) {
            return MeasureTool.EAST;
        } else if (this.measureShapeType.equals(MeasureTool.SHAPE_QUAD) || this.measureShapeType.equals(MeasureTool.SHAPE_SQUARE)) {
            return MeasureTool.NORTHEAST;
        }

        return null;
    }

    protected Angle getShapeInitialHeading() {
        return this.wwd.view().getHeading();
    }

    protected void updateShapeProperties(String control, Position newPosition, String mode) {
        // Update the shape's center position
        // Update the shape's orientation.
        // Update the shape's center position and dimensions.
        switch (control) {
            case MeasureTool.CENTER -> this.updateShapeCenter(control, newPosition);
            case MeasureTool.NORTH_LEADER -> this.updateShapeOrientation(control, newPosition);
            default -> this.updateShapeSize(control, newPosition);
        }
    }

    protected void updateShapeCenter(String control, Position newPosition) {
        this.shapeCenterPosition = newPosition;
    }

    protected void updateShapeOrientation(String control, Position newPosition) {
        // Compute the control point's azimuth in shape local coordinates.
        Angle controlAzimuth = MeasureTool.computeControlPointAzimuth(control, this.shapeRectangle.width,
            this.shapeRectangle.height);

        // Compute the shape's new azimuth as the difference between the great arc azimuth from the shape's
        // center position and the new corner position, and the corner's azimuth in shape local coordinates.
        Angle newShapeAzimuth = LatLon.greatCircleAzimuth(this.shapeCenterPosition, newPosition);
        newShapeAzimuth = newShapeAzimuth.sub(controlAzimuth);

        // Set the shape's new orientation.
        this.shapeOrientation = MeasureTool.computeNormalizedHeading(newShapeAzimuth);
    }

    protected void updateShapeSize(String control, Position newPosition) {
        if (this.measureShapeType.equals(MeasureTool.SHAPE_ELLIPSE) || this.measureShapeType.equals(MeasureTool.SHAPE_CIRCLE)) {
            // Compute azimuth and arc length which define the great arc spanning the shape's center position and the
            // control point's position.
            Angle refAzimiuth = MeasureTool.computeControlPointAzimuth(control, 1.0d, 1.0d).add(this.shapeOrientation);
            Angle controlAzimuth = LatLon.greatCircleAzimuth(this.shapeCenterPosition, newPosition);
            Angle controlArcLength = LatLon.greatCircleDistance(this.shapeCenterPosition, newPosition);
            // Compute the arc length in meters of the great arc between the shape's center position and the control
            // point's position.
            Angle diffAngle = refAzimiuth.angularDistanceTo(controlAzimuth);
            double globeRadius = this.wwd.model().getGlobe().getRadiusAt(this.shapeCenterPosition);
            double arcLengthMeters = Math.abs(diffAngle.cos()) * Math.abs(controlArcLength.radians()) * globeRadius;

            double widthMeters;
            double heightMeters;

            if (control.equals(MeasureTool.EAST) || control.equals(MeasureTool.WEST)) {
                widthMeters = 2.0d * arcLengthMeters;
                heightMeters = (this.shapeRectangle != null) ? this.shapeRectangle.getHeight() : widthMeters;

                if (this.measureShapeType.equals(MeasureTool.SHAPE_CIRCLE)) {
                    //noinspection SuspiciousNameCombination
                    heightMeters = widthMeters;
                } // during shape creation
                else if (this.controller != null && this.controller.isActive()) {
                    heightMeters = 0.6 * widthMeters;
                }
            } else // if (control.equals(NORTH) || control.equals(SOUTH))
            {
                heightMeters = 2.0d * arcLengthMeters;
                widthMeters = (this.shapeRectangle != null) ? this.shapeRectangle.getWidth() : heightMeters;

                if (this.measureShapeType.equals(MeasureTool.SHAPE_CIRCLE)) {
                    //noinspection SuspiciousNameCombination
                    widthMeters = heightMeters;
                } // during shape creation
                else if (this.controller != null && this.controller.isActive()) {
                    widthMeters = 0.6 * heightMeters;
                }
            }

            if (widthMeters <= MeasureTool.SHAPE_MIN_WIDTH_METERS) {
                widthMeters = MeasureTool.SHAPE_MIN_WIDTH_METERS;
            }
            if (heightMeters <= MeasureTool.SHAPE_MIN_HEIGHT_METERS) {
                heightMeters = MeasureTool.SHAPE_MIN_HEIGHT_METERS;
            }

            this.shapeRectangle = new Rectangle2D.Double(0.0d, 0.0d, widthMeters, heightMeters);

            // Determine if the dragged control point crossed the shape's horizontal or vertical boundary, causing
            // the shape to "flip". If so, swap the control point with its horizontal or vertical opposite. This
            // ensures that control points have the correct orientation.
            this.swapEdgeControls(control, newPosition);
        } else if (this.measureShapeType.equals(MeasureTool.SHAPE_QUAD) || this.measureShapeType.equals(MeasureTool.SHAPE_SQUARE)) {
            ControlPoint oppositeControlPoint = this.getOppositeControl(control);

            // Compute the corner position diagonal from the current corner position, and compute the azimuth
            // and arc length which define a great arc between the two positions.
            Angle diagonalAzimuth = LatLon.greatCircleAzimuth(oppositeControlPoint.getPosition(), newPosition);
            Angle diagonalArcLength = LatLon.greatCircleDistance(oppositeControlPoint.getPosition(), newPosition);

            // Compute the shape's center location as the mid point on the great arc between the two diagonal
            // control points.
            LatLon newCenterLocation = LatLon.greatCircleEndPosition(oppositeControlPoint.getPosition(),
                diagonalAzimuth, diagonalArcLength.divide(2.0d));

            // Compute the azimuth and arc length which define a great arc between the new center position and
            // the new corner position.
            Angle controlAzimuth = LatLon.greatCircleAzimuth(newCenterLocation, newPosition);
            controlAzimuth = this.computeControlPointAzimuthInShapeCoordinates(control, controlAzimuth);
            Angle controlArcLength = LatLon.greatCircleDistance(newCenterLocation, newPosition);
            // Compute the arc length in meters of the great arc between the new center position and the new
            // corner position.
            double globeRadius = this.wwd.model().getGlobe().getRadiusAt(newCenterLocation);
            double arcLengthMeters = controlArcLength.radians() * globeRadius;

            // Compute shape's the width and height in meters from the diagonal between the shape's new center
            // position and its new corner position.
            double widthMeters = 2.0d * arcLengthMeters * Math.abs(controlAzimuth.sin());
            double heightMeters = 2.0d * arcLengthMeters * Math.abs(controlAzimuth.cos());

            if (widthMeters <= MeasureTool.SHAPE_MIN_WIDTH_METERS) {
                widthMeters = MeasureTool.SHAPE_MIN_WIDTH_METERS;
            }
            if (heightMeters <= MeasureTool.SHAPE_MIN_HEIGHT_METERS) {
                heightMeters = MeasureTool.SHAPE_MIN_HEIGHT_METERS;
            }

            if (this.measureShapeType.equals(MeasureTool.SHAPE_SQUARE)) {
                // Force the square to have equivalent dimensions.
                double sizeMeters = Math.min(widthMeters, heightMeters);
                widthMeters = sizeMeters;
                heightMeters = sizeMeters;

                // Determine if the dragged control point crossed the shape's horizontal or vertical boundary, causing
                // the shape to "flip". If so, swap the control point with its horizontal or vertical opposite. This
                // ensures that control points have the correct orientation. We perform this operation prior to
                // adjusting the square's fixed corner location, because flipping the shape changes which corner we
                // adjust.
                this.swapCornerControls(control, newPosition);

                // Forcing the square to have equivalent width and height causes the opposite control point to move
                // from its current location. Move the square's opposite control point back to its original location
                // so that the square drags from a fixed corner out to the current control point.
                LatLon location = MeasureTool.moveShapeByControlPoint(oppositeControlPoint, this.wwd.model().getGlobe(),
                    this.shapeOrientation, newCenterLocation, widthMeters, heightMeters);
                if (location != null) {
                    newCenterLocation = location;
                }
            }

            // Set the shape's new center position and new dimensions.
            this.shapeCenterPosition = new Position(newCenterLocation, 0.0d);
            this.shapeRectangle = new Rectangle2D.Double(0.0d, 0.0d, widthMeters, heightMeters);

            if (this.measureShapeType.equals(MeasureTool.SHAPE_QUAD)) {
                // Determine if the dragged control point crossed the shape's horizontal or vertical boundary, causing
                // the shape to "flip". If so, swap the control point with its horizontal or vertical opposite. This
                // ensures that control points have the correct orientation.
                this.swapCornerControls(control, newPosition);
            }
        }
    }

    public ControlPoint getControlPoint(String control) {
        for (Renderable cp : this.controlPoints) {
            String value = ((KV) cp).getStringValue(MeasureTool.CONTROL_TYPE_REGULAR_SHAPE);
            if (value != null && value.equals(control)) {
                return (ControlPoint) cp;
            }
        }
        return null;
    }

    protected String computeCornerControl(Position position) {
        if (this.shapeCenterPosition == null || this.shapeOrientation == null) {
            return null;
        }

        Angle azimuth = LatLon.greatCircleAzimuth(this.shapeCenterPosition, position).sub(this.shapeOrientation);
        azimuth = MeasureTool.computeNormalizedHeading(azimuth);

        if (azimuth.degrees < 90) {
            return MeasureTool.NORTHEAST;
        } else if (azimuth.degrees < 180) {
            return MeasureTool.SOUTHEAST;
        } else if (azimuth.degrees < 270) {
            return MeasureTool.SOUTHWEST;
        } else {
            return MeasureTool.NORTHWEST;
        }
    }

    protected ControlPoint getOppositeControl(String control) {
        if (this.controlPoints.isEmpty()) {
            return null;
        } else if (this.controlPoints.size() == 1) {
            return getControlPoint(MeasureTool.CENTER);
        } else if (control.equals(MeasureTool.NORTH)) {
            return getControlPoint(MeasureTool.SOUTH);
        } else if (control.equals(MeasureTool.EAST)) {
            return getControlPoint(MeasureTool.WEST);
        } else if (control.equals(MeasureTool.SOUTH)) {
            return getControlPoint(MeasureTool.NORTH);
        } else if (control.equals(MeasureTool.WEST)) {
            return getControlPoint(MeasureTool.EAST);
        } else if (control.equals(MeasureTool.NORTHEAST)) {
            return getControlPoint(MeasureTool.SOUTHWEST);
        } else if (control.equals(MeasureTool.SOUTHEAST)) {
            return getControlPoint(MeasureTool.NORTHWEST);
        } else if (control.equals(MeasureTool.SOUTHWEST)) {
            return getControlPoint(MeasureTool.NORTHEAST);
        } else if (control.equals(MeasureTool.NORTHWEST)) {
            return getControlPoint(MeasureTool.SOUTHEAST);
        }
        return null;
    }

    protected void swapEdgeControls(String control, Position position) {
        if (this.controlPoints.size() < 2) {
            return;
        }

        if (this.shapeCenterPosition == null || this.shapeOrientation == null) {
            return;
        }

        Angle azimuth = LatLon.greatCircleAzimuth(this.shapeCenterPosition, position).sub(this.shapeOrientation);
        azimuth = MeasureTool.computeNormalizedHeading(azimuth);

        if ((control.equals(MeasureTool.NORTH) && azimuth.degrees < 270 && azimuth.degrees > 90)
            || (control.equals(MeasureTool.SOUTH) && (azimuth.degrees > 270 || azimuth.degrees < 90))) {
            for (Renderable r : this.controlPoints) {
                KV cp = (KV) r;
                String c = cp.getStringValue(MeasureTool.CONTROL_TYPE_REGULAR_SHAPE);
                if (c == null) {
                    continue;
                }

                if (c.equals(MeasureTool.NORTH)) {
                    cp.set(MeasureTool.CONTROL_TYPE_REGULAR_SHAPE, MeasureTool.SOUTH);
                } else if (c.equals(MeasureTool.SOUTH)) {
                    cp.set(MeasureTool.CONTROL_TYPE_REGULAR_SHAPE, MeasureTool.NORTH);
                }
            }
        }

        if ((control.equals(MeasureTool.EAST) && (azimuth.degrees < 360 && azimuth.degrees > 180))
            || (control.equals(MeasureTool.WEST) && (azimuth.degrees < 180 && azimuth.degrees > 0))) {
            for (Renderable r : this.controlPoints) {
                KV cp = (KV) r;
                String c = cp.getStringValue(MeasureTool.CONTROL_TYPE_REGULAR_SHAPE);
                if (c == null) {
                    continue;
                }

                if (c.equals(MeasureTool.EAST)) {
                    cp.set(MeasureTool.CONTROL_TYPE_REGULAR_SHAPE, MeasureTool.WEST);
                } else if (c.equals(MeasureTool.WEST)) {
                    cp.set(MeasureTool.CONTROL_TYPE_REGULAR_SHAPE, MeasureTool.EAST);
                }
            }
        }
    }

    protected void swapCornerControls(CharSequence control, Position position) {
        if (this.controlPoints.size() < 2) {
            return;
        }

        String newControl = this.computeCornerControl(position);
        if (control.equals(newControl)) {
            return;  // no need to swap
        }
        // For corner controls NE, SE, SW, NW
        if (control.length() != 2 || newControl.length() != 2) {
            return;
        }

        if (control.charAt(0) != newControl.charAt(0)) {
            for (Renderable r : this.controlPoints) {
                KV cp = (KV) r;
                String c = cp.getStringValue(MeasureTool.CONTROL_TYPE_REGULAR_SHAPE);
                if (c == null) {
                    continue;
                }

                switch (c) {
                    case MeasureTool.NORTHEAST -> cp.set(MeasureTool.CONTROL_TYPE_REGULAR_SHAPE, MeasureTool.SOUTHEAST);
                    case MeasureTool.SOUTHEAST -> cp.set(MeasureTool.CONTROL_TYPE_REGULAR_SHAPE, MeasureTool.NORTHEAST);
                    case MeasureTool.SOUTHWEST -> cp.set(MeasureTool.CONTROL_TYPE_REGULAR_SHAPE, MeasureTool.NORTHWEST);
                    case MeasureTool.NORTHWEST -> cp.set(MeasureTool.CONTROL_TYPE_REGULAR_SHAPE, MeasureTool.SOUTHWEST);
                }
            }
        }
        if (control.charAt(1) != newControl.charAt(1)) {
            for (Renderable r : this.controlPoints) {
                KV cp = (KV) r;
                String c = cp.getStringValue(MeasureTool.CONTROL_TYPE_REGULAR_SHAPE);
                if (c == null) {
                    continue;
                }

                switch (c) {
                    case MeasureTool.NORTHEAST -> cp.set(MeasureTool.CONTROL_TYPE_REGULAR_SHAPE, MeasureTool.NORTHWEST);
                    case MeasureTool.SOUTHEAST -> cp.set(MeasureTool.CONTROL_TYPE_REGULAR_SHAPE, MeasureTool.SOUTHWEST);
                    case MeasureTool.SOUTHWEST -> cp.set(MeasureTool.CONTROL_TYPE_REGULAR_SHAPE, MeasureTool.SOUTHEAST);
                    case MeasureTool.NORTHWEST -> cp.set(MeasureTool.CONTROL_TYPE_REGULAR_SHAPE, MeasureTool.NORTHEAST);
                }
            }
        }
    }

    protected LatLon computeQuadEdgeMidpointLocation(String control, Globe globe, Angle heading, LatLon center,
        double width, double height) {
        LatLon ne = MeasureTool.computeControlPointLocation(MeasureTool.NORTHEAST, globe, heading, center, width, height);
        LatLon se = MeasureTool.computeControlPointLocation(MeasureTool.SOUTHEAST, globe, heading, center, width, height);
        LatLon sw = MeasureTool.computeControlPointLocation(MeasureTool.SOUTHWEST, globe, heading, center, width, height);
        LatLon nw = MeasureTool.computeControlPointLocation(MeasureTool.NORTHWEST, globe, heading, center, width, height);

        switch (control) {
            case MeasureTool.NORTH:
                return LatLon.interpolate(this.pathType, 0.5, nw, ne);
            case MeasureTool.EAST:
                return LatLon.interpolate(this.pathType, 0.5, ne, se);
            case MeasureTool.SOUTH:
                return LatLon.interpolate(this.pathType, 0.5, sw, se);
            case MeasureTool.WEST:
                return LatLon.interpolate(this.pathType, 0.5, sw, nw);
            default:
                break;
        }

        return null;
    }

    protected Angle computeControlPointAzimuthInShapeCoordinates(String control, Angle azimuth) {
        switch (control) {
            case MeasureTool.NORTHEAST:
                return azimuth.sub(this.shapeOrientation);
            case MeasureTool.SOUTHEAST:
                return this.shapeOrientation.addDegrees(180).sub(azimuth);
            case MeasureTool.SOUTHWEST:
                return azimuth.sub(this.shapeOrientation.addDegrees(180));
            case MeasureTool.NORTHWEST:
                return this.shapeOrientation.sub(azimuth);
            default:
                break;
        }

        return null;
    }

    protected void updateShapeControlPoints() {
        if (this.shapeCenterPosition != null) {
            // Set center control point
            if (this.controlPoints.size() < 1) {
                addControlPoint(Position.ZERO, MeasureTool.CONTROL_TYPE_REGULAR_SHAPE, MeasureTool.CENTER);
            }

            // Update center control point position
            ((GlobeAnnotation) this.controlPoints.get(0)).setPosition(new Position(this.shapeCenterPosition, 0));
        }

        if (this.shapeRectangle != null) {
            if (this.controlPoints.size() < 5) {
                // Add control points in four directions - CW from north
                if (this.measureShapeType.equals(MeasureTool.SHAPE_ELLIPSE) || this.measureShapeType.equals(
                    MeasureTool.SHAPE_CIRCLE)) {
                    this.addControlPoint(Position.ZERO, MeasureTool.CONTROL_TYPE_REGULAR_SHAPE, MeasureTool.NORTH);
                    this.addControlPoint(Position.ZERO, MeasureTool.CONTROL_TYPE_REGULAR_SHAPE, MeasureTool.EAST);
                    this.addControlPoint(Position.ZERO, MeasureTool.CONTROL_TYPE_REGULAR_SHAPE, MeasureTool.SOUTH);
                    this.addControlPoint(Position.ZERO, MeasureTool.CONTROL_TYPE_REGULAR_SHAPE, MeasureTool.WEST);
                } // Add control points at four corners - CW from north
                else if (this.measureShapeType.equals(MeasureTool.SHAPE_QUAD) || this.measureShapeType.equals(
                    MeasureTool.SHAPE_SQUARE)) {
                    this.addControlPoint(Position.ZERO, MeasureTool.CONTROL_TYPE_REGULAR_SHAPE, MeasureTool.NORTHEAST);
                    this.addControlPoint(Position.ZERO, MeasureTool.CONTROL_TYPE_REGULAR_SHAPE, MeasureTool.SOUTHEAST);
                    this.addControlPoint(Position.ZERO, MeasureTool.CONTROL_TYPE_REGULAR_SHAPE, MeasureTool.SOUTHWEST);
                    this.addControlPoint(Position.ZERO, MeasureTool.CONTROL_TYPE_REGULAR_SHAPE, MeasureTool.NORTHWEST);
                }

                // Add a control point with a leader to the top of the shape.
                this.addControlPointWithLeader(Position.ZERO, MeasureTool.CONTROL_TYPE_REGULAR_SHAPE, MeasureTool.NORTH_LEADER,
                    MeasureTool.CONTROL_TYPE_LEADER_ORIGIN, MeasureTool.NORTH);
            }

            Globe globe = this.getWwd().model().getGlobe();

            // Update control points positions
            for (Renderable r : this.controlPoints) {
                ControlPoint cp = (ControlPoint) r;

                String control = cp.getStringValue(MeasureTool.CONTROL_TYPE_REGULAR_SHAPE);
                if (control == null) {
                    continue;
                }

                LatLon controlLocation = MeasureTool.computeControlPointLocation(control, globe, this.shapeOrientation,
                    this.shapeCenterPosition, this.shapeRectangle.getWidth(), this.shapeRectangle.getHeight());
                if (controlLocation == null) {
                    continue;
                }

                cp.setPosition(new Position(controlLocation, 0.0d));

                if (cp instanceof ControlPointWithLeader) {
                    this.updateControlPointWithLeader((ControlPointWithLeader) cp, controlLocation);
                }
            }
        }
    }

    protected void updateControlPointWithLeader(ControlPointWithLeader cp, LatLon controlLocation) {
        Globe globe = this.getWwd().model().getGlobe();

        String leaderControl = cp.getStringValue(MeasureTool.CONTROL_TYPE_LEADER_ORIGIN);
        if (leaderControl == null) {
            return;
        }

        LatLon leaderBegin;

        if (this.measureShapeType.equals(MeasureTool.SHAPE_QUAD) || this.measureShapeType.equals(MeasureTool.SHAPE_SQUARE)) {
            leaderBegin = this.computeQuadEdgeMidpointLocation(leaderControl, globe,
                this.shapeOrientation, this.shapeCenterPosition, this.shapeRectangle.getWidth(),
                this.shapeRectangle.getHeight());
        } else {
            leaderBegin = MeasureTool.computeControlPointLocation(leaderControl, globe,
                this.shapeOrientation, this.shapeCenterPosition, this.shapeRectangle.getWidth(),
                this.shapeRectangle.getHeight());
        }

        if (leaderBegin == null) {
            return;
        }

        cp.setLeaderLocations(leaderBegin, controlLocation);
    }

    protected void updateMeasureShape() {
        // Update line
        if (this.measureShapeType.equals(MeasureTool.SHAPE_LINE) || this.measureShapeType.equals(MeasureTool.SHAPE_PATH)) {
            if (this.positions.size() > 1 && this.line == null) {
                // Init path
                this.line = new Path();
                setFollowTerrain(this.isFollowTerrain());
                this.line.setPathType(this.getPathType());
                ShapeAttributes attrs = new BasicShapeAttributes();
                attrs.setOutlineWidth(this.getLineWidth());
                attrs.setOutlineMaterial(new Material(this.getLineColor()));
                this.line.setAttributes(attrs);
                this.shapeLayer.add(this.line);
            }
            if (this.positions.size() < 2 && this.line != null) {
                // Remove line if less then 2 positions
                this.shapeLayer.remove(this.line);
                this.line = null;
            }
            // Update current line
            if (this.positions.size() > 1 && this.line != null) {
                this.line.setPositions(this.positions);
            }

            if (this.surfaceShape != null) {
                // Remove surface shape if necessary
                this.shapeLayer.remove(this.surfaceShape);
                this.surfaceShape = null;
            }
        } // Update polygon
        else if (this.measureShapeType.equals(MeasureTool.SHAPE_POLYGON)) {
            if (this.positions.size() >= 4 && this.surfaceShape == null) {
                // Init surface shape
                this.surfaceShape = new SurfacePolygon(this.positions);
                ShapeAttributes attr = new BasicShapeAttributes();
                attr.setInteriorMaterial(new Material(this.getFillColor()));
                attr.setInteriorOpacity(this.getFillColor().getAlpha() / 255.0d);
                attr.setOutlineMaterial(new Material(this.getLineColor()));
                attr.setOutlineOpacity(this.getLineColor().getAlpha() / 255.0d);
                attr.setOutlineWidth(this.getLineWidth());
                this.surfaceShape.setAttributes(attr);
                this.shapeLayer.add(this.surfaceShape);
            }
            if (this.positions.size() <= 3 && this.surfaceShape != null) {
                // Remove surface shape if only three positions or less - last is same as first
                this.shapeLayer.remove(this.surfaceShape);
                this.surfaceShape = null;
            }
            if (this.surfaceShape != null) {
                // Update current shape
                ((SurfacePolygon) this.surfaceShape).setLocations(this.positions);
            }
            // Remove line if necessary
            if (this.line != null) {
                this.shapeLayer.remove(this.line);
                this.line = null;
            }
        } // Update regular shape
        else if (this.isRegularShape()) {
            if (this.shapeCenterPosition != null && this.shapeRectangle != null && this.surfaceShape == null) {
                // Init surface shape
                switch (this.measureShapeType) {
                    case MeasureTool.SHAPE_QUAD:
                        this.surfaceShape = new SurfaceQuad(this.shapeCenterPosition,
                            this.shapeRectangle.width, this.shapeRectangle.height, this.shapeOrientation);
                        break;
                    case MeasureTool.SHAPE_SQUARE:
                        this.surfaceShape = new SurfaceSquare(this.shapeCenterPosition,
                            this.shapeRectangle.width);
                        break;
                    case MeasureTool.SHAPE_ELLIPSE:
                        this.surfaceShape = new SurfaceEllipse(this.shapeCenterPosition,
                            this.shapeRectangle.width / 2, this.shapeRectangle.height / 2, this.shapeOrientation,
                            this.shapeIntervals);
                        break;
                    case MeasureTool.SHAPE_CIRCLE:
                        this.surfaceShape = new SurfaceCircle(this.shapeCenterPosition,
                            this.shapeRectangle.width / 2, this.shapeIntervals);
                        break;
                    default:
                        break;
                }

                ShapeAttributes attr = new BasicShapeAttributes();
                attr.setInteriorMaterial(new Material(this.getFillColor()));
                attr.setInteriorOpacity(this.getFillColor().getAlpha() / 255.0d);
                attr.setOutlineMaterial(new Material(this.getLineColor()));
                attr.setOutlineOpacity(this.getLineColor().getAlpha() / 255.0d);
                attr.setOutlineWidth(this.getLineWidth());
                this.surfaceShape.setAttributes(attr);
                this.shapeLayer.add(this.surfaceShape);
            }
            if (this.shapeRectangle == null && this.surfaceShape != null) {
                // Remove surface shape if not defined
                this.shapeLayer.remove(this.surfaceShape);
                this.surfaceShape = null;
                this.positions.clear();
            }
            if (this.surfaceShape != null) {
                // Update current shape
                if (this.measureShapeType.equals(MeasureTool.SHAPE_QUAD) || this.measureShapeType.equals(MeasureTool.SHAPE_SQUARE)) {
                    ((SurfaceQuad) this.surfaceShape).setCenter(this.shapeCenterPosition);
                    ((SurfaceQuad) this.surfaceShape).setSize(this.shapeRectangle.width, this.shapeRectangle.height);
                    ((SurfaceQuad) this.surfaceShape).setHeading(this.shapeOrientation);
                }
                if (this.measureShapeType.equals(MeasureTool.SHAPE_ELLIPSE) || this.measureShapeType.equals(
                    MeasureTool.SHAPE_CIRCLE)) {
                    ((SurfaceEllipse) this.surfaceShape).setCenter(this.shapeCenterPosition);
                    ((SurfaceEllipse) this.surfaceShape).setRadii(this.shapeRectangle.width / 2,
                        this.shapeRectangle.height / 2);
                    ((SurfaceEllipse) this.surfaceShape).setHeading(this.shapeOrientation);
                }
                // Update position from shape list with zero elevation
                updatePositionsFromShape();
            }
            // Remove line if necessary
            if (this.line != null) {
                this.shapeLayer.remove(this.line);
                this.line = null;
            }
        }
    }

    protected void updatePositionsFromShape() {
        Globe globe = this.wwd.model().getGlobe();

        this.positions.clear();

        Iterable<? extends LatLon> locations = this.surfaceShape.getLocations(globe);
        if (locations != null) {
            for (LatLon latLon : locations) {
                this.positions.add(new Position(latLon, 0));
            }
        }
    }

    @Override
    public void dispose() {
        this.setController(null);
        if (this.applicationLayer != null) {
            this.applicationLayer.remove(this.layer);
        } else {
            this.wwd.model().getLayers().remove(this.layer);
        }
        this.layer.clear();
        this.shapeLayer.clear();
        this.controlPoints.clear();
    }

    protected void addControlPoint(Position position, String key, Object value) {
        ControlPoint controlPoint = new ControlPoint(new Position(position, 0), this.controlPointsAttributes, this);
        controlPoint.set(key, value);
        this.doAddControlPoint(controlPoint);
    }

    protected void addControlPointWithLeader(Position position, String controlKey, Object control, String leaderKey,
        Object leader) {
        ControlPointWithLeader controlPoint = new ControlPointWithLeader(new Position(position, 0),
            this.controlPointWithLeaderAttributes, this.leaderAttributes, this);
        controlPoint.set(controlKey, control);
        controlPoint.set(leaderKey, leader);
        this.doAddControlPoint(controlPoint);
    }

    protected void doAddControlPoint(Renderable controlPoint) {
        this.controlPoints.add(controlPoint);
    }

    public void updateAnnotation(Position pos) {
        if (pos == null) {
            this.annotation.getAttributes().setVisible(false);
            return;
        }

        String displayString = this.getDisplayString(pos);

        if (displayString == null) {
            this.annotation.getAttributes().setVisible(false);
            return;
        }

        this.annotation.setText(displayString);

        this.annotation.setPosition(pos);
        this.annotation.getAttributes().setVisible(true);
    }

    protected String getDisplayString(Position pos) {
        String displayString = null;

        if (pos != null) {
            if (this.measureShapeType.equals(MeasureTool.SHAPE_CIRCLE) && this.shapeRectangle != null) {
                displayString = this.formatCircleMeasurements(pos);
            } else if (this.measureShapeType.equals(MeasureTool.SHAPE_SQUARE) && this.shapeRectangle != null) {
                displayString = this.formatSquareMeasurements(pos);
            } else if (this.measureShapeType.equals(MeasureTool.SHAPE_QUAD) && this.shapeRectangle != null) {
                displayString = this.formatQuadMeasurements(pos);
            } else if (this.measureShapeType.equals(MeasureTool.SHAPE_ELLIPSE) && this.shapeRectangle != null) {
                displayString = this.formatEllipseMeasurements(pos);
            } else if (this.measureShapeType.equals(MeasureTool.SHAPE_LINE) || this.measureShapeType.equals(MeasureTool.SHAPE_PATH)) {
                displayString = this.formatLineMeasurements(pos);
            } else if (this.measureShapeType.equals(MeasureTool.SHAPE_POLYGON)) {
                displayString = this.formatPolygonMeasurements(pos);
            }
        }

        return displayString;
    }

    protected String formatCircleMeasurements(Position pos) {
        StringBuilder sb = new StringBuilder();

        sb.append(this.unitsFormat.areaNL(this.getLabel(MeasureTool.AREA_LABEL), this.getArea()));
        sb.append(this.unitsFormat.lengthNL(this.getLabel(MeasureTool.PERIMETER_LABEL), this.getLength()));

        if (this.shapeRectangle != null) {
            sb.append(this.unitsFormat.lengthNL(this.getLabel(MeasureTool.RADIUS_LABEL), this.shapeRectangle.width / 2.0d));
        }

        if (this.getCenterPosition() != null && areLocationsRedundant(this.getCenterPosition(), pos)) {
            sb.append(
                this.unitsFormat.angleNL(this.getLabel(MeasureTool.CENTER_LATITUDE_LABEL), this.getCenterPosition().getLatitude()));
            sb.append(this.unitsFormat.angleNL(this.getLabel(MeasureTool.CENTER_LONGITUDE_LABEL),
                this.getCenterPosition().getLongitude()));
        }

        if (!this.areLocationsRedundant(pos, this.getCenterPosition())) {
            sb.append(this.unitsFormat.angleNL(this.getLabel(MeasureTool.LATITUDE_LABEL), pos.getLatitude()));
            sb.append(this.unitsFormat.angleNL(this.getLabel(MeasureTool.LONGITUDE_LABEL), pos.getLongitude()));
        }

        return sb.toString();
    }

    protected String formatEllipseMeasurements(Position pos) {
        StringBuilder sb = new StringBuilder();

        sb.append(this.unitsFormat.areaNL(this.getLabel(MeasureTool.AREA_LABEL), this.getArea()));
        sb.append(this.unitsFormat.lengthNL(this.getLabel(MeasureTool.PERIMETER_LABEL), this.getLength()));

        if (this.shapeRectangle != null) {
            sb.append(this.unitsFormat.lengthNL(this.getLabel(MeasureTool.MAJOR_AXIS_LABEL), this.shapeRectangle.width));
            sb.append(this.unitsFormat.lengthNL(this.getLabel(MeasureTool.MINOR_AXIS_LABEL), this.shapeRectangle.height));
        }

        if (this.getOrientation() != null) {
            sb.append(this.unitsFormat.angleNL(this.getLabel(MeasureTool.HEADING_LABEL), this.getOrientation()));
        }

        if (this.getCenterPosition() != null && areLocationsRedundant(this.getCenterPosition(), pos)) {
            sb.append(
                this.unitsFormat.angleNL(this.getLabel(MeasureTool.CENTER_LATITUDE_LABEL), this.getCenterPosition().getLatitude()));
            sb.append(this.unitsFormat.angleNL(this.getLabel(MeasureTool.CENTER_LONGITUDE_LABEL),
                this.getCenterPosition().getLongitude()));
        }

        if (!this.areLocationsRedundant(pos, this.getCenterPosition())) {
            sb.append(this.unitsFormat.angleNL(this.getLabel(MeasureTool.LATITUDE_LABEL), pos.getLatitude()));
            sb.append(this.unitsFormat.angleNL(this.getLabel(MeasureTool.LONGITUDE_LABEL), pos.getLongitude()));
        }

        return sb.toString();
    }

    protected String formatSquareMeasurements(Position pos) {
        StringBuilder sb = new StringBuilder();

        sb.append(this.unitsFormat.areaNL(this.getLabel(MeasureTool.AREA_LABEL), this.getArea()));
        sb.append(this.unitsFormat.lengthNL(this.getLabel(MeasureTool.PERIMETER_LABEL), this.getLength()));

        if (this.shapeRectangle != null) {
            sb.append(this.unitsFormat.lengthNL(this.getLabel(MeasureTool.WIDTH_LABEL), this.shapeRectangle.width));
        }

        if (this.getOrientation() != null) {
            sb.append(this.unitsFormat.angleNL(this.getLabel(MeasureTool.HEADING_LABEL), this.getOrientation()));
        }

        if (this.getCenterPosition() != null && areLocationsRedundant(this.getCenterPosition(), pos)) {
            sb.append(
                this.unitsFormat.angleNL(this.getLabel(MeasureTool.CENTER_LATITUDE_LABEL), this.getCenterPosition().getLatitude()));
            sb.append(this.unitsFormat.angleNL(this.getLabel(MeasureTool.CENTER_LONGITUDE_LABEL),
                this.getCenterPosition().getLongitude()));
        }

        if (!this.areLocationsRedundant(pos, this.getCenterPosition())) {
            sb.append(this.unitsFormat.angleNL(this.getLabel(MeasureTool.LATITUDE_LABEL), pos.getLatitude()));
            sb.append(this.unitsFormat.angleNL(this.getLabel(MeasureTool.LONGITUDE_LABEL), pos.getLongitude()));
        }

        return sb.toString();
    }

    protected String formatQuadMeasurements(Position pos) {
        StringBuilder sb = new StringBuilder();

        sb.append(this.unitsFormat.areaNL(this.getLabel(MeasureTool.AREA_LABEL), this.getArea()));
        sb.append(this.unitsFormat.lengthNL(this.getLabel(MeasureTool.PERIMETER_LABEL), this.getLength()));

        if (this.shapeRectangle != null) {
            sb.append(this.unitsFormat.lengthNL(this.getLabel(MeasureTool.WIDTH_LABEL), this.shapeRectangle.width));
            sb.append(this.unitsFormat.lengthNL(this.getLabel(MeasureTool.HEIGHT_LABEL), this.shapeRectangle.height));
        }

        if (this.getOrientation() != null) {
            sb.append(this.unitsFormat.angleNL(this.getLabel(MeasureTool.HEADING_LABEL), this.getOrientation()));
        }

        if (this.getCenterPosition() != null && areLocationsRedundant(this.getCenterPosition(), pos)) {
            sb.append(
                this.unitsFormat.angleNL(this.getLabel(MeasureTool.CENTER_LATITUDE_LABEL), this.getCenterPosition().getLatitude()));
            sb.append(this.unitsFormat.angleNL(this.getLabel(MeasureTool.CENTER_LONGITUDE_LABEL),
                this.getCenterPosition().getLongitude()));
        }

        if (!this.areLocationsRedundant(pos, this.getCenterPosition())) {
            sb.append(this.unitsFormat.angleNL(this.getLabel(MeasureTool.LATITUDE_LABEL), pos.getLatitude()));
            sb.append(this.unitsFormat.angleNL(this.getLabel(MeasureTool.LONGITUDE_LABEL), pos.getLongitude()));
        }

        return sb.toString();
    }

    protected String formatPolygonMeasurements(Position pos) {
        StringBuilder sb = new StringBuilder();

        sb.append(this.unitsFormat.areaNL(this.getLabel(MeasureTool.AREA_LABEL), this.getArea()));
        sb.append(this.unitsFormat.lengthNL(this.getLabel(MeasureTool.PERIMETER_LABEL), this.getLength()));

        if (this.getCenterPosition() != null && areLocationsRedundant(this.getCenterPosition(), pos)) {
            sb.append(
                this.unitsFormat.angleNL(this.getLabel(MeasureTool.CENTER_LATITUDE_LABEL), this.getCenterPosition().getLatitude()));
            sb.append(this.unitsFormat.angleNL(this.getLabel(MeasureTool.CENTER_LONGITUDE_LABEL),
                this.getCenterPosition().getLongitude()));
        }

        if (!this.areLocationsRedundant(pos, this.getCenterPosition())) {
            sb.append(this.unitsFormat.angleNL(this.getLabel(MeasureTool.LATITUDE_LABEL), pos.getLatitude()));
            sb.append(this.unitsFormat.angleNL(this.getLabel(MeasureTool.LONGITUDE_LABEL), pos.getLongitude()));
        }

        return sb.toString();
    }

    protected String formatLineMeasurements(Position pos) {
        // TODO: Compute the heading of individual path segments
        StringBuilder sb = new StringBuilder();

        sb.append(this.unitsFormat.lengthNL(this.getLabel(MeasureTool.LENGTH_LABEL), this.getLength()));

        Double accumLength = this.computeAccumulatedLength(pos);
        if (accumLength != null && accumLength >= 1 && !MeasureTool.lengthsEssentiallyEqual(this.getLength(), accumLength)) {
            sb.append(this.unitsFormat.lengthNL(this.getLabel(MeasureTool.ACCUMULATED_LABEL), accumLength));
        }

        if (this.getOrientation() != null) {
            sb.append(this.unitsFormat.angleNL(this.getLabel(MeasureTool.HEADING_LABEL), this.getOrientation()));
        }

        sb.append(this.unitsFormat.angleNL(this.getLabel(MeasureTool.LATITUDE_LABEL), pos.getLatitude()));
        sb.append(this.unitsFormat.angleNL(this.getLabel(MeasureTool.LONGITUDE_LABEL), pos.getLongitude()));

        return sb.toString();
    }

    protected Double computeAccumulatedLength(LatLon pos) {
        if (this.positions.size() <= 2) {
            return null;
        }

        double radius = this.wwd.model().getGlobe().getRadius();
        double distanceFromStart = 0;
        int segmentIndex = 0;
        LatLon pos1 = this.positions.get(segmentIndex);
        for (int i = 1; i < this.positions.size(); i++) {
            LatLon pos2 = this.positions.get(i);
            double segmentLength = LatLon.greatCircleDistance(pos1, pos2).radians() * radius;

            // Check whether the position is inside the segment
            double length1 = LatLon.greatCircleDistance(pos1, pos).radians() * radius;
            double length2 = LatLon.greatCircleDistance(pos2, pos).radians() * radius;
            if (length1 <= segmentLength && length2 <= segmentLength) {
                // Compute portion of segment length
                distanceFromStart += length1 / (length1 + length2) * segmentLength;
                break;
            } else {
                distanceFromStart += segmentLength;
            }
            pos1 = pos2;
        }

        double gcPathLength = this.computePathLength();

        return distanceFromStart < gcPathLength ? this.getLength() * (distanceFromStart / gcPathLength) : null;
    }

    protected double computePathLength() {
        double pathLengthRadians = 0;

        LatLon pos1 = null;
        for (LatLon pos2 : this.positions) {
            if (pos1 != null) {
                pathLengthRadians += LatLon.greatCircleDistance(pos1, pos2).radians();
            }
            pos1 = pos2;
        }

        return pathLengthRadians * this.wwd.model().getGlobe().getRadius();
    }

    protected boolean areLocationsRedundant(LatLon locA, LatLon locB) {
        if (locA == null || locB == null) {
            return false;
        }

        String aLat = this.unitsFormat.angleNL("", locA.getLatitude());
        String bLat = this.unitsFormat.angleNL("", locB.getLatitude());

        if (!aLat.equals(bLat)) {
            return false;
        }

        String aLon = this.unitsFormat.angleNL("", locA.getLongitude());
        String bLon = this.unitsFormat.angleNL("", locB.getLongitude());

        return aLon.equals(bLon);
    }

    protected static class CustomRenderableLayer extends RenderableLayer implements PreRenderable, Renderable {

        @Override
        public void render(DrawContext dc) {
            if (dc.isPickingMode() && !this.isPickEnabled()) {
                return;
            }
            if (!this.isEnabled()) {
                return;
            }

            super.render(dc);
        }
    }

    // *** Control points ***
    public static class ControlPoint extends GlobeAnnotation {

        final MeasureTool parent;

        public ControlPoint(Position position, AnnotationAttributes attributes, MeasureTool parent) {
            super("", position, attributes);
            this.parent = parent;
        }

        public MeasureTool getParent() {
            return this.parent;
        }
    }

    protected static class ControlPointWithLeader extends ControlPoint implements PreRenderable {

        protected final SurfacePolyline leaderLine;

        public ControlPointWithLeader(Position position, AnnotationAttributes controlPointAttributes,
            ShapeAttributes leaderAttributes, MeasureTool parent) {
            super(position, controlPointAttributes, parent);

            this.leaderLine = new SurfacePolyline(leaderAttributes);
        }

        @Override
        public void preRender(DrawContext dc) {
            if (dc == null) {
                String message = Logging.getMessage("nullValue.DrawContextIsNull");
                Logging.logger().severe(message);
                throw new IllegalArgumentException(message);
            }

            if (this.leaderLine != null) {
                this.leaderLine.preRender(dc);
            }
        }

        @Override
        public void render(DrawContext dc) {
            if (dc == null) {
                String message = Logging.getMessage("nullValue.DrawContextIsNull");
                Logging.logger().severe(message);
                throw new IllegalArgumentException(message);
            }

            if (this.leaderLine != null) {
                this.leaderLine.render(dc);
            }

            super.render(dc);
        }

        public void setLeaderLocations(LatLon begin, LatLon end) {
            if (begin == null) {
                String message = Logging.getMessage("nullValue.BeginIsNull");
                Logging.logger().severe(message);
                throw new IllegalArgumentException(message);
            }

            if (end == null) {
                String message = Logging.getMessage("nullValue.EndIsNull");
                Logging.logger().severe(message);
                throw new IllegalArgumentException(message);
            }

            this.leaderLine.setLocations(Arrays.asList(begin, end));
        }
    }
}