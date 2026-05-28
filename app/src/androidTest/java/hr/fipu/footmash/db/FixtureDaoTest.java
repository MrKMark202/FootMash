package hr.fipu.footmash.db;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import hr.fipu.footmash.model.Fixture;
import hr.fipu.footmash.model.GoalScorer;
import hr.fipu.footmash.model.MatchResult;

/**
 * Instrumented tests for FixtureDao: fixture grouping by matchday,
 * simulation marking, and result/goal-scorer joins.
 */
@RunWith(AndroidJUnit4.class)
public class FixtureDaoTest {

    private static final int SEASON = 1;
    private static final int OTHER_SEASON = 2;

    private AppDatabase db;
    private FixtureDao dao;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase.class)
                .allowMainThreadQueries()
                .build();
        dao = db.fixtureDao();
    }

    @After
    public void tearDown() {
        db.close();
    }

    private static Fixture fixture(int seasonId, int matchday, int homeId, String home,
                                   int awayId, String away, boolean user) {
        Fixture f = new Fixture();
        f.setSeasonId(seasonId);
        f.setMatchday(matchday);
        f.setHomeTeamId(homeId);
        f.setHomeTeamName(home);
        f.setAwayTeamId(awayId);
        f.setAwayTeamName(away);
        f.setUserTeam(user);
        f.setSimulated(false);
        return f;
    }

    // ─── Insert + query ──────────────────────────────────────────────────────

    @Test
    public void countAndFetchByMatchday() {
        dao.insertAll(Arrays.asList(
            fixture(SEASON, 1, 0, "Us", 10, "A", true),
            fixture(SEASON, 1, 11, "B", 12, "C", false),
            fixture(SEASON, 2, 10, "A", 0, "Us", true),
            fixture(OTHER_SEASON, 1, 1, "X", 2, "Y", false)
        ));

        assertEquals(3, dao.countFixtures(SEASON));
        assertEquals(1, dao.countFixtures(OTHER_SEASON));

        List<Fixture> md1 = dao.getFixturesByMatchdaySync(SEASON, 1);
        assertEquals(2, md1.size());
        for (Fixture f : md1) assertEquals(1, f.getMatchday());

        List<Fixture> md2 = dao.getFixturesByMatchdaySync(SEASON, 2);
        assertEquals(1, md2.size());
    }

    @Test
    public void countReturnsZeroForUnknownSeason() {
        assertEquals(0, dao.countFixtures(999));
    }

    // ─── Next matchday tracking ──────────────────────────────────────────────

    @Test
    public void nextMatchdayIsTheLowestUnsimulated() {
        List<Fixture> seed = new ArrayList<>(Arrays.asList(
            fixture(SEASON, 1, 0, "Us", 10, "A", true),
            fixture(SEASON, 2, 10, "A", 0, "Us", true),
            fixture(SEASON, 3, 0, "Us", 11, "B", true)
        ));
        dao.insertAll(seed);

        assertEquals(1, dao.getNextMatchdaySync(SEASON));

        // Mark every fixture in md1 simulated.
        for (Fixture f : dao.getFixturesByMatchdaySync(SEASON, 1)) {
            dao.markSimulated(f.getId());
        }
        assertEquals(2, dao.getNextMatchdaySync(SEASON));
    }

    @Test
    public void markSimulatedFlipsTheFlag() {
        dao.insertAll(Arrays.asList(
            fixture(SEASON, 1, 0, "Us", 10, "A", true)
        ));
        Fixture before = dao.getFixturesByMatchdaySync(SEASON, 1).get(0);
        assertTrue("seed fixture starts unsimulated", !before.isSimulated());

        dao.markSimulated(before.getId());
        Fixture after = dao.getFixturesByMatchdaySync(SEASON, 1).get(0);
        assertTrue("fixture should be simulated after mark", after.isSimulated());
    }

    // ─── Match result + scorers ──────────────────────────────────────────────

    @Test
    public void matchResultRoundTripsByFixtureId() {
        dao.insertAll(Arrays.asList(
            fixture(SEASON, 1, 0, "Us", 10, "A", true)
        ));
        Fixture f = dao.getFixturesByMatchdaySync(SEASON, 1).get(0);

        MatchResult mr = new MatchResult();
        mr.setFixtureId(f.getId());
        mr.setHomeGoals(2);
        mr.setAwayGoals(1);
        dao.insertMatchResult(mr);

        MatchResult fetched = dao.getMatchResultSync(f.getId());
        assertNotNull(fetched);
        assertEquals(2, fetched.getHomeGoals());
        assertEquals(1, fetched.getAwayGoals());
    }

    @Test
    public void getMatchResultSyncReturnsNullForMissing() {
        assertNull(dao.getMatchResultSync(999));
    }

    @Test
    public void goalScorersAreOrderedByMinute() {
        dao.insertAll(Arrays.asList(
            fixture(SEASON, 1, 0, "Us", 10, "A", true)
        ));
        Fixture f = dao.getFixturesByMatchdaySync(SEASON, 1).get(0);

        dao.insertGoalScorer(scorer(SEASON, f.getId(), "Late", "Us", 88, true));
        dao.insertGoalScorer(scorer(SEASON, f.getId(), "Early", "Us", 12, true));
        dao.insertGoalScorer(scorer(SEASON, f.getId(), "Middle", "A", 45, false));

        List<GoalScorer> goals = dao.getGoalScorersByFixture(f.getId());
        assertEquals(3, goals.size());
        assertEquals("Early", goals.get(0).getPlayerName());
        assertEquals("Middle", goals.get(1).getPlayerName());
        assertEquals("Late", goals.get(2).getPlayerName());
    }

    @Test
    public void getGoalCountByPlayerAggregatesAcrossFixtures() {
        dao.insertAll(Arrays.asList(
            fixture(SEASON, 1, 0, "Us", 10, "A", true),
            fixture(SEASON, 2, 10, "A", 0, "Us", true)
        ));
        Fixture f1 = dao.getFixturesByMatchdaySync(SEASON, 1).get(0);
        Fixture f2 = dao.getFixturesByMatchdaySync(SEASON, 2).get(0);

        dao.insertGoalScorer(scorer(SEASON, f1.getId(), "Striker A", "Us", 12, true));
        dao.insertGoalScorer(scorer(SEASON, f1.getId(), "Striker A", "Us", 67, true));
        dao.insertGoalScorer(scorer(SEASON, f2.getId(), "Striker A", "Us", 33, true));
        dao.insertGoalScorer(scorer(SEASON, f1.getId(), "Other", "A", 50, false));

        assertEquals(3, dao.getGoalCountByPlayer(SEASON, "Striker A"));
        assertEquals(1, dao.getGoalCountByPlayer(SEASON, "Other"));
        assertEquals(0, dao.getGoalCountByPlayer(SEASON, "Nobody"));
    }

    // ─── Cascading delete on next season ─────────────────────────────────────

    @Test
    public void deleteFixturesForSeasonRemovesEverythingForThatSeason() {
        dao.insertAll(Arrays.asList(
            fixture(SEASON, 1, 0, "Us", 10, "A", true),
            fixture(OTHER_SEASON, 1, 1, "X", 2, "Y", false)
        ));
        Fixture f = dao.getFixturesByMatchdaySync(SEASON, 1).get(0);

        MatchResult mr = new MatchResult();
        mr.setFixtureId(f.getId());
        mr.setHomeGoals(0);
        mr.setAwayGoals(0);
        dao.insertMatchResult(mr);
        dao.insertGoalScorer(scorer(SEASON, f.getId(), "X", "Us", 1, true));

        dao.deleteMatchResultsForSeason(SEASON);
        dao.deleteGoalScorersForSeason(SEASON);
        dao.deleteFixturesForSeason(SEASON);

        assertEquals(0, dao.countFixtures(SEASON));
        assertEquals("other-season data must be untouched",
            1, dao.countFixtures(OTHER_SEASON));
        assertNull(dao.getMatchResultSync(f.getId()));
        assertEquals(0, dao.getGoalCountByPlayer(SEASON, "X"));
    }

    private static GoalScorer scorer(int seasonId, int fixtureId, String name, String team,
                                     int minute, boolean userTeam) {
        GoalScorer g = new GoalScorer();
        g.setSeasonId(seasonId);
        g.setFixtureId(fixtureId);
        g.setPlayerName(name);
        g.setTeamName(team);
        g.setMinute(minute);
        g.setUserTeamPlayer(userTeam);
        return g;
    }
}
