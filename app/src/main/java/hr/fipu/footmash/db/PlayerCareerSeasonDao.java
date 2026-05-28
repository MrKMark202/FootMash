package hr.fipu.footmash.db;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

import hr.fipu.footmash.model.PlayerCareerSeason;

@Dao
public interface PlayerCareerSeasonDao {

    @Insert
    long insert(PlayerCareerSeason season);

    @Query("SELECT * FROM player_career_season WHERE playerId = :playerId "
         + "ORDER BY seasonYear ASC")
    LiveData<List<PlayerCareerSeason>> getByPlayer(int playerId);

    @Query("SELECT * FROM player_career_season WHERE playerId = :playerId "
         + "ORDER BY seasonYear ASC")
    List<PlayerCareerSeason> getByPlayerSync(int playerId);

    /**
     * Counts how many seasons the player has spent at a given club. Used by
     * the transfer-window engine: once this hits 2 for the current club, the
     * career hub surfaces the "Prelazni rok" CTA.
     */
    @Query("SELECT COUNT(*) FROM player_career_season "
         + "WHERE playerId = :playerId AND clubId = :clubId")
    int countSeasonsAtClub(int playerId, int clubId);

    @Query("DELETE FROM player_career_season WHERE playerId = :playerId")
    void deleteAllForPlayer(int playerId);
}
