package netvr;

import jcog.data.map.CellMap;
import org.eclipse.collections.api.tuple.primitive.ObjectFloatPair;
import org.eclipse.collections.impl.map.mutable.primitive.ObjectFloatHashMap;
import spacegraph.SpaceGraph;
import spacegraph.space2d.container.*;
import spacegraph.space2d.container.graph.*;
import spacegraph.space2d.container.grid.Gridding;
import spacegraph.space2d.container.layout.Force2D;
import spacegraph.space2d.widget.button.PushButton;
import spacegraph.space2d.widget.menu.TabMenu;
import spacegraph.space2d.widget.text.*;
import spacegraph.space2d.widget.textedit.TextEdit;

import java.io.*;
import java.nio.charset.Charset;
import java.util.Map;

public class TagCloud<X> extends Graph2D<ObjectFloatPair<X>> {

    private final ObjectFloatHashMap<X> map;

    public TagCloud(ObjectFloatHashMap<X> t) {
        super();
        this.build((x) -> {

            String s = x.id.getOne().toString();

            x.set(new PushButton(new VectorLabel(s), () -> {
                SpaceGraph.window(new LabeledPane(
                    new BitmapLabel(s),
                    new Bordering().center(new TabMenu(Map.of(
                        "summary", () -> new EmptySurface(),
                        "become", () -> new EmptySurface(),
                        "define", () -> {
                            final String definition;
                            try {
                                final ByteArrayOutputStream b = new ByteArrayOutputStream();
                                PrintStream p = new PrintStream(b);
                                new WordNet("/home/me/wordnet30").walk(s, p);
                                definition = b.toString(Charset.defaultCharset());
                            }
                            catch (IOException e) {
                                throw new RuntimeException(e);
                            }

                            return new TextEdit(32, 32).text(definition);
                        },
                        "..", () -> new EmptySurface())
                    )).south(new Gridding(
                        PushButton.awesome("thumbs-down"),
                        PushButton.awesome("thumbs-up")
                    ))), 600, 500);
            }));
        });
        update(new Force2D<>());
        render(new Graph2DRenderer<>() {

            float max;

            @Override
            public void nodes(CellMap<ObjectFloatPair<X>, ? extends NodeVis<ObjectFloatPair<X>>> cells, GraphEditor<ObjectFloatPair<X>> edit) {
                max = t.isEmpty() ? 1 : t.max();
                Graph2DRenderer.super.nodes(cells, edit);
            }

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
