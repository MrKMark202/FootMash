package hr.fipu.footmash.ui.season;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.List;

import hr.fipu.footmash.FootMashApp;
import hr.fipu.footmash.db.AppDatabase;
import hr.fipu.footmash.db.TopScorerRow;
import hr.fipu.footmash.di.AppContainer;
import hr.fipu.footmash.model.Fixture;
import hr.fipu.footmash.model.SeasonStanding;
import hr.fipu.footmash.model.UserClub;
import hr.fipu.footmash.repository.SeasonRepository;

public class SeasonHubViewModel extends AndroidViewModel {

    private final AppDatabase db;
    private final SeasonRepository seasonRepo;
    private int clubId;

    private final MutableLiveData<UserClub> club = new MutableLiveData<>();
    private LiveData<List<SeasonStanding>> standings;
    private LiveData<List<SeasonStanding>> uclStandings;
    private LiveData<Fixture> nextUserFixture;
    private LiveData<Integer> nextMatchday;

    private final MutableLiveData<Leaders> leaders = new MutableLiveData<>();

    public SeasonHubViewModel(@NonNull Application application) {
        super(application);
        AppContainer container = FootMashApp.container(application);
        db = container.database();
        seasonRepo = container.seasonRepository();
    }

    public void init(int clubId) {
        if (this.clubId == clubId && standings != null) return; // already initialized
        this.clubId = clubId;

        standings       = db.standingDao().getStandings(clubId);
        uclStandings    = db.standingDao().getStandings(SeasonRepository.uclSeasonId(clubId));
        nextUserFixture = db.fixtureDao().getNextUserFixture(clubId);
        nextMatchday    = db.fixtureDao().getNextMatchdayLive(clubId);

        new Thread(() -> {
            UserClub c = db.userClubDao().getClubByIdSync(clubId);
            if (c != null) club.postValue(c);
            seasonRepo.startSeasonIfNeeded(clubId);
            seasonRepo.setupUclIfNeeded(clubId);
        }).start();
    }

    public LiveData<List<SeasonStanding>> getUclStandings() { return uclStandings; }
    public int getUclId() { return SeasonRepository.uclSeasonId(clubId); }

    /**
     * Matchday that starts the second half of a 20-team season (38 games → the
     * winter break sits after matchday 19). The winter window opens when this is
     * the next unplayed matchday and it hasn't been passed yet.
     */
    public static final int WINTER_BREAK_NEXT_MATCHDAY = 20;

    public LiveData<UserClub> getClub() { return club; }
    public LiveData<List<SeasonStanding>> getStandings() { return standings; }
    public LiveData<Fixture> getNextUserFixture() { return nextUserFixture; }
    public LiveData<Integer> getNextMatchday() { return nextMatchday; }
    public int getClubId() { return clubId; }

    // --- Season leaderboards (top scorers / assisters / clean sheets) ---

    /** Snapshot of the three player/team leaderboards for the current season. */
    public static class Leaders {
        public final List<TopScorerRow> scorers;
        public final List<TopScorerRow> assisters;
        public final List<TopScorerRow> cleanSheets;

        Leaders(List<TopScorerRow> scorers, List<TopScorerRow> assisters,
                List<TopScorerRow> cleanSheets) {
            this.scorers = scorers;
            this.assisters = assisters;
            this.cleanSheets = cleanSheets;
        }

        public boolean isEmpty() {
            return scorers.isEmpty() && assisters.isEmpty() && cleanSheets.isEmpty();
        }
    }

    public LiveData<Leaders> getLeaders() { return leaders; }

    /** Recomputes the league leaderboards off the UI thread. */
    public void refreshLeaders() {
        refreshLeaders(clubId);
    }

    /** Recomputes leaderboards for an arbitrary competition season id (league or UCL). */
    public void refreshLeaders(int seasonId) {
        new Thread(() -> leaders.postValue(new Leaders(
            db.standingDao().getTopScorers(seasonId, 5),
            db.standingDao().getTopAssisters(seasonId, 5),
            db.standingDao().getTopCleanSheets(seasonId, 5)))).start();
    }

    // --- Whole-season simulation ---

    public enum SeasonSimState { IDLE, RUNNING, DONE }

    private final MutableLiveData<SeasonSimState> seasonSimState =
        new MutableLiveData<>(SeasonSimState.IDLE);
    private final MutableLiveData<String> seasonSimProgress = new MutableLiveData<>("");
    /** True when the last run drained all 38 matchdays; false if it paused at the break. */
    private final MutableLiveData<Boolean> seasonFinished = new MutableLiveData<>(false);
    /** Flipped by the UI to halt an in-progress whole-season run after the current matchday. */
    private volatile boolean stopRequested = false;

    public LiveData<SeasonSimState> getSeasonSimState() { return seasonSimState; }
    public LiveData<String> getSeasonSimProgress() { return seasonSimProgress; }
    public LiveData<Boolean> getSeasonFinished() { return seasonFinished; }

    /**
     * Simulates remaining matchdays back-to-back on a background thread, pausing
     * at the winter break (after matchday 19) until the user has passed through
     * the winter transfer window.
     */
    public void simulateWholeSeason(String apiKey) {
        if (seasonSimState.getValue() == SeasonSimState.RUNNING) return;
        stopRequested = false;
        seasonSimState.setValue(SeasonSimState.RUNNING);
        new Thread(() -> {
            UserClub current = db.userClubDao().getClubByIdSync(clubId);
            boolean winterDone = current != null && current.isWinterWindowDone();
            int guard = 0;
            int matchday;
            while ((matchday = db.fixtureDao().getNextMatchdaySync(clubId)) > 0 && guard < 60) {
                // Honour a user stop request before starting the next matchday.
                if (stopRequested) break;
                // Stop at the winter break so the user can run the window first.
                if (!winterDone && matchday >= WINTER_BREAK_NEXT_MATCHDAY) break;
                guard++;
                seasonSimProgress.postValue("Simuliram kolo " + matchday + " / 38");
                if (!seasonRepo.simulateMatchday(clubId, matchday, apiKey)) break;
                // Advance the Champions League alongside the league (it finishes
                // earlier, then this is a cheap no-op).
                seasonRepo.simulateUclMatchday(clubId);
            }
            seasonSimProgress.postValue("");
            seasonFinished.postValue(db.fixtureDao().getNextMatchdaySync(clubId) == 0);
            seasonSimState.postValue(SeasonSimState.DONE);
        }).start();
    }

    /**
     * Requests the running whole-season simulation to halt. It stops after the
     * current matchday finishes (a matchday is one atomic Gemini call), leaving
     * the standings and player stats up to date on the hub.
     */
    public void stopSeasonSimulation() {
        stopRequested = true;
        seasonSimProgress.postValue("Zaustavljam...");
    }

    /** Re-reads the club row so budget / winter-window state refresh on return. */
    public void refreshClub() {
        new Thread(() -> {
            UserClub c = db.userClubDao().getClubByIdSync(clubId);
            if (c != null) club.postValue(c);
        }).start();
    }

    /** Marks the winter window passed, unlocking the second half of the season. */
    public void markWinterWindowDone() {
        new Thread(() -> {
            UserClub c = db.userClubDao().getClubByIdSync(clubId);
            if (c != null) {
                c.setWinterWindowDone(true);
                db.userClubDao().updateClub(c);
                club.postValue(c);
            }
        }).start();
    }
}
