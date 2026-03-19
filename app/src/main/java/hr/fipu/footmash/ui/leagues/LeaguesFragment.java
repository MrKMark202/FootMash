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
import hr.fipu.footmash.databinding.FragmentLeaguesBinding;

public class LeaguesFragment extends Fragment {

    private FragmentLeaguesBinding binding;
    private LeaguesViewModel viewModel;
    private LeaguesAdapter adapter;
    private String countryName = "World";

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentLeaguesBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(LeaguesViewModel.class);

        if (getArguments() != null) {
            countryName = getArguments().getString("countryName", "World");
        }
        
        binding.toolbar.setTitle(countryName + " " + getString(R.string.leagues_title));
        binding.toolbar.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material);
        binding.toolbar.setNavigationOnClickListener(v -> Navigation.findNavController(v).navigateUp());

        setupRecyclerView();
        observeViewModel();
    }

    private void setupRecyclerView() {
        adapter = new LeaguesAdapter(league -> {
            Bundle args = new Bundle();
            args.putInt("leagueId", league.getLeagueKey());
            args.putInt("season", 2024); 
            args.putString("leagueName", league.getLeagueName());
            
            Navigation.findNavController(binding.getRoot()).navigate(R.id.action_leagues_to_standings, args);
        });

        binding.recyclerLeagues.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerLeagues.setAdapter(adapter);
    }

    private void observeViewModel() {
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.textEmpty.setVisibility(View.GONE);

        viewModel.getLeagues().observe(getViewLifecycleOwner(), leagues -> {
            binding.progressBar.setVisibility(View.GONE);
            if (leagues != null && !leagues.isEmpty()) {
                adapter.setLeagues(leagues);
                binding.recyclerLeagues.setVisibility(View.VISIBLE);
                binding.textEmpty.setVisibility(View.GONE);
            } else {
                binding.recyclerLeagues.setVisibility(View.GONE);
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
