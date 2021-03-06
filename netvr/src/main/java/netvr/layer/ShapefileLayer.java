package netvr.layer;

import gov.nasa.worldwind.formats.shapefile.*;
import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.render.Polygon;
import gov.nasa.worldwind.render.*;
import gov.nasa.worldwind.util.*;

import java.awt.*;
import java.util.*;

import static gov.nasa.worldwind.WorldWind.RELATIVE_TO_GROUND;

public class ShapefileLayer extends TextAndShapesLayer {
    static final OSMShapes[] shapeArray = {
        new OSMShapes(Color.BLACK, 0.5D, 30000.0D),
        new OSMShapes(Color.GREEN, 0.5D, 100000.0D),
        new OSMShapes(Color.CYAN, 1.0D, 500000.0D),
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

        for (OSMShapes ss : ShapefileLayer.shapeArray) {
            if (!ss.locations.isEmpty()) {
                SurfaceIcons l = ShapefileLayer.surfaceIcons(ss, ss.locations);
                l.setUseMipMaps(false);
                add(l);
            }

            if (!ss.labels.isEmpty()) {
                for (Label v : ss.labels) {
                    addLabel(v);
                }
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

    static private ArrayList<Position> positions(ShapefileRecord r, int i) {
        return ShapefileLayer.positions(r, i, 0);
    }

    static private ArrayList<Position> positions(ShapefileRecord r, int i, float elevation) {
        ArrayList<Position> l;
        VecBuffer points = r.getPointBuffer(i);
        final int ps = points.getSize();
        if (ps > 0) {
            l = new ArrayList<>(ps);
            Iterable<LatLon> p = points.getLocations();
            p.forEach(q -> l.add(new Position(q, elevation)));
        } else
            l = null;
        return l;
    }

    protected static boolean include(double[] boundingRectangle) {
        return true;
    }

    public void accept(ShapefileRecord r) {

        if (!ShapefileLayer.include(r.getBoundingRectangle()))
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
                gov.nasa.worldwind.render.Polygon p = new Polygon(ShapefileLayer.positions(R, x, 1)) {
                    //SurfacePolygon p = new SurfacePolygon(positions(R, x)) {
                    @Override
                    public void pick(DrawContext dc, Point pickPoint) {
                        super.pick(dc, pickPoint);
                        System.out.println(metaString);
                    }
                };
                p.setDragEnabled(false);

//                    p.setOutlinePickWidth(0);

                p.setAltitudeMode(RELATIVE_TO_GROUND);
                final ShapeAttributes aa = new BasicShapeAttributes();
                //aa.setOutlineWidth(3);
                aa.setInteriorOpacity(0.5f);
                aa.setDrawInterior(true);
                aa.setDrawOutline(false);

                aa.setInteriorMaterial(new Material(
                    Color.getHSBColor((Math.abs(type.hashCode()) % 1000) / 1000f, 0.8f, 0.8f)
                    //new Color(1, 1, 1f)
                ));

                p.setAttributes(aa);

                add(p);
            }
        } else if (r.isPolylineRecord()) {

            //final SurfacePolylines P = new SurfacePolylines(r.asPolylineRecord().getCompoundPointBuffer());
            Path P = null;
            int pp = r.getNumberOfParts();
            if (pp > 1)
                throw new UnsupportedOperationException();
            for (int i = 0; i < pp; i++) {
                ArrayList<Position> l = ShapefileLayer.positions(r, i);

                if (l != null) {
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
                shapes = ShapefileLayer.shapeArray[0];
            } else if (type.equalsIgnoreCase("village")) {
                shapes = ShapefileLayer.shapeArray[1];
            } else if (type.equalsIgnoreCase("town")) {
                shapes = ShapefileLayer.shapeArray[2];
            } else if (type.equalsIgnoreCase("city")) {
                shapes = ShapefileLayer.shapeArray[3];
            } else
                shapes = ShapefileLayer.shapeArray[4];

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
            double eyeElevation = dc.view().getEyePosition().getElevation();
            return this.minActiveAltitude <= eyeElevation && eyeElevation <= this.maxActiveAltitude;
        }
    }
}