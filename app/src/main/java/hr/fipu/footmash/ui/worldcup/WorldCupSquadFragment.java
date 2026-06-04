package hr.fipu.footmash.ui.worldcup;

import android.graphics.Typeface;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
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

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import hr.fipu.footmash.R;
import hr.fipu.footmash.databinding.FragmentWcSquadBinding;
import hr.fipu.footmash.db.LogoAssets;
import hr.fipu.footmash.model.FormationSlot;
import hr.fipu.footmash.season.FormationCatalog;
import hr.fipu.footmash.season.TraitEngine;
import hr.fipu.footmash.ui.util.InitialsBadgeDrawable;
import hr.fipu.footmash.worldcup.NationSquadBuilder;
import hr.fipu.footmash.worldcup.WcPlayer;
import hr.fipu.footmash.worldcup.WorldCupData;

/**
 * Builds the user's 23-man national squad from real (and filler) players, then
 * starts the tournament. The starting XI is auto-derived from the selected squad
 * and chosen formation and previewed on a pitch.
 */
public class WorldCupSquadFragment extends Fragment {

    private static final int SQUAD_SIZE = 23;

    private FragmentWcSquadBinding binding;
    private WorldCupViewModel viewModel;

    private String nationKey;
    private String formation = FormationCatalog.DEFAULT;
    private List<WcPlayer> pool = new ArrayList<>();
    private final Set<Integer> selectedIds = new LinkedHashSet<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentWcSquadBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(WorldCupViewModel.class);

        Bundle args = getArguments();
        nationKey = args != null ? args.getString("nationKey") : null;
        WorldCupData.Nation nation = WorldCupData.byKey(nationKey);
        if (nation == null) { requireActivity().onBackPressed(); return; }

        binding.textNation.setText(nation.name);
        Glide.with(this)
            .load(LogoAssets.assetUri(nation.logo))
            .placeholder(new InitialsBadgeDrawable(nation.name))
            .into(binding.imageNation);

        setupFormationSpinner();

        binding.buttonConfirm.setOnClickListener(v -> confirm());

        viewModel.getPool().observe(getViewLifecycleOwner(), this::onPool);
        viewModel.getBusy().observe(getViewLifecycleOwner(), busy ->
            binding.buttonConfirm.setEnabled(!Boolean.TRUE.equals(busy) && selectedIds.size() == SQUAD_SIZE));
        viewModel.loadPool(nationKey);
    }

    private void setupFormationSpinner() {
        List<String> names = FormationCatalog.names();
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
            android.R.layout.simple_spinner_dropdown_item, names);
        binding.spinnerFormation.setAdapter(adapter);
        int def = names.indexOf(FormationCatalog.DEFAULT);
        if (def >= 0) binding.spinnerFormation.setSelection(def);
        binding.spinnerFormation.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View v, int position, long id) {
                formation = (String) parent.getItemAtPosition(position);
                refreshPitch();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void onPool(List<WcPlayer> loaded) {
        pool = loaded != null ? loaded : new ArrayList<>();
        selectedIds.clear();
        for (WcPlayer p : defaultSquad(pool)) selectedIds.add(p.id);
        buildRows();
        updateCounter();
        refreshPitch();
    }

    /** A sensible starting selection: three keepers plus the best 20 outfielders. */
    private List<WcPlayer> defaultSquad(List<WcPlayer> pool) {
        List<WcPlayer> sorted = new ArrayList<>(pool);
        sorted.sort((a, b) -> Integer.compare(b.overall, a.overall));
        List<WcPlayer> out = new ArrayList<>();
        int gk = 0;
        for (WcPlayer p : sorted) {
            if ("GK".equals(TraitEngine.groupOf(p.position)) && gk < 3) { out.add(p); gk++; }
        }
        for (WcPlayer p : sorted) {
            if (out.size() >= SQUAD_SIZE) break;
            if (!out.contains(p)) out.add(p);
        }
        return out;
    }

    private void buildRows() {
        binding.containerPlayers.removeAllViews();
        List<WcPlayer> sorted = new ArrayList<>(pool);
        sorted.sort((a, b) -> Integer.compare(b.overall, a.overall));
        for (WcPlayer p : sorted) {
            binding.containerPlayers.addView(buildRow(p));
        }
    }

    private View buildRow(WcPlayer p) {
        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        int padH = dp(12), padV = dp(10);
        row.setPadding(padH, padV, padH, padV);
        LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rlp.bottomMargin = dp(6);
        row.setLayoutParams(rlp);

        TextView pos = new TextView(requireContext());
        pos.setText(p.position);
        pos.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_on_primary));
        pos.setTextSize(11f);
        pos.setTypeface(pos.getTypeface(), Typeface.BOLD);
        pos.setGravity(Gravity.CENTER);
        pos.setBackgroundResource(R.drawable.bg_pill_selected);
        pos.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(),
            posColor(p.position)));
        pos.setMinWidth(dp(42));
        pos.setPadding(dp(8), dp(3), dp(8), dp(3));
        row.addView(pos);

        TextView name = new TextView(requireContext());
        String label = p.name + (p.filler ? "  ✎" : "");
        name.setText(label);
        name.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary));
        name.setTextSize(15f);
        LinearLayout.LayoutParams nlp = new LinearLayout.LayoutParams(
            0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        nlp.leftMargin = dp(12);
        name.setLayoutParams(nlp);
        row.addView(name);

        TextView ovr = new TextView(requireContext());
        ovr.setText(String.valueOf(p.overall));
        ovr.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary));
        ovr.setTextSize(15f);
        ovr.setTypeface(ovr.getTypeface(), Typeface.BOLD);
        row.addView(ovr);

        applyRowState(row, selectedIds.contains(p.id));
        row.setOnClickListener(v -> toggle(p, row));
        return row;
    }

    private void toggle(WcPlayer p, View row) {
        if (selectedIds.contains(p.id)) {
            selectedIds.remove(p.id);
            applyRowState(row, false);
        } else {
            if (selectedIds.size() >= SQUAD_SIZE) {
                Toast.makeText(requireContext(),
                    "Maksimalno " + SQUAD_SIZE + " igrača", Toast.LENGTH_SHORT).show();
                return;
            }
            selectedIds.add(p.id);
            applyRowState(row, true);
        }
        updateCounter();
        refreshPitch();
    }

    private void applyRowState(View row, boolean selected) {
        row.setBackgroundResource(selected ? R.drawable.bg_pill_selected
                                            : R.drawable.bg_pill_unselected);
    }

    private void updateCounter() {
        binding.textCounter.setText(getString(R.string.wc_selected)
            + " " + selectedIds.size() + "/" + SQUAD_SIZE);
        boolean ready = selectedIds.size() == SQUAD_SIZE;
        binding.buttonConfirm.setEnabled(ready);
    }

    private List<WcPlayer> selectedPlayers() {
        List<WcPlayer> out = new ArrayList<>();
        for (WcPlayer p : pool) if (selectedIds.contains(p.id)) out.add(p);
        return out;
    }

    private void refreshPitch() {
        final List<WcPlayer> xi = NationSquadBuilder.autoXi(selectedPlayers(), formation);
        final List<FormationSlot> slots = FormationCatalog.get(formation);
        binding.pitch.post(() -> placeTokens(xi, slots));
    }

    private void placeTokens(List<WcPlayer> xi, List<FormationSlot> slots) {
        if (binding == null) return;
        FrameLayout pitch = binding.pitch;
        pitch.removeAllViews();
        int w = pitch.getWidth(), h = pitch.getHeight();
        if (w == 0 || h == 0) return;
        int token = dp(46);
        for (int i = 0; i < slots.size() && i < xi.size(); i++) {
            FormationSlot slot = slots.get(i);
            WcPlayer p = xi.get(i);
            TextView t = new TextView(requireContext());
            t.setText(shortName(p.name));
            t.setMaxLines(1);
            t.setGravity(Gravity.CENTER);
            t.setTextSize(9f);
            t.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_on_primary));
            t.setBackgroundResource(R.drawable.bg_pitch_slot_filled);
            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(token, token);
            lp.leftMargin = clamp((int) (slot.xPct * w - token / 2f), 0, w - token);
            lp.topMargin = clamp((int) (slot.yPct * h - token / 2f), 0, h - token);
            t.setLayoutParams(lp);
            pitch.addView(t);
        }
    }

    private void confirm() {
        if (selectedIds.size() != SQUAD_SIZE) return;
        List<WcPlayer> squad = selectedPlayers();
        List<WcPlayer> xi = NationSquadBuilder.autoXi(squad, formation);
        List<Integer> xiIds = new ArrayList<>();
        for (WcPlayer p : xi) xiIds.add(p.id);
        viewModel.start(nationKey, formation, squad, xiIds);
        Navigation.findNavController(requireView()).navigate(R.id.action_squad_to_hub);
    }

    private static String shortName(String name) {
        if (name == null) return "";
        String[] parts = name.trim().split("\\s+");
        return parts[parts.length - 1];
    }

    private static int posColor(String position) {
        switch (TraitEngine.groupOf(position)) {
            case "GK": return R.color.position_gk;
            case "DF": return R.color.position_df;
            case "FW": return R.color.position_fw;
            default:   return R.color.position_mf;
        }
    }

    private static int clamp(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
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
