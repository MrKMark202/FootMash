package hr.fipu.footmash.ui.home;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.List;

import hr.fipu.footmash.api.ApiClient;
import hr.fipu.footmash.model.LeagueResponse;
import hr.fipu.footmash.repository.FootballRepository;

public class HomeViewModel extends ViewModel {

    private final FootballRepository repository;
    private LiveData<List<LeagueResponse>> featuredLeagues;

    public HomeViewModel() {
        repository = new FootballRepository();
    }

    public LiveData<List<LeagueResponse>> getFeaturedLeagues() {
        if (featuredLeagues == null) {
            featuredLeagues = repository.getAllLeagues(); 
        }
        return featuredLeagues;
    }

    public void refreshFeaturedLeagues() {
        featuredLeagues = repository.getAllLeagues();
    }
}
