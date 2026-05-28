package hr.fipu.footmash.ui.ailab.career;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.List;

import hr.fipu.footmash.FootMashApp;
import hr.fipu.footmash.career.TransferEligibility;
import hr.fipu.footmash.db.AppDatabase;
import hr.fipu.footmash.db.CustomPlayerDao;
import hr.fipu.footmash.db.PlayerCareerSeasonDao;
import hr.fipu.footmash.model.CustomPlayer;
import hr.fipu.footmash.model.PlayerCareerSeason;

/**
 * Loads the current state for the career hub: the player row, the player's
 * past seasons (chronological), and a derived {@link Cta} telling the UI
 * which single primary action to surface right now.
 *
 * <p>State precedence:
 * <ol>
 *     <li>{@code pointsToSpend > 0} → SPEND_POINTS (must spend before sim)</li>
 *     <li>2+ seasons at current club → TRANSFER_WINDOW (wired in commit 6)</li>
 *     <li>otherwise → SIMULATE_SEASON</li>
 * </ol>
 */
public class CareerHubViewModel extends AndroidViewModel {

    public enum Cta {
        SIMULATE_AUTUMN,
        WINTER_TRANSFER,
        SIMULATE_SPRING,
        SPEND_POINTS,
        TRANSFER_WINDOW   // end-of-season transfer (existing summer behaviour)
    }

    private final CustomPlayerDao playerDao;
    private final PlayerCareerSeasonDao seasonDao;

    private final MutableLiveData<Integer> playerIdLive = new MutableLiveData<>();
    private final LiveData<CustomPlayer> player;
    private final LiveData<List<PlayerCareerSeason>> seasons;
    private final MediatorLiveData<Cta> cta = new MediatorLiveData<>();

    public CareerHubViewModel(@NonNull Application application) {
        super(application);
        AppDatabase db = FootMashApp.container(application).database();
        playerDao = db.customPlayerDao();
        seasonDao = db.playerCareerSeasonDao();

        // switchMap-style: player + seasons re-bind whenever playerId changes.
        player = androidx.lifecycle.Transformations.switchMap(playerIdLive,
            id -> id == null ? new MutableLiveData<>(null) : playerDao.getPlayerById(id));
        seasons = androidx.lifecycle.Transformations.switchMap(playerIdLive,
            id -> id == null ? new MutableLiveData<>(java.util.Collections.emptyList())
                              : seasonDao.getByPlayer(id));

        cta.addSource(player,  p -> recomputeCta());
        cta.addSource(seasons, s -> recomputeCta());
    }

    public void init(int playerId) {
        if (playerIdLive.getValue() != null && playerIdLive.getValue() == playerId) return;
        playerIdLive.setValue(playerId);
    }

    public LiveData<CustomPlayer> getPlayer() { return player; }
    public LiveData<List<PlayerCareerSeason>> getSeasons() { return seasons; }
    public LiveData<Cta> getCta() { return cta; }

    /** Seasons played at the player's current club (excluding any prior clubs). */
    public int seasonsAtCurrentClub() {
        CustomPlayer p = player.getValue();
        List<PlayerCareerSeason> hist = seasons.getValue();
        if (p == null || hist == null) return 0;
        int n = 0;
        for (PlayerCareerSeason s : hist) {
            if (s.getClubId() == p.getTargetTeamId()) n++;
        }
        return n;
    }

    /**
     * True when the player has spent ≥2 seasons at the current club AND we
     * haven't already shown offers at the current count. Decision logic
     * lives in {@link TransferEligibility} so it can be unit-tested without
     * Android dependencies.
     */
    public boolean isTransferEligible() {
        CustomPlayer p = player.getValue();
        if (p == null) return false;
        return TransferEligibility.isEligible(
            seasonsAtCurrentClub(), p.getTransferDismissedAt());
    }

    private void recomputeCta() {
        CustomPlayer p = player.getValue();
        if (p == null) { cta.setValue(Cta.SIMULATE_AUTUMN); return; }
        // Mid-season states take precedence over end-of-season decisions.
        switch (p.getSeasonHalfState()) {
            case 1: cta.setValue(Cta.WINTER_TRANSFER); return;
            case 2: cta.setValue(Cta.SIMULATE_SPRING); return;
            default: /* state 0 -- fall through to end-of-season logic */
        }
        if (p.getPointsToSpend() > 0) { cta.setValue(Cta.SPEND_POINTS); return; }
        if (isTransferEligible())     { cta.setValue(Cta.TRANSFER_WINDOW); return; }
        cta.setValue(Cta.SIMULATE_AUTUMN);
    }
}
