package hr.fipu.footmash.ui.teams;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;

import hr.fipu.footmash.db.AppDatabase;
import hr.fipu.footmash.model.RealTeam;
import hr.fipu.footmash.model.TeamResponse;

public class TeamsViewModel extends AndroidViewModel {

    private final AppDatabase db;

    public TeamsViewModel(@NonNull Application application) {
        super(application);
        db = AppDatabase.getInstance(application);
    }

    public LiveData<List<TeamResponse>> getTeamsByLeague(int leagueId, int season) {
        return Transformations.map(db.realTeamDao().getTeamsByLeague(leagueId), teams -> {
            List<TeamResponse> mapped = new ArrayList<>();
            if (teams == null) return mapped;
            for (RealTeam t : teams) {
                mapped.add(toResponse(t));
            }
            return mapped;
        });
    }

    public LiveData<List<TeamResponse>> getTeamDetail(int teamId) {
        MutableLiveData<List<TeamResponse>> result = new MutableLiveData<>();
        Executors.newSingleThreadExecutor().execute(() -> {
            RealTeam team = db.realTeamDao().getTeamById(teamId);
            if (team == null) {
                result.postValue(Collections.emptyList());
            } else {
                result.postValue(Collections.singletonList(toResponse(team)));
            }
        });
        return result;
    }

    private TeamResponse toResponse(RealTeam t) {
        TeamResponse r = new TeamResponse();
        r.setTeamKey(t.getId());
        r.setTeamName(t.getName());
        r.setTeamBadge(t.getBadgeUrl());
        return r;
    }
}
