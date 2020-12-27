package netvr;

import com.carrotsearch.hppc.LongArrayList;
import com.graphhopper.reader.*;
import gov.nasa.worldwind.avlist.AVList;
import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.globes.*;
import gov.nasa.worldwind.layers.RenderableLayer;
import gov.nasa.worldwind.render.*;
import gov.nasa.worldwind.util.measure.AreaMeasurer;
import jcog.Util;
import org.eclipse.collections.impl.map.mutable.primitive.LongObjectHashMap;

import java.io.IOException;
import java.util.*;

import static gov.nasa.worldwind.WorldWind.RELATIVE_TO_GROUND;

public class AdaptiveOSMLayer extends RenderableLayer {

    /**
     * key for description data
     */
    public static final String DESCRIPTION = "_";
    /**
     * in degrees lat,lon
     */
    static final double gridRes = 0.002;
    private static final double DEFAULT_ELEVATION = 4;
    private static final double DEFAULT_ELEVATION_USE = 2;
    static final private Set<String> keysExcl = Set.of("area", "source", "image", "ref:bag", "source:date");
    public final LongObjectHashMap<ReaderWay> ways = new LongObjectHashMap<>();
    public final LongObjectHashMap<Map<String, String>> meta = new LongObjectHashMap<>();
    private final LongObjectHashMap<ReaderNode> nodes = new LongObjectHashMap<>();
    private final LongObjectHashMap<ReaderRelation> relations = new LongObjectHashMap<>();

    public final AdaptiveOSMLayer focus(LatLon at, float radiusDegrees) {
        return focus(new Sector(
            at.latitude - radiusDegrees, at.latitude + radiusDegrees,
            at.longitude - radiusDegrees, at.longitude + radiusDegrees));
    }

    public synchronized AdaptiveOSMLayer focus(Sector sector) {
        double latMin = Util.round(sector.latMin - AdaptiveOSMLayer.gridRes / 2, AdaptiveOSMLayer.gridRes);
        double lonMin = Util.round(sector.lonMin - AdaptiveOSMLayer.gridRes / 2, AdaptiveOSMLayer.gridRes);
        double latMax = Util.round(sector.latMax + AdaptiveOSMLayer.gridRes / 2, AdaptiveOSMLayer.gridRes);
        double lonMax = Util.round(sector.lonMax + AdaptiveOSMLayer.gridRes / 2, AdaptiveOSMLayer.gridRes);

        int latCells = Math.max(1, (int) Math.ceil((latMax - latMin) / AdaptiveOSMLayer.gridRes));
        int lonCells = Math.max(1, (int) Math.ceil((lonMax - lonMin) / AdaptiveOSMLayer.gridRes));
        for (int i = 0; i < latCells; i++) {
            final double latI = latMin + i * AdaptiveOSMLayer.gridRes;
            for (int j = 0; j < lonCells; j++) {
                final double lonJ = lonMin + j * AdaptiveOSMLayer.gridRes;
                _focus(new Sector(latI, latI + AdaptiveOSMLayer.gridRes, lonJ, lonJ + AdaptiveOSMLayer.gridRes));
            }
        }
        Globe g = new EarthFlat();

        renderables.sort((a, b) -> {
            if (a == b)
                return 0;
            final boolean ap = a instanceof Polygon;
            final boolean bp = b instanceof Polygon;
            if (ap && !bp)
                return +1;
            else if (bp && !ap)
                return -1;
            else if (ap && bp) {
                final double A = area((Polygon) a, g);
                final double B = area((Polygon) b, g);
                int da = Double.compare(B, A);
                if (da != 0)
                    return da;
            }

            return Integer.compare(System.identityHashCode(a), System.identityHashCode(b));
        });
        return this;
    }

    private double area(Polygon x, Globe g) {
        return new AreaMeasurer(x.outerBoundary()).getArea(g);
    }

    protected AdaptiveOSMLayer _focus(Sector sector) {

        try {
            OSMLoader.osm(sector, this::read);
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        return this;
    }

    private void read(ReaderElement z) {
        switch (z.getType()) {
            case ReaderElement.RELATION -> readRelation((ReaderRelation) z);
            case ReaderElement.NODE -> readNode(z);
            case ReaderElement.WAY -> readWay((ReaderWay) z);
        }
    }

    private void readNode(ReaderElement z) {
        nodes.put(z.getId(), (ReaderNode) z);
    }

    private void readRelation(ReaderRelation z) {
        ReaderRelation r = z;
    }

    private void readWay(ReaderWay z) {
        if (ways.put(z.getId(), z) != null)
            return; //already read

        //TODO use VarHandle to access private field 'properties'
        boolean landUse = false;
        Map<String, String> m = new HashMap();
        meta.put(z.getId(), m);
        for (String k : z.getKeysWithPrefix("")) {
            if (!AdaptiveOSMLayer.keysExcl.contains(k))
                m.put(k, z.getTag(k));
            if (!landUse && k.equals("landuse"))
                landUse = true;
        }

        LongArrayList wayNodes = z.getNodes();

        final int n = wayNodes.size();
        List<Position> latlon = new ArrayList(n);
        boolean closed = wayNodes.get(0) == wayNodes.get(n - 1);
        for (int i = 0; i < n; i++) {
            final long nodeId = wayNodes.get(i);
            final ReaderNode node = nodes.get(nodeId);
            //TODO ele

            double e = node.getEle();
            if (e != e)
                e = landUse ? AdaptiveOSMLayer.DEFAULT_ELEVATION_USE : AdaptiveOSMLayer.DEFAULT_ELEVATION;

            latlon.add(Position.fromDegrees(node.getLat(), node.getLon(), e));
        }

        Renderable p;
        if (closed)
            p = readArea(z, m, latlon);
        else
            p = readPath(z, m, latlon);

        if (p != null) {
            ((AVList) p).set(AdaptiveOSMLayer.DESCRIPTION, m);
            add(p);
        }
    }

    private Path readPath(ReaderWay w, Map<String, String> properties, List<Position> latlon) {
        Path p = new Path(latlon);
        p.setFollowTerrain(true);
        p.setSurfacePath(true);
        return p;
    }

    private Polygon readArea(ReaderWay w, Map<String, String> properties, List<Position> latlon) {

        Polygon p = new Polygon(latlon);/* {
                @Override
                public void pick(DrawContext dc, Point pickPoint) {
                    super.pick(dc, pickPoint);
                    if (!properties.isEmpty())
                        System.out.println(properties);
                }
            };*/
        p.setAltitudeMode(RELATIVE_TO_GROUND);

        Material m = null;
        if (m == null) {
            final String building = properties.get("building");
            if (building != null) {
                m = switch (building) {
                    case "house" -> Material.RED;
                    default -> Material.ORANGE;
                };
            }
        }
        if (m == null) {
            final String l = properties.get("landuse");
            if (l != null) {
                m = switch (l) {
                    case "grass", "farmland" -> Material.GREEN;
                    default -> m;
                };
            }
        }
        if (m == null) {
            final String s = properties.get("surface");
            if (s != null) {
                m = switch (s) {
                    case "grass", "cobblestone" -> Material.BLACK;
                    default -> m;
                };
            }
        }
        if (m == null)
            m = Material.GRAY;

        final BasicShapeAttributes a = new BasicShapeAttributes();
        a.setInteriorOpacity(0.5f);
        a.setInteriorMaterial(m);
        p.setAttributes(a);
        return p;
    }
}
