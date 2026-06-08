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
import androidx.navigation.Navigation;

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

    /** Drives press-and-hold auto-repeat on the +/- buttons. */
    private final android.os.Handler repeatHandler =
        new android.os.Handler(android.os.Looper.getMainLooper());

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
        attachRepeat(row.btnStatMinus, index, -1);
        attachRepeat(row.btnStatPlus,  index, +1);
    }

    /**
     * Makes a +/- button repeat while held, accelerating from ~3/s up to ~30/s so
     * a full 100-point budget can be poured into a stat in about a second instead
     * of 100 taps. A quick tap still nudges by one (and keeps TalkBack working).
     */
    @android.annotation.SuppressLint("ClickableViewAccessibility")
    private void attachRepeat(View button, int index, int delta) {
        button.setOnClickListener(v -> { viewModel.bumpStat(index, delta); refreshUi(); });
        button.setOnTouchListener(new View.OnTouchListener() {
            boolean held;
            long interval;
            final Runnable repeater = new Runnable() {
                @Override public void run() {
                    held = true;
                    viewModel.bumpStat(index, delta);
                    refreshUi();
                    interval = Math.max(30, interval - 35);   // accelerate
                    repeatHandler.postDelayed(this, interval);
                }
            };
            @Override public boolean onTouch(View v, android.view.MotionEvent e) {
                switch (e.getActionMasked()) {
                    case android.view.MotionEvent.ACTION_DOWN:
                        held = false;
                        interval = 300;
                        v.setPressed(true);
                        repeatHandler.postDelayed(repeater, 350);  // hold threshold
                        return true;
                    case android.view.MotionEvent.ACTION_UP:
                        v.setPressed(false);
                        repeatHandler.removeCallbacks(repeater);
                        if (!held) v.performClick();               // it was a tap
                        return true;
                    case android.view.MotionEvent.ACTION_CANCEL:
                        v.setPressed(false);
                        repeatHandler.removeCallbacks(repeater);
                        return true;
                    default:
                        return false;
                }
            }
        });
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
        binding.btnNext.setOnClickListener(v -> {
            if (!viewModel.isStep2Valid()) return;
            Navigation.findNavController(v)
                .navigate(R.id.action_playerStats_to_playerPreview);
        });
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
        repeatHandler.removeCallbacksAndMessages(null);
        binding = null;
        rowPace = rowShoot = rowPass = rowDrib = rowDef = rowPhys = null;
    }
}
