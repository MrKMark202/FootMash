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

    // --- Season leaderboards (TopScorerRow.goals carries the leaderboard count) ---

    @Query("SELECT playerName, teamName, COUNT(*) AS goals FROM goal_scorer " +
           "WHERE seasonId = :seasonId AND playerName IS NOT NULL AND playerName != '' " +
           "GROUP BY playerName, teamName ORDER BY goals DESC, playerName ASC LIMIT :limit")
    List<TopScorerRow> getTopScorers(int seasonId, int limit);

    @Query("SELECT assistName AS playerName, teamName, COUNT(*) AS goals FROM goal_scorer " +
           "WHERE seasonId = :seasonId AND assistName IS NOT NULL AND assistName != '' " +
           "GROUP BY assistName, teamName ORDER BY goals DESC, assistName ASC LIMIT :limit")
    List<TopScorerRow> getTopAssisters(int seasonId, int limit);

    @Query("SELECT name AS playerName, name AS teamName, COUNT(*) AS goals FROM (" +
           "SELECT f.homeTeamName AS name FROM fixture f " +
           "JOIN match_result m ON m.fixtureId = f.id " +
           "WHERE f.seasonId = :seasonId AND f.isSimulated = 1 AND m.awayGoals = 0 " +
           "UNION ALL " +
           "SELECT f.awayTeamName AS name FROM fixture f " +
           "JOIN match_result m ON m.fixtureId = f.id " +
           "WHERE f.seasonId = :seasonId AND f.isSimulated = 1 AND m.homeGoals = 0" +
           ") WHERE name IS NOT NULL AND name != '' " +
           "GROUP BY name ORDER BY goals DESC, name ASC LIMIT :limit")
    List<TopScorerRow> getTopCleanSheets(int seasonId, int limit);

    @Query("DELETE FROM season_standing WHERE seasonId = :seasonId")
    void deleteStandingsForSeason(int seasonId);
}
