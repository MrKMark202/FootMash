package hr.fipu.footmash.ui.ailab.create;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import hr.fipu.footmash.R;
import hr.fipu.footmash.databinding.FragmentPlayerStatsBinding;
import hr.fipu.footmash.databinding.ItemStatRowBinding;

/**
 * Step 2 of the player creation wizard: pick a position and distribute
 * {@link PlayerCreationViewModel#POINTS_TO_SPEND} points across six stats,
 * each starting from {@link PlayerCreationViewModel#STAT_BASELINE} and
 * capped at {@link PlayerCreationViewModel#STAT_CAP}.
 *
 * <p>The Next button only enables once the budget is fully spent
 * <em>and</em> a position is picked.
 */
public class PlayerStatsFragment extends Fragment {

    private FragmentPlayerStatsBinding binding;
    private PlayerCreationViewModel viewModel;

    private ItemStatRowBinding rowPace, rowShoot, rowPass, rowDrib, rowDef, rowPhys;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentPlayerStatsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireActivity()).get(PlayerCreationViewModel.class);

        bindStatRows();
        bindPositionDropdown();
        bindNextButton();
        refreshUi();
    }

    private void bindStatRows() {
        rowPace  = ItemStatRowBinding.bind(binding.rowPace.getRoot());
        rowShoot = ItemStatRowBinding.bind(binding.rowShooting.getRoot());
        rowPass  = ItemStatRowBinding.bind(binding.rowPassing.getRoot());
        rowDrib  = ItemStatRowBinding.bind(binding.rowDribbling.getRoot());
        rowDef   = ItemStatRowBinding.bind(binding.rowDefending.getRoot());
        rowPhys  = ItemStatRowBinding.bind(binding.rowPhysical.getRoot());

        wireRow(rowPace,  getString(R.string.pace),      PlayerCreationViewModel.IDX_PACE);
        wireRow(rowShoot, getString(R.string.shooting),  PlayerCreationViewModel.IDX_SHOOTING);
        wireRow(rowPass,  getString(R.string.passing),   PlayerCreationViewModel.IDX_PASSING);
        wireRow(rowDrib,  getString(R.string.dribbling), PlayerCreationViewModel.IDX_DRIBBLING);
        wireRow(rowDef,   getString(R.string.defending), PlayerCreationViewModel.IDX_DEFENDING);
        wireRow(rowPhys,  getString(R.string.physical),  PlayerCreationViewModel.IDX_PHYSICAL);
    }

    private void wireRow(ItemStatRowBinding row, String label, int index) {
        row.textStatLabel.setText(label);
        row.btnStatMinus.setOnClickListener(v -> { viewModel.bumpStat(index, -1); refreshUi(); });
        row.btnStatPlus .setOnClickListener(v -> { viewModel.bumpStat(index, +1); refreshUi(); });
    }

    private void bindPositionDropdown() {
        String[] positions = getResources().getStringArray(R.array.seed_positions);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
            requireContext(), android.R.layout.simple_list_item_1, positions);
        binding.autoCompletePosition.setAdapter(adapter);

        String current = viewModel.getPosition().getValue();
        if (current != null && !current.isEmpty()) {
            binding.autoCompletePosition.setText(current, false);
        }

        binding.autoCompletePosition.setOnItemClickListener((parent, v, pos, id) -> {
            viewModel.setPosition((String) parent.getItemAtPosition(pos));
            refreshUi();
        });
    }

    private void bindNextButton() {
        // Navigation to step 3 is wired in the next commit when the destination exists.
        binding.btnNext.setOnClickListener(v -> { /* wired in commit 3 */ });
    }

    private void refreshUi() {
        rowPace.textStatValue .setText(String.valueOf(viewModel.statValueAt(PlayerCreationViewModel.IDX_PACE)));
        rowShoot.textStatValue.setText(String.valueOf(viewModel.statValueAt(PlayerCreationViewModel.IDX_SHOOTING)));
        rowPass.textStatValue .setText(String.valueOf(viewModel.statValueAt(PlayerCreationViewModel.IDX_PASSING)));
        rowDrib.textStatValue .setText(String.valueOf(viewModel.statValueAt(PlayerCreationViewModel.IDX_DRIBBLING)));
        rowDef.textStatValue  .setText(String.valueOf(viewModel.statValueAt(PlayerCreationViewModel.IDX_DEFENDING)));
        rowPhys.textStatValue .setText(String.valueOf(viewModel.statValueAt(PlayerCreationViewModel.IDX_PHYSICAL)));

        int remaining = viewModel.pointsRemaining();
        binding.textPointsRemaining.setText(String.valueOf(remaining));
        binding.textOverall.setText(String.valueOf(viewModel.currentOverall()));

        binding.btnNext.setEnabled(viewModel.isStep2Valid());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
        rowPace = rowShoot = rowPass = rowDrib = rowDef = rowPhys = null;
    }
}
