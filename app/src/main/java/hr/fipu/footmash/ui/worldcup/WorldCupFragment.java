package hr.fipu.footmash.ui.worldcup;

import android.graphics.Typeface;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.bumptech.glide.Glide;

import java.util.List;
import java.util.Map;

import hr.fipu.footmash.R;
import hr.fipu.footmash.databinding.FragmentWorldCupBinding;
import hr.fipu.footmash.db.LogoAssets;
import hr.fipu.footmash.ui.util.InitialsBadgeDrawable;
import hr.fipu.footmash.worldcup.WcTournament;
import hr.fipu.footmash.worldcup.WorldCupData;

/**
 * World Cup tab: lists every nation in the projected 2026 field, grouped by
 * confederation. Rows are built programmatically (no DB / network) from
 * {@link WorldCupData}.
 */
public class WorldCupFragment extends Fragment {

    private FragmentWorldCupBinding binding;
    private WorldCupViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentWorldCupBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.textWcTitle.setText(WorldCupData.TITLE);
        binding.textWcSubtitle.setText(WorldCupData.SUBTITLE);

        viewModel = new ViewModelProvider(this).get(WorldCupViewModel.class);
        viewModel.getState().observe(getViewLifecycleOwner(), this::bindCta);
        viewModel.refresh();

        Map<String, List<WorldCupData.Nation>> grouped = WorldCupData.byConfederation();
        for (Map.Entry<String, List<WorldCupData.Nation>> e : grouped.entrySet()) {
            addSection(confederationLabel(e.getKey()), e.getValue());
        }
    }

    /** Switches the primary button between "start" and "continue" based on saved state. */
    private void bindCta(@Nullable WcTournament t) {
        boolean active = t != null;
        binding.buttonWcAction.setText(active ? R.string.wc_continue : R.string.wc_start);
        binding.buttonWcAction.setOnClickListener(v -> Navigation.findNavController(v)
            .navigate(active ? R.id.action_worldCup_to_hub
                             : R.id.action_worldCup_to_nationPicker));
    }

    @Override
    public void onResume() {
        super.onResume();
        if (viewModel != null) viewModel.refresh();
    }

    private void addSection(String header, List<WorldCupData.Nation> nations) {
        TextView title = new TextView(requireContext());
        title.setText(header + "  ·  " + nations.size());
        title.setTextColor(ContextCompat.getColor(requireContext(), R.color.accent_blue));
        title.setTextSize(13f);
        title.setTypeface(title.getTypeface(), Typeface.BOLD);
        title.setLetterSpacing(0.06f);
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tlp.topMargin = dp(18);
        tlp.bottomMargin = dp(6);
        title.setLayoutParams(tlp);
        binding.containerNations.addView(title);

        for (WorldCupData.Nation n : nations) {
            binding.containerNations.addView(buildRow(n));
        }
    }

    private View buildRow(WorldCupData.Nation n) {
        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setBackgroundResource(R.drawable.bg_pill_unselected);
        int padH = dp(14), padV = dp(12);
        row.setPadding(padH, padV, padH, padV);
        LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rlp.bottomMargin = dp(8);
        row.setLayoutParams(rlp);

        ImageView crest = new ImageView(requireContext());
        crest.setLayoutParams(new LinearLayout.LayoutParams(dp(30), dp(30)));
        Glide.with(this)
            .load(LogoAssets.assetUri(n.logo))
            .placeholder(new InitialsBadgeDrawable(n.name))
            .into(crest);
        row.addView(crest);

        TextView name = new TextView(requireContext());
        name.setText(n.name);
        name.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary));
        name.setTextSize(16f);
        name.setTypeface(name.getTypeface(), Typeface.BOLD);
        LinearLayout.LayoutParams nlp = new LinearLayout.LayoutParams(
            0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        nlp.leftMargin = dp(14);
        name.setLayoutParams(nlp);
        row.addView(name);

        if (n.host) {
            TextView host = new TextView(requireContext());
            host.setText("DOMAĆIN");
            host.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_on_primary));
            host.setBackgroundResource(R.drawable.bg_pill_selected);
            host.setTextSize(10f);
            host.setTypeface(host.getTypeface(), Typeface.BOLD);
            host.setPadding(dp(10), dp(4), dp(10), dp(4));
            row.addView(host);
        }

        return row;
    }

    private static String confederationLabel(String code) {
        switch (code) {
            case "UEFA":     return "EUROPA (UEFA)";
            case "CONMEBOL": return "JUŽNA AMERIKA (CONMEBOL)";
            case "CONCACAF": return "SJ. & SR. AMERIKA (CONCACAF)";
            case "CAF":      return "AFRIKA (CAF)";
            case "AFC":      return "AZIJA (AFC)";
            case "OFC":      return "OCEANIJA (OFC)";
            default:         return code;
        }
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
