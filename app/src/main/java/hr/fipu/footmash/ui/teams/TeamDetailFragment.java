package hr.fipu.footmash.ui.teams;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.bumptech.glide.Glide;

import hr.fipu.footmash.R;
import hr.fipu.footmash.databinding.FragmentTeamDetailBinding;
import hr.fipu.footmash.model.TeamStatisticsResponse.TeamStats;

public class TeamDetailFragment extends Fragment {

    private FragmentTeamDetailBinding binding;
    private TeamsViewModel viewModel;
    private int teamId;
    private int leagueId;
    private int season;
    private String teamName;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentTeamDetailBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(TeamsViewModel.class);

        if (getArguments() != null) {
            teamId = getArguments().getInt("teamId", -1);
            leagueId = getArguments().getInt("leagueId", -1);
            season = getArguments().getInt("season", 2023);
            teamName = getArguments().getString("teamName", "");
        }

        binding.textTeamName.setText(teamName);

        // Fetch team statistics
        if (teamId != -1 && leagueId != -1) {
            observeViewModel();
        } else {
            Log.e("TeamDetail", "Nedostaje teamId ili leagueId za dohvaćanje statistike");
        }

        // Navegacija na roster
        binding.btnViewSquad.setOnClickListener(v -> {
            Bundle args = new Bundle();
            args.putInt("teamId", teamId);
            args.putInt("season", season);
            args.putString("teamName", teamName);
            Navigation.findNavController(v).navigate(R.id.action_team_to_players, args);
        });
    }

    private void observeViewModel() {
        binding.progressBar.setVisibility(View.VISIBLE);
        viewModel.getTeamStatistics(leagueId, season, teamId).observe(getViewLifecycleOwner(), statsResponse -> {
            binding.progressBar.setVisibility(View.GONE);
            if (statsResponse != null) {
                // Info o timu
                if (statsResponse.getTeam() != null) {
                    Glide.with(this)
                            .load(statsResponse.getTeam().getLogo())
                            .into(binding.imageTeamLogo);
                    binding.textTeamName.setText(statsResponse.getTeam().getName());
                }

                // Info o stadionu (dobiveno putem teams API prije, ali može i prazno zasad dok ne spojimo)
                // Ako želimo baš detalje stadiona trebali bi ih dobiti iz Teams poziva,
                // ali na ovom ekranu smo iz Statistics poziva. Ovdje prikazujemo formu:
                
                binding.textForm.setText("Forma: " + (statsResponse.getForm() != null ? statsResponse.getForm() : "N/A"));
                
                if (statsResponse.getFixtures() != null) {
                    binding.textFixtures.setText("Odigrano utakmica: " + statsResponse.getFixtures().getPlayed().getTotal());
                }
                
                if (statsResponse.getGoals() != null && statsResponse.getGoals().getGoalsFor() != null) {
                    binding.textGoals.setText("Zabijeni golovi: " + statsResponse.getGoals().getGoalsFor().getTotal().getTotal());
                }

                // Privremeno stavljamo "N/A" za ove podatke dok ih ne spremimo i prenesemo iz Teams adaptera
                binding.textTeamCountry.setText("Država tima");
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
