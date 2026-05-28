package hr.fipu.footmash.career;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Unit tests for the transfer-window eligibility rule. Verifies all four
 * gates: the 2-season floor, the never-dismissed shortcut, the 2-season
 * cooldown after a decline, and that the cooldown re-arms correctly.
 */
public class TransferEligibilityTest {

    // ─── Floor: needs at least 2 seasons at the same club ───────────────────

    @Test
    public void firstSeasonNeverEligible() {
        assertFalse(TransferEligibility.isEligible(0, -1));
        assertFalse(TransferEligibility.isEligible(1, -1));
    }

    @Test
    public void zeroSeasonsAtNewClubNeverEligible() {
        // Player just transferred -- transferDismissedAt was reset to -1
        // and seasonsAtClub starts at 0.
        assertFalse(TransferEligibility.isEligible(0, -1));
    }

    // ─── First window (never dismissed) ─────────────────────────────────────

    @Test
    public void twoSeasonsAtFirstClubOpensWindow() {
        assertTrue(TransferEligibility.isEligible(2, -1));
    }

    @Test
    public void threePlusSeasonsAtFirstClubStillEligibleIfNeverDismissed() {
        assertTrue(TransferEligibility.isEligible(3, -1));
        assertTrue(TransferEligibility.isEligible(5, -1));
    }

    // ─── Cooldown after declining ────────────────────────────────────────────

    @Test
    public void seasonImmediatelyAfterDeclineIsNotEligible() {
        // User declined at season 2 -> dismissedAt = 2.
        // Simming once brings them to season 3; window should still be shut.
        assertFalse(TransferEligibility.isEligible(3, 2));
    }

    @Test
    public void cooldownExpiresAtTwoSeasonsLater() {
        // User declined at 2. Two more sims -> seasonsAtClub = 4. Window reopens.
        assertTrue(TransferEligibility.isEligible(4, 2));
    }

    @Test
    public void deepCareerStillReSpansWindowsEveryTwoSeasons() {
        // Declined at 6 -> needs to wait until 8.
        assertFalse(TransferEligibility.isEligible(6, 6));
        assertFalse(TransferEligibility.isEligible(7, 6));
        assertTrue (TransferEligibility.isEligible(8, 6));
    }

    // ─── Constants are the contract — guards against accidental retuning ────

    @Test
    public void firstWindowThresholdMatchesDesign() {
        assertTrue("Design promise: first offers after exactly 2 seasons",
            TransferEligibility.isEligible(TransferEligibility.FIRST_WINDOW_AT, -1));
        assertFalse(
            TransferEligibility.isEligible(TransferEligibility.FIRST_WINDOW_AT - 1, -1));
    }

    @Test
    public void cooldownLengthMatchesDesign() {
        // Decline at 2, wait COOLDOWN seasons. Should be eligible at 2 + COOLDOWN.
        int dismissed = 2;
        assertFalse(TransferEligibility.isEligible(
            dismissed + TransferEligibility.COOLDOWN_SEASONS - 1, dismissed));
        assertTrue(TransferEligibility.isEligible(
            dismissed + TransferEligibility.COOLDOWN_SEASONS, dismissed));
    }
}
