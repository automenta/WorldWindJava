package netvr;

import com.carrotsearch.hppc.procedures.IntProcedure;
import com.graphhopper.coll.GHIntHashSet;
import org.semanticweb.yars.nx.*;
import org.semanticweb.yars.nx.parser.*;
import org.semanticweb.yars.turtle.TurtleParser;

import java.io.FileInputStream;
import java.net.URI;
import java.util.*;
import java.util.zip.GZIPInputStream;

/**
 * https://dumps.wikimedia.org/other/categoriesrdf/latest/ https://dumps.wikimedia.org/other/categoriesrdf/latest/simplewiki-20201128-categories.ttl.gz
 * https://dumps.wikimedia.org/simplewiki/latest/
 */
public class Ontology {

    List<Category> id = new ArrayList<>(64 * 1024);

    Map<String, Category> cat = new HashMap<>(64 * 1024);

    public Ontology(String categoriesFile)
        throws ParseException, InterruptedException, java.io.IOException {
        this(new TurtleParser(
            new GZIPInputStream(
                new FileInputStream(categoriesFile),
                8 * 1024 * 1024
            ),
            URI.create(
                "http://_"
                //"https://simple.wikipedia.org/wiki/"
            )
        ));
    }

    public Ontology(TurtleParser p) throws ParseException, InterruptedException {

        Resource category = new Resource(
            "https://www.mediawiki.org/ontology#isInCategory"
            //"mediawiki:isInCategory"
        );

        p.parse(new Callback() {
            @Override
            protected void startDocumentInternal() {

            }

            @Override
            protected void endDocumentInternal() {

            }

            @Override
            protected void processStatementInternal(Node[] n) {
                //System.out.println(Arrays.toString(n));
                if (category.equals(n[1])) {
                    var s = tag(n[0]);
                    if (s != null) {
                        var p = tag(n[2]);
                        if (p != null) {
                            s.parent.add(p.id);
                            p.child.add(s.id);
                        }
                    }
                }
            }

            private Category tag(Node node) {
                final String l = node.getLabel();
                final int beginIndex = l.lastIndexOf(':') + 1;
                if (beginIndex < 0)
                    return null;

                String t = l.substring(beginIndex);
                if (!filter(t))
                    return null;
                return cat.computeIfAbsent(t, (T) -> {
                    final int nextID = id.size();
                    final Category y = new Category(T, nextID);
                    id.add(y);
                    return y;
                });
            }

            private boolean filter(String t) {
                if (t.isEmpty())
                    return false;
                if (Character.isDigit(t.charAt(0)))
                    return false; //ignore years, etc
                if (t.startsWith("User_"))
                    return false;
                if (t.startsWith("Pages_"))
                    return false;
                if (t.startsWith("Clean-up_categories_"))
                    return false;
                if (t.endsWith("_events"))
                    return false;
                return !t.startsWith("CS1_");
            }
        });
    }

    public static void main(String[] args) throws ParseException, InterruptedException, java.io.IOException {
        final String categoriesFile =
            //"/home/me/d/simplewiki-20201128-categories.ttl.gz"
            "/home/me/d/enwiktionary-20201205-categories.ttl.gz"
//            "/home/me/d/enwiki-20201205-categories.ttl.gz"
            ;
        Ontology o = new Ontology(categoriesFile);
        for (Category c : o.cat.values()) {
            System.out.println(c);
            System.out.println('\t' + o.toString(c.ancestors(o)));
        }
    }

    public Category get(int i) {
        return id.get(i);
    }

    public Category get(String i) {
        return cat.get(i);
    }

    public String name(int i) {
        return get(i).name;
    }

    public GHIntHashSet ancestors(int i) {
        return get(i).ancestors(this);
    }

    private String toString(GHIntHashSet ids) {
        StringBuilder sb = new StringBuilder(ids.size() * 32);
        sb.append('[');
        ids.forEach((IntProcedure) ((int i) -> sb.append(name(i)).append(',')));
        sb.setLength(sb.length() - 1);
        sb.append(']');
        return sb.toString();
    }

    static class Category {
        final int id;
        final GHIntHashSet parent = new GHIntHashSet(0);
        final GHIntHashSet child = new GHIntHashSet(0);
        private final String name;

        Category(String name, int id) {
            this.name = name;
            this.id = id;
        }

        @Override
        public String toString() {
            return name + '=' + parent;
        }

        /**
         * dfs
         */
        public GHIntHashSet ancestors(Ontology o) {
            GHIntHashSet p = new GHIntHashSet();
            ancestors(o, p);
            return p;
        }

        public void ancestors(Ontology o, GHIntHashSet p) {
            parent.forEach((IntProcedure) (int i) -> {
                if (p.add(i)) {
                    o.get(i).ancestors(o, p);
                }
            });
        }
    }
}

