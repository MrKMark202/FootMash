package hr.fipu.footmash.ui.season;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import hr.fipu.footmash.db.AppDatabase;
import hr.fipu.footmash.model.FormationSlot;
import hr.fipu.footmash.model.RealPlayer;
import hr.fipu.footmash.model.SynergyResult;
import hr.fipu.footmash.model.UserClub;
import hr.fipu.footmash.model.UserSquad;
import hr.fipu.footmash.season.FormationCatalog;
import hr.fipu.footmash.season.TraitEngine;

/**
 * Unified state for the draft / squad-editor canvas — pitch slots, the substitutes
 * bench, budget, formation, and the slot-targeted market modal. Lives at activity
 * scope; {@link #init} fully resets state.
 *
 * <p>Two modes share this screen:
 * <ul>
 *   <li><b>Draft</b> — initial squad building: buy starters and bench players within budget.</li>
 *   <li><b>Edit</b> ({@link #isEditMode()}) — mid-season management: rearrange owned
 *       players between the XI and the bench and change formation. No buying/selling.</li>
 * </ul>
 */
public class DraftViewModel extends AndroidViewModel {

    public enum DraftMode { NEW_CLUB, EXISTING_CLUB }

    /** Sentinel slot key used when the market modal is buying a bench player. */
    public static final String BENCH_KEY = "__bench__";
    /** Maximum substitutes on the bench (squad = 11 + bench). */
    public static final int MAX_BENCH = 7;

    private final AppDatabase db;

    private int clubId = -1;
    private int leagueId = -1;
    private DraftMode mode = DraftMode.NEW_CLUB;
    private boolean editMode = false;

    private final MutableLiveData<String> formation = new MutableLiveData<>(FormationCatalog.DEFAULT);
    private final MutableLiveData<Map<String, RealPlayer>> assignments =
            new MutableLiveData<>(new LinkedHashMap<>());
    private final MutableLiveData<List<RealPlayer>> bench =
            new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Long> remainingBudget = new MutableLiveData<>(0L);
    private final MutableLiveData<String> selectedSlotKey = new MutableLiveData<>(null);

    private final MediatorLiveData<List<RealPlayer>> marketCandidates = new MediatorLiveData<>();
    private final MediatorLiveData<SynergyResult> synergy = new MediatorLiveData<>();
    private LiveData<List<RealPlayer>> leaguePlayers;

    public DraftViewModel(@NonNull Application application) {
        super(application);
        db = AppDatabase.getInstance(application);

        // Live trait synergy of whoever is currently placed on the pitch.
        synergy.setValue(SynergyResult.empty());
        synergy.addSource(assignments, a -> {
            List<RealPlayer> xi = (a != null)
                    ? new ArrayList<>(a.values()) : new ArrayList<>();
            synergy.setValue(TraitEngine.computeSynergy(xi));
        });
    }

    /** Reset and load draft state for the given club. Safe to call repeatedly. */
    public void init(int clubId, int leagueId, DraftMode mode, boolean editMode) {
        this.clubId = clubId;
        this.leagueId = leagueId;
        this.mode = mode;
        this.editMode = editMode;

        if (leaguePlayers != null) marketCandidates.removeSource(leaguePlayers);
        marketCandidates.removeSource(assignments);
        marketCandidates.removeSource(bench);
        marketCandidates.removeSource(selectedSlotKey);
        marketCandidates.removeSource(remainingBudget);

        leaguePlayers = db.realPlayerDao().getPlayersByLeague(leagueId);

        marketCandidates.addSource(leaguePlayers, p -> recomputeMarket());
        marketCandidates.addSource(assignments, p -> recomputeMarket());
        marketCandidates.addSource(bench, p -> recomputeMarket());
        marketCandidates.addSource(selectedSlotKey, p -> recomputeMarket());
        marketCandidates.addSource(remainingBudget, p -> recomputeMarket());

        assignments.setValue(new LinkedHashMap<>());
        bench.setValue(new ArrayList<>());

        new Thread(() -> {
            UserClub club = db.userClubDao().getClubByIdSync(clubId);
            if (club == null) return;

            String f = (club.getFormation() != null
                    && FormationCatalog.FORMATIONS.containsKey(club.getFormation()))
                    ? club.getFormation()
                    : FormationCatalog.DEFAULT;
            formation.postValue(f);
            remainingBudget.postValue(club.getBudget());

            // Restore the starting XI (by pitch slot) and the bench from user_squad.
            List<UserSquad> existing = db.userClubDao().getSquadByClubSync(clubId);
            if (existing != null && !existing.isEmpty()) {
                Map<String, RealPlayer> seededXi = new LinkedHashMap<>();
                List<RealPlayer> seededBench = new ArrayList<>();
                for (UserSquad sq : existing) {
                    RealPlayer p = db.realPlayerDao().getPlayerById(sq.getPlayerId());
                    if (p == null) continue;
                    if (sq.isStartingXI() && sq.getPitchPosition() != null) {
                        seededXi.put(sq.getPitchPosition(), p);
                    } else {
                        seededBench.add(p);
                    }
                }
                assignments.postValue(seededXi);
                bench.postValue(seededBench);
            }
        }).start();
    }

    public void selectSlot(@Nullable String slotKey) {
        selectedSlotKey.setValue(slotKey);
    }

    /** Opens the market modal to buy a bench player (no position filter). */
    public void selectBench() {
        selectedSlotKey.setValue(BENCH_KEY);
    }

    /** Buys the player into the currently-selected slot, deducting their market value. */
    public void buyForSlot(String slotKey, RealPlayer player) {
        if (slotKey == null || player == null) return;
        new Thread(() -> {
            UserClub club = db.userClubDao().getClubByIdSync(clubId);
            if (club == null) return;
            long cost = player.getMarketValue();
            if (club.getBudget() < cost) return;

            Map<String, RealPlayer> current = currentAssignments();
            RealPlayer previous = current.get(slotKey);
            long refund = 0L;
            if (previous != null) {
                db.userClubDao().removePlayerFromSquad(previous.getId(), clubId);
                refund = previous.getMarketValue();
            }

            UserSquad sq = new UserSquad();
            sq.setClubId(clubId);
            sq.setPlayerId(player.getId());
            sq.setStartingXI(true);
            sq.setPitchPosition(slotKey);
            db.userClubDao().insertSquadPlayer(sq);

            long newBudget = club.getBudget() + refund - cost;
            club.setBudget(newBudget);
            db.userClubDao().updateClub(club);

            current.put(slotKey, player);
            assignments.postValue(current);
            remainingBudget.postValue(newBudget);
            selectedSlotKey.postValue(null);
        }).start();
    }

    /** Buys a player onto the bench (substitute), deducting their market value. */
    public void buyForBench(RealPlayer player) {
        if (player == null) return;
        new Thread(() -> {
            UserClub club = db.userClubDao().getClubByIdSync(clubId);
            if (club == null) return;
            List<RealPlayer> b = currentBench();
            if (b.size() >= MAX_BENCH) return;
            long cost = player.getMarketValue();
            if (club.getBudget() < cost) return;

            UserSquad sq = new UserSquad();
            sq.setClubId(clubId);
            sq.setPlayerId(player.getId());
            sq.setStartingXI(false);
            sq.setPitchPosition(null);
            db.userClubDao().insertSquadPlayer(sq);

            long newBudget = club.getBudget() - cost;
            club.setBudget(newBudget);
            db.userClubDao().updateClub(club);

            b.add(player);
            bench.postValue(b);
            remainingBudget.postValue(newBudget);
            selectedSlotKey.postValue(null);
        }).start();
    }

    /** Sells the player currently in this slot, refunding their market value. */
    public void sellFromSlot(String slotKey) {
        if (slotKey == null) return;
        new Thread(() -> {
            Map<String, RealPlayer> current = currentAssignments();
            RealPlayer player = current.get(slotKey);
            if (player == null) return;

            db.userClubDao().removePlayerFromSquad(player.getId(), clubId);

            UserClub club = db.userClubDao().getClubByIdSync(clubId);
            if (club == null) return;
            long newBudget = club.getBudget() + player.getMarketValue();
            club.setBudget(newBudget);
            db.userClubDao().updateClub(club);

            current.remove(slotKey);
            assignments.postValue(current);
            remainingBudget.postValue(newBudget);
        }).start();
    }

    /** Sells a bench player, refunding their market value. */
    public void sellFromBench(RealPlayer player) {
        if (player == null) return;
        new Thread(() -> {
            db.userClubDao().removePlayerFromSquad(player.getId(), clubId);
            UserClub club = db.userClubDao().getClubByIdSync(clubId);
            if (club == null) return;
            long newBudget = club.getBudget() + player.getMarketValue();
            club.setBudget(newBudget);
            db.userClubDao().updateClub(club);

            List<RealPlayer> b = currentBench();
            removeById(b, player.getId());
            bench.postValue(b);
            remainingBudget.postValue(newBudget);
        }).start();
    }

    /** Moves the player in {@code slotKey} from the starting XI to the bench. */
    public void moveToBench(String slotKey) {
        if (slotKey == null) return;
        new Thread(() -> {
            Map<String, RealPlayer> a = currentAssignments();
            RealPlayer p = a.get(slotKey);
            if (p == null) return;
            persistSquadRow(p.getId(), false, null);
            a.remove(slotKey);
            List<RealPlayer> b = currentBench();
            b.add(p);
            assignments.postValue(a);
            bench.postValue(b);
        }).start();
    }

    /** Moves a bench player into a pitch slot; any current occupant drops to the bench. */
    public void assignFromBench(RealPlayer player, String slotKey) {
        if (player == null || slotKey == null) return;
        new Thread(() -> {
            Map<String, RealPlayer> a = currentAssignments();
            List<RealPlayer> b = currentBench();

            RealPlayer prev = a.get(slotKey);
            if (prev != null) {
                persistSquadRow(prev.getId(), false, null);
                b.add(prev);
            }
            persistSquadRow(player.getId(), true, slotKey);
            removeById(b, player.getId());
            a.put(slotKey, player);

            assignments.postValue(a);
            bench.postValue(b);
        }).start();
    }

    /** Switches formation, best-fitting current starters into the new slots. */
    public void changeFormation(String newFormation) {
        if (newFormation == null || !FormationCatalog.FORMATIONS.containsKey(newFormation)) return;
        new Thread(() -> {
            List<RealPlayer> pool = new ArrayList<>(currentAssignments().values());
            List<RealPlayer> newBench = currentBench();
            Map<String, RealPlayer> newXi = new LinkedHashMap<>();
            Set<Integer> used = new HashSet<>();

            for (FormationSlot slot : FormationCatalog.get(newFormation)) {
                for (RealPlayer p : pool) {
                    if (used.contains(p.getId())) continue;
                    if (!FormationCatalog.matchesPosGroup(p.getPosition(), slot.posGroup)) continue;
                    newXi.put(slot.key, p);
                    used.add(p.getId());
                    break;
                }
            }
            for (RealPlayer p : pool) {
                if (!used.contains(p.getId())) newBench.add(p);
            }

            UserClub club = db.userClubDao().getClubByIdSync(clubId);
            if (club != null) {
                club.setFormation(newFormation);
                db.userClubDao().updateClub(club);
            }
            for (Map.Entry<String, RealPlayer> e : newXi.entrySet()) {
                persistSquadRow(e.getValue().getId(), true, e.getKey());
            }
            for (RealPlayer p : newBench) {
                persistSquadRow(p.getId(), false, null);
            }

            formation.postValue(newFormation);
            assignments.postValue(newXi);
            bench.postValue(newBench);
        }).start();
    }

    private void persistSquadRow(int playerId, boolean startingXI, String pitchPosition) {
        UserSquad sq = db.userClubDao().getSquadEntryByPlayer(playerId, clubId);
        if (sq == null) return;
        sq.setStartingXI(startingXI);
        sq.setPitchPosition(pitchPosition);
        db.userClubDao().updateSquadPlayer(sq);
    }

    public boolean isLineupComplete() {
        return validateLineup().legal;
    }

    public static class LineupValidation {
        public final boolean legal;
        public final List<String> reasons;
        public LineupValidation(boolean legal, List<String> reasons) {
            this.legal = legal;
            this.reasons = reasons;
        }
    }

    public LineupValidation validateLineup() {
        Map<String, RealPlayer> a = assignments.getValue();
        Long b = remainingBudget.getValue();
        String f = formation.getValue();
        List<String> reasons = new ArrayList<>();

        if (f == null || a == null || b == null) {
            reasons.add("Stanje sastava nije učitano");
            return new LineupValidation(false, reasons);
        }

        List<FormationSlot> slots = FormationCatalog.get(f);
        int needed = slots.size();
        int filled = a.size();
        if (filled < needed) {
            reasons.add("Postavljeno " + filled + "/" + needed + " igrača");
        }

        if (b < 0) {
            reasons.add(String.format("Budžet je negativan: €%.1fM", b / 1_000_000.0));
        }

        int gkCount = 0;
        for (RealPlayer p : a.values()) {
            if ("GK".equals(p.getPosition())) gkCount++;
        }
        if (filled == needed && gkCount != 1) {
            reasons.add("Sastav mora imati točno jednog vratara (trenutno: " + gkCount + ")");
        }

        Set<Integer> seen = new HashSet<>();
        for (RealPlayer p : a.values()) {
            if (!seen.add(p.getId())) {
                reasons.add("Igrač " + p.getName() + " je dodijeljen više puta");
                break;
            }
        }

        return new LineupValidation(reasons.isEmpty(), reasons);
    }

    public LiveData<String> getFormation() { return formation; }
    public LiveData<Map<String, RealPlayer>> getAssignments() { return assignments; }
    public LiveData<List<RealPlayer>> getBench() { return bench; }
    public LiveData<Long> getRemainingBudget() { return remainingBudget; }
    public LiveData<String> getSelectedSlotKey() { return selectedSlotKey; }
    public LiveData<List<RealPlayer>> getMarketCandidates() { return marketCandidates; }
    public LiveData<SynergyResult> getSynergy() { return synergy; }

    public int getClubId() { return clubId; }
    public int getLeagueId() { return leagueId; }
    public DraftMode getMode() { return mode; }
    public boolean isEditMode() { return editMode; }
    public int benchCount() {
        List<RealPlayer> b = bench.getValue();
        return b != null ? b.size() : 0;
    }

    /** Returns the FormationSlot matching the currently-selected slot key, or null. */
    @Nullable
    public FormationSlot getSelectedSlot() {
        String key = selectedSlotKey.getValue();
        String f = formation.getValue();
        if (key == null || BENCH_KEY.equals(key) || f == null) return null;
        for (FormationSlot slot : FormationCatalog.get(f)) {
            if (slot.key.equals(key)) return slot;
        }
        return null;
    }

    private Map<String, RealPlayer> currentAssignments() {
        Map<String, RealPlayer> a = assignments.getValue();
        return a != null ? new LinkedHashMap<>(a) : new LinkedHashMap<>();
    }

    private List<RealPlayer> currentBench() {
        List<RealPlayer> b = bench.getValue();
        return b != null ? new ArrayList<>(b) : new ArrayList<>();
    }

    private static void removeById(List<RealPlayer> list, int id) {
        for (Iterator<RealPlayer> it = list.iterator(); it.hasNext(); ) {
            if (it.next().getId() == id) { it.remove(); return; }
        }
    }

    private void recomputeMarket() {
        String key = selectedSlotKey.getValue();
        if (key == null) {
            marketCandidates.setValue(new ArrayList<>());
            return;
        }
        boolean benchMode = BENCH_KEY.equals(key);
        FormationSlot slot = benchMode ? null : getSelectedSlot();
        if (!benchMode && slot == null) {
            marketCandidates.setValue(new ArrayList<>());
            return;
        }
        List<RealPlayer> all = leaguePlayers != null ? leaguePlayers.getValue() : null;
        if (all == null) {
            marketCandidates.setValue(new ArrayList<>());
            return;
        }
        Long budget = remainingBudget.getValue();
        long b = budget != null ? budget : 0L;

        // Exclude every player already owned (XI + bench); a slot's own current
        // occupant is kept buyable so it can be replaced.
        Set<Integer> owned = new HashSet<>();
        Map<String, RealPlayer> a = assignments.getValue();
        if (a != null) {
            for (Map.Entry<String, RealPlayer> e : a.entrySet()) {
                if (benchMode || !e.getKey().equals(key)) owned.add(e.getValue().getId());
            }
        }
        List<RealPlayer> bn = bench.getValue();
        if (bn != null) for (RealPlayer p : bn) owned.add(p.getId());

        List<RealPlayer> result = new ArrayList<>();
        for (RealPlayer p : all) {
            if (!benchMode && !FormationCatalog.matchesPosGroup(p.getPosition(), slot.posGroup)) {
                continue;
            }
            if (owned.contains(p.getId())) continue;
            if (p.getMarketValue() > b) continue;
            result.add(p);
        }
        marketCandidates.setValue(result);
    }
}
