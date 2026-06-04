package hr.fipu.footmash.ui.season;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import hr.fipu.footmash.FootMashApp;
import hr.fipu.footmash.R;
import hr.fipu.footmash.databinding.FragmentLeaguePickerBinding;
import hr.fipu.footmash.databinding.ItemSavedClubBinding;
import hr.fipu.footmash.model.UserClub;

/**
 * Sezona tab entry. Hosts:
 *
 * <ul>
 *     <li>"Moji klubovi" — every saved {@link UserClub}. Tap activates the
 *         club (deactivates the others) and jumps to {@code SeasonHubFragment}.
 *         Long-press deletes the club and its entire Season Mode footprint.</li>
 *     <li>"Nova sezona" — the five seeded leagues used to start a fresh club.</li>
 * </ul>
 */
public class LeaguePickerFragment extends Fragment {

    private FragmentLeaguePickerBinding binding;

    static class LeagueOption {
        final int leagueId;
        final String name;
        final String country;
        final String emoji;
        final int teamCount;

        LeagueOption(int leagueId, String name, String country, String emoji, int teamCount) {
            this.leagueId = leagueId;
            this.name = name;
            this.country = country;
            this.emoji = emoji;
            this.teamCount = teamCount;
        }
    }

    private static final List<LeagueOption> LEAGUES = Arrays.asList(
        new LeagueOption(177, "Premier League", "Engleska", "🏴󠁧󠁢󠁥󠁮󠁧󠁿", 20),
        new LeagueOption(302, "La Liga", "Španjolska", "🇪🇸", 20),
        new LeagueOption(78,  "Bundesliga", "Njemačka", "🇩🇪", 20),
        new LeagueOption(207, "Serie A", "Italija", "🇮🇹", 20),
        new LeagueOption(168, "Ligue 1", "Francuska", "🇫🇷", 20)
    );

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentLeaguePickerBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        renderLeagues();
        observeSavedClubs();
    }

    // ─── New-season league picker ────────────────────────────────────────────

    private void renderLeagues() {
        binding.containerLeagues.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        for (LeagueOption opt : LEAGUES) {
            View card = inflater.inflate(R.layout.item_league_pick,
                binding.containerLeagues, false);
            TextView emoji  = card.findViewById(R.id.textLeagueEmoji);
            TextView name   = card.findViewById(R.id.textLeagueName);
            TextView country = card.findViewById(R.id.textLeagueCountry);
            TextView teams  = card.findViewById(R.id.textLeagueTeams);
            emoji.setText(opt.emoji);
            name.setText(opt.name);
            country.setText(opt.country);
            teams.setText(opt.teamCount + " klubova · Sezona 2025/26");
            card.setOnClickListener(v -> {
                Bundle args = new Bundle();
                args.putInt("leagueId", opt.leagueId);
                args.putString("leagueName", opt.name);
                Navigation.findNavController(v)
                    .navigate(R.id.action_leaguePicker_to_clubMode, args);
            });
            binding.containerLeagues.addView(card);
        }
    }

    // ─── Saved clubs list ────────────────────────────────────────────────────

    private void observeSavedClubs() {
        FootMashApp.container(requireContext())
            .database()
            .userClubDao()
            .getAllClubs()
            .observe(getViewLifecycleOwner(), this::renderSavedClubs);
    }

    private void renderSavedClubs(@Nullable List<UserClub> clubs) {
        binding.containerSavedClubs.removeAllViews();
        boolean empty = clubs == null || clubs.isEmpty();
        int visibility = empty ? View.GONE : View.VISIBLE;
        binding.textSavedClubsHeader.setVisibility(visibility);
        binding.textSavedClubsHint.setVisibility(visibility);
        binding.dividerSavedClubs.setVisibility(visibility);
        if (empty) return;

        LayoutInflater inflater = LayoutInflater.from(requireContext());
        for (UserClub c : clubs) {
            View row = inflater.inflate(R.layout.item_saved_club,
                binding.containerSavedClubs, false);
            bindClubRow(row, c);
            binding.containerSavedClubs.addView(row);
        }
    }

    private void bindClubRow(View row, UserClub c) {
        ItemSavedClubBinding b = ItemSavedClubBinding.bind(row);
        b.textSavedClubName.setText(c.getClubName());
        b.textSavedClubMeta.setText(String.format(Locale.getDefault(),
            "%s · Sezona %s · %s",
            c.getLeagueName(),
            c.getSeasonLabel(),
            c.getFormation() != null ? c.getFormation() : "—"));
        b.textSavedClubActive.setVisibility(c.isActive() ? View.VISIBLE : View.GONE);

        row.setOnClickListener(v -> resumeClub(c));
        b.btnDeleteClub.setOnClickListener(v -> confirmDelete(c));
        row.setOnLongClickListener(v -> {
            confirmDelete(c);
            return true;
        });
    }

    private void resumeClub(UserClub c) {
        // Flip the active flag on a bg thread, then navigate. The season hub
        // looks up the club by id, not by isActive, so navigation is
        // independent of the flag — but flipping it keeps DB state consistent
        // with "the user's current focus is this club".
        new Thread(() -> {
            FootMashApp.container(requireContext())
                .database().userClubDao().deactivateAll();
            FootMashApp.container(requireContext())
                .database().userClubDao().activate(c.getId());
            if (!isAdded()) return;
            requireActivity().runOnUiThread(() -> {
                Bundle args = new Bundle();
                args.putInt("clubId", c.getId());
                NavOptions opts = new NavOptions.Builder()
                    .setPopUpTo(R.id.nav_season, true)
                    .build();
                Navigation.findNavController(requireView())
                    .navigate(R.id.nav_season_hub, args, opts);
            });
        }).start();
    }

    private void confirmDelete(UserClub c) {
        new MaterialAlertDialogBuilder(requireContext(), R.style.FootMashDialog)
            .setTitle("Obriši klub?")
            .setMessage(c.getClubName() + " — " + c.getLeagueName()
                + "\nSezona i sva natjecanja bit će obrisani.")
            .setNegativeButton("Odustani", null)
            .setPositiveButton("Obriši", (dlg, which) ->
                new Thread(() -> FootMashApp.container(requireContext())
                    .savedGamesRepository()
                    .deleteClub(c.getId())).start())
            .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
