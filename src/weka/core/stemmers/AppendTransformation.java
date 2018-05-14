package weka.core.stemmers;

/**
 * Created by apetra on 9/15/2017.
 */
public class AppendTransformation implements Transformation {
    private static final long serialVersionUID = 5427699406177734026L;

    String suffix;

    public AppendTransformation(String word) {
        this.suffix = word;
    }

    @Override
    public String transform(String stem) {
        return stem + suffix;
    }
}
