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

import com.bumptech.glide.Glide;

import hr.fipu.footmash.R;
import hr.fipu.footmash.databinding.FragmentWcGroupsBinding;
import hr.fipu.footmash.db.LogoAssets;
import hr.fipu.footmash.ui.util.InitialsBadgeDrawable;
import hr.fipu.footmash.worldcup.WcTournament;
import hr.fipu.footmash.worldcup.WorldCupData;

/** Shows the 12 group standing tables; the top two of each group (green) advance,
 *  alongside the eight best third-placed nations. */
public class WorldCupGroupsFragment extends Fragment {

    private FragmentWcGroupsBinding binding;
    private WorldCupViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentWcGroupsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(WorldCupViewModel.class);
        viewModel.getState().observe(getViewLifecycleOwner(), this::bind);
        viewModel.refresh();
    }

    private void bind(@Nullable WcTournament t) {
        binding.containerGroups.removeAllViews();
        if (t == null) return;
        for (WcTournament.WcGroup g : t.groups) {
            binding.containerGroups.addView(buildGroup(t, g));
        }
    }

    private View buildGroup(WcTournament t, WcTournament.WcGroup g) {
        LinearLayout card = new LinearLayout(requireContext());
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.bg_pill_unselected);
        card.setPadding(dp(12), dp(12), dp(12), dp(12));
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        clp.bottomMargin = dp(12);
        card.setLayoutParams(clp);

        TextView header = new TextView(requireContext());
        header.setText("Skupina " + g.name);
        header.setTextColor(ContextCompat.getColor(requireContext(), R.color.accent_blue));
        header.setTextSize(15f);
        header.setTypeface(header.getTypeface(), Typeface.BOLD);
        card.addView(header);

        card.addView(headerRow());

        int pos = 1;
        for (WcTournament.WcStanding s : g.table) {
            card.addView(teamRow(pos, s));
            pos++;
        }
        return card;
    }

    private View headerRow() {
        LinearLayout row = newRow();
        row.addView(cell("#", 0.10f, Gravity.START, R.color.text_secondary, false));
        row.addView(cell("", 0.12f, Gravity.START, R.color.text_secondary, false));
        row.addView(cell("Reprezentacija", 0.42f, Gravity.START, R.color.text_secondary, false));
        row.addView(cell("P", 0.10f, Gravity.CENTER, R.color.text_secondary, false));
        row.addView(cell("GR", 0.13f, Gravity.CENTER, R.color.text_secondary, false));
        row.addView(cell("B", 0.13f, Gravity.CENTER, R.color.text_secondary, false));
        return row;
    }

    private View teamRow(int pos, WcTournament.WcStanding s) {
        LinearLayout row = newRow();
        boolean qualifies = pos <= 2;
        int color = qualifies ? R.color.success : R.color.text_primary;

        row.addView(cell(String.valueOf(pos), 0.10f, Gravity.START, color, qualifies));

        ImageView crest = new ImageView(requireContext());
        LinearLayout.LayoutParams ilp = new LinearLayout.LayoutParams(0, dp(22), 0.12f);
        ilp.gravity = Gravity.CENTER_VERTICAL;
        crest.setLayoutParams(ilp);
        WorldCupData.Nation n = WorldCupData.byKey(s.nationKey);
        Glide.with(this)
            .load(LogoAssets.assetUri(n != null ? n.logo : ""))
            .placeholder(new InitialsBadgeDrawable(name(s.nationKey)))
            .into(crest);
        row.addView(crest);

        row.addView(cell(name(s.nationKey), 0.42f, Gravity.START, color, qualifies));
        row.addView(cell(String.valueOf(s.played), 0.10f, Gravity.CENTER, color, false));
        row.addView(cell(signed(s.gd()), 0.13f, Gravity.CENTER, color, false));
        row.addView(cell(String.valueOf(s.pts), 0.13f, Gravity.CENTER, color, true));
        return row;
    }

    private LinearLayout newRow() {
        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(5), 0, dp(5));
        row.setLayoutParams(new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return row;
    }

    private TextView cell(String text, float weight, int gravity, int colorRes, boolean bold) {
        TextView tv = new TextView(requireContext());
        tv.setText(text);
        tv.setTextColor(ContextCompat.getColor(requireContext(), colorRes));
        tv.setTextSize(13f);
        tv.setGravity(gravity);
        if (bold) tv.setTypeface(tv.getTypeface(), Typeface.BOLD);
        tv.setLayoutParams(new LinearLayout.LayoutParams(0,
            ViewGroup.LayoutParams.WRAP_CONTENT, weight));
        return tv;
    }

    private static String name(String key) {
        WorldCupData.Nation n = WorldCupData.byKey(key);
        return n != null ? n.name : key;
    }

    private static String signed(int v) {
        return v > 0 ? "+" + v : String.valueOf(v);
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
