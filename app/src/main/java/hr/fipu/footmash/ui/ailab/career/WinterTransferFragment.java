package hr.fipu.footmash.ui.ailab.career;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.bumptech.glide.Glide;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import hr.fipu.footmash.FootMashApp;
import hr.fipu.footmash.R;
import hr.fipu.footmash.databinding.FragmentTransferOffersBinding;
import hr.fipu.footmash.db.AppDatabase;
import hr.fipu.footmash.model.CustomPlayer;
import hr.fipu.footmash.model.RealPlayer;
import hr.fipu.footmash.model.RealTeam;
import hr.fipu.footmash.season.TraitEngine;
import hr.fipu.footmash.ui.ailab.create.ClubOfferEngine;
import hr.fipu.footmash.ui.util.InitialsBadgeDrawable;

/**
 * Winter transfer window — fires mid-season after the autumn half. Shows one
 * same-league offer (mid-season interest is thinner than summer's pool) plus
 * a "stay" outlined button. Either choice flips the player's
 * {@code seasonHalfState} from 1 to 2 so the career hub can offer the spring
 * sim next.
 *
 * <p>Re-uses {@code fragment_transfer_offers.xml} because the visual shape is
 * identical to the summer window; only the intro copy and offer count differ.
 */
public class WinterTransferFragment extends Fragment {

    public static final String ARG_PLAYER_ID = "playerId";

    private FragmentTransferOffersBinding binding;
    private int playerId;
    private CustomPlayer player;
    private List<ClubOfferEngine.RankedTeam> offers = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentTransferOffersBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        playerId = getArguments() != null ? getArguments().getInt(ARG_PLAYER_ID, 0) : 0;
        if (playerId <= 0) {
            Toast.makeText(requireContext(), "Nedostaje ID igrača", Toast.LENGTH_SHORT).show();
            Navigation.findNavController(view).popBackStack();
            return;
        }

        binding.btnStay.setText("Ostani do kraja sezone");
        binding.btnStay.setOnClickListener(v -> stay());
        loadOffer();
    }

    private void loadOffer() {
        AppDatabase db = FootMashApp.container(requireContext()).database();
        new Thread(() -> {
            CustomPlayer p = db.customPlayerDao().getPlayerByIdSync(playerId);
            if (p == null) return;

            // Suitors can come from any league, not just the player's own.
            List<RealTeam> candidateTeams = db.realTeamDao().getAllTeamsSync();
            List<ClubOfferEngine.RankedTeam> ranked = new ArrayList<>();
            for (RealTeam t : candidateTeams) {
                if (t.getId() == p.getTargetTeamId()) continue;   // current club excluded
                List<RealPlayer> roster = db.realPlayerDao().getPlayersByTeamSync(t.getId());
                int eff = TraitEngine.effectiveRating(TraitEngine.bestXi(roster));
                ranked.add(new ClubOfferEngine.RankedTeam(t, eff));
            }
            Collections.sort(ranked,
                (a, b) -> Integer.compare(b.effectiveOverall, a.effectiveOverall));

            // Engine returns up to 3 weighted by player OVR; winter mid-season
            // interest is thinner so we take the single strongest pick.
            List<ClubOfferEngine.RankedTeam> drawn =
                ClubOfferEngine.pickOffers(ranked, p.getOverall(), new Random());
            List<ClubOfferEngine.RankedTeam> singleton = drawn.isEmpty()
                ? Collections.emptyList()
                : drawn.subList(0, 1);

            if (!isAdded()) return;
            requireActivity().runOnUiThread(() -> {
                if (binding == null) return;
                player = p;
                offers = singleton;
                binding.textIntro.setText("Zimski prelazni rok • "
                    + p.getFullName() + " (OVR " + p.getOverall() + ") • "
                    + p.getTargetTeamName());
                binding.progressOffers.setVisibility(View.GONE);
                renderOffer();
            });
        }).start();
    }

    private void renderOffer() {
        binding.containerOffers.removeAllViews();
        if (offers == null || offers.isEmpty()) {
            // No suitor materialised; auto-advance to the spring sim.
            stay();
            return;
        }
        ClubOfferEngine.RankedTeam offer = offers.get(0);
        View card = LayoutInflater.from(requireContext())
            .inflate(R.layout.item_club_offer, binding.containerOffers, false);
        bindOfferCard(card, offer);
        binding.containerOffers.addView(card);
    }

    private void bindOfferCard(View card, ClubOfferEngine.RankedTeam offer) {
        TextView nameView   = card.findViewById(R.id.textClubName);
        TextView tierView   = card.findViewById(R.id.textClubTier);
        TextView ovrView    = card.findViewById(R.id.textClubOverall);
        ImageView badgeView = card.findViewById(R.id.imageClubBadge);
        MaterialCardView mcv = (MaterialCardView) card;

        nameView.setText(offer.team.getName());
        ovrView.setText(String.valueOf(offer.effectiveOverall));
        tierView.setText(tierLabel(offer.effectiveOverall, player.getOverall()));

        Glide.with(this)
            .load(offer.team.getBadgeUrl())
            .placeholder(new InitialsBadgeDrawable(offer.team.getName()))
            .into(badgeView);

        mcv.setStrokeColor(getResources().getColor(R.color.divider, null));
        mcv.setStrokeWidth((int) (getResources().getDisplayMetrics().density));

        card.setOnClickListener(v -> accept(offer));
    }

    private void accept(ClubOfferEngine.RankedTeam offer) {
        if (player == null) return;
        player.setTargetTeamId(offer.team.getId());
        player.setTargetTeamName(offer.team.getName());
        player.setTargetTeamLogo(offer.team.getBadgeUrl());
        // Mid-season transfer: reset the seasons-at-club counter so the
        // summer window doesn't trigger against the new club until they've
        // had two full seasons together.
        player.setTransferDismissedAt(-1);
        // Spring half is played at the new club.
        player.setSeasonHalfState(2);
        persistAndPop();
    }

    private void stay() {
        if (player == null) return;
        player.setSeasonHalfState(2);
        persistAndPop();
    }

    private void persistAndPop() {
        AppDatabase db = FootMashApp.container(requireContext()).database();
        new Thread(() -> {
            db.customPlayerDao().update(player);
            if (!isAdded()) return;
            requireActivity().runOnUiThread(() ->
                Navigation.findNavController(requireView()).popBackStack());
        }).start();
    }

    /** Same wording as the summer window for visual continuity. */
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
