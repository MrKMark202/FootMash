package hr.fipu.footmash.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Model za korisnikov kreirani igrač.
 * Sprema se u Room bazu podataka.
 */
@Entity(tableName = "custom_players")
public class CustomPlayer {

    @PrimaryKey(autoGenerate = true)
    private int id;

    private String firstName;
    private String lastName;
    private int age;
    private String position; // GK, DF, MF, FW
    private String nationality;

    // Atributi igrača (0-100)
    private int pace;
    private int shooting;
    private int passing;
    private int dribbling;
    private int defending;
    private int physical;

    // Referenca na klub u koji je ubačen
    private int targetTeamId;
    private String targetTeamName;
    private String targetTeamLogo;

    // Referenca na ligu
    private int targetLeagueId;
    private int targetSeason;

    // Getteri i setteri
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public int getAge() { return age; }
    public void setAge(int age) { this.age = age; }

    public String getPosition() { return position; }
    public void setPosition(String position) { this.position = position; }

    public String getNationality() { return nationality; }
    public void setNationality(String nationality) { this.nationality = nationality; }

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

    public int getTargetTeamId() { return targetTeamId; }
    public void setTargetTeamId(int targetTeamId) { this.targetTeamId = targetTeamId; }

    public String getTargetTeamName() { return targetTeamName; }
    public void setTargetTeamName(String targetTeamName) { this.targetTeamName = targetTeamName; }

    public String getTargetTeamLogo() { return targetTeamLogo; }
    public void setTargetTeamLogo(String targetTeamLogo) { this.targetTeamLogo = targetTeamLogo; }

    public int getTargetLeagueId() { return targetLeagueId; }
    public void setTargetLeagueId(int targetLeagueId) { this.targetLeagueId = targetLeagueId; }

    public int getTargetSeason() { return targetSeason; }
    public void setTargetSeason(int targetSeason) { this.targetSeason = targetSeason; }

    public int getOverall() {
        return (pace + shooting + passing + dribbling + defending + physical) / 6;
    }

    public String getFullName() {
        return firstName + " " + lastName;
    }
}
