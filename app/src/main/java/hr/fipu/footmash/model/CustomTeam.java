package hr.fipu.footmash.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;
import hr.fipu.footmash.db.Converters;

import java.util.List;

/**
 * Model za korisnikov kreirani tim.
 * Sprema se u Room bazu podataka.
 */
@Entity(tableName = "custom_teams")
@TypeConverters(Converters.class)
public class CustomTeam {

    @PrimaryKey(autoGenerate = true)
    private int id;

    private String name;
    private String formation; // 4-4-2, 4-3-3, 3-5-2 itd.

    // Prosječni atributi tima (izračunati iz igrača)
    private int avgPace;
    private int avgShooting;
    private int avgPassing;
    private int avgDribbling;
    private int avgDefending;
    private int avgPhysical;

    // Lista ID-ova custom igrača u timu
    private List<Integer> playerIds;

    // Referenca na ligu u koju je ubačen
    private int targetLeagueId;
    private String targetLeagueName;
    private int targetSeason;

    // Getteri i setteri
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getFormation() { return formation; }
    public void setFormation(String formation) { this.formation = formation; }

    public int getAvgPace() { return avgPace; }
    public void setAvgPace(int avgPace) { this.avgPace = avgPace; }

    public int getAvgShooting() { return avgShooting; }
    public void setAvgShooting(int avgShooting) { this.avgShooting = avgShooting; }

    public int getAvgPassing() { return avgPassing; }
    public void setAvgPassing(int avgPassing) { this.avgPassing = avgPassing; }

    public int getAvgDribbling() { return avgDribbling; }
    public void setAvgDribbling(int avgDribbling) { this.avgDribbling = avgDribbling; }

    public int getAvgDefending() { return avgDefending; }
    public void setAvgDefending(int avgDefending) { this.avgDefending = avgDefending; }

    public int getAvgPhysical() { return avgPhysical; }
    public void setAvgPhysical(int avgPhysical) { this.avgPhysical = avgPhysical; }

    public List<Integer> getPlayerIds() { return playerIds; }
    public void setPlayerIds(List<Integer> playerIds) { this.playerIds = playerIds; }

    public int getTargetLeagueId() { return targetLeagueId; }
    public void setTargetLeagueId(int targetLeagueId) { this.targetLeagueId = targetLeagueId; }

    public String getTargetLeagueName() { return targetLeagueName; }
    public void setTargetLeagueName(String targetLeagueName) { this.targetLeagueName = targetLeagueName; }

    public int getTargetSeason() { return targetSeason; }
    public void setTargetSeason(int targetSeason) { this.targetSeason = targetSeason; }

    public int getOverall() {
        return (avgPace + avgShooting + avgPassing + avgDribbling + avgDefending + avgPhysical) / 6;
    }
}
