package hr.fipu.footmash.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "fixture")
public class Fixture {

    @PrimaryKey(autoGenerate = true)
    private int id;

    private int seasonId;
    private int matchday;
    private int homeTeamId;
    private String homeTeamName;
    private int awayTeamId;
    private String awayTeamName;
    private boolean isUserTeam;
    private boolean isSimulated;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getSeasonId() { return seasonId; }
    public void setSeasonId(int seasonId) { this.seasonId = seasonId; }

    public int getMatchday() { return matchday; }
    public void setMatchday(int matchday) { this.matchday = matchday; }

    public int getHomeTeamId() { return homeTeamId; }
    public void setHomeTeamId(int homeTeamId) { this.homeTeamId = homeTeamId; }

    public String getHomeTeamName() { return homeTeamName; }
    public void setHomeTeamName(String homeTeamName) { this.homeTeamName = homeTeamName; }

    public int getAwayTeamId() { return awayTeamId; }
    public void setAwayTeamId(int awayTeamId) { this.awayTeamId = awayTeamId; }

    public String getAwayTeamName() { return awayTeamName; }
    public void setAwayTeamName(String awayTeamName) { this.awayTeamName = awayTeamName; }

    public boolean isUserTeam() { return isUserTeam; }
    public void setUserTeam(boolean userTeam) { isUserTeam = userTeam; }

    public boolean isSimulated() { return isSimulated; }
    public void setSimulated(boolean simulated) { isSimulated = simulated; }
}
