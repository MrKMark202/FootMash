package hr.fipu.footmash;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import hr.fipu.footmash.model.RealPlayer;
import hr.fipu.footmash.model.SynergyResult;
import hr.fipu.footmash.model.Trait;
import hr.fipu.footmash.season.TraitEngine;

/** Unit tests for the trait derivation and synergy engine — pure logic, no Android. */
public class TraitEngineTest {

    private static RealPlayer player(String pos, int pace, int sho, int pas,
                                     int dri, int def, int phy, int ovr) {
        RealPlayer p = new RealPlayer();
        p.setName("Test " + pos);
        p.setPosition(pos);
        p.setPace(pace);
        p.setShooting(sho);
        p.setPassing(pas);
        p.setDribbling(dri);
        p.setDefending(def);
        p.setPhysical(phy);
        p.setOverall(ovr);
        return p;
    }

    // ─── Derivation ────────────────────────────────────────────────────────────

    @Test
    public void poacherStrikerGetsGoalPoacher() {
        RealPlayer striker = player("ST", 88, 93, 60, 80, 40, 85, 90);
        List<Trait> traits = TraitEngine.deriveTraits(striker);
        assertTrue("a high-shooting striker should be a Goal Poacher",
                traits.contains(Trait.GOAL_POACHER));
    }

    @Test
    public void crosserMidfielderGetsCurvedCrosser() {
        RealPlayer wideMid = player("RM", 86, 70, 85, 82, 60, 70, 84);
        List<Trait> traits = TraitEngine.deriveTraits(wideMid);
        assertTrue("a fast, accurate wide midfielder should be a Curved Crosser",
                traits.contains(Trait.CURVED_CROSSER));
    }

    @Test
    public void traitsCappedAtThreeAndNeverEmpty() {
        RealPlayer star = player("ST", 95, 95, 90, 95, 60, 90, 94);
        List<Trait> traits = TraitEngine.deriveTraits(star);
        assertTrue("at most 3 traits", traits.size() <= 3);
        assertFalse("at least 1 trait", traits.isEmpty());

        RealPlayer journeyman = player("CB", 50, 40, 45, 45, 55, 55, 62);
        assertFalse("even a weak player keeps a defining trait",
                TraitEngine.deriveTraits(journeyman).isEmpty());
    }

    @Test
    public void positionGroupMapping() {
        assertEquals("GK", TraitEngine.groupOf("GK"));
        assertEquals("DF", TraitEngine.groupOf("LB"));
        assertEquals("MF", TraitEngine.groupOf("CDM"));
        assertEquals("FW", TraitEngine.groupOf("LW"));
    }

    // ─── Synergy ───────────────────────────────────────────────────────────────

    @Test
    public void crosserPlusPoacherIsPositiveSynergy() {
        List<RealPlayer> xi = new ArrayList<>();
        xi.add(player("RM", 86, 70, 85, 82, 55, 70, 84));   // Curved Crosser
        xi.add(player("ST", 88, 93, 60, 80, 40, 85, 90));   // Goal Poacher
        SynergyResult r = TraitEngine.computeSynergy(xi);
        assertTrue("crosser + poacher should raise synergy", r.delta > 0);
        assertFalse("the positive combo should be listed", r.positives.isEmpty());
    }

    @Test
    public void poacherPlusFalseNineIsNegativeSynergy() {
        List<RealPlayer> xi = new ArrayList<>();
        xi.add(player("ST", 88, 93, 60, 80, 40, 85, 90));   // Goal Poacher
        xi.add(player("ST", 80, 64, 86, 88, 45, 68, 86));   // False 9 (low shooting)
        SynergyResult r = TraitEngine.computeSynergy(xi);
        assertTrue("poacher + false 9 should lower synergy", r.delta < 0);
        assertFalse("the negative combo should be listed", r.negatives.isEmpty());
    }

    @Test
    public void emptyOrSingleLineupIsNeutral() {
        assertEquals(0, TraitEngine.computeSynergy(new ArrayList<>()).delta);
        List<RealPlayer> one = new ArrayList<>();
        one.add(player("ST", 88, 93, 60, 80, 40, 85, 90));
        assertEquals(0, TraitEngine.computeSynergy(one).delta);
    }

    // ─── Best XI & effective rating ────────────────────────────────────────────

    @Test
    public void bestXiPicksElevenAndEffectiveRatingTracksAverage() {
        List<RealPlayer> roster = new ArrayList<>();
        roster.add(player("GK", 50, 20, 70, 45, 18, 80, 84));
        for (int i = 0; i < 6; i++) roster.add(player("CB", 70, 45, 70, 65, 80, 82, 80));
        for (int i = 0; i < 6; i++) roster.add(player("CM", 72, 70, 82, 80, 70, 75, 81));
        for (int i = 0; i < 6; i++) roster.add(player("ST", 85, 84, 65, 82, 42, 80, 83));

        List<RealPlayer> xi = TraitEngine.bestXi(roster);
        assertEquals("best XI must be 11 players", 11, xi.size());

        int eff = TraitEngine.effectiveRating(xi);
        int avg = TraitEngine.avgOverall(xi);
        assertTrue("effective rating stays within the synergy delta band of the average",
                Math.abs(eff - avg) <= 6);
    }
}
