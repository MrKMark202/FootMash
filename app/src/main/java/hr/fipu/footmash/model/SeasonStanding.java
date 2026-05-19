package hr.fipu.footmash.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "season_standing")
public class SeasonStanding {

    @PrimaryKey(autoGenerate = true)
    private int id;

    private int seasonId;
    private int teamId;
    private String teamName;
    private int played;
    private int won;
    private int drawn;
    private int lost;
    private int goalsFor;
    private int goalsAgainst;
    private int points;
    private boolean isUserTeam;
    private String badgeUrl;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getSeasonId() { return seasonId; }
    public void setSeasonId(int seasonId) { this.seasonId = seasonId; }

    public int getTeamId() { return teamId; }
    public void setTeamId(int teamId) { this.teamId = teamId; }

    public String getTeamName() { return teamName; }
    public void setTeamName(String teamName) { this.teamName = teamName; }

    public int getPlayed() { return played; }
    public void setPlayed(int played) { this.played = played; }

    public int getWon() { return won; }
    public void setWon(int won) { this.won = won; }

    public int getDrawn() { return drawn; }
    public void setDrawn(int drawn) { this.drawn = drawn; }

    public int getLost() { return lost; }
    public void setLost(int lost) { this.lost = lost; }

    public int getGoalsFor() { return goalsFor; }
    public void setGoalsFor(int goalsFor) { this.goalsFor = goalsFor; }

    public int getGoalsAgainst() { return goalsAgainst; }
    public void setGoalsAgainst(int goalsAgainst) { this.goalsAgainst = goalsAgainst; }

    public int getPoints() { return points; }
    public void setPoints(int points) { this.points = points; }

    public boolean isUserTeam() { return isUserTeam; }
    public void setUserTeam(boolean userTeam) { isUserTeam = userTeam; }

    public String getBadgeUrl() { return badgeUrl; }
    public void setBadgeUrl(String badgeUrl) { this.badgeUrl = badgeUrl; }

    public int getGoalDiff() { return goalsFor - goalsAgainst; }
}
