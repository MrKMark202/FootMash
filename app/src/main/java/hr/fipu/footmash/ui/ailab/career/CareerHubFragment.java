package hr.fipu.footmash.ui.ailab.career;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import hr.fipu.footmash.R;

import java.util.List;
import java.util.Locale;

import hr.fipu.footmash.databinding.FragmentCareerHubBinding;
import hr.fipu.footmash.databinding.ItemCareerSeasonRowBinding;
import hr.fipu.footmash.model.CustomPlayer;
import hr.fipu.footmash.model.PlayerCareerSeason;

/**
 * Hub for a single custom player's career. Renders:
 *
 * <ul>
 *     <li>Profile card — name, position, nationality, current OVR, club,
 *         current season year, and "X seasons at club"</li>
 *     <li>Past seasons — chronological cards from {@link PlayerCareerSeason}</li>
 *     <li>One state-driven primary CTA — simulate / spend points / transfer.
 *         CTA navigation lands in commits 4, 5, 6.</li>
 * </ul>
 *
 * <p>Takes a {@code playerId} argument from the nav graph.
 */
public class CareerHubFragment extends Fragment {

    public static final String ARG_PLAYER_ID = "playerId";

    private FragmentCareerHubBinding binding;
    private CareerHubViewModel viewModel;
    private int playerId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentCareerHubBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(CareerHubViewModel.class);

        playerId = getArguments() != null
            ? getArguments().getInt(ARG_PLAYER_ID, 0) : 0;
        if (playerId <= 0) {
            Toast.makeText(requireContext(), "Nedostaje ID igrača", Toast.LENGTH_SHORT).show();
            return;
        }
        viewModel.init(playerId);

        viewModel.getPlayer().observe(getViewLifecycleOwner(), this::renderProfile);
        viewModel.getSeasons().observe(getViewLifecycleOwner(), this::renderSeasons);
        viewModel.getCta().observe(getViewLifecycleOwner(), this::renderCta);

        binding.btnPrimaryCta.setOnClickListener(v -> {
            CareerHubViewModel.Cta cta = viewModel.getCta().getValue();
            if (cta == null) return;
            Bundle args = new Bundle();
            args.putInt(SimulateSeasonFragment.ARG_PLAYER_ID, playerId);
            switch (cta) {
                case SIMULATE_SEASON:
                    Navigation.findNavController(v)
                        .navigate(R.id.action_careerHub_to_simulateSeason, args);
                    break;
                case SPEND_POINTS:
                    Navigation.findNavController(v)
                        .navigate(R.id.action_careerHub_to_statGrowth, args);
                    break;
                case TRANSFER_WINDOW:
                    // Transfer-offers navigation ships in commit 6.
                    break;
            }
        });
    }

    private void renderProfile(@Nullable CustomPlayer p) {
        if (p == null) return;
        binding.textPlayerHeader.setText(p.getFullName());
        binding.textPlayerName.setText(p.getFullName());
        binding.textPlayerMeta.setText(p.getPosition() + " • " + p.getNationality());
        binding.textPlayerOverall.setText(String.valueOf(p.getOverall()));
        binding.textPlayerClub.setText(p.getTargetTeamName() != null
            ? p.getTargetTeamName() : "Slobodan agent");

        int seasonsHere = viewModel.seasonsAtCurrentClub();
        String seasonLine = String.format(Locale.getDefault(),
            "Sezona %d/%02d • %d sezona u klubu",
            p.getCurrentSeasonYear(),
            (p.getCurrentSeasonYear() + 1) % 100,
            seasonsHere);
        binding.textPlayerSeasonInfo.setText(seasonLine);
    }

    private void renderSeasons(@Nullable List<PlayerCareerSeason> seasons) {
        binding.containerSeasons.removeAllViews();
        if (seasons == null || seasons.isEmpty()) {
            binding.textNoSeasonsHint.setVisibility(View.VISIBLE);
            return;
        }
        binding.textNoSeasonsHint.setVisibility(View.GONE);

        LayoutInflater inflater = LayoutInflater.from(requireContext());
        // Most recent at the top.
        for (int i = seasons.size() - 1; i >= 0; i--) {
            PlayerCareerSeason s = seasons.get(i);
            View row = inflater.inflate(
                hr.fipu.footmash.R.layout.item_career_season_row,
                binding.containerSeasons, false);
            bindSeasonRow(row, s);
            binding.containerSeasons.addView(row);
        }
    }

    private void bindSeasonRow(View row, PlayerCareerSeason s) {
        ItemCareerSeasonRowBinding b = ItemCareerSeasonRowBinding.bind(row);
        b.textSeasonLabel.setText(s.getSeasonLabel());
        b.textSeasonClub.setText(s.getClubName());
        b.textSeasonFinish.setText(ordinalCroatian(s.getClubFinalPosition()) + " mjesto");
        b.textSeasonStats.setText(String.format(Locale.getDefault(),
            "%d nastupa • %d G • %d A • ocjena %.1f",
            s.getAppearances(), s.getGoals(), s.getAssists(), s.getAvgRating()));
        b.textSeasonReward.setText("+" + s.getPointsEarned()
            + " bodova • OVR " + s.getOvrAtSeasonEnd());
    }

    private void renderCta(@Nullable CareerHubViewModel.Cta cta) {
        if (cta == null) return;
        switch (cta) {
            case SPEND_POINTS:
                binding.btnPrimaryCta.setText("Potroši bodove");
                binding.btnPrimaryCta.setBackgroundTintList(
                    getResources().getColorStateList(hr.fipu.footmash.R.color.warning, null));
                break;
            case TRANSFER_WINDOW:
                binding.btnPrimaryCta.setText("Prelazni rok");
                binding.btnPrimaryCta.setBackgroundTintList(
                    getResources().getColorStateList(hr.fipu.footmash.R.color.accent_red, null));
                break;
            case SIMULATE_SEASON:
            default:
                binding.btnPrimaryCta.setText("Simuliraj sezonu");
                binding.btnPrimaryCta.setBackgroundTintList(
                    getResources().getColorStateList(hr.fipu.footmash.R.color.accent_blue, null));
                break;
        }
    }

    /** Lightweight ordinal helper for "1.", "2.", ... in Croatian text. */
    private static String ordinalCroatian(int n) { return n + "."; }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
