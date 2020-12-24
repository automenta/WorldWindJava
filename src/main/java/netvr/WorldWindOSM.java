package netvr;

import gov.nasa.worldwind.BasicModel;
import gov.nasa.worldwind.avlist.AVList;
import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.layers.*;
import gov.nasa.worldwind.layers.earth.*;
import gov.nasa.worldwind.layers.sky.*;
import gov.nasa.worldwind.pick.PickedObject;
import gov.nasa.worldwind.render.*;
import gov.nasa.worldwind.util.WWUtil;
import gov.nasa.worldwind.video.LayerList;
import gov.nasa.worldwind.video.newt.WorldWindowNEWT;
import jcog.exe.Exe;
import spacegraph.layer.OrthoSurfaceGraph;
import spacegraph.space2d.Surface;
import spacegraph.space2d.container.Bordering;
import spacegraph.space2d.container.grid.Gridding;
import spacegraph.space2d.widget.Widget;
import spacegraph.space2d.widget.button.PushButton;
import spacegraph.space2d.widget.slider.FloatSlider;
import spacegraph.space2d.widget.textedit.TextEdit;
import spacegraph.video.*;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

public class WorldWindOSM {
    static {
        System.setProperty("java.awt.headless", "true");
    }

    public WorldWindOSM() {
    }

    public static void main(String[] args) {
        mainNEWT();
        //mainAWT();
    }

    private static void mainNEWT() {
        JoglWindow j = new JoglWindow(1024, 800);


        final OSMModel world = new OSMModel();

        final WorldWindowNEWT w =
            //new WorldWindowNEWT(world, 1024, 800);
            new WorldWindowNEWT(world);

        w.setWindow(j);

        final TextEdit out = new TextEdit(64,24);
        final TextEdit in = new TextEdit(24).onKeyPress((k) -> {

        });
        final PushButton scan = new PushButton("Scan", () -> {
        });
        Surface param = new Gridding(
            new FloatSlider("A", 0.5f, 0,1),
            new FloatSlider("B", 0.1f, 0,1),
            new FloatSlider("C", 0.3f, 0,1),
            new FloatSlider("D", 0.8f, 0,1),
            new FloatSlider("E", 0.1f, 0,1)
        );

        new OrthoSurfaceGraph(
            new Bordering()
                .north(param)
                .west(new Gridding(new Widget(in), new Widget(out), scan))
            , j);

        j.runLater(() -> {
//            Exe.runLater(()->{
//                //double lat = 53.00821, lon = 7.18812 + 0.0015;
//                double lon = -115.8195, lat = 37.2410;
//                focus(world, w, lon, lat, 0.005f);
//            });

            w.input().addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    if (e.getButton()==1 && e.getClickCount()==2) {
                        Position p = w.view().computePositionFromScreenPoint(e.getPoint());
                        focus(world, w, p.longitude, p.latitude, 0.001f);
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
//                        //System.out.println(top.position());
//                        final Position where = top.position();
//                        final BasicMarker m = new BasicMarker(
//                            where,
//                            new BasicMarkerAttributes(Material.ORANGE, BasicMarkerShape.CUBE, 1.0d, 10, 10)
//                        );
//
//                        GlobeAnnotation a = new GlobeAnnotation("AGL Annotation", where);
////                    a.getAttributes().setFrameShape(SHAPE_NONE);
//                        //a.getAttributes().setLeader();
//
////                    a.setAlwaysOnTop(true);
//                        world.notes.removeAllAnnotations();
//                        world.notes.addAnnotation(a);
//
////                    GlobeAnnotationBalloon a = new GlobeAnnotationBalloon("AGL Annotation", where);
////                    a.setAlwaysOnTop(true);
////                    world.renderables.add(a);
//
//                        world.markers.setMarkers(
//                            List.of(m)
//                        );
                    }else {
                        out.text(describe(top));
                    }
                } else {
                    if (s.isRollover()) {
                        PickedObject top =
                            s.getTopPickedObject();
                        if (top!=null)
                            System.out.println(describe(top));
                    }
                }
            });
        });
    }

    private static void focus(OSMModel world, WorldWindowNEWT w, double lon, double lat, float rad) {
        Exe.runLater(()->{
            world.osm.focus(
                LatLon.fromDegrees(lat, lon), rad
            );
            w.view().goTo(new Position(LatLon.fromDegrees(lat, lon), 0), 400);
        });
    }

    private static String describe(PickedObject x) {

        Object y = x.get();
        if (y instanceof AVList) {
            Object z = ((AVList)y).get(AdaptiveOSMLayer.DESCRIPTION);
            if (z!=null)
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

            LayerList l = new LayerList();
            l.add(new StarsLayer());
            l.add(new SkyGradientLayer());

            l.add(new OSMMapnikLayer());
//            l.add(new BMNGWMSLayer());

            osm = new AdaptiveOSMLayer();
            l.add(osm);

            markers = new MarkerLayer();
            l.add(markers);

            notes = new AnnotationLayer();
            l.add(notes);

            l.add(renderables);


/* //SHAPEFILE
    /tmp/shp1/buildings.shp  /tmp/shp1/places.shp    /tmp/shp1/roads.shp
    /tmp/shp1/landuse.shp    /tmp/shp1/points.shp    /tmp/shp1/waterways.shp
    /tmp/shp1/natural.shp    /tmp/shp1/railways.shp */
//            l.add(new ShapefileLayer(new Shapefile(new File("/tmp/shp1/buildings.shp"))));
//            l.add(new ShapefileLayer(new Shapefile(new File("/tmp/shp1/places.shp"))));
//            l.add(new ShapefileLayer(new Shapefile(new File("/tmp/shp1/roads.shp"))));
//            l.add(new ShapefileLayer(new Shapefile(new File("/tmp/shp1/landuse.shp"))));
//            l.add(new ShapefileLayer(new Shapefile(new File("/tmp/shp1/points.shp"))));
//            l.add(new ShapefileLayer(new Shapefile(new File("/tmp/shp1/waterways.shp"))));
//            l.add(new ShapefileLayer(new Shapefile(new File("/tmp/shp1/natural.shp"))));
//            l.add(new ShapefileLayer(new Shapefile(new File("/tmp/shp1/railways.shp"))));
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
