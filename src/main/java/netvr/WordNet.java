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
        run(wnHome, lemma);
    }

    @SuppressWarnings("SameReturnValue")
    public static boolean run(final String wnhome, final String lemma) throws IOException {
        final WordNet jwi = new WordNet(wnhome);
        jwi.walk(lemma);
        return true;
    }

    public void walk(final String lemma) {
        for (final POS pos : POS.values()) {
            // a line in an index file
            final IIndexWord idx = this.dict.getIndexWord(lemma, pos);
            if (idx != null) {
                // index
                System.out.println();
                System.out.println("================================================================================");
                System.out.println("■ pos = " + pos.name());
                // System.out.println("lemma = " + idx.getLemma());
                Set<IPointer> pointers = idx.getPointers();
                for (IPointer ptr : pointers) {
                    System.out.println("has relation = " + ptr.toString());
                }

                // senseid=(lemma, synsetid, sensenum)
                final List<IWordID> senseids = idx.getWordIDs();
                for (final IWordID senseid : senseids) // synset id, sense number, and lemma
                {
                    System.out.println(
                        "--------------------------------------------------------------------------------");
                    //System.out.println("senseid = " + senseid.toString());

                    // sense=(senseid, lexid, sensekey, synset)
                    IWord sense = this.dict.getWord(senseid);
                    System.out.println("● sense = " + sense.toString() + " lexid=" + sense.getLexicalID() + " sensekey="
                        + sense.getSenseKey());
                    Map<IPointer, List<IWordID>> relatedmap = sense.getRelatedMap();
                    if (relatedmap != null) {
                        for (Map.Entry<IPointer, List<IWordID>> entry : relatedmap.entrySet()) {
                            IPointer pointer = entry.getKey();
                            for (IWordID relatedid : entry.getValue()) {
                                IWord related = this.dict.getWord(relatedid);
                                System.out.println("  related " + pointer + " = " + related.getLemma() + " synset="
                                    + related.getSynset().toString());
                            }
                        }
                    }

                    AdjMarker marker = sense.getAdjectiveMarker();
                    if (marker != null)
                        System.out.println("  marker = " + marker);
                    List<IVerbFrame> verbFrames = sense.getVerbFrames();
                    if (verbFrames != null) {
						for (IVerbFrame verbFrame : verbFrames) {
							System.out.println(
								"  verbframe = " + verbFrame.getTemplate() + " : " + verbFrame.instantiateTemplate(
									lemma));
						}
                    }
                    ISenseEntry senseEntry = this.dict.getSenseEntry(sense.getSenseKey());
                    if (senseEntry == null)
                        throw new IllegalArgumentException(
                            sense.getSenseKey().toString() + " at offset " + sense.getSynset().getOffset()
                                + " with pos " + sense.getPOS().toString());
                    System.out.println(
                        "  sensenum = " + senseEntry.getSenseNumber() + " tagcnt=" + senseEntry.getTagCount());

                    // synset
                    final ISynsetID synsetid = senseid.getSynsetID();
                    final ISynset synset = this.dict.getSynset(synsetid);
                    System.out.println("● synset = " + toString(synset));

                    walk(synset, 1);
                }
            }
        }
    }

    public void walk(final ISynset synset, final int level) {
        final String indentSpace = new String(new char[level]).replace('\0', '\t');
        final Map<IPointer, List<ISynsetID>> links = synset.getRelatedMap();
        for (final Map.Entry<IPointer, List<ISynsetID>> entry : links.entrySet()) {
            final IPointer p = entry.getKey();
            System.out.println(indentSpace + "🡆 " + p.getName());
            final List<ISynsetID> relations2 = entry.getValue();
            for (final ISynsetID synsetid2 : relations2) {
                final ISynset synset2 = this.dict.getSynset(synsetid2);
                System.out.println(indentSpace + toString(synset2));

                walk(synset2, p, level + 1);
            }
        }
    }

    public void walk(final ISynset synset, final IPointer p, final int level) {
        final String indentSpace = new String(new char[level]).replace('\0', '\t');
        final List<ISynsetID> relations2 = synset.getRelatedSynsets(p);
        for (final ISynsetID synsetid2 : relations2) {
            final ISynset synset2 = this.dict.getSynset(synsetid2);
            System.out.println(indentSpace + toString(synset2));
            if (canRecurse(p))
                walk(synset2, p, level + 1);
        }
    }

    public static String toString(final ISynset synset) {
        return getMembers(synset) + synset.getGloss();
    }

    public static String getMembers(final ISynset synset) {
        final StringBuilder sb = new StringBuilder();
        sb.append('{');
        boolean first = true;
        for (final IWord sense : synset.getWords()) {
            if (first) {
                first = false;
            } else {
                sb.append(' ');
            }
            sb.append(sense.getLemma());
        }
        sb.append('}');
        sb.append(' ');
        return sb.toString();
    }

    private static boolean canRecurse(IPointer p) {
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
}
