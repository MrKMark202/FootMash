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
 * Runs the {@link PlayerCareerEngine} for one season on a background thread,
 * persists the resulting {@link PlayerCareerSeason} row, updates the
 * player's pointsToSpend and currentSeasonYear, then surfaces a focused
 * result card with a "Nastavi" button that pops back to the career hub.
 */
public class SimulateSeasonFragment extends Fragment {

    public static final String ARG_PLAYER_ID = "playerId";

    private FragmentSimulateSeasonBinding binding;
    private int playerId;
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

            // Compute host team OVR from real_players roster. If the team
            // has no seeded roster, fall back to 75 (mid-table baseline).
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
                PlayerCareerEngine.simulate(stats, teamOvr, new Random());

            PlayerCareerSeason record = buildRecord(player, outcome);
            db.playerCareerSeasonDao().insert(record);

            player.setCurrentSeasonYear(player.getCurrentSeasonYear() + 1);
            player.setPointsToSpend(player.getPointsToSpend() + outcome.pointsEarned);
            db.customPlayerDao().update(player);

            postResult(player, outcome);
        }).start();
    }

    private static PlayerCareerSeason buildRecord(CustomPlayer p,
                                                  PlayerCareerEngine.SeasonOutcome o) {
        PlayerCareerSeason r = new PlayerCareerSeason();
        r.setPlayerId(p.getId());
        r.setSeasonYear(p.getCurrentSeasonYear());
        r.setClubId(p.getTargetTeamId());
        r.setClubName(p.getTargetTeamName());
        r.setLeagueId(p.getTargetLeagueId());
        r.setAppearances(o.appearances);
        r.setGoals(o.goals);
        r.setAssists(o.assists);
        r.setAvgRating(o.avgRating);
        r.setClubFinalPosition(o.clubFinalPosition);
        r.setPointsEarned(o.pointsEarned);
        r.setOvrAtSeasonEnd(p.getOverall()); // OVR before spending the new points
        return r;
    }

    private void postResult(CustomPlayer player, PlayerCareerEngine.SeasonOutcome o) {
        if (!isAdded()) return;
        requireActivity().runOnUiThread(() -> {
            if (binding == null) return;
            binding.textSeasonLabel.setText(player.getCurrentSeasonYear() - 1
                + "/" + String.format(Locale.getDefault(),
                                      "%02d", player.getCurrentSeasonYear() % 100));
            binding.textClubLine.setText(player.getTargetTeamName());

            stat(binding.rowApps,    "Nastupi",
                o.appearances + " / " + PlayerCareerEngine.MATCHES_PER_SEASON);
            stat(binding.rowGoals,   "Golovi",          String.valueOf(o.goals));
            stat(binding.rowAssists, "Asistencije",     String.valueOf(o.assists));
            stat(binding.rowRating,  "Prosječna ocjena",
                String.format(Locale.getDefault(), "%.1f", o.avgRating));
            stat(binding.rowFinish,  "Klub završio",    o.clubFinalPosition + ".");

            binding.textReward.setText("+" + o.pointsEarned + " bodova");

            binding.layoutLoading.setVisibility(View.GONE);
            binding.layoutResult.setVisibility(View.VISIBLE);
            binding.btnContinue.setVisibility(View.VISIBLE);
        });
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
