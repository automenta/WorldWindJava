package netvr;

import edu.mit.jwi.Dictionary;
import edu.mit.jwi.*;
import edu.mit.jwi.item.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * JWI
 *
 * @author Bernard Bou
 */
public class WordNet {
    private final IDictionary dict;

    public WordNet(final String wnhome) throws IOException {
        // construct the URL to the WordNet dictionary directory

        // construct the dictionary object and open it
        this.dict = new Dictionary(new File(wnhome).toURI().toURL());
        this.dict.setCharset(StandardCharsets.UTF_8);
        this.dict.open();
    }

    /**
     * @param args argument
     * @throws IOException io exception
     */
    public static void main(final String[] args) throws IOException {
        final String wnHome = "/home/me/wordnet30"; //args[0];
        final String lemma = "monkey";//args[1];
        WordNet.run(wnHome, lemma);
    }

    @SuppressWarnings("SameReturnValue")
    public static void run(final String wnhome, final String lemma) throws IOException {
        new WordNet(wnhome).walk(lemma);
    }

    public static String toString(final ISynset synset) {
        return WordNet.members(synset) + synset.getGloss();
    }

    public static String members(final ISynset synset) {
        final StringBuilder sb = new StringBuilder().append('{');
        boolean first = true;
        for (final IWord sense : synset.getWords()) {
            if (first) {
                first = false;
            } else {
                sb.append(' ');
            }
            sb.append(sense.getLemma());
        }
        return sb.append('}').append(' ').toString();
    }

    private static boolean recurse(IPointer p) {
        String symbol = p.getSymbol();
        //noinspection EnhancedSwitchMigration
        switch (symbol) {
            case "@": // hypernym
            case "~": // hyponym
            case "%p": // part holonym
            case "#p": // part meronym
            case "%m": // member holonym
            case "#m": // member meronym
            case "%s": // substance holonym
            case "#s": // substance meronym
            case "*": // entail
            case ">": // cause
                return true;
        }
        return false;
    }

    public void walk(final String lemma) {
        walk(lemma, System.out);
    }

    public void walk(final String lemma, PrintStream out) {
        for (final POS pos : POS.values())
            walk(lemma, pos, out);
    }

    private boolean walk(String lemma, POS pos, PrintStream out) {
        // a line in an index file
        final IIndexWord idx = this.dict.getIndexWord(lemma, pos);
        if (idx == null)
            return false;

        // index
        out.println();
        out.println("================================================================================");
        out.println("■ pos = " + pos.name());
        // out.println("lemma = " + idx.getLemma());
        Set<IPointer> pointers = idx.getPointers();
        for (IPointer ptr : pointers) {
            out.println("has relation = " + ptr.toString());
        }

        // senseid=(lemma, synsetid, sensenum)
        final List<IWordID> senseids = idx.getWordIDs();
        for (final IWordID senseid : senseids) { // synset id, sense number, and lemma
            walk(lemma, senseid, out);
        }
        return true;
    }

    private void walk(String lemma, IWordID senseid, PrintStream out) {
        out.println(
            "--------------------------------------------------------------------------------");
        //out.println("senseid = " + senseid.toString());

        // sense=(senseid, lexid, sensekey, synset)
        IWord sense = this.dict.getWord(senseid);
        out.println("● sense = " + sense.toString() + " lexid=" + sense.getLexicalID() + " sensekey="
            + sense.getSenseKey());
        Map<IPointer, List<IWordID>> relatedmap = sense.getRelatedMap();
        if (relatedmap != null) {
            for (Map.Entry<IPointer, List<IWordID>> entry : relatedmap.entrySet()) {
                IPointer pointer = entry.getKey();
                for (IWordID relatedid : entry.getValue()) {
                    IWord related = this.dict.getWord(relatedid);
                    out.println("  related " + pointer + " = " + related.getLemma() + " synset="
                        + related.getSynset().toString());
                }
            }
        }

        AdjMarker marker = sense.getAdjectiveMarker();
        if (marker != null)
            out.println("  marker = " + marker);
        List<IVerbFrame> verbFrames = sense.getVerbFrames();
        if (verbFrames != null) {
            for (IVerbFrame verbFrame : verbFrames) {
                out.println(
                    "  verbframe = " + verbFrame.getTemplate() + " : " + verbFrame.instantiateTemplate(
                        lemma));
            }
        }
        ISenseEntry senseEntry = this.dict.getSenseEntry(sense.getSenseKey());
        if (senseEntry == null)
            throw new IllegalArgumentException(
                sense.getSenseKey().toString() + " at offset " + sense.getSynset().getOffset()
                    + " with pos " + sense.getPOS().toString());
        out.println(
            "  sensenum = " + senseEntry.getSenseNumber() + " tagcnt=" + senseEntry.getTagCount());

        // synset
        final ISynsetID synsetid = senseid.getSynsetID();
        final ISynset synset = this.dict.getSynset(synsetid);
        out.println("● synset = " + WordNet.toString(synset));

        walk(synset, 1, out);
    }

    public void walk(final ISynset synset, final int level, PrintStream out) {
        final String indentSpace = indent(level);
        final Map<IPointer, List<ISynsetID>> links = synset.getRelatedMap();
        for (final Map.Entry<IPointer, List<ISynsetID>> entry : links.entrySet()) {
            final IPointer p = entry.getKey();
            out.println(indentSpace + "🡆 " + p.getName());
            final List<ISynsetID> relations2 = entry.getValue();
            for (final ISynsetID synsetid2 : relations2) {
                final ISynset synset2 = this.dict.getSynset(synsetid2);
                out.println(indentSpace + WordNet.toString(synset2));

                walk(synset2, p, level + 1, out);
            }
        }
    }


    public void walk(final ISynset synset, final IPointer p, final int level, PrintStream out) {
        final String indentSpace = indent(level);
        final List<ISynsetID> relations2 = synset.getRelatedSynsets(p);
        for (final ISynsetID synsetid2 : relations2) {
            final ISynset synset2 = this.dict.getSynset(synsetid2);
            out.println(indentSpace + WordNet.toString(synset2));
            if (WordNet.recurse(p))
                walk(synset2, p, level + 1, out);
        }
    }

    private static String indent(int level) {
        return new String(new char[level]).replace('\0', '\t');
    }

}
