package hr.fipu.footmash.ui.players;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;

import java.util.ArrayList;
import java.util.List;

import hr.fipu.footmash.db.AppDatabase;
import hr.fipu.footmash.model.PlayerResponse;
import hr.fipu.footmash.model.RealPlayer;

public class PlayersViewModel extends AndroidViewModel {

    private final AppDatabase db;

    public PlayersViewModel(@NonNull Application application) {
        super(application);
        db = AppDatabase.getInstance(application);
    }

    public LiveData<List<PlayerResponse>> getPlayersByTeam(int teamId, int season) {
        return Transformations.map(db.realPlayerDao().getPlayersByTeam(teamId), players -> {
            List<PlayerResponse> mapped = new ArrayList<>();
            if (players == null) return mapped;
            for (RealPlayer p : players) {
                PlayerResponse r = new PlayerResponse();
                r.setPlayerKey(p.getId());
                r.setPlayerName(p.getName());
                r.setPlayerType(p.getPosition());
                r.setPlayerCountry(p.getNationality());
                r.setPlayerAge(String.valueOf(p.getAge()));
                r.setPlayerGoals(String.valueOf(p.getOverall()));
                r.setPlayerMatchPlayed("-");
                r.setPlayerYellowCards("-");
                r.setPlayerRedCards("-");
                r.setPlayerNumber(null);
                r.setPlayerImage(null);
                mapped.add(r);
            }
            return mapped;
        });
    }
}
