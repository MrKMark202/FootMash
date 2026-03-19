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
        viewModel.getTeamDetail(teamId).observe(getViewLifecycleOwner(), teams -> {
            binding.progressBar.setVisibility(View.GONE);
            if (teams != null && !teams.isEmpty()) {
                hr.fipu.footmash.model.TeamResponse team = teams.get(0);
                
                // Info o timu
                Glide.with(this)
                        .load(team.getTeamBadge())
                        .placeholder(R.drawable.ic_launcher_background)
                        .into(binding.imageTeamLogo);
                binding.textTeamName.setText(team.getTeamName());

                // AllSportsAPI Teams detalji mogu sadržavati stadion i ostalo, 
                // ali naša trenutna klasa TeamResponse ima samo osnovna polja.
                // Privremeno sakrivamo ili stavljamo N/A za detaljne statistike 
                // koje smo imali u API-Football (formu, odigrano, golove itd.)
                
                binding.textForm.setText("Klub: " + team.getTeamName());
                binding.textFixtures.setText("ID: " + team.getTeamKey());
                binding.textGoals.setText("");
                binding.textTeamCountry.setText(""); 
            } else {
                binding.textTeamName.setText(teamName);
                binding.textForm.setText("Podaci o klubu nisu dostupni.");
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
