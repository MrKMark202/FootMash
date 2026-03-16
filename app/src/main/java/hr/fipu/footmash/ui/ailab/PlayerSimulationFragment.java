package hr.fipu.footmash.ui.ailab;

import android.content.Context;
import android.content.SharedPreferences;
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

import com.google.gson.Gson;

import java.util.List;

import hr.fipu.footmash.databinding.FragmentPlayerSimulationBinding;
import hr.fipu.footmash.model.PlayerResponse;

public class PlayerSimulationFragment extends Fragment {

    private FragmentPlayerSimulationBinding binding;
    private PlayerSimulationViewModel viewModel;
    
    // Ključ se sada dohvaća iz BuildConfig-a (local.properties)
    private final String GEMINI_API_KEY = hr.fipu.footmash.BuildConfig.GEMINI_API_KEY;

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
    }

    private void runSimulationFlow() {
        String name = binding.editPlayerName.getText().toString().trim();
        String position = binding.editPlayerPosition.getText().toString().trim();
        String teamIdStr = binding.editTargetTeam.getText().toString().trim();

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(position) || TextUtils.isEmpty(teamIdStr)) {
            Toast.makeText(requireContext(), "Molimo unesite sve podatke", Toast.LENGTH_SHORT).show();
            return;
        }

        int teamId = Integer.parseInt(teamIdStr);
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.textSimulationResult.setText("Dohvaćam podatke rostera kluba...");

        // Faza 1: Dohvati roster trenutnog kluba iz API-Football
        viewModel.getTeamRoster(teamId, 2023).observe(getViewLifecycleOwner(), players -> {
            if (players != null && !players.isEmpty()) {
                binding.textSimulationResult.setText("Roster dohvaćen. Šaljem podatke Gemini umjetnoj inteligenciji...");
                
                // Serijaliziramo roster (samo prva 3 bitna imena da ne probijemo token limit, ili cijelu listu)
                StringBuilder sb = new StringBuilder();
                int limit = Math.min(10, players.size());
                for (int i = 0; i < limit; i++) {
                    PlayerResponse.PlayerInfo p = players.get(i).getPlayer();
                    PlayerResponse.Statistics s = null;
                    if (players.get(i).getStatistics() != null && !players.get(i).getStatistics().isEmpty()) {
                        s = players.get(i).getStatistics().get(0);
                    }
                    if (p != null) {
                        sb.append(p.getName()).append(" ");
                        if (s != null && s.getGames() != null && s.getGames().getPosition() != null) {
                            sb.append("(").append(s.getGames().getPosition()).append(")");
                        }
                        sb.append(", ");
                    }
                }
                
                String rosterInfo = sb.toString();

                // Faza 2: Pošalji podatke u Gemini za simulaciju
                viewModel.runSimulation(name, position, rosterInfo, GEMINI_API_KEY)
                        .observe(getViewLifecycleOwner(), result -> {
                            binding.progressBar.setVisibility(View.GONE);
                            binding.textSimulationResult.setText(result);
                        });
                
            } else {
                binding.progressBar.setVisibility(View.GONE);
                binding.textSimulationResult.setText("Greška pri dohvaćanju tima iz API-Football. Provjerite ID.");
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
