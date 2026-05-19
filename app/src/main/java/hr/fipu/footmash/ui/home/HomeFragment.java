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
        binding.btnSearch.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.nav_leagues));

        binding.featuredMatchCard.setOnClickListener(v ->
                navigateToTeams(177, "Premier League"));

        binding.chipPremierLeague.setOnClickListener(v ->
                navigateToTeams(177, "Premier League"));

        binding.chipCup.setOnClickListener(v ->
                navigateToTeams(302, "La Liga"));

        binding.textAllMatches.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.nav_leagues));
    }

    private void navigateToTeams(int leagueId, String leagueName) {
        Bundle args = new Bundle();
        args.putInt("leagueId", leagueId);
        args.putInt("season", 2024);
        args.putString("leagueName", leagueName);
        Navigation.findNavController(binding.getRoot())
                .navigate(R.id.nav_teams_list, args);
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

        viewModel.getActiveClub().observe(getViewLifecycleOwner(), club -> {
            if (club == null) {
                binding.featuredMatchCard.setVisibility(View.GONE);
                binding.lastMatchHeader.setVisibility(View.GONE);
            } else {
                viewModel.getLastPlayedUserFixture(club.getId()).observe(getViewLifecycleOwner(), fixture -> {
                    if (fixture == null) {
                        binding.featuredMatchCard.setVisibility(View.GONE);
                        binding.lastMatchHeader.setVisibility(View.GONE);
                    } else {
                        binding.featuredMatchCard.setVisibility(View.VISIBLE);
                        binding.lastMatchHeader.setVisibility(View.VISIBLE);
                        bindFixture(fixture, club);
                    }
                });
            }
        });
    }

    private void bindFixture(Fixture fixture, UserClub club) {
        binding.textHomeLeague.setText(club.getLeagueName());
        binding.textHomeMatchday.setText("Kolo " + fixture.getMatchday());

        binding.textHomeTeamName.setText(fixture.getHomeTeamName());
        binding.textAwayTeamName.setText(fixture.getAwayTeamName());

        viewModel.getMatchResult(fixture.getId()).observe(getViewLifecycleOwner(), result -> {
            if (result != null) {
                binding.textMatchScore.setText(result.getHomeGoals() + " : " + result.getAwayGoals());
            }
        });

        viewModel.getGoalScorers(fixture.getId()).observe(getViewLifecycleOwner(), scorers -> {
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

        // Fetch badges
        viewModel.getTeamBadgeUrl(fixture.getHomeTeamId()).observe(getViewLifecycleOwner(), url -> {
            String badgeUrl = url;
            if (badgeUrl == null || badgeUrl.isEmpty()) {
                String kebabName = fixture.getHomeTeamName().toLowerCase()
                    .replace(" & ", "-")
                    .replace("&", "")
                    .replace(" ", "-")
                    .replace(".", "");
                badgeUrl = "https://apiv2.allsportsapi.com/logo/" + fixture.getHomeTeamId() + "_" + kebabName + ".jpg";
            }
            Glide.with(this).load(badgeUrl).placeholder(new InitialsBadgeDrawable(fixture.getHomeTeamName())).into(binding.imgHomeTeamBadge);
        });

        viewModel.getTeamBadgeUrl(fixture.getAwayTeamId()).observe(getViewLifecycleOwner(), url -> {
            String badgeUrl = url;
            if (badgeUrl == null || badgeUrl.isEmpty()) {
                String kebabName = fixture.getAwayTeamName().toLowerCase()
                    .replace(" & ", "-")
                    .replace("&", "")
                    .replace(" ", "-")
                    .replace(".", "");
                badgeUrl = "https://apiv2.allsportsapi.com/logo/" + fixture.getAwayTeamId() + "_" + kebabName + ".jpg";
            }
            Glide.with(this).load(badgeUrl).placeholder(new InitialsBadgeDrawable(fixture.getAwayTeamName())).into(binding.imgAwayTeamBadge);
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
