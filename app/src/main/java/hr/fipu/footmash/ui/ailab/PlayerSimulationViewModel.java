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
import hr.fipu.footmash.model.RealPlayer;
import hr.fipu.footmash.model.RealTeam;

public class PlayerSimulationViewModel extends AndroidViewModel {

    private final AppDatabase db;
    private final GeminiRepository geminiRepository;

    public PlayerSimulationViewModel(@NonNull Application application) {
        super(application);
        db = AppDatabase.getInstance(application);
        geminiRepository = new GeminiRepository();
    }

    public LiveData<List<LeagueInfo>> getLeagues() {
        return db.realTeamDao().getDistinctLeagues();
    }

    public LiveData<List<RealTeam>> getTeamsByLeague(int leagueId) {
        return db.realTeamDao().getTeamsByLeague(leagueId);
    }

    public LiveData<List<RealPlayer>> getTeamRoster(int teamId) {
        return db.realPlayerDao().getPlayersByTeam(teamId);
    }

    public LiveData<String> runSimulation(String playerName, String position,
                                          String teamName, String rosterInfo,
                                          String apiKey) {
        String prompt = "Klub: " + teamName + "\n" +
                "Trenutni igrači tog kluba:\n" + rosterInfo + "\n\n" +
                "Korisnik je kreirao novog igrača — Ime: " + playerName +
                ", Pozicija: " + position + ".\n" +
                "Analiziraj trenutni roster ovog kluba na toj poziciji i predvidi kako će se " +
                playerName + " snaći. Napiši kratku analizu, predviđen broj nastupa, " +
                "golova (ako je napadač) te zaključak. Piši na hrvatskom jeziku.";

        MutableLiveData<String> result = new MutableLiveData<>();
        Executors.newSingleThreadExecutor().execute(() -> {
            String response = geminiRepository.callSync(prompt, apiKey);
            result.postValue(response != null ? response :
                    "Greška pri pozivu Gemini API-ja. Provjerite ključ i mrežu.");
        });
        return result;
    }
}
