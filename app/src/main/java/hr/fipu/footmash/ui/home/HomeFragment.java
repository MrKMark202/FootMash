package hr.fipu.footmash.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import java.util.ArrayList;

import hr.fipu.footmash.R;
import hr.fipu.footmash.databinding.FragmentHomeBinding;
import hr.fipu.footmash.ui.leagues.LeaguesAdapter;

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
        // Uklonjeno jer su gumbi maknuti s novim Behance UI dizajnom
        // Navigacija se sada odvija primarno kroz donju navigacijsku traku
    }

    private void setupRecyclerView() {
        featuredLeaguesAdapter = new LeaguesAdapter(league -> {
            Bundle args = new Bundle();
            args.putInt("leagueId", league.getLeague().getId());
            args.putInt("season", 2023); // Default to current season
            args.putString("leagueName", league.getLeague().getName());
            Navigation.findNavController(binding.getRoot()).navigate(R.id.nav_standings, args);
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
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
