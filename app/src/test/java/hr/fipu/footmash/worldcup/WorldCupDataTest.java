package hr.fipu.footmash.worldcup;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** Sanity checks on the static 48-nation World Cup field. */
public class WorldCupDataTest {

    @Test
    public void fortyEightNations() {
        assertEquals(48, WorldCupData.count());
        assertEquals(48, WorldCupData.all().size());
    }

    @Test
    public void everyNationIsUsableForSquadsAndRatings() {
        for (WorldCupData.Nation n : WorldCupData.all()) {
            assertFalse("missing nationality mapping for " + n.key, n.englishNames.isEmpty());
            assertTrue("baseline out of range for " + n.key,
                n.baseline >= 40 && n.baseline <= 99);
            assertSame(n, WorldCupData.byKey(n.key));
        }
    }

    @Test
    public void unknownKeyIsNull() {
        org.junit.Assert.assertNull(WorldCupData.byKey("atlantis"));
        org.junit.Assert.assertNull(WorldCupData.byKey(null));
    }
}
