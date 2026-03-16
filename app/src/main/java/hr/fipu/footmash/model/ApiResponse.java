package hr.fipu.footmash.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * Generički wrapper za sve API-Football odgovore.
 * API uvijek vraća: { "get": "...", "parameters": {...}, "errors": [...], "results": N, "paging": {...}, "response": [...] }
 */
public class ApiResponse<T> {

    @SerializedName("get")
    private String get;

    @SerializedName("parameters")
    private Object parameters;

    @SerializedName("errors")
    private Object errors;

    @SerializedName("results")
    private int results;

    @SerializedName("paging")
    private Paging paging;

    @SerializedName("response")
    private List<T> response;

    public String getGet() { return get; }
    public Object getParameters() { return parameters; }
    public Object getErrors() { return errors; }
    public int getResults() { return results; }
    public Paging getPaging() { return paging; }
    public List<T> getResponse() { return response; }

    public static class Paging {
        @SerializedName("current")
        private int current;
        @SerializedName("total")
        private int total;

        public int getCurrent() { return current; }
        public int getTotal() { return total; }
    }
}
