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

        PlayerResponse.PlayerInfo p = playerResponse.getPlayer();
        if (p != null) {
            binding.textPlayerName.setText(p.getName());
            binding.textNationality.setText(p.getNationality());
            binding.textAge.setText(String.valueOf(p.getAge()));
            binding.textHeight.setText(p.getHeight() != null ? p.getHeight() : "N/A");
            binding.textWeight.setText(p.getWeight() != null ? p.getWeight() : "N/A");

            Glide.with(this)
                    .load(p.getPhoto())
                    .transform(new CircleCrop())
                    .placeholder(R.drawable.ic_launcher_background)
                    .into(binding.imagePlayerPhoto);
        }

        if (playerResponse.getStatistics() != null && !playerResponse.getStatistics().isEmpty()) {
            PlayerResponse.Statistics stat = playerResponse.getStatistics().get(0);
            
            // Postavi osnovni info o tekucem klubu i ligi 
            if (stat.getTeam() != null) {
                binding.textClubInfo.setText(stat.getTeam().getName());
            }

            // Statistika utakmica
            if (stat.getGames() != null) {
                binding.textPosition.setText(stat.getGames().getPosition() != null ? stat.getGames().getPosition() : "N/A");
                binding.textAppearances.setText(String.valueOf(stat.getGames().getAppearences()));
                binding.textMinutes.setText(stat.getGames().getMinutes() + " min");
                binding.textRating.setText(stat.getGames().getRating() != null ? stat.getGames().getRating() : "N/A");
            } else {
                binding.textPosition.setText("N/A");
                binding.textAppearances.setText("0");
                binding.textMinutes.setText("0 min");
                binding.textRating.setText("N/A");
            }

            // Ofenzivna statistika
            if (stat.getGoals() != null) {
                binding.textGoals.setText(String.valueOf(stat.getGoals().getTotal()));
                binding.textAssists.setText(String.valueOf(stat.getGoals().getAssists()));
            } else {
                binding.textGoals.setText("0");
                binding.textAssists.setText("0");
            }

            // Kartoni
            if (stat.getCards() != null) {
                binding.textYellow.setText(String.valueOf(stat.getCards().getYellow()));
                binding.textRed.setText(String.valueOf(stat.getCards().getRed()));
            } else {
                binding.textYellow.setText("0");
                binding.textRed.setText("0");
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
