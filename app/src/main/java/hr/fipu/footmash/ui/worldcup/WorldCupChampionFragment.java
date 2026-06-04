package hr.fipu.footmash.ui.worldcup;

import android.os.Bundle;
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
import hr.fipu.footmash.databinding.FragmentWcChampionBinding;
import hr.fipu.footmash.db.LogoAssets;
import hr.fipu.footmash.ui.util.InitialsBadgeDrawable;
import hr.fipu.footmash.worldcup.WcTournament;
import hr.fipu.footmash.worldcup.WorldCupData;

/** Celebrates the winner and lets the player start a fresh tournament. */
public class WorldCupChampionFragment extends Fragment {

    private FragmentWcChampionBinding binding;
    private WorldCupViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentWcChampionBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(WorldCupViewModel.class);
        viewModel.getState().observe(getViewLifecycleOwner(), this::bind);
        viewModel.refresh();

        binding.buttonNewTournament.setOnClickListener(v -> {
            viewModel.reset();
            Navigation.findNavController(v).navigate(R.id.action_champion_to_worldCup);
        });
    }

    private void bind(@Nullable WcTournament t) {
        if (t == null || t.championKey == null) return;
        WorldCupData.Nation champ = WorldCupData.byKey(t.championKey);
        binding.textChampion.setText(name(t.championKey));
        Glide.with(this)
            .load(LogoAssets.assetUri(champ != null ? champ.logo : ""))
            .placeholder(new InitialsBadgeDrawable(name(t.championKey)))
            .into(binding.imageChampion);

        StringBuilder sb = new StringBuilder();
        sb.append("🥈 ").append(name(t.runnerUpKey)).append("\n");
        sb.append("🥉 ").append(name(t.thirdKey));
        binding.textPodium.setText(sb.toString());
    }

    private static String name(String key) {
        WorldCupData.Nation n = WorldCupData.byKey(key);
        return n != null ? n.name : (key == null ? "—" : key);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
