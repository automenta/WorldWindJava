package netvr;

//import com.bulletphysics.collision.broadphase.DbvtAabbMm;

import com.graphhopper.util.EdgeIteratorState;
import com.jogamp.opengl.GL2;

//import javax.vecmath.Vector3f;

public class EdgeVis extends Vis {
    final int id;
    final float lat, lon, lat2, lon2;
    final float[] color = new float[3];
    float thick = 1;

    public EdgeVis(EdgeIteratorState edge, double lat, double lon, double lat2, double lon2) {
        this.id = edge.getEdge();
        this.lat = (float) lat;
        this.lon = (float) lon;
        this.lat2 = (float) lat2;
        this.lon2 = (float) lon2;
    }

    @Override
    public void draw(GL2 gl) {
        gl.glBegin(GL2.GL_LINES);
        gl.glColor3fv(color, 0);
        gl.glVertex3f(lon, lat, 0);
        gl.glVertex3f(lon2, lat2, 0);
        gl.glEnd();
    }
}
