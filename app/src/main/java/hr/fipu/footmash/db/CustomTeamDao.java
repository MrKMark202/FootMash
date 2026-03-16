package hr.fipu.footmash.db;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;
import hr.fipu.footmash.model.CustomTeam;

@Dao
public interface CustomTeamDao {

    @Insert
    long insert(CustomTeam team);

    @Update
    void update(CustomTeam team);

    @Delete
    void delete(CustomTeam team);

    @Query("SELECT * FROM custom_teams ORDER BY id DESC")
    LiveData<List<CustomTeam>> getAllTeams();

    @Query("SELECT * FROM custom_teams WHERE id = :teamId")
    LiveData<CustomTeam> getTeamById(int teamId);

    @Query("DELETE FROM custom_teams")
    void deleteAll();
}
