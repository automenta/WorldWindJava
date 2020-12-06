package netvr;

import org.semanticweb.yars.nx.*;
import org.semanticweb.yars.nx.parser.*;
import org.semanticweb.yars.turtle.TurtleParser;

import java.io.*;
import java.net.URI;
import java.util.*;
import java.util.zip.GZIPInputStream;

/**
 * https://dumps.wikimedia.org/other/categoriesrdf/latest/
 * https://dumps.wikimedia.org/other/categoriesrdf/latest/simplewiki-20201128-categories.ttl.gz
 * https://dumps.wikimedia.org/simplewiki/latest/ */
public class Ontology {

    static class Category {
        final int id;
        final Set<Integer> parent = new HashSet<>();
        final Set<Integer> child = new HashSet<>();
        private final String name;

        Category(String name, int id) {
            this.name = name;
            this.id = id;
        }

        @Override
        public String toString() {
            return name + "=" + parent;
        }
    }

    public static void main(String[] args) throws IOException, ParseException, InterruptedException {
        TurtleParser p = new TurtleParser(
            new GZIPInputStream(
                new FileInputStream("/home/me/d/simplewiki-20201128-categories.ttl.gz"),
                8 * 1024 * 1024
            ),
            URI.create("http://_")
        );

        List<String> TAGS = new ArrayList(64*1024);
        Map<String,Category> CAT = new HashMap<>(64*1024);

        Resource category = new Resource(
        "https://www.mediawiki.org/ontology#isInCategory"
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
                    if (s!=null) {
                        var p = tag(n[2]);
                        if (p!=null) {
                            s.parent.add(p.id);
                            p.child.add(s.id);
                        }
                    }
                }
            }

            final int prefixLen = "https://simple.wikipedia.org/wiki/Category:".length();

            private Category tag(Node node) {
                String t = node.getLabel().substring(prefixLen);
                if (!filter(t)) return null;
                return CAT.computeIfAbsent(t, (T) -> {
                    final int nextID = TAGS.size();
                    TAGS.add(T);
                    return new Category(T, nextID);
                });
            }

            private boolean filter(String t) {
                if (Character.isDigit(t.charAt(0))) return false; //ignore years, etc
                if (t.startsWith("User_")) return false;
                if (t.startsWith("Pages_")) return false;
                if (t.startsWith("Clean-up_categories_")) return false;
                if (t.endsWith("_events")) return false;
                return !t.startsWith("CS1_");
            }
        });
        CAT.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(n -> System.out.println(n.getValue()));
        System.out.println(TAGS.size() + " total");
    }
}

