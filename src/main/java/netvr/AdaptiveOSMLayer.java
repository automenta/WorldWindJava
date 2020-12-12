package netvr;

import com.carrotsearch.hppc.*;
import com.graphhopper.reader.*;
import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.layers.RenderableLayer;
import gov.nasa.worldwind.render.*;
import gov.nasa.worldwind.render.markers.BasicMarkerShape;

import java.io.IOException;
import java.util.*;

public class AdaptiveOSMLayer extends RenderableLayer {


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
                            List<LatLon> latlon = new ArrayList(n);
                            boolean closed = wayNodes.get(0)==wayNodes.get(n-1);
                            for (int i = 0; i < n; i++) {
                                final long nodeId = wayNodes.get(i);
                                final ReaderNode node = nodes.get(nodeId);
                                //TODO ele
                                latlon.add(LatLon.fromDegrees(node.getLat(), node.getLon()));
                            }
                            if (closed) {
                                SurfacePolygon p = new SurfacePolygon(latlon);
                                add(p);
                            } else {
                                Path p = new Path(latlon, 0);
                                p.setFollowTerrain(true);
                                p.setSurfacePath(true);
                                add(p);
                            }
                            //System.out.println(z);
                            break;
                    }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }

        return this;
    }
}
