package hr.fipu.footmash.worldcup;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import hr.fipu.footmash.season.TraitEngine;

/**
 * Pure-logic tests for the nation pool / squad helpers. A {@code null} DAO means
 * "no real players", which exercises the filler-generation path used for thin
 * national pools — no Android or database dependencies.
 */
public class NationSquadBuilderTest {

    private WorldCupData.Nation thinNation() {
        // Any nation works with a null DAO; New Zealand is the canonical thin pool.
        return WorldCupData.byKey("new-zealand");
    }

    @Test
    public void fillersGiveAFullBalancedPool() {
        List<WcPlayer> pool = NationSquadBuilder.buildPool(null, thinNation());
        assertTrue("pool must support a 23-man squad", pool.size() >= 23);

        Map<String, Integer> byGroup = new HashMap<>();
        for (WcPlayer p : pool) {
            String g = TraitEngine.groupOf(p.position);
            byGroup.put(g, byGroup.getOrDefault(g, 0) + 1);
        }
        assertTrue(byGroup.getOrDefault("GK", 0) >= 3);
        assertTrue(byGroup.getOrDefault("DF", 0) >= 8);
        assertTrue(byGroup.getOrDefault("MF", 0) >= 8);
        assertTrue(byGroup.getOrDefault("FW", 0) >= 6);
    }

    @Test
    public void poolIsDeterministicPerNation() {
        List<WcPlayer> a = NationSquadBuilder.buildPool(null, thinNation());
        List<WcPlayer> b = NationSquadBuilder.buildPool(null, thinNation());
        assertEquals(a.size(), b.size());
        for (int i = 0; i < a.size(); i++) {
            assertEquals(a.get(i).name, b.get(i).name);
            assertEquals(a.get(i).position, b.get(i).position);
        }
    }

    @Test
    public void autoXiFillsFormationStartingWithKeeper() {
        List<WcPlayer> pool = NationSquadBuilder.buildPool(null, thinNation());
        List<WcPlayer> xi = NationSquadBuilder.autoXi(pool, "4-3-3");
        assertEquals(11, xi.size());
        assertEquals("GK", TraitEngine.groupOf(xi.get(0).position));
    }

    @Test
    public void nationRatingAtLeastBaseline() {
        WorldCupData.Nation n = thinNation();
        int rating = NationSquadBuilder.nationRating(null, n);
        assertTrue(rating >= n.baseline);
        assertTrue(rating <= 95);
    }
}
