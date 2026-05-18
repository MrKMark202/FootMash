package hr.fipu.footmash.db;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

import hr.fipu.footmash.model.RealPlayer;
import hr.fipu.footmash.model.UserClub;
import hr.fipu.footmash.model.UserSquad;

@Dao
public interface UserClubDao {

    @Insert
    long insertClub(UserClub club);

    @Update
    void updateClub(UserClub club);

    @Query("SELECT * FROM user_club WHERE isActive = 1 LIMIT 1")
    LiveData<UserClub> getActiveClub();

    @Query("SELECT * FROM user_club WHERE isActive = 1 LIMIT 1")
    UserClub getActiveClubSync();

    @Query("SELECT * FROM user_club WHERE id = :clubId")
    UserClub getClubByIdSync(int clubId);

    @Query("UPDATE user_club SET isActive = 0")
    void deactivateAll();

    // --- Squad ---

    @Insert
    void insertSquadPlayer(UserSquad entry);

    @Delete
    void removeSquadPlayer(UserSquad entry);

    @Query("DELETE FROM user_squad WHERE playerId = :playerId AND clubId = :clubId")
    void removePlayerFromSquad(int playerId, int clubId);

    @Query("SELECT * FROM user_squad WHERE clubId = :clubId")
    LiveData<List<UserSquad>> getSquadByClub(int clubId);

    @Query("SELECT COUNT(*) FROM user_squad WHERE clubId = :clubId")
    int getSquadCount(int clubId);

    @Query("SELECT real_players.* FROM real_players " +
           "INNER JOIN user_squad ON real_players.id = user_squad.playerId " +
           "WHERE user_squad.clubId = :clubId " +
           "ORDER BY real_players.overall DESC")
    LiveData<List<RealPlayer>> getSignedPlayers(int clubId);

    @Query("SELECT playerId FROM user_squad WHERE clubId = :clubId")
    List<Integer> getSignedPlayerIdsSync(int clubId);

    @Query("SELECT playerId FROM user_squad WHERE clubId = :clubId")
    LiveData<List<Integer>> getSignedPlayerIds(int clubId);

    @Query("SELECT * FROM user_squad WHERE clubId = :clubId")
    List<UserSquad> getSquadByClubSync(int clubId);

    @Update
    void updateSquadPlayer(UserSquad entry);

    @Query("SELECT * FROM user_squad WHERE playerId = :playerId AND clubId = :clubId LIMIT 1")
    UserSquad getSquadEntryByPlayer(int playerId, int clubId);

    @Query("UPDATE user_squad SET isStartingXI = 0, pitchPosition = NULL WHERE clubId = :clubId")
    void resetStartingXI(int clubId);
}
