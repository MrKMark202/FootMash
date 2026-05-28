package hr.fipu.footmash.db;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.List;

import hr.fipu.footmash.model.GoalScorer;
import hr.fipu.footmash.model.SeasonStanding;

/**
 * Instrumented tests for StandingDao: ordering by points/GD/GF and the
 * top-scorer GROUP BY aggregation.
 */
@RunWith(AndroidJUnit4.class)
public class StandingDaoTest {

    private static final int SEASON = 1;
    private static final int OTHER_SEASON = 2;

    private AppDatabase db;
    private StandingDao dao;
    private FixtureDao fixtureDao;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase.class)
                .allowMainThreadQueries()
                .build();
        dao = db.standingDao();
        fixtureDao = db.fixtureDao();
    }

    @After
    public void tearDown() {
        db.close();
    }

    private static SeasonStanding row(int seasonId, String name, int pts, int gf, int ga,
                                      boolean user) {
        SeasonStanding s = new SeasonStanding();
        s.setSeasonId(seasonId);
        s.setTeamId(name.hashCode());
        s.setTeamName(name);
        s.setPoints(pts);
        s.setGoalsFor(gf);
        s.setGoalsAgainst(ga);
        s.setUserTeam(user);
        return s;
    }

    private static GoalScorer goal(int seasonId, int fixtureId, String player, String team) {
        GoalScorer g = new GoalScorer();
        g.setSeasonId(seasonId);
        g.setFixtureId(fixtureId);
        g.setPlayerName(player);
        g.setTeamName(team);
        g.setMinute(45);
        return g;
    }

    // ─── Insert + scoped count ───────────────────────────────────────────────

    @Test
    public void countScopesBySeason() {
        dao.insertAll(Arrays.asList(
            row(SEASON, "A", 0, 0, 0, false),
            row(SEASON, "B", 0, 0, 0, false),
            row(OTHER_SEASON, "C", 0, 0, 0, false)
        ));
        assertEquals(2, dao.count(SEASON));
        assertEquals(1, dao.count(OTHER_SEASON));
    }

    // ─── Ordering: points DESC, then GD DESC, then GF DESC ───────────────────

    @Test
    public void standingsOrderedByPointsDescending() {
        dao.insertAll(Arrays.asList(
            row(SEASON, "Mid",  20, 25, 20, false),
            row(SEASON, "Top",  40, 30, 10, false),
            row(SEASON, "Bot",   5, 10, 30, false)
        ));
        List<SeasonStanding> table = dao.getStandingsSync(SEASON);
        assertEquals("Top", table.get(0).getTeamName());
        assertEquals("Mid", table.get(1).getTeamName());
        assertEquals("Bot", table.get(2).getTeamName());
    }

    @Test
    public void pointsTiebreakerIsGoalDifference() {
        dao.insertAll(Arrays.asList(
            row(SEASON, "BetterGD", 30, 40, 20, false),  // GD +20
            row(SEASON, "WorseGD",  30, 25, 20, false),  // GD +5
            row(SEASON, "MidGD",    30, 30, 20, false)   // GD +10
        ));
        List<SeasonStanding> table = dao.getStandingsSync(SEASON);
        assertEquals("BetterGD", table.get(0).getTeamName());
        assertEquals("MidGD",    table.get(1).getTeamName());
        assertEquals("WorseGD",  table.get(2).getTeamName());
    }

    @Test
    public void goalsForBreaksRemainingTies() {
        dao.insertAll(Arrays.asList(
            row(SEASON, "MoreScored", 30, 50, 30, false),  // 30 pts, GD +20, GF 50
            row(SEASON, "LessScored", 30, 30, 10, false)   // 30 pts, GD +20, GF 30
        ));
        List<SeasonStanding> table = dao.getStandingsSync(SEASON);
        assertEquals("MoreScored", table.get(0).getTeamName());
        assertEquals("LessScored", table.get(1).getTeamName());
    }

    // ─── User standing + by-name lookups ─────────────────────────────────────

    @Test
    public void getByTeamNameReturnsTheMatch() {
        dao.insertAll(Arrays.asList(
            row(SEASON, "Hunted", 10, 5, 5, false),
            row(SEASON, "Other",  20, 9, 4, false)
        ));
        SeasonStanding found = dao.getByTeamName(SEASON, "Hunted");
        assertNotNull(found);
        assertEquals(10, found.getPoints());
    }

    // ─── Top scorers GROUP BY ────────────────────────────────────────────────

    @Test
    public void topScorersAggregatesAndSortsByGoalCount() {
        // Some fixture ids — the DAO only joins through goal_scorer so the
        // fixture rows don't need to exist for this query, but inserting them
        // keeps the test honest about cascading behaviour.
        dao.insertAll(Arrays.asList(
            row(SEASON, "Us", 0, 0, 0, true),
            row(SEASON, "Rival", 0, 0, 0, false)
        ));

        // 5 goals for Striker A, 3 for B, 1 for C.
        for (int i = 0; i < 5; i++) fixtureDao.insertGoalScorer(goal(SEASON, 100 + i, "Striker A", "Us"));
        for (int i = 0; i < 3; i++) fixtureDao.insertGoalScorer(goal(SEASON, 200 + i, "Striker B", "Rival"));
        fixtureDao.insertGoalScorer(goal(SEASON, 300, "Striker C", "Us"));
        // A goal in a different season — must not bleed in.
        fixtureDao.insertGoalScorer(goal(OTHER_SEASON, 999, "Striker A", "Us"));

        List<TopScorerRow> top = dao.getTopScorers(SEASON);
        assertEquals(3, top.size());
        assertEquals("Striker A", top.get(0).playerName);
        assertEquals(5, top.get(0).goals);
        assertEquals("Striker B", top.get(1).playerName);
        assertEquals(3, top.get(1).goals);
        assertEquals("Striker C", top.get(2).playerName);
        assertEquals(1, top.get(2).goals);
    }

    @Test
    public void topScorersGroupsByPlayerAndTeamCombo() {
        // Same player name on two different teams (transferred mid-season)
        // should appear as separate rows.
        for (int i = 0; i < 4; i++) fixtureDao.insertGoalScorer(goal(SEASON, 10 + i, "Common Name", "Team X"));
        for (int i = 0; i < 2; i++) fixtureDao.insertGoalScorer(goal(SEASON, 20 + i, "Common Name", "Team Y"));

        List<TopScorerRow> top = dao.getTopScorers(SEASON);
        assertEquals(2, top.size());
        // Higher count first
        assertEquals("Team X", top.get(0).teamName);
        assertEquals(4, top.get(0).goals);
        assertEquals("Team Y", top.get(1).teamName);
        assertEquals(2, top.get(1).goals);
    }

    @Test
    public void topScorersReturnsEmptyWhenNoGoals() {
        List<TopScorerRow> top = dao.getTopScorers(SEASON);
        assertNotNull(top);
        assertTrue(top.isEmpty());
    }

    // ─── Delete on season reset ──────────────────────────────────────────────

    @Test
    public void deleteStandingsForSeasonScopedBySeason() {
        dao.insertAll(Arrays.asList(
            row(SEASON, "A", 0, 0, 0, false),
            row(OTHER_SEASON, "B", 0, 0, 0, false)
        ));
        dao.deleteStandingsForSeason(SEASON);
        assertEquals(0, dao.count(SEASON));
        assertEquals(1, dao.count(OTHER_SEASON));
    }

    @Test
    public void updatePersistsTotals() {
        dao.insertAll(Arrays.asList(row(SEASON, "Live", 0, 0, 0, true)));
        SeasonStanding live = dao.getByTeamName(SEASON, "Live");
        live.setPlayed(5);
        live.setWon(3);
        live.setDrawn(1);
        live.setLost(1);
        live.setGoalsFor(10);
        live.setGoalsAgainst(5);
        live.setPoints(10);
        dao.update(live);

        SeasonStanding reloaded = dao.getByTeamName(SEASON, "Live");
        assertEquals(5,  reloaded.getPlayed());
        assertEquals(3,  reloaded.getWon());
        assertEquals(10, reloaded.getPoints());
        assertEquals(5,  reloaded.getGoalDiff());
    }
}
