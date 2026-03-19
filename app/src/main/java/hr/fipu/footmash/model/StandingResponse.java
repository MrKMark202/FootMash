package hr.fipu.footmash.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * Model za Standing odgovor iz /standings endpoint-a.
 * Struktura: { "league": { "id": ..., "standings": [[...]] } }
 */
public class StandingResponse {

    @SerializedName("standing_place")
    private int standingPlace;

    @SerializedName("team_key")
    private int teamKey;

    @SerializedName("standing_team")
    private String standingTeam;

    @SerializedName("standing_pts")
    private int standingPts;

    @SerializedName("standing_W")
    private int standingW;

    @SerializedName("standing_D")
    private int standingD;

    @SerializedName("standing_L")
    private int standingL;

    @SerializedName("standing_GD")
    private int standingGD;

    public int getStandingPlace() { return standingPlace; }
    public int getTeamKey() { return teamKey; }
    public String getStandingTeam() { return standingTeam; }
    public int getStandingPts() { return standingPts; }
    public int getStandingW() { return standingW; }
    public int getStandingD() { return standingD; }
    public int getStandingL() { return standingL; }
    public int getStandingGD() { return standingGD; }
}
