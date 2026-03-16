package hr.fipu.footmash.ui.players;

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

import com.google.gson.Gson;

import hr.fipu.footmash.R;
import hr.fipu.footmash.databinding.FragmentPlayersBinding;

public class PlayersFragment extends Fragment {

    private FragmentPlayersBinding binding;
    private PlayersViewModel viewModel;
    private PlayersAdapter adapter;
    private int teamId;
    private int season;
    private String teamName;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentPlayersBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(PlayersViewModel.class);

        if (getArguments() != null) {
            teamId = getArguments().getInt("teamId", -1);
            season = getArguments().getInt("season", 2023);
            teamName = getArguments().getString("teamName", "");
        }
        
        binding.toolbar.setTitle(teamName.isEmpty() ? getString(R.string.players_title) : "Roster: " + teamName);
        binding.toolbar.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material);
        binding.toolbar.setNavigationOnClickListener(v -> Navigation.findNavController(v).navigateUp());

        setupRecyclerView();
        if (teamId != -1) {
            observeViewModel();
        } else {
            binding.textEmpty.setVisibility(View.VISIBLE);
        }
    }

    private void setupRecyclerView() {
        adapter = new PlayersAdapter(playerResponse -> {
            Bundle args = new Bundle();
            // Serijalizirajmo objekt jer ga je teško proslijediti cijelog, ili možemo samo poslati info
            String playerDataJson = new Gson().toJson(playerResponse);
            args.putString("playerData", playerDataJson);
            
            Navigation.findNavController(binding.getRoot()).navigate(R.id.action_players_to_detail, args);
        });

        binding.recyclerPlayers.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerPlayers.setAdapter(adapter);
    }

    private void observeViewModel() {
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.textEmpty.setVisibility(View.GONE);

        viewModel.getPlayersByTeam(teamId, season).observe(getViewLifecycleOwner(), players -> {
            binding.progressBar.setVisibility(View.GONE);
            if (players != null && !players.isEmpty()) {
                adapter.setPlayers(players);
                binding.recyclerPlayers.setVisibility(View.VISIBLE);
                binding.textEmpty.setVisibility(View.GONE);
            } else {
                binding.recyclerPlayers.setVisibility(View.GONE);
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
