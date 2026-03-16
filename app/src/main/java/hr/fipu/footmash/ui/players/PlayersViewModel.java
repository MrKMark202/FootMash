package hr.fipu.footmash.ui.players;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import java.util.List;

import hr.fipu.footmash.model.PlayerResponse;
import hr.fipu.footmash.repository.FootballRepository;

public class PlayersViewModel extends ViewModel {

    private final FootballRepository repository;

    public PlayersViewModel() {
        repository = new FootballRepository();
    }

    public LiveData<List<PlayerResponse>> getPlayersByTeam(int teamId, int season) {
        return repository.getPlayersByTeam(teamId, season);
    }
}
