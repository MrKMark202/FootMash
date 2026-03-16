package hr.fipu.footmash.db;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;
import hr.fipu.footmash.model.CustomPlayer;

@Dao
public interface CustomPlayerDao {

    @Insert
    long insert(CustomPlayer player);

    @Update
    void update(CustomPlayer player);

    @Delete
    void delete(CustomPlayer player);

    @Query("SELECT * FROM custom_players ORDER BY id DESC")
    LiveData<List<CustomPlayer>> getAllPlayers();

    @Query("SELECT * FROM custom_players WHERE id = :playerId")
    LiveData<CustomPlayer> getPlayerById(int playerId);

    @Query("SELECT * FROM custom_players WHERE targetTeamId = :teamId")
    LiveData<List<CustomPlayer>> getPlayersByTeam(int teamId);

    @Query("DELETE FROM custom_players")
    void deleteAll();
}
