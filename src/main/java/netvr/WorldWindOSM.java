package netvr;

import gov.nasa.worldwind.BasicModel;
import gov.nasa.worldwind.avlist.AVList;
import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.layers.RenderableLayer;
import gov.nasa.worldwind.layers.earth.*;
import gov.nasa.worldwind.layers.sky.*;
import gov.nasa.worldwind.pick.PickedObject;
import gov.nasa.worldwind.render.*;
import gov.nasa.worldwind.util.WWUtil;
import gov.nasa.worldwind.video.LayerList;
import gov.nasa.worldwind.video.newt.WorldWindowNEWT;
import jcog.exe.Exe;
import netvr.layer.*;
import org.eclipse.collections.impl.map.mutable.primitive.ObjectFloatHashMap;
import spacegraph.layer.OrthoSurfaceGraph;
import spacegraph.space2d.Surface;
import spacegraph.space2d.container.*;
import spacegraph.space2d.container.grid.Gridding;
import spacegraph.space2d.widget.Widget;
import spacegraph.space2d.widget.button.*;
import spacegraph.space2d.widget.meta.TagCloud;
import spacegraph.space2d.widget.slider.FloatSlider;
import spacegraph.space2d.widget.textedit.TextEdit;
import spacegraph.video.JoglWindow;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

public class WorldWindOSM {
//    static {
//        System.setProperty("java.awt.headless", "true");
//    }



    public WorldWindOSM() {
    }

    public static void main(String[] args) {
        WorldWindOSM.mainNEWT();
        //mainAWT();
    }

    private static void mainNEWT() {
        JoglWindow j = new JoglWindow(1024, 800);

        final NetVRModel world = new NetVRModel();

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
            z.togglerIcon("home", ()->{
                ObjectFloatHashMap<String> t = w.tagsInView();
                return new TagCloud(t); //HACK TODO
            }),
            z.togglerIcon("cogs", ()-> new Gridding(
                world.getLayers().stream().map(ll ->
                    Splitting.column(
                        new FloatSlider((float) ll.getOpacity(), 0, 1).on(ll::setOpacity),
                        0.75f,
                        new CheckBox(ll.name(), ll::setEnabled).on(ll.isEnabled())
                    )
                )
            ))
            , new Widget(out), scan));

        var o = new OrthoSurfaceGraph(z, j);

        j.runLater(() -> {
//            o.keyboard.focus(z);

            w.input().addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    if (e.getButton() == 1 && e.getClickCount() == 2) {
                        Position p = w.view().computePositionFromScreenPoint(e.getPoint());
                        WorldWindOSM.focus(world, w, p.longitude, p.latitude, 0.001f);
                    }
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


    private static void focus(NetVRModel world, WorldWindowNEWT w, double lon, double lat, float rad) {
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

    static class NetVRModel extends BasicModel {

//        public final RenderableLayer renderables = new RenderableLayer();
//        public final AnnotationLayer notes;
//        public final MarkerLayer markers;
        private final AdaptiveOSMLayer osm;

        public NetVRModel() {
            super(new LayerList());
            LayerList l = getLayers();
            l.add(new StarsLayer());
            l.add(new SkyGradientLayer());

            l.add(new OSMMapnikLayer());
            l.add(new BMNGWMSLayer().setEnabled(false));
            l.add(new LandsatI3WMSLayer().setEnabled(false));

            osm = new AdaptiveOSMLayer();
            l.add(osm);

//            markers = new MarkerLayer();
//            l.add(markers);
//
//            notes = new AnnotationLayer();
//            l.add(notes);
//
//            l.add(renderables);

            String earthquakeFeedUrl = "https://earthquake.usgs.gov/earthquakes/feed/v1.0/summary/2.5_week.geojson";

            l.add(new USGSEarthquakeLayer("Earthquakes", earthquakeFeedUrl));


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
