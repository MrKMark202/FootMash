package hr.fipu.footmash.ui.ailab.career;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import java.util.List;
import java.util.Locale;
import java.util.Random;

import hr.fipu.footmash.FootMashApp;
import hr.fipu.footmash.career.PlayerCareerEngine;
import hr.fipu.footmash.databinding.FragmentSimulateSeasonBinding;
import hr.fipu.footmash.databinding.ItemStatReadoutBinding;
import hr.fipu.footmash.db.AppDatabase;
import hr.fipu.footmash.model.CustomPlayer;
import hr.fipu.footmash.model.PlayerCareerSeason;
import hr.fipu.footmash.model.RealPlayer;
import hr.fipu.footmash.season.TraitEngine;

/**
 * Runs the {@link PlayerCareerEngine} for one half-season (autumn or spring)
 * on a background thread.
 *
 * <ul>
 *     <li><b>AUTUMN</b>: simulates the first half, banks the stats into the
 *         player's {@code halfSeason*} scratch columns, flips
 *         {@code seasonHalfState} to 1 (winter window armed).</li>
 *     <li><b>SPRING</b>: simulates the second half, combines with the banked
 *         autumn tally, inserts the consolidated {@link PlayerCareerSeason},
 *         awards the reward, resets {@code seasonHalfState} to 0, and
 *         increments {@code currentSeasonYear}.</li>
 * </ul>
 */
public class SimulateSeasonFragment extends Fragment {

    public static final String ARG_PLAYER_ID = "playerId";
    public static final String ARG_HALF      = "half";

    private FragmentSimulateSeasonBinding binding;
    private int playerId;
    private PlayerCareerEngine.Half half = PlayerCareerEngine.Half.AUTUMN;
    /** Once-only guard so a rotation doesn't double-run the simulation. */
    private volatile boolean simulationFired = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentSimulateSeasonBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        playerId = getArguments() != null ? getArguments().getInt(ARG_PLAYER_ID, 0) : 0;
        String halfArg = getArguments() != null ? getArguments().getString(ARG_HALF) : null;
        half = "SPRING".equals(halfArg)
            ? PlayerCareerEngine.Half.SPRING
            : PlayerCareerEngine.Half.AUTUMN;

        if (playerId <= 0) {
            Toast.makeText(requireContext(), "Nedostaje ID igrača", Toast.LENGTH_SHORT).show();
            Navigation.findNavController(view).popBackStack();
            return;
        }

        binding.btnContinue.setOnClickListener(v ->
            Navigation.findNavController(v).popBackStack());

        if (savedInstanceState == null && !simulationFired) {
            simulationFired = true;
            runSimulation();
        }
    }

    private void runSimulation() {
        AppDatabase db = FootMashApp.container(requireContext()).database();

        new Thread(() -> {
            CustomPlayer player = db.customPlayerDao().getPlayerByIdSync(playerId);
            if (player == null) {
                postError("Igrač nije pronađen");
                return;
            }

            List<RealPlayer> roster = db.realPlayerDao()
                .getPlayersByTeamSync(player.getTargetTeamId());
            int teamOvr = (roster == null || roster.isEmpty())
                ? 75
                : TraitEngine.effectiveRating(TraitEngine.bestXi(roster));

            PlayerCareerEngine.PlayerStats stats = new PlayerCareerEngine.PlayerStats(
                player.getOverall(),
                player.getShooting(),
                player.getPassing(),
                player.getDribbling(),
                player.getPosition());

            PlayerCareerEngine.SeasonOutcome outcome =
                PlayerCareerEngine.simulateHalf(stats, teamOvr, half, new Random());

            if (half == PlayerCareerEngine.Half.AUTUMN) {
                bankAutumn(db, player, outcome);
            } else {
                consolidateSeason(db, player, outcome, teamOvr);
            }

            postResult(player, outcome);
        }).start();
    }

    /** AUTUMN: bank stats on the player row; arm the winter window. */
    private static void bankAutumn(AppDatabase db, CustomPlayer p,
                                   PlayerCareerEngine.SeasonOutcome o) {
        p.setHalfSeasonApps(o.appearances);
        p.setHalfSeasonGoals(o.goals);
        p.setHalfSeasonAssists(o.assists);
        // We average the season at end. Stash rating-weighted-by-apps so
        // the spring combine can compute (sumA * appsA + sumB * appsB) / total.
        p.setHalfSeasonRatingTotal(o.avgRating * o.appearances);
        p.setSeasonHalfState(1);
        db.customPlayerDao().update(p);
    }

    /** SPRING: combine with banked autumn, insert season row, award points. */
    private static void consolidateSeason(AppDatabase db, CustomPlayer p,
                                          PlayerCareerEngine.SeasonOutcome spring,
                                          int teamOvr) {
        int totalApps    = p.getHalfSeasonApps()    + spring.appearances;
        int totalGoals   = p.getHalfSeasonGoals()   + spring.goals;
        int totalAssists = p.getHalfSeasonAssists() + spring.assists;
        float combinedRating = totalApps == 0
            ? spring.avgRating
            : (p.getHalfSeasonRatingTotal() + spring.avgRating * spring.appearances) / totalApps;

        int finishPos    = generateClubPosition(teamOvr);
        int pointsEarned = PlayerCareerEngine.pointsForRating(combinedRating);

        PlayerCareerSeason record = new PlayerCareerSeason();
        record.setPlayerId(p.getId());
        record.setSeasonYear(p.getCurrentSeasonYear());
        record.setClubId(p.getTargetTeamId());
        record.setClubName(p.getTargetTeamName());
        record.setLeagueId(p.getTargetLeagueId());
        record.setAppearances(totalApps);
        record.setGoals(totalGoals);
        record.setAssists(totalAssists);
        record.setAvgRating(combinedRating);
        record.setClubFinalPosition(finishPos);
        record.setPointsEarned(pointsEarned);
        record.setOvrAtSeasonEnd(p.getOverall());
        db.playerCareerSeasonDao().insert(record);

        p.clearHalfSeasonScratch();
        p.setSeasonHalfState(0);
        p.setCurrentSeasonYear(p.getCurrentSeasonYear() + 1);
        p.setPointsToSpend(p.getPointsToSpend() + pointsEarned);
        db.customPlayerDao().update(p);
    }

    /** Mirrors the engine's hidden formula since spring callers re-derive it once. */
    private static int generateClubPosition(int teamOvr) {
        float base = 20f - (teamOvr - 65) / 1.5f;
        int rounded = Math.round(base);
        return Math.max(1, Math.min(20, rounded));
    }

    private void postResult(CustomPlayer player, PlayerCareerEngine.SeasonOutcome o) {
        if (!isAdded()) return;
        requireActivity().runOnUiThread(() -> {
            if (binding == null) return;

            boolean spring = half == PlayerCareerEngine.Half.SPRING;
            int displayYear = spring
                ? player.getCurrentSeasonYear() - 1   // post-consolidation: year already advanced
                : player.getCurrentSeasonYear();
            String halfLabel = spring ? "proljeće" : "jesen";

            binding.textSeasonLabel.setText(displayYear
                + "/" + String.format(Locale.getDefault(),
                                      "%02d", (displayYear + 1) % 100)
                + " • " + halfLabel);
            binding.textClubLine.setText(player.getTargetTeamName());

            int matchesAvailable = PlayerCareerEngine.MATCHES_PER_HALF;
            stat(binding.rowApps,    "Nastupi",
                o.appearances + " / " + matchesAvailable);
            stat(binding.rowGoals,   "Golovi",          String.valueOf(o.goals));
            stat(binding.rowAssists, "Asistencije",     String.valueOf(o.assists));
            stat(binding.rowRating,  "Prosječna ocjena",
                String.format(Locale.getDefault(), "%.1f", o.avgRating));
            stat(binding.rowFinish,
                spring ? "Klub završio" : "Polusezonska forma",
                spring ? String.valueOf(o.clubFinalPosition) + "."
                       : describeForm(o.avgRating));

            int rewardForReadout = spring
                ? player.getPointsToSpend()  // post-consolidation it's the new total
                : 0;
            binding.textReward.setText(spring
                ? "+" + (player.getPointsToSpend() - 0) + " bodova ukupno za sezonu"
                : "Bodovi se dodjeljuju nakon proljeća");

            binding.layoutLoading.setVisibility(View.GONE);
            binding.layoutResult.setVisibility(View.VISIBLE);
            binding.btnContinue.setVisibility(View.VISIBLE);
        });
    }

    private static String describeForm(float rating) {
        if (rating >= 7.5f) return "Izvanredna";
        if (rating >= 7.0f) return "Dobra";
        if (rating >= 6.5f) return "Solidna";
        return "Slaba";
    }

    private void stat(ItemStatReadoutBinding row, String label, String value) {
        row.textStatLabel.setText(label);
        row.textStatValue.setText(value);
    }

    private void postError(String message) {
        if (!isAdded()) return;
        requireActivity().runOnUiThread(() -> {
            Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
            Navigation.findNavController(requireView()).popBackStack();
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
