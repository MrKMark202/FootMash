package hr.fipu.footmash.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "match_result")
public class MatchResult {

    @PrimaryKey(autoGenerate = true)
    private int id;

    private int fixtureId;
    private int homeGoals;
    private int awayGoals;
    private String matchSummary;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getFixtureId() { return fixtureId; }
    public void setFixtureId(int fixtureId) { this.fixtureId = fixtureId; }

    public int getHomeGoals() { return homeGoals; }
    public void setHomeGoals(int homeGoals) { this.homeGoals = homeGoals; }

    public int getAwayGoals() { return awayGoals; }
    public void setAwayGoals(int awayGoals) { this.awayGoals = awayGoals; }

    public String getMatchSummary() { return matchSummary; }
    public void setMatchSummary(String matchSummary) { this.matchSummary = matchSummary; }
}
