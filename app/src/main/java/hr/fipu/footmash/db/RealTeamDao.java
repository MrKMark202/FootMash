package hr.fipu.footmash.db;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

import hr.fipu.footmash.model.LeagueInfo;
import hr.fipu.footmash.model.RealTeam;

@Dao
public interface RealTeamDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<RealTeam> teams);

    @Query("SELECT * FROM real_teams WHERE leagueId = :leagueId ORDER BY name ASC")
    LiveData<List<RealTeam>> getTeamsByLeague(int leagueId);

    @Query("SELECT * FROM real_teams WHERE id = :teamId")
    RealTeam getTeamById(int teamId);

    @Query("SELECT * FROM real_teams WHERE leagueId = :leagueId ORDER BY name ASC")
    List<RealTeam> getTeamsByLeagueSync(int leagueId);

    @Query("SELECT DISTINCT leagueId, leagueName FROM real_teams ORDER BY leagueName ASC")
    LiveData<List<LeagueInfo>> getDistinctLeagues();

    @Query("SELECT COUNT(*) FROM real_teams")
    int getCount();

    @Query("DELETE FROM real_teams")
    void deleteAll();
}
