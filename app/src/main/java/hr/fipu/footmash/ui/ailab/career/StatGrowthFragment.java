package hr.fipu.footmash.ui.ailab.career;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import hr.fipu.footmash.FootMashApp;
import hr.fipu.footmash.R;
import hr.fipu.footmash.databinding.FragmentStatGrowthBinding;
import hr.fipu.footmash.databinding.ItemStatRowBinding;
import hr.fipu.footmash.db.AppDatabase;
import hr.fipu.footmash.db.CustomPlayerDao;
import hr.fipu.footmash.model.CustomPlayer;

/**
 * Spend the player's {@code pointsToSpend} budget across the 6 attributes.
 *
 * <p>Rules (matching the creation wizard but applied to current values):
 * <ul>
 *     <li>Each stat is capped at 99 — the {@code +} button refuses further increments past it</li>
 *     <li>Cumulative spend cannot exceed {@code pointsToSpend}</li>
 *     <li>No respec — the {@code -} button only undoes additions made this session;
 *         it never drops a stat below the value it had on entry</li>
 * </ul>
 *
 * <p>Confirm persists the new stats, zeros {@code pointsToSpend}, and pops back to the hub.
 */
public class StatGrowthFragment extends Fragment {

    public static final String ARG_PLAYER_ID = "playerId";

    private static final int STAT_CAP = 99;
    private static final int STAT_COUNT = 6;

    private static final int IDX_PACE      = 0;
    private static final int IDX_SHOOTING  = 1;
    private static final int IDX_PASSING   = 2;
    private static final int IDX_DRIBBLING = 3;
    private static final int IDX_DEFENDING = 4;
    private static final int IDX_PHYSICAL  = 5;

    private FragmentStatGrowthBinding binding;
    private ItemStatRowBinding rowPace, rowShoot, rowPass, rowDrib, rowDef, rowPhys;

    private int playerId;
    private CustomPlayer player;
    /** Baseline values captured on load — the floor for the {@code -} button. */
    private final int[] baseline = new int[STAT_COUNT];
    /** Per-stat additions chosen during this session. */
    private final int[] additions = new int[STAT_COUNT];
    private int budget;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentStatGrowthBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        playerId = getArguments() != null ? getArguments().getInt(ARG_PLAYER_ID, 0) : 0;
        if (playerId <= 0) {
            Toast.makeText(requireContext(), "Nedostaje ID igrača", Toast.LENGTH_SHORT).show();
            Navigation.findNavController(view).popBackStack();
            return;
        }
        bindRows();
        loadPlayer();

        binding.btnConfirm.setOnClickListener(v -> confirm());
    }

    private void bindRows() {
        rowPace  = ItemStatRowBinding.bind(binding.rowPace.getRoot());
        rowShoot = ItemStatRowBinding.bind(binding.rowShooting.getRoot());
        rowPass  = ItemStatRowBinding.bind(binding.rowPassing.getRoot());
        rowDrib  = ItemStatRowBinding.bind(binding.rowDribbling.getRoot());
        rowDef   = ItemStatRowBinding.bind(binding.rowDefending.getRoot());
        rowPhys  = ItemStatRowBinding.bind(binding.rowPhysical.getRoot());

        wireRow(rowPace,  getString(R.string.pace),      IDX_PACE);
        wireRow(rowShoot, getString(R.string.shooting),  IDX_SHOOTING);
        wireRow(rowPass,  getString(R.string.passing),   IDX_PASSING);
        wireRow(rowDrib,  getString(R.string.dribbling), IDX_DRIBBLING);
        wireRow(rowDef,   getString(R.string.defending), IDX_DEFENDING);
        wireRow(rowPhys,  getString(R.string.physical),  IDX_PHYSICAL);
    }

    private void wireRow(ItemStatRowBinding row, String label, int index) {
        row.textStatLabel.setText(label);
        row.btnStatMinus.setOnClickListener(v -> { bump(index, -1); refresh(); });
        row.btnStatPlus .setOnClickListener(v -> { bump(index, +1); refresh(); });
    }

    private void loadPlayer() {
        AppDatabase db = FootMashApp.container(requireContext()).database();
        new Thread(() -> {
            CustomPlayer p = db.customPlayerDao().getPlayerByIdSync(playerId);
            if (p == null) return;
            if (!isAdded()) return;
            requireActivity().runOnUiThread(() -> {
                if (binding == null) return;
                player = p;
                baseline[IDX_PACE]      = p.getPace();
                baseline[IDX_SHOOTING]  = p.getShooting();
                baseline[IDX_PASSING]   = p.getPassing();
                baseline[IDX_DRIBBLING] = p.getDribbling();
                baseline[IDX_DEFENDING] = p.getDefending();
                baseline[IDX_PHYSICAL]  = p.getPhysical();
                budget = p.getPointsToSpend();
                refresh();
            });
        }).start();
    }

    /** Adjusts the addition for one stat; clamps to remaining budget + 99 cap + baseline floor. */
    private void bump(int index, int delta) {
        if (player == null) return;
        int next = additions[index] + delta;
        if (next < 0) next = 0;                                  // never below baseline
        if (baseline[index] + next > STAT_CAP) {
            next = STAT_CAP - baseline[index];                   // never above 99
        }
        int spent = totalSpent() - additions[index] + next;
        if (spent > budget) next = budget - (totalSpent() - additions[index]);
        additions[index] = Math.max(0, next);
    }

    private void refresh() {
        if (player == null || binding == null) return;
        rowPace.textStatValue .setText(String.valueOf(baseline[IDX_PACE]      + additions[IDX_PACE]));
        rowShoot.textStatValue.setText(String.valueOf(baseline[IDX_SHOOTING]  + additions[IDX_SHOOTING]));
        rowPass.textStatValue .setText(String.valueOf(baseline[IDX_PASSING]   + additions[IDX_PASSING]));
        rowDrib.textStatValue .setText(String.valueOf(baseline[IDX_DRIBBLING] + additions[IDX_DRIBBLING]));
        rowDef.textStatValue  .setText(String.valueOf(baseline[IDX_DEFENDING] + additions[IDX_DEFENDING]));
        rowPhys.textStatValue .setText(String.valueOf(baseline[IDX_PHYSICAL]  + additions[IDX_PHYSICAL]));

        binding.textPointsRemaining.setText(String.valueOf(budget - totalSpent()));
        binding.textOverall.setText(String.valueOf(currentOverall()));
        binding.btnConfirm.setEnabled(totalSpent() == budget); // must spend everything
    }

    private int totalSpent() {
        int t = 0;
        for (int v : additions) t += v;
        return t;
    }

    private int currentOverall() {
        int sum = 0;
        for (int i = 0; i < STAT_COUNT; i++) sum += baseline[i] + additions[i];
        return sum / STAT_COUNT;
    }

    private void confirm() {
        if (player == null) return;
        if (totalSpent() != budget) {
            Toast.makeText(requireContext(), "Potroši sve bodove prije potvrde",
                Toast.LENGTH_SHORT).show();
            return;
        }
        player.setPace      (baseline[IDX_PACE]      + additions[IDX_PACE]);
        player.setShooting  (baseline[IDX_SHOOTING]  + additions[IDX_SHOOTING]);
        player.setPassing   (baseline[IDX_PASSING]   + additions[IDX_PASSING]);
        player.setDribbling (baseline[IDX_DRIBBLING] + additions[IDX_DRIBBLING]);
        player.setDefending (baseline[IDX_DEFENDING] + additions[IDX_DEFENDING]);
        player.setPhysical  (baseline[IDX_PHYSICAL]  + additions[IDX_PHYSICAL]);
        player.setPointsToSpend(0);

        CustomPlayerDao dao = FootMashApp.container(requireContext()).database().customPlayerDao();
        new Thread(() -> {
            dao.update(player);
            if (!isAdded()) return;
            requireActivity().runOnUiThread(() ->
                Navigation.findNavController(requireView()).popBackStack());
        }).start();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
        rowPace = rowShoot = rowPass = rowDrib = rowDef = rowPhys = null;
    }
}
