package hr.fipu.footmash.ui.home;

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

public class HomeViewModel extends AndroidViewModel {

    private final AppDatabase db;
    private LiveData<List<LeagueResponse>> featuredLeagues;

    public HomeViewModel(@NonNull Application application) {
        super(application);
        db = AppDatabase.getInstance(application);
    }

    public LiveData<List<LeagueResponse>> getFeaturedLeagues() {
        if (featuredLeagues == null) {
            featuredLeagues = Transformations.map(db.realTeamDao().getDistinctLeagues(), leagues -> {
                List<LeagueResponse> mapped = new ArrayList<>();
                if (leagues == null) return mapped;
                for (LeagueInfo info : leagues) {
                    LeagueResponse r = new LeagueResponse();
                    r.setLeagueKey(info.getId());
                    r.setLeagueName(info.getName());
                    mapped.add(r);
                }
                return mapped;
            });
        }
        return featuredLeagues;
    }
}
