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
}
