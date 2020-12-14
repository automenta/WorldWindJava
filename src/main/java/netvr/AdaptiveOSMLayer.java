package netvr;

import com.carrotsearch.hppc.*;
import com.graphhopper.reader.*;
import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.layers.RenderableLayer;
import gov.nasa.worldwind.render.*;
import gov.nasa.worldwind.render.Polygon;

import java.awt.*;
import java.io.IOException;
import java.util.*;
import java.util.List;

import static gov.nasa.worldwind.WorldWind.RELATIVE_TO_GROUND;

public class AdaptiveOSMLayer extends RenderableLayer {

    static final private Set<String> keysExcl = Set.of("area", "source", "image", "ref:bag", "source:date");

    public final AdaptiveOSMLayer focus(LatLon at, float radiusDegrees) {
        return focus(new Sector(
            at.latitude-radiusDegrees,at.latitude+ radiusDegrees,
            at.longitude-radiusDegrees, at.longitude+radiusDegrees));
    }

    public AdaptiveOSMLayer focus(Sector sector) {

        LongObjectHashMap<ReaderNode> nodes = new LongObjectHashMap();
        try {
            OSMLoader.osm(sector,
                z->{
                    switch (z.getType()) {
                        case ReaderElement.RELATION:
                            ReaderRelation r = (ReaderRelation)z;
                            break;
                        case ReaderElement.NODE:
                            nodes.put(z.getId(), (ReaderNode) z);
                            break;
                        case ReaderElement.WAY:
                            final ReaderWay w = (ReaderWay) z;
                            LongArrayList wayNodes = w.getNodes();

                            final int n = wayNodes.size();
                            List<Position> latlon = new ArrayList(n);
                            boolean closed = wayNodes.get(0)==wayNodes.get(n-1);
                            for (int i = 0; i < n; i++) {
                                final long nodeId = wayNodes.get(i);
                                final ReaderNode node = nodes.get(nodeId);
                                //TODO ele

                                double e = node.getEle();
                                if (e!=e) e = 1;

                                latlon.add(Position.fromDegrees(node.getLat(), node.getLon(), e));
                            }
                            if (closed) {
                                //SurfacePolygon p = new SurfacePolygon(latlon);

                                //TODO use VarHandle to access private field 'properties'
                                Map<String,String> properties = new HashMap();
                                for (String k : w.getKeysWithPrefix("")) {
                                    if (!keysExcl.contains(k))
                                        properties.put(k, w.getTag(k));
                                }

                                Polygon p = new Polygon(latlon) {
                                    @Override
                                    public void pick(DrawContext dc, Point pickPoint) {
                                        super.pick(dc, pickPoint);
                                        System.out.println(properties);
                                    }
                                };
                                p.setAltitudeMode(RELATIVE_TO_GROUND);

                                Material m = null;
                                if (m==null) {
                                    final String building = properties.get("building");
                                    if (building!=null) {
                                        switch (building) {
                                            case "house":
                                                m = Material.RED;
                                                break;
                                            default:
                                                m = Material.ORANGE;
                                                break;
                                        }
                                    }
                                }
                                if (m==null) {
                                    final String l = properties.get("landuse");
                                    if (l!=null) {
                                        switch (l) {
                                            case "grass":
                                            case "farmland":
                                                m = Material.GREEN;
                                                break;
                                        }
                                    }
                                }
                                if (m==null) {
                                    final String s = properties.get("surface");
                                    if (s!=null) {
                                        switch (s) {
                                            case "grass":
                                            case "cobblestone":
                                                m = Material.BLACK;
                                                break;
                                        }
                                    }
                                }
                                if (m == null)
                                    m = Material.GRAY;

                                final BasicShapeAttributes a = new BasicShapeAttributes();
                                a.setInteriorOpacity(0.5f);
                                a.setInteriorMaterial(m);
                                p.setAttributes(a);
                                add(p);
                            } else {
                                Path p = new Path(latlon);
                                p.setFollowTerrain(true);
                                p.setSurfacePath(true);
                                add(p);
                            }
                            //System.out.println(z);
                            break;
                    }
            });
//
//            Globe g =new EarthFlat();
//
//            renderables.sort((a,b) -> {
//                if (a==b) return 0;
//                if (a instanceof SurfacePolygon && b instanceof SurfacePolygon) {
//                    int da = Double.compare(
//                        ((SurfacePolygon)b).getArea(g),
//                        ((SurfacePolygon)a).getArea(g)
//                    );
//                    if (da!=0) return da;
//                }
//
//                return Integer.compare(System.identityHashCode(a), System.identityHashCode(b));
//            });
        } catch (IOException e) {
            e.printStackTrace();
        }

        return this;
    }
}
