package netvr;

import com.jogamp.opengl.GL2;
import gov.nasa.worldwind.BasicModel;
import gov.nasa.worldwind.avlist.KV;
import gov.nasa.worldwind.event.SelectListener;
import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.layers.RenderableLayer;
import gov.nasa.worldwind.layers.earth.*;
import gov.nasa.worldwind.layers.sky.*;
import gov.nasa.worldwind.layers.tool.LatLonGraticuleLayer;
import gov.nasa.worldwind.pick.PickedObject;
import gov.nasa.worldwind.render.*;
import gov.nasa.worldwind.util.*;
import gov.nasa.worldwind.video.LayerList;
import gov.nasa.worldwind.video.newt.WorldWindowNEWT;
import jcog.exe.Exe;
import jcog.thing.*;
import netvr.layer.*;
import org.eclipse.collections.impl.map.mutable.primitive.ObjectFloatHashMap;
import org.jetbrains.annotations.*;
import spacegraph.layer.OrthoSurfaceGraph;
import spacegraph.space2d.Surface;
import spacegraph.space2d.container.*;
import spacegraph.space2d.container.grid.Gridding;
import spacegraph.space2d.widget.Widget;
import spacegraph.space2d.widget.button.*;
import spacegraph.space2d.widget.meta.ObjectSurface;
import spacegraph.space2d.widget.slider.FloatSlider;
import spacegraph.space2d.widget.text.*;
import spacegraph.video.JoglWindow;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.function.Consumer;

public class NetVR extends Thing<NetVR,String> {

    private final JoglWindow j;
    private final NetVRModel world;
    private final WorldWindowNEWT window;

    final BorderingView z = new BorderingView();
    final OrthoSurfaceGraph o;

    abstract static class NMode extends Part<NetVR> {
        public abstract String name();
        public abstract String icon();
        public abstract Object menu();

        @Nullable public abstract Extent extent();

        //TODO serialize/deserialize to byte[]
    }

    /** primary focus */
    NMode focus = null;

    /** ambient focuses */
    //final ConcurrentFastIteratingHashMap<String,NMode> modes = new ConcurrentFastIteratingHashMap<>(new NMode[0]);

    public synchronized void setFocus(NMode next) {
        NMode prev = this.focus;
        if (next==prev) return;
        if (prev!=null)
            remove(prev);

        Surface nextNorth;
        if (next!=null) {
        add(next);

        final Object oo = next.menu();
        final Surface OO = oo instanceof Surface ? (Surface)oo : (oo != null ? new ObjectSurface(oo) : new BitmapLabel(next.name()));

            nextNorth = new Widget(OO);
        } else
            nextNorth = null;

        z.north(nextNorth);
        focus = next;
    }

    public void add(NMode f) {
        add(f.name(), f);
    }

    public static class WhereIs extends NMode {

        final Object target;
        private final Consumer<Object> where;
        private WorldWindowNEWT w;

        public WhereIs(Object target, Consumer<Object> where) {
            this.target = target;
            this.where = where;
        }

        @Override
        public String name() {
            return "where is " + target;
        }

        @Override
        public String icon() {
            return null;
        }

        @Override
        public Object menu() {
            return null;
        }

        @Nullable
        @Override
        public Extent extent() {
            //TODO previous known location? ex: undo
            return null;
        }

        protected void select(Position p) {
            where.accept(p);
        }

        protected void select(PickedObject p) {
            where.accept(p);
        }

        final LatLonGraticuleLayer grat = new LatLonGraticuleLayer();

        final MouseAdapter l1 = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.getButton() == 1 && e.getClickCount() == 2) {
                    Position p = w.view().computePositionFromScreenPoint(e.getPoint());
                    select(p);
                    //NetVR.focus((NetVRModel) w.model(), w, p.longitude, p.latitude, 0.001f);
                }
            }
        };
        final SelectListener l2 = s -> {
            if (s.isRightClick()) {
            } else if (s.isLeftClick()) {
                PickedObject top = s.getTopPickedObject();
                if (top == null || top.isTerrain()) {
                } else {
//                    out.text(NetVR.describe(top));
                    select(top);
                }
            } else {
//                if (s.isRollover()) {
//                    PickedObject top =
//                        s.getTopPickedObject();
//                    if (top != null)
//                        System.out.println(NetVR.describe(top));
//                }
            }
        };

        @Override
        protected void start(NetVR n) {

            n.world.layers.add(grat);

            w = n.window;
            w.input().addMouseListener(l1);
            w.addSelectListener(l2);
        }

        @Override
        protected void stop(NetVR n) {
            n.world.layers.remove(grat);
            w.input().removeMouseListener(l1);
            w.removeSelectListener(l2);
            w = null;
        }
    }

    public static void main(String[] args) {
        var n = new NetVR();
        n.add(new NMode() {

            @Override
            public String name() {
                return "ui"; /* base */
            }

            @Override
            public String icon() {
                return null;
            }

            @Override
            public Object menu() {
                return null;
            }

            @Nullable
            @Override
            public Extent extent() {
                return null;
            }

            @Override
            protected void start(NetVR N) {
                var world = N.world;
                var w = N.window;

//                final TextEdit out = new TextEdit(64, 24);
//
//                final PushButton scan = new PushButton("Scan", () -> {
//                });

                var z = N.z;
//                Surface param = new Gridding(
//                    new TextEdit(16),
//                    new FloatSlider("A", 0.5f, 0, 1),
//                    new FloatSlider("B", 0.1f, 0, 1),
//                    new FloatSlider("C", 0.3f, 0, 1),
//                    new FloatSlider("D", 0.8f, 0, 1)
//                );

//                z.north(param);
                z.northwest(new Gridding(
                    z.togglerIcon("home", ()->{
                        return LabeledPane.the("Me", new Gridding(
                            new PushButton("Name"),
                            new PushButton("Where", ()-> {
                                n.setFocus(new WhereIs("user", where -> {
                                    n.setFocus(null);
                                    System.out.println("user at " + where);
                                }));
                            })
                        ));
                    }),
                    z.togglerIcon("bullseye", ()->{
                        ObjectFloatHashMap<String> t = w.tagsInView();
                        return new TagCloud(t); //HACK TODO
                    }),
                    z.togglerIcon("cogs", () -> new Gridding(
                        world.layers.stream().map(NetVR::layerWidget)
                    ))
                    //, new Widget(out), scan
                ));


//                j.runLater(() -> {
//            o.keyboard.focus(z);
//
//                    final MouseAdapter l1 = new MouseAdapter() {
//                        @Override
//                        public void mousePressed(MouseEvent e) {
//                            if (e.getButton() == 1 && e.getClickCount() == 2) {
//                                Position p = w.view().computePositionFromScreenPoint(e.getPoint());
//                                NetVR.focus(world, w, p.longitude, p.latitude, 0.001f);
//                            }
//                        }
//                    };
//                    final SelectListener l2 = s -> {
//                        if (s.isRightClick()) {
//                        } else if (s.isLeftClick()) {
//                            PickedObject top = s.getTopPickedObject();
//                            if (top == null || top.isTerrain()) {
//                            } else {
//                                out.text(NetVR.describe(top));
//                            }
//                        } else {
//                            if (s.isRollover()) {
//                                PickedObject top =
//                                    s.getTopPickedObject();
//                                if (top != null)
//                                    System.out.println(NetVR.describe(top));
//                            }
//                        }
//                    };
//
//                    w.input().addMouseListener(l1);
//                    w.addSelectListener(l2);
//                });

            }

            @Override
            protected void stop(NetVR netVR) {

            }
        });

    }

    @NotNull
    private static Splitting<?, ?> layerWidget(Layer ll) {
        return Splitting.column(
            new FloatSlider((float) ll.getOpacity(), 0, 1).on(ll::setOpacity),
            0.75f,
            new CheckBox(ll.name(), ll::setEnabled).on(ll.isEnabled())
        );
    }

    public NetVR() {
        super();
        j = new JoglWindow(1024, 800);

        world = new NetVRModel();

        window = new WorldWindowNEWT(world) {
            @Override
            public void init(GL2 g) {
                super.init(g);
                SelectListener selector = new BasicDragger(this);
                addSelectListener(selector);
            }
        };
        window.setWindow(j);

        o = new OrthoSurfaceGraph(z, j);




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
        if (y instanceof KV) {
            Object z = ((KV) y).get(AdaptiveOSMLayer.DESCRIPTION);
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
            LayerList l = this.layers;
            l.add(new StarsLayer());
            l.add(new SkyGradientLayer());

            l.add(new OSMMapnikLayer());
            l.add(new BMNGWMSLayer().setEnabled(false));
            l.add(new LandsatI3WMSLayer().setEnabled(false));

            osm = new AdaptiveOSMLayer();
            l.add(osm);

            Focus f = new Focus(new Position(35, -80, 0));
            l.add(new FocusLayer(f));

//            markers = new MarkerLayer();
//            l.add(markers);
//
//            notes = new AnnotationLayer();
//            l.add(notes);
//
//            l.add(renderables);

            l.add(new USGSEarthquakeLayer("Earthquakes",
                "https://earthquake.usgs.gov/earthquakes/feed/v1.0/summary/2.5_week.geojson"));


/* //SHAPEFILE
    /tmp/shp1/buildings.shp  /tmp/shp1/places.shp    /tmp/shp1/roads.shp
    /tmp/shp1/landuse.shp    /tmp/shp1/points.shp    /tmp/shp1/waterways.shp
    /tmp/shp1/natural.shp    /tmp/shp1/railways.shp */
        }


    }

}