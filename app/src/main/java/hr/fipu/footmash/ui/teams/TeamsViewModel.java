package hr.fipu.footmash.ui.teams;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import java.util.List;

import hr.fipu.footmash.model.TeamResponse;
import hr.fipu.footmash.model.TeamStatisticsResponse.TeamStats;
import hr.fipu.footmash.repository.FootballRepository;

public class TeamsViewModel extends ViewModel {

    private final FootballRepository repository;

    public TeamsViewModel() {
        repository = new FootballRepository();
    }

    public LiveData<List<TeamResponse>> getTeamsByLeague(int leagueId, int season) {
        return repository.getTeamsByLeague(leagueId, season);
    }

    public LiveData<List<TeamResponse>> getTeamDetail(int teamId) {
        return repository.getTeamDetail(teamId);
    }
}
