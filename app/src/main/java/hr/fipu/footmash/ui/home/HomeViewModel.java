package hr.fipu.footmash.ui.home;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import java.util.ArrayList;
import java.util.List;

import hr.fipu.footmash.db.AppDatabase;
import hr.fipu.footmash.model.LeagueInfo;
import hr.fipu.footmash.model.LeagueResponse;
import hr.fipu.footmash.model.UserClub;
import hr.fipu.footmash.model.Fixture;
import hr.fipu.footmash.model.MatchResult;

public class HomeViewModel extends AndroidViewModel {

    private final AppDatabase db;
    private LiveData<List<LeagueResponse>> featuredLeagues;

    private final LiveData<UserClub> activeClub;
    /** Last played fixture for the active club; swaps source when the club changes. */
    private final LiveData<Fixture> lastFixture;
    private final LiveData<MatchResult> lastResult;
    private final LiveData<List<hr.fipu.footmash.model.GoalScorer>> lastScorers;
    private final LiveData<String> homeBadgeUrl;
    private final LiveData<String> awayBadgeUrl;

    public HomeViewModel(@NonNull Application application) {
        super(application);
        db = AppDatabase.getInstance(application);
        activeClub = db.userClubDao().getActiveClub();

        // Chain the dependent queries with switchMap so each downstream source is
        // swapped (not re-subscribed) when its key changes — observing these once
        // in the fragment avoids stacking observers on every emission.
        lastFixture = Transformations.switchMap(activeClub, club ->
            club == null ? noData()
                         : db.fixtureDao().getLastPlayedUserFixture(club.getId()));
        lastResult = Transformations.switchMap(lastFixture, fx ->
            fx == null ? noData() : db.fixtureDao().getMatchResult(fx.getId()));
        lastScorers = Transformations.switchMap(lastFixture, fx ->
            fx == null ? noData() : db.fixtureDao().getGoalScorersByFixtureLive(fx.getId()));
        homeBadgeUrl = Transformations.switchMap(lastFixture, fx ->
            fx == null ? noData() : db.realTeamDao().getBadgeUrlLive(fx.getHomeTeamId()));
        awayBadgeUrl = Transformations.switchMap(lastFixture, fx ->
            fx == null ? noData() : db.realTeamDao().getBadgeUrlLive(fx.getAwayTeamId()));
    }

    private static <T> LiveData<T> noData() {
        return new MutableLiveData<>(null);
    }

    public LiveData<UserClub> getActiveClub() { return activeClub; }
    public LiveData<Fixture> getLastFixture() { return lastFixture; }
    public LiveData<MatchResult> getLastResult() { return lastResult; }
    public LiveData<List<hr.fipu.footmash.model.GoalScorer>> getLastScorers() { return lastScorers; }
    public LiveData<String> getHomeBadgeUrl() { return homeBadgeUrl; }
    public LiveData<String> getAwayBadgeUrl() { return awayBadgeUrl; }

    public LiveData<List<LeagueResponse>> getFeaturedLeagues() {
        if (featuredLeagues == null) {
            featuredLeagues = Transformations.map(db.realTeamDao().getDistinctLeagues(), leagues -> {
                List<LeagueResponse> mapped = new ArrayList<>();
                if (leagues == null) return mapped;
                for (LeagueInfo info : leagues) {
                    LeagueResponse r = new LeagueResponse();
                    r.setLeagueKey(info.getId());
                    r.setLeagueName(info.getName());
                    r.setLeagueLogo(hr.fipu.footmash.db.LogoAssets.leagueCrestUri(info.getId()));
                    mapped.add(r);
                }
                return mapped;
            });
        }
        return featuredLeagues;
    }
}
