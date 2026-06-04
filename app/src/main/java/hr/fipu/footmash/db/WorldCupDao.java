package hr.fipu.footmash.db;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import hr.fipu.footmash.model.WorldCupState;

@Dao
public interface WorldCupDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(WorldCupState state);

    @Query("SELECT * FROM world_cup WHERE id = " + WorldCupState.ROW_ID)
    WorldCupState getSync();

    @Query("SELECT * FROM world_cup WHERE id = " + WorldCupState.ROW_ID)
    LiveData<WorldCupState> get();

    @Query("DELETE FROM world_cup")
    void clear();
}
