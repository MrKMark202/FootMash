package hr.fipu.footmash.season;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import hr.fipu.footmash.model.FormationSlot;

/**
 * Unit tests for the formation catalog: every formation has exactly 11 slots
 * with unique keys, exactly one GK, and position-group matching covers all
 * the player positions emitted by the seed data.
 */
public class FormationCatalogTest {

    private static final String[] ALL_FORMATIONS = {"4-4-2", "4-3-3", "3-5-2", "4-2-3-1"};

    @Test
    public void everyFormationHasExactlyElevenSlots() {
        for (String name : ALL_FORMATIONS) {
            List<FormationSlot> slots = FormationCatalog.get(name);
            assertNotNull("formation " + name + " must exist", slots);
            assertEquals("formation " + name + " must have 11 slots", 11, slots.size());
        }
    }

    @Test
    public void everyFormationHasExactlyOneGoalkeeper() {
        for (String name : ALL_FORMATIONS) {
            int gks = 0;
            for (FormationSlot s : FormationCatalog.get(name)) {
                if ("GK".equals(s.posGroup)) gks++;
            }
            assertEquals("formation " + name + " must have one GK slot", 1, gks);
        }
    }

    @Test
    public void slotKeysAreUniqueWithinAFormation() {
        for (String name : ALL_FORMATIONS) {
            Set<String> keys = new HashSet<>();
            for (FormationSlot s : FormationCatalog.get(name)) keys.add(s.key);
            assertEquals("slot keys in " + name + " must be unique",
                11, keys.size());
        }
    }

    @Test
    public void slotCoordinatesAreNormalised() {
        for (String name : ALL_FORMATIONS) {
            for (FormationSlot s : FormationCatalog.get(name)) {
                assertTrue("xPct in [0,1] for " + name + "/" + s.key,
                    s.xPct >= 0f && s.xPct <= 1f);
                assertTrue("yPct in [0,1] for " + name + "/" + s.key,
                    s.yPct >= 0f && s.yPct <= 1f);
            }
        }
    }

    @Test
    public void unknownFormationFallsBackToDefault() {
        List<FormationSlot> fallback = FormationCatalog.get("nonsense-formation");
        List<FormationSlot> def      = FormationCatalog.get(FormationCatalog.DEFAULT);
        assertEquals("unknown name must return the default 11-slot layout",
            def.size(), fallback.size());
    }

    @Test
    public void namesEnumeratesAllFourFormations() {
        List<String> names = FormationCatalog.names();
        assertEquals(4, names.size());
        for (String expected : ALL_FORMATIONS) {
            assertTrue("names() must include " + expected, names.contains(expected));
        }
    }

    // ─── matchesPosGroup — every seed-data position routes somewhere ─────────

    @Test
    public void defenderPositionsMatchDfGroup() {
        assertTrue(FormationCatalog.matchesPosGroup("CB", "DF"));
        assertTrue(FormationCatalog.matchesPosGroup("LB", "DF"));
        assertTrue(FormationCatalog.matchesPosGroup("RB", "DF"));
        assertFalse(FormationCatalog.matchesPosGroup("ST", "DF"));
    }

    @Test
    public void midfielderPositionsMatchMfGroup() {
        assertTrue(FormationCatalog.matchesPosGroup("CM", "MF"));
        assertTrue(FormationCatalog.matchesPosGroup("CDM", "MF"));
        assertTrue(FormationCatalog.matchesPosGroup("CAM", "MF"));
        assertTrue(FormationCatalog.matchesPosGroup("LM", "MF"));
        assertTrue(FormationCatalog.matchesPosGroup("RM", "MF"));
        assertTrue(FormationCatalog.matchesPosGroup("LAM", "MF"));
        assertTrue(FormationCatalog.matchesPosGroup("RAM", "MF"));
    }

    @Test
    public void forwardPositionsMatchFwGroup() {
        assertTrue(FormationCatalog.matchesPosGroup("ST", "FW"));
        assertTrue(FormationCatalog.matchesPosGroup("FW", "FW"));
        assertTrue(FormationCatalog.matchesPosGroup("LW", "FW"));
        assertTrue(FormationCatalog.matchesPosGroup("RW", "FW"));
        assertFalse(FormationCatalog.matchesPosGroup("CB", "FW"));
    }

    @Test
    public void goalkeeperMatchesGkGroupOnly() {
        assertTrue(FormationCatalog.matchesPosGroup("GK", "GK"));
        assertFalse(FormationCatalog.matchesPosGroup("CB", "GK"));
        assertFalse(FormationCatalog.matchesPosGroup("GK", "DF"));
    }

    @Test
    public void matchesPosGroupHandlesNulls() {
        assertFalse(FormationCatalog.matchesPosGroup(null, "DF"));
        assertFalse(FormationCatalog.matchesPosGroup("CB", null));
        assertFalse(FormationCatalog.matchesPosGroup(null, null));
    }
}
