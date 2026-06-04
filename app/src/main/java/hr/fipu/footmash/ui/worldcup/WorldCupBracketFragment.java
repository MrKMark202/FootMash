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
import hr.fipu.footmash.databinding.FragmentWcBracketBinding;
import hr.fipu.footmash.db.LogoAssets;
import hr.fipu.footmash.ui.util.InitialsBadgeDrawable;
import hr.fipu.footmash.worldcup.WcTournament;
import hr.fipu.footmash.worldcup.WorldCupData;

/** Renders the knockout bracket as horizontally scrollable round columns. */
public class WorldCupBracketFragment extends Fragment {

    private FragmentWcBracketBinding binding;
    private WorldCupViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentWcBracketBinding.inflate(inflater, container, false);
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
        binding.containerBracket.removeAllViews();
        if (t == null) return;
        if (t.knockout.isEmpty()) {
            TextView tv = new TextView(requireContext());
            tv.setText("Ždrijeb počinje nakon skupina.");
            tv.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
            tv.setPadding(dp(8), dp(24), dp(8), dp(8));
            binding.containerBracket.addView(tv);
            return;
        }
        for (WcTournament.WcRound r : t.knockout) {
            binding.containerBracket.addView(buildColumn(t, r));
        }
    }

    private View buildColumn(WcTournament t, WcTournament.WcRound round) {
        LinearLayout col = new LinearLayout(requireContext());
        col.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(dp(170),
            ViewGroup.LayoutParams.WRAP_CONTENT);
        clp.rightMargin = dp(10);
        col.setLayoutParams(clp);

        TextView header = new TextView(requireContext());
        header.setText(roundName(round.stage));
        header.setTextColor(ContextCompat.getColor(requireContext(), R.color.accent_blue));
        header.setTextSize(13f);
        header.setTypeface(header.getTypeface(), Typeface.BOLD);
        header.setPadding(dp(2), dp(2), dp(2), dp(10));
        col.addView(header);

        boolean isFinal = WcTournament.STAGE_FINAL.equals(round.stage);
        for (int i = 0; i < round.ties.size(); i++) {
            // In the FINAL round ties[0] is the third-place game, ties[1] is the final.
            if (isFinal) {
                String label = i == 0 ? getString(R.string.wc_round_third)
                                      : getString(R.string.wc_round_final);
                TextView l = new TextView(requireContext());
                l.setText(label);
                l.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
                l.setTextSize(11f);
                l.setPadding(dp(2), dp(8), dp(2), dp(4));
                col.addView(l);
            }
            col.addView(buildTie(t, round.ties.get(i)));
        }
        return col;
    }

    private View buildTie(WcTournament t, WcTournament.WcMatch m) {
        LinearLayout card = new LinearLayout(requireContext());
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.bg_pill_unselected);
        card.setPadding(dp(8), dp(8), dp(8), dp(8));
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        clp.bottomMargin = dp(10);
        card.setLayoutParams(clp);

        card.addView(side(t, m, true));
        card.addView(side(t, m, false));
        if (m.penalties) {
            TextView pen = new TextView(requireContext());
            pen.setText("(" + m.homePens + "-" + m.awayPens + " pen)");
            pen.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
            pen.setTextSize(10f);
            pen.setPadding(0, dp(4), 0, 0);
            card.addView(pen);
        }
        return card;
    }

    private View side(WcTournament t, WcTournament.WcMatch m, boolean home) {
        String key = home ? m.homeKey : m.awayKey;
        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(3), 0, dp(3));

        ImageView crest = new ImageView(requireContext());
        crest.setLayoutParams(new LinearLayout.LayoutParams(dp(20), dp(20)));
        WorldCupData.Nation n = WorldCupData.byKey(key);
        Glide.with(this)
            .load(LogoAssets.assetUri(n != null ? n.logo : ""))
            .placeholder(new InitialsBadgeDrawable(name(key)))
            .into(crest);
        row.addView(crest);

        boolean winner = m.played && key != null && key.equals(m.winnerKey);
        boolean isUser = key != null && key.equals(t.userNationKey);

        TextView nm = new TextView(requireContext());
        nm.setText(name(key));
        nm.setMaxLines(1);
        nm.setTextSize(12f);
        nm.setTextColor(ContextCompat.getColor(requireContext(),
            isUser ? R.color.accent_blue : (winner ? R.color.text_primary : R.color.text_secondary)));
        if (winner || isUser) nm.setTypeface(nm.getTypeface(), Typeface.BOLD);
        LinearLayout.LayoutParams nlp = new LinearLayout.LayoutParams(0,
            ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        nlp.leftMargin = dp(6);
        nm.setLayoutParams(nlp);
        row.addView(nm);

        TextView score = new TextView(requireContext());
        score.setText(m.played ? String.valueOf(home ? m.homeGoals : m.awayGoals) : "–");
        score.setTextSize(12f);
        score.setTextColor(ContextCompat.getColor(requireContext(),
            winner ? R.color.text_primary : R.color.text_secondary));
        if (winner) score.setTypeface(score.getTypeface(), Typeface.BOLD);
        row.addView(score);
        return row;
    }

    private String roundName(String stage) {
        switch (stage) {
            case WcTournament.STAGE_R32:   return getString(R.string.wc_round_r32);
            case WcTournament.STAGE_R16:   return getString(R.string.wc_round_r16);
            case WcTournament.STAGE_QF:    return getString(R.string.wc_round_qf);
            case WcTournament.STAGE_SF:    return getString(R.string.wc_round_sf);
            case WcTournament.STAGE_FINAL: return getString(R.string.wc_round_final);
            default: return stage;
        }
    }

    private static String name(String key) {
        WorldCupData.Nation n = WorldCupData.byKey(key);
        return n != null ? n.name : (key == null ? "—" : key);
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
