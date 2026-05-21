package hr.fipu.footmash.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "real_players")
public class RealPlayer {

    @PrimaryKey
    private int id;

    private String name;
    private String position;
    private String nationality;
    private int age;

    private int pace;
    private int shooting;
    private int passing;
    private int dribbling;
    private int defending;
    private int physical;
    private int overall;

    private int teamId;
    private String teamName;
    private int leagueId;
    private String leagueName;

    /**
     * Dynamic form: points added to {@link #overall} from in-season performance
     * (goals, assists, results). 0 at the start of every season; see
     * {@code SeasonRepository.applyPlayerGrowth}.
     */
    private int formDelta;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPosition() { return position; }
    public void setPosition(String position) { this.position = position; }

    public String getNationality() { return nationality; }
    public void setNationality(String nationality) { this.nationality = nationality; }

    public int getAge() { return age; }
    public void setAge(int age) { this.age = age; }

    public int getPace() { return pace; }
    public void setPace(int pace) { this.pace = pace; }

    public int getShooting() { return shooting; }
    public void setShooting(int shooting) { this.shooting = shooting; }

    public int getPassing() { return passing; }
    public void setPassing(int passing) { this.passing = passing; }

    public int getDribbling() { return dribbling; }
    public void setDribbling(int dribbling) { this.dribbling = dribbling; }

    public int getDefending() { return defending; }
    public void setDefending(int defending) { this.defending = defending; }

    public int getPhysical() { return physical; }
    public void setPhysical(int physical) { this.physical = physical; }

    public int getOverall() { return overall; }
    public void setOverall(int overall) { this.overall = overall; }

    public int getFormDelta() { return formDelta; }
    public void setFormDelta(int formDelta) { this.formDelta = formDelta; }

    /** Overall rating including in-season form growth/decline. */
    public int getEffectiveOverall() {
        return overall + formDelta;
    }

    public int getTeamId() { return teamId; }
    public void setTeamId(int teamId) { this.teamId = teamId; }

    public String getTeamName() { return teamName; }
    public void setTeamName(String teamName) { this.teamName = teamName; }

    public int getLeagueId() { return leagueId; }
    public void setLeagueId(int leagueId) { this.leagueId = leagueId; }

    public String getLeagueName() { return leagueName; }
    public void setLeagueName(String leagueName) { this.leagueName = leagueName; }

    public long getMarketValue() {
        if (overall >= 90) return 70_000_000L;
        if (overall >= 85) return 42_000_000L;
        if (overall >= 80) return 22_000_000L;
        if (overall >= 75) return 10_000_000L;
        if (overall >= 70) return 4_000_000L;
        return 1_000_000L;
    }
}
