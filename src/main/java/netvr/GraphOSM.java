package netvr;

import com.carrotsearch.hppc.LongArrayList;
import com.graphhopper.reader.*;
import com.graphhopper.reader.osm.*;
import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.storage.*;

import java.io.File;

abstract public class GraphOSM extends GraphHopperOSM {
//    public final Dbvt<Vis> ways = new Dbvt<>();

    static GraphHopperStorage ramGraph(String directory, EncodingManager encodingManager, boolean is3D,
        boolean turnRestrictionsImport) {
        return new GraphHopperStorage(new RAMDirectory(directory, false),
            encodingManager, is3D, turnRestrictionsImport);
    }

    @Override
    public boolean load(String graphHopperFolder) {
        boolean l = super.load(graphHopperFolder); //HACK

        DataReader reader = new FullWayReader(
            GraphOSM.ramGraph("x", getEncodingManager(), true, false));

        reader.setFile(new File(getOSMFile()));
        //reader.setWorkerThreads(Runtime.getRuntime().availableProcessors());
        try {
            reader.readGraph();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return l; //disable caching
    }

    abstract public void add(Vis v);

    private static class FullWayReader extends OSMReader {
//        final NodeAccess nodeAccess = getGraphHopperStorage().getNodeAccess();

        public FullWayReader(GraphHopperStorage ghStorage) {
            super(ghStorage);
        }

        @Override
        protected void processRelation(ReaderRelation relation) {

        }

        @Override
        protected void processWay(ReaderWay way) {
            //super.processWay(way);
            final LongArrayList wn = way.getNodes();
            if (wn.size() > 2
                //way.getTag("building", null)!=null
            ) {
            }
        }
    }
}
