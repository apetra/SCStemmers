package weka.core.stemmers;

import java.io.Serializable;

/**
 * Created by apetra on 9/15/2017.
 */
public interface Transformation extends Serializable {

    public String transform(String stem);
}
