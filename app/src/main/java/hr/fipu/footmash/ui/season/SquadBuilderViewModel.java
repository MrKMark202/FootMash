package hr.fipu.footmash.ui.season;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import hr.fipu.footmash.db.AppDatabase;
import hr.fipu.footmash.model.FormationSlot;
import hr.fipu.footmash.model.RealPlayer;
import hr.fipu.footmash.model.UserClub;
import hr.fipu.footmash.model.UserSquad;

public class SquadBuilderViewModel extends AndroidViewModel {

    public static final Map<String, List<FormationSlot>> FORMATIONS = new LinkedHashMap<>();

    static {
        FORMATIONS.put("4-4-2", Arrays.asList(
            new FormationSlot("GK",   "GK",  "GK",  0.50f, 0.88f),
            new FormationSlot("LB",   "LB",  "DF",  0.10f, 0.72f),
            new FormationSlot("CB1",  "CB",  "DF",  0.33f, 0.72f),
            new FormationSlot("CB2",  "CB",  "DF",  0.67f, 0.72f),
            new FormationSlot("RB",   "RB",  "DF",  0.90f, 0.72f),
            new FormationSlot("LM",   "LM",  "MF",  0.10f, 0.50f),
            new FormationSlot("CM1",  "CM",  "MF",  0.33f, 0.50f),
            new FormationSlot("CM2",  "CM",  "MF",  0.67f, 0.50f),
            new FormationSlot("RM",   "RM",  "MF",  0.90f, 0.50f),
            new FormationSlot("ST1",  "ST",  "FW",  0.33f, 0.20f),
            new FormationSlot("ST2",  "ST",  "FW",  0.67f, 0.20f)
        ));
        FORMATIONS.put("4-3-3", Arrays.asList(
            new FormationSlot("GK",   "GK",  "GK",  0.50f, 0.88f),
            new FormationSlot("LB",   "LB",  "DF",  0.10f, 0.72f),
            new FormationSlot("CB1",  "CB",  "DF",  0.33f, 0.72f),
            new FormationSlot("CB2",  "CB",  "DF",  0.67f, 0.72f),
            new FormationSlot("RB",   "RB",  "DF",  0.90f, 0.72f),
            new FormationSlot("CM1",  "CM",  "MF",  0.20f, 0.50f),
            new FormationSlot("CM2",  "CM",  "MF",  0.50f, 0.50f),
            new FormationSlot("CM3",  "CM",  "MF",  0.80f, 0.50f),
            new FormationSlot("LW",   "LW",  "FW",  0.15f, 0.20f),
            new FormationSlot("ST",   "ST",  "FW",  0.50f, 0.20f),
            new FormationSlot("RW",   "RW",  "FW",  0.85f, 0.20f)
        ));
        FORMATIONS.put("3-5-2", Arrays.asList(
            new FormationSlot("GK",   "GK",  "GK",  0.50f, 0.88f),
            new FormationSlot("CB1",  "CB",  "DF",  0.25f, 0.72f),
            new FormationSlot("CB2",  "CB",  "DF",  0.50f, 0.72f),
            new FormationSlot("CB3",  "CB",  "DF",  0.75f, 0.72f),
            new FormationSlot("LM",   "LM",  "MF",  0.08f, 0.52f),
            new FormationSlot("CM1",  "CM",  "MF",  0.28f, 0.52f),
            new FormationSlot("CM2",  "CM",  "MF",  0.50f, 0.52f),
            new FormationSlot("CM3",  "CM",  "MF",  0.72f, 0.52f),
            new FormationSlot("RM",   "RM",  "MF",  0.92f, 0.52f),
            new FormationSlot("ST1",  "ST",  "FW",  0.33f, 0.20f),
            new FormationSlot("ST2",  "ST",  "FW",  0.67f, 0.20f)
        ));
        FORMATIONS.put("4-2-3-1", Arrays.asList(
            new FormationSlot("GK",   "GK",  "GK",  0.50f, 0.88f),
            new FormationSlot("LB",   "LB",  "DF",  0.10f, 0.74f),
            new FormationSlot("CB1",  "CB",  "DF",  0.33f, 0.74f),
            new FormationSlot("CB2",  "CB",  "DF",  0.67f, 0.74f),
            new FormationSlot("RB",   "RB",  "DF",  0.90f, 0.74f),
            new FormationSlot("CDM1", "CDM", "MF",  0.35f, 0.58f),
            new FormationSlot("CDM2", "CDM", "MF",  0.65f, 0.58f),
            new FormationSlot("LAM",  "LAM", "MF",  0.18f, 0.38f),
            new FormationSlot("CAM",  "CAM", "MF",  0.50f, 0.38f),
            new FormationSlot("RAM",  "RAM", "MF",  0.82f, 0.38f),
            new FormationSlot("ST",   "ST",  "FW",  0.50f, 0.18f)
        ));
    }

    private final AppDatabase db;
    private int clubId;

    private final MutableLiveData<String> formation = new MutableLiveData<>("4-4-2");
    private final MutableLiveData<Map<String, RealPlayer>> assignments =
            new MutableLiveData<>(new LinkedHashMap<>());
    private LiveData<List<RealPlayer>> signedPlayers;

    public SquadBuilderViewModel(@NonNull Application application) {
        super(application);
        db = AppDatabase.getInstance(application);
    }

    public void init(int clubId) {
        this.clubId = clubId;
        signedPlayers = db.userClubDao().getSignedPlayers(clubId);
        new Thread(() -> {
            UserClub club = db.userClubDao().getClubByIdSync(clubId);
            if (club != null && club.getFormation() != null
                    && FORMATIONS.containsKey(club.getFormation())) {
                formation.postValue(club.getFormation());
            }
        }).start();
    }

    public void setFormation(String f) {
        formation.setValue(f);
        assignments.setValue(new LinkedHashMap<>());
    }

    public void assignPlayer(String slotKey, RealPlayer player) {
        Map<String, RealPlayer> current = new LinkedHashMap<>(
                assignments.getValue() != null ? assignments.getValue() : new LinkedHashMap<>());
        Iterator<Map.Entry<String, RealPlayer>> it = current.entrySet().iterator();
        while (it.hasNext()) {
            if (it.next().getValue().getId() == player.getId()) it.remove();
        }
        current.put(slotKey, player);
        assignments.setValue(current);
    }

    public void unassignSlot(String slotKey) {
        Map<String, RealPlayer> current = new LinkedHashMap<>(
                assignments.getValue() != null ? assignments.getValue() : new LinkedHashMap<>());
        current.remove(slotKey);
        assignments.setValue(current);
    }

    public boolean isLineupComplete() {
        Map<String, RealPlayer> assigned = assignments.getValue();
        return assigned != null && assigned.size() == 11;
    }

    public void saveLineup(Runnable onComplete) {
        Map<String, RealPlayer> assigned = new LinkedHashMap<>(
                assignments.getValue() != null ? assignments.getValue() : new LinkedHashMap<>());
        String currentFormation = formation.getValue();
        new Thread(() -> {
            UserClub club = db.userClubDao().getClubByIdSync(clubId);
            if (club != null) {
                club.setFormation(currentFormation);
                db.userClubDao().updateClub(club);
            }
            db.userClubDao().resetStartingXI(clubId);
            for (Map.Entry<String, RealPlayer> entry : assigned.entrySet()) {
                UserSquad sq = db.userClubDao().getSquadEntryByPlayer(
                        entry.getValue().getId(), clubId);
                if (sq != null) {
                    sq.setStartingXI(true);
                    sq.setPitchPosition(entry.getKey());
                    db.userClubDao().updateSquadPlayer(sq);
                }
            }
            onComplete.run();
        }).start();
    }

    public LiveData<List<RealPlayer>> getSignedPlayers() { return signedPlayers; }
    public LiveData<String> getFormation() { return formation; }
    public LiveData<Map<String, RealPlayer>> getAssignments() { return assignments; }
    public int getClubId() { return clubId; }
}
