package weka.core.stemmers;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <p>
 * Ova klasa implementira stemer za hrvatski "Simple stemmer for Croatian v0.1" Nikole Ljubešića i Ivana Pandžića. Originalna implementacija u Python-u je dostupna na adresi:
 * <br>
 * <a href="http://nlp.ffzg.hr/resources/tools/stemmer-for-croatian/">http://nlp.ffzg.hr/resources/tools/stemmer-for-croatian/</a>
 * </p>
 * <p>
 * Stemer predstavlja poboljšanje ranijeg algoritma opisanog u radu:
 * <br>
 * Retrieving Information in Croatian: Building a Simple and Efficient Rule-Based Stemmer, Nikola Ljubešić, Damir Boras, Ozren Kubelka, Digital Information and Heritage, 313–320 (2007).
 * </p>
 * <br>
 * <p><i>
 * This class implements the "Simple stemmer for Croatian v0.1" by Nikola Ljubešić and Ivan Pandžić. The original implementation in Python is available at:
 * <br>
 * <a href="http://nlp.ffzg.hr/resources/tools/stemmer-for-croatian/">http://nlp.ffzg.hr/resources/tools/stemmer-for-croatian/</a></i>
 * </p>
 * <p><i>
 * The stemmer represents an improvement of an earlier algorithm described in the paper:
 * <br>
 * Retrieving Information in Croatian: Building a Simple and Efficient Rule-Based Stemmer, Nikola Ljubešić, Damir Boras, Ozren Kubelka, Digital Information and Heritage, 313–320 (2007).
 * </i></p>
 *
 * @author Vuk Batanović
 *         <br>
 * @see <i>Reliable Baselines for Sentiment Analysis in Resource-Limited Languages: The Serbian Movie Review Dataset</i>, Vuk Batanović, Boško Nikolić, Milan Milosavljević, in Proceedings of the 10th International Conference on Language Resources and Evaluation (LREC 2016), pp. 2688-2696, Portorož, Slovenia (2016).
 * <br>
 * https://github.com/vukbatanovic/SCStemmers
 * <br>
 */
public class AnticStemmer2 extends SCStemmer {

    private static final long serialVersionUID = 7637737414857811723L;

    /**
     * Mapa sufiksnih transformacija.
     * <p>
     * <i>The map of suffix transformations.</i>
     */
    private HashMap<String, String> transformations;

    /**
     * Mapa transformacija nakon stemovanja.
     * <p>
     * <i>The map of transformations after stemming.</i>
     */

    private HashMap<String, HashMap<String, Transformation>> postTransformations;
    /**
     * Lista stop-reči. Korišćena je implementacija u vidu hashseta radi brzine.
     * <p>
     * <i>The list of stop-words. A hashset implementation was used for the sake of efficiency.</i>
     */
    private HashSet<String> stopset;

    /**
     * Lista početnih delova reči.
     * <p>
     * <i>The list of word beginnings.</i>
     */
    public ArrayList<String> wordStart;

    /**
     * Lista završetaka reči.
     * <p>
     * <i>The list of word endings.</i>
     */
    public ArrayList<String> wordEnd;

    /**
     * Lista morfoloških obrazaca reči.
     * <p>
     * <i>The list of morphological patterns of words.</i>
     */
    private ArrayList<Pattern> wordPatterns;

    /**
     * Skup samoglasnika.
     * <p>
     * <i>The set of vowels.</i>
     */
    private static final Pattern vowelPattern = Pattern.compile("[aeiouR]");

    /**
     * Ako se naiđe na neku od stop-reči, ona se preskače. U suprotnom, sufiks reči se najpre transformiše a zatim i uklanja.
     * <p>
     * <i>If a stop-word is encountered, it is skipped. Otherwise, the suffix of the word is first transformed and then removed.</i>
     *
     * @param word Reč koju treba obraditi
     *             <br><i>The word that should be processed</i>
     * @return Stemovana reč
     * <br><i> The stemmed word</i>
     */
    public String stemWord(String word) {
        String stemmed;
        if (word.startsWith("naj")) {
            word = word.substring(3);
        }
        if(word.startsWith("ne") && !word.startsWith("ne_")){
            word = "ne_" + word.substring(2);
        }
        stemmed = transform(word);

        for (int i = 0; i < wordPatterns.size(); i++) {
            Matcher matcher = wordPatterns.get(i).matcher(stemmed);
            if (matcher.matches()) {
                // group(0) contains the entire string that matched the pattern.
                stemmed = getPostTransformation(stemmed, matcher.group(1), wordStart.get(i), wordEnd.get(i));
//                System.out.println(stemmed + ": " + wordPatterns.get(i));
                break;
            }
        }
//        }
//        metrics.processNewStem(word, stemmed);
        return stemmed;
    }

    /**
     * Stemuje liniju teksta
     * <p>
     * <i>Stems a line of text</i>
     */
    @Override
    public String stemLine(String line) {
        String[] words = line.split("\\b");
        StringBuffer sb = new StringBuffer();
        for (String word : words) {
            if (word.length() > 1) {
                sb.append(stemWord(word));
            }
        }
        return sb.toString().trim();
    }

    //    @Override
//    public String stemWord(String line) {
//        String[] words = line.split(" ");
//        if (words.length > 1) {
//            return line;
//        } else {
//            return stemWord(line);
//        }
//
//
//    }
//    @Override
//    public String stemWord(String line) {
//        String[] words = line.split(" ");
//        StringBuffer sb = new StringBuffer();
//        for (String word : words) {
//            if (word.length() > 1) {
//                sb.append(stemSingleWord(word));
//                sb.append(" ");
//            }
//        }
//        return sb.toString().trim();
//    }

    /**
     * Zamenjuje sufiks reči transformisanom varijantom tog sufiksa
     * <p>
     * <i>Replaces the word suffix with a transformed variant of that suffix</i>
     *
     * @param word Reč koju treba obraditi
     *             <br><i>The word that should be processed</i>
     * @return Transformisana reč
     * <br><i> The transformed word</i>
     */

    private String transform(String word) {
        for (String key : transformations.keySet())
            //fix za transform da ne trasnformise unutrasnjost reci
            if (word.endsWith(key))
                return word.replaceAll(key + "$", transformations.get(key));
        return word;
    }

    /**
     * Kapitalizuje slogotvorno R u zadatoj reči, ako postoji
     * <p>
     * <i>Capitalizes the syllabic R in the given word, if it exists</i>
     *
     * @param word Reč koju treba obraditi
     *             <br><i>The word that should be processed</i>
     * @return Reč sa kapitalizovanim slogotvornim R
     * <br><i>The word with the syllabic R capitalized</i>
     */
    private String capitalizeSyllabicR(String word) {
        return word.replaceAll("(^|[^aeiou])r($|[^aeiou])", "$1R$2");
    }

    /**
     * Proverava da li reč sadrži samoglasnik/slogotvorno R
     * <p>
     * <i>Checks whether the word contains a vowel/syllabic R</i>
     *
     * @param word Reč koju treba obraditi
     *             <br><i>The word that should be processed</i>
     * @return True ako reč sadrži samoglasnik/slogotvorno R, false u suprotnom
     * <br><i>True if the word contains a vowel/syllabic R, false otherwise</i>
     */
    private boolean hasAVowel(String word) {
        Matcher matcher = vowelPattern.matcher(capitalizeSyllabicR(word));
        return matcher.find();
    }

    private void addPostTransformation(String stem, String suffixes, Transformation transform) {
        if (!postTransformations.containsKey(stem)) {
            postTransformations.put(stem, new HashMap<>(200));
        }
        postTransformations.get(stem).put(suffixes, transform);
    }

    private String getPostTransformation(String word, String stem, String wordStart, String wordEnd) {

        if (postTransformations.containsKey(wordStart)) {
            if (postTransformations.get(wordStart).containsKey(wordEnd)) {
                stem = postTransformations.get(wordStart).get(wordEnd).transform(stem);
            }
        }

        if (hasAVowel(stem) && stem.length() > 1) {
            return stem;
        } else {
            return word;
        }
//        Set<String> keySetStart = postTransformations.keySet();
//        String[] keysStart = keySetStart.toArray(new String[keySetStart.size()]);
//        for (String keyStart : keysStart) {
//            if (Pattern.compile(keyStart).matcher(stem).matches()) {
//                Set<String> keySet = postTransformations.get(keyStart).keySet();
//                String[] keysEnd = keySet.toArray(new String[keySet.size()]);
//                for (String keyEnd : keysEnd) {
//                    if (Pattern.compile(keyEnd).matcher(suffix).matches()) {
//
//                    }
//                }
//            }
//
//        }


    }

//    private String getPostTransformedStem(String keyStart, String stem, String suffix) {
//        Set<String> keySet = postTransformations.get(keyStart).keySet();
//        String[] keysEnd = keySet.toArray(new String[keySet.size()]);
//        for (String keyEnd : keysEnd) {
//            if (Pattern.compile(keyEnd).matcher(suffix).matches()) {
//                return postTransformations.get(keyStart).get(keyEnd).transform(stem);
//            }
//        }
//        if (hasAVowel(stem) && stem.length() > 1) {
//            return stem;
//        } else {
//            return stem + suffix;
//        }
//    }

    protected void initRules() {
        // RULES
        wordStart = new ArrayList<String>();
        wordEnd = new ArrayList<String>();
        wordPatterns = new ArrayList<Pattern>();
        postTransformations = new HashMap<>(200);

        /**
         * SIR: Pravila za zamenice
         */

        //licne zamenice
        wordStart.add("m");
        wordEnd.add("ene|eni|nom|e");
        addPostTransformation("m", "ene|eni|nom|e", new ReplaceTransformation("ja"));//i kao nastavak iskljuceno zbog zamenice "mi"
        wordStart.add("t");
        wordEnd.add("i|e|ebe|ebi|obom");
        addPostTransformation("t", "i|e|ebe|ebi|obom", new ReplaceTransformation("ti"));
        wordStart.add("s");
        wordEnd.add("e|ebe|ebi|obom");
        addPostTransformation("s", "e|ebe|ebi|obom", new ReplaceTransformation("sebe"));


        wordStart.add("n");
        wordEnd.add("as|ama|am");
        addPostTransformation("n", "as|ama|am", new ReplaceTransformation("mi"));
        wordStart.add("v");
        wordEnd.add("as|ama|am");
        addPostTransformation("v", "as|ama|am", new ReplaceTransformation("vi"));

        //licne 3. lice
        wordStart.add("nj");
        wordEnd.add("(eg|em|ega|emu|im|ime)?");
        addPostTransformation("nj", "(eg|em|ega|emu|im|ime)?", new ReplaceTransformation("on"));
        wordStart.add("nj|j");
        wordEnd.add("e|oj|u|om|ome");
        addPostTransformation("nj|j", "e|oj|u|om|ome", new ReplaceTransformation("ona"));
        wordStart.add("nj");
        wordEnd.add("ih|ima");
        addPostTransformation("nj", "ih|ima", new ReplaceTransformation("oni"));

        wordStart.add("mu|ga");
        wordEnd.add("");
        addPostTransformation("mu|ga", "", new ReplaceTransformation("on"));
        wordStart.add("im|ih|oni");
        wordEnd.add("");
        addPostTransformation("im|ih|oni", "", new ReplaceTransformation("oni"));


        //pokazne
        wordStart.add("t|ov");
        wordEnd.add("aj|og|oga|om|ome|omu|im|ime|a|e|oj|u|om|i|ih|ima|o");
        addPostTransformation("t|ov", "aj|og|oga|om|ome|omu|im|ime|a|e|oj|u|om|i|ih|ima|o", new AppendTransformation("aj"));
        wordStart.add("on");
        wordEnd.add("aj|og|oga|om|ome|omu|im|ime|oj|u|om|ih|ima");
        addPostTransformation("on", "aj|og|oga|om|ome|omu|im|ime|oj|u|om|ih|ima", new AppendTransformation("aj"));
        wordStart.add(".+olik|njegov|nje(zi)?n|njihov|njin");
        wordEnd.add("i|og|oga|om|ome|omu|im|ime|o|a|e|oj|u|om|i|ih|ima");

        wordStart.add(".*kv");
        wordEnd.add("og|oga|om|ome|omu|im|ime|a|e|oj|u|om|ima|ih|i|o");
        addPostTransformation(".*kv", "og|oga|om|ome|omu|im|ime|a|e|oj|u|om|ima|ih|i|o", new ReplaceTransformation("kv", "kav"));

        //upitno-odnosne
        wordStart.add(".*koj|.*čij");
        wordEnd.add("i|eg|ega|em|emu|im|ima|ih|a|e|oj|u|om");
        addPostTransformation(".*koj|.*čij", "i|eg|ega|em|emu|im|ima|ih|a|e|oj|u|om", new AppendTransformation("i"));


        //prisvojne
        wordStart.add("mo|tvo|svo");
        wordEnd.add("j|(je)?g|(je)?ga|(je)?m|me|(je)?mu|jim|jih");
        addPostTransformation("mo|tvo|svo", "j|(je)?g|(je)?ga|(je)?m|me|(je)?mu|jim|jih", new AppendTransformation("j"));
        wordStart.add("vaš|naš");
        wordEnd.add("i|eg|em|ega|emu|im|ime|ima|ih|a|e|oj|u|om");


        wordStart.add("(i|ni|ne|sva)*č");
        wordEnd.add("eg|ega|em|emu|im|ime");
        addPostTransformation("(i|ni|ne|sva)*č", "eg|ega|em|emu|im|ime", new ReplaceTransformation("č", "što"));
        wordStart.add("(i|ni|ne|sva)*(što|šta)");
        wordEnd.add("");
        addPostTransformation("(i|ni|ne|sva)*(što|šta)", "", new ReplaceTransformation("šta", "što"));
        wordStart.add("(i|ni|ne|sva)*k");
        wordEnd.add("o|og|oga|om|ome|omu|im|ime");
        addPostTransformation(".*(i|ni|ne|sva)*k", "o|og|oga|om|ome|omu|im|ime", new AppendTransformation("o"));

        /**
         * SIR: Pravila za pomocne glagole
         */


        //biti
        wordStart.add("bud");
        wordEnd.add("em|eš|e|emo|ete|u|i|imo|ite|ući");
        addPostTransformation("bud", "em|eš|e|emo|ete|u|i|imo|ite|ući", new ReplaceTransformation("biti"));

        //changed: MAS1
        //ukloniti bivši jer je češći pridev
        wordStart.add("bi");
        wordEnd.add("ti|o|la|lo|li|le|la|h|smo|ste|še|ću|ćeš|će|ćemo|ćete");
        addPostTransformation("bi", "ti|o|la|lo|li|le|la|h|smo|ste|še|ću|ćeš|će|ćemo|ćete|vši", new ReplaceTransformation("biti"));

        //added: MAS1
        //dodati bivati
        wordStart.add("biva");
        wordEnd.add("ti|o|la|lo|li|le|la|h|smo|ste|še|ću|ćeš|će|ćemo|ćete|m|š|mo|te|ju|jući|");
        addPostTransformation("biva", "ti|o|la|lo|li|le|la|h|smo|ste|še|ću|ćeš|će|ćemo|ćete|m|š|mo|te|ju|jući|", new ReplaceTransformation("biti"));

        wordStart.add("be");
        wordEnd.add("(ja)?h|(ja)?smo|(ja)?ste|(ja)?še|(ja)?hu");
        addPostTransformation("be", "(ja)?h|(ja)?smo|(ja)?ste|(ja)?še|(ja)?hu", new ReplaceTransformation("biti"));

        wordStart.add("(je)?");
        wordEnd.add("sam|si|st|ste|smo|su");
        addPostTransformation("(je)?", "sam|si|st|ste|smo|su", new ReplaceTransformation("biti"));

        wordStart.add("ni");
        wordEnd.add("sam|si|st|ste|smo|su|je");
        addPostTransformation("ni", "sam|si|st|ste|smo|su|je", new ReplaceTransformation("NE_biti"));

        //hteti
        wordStart.add("(ho)?");
        wordEnd.add("ću|ćeš|će|ćemo|ćete|teći");
        addPostTransformation("(ho)?", "ću|ćeš|će|ćemo|ćete|teći", new ReplaceTransformation("hteti"));

        wordStart.add("ne");
        wordEnd.add("ću|ćeš|će|ćemo|ćete");
        addPostTransformation("ne", "ću|ćeš|će|ćemo|ćete", new ReplaceTransformation("NE_hteti"));

        wordStart.add("hte");
        wordEnd.add("ti|o|la|lo|li|le|(do)?h|(de)?|(do)?smo|(do)?ste|(do)še?|h|v|vši");
        addPostTransformation("hte", "ti|o|la|lo|li|le|(do)?h|(de)?|(do)?smo|(do)?ste|(do)še?|h|v|vši", new ReplaceTransformation("hteti"));

        //moći
        wordStart.add("mog");
        wordEnd.add("u|ao|la|lo|li|oh|oše|osmo|oste|ući|avši");
        addPostTransformation("mog", "u|ao|la|lo|li|oh|oše|osmo|oste|ući|avši", new ReplaceTransformation("moći"));

        wordStart.add("mož");
        wordEnd.add("eš|e|emo|ete");
        addPostTransformation("mož", "eš|e|emo|ete", new ReplaceTransformation("moći"));


        //imati
        wordStart.add("ne");
        wordEnd.add("mam|maš|ma|mamo|mate|maju");
        addPostTransformation("ne", "mam|maš|ma|mamo|mate|maju", new ReplaceTransformation("NE_imati"));

        /**
         * SIR: Pravila za komparativ prideva
         */


        //nepravilni
        wordStart.add("bolj");
        wordEnd.add("i|eg|ega|em|emu|im|ima|ih|a|e|oj|u|om");
        addPostTransformation("bolj", "i|eg|ega|em|emu|im|ima|ih|a|e|oj|u|om", new ReplaceTransformation("dobr"));
        wordStart.add("gor");
        wordEnd.add("i|eg|ega|em|emu|im|ima|ih|a|e|oj|u|om");
        addPostTransformation("gor", "i|eg|ega|em|emu|im|ima|ih|a|e|oj|u|om", new ReplaceTransformation("loš"));
        wordStart.add("već");
        wordEnd.add("i|eg|ega|em|emu|im|ima|ih|a|e|oj|u|om");
        addPostTransformation("već", "i|eg|ega|em|emu|im|ima|ih|a|e|oj|u|om", new ReplaceTransformation("velik"));
        wordStart.add("manj");
        wordEnd.add("i|eg|ega|em|emu|im|ima|ih|a|e|oj|u|om");
        addPostTransformation("manj", "i|eg|ega|em|emu|im|ima|ih|a|e|oj|u|om", new ReplaceTransformation("mal"));

        //ši
        wordStart.add("lep|lak|mek");
        wordEnd.add("ši|šeg|šem|šeg|šim|ša|še|šu|šom|šoj|šim|ši|ših|šima");


        //izuzetak od pravila dug-duži
        wordStart.add("už");
        wordEnd.add("i|eg|ega|em|emu|im|ima|ih|a|e|oj|u|om");
        addPostTransformation("už", "i|eg|ega|em|emu|im|ima|ih|a|e|oj|u|om", new ReplaceTransformation("uz"));

        /**
         * MAS1: Pravila za tvorbu imenica i prideva
         */

        wordStart.add(".{3,}");
        wordEnd.add("ost|osti|ostima|ošću");

        wordStart.add(".{3,}");
        wordEnd.add("(av|ičast|ikav|uškast|kast|ahan|ašan|unjav|cat|ovetn)(oga|ome|omu|ima|og|om|im|ih|oj|i|e|o|a|u|)");

        wordStart.add(".{3,}");
        wordEnd.add("(ot|oć|ic)(a|e|i|u|o|om|ama)");

        wordStart.add(".{3,}n");
        wordEnd.add("ik|ika|ike|iku|ikom");

        // added -čki: MAS1
        wordStart.add(".+(s|š|č)k");
        wordEnd.add("ijima|ijega|ijemu|ijem|ijim|ijih|ijoj|ijeg|iji|ije|ija|oga|ome|omu|ima|og|om|im|ih|oj|i|e|o|a|u");
        wordStart.add(".+(s|š)tv");
        wordEnd.add("ima|om|o|a|u");
        wordStart.add(".+(t|m|p|r|g)anij");
        wordEnd.add("ama|ima|om|a|u|e|i|");
        wordStart.add(".+an");
        wordEnd.add("inom|ina|inu|ine|ima|in|om|u|i|a|e|");
        wordStart.add(".+in");
        wordEnd.add("ima|ama|om|a|e|i|u|o|");
        wordStart.add(".+on");
        wordEnd.add("ovima|ova|ove|ovi|ima|om|a|e|i|u|");
        wordStart.add(".+n");
        wordEnd.add("ijima|ijega|ijemu|ijeg|ijem|ijim|ijih|ijoj|iji|ije|ija|iju|ima|ome|omu|oga|oj|om|ih|im|og|o|e|a|u|i|");
        wordStart.add(".+(a|e|u)ć");
        wordEnd.add("oga|ome|omu|ega|emu|ima|oj|ih|om|eg|em|og|uh|im|e|a");
        wordStart.add(".+ugov");
        wordEnd.add("ima|i|e|a");
        wordStart.add(".+ug");
        wordEnd.add("ama|om|a|e|i|u|o");
        wordStart.add(".+log");
        wordEnd.add("ama|om|a|u|e|");
        wordStart.add(".+[^eo]g");
        wordEnd.add("ovima|ama|ovi|ove|ova|om|a|e|i|u|o|");
        wordStart.add(".+(rrar|ott|ss|ll)i");
        wordEnd.add("jem|ja|ju|o|");
        wordStart.add(".+uj");
        wordEnd.add("ući|emo|ete|mo|em|eš|e|u|");
        wordStart.add(".+(c|č|ć|đ|l|r)aj");
        wordEnd.add("evima|evi|eva|eve|ama|ima|em|a|e|i|u|");
        wordStart.add(".+(b|c|d|l|n|m|ž|g|f|p|r|s|t|z)ij");
        wordEnd.add("ima|ama|om|a|e|i|u|o|");
        wordStart.add(".+[^z]nal");
        wordEnd.add("ima|ama|om|a|e|i|u|o|");
        wordStart.add(".+ijal");
        wordEnd.add("ima|ama|om|a|e|i|u|o|");
        wordStart.add(".+ozil");
        wordEnd.add("ima|om|a|e|u|i|");
        wordStart.add(".+olov");
        wordEnd.add("ima|i|a|e");
        wordStart.add(".+ol");
        wordEnd.add("ima|om|a|u|e|i|");
        wordStart.add(".+lem");
        wordEnd.add("ama|ima|om|a|e|i|u|o|");
        wordStart.add(".+ram");
        wordEnd.add("ama|om|a|e|i|u|o");
        wordStart.add(".+(a|d|e|o)r");
        wordEnd.add("ama|ima|om|u|a|e|i|");
        wordStart.add(".+(e|i)s");
        wordEnd.add("ima|om|e|a|u");
        wordStart.add(".+(t|n|j|k|j|t|b|g|v)aš");
        wordEnd.add("ama|ima|om|em|a|u|i|e|");
        wordStart.add(".+(e|i)š");
        wordEnd.add("ima|ama|om|em|i|e|a|u|");
        wordStart.add(".+ikat");
        wordEnd.add("ima|om|a|e|i|u|o|");
        wordStart.add(".+lat");
        wordEnd.add("ima|om|a|e|i|u|o|");
        wordStart.add(".+et");
        wordEnd.add("ama|ima|om|a|e|i|u|o|");
        wordStart.add(".+(e|i|k|o)st");
        wordEnd.add("ima|ama|om|a|e|i|u|o|");
        wordStart.add(".+išt");
        wordEnd.add("ima|em|a|e|u");
        wordStart.add(".+ova");
        wordEnd.add("smo|ste|hu|ti|še|li|la|le|lo|t|h|o");
        wordStart.add(".+(a|e|i)v");
        wordEnd.add("ijemu|ijima|ijega|ijeg|ijem|ijim|ijih|ijoj|oga|ome|omu|ima|ama|iji|ije|ija|iju|im|ih|oj|om|og|i|a|u|e|o|");
        wordStart.add(".+[^dkml]ov");
        wordEnd.add("ijemu|ijima|ijega|ijeg|ijem|ijim|ijih|ijoj|oga|ome|omu|ima|iji|ije|ija|iju|im|ih|oj|om|og|i|a|u|e|o|");
        wordStart.add(".+(m|l)ov");
        wordEnd.add("ima|om|a|u|e|i|");
        wordStart.add(".+el");
        wordEnd.add("ijemu|ijima|ijega|ijeg|ijem|ijim|ijih|ijoj|oga|ome|omu|ima|iji|ije|ija|iju|im|ih|oj|om|og|i|a|u|e|o|");
        wordStart.add(".+(a|e|š)nj");
        wordEnd.add("ijemu|ijima|ijega|ijeg|ijem|ijim|ijih|ijoj|oga|ome|omu|ima|iji|ije|ija|iju|ega|emu|eg|em|im|ih|oj|om|og|a|e|i|o|u");
        wordStart.add(".+čin");
        wordEnd.add("ama|ome|omu|oga|ima|og|om|im|ih|oj|a|u|i|o|e|");
        wordStart.add(".+roši");
        wordEnd.add("vši|smo|ste|še|mo|te|ti|li|la|lo|le|m|š|t|h|o");
        wordStart.add(".+oš");
        wordEnd.add("ijemu|ijima|ijega|ijeg|ijem|ijim|ijih|ijoj|oga|ome|omu|ima|iji|ije|ija|iju|im|ih|oj|om|og|i|a|u|e|");
        wordStart.add(".+(e|o)vit");
        wordEnd.add("ijima|ijega|ijemu|ijem|ijim|ijih|ijoj|ijeg|iji|ije|ija|oga|ome|omu|ima|og|om|im|ih|oj|i|e|o|a|u|");
        wordStart.add(".+ast");
        wordEnd.add("ijima|ijega|ijemu|ijem|ijim|ijih|ijoj|ijeg|iji|ije|ija|oga|ome|omu|ima|og|om|im|ih|oj|i|e|o|a|u|");
        wordStart.add(".+k");
        wordEnd.add("ijemu|ijima|ijega|ijeg|ijem|ijim|ijih|ijoj|oga|ome|omu|ima|iji|ije|ija|iju|im|ih|oj|om|og|i|a|u|e|o|");
        wordStart.add(".+(e|a|i|u)va");
        wordEnd.add("jući|smo|ste|jmo|jte|ju|la|le|li|lo|mo|na|ne|ni|no|te|ti|še|hu|h|j|m|n|o|t|v|š|");
        wordStart.add(".+ir");
        wordEnd.add("ujemo|ujete|ujući|ajući|ivat|ujem|uješ|ujmo|ujte|avši|asmo|aste|ati|amo|ate|aju|aše|ahu|ala|alo|ali|ale|uje|uju|uj|al|an|am|aš|at|ah|ao");
        wordStart.add(".+ač");
        wordEnd.add("ismo|iste|iti|imo|ite|iše|eći|ila|ilo|ili|ile|ena|eno|eni|ene|io|im|iš|it|ih|en|i|e");
        wordStart.add(".+ača");
        wordEnd.add("vši|smo|ste|smo|ste|hu|ti|mo|te|še|la|lo|li|le|ju|na|no|ni|ne|o|m|š|t|h|n");
        wordStart.add(".+n");
        wordEnd.add("uvši|usmo|uste|ući|imo|ite|emo|ete|ula|ulo|ule|uli|uto|uti|uta|em|eš|uo|ut|e|u|i");
        wordStart.add(".+ni");
        wordEnd.add("vši|smo|ste|ti|mo|te|mo|te|la|lo|le|li|m|š|o");
        wordStart.add(".+((a|r|i|p|e|u)st|[^o]g|ik|uc|oj|aj|lj|ak|ck|čk|šk|uk|nj|im|ar|at|et|št|it|ot|ut|zn|zv)a");
        wordEnd.add("jući|vši|smo|ste|jmo|jte|jem|mo|te|je|ju|ti|še|hu|la|li|le|lo|na|no|ni|ne|t|h|o|j|n|m|š");
        wordStart.add(".+ur");
        wordEnd.add("ajući|asmo|aste|ajmo|ajte|amo|ate|aju|ati|aše|ahu|ala|ali|ale|alo|ana|ano|ani|ane|al|at|ah|ao|aj|an|am|aš");
        wordStart.add(".+(a|i|o)staj");
        wordEnd.add("asmo|aste|ahu|ati|emo|ete|aše|ali|ući|ala|alo|ale|mo|ao|em|eš|at|ah|te|e|u|");
        wordStart.add(".+(b|c|č|ć|d|e|f|g|j|k|n|r|t|u|v)a");
        wordEnd.add("lama|lima|lom|lu|li|la|le|lo|l");
        wordStart.add(".+(t|č|j|ž|š)aj");
        wordEnd.add("evima|evi|eva|eve|ama|ima|em|a|e|i|u|");
        wordStart.add(".+([^o]m|ič|nč|uč|b|c|ć|d|đ|h|j|k|l|n|p|r|s|š|v|z|ž)a");
        wordEnd.add("jući|vši|smo|ste|jmo|jte|mo|te|ju|ti|še|hu|la|li|le|lo|na|no|ni|ne|t|h|o|j|n|m|š");
        wordStart.add(".+(a|i|o)sta");
        wordEnd.add("dosmo|doste|doše|nemo|demo|nete|dete|nimo|nite|nila|vši|nem|dem|neš|deš|doh|de|ti|ne|nu|du|la|li|lo|le|t|o");
        wordStart.add(".+ta");
        wordEnd.add("smo|ste|jmo|jte|vši|ti|mo|te|ju|še|la|lo|le|li|na|no|ni|ne|n|j|o|m|š|t|h");
        wordStart.add(".+inj");
        wordEnd.add("asmo|aste|ati|emo|ete|ali|ala|alo|ale|aše|ahu|em|eš|at|ah|ao");
        wordStart.add(".+as");
        wordEnd.add("temo|tete|timo|tite|tući|tem|teš|tao|te|li|ti|la|lo|le");
        wordStart.add(".+(elj|ulj|tit|ac|ič|od|oj|et|av|ov)i");
        wordEnd.add("vši|eći|smo|ste|še|mo|te|ti|li|la|lo|le|m|š|t|h|o");
        wordStart.add(".+(tit|jeb|ar|ed|uš|ič)i");
        wordEnd.add("jemo|jete|jem|ješ|smo|ste|jmo|jte|vši|mo|še|te|ti|ju|je|la|lo|li|le|t|m|š|h|j|o");
        wordStart.add(".+(b|č|d|l|m|p|r|s|š|ž)i");
        wordEnd.add("jemo|jete|jem|ješ|smo|ste|jmo|jte|vši|mo|lu|še|te|ti|ju|je|la|lo|li|le|t|m|š|h|j|o");
        wordStart.add(".+luč");
        wordEnd.add("ujete|ujući|ujemo|ujem|uješ|ismo|iste|ujmo|ujte|uje|uju|iše|iti|imo|ite|ila|ilo|ili|ile|ena|eno|eni|ene|uj|io|en|im|iš|it|ih|e|i");
        wordStart.add(".+jeti");
        wordEnd.add("smo|ste|še|mo|te|ti|li|la|lo|le|m|š|t|h|o");
        wordStart.add(".+e");
        wordEnd.add("lama|lima|lom|lu|li|la|le|lo|l");
        wordStart.add(".+i");
        wordEnd.add("lama|lima|lom|lu|li|la|le|lo|l");
        wordStart.add(".+at");
        wordEnd.add("ijega|ijemu|ijima|ijeg|ijem|ijih|ijim|ima|oga|ome|omu|iji|ije|ija|iju|oj|og|om|im|ih|a|u|i|e|o|");
        wordStart.add(".+et");
        wordEnd.add("avši|ući|emo|imo|em|eš|e|u|i");


        //komparativ - pokriva slucajeve neporkrivene prethodnim pravilima
        //na kraju liste zbog ostalih pravila sa istim nastavcima
        //izbacena pravila za zenski rod
        wordStart.add(".+");
        wordEnd.add("ijima|ijega|ijemu|ijem|ijim|ijih|ijoj|ijeg");
        wordStart.add(".+");
        wordEnd.add("ajući|alima|alom|avši|asmo|aste|ajmo|ajte|ivši|amo|ate|aju|ati|aše|ahu|ali|ala|ale|alo|ana|ano|ani|ane|am|aš|at|ah|ao|aj|an");

        wordStart.add(".+(ž|đ|č|ć|nj|lj|š)");
        wordEnd.add("i|eg|ega|em|emu|im|ima|ih|a|e|oj|u|om");
        addPostTransformation(".+(ž|đ|č|ć|nj|lj|š)", "i|eg|ega|em|emu|im|ima|ih|a|e|oj|u|om", new JotationTransformation());

        wordStart.add(".+");
        wordEnd.add("anje|enje|anja|enja|enom|enoj|enog|enim|enih|anom|anoj|anog|anim|anih|eno|ovi|ova|oga|ima|ove|enu|anu|ena|ama");
        wordStart.add(".+");
        wordEnd.add("om|og|im|ih|em|oj|an|u|o|i|e|a");


        // TRANSFORMATIONS
        transformations = new HashMap<String, String>(200);
        transformations.put("lozi", "loga");
        transformations.put("lozima", "loga");
        transformations.put("pjesi", "pjeh");
        transformations.put("pjesima", "pjeh");
        transformations.put("vojci", "vojka");
        transformations.put("bojci", "bojka");
        transformations.put("jaci", "jak");
        transformations.put("jacima", "jak");
        transformations.put("anjac", "anjca");
        transformations.put("ajac", "ajca");
        transformations.put("ajaca", "ajca");
        transformations.put("ljaca", "ljca");
        transformations.put("ljac", "ljca");
        transformations.put("ejac", "ejca");
        transformations.put("ejaca", "ejca");
        transformations.put("ojac", "ojca");
        transformations.put("ojaca", "ojca");
        transformations.put("ajaka", "ajka");
        transformations.put("ojaka", "ojka");
        transformations.put("šaca", "šca");
        transformations.put("šac", "šca");
        transformations.put("inzima", "ing");
        transformations.put("inzi", "ing");
        transformations.put("tvenici", "tvenik");
        transformations.put("tetici", "tetika");
        transformations.put("teticima", "tetika");
        transformations.put("nstava", "nstva");
        transformations.put("nicima", "nik");
        transformations.put("ticima", "tik");
        transformations.put("zicima", "zik");
        transformations.put("snici", "snik");
        transformations.put("kuse", "kusi");
        transformations.put("kustava", "kustva");
        transformations.put("rave", "ravi");
        transformations.put("cenata", "centa");
        transformations.put("denata", "denta");
        transformations.put("genata", "genta");
        transformations.put("lenata", "lenta");
        transformations.put("menata", "menta");
        transformations.put("jenata", "jenta");
        transformations.put("venata", "venta");
        transformations.put("šave", "šavi");
        transformations.put("manata", "manta");
        transformations.put("tanata", "tanta");
        transformations.put("lanata", "lanta");
        transformations.put("sanata", "santa");
        transformations.put("ačak", "ačka");
        transformations.put("ačaka", "ačka");
        transformations.put("ušak", "uška");
        transformations.put("atak", "atka");
        transformations.put("ataka", "atka");
        transformations.put("atci", "atka");
        transformations.put("atcima", "atka");
        transformations.put("etak", "etka");
        transformations.put("etaka", "etka");
        transformations.put("itak", "itka");
        transformations.put("itaka", "itka");
        transformations.put("itci", "itka");
        transformations.put("otak", "otka");
        transformations.put("otaka", "otka");
        transformations.put("utak", "utka");
        transformations.put("utaka", "utka");
        transformations.put("utci", "utka");
        transformations.put("utcima", "utka");
        transformations.put("ojsci", "ojska");
        transformations.put("esama", "esma");
        transformations.put("metara", "metra");
        transformations.put("centar", "centra");
        transformations.put("centara", "centra");
        transformations.put("istara", "istra");
        transformations.put("istar", "istra");
        transformations.put("daba", "dba");
        transformations.put("čcima", "čka");
        transformations.put("čci", "čka");
        transformations.put("mac", "mca");
        transformations.put("maca", "mca");
        transformations.put("naca", "nca");
        transformations.put("nac", "nca");
        transformations.put("anaka", "anki");
        transformations.put("vac", "vca");
        transformations.put("vaca", "vca");
        transformations.put("saca", "sca");
        transformations.put("sac", "sca");
        transformations.put("raca", "rca");
        transformations.put("rac", "rca");
        transformations.put("aoca", "alca");
        transformations.put("alaca", "alca");
        transformations.put("alac", "alca");
        transformations.put("elaca", "elca");
        transformations.put("elac", "elca");
        transformations.put("olaca", "olca");
        transformations.put("olac", "olca");
        transformations.put("olce", "olca");
        transformations.put("njac", "njca");
        transformations.put("njaca", "njca");
        transformations.put("ekata", "ekta");
        transformations.put("ekat", "ekta");
        transformations.put("izam", "izma");
        transformations.put("izama", "izma");
        transformations.put("jebe", "jebi");


        // added: SIR
        transformations.put("eo", "el");
        // added: MAS1
        transformations.put("an", "ni");


        for (int i = 0; i < wordStart.size(); i++) {
            String pattern = "^(" + wordStart.get(i) + ")(" + wordEnd.get(i) + ")$";
            wordPatterns.add(Pattern.compile(pattern));
        }
    }
}
