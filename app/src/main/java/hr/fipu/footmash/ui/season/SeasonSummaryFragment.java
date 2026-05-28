package hr.fipu.footmash.ui.season;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;

import hr.fipu.footmash.FootMashApp;
import hr.fipu.footmash.R;
import hr.fipu.footmash.databinding.FragmentSeasonSummaryBinding;
import hr.fipu.footmash.db.AppDatabase;
import hr.fipu.footmash.model.UserClub;
import hr.fipu.footmash.repository.SeasonRepository;
import hr.fipu.footmash.ui.util.ClubColors;

public class SeasonSummaryFragment extends Fragment {

    private FragmentSeasonSummaryBinding binding;
    private SeasonSummaryViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentSeasonSummaryBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        int clubId = getArguments() != null ? getArguments().getInt("clubId", -1) : -1;
        if (clubId == -1) return;

        viewModel = new ViewModelProvider(this).get(SeasonSummaryViewModel.class);
        viewModel.init(clubId);

        viewModel.getSummaryData().observe(getViewLifecycleOwner(), data -> {
            if (data == null) return;

            binding.textChampionName.setText(data.champion);
            binding.textChampionPoints.setText(data.championPoints + " bod.");

            binding.textGoldenBootPlayer.setText(data.goldenBootPlayer);
            binding.textGoldenBootDetails.setText(
                data.goldenBootTeam + " · " + data.goldenBootGoals + " gol.");

            binding.textUserPosition.setText(String.valueOf(data.userPosition));
            binding.textUserTeamName.setText(data.userTeamName);
            binding.textUserRecord.setText(
                "P" + data.userWon + " / R" + data.userDrawn + " / I" + data.userLost);
            binding.textUserGoals.setText(data.userGoalsFor + ":" + data.userGoalsAgainst);
            binding.textUserPoints.setText(data.userPoints + " bod.");

            String positionSuffix;
            int pos = data.userPosition;
            if (pos == 1) positionSuffix = "Prvak si! Čestitke!";
            else if (pos <= 4) positionSuffix = "Odlično! Europska mjesta!";
            else if (pos <= 10) positionSuffix = "Solidna sezona!";
            else if (pos <= 17) positionSuffix = "Preživjeli ste!";
            else positionSuffix = "Ispali ste!";
            binding.textUserPositionNote.setText(positionSuffix);
        });

        binding.btnNewSeason.setOnClickListener(v -> {
            NavOptions opts = new NavOptions.Builder()
                .setPopUpTo(R.id.nav_season, true)
                .build();
            Navigation.findNavController(v).navigate(R.id.nav_season, null, opts);
        });

        binding.btnNextSeason.setOnClickListener(v -> {
            binding.btnNextSeason.setEnabled(false);
            binding.btnNextSeason.setText("Pripremam sezonu...");
            SeasonRepository repo = FootMashApp.container(requireContext()).seasonRepository();
            new Thread(() -> {
                repo.startNextSeason(clubId);
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    Bundle args = new Bundle();
                    args.putInt("clubId", clubId);
                    NavOptions opts = new NavOptions.Builder()
                        .setPopUpTo(R.id.nav_season_hub, true)
                        .build();
                    Navigation.findNavController(v).navigate(R.id.nav_season_hub, args, opts);
                });
            }).start();
        });

        applyClubTheme(clubId);
    }

    /** Loads the club off-thread and re-skins the user-result card with its colours. */
    private void applyClubTheme(int clubId) {
        AppDatabase db = AppDatabase.getInstance(requireContext());
        new Thread(() -> {
            UserClub club = db.userClubDao().getClubByIdSync(clubId);
            if (club == null || !isAdded()) return;
            ClubColors.Theme theme = ClubColors.of(club.getRealTeamSourceId());
            requireActivity().runOnUiThread(() -> {
                if (binding == null) return;
                ClubColors.styleButton(binding.btnNextSeason, theme);
                binding.textSeasonLabel.setText("Sezona " + club.getSeasonLabel());
                binding.cardUserResult.setStrokeColor(theme.primary);
                binding.textUserResultLabel.setTextColor(theme.primary);
                binding.textUserPosition.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(theme.primary));
                binding.textUserPosition.setTextColor(theme.onPrimary);
            });
        }).start();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
