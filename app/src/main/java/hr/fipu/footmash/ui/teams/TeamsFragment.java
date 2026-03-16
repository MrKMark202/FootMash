package hr.fipu.footmash.ui.teams;

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
import hr.fipu.footmash.databinding.FragmentTeamsBinding;

public class TeamsFragment extends Fragment {

    private FragmentTeamsBinding binding;
    private TeamsViewModel viewModel;
    private TeamsAdapter adapter;
    private int leagueId;
    private int season;
    private String leagueName;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentTeamsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(TeamsViewModel.class);

        if (getArguments() != null) {
            leagueId = getArguments().getInt("leagueId", -1);
            season = getArguments().getInt("season", 2023);
            leagueName = getArguments().getString("leagueName", "");
        }
        
        binding.toolbar.setTitle(leagueName.isEmpty() ? getString(R.string.teams_title) : leagueName);
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
        adapter = new TeamsAdapter(teamResponse -> {
            Bundle args = new Bundle();
            args.putInt("teamId", teamResponse.getTeam().getId());
            args.putInt("leagueId", leagueId);
            args.putInt("season", season);
            args.putString("teamName", teamResponse.getTeam().getName());
            
            Navigation.findNavController(binding.getRoot()).navigate(R.id.action_teams_to_detail, args);
        });

        binding.recyclerTeams.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerTeams.setAdapter(adapter);
    }

    private void observeViewModel() {
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.textEmpty.setVisibility(View.GONE);

        viewModel.getTeamsByLeague(leagueId, season).observe(getViewLifecycleOwner(), teams -> {
            binding.progressBar.setVisibility(View.GONE);
            if (teams != null && !teams.isEmpty()) {
                adapter.setTeams(teams);
                binding.recyclerTeams.setVisibility(View.VISIBLE);
                binding.textEmpty.setVisibility(View.GONE);
            } else {
                binding.recyclerTeams.setVisibility(View.GONE);
                binding.textEmpty.setVisibility(View.VISIBLE);
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
