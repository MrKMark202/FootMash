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

import hr.fipu.footmash.databinding.FragmentTeamSimulationBinding;
import hr.fipu.footmash.model.LeagueInfo;
import hr.fipu.footmash.model.RealTeam;

public class TeamSimulationFragment extends Fragment {

    private FragmentTeamSimulationBinding binding;
    private TeamSimulationViewModel viewModel;

    private final String GEMINI_API_KEY = hr.fipu.footmash.BuildConfig.GEMINI_API_KEY;

    private List<LeagueInfo> allLeagues = new ArrayList<>();
    private LeagueInfo selectedLeague;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentTeamSimulationBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(TeamSimulationViewModel.class);

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
                    break;
                }
            }
        });
    }

    private void runSimulationFlow() {
        String name = binding.editTeamName.getText().toString().trim();
        String ratingStr = binding.editTeamRating.getText().toString().trim();

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(ratingStr) || selectedLeague == null) {
            Toast.makeText(requireContext(), "Popunite sva polja i odaberite ligu",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        int rating;
        try {
            rating = Integer.parseInt(ratingStr);
        } catch (NumberFormatException e) {
            Toast.makeText(requireContext(), "Ocjena mora biti broj (0-100)",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        binding.progressBar.setVisibility(View.VISIBLE);
        binding.textSimulationResult.setText("Dohvaćam klubove lige...");

        startSimulation(name, rating, selectedLeague);
    }

    private void startSimulation(String name, int rating, LeagueInfo league) {
        viewModel.getTeamsInLeague(league.getId()).observe(getViewLifecycleOwner(), teams -> {
            if (teams == null || teams.isEmpty()) {
                binding.progressBar.setVisibility(View.GONE);
                binding.textSimulationResult.setText("Nema klubova u toj ligi.");
                return;
            }

            StringBuilder sb = new StringBuilder();
            int limit = Math.min(20, teams.size());
            for (int i = 0; i < limit; i++) {
                RealTeam team = teams.get(i);
                sb.append("- ").append(team.getName()).append("\n");
            }

            binding.textSimulationResult.setText("Šaljem podatke Gemini-ju...");

            viewModel.runSimulation(name, rating, league.getName(), sb.toString(), GEMINI_API_KEY)
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
