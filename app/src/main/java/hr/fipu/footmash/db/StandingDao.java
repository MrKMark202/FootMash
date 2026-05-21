package hr.fipu.footmash.db;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

import hr.fipu.footmash.model.SeasonStanding;

@Dao
public interface StandingDao {

    @Insert
    void insertAll(List<SeasonStanding> standings);

    @Update
    void update(SeasonStanding standing);

    @Query("SELECT COUNT(*) FROM season_standing WHERE seasonId = :seasonId")
    int count(int seasonId);

    @Query("SELECT * FROM season_standing WHERE seasonId = :seasonId " +
           "ORDER BY points DESC, (goalsFor - goalsAgainst) DESC, goalsFor DESC")
    LiveData<List<SeasonStanding>> getStandings(int seasonId);

    @Query("SELECT * FROM season_standing WHERE seasonId = :seasonId " +
           "ORDER BY points DESC, (goalsFor - goalsAgainst) DESC, goalsFor DESC")
    List<SeasonStanding> getStandingsSync(int seasonId);

    @Query("SELECT * FROM season_standing WHERE seasonId = :seasonId AND teamName = :teamName LIMIT 1")
    SeasonStanding getByTeamName(int seasonId, String teamName);

    @Query("SELECT * FROM season_standing WHERE seasonId = :seasonId AND isUserTeam = 1 LIMIT 1")
    LiveData<SeasonStanding> getUserStanding(int seasonId);

    @Query("SELECT playerName, teamName, COUNT(*) AS goals FROM goal_scorer " +
           "WHERE seasonId = :seasonId GROUP BY playerName, teamName ORDER BY goals DESC LIMIT 20")
    List<TopScorerRow> getTopScorers(int seasonId);

    @Query("DELETE FROM season_standing WHERE seasonId = :seasonId")
    void deleteStandingsForSeason(int seasonId);
}
