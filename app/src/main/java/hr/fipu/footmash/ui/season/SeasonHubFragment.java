package hr.fipu.footmash.ui.season;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.List;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import androidx.palette.graphics.Palette;
import androidx.core.graphics.ColorUtils;

import hr.fipu.footmash.R;
import hr.fipu.footmash.databinding.FragmentSeasonHubBinding;
import hr.fipu.footmash.model.Fixture;
import hr.fipu.footmash.model.SeasonStanding;
import hr.fipu.footmash.model.UserClub;
import hr.fipu.footmash.ui.util.ClubColors;

public class SeasonHubFragment extends Fragment {

    private FragmentSeasonHubBinding binding;
    private SeasonHubViewModel viewModel;

    /** Colour identity of the inherited club; DEFAULT for self-founded clubs. */
    private ClubColors.Theme theme = ClubColors.DEFAULT;
    private List<SeasonStanding> lastStandings;
    private final String GEMINI_API_KEY = hr.fipu.footmash.BuildConfig.GEMINI_API_KEY;
    private boolean seasonSimNavigated = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentSeasonHubBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        int clubId = getArguments() != null ? getArguments().getInt("clubId", -1) : -1;
        if (clubId == -1) return;

        viewModel = new ViewModelProvider(this).get(SeasonHubViewModel.class);
        viewModel.init(clubId);

        observeViewModel();

        binding.btnSimulate.setOnClickListener(v -> {
            Integer matchday = viewModel.getNextMatchday().getValue();
            Bundle args = new Bundle();
            args.putInt("clubId", clubId);
            if (matchday == null || matchday == 0) {
                Navigation.findNavController(v).navigate(R.id.action_seasonHub_to_summary, args);
            } else {
                args.putInt("matchday", matchday);
                Navigation.findNavController(v).navigate(R.id.action_seasonHub_to_matchday, args);
            }
        });

        binding.btnEditSquad.setOnClickListener(v -> {
            Bundle args = new Bundle();
            args.putInt("clubId", clubId);
            args.putBoolean("editMode", true);
            Navigation.findNavController(v).navigate(R.id.action_seasonHub_to_draft, args);
        });

        binding.btnSimulateSeason.setOnClickListener(v -> confirmSimulateSeason());
        viewModel.getSeasonSimState().observe(getViewLifecycleOwner(), this::bindSeasonSimState);
        viewModel.getSeasonSimProgress().observe(getViewLifecycleOwner(), txt -> {
            if (binding != null
                    && viewModel.getSeasonSimState().getValue()
                       == SeasonHubViewModel.SeasonSimState.RUNNING) {
                binding.btnSimulateSeason.setText(txt);
            }
        });
    }

    private void confirmSimulateSeason() {
        new MaterialAlertDialogBuilder(requireContext(), R.style.FootMashDialog)
            .setTitle("Simulirati cijelu sezonu?")
            .setMessage("Sva preostala kola bit će odigrana odjednom. Ovo može potrajati.")
            .setPositiveButton("Simuliraj", (d, w) ->
                viewModel.simulateWholeSeason(GEMINI_API_KEY))
            .setNegativeButton("Odustani", null)
            .show();
    }

    private void bindSeasonSimState(SeasonHubViewModel.SeasonSimState state) {
        if (binding == null || state == null) return;
        boolean running = state == SeasonHubViewModel.SeasonSimState.RUNNING;
        binding.progressSeason.setVisibility(running ? View.VISIBLE : View.GONE);
        binding.btnSimulate.setEnabled(!running);
        binding.btnSimulate.setAlpha(running ? 0.5f : 1f);
        binding.btnSimulateSeason.setEnabled(!running);
        binding.btnSimulateSeason.setAlpha(running ? 0.7f : 1f);
        binding.btnEditSquad.setEnabled(!running);
        if (!running) binding.btnSimulateSeason.setText("Simuliraj sezonu");

        if (state == SeasonHubViewModel.SeasonSimState.DONE && !seasonSimNavigated) {
            seasonSimNavigated = true;
            Bundle args = new Bundle();
            args.putInt("clubId", viewModel.getClubId());
            Navigation.findNavController(requireView())
                .navigate(R.id.action_seasonHub_to_summary, args);
        }
    }

    private void observeViewModel() {
        viewModel.getClub().observe(getViewLifecycleOwner(), this::bindClub);
        viewModel.getNextUserFixture().observe(getViewLifecycleOwner(), this::bindNextMatch);
        viewModel.getNextMatchday().observe(getViewLifecycleOwner(), this::bindMatchday);
        viewModel.getStandings().observe(getViewLifecycleOwner(), this::bindStandings);
    }

    private void bindClub(UserClub club) {
        if (club == null) return;
        binding.textClubName.setText(club.getClubName());
        binding.textLeague.setText(club.getLeagueName() + " · " + club.getSeasonLabel());
        double budgetM = club.getBudget() / 1_000_000.0;
        binding.textBudget.setText(String.format("Preostali budžet: €%.1fM", budgetM));

        // Re-skin the hub with the inherited club's colours.
        theme = ClubColors.of(club.getRealTeamSourceId());
        ClubColors.styleButton(binding.btnSimulate, theme);
        binding.textMatchdayLabel.setTextColor(theme.primary);
        binding.textClubName.setTextColor(theme.primary);
        if (lastStandings != null) bindStandings(lastStandings);
    }

    private void bindNextMatch(Fixture f) {
        if (f == null) {
            binding.cardNextMatch.setVisibility(View.GONE);
            return;
        }
        binding.cardNextMatch.setVisibility(View.VISIBLE);
        binding.textHomeTeam.setText(f.getHomeTeamName());
        binding.textAwayTeam.setText(f.getAwayTeamName());
    }

    private void bindMatchday(Integer matchday) {
        if (matchday == null || matchday == 0) {
            binding.textMatchdayLabel.setText("Sezona završena!");
            binding.btnSimulate.setText("Pogledaj završnicu");
        } else {
            binding.textMatchdayLabel.setText("Kolo " + matchday);
            binding.btnSimulate.setText("Simuliraj kolo " + matchday);
        }
    }

    private void bindStandings(List<SeasonStanding> list) {
        lastStandings = list;
        LinearLayout container = binding.standingsContainer;
        container.removeAllViews();
        if (list == null || list.isEmpty()) return;

        for (int i = 0; i < list.size(); i++) {
            SeasonStanding s = list.get(i);
            View row = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_standing_row, container, false);

            ((TextView) row.findViewById(R.id.text_rank)).setText(String.valueOf(i + 1));
            ((TextView) row.findViewById(R.id.text_team_name)).setText(s.getTeamName());
            ((TextView) row.findViewById(R.id.text_played)).setText(String.valueOf(s.getPlayed()));
            ((TextView) row.findViewById(R.id.text_won)).setText(String.valueOf(s.getWon()));
            ((TextView) row.findViewById(R.id.text_drawn)).setText(String.valueOf(s.getDrawn()));
            ((TextView) row.findViewById(R.id.text_lost)).setText(String.valueOf(s.getLost()));
            ((TextView) row.findViewById(R.id.text_pts)).setText(String.valueOf(s.getPoints()));

            TextView nameView = row.findViewById(R.id.text_team_name);
            if (s.isUserTeam()) {
                // The user's row is highlighted in their club's colours.
                nameView.setTextColor(theme.primary);
                nameView.setTypeface(null, android.graphics.Typeface.BOLD);
                row.setBackgroundColor(theme.rowTint());
            } else {
                loadRowTint(row, s);
            }

            container.addView(row);
        }
    }

    /** Tints a non-user standings row with the dominant colour of its badge. */
    private void loadRowTint(View row, SeasonStanding s) {
        String bUrl = s.getBadgeUrl();
        if (bUrl == null || bUrl.isEmpty()) {
            String kebabName = s.getTeamName().toLowerCase()
                .replace(" & ", "-")
                .replace("&", "")
                .replace(" ", "-")
                .replace(".", "");
            bUrl = "https://apiv2.allsportsapi.com/logo/" + s.getTeamId() + "_" + kebabName + ".jpg";
        }
        Glide.with(this)
            .asBitmap()
            .load(bUrl)
            .into(new CustomTarget<Bitmap>() {
                @Override
                public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                    Palette.from(resource).generate(palette -> {
                        if (palette != null && isAdded()) {
                            int defaultColor = ContextCompat.getColor(requireContext(), R.color.background);
                            int dominantColor = palette.getDominantColor(defaultColor);
                            row.setBackgroundColor(ColorUtils.setAlphaComponent(dominantColor, 40));
                        }
                    });
                }
                @Override
                public void onLoadCleared(@Nullable Drawable placeholder) {}
            });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
