package weka.core.stemmers;

/**
 * Created by apetra on 9/15/2017.
 */
public class ReplaceTransformation implements Transformation {

    private static final long serialVersionUID = 5427699406177734026L;

    String string;
    String replaceString;

    public ReplaceTransformation(String replace) {
        this.replaceString = replace;
    }

    public ReplaceTransformation(String string, String replace) {
        this.replaceString = replace;
        this.string = string;
    }

    @Override
    public String transform(String stem) {

        if (string == null) {
           return replaceString;
        }
        if(stem.lastIndexOf(string)==-1){
            return stem;
        }
        return stem.substring(0, stem.lastIndexOf(string)) + replaceString + stem.substring(stem.lastIndexOf(string) + string.length());

    }
}
