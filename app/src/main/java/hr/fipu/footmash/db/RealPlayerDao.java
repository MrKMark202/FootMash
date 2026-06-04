package hr.fipu.footmash.db;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

import hr.fipu.footmash.model.RealPlayer;

@Dao
public interface RealPlayerDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<RealPlayer> players);

    @Query("SELECT * FROM real_players WHERE leagueId = :leagueId ORDER BY overall DESC")
    LiveData<List<RealPlayer>> getPlayersByLeague(int leagueId);

    /** Every seeded player across all leagues — used for the cross-league market. */
    @Query("SELECT * FROM real_players ORDER BY overall DESC")
    LiveData<List<RealPlayer>> getAllPlayers();

    @Query("SELECT * FROM real_players WHERE teamId = :teamId ORDER BY overall DESC")
    LiveData<List<RealPlayer>> getPlayersByTeam(int teamId);

    @Query("SELECT * FROM real_players WHERE teamId = :teamId ORDER BY overall DESC")
    List<RealPlayer> getPlayersByTeamSync(int teamId);

    @Query("SELECT * FROM real_players WHERE leagueId = :leagueId AND position = :position ORDER BY overall DESC")
    LiveData<List<RealPlayer>> getPlayersByLeagueAndPosition(int leagueId, String position);

    @Query("SELECT * FROM real_players WHERE id = :playerId")
    RealPlayer getPlayerById(int playerId);

    /** Every real player matching any of the given nationality strings — used to
     *  assemble a nation's World Cup pool. */
    @Query("SELECT * FROM real_players WHERE nationality IN (:nationalities) ORDER BY overall DESC")
    List<RealPlayer> getPlayersByNationalityInSync(List<String> nationalities);

    @Query("SELECT COUNT(*) FROM real_players")
    int getCount();

    @Query("DELETE FROM real_players")
    void deleteAll();

    /** Sets a player's dynamic form delta (added to overall in-season). */
    @Query("UPDATE real_players SET formDelta = :delta WHERE id = :playerId")
    void setFormDelta(int playerId, int delta);

    /** Clears all form growth/decline — called when a new season begins. */
    @Query("UPDATE real_players SET formDelta = 0")
    void resetAllFormDelta();
}
