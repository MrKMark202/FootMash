package hr.fipu.footmash.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * Model za Standing odgovore.
 * AllSportsAPI za Standings vraća:
 * { "success": 1, "result": { "total": [...], "home": [...], "away": [...] } }
 */
public class AllSportsStandingsResponse {
    @SerializedName("success")
    private int success;

    @SerializedName("result")
    private StandingResult result;

    public int getSuccess() { return success; }
    public StandingResult getResult() { return result; }

    public static class StandingResult {
        @SerializedName("total")
        private List<StandingResponse> total;

        public List<StandingResponse> getTotal() { return total; }
    }
}
