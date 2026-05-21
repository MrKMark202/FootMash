package hr.fipu.footmash.ui.season;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import hr.fipu.footmash.R;
import hr.fipu.footmash.databinding.FragmentClubSetupBinding;
import hr.fipu.footmash.db.AppDatabase;
import hr.fipu.footmash.model.UserClub;

public class ClubSetupFragment extends Fragment {

    private FragmentClubSetupBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentClubSetupBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Bundle args = getArguments();
        int leagueId = args != null ? args.getInt("leagueId", 177) : 177;
        String leagueName = args != null ? args.getString("leagueName", "Premier League") : "Premier League";

        binding.textSelectedLeague.setText("⚽ " + leagueName);

        binding.btnConfirm.setOnClickListener(v -> {
            String clubName = binding.editClubName.getText() != null
                    ? binding.editClubName.getText().toString().trim() : "";

            if (TextUtils.isEmpty(clubName)) {
                binding.layoutClubName.setError("Unesi naziv kluba");
                return;
            }

            binding.btnConfirm.setEnabled(false);
            createClub(clubName, leagueId, leagueName, view);
        });
    }

    private void createClub(String clubName, int leagueId, String leagueName, View navView) {
        AppDatabase db = AppDatabase.getInstance(requireContext());

        new Thread(() -> {
            db.userClubDao().deactivateAll();

            UserClub club = new UserClub();
            club.setClubName(clubName);
            club.setLeagueId(leagueId);
            club.setLeagueName(leagueName);
            club.setBudget(UserClub.STARTING_BUDGET);
            club.setFormation("4-3-3");
            club.setSeasonYear(2025);
            club.setActive(true);

            long newId = db.userClubDao().insertClub(club);

            requireActivity().runOnUiThread(() -> {
                Bundle args = new Bundle();
                args.putInt("clubId", (int) newId);
                Navigation.findNavController(navView)
                          .navigate(R.id.action_clubSetup_to_formationPicker, args);
            });
        }).start();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
