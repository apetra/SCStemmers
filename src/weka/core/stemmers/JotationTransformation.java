package weka.core.stemmers;

import java.util.HashMap;

/**
 * Created by apetra on 9/15/2017.
 */
public class JotationTransformation implements Transformation {
    private static final long serialVersionUID = 5427699406177734026L;

    HashMap<String, String> transformations = new HashMap<String, String>(200);


    public JotationTransformation() {
        //konflikt
        //glagoli drži
        transformations.put("rž", "rz");
        transformations.put("iž", "iz");


        transformations.put("ož", "og");
        transformations.put("až", "ag");
        transformations.put("už", "ug");


        //ne dolazi do promene, nema potrebe za transformacijom
//        transformations.put("ež", "ež");


        transformations.put("đ", "d");
        transformations.put("č", "k");
        transformations.put("ć", "t");
        transformations.put("nj", "n");

        //konflikt
        //dajemo prednost h jer se sj javlja samo u viši, koji dodajemo u listu nepravilnih
        //transformations.put("š","s");
        //glagoli piši, diši
        transformations.put("š", "s");

        transformations.put("plj", "p");
        transformations.put("blj", "b");
        transformations.put("mlj", "m");
        transformations.put("vlj", "v");
        transformations.put("lj", "l");
    }

    @Override
    public String transform(String word) {
        for (String key : transformations.keySet())
            if (word.endsWith(key)) {
                return word.substring(0, word.lastIndexOf(key)) + transformations.get(key);
            }
        return word;
    }

}
