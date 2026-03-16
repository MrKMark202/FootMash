package hr.fipu.footmash.ui.leagues;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import java.util.List;

import hr.fipu.footmash.model.StandingResponse;
import hr.fipu.footmash.repository.FootballRepository;

public class StandingsViewModel extends ViewModel {

    private final FootballRepository repository;

    public StandingsViewModel() {
        repository = new FootballRepository();
    }

    public LiveData<List<StandingResponse>> getStandings(int leagueId, int season) {
        return repository.getStandings(leagueId, season);
    }
}
