package hr.fipu.footmash.ui.ailab;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import java.util.ArrayList;
import java.util.List;

import hr.fipu.footmash.databinding.FragmentPlayerSimulationBinding;
import hr.fipu.footmash.model.LeagueInfo;
import hr.fipu.footmash.model.RealPlayer;
import hr.fipu.footmash.model.RealTeam;

public class PlayerSimulationFragment extends Fragment {

    private FragmentPlayerSimulationBinding binding;
    private PlayerSimulationViewModel viewModel;

    private final String GEMINI_API_KEY = hr.fipu.footmash.BuildConfig.GEMINI_API_KEY;

    private List<LeagueInfo> allLeagues = new ArrayList<>();
    private List<RealTeam> leagueTeams = new ArrayList<>();

    private LeagueInfo selectedLeague;
    private RealTeam selectedTeam;

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
                Toast.makeText(requireContext(),
                        "Nema lokalnih liga. Pokreni aplikaciju ponovno da se učitaju seed podaci.",
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private void setupLeagueAutocomplete() {
        List<String> leagueNames = new ArrayList<>();
        for (LeagueInfo league : allLeagues) {
            if (league.getName() != null) leagueNames.add(league.getName());
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_dropdown_item_1line, leagueNames);
        binding.autoCompleteLeague.setAdapter(adapter);

        binding.autoCompleteLeague.setOnItemClickListener((parent, view, position, id) -> {
            String selectedName = (String) parent.getItemAtPosition(position);
            for (LeagueInfo league : allLeagues) {
                if (selectedName != null && selectedName.equals(league.getName())) {
                    selectedLeague = league;
                    binding.autoCompleteTeam.setText("", false);
                    selectedTeam = null;
                    loadTeams(selectedLeague.getId());
                    break;
                }
            }
        });
    }

    private void loadTeams(int leagueId) {
        viewModel.getTeamsByLeague(leagueId).observe(getViewLifecycleOwner(), teams -> {
            if (teams != null && !teams.isEmpty()) {
                this.leagueTeams = teams;
                setupTeamAutocomplete();
            }
        });
    }

    private void setupTeamAutocomplete() {
        List<String> teamNames = new ArrayList<>();
        for (RealTeam team : leagueTeams) {
            if (team.getName() != null) teamNames.add(team.getName());
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_dropdown_item_1line, teamNames);
        binding.autoCompleteTeam.setAdapter(adapter);

        binding.autoCompleteTeam.setOnItemClickListener((parent, view, position, id) -> {
            String selectedName = (String) parent.getItemAtPosition(position);
            for (RealTeam team : leagueTeams) {
                if (selectedName != null && selectedName.equals(team.getName())) {
                    selectedTeam = team;
                    break;
                }
            }
        });
    }

    private void runSimulationFlow() {
        String name = binding.editPlayerName.getText().toString().trim();
        String position = binding.editPlayerPosition.getText().toString().trim();

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(position) || selectedTeam == null) {
            Toast.makeText(requireContext(), "Popunite sva polja i odaberite klub",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        binding.progressBar.setVisibility(View.VISIBLE);
        binding.textSimulationResult.setText("Dohvaćam roster kluba...");

        startRosterFetch(name, position, selectedTeam);
    }

    private void startRosterFetch(String name, String position, RealTeam team) {
        viewModel.getTeamRoster(team.getId()).observe(getViewLifecycleOwner(), players -> {
            if (players == null || players.isEmpty()) {
                binding.progressBar.setVisibility(View.GONE);
                binding.textSimulationResult.setText("Nema igrača za taj klub.");
                return;
            }

            StringBuilder sb = new StringBuilder();
            for (RealPlayer p : players) {
                sb.append("- ").append(p.getName())
                        .append(" (").append(p.getPosition())
                        .append(", OVR ").append(p.getOverall()).append(")\n");
            }

            binding.textSimulationResult.setText("Šaljem podatke Gemini-ju...");

            viewModel.runSimulation(name, position, team.getName(), sb.toString(), GEMINI_API_KEY)
                    .observe(getViewLifecycleOwner(), result -> {
                        binding.progressBar.setVisibility(View.GONE);
                        binding.textSimulationResult.setText(result);
                    });
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
