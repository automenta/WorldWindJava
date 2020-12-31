package spacegraph.space2d.widget.meta;

import jcog.Util;
import org.eclipse.collections.api.tuple.primitive.ObjectFloatPair;
import org.eclipse.collections.impl.map.mutable.primitive.ObjectFloatHashMap;
import spacegraph.space2d.Surface;
import spacegraph.space2d.container.graph.*;
import spacegraph.space2d.container.layout.Force2D;
import spacegraph.space2d.widget.button.PushButton;
import spacegraph.space2d.widget.text.VectorLabel;

public class TagCloud<X> extends Graph2D<ObjectFloatPair<X>> {

    private final ObjectFloatHashMap<X> map;

    public TagCloud(ObjectFloatHashMap<X> t) {
        super();
        this.build((x) -> {
            x.set(new PushButton(new VectorLabel(x.id.getOne().toString())));
        });
        update(new Force2D<>());
        render(new Graph2DRenderer<>() {

            final float max = t.max();

            @Override
            public void node(NodeVis<ObjectFloatPair<X>> nodeVis, GraphEditor<ObjectFloatPair<X>> graphEditor) {
//                System.out.println(nodeVis);
                float pri = (nodeVis.id.getTwo() / max);
                nodeVis.pri = pri;
//                nodeVis.resize(pri, pri);
            }
        });
        set(t.keyValuesView());
        this.map = t;
    }

}
