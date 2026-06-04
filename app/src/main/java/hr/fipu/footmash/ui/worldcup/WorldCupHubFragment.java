package hr.fipu.footmash.ui.worldcup;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.List;

import hr.fipu.footmash.R;
import hr.fipu.footmash.databinding.FragmentWcHubBinding;
import hr.fipu.footmash.worldcup.WcTournament;
import hr.fipu.footmash.worldcup.WorldCupData;

/** Tournament dashboard: shows the current stage and the user's next match, and
 *  advances the tournament one matchday/round per "Simuliraj" press. */
public class WorldCupHubFragment extends Fragment {

    private final String GEMINI_API_KEY = hr.fipu.footmash.BuildConfig.GEMINI_API_KEY;

    private FragmentWcHubBinding binding;
    private WorldCupViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentWcHubBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(WorldCupViewModel.class);

        binding.buttonSimulate.setOnClickListener(v -> viewModel.simulate(GEMINI_API_KEY));
        binding.buttonGroups.setOnClickListener(v ->
            Navigation.findNavController(v).navigate(R.id.action_hub_to_groups));
        binding.buttonBracket.setOnClickListener(v ->
            Navigation.findNavController(v).navigate(R.id.action_hub_to_bracket));
        binding.buttonChampion.setOnClickListener(v ->
            Navigation.findNavController(v).navigate(R.id.action_hub_to_champion));

        viewModel.getState().observe(getViewLifecycleOwner(), this::bind);
        viewModel.getBusy().observe(getViewLifecycleOwner(), busy -> {
            boolean b = Boolean.TRUE.equals(busy);
            binding.progressSim.setVisibility(b ? View.VISIBLE : View.GONE);
            binding.buttonSimulate.setEnabled(!b);
            binding.buttonSimulate.setText(b ? R.string.wc_simulating : R.string.wc_simulate);
        });
        viewModel.getLastResults().observe(getViewLifecycleOwner(), this::showResults);
        viewModel.refresh();
    }

    private void bind(@Nullable WcTournament t) {
        if (t == null) return;
        binding.textStage.setText(stageLabel(t));
        boolean done = WcTournament.STAGE_DONE.equals(t.stage);
        binding.buttonChampion.setVisibility(done ? View.VISIBLE : View.GONE);
        binding.buttonSimulate.setVisibility(done ? View.GONE : View.VISIBLE);
        binding.cardNextMatch.setVisibility(done ? View.GONE : View.VISIBLE);

        WcTournament.WcMatch next = nextUserMatch(t);
        if (next != null) {
            binding.textNextMatch.setText(name(next.homeKey) + "  vs  " + name(next.awayKey));
            binding.textNextMatchSub.setText(roundName(t.stage, t));
        } else if (!done) {
            binding.textNextMatch.setText(R.string.wc_eliminated);
            binding.textNextMatchSub.setText(roundName(t.stage, t));
        }
    }

    /** The user's upcoming unplayed match in the active stage, or null. */
    private WcTournament.WcMatch nextUserMatch(WcTournament t) {
        if (WcTournament.STAGE_GROUP.equals(t.stage)) {
            for (WcTournament.WcMatch m : t.groupFixtures) {
                if (m.matchday == t.groupMatchday && !m.played && involvesUser(t, m)) return m;
            }
            return null;
        }
        for (WcTournament.WcRound r : t.knockout) {
            if (r.played) continue;
            for (WcTournament.WcMatch m : r.ties) {
                if (!m.played && involvesUser(t, m)) return m;
            }
            break;
        }
        return null;
    }

    private boolean involvesUser(WcTournament t, WcTournament.WcMatch m) {
        return m.homeKey.equals(t.userNationKey) || m.awayKey.equals(t.userNationKey);
    }

    private String stageLabel(WcTournament t) {
        if (WcTournament.STAGE_GROUP.equals(t.stage)) {
            return getString(R.string.wc_group_stage, Math.min(t.groupMatchday, 3));
        }
        if (WcTournament.STAGE_DONE.equals(t.stage)) return getString(R.string.wc_champion);
        return roundName(t.stage, t);
    }

    private String roundName(String stage, WcTournament t) {
        switch (stage) {
            case WcTournament.STAGE_R32:  return getString(R.string.wc_round_r32);
            case WcTournament.STAGE_R16:  return getString(R.string.wc_round_r16);
            case WcTournament.STAGE_QF:   return getString(R.string.wc_round_qf);
            case WcTournament.STAGE_SF:   return getString(R.string.wc_round_sf);
            case WcTournament.STAGE_FINAL:return getString(R.string.wc_round_final);
            case WcTournament.STAGE_GROUP:return getString(R.string.wc_group_stage,
                Math.min(t.groupMatchday, 3));
            default: return "";
        }
    }

    private void showResults(List<WcTournament.WcMatch> matches) {
        if (matches == null || matches.isEmpty() || !isAdded()) return;
        WcTournament t = viewModel.getState().getValue();
        String userKey = t != null ? t.userNationKey : null;
        StringBuilder sb = new StringBuilder();
        for (WcTournament.WcMatch m : matches) {
            sb.append(name(m.homeKey)).append("  ")
              .append(m.homeGoals).append(" – ").append(m.awayGoals)
              .append("  ").append(name(m.awayKey));
            if (m.penalties) {
                sb.append("  (").append(m.homePens).append("-").append(m.awayPens).append(" pen)");
            }
            sb.append("\n");
            boolean userMatch = userKey != null
                && (userKey.equals(m.homeKey) || userKey.equals(m.awayKey));
            if (userMatch && m.scorers != null && !m.scorers.isEmpty()) {
                for (WcTournament.WcScorer s : m.scorers) {
                    sb.append("   ⚽ ").append(s.minute).append("' ").append(s.name).append("\n");
                }
            }
            sb.append("\n");
        }
        new MaterialAlertDialogBuilder(requireContext(), R.style.FootMashDialog)
            .setTitle(R.string.wc_results_title)
            .setMessage(sb.toString().trim())
            .setPositiveButton("OK", null)
            .show();
    }

    private static String name(String key) {
        WorldCupData.Nation n = WorldCupData.byKey(key);
        return n != null ? n.name : (key == null ? "—" : key);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
