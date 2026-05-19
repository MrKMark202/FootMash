package hr.fipu.footmash.ui.season;

import android.app.AlertDialog;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import hr.fipu.footmash.R;
import hr.fipu.footmash.databinding.FragmentDraftCanvasBinding;
import hr.fipu.footmash.db.AppDatabase;
import hr.fipu.footmash.model.FormationSlot;
import hr.fipu.footmash.model.RealPlayer;
import hr.fipu.footmash.model.UserClub;
import hr.fipu.footmash.season.FormationCatalog;

public class DraftCanvasFragment extends Fragment {

    private FragmentDraftCanvasBinding binding;
    private DraftViewModel viewModel;
    private final Map<String, View> slotViews = new HashMap<>();
    private boolean pitchReady = false;
    private int clubId = -1;

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
        if (clubId == -1) {
            Toast.makeText(requireContext(), "Greška: klub nije pronađen", Toast.LENGTH_SHORT).show();
            return;
        }

        viewModel = new ViewModelProvider(requireActivity()).get(DraftViewModel.class);

        // Load club to determine mode, then initialise the VM
        AppDatabase db = AppDatabase.getInstance(requireContext());
        new Thread(() -> {
            UserClub club = db.userClubDao().getClubByIdSync(clubId);
            if (club == null) return;
            DraftViewModel.DraftMode mode = club.getRealTeamSourceId() != null
                ? DraftViewModel.DraftMode.EXISTING_CLUB
                : DraftViewModel.DraftMode.NEW_CLUB;
            String name = club.getClubName();
            String formation = club.getFormation();

            requireActivity().runOnUiThread(() -> {
                viewModel.init(clubId, club.getLeagueId(), mode);

                binding.textClubName.setText(name);
                binding.textFormationLabel.setText(formation + " · "
                    + (mode == DraftViewModel.DraftMode.EXISTING_CLUB
                        ? "naslijeđeni klub" : "novi klub"));

                binding.textHelp.setText(mode == DraftViewModel.DraftMode.EXISTING_CLUB
                    ? "Klikni igrača za prodaju. Klikni prazan slot za kupovinu."
                    : "Klikni prazan slot za kupovinu igrača s tržišta.");

                observeViewModel();
                binding.btnStartSeason.setOnClickListener(v -> onStartSeason(view));
            });
        }).start();

        binding.pitchContainer.getViewTreeObserver().addOnGlobalLayoutListener(
            new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    if (!pitchReady && binding.pitchContainer.getWidth() > 0) {
                        pitchReady = true;
                        binding.pitchContainer.getViewTreeObserver()
                            .removeOnGlobalLayoutListener(this);
                        renderPitchIfReady();
                    }
                }
            }
        );
    }

    private void observeViewModel() {
        viewModel.getFormation().observe(getViewLifecycleOwner(), f -> renderPitchIfReady());

        viewModel.getAssignments().observe(getViewLifecycleOwner(), assigned -> {
            if (!pitchReady || slotViews.isEmpty()) {
                renderPitchIfReady();
                return;
            }
            String f = viewModel.getFormation().getValue();
            if (f == null) return;
            for (FormationSlot slot : FormationCatalog.get(f)) {
                View slotView = slotViews.get(slot.key);
                if (slotView != null) {
                    updateSlotView(slotView, slot, assigned.get(slot.key));
                }
            }
            updateStartButton(assigned, viewModel.getRemainingBudget().getValue());
        });

        viewModel.getRemainingBudget().observe(getViewLifecycleOwner(), budget -> {
            double millions = budget / 1_000_000.0;
            binding.textBudget.setText(String.format("€%.1fM", millions));
            updateStartButton(viewModel.getAssignments().getValue(), budget);
        });
    }

    private void updateStartButton(Map<String, RealPlayer> assigned, Long budget) {
        String f = viewModel.getFormation().getValue();
        if (f == null) return;
        int needed = FormationCatalog.get(f).size();
        int filled = assigned != null ? assigned.size() : 0;
        boolean budgetOk = budget != null && budget >= 0;
        boolean complete = filled == needed && budgetOk;

        binding.textLineupStatus.setText(filled + "/" + needed + " postavljeno");
        binding.btnStartSeason.setEnabled(complete);
        binding.btnStartSeason.setAlpha(complete ? 1.0f : 0.5f);
    }

    private void renderPitchIfReady() {
        if (!pitchReady) return;
        String f = viewModel.getFormation().getValue();
        if (f == null) return;
        renderPitch(f);
    }

    private void renderPitch(String formation) {
        binding.pitchContainer.removeAllViews();
        slotViews.clear();

        List<FormationSlot> slots = FormationCatalog.get(formation);
        Map<String, RealPlayer> assigned = viewModel.getAssignments().getValue();
        if (assigned == null) assigned = new HashMap<>();

        int pitchW = binding.pitchContainer.getWidth();
        int pitchH = binding.pitchContainer.getHeight();
        int slotW = dpToPx(64);
        int slotH = dpToPx(56);

        for (FormationSlot slot : slots) {
            View slotView = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_pitch_slot, binding.pitchContainer, false);

            updateSlotView(slotView, slot, assigned.get(slot.key));

            int left = (int)(slot.xPct * pitchW) - slotW / 2;
            int top  = (int)(slot.yPct * pitchH) - slotH / 2;
            left = Math.max(4, Math.min(pitchW - slotW - 4, left));
            top  = Math.max(4, Math.min(pitchH - slotH - 4, top));

            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(slotW, slotH);
            lp.leftMargin = left;
            lp.topMargin  = top;

            binding.pitchContainer.addView(slotView, lp);
            slotViews.put(slot.key, slotView);

            slotView.setOnClickListener(v -> onSlotClicked(slot));
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

    private void onSlotClicked(FormationSlot slot) {
        Map<String, RealPlayer> assigned = viewModel.getAssignments().getValue();
        RealPlayer current = assigned != null ? assigned.get(slot.key) : null;

        if (current != null) {
            // Sell prompt
            long refund = current.getMarketValue();
            new AlertDialog.Builder(requireContext())
                .setTitle("Prodati igrača?")
                .setMessage(current.getName() + " (OVR " + current.getOverall()
                    + ")\n+€" + formatMillions(refund))
                .setPositiveButton("Prodaj", (d, w) -> viewModel.sellFromSlot(slot.key))
                .setNegativeButton("Odustani", null)
                .show();
        } else {
            viewModel.selectSlot(slot.key);
            new PlayerMarketBottomSheet()
                .show(getParentFragmentManager(), "market");
        }
    }

    private void onStartSeason(View navView) {
        DraftViewModel.LineupValidation v = viewModel.validateLineup();
        if (!v.legal) {
            StringBuilder msg = new StringBuilder();
            for (String r : v.reasons) msg.append("• ").append(r).append('\n');
            new AlertDialog.Builder(requireContext())
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
