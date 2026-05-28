package hr.fipu.footmash.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import hr.fipu.footmash.model.Fixture;
import hr.fipu.footmash.model.RealTeam;

/**
 * Unit tests for the round-robin fixture generator. This is the only piece of
 * SeasonRepository that's pure-static and DB-free, which makes it directly
 * unit-testable. The rest of the repository needs an in-memory Room database
 * (covered separately in androidTest).
 */
public class SeasonFixturesTest {

    private static RealTeam team(int id, String name) {
        RealTeam t = new RealTeam();
        t.setId(id);
        t.setName(name);
        return t;
    }

    private static List<RealTeam> makeOpponents(int count) {
        List<RealTeam> teams = new ArrayList<>();
        for (int i = 1; i <= count; i++) teams.add(team(i, "Team" + i));
        return teams;
    }

    // ─── A full 20-team season is the production case ────────────────────────

    @Test
    public void fullSeasonProduces380Fixtures() {
        // 1 user club + 19 real opponents = 20 teams → 38 matchdays × 10 = 380.
        List<Fixture> all = SeasonRepository.generateFixtures(
            1, "User FC", makeOpponents(19));
        assertEquals(380, all.size());
    }

    @Test
    public void fullSeasonHas38Matchdays() {
        List<Fixture> all = SeasonRepository.generateFixtures(
            1, "User FC", makeOpponents(19));
        Set<Integer> matchdays = new HashSet<>();
        for (Fixture f : all) matchdays.add(f.getMatchday());
        assertEquals(38, matchdays.size());
        for (int md = 1; md <= 38; md++) {
            assertTrue("matchday " + md + " must be present", matchdays.contains(md));
        }
    }

    @Test
    public void eachMatchdayHasExactly10Fixtures() {
        List<Fixture> all = SeasonRepository.generateFixtures(
            1, "User FC", makeOpponents(19));
        Map<Integer, Integer> counts = new HashMap<>();
        for (Fixture f : all) {
            counts.merge(f.getMatchday(), 1, Integer::sum);
        }
        for (Map.Entry<Integer, Integer> e : counts.entrySet()) {
            assertEquals("matchday " + e.getKey() + " must have 10 fixtures",
                Integer.valueOf(10), e.getValue());
        }
    }

    @Test
    public void everyTeamPlaysExactly38Matches() {
        List<Fixture> all = SeasonRepository.generateFixtures(
            1, "User FC", makeOpponents(19));
        Map<Integer, Integer> appearances = new HashMap<>();
        for (Fixture f : all) {
            appearances.merge(f.getHomeTeamId(), 1, Integer::sum);
            appearances.merge(f.getAwayTeamId(), 1, Integer::sum);
        }
        assertEquals("must include user club + 19 opponents", 20, appearances.size());
        for (Map.Entry<Integer, Integer> e : appearances.entrySet()) {
            assertEquals("team " + e.getKey() + " must play 38 games",
                Integer.valueOf(38), e.getValue());
        }
    }

    @Test
    public void everyPairPlaysExactlyTwiceOnceAtEachVenue() {
        List<Fixture> all = SeasonRepository.generateFixtures(
            1, "User FC", makeOpponents(19));
        // For each ordered pair (home, away), expect exactly 1 fixture.
        Map<Long, Integer> ordered = new HashMap<>();
        for (Fixture f : all) {
            long key = ((long) f.getHomeTeamId() << 32) | (f.getAwayTeamId() & 0xffffffffL);
            ordered.merge(key, 1, Integer::sum);
        }
        for (Map.Entry<Long, Integer> e : ordered.entrySet()) {
            assertEquals("home/away pair " + Long.toHexString(e.getKey()) + " duplicated",
                Integer.valueOf(1), e.getValue());
        }
        // 20 teams → 20*19 = 380 ordered pairs (each plays everyone home + away).
        assertEquals(380, ordered.size());
    }

    @Test
    public void noTeamEverPlaysItself() {
        List<Fixture> all = SeasonRepository.generateFixtures(
            1, "User FC", makeOpponents(19));
        for (Fixture f : all) {
            assertNotEquals("fixture " + f.getMatchday() + " has team playing itself",
                f.getHomeTeamId(), f.getAwayTeamId());
        }
    }

    @Test
    public void userClubGetsTeamIdZero() {
        // The user club is keyed by id 0 throughout the season engine — it's how
        // SeasonRepository distinguishes the player's team from the AI teams.
        List<Fixture> all = SeasonRepository.generateFixtures(
            7, "My Club", makeOpponents(3));
        boolean foundUserHome = false, foundUserAway = false;
        for (Fixture f : all) {
            if (f.getHomeTeamId() == 0) {
                assertEquals("My Club", f.getHomeTeamName());
                assertTrue("isUserTeam flag must be set on user matches", f.isUserTeam());
                foundUserHome = true;
            }
            if (f.getAwayTeamId() == 0) {
                assertEquals("My Club", f.getAwayTeamName());
                assertTrue(f.isUserTeam());
                foundUserAway = true;
            }
        }
        assertTrue("user club should host some matches", foundUserHome);
        assertTrue("user club should also play away", foundUserAway);
    }

    @Test
    public void allFixturesTaggedWithGivenSeasonId() {
        List<Fixture> all = SeasonRepository.generateFixtures(
            42, "Club X", makeOpponents(3));
        for (Fixture f : all) {
            assertEquals(42, f.getSeasonId());
            assertTrue("fixture must start unsimulated", !f.isSimulated());
        }
    }

    // ─── Edge cases ──────────────────────────────────────────────────────────

    @Test
    public void smallLeagueOfFourTeamsProduces12Fixtures() {
        // 1 user + 3 opponents = 4 teams → 6 matchdays × 2 fixtures = 12.
        List<Fixture> all = SeasonRepository.generateFixtures(
            1, "User", makeOpponents(3));
        assertEquals(12, all.size());
        Set<Integer> matchdays = new HashSet<>();
        for (Fixture f : all) matchdays.add(f.getMatchday());
        assertEquals(6, matchdays.size());
    }

    @Test
    public void twoTeamLeagueProducesHomeAndAway() {
        List<Fixture> all = SeasonRepository.generateFixtures(
            1, "User", Arrays.asList(team(99, "Rival")));
        assertEquals(2, all.size());
        // One should be User-home, the other Rival-home.
        boolean userHome = false, rivalHome = false;
        for (Fixture f : all) {
            if (f.getHomeTeamId() == 0) userHome = true;
            if (f.getHomeTeamId() == 99) rivalHome = true;
        }
        assertTrue("user must host once", userHome);
        assertTrue("rival must host once", rivalHome);
    }
}
