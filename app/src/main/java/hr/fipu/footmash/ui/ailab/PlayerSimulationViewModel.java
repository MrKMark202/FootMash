package hr.fipu.footmash.ui.ailab;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.List;

import hr.fipu.footmash.ai.GeminiRepository;
import hr.fipu.footmash.model.LeagueResponse;
import hr.fipu.footmash.model.PlayerResponse;
import hr.fipu.footmash.model.TeamResponse;
import hr.fipu.footmash.repository.FootballRepository;

public class PlayerSimulationViewModel extends ViewModel {

    private final FootballRepository footballRepository;
    private final GeminiRepository geminiRepository;

    public PlayerSimulationViewModel() {
        footballRepository = new FootballRepository();
        geminiRepository = new GeminiRepository();
    }

    // 1. Dohvati igrače kluba (kako bi znali tko je u ekipi)
    public LiveData<List<PlayerResponse>> getTeamRoster(int teamId, int season) {
        return footballRepository.getPlayersByTeam(teamId, season);
    }

    public LiveData<List<LeagueResponse>> getLeagues() {
        return footballRepository.getAllLeagues();
    }

    public LiveData<List<TeamResponse>> getTeamsByLeague(int leagueId, int season) {
        return footballRepository.getTeamsByLeague(leagueId, season);
    }

    // 2. Pošalji prompt Geminiju
    public LiveData<String> runSimulation(String playerName, String position, String teamJson, String apiKey) {
        String prompt = "Ovo su trenutni igrači kluba:\n" + teamJson + "\n\n" +
                "Korisnik je kreirao novog igrača: Ime: " + playerName + ", Pozicija: " + position + ".\n" +
                "Analiziraj trenutni roster ovog kluba na toj poziciji i predvidi kako će se " + playerName + " snaći. " +
                "Napiši kratku analizu, predviđen broj nastupa, golova (ako je napadač) te zaključak. Piši na Hrvatskom jeziku.";
                
        return geminiRepository.generateSimulation(prompt, apiKey);
    }
}
