package hr.fipu.footmash.ui.ailab.create;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import hr.fipu.footmash.model.RealTeam;
import hr.fipu.footmash.model.Trait;
import hr.fipu.footmash.season.TraitEngine;

/**
 * Shared state for the multi-step player creation wizard. Scoped to the
 * MainActivity so all wizard fragments observe the same instance. Callers
 * must invoke {@link #reset()} when entering step 1 to clear any state
 * left over from a previous run.
 *
 * <p>Wizard layout:
 * <ol>
 *     <li>Identity — first name, last name, nationality</li>
 *     <li>Stats + position — 100 points across 6 stats on a 50 baseline</li>
 *     <li>Preview + traits — pick up to 3 traits filtered by position group</li>
 *     <li>Club offers — league pick → 3 OVR-weighted clubs</li>
 *     <li>Signed — confirmation and persistence</li>
 * </ol>
 */
public class PlayerCreationViewModel extends ViewModel {

    public static final int STAT_BASELINE   = 50;
    public static final int STAT_CAP        = 99;
    public static final int POINTS_TO_SPEND = 100;
    public static final int STAT_COUNT      = 6;
    public static final int MAX_TRAITS      = 3;

    /** Stat array indices. Matches the row order shown in the UI. */
    public static final int IDX_PACE      = 0;
    public static final int IDX_SHOOTING  = 1;
    public static final int IDX_PASSING   = 2;
    public static final int IDX_DRIBBLING = 3;
    public static final int IDX_DEFENDING = 4;
    public static final int IDX_PHYSICAL  = 5;

    // ─── Step 1: identity ────────────────────────────────────────────────────
    private final MutableLiveData<String> firstName   = new MutableLiveData<>("");
    private final MutableLiveData<String> lastName    = new MutableLiveData<>("");
    private final MutableLiveData<String> nationality = new MutableLiveData<>("");

    // ─── Step 2: stats + position ────────────────────────────────────────────
    private final MutableLiveData<String> position = new MutableLiveData<>("");
    /** Additions on top of {@link #STAT_BASELINE} — sum must reach POINTS_TO_SPEND. */
    private final MutableLiveData<int[]> statAdditions =
            new MutableLiveData<>(new int[STAT_COUNT]);

    // ─── Step 3: traits ──────────────────────────────────────────────────────
    private final MutableLiveData<List<Trait>> selectedTraits =
            new MutableLiveData<>(new ArrayList<>());

    // ─── Step 4: club offers ─────────────────────────────────────────────────
    private final MutableLiveData<Integer> selectedLeagueId = new MutableLiveData<>(null);
    private final MutableLiveData<String>  selectedLeagueName = new MutableLiveData<>("");
    private final MutableLiveData<List<ClubOfferEngine.RankedTeam>> currentOffers =
            new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<RealTeam> selectedClub = new MutableLiveData<>(null);

    /** Clears every wizard field. Call from step 1 onViewCreated. */
    public void reset() {
        firstName.setValue("");
        lastName.setValue("");
        nationality.setValue("");
        position.setValue("");
        statAdditions.setValue(new int[STAT_COUNT]);
        selectedTraits.setValue(new ArrayList<>());
        selectedLeagueId.setValue(null);
        selectedLeagueName.setValue("");
        currentOffers.setValue(new ArrayList<>());
        selectedClub.setValue(null);
    }

    public LiveData<String> getFirstName()   { return firstName; }
    public LiveData<String> getLastName()    { return lastName; }
    public LiveData<String> getNationality() { return nationality; }

    public void setFirstName(String v)   { firstName.setValue(v == null ? "" : v); }
    public void setLastName(String v)    { lastName.setValue(v == null ? "" : v); }
    public void setNationality(String v) { nationality.setValue(v == null ? "" : v); }

    public boolean isStep1Valid() {
        return notBlank(firstName.getValue())
            && notBlank(lastName.getValue())
            && notBlank(nationality.getValue());
    }

    // ─── Step 2 accessors ────────────────────────────────────────────────────

    public LiveData<String>  getPosition()       { return position; }
    public LiveData<int[]>   getStatAdditions()  { return statAdditions; }

    public void setPosition(String v) { position.setValue(v == null ? "" : v); }

    /**
     * Sets the addition for one stat, clamped to the remaining-budget envelope.
     * Returns the actual value applied — never above POINTS_TO_SPEND total and
     * never above STAT_CAP - STAT_BASELINE for an individual stat.
     */
    public int trySetStatAddition(int index, int requested) {
        if (index < 0 || index >= STAT_COUNT) return 0;
        int perStatMax = STAT_CAP - STAT_BASELINE;
        int clamped = Math.max(0, Math.min(perStatMax, requested));
        int[] cur = currentStatsCopy();
        int budgetForThisStat = POINTS_TO_SPEND - pointsSpent(cur) + cur[index];
        clamped = Math.min(clamped, budgetForThisStat);
        cur[index] = clamped;
        statAdditions.setValue(cur);
        return clamped;
    }

    /** Convenience: bump a stat by {@code delta} (positive or negative). */
    public int bumpStat(int index, int delta) {
        if (index < 0 || index >= STAT_COUNT) return 0;
        int[] cur = currentStatsCopy();
        return trySetStatAddition(index, cur[index] + delta);
    }

    public int pointsSpent() {
        return pointsSpent(statAdditions.getValue());
    }

    public int pointsRemaining() {
        return POINTS_TO_SPEND - pointsSpent();
    }

    public int statValueAt(int index) {
        int[] adds = statAdditions.getValue();
        if (adds == null || index < 0 || index >= STAT_COUNT) return STAT_BASELINE;
        return STAT_BASELINE + adds[index];
    }

    public int currentOverall() {
        return (STAT_COUNT * STAT_BASELINE + pointsSpent()) / STAT_COUNT;
    }

    public boolean isStep2Valid() {
        return notBlank(position.getValue()) && pointsSpent() == POINTS_TO_SPEND;
    }

    // ─── Step 3 accessors ────────────────────────────────────────────────────

    public LiveData<List<Trait>> getSelectedTraits() { return selectedTraits; }

    /** Position group derived from the selected position ("GK"/"DF"/"MF"/"FW"). */
    public String positionGroup() {
        return TraitEngine.groupOf(position.getValue());
    }

    /** Traits eligible for the current position group, in enum order. */
    public List<Trait> eligibleTraits() {
        String group = positionGroup();
        List<Trait> result = new ArrayList<>();
        for (Trait t : Trait.values()) {
            if (t.group.equals(group)) result.add(t);
        }
        return Collections.unmodifiableList(result);
    }

    /**
     * Toggles a trait in the selection. Adding fails when the cap is reached
     * or the trait doesn't belong to the position group. Returns the resulting
     * state ({@code true} = trait is now selected, {@code false} = not selected).
     */
    public boolean toggleTrait(Trait t) {
        if (t == null || !t.group.equals(positionGroup())) return false;
        List<Trait> cur = new ArrayList<>(currentTraits());
        if (cur.contains(t)) {
            cur.remove(t);
            selectedTraits.setValue(cur);
            return false;
        }
        if (cur.size() >= MAX_TRAITS) return cur.contains(t);
        cur.add(t);
        selectedTraits.setValue(cur);
        return true;
    }

    public boolean isTraitSelected(Trait t) {
        return currentTraits().contains(t);
    }

    public int traitsSelectedCount() {
        return currentTraits().size();
    }

    private List<Trait> currentTraits() {
        List<Trait> v = selectedTraits.getValue();
        return v == null ? Collections.emptyList() : v;
    }

    public boolean isStep3Valid() {
        // The user must pick at least one trait. Zero feels like an oversight,
        // not a deliberate "no traits" choice.
        return !currentTraits().isEmpty();
    }

    // ─── Step 4 accessors ────────────────────────────────────────────────────

    public LiveData<Integer> getSelectedLeagueId()   { return selectedLeagueId; }
    public LiveData<String>  getSelectedLeagueName() { return selectedLeagueName; }
    public LiveData<List<ClubOfferEngine.RankedTeam>> getCurrentOffers() { return currentOffers; }
    public LiveData<RealTeam> getSelectedClub() { return selectedClub; }

    public void setSelectedLeague(int leagueId, String leagueName) {
        selectedLeagueId.setValue(leagueId);
        selectedLeagueName.setValue(leagueName == null ? "" : leagueName);
        // Picking a new league invalidates any prior offers + selection.
        currentOffers.setValue(new ArrayList<>());
        selectedClub.setValue(null);
    }

    public void setCurrentOffers(List<ClubOfferEngine.RankedTeam> offers) {
        currentOffers.setValue(offers == null ? new ArrayList<>() : offers);
        // Re-generating offers also clears any prior selection.
        selectedClub.setValue(null);
    }

    public void selectClub(RealTeam team) {
        selectedClub.setValue(team);
    }

    public boolean isStep4Valid() {
        return selectedClub.getValue() != null;
    }

    private int[] currentStatsCopy() {
        int[] v = statAdditions.getValue();
        if (v == null) return new int[STAT_COUNT];
        return v.clone();
    }

    private static int pointsSpent(int[] adds) {
        if (adds == null) return 0;
        int total = 0;
        for (int v : adds) total += v;
        return total;
    }

    public String fullName() {
        String first = firstName.getValue() == null ? "" : firstName.getValue().trim();
        String last  = lastName.getValue()  == null ? "" : lastName.getValue().trim();
        if (first.isEmpty()) return last;
        if (last.isEmpty())  return first;
        return first + " " + last;
    }

    private static boolean notBlank(String s) {
        return s != null && !s.trim().isEmpty();
    }
}
