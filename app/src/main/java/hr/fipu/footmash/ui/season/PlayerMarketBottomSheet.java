package hr.fipu.footmash.ui.season;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

import hr.fipu.footmash.R;
import hr.fipu.footmash.databinding.BottomSheetPlayerMarketBinding;
import hr.fipu.footmash.model.FormationSlot;
import hr.fipu.footmash.model.RealPlayer;

public class PlayerMarketBottomSheet extends BottomSheetDialogFragment {

    private BottomSheetPlayerMarketBinding binding;
    private DraftViewModel viewModel;
    private CandidatesAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = BottomSheetPlayerMarketBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(DraftViewModel.class);

        FormationSlot slot = viewModel.getSelectedSlot();
        if (slot != null) {
            binding.textSlotTitle.setText("Kupi " + slot.label);
            binding.textSlotSubtitle.setText("Pozicija " + slot.posGroup);
        } else {
            binding.textSlotTitle.setText("Tržište");
        }

        adapter = new CandidatesAdapter(player -> {
            String key = viewModel.getSelectedSlotKey().getValue();
            if (key != null) viewModel.buyForSlot(key, player);
            dismiss();
        });
        binding.recyclerCandidates.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerCandidates.setAdapter(adapter);

        viewModel.getMarketCandidates().observe(getViewLifecycleOwner(), candidates -> {
            adapter.setPlayers(candidates);
            boolean empty = candidates == null || candidates.isEmpty();
            binding.textEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
            binding.recyclerCandidates.setVisibility(empty ? View.GONE : View.VISIBLE);
        });

        viewModel.getRemainingBudget().observe(getViewLifecycleOwner(), budget -> {
            double m = budget / 1_000_000.0;
            binding.textBudgetChip.setText(String.format("€%.1fM", m));
        });
    }

    @Override
    public void onDismiss(@NonNull android.content.DialogInterface dialog) {
        super.onDismiss(dialog);
        if (viewModel != null) viewModel.selectSlot(null);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    // --- adapter ---

    interface OnBuy {
        void onBuy(RealPlayer player);
    }

    static class CandidatesAdapter extends RecyclerView.Adapter<CandidatesAdapter.VH> {
        private List<RealPlayer> players = new ArrayList<>();
        private final OnBuy callback;

        CandidatesAdapter(OnBuy callback) { this.callback = callback; }

        void setPlayers(List<RealPlayer> p) {
            this.players = p != null ? p : new ArrayList<>();
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_market_player, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            RealPlayer p = players.get(position);
            holder.overall.setText(String.valueOf(p.getOverall()));
            holder.name.setText(p.getName());
            holder.position.setText(p.getPosition());
            holder.team.setText(p.getTeamName());
            holder.value.setText(formatValue(p.getMarketValue()));

            int color = overallColor(p.getOverall());
            holder.overall.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(color));

            holder.buy.setOnClickListener(v -> callback.onBuy(p));
        }

        @Override
        public int getItemCount() { return players.size(); }

        private String formatValue(long v) {
            if (v >= 1_000_000) return String.format("€%.1fM", v / 1_000_000.0);
            return String.format("€%.0fK", v / 1_000.0);
        }

        private int overallColor(int overall) {
            if (overall >= 88) return Color.parseColor("#FFD700");
            if (overall >= 83) return Color.parseColor("#C0C0C0");
            if (overall >= 78) return Color.parseColor("#CD7F32");
            return Color.parseColor("#2962FF");
        }

        static class VH extends RecyclerView.ViewHolder {
            TextView overall, name, position, team, value;
            MaterialButton buy;
            VH(@NonNull View v) {
                super(v);
                overall = v.findViewById(R.id.textOverall);
                name = v.findViewById(R.id.textPlayerName);
                position = v.findViewById(R.id.textPosition);
                team = v.findViewById(R.id.textTeamName);
                value = v.findViewById(R.id.textMarketValue);
                buy = v.findViewById(R.id.btnBuy);
            }
        }
    }
}
