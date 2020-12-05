package netvr;

import gov.nasa.worldwind.BasicModel;
import gov.nasa.worldwind.formats.shapefile.*;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.layers.*;
import gov.nasa.worldwind.layers.earth.*;
import gov.nasa.worldwind.render.Polygon;
import gov.nasa.worldwind.video.LayerList;
import gov.nasa.worldwind.layers.sky.SkyGradientLayer;
import gov.nasa.worldwind.layers.sky.StarsLayer;
import gov.nasa.worldwind.render.*;
import gov.nasa.worldwind.util.VecBuffer;
import gov.nasa.worldwind.util.WWUtil;
import gov.nasa.worldwind.video.newt.WorldWindowNEWT;


import java.awt.*;
import java.io.File;
import java.util.*;

import static gov.nasa.worldwind.WorldWind.*;

public class WorldWindOSM {

    public WorldWindOSM() {
    }

    public static void main(String[] args) {
        mainNEWT();
        //mainAWT();
    }

    private static void mainNEWT() {

        final WorldWindowNEWT w = new WorldWindowNEWT(new OSMModel(), 1024, 800);
//        w.view().goTo(new Position(LatLon.fromDegrees(24.907,54.854), 0), 400);
    }

    static class OSMModel extends BasicModel {

        public OSMModel() {

            LayerList l = new LayerList();
            l.add(new StarsLayer());
            l.add(new SkyGradientLayer());

//            l.add(new OSMMapnikLayer());
            l.add(new BMNGWMSLayer());

            l.add(new ShapefileLayer(new Shapefile(new File("/tmp/shp1/places.shp"))));
            l.add(new ShapefileLayer(new Shapefile(new File("/tmp/shp1/roads.shp"))));
            l.add(new ShapefileLayer(new Shapefile(new File("/tmp/shp1/waterways.shp"))));
            l.add(new ShapefileLayer(new Shapefile(new File("/tmp/shp1/buildings.shp"))));
            l.add(new ShapefileLayer(new Shapefile(new File("/tmp/shp1/natural.shp"))));
            l.add(new ShapefileLayer(new Shapefile(new File("/tmp/shp1/landuse.shp"))));
            setLayers(l);

        }


    }


    public static class ShapefileLayer extends TextAndShapesLayer {
        static final OSMShapes[] shapeArray = new OSMShapes[]{
                new OSMShapes(Color.BLACK, 0.5D, 30000.0D), new OSMShapes(Color.GREEN, 0.5D, 100000.0D), new OSMShapes(Color.CYAN, 1.0D, 500000.0D),
                new OSMShapes(Color.YELLOW, 2.0D, 3000000.0D),
                new OSMShapes(Color.ORANGE, 0.25D, 10000.0D)
        };
        private final Shapefile shp;

        public ShapefileLayer(Shapefile shp) {
            this.shp = shp;

            setMaxActiveAltitude(20000);
            setPickEnabled(true);


            while (shp.hasNext()) {
                accept(shp.nextRecord());
            }

            for (OSMShapes ss : shapeArray) {
                if (!ss.locations.isEmpty()) {
                    SurfaceIcons l = surfaceIcons(ss, ss.locations);
                    l.setUseMipMaps(false);
                    add(l);
                }

                if (!ss.labels.isEmpty()) {
                    for (Label v : ss.labels)
                        addLabel(v);
                }
            }
        }

        private static SurfaceIcons surfaceIcons(OSMShapes ss, Iterable<? extends LatLon> locations) {
            SurfaceIcons l = new SurfaceIcons(
                    PatternFactory.createPattern("PatternFactory.PatternCircle",
                            0.8F, ss.foreground), locations);
            l.setMaxSize(100.0D * ss.scale);
            l.setMinSize(10.0D);
            l.setScale(ss.scale);
            l.setOpacity(0.8D);
            return l;
        }

        public void accept(ShapefileRecord r) {

            if (!include(r.getBoundingRectangle()))
                return;

            DBaseRecord attr = r.getAttributes();

            final Set<Map.Entry<String, Object>> meta = attr.getEntries();
            String metaString = meta.toString();

            if (r.isPolygonRecord()) {

                Object type = attr.get("type");
                if (type == null || type.toString().isEmpty())
                    return; //type = "";

                final ShapefileRecordPolygon R = r.asPolygonRecord();
                int parts = R.getNumberOfParts();
                for (int x = 0; x < parts; x++) {
                    Polygon p = new Polygon(positions(R, x, 1)) {
                    //SurfacePolygon p = new SurfacePolygon(positions(R, x)) {
                        @Override
                        public void pick(DrawContext dc, Point pickPoint) {
                            super.pick(dc, pickPoint);
                            System.out.println(metaString);
                        }
                    };
                    //Polygon p = new Polygon(positions(R, x));
                    //SurfacePolygons p = new SurfacePolygons(r.asPolygonRecord().getCompoundPointBuffer());
                    p.setDragEnabled(false);


//                    p.setOutlinePickWidth(0);

                    p.setAltitudeMode(RELATIVE_TO_GROUND);
                    final ShapeAttributes aa = new BasicShapeAttributes();
                    //aa.setOutlineWidth(3);
                    aa.setInteriorOpacity(0.5f);
                    aa.setDrawInterior(true);
                    aa.setDrawOutline(false);



                    aa.setInteriorMaterial(new Material(
                        Color.getHSBColor((Math.abs(type.hashCode()) % 1000)/1000f, 0.8f, 0.8f)
                        //new Color(1, 1, 1f)
                    ));

                    p.setAttributes(aa);
//                    p.setHighlightAttributes(aa);
//                    p.addPropertyChangeListener(e -> System.out.println(e));

                    add(p);
                }

            } else if (r.isPolylineRecord()) {

                //final SurfacePolylines P = new SurfacePolylines(r.asPolylineRecord().getCompoundPointBuffer());
                Path P = null;
                int pp = r.getNumberOfParts();
                if (pp > 1)
                    throw new UnsupportedOperationException();
                for (int i = 0; i < pp; i++) {
                    ArrayList<Position> l = positions(r, i);

                    if (l!=null) {
                        P = new Path(l) {
                            @Override
                            public void pick(DrawContext dc, Point pickPoint) {
                                super.pick(dc, pickPoint);
                                System.out.println(metaString);
                            }
                        };
                        P.setDragEnabled(false);
                        P.setOutlinePickWidth(1);
                        P.setFollowTerrain(true);
                        P.setSurfacePath(true);
//                        P.setShowPositions(true);
                        break;
                    }
                }
//                P.setDragEnabled(false);

                Object SPEED = attr.get("maxspeed");
                int speed;
                if (SPEED instanceof Long) {
                    speed = ((Long) SPEED).intValue();
                } else
                    speed = 0;

                final ShapeAttributes lineAttr = new BasicShapeAttributes();
                lineAttr.setOutlineWidth(3);
                lineAttr.setDrawInterior(false);
//                                            lineAttr.setOutlineOpacity(0.85f);
                lineAttr.setOutlineMaterial(new Material(new Color(Math.min(1, speed / 50f), 0, 0f)));

                P.setAttributes(lineAttr);

                //P.setTexelsPerEdgeInterval(10);
                add(P);

            } else if (r.isPointRecord()) {
                double[] pointCoords = ((ShapefileRecordPoint) r).getPoint();
                LatLon location = LatLon.fromDegrees(pointCoords[1], pointCoords[0]);

                String type = (String) attr.get("type");

                OSMShapes shapes;
                if (type.equalsIgnoreCase("hamlet")) {
                    shapes = shapeArray[0];
                } else if (type.equalsIgnoreCase("village")) {
                    shapes = shapeArray[1];
                } else if (type.equalsIgnoreCase("town")) {
                    shapes = shapeArray[2];
                } else if (type.equalsIgnoreCase("city")) {
                    shapes = shapeArray[3];
                } else
                    shapes = shapeArray[4];

                String name = null;
                if (meta != null) {
                    for (Map.Entry<String, Object> e : meta) {
                        if (e.getKey().equalsIgnoreCase("name")) {
                            name = (String) e.getValue();
                            break;
                        }
                    }
                }
                if (!WWUtil.isEmpty(name)) {
                    Label label = new Label(name, new Position(location, 0.0D));
                    label.setFont(shapes.font);
                    label.setColor(shapes.foreground);
                    label.setBackgroundColor(shapes.background);
                    label.setMaxActiveAltitude(shapes.labelMaxAltitude);
                    label.setPriority(shapes.labelMaxAltitude);
                    shapes.labels.add(label);
                } else {
                    shapes.locations.add(location);
                }

            } else {
                System.out.println("unknown: " + r);
            }

        }


        static private ArrayList<Position> positions(ShapefileRecord r, int i) {
            return positions(r, i, 0);
        }

        static private ArrayList<Position> positions(ShapefileRecord r, int i, float elevation) {
            ArrayList<Position> l;
            VecBuffer points = r.getPointBuffer(i);
            final int ps = points.getSize();
            if (ps > 0) {
                l = new ArrayList<>(ps);
                Iterable<LatLon> p = points.getLocations();
                p.forEach(q -> l.add(new Position(q, elevation)));
            }
            else l = null;
            return l;
        }

        protected static boolean include(double[] boundingRectangle) {
            return true;
        }

    }

//        public static Layer makeLayerFromOSMPlacesShapefile(Shapefile shp) {
//
//            TextAndShapesLayer layer = new TextAndShapesLayer();
//            layer.setPickEnabled(false);
//
//
//            while(true) {
//                ShapefileRecord record;
//                OSMShapes shapes;
//                Label label;
//                DBaseRecord attr = null;
//                Object o;
//                do {
//                    do {
//                        do {
//                            if (!shp.hasNext()) {
//
//
//                                return layer;
//                            }
//
//                            record = shp.nextRecord();
//                            if (record != null) {
//
//
//                            }
//                        } while(record == null);
//                    } while(!record.getShapeType().equals("gov.nasa.worldwind.formats.shapefile.Shapefile.ShapePoint"));
//
//                    o = attr.get("type");
//                } while(!(o instanceof String));
//
////                String type = (String)o;
////                if (type.equalsIgnoreCase("hamlet")) {
////                    shapes = shapeArray[0];
////                } else if (type.equalsIgnoreCase("village")) {
////                    shapes = shapeArray[1];
////                } else if (type.equalsIgnoreCase("town")) {
////                    shapes = shapeArray[2];
////                } else if (type.equalsIgnoreCase("city")) {
////                    shapes = shapeArray[3];
////                } else
////                    shapes = shapeArray[4];
//
////                String name = null;
////                if (attr.getEntries() != null) {
////                    for (Map.Entry<String, Object> e : attr.getEntries()) {
////                        if (e.getKey().equalsIgnoreCase("name")) {
////                            name = (String) e.getValue();
////                            break;
////                        }
////                    }
////                }
//
////                double[] pointCoords = ((ShapefileRecordPoint)record).getPoint();
////                LatLon location = LatLon.fromDegrees(pointCoords[1], pointCoords[0]);
////                if (!WWUtil.isEmpty(name)) {
////                    label = new Label(name, new Position(location, 0.0D));
////                    label.setFont(shapes.font);
////                    label.setColor(shapes.foreground);
////                    label.setBackgroundColor(shapes.background);
////                    label.setMaxActiveAltitude(shapes.labelMaxAltitude);
////                    label.setPriority(shapes.labelMaxAltitude);
////                    shapes.labels.add(label);
////                }
//
////                shapes.locations.add(location);
//            }
//        }


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


//    private static void mainAWT() {
//        EventQueue.invokeLater(() -> {
//            JFrame f = new JFrame();
//
//            WorldWindowGLCanvas wwd = new WorldWindowGLCanvas();
//            wwd.setPreferredSize(new Dimension(1000, 800));
//            f.getContentPane().add(wwd, BorderLayout.CENTER);
//
//            wwd.setModel(new OSMModel());
//
//            f.pack();
//            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//            f.setVisible(true);
//
//        });
//    }
}
