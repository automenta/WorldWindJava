//package netvr;
//
////import com.bulletphysics.collision.broadphase.DbvtAabbMm;
//import com.carrotsearch.hppc.LongContainer;
//import com.graphhopper.coll.LongIntMap;
//import com.graphhopper.util.PointAccess;
//import com.jogamp.opengl.GL2;
//
////import javax.vecmath.Vector3f;
//import java.util.*;
//
//public class WayVis extends Vis {
//
//
//    private final long[] nids;
//    final List<Vector3f> poly;
//
//    public WayVis(LongContainer nodes, LongIntMap nodeMap, PointAccess n) {
//
//        this.nids = nodes.toArray();
//        poly = new ArrayList<>(nids.length);
//        for (long X : nids) {
//            int x = getInternalNodeIdOfOsmNode(X, nodeMap);
//            if (x < 0) continue;
//            double lon = n.getLon(x), lat = n.getLat(x);
//            poly.add(new Vector3f((float)lon, (float)lat, 0));
//        }
//    }
//    static int getInternalNodeIdOfOsmNode(long nodeOsmId, LongIntMap nodeMap) {
//        int id = nodeMap.get(nodeOsmId);
//        return id < -2 ? -id - 3 : -1;
//    }
//
////    @Override
////    public DbvtAabbMm box() {
////        return new DbvtAabbMm().merge(poly);
////    }
//
//    @Override
//    public void draw(GL2 gl) {
//        gl.glBegin(GL2.GL_POLYGON);
//        gl.glColor3f(0.5f, 0.5f, 0.5f);
//        for (Vector3f v : poly)
//            gl.glVertex3f(v.x, v.y, v.z);
//
//        gl.glEnd();
//    }
//}
