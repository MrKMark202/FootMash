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

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import hr.fipu.footmash.R;
import hr.fipu.footmash.databinding.FragmentWcStatsBinding;
import hr.fipu.footmash.db.LogoAssets;
import hr.fipu.footmash.ui.util.InitialsBadgeDrawable;
import hr.fipu.footmash.worldcup.WcTournament;
import hr.fipu.footmash.worldcup.WorldCupData;

/**
 * Tournament-wide scoring leaderboard: aggregates every recorded goal and assist
 * across the group stage and knockout rounds into a top-scorers and top-assisters
 * ranking.
 */
public class WorldCupStatsFragment extends Fragment {

    private static final int MAX_ROWS = 12;

    private FragmentWcStatsBinding binding;
    private WorldCupViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentWcStatsBinding.inflate(inflater, container, false);
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
        binding.containerScorers.removeAllViews();
        binding.containerAssists.removeAllViews();
        if (t == null) return;

        Map<String, Tally> scorers = new LinkedHashMap<>();
        Map<String, Tally> assists = new LinkedHashMap<>();
        for (WcTournament.WcMatch m : allMatches(t)) {
            if (m.scorers == null) continue;
            for (WcTournament.WcScorer s : m.scorers) {
                if (s.name != null && !s.name.trim().isEmpty()) {
                    bump(scorers, s.name.trim(), s.nationKey);
                }
                if (s.assist != null && !s.assist.trim().isEmpty()) {
                    bump(assists, s.assist.trim(), s.nationKey);
                }
            }
        }

        List<Tally> topScorers = ranked(scorers);
        List<Tally> topAssists = ranked(assists);

        boolean empty = topScorers.isEmpty() && topAssists.isEmpty();
        binding.textEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        binding.headerScorers.setVisibility(topScorers.isEmpty() ? View.GONE : View.VISIBLE);
        binding.headerAssists.setVisibility(topAssists.isEmpty() ? View.GONE : View.VISIBLE);

        int rank = 1;
        for (Tally tly : topScorers) {
            binding.containerScorers.addView(buildRow(rank++, tly, true));
        }
        rank = 1;
        for (Tally tly : topAssists) {
            binding.containerAssists.addView(buildRow(rank++, tly, false));
        }
    }

    /** Every played match across the group stage and every knockout round. */
    private List<WcTournament.WcMatch> allMatches(WcTournament t) {
        List<WcTournament.WcMatch> out = new ArrayList<>();
        if (t.groupFixtures != null) {
            for (WcTournament.WcMatch m : t.groupFixtures) if (m.played) out.add(m);
        }
        if (t.knockout != null) {
            for (WcTournament.WcRound r : t.knockout) {
                for (WcTournament.WcMatch m : r.ties) if (m.played) out.add(m);
            }
        }
        return out;
    }

    private static void bump(Map<String, Tally> map, String name, String nationKey) {
        String key = name + "|" + nationKey;
        Tally tly = map.get(key);
        if (tly == null) {
            tly = new Tally(name, nationKey);
            map.put(key, tly);
        }
        tly.count++;
    }

    private static List<Tally> ranked(Map<String, Tally> map) {
        List<Tally> list = new ArrayList<>(map.values());
        Collections.sort(list, (a, b) -> b.count - a.count);
        if (list.size() > MAX_ROWS) list = list.subList(0, MAX_ROWS);
        return list;
    }

    private View buildRow(int rank, Tally tly, boolean goals) {
        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setBackgroundResource(R.drawable.bg_pill_unselected);
        int padH = dp(12), padV = dp(10);
        row.setPadding(padH, padV, padH, padV);
        LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rlp.bottomMargin = dp(6);
        row.setLayoutParams(rlp);

        TextView pos = new TextView(requireContext());
        pos.setText(rank <= 3 ? medal(rank) : (rank + "."));
        pos.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
        pos.setTextSize(14f);
        pos.setMinWidth(dp(28));
        row.addView(pos);

        ImageView crest = new ImageView(requireContext());
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(dp(22), dp(22));
        clp.leftMargin = dp(6);
        crest.setLayoutParams(clp);
        WorldCupData.Nation n = WorldCupData.byKey(tly.nationKey);
        Glide.with(this)
            .load(LogoAssets.assetUri(n != null ? n.logo : ""))
            .placeholder(new InitialsBadgeDrawable(nationName(tly.nationKey)))
            .into(crest);
        row.addView(crest);

        TextView name = new TextView(requireContext());
        name.setText(tly.name);
        name.setMaxLines(1);
        name.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary));
        name.setTextSize(15f);
        if (rank == 1) name.setTypeface(name.getTypeface(), Typeface.BOLD);
        LinearLayout.LayoutParams nlp = new LinearLayout.LayoutParams(
            0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        nlp.leftMargin = dp(10);
        name.setLayoutParams(nlp);
        row.addView(name);

        TextView count = new TextView(requireContext());
        String unit = getString(goals ? R.string.wc_stats_goals : R.string.wc_stats_assists);
        count.setText(tly.count + " " + unit);
        count.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary));
        count.setTextSize(15f);
        count.setTypeface(count.getTypeface(), Typeface.BOLD);
        row.addView(count);

        return row;
    }

    private static String medal(int rank) {
        switch (rank) {
            case 1:  return "🥇";
            case 2:  return "🥈";
            default: return "🥉";
        }
    }

    private static String nationName(String key) {
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

    /** A player's running goal or assist count, scoped to their nation. */
    private static class Tally {
        final String name;
        final String nationKey;
        int count;

        Tally(String name, String nationKey) {
            this.name = name;
            this.nationKey = nationKey;
        }
    }
}
