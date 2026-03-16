package hr.fipu.footmash.ui.ailab;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import java.util.List;

import hr.fipu.footmash.databinding.FragmentTeamSimulationBinding;
import hr.fipu.footmash.model.StandingResponse;

public class TeamSimulationFragment extends Fragment {

    private FragmentTeamSimulationBinding binding;
    private TeamSimulationViewModel viewModel;
    
    // Ključ koji nam je korisnik dao unaprijed:
    private final String GEMINI_API_KEY = "AIzaSyCQmFGmVTqOMQ6A_hvYT6_T6k84VHMFYTA";

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
    }

    private void runSimulationFlow() {
        String name = binding.editTeamName.getText().toString().trim();
        String ratingStr = binding.editTeamRating.getText().toString().trim();
        String leagueIdStr = binding.editTargetLeague.getText().toString().trim();

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(ratingStr) || TextUtils.isEmpty(leagueIdStr)) {
            Toast.makeText(requireContext(), "Molimo unesite sve podatke", Toast.LENGTH_SHORT).show();
            return;
        }

        int leagueId = Integer.parseInt(leagueIdStr);
        int rating = Integer.parseInt(ratingStr);
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.textSimulationResult.setText("Dohvaćam tablicu ciljane lige...");

        // Faza 1: Dohvati tablicu lige iz API-Football
        viewModel.getLeagueStandings(leagueId, 2023).observe(getViewLifecycleOwner(), standingsResponses -> {
            boolean hasStandings = standingsResponses != null && !standingsResponses.isEmpty() 
                && standingsResponses.get(0).getLeague() != null 
                && standingsResponses.get(0).getLeague().getStandings() != null
                && !standingsResponses.get(0).getLeague().getStandings().isEmpty();

            if (hasStandings) {
                binding.textSimulationResult.setText("Tablica dohvaćena. Šaljem podatke Gemini umjetnoj inteligenciji...");
                List<StandingResponse.StandingEntry> standings = standingsResponses.get(0).getLeague().getStandings().get(0);
                
                // Serijaliziramo top 5 do max 10 timova za kontekst (da ne preopteretimo prompt previše tekstom)
                StringBuilder sb = new StringBuilder();
                int limit = Math.min(10, standings.size());
                for (int i = 0; i < limit; i++) {
                    StandingResponse.StandingEntry info = standings.get(i);
                    sb.append(info.getRank()).append(". ");
                    if (info.getTeam() != null) sb.append(info.getTeam().getName());
                    sb.append(" (Bodovi: ").append(info.getPoints()).append(")\n");
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
                binding.textSimulationResult.setText("Greška pri dohvaćanju tablice s API-Football. Provjerite ID lige.");
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
