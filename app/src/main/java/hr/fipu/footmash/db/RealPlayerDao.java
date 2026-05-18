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

    @Query("SELECT * FROM real_players WHERE teamId = :teamId ORDER BY overall DESC")
    LiveData<List<RealPlayer>> getPlayersByTeam(int teamId);

    @Query("SELECT * FROM real_players WHERE leagueId = :leagueId AND position = :position ORDER BY overall DESC")
    LiveData<List<RealPlayer>> getPlayersByLeagueAndPosition(int leagueId, String position);

    @Query("SELECT * FROM real_players WHERE id = :playerId")
    RealPlayer getPlayerById(int playerId);

    @Query("SELECT COUNT(*) FROM real_players")
    int getCount();

    @Query("DELETE FROM real_players")
    void deleteAll();
}
