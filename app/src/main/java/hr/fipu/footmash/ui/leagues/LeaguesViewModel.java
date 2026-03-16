package hr.fipu.footmash.ui.leagues;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import java.util.List;

import hr.fipu.footmash.model.CountryResponse;
import hr.fipu.footmash.model.LeagueResponse;
import hr.fipu.footmash.repository.FootballRepository;

public class LeaguesViewModel extends ViewModel {

    private final FootballRepository repository;

    public LeaguesViewModel() {
        repository = new FootballRepository();
    }

    public LiveData<List<CountryResponse>> getCountries() {
        return repository.getCountries();
    }

    public LiveData<List<LeagueResponse>> getLeaguesByCountry(String countryName) {
        return repository.getLeaguesByCountry(countryName);
    }
}
