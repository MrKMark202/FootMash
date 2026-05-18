package hr.fipu.footmash.ui.season;

import android.app.AlertDialog;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.LinearLayout;
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
import hr.fipu.footmash.databinding.FragmentSquadBuilderBinding;
import hr.fipu.footmash.model.FormationSlot;
import hr.fipu.footmash.model.RealPlayer;

public class SquadBuilderFragment extends Fragment {

    private FragmentSquadBuilderBinding binding;
    private SquadBuilderViewModel viewModel;
    private final Map<String, View> slotViews = new HashMap<>();
    private boolean pitchReady = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentSquadBuilderBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        int clubId = getArguments() != null ? getArguments().getInt("clubId", -1) : -1;
        if (clubId == -1) {
            Toast.makeText(requireContext(), "Greška: klub nije pronađen", Toast.LENGTH_SHORT).show();
            return;
        }

        viewModel = new ViewModelProvider(this).get(SquadBuilderViewModel.class);
        viewModel.init(clubId);

        setupFormationChips();
        observeViewModel();
        binding.btnConfirmLineup.setOnClickListener(v -> onConfirmLineup(view));

        binding.pitchContainer.getViewTreeObserver().addOnGlobalLayoutListener(
            new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    if (!pitchReady && binding.pitchContainer.getWidth() > 0) {
                        pitchReady = true;
                        binding.pitchContainer.getViewTreeObserver()
                            .removeOnGlobalLayoutListener(this);
                        renderPitch();
                    }
                }
            }
        );
    }

    private void setupFormationChips() {
        LinearLayout container = binding.formationChipsContainer;
        for (String name : SquadBuilderViewModel.FORMATIONS.keySet()) {
            TextView chip = new TextView(requireContext());
            chip.setText(name);
            chip.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            chip.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
            int hPad = dpToPx(12);
            int vPad = dpToPx(6);
            chip.setPadding(hPad, vPad, hPad, vPad);
            chip.setBackground(ContextCompat.getDrawable(requireContext(), R.drawable.bg_pill_unselected));

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, 0, dpToPx(8), 0);
            chip.setLayoutParams(lp);

            chip.setOnClickListener(v -> {
                viewModel.setFormation(name);
                updateFormationChips(name);
            });
            container.addView(chip);
        }
    }

    private void updateFormationChips(String selected) {
        LinearLayout container = binding.formationChipsContainer;
        List<String> names = new ArrayList<>(SquadBuilderViewModel.FORMATIONS.keySet());
        for (int i = 0; i < container.getChildCount() && i < names.size(); i++) {
            View child = container.getChildAt(i);
            if (!(child instanceof TextView)) continue;
            boolean active = names.get(i).equals(selected);
            child.setBackground(ContextCompat.getDrawable(requireContext(),
                active ? R.drawable.bg_pill_selected : R.drawable.bg_pill_unselected));
            ((TextView) child).setTextColor(ContextCompat.getColor(requireContext(),
                active ? R.color.text_on_primary : R.color.text_secondary));
        }
    }

    private void observeViewModel() {
        viewModel.getFormation().observe(getViewLifecycleOwner(), formation -> {
            updateFormationChips(formation);
            if (pitchReady) renderPitch();
        });

        viewModel.getAssignments().observe(getViewLifecycleOwner(), assigned -> {
            if (!pitchReady || slotViews.isEmpty()) return;
            String formation = viewModel.getFormation().getValue();
            if (formation == null) return;
            List<FormationSlot> slots = SquadBuilderViewModel.FORMATIONS.get(formation);
            if (slots == null) return;
            for (FormationSlot slot : slots) {
                View slotView = slotViews.get(slot.key);
                if (slotView != null) {
                    updateSlotView(slotView, slot, assigned.get(slot.key));
                }
            }
            int filled = assigned.size();
            binding.textLineupStatus.setText(filled + "/11 postavljeno");
            boolean complete = filled == 11;
            binding.btnConfirmLineup.setEnabled(complete);
            binding.btnConfirmLineup.setAlpha(complete ? 1.0f : 0.5f);
        });
    }

    private void renderPitch() {
        binding.pitchContainer.removeAllViews();
        slotViews.clear();

        String formation = viewModel.getFormation().getValue();
        if (formation == null) formation = "4-4-2";
        List<FormationSlot> slots = SquadBuilderViewModel.FORMATIONS.get(formation);
        if (slots == null) return;

        Map<String, RealPlayer> assigned = viewModel.getAssignments().getValue();
        if (assigned == null) assigned = new HashMap<>();

        int pitchW = binding.pitchContainer.getWidth();
        int pitchH = binding.pitchContainer.getHeight();
        int slotW = dpToPx(60);
        int slotH = dpToPx(52);

        for (FormationSlot slot : slots) {
            View slotView = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_pitch_slot, binding.pitchContainer, false);

            updateSlotView(slotView, slot, assigned.get(slot.key));

            int left = (int)(slot.xPct * pitchW) - slotW / 2;
            int top  = (int)(slot.yPct * pitchH) - slotH / 2;
            left = Math.max(4, Math.min(pitchW - slotW - 4, left));
            top  = Math.max(4, Math.min(pitchH - slotH - 4, top));

            android.widget.FrameLayout.LayoutParams lp =
                new android.widget.FrameLayout.LayoutParams(slotW, slotH);
            lp.leftMargin = left;
            lp.topMargin  = top;

            binding.pitchContainer.addView(slotView, lp);
            slotViews.put(slot.key, slotView);

            final FormationSlot finalSlot = slot;
            slotView.setOnClickListener(v -> showPlayerPicker(finalSlot));
        }
    }

    private void updateSlotView(View slotView, FormationSlot slot, RealPlayer player) {
        TextView tvName  = slotView.findViewById(R.id.text_slot_name);
        TextView tvExtra = slotView.findViewById(R.id.text_slot_extra);

        if (player != null) {
            tvName.setText(getLastName(player.getName()));
            tvExtra.setText(String.valueOf(player.getOverall()));
            slotView.setBackground(ContextCompat.getDrawable(requireContext(),
                R.drawable.bg_pitch_slot_filled));
            tvName.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary));
            tvExtra.setTextColor(overallColor(player.getOverall()));
        } else {
            tvName.setText(slot.label);
            tvExtra.setText("+");
            slotView.setBackground(ContextCompat.getDrawable(requireContext(),
                R.drawable.bg_pitch_slot_empty));
            tvName.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
            tvExtra.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
        }
    }

    private void showPlayerPicker(FormationSlot slot) {
        List<RealPlayer> allSigned = viewModel.getSignedPlayers().getValue();
        if (allSigned == null) allSigned = new ArrayList<>();

        List<RealPlayer> eligible = new ArrayList<>();
        for (RealPlayer p : allSigned) {
            if (matchesPosGroup(p.getPosition(), slot.posGroup)) eligible.add(p);
        }

        Map<String, RealPlayer> current = viewModel.getAssignments().getValue();
        RealPlayer currentPlayer = current != null ? current.get(slot.key) : null;

        String[] items = new String[eligible.size()];
        for (int i = 0; i < eligible.size(); i++) {
            RealPlayer p = eligible.get(i);
            items[i] = p.getName() + "  (OVR " + p.getOverall() + ")";
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext())
            .setTitle(slot.label + " — odaberi igrača");

        if (items.length > 0) {
            List<RealPlayer> finalEligible = eligible;
            builder.setItems(items, (dialog, which) ->
                viewModel.assignPlayer(slot.key, finalEligible.get(which)));
        } else {
            builder.setMessage("Nema dostupnih igrača za poziciju " + slot.label);
        }

        if (currentPlayer != null) {
            builder.setNeutralButton("Ukloni", (d, w) -> viewModel.unassignSlot(slot.key));
        }

        builder.setNegativeButton("Odustani", null).show();
    }

    private void onConfirmLineup(View navView) {
        if (!viewModel.isLineupComplete()) {
            Toast.makeText(requireContext(), "Popuni svih 11 pozicija", Toast.LENGTH_SHORT).show();
            return;
        }
        binding.btnConfirmLineup.setEnabled(false);
        viewModel.saveLineup(() -> requireActivity().runOnUiThread(() -> {
            Bundle args = new Bundle();
            args.putInt("clubId", viewModel.getClubId());
            Navigation.findNavController(navView)
                .navigate(R.id.action_squadBuilder_to_seasonHub, args);
        }));
    }

    private boolean matchesPosGroup(String pos, String group) {
        if (pos == null) return false;
        switch (group) {
            case "GK": return pos.equals("GK");
            case "DF": return pos.equals("CB") || pos.equals("RB") || pos.equals("LB");
            case "MF": return pos.equals("CM") || pos.equals("CDM") || pos.equals("CAM")
                           || pos.equals("LM") || pos.equals("RM");
            case "FW": return pos.equals("ST") || pos.equals("FW")
                           || pos.equals("LW") || pos.equals("RW");
            default:   return false;
        }
    }

    private String getLastName(String fullName) {
        if (fullName == null || fullName.isEmpty()) return "";
        String[] parts = fullName.trim().split("\\s+");
        return parts[parts.length - 1];
    }

    private int overallColor(int overall) {
        if (overall >= 88)
            return ContextCompat.getColor(requireContext(), R.color.warning);
        if (overall >= 83)
            return ContextCompat.getColor(requireContext(), R.color.text_secondary);
        return ContextCompat.getColor(requireContext(), R.color.text_primary);
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
