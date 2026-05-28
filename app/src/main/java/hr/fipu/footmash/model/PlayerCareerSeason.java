package hr.fipu.footmash.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * One row per simulated season for a {@link CustomPlayer}. Persisted so the
 * career hub can render the full history, the transfer-window engine can count
 * consecutive seasons at the same club, and the user can see how their
 * stat-growth decisions affected later seasons.
 *
 * <p>Rows are inserted by {@code SimulateSeasonFragment} after the
 * {@code PlayerCareerEngine} returns an outcome; they are read-only after
 * insert (no editing past results).
 */
@Entity(tableName = "player_career_season")
public class PlayerCareerSeason {

    @PrimaryKey(autoGenerate = true)
    private int id;

    private int playerId;
    private int seasonYear;
    private int clubId;
    private String clubName;
    private int leagueId;

    private int appearances;
    private int goals;
    private int assists;
    private float avgRating;
    private int clubFinalPosition;
    private int pointsEarned;
    private int ovrAtSeasonEnd;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getPlayerId() { return playerId; }
    public void setPlayerId(int playerId) { this.playerId = playerId; }

    public int getSeasonYear() { return seasonYear; }
    public void setSeasonYear(int seasonYear) { this.seasonYear = seasonYear; }

    public int getClubId() { return clubId; }
    public void setClubId(int clubId) { this.clubId = clubId; }

    public String getClubName() { return clubName; }
    public void setClubName(String clubName) { this.clubName = clubName; }

    public int getLeagueId() { return leagueId; }
    public void setLeagueId(int leagueId) { this.leagueId = leagueId; }

    public int getAppearances() { return appearances; }
    public void setAppearances(int appearances) { this.appearances = appearances; }

    public int getGoals() { return goals; }
    public void setGoals(int goals) { this.goals = goals; }

    public int getAssists() { return assists; }
    public void setAssists(int assists) { this.assists = assists; }

    public float getAvgRating() { return avgRating; }
    public void setAvgRating(float avgRating) { this.avgRating = avgRating; }

    public int getClubFinalPosition() { return clubFinalPosition; }
    public void setClubFinalPosition(int clubFinalPosition) {
        this.clubFinalPosition = clubFinalPosition;
    }

    public int getPointsEarned() { return pointsEarned; }
    public void setPointsEarned(int pointsEarned) { this.pointsEarned = pointsEarned; }

    public int getOvrAtSeasonEnd() { return ovrAtSeasonEnd; }
    public void setOvrAtSeasonEnd(int ovrAtSeasonEnd) {
        this.ovrAtSeasonEnd = ovrAtSeasonEnd;
    }

    /** Season formatted for display, e.g. "2025/26". */
    public String getSeasonLabel() {
        return seasonYear + "/" + String.format("%02d", (seasonYear + 1) % 100);
    }
}
