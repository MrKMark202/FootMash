package hr.fipu.footmash.ui.leagues;

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
import hr.fipu.footmash.databinding.FragmentStandingsBinding;

public class StandingsFragment extends Fragment {

    private FragmentStandingsBinding binding;
    private StandingsViewModel viewModel;
    private StandingsAdapter adapter;
    private int leagueId;
    private int season;
    private String leagueName;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentStandingsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(StandingsViewModel.class);

        if (getArguments() != null) {
            leagueId = getArguments().getInt("leagueId", -1);
            season = getArguments().getInt("season", 2023);
            leagueName = getArguments().getString("leagueName", "");
        }
        
        binding.toolbar.setTitle(leagueName.isEmpty() ? getString(R.string.standings_title) : "Tablica: " + leagueName);
        binding.toolbar.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material);
        binding.toolbar.setNavigationOnClickListener(v -> Navigation.findNavController(v).navigateUp());

        setupRecyclerView();
        if (leagueId != -1) {
            observeViewModel();
        } else {
            binding.textEmpty.setVisibility(View.VISIBLE);
        }
    }

    private void setupRecyclerView() {
        adapter = new StandingsAdapter();
        adapter.setOnTeamClickListener(standing -> {
            Bundle args = new Bundle();
            args.putInt("teamId", standing.getTeamKey());
            args.putInt("leagueId", leagueId);
            args.putInt("season", season);
            args.putString("teamName", standing.getStandingTeam());
            Navigation.findNavController(binding.getRoot()).navigate(R.id.nav_team_detail, args);
        });
        binding.recyclerStandings.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerStandings.setAdapter(adapter);
    }

    private void observeViewModel() {
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.textEmpty.setVisibility(View.GONE);

        viewModel.getStandings(leagueId, season).observe(getViewLifecycleOwner(), standings -> {
            binding.progressBar.setVisibility(View.GONE);
            
            if (standings != null && !standings.isEmpty()) {
                adapter.setStandings(standings);
                binding.recyclerStandings.setVisibility(View.VISIBLE);
                binding.textEmpty.setVisibility(View.GONE);
            } else {
                showEmpty();
            }
        });
    }

    private void showEmpty() {
        binding.recyclerStandings.setVisibility(View.GONE);
        binding.textEmpty.setVisibility(View.VISIBLE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
