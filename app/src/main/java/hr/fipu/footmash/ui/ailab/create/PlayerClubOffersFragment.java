package hr.fipu.footmash.ui.ailab.create;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import hr.fipu.footmash.FootMashApp;
import hr.fipu.footmash.R;
import hr.fipu.footmash.databinding.FragmentPlayerClubOffersBinding;
import hr.fipu.footmash.db.AppDatabase;
import hr.fipu.footmash.model.RealPlayer;
import hr.fipu.footmash.model.RealTeam;
import hr.fipu.footmash.season.TraitEngine;
import hr.fipu.footmash.ui.util.InitialsBadgeDrawable;

/**
 * Step 4 of the player creation wizard: pick a league and choose one of three
 * randomly-drawn club offers, weighted by player OVR via {@link ClubOfferEngine}.
 *
 * <p>League selection kicks off a background load: the league's 20 teams plus
 * each team's full roster, used to compute every team's effective overall
 * (avg overall + synergy via {@link TraitEngine#effectiveRating}). The engine
 * then picks three offers from the tier matching the player's OVR.
 */
public class PlayerClubOffersFragment extends Fragment {

    private static class LeagueOption {
        final int id;
        final String name;
        LeagueOption(int id, String name) { this.id = id; this.name = name; }
        @NonNull @Override public String toString() { return name; }
    }

    /** Mirrors LeaguePickerFragment's 5 seeded leagues. */
    private static final List<LeagueOption> LEAGUES = Arrays.asList(
        new LeagueOption(177, "Premier League"),
        new LeagueOption(302, "La Liga"),
        new LeagueOption(78,  "Bundesliga"),
        new LeagueOption(207, "Serie A"),
        new LeagueOption(168, "Ligue 1")
    );

    private FragmentPlayerClubOffersBinding binding;
    private PlayerCreationViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentPlayerClubOffersBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireActivity()).get(PlayerCreationViewModel.class);

        bindLeagueDropdown();
        bindOffersObservers();
        bindNextButton();
        restoreFromViewModel();
    }

    private void bindLeagueDropdown() {
        ArrayAdapter<LeagueOption> adapter = new ArrayAdapter<>(
            requireContext(), android.R.layout.simple_list_item_1, LEAGUES);
        binding.autoCompleteLeague.setAdapter(adapter);

        binding.autoCompleteLeague.setOnItemClickListener((parent, v, pos, id) -> {
            LeagueOption opt = (LeagueOption) parent.getItemAtPosition(pos);
            viewModel.setSelectedLeague(opt.id, opt.name);
            loadOffersForLeague(opt.id);
        });
    }

    private void bindOffersObservers() {
        viewModel.getCurrentOffers().observe(getViewLifecycleOwner(), this::renderOffers);
        viewModel.getSelectedClub().observe(getViewLifecycleOwner(), club ->
            binding.btnNext.setEnabled(club != null));
    }

    private void bindNextButton() {
        binding.btnNext.setOnClickListener(v -> {
            if (!viewModel.isStep4Valid()) return;
            // Step 5 (signed) navigation is wired in the next commit.
        });
    }

    private void restoreFromViewModel() {
        String leagueName = viewModel.getSelectedLeagueName().getValue();
        Integer leagueId  = viewModel.getSelectedLeagueId().getValue();
        if (leagueName != null && !leagueName.isEmpty()) {
            binding.autoCompleteLeague.setText(leagueName, false);
        }
        List<ClubOfferEngine.RankedTeam> existing = viewModel.getCurrentOffers().getValue();
        if (existing != null && !existing.isEmpty()) {
            renderOffers(existing);
        } else if (leagueId != null) {
            loadOffersForLeague(leagueId);
        }
    }

    private void loadOffersForLeague(int leagueId) {
        binding.progressOffers.setVisibility(View.VISIBLE);
        binding.textOffersHint.setVisibility(View.GONE);
        binding.containerOffers.removeAllViews();

        AppDatabase db = FootMashApp.container(requireContext()).database();
        int playerOverall = viewModel.currentOverall();

        new Thread(() -> {
            List<RealTeam> teams = db.realTeamDao().getTeamsByLeagueSync(leagueId);
            List<ClubOfferEngine.RankedTeam> ranked = new ArrayList<>();
            for (RealTeam t : teams) {
                List<RealPlayer> roster = db.realPlayerDao().getPlayersByTeamSync(t.getId());
                int eff = TraitEngine.effectiveRating(TraitEngine.bestXi(roster));
                ranked.add(new ClubOfferEngine.RankedTeam(t, eff));
            }
            Collections.sort(ranked, (a, b) -> Integer.compare(b.effectiveOverall, a.effectiveOverall));

            List<ClubOfferEngine.RankedTeam> offers = ClubOfferEngine.pickOffers(
                ranked, playerOverall, new Random());

            if (!isAdded()) return;
            requireActivity().runOnUiThread(() -> {
                binding.progressOffers.setVisibility(View.GONE);
                viewModel.setCurrentOffers(offers);
            });
        }).start();
    }

    private void renderOffers(List<ClubOfferEngine.RankedTeam> offers) {
        binding.containerOffers.removeAllViews();
        if (offers == null || offers.isEmpty()) {
            binding.textOffersHint.setVisibility(View.VISIBLE);
            return;
        }
        binding.textOffersHint.setVisibility(View.GONE);
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        int playerOverall = viewModel.currentOverall();
        for (ClubOfferEngine.RankedTeam offer : offers) {
            View card = inflater.inflate(R.layout.item_club_offer, binding.containerOffers, false);
            bindOfferCard(card, offer, playerOverall);
            binding.containerOffers.addView(card);
        }
    }

    private void bindOfferCard(View card, ClubOfferEngine.RankedTeam offer, int playerOverall) {
        TextView nameView    = card.findViewById(R.id.textClubName);
        TextView tierView    = card.findViewById(R.id.textClubTier);
        TextView ovrView     = card.findViewById(R.id.textClubOverall);
        ImageView badgeView  = card.findViewById(R.id.imageClubBadge);
        MaterialCardView mcv = (MaterialCardView) card;

        nameView.setText(offer.team.getName());
        ovrView.setText(String.valueOf(offer.effectiveOverall));
        tierView.setText(tierLabel(offer.effectiveOverall, playerOverall));

        Glide.with(this)
            .load(offer.team.getBadgeUrl())
            .placeholder(new InitialsBadgeDrawable(offer.team.getName()))
            .into(badgeView);

        card.setOnClickListener(v -> selectOffer(offer, mcv));
        applySelectedStroke(mcv, isSelected(offer));
    }

    private void selectOffer(ClubOfferEngine.RankedTeam offer, MaterialCardView clickedCard) {
        viewModel.selectClub(offer.team);
        // Snap stroke colours: only the chosen card gets the accent border.
        for (int i = 0; i < binding.containerOffers.getChildCount(); i++) {
            View child = binding.containerOffers.getChildAt(i);
            if (child instanceof MaterialCardView) {
                applySelectedStroke((MaterialCardView) child, child == clickedCard);
            }
        }
    }

    private boolean isSelected(ClubOfferEngine.RankedTeam offer) {
        RealTeam sel = viewModel.getSelectedClub().getValue();
        return sel != null && sel.getId() == offer.team.getId();
    }

    private void applySelectedStroke(MaterialCardView card, boolean selected) {
        card.setStrokeColor(getResources().getColor(
            selected ? R.color.accent_blue : R.color.divider, null));
        card.setStrokeWidth((int) (getResources().getDisplayMetrics().density * (selected ? 2 : 1)));
    }

    private static String tierLabel(int teamOverall, int playerOverall) {
        int delta = teamOverall - playerOverall;
        if (delta >= 8)  return "Vrhunski klub";
        if (delta >= 2)  return "Jak klub";
        if (delta >= -4) return "Konkurentan klub";
        return "Manji klub";
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
