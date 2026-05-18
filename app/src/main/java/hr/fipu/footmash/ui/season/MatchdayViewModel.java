package hr.fipu.footmash.ui.season;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.ArrayList;
import java.util.List;

import hr.fipu.footmash.db.AppDatabase;
import hr.fipu.footmash.model.Fixture;
import hr.fipu.footmash.model.GoalScorer;
import hr.fipu.footmash.model.MatchResult;
import hr.fipu.footmash.repository.SeasonRepository;

public class MatchdayViewModel extends AndroidViewModel {

    public enum SimState { IDLE, SIMULATING, DONE, ERROR }

    public static class FixtureDisplay {
        public final Fixture fixture;
        public final MatchResult result;
        public final String scorersText;

        FixtureDisplay(Fixture fixture, MatchResult result, String scorersText) {
            this.fixture = fixture;
            this.result = result;
            this.scorersText = scorersText;
        }
    }

    private final AppDatabase db;
    private final SeasonRepository seasonRepo;

    private int clubId;
    private int matchday;

    private final MutableLiveData<List<FixtureDisplay>> fixtures = new MutableLiveData<>();
    private final MutableLiveData<SimState> simState = new MutableLiveData<>(SimState.IDLE);

    public MatchdayViewModel(@NonNull Application application) {
        super(application);
        db = AppDatabase.getInstance(application);
        seasonRepo = new SeasonRepository(application);
    }

    public void init(int clubId, int matchday) {
        this.clubId = clubId;
        this.matchday = matchday;
        new Thread(() -> fixtures.postValue(buildDisplays())).start();
    }

    public void simulate(String apiKey) {
        if (SimState.SIMULATING.equals(simState.getValue())) return;
        simState.setValue(SimState.SIMULATING);
        new Thread(() -> {
            boolean ok = seasonRepo.simulateMatchday(clubId, matchday, apiKey);
            if (ok) {
                fixtures.postValue(buildDisplays());
                simState.postValue(SimState.DONE);
            } else {
                simState.postValue(SimState.ERROR);
            }
        }).start();
    }

    private List<FixtureDisplay> buildDisplays() {
        List<Fixture> raw = db.fixtureDao().getFixturesByMatchdaySync(clubId, matchday);
        List<FixtureDisplay> list = new ArrayList<>();
        for (Fixture f : raw) {
            MatchResult mr = db.fixtureDao().getMatchResultSync(f.getId());
            String scorersText = "";
            if (mr != null) {
                List<GoalScorer> scorers = db.fixtureDao().getGoalScorersByFixture(f.getId());
                scorersText = buildScorersText(scorers, f.getHomeTeamName(), f.getAwayTeamName());
            }
            list.add(new FixtureDisplay(f, mr, scorersText));
        }
        return list;
    }

    private static String buildScorersText(List<GoalScorer> scorers, String home, String away) {
        StringBuilder homeSb = new StringBuilder();
        StringBuilder awaySb = new StringBuilder();
        for (GoalScorer g : scorers) {
            if (g.getTeamName().equals(home)) {
                if (homeSb.length() > 0) homeSb.append(", ");
                homeSb.append(g.getPlayerName()).append(" ").append(g.getMinute()).append("'");
            } else {
                if (awaySb.length() > 0) awaySb.append(", ");
                awaySb.append(g.getPlayerName()).append(" ").append(g.getMinute()).append("'");
            }
        }
        return homeSb.toString() + "|" + awaySb.toString();
    }

    public LiveData<List<FixtureDisplay>> getFixtures() { return fixtures; }
    public LiveData<SimState> getSimState() { return simState; }
    public int getMatchday() { return matchday; }
}
