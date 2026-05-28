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

import java.util.List;

import hr.fipu.footmash.model.UserClub;
import hr.fipu.footmash.model.UserSquad;

/**
 * Instrumented tests for UserClubDao using an in-memory Room database.
 * Verifies the active-club invariant and squad bookkeeping.
 */
@RunWith(AndroidJUnit4.class)
public class UserClubDaoTest {

    private AppDatabase db;
    private UserClubDao dao;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase.class)
                .allowMainThreadQueries()
                .build();
        dao = db.userClubDao();
    }

    @After
    public void tearDown() {
        db.close();
    }

    private static UserClub club(String name, boolean active) {
        UserClub c = new UserClub();
        c.setClubName(name);
        c.setLeagueId(1);
        c.setLeagueName("Premier League");
        c.setFormation("4-3-3");
        c.setBudget(UserClub.STARTING_BUDGET);
        c.setSeasonYear(2025);
        c.setActive(active);
        return c;
    }

    // ─── Club lifecycle ──────────────────────────────────────────────────────

    @Test
    public void insertedClubIsRetrievableById() {
        long id = dao.insertClub(club("Test FC", true));
        UserClub found = dao.getClubByIdSync((int) id);
        assertNotNull(found);
        assertEquals("Test FC", found.getClubName());
        assertEquals(UserClub.STARTING_BUDGET, found.getBudget());
    }

    @Test
    public void getActiveClubReturnsTheOneActive() {
        dao.insertClub(club("Old", false));
        long activeId = dao.insertClub(club("Current", true));
        UserClub active = dao.getActiveClubSync();
        assertNotNull(active);
        assertEquals((int) activeId, active.getId());
        assertEquals("Current", active.getClubName());
    }

    @Test
    public void getActiveClubReturnsNullWhenNoneActive() {
        dao.insertClub(club("Old1", false));
        dao.insertClub(club("Old2", false));
        assertNull(dao.getActiveClubSync());
    }

    @Test
    public void deactivateAllClearsTheActiveFlagEverywhere() {
        dao.insertClub(club("A", true));
        dao.insertClub(club("B", false));
        dao.insertClub(club("C", true)); // simulate an inconsistent state
        dao.deactivateAll();
        assertNull("no club should remain active", dao.getActiveClubSync());
    }

    @Test
    public void updateClubPersistsChanges() {
        long id = dao.insertClub(club("Initial", true));
        UserClub fetched = dao.getClubByIdSync((int) id);
        fetched.setBudget(1_000_000L);
        fetched.setSeasonYear(2027);
        dao.updateClub(fetched);

        UserClub reloaded = dao.getClubByIdSync((int) id);
        assertEquals(1_000_000L, reloaded.getBudget());
        assertEquals(2027, reloaded.getSeasonYear());
    }

    // ─── Squad bookkeeping ───────────────────────────────────────────────────

    private static UserSquad squadEntry(int clubId, int playerId, boolean starting, String pos) {
        UserSquad s = new UserSquad();
        s.setClubId(clubId);
        s.setPlayerId(playerId);
        s.setStartingXI(starting);
        s.setPitchPosition(pos);
        return s;
    }

    @Test
    public void squadCountReflectsInsertedEntries() {
        long clubId = dao.insertClub(club("Squad FC", true));
        assertEquals(0, dao.getSquadCount((int) clubId));
        dao.insertSquadPlayer(squadEntry((int) clubId, 100, true, "GK"));
        dao.insertSquadPlayer(squadEntry((int) clubId, 101, true, "CB1"));
        dao.insertSquadPlayer(squadEntry((int) clubId, 102, false, null));
        assertEquals(3, dao.getSquadCount((int) clubId));
    }

    @Test
    public void getSquadByClubScopesByClubId() {
        long aId = dao.insertClub(club("A", true));
        long bId = dao.insertClub(club("B", false));
        dao.insertSquadPlayer(squadEntry((int) aId, 1, true, "GK"));
        dao.insertSquadPlayer(squadEntry((int) aId, 2, true, "ST1"));
        dao.insertSquadPlayer(squadEntry((int) bId, 99, true, "GK"));

        List<UserSquad> aSquad = dao.getSquadByClubSync((int) aId);
        assertEquals(2, aSquad.size());
        for (UserSquad s : aSquad) assertEquals((int) aId, s.getClubId());

        List<UserSquad> bSquad = dao.getSquadByClubSync((int) bId);
        assertEquals(1, bSquad.size());
    }

    @Test
    public void removePlayerFromSquadDeletesOnlyThatEntry() {
        long clubId = dao.insertClub(club("RemoveFC", true));
        dao.insertSquadPlayer(squadEntry((int) clubId, 10, true, "GK"));
        dao.insertSquadPlayer(squadEntry((int) clubId, 20, true, "CB1"));
        dao.insertSquadPlayer(squadEntry((int) clubId, 30, false, null));

        dao.removePlayerFromSquad(20, (int) clubId);
        List<UserSquad> remaining = dao.getSquadByClubSync((int) clubId);
        assertEquals(2, remaining.size());
        for (UserSquad s : remaining) {
            assertTrue("player 20 should be gone", s.getPlayerId() != 20);
        }
    }

    @Test
    public void resetStartingXIClearsXiFlagsAndPositions() {
        long clubId = dao.insertClub(club("ResetFC", true));
        dao.insertSquadPlayer(squadEntry((int) clubId, 1, true, "GK"));
        dao.insertSquadPlayer(squadEntry((int) clubId, 2, true, "ST1"));
        dao.insertSquadPlayer(squadEntry((int) clubId, 3, false, null));

        dao.resetStartingXI((int) clubId);

        for (UserSquad s : dao.getSquadByClubSync((int) clubId)) {
            assertTrue("isStartingXI must be false after reset", !s.isStartingXI());
            assertNull("pitchPosition must be null after reset", s.getPitchPosition());
        }
    }
}
