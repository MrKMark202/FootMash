package hr.fipu.footmash.ui.season;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.List;

import hr.fipu.footmash.db.AppDatabase;
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
    private LiveData<Fixture> nextUserFixture;
    private LiveData<Integer> nextMatchday;

    public SeasonHubViewModel(@NonNull Application application) {
        super(application);
        db = AppDatabase.getInstance(application);
        seasonRepo = new SeasonRepository(application);
    }

    public void init(int clubId) {
        if (this.clubId == clubId && standings != null) return; // already initialized
        this.clubId = clubId;

        standings       = db.standingDao().getStandings(clubId);
        nextUserFixture = db.fixtureDao().getNextUserFixture(clubId);
        nextMatchday    = db.fixtureDao().getNextMatchdayLive(clubId);

        new Thread(() -> {
            UserClub c = db.userClubDao().getClubByIdSync(clubId);
            if (c != null) club.postValue(c);
            seasonRepo.startSeasonIfNeeded(clubId);
        }).start();
    }

    public LiveData<UserClub> getClub() { return club; }
    public LiveData<List<SeasonStanding>> getStandings() { return standings; }
    public LiveData<Fixture> getNextUserFixture() { return nextUserFixture; }
    public LiveData<Integer> getNextMatchday() { return nextMatchday; }
    public int getClubId() { return clubId; }

    // --- Whole-season simulation ---

    public enum SeasonSimState { IDLE, RUNNING, DONE }

    private final MutableLiveData<SeasonSimState> seasonSimState =
        new MutableLiveData<>(SeasonSimState.IDLE);
    private final MutableLiveData<String> seasonSimProgress = new MutableLiveData<>("");

    public LiveData<SeasonSimState> getSeasonSimState() { return seasonSimState; }
    public LiveData<String> getSeasonSimProgress() { return seasonSimProgress; }

    /** Simulates every remaining matchday back-to-back on a background thread. */
    public void simulateWholeSeason(String apiKey) {
        if (seasonSimState.getValue() == SeasonSimState.RUNNING) return;
        seasonSimState.setValue(SeasonSimState.RUNNING);
        new Thread(() -> {
            int guard = 0;
            int matchday;
            while ((matchday = db.fixtureDao().getNextMatchdaySync(clubId)) > 0 && guard < 60) {
                guard++;
                seasonSimProgress.postValue("Simuliram kolo " + matchday + " / 38");
                if (!seasonRepo.simulateMatchday(clubId, matchday, apiKey)) break;
            }
            seasonSimProgress.postValue("");
            seasonSimState.postValue(SeasonSimState.DONE);
        }).start();
    }
}
