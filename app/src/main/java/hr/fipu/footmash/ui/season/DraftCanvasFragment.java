package hr.fipu.footmash.ui.season;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import hr.fipu.footmash.R;
import hr.fipu.footmash.databinding.FragmentDraftCanvasBinding;
import hr.fipu.footmash.db.AppDatabase;
import hr.fipu.footmash.model.FormationSlot;
import hr.fipu.footmash.model.RealPlayer;
import hr.fipu.footmash.model.Trait;
import hr.fipu.footmash.model.SynergyResult;
import hr.fipu.footmash.model.UserClub;
import hr.fipu.footmash.season.FormationCatalog;
import hr.fipu.footmash.season.TraitEngine;
import hr.fipu.footmash.ui.util.ClubColors;

/**
 * The pitch canvas — used both for the initial squad draft and (in edit mode,
 * reached from the Season Hub) for mid-season squad management: changing
 * formation and moving players between the starting XI and the bench.
 */
public class DraftCanvasFragment extends Fragment {

    private FragmentDraftCanvasBinding binding;
    private DraftViewModel viewModel;
    private final Map<String, View> slotViews = new HashMap<>();
    private final Map<String, TextView> formationChips = new HashMap<>();
    private boolean pitchReady = false;
    private int clubId = -1;
    private boolean editMode = false;
    private SynergyResult lastSynergy;
    private ClubColors.Theme theme = ClubColors.DEFAULT;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentDraftCanvasBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        clubId = getArguments() != null ? getArguments().getInt("clubId", -1) : -1;
        editMode = getArguments() != null && getArguments().getBoolean("editMode", false);
        if (clubId == -1) {
            Toast.makeText(requireContext(), "Greška: klub nije pronađen", Toast.LENGTH_SHORT).show();
            return;
        }

        viewModel = new ViewModelProvider(requireActivity()).get(DraftViewModel.class);

        AppDatabase db = AppDatabase.getInstance(requireContext());
        new Thread(() -> {
            UserClub club = db.userClubDao().getClubByIdSync(clubId);
            if (club == null) return;
            DraftViewModel.DraftMode mode = club.getRealTeamSourceId() != null
                ? DraftViewModel.DraftMode.EXISTING_CLUB
                : DraftViewModel.DraftMode.NEW_CLUB;
            String name = club.getClubName();
            String formation = club.getFormation();
            theme = ClubColors.of(club.getRealTeamSourceId());

            requireActivity().runOnUiThread(() -> {
                if (binding == null) return;
                viewModel.init(clubId, club.getLeagueId(), mode, editMode);

                binding.textClubName.setText(name);
                binding.textClubName.setTextColor(theme.primary);
                binding.textFormationLabel.setText(formation + " · "
                    + (editMode ? "uređivanje sastava"
                       : mode == DraftViewModel.DraftMode.EXISTING_CLUB
                         ? "naslijeđeni klub" : "novi klub"));

                binding.textHelp.setText(editMode
                    ? "Tapni slot ili igrača da mijenjaš sastav. Formaciju biraš gore."
                    : "Tapni prazan slot za kupovinu. Tapni igrača za izmjene.");

                binding.btnStartSeason.setText(editMode ? "Spremi sastav" : "Pokreni sezonu");
                binding.btnAddBench.setVisibility(editMode ? View.GONE : View.VISIBLE);
                ClubColors.styleButton(binding.btnStartSeason, theme);
                binding.textSynergyMore.setTextColor(theme.primary);

                buildFormationChips();
                observeViewModel();
                binding.btnStartSeason.setOnClickListener(v -> onStartSeason(view));
                binding.btnAddBench.setOnClickListener(v -> onAddBench());
            });
        }).start();

        binding.pitchContainer.getViewTreeObserver().addOnGlobalLayoutListener(
            new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    if (!pitchReady && binding != null && binding.pitchContainer.getWidth() > 0) {
                        pitchReady = true;
                        binding.pitchContainer.getViewTreeObserver()
                            .removeOnGlobalLayoutListener(this);
                        renderPitchIfReady();
                    }
                }
            }
        );
    }

    // ─── Formation selector ────────────────────────────────────────────────────

    private void buildFormationChips() {
        binding.formationChips.removeAllViews();
        formationChips.clear();
        for (String name : FormationCatalog.names()) {
            TextView chip = new TextView(requireContext());
            chip.setText(name);
            chip.setTextSize(15f);
            chip.setTypeface(chip.getTypeface(), android.graphics.Typeface.BOLD);
            int padH = dpToPx(14), padV = dpToPx(7);
            chip.setPadding(padH, padV, padH, padV);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMarginEnd(dpToPx(8));
            chip.setLayoutParams(lp);
            chip.setOnClickListener(v -> {
                String currentFormation = viewModel.getFormation().getValue();
                if (!name.equals(currentFormation)) viewModel.changeFormation(name);
            });
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

    // ─── Observers ─────────────────────────────────────────────────────────────

    private void observeViewModel() {
        viewModel.getFormation().observe(getViewLifecycleOwner(), f -> {
            highlightFormationChip(f);
            renderPitchIfReady();
        });

        viewModel.getAssignments().observe(getViewLifecycleOwner(), assigned -> {
            if (!pitchReady || slotViews.isEmpty()) {
                renderPitchIfReady();
            } else {
                String f = viewModel.getFormation().getValue();
                if (f != null) {
                    for (FormationSlot slot : FormationCatalog.get(f)) {
                        View slotView = slotViews.get(slot.key);
                        if (slotView != null) {
                            updateSlotView(slotView, slot, assigned.get(slot.key));
                        }
                    }
                }
            }
            updateStartButton(assigned, viewModel.getRemainingBudget().getValue());
        });

        viewModel.getBench().observe(getViewLifecycleOwner(), this::renderBench);

        viewModel.getRemainingBudget().observe(getViewLifecycleOwner(), budget -> {
            double millions = budget / 1_000_000.0;
            binding.textBudget.setText(String.format("€%.1fM", millions));
            updateStartButton(viewModel.getAssignments().getValue(), budget);
        });

        viewModel.getSynergy().observe(getViewLifecycleOwner(), this::bindSynergy);
        binding.cardSynergy.setOnClickListener(v -> showSynergyDialog());
    }

    private void bindSynergy(SynergyResult s) {
        lastSynergy = s;
        if (s == null) return;
        binding.textSynergyValue.setText("⚡ Sinergija " + s.rating);
        String deltaText;
        int color;
        if (s.delta > 0) {
            deltaText = "+" + s.delta + " OVR · dobre kombinacije";
            color = ContextCompat.getColor(requireContext(), R.color.success);
        } else if (s.delta < 0) {
            deltaText = s.delta + " OVR · loše kombinacije";
            color = ContextCompat.getColor(requireContext(), R.color.error);
        } else {
            deltaText = "neutralno";
            color = ContextCompat.getColor(requireContext(), R.color.text_secondary);
        }
        binding.textSynergyDelta.setText(deltaText);
        binding.textSynergyDelta.setTextColor(color);
    }

    private void showSynergyDialog() {
        SynergyResult s = lastSynergy;
        if (s == null) return;
        View root = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_synergy, null, false);
        LinearLayout table = root.findViewById(R.id.synergy_table);

        int green = ContextCompat.getColor(requireContext(), R.color.success);
        int amber = ContextCompat.getColor(requireContext(), R.color.warning);
        if (!s.positives.isEmpty()) {
            addSynergyHeader(table, "DOBRE KOMBINACIJE", green);
            for (String line : s.positives) addSynergyRow(table, line, green);
        }
        if (!s.negatives.isEmpty()) {
            addSynergyHeader(table, "LOŠE KOMBINACIJE", amber);
            for (String line : s.negatives) addSynergyRow(table, line, amber);
        }
        if (s.positives.isEmpty() && s.negatives.isEmpty()) {
            TextView hint = new TextView(requireContext());
            hint.setText("Još nema aktivnih kombinacija. Posloži igrače s "
                + "osobinama koje se nadopunjuju.");
            hint.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
            hint.setTextSize(14f);
            table.addView(hint);
        }
        new MaterialAlertDialogBuilder(requireContext(), R.style.FootMashDialog)
            .setTitle("Sinergija sastava · " + s.rating + "/100  (" + signedDelta(s.delta) + " OVR)")
            .setView(root)
            .setPositiveButton("U redu", null)
            .show();
    }

    private void updateStartButton(Map<String, RealPlayer> assigned, Long budget) {
        String f = viewModel.getFormation().getValue();
        if (f == null) return;
        int needed = FormationCatalog.get(f).size();
        int filled = assigned != null ? assigned.size() : 0;
        binding.textLineupStatus.setText(filled + "/" + needed + " · klupa " + viewModel.benchCount());

        if (editMode) {
            binding.btnStartSeason.setEnabled(true);
            binding.btnStartSeason.setAlpha(1.0f);
            return;
        }
        boolean budgetOk = budget != null && budget >= 0;
        boolean complete = filled == needed && budgetOk;
        binding.btnStartSeason.setEnabled(complete);
        binding.btnStartSeason.setAlpha(complete ? 1.0f : 0.5f);
    }

    // ─── Pitch rendering ───────────────────────────────────────────────────────

    private void renderPitchIfReady() {
        if (!pitchReady) return;
        String f = viewModel.getFormation().getValue();
        if (f != null) renderPitch(f);
    }

    private void renderPitch(String formation) {
        binding.pitchContainer.removeAllViews();
        slotViews.clear();

        List<FormationSlot> slots = FormationCatalog.get(formation);
        Map<String, RealPlayer> assigned = viewModel.getAssignments().getValue();
        if (assigned == null) assigned = new HashMap<>();

        int pitchW = binding.pitchContainer.getWidth();
        int pitchH = binding.pitchContainer.getHeight();
        int slotW = dpToPx(74);
        int slotH = dpToPx(64);

        for (FormationSlot slot : slots) {
            View slotView = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_pitch_slot, binding.pitchContainer, false);
            updateSlotView(slotView, slot, assigned.get(slot.key));

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

    private void updateSlotView(View slotView, FormationSlot slot, RealPlayer player) {
        TextView tvName = slotView.findViewById(R.id.text_slot_name);
        TextView tvExtra = slotView.findViewById(R.id.text_slot_extra);

        if (player != null) {
            tvName.setText(getLastName(player.getName()));
            tvExtra.setText(String.valueOf(player.getEffectiveOverall()));
            slotView.setBackgroundResource(R.drawable.bg_pitch_slot_filled);
            slotView.setBackgroundTintList(ColorStateList.valueOf(theme.primary));
            tvName.setTextColor(theme.onPrimary);
            tvExtra.setTextColor(theme.onPrimary);
        } else {
            tvName.setText(slot.label);
            tvExtra.setText("+");
            slotView.setBackgroundResource(R.drawable.bg_pitch_slot_empty);
            slotView.setBackgroundTintList(null);
            tvName.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
            tvExtra.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
        }
    }

    // ─── Bench rendering ───────────────────────────────────────────────────────

    private void renderBench(List<RealPlayer> bench) {
        binding.benchContainer.removeAllViews();
        boolean empty = bench == null || bench.isEmpty();
        binding.textBenchEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        if (empty) return;

        for (RealPlayer p : bench) {
            View chip = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_pitch_slot, binding.benchContainer, false);
            TextView tvName = chip.findViewById(R.id.text_slot_name);
            TextView tvExtra = chip.findViewById(R.id.text_slot_extra);
            tvName.setText(getLastName(p.getName()));
            tvName.setTextColor(theme.onPrimary);
            tvExtra.setText(String.valueOf(p.getEffectiveOverall()));
            tvExtra.setTextColor(theme.onPrimary);
            chip.setBackgroundResource(R.drawable.bg_pitch_slot_filled);
            chip.setBackgroundTintList(ColorStateList.valueOf(theme.primary));

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                dpToPx(74), dpToPx(64));
            lp.setMarginEnd(dpToPx(6));
            chip.setLayoutParams(lp);
            chip.setOnClickListener(v -> onBenchClicked(p));
            binding.benchContainer.addView(chip);
        }
    }

    // ─── Interactions ──────────────────────────────────────────────────────────

    private void onSlotClicked(FormationSlot slot) {
        Map<String, RealPlayer> assigned = viewModel.getAssignments().getValue();
        RealPlayer current = assigned != null ? assigned.get(slot.key) : null;

        if (current == null) {
            List<RealPlayer> options = eligibleBench(slot);
            String[] labels = playerLabels(options);
            MaterialAlertDialogBuilder b = new MaterialAlertDialogBuilder(requireContext(), R.style.FootMashDialog)
                .setTitle("Popuni: " + slot.label);
            if (labels.length == 0) {
                b.setMessage("Nema dostupnih igrača na klupi za ovu poziciju.");
            } else {
                b.setItems(labels, (d, w) ->
                    viewModel.assignFromBench(options.get(w), slot.key));
            }
            if (!editMode) {
                b.setPositiveButton("Kupi s tržišta", (d, w) -> {
                    viewModel.selectSlot(slot.key);
                    new PlayerMarketBottomSheet().show(getParentFragmentManager(), "market");
                });
            }
            b.setNegativeButton("Odustani", null).show();
        } else {
            showPlayerInfoDialog(current, slot.key, false);
        }
    }

    private void onBenchClicked(RealPlayer player) {
        showPlayerInfoDialog(player, null, true);
    }

    // --- Player info dialog ---

    private void showPlayerInfoDialog(RealPlayer p, String slotKey, boolean fromBench) {
        View root = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_player_info, null, false);

        TextView ovr = root.findViewById(R.id.info_overall);
        ovr.setText(String.valueOf(p.getEffectiveOverall()));
        ovr.setBackgroundTintList(ColorStateList.valueOf(overallBadgeColor(p.getEffectiveOverall())));
        ovr.setTextColor(p.getEffectiveOverall() >= 78 ? 0xFF111111 : 0xFFFFFFFF);

        ((TextView) root.findViewById(R.id.info_name)).setText(p.getName());
        StringBuilder meta = new StringBuilder();
        meta.append(p.getPosition() != null ? p.getPosition() : "-");
        meta.append("  ·  ").append(p.getAge()).append(" god.");
        if (p.getNationality() != null && !p.getNationality().isEmpty()) {
            meta.append("  ·  ").append(p.getNationality());
        }
        ((TextView) root.findViewById(R.id.info_meta)).setText(meta.toString());

        TextView form = root.findViewById(R.id.info_form);
        int fd = p.getFormDelta();
        if (fd != 0) {
            form.setVisibility(View.VISIBLE);
            form.setText("Forma " + signedDelta(fd) + "  (osnovni OVR " + p.getOverall() + ")");
            form.setTextColor(ContextCompat.getColor(requireContext(),
                fd > 0 ? R.color.success : R.color.error));
        }

        LinearLayout traitBox = root.findViewById(R.id.info_traits);
        List<Trait> traits = TraitEngine.deriveTraits(p);
        if (traits.isEmpty()) {
            traitBox.setVisibility(View.GONE);
        } else {
            LinearLayout chipRow = null;
            for (int i = 0; i < traits.size(); i++) {
                if (i % 2 == 0) {
                    chipRow = new LinearLayout(requireContext());
                    chipRow.setOrientation(LinearLayout.HORIZONTAL);
                    LinearLayout.LayoutParams rl = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    if (i > 0) rl.topMargin = dpToPx(6);
                    chipRow.setLayoutParams(rl);
                    traitBox.addView(chipRow);
                }
                chipRow.addView(makeTraitChip(traits.get(i), i % 2 == 1));
            }
        }

        LinearLayout attrs = root.findViewById(R.id.info_attrs);
        addAttrRow(attrs, "Brzina", p.getPace());
        addAttrRow(attrs, "Šut", p.getShooting());
        addAttrRow(attrs, "Dodavanje", p.getPassing());
        addAttrRow(attrs, "Driblanje", p.getDribbling());
        addAttrRow(attrs, "Obrana", p.getDefending());
        addAttrRow(attrs, "Fizički", p.getPhysical());

        MaterialAlertDialogBuilder b = new MaterialAlertDialogBuilder(
            requireContext(), R.style.FootMashDialog).setView(root);
        if (fromBench) {
            b.setPositiveButton("U prvi sastav", (d, w) -> promoteFromBench(p));
            if (!editMode) {
                b.setNeutralButton("Prodaj +€" + formatMillions(p.getMarketValue()),
                    (d, w) -> viewModel.sellFromBench(p));
            }
        } else {
            b.setPositiveButton("Na klupu", (d, w) -> viewModel.moveToBench(slotKey));
            if (!editMode) {
                b.setNeutralButton("Prodaj +€" + formatMillions(p.getMarketValue()),
                    (d, w) -> viewModel.sellFromSlot(slotKey));
            }
        }
        b.setNegativeButton("Zatvori", null);
        b.show();
    }

    private void promoteFromBench(RealPlayer p) {
        List<FormationSlot> slots = emptyEligibleSlots(p);
        if (slots.isEmpty()) {
            Toast.makeText(requireContext(),
                "Nema slobodnog slota za ovog igrača.", Toast.LENGTH_SHORT).show();
        } else if (slots.size() == 1) {
            viewModel.assignFromBench(p, slots.get(0).key);
        } else {
            String[] labels = new String[slots.size()];
            for (int i = 0; i < slots.size(); i++) labels[i] = "Uvrsti na: " + slots.get(i).label;
            new MaterialAlertDialogBuilder(requireContext(), R.style.FootMashDialog)
                .setTitle(p.getName())
                .setItems(labels, (d, w) -> viewModel.assignFromBench(p, slots.get(w).key))
                .setNegativeButton("Odustani", null)
                .show();
        }
    }

    private TextView makeTraitChip(Trait t, boolean marginStart) {
        TextView chip = new TextView(requireContext());
        chip.setText(t.label);
        chip.setTextSize(10f);
        chip.setTypeface(chip.getTypeface(), android.graphics.Typeface.BOLD);
        chip.setTextColor(traitGroupColor(t.group));
        chip.setBackgroundResource(R.drawable.bg_trait_chip);
        int padH = dpToPx(8), padV = dpToPx(3);
        chip.setPadding(padH, padV, padH, padV);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        if (marginStart) lp.setMarginStart(dpToPx(6));
        chip.setLayoutParams(lp);
        return chip;
    }

    private int traitGroupColor(String group) {
        switch (group) {
            case "GK": return 0xFFFFCC00;
            case "DF": return 0xFF32ADE6;
            case "FW": return 0xFFFF6B6B;
            default:   return 0xFF34C759;
        }
    }

    private void addAttrRow(LinearLayout table, String label, int value) {
        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dpToPx(5), 0, dpToPx(5));

        TextView tvLabel = new TextView(requireContext());
        tvLabel.setText(label);
        tvLabel.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
        tvLabel.setTextSize(13f);
        tvLabel.setLayoutParams(new LinearLayout.LayoutParams(dpToPx(92),
            ViewGroup.LayoutParams.WRAP_CONTENT));
        row.addView(tvLabel);

        int c = attrColor(value);
        ProgressBar bar = new ProgressBar(requireContext(), null,
            android.R.attr.progressBarStyleHorizontal);
        bar.setMax(99);
        bar.setProgress(Math.max(0, Math.min(99, value)));
        bar.setProgressTintList(ColorStateList.valueOf(c));
        bar.setProgressBackgroundTintList(ColorStateList.valueOf(
            ContextCompat.getColor(requireContext(), R.color.divider)));
        LinearLayout.LayoutParams bl = new LinearLayout.LayoutParams(0, dpToPx(8), 1f);
        bl.setMarginStart(dpToPx(10));
        bl.setMarginEnd(dpToPx(10));
        bar.setLayoutParams(bl);
        row.addView(bar);

        TextView tvValue = new TextView(requireContext());
        tvValue.setText(String.valueOf(value));
        tvValue.setTextColor(c);
        tvValue.setTextSize(15f);
        tvValue.setTypeface(tvValue.getTypeface(), android.graphics.Typeface.BOLD);
        tvValue.setGravity(Gravity.END);
        tvValue.setLayoutParams(new LinearLayout.LayoutParams(dpToPx(34),
            ViewGroup.LayoutParams.WRAP_CONTENT));
        row.addView(tvValue);

        table.addView(row);
    }

    private int attrColor(int v) {
        if (v >= 80) return ContextCompat.getColor(requireContext(), R.color.success);
        if (v >= 66) return ContextCompat.getColor(requireContext(), R.color.warning);
        return ContextCompat.getColor(requireContext(), R.color.error);
    }

    private int overallBadgeColor(int ovr) {
        if (ovr >= 88) return 0xFFFFD700;
        if (ovr >= 83) return 0xFFC0C0C0;
        if (ovr >= 78) return 0xFFCD7F32;
        return 0xFF2962FF;
    }

    private void addSynergyHeader(LinearLayout table, String text, int color) {
        TextView h = new TextView(requireContext());
        h.setText(text);
        h.setTextColor(color);
        h.setTextSize(12f);
        h.setTypeface(h.getTypeface(), android.graphics.Typeface.BOLD);
        h.setLetterSpacing(0.08f);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dpToPx(14);
        lp.bottomMargin = dpToPx(2);
        h.setLayoutParams(lp);
        table.addView(h);
    }

    private void addSynergyRow(LinearLayout table, String line, int countColor) {
        String combo = line;
        String count = "";
        int idx = line.lastIndexOf('×');
        if (idx > 0) {
            count = line.substring(idx).trim();
            combo = line.substring(0, idx).trim();
        }
        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dpToPx(7), 0, dpToPx(7));

        TextView tvCombo = new TextView(requireContext());
        tvCombo.setText(combo);
        tvCombo.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary));
        tvCombo.setTextSize(13f);
        tvCombo.setLayoutParams(new LinearLayout.LayoutParams(
            0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(tvCombo);

        if (!count.isEmpty()) {
            TextView tvCount = new TextView(requireContext());
            tvCount.setText(count);
            tvCount.setTextColor(countColor);
            tvCount.setTextSize(13f);
            tvCount.setTypeface(tvCount.getTypeface(), android.graphics.Typeface.BOLD);
            tvCount.setPadding(dpToPx(10), 0, 0, 0);
            row.addView(tvCount);
        }
        table.addView(row);

        View div = new View(requireContext());
        div.setLayoutParams(new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 1));
        div.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.divider));
        table.addView(div);
    }

    private void onAddBench() {
        if (viewModel.benchCount() >= DraftViewModel.MAX_BENCH) {
            Toast.makeText(requireContext(),
                "Klupa je puna (" + DraftViewModel.MAX_BENCH + " igrača)",
                Toast.LENGTH_SHORT).show();
            return;
        }
        viewModel.selectBench();
        new PlayerMarketBottomSheet().show(getParentFragmentManager(), "market");
    }

    private List<RealPlayer> eligibleBench(FormationSlot slot) {
        List<RealPlayer> out = new ArrayList<>();
        List<RealPlayer> bench = viewModel.getBench().getValue();
        if (bench == null) return out;
        for (RealPlayer p : bench) {
            if (FormationCatalog.matchesPosGroup(p.getPosition(), slot.posGroup)) out.add(p);
        }
        return out;
    }

    private List<FormationSlot> emptyEligibleSlots(RealPlayer player) {
        List<FormationSlot> out = new ArrayList<>();
        String f = viewModel.getFormation().getValue();
        Map<String, RealPlayer> assigned = viewModel.getAssignments().getValue();
        if (f == null) return out;
        for (FormationSlot slot : FormationCatalog.get(f)) {
            boolean filled = assigned != null && assigned.get(slot.key) != null;
            if (!filled && FormationCatalog.matchesPosGroup(player.getPosition(), slot.posGroup)) {
                out.add(slot);
            }
        }
        return out;
    }

    private String[] playerLabels(List<RealPlayer> players) {
        String[] labels = new String[players.size()];
        for (int i = 0; i < players.size(); i++) {
            RealPlayer p = players.get(i);
            labels[i] = p.getName() + "  ·  " + p.getEffectiveOverall() + " OVR  ·  " + p.getPosition();
        }
        return labels;
    }

    private void onStartSeason(View navView) {
        if (editMode) {
            Navigation.findNavController(navView).popBackStack();
            return;
        }
        DraftViewModel.LineupValidation v = viewModel.validateLineup();
        if (!v.legal) {
            StringBuilder msg = new StringBuilder();
            for (String r : v.reasons) msg.append("• ").append(r).append('\n');
            new MaterialAlertDialogBuilder(requireContext(), R.style.FootMashDialog)
                .setTitle("Sastav nije spreman")
                .setMessage(msg.toString().trim())
                .setPositiveButton("U redu", null)
                .show();
            return;
        }
        binding.btnStartSeason.setEnabled(false);
        Bundle args = new Bundle();
        args.putInt("clubId", clubId);
        Navigation.findNavController(navView)
            .navigate(R.id.action_draftCanvas_to_seasonHub, args);
    }

    private String getLastName(String fullName) {
        if (fullName == null || fullName.isEmpty()) return "";
        String[] parts = fullName.trim().split("\\s+");
        return parts[parts.length - 1];
    }

    private int overallColor(int overall) {
        if (overall >= 88) return ContextCompat.getColor(requireContext(), R.color.warning);
        if (overall >= 83) return ContextCompat.getColor(requireContext(), R.color.text_secondary);
        return ContextCompat.getColor(requireContext(), R.color.text_primary);
    }

    private String formatMillions(long v) {
        return String.format("%.1fM", v / 1_000_000.0);
    }

    private String signedDelta(int v) {
        return v > 0 ? "+" + v : String.valueOf(v);
    }

    private int dpToPx(int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp,
            requireContext().getResources().getDisplayMetrics());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
