package netvr.layer;

import gov.nasa.worldwind.*;
import gov.nasa.worldwind.drag.*;
import gov.nasa.worldwind.event.SelectListener;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.globes.Globe;
import gov.nasa.worldwind.layers.MarkerLayer;
import gov.nasa.worldwind.render.*;
import gov.nasa.worldwind.render.markers.*;
import gov.nasa.worldwind.util.BasicDragger;
import jcog.event.Off;
import netvr.Focus;

import java.util.*;

public class FocusLayer extends MarkerLayer {

    static final BasicMarkerAttributes a = new BasicMarkerAttributes();
    static {
        a.setShapeType(BasicMarkerShape.CUBE);
        a.setMaterial(Material.ORANGE);
        a.setMarkerPixels(32);
        a.setOpacity(0.5f);
        a.setMaxMarkerSize(250000);
    }


    public FocusLayer(Focus f) {
//        List<Marker> handles = new ArrayList<>(handlePositions.size());
//        for (int i = 0; i < handlePositions.size(); i++) {
//            handles.add(new ShapeEditor.ControlPointMarker(new Position(handlePositions.get(i), 0), markerAttrs, i));
//        }

        super(List.of(new MyBasicMarker(f)));
        BasicMarker m = (BasicMarker) getMarkers().iterator().next(); //HACK

        this.setOverrideMarkerElevation(false);
        setPickEnabled(true);


    }

    private static class MyBasicMarker extends BasicMarker implements Draggable {
        public MyBasicMarker(Focus f) {
            super(f.pos, FocusLayer.a);
        }

        @Override
        public boolean isDragEnabled() {
            return true;
        }

        @Override
        public void drag(DragContext c) {
//            System.out.println(c);
            final View v = c.getView();

            Position where = v.computePositionFromScreenPoint(c.getPoint());
            double alt = c.getGlobe().elevation(where);

            setPosition(new Position(where.lat, where.lon, alt) );
        }

    }

//        @Override
//    protected void doPreRender(DrawContext dc) {
//        super.doPreRender(dc);
//        if (selectorOff == null) {
//            //dc.view().input().wwd().
//            WorldWindow w = dc.wwd();
//
//            SelectListener selector =
//                new BasicDragger(w);
////                (s) -> {
////                System.out.println(s);
////            };
//            w.addSelectListener(selector);
//            selectorOff = ()->w.removeSelectListener(selector); //HACK
//        }
//    }
//
//    @Override
//    public void dispose() {
//        selectorOff.close();
//        selectorOff = null;
//    }
}