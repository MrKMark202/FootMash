package hr.fipu.footmash.ui.leagues;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import hr.fipu.footmash.db.AppDatabase;
import hr.fipu.footmash.db.LogoAssets;
import hr.fipu.footmash.model.LeagueInfo;
import hr.fipu.footmash.model.LeagueLore;
import hr.fipu.footmash.model.LeagueResponse;
import hr.fipu.footmash.model.RealTeam;

public class LeaguesViewModel extends AndroidViewModel {

    private final AppDatabase db;
    private final MediatorLiveData<List<LeagueResponse>> leagues = new MediatorLiveData<>();
    private final LiveData<List<LeagueInfo>> leaguesSrc;
    private final LiveData<List<RealTeam>> teamsSrc;

    public LeaguesViewModel(@NonNull Application application) {
        super(application);
        db = AppDatabase.getInstance(application);

        leaguesSrc = db.realTeamDao().getDistinctLeagues();
        teamsSrc = db.realTeamDao().getAllTeamsLive();

        leagues.addSource(leaguesSrc, l -> rebuild());
        leagues.addSource(teamsSrc, t -> rebuild());
    }

    public LiveData<List<LeagueResponse>> getLeagues() {
        return leagues;
    }

    private void rebuild() {
        List<LeagueInfo> infos = leaguesSrc.getValue();
        if (infos == null) return;

        // Group every seeded club under its league id.
        Map<Integer, List<RealTeam>> clubsByLeague = new LinkedHashMap<>();
        List<RealTeam> teams = teamsSrc.getValue();
        if (teams != null) {
            for (RealTeam t : teams) {
                clubsByLeague.computeIfAbsent(t.getLeagueId(), k -> new ArrayList<>()).add(t);
            }
        }

        List<LeagueResponse> mapped = new ArrayList<>();
        for (LeagueInfo info : infos) {
            LeagueResponse r = new LeagueResponse();
            r.setLeagueKey(info.getId());
            r.setLeagueName(info.getName());
            r.setLeagueLogo(LogoAssets.leagueCrestUri(info.getId()));

            LeagueLore lore = LeagueLore.forLeague(info.getId());
            r.setTagline(lore.tagline);
            r.setSummary(lore.summary);
            r.setLegends(lore.legends);

            List<RealTeam> clubs = clubsByLeague.get(info.getId());
            r.setClubs(clubs != null ? clubs : new ArrayList<>());
            mapped.add(r);
        }
        leagues.setValue(mapped);
    }
}
