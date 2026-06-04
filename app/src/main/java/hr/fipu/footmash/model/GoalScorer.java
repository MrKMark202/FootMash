package hr.fipu.footmash.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "goal_scorer")
public class GoalScorer {

    @PrimaryKey(autoGenerate = true)
    private int id;

    private int seasonId;
    private int fixtureId;
    private String playerName;
    private String assistName;
    private String teamName;
    private int minute;
    private boolean isUserTeamPlayer;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getSeasonId() { return seasonId; }
    public void setSeasonId(int seasonId) { this.seasonId = seasonId; }

    public int getFixtureId() { return fixtureId; }
    public void setFixtureId(int fixtureId) { this.fixtureId = fixtureId; }

    public String getPlayerName() { return playerName; }
    public void setPlayerName(String playerName) { this.playerName = playerName; }

    public String getAssistName() { return assistName; }
    public void setAssistName(String assistName) { this.assistName = assistName; }

    public String getTeamName() { return teamName; }
    public void setTeamName(String teamName) { this.teamName = teamName; }

    public int getMinute() { return minute; }
    public void setMinute(int minute) { this.minute = minute; }

    public boolean isUserTeamPlayer() { return isUserTeamPlayer; }
    public void setUserTeamPlayer(boolean userTeamPlayer) { isUserTeamPlayer = userTeamPlayer; }
}
