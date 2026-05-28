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

    /**
     * Comma-separated Trait enum names (e.g. "GOAL_POACHER,CLINICAL_FINISHER").
     * Populated by the create-player wizard; not used by simulation yet.
     */
    private String traits;

    /**
     * Unspent attribute points earned from the last simulated season. The
     * career hub gates "Simuliraj sezonu" while this is &gt; 0 — the user must
     * spend the points first via the stat-growth screen.
     */
    private int pointsToSpend;

    /**
     * Current season year. Initially equals {@link #targetSeason}; advances by
     * one every time the career engine simulates a season for this player.
     */
    private int currentSeasonYear;

    /**
     * The {@code seasonsAtCurrentClub} count at which the user most recently
     * dismissed a transfer window (chose to stay). {@code -1} means no
     * dismissal yet. Used to wait 2 more seasons before offering again, so
     * declining doesn't lock the player out of further sim turns. Reset to
     * {@code -1} whenever the player transfers to a new club.
     */
    private int transferDismissedAt = -1;

    /**
     * Position inside the current season cycle:
     * <ul>
     *   <li>0 — pre-autumn: CTA simulates the first half (~19 matches)</li>
     *   <li>1 — post-autumn: CTA opens the winter transfer window</li>
     *   <li>2 — post-winter: CTA simulates the second half and finalises the season</li>
     * </ul>
     * Returns to 0 after the spring sim writes the consolidated season record.
     */
    private int seasonHalfState;

    // Running tally of the first-half outcome. Held in scratch fields on the
    // player row (rather than a separate table) because they reset every year
    // and never coexist with another in-flight half.
    private int halfSeasonApps;
    private int halfSeasonGoals;
    private int halfSeasonAssists;
    /** Sum of {@code avgRating * apps} so the combined avg can be recomputed at season end. */
    private float halfSeasonRatingTotal;

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

    public String getTraits() { return traits; }
    public void setTraits(String traits) { this.traits = traits; }

    public int getPointsToSpend() { return pointsToSpend; }
    public void setPointsToSpend(int pointsToSpend) { this.pointsToSpend = pointsToSpend; }

    public int getCurrentSeasonYear() { return currentSeasonYear; }
    public void setCurrentSeasonYear(int currentSeasonYear) {
        this.currentSeasonYear = currentSeasonYear;
    }

    public int getTransferDismissedAt() { return transferDismissedAt; }
    public void setTransferDismissedAt(int transferDismissedAt) {
        this.transferDismissedAt = transferDismissedAt;
    }

    public int getSeasonHalfState() { return seasonHalfState; }
    public void setSeasonHalfState(int seasonHalfState) {
        this.seasonHalfState = seasonHalfState;
    }

    public int getHalfSeasonApps() { return halfSeasonApps; }
    public void setHalfSeasonApps(int v) { this.halfSeasonApps = v; }

    public int getHalfSeasonGoals() { return halfSeasonGoals; }
    public void setHalfSeasonGoals(int v) { this.halfSeasonGoals = v; }

    public int getHalfSeasonAssists() { return halfSeasonAssists; }
    public void setHalfSeasonAssists(int v) { this.halfSeasonAssists = v; }

    public float getHalfSeasonRatingTotal() { return halfSeasonRatingTotal; }
    public void setHalfSeasonRatingTotal(float v) { this.halfSeasonRatingTotal = v; }

    /** Clears the half-season scratch fields. Called after the spring sim consolidates. */
    public void clearHalfSeasonScratch() {
        halfSeasonApps = 0;
        halfSeasonGoals = 0;
        halfSeasonAssists = 0;
        halfSeasonRatingTotal = 0f;
    }

    public int getOverall() {
        return (pace + shooting + passing + dribbling + defending + physical) / 6;
    }

    public String getFullName() {
        return firstName + " " + lastName;
    }
}
