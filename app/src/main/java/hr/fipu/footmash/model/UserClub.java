package hr.fipu.footmash.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "user_club")
public class UserClub {

    @PrimaryKey(autoGenerate = true)
    private int id;

    private String clubName;
    private int leagueId;
    private String leagueName;
    private String formation;
    private long budget;
    private int seasonYear;
    private boolean isActive;

    public static final long STARTING_BUDGET = 100_000_000L;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getClubName() { return clubName; }
    public void setClubName(String clubName) { this.clubName = clubName; }

    public int getLeagueId() { return leagueId; }
    public void setLeagueId(int leagueId) { this.leagueId = leagueId; }

    public String getLeagueName() { return leagueName; }
    public void setLeagueName(String leagueName) { this.leagueName = leagueName; }

    public String getFormation() { return formation; }
    public void setFormation(String formation) { this.formation = formation; }

    public long getBudget() { return budget; }
    public void setBudget(long budget) { this.budget = budget; }

    public int getSeasonYear() { return seasonYear; }
    public void setSeasonYear(int seasonYear) { this.seasonYear = seasonYear; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
}
