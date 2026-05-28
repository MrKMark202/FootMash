package hr.fipu.footmash.ui.ailab.create;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import hr.fipu.footmash.model.RealTeam;

/**
 * Picks the 3 random clubs that "offer" the user's newly created player.
 *
 * <p>Teams are sorted descending by their effective overall rating (computed
 * upstream — this class only consumes a ranked list) and split into three tiers
 * by percentile:
 *
 * <ul>
 *   <li><b>Elite</b> — top 30% of the league</li>
 *   <li><b>Mid</b>   — middle 35%</li>
 *   <li><b>Lower</b> — bottom 35%</li>
 * </ul>
 *
 * The player is bucketed by overall (≥80 elite, 70–79 mid, &lt;70 lower) and the
 * three offers are drawn from that bucket first, then from adjacent buckets if
 * the primary bucket cannot provide three.
 *
 * <p>All logic is pure (no Android, no DB) and seedable via the {@code Random}
 * parameter — verified by {@code ClubOfferEngineTest}.
 */
public final class ClubOfferEngine {

    public static final int OFFER_COUNT = 3;

    public enum Tier { ELITE, MID, LOWER }

    /** League team plus its computed effective overall, used to drive bucketing. */
    public static class RankedTeam {
        public final RealTeam team;
        public final int effectiveOverall;

        public RankedTeam(RealTeam team, int effectiveOverall) {
            this.team = team;
            this.effectiveOverall = effectiveOverall;
        }
    }

    private ClubOfferEngine() {}

    /** Player OVR → which tier of clubs to draw from first. */
    public static Tier tierForPlayer(int playerOverall) {
        if (playerOverall >= 80) return Tier.ELITE;
        if (playerOverall >= 70) return Tier.MID;
        return Tier.LOWER;
    }

    /**
     * Picks up to {@link #OFFER_COUNT} clubs as offers. Caller provides teams
     * sorted descending by effective overall and a seeded {@link Random}.
     */
    public static List<RankedTeam> pickOffers(List<RankedTeam> teamsRankedDesc,
                                              int playerOverall,
                                              Random rng) {
        int n = teamsRankedDesc.size();
        if (n == 0) return Collections.emptyList();

        // Tier split: 30% elite, 35% mid, 35% lower. Clamp so each non-empty
        // tier has at least one team when the league is small.
        int eliteEnd = clamp((int) Math.ceil(n * 0.30), 1, n);
        int midEnd   = clamp((int) Math.ceil(n * 0.65), eliteEnd, n);

        List<RankedTeam> elite = new ArrayList<>(teamsRankedDesc.subList(0, eliteEnd));
        List<RankedTeam> mid   = new ArrayList<>(teamsRankedDesc.subList(eliteEnd, midEnd));
        List<RankedTeam> lower = new ArrayList<>(teamsRankedDesc.subList(midEnd, n));

        Tier tier = tierForPlayer(playerOverall);
        List<List<RankedTeam>> drawOrder;
        switch (tier) {
            case ELITE: drawOrder = Arrays.asList(elite, mid, lower); break;
            case MID:   drawOrder = Arrays.asList(mid, elite, lower); break;
            default:    drawOrder = Arrays.asList(lower, mid, elite); break;
        }

        List<RankedTeam> result = new ArrayList<>();
        for (List<RankedTeam> bucket : drawOrder) {
            if (result.size() >= OFFER_COUNT) break;
            List<RankedTeam> pool = new ArrayList<>(bucket);
            Collections.shuffle(pool, rng);
            for (RankedTeam t : pool) {
                if (result.size() >= OFFER_COUNT) break;
                result.add(t);
            }
        }
        return result;
    }

    private static int clamp(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}
