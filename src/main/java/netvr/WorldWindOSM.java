package netvr;

import com.jogamp.opengl.*;
import edu.mit.jwi.Nullable;
import gov.nasa.worldwind.*;
import gov.nasa.worldwind.avlist.*;
import gov.nasa.worldwind.formats.geojson.GeoJSONPoint;
import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.layers.*;
import gov.nasa.worldwind.layers.earth.*;
import gov.nasa.worldwind.layers.sky.*;
import gov.nasa.worldwind.pick.PickedObject;
import gov.nasa.worldwind.render.*;
import gov.nasa.worldwind.render.Cylinder;
import gov.nasa.worldwind.util.WWUtil;
import gov.nasa.worldwind.video.LayerList;
import gov.nasa.worldwind.video.newt.WorldWindowNEWT;
import jcog.exe.Exe;
import spacegraph.layer.OrthoSurfaceGraph;
import spacegraph.space2d.Surface;
import spacegraph.space2d.container.*;
import spacegraph.space2d.container.grid.Gridding;
import spacegraph.space2d.container.unit.AspectAlign;
import spacegraph.space2d.widget.Widget;
import spacegraph.space2d.widget.button.*;
import spacegraph.space2d.widget.meta.*;
import spacegraph.space2d.widget.slider.FloatSlider;
import spacegraph.space2d.widget.textedit.TextEdit;
import spacegraph.video.*;

import javax.naming.directory.BasicAttributes;
import java.awt.*;
import java.awt.event.*;
import java.nio.DoubleBuffer;
import java.util.*;

public class WorldWindOSM {
    static {
        System.setProperty("java.awt.headless", "true");
    }



    public WorldWindOSM() {
    }

    public static void main(String[] args) {
        WorldWindOSM.mainNEWT();
        //mainAWT();
    }

    private static void mainNEWT() {
        JoglWindow j = new JoglWindow(1024, 800);

        final OSMModel world = new OSMModel();

        final WorldWindowNEWT w =
            //new WorldWindowNEWT(world, 1024, 800);
            new WorldWindowNEWT(world);

        w.setWindow(j);

        final TextEdit out = new TextEdit(64, 24);


        final PushButton scan = new PushButton("Scan", () -> {
        });
        Surface param = new Gridding(
            new TextEdit(16),
            new FloatSlider("A", 0.5f, 0, 1),
            new FloatSlider("B", 0.1f, 0, 1),
            new FloatSlider("C", 0.3f, 0, 1),
            new FloatSlider("D", 0.8f, 0, 1)
        );

        final BorderingView z = new BorderingView();
        z.north(param);
        z.west(new Gridding(
            z.togglerIcon("home", Surfaces.TODO),
            z.togglerIcon("cogs", ()-> new Gridding(
                world.getLayers().stream().map(ll ->
                    Splitting.column(
                        new FloatSlider((float) ll.getOpacity(), 0, 1).on(ll::setOpacity),
                        0.75f,
                        new CheckBox(ll.name(), ll::setEnabled).on(ll.isEnabled())
                    )
                )
            )), new Widget(out), scan));

        new OrthoSurfaceGraph(z, j);

        j.runLater(() -> {

            w.input().addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    if (e.getButton() == 1 && e.getClickCount() == 2) {
                        Position p = w.view().computePositionFromScreenPoint(e.getPoint());
                        WorldWindOSM.focus(world, w, p.longitude, p.latitude, 0.001f);
                    }
                }

                @Override
                public void mouseClicked(MouseEvent e) {
                }
            });
            w.addSelectListener(s -> {
                if (s.isRightClick()) {
                } else if (s.isLeftClick()) {
                    PickedObject top = s.getTopPickedObject();
                    if (top == null || top.isTerrain()) {
                    } else {
                        out.text(WorldWindOSM.describe(top));
                    }
                } else {
                    if (s.isRollover()) {
                        PickedObject top =
                            s.getTopPickedObject();
                        if (top != null)
                            System.out.println(WorldWindOSM.describe(top));
                    }
                }
            });
        });
    }

    private static void focus(OSMModel world, WorldWindowNEWT w, double lon, double lat, float rad) {
        Exe.runLater(() -> {
            world.osm.focus(
                LatLon.fromDegrees(lat, lon), rad
            );
            w.view().goTo(new Position(LatLon.fromDegrees(lat, lon), 0), 400);
        });
    }

    private static String describe(PickedObject x) {

        Object y = x.get();
        if (y instanceof AVList) {
            Object z = ((AVList) y).get(AdaptiveOSMLayer.DESCRIPTION);
            if (z != null)
                return z.toString();
        }
        return x.toString();
    }

    static class OSMModel extends BasicModel {

        public final RenderableLayer renderables = new RenderableLayer();
        public final AnnotationLayer notes;
        public final MarkerLayer markers;
        private final AdaptiveOSMLayer osm;

        public OSMModel() {
            super(new LayerList());
            LayerList l = getLayers();
            l.add(new StarsLayer());
            l.add(new SkyGradientLayer());

            l.add(new OSMMapnikLayer());
            l.add(new BMNGWMSLayer().setEnabled(false));

            osm = new AdaptiveOSMLayer();
            l.add(osm);

            markers = new MarkerLayer();
            l.add(markers);

            notes = new AnnotationLayer();
            l.add(notes);

            l.add(renderables);

            String earthquakeFeedUrl = "https://earthquake.usgs.gov/earthquakes/feed/v1.0/summary/2.5_week.geojson";

            l.add(new GeoJSONLayer("Earthquakes", earthquakeFeedUrl, new GeoJSON.GeoJSONRenderer() {

                protected static final long MILLISECONDS_PER_MINUTE = 60000;
                protected static final long MILLISECONDS_PER_HOUR = 60 * MILLISECONDS_PER_MINUTE;
                protected static final long MILLISECONDS_PER_DAY = 24 * MILLISECONDS_PER_HOUR;
                protected static final String USGS_EARTHQUAKE_MAGNITUDE = "mag";
                protected static final String USGS_EARTHQUAKE_PLACE = "place";
                protected static final String USGS_EARTHQUAKE_TIME = "time";

                private final Color[] eqColors =
                    {
                        Color.RED,
                        Color.ORANGE,
                        Color.YELLOW,
                        Color.GREEN,
                        Color.BLUE,
                        Color.GRAY,
                        Color.BLACK,
                    };

                private AnnotationAttributes eqAttributes;

                final long updateTime = System.currentTimeMillis();

                @Override
                protected void addRenderableForPoint(GeoJSONPoint geom, RenderableLayer l, AVList properties) {
                    addEarthquake(geom, l, properties);
                }

                private void addEarthquake(GeoJSONPoint geom, RenderableLayer layer, AVList p) {
                    if (eqAttributes == null) {
                        // Init default attributes for all eq
                        eqAttributes = new AnnotationAttributes();
                        eqAttributes.setLeader(AVKey.SHAPE_NONE);
                        eqAttributes.setDrawOffset(new Point(0, -16));
                        eqAttributes.setSize(new Dimension(32, 32));
                        eqAttributes.setBorderWidth(0);
                        eqAttributes.setCornerRadius(0);
                        eqAttributes.setBackgroundColor(new Color(0, 0, 0, 0));
                    }
                    Number eqTime = (Number) p.get(USGS_EARTHQUAKE_TIME);
                    int elapsedDays;
                    if (eqTime != null) {
                        // Compute days elapsed since earthquake event
                        elapsedDays = (int) ((this.updateTime - eqTime.longValue()) / MILLISECONDS_PER_DAY);

                        // Update latest earthquake event
//                        if (this.latestEq != null) {
//                            Number latestEqTime = (Number) this.latestEq.get(USGS_EARTHQUAKE_TIME);
//                            if (latestEqTime.longValue() < eqTime.longValue())
//                                this.latestEq = eq;
//                        } else {
//                            this.latestEq = eq;
//                        }
                    } else
                        elapsedDays = 6;

                    Number eqMagnitude = (Number) p.get(USGS_EARTHQUAKE_MAGNITUDE);

                    final double radMeters = eqMagnitude.doubleValue() * 32_000;
                    final Cylinder eq = new Cylinder(geom.getPosition(),
                        radMeters/2,
                        radMeters
                        );

//                    final SurfaceCircle eq = new SurfaceCircle(
//                        geom.getPosition(),
//                        radMeters);
                    final BasicShapeAttributes a = new BasicShapeAttributes();
                    eq.setAttributes(a);
                    a.setDrawOutline(false);
                    float alpha = 1f / (1+elapsedDays);
                    a.setInteriorMaterial(new Material(
                        new Color(0.5f + 0.5f*alpha, 0, 0)
                    ));
                    a.setInteriorOpacity(0.5f*alpha);
                    eq.setHighlightAttributes(a);

                    layer.add(eq);

//                    EqAnnotation eq = new EqAnnotation(geom.getPosition(), eqAttributes);
//                    eq.setAltitudeMode(WorldWind.CLAMP_TO_GROUND); // GeoJON point's 3rd coordinate indicates depth
//                    eq.setValues(properties);
//
//                    eq.getAttributes().setTextColor(eqColors[Math.min(elapsedDays, eqColors.length - 1)]);
//
//                    eq.getAttributes().setScale(eqMagnitude.doubleValue() / 4);
//
//                    layer.add(eq);
                }

                class EqAnnotation extends GlobeAnnotation {
                    // Override annotation drawing for a simple circle
                    private DoubleBuffer shapeBuffer;

                    public EqAnnotation(Position position, AnnotationAttributes defaults) {
                        super("", position, defaults);
                    }

                    protected void applyScreenTransform(DrawContext dc, int x, int y, int width, int height, double scale) {
                        double finalScale = scale * this.computeScale(dc);

                        GL2 gl = dc.getGL2(); // GL initialization checks for GL2 compatibility.
                        gl.glTranslated(x, y, 0);
                        gl.glScaled(finalScale, finalScale, 1);
                    }

                    protected void doDraw(DrawContext dc, int width, int height, double opacity, Position pickPosition) {
                        // Draw colored circle around screen point - use annotation's text color
                        if (dc.isPickingMode()) {
                            this.bindPickableObject(dc, pickPosition);
                        }

                        AbstractAnnotation.applyColor(dc, this.getAttributes().getTextColor(), 0.6 * opacity, true);

                        // Draw 32x32 shape from its bottom left corner
                        int size = 32;
                        if (this.shapeBuffer == null)
                            this.shapeBuffer = FrameFactory.createShapeBuffer(AVKey.SHAPE_ELLIPSE, size, size, 0, null);
                        GL2 gl = dc.getGL2(); // GL initialization checks for GL2 compatibility.
                        gl.glTranslatef(-size / 2.0f, -size / 2.0f, 0);
                        FrameFactory.drawBuffer(dc, GL.GL_TRIANGLE_FAN, this.shapeBuffer);
                    }
                }

            }));


/* //SHAPEFILE
    /tmp/shp1/buildings.shp  /tmp/shp1/places.shp    /tmp/shp1/roads.shp
    /tmp/shp1/landuse.shp    /tmp/shp1/points.shp    /tmp/shp1/waterways.shp
    /tmp/shp1/natural.shp    /tmp/shp1/railways.shp */
            setLayers(l);
        }
    }

    protected static class OSMShapes {
        public final Collection<LatLon> locations = new ArrayList();
        public final Collection<Label> labels = new ArrayList();
        public final Color foreground;
        public final Color background;
        public final Font font;
        public final double scale;
        public final double labelMaxAltitude;

        public OSMShapes(Color color, double scale, double labelMaxAltitude) {
            this.foreground = color;
            this.background = WWUtil.computeContrastingColor(color);
            this.font = new Font("Arial", 1, 10 + (int) (3.0D * scale));
            this.scale = scale;
            this.labelMaxAltitude = labelMaxAltitude;
        }
    }

    protected static class Label extends UserFacingText {
        protected double minActiveAltitude = -1.7976931348623157E308D;
        protected double maxActiveAltitude = 1.7976931348623157E308D;

        public Label(CharSequence text, Position position) {
            super(text, position);
        }

        public void setMinActiveAltitude(double altitude) {
            this.minActiveAltitude = altitude;
        }

        public void setMaxActiveAltitude(double altitude) {
            this.maxActiveAltitude = altitude;
        }

        public boolean isActive(DrawContext dc) {
            double eyeElevation = dc.getView().getEyePosition().getElevation();
            return this.minActiveAltitude <= eyeElevation && eyeElevation <= this.maxActiveAltitude;
        }
    }

    protected static class TextAndShapesLayer extends RenderableLayer {
        protected final Collection<GeographicText> labels = new ArrayList();
        protected final GeographicTextRenderer textRenderer = new GeographicTextRenderer();

        public TextAndShapesLayer() {
            this.textRenderer.setCullTextEnabled(true);
            this.textRenderer.setCullTextMargin(2);
            this.textRenderer.setDistanceMaxScale(2.0D);
            this.textRenderer.setDistanceMinScale(0.5D);
            this.textRenderer.setDistanceMinOpacity(0.5D);
            this.textRenderer.setEffect("gov.nasa.worldwind.avkey.TextEffectOutline");
        }

        public void addLabel(GeographicText label) {
            this.labels.add(label);
        }

        public void doRender(DrawContext dc) {
            super.doRender(dc);
            this.setActiveLabels(dc);
            this.textRenderer.render(dc, this.labels);
        }

        protected void setActiveLabels(DrawContext dc) {

            for (GeographicText text : this.labels) {
                if (text instanceof Label)
                    text.setVisible(((Label) text).isActive(dc));
            }
        }
    }
}
