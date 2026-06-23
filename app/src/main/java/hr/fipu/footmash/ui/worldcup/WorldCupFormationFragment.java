package hr.fipu.footmash.ui.worldcup;

import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import hr.fipu.footmash.R;
import hr.fipu.footmash.databinding.FragmentWcFormationBinding;
import hr.fipu.footmash.model.FormationSlot;
import hr.fipu.footmash.season.FormationCatalog;
import hr.fipu.footmash.worldcup.NationSquadBuilder;
import hr.fipu.footmash.worldcup.WcPlayer;
import hr.fipu.footmash.worldcup.WcTournament;

/**
 * Mid-tournament team editor built on the same pitch-slot + bench model as the
 * league draft canvas: pick a formation from the chips, tap a pitch slot to fill
 * it from the bench (or send a starter back to the bench), and tap a bench player
 * to promote them. Saving persists the formation and the starting XI in slot order.
 */
public class WorldCupFormationFragment extends Fragment {

    private FragmentWcFormationBinding binding;
    private WorldCupViewModel viewModel;

    private final Map<String, TextView> formationChips = new HashMap<>();
    private final Map<String, View> slotViews = new HashMap<>();

    private String formation = FormationCatalog.DEFAULT;
    private List<WcPlayer> squad = new ArrayList<>();
    /** slotKey → assigned starter. Insertion order follows the formation's slots. */
    private final Map<String, WcPlayer> assignments = new LinkedHashMap<>();
    private boolean pitchReady = false;
    private boolean initialised = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentWcFormationBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(WorldCupViewModel.class);

        buildFormationChips();
        binding.buttonSave.setOnClickListener(v -> save());

        viewModel.getState().observe(getViewLifecycleOwner(), this::onState);
        viewModel.refresh();

        binding.pitchContainer.getViewTreeObserver().addOnGlobalLayoutListener(
            new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    if (!pitchReady && binding != null && binding.pitchContainer.getWidth() > 0) {
                        pitchReady = true;
                        binding.pitchContainer.getViewTreeObserver()
                            .removeOnGlobalLayoutListener(this);
                        renderPitch();
                    }
                }
            });
    }

    /** Seed the editor from saved state once, then render. */
    private void onState(@Nullable WcTournament t) {
        if (t == null || initialised) return;
        initialised = true;
        squad = t.squad != null ? new ArrayList<>(t.squad) : new ArrayList<>();
        formation = t.formation != null ? t.formation : FormationCatalog.DEFAULT;

        List<WcPlayer> xi = new ArrayList<>();
        if (t.startingXiIds != null) {
            Map<Integer, WcPlayer> byId = new HashMap<>();
            for (WcPlayer p : squad) byId.put(p.id, p);
            for (Integer id : t.startingXiIds) {
                WcPlayer p = byId.get(id);
                if (p != null) xi.add(p);
            }
        }
        if (xi.size() < FormationCatalog.get(formation).size()) {
            xi = NationSquadBuilder.autoXi(squad, formation);
        }
        assignXi(xi);

        highlightFormationChip(formation);
        renderPitch();
        renderBench();
        updateStatus();
    }

    // ─── Formation chips ───────────────────────────────────────────────────────

    private void buildFormationChips() {
        binding.formationChips.removeAllViews();
        formationChips.clear();
        for (String name : FormationCatalog.names()) {
            TextView chip = new TextView(requireContext());
            chip.setText(name);
            chip.setTextSize(15f);
            chip.setTypeface(chip.getTypeface(), Typeface.BOLD);
            int padH = dp(14), padV = dp(7);
            chip.setPadding(padH, padV, padH, padV);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMarginEnd(dp(8));
            chip.setLayoutParams(lp);
            chip.setOnClickListener(v -> changeFormation(name));
            binding.formationChips.addView(chip);
            formationChips.put(name, chip);
        }
    }

    private void highlightFormationChip(String active) {
        for (Map.Entry<String, TextView> e : formationChips.entrySet()) {
            boolean on = e.getKey().equals(active);
            e.getValue().setBackgroundResource(
                on ? R.drawable.bg_pill_selected : R.drawable.bg_pill_unselected);
            e.getValue().setTextColor(ContextCompat.getColor(requireContext(),
                on ? R.color.text_on_primary : R.color.text_secondary));
        }
    }

    private void changeFormation(String name) {
        if (name.equals(formation)) return;
        formation = name;
        // Keep the current starters; re-slot them into the new shape (filling any
        // shortfall from the bench).
        List<WcPlayer> starters = new ArrayList<>(assignments.values());
        List<WcPlayer> xi = NationSquadBuilder.autoXi(
            starters.size() >= FormationCatalog.get(formation).size() ? starters : squad, formation);
        assignXi(xi);
        highlightFormationChip(formation);
        renderPitch();
        renderBench();
        updateStatus();
    }

    /** Maps an ordered XI list onto the current formation's slots. */
    private void assignXi(List<WcPlayer> xi) {
        assignments.clear();
        List<FormationSlot> slots = FormationCatalog.get(formation);
        for (int i = 0; i < slots.size() && i < xi.size(); i++) {
            assignments.put(slots.get(i).key, xi.get(i));
        }
    }

    // ─── Pitch ─────────────────────────────────────────────────────────────────

    private void renderPitch() {
        if (!pitchReady || binding == null) return;
        binding.pitchContainer.removeAllViews();
        slotViews.clear();

        List<FormationSlot> slots = FormationCatalog.get(formation);
        int pitchW = binding.pitchContainer.getWidth();
        int pitchH = binding.pitchContainer.getHeight();
        int slotW = dp(74), slotH = dp(64);

        for (FormationSlot slot : slots) {
            View slotView = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_pitch_slot, binding.pitchContainer, false);
            updateSlotView(slotView, slot, assignments.get(slot.key));

            int left = (int) (slot.xPct * pitchW) - slotW / 2;
            int top = (int) (slot.yPct * pitchH) - slotH / 2;
            left = Math.max(4, Math.min(pitchW - slotW - 4, left));
            top = Math.max(4, Math.min(pitchH - slotH - 8, top));

            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(slotW, slotH);
            lp.leftMargin = left;
            lp.topMargin = top;
            binding.pitchContainer.addView(slotView, lp);
            slotViews.put(slot.key, slotView);
            slotView.setOnClickListener(v -> onSlotClicked(slot));
        }
    }

    private void updateSlotView(View slotView, FormationSlot slot, WcPlayer player) {
        TextView tvName = slotView.findViewById(R.id.text_slot_name);
        TextView tvExtra = slotView.findViewById(R.id.text_slot_extra);
        if (player != null) {
            tvName.setText(lastName(player.name));
            tvExtra.setText(String.valueOf(player.overall));
            slotView.setBackgroundResource(R.drawable.bg_pitch_slot_filled);
            slotView.setBackgroundTintList(ColorStateList.valueOf(
                ContextCompat.getColor(requireContext(), R.color.accent_blue)));
            tvName.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_on_primary));
            tvExtra.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_on_primary));
        } else {
            tvName.setText(slot.label);
            tvExtra.setText("+");
            slotView.setBackgroundResource(R.drawable.bg_pitch_slot_empty);
            slotView.setBackgroundTintList(null);
            tvName.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
            tvExtra.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
        }
    }

    // ─── Bench ─────────────────────────────────────────────────────────────────

    private List<WcPlayer> bench() {
        List<WcPlayer> out = new ArrayList<>();
        for (WcPlayer p : squad) if (!isStarter(p)) out.add(p);
        out.sort((a, b) -> Integer.compare(b.overall, a.overall));
        return out;
    }

    private boolean isStarter(WcPlayer p) {
        for (WcPlayer s : assignments.values()) if (s != null && s.id == p.id) return true;
        return false;
    }

    private void renderBench() {
        if (binding == null) return;
        binding.benchContainer.removeAllViews();
        List<WcPlayer> bench = bench();
        binding.textBenchEmpty.setVisibility(bench.isEmpty() ? View.VISIBLE : View.GONE);
        for (WcPlayer p : bench) {
            View chip = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_pitch_slot, binding.benchContainer, false);
            TextView tvName = chip.findViewById(R.id.text_slot_name);
            TextView tvExtra = chip.findViewById(R.id.text_slot_extra);
            tvName.setText(lastName(p.name));
            tvName.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_on_primary));
            tvExtra.setText(p.position + " · " + p.overall);
            tvExtra.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_on_primary));
            chip.setBackgroundResource(R.drawable.bg_pitch_slot_filled);
            chip.setBackgroundTintList(ColorStateList.valueOf(
                ContextCompat.getColor(requireContext(), R.color.surface)));

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(74), dp(64));
            lp.setMarginEnd(dp(6));
            chip.setLayoutParams(lp);
            chip.setOnClickListener(v -> onBenchClicked(p));
            binding.benchContainer.addView(chip);
        }
    }

    // ─── Interactions ──────────────────────────────────────────────────────────

    private void onSlotClicked(FormationSlot slot) {
        WcPlayer current = assignments.get(slot.key);
        if (current != null) {
            new MaterialAlertDialogBuilder(requireContext(), R.style.FootMashDialog)
                .setTitle(current.name + "  ·  " + current.position + "  ·  " + current.overall)
                .setPositiveButton(R.string.wc_to_bench, (d, w) -> {
                    assignments.remove(slot.key);
                    refreshAll();
                })
                .setNegativeButton(R.string.wc_close, null)
                .show();
            return;
        }
        // Empty slot: offer eligible bench players (by position), then the rest.
        List<WcPlayer> options = new ArrayList<>();
        for (WcPlayer p : bench()) {
            if (FormationCatalog.matchesPosGroup(p.position, slot.posGroup)) options.add(p);
        }
        for (WcPlayer p : bench()) {
            if (!options.contains(p)) options.add(p);
        }
        if (options.isEmpty()) {
            Toast.makeText(requireContext(), R.string.wc_no_bench_for_slot, Toast.LENGTH_SHORT).show();
            return;
        }
        String[] labels = labels(options);
        new MaterialAlertDialogBuilder(requireContext(), R.style.FootMashDialog)
            .setTitle(getString(R.string.wc_fill_slot) + ": " + slot.label)
            .setItems(labels, (d, w) -> {
                assignments.put(slot.key, options.get(w));
                refreshAll();
            })
            .setNegativeButton(R.string.wc_close, null)
            .show();
    }

    private void onBenchClicked(WcPlayer p) {
        List<FormationSlot> empties = new ArrayList<>();
        List<FormationSlot> slots = FormationCatalog.get(formation);
        for (FormationSlot s : slots) {
            if (assignments.get(s.key) == null
                && FormationCatalog.matchesPosGroup(p.position, s.posGroup)) {
                empties.add(s);
            }
        }
        // No natural slot free → allow swapping into any empty slot, else any slot.
        if (empties.isEmpty()) {
            for (FormationSlot s : slots) if (assignments.get(s.key) == null) empties.add(s);
        }
        if (empties.isEmpty()) {
            // XI is full: let the user pick which starter to replace.
            replaceStarter(p, slots);
            return;
        }
        if (empties.size() == 1) {
            assignments.put(empties.get(0).key, p);
            refreshAll();
            return;
        }
        String[] labels = new String[empties.size()];
        for (int i = 0; i < empties.size(); i++) labels[i] = "Uvrsti: " + empties.get(i).label;
        new MaterialAlertDialogBuilder(requireContext(), R.style.FootMashDialog)
            .setTitle(p.name)
            .setItems(labels, (d, w) -> {
                assignments.put(empties.get(w).key, p);
                refreshAll();
            })
            .setNegativeButton(R.string.wc_close, null)
            .show();
    }

    private void replaceStarter(WcPlayer incoming, List<FormationSlot> slots) {
        String[] labels = new String[slots.size()];
        for (int i = 0; i < slots.size(); i++) {
            WcPlayer cur = assignments.get(slots.get(i).key);
            labels[i] = slots.get(i).label + ": " + (cur != null ? lastName(cur.name) : "—");
        }
        new MaterialAlertDialogBuilder(requireContext(), R.style.FootMashDialog)
            .setTitle(getString(R.string.wc_to_xi) + ": " + incoming.name)
            .setItems(labels, (d, w) -> {
                assignments.put(slots.get(w).key, incoming);
                refreshAll();
            })
            .setNegativeButton(R.string.wc_close, null)
            .show();
    }

    private void refreshAll() {
        renderPitch();
        renderBench();
        updateStatus();
    }

    private void updateStatus() {
        int needed = FormationCatalog.get(formation).size();
        int filled = assignments.size();
        binding.textLineupStatus.setText(filled + "/" + needed + " · klupa " + bench().size());
        boolean complete = filled == needed;
        binding.buttonSave.setEnabled(complete);
        binding.buttonSave.setAlpha(complete ? 1f : 0.5f);
    }

    private void save() {
        List<FormationSlot> slots = FormationCatalog.get(formation);
        List<Integer> ids = new ArrayList<>();
        for (FormationSlot s : slots) {
            WcPlayer p = assignments.get(s.key);
            if (p == null) return;
            ids.add(p.id);
        }
        viewModel.saveTeam(formation, ids);
        Toast.makeText(requireContext(), R.string.wc_save_team, Toast.LENGTH_SHORT).show();
        Navigation.findNavController(requireView()).navigateUp();
    }

    private String[] labels(List<WcPlayer> players) {
        String[] out = new String[players.size()];
        for (int i = 0; i < players.size(); i++) {
            WcPlayer p = players.get(i);
            out[i] = p.name + "  ·  " + p.overall + " OVR  ·  " + p.position;
        }
        return out;
    }

    private static String lastName(String name) {
        if (name == null) return "";
        String[] parts = name.trim().split("\\s+");
        return parts[parts.length - 1];
    }

    private int dp(int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp,
            getResources().getDisplayMetrics());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
