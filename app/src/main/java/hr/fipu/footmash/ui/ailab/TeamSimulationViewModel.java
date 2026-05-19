package hr.fipu.footmash.ui.ailab;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.List;
import java.util.concurrent.Executors;

import hr.fipu.footmash.ai.GeminiRepository;
import hr.fipu.footmash.db.AppDatabase;
import hr.fipu.footmash.model.LeagueInfo;
import hr.fipu.footmash.model.RealTeam;

public class TeamSimulationViewModel extends AndroidViewModel {

    private final AppDatabase db;
    private final GeminiRepository geminiRepository;

    public TeamSimulationViewModel(@NonNull Application application) {
        super(application);
        db = AppDatabase.getInstance(application);
        geminiRepository = new GeminiRepository();
    }

    public LiveData<List<LeagueInfo>> getLeagues() {
        return db.realTeamDao().getDistinctLeagues();
    }

    public LiveData<List<RealTeam>> getTeamsInLeague(int leagueId) {
        return db.realTeamDao().getTeamsByLeague(leagueId);
    }

    public LiveData<String> runSimulation(String teamName, int teamRating,
                                          String leagueName, String standingsInfo,
                                          String apiKey) {
        String prompt = "Liga: " + leagueName + "\n" +
                "Klubovi koji već igraju u toj ligi:\n" + standingsInfo + "\n\n" +
                "Korisnik je kreirao novi klub pod nazivom: " + teamName +
                ", s internom ocjenom " + teamRating + "/100.\n" +
                "Pretpostavimo da ovaj klub uđe u ligu. Na temelju kvalitete drugih klubova, " +
                "napravi zanimljivu analizu i predvidi s koliko će bodova " + teamName +
                " završiti sezonu i na kojem mjestu. " +
                "Piši analizu na hrvatskom jeziku u 2 kratka odlomka.";

        MutableLiveData<String> result = new MutableLiveData<>();
        Executors.newSingleThreadExecutor().execute(() -> {
            String response = geminiRepository.callSync(prompt, apiKey);
            result.postValue(response != null ? response :
                    "Greška pri pozivu Gemini API-ja. Provjerite ključ i mrežu.");
        });
        return result;
    }
}
