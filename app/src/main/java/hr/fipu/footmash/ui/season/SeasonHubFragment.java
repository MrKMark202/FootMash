package hr.fipu.footmash.ui.season;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import java.util.List;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import androidx.palette.graphics.Palette;
import androidx.core.graphics.ColorUtils;

import hr.fipu.footmash.R;
import hr.fipu.footmash.databinding.FragmentSeasonHubBinding;
import hr.fipu.footmash.model.Fixture;
import hr.fipu.footmash.model.SeasonStanding;
import hr.fipu.footmash.model.UserClub;

public class SeasonHubFragment extends Fragment {

    private FragmentSeasonHubBinding binding;
    private SeasonHubViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentSeasonHubBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        int clubId = getArguments() != null ? getArguments().getInt("clubId", -1) : -1;
        if (clubId == -1) return;

        viewModel = new ViewModelProvider(this).get(SeasonHubViewModel.class);
        viewModel.init(clubId);

        observeViewModel();

        binding.btnSimulate.setOnClickListener(v -> {
            Integer matchday = viewModel.getNextMatchday().getValue();
            Bundle args = new Bundle();
            args.putInt("clubId", clubId);
            if (matchday == null || matchday == 0) {
                Navigation.findNavController(v).navigate(R.id.action_seasonHub_to_summary, args);
            } else {
                args.putInt("matchday", matchday);
                Navigation.findNavController(v).navigate(R.id.action_seasonHub_to_matchday, args);
            }
        });
    }

    private void observeViewModel() {
        viewModel.getClub().observe(getViewLifecycleOwner(), this::bindClub);
        viewModel.getNextUserFixture().observe(getViewLifecycleOwner(), this::bindNextMatch);
        viewModel.getNextMatchday().observe(getViewLifecycleOwner(), this::bindMatchday);
        viewModel.getStandings().observe(getViewLifecycleOwner(), this::bindStandings);
    }

    private void bindClub(UserClub club) {
        if (club == null) return;
        binding.textClubName.setText(club.getClubName());
        binding.textLeague.setText(club.getLeagueName() + " · 2024/25");
        double budgetM = club.getBudget() / 1_000_000.0;
        binding.textBudget.setText(String.format("Preostali budžet: €%.1fM", budgetM));
    }

    private void bindNextMatch(Fixture f) {
        if (f == null) {
            binding.cardNextMatch.setVisibility(View.GONE);
            return;
        }
        binding.cardNextMatch.setVisibility(View.VISIBLE);
        binding.textHomeTeam.setText(f.getHomeTeamName());
        binding.textAwayTeam.setText(f.getAwayTeamName());
    }

    private void bindMatchday(Integer matchday) {
        if (matchday == null || matchday == 0) {
            binding.textMatchdayLabel.setText("Sezona završena!");
            binding.btnSimulate.setText("Pogledaj završnicu");
        } else {
            binding.textMatchdayLabel.setText("Kolo " + matchday);
            binding.btnSimulate.setText("Simuliraj kolo " + matchday);
        }
    }

    private void bindStandings(List<SeasonStanding> list) {
        LinearLayout container = binding.standingsContainer;
        container.removeAllViews();
        if (list == null || list.isEmpty()) return;

        for (int i = 0; i < list.size(); i++) {
            SeasonStanding s = list.get(i);
            View row = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_standing_row, container, false);

            ((TextView) row.findViewById(R.id.text_rank)).setText(String.valueOf(i + 1));
            ((TextView) row.findViewById(R.id.text_team_name)).setText(s.getTeamName());
            ((TextView) row.findViewById(R.id.text_played)).setText(String.valueOf(s.getPlayed()));
            ((TextView) row.findViewById(R.id.text_won)).setText(String.valueOf(s.getWon()));
            ((TextView) row.findViewById(R.id.text_drawn)).setText(String.valueOf(s.getDrawn()));
            ((TextView) row.findViewById(R.id.text_lost)).setText(String.valueOf(s.getLost()));
            ((TextView) row.findViewById(R.id.text_pts)).setText(String.valueOf(s.getPoints()));

            if (s.isUserTeam()) {
                ((TextView) row.findViewById(R.id.text_team_name))
                    .setTextColor(ContextCompat.getColor(requireContext(), R.color.accent_blue));
                ((TextView) row.findViewById(R.id.text_team_name)).setTypeface(null, android.graphics.Typeface.BOLD);
            }

            String bUrl = s.getBadgeUrl();
            if (bUrl == null || bUrl.isEmpty()) {
                String kebabName = s.getTeamName().toLowerCase()
                    .replace(" & ", "-")
                    .replace("&", "")
                    .replace(" ", "-")
                    .replace(".", "");
                bUrl = "https://apiv2.allsportsapi.com/logo/" + s.getTeamId() + "_" + kebabName + ".jpg";
            }

            Glide.with(this)
                .asBitmap()
                .load(bUrl)
                .into(new CustomTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                        Palette.from(resource).generate(palette -> {
                            if (palette != null && isAdded()) {
                                int defaultColor = ContextCompat.getColor(requireContext(), R.color.background);
                                int dominantColor = palette.getDominantColor(defaultColor);
                                int alpha = s.isUserTeam() ? 100 : 40; 
                                int transparentColor = ColorUtils.setAlphaComponent(dominantColor, alpha); 
                                row.setBackgroundColor(transparentColor);
                            }
                        });
                    }
                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {}
                });

            container.addView(row);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
