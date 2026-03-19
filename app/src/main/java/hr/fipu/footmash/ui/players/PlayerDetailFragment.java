package hr.fipu.footmash.ui.players;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.google.gson.Gson;

import hr.fipu.footmash.R;
import hr.fipu.footmash.databinding.FragmentPlayerDetailBinding;
import hr.fipu.footmash.model.PlayerResponse;

public class PlayerDetailFragment extends Fragment {

    private FragmentPlayerDetailBinding binding;
    private PlayerResponse playerResponse;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentPlayerDetailBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            String json = getArguments().getString("playerData", "");
            if (!json.isEmpty()) {
                playerResponse = new Gson().fromJson(json, PlayerResponse.class);
            }
        }

        binding.toolbar.setTitle(R.string.player_details);
        binding.toolbar.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material);
        binding.toolbar.setNavigationOnClickListener(v -> Navigation.findNavController(v).navigateUp());

        populateUI();
    }

    private void populateUI() {
        if (playerResponse == null) return;

        binding.textPlayerName.setText(playerResponse.getPlayerName());
        binding.textNationality.setText(playerResponse.getPlayerCountry());
        binding.textAge.setText(playerResponse.getPlayerAge() != null ? playerResponse.getPlayerAge() : "N/A");
        binding.textHeight.setText(playerResponse.getPlayerNumber() != null ? "Broj: " + playerResponse.getPlayerNumber() : "N/A");
        binding.textWeight.setText("-");

        Glide.with(this)
                .load(playerResponse.getPlayerImage())
                .transform(new CircleCrop())
                .placeholder(R.drawable.ic_launcher_background)
                .into(binding.imagePlayerPhoto);

        binding.textClubInfo.setText(""); // This info might be missing in simple player response
        binding.textPosition.setText(playerResponse.getPlayerType() != null ? playerResponse.getPlayerType() : "N/A");
        binding.textAppearances.setText(playerResponse.getPlayerMatchPlayed() != null ? playerResponse.getPlayerMatchPlayed() : "0");
        binding.textMinutes.setText("-");
        binding.textRating.setText("-");

        binding.textGoals.setText(playerResponse.getPlayerGoals() != null ? playerResponse.getPlayerGoals() : "0");
        binding.textAssists.setText("-");

        binding.textYellow.setText(playerResponse.getPlayerYellowCards() != null ? playerResponse.getPlayerYellowCards() : "0");
        binding.textRed.setText(playerResponse.getPlayerRedCards() != null ? playerResponse.getPlayerRedCards() : "0");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
