package hr.fipu.footmash.model;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * Unit tests for the RealPlayer pricing brackets and effective-overall
 * computation. Pure POJO — no Android, no Room.
 */
public class RealPlayerTest {

    private static RealPlayer ofOverall(int overall) {
        RealPlayer p = new RealPlayer();
        p.setOverall(overall);
        return p;
    }

    // ─── getMarketValue — bracket boundaries ─────────────────────────────────

    @Test
    public void marketValue_topTier_at90_plus() {
        assertEquals(70_000_000L, ofOverall(90).getMarketValue());
        assertEquals(70_000_000L, ofOverall(99).getMarketValue());
    }

    @Test
    public void marketValue_secondTier_85_to_89() {
        assertEquals(42_000_000L, ofOverall(89).getMarketValue());
        assertEquals(42_000_000L, ofOverall(85).getMarketValue());
    }

    @Test
    public void marketValue_thirdTier_80_to_84() {
        assertEquals(22_000_000L, ofOverall(84).getMarketValue());
        assertEquals(22_000_000L, ofOverall(80).getMarketValue());
    }

    @Test
    public void marketValue_fourthTier_75_to_79() {
        assertEquals(10_000_000L, ofOverall(79).getMarketValue());
        assertEquals(10_000_000L, ofOverall(75).getMarketValue());
    }

    @Test
    public void marketValue_fifthTier_70_to_74() {
        assertEquals(4_000_000L, ofOverall(74).getMarketValue());
        assertEquals(4_000_000L, ofOverall(70).getMarketValue());
    }

    @Test
    public void marketValue_floor_below70() {
        assertEquals(1_000_000L, ofOverall(69).getMarketValue());
        assertEquals(1_000_000L, ofOverall(50).getMarketValue());
        assertEquals(1_000_000L, ofOverall(0).getMarketValue());
    }

    // ─── getEffectiveOverall ─────────────────────────────────────────────────

    @Test
    public void effectiveOverall_defaultsToBaseWhenNoFormDelta() {
        RealPlayer p = ofOverall(82);
        assertEquals(82, p.getEffectiveOverall());
    }

    @Test
    public void effectiveOverall_addsPositiveAndNegativeForm() {
        RealPlayer hot = ofOverall(80);
        hot.setFormDelta(6);
        assertEquals(86, hot.getEffectiveOverall());

        RealPlayer slumping = ofOverall(80);
        slumping.setFormDelta(-4);
        assertEquals(76, slumping.getEffectiveOverall());
    }

    @Test
    public void effectiveOverall_doesNotClamp() {
        // Clamping is enforced at write time in SeasonRepository — the getter
        // simply returns base + delta even with out-of-band values.
        RealPlayer p = ofOverall(80);
        p.setFormDelta(20);
        assertEquals(100, p.getEffectiveOverall());
    }
}
