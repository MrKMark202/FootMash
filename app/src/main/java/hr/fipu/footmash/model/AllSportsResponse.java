package hr.fipu.footmash.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * Generički wrapper za AllSportsAPI odgovore.
 */
public class AllSportsResponse<T> {

    @SerializedName("success")
    private int success;

    @SerializedName("result")
    private List<T> result;

    public int getSuccess() {
        return success;
    }

    public List<T> getResult() {
        return result;
    }
}
