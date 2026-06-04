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
import hr.fipu.footmash.db.LogoAssets;
import hr.fipu.footmash.db.TopScorerRow;
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
    private List<SeasonStanding> lastUclStandings;
    private boolean showingUcl = false;
    private String userClubName;
    private UserClub lastClub;
    private Integer lastNextMatchday;
    private int clubId = -1;
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

        clubId = getArguments() != null ? getArguments().getInt("clubId", -1) : -1;
        if (clubId == -1) return;

        viewModel = new ViewModelProvider(this).get(SeasonHubViewModel.class);
        viewModel.init(clubId);

        observeViewModel();

        binding.btnOpenTransfers.setOnClickListener(v -> openTransferWindow());
        binding.btnCloseWindow.setOnClickListener(v -> viewModel.markWinterWindowDone());

        // Competition toggle: league table ↔ Champions League table.
        Glide.with(this).load(LogoAssets.assetUri(LogoAssets.UCL_CREST)).into(binding.btnCompUcl);
        binding.btnCompLeague.setOnClickListener(v -> showCompetition(false));
        binding.btnCompUcl.setOnClickListener(v -> showCompetition(true));
        applyCompetitionTheme(false);

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

        binding.btnSimulateSeason.setOnClickListener(v -> {
            if (viewModel.getSeasonSimState().getValue()
                    == SeasonHubViewModel.SeasonSimState.RUNNING) {
                viewModel.stopSeasonSimulation();   // tap again = stop
            } else {
                confirmSimulateSeason();
            }
        });
        viewModel.getSeasonSimState().observe(getViewLifecycleOwner(), this::bindSeasonSimState);
        viewModel.getSeasonSimProgress().observe(getViewLifecycleOwner(), txt -> {
            // Progress goes on the matchday label so the season button can stay a
            // tappable "stop" control while the simulation runs.
            if (binding != null && txt != null && !txt.isEmpty()
                    && viewModel.getSeasonSimState().getValue()
                       == SeasonHubViewModel.SeasonSimState.RUNNING) {
                binding.textMatchdayLabel.setText(txt);
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        // Budget / winter-window state may have changed in the transfer editor.
        if (viewModel != null && clubId != -1) viewModel.refreshClub();
    }

    private void openTransferWindow() {
        Bundle args = new Bundle();
        args.putInt("clubId", clubId);
        args.putBoolean("editMode", true);
        args.putBoolean("transferMode", true);
        Navigation.findNavController(requireView())
            .navigate(R.id.action_seasonHub_to_draft, args);
    }

    /**
     * Shows the summer/winter transfer-window card when the corresponding window
     * is open, and locks the simulate buttons during the winter break until the
     * window is closed.
     */
    private void updateTransferWindow() {
        if (binding == null) return;
        Integer next = lastNextMatchday;
        boolean summerOpen = next != null && next == 1;
        boolean winterOpen = next != null
            && next == SeasonHubViewModel.WINTER_BREAK_NEXT_MATCHDAY
            && lastClub != null && !lastClub.isWinterWindowDone();

        if (!summerOpen && !winterOpen) {
            binding.cardTransferWindow.setVisibility(View.GONE);
            setSimulateLocked(false);
            return;
        }

        binding.cardTransferWindow.setVisibility(View.VISIBLE);
        double budgetM = lastClub != null ? lastClub.getBudget() / 1_000_000.0 : 0;
        binding.textTransferWindowSubtitle.setText(
            String.format("Budžet za transfere: €%.1fM", budgetM));

        if (winterOpen) {
            binding.textTransferWindowTitle.setText("❄️ Zimski prelazni rok");
            binding.btnCloseWindow.setVisibility(View.VISIBLE);
            // Lock simulation until the user passes through the winter window.
            setSimulateLocked(true);
        } else {
            binding.textTransferWindowTitle.setText("☀️ Ljetni prelazni rok");
            binding.btnCloseWindow.setVisibility(View.GONE);
            setSimulateLocked(false);
        }
    }

    private void setSimulateLocked(boolean locked) {
        if (binding == null) return;
        // Don't fight the running-season state, which manages its own enabled-ness.
        if (viewModel != null && viewModel.getSeasonSimState().getValue()
                == SeasonHubViewModel.SeasonSimState.RUNNING) return;
        binding.btnSimulate.setEnabled(!locked);
        binding.btnSimulate.setAlpha(locked ? 0.4f : 1f);
        binding.btnSimulateSeason.setEnabled(!locked);
        binding.btnSimulateSeason.setAlpha(locked ? 0.4f : 1f);
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
        binding.btnEditSquad.setEnabled(!running);

        // While running, the season button stays enabled and becomes a red STOP
        // control so the user can halt the simulation whenever they want.
        binding.btnSimulateSeason.setEnabled(true);
        binding.btnSimulateSeason.setAlpha(1f);
        if (running) {
            binding.btnSimulateSeason.setText("⏹ Zaustavi simulaciju");
            binding.btnSimulateSeason.setBackgroundTintList(android.content.res.ColorStateList
                .valueOf(ContextCompat.getColor(requireContext(), R.color.error)));
            binding.btnSimulateSeason.setTextColor(
                ContextCompat.getColor(requireContext(), R.color.text_on_primary));
        } else {
            binding.btnSimulateSeason.setText("Simuliraj sezonu");
            binding.btnSimulateSeason.setBackgroundTintList(android.content.res.ColorStateList
                .valueOf(ContextCompat.getColor(requireContext(), R.color.divider)));
            binding.btnSimulateSeason.setTextColor(
                ContextCompat.getColor(requireContext(), R.color.text_primary));
        }

        if (state == SeasonHubViewModel.SeasonSimState.DONE && !seasonSimNavigated) {
            Boolean finished = viewModel.getSeasonFinished().getValue();
            if (finished != null && finished) {
                seasonSimNavigated = true;
                Bundle args = new Bundle();
                args.putInt("clubId", viewModel.getClubId());
                Navigation.findNavController(requireView())
                    .navigate(R.id.action_seasonHub_to_summary, args);
            } else {
                // Paused at the winter break — surface the window, stay on the hub.
                viewModel.refreshClub();
                updateTransferWindow();
            }
        }
    }

    private void observeViewModel() {
        viewModel.getClub().observe(getViewLifecycleOwner(), this::bindClub);
        viewModel.getNextUserFixture().observe(getViewLifecycleOwner(), this::bindNextMatch);
        viewModel.getNextMatchday().observe(getViewLifecycleOwner(), this::bindMatchday);
        viewModel.getStandings().observe(getViewLifecycleOwner(), this::onLeagueStandings);
        viewModel.getUclStandings().observe(getViewLifecycleOwner(), this::onUclStandings);
        viewModel.getLeaders().observe(getViewLifecycleOwner(), this::bindLeaders);
    }

    private void bindClub(UserClub club) {
        if (club == null) return;
        lastClub = club;
        userClubName = club.getClubName();
        updateTransferWindow();
        binding.textClubName.setText(club.getClubName());
        binding.textLeague.setText(club.getLeagueName() + " · " + club.getSeasonLabel());
        double budgetM = club.getBudget() / 1_000_000.0;
        binding.textBudget.setText(String.format("Preostali budžet: €%.1fM", budgetM));

        // Re-skin the hub with the inherited club's colours.
        theme = ClubColors.of(club.getRealTeamSourceId());
        ClubColors.styleButton(binding.btnSimulate, theme);
        binding.textMatchdayLabel.setTextColor(theme.primary);
        binding.textClubName.setTextColor(theme.primary);

        // The league crest drives the left toggle button.
        String crest = LogoAssets.leagueCrestUri(club.getLeagueId());
        if (crest != null) Glide.with(this).load(crest).into(binding.btnCompLeague);

        renderActive();
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
        lastNextMatchday = matchday;
        if (matchday == null || matchday == 0) {
            binding.textMatchdayLabel.setText("Sezona završena!");
            binding.btnSimulate.setText("Pogledaj završnicu");
        } else {
            binding.textMatchdayLabel.setText("Kolo " + matchday);
            binding.btnSimulate.setText("Simuliraj kolo " + matchday);
        }
        updateTransferWindow();
    }

    // ─── Competition toggle (league table ↔ Champions League) ───────────────────

    private void onLeagueStandings(List<SeasonStanding> list) {
        lastStandings = list;
        if (!showingUcl) {
            renderStandings(list, false);
            viewModel.refreshLeaders(clubId);
        }
    }

    private void onUclStandings(List<SeasonStanding> list) {
        lastUclStandings = list;
        if (showingUcl) {
            renderStandings(list, true);
            viewModel.refreshLeaders(viewModel.getUclId());
        }
    }

    private void renderActive() {
        renderStandings(showingUcl ? lastUclStandings : lastStandings, showingUcl);
    }

    private void showCompetition(boolean ucl) {
        showingUcl = ucl;
        applyCompetitionTheme(ucl);
        renderActive();
        viewModel.refreshLeaders(ucl ? viewModel.getUclId() : clubId);
    }

    /** Recolours the table + leaders cards and the toggle to the active competition. */
    private void applyCompetitionTheme(boolean ucl) {
        if (binding == null) return;
        int surface = ContextCompat.getColor(requireContext(), R.color.surface);
        int uclSurface = ContextCompat.getColor(requireContext(), R.color.ucl_surface);
        int uclAccent = ContextCompat.getColor(requireContext(), R.color.ucl_accent);
        int secondary = ContextCompat.getColor(requireContext(), R.color.text_secondary);
        int primary = ContextCompat.getColor(requireContext(), R.color.text_primary);

        binding.cardStandings.setCardBackgroundColor(ucl ? uclSurface : surface);
        binding.cardLeaders.setCardBackgroundColor(ucl ? uclSurface : surface);
        binding.textCompTitle.setText(ucl ? "LIGA PRVAKA" : "TABLICA");
        binding.textCompTitle.setTextColor(ucl ? uclAccent : secondary);

        int labelColor = ucl ? uclAccent : primary;
        binding.labelScorers.setTextColor(labelColor);
        binding.labelAssisters.setTextColor(labelColor);
        binding.labelClean.setTextColor(labelColor);

        binding.btnCompLeague.setBackgroundResource(
            ucl ? R.drawable.bg_pill_unselected : R.drawable.bg_pill_selected);
        binding.btnCompUcl.setBackgroundResource(
            ucl ? R.drawable.bg_pill_selected : R.drawable.bg_pill_unselected);
    }

    private void renderStandings(List<SeasonStanding> list, boolean ucl) {
        if (binding == null) return;
        LinearLayout container = binding.standingsContainer;
        container.removeAllViews();
        if (list == null || list.isEmpty()) return;

        int uclAccent = ContextCompat.getColor(requireContext(), R.color.ucl_accent);
        int primary = ContextCompat.getColor(requireContext(), R.color.text_primary);
        int zoneTint = ColorUtils.setAlphaComponent(
            ContextCompat.getColor(requireContext(), R.color.ucl_zone), 70);
        int uclUserRow = ColorUtils.setAlphaComponent(uclAccent, 50);
        int uclRow = ColorUtils.setAlphaComponent(uclAccent, 22);

        for (int i = 0; i < list.size(); i++) {
            SeasonStanding s = list.get(i);
            View row = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_standing_row, container, false);

            TextView rankView = row.findViewById(R.id.text_rank);
            TextView nameView = row.findViewById(R.id.text_team_name);
            rankView.setText(String.valueOf(i + 1));
            nameView.setText(s.getTeamName());
            ((TextView) row.findViewById(R.id.text_played)).setText(String.valueOf(s.getPlayed()));
            ((TextView) row.findViewById(R.id.text_won)).setText(String.valueOf(s.getWon()));
            ((TextView) row.findViewById(R.id.text_drawn)).setText(String.valueOf(s.getDrawn()));
            ((TextView) row.findViewById(R.id.text_lost)).setText(String.valueOf(s.getLost()));
            ((TextView) row.findViewById(R.id.text_pts)).setText(String.valueOf(s.getPoints()));

            if (s.isUserTeam()) {
                nameView.setTextColor(ucl ? uclAccent : theme.primary);
                nameView.setTypeface(null, android.graphics.Typeface.BOLD);
                row.setBackgroundColor(ucl ? uclUserRow : theme.rowTint());
            } else if (ucl) {
                nameView.setTextColor(primary);
                row.setBackgroundColor(uclRow);
            } else if (i < 5) {
                // Top 5 = Champions League qualification zone.
                nameView.setTextColor(primary);
                rankView.setTextColor(uclAccent);
                rankView.setTypeface(null, android.graphics.Typeface.BOLD);
                row.setBackgroundColor(zoneTint);
            } else {
                loadRowTint(row, s);
            }

            container.addView(row);
        }
    }

    // ─── Season leaderboards ────────────────────────────────────────────────────

    private void bindLeaders(SeasonHubViewModel.Leaders l) {
        if (binding == null || l == null) return;
        if (l.isEmpty()) {
            binding.cardLeaders.setVisibility(View.GONE);
            return;
        }
        binding.cardLeaders.setVisibility(View.VISIBLE);
        fillLeaderSection(binding.labelScorers, binding.leadersScorersContainer,
            l.scorers, true);
        fillLeaderSection(binding.labelAssisters, binding.leadersAssistersContainer,
            l.assisters, true);
        // Clean sheets are kept by the whole team, so the team name is the holder.
        fillLeaderSection(binding.labelClean, binding.leadersCleanContainer,
            l.cleanSheets, false);
    }

    private void fillLeaderSection(TextView label, LinearLayout container,
                                   List<TopScorerRow> rows, boolean showTeam) {
        container.removeAllViews();
        boolean empty = rows == null || rows.isEmpty();
        label.setVisibility(empty ? View.GONE : View.VISIBLE);
        container.setVisibility(empty ? View.GONE : View.VISIBLE);
        if (empty) return;

        for (int i = 0; i < rows.size(); i++) {
            TopScorerRow r = rows.get(i);
            View row = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_leader_row, container, false);

            ((TextView) row.findViewById(R.id.text_leader_rank)).setText(String.valueOf(i + 1));
            TextView nameView = row.findViewById(R.id.text_leader_name);
            nameView.setText(r.playerName);
            TextView teamView = row.findViewById(R.id.text_leader_team);
            if (showTeam && r.teamName != null && !r.teamName.isEmpty()) {
                teamView.setText(r.teamName);
                teamView.setVisibility(View.VISIBLE);
            } else {
                teamView.setVisibility(View.GONE);
            }
            ((TextView) row.findViewById(R.id.text_leader_value)).setText(String.valueOf(r.goals));

            // Highlight the user's club in their colour.
            if (userClubName != null && userClubName.equals(r.teamName)) {
                nameView.setTextColor(theme.primary);
                nameView.setTypeface(null, android.graphics.Typeface.BOLD);
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
