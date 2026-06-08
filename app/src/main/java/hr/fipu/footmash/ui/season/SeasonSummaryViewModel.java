package hr.fipu.footmash.ui.season;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.List;

import hr.fipu.footmash.db.AppDatabase;
import hr.fipu.footmash.db.TopScorerRow;
import hr.fipu.footmash.model.SeasonStanding;

public class SeasonSummaryViewModel extends AndroidViewModel {

    public static class SummaryData {
        public final String champion;
        public final int championPoints;
        public final String goldenBootPlayer;
        public final String goldenBootTeam;
        public final int goldenBootGoals;
        public final String userTeamName;
        public final int userPosition;
        public final int userPoints;
        public final int userWon;
        public final int userDrawn;
        public final int userLost;
        public final int userGoalsFor;
        public final int userGoalsAgainst;

        SummaryData(String champion, int championPoints,
                    String goldenBootPlayer, String goldenBootTeam, int goldenBootGoals,
                    String userTeamName, int userPosition,
                    int userPoints, int userWon, int userDrawn, int userLost,
                    int userGoalsFor, int userGoalsAgainst) {
            this.champion = champion;
            this.championPoints = championPoints;
            this.goldenBootPlayer = goldenBootPlayer;
            this.goldenBootTeam = goldenBootTeam;
            this.goldenBootGoals = goldenBootGoals;
            this.userTeamName = userTeamName;
            this.userPosition = userPosition;
            this.userPoints = userPoints;
            this.userWon = userWon;
            this.userDrawn = userDrawn;
            this.userLost = userLost;
            this.userGoalsFor = userGoalsFor;
            this.userGoalsAgainst = userGoalsAgainst;
        }
    }

    private final AppDatabase db;
    private final MutableLiveData<SummaryData> summaryData = new MutableLiveData<>();
    private final MutableLiveData<List<SeasonStanding>> fullTable = new MutableLiveData<>();

    public SeasonSummaryViewModel(@NonNull Application application) {
        super(application);
        db = AppDatabase.getInstance(application);
    }

    public void init(int clubId) {
        new Thread(() -> {
            List<SeasonStanding> standings = db.standingDao().getStandingsSync(clubId);
            List<TopScorerRow> topScorers = db.standingDao().getTopScorers(clubId);
            if (standings.isEmpty()) return;
            fullTable.postValue(standings);

            SeasonStanding champ = standings.get(0);

            TopScorerRow golden = topScorers.isEmpty() ? null : topScorers.get(0);
            String goldenPlayer = golden != null ? golden.playerName : "—";
            String goldenTeam = golden != null ? golden.teamName : "—";
            int goldenGoals = golden != null ? golden.goals : 0;

            SeasonStanding userRow = null;
            int userPos = 1;
            for (int i = 0; i < standings.size(); i++) {
                if (standings.get(i).isUserTeam()) {
                    userPos = i + 1;
                    userRow = standings.get(i);
                    break;
                }
            }
            if (userRow == null) return;

            summaryData.postValue(new SummaryData(
                champ.getTeamName(), champ.getPoints(),
                goldenPlayer, goldenTeam, goldenGoals,
                userRow.getTeamName(), userPos,
                userRow.getPoints(), userRow.getWon(),
                userRow.getDrawn(), userRow.getLost(),
                userRow.getGoalsFor(), userRow.getGoalsAgainst()
            ));
        }).start();
    }

    public LiveData<SummaryData> getSummaryData() { return summaryData; }

    public LiveData<List<SeasonStanding>> getFullTable() { return fullTable; }
}
