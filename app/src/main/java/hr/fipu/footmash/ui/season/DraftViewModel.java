package hr.fipu.footmash.ui.season;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import hr.fipu.footmash.db.AppDatabase;
import hr.fipu.footmash.model.FormationSlot;
import hr.fipu.footmash.model.RealPlayer;
import hr.fipu.footmash.model.UserClub;
import hr.fipu.footmash.model.UserSquad;
import hr.fipu.footmash.season.FormationCatalog;

/**
 * Unified state for the draft canvas — pitch slots, assignments, budget, and the
 * slot-targeted market modal. Lives at activity scope; {@link #init} fully resets state.
 */
public class DraftViewModel extends AndroidViewModel {

    public enum DraftMode { NEW_CLUB, EXISTING_CLUB }

    private final AppDatabase db;

    private int clubId = -1;
    private int leagueId = -1;
    private DraftMode mode = DraftMode.NEW_CLUB;

    private final MutableLiveData<String> formation = new MutableLiveData<>(FormationCatalog.DEFAULT);
    private final MutableLiveData<Map<String, RealPlayer>> assignments =
            new MutableLiveData<>(new LinkedHashMap<>());
    private final MutableLiveData<Long> remainingBudget = new MutableLiveData<>(0L);
    private final MutableLiveData<String> selectedSlotKey = new MutableLiveData<>(null);

    private final MediatorLiveData<List<RealPlayer>> marketCandidates = new MediatorLiveData<>();
    private LiveData<List<RealPlayer>> leaguePlayers;

    public DraftViewModel(@NonNull Application application) {
        super(application);
        db = AppDatabase.getInstance(application);
    }

    /** Reset and load draft state for the given club. Safe to call repeatedly. */
    public void init(int clubId, int leagueId, DraftMode mode) {
        this.clubId = clubId;
        this.leagueId = leagueId;
        this.mode = mode;

        // remove any previous market source
        if (leaguePlayers != null) marketCandidates.removeSource(leaguePlayers);
        marketCandidates.removeSource(assignments);
        marketCandidates.removeSource(selectedSlotKey);
        marketCandidates.removeSource(remainingBudget);

        leaguePlayers = db.realPlayerDao().getPlayersByLeague(leagueId);

        marketCandidates.addSource(leaguePlayers, p -> recomputeMarket());
        marketCandidates.addSource(assignments, p -> recomputeMarket());
        marketCandidates.addSource(selectedSlotKey, p -> recomputeMarket());
        marketCandidates.addSource(remainingBudget, p -> recomputeMarket());

        assignments.setValue(new LinkedHashMap<>());

        new Thread(() -> {
            UserClub club = db.userClubDao().getClubByIdSync(clubId);
            if (club == null) return;

            String f = (club.getFormation() != null && FormationCatalog.FORMATIONS.containsKey(club.getFormation()))
                    ? club.getFormation()
                    : FormationCatalog.DEFAULT;
            formation.postValue(f);
            remainingBudget.postValue(club.getBudget());

            // Pre-populate assignments from any existing starting-XI rows
            List<UserSquad> existing = db.userClubDao().getSquadByClubSync(clubId);
            if (existing != null && !existing.isEmpty()) {
                Map<String, RealPlayer> seeded = new LinkedHashMap<>();
                for (UserSquad sq : existing) {
                    if (!sq.isStartingXI() || sq.getPitchPosition() == null) continue;
                    RealPlayer p = db.realPlayerDao().getPlayerById(sq.getPlayerId());
                    if (p != null) seeded.put(sq.getPitchPosition(), p);
                }
                assignments.postValue(seeded);
            }
        }).start();
    }

    public void selectSlot(@Nullable String slotKey) {
        selectedSlotKey.setValue(slotKey);
    }

    /** Buys the player into the currently-selected slot, deducting their market value. */
    public void buyForSlot(String slotKey, RealPlayer player) {
        if (slotKey == null || player == null) return;
        new Thread(() -> {
            UserClub club = db.userClubDao().getClubByIdSync(clubId);
            if (club == null) return;
            long cost = player.getMarketValue();
            if (club.getBudget() < cost) return;

            // Remove any previous occupant of this slot from user_squad
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
    public LiveData<Long> getRemainingBudget() { return remainingBudget; }
    public LiveData<String> getSelectedSlotKey() { return selectedSlotKey; }
    public LiveData<List<RealPlayer>> getMarketCandidates() { return marketCandidates; }

    public int getClubId() { return clubId; }
    public int getLeagueId() { return leagueId; }
    public DraftMode getMode() { return mode; }

    /** Returns the FormationSlot matching the currently-selected slot key, or null. */
    @Nullable
    public FormationSlot getSelectedSlot() {
        String key = selectedSlotKey.getValue();
        String f = formation.getValue();
        if (key == null || f == null) return null;
        for (FormationSlot slot : FormationCatalog.get(f)) {
            if (slot.key.equals(key)) return slot;
        }
        return null;
    }

    private Map<String, RealPlayer> currentAssignments() {
        Map<String, RealPlayer> a = assignments.getValue();
        return a != null ? new LinkedHashMap<>(a) : new LinkedHashMap<>();
    }

    private void recomputeMarket() {
        String key = selectedSlotKey.getValue();
        if (key == null) {
            marketCandidates.setValue(new ArrayList<>());
            return;
        }
        FormationSlot slot = getSelectedSlot();
        if (slot == null) {
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

        Set<Integer> occupied = new HashSet<>();
        Map<String, RealPlayer> a = assignments.getValue();
        if (a != null) {
            for (Map.Entry<String, RealPlayer> e : a.entrySet()) {
                if (!e.getKey().equals(key)) occupied.add(e.getValue().getId());
            }
        }

        List<RealPlayer> result = new ArrayList<>();
        for (RealPlayer p : all) {
            if (!FormationCatalog.matchesPosGroup(p.getPosition(), slot.posGroup)) continue;
            if (occupied.contains(p.getId())) continue;
            if (p.getMarketValue() > b) continue;
            result.add(p);
        }
        marketCandidates.setValue(result);
    }
}
