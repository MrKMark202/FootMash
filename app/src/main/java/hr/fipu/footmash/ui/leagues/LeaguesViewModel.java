package hr.fipu.footmash.ui.leagues;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;

import java.util.ArrayList;
import java.util.List;

import hr.fipu.footmash.db.AppDatabase;
import hr.fipu.footmash.model.LeagueInfo;
import hr.fipu.footmash.model.LeagueResponse;

public class LeaguesViewModel extends AndroidViewModel {

    private final AppDatabase db;

    public LeaguesViewModel(@NonNull Application application) {
        super(application);
        db = AppDatabase.getInstance(application);
    }

    public LiveData<List<LeagueResponse>> getLeagues() {
        return Transformations.map(db.realTeamDao().getDistinctLeagues(), leagues -> {
            List<LeagueResponse> mapped = new ArrayList<>();
            if (leagues == null) return mapped;
            for (LeagueInfo info : leagues) {
                LeagueResponse r = new LeagueResponse();
                r.setLeagueKey(info.getId());
                r.setLeagueName(info.getName());
                if (info.getId() == 177) {
                    r.setLeagueLogo("https://apiv2.allsportsapi.com/logo/logo_leagues/177_premier-league.png");
                } else if (info.getId() == 302) {
                    r.setLeagueLogo("https://apiv2.allsportsapi.com/logo/logo_leagues/302_la-liga.png");
                }
                mapped.add(r);
            }
            return mapped;
        });
    }
}
