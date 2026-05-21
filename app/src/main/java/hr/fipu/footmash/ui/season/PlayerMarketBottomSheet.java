package hr.fipu.footmash.ui.season;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import hr.fipu.footmash.R;
import hr.fipu.footmash.databinding.BottomSheetPlayerMarketBinding;
import hr.fipu.footmash.model.FormationSlot;
import hr.fipu.footmash.model.RealPlayer;
import hr.fipu.footmash.model.Trait;
import hr.fipu.footmash.season.TraitEngine;

/**
 * Slot-targeted player market. Players can be narrowed with a name search and a
 * position filter (the position filter is shown only in bench mode, where the
 * market spans every position).
 */
public class PlayerMarketBottomSheet extends BottomSheetDialogFragment {

    private BottomSheetPlayerMarketBinding binding;
    private DraftViewModel viewModel;
    private CandidatesAdapter adapter;

    private final List<RealPlayer> allCandidates = new ArrayList<>();
    private final Map<String, TextView> filterChips = new LinkedHashMap<>();
    private String searchQuery = "";
    private String posFilter = "ALL";

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

        String selectedKey = viewModel.getSelectedSlotKey().getValue();
        boolean benchMode = DraftViewModel.BENCH_KEY.equals(selectedKey);
        FormationSlot slot = viewModel.getSelectedSlot();
        if (benchMode) {
            binding.textSlotTitle.setText("Dodaj na klupu");
            binding.textSlotSubtitle.setText("Zamjenski igrač");
        } else if (slot != null) {
            binding.textSlotTitle.setText("Kupi " + slot.label);
            binding.textSlotSubtitle.setText("Pozicija " + slot.posGroup);
        } else {
            binding.textSlotTitle.setText("Tržište");
        }

        adapter = new CandidatesAdapter(player -> {
            String key = viewModel.getSelectedSlotKey().getValue();
            if (DraftViewModel.BENCH_KEY.equals(key)) {
                viewModel.buyForBench(player);
            } else if (key != null) {
                viewModel.buyForSlot(key, player);
            }
            dismiss();
        });
        binding.recyclerCandidates.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerCandidates.setAdapter(adapter);

        // Position filter only makes sense when the market spans every position.
        binding.filterChipsScroll.setVisibility(benchMode ? View.VISIBLE : View.GONE);
        buildFilterChips();

        binding.inputSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {
                searchQuery = s != null ? s.toString() : "";
                applyFilter();
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        viewModel.getMarketCandidates().observe(getViewLifecycleOwner(), candidates -> {
            allCandidates.clear();
            if (candidates != null) allCandidates.addAll(candidates);
            applyFilter();
        });

        viewModel.getRemainingBudget().observe(getViewLifecycleOwner(), budget -> {
            double m = budget / 1_000_000.0;
            binding.textBudgetChip.setText(String.format("€%.1fM", m));
        });
    }

    // --- Filtering -------------------------------------------------------------

    private void buildFilterChips() {
        String[][] defs = {
            {"ALL", "Svi"}, {"GK", "GK"}, {"DF", "DF"}, {"MF", "MF"}, {"FW", "FW"}
        };
        binding.filterChips.removeAllViews();
        filterChips.clear();
        for (String[] d : defs) {
            TextView chip = new TextView(requireContext());
            chip.setText(d[1]);
            chip.setTextSize(12f);
            chip.setTypeface(chip.getTypeface(), Typeface.BOLD);
            int padH = dp(14), padV = dp(6);
            chip.setPadding(padH, padV, padH, padV);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMarginEnd(dp(6));
            chip.setLayoutParams(lp);
            chip.setOnClickListener(v -> {
                posFilter = d[0];
                highlightFilterChips();
                applyFilter();
            });
            binding.filterChips.addView(chip);
            filterChips.put(d[0], chip);
        }
        highlightFilterChips();
    }

    private void highlightFilterChips() {
        for (Map.Entry<String, TextView> e : filterChips.entrySet()) {
            boolean on = e.getKey().equals(posFilter);
            e.getValue().setBackgroundResource(
                on ? R.drawable.bg_pill_selected : R.drawable.bg_pill_unselected);
            e.getValue().setTextColor(ContextCompat.getColor(requireContext(),
                on ? R.color.text_on_primary : R.color.text_secondary));
        }
    }

    private void applyFilter() {
        if (binding == null) return;
        List<RealPlayer> out = new ArrayList<>();
        String q = searchQuery.trim().toLowerCase();
        for (RealPlayer p : allCandidates) {
            if (!"ALL".equals(posFilter)
                    && !posFilter.equals(TraitEngine.groupOf(p.getPosition()))) continue;
            if (!q.isEmpty()
                    && (p.getName() == null || !p.getName().toLowerCase().contains(q))) continue;
            out.add(p);
        }
        adapter.setPlayers(out);
        boolean empty = out.isEmpty();
        binding.textEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        binding.recyclerCandidates.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    private int dp(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
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

    // --- Adapter ---------------------------------------------------------------

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
            holder.overall.setText(String.valueOf(p.getEffectiveOverall()));
            holder.name.setText(p.getName());
            holder.position.setText(p.getPosition());
            holder.team.setText(p.getTeamName());
            holder.value.setText(formatValue(p.getMarketValue()));

            int color = overallColor(p.getEffectiveOverall());
            holder.overall.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(color));

            bindTraitChips(holder.traitChips, p);

            holder.buy.setOnClickListener(v -> callback.onBuy(p));
        }

        @Override
        public int getItemCount() { return players.size(); }

        private void bindTraitChips(LinearLayout container, RealPlayer p) {
            container.removeAllViews();
            List<Trait> traits = TraitEngine.deriveTraits(p);
            int shown = Math.min(2, traits.size());
            for (int i = 0; i < shown; i++) {
                Trait t = traits.get(i);
                TextView chip = new TextView(container.getContext());
                chip.setText(t.label);
                chip.setTextSize(9f);
                chip.setTextColor(groupColor(t.group));
                chip.setTypeface(chip.getTypeface(), Typeface.BOLD);
                chip.setBackgroundResource(R.drawable.bg_trait_chip);
                int padH = dp(container, 6), padV = dp(container, 2);
                chip.setPadding(padH, padV, padH, padV);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                if (i > 0) lp.setMarginStart(dp(container, 4));
                chip.setLayoutParams(lp);
                container.addView(chip);
            }
            container.setVisibility(shown == 0 ? View.GONE : View.VISIBLE);
        }

        private int groupColor(String group) {
            switch (group) {
                case "GK": return Color.parseColor("#FFCC00");
                case "DF": return Color.parseColor("#32ADE6");
                case "FW": return Color.parseColor("#FF6B6B");
                default:   return Color.parseColor("#34C759");
            }
        }

        private int dp(View v, int dp) {
            return Math.round(dp * v.getResources().getDisplayMetrics().density);
        }

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
            LinearLayout traitChips;
            MaterialButton buy;
            VH(@NonNull View v) {
                super(v);
                overall = v.findViewById(R.id.textOverall);
                name = v.findViewById(R.id.textPlayerName);
                position = v.findViewById(R.id.textPosition);
                team = v.findViewById(R.id.textTeamName);
                value = v.findViewById(R.id.textMarketValue);
                traitChips = v.findViewById(R.id.traitChips);
                buy = v.findViewById(R.id.btnBuy);
            }
        }
    }
}
