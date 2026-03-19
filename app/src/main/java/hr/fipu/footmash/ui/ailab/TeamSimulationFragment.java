package hr.fipu.footmash.ui.ailab;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;
import java.util.ArrayList;

import android.app.AlertDialog;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import java.util.ArrayList;
import java.util.List;

import hr.fipu.footmash.databinding.FragmentTeamSimulationBinding;
import hr.fipu.footmash.model.StandingResponse;

public class TeamSimulationFragment extends Fragment {

    private FragmentTeamSimulationBinding binding;
    private TeamSimulationViewModel viewModel;
    
    // Ključ se sada dohvaća iz BuildConfig-a (local.properties)
    private final String GEMINI_API_KEY = hr.fipu.footmash.BuildConfig.GEMINI_API_KEY;

    private List<hr.fipu.footmash.model.LeagueResponse> allLeagues = new ArrayList<>();
    private int selectedLeagueId = -1;

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
                    break;
                }
            }
        });
    }

    private void runSimulationFlow() {
        String name = binding.editTeamName.getText().toString().trim();
        String ratingStr = binding.editTeamRating.getText().toString().trim();

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(ratingStr) || selectedLeagueId == -1) {
            Toast.makeText(requireContext(), "Popunite sva polja i odaberite ligu", Toast.LENGTH_SHORT).setDuration(Toast.LENGTH_SHORT);
            return;
        }

        int rating = Integer.parseInt(ratingStr);
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.textSimulationResult.setText("Dohvaćam tablicu lige...");

        startStandingsFetch(name, rating, selectedLeagueId);
    }

    private void startStandingsFetch(String name, int rating, int leagueId) {
        binding.textSimulationResult.setText("Dohvaćam tablicu ciljane lige...");

        // Faza 1: Dohvati tablicu lige iz AllSportsAPI
        viewModel.getLeagueStandings(leagueId, 0).observe(getViewLifecycleOwner(), standings -> {
            if (standings != null && !standings.isEmpty()) {
                binding.textSimulationResult.setText("Tablica dohvaćena. Šaljem podatke Gemini umjetnoj inteligenciji...");
                
                // Serijaliziramo top timove za kontekst
                StringBuilder sb = new StringBuilder();
                int limit = Math.min(10, standings.size());
                for (int i = 0; i < limit; i++) {
                    hr.fipu.footmash.model.StandingResponse info = standings.get(i);
                    sb.append(info.getStandingPlace()).append(". ")
                      .append(info.getStandingTeam())
                      .append(" (Bodovi: ").append(info.getStandingPts()).append(")\n");
                }
                
                String standingsInfo = sb.toString();

                // Faza 2: Pošalji podatke u Gemini za simulaciju
                viewModel.runSimulation(name, rating, standingsInfo, GEMINI_API_KEY)
                        .observe(getViewLifecycleOwner(), result -> {
                            binding.progressBar.setVisibility(View.GONE);
                            binding.textSimulationResult.setText(result);
                        });
                
            } else {
                binding.progressBar.setVisibility(View.GONE);
                binding.textSimulationResult.setText("Greška pri dohvaćanju tablice. Provjerite API ključ.");
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
