package hr.fipu.footmash.model;

import com.google.gson.annotations.SerializedName;

/**
 * Plain POJO for a team. Populated from local seed data.
 */
public class TeamResponse {

    @SerializedName("team_key")
    private int teamKey;

    @SerializedName("team_name")
    private String teamName;

    @SerializedName("team_badge")
    private String teamBadge;

    public int getTeamKey() { return teamKey; }
    public String getTeamName() { return teamName; }
    public String getTeamBadge() { return teamBadge; }

    public void setTeamKey(int teamKey) { this.teamKey = teamKey; }
    public void setTeamName(String teamName) { this.teamName = teamName; }
    public void setTeamBadge(String teamBadge) { this.teamBadge = teamBadge; }
}
