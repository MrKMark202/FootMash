package hr.fipu.footmash.ui.ailab.create;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import hr.fipu.footmash.model.RealTeam;

/**
 * Unit tests for ClubOfferEngine. Tier assignment, offer count, and the
 * bucketing strategy are all checked with deterministic seeded RNGs.
 */
public class ClubOfferEngineTest {

    /** 20-team Premier-League-shaped league, OVR descending from 90 to 71. */
    private static List<ClubOfferEngine.RankedTeam> twentyTeamLeague() {
        List<ClubOfferEngine.RankedTeam> teams = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            RealTeam t = new RealTeam();
            t.setId(i + 1);
            t.setName("Team-" + (i + 1));
            teams.add(new ClubOfferEngine.RankedTeam(t, 90 - i));
        }
        return teams;
    }

    private static Random seeded() { return new Random(12345L); }

    // ─── Tier assignment ─────────────────────────────────────────────────────

    @Test
    public void tierForPlayer_eliteAt80AndAbove() {
        assertEquals(ClubOfferEngine.Tier.ELITE, ClubOfferEngine.tierForPlayer(80));
        assertEquals(ClubOfferEngine.Tier.ELITE, ClubOfferEngine.tierForPlayer(90));
        assertEquals(ClubOfferEngine.Tier.ELITE, ClubOfferEngine.tierForPlayer(99));
    }

    @Test
    public void tierForPlayer_midAt70Through79() {
        assertEquals(ClubOfferEngine.Tier.MID, ClubOfferEngine.tierForPlayer(70));
        assertEquals(ClubOfferEngine.Tier.MID, ClubOfferEngine.tierForPlayer(75));
        assertEquals(ClubOfferEngine.Tier.MID, ClubOfferEngine.tierForPlayer(79));
    }

    @Test
    public void tierForPlayer_lowerBelow70() {
        assertEquals(ClubOfferEngine.Tier.LOWER, ClubOfferEngine.tierForPlayer(69));
        assertEquals(ClubOfferEngine.Tier.LOWER, ClubOfferEngine.tierForPlayer(55));
        assertEquals(ClubOfferEngine.Tier.LOWER, ClubOfferEngine.tierForPlayer(0));
    }

    // ─── Offer count + uniqueness ────────────────────────────────────────────

    @Test
    public void picks3OffersFromAFullLeague() {
        List<ClubOfferEngine.RankedTeam> offers = ClubOfferEngine.pickOffers(
            twentyTeamLeague(), 85, seeded());
        assertEquals(ClubOfferEngine.OFFER_COUNT, offers.size());
    }

    @Test
    public void offersAreUnique() {
        for (int playerOvr : new int[]{90, 75, 60}) {
            List<ClubOfferEngine.RankedTeam> offers = ClubOfferEngine.pickOffers(
                twentyTeamLeague(), playerOvr, new Random(playerOvr));
            Set<Integer> ids = new HashSet<>();
            for (ClubOfferEngine.RankedTeam rt : offers) ids.add(rt.team.getId());
            assertEquals("no duplicate offers (player ovr " + playerOvr + ")",
                offers.size(), ids.size());
        }
    }

    @Test
    public void emptyLeagueReturnsEmpty() {
        List<ClubOfferEngine.RankedTeam> offers = ClubOfferEngine.pickOffers(
            Collections.emptyList(), 80, seeded());
        assertNotNull(offers);
        assertTrue(offers.isEmpty());
    }

    @Test
    public void smallerLeagueReturnsWhatItCan() {
        List<ClubOfferEngine.RankedTeam> tiny = twentyTeamLeague().subList(0, 2);
        List<ClubOfferEngine.RankedTeam> offers = ClubOfferEngine.pickOffers(
            tiny, 80, seeded());
        assertEquals(2, offers.size());
    }

    // ─── Bucketing strategy ──────────────────────────────────────────────────

    @Test
    public void elitePlayerDrawsFromTopOfLeague() {
        // 20 teams, top 30% = first 6 (overall 90..85).
        // An elite player (OVR 90) should get all 3 offers from that bucket.
        List<ClubOfferEngine.RankedTeam> offers = ClubOfferEngine.pickOffers(
            twentyTeamLeague(), 90, seeded());
        for (ClubOfferEngine.RankedTeam rt : offers) {
            assertTrue("elite offers must come from the top tier (>=85), got "
                    + rt.effectiveOverall,
                rt.effectiveOverall >= 85);
        }
    }

    @Test
    public void weakPlayerDrawsFromBottomOfLeague() {
        // Lower tier = bottom 35% of 20 = last 7 teams (overall 77..71).
        // Player OVR 60 → LOWER → all 3 must come from that range.
        List<ClubOfferEngine.RankedTeam> offers = ClubOfferEngine.pickOffers(
            twentyTeamLeague(), 60, seeded());
        for (ClubOfferEngine.RankedTeam rt : offers) {
            assertTrue("weak-player offers must come from the lower tier (<=77), got "
                    + rt.effectiveOverall,
                rt.effectiveOverall <= 77);
        }
    }

    @Test
    public void midPlayerDrawsFromMidTable() {
        // Mid tier = middle 35% = positions 7..13 (overall 84..78).
        List<ClubOfferEngine.RankedTeam> offers = ClubOfferEngine.pickOffers(
            twentyTeamLeague(), 75, seeded());
        for (ClubOfferEngine.RankedTeam rt : offers) {
            assertTrue("mid-player offers must come from the mid tier (78..84), got "
                    + rt.effectiveOverall,
                rt.effectiveOverall >= 78 && rt.effectiveOverall <= 84);
        }
    }

    // ─── Fallback behaviour when primary tier is too small ───────────────────

    @Test
    public void fallsBackToAdjacentTierWhenPrimaryIsTooSmall() {
        // League of 4 teams: top 30% rounds up to 2 elite (90, 85),
        // mid is 1 team (80), lower is 1 team (75).
        // Elite player (OVR 90) wants 3 offers — only 2 elite exist, so the
        // engine spills into mid (and lower if needed).
        List<ClubOfferEngine.RankedTeam> tiny = new ArrayList<>();
        for (int ovr : new int[]{90, 85, 80, 75}) {
            RealTeam t = new RealTeam();
            t.setId(ovr);
            t.setName("Team-" + ovr);
            tiny.add(new ClubOfferEngine.RankedTeam(t, ovr));
        }

        List<ClubOfferEngine.RankedTeam> offers = ClubOfferEngine.pickOffers(
            tiny, 90, seeded());
        assertEquals(3, offers.size());

        // The third offer must come from outside the 90/85 pair.
        boolean sawOutsideElite = false;
        for (ClubOfferEngine.RankedTeam rt : offers) {
            if (rt.effectiveOverall < 85) sawOutsideElite = true;
        }
        assertTrue("must spill into mid/lower when elite only has 2", sawOutsideElite);
    }

    // ─── Determinism ─────────────────────────────────────────────────────────

    @Test
    public void sameSeedProducesSameOffers() {
        List<ClubOfferEngine.RankedTeam> a = ClubOfferEngine.pickOffers(
            twentyTeamLeague(), 75, new Random(99L));
        List<ClubOfferEngine.RankedTeam> b = ClubOfferEngine.pickOffers(
            twentyTeamLeague(), 75, new Random(99L));
        assertEquals(a.size(), b.size());
        for (int i = 0; i < a.size(); i++) {
            assertEquals("offer " + i + " must match across runs with the same seed",
                a.get(i).team.getId(), b.get(i).team.getId());
        }
    }

    @Test
    public void differentSeedsCanProduceDifferentOffers() {
        // Probabilistic but very likely — out of 7 mid teams, the chance
        // of two different seeds picking the same 3 is ~3/35 ≈ 8.5%.
        List<ClubOfferEngine.RankedTeam> a = ClubOfferEngine.pickOffers(
            twentyTeamLeague(), 75, new Random(1L));
        List<ClubOfferEngine.RankedTeam> b = ClubOfferEngine.pickOffers(
            twentyTeamLeague(), 75, new Random(7L));

        Set<Integer> aIds = new HashSet<>();
        for (ClubOfferEngine.RankedTeam rt : a) aIds.add(rt.team.getId());
        Set<Integer> bIds = new HashSet<>();
        for (ClubOfferEngine.RankedTeam rt : b) bIds.add(rt.team.getId());

        assertFalse("seeds 1 and 7 should yield different offer sets", aIds.equals(bIds));
    }
}
