package hr.fipu.footmash.ui.season;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.ArrayList;
import java.util.List;

import hr.fipu.footmash.db.AppDatabase;
import hr.fipu.footmash.model.RealPlayer;
import hr.fipu.footmash.model.UserClub;
import hr.fipu.footmash.model.UserSquad;

public class TransferMarketViewModel extends AndroidViewModel {

    private final AppDatabase db;

    private int clubId;
    private int leagueId;

    private final MutableLiveData<String> positionFilter = new MutableLiveData<>("ALL");
    private final MutableLiveData<Long> remainingBudget = new MutableLiveData<>(UserClub.STARTING_BUDGET);
    private final MutableLiveData<Integer> squadCount = new MutableLiveData<>(0);

    private LiveData<List<RealPlayer>> filteredPlayers;
    private LiveData<List<Integer>> signedPlayerIds;

    public TransferMarketViewModel(@NonNull Application application) {
        super(application);
        db = AppDatabase.getInstance(application);
    }

    public void init(int clubId, int leagueId) {
        this.clubId = clubId;
        this.leagueId = leagueId;

        signedPlayerIds = db.userClubDao().getSignedPlayerIds(clubId);

        LiveData<List<RealPlayer>> allPlayers = db.realPlayerDao().getPlayersByLeague(leagueId);

        MediatorLiveData<List<RealPlayer>> combined = new MediatorLiveData<>();
        combined.addSource(allPlayers, players -> {
            String filter = positionFilter.getValue();
            combined.setValue(applyFilter(players, filter));
        });
        combined.addSource(positionFilter, filter -> {
            List<RealPlayer> players = allPlayers.getValue();
            if (players != null) combined.setValue(applyFilter(players, filter));
        });
        filteredPlayers = combined;

        new Thread(() -> {
            UserClub club = db.userClubDao().getClubByIdSync(clubId);
            if (club != null) remainingBudget.postValue(club.getBudget());
            squadCount.postValue(db.userClubDao().getSquadCount(clubId));
        }).start();
    }

    private List<RealPlayer> applyFilter(List<RealPlayer> all, String filter) {
        if (all == null) return new ArrayList<>();
        if ("ALL".equals(filter) || filter == null) return all;
        List<RealPlayer> result = new ArrayList<>();
        for (RealPlayer p : all) {
            if (matchesFilter(p.getPosition(), filter)) result.add(p);
        }
        return result;
    }

    private boolean matchesFilter(String pos, String filter) {
        if (pos == null) return false;
        switch (filter) {
            case "GK": return pos.equals("GK");
            case "DF": return pos.equals("CB") || pos.equals("RB") || pos.equals("LB");
            case "MF": return pos.equals("CM") || pos.equals("CDM") || pos.equals("CAM")
                           || pos.equals("LM") || pos.equals("RM");
            case "FW": return pos.equals("ST") || pos.equals("FW") || pos.equals("LW") || pos.equals("RW");
            default:   return true;
        }
    }

    public void setPositionFilter(String filter) {
        positionFilter.setValue(filter);
    }

    public void signPlayer(RealPlayer player) {
        new Thread(() -> {
            long budget = db.userClubDao().getClubByIdSync(clubId).getBudget();
            long cost = player.getMarketValue();
            if (budget < cost) return;

            UserSquad entry = new UserSquad();
            entry.setClubId(clubId);
            entry.setPlayerId(player.getId());
            entry.setStartingXI(false);
            db.userClubDao().insertSquadPlayer(entry);

            long newBudget = budget - cost;
            UserClub club = db.userClubDao().getClubByIdSync(clubId);
            club.setBudget(newBudget);
            db.userClubDao().updateClub(club);

            remainingBudget.postValue(newBudget);
            int count = db.userClubDao().getSquadCount(clubId);
            squadCount.postValue(count);
        }).start();
    }

    public void unsignPlayer(int playerId) {
        new Thread(() -> {
            RealPlayer player = db.realPlayerDao().getPlayerById(playerId);
            if (player == null) return;

            db.userClubDao().removePlayerFromSquad(playerId, clubId);

            UserClub club = db.userClubDao().getClubByIdSync(clubId);
            long newBudget = club.getBudget() + player.getMarketValue();
            club.setBudget(newBudget);
            db.userClubDao().updateClub(club);

            remainingBudget.postValue(newBudget);
            int count = db.userClubDao().getSquadCount(clubId);
            squadCount.postValue(count);
        }).start();
    }

    public LiveData<List<RealPlayer>> getFilteredPlayers() { return filteredPlayers; }
    public LiveData<List<Integer>> getSignedPlayerIds() { return signedPlayerIds; }
    public LiveData<Long> getRemainingBudget() { return remainingBudget; }
    public LiveData<Integer> getSquadCount() { return squadCount; }
    public int getClubId() { return clubId; }
}
