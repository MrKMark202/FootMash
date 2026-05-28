package hr.fipu.footmash.career;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Random;

import hr.fipu.footmash.career.PlayerCareerEngine.PlayerStats;
import hr.fipu.footmash.career.PlayerCareerEngine.SeasonOutcome;

/**
 * Unit tests for the season-by-season career engine. Probabilistic, so most
 * assertions either run with a fixed seed for determinism or aggregate
 * across many iterations to check distributions.
 */
public class PlayerCareerEngineTest {

    private static final int ITERATIONS = 500;

    private static PlayerStats player(int ovr, int sho, int pas, int dri, String pos) {
        return new PlayerStats(ovr, sho, pas, dri, pos);
    }

    // ─── Points tier mapping ─────────────────────────────────────────────────

    @Test
    public void pointsTier_belowAvgGets6() {
        assertEquals(6, PlayerCareerEngine.pointsForRating(5.0f));
        assertEquals(6, PlayerCareerEngine.pointsForRating(6.49f));
    }

    @Test
    public void pointsTier_avgGets10() {
        assertEquals(10, PlayerCareerEngine.pointsForRating(6.5f));
        assertEquals(10, PlayerCareerEngine.pointsForRating(6.99f));
    }

    @Test
    public void pointsTier_goodGets14() {
        assertEquals(14, PlayerCareerEngine.pointsForRating(7.0f));
        assertEquals(14, PlayerCareerEngine.pointsForRating(7.49f));
    }

    @Test
    public void pointsTier_exceptionalGets18() {
        assertEquals(18, PlayerCareerEngine.pointsForRating(7.5f));
        assertEquals(18, PlayerCareerEngine.pointsForRating(9.5f));
    }

    // ─── Appearances ─────────────────────────────────────────────────────────

    @Test
    public void appsAlwaysWithinValidRange() {
        for (int i = 0; i < ITERATIONS; i++) {
            SeasonOutcome o = PlayerCareerEngine.simulate(
                player(75, 70, 70, 70, "CM"), 78, new Random(i));
            assertTrue("apps " + o.appearances + " out of range",
                o.appearances >= 0 && o.appearances <= PlayerCareerEngine.MATCHES_PER_SEASON);
        }
    }

    @Test
    public void strongerThanTeamPlayerStartsMostMatches() {
        int totalApps = 0;
        for (int i = 0; i < ITERATIONS; i++) {
            SeasonOutcome o = PlayerCareerEngine.simulate(
                player(88, 80, 80, 80, "ST"), 75, new Random(i));
            totalApps += o.appearances;
        }
        float avg = totalApps / (float) ITERATIONS;
        // 88 OVR at a 75 team is locked in -- should average near 38.
        assertTrue("expected high apps for star player, got avg " + avg, avg >= 32f);
    }

    @Test
    public void weakerThanTeamPlayerRotates() {
        int totalApps = 0;
        for (int i = 0; i < ITERATIONS; i++) {
            SeasonOutcome o = PlayerCareerEngine.simulate(
                player(62, 60, 60, 60, "CM"), 80, new Random(i));
            totalApps += o.appearances;
        }
        float avg = totalApps / (float) ITERATIONS;
        // 62 at an 80 team rotates -- expect significantly below full season.
        assertTrue("expected rotation player apps below 25, got avg " + avg, avg < 25f);
    }

    // ─── Position-aware output ───────────────────────────────────────────────

    @Test
    public void forwardsScoreMoreGoalsThanDefendersOnAverage() {
        int fwGoals = 0, dfGoals = 0;
        for (int i = 0; i < ITERATIONS; i++) {
            fwGoals += PlayerCareerEngine.simulate(
                player(80, 85, 70, 80, "ST"), 78, new Random(i)).goals;
            dfGoals += PlayerCareerEngine.simulate(
                player(80, 60, 70, 60, "CB"), 78, new Random(i)).goals;
        }
        assertTrue("forwards should out-score defenders by a wide margin: FW="
                + fwGoals + " vs DF=" + dfGoals,
            fwGoals > dfGoals * 4);
    }

    @Test
    public void midfieldersGetMoreAssistsThanForwardsOnAverage() {
        int mfAssists = 0, fwAssists = 0;
        for (int i = 0; i < ITERATIONS; i++) {
            mfAssists += PlayerCareerEngine.simulate(
                player(78, 70, 85, 80, "CAM"), 78, new Random(i)).assists;
            fwAssists += PlayerCareerEngine.simulate(
                player(80, 85, 65, 80, "ST"), 78, new Random(i)).assists;
        }
        assertTrue("midfielders should out-assist forwards: MF="
                + mfAssists + " vs FW=" + fwAssists,
            mfAssists > fwAssists);
    }

    @Test
    public void goalkeepersScoreEssentiallyNothing() {
        int total = 0;
        for (int i = 0; i < ITERATIONS; i++) {
            total += PlayerCareerEngine.simulate(
                player(80, 30, 70, 30, "GK"), 78, new Random(i)).goals;
        }
        // 500 GK seasons should produce well under 5 goals total.
        assertTrue("GKs should score near zero across 500 seasons, got " + total,
            total < 10);
    }

    // ─── Rating shape ────────────────────────────────────────────────────────

    @Test
    public void avgRatingStaysInValidBand() {
        for (int i = 0; i < ITERATIONS; i++) {
            SeasonOutcome o = PlayerCareerEngine.simulate(
                player(75, 70, 70, 70, "CM"), 78, new Random(i));
            assertTrue("rating " + o.avgRating + " out of band [5.0, 9.5]",
                o.avgRating >= 5.0f && o.avgRating <= 9.5f);
        }
    }

    @Test
    public void higherPlayerOvrCorrelatesWithBetterRating() {
        float lowSum = 0f, highSum = 0f;
        for (int i = 0; i < ITERATIONS; i++) {
            lowSum  += PlayerCareerEngine.simulate(
                player(65, 60, 60, 60, "CM"), 78, new Random(i)).avgRating;
            highSum += PlayerCareerEngine.simulate(
                player(88, 80, 80, 80, "CM"), 78, new Random(i)).avgRating;
        }
        assertTrue("high-OVR player should average better rating: low="
                + (lowSum / ITERATIONS) + " vs high=" + (highSum / ITERATIONS),
            highSum > lowSum);
    }

    // ─── Determinism ─────────────────────────────────────────────────────────

    @Test
    public void sameSeedProducesSameOutcome() {
        PlayerStats stats = player(78, 75, 75, 75, "CM");
        SeasonOutcome a = PlayerCareerEngine.simulate(stats, 78, new Random(42L));
        SeasonOutcome b = PlayerCareerEngine.simulate(stats, 78, new Random(42L));
        assertEquals(a.appearances,       b.appearances);
        assertEquals(a.goals,             b.goals);
        assertEquals(a.assists,           b.assists);
        assertEquals(a.avgRating,         b.avgRating, 0.0001f);
        assertEquals(a.clubFinalPosition, b.clubFinalPosition);
        assertEquals(a.pointsEarned,      b.pointsEarned);
    }
}
