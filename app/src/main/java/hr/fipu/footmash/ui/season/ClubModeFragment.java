package hr.fipu.footmash.ui.season;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import hr.fipu.footmash.R;
import hr.fipu.footmash.databinding.FragmentClubModeBinding;

public class ClubModeFragment extends Fragment {

    private FragmentClubModeBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentClubModeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Bundle args = getArguments();
        int leagueId = args != null ? args.getInt("leagueId", 177) : 177;
        String leagueName = args != null ? args.getString("leagueName", "Premier League") : "Premier League";

        binding.textLeagueLabel.setText("⚽ " + leagueName);

        binding.cardNewClub.setOnClickListener(v -> {
            Bundle out = new Bundle();
            out.putInt("leagueId", leagueId);
            out.putString("leagueName", leagueName);
            Navigation.findNavController(view)
                .navigate(R.id.action_clubMode_to_clubSetup, out);
        });

        binding.cardExistingClub.setOnClickListener(v -> {
            Bundle out = new Bundle();
            out.putInt("leagueId", leagueId);
            out.putString("leagueName", leagueName);
            Navigation.findNavController(view)
                .navigate(R.id.action_clubMode_to_existingClubPicker, out);
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
