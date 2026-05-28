package hr.fipu.footmash.repository;

import hr.fipu.footmash.db.AppDatabase;
import hr.fipu.footmash.model.CustomPlayer;
import hr.fipu.footmash.model.UserClub;

/**
 * Central place for "delete an entire saved game" operations. Both supported
 * flows persist child rows across several tables, so the cascade has to be
 * spelled out — Room ForeignKey CASCADE would require schema changes and the
 * affected tables are mature.
 *
 * <p>All methods are synchronous and intended to run on a background thread;
 * caller wraps them in a {@code new Thread(...).start()} or similar.
 */
public class SavedGamesRepository {

    private final AppDatabase db;

    public SavedGamesRepository(AppDatabase db) {
        this.db = db;
    }

    /**
     * Removes a custom player and every {@code PlayerCareerSeason} attached
     * to them. Safe to call for a player with no career rows.
     *
     * @return {@code true} if the player existed and was deleted
     */
    public boolean deletePlayer(int playerId) {
        CustomPlayer player = db.customPlayerDao().getPlayerByIdSync(playerId);
        if (player == null) return false;
        db.playerCareerSeasonDao().deleteAllForPlayer(playerId);
        db.customPlayerDao().delete(player);
        return true;
    }

    /**
     * Removes a user club and the full Season Mode footprint that belongs to
     * it: squad assignments, fixtures, match results, goal scorers, standings.
     * Other clubs' data is untouched because every child table is keyed on
     * the club id (== season id) the season engine uses.
     *
     * @return {@code true} if the club existed and was deleted
     */
    public boolean deleteClub(int clubId) {
        UserClub club = db.userClubDao().getClubByIdSync(clubId);
        if (club == null) return false;

        // Match results live behind fixtures, so wipe them before fixtures.
        db.fixtureDao().deleteMatchResultsForSeason(clubId);
        db.fixtureDao().deleteGoalScorersForSeason(clubId);
        db.fixtureDao().deleteFixturesForSeason(clubId);
        db.standingDao().deleteStandingsForSeason(clubId);
        db.userClubDao().deleteSquadForClub(clubId);
        db.userClubDao().deleteClub(club);
        return true;
    }
}
