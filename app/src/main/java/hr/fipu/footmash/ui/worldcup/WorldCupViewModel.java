package hr.fipu.footmash.ui.worldcup;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import hr.fipu.footmash.FootMashApp;
import hr.fipu.footmash.repository.WorldCupRepository;
import hr.fipu.footmash.worldcup.WcPlayer;
import hr.fipu.footmash.worldcup.WcTournament;

/**
 * Backs every World Cup screen. State lives in the {@code world_cup} table; this
 * just exposes it as LiveData and runs the repository off the main thread.
 */
public class WorldCupViewModel extends AndroidViewModel {

    private final WorldCupRepository repo;

    private final MutableLiveData<WcTournament> state = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loaded = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> busy = new MutableLiveData<>(false);
    private final MutableLiveData<List<WcPlayer>> pool = new MutableLiveData<>();
    private final MutableLiveData<Map<String, Integer>> counts = new MutableLiveData<>();
    /** One-shot list of the matches simulated by the last {@link #simulate} call. */
    private final MutableLiveData<List<WcTournament.WcMatch>> lastResults = new MutableLiveData<>();

    public WorldCupViewModel(@NonNull Application application) {
        super(application);
        repo = FootMashApp.container(application).worldCupRepository();
    }

    public LiveData<WcTournament> getState() { return state; }
    public LiveData<Boolean> getLoaded() { return loaded; }
    public LiveData<Boolean> getBusy() { return busy; }
    public LiveData<List<WcPlayer>> getPool() { return pool; }
    public LiveData<List<WcTournament.WcMatch>> getLastResults() { return lastResults; }

    public void refresh() {
        new Thread(() -> {
            state.postValue(repo.load());
            loaded.postValue(true);
        }).start();
    }

    public void loadPool(String nationKey) {
        new Thread(() -> pool.postValue(repo.poolFor(nationKey))).start();
    }

    public LiveData<Map<String, Integer>> getCounts() { return counts; }

    public void loadCounts() {
        new Thread(() -> counts.postValue(repo.realPlayerCounts())).start();
    }

    public void start(String nationKey, String formation,
                      List<WcPlayer> squad, List<Integer> xiIds) {
        if (Boolean.TRUE.equals(busy.getValue())) return;
        busy.setValue(true);
        new Thread(() -> {
            WcTournament t = repo.startTournament(nationKey, formation, squad, xiIds);
            state.postValue(t);
            busy.postValue(false);
        }).start();
    }

    public void simulate(String apiKey) {
        if (Boolean.TRUE.equals(busy.getValue())) return;
        WcTournament prev = state.getValue();
        if (prev == null || WcTournament.STAGE_DONE.equals(prev.stage)) return;
        final String prevStage = prev.stage;
        final int prevMd = prev.groupMatchday;
        busy.setValue(true);
        new Thread(() -> {
            WcTournament t = repo.simulateNextStage(apiKey);
            lastResults.postValue(extractResults(t, prevStage, prevMd));
            state.postValue(t);
            busy.postValue(false);
        }).start();
    }

    /**
     * Clears the one-shot results so the dialog isn't re-shown when the hub view is
     * recreated (e.g. after returning from the bracket/groups screen).
     */
    public void consumeResults() {
        lastResults.setValue(null);
    }

    /** Saves a new formation and/or manually chosen starting XI (slot order). */
    public void saveTeam(String formation, List<Integer> xiIds) {
        if (Boolean.TRUE.equals(busy.getValue())) return;
        busy.setValue(true);
        new Thread(() -> {
            WcTournament t = repo.saveTeam(formation, xiIds);
            state.postValue(t);
            busy.postValue(false);
        }).start();
    }

    private List<WcTournament.WcMatch> extractResults(WcTournament t, String prevStage, int prevMd) {
        List<WcTournament.WcMatch> out = new ArrayList<>();
        if (t == null) return out;
        if (WcTournament.STAGE_GROUP.equals(prevStage)) {
            for (WcTournament.WcMatch m : t.groupFixtures) {
                if (m.matchday == prevMd) out.add(m);
            }
        } else {
            for (WcTournament.WcRound r : t.knockout) {
                if (prevStage.equals(r.stage)) { out.addAll(r.ties); break; }
            }
        }
        return out;
    }

    public void reset() {
        new Thread(() -> {
            repo.clear();
            state.postValue(null);
        }).start();
    }
}
