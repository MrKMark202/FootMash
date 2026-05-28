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
 * Transfer window: shows two same-league offers weighted to the player's
 * current OVR (post-growth), plus a "stay" outlined button.
 *
 * <p>Picking an offer mutates the {@link CustomPlayer}'s targetTeam* fields
 * and resets {@code transferDismissedAt} so the new club starts a fresh
 * 2-season cycle. Staying records the current seasons-at-club into
 * {@code transferDismissedAt} so the user can sim two more seasons before
 * the next window opens.
 */
public class TransferOffersFragment extends Fragment {

    public static final String ARG_PLAYER_ID = "playerId";

    private FragmentTransferOffersBinding binding;
    private int playerId;
    private CustomPlayer player;
    /** Same as the create-wizard's source list — kept in sync deliberately. */
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

        binding.btnStay.setOnClickListener(v -> stayAtCurrentClub());
        loadAndPickOffers();
    }

    private void loadAndPickOffers() {
        AppDatabase db = FootMashApp.container(requireContext()).database();
        new Thread(() -> {
            CustomPlayer p = db.customPlayerDao().getPlayerByIdSync(playerId);
            if (p == null) return;

            List<RealTeam> leagueTeams =
                db.realTeamDao().getTeamsByLeagueSync(p.getTargetLeagueId());
            List<ClubOfferEngine.RankedTeam> ranked = new ArrayList<>();
            for (RealTeam t : leagueTeams) {
                // Exclude the current club from the offer pool — these are
                // suitors trying to poach, not the same place.
                if (t.getId() == p.getTargetTeamId()) continue;
                List<RealPlayer> roster = db.realPlayerDao().getPlayersByTeamSync(t.getId());
                int eff = TraitEngine.effectiveRating(TraitEngine.bestXi(roster));
                ranked.add(new ClubOfferEngine.RankedTeam(t, eff));
            }
            Collections.sort(ranked,
                (a, b) -> Integer.compare(b.effectiveOverall, a.effectiveOverall));

            // Engine returns 3; we take the first 2.
            List<ClubOfferEngine.RankedTeam> drawn =
                ClubOfferEngine.pickOffers(ranked, p.getOverall(), new Random());
            List<ClubOfferEngine.RankedTeam> pair = drawn.size() > 2
                ? drawn.subList(0, 2) : drawn;

            if (!isAdded()) return;
            requireActivity().runOnUiThread(() -> {
                if (binding == null) return;
                player = p;
                offers = pair;
                binding.textIntro.setText(p.getFullName() + " (OVR " + p.getOverall()
                    + ") • " + p.getTargetTeamName() + " ga želi zadržati, "
                    + "ali drugi klubovi su poslali ponude.");
                binding.progressOffers.setVisibility(View.GONE);
                renderOffers();
            });
        }).start();
    }

    private void renderOffers() {
        binding.containerOffers.removeAllViews();
        if (offers == null || offers.isEmpty()) {
            Toast.makeText(requireContext(), "Trenutno nema ponuda", Toast.LENGTH_SHORT).show();
            return;
        }
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        for (ClubOfferEngine.RankedTeam offer : offers) {
            View card = inflater.inflate(R.layout.item_club_offer,
                binding.containerOffers, false);
            bindOfferCard(card, offer);
            binding.containerOffers.addView(card);
        }
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

        card.setOnClickListener(v -> acceptOffer(offer));
    }

    private void acceptOffer(ClubOfferEngine.RankedTeam offer) {
        if (player == null) return;
        player.setTargetTeamId(offer.team.getId());
        player.setTargetTeamName(offer.team.getName());
        player.setTargetTeamLogo(offer.team.getBadgeUrl());
        player.setTransferDismissedAt(-1); // fresh 2-season clock at new club

        AppDatabase db = FootMashApp.container(requireContext()).database();
        new Thread(() -> {
            db.customPlayerDao().update(player);
            if (!isAdded()) return;
            requireActivity().runOnUiThread(() ->
                Navigation.findNavController(requireView()).popBackStack());
        }).start();
    }

    private void stayAtCurrentClub() {
        if (player == null) return;
        // Snapshot the seasons-at-club count so the next window opens 2 sims
        // from now, not on the immediate next return to the hub.
        int n = countSeasonsAtClub();
        player.setTransferDismissedAt(n);

        AppDatabase db = FootMashApp.container(requireContext()).database();
        new Thread(() -> {
            db.customPlayerDao().update(player);
            if (!isAdded()) return;
            requireActivity().runOnUiThread(() ->
                Navigation.findNavController(requireView()).popBackStack());
        }).start();
    }

    private int countSeasonsAtClub() {
        AppDatabase db = FootMashApp.container(requireContext()).database();
        return db.playerCareerSeasonDao()
            .countSeasonsAtClub(player.getId(), player.getTargetTeamId());
    }

    /** Mirrors PlayerClubOffersFragment.tierLabel so the wording matches. */
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
