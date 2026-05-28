package hr.fipu.footmash.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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

import java.util.Arrays;
import java.util.Collections;

import hr.fipu.footmash.db.AppDatabase;
import hr.fipu.footmash.model.CustomPlayer;
import hr.fipu.footmash.model.Fixture;
import hr.fipu.footmash.model.GoalScorer;
import hr.fipu.footmash.model.MatchResult;
import hr.fipu.footmash.model.PlayerCareerSeason;
import hr.fipu.footmash.model.SeasonStanding;
import hr.fipu.footmash.model.UserClub;
import hr.fipu.footmash.model.UserSquad;

/**
 * Instrumented tests for cascade deletes via {@link SavedGamesRepository}.
 * Both delete paths walk several child tables, so the tests insert "noise"
 * rows for unrelated players / clubs and verify those survive.
 */
@RunWith(AndroidJUnit4.class)
public class SavedGamesRepositoryTest {

    private AppDatabase db;
    private SavedGamesRepository repo;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase.class)
            .allowMainThreadQueries()
            .build();
        repo = new SavedGamesRepository(db);
    }

    @After
    public void tearDown() { db.close(); }

    // ─── deletePlayer ────────────────────────────────────────────────────────

    @Test
    public void deletePlayer_removesPlayerAndAllCareerRows() {
        long id = db.customPlayerDao().insert(player("Luka", "Modric"));

        db.playerCareerSeasonDao().insert(careerSeason((int) id, 2025, 100));
        db.playerCareerSeasonDao().insert(careerSeason((int) id, 2026, 100));
        db.playerCareerSeasonDao().insert(careerSeason((int) id, 2027, 100));

        assertTrue(repo.deletePlayer((int) id));

        assertNull("player row must be gone",
            db.customPlayerDao().getPlayerByIdSync((int) id));
        assertEquals("career rows must be gone",
            0, db.playerCareerSeasonDao().getByPlayerSync((int) id).size());
    }

    @Test
    public void deletePlayer_leavesOtherPlayersUntouched() {
        long lukaId = db.customPlayerDao().insert(player("Luka", "Modric"));
        long ronId  = db.customPlayerDao().insert(player("Ronaldo", "Test"));

        db.playerCareerSeasonDao().insert(careerSeason((int) lukaId, 2025, 100));
        db.playerCareerSeasonDao().insert(careerSeason((int) ronId,  2025, 200));

        repo.deletePlayer((int) lukaId);

        assertNotNull("other player must survive",
            db.customPlayerDao().getPlayerByIdSync((int) ronId));
        assertEquals("other player's career row must survive",
            1, db.playerCareerSeasonDao().getByPlayerSync((int) ronId).size());
    }

    @Test
    public void deletePlayer_returnsFalseForMissingId() {
        assertFalse(repo.deletePlayer(999));
    }

    @Test
    public void deletePlayer_isSafeForPlayerWithNoCareer() {
        long id = db.customPlayerDao().insert(player("Fresh", "Signing"));
        assertTrue("zero-career player should still delete cleanly",
            repo.deletePlayer((int) id));
        assertNull(db.customPlayerDao().getPlayerByIdSync((int) id));
    }

    // ─── deleteClub ──────────────────────────────────────────────────────────

    @Test
    public void deleteClub_removesEveryChildTable() {
        long clubId = db.userClubDao().insertClub(club("My FC"));
        seedFullSeason((int) clubId);

        assertTrue(repo.deleteClub((int) clubId));

        assertEquals(0, db.userClubDao().getSquadCount((int) clubId));
        assertEquals(0, db.fixtureDao().countFixtures((int) clubId));
        assertNull(db.userClubDao().getClubByIdSync((int) clubId));
    }

    @Test
    public void deleteClub_leavesOtherClubsUntouched() {
        long aId = db.userClubDao().insertClub(club("Keeper FC"));
        long bId = db.userClubDao().insertClub(club("Goner FC"));
        seedFullSeason((int) aId);
        seedFullSeason((int) bId);

        repo.deleteClub((int) bId);

        assertNotNull(db.userClubDao().getClubByIdSync((int) aId));
        assertEquals("survivor's squad intact",
            3, db.userClubDao().getSquadCount((int) aId));
        assertEquals("survivor's fixtures intact",
            2, db.fixtureDao().countFixtures((int) aId));
    }

    @Test
    public void deleteClub_returnsFalseForMissingId() {
        assertFalse(repo.deleteClub(999));
    }

    @Test
    public void deleteClub_handlesEmptyClubGracefully() {
        long clubId = db.userClubDao().insertClub(club("Empty FC"));
        // No squad / fixtures / etc — just the parent row.
        assertTrue(repo.deleteClub((int) clubId));
        assertNull(db.userClubDao().getClubByIdSync((int) clubId));
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private static CustomPlayer player(String first, String last) {
        CustomPlayer p = new CustomPlayer();
        p.setFirstName(first);
        p.setLastName(last);
        p.setPosition("ST");
        p.setNationality("Hrvatska");
        p.setPace(70); p.setShooting(70); p.setPassing(70);
        p.setDribbling(70); p.setDefending(50); p.setPhysical(70);
        p.setTargetTeamId(1);
        p.setTargetTeamName("Some Club");
        p.setTargetLeagueId(177);
        p.setTargetSeason(2025);
        p.setCurrentSeasonYear(2025);
        return p;
    }

    private static PlayerCareerSeason careerSeason(int playerId, int year, int clubId) {
        PlayerCareerSeason s = new PlayerCareerSeason();
        s.setPlayerId(playerId);
        s.setSeasonYear(year);
        s.setClubId(clubId);
        s.setClubName("X");
        s.setLeagueId(0);
        s.setAppearances(30);
        s.setGoals(10);
        s.setAssists(5);
        s.setAvgRating(7.1f);
        s.setClubFinalPosition(8);
        s.setPointsEarned(14);
        s.setOvrAtSeasonEnd(70);
        return s;
    }

    private static UserClub club(String name) {
        UserClub c = new UserClub();
        c.setClubName(name);
        c.setLeagueId(177);
        c.setLeagueName("Premier League");
        c.setFormation("4-3-3");
        c.setBudget(UserClub.STARTING_BUDGET);
        c.setSeasonYear(2025);
        c.setActive(true);
        return c;
    }

    /** Inserts 3 squad rows, 2 fixtures with results, 2 scorers, 1 standing — all keyed on clubId. */
    private void seedFullSeason(int clubId) {
        for (int playerId : new int[]{10, 20, 30}) {
            UserSquad s = new UserSquad();
            s.setClubId(clubId);
            s.setPlayerId(playerId);
            s.setStartingXI(true);
            s.setPitchPosition("GK");
            db.userClubDao().insertSquadPlayer(s);
        }
        db.fixtureDao().insertAll(Arrays.asList(
            fixture(clubId, 1, "Home"),
            fixture(clubId, 2, "Home")));
        Fixture firstFixture = db.fixtureDao().getFixturesByMatchdaySync(clubId, 1).get(0);
        MatchResult mr = new MatchResult();
        mr.setFixtureId(firstFixture.getId());
        mr.setHomeGoals(2);
        mr.setAwayGoals(1);
        db.fixtureDao().insertMatchResult(mr);
        db.fixtureDao().insertGoalScorer(scorer(clubId, firstFixture.getId(), "Scorer 1"));
        db.fixtureDao().insertGoalScorer(scorer(clubId, firstFixture.getId(), "Scorer 2"));

        SeasonStanding st = new SeasonStanding();
        st.setSeasonId(clubId);
        st.setTeamId(0);
        st.setTeamName("Self");
        st.setUserTeam(true);
        db.standingDao().insertAll(Collections.singletonList(st));
    }

    private static Fixture fixture(int seasonId, int matchday, String homeName) {
        Fixture f = new Fixture();
        f.setSeasonId(seasonId);
        f.setMatchday(matchday);
        f.setHomeTeamId(0);
        f.setHomeTeamName(homeName);
        f.setAwayTeamId(99);
        f.setAwayTeamName("Visitor");
        f.setUserTeam(true);
        f.setSimulated(false);
        return f;
    }

    private static GoalScorer scorer(int seasonId, int fixtureId, String name) {
        GoalScorer g = new GoalScorer();
        g.setSeasonId(seasonId);
        g.setFixtureId(fixtureId);
        g.setPlayerName(name);
        g.setTeamName("Self");
        g.setMinute(45);
        g.setUserTeamPlayer(true);
        return g;
    }
}
