package hr.fipu.footmash.ui.worldcup;

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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import hr.fipu.footmash.R;
import hr.fipu.footmash.databinding.FragmentWcNationPickerBinding;
import hr.fipu.footmash.db.LogoAssets;
import hr.fipu.footmash.ui.util.InitialsBadgeDrawable;
import hr.fipu.footmash.worldcup.WorldCupData;

/** Lets the player choose the nation they will manage through the tournament. */
public class WorldCupNationPickerFragment extends Fragment {

    private FragmentWcNationPickerBinding binding;
    private WorldCupViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentWcNationPickerBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(WorldCupViewModel.class);
        viewModel.getCounts().observe(getViewLifecycleOwner(), this::buildRows);
        viewModel.loadCounts();
    }

    private void buildRows(Map<String, Integer> counts) {
        binding.containerNations.removeAllViews();
        List<WorldCupData.Nation> nations = new ArrayList<>(WorldCupData.all());
        // Deepest real-player pools first so a playable nation is easy to find.
        nations.sort((a, b) -> {
            int ca = counts.getOrDefault(a.key, 0), cb = counts.getOrDefault(b.key, 0);
            if (ca != cb) return Integer.compare(cb, ca);
            return a.name.compareTo(b.name);
        });
        for (WorldCupData.Nation n : nations) {
            binding.containerNations.addView(buildRow(n, counts.getOrDefault(n.key, 0)));
        }
    }

    private View buildRow(WorldCupData.Nation n, int realCount) {
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
        crest.setLayoutParams(new LinearLayout.LayoutParams(dp(32), dp(32)));
        Glide.with(this)
            .load(LogoAssets.assetUri(n.logo))
            .placeholder(new InitialsBadgeDrawable(n.name))
            .into(crest);
        row.addView(crest);

        LinearLayout texts = new LinearLayout(requireContext());
        texts.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(
            0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        tlp.leftMargin = dp(14);
        texts.setLayoutParams(tlp);

        TextView name = new TextView(requireContext());
        name.setText(n.name);
        name.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary));
        name.setTextSize(16f);
        name.setTypeface(name.getTypeface(), android.graphics.Typeface.BOLD);
        texts.addView(name);

        TextView sub = new TextView(requireContext());
        sub.setText(realCount + " " + getString(R.string.wc_players_available)
            + "  ·  OVR " + n.baseline);
        sub.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
        sub.setTextSize(12f);
        texts.addView(sub);

        row.addView(texts);

        row.setOnClickListener(v -> {
            Bundle args = new Bundle();
            args.putString("nationKey", n.key);
            Navigation.findNavController(v).navigate(R.id.action_nationPicker_to_squad, args);
        });
        return row;
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
