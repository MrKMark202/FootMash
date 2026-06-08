package hr.fipu.footmash.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import hr.fipu.footmash.R;
import hr.fipu.footmash.databinding.FragmentHomeBinding;
import hr.fipu.footmash.ui.leagues.LeaguesAdapter;
import hr.fipu.footmash.model.Fixture;
import hr.fipu.footmash.model.GoalScorer;
import hr.fipu.footmash.model.UserClub;
import hr.fipu.footmash.ui.util.InitialsBadgeDrawable;
import com.bumptech.glide.Glide;
import java.util.List;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private HomeViewModel viewModel;
    private LeaguesAdapter featuredLeaguesAdapter;
    private UserClub activeClub;
    private Fixture currentFixture;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(HomeViewModel.class);

        setupClickListeners();
        setupRecyclerView();
        observeViewModel();
    }

    private void setupClickListeners() {
        // Tapping the "last match" card jumps back to the season the user is
        // managing, so they can pick up where they left off.
        binding.featuredMatchCard.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.nav_season));

        binding.textAllMatches.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.nav_leagues));

        // Quick-action shortcuts to the main sections.
        binding.cardActionCareer.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.nav_ai_lab));
        binding.cardActionSeason.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.nav_season));
        binding.cardActionWorldCup.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.nav_world_cup));
        binding.cardActionLeagues.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.nav_leagues));
    }

    private void setupRecyclerView() {
        featuredLeaguesAdapter = new LeaguesAdapter(league -> {
            Bundle args = new Bundle();
            args.putInt("leagueId", league.getLeagueKey());
            args.putInt("season", 2024);
            args.putString("leagueName", league.getLeagueName());
            Navigation.findNavController(binding.getRoot())
                    .navigate(R.id.nav_teams_list, args);
        });

        binding.recyclerFeaturedLeagues.setLayoutManager(
                new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        );
        binding.recyclerFeaturedLeagues.setAdapter(featuredLeaguesAdapter);
    }

    private void observeViewModel() {
        binding.progressBar.setVisibility(View.VISIBLE);
        viewModel.getFeaturedLeagues().observe(getViewLifecycleOwner(), leagues -> {
            binding.progressBar.setVisibility(View.GONE);
            featuredLeaguesAdapter.setLeagues(leagues);
        });

        // Each source is observed exactly once; switchMap in the ViewModel swaps
        // the underlying query when the active club / fixture changes.
        viewModel.getActiveClub().observe(getViewLifecycleOwner(), club -> activeClub = club);

        viewModel.getLastFixture().observe(getViewLifecycleOwner(), fixture -> {
            currentFixture = fixture;
            boolean show = fixture != null;
            binding.featuredMatchCard.setVisibility(show ? View.VISIBLE : View.GONE);
            binding.lastMatchHeader.setVisibility(show ? View.VISIBLE : View.GONE);
            if (show) bindFixture(fixture);
        });

        viewModel.getLastResult().observe(getViewLifecycleOwner(), result -> {
            if (result != null) {
                binding.textMatchScore.setText(result.getHomeGoals() + " : " + result.getAwayGoals());
            } else {
                binding.textMatchScore.setText("-");
            }
        });

        viewModel.getLastScorers().observe(getViewLifecycleOwner(), scorers -> {
            if (scorers != null && !scorers.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                for (GoalScorer s : scorers) {
                    if (sb.length() > 0) sb.append(", ");
                    sb.append(s.getPlayerName()).append(" (").append(s.getMinute()).append("')");
                }
                binding.textMatchScorers.setText(sb.toString());
            } else {
                binding.textMatchScorers.setText("");
            }
        });

        viewModel.getHomeBadgeUrl().observe(getViewLifecycleOwner(), url -> {
            if (currentFixture != null) loadBadge(url, currentFixture.getHomeTeamId(),
                currentFixture.getHomeTeamName(), binding.imgHomeTeamBadge);
        });
        viewModel.getAwayBadgeUrl().observe(getViewLifecycleOwner(), url -> {
            if (currentFixture != null) loadBadge(url, currentFixture.getAwayTeamId(),
                currentFixture.getAwayTeamName(), binding.imgAwayTeamBadge);
        });
    }

    private void bindFixture(Fixture fixture) {
        binding.textHomeLeague.setText(activeClub != null ? activeClub.getLeagueName() : "");
        binding.textHomeMatchday.setText("Kolo " + fixture.getMatchday());
        binding.textHomeTeamName.setText(fixture.getHomeTeamName());
        binding.textAwayTeamName.setText(fixture.getAwayTeamName());
    }

    private void loadBadge(String url, int teamId, String teamName, android.widget.ImageView target) {
        String badgeUrl = url;
        if (badgeUrl == null || badgeUrl.isEmpty()) {
            String kebabName = teamName.toLowerCase()
                .replace(" & ", "-")
                .replace("&", "")
                .replace(" ", "-")
                .replace(".", "");
            badgeUrl = "https://apiv2.allsportsapi.com/logo/" + teamId + "_" + kebabName + ".jpg";
        }
        Glide.with(this).load(badgeUrl)
            .placeholder(new InitialsBadgeDrawable(teamName)).into(target);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
