package hr.fipu.footmash.ui.ailab.create;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.chip.Chip;

import java.util.List;

import hr.fipu.footmash.R;
import hr.fipu.footmash.databinding.FragmentPlayerPreviewBinding;
import hr.fipu.footmash.databinding.ItemStatReadoutBinding;
import hr.fipu.footmash.model.Trait;

/**
 * Step 3 of the player creation wizard: shows the fully-allocated player as
 * a card (name, position, nationality, OVR, 6 stats) and lets the user pick
 * up to {@link PlayerCreationViewModel#MAX_TRAITS} traits filtered by the
 * position group.
 *
 * <p>The Next button enables once at least one trait is selected — zero
 * traits feels like an oversight rather than a deliberate choice.
 */
public class PlayerPreviewFragment extends Fragment {

    private FragmentPlayerPreviewBinding binding;
    private PlayerCreationViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentPlayerPreviewBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireActivity()).get(PlayerCreationViewModel.class);

        renderPlayerCard();
        renderStatGrid();
        buildTraitChips();
        refreshTraitCounter();

        binding.btnNext.setOnClickListener(v -> {
            if (!viewModel.isStep3Valid()) return;
            // Step 4 navigation is wired in the next commit.
        });
    }

    private void renderPlayerCard() {
        binding.textPlayerName.setText(viewModel.fullName());
        binding.textPlayerMeta.setText(
            viewModel.getPosition().getValue() + " • "
            + viewModel.getNationality().getValue());
        binding.textOverall.setText(String.valueOf(viewModel.currentOverall()));
    }

    private void renderStatGrid() {
        labelStat(binding.statPace,      getString(R.string.pace),      PlayerCreationViewModel.IDX_PACE);
        labelStat(binding.statShooting,  getString(R.string.shooting),  PlayerCreationViewModel.IDX_SHOOTING);
        labelStat(binding.statPassing,   getString(R.string.passing),   PlayerCreationViewModel.IDX_PASSING);
        labelStat(binding.statDribbling, getString(R.string.dribbling), PlayerCreationViewModel.IDX_DRIBBLING);
        labelStat(binding.statDefending, getString(R.string.defending), PlayerCreationViewModel.IDX_DEFENDING);
        labelStat(binding.statPhysical,  getString(R.string.physical),  PlayerCreationViewModel.IDX_PHYSICAL);
    }

    private void labelStat(ItemStatReadoutBinding row, String label, int index) {
        row.textStatLabel.setText(label);
        row.textStatValue.setText(String.valueOf(viewModel.statValueAt(index)));
    }

    private void buildTraitChips() {
        binding.chipGroupTraits.removeAllViews();
        List<Trait> eligible = viewModel.eligibleTraits();
        for (Trait t : eligible) {
            Chip chip = new Chip(requireContext());
            chip.setText(t.label);
            chip.setCheckable(true);
            chip.setChecked(viewModel.isTraitSelected(t));
            chip.setOnClickListener(v -> onChipClicked(chip, t));
            binding.chipGroupTraits.addView(chip);
        }
    }

    private void onChipClicked(Chip chip, Trait trait) {
        boolean wasSelected = viewModel.isTraitSelected(trait);
        // The chip's check state has already flipped — viewModel.toggleTrait
        // refuses to add past the cap, so we may need to snap it back.
        if (!wasSelected && viewModel.traitsSelectedCount() >= PlayerCreationViewModel.MAX_TRAITS) {
            chip.setChecked(false);
            Toast.makeText(requireContext(),
                "Maksimalno " + PlayerCreationViewModel.MAX_TRAITS + " stilova",
                Toast.LENGTH_SHORT).show();
            return;
        }
        viewModel.toggleTrait(trait);
        refreshTraitCounter();
    }

    private void refreshTraitCounter() {
        int n = viewModel.traitsSelectedCount();
        binding.textTraitsCounter.setText("Odabrano: " + n + " / " + PlayerCreationViewModel.MAX_TRAITS);
        binding.btnNext.setEnabled(viewModel.isStep3Valid());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
