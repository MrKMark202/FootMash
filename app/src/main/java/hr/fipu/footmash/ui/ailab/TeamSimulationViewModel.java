package hr.fipu.footmash.ui.ailab;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import java.util.List;

import hr.fipu.footmash.ai.GeminiRepository;
import hr.fipu.footmash.model.LeagueResponse;
import hr.fipu.footmash.model.StandingResponse;
import hr.fipu.footmash.repository.FootballRepository;

public class TeamSimulationViewModel extends ViewModel {

    private final FootballRepository footballRepository;
    private final GeminiRepository geminiRepository;

    public TeamSimulationViewModel() {
        footballRepository = new FootballRepository();
        geminiRepository = new GeminiRepository();
    }

    public LiveData<List<LeagueResponse>> getLeagues() {
        return footballRepository.getAllLeagues();
    }

    // 1. Dohvati trenutnu tablicu lige
    public LiveData<List<StandingResponse>> getLeagueStandings(int leagueId, int season) {
        return footballRepository.getStandings(leagueId, season);
    }

    // 2. Pošalji prompt Geminiju
    public LiveData<String> runSimulation(String teamName, int teamRating, String standingsJson, String apiKey) {
        String prompt = "Trenutno stanje ciljane lige (imena klubova, odigrane utakmice, bodovi):\n" + standingsJson + "\n\n" +
                "Korisnik je kreirao novi klub pod nazivom: " + teamName + ", s internom ocjenom " + teamRating + "/100.\n" +
                "Pretpostavimo da je ta liga stvarna. Na temelju kvalitete drugih timova u ligi, napravi " +
                "zanimljivu analizu i predvidi s koliko će bodova " + teamName + " završiti sezonu i na kojem mjestu. " +
                "Piši analizu na Hrvatskom jeziku u 2 kratka odlomka.";
                
        return geminiRepository.generateSimulation(prompt, apiKey);
    }
}
