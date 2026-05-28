package hr.fipu.footmash.ai;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import hr.fipu.footmash.model.Fixture;
import hr.fipu.footmash.model.RealPlayer;

/**
 * Unit tests for the local probability simulator. The simulator uses a static
 * Random, so results vary; we test invariants that must hold every run rather
 * than exact values.
 */
public class LocalSimulatorTest {

    private static final int ITERATIONS = 200;

    private static Fixture fixture(String home, String away) {
        Fixture f = new Fixture();
        f.setHomeTeamName(home);
        f.setAwayTeamName(away);
        return f;
    }

    private static RealPlayer player(String name) {
        RealPlayer p = new RealPlayer();
        p.setName(name);
        return p;
    }

    // ─── Score / scorer invariants ───────────────────────────────────────────

    @Test
    public void scorersCountAlwaysMatchesGoalsTotal() {
        List<Fixture> fixtures = Collections.singletonList(fixture("Home", "Away"));
        Map<String, Integer> ratings = new HashMap<>();
        ratings.put("Home", 80);
        ratings.put("Away", 78);

        for (int i = 0; i < ITERATIONS; i++) {
            List<MatchSimulator.ParsedMatch> results = LocalSimulator.simulateAll(
                fixtures, ratings, Collections.emptyMap());
            MatchSimulator.ParsedMatch m = results.get(0);
            assertEquals("scorers.size must equal home+away goals on iteration " + i,
                m.homeGoals + m.awayGoals, m.scorers.size());
        }
    }

    @Test
    public void goalsAndMinutesStayWithinValidRange() {
        List<Fixture> fixtures = Collections.singletonList(fixture("A", "B"));
        for (int i = 0; i < ITERATIONS; i++) {
            MatchSimulator.ParsedMatch m = LocalSimulator.simulateAll(
                fixtures, Collections.emptyMap(), Collections.emptyMap()).get(0);
            assertTrue("home goals 0..4", m.homeGoals >= 0 && m.homeGoals <= 4);
            assertTrue("away goals 0..4", m.awayGoals >= 0 && m.awayGoals <= 4);
            for (MatchSimulator.Scorer s : m.scorers) {
                assertTrue("minute in 1..90", s.minute >= 1 && s.minute <= 90);
                assertTrue("team is home or away",
                    "home".equals(s.team) || "away".equals(s.team));
                assertNotNull("scorer name not null", s.name);
            }
        }
    }

    @Test
    public void scorerTeamMatchesHomeOrAwaySide() {
        // Of the scorers, exactly homeGoals should be tagged "home" and
        // exactly awayGoals should be tagged "away".
        List<Fixture> fixtures = Collections.singletonList(fixture("X", "Y"));
        for (int i = 0; i < ITERATIONS; i++) {
            MatchSimulator.ParsedMatch m = LocalSimulator.simulateAll(
                fixtures, Collections.emptyMap(), Collections.emptyMap()).get(0);
            int home = 0, away = 0;
            for (MatchSimulator.Scorer s : m.scorers) {
                if ("home".equals(s.team)) home++; else if ("away".equals(s.team)) away++;
            }
            assertEquals("home scorers count", m.homeGoals, home);
            assertEquals("away scorers count", m.awayGoals, away);
        }
    }

    // ─── Roster handling ─────────────────────────────────────────────────────

    @Test
    public void usesProvidedRosterForScorerNames() {
        // When a roster is provided, scorer names should come from it.
        List<RealPlayer> homeRoster = Arrays.asList(
            player("Alpha One"), player("Beta Two"), player("Gamma Three"));
        Map<String, List<RealPlayer>> rosters = new HashMap<>();
        rosters.put("Home", homeRoster);
        rosters.put("Away", new ArrayList<>());

        List<Fixture> fixtures = Collections.singletonList(fixture("Home", "Away"));
        boolean sawNamedScorer = false;
        for (int i = 0; i < ITERATIONS && !sawNamedScorer; i++) {
            MatchSimulator.ParsedMatch m = LocalSimulator.simulateAll(
                fixtures, Collections.emptyMap(), rosters).get(0);
            for (MatchSimulator.Scorer s : m.scorers) {
                if (!"home".equals(s.team)) continue;
                if (s.name != null && (s.name.contains("Alpha") || s.name.contains("Beta")
                                    || s.name.contains("Gamma"))) {
                    sawNamedScorer = true;
                    break;
                }
            }
        }
        // Hard to assert deterministically across 200 trials, but with a
        // roster of 3 names and ~0.5 goals/match on the home side we should
        // have seen at least one. If this flakes we should reconsider, but
        // statistically the chance is effectively zero.
        assertTrue("home scorers should come from the supplied roster", sawNamedScorer);
    }

    @Test
    public void survivesEmptyRosters() {
        // The simulator should not crash when no roster is available — it
        // generates a mock roster internally.
        List<Fixture> fixtures = Collections.singletonList(fixture("Home", "Away"));
        for (int i = 0; i < ITERATIONS; i++) {
            MatchSimulator.ParsedMatch m = LocalSimulator.simulateAll(
                fixtures, null, null).get(0);
            assertNotNull(m);
            for (MatchSimulator.Scorer s : m.scorers) {
                assertNotNull("scorer name should never be null", s.name);
                assertTrue("scorer name should never be empty", !s.name.isEmpty());
            }
        }
    }

    // ─── Result count matches input ──────────────────────────────────────────

    @Test
    public void simulateAllReturnsOneResultPerFixture() {
        List<Fixture> fixtures = Arrays.asList(
            fixture("A", "B"), fixture("C", "D"), fixture("E", "F"));
        List<MatchSimulator.ParsedMatch> results = LocalSimulator.simulateAll(
            fixtures, Collections.emptyMap(), Collections.emptyMap());
        assertEquals(3, results.size());
    }
}
