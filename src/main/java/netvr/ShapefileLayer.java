package netvr;

import gov.nasa.worldwind.formats.shapefile.*;
import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.render.*;
import gov.nasa.worldwind.render.Polygon;
import gov.nasa.worldwind.util.*;

import java.awt.*;
import java.util.*;

import static gov.nasa.worldwind.WorldWind.RELATIVE_TO_GROUND;

public class ShapefileLayer extends WorldWindOSM.TextAndShapesLayer {
    static final WorldWindOSM.OSMShapes[] shapeArray = new WorldWindOSM.OSMShapes[] {
        new WorldWindOSM.OSMShapes(Color.BLACK, 0.5D, 30000.0D),
        new WorldWindOSM.OSMShapes(Color.GREEN, 0.5D, 100000.0D),
        new WorldWindOSM.OSMShapes(Color.CYAN, 1.0D, 500000.0D),
        new WorldWindOSM.OSMShapes(Color.YELLOW, 2.0D, 3000000.0D),
        new WorldWindOSM.OSMShapes(Color.ORANGE, 0.25D, 10000.0D)
    };
    private final Shapefile shp;

    public ShapefileLayer(Shapefile shp) {
        this.shp = shp;

        setMaxActiveAltitude(20000);
        setPickEnabled(true);

        while (shp.hasNext()) {
            accept(shp.nextRecord());
        }

        for (WorldWindOSM.OSMShapes ss : shapeArray) {
            if (!ss.locations.isEmpty()) {
                SurfaceIcons l = surfaceIcons(ss, ss.locations);
                l.setUseMipMaps(false);
                add(l);
            }

            if (!ss.labels.isEmpty()) {
                for (WorldWindOSM.Label v : ss.labels) {
                    addLabel(v);
                }
            }
        }
    }

    private static SurfaceIcons surfaceIcons(WorldWindOSM.OSMShapes ss, Iterable<? extends LatLon> locations) {
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
                gov.nasa.worldwind.render.Polygon p = new Polygon(positions(R, x, 1)) {
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
        }
        else if (r.isPolylineRecord()) {

            //final SurfacePolylines P = new SurfacePolylines(r.asPolylineRecord().getCompoundPointBuffer());
            Path P = null;
            int pp = r.getNumberOfParts();
            if (pp > 1)
                throw new UnsupportedOperationException();
            for (int i = 0; i < pp; i++) {
                ArrayList<Position> l = positions(r, i);

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
            }
            else
                speed = 0;

            final ShapeAttributes lineAttr = new BasicShapeAttributes();
            lineAttr.setOutlineWidth(3);
            lineAttr.setDrawInterior(false);
//                                            lineAttr.setOutlineOpacity(0.85f);
            lineAttr.setOutlineMaterial(new Material(new Color(Math.min(1, speed / 50f), 0, 0f)));

            P.setAttributes(lineAttr);

            //P.setTexelsPerEdgeInterval(10);
            add(P);
        }
        else if (r.isPointRecord()) {
            double[] pointCoords = ((ShapefileRecordPoint) r).getPoint();
            LatLon location = LatLon.fromDegrees(pointCoords[1], pointCoords[0]);

            String type = (String) attr.get("type");

            WorldWindOSM.OSMShapes shapes;
            if (type.equalsIgnoreCase("hamlet")) {
                shapes = shapeArray[0];
            }
            else if (type.equalsIgnoreCase("village")) {
                shapes = shapeArray[1];
            }
            else if (type.equalsIgnoreCase("town")) {
                shapes = shapeArray[2];
            }
            else if (type.equalsIgnoreCase("city")) {
                shapes = shapeArray[3];
            }
            else
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
                WorldWindOSM.Label label = new WorldWindOSM.Label(name, new Position(location, 0.0D));
                label.setFont(shapes.font);
                label.setColor(shapes.foreground);
                label.setBackgroundColor(shapes.background);
                label.setMaxActiveAltitude(shapes.labelMaxAltitude);
                label.setPriority(shapes.labelMaxAltitude);
                shapes.labels.add(label);
            }
            else {
                shapes.locations.add(location);
            }
        }
        else {
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
        else
            l = null;
        return l;
    }

    protected static boolean include(double[] boundingRectangle) {
        return true;
    }
}
