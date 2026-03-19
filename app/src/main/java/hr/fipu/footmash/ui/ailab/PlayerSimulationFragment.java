package hr.fipu.footmash.ui.ailab;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;

import hr.fipu.footmash.databinding.FragmentPlayerSimulationBinding;
import hr.fipu.footmash.model.PlayerResponse;

public class PlayerSimulationFragment extends Fragment {

    private FragmentPlayerSimulationBinding binding;
    private PlayerSimulationViewModel viewModel;
    
    // Ključ se sada dohvaća iz BuildConfig-a (local.properties)
    private final String GEMINI_API_KEY = hr.fipu.footmash.BuildConfig.GEMINI_API_KEY;

    private List<hr.fipu.footmash.model.LeagueResponse> allLeagues = new ArrayList<>();
    private List<hr.fipu.footmash.model.TeamResponse> leagueTeams = new ArrayList<>();
    
    private int selectedLeagueId = -1;
    private int selectedTeamId = -1;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentPlayerSimulationBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(PlayerSimulationViewModel.class);

        binding.btnRunSimulation.setOnClickListener(v -> runSimulationFlow());

        loadLeagues();
    }

    private void loadLeagues() {
        viewModel.getLeagues().observe(getViewLifecycleOwner(), leagues -> {
            if (leagues != null && !leagues.isEmpty()) {
                this.allLeagues = leagues;
                setupLeagueAutocomplete();
            } else {
                Toast.makeText(requireContext(), "API nije vratio lige. Provjerite ključ i internet.", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void setupLeagueAutocomplete() {
        if (allLeagues == null) return;
        List<String> leagueNames = new ArrayList<>();
        for (hr.fipu.footmash.model.LeagueResponse league : allLeagues) {
            leagueNames.add(league.getLeagueName());
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_dropdown_item_1line, leagueNames);
        binding.autoCompleteLeague.setAdapter(adapter);

        binding.autoCompleteLeague.setOnItemClickListener((parent, view, position, id) -> {
            String selectedHeader = (String) parent.getItemAtPosition(position);
            for (hr.fipu.footmash.model.LeagueResponse league : allLeagues) {
                if (league.getLeagueName().equals(selectedHeader)) {
                    selectedLeagueId = league.getLeagueKey();
                    binding.autoCompleteTeam.setText(""); // Reset teams
                    selectedTeamId = -1;
                    loadTeams(selectedLeagueId);
                    break;
                }
            }
        });
    }

    private void loadTeams(int leagueId) {
        viewModel.getTeamsByLeague(leagueId, 0).observe(getViewLifecycleOwner(), teams -> {
            if (teams != null && !teams.isEmpty()) {
                this.leagueTeams = teams;
                setupTeamAutocomplete();
            }
        });
    }

    private void setupTeamAutocomplete() {
        if (leagueTeams == null) return;
        List<String> teamNames = new ArrayList<>();
        for (hr.fipu.footmash.model.TeamResponse team : leagueTeams) {
            teamNames.add(team.getTeamName());
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_dropdown_item_1line, teamNames);
        binding.autoCompleteTeam.setAdapter(adapter);

        binding.autoCompleteTeam.setOnItemClickListener((parent, view, position, id1) -> {
            String selectedHeader = (String) parent.getItemAtPosition(position);
            for (hr.fipu.footmash.model.TeamResponse team : leagueTeams) {
                if (team.getTeamName().equals(selectedHeader)) {
                    selectedTeamId = team.getTeamKey();
                    break;
                }
            }
        });
    }

    private void runSimulationFlow() {
        String name = binding.editPlayerName.getText().toString().trim();
        String position = binding.editPlayerPosition.getText().toString().trim();

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(position) || selectedTeamId == -1) {
            Toast.makeText(requireContext(), "Popunite sva polja i odaberite klub", Toast.LENGTH_SHORT).show();
            return;
        }

        binding.progressBar.setVisibility(View.VISIBLE);
        binding.textSimulationResult.setText("Dohvaćam roster kluba...");

        startRosterFetch(name, position, selectedTeamId);
    }

    private void startRosterFetch(String name, String position, int teamId) {
        binding.textSimulationResult.setText("Dohvaćam podatke rostera kluba...");

        // Faza 1: Dohvati roster trenutnog kluba
        viewModel.getTeamRoster(teamId, 0).observe(getViewLifecycleOwner(), players -> {
            if (players != null && !players.isEmpty()) {
                binding.textSimulationResult.setText("Roster dohvaćen. Šaljem podatke Gemini umjetnoj inteligenciji...");
                
                // Serijaliziramo roster (samo prva 3 bitna imena da ne probijemo token limit, ili cijelu listu)
                StringBuilder sb = new StringBuilder();
                for (hr.fipu.footmash.model.PlayerResponse p : players) {
                    sb.append("- ").append(p.getPlayerName()).append(" (").append(p.getPlayerType()).append(")\n");
                }
                
                String teamJson = sb.toString();
                
                viewModel.runSimulation(name, position, teamJson, GEMINI_API_KEY)
                        .observe(getViewLifecycleOwner(), result -> {
                            binding.progressBar.setVisibility(View.GONE);
                            binding.textSimulationResult.setText(result);
                        });
            } else {
                binding.progressBar.setVisibility(View.GONE);
                binding.textSimulationResult.setText("Greška pri dohvaćanju rostera. Provjerite API ključ.");
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
