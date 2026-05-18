package hr.fipu.footmash.db;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

import hr.fipu.footmash.model.Fixture;
import hr.fipu.footmash.model.GoalScorer;
import hr.fipu.footmash.model.MatchResult;

@Dao
public interface FixtureDao {

    @Insert
    void insertAll(List<Fixture> fixtures);

    @Insert
    long insertMatchResult(MatchResult result);

    @Insert
    void insertGoalScorer(GoalScorer scorer);

    @Update
    void updateFixture(Fixture fixture);

    @Query("SELECT COUNT(*) FROM fixture WHERE seasonId = :seasonId")
    int countFixtures(int seasonId);

    @Query("SELECT MIN(matchday) FROM fixture WHERE seasonId = :seasonId AND isSimulated = 0")
    LiveData<Integer> getNextMatchdayLive(int seasonId);

    @Query("SELECT MIN(matchday) FROM fixture WHERE seasonId = :seasonId AND isSimulated = 0")
    int getNextMatchdaySync(int seasonId);

    @Query("SELECT * FROM fixture WHERE seasonId = :seasonId AND isUserTeam = 1 AND isSimulated = 0 ORDER BY matchday ASC LIMIT 1")
    LiveData<Fixture> getNextUserFixture(int seasonId);

    @Query("SELECT * FROM fixture WHERE seasonId = :seasonId AND matchday = :matchday ORDER BY id ASC")
    LiveData<List<Fixture>> getFixturesByMatchday(int seasonId, int matchday);

    @Query("SELECT * FROM fixture WHERE seasonId = :seasonId AND matchday = :matchday ORDER BY id ASC")
    List<Fixture> getFixturesByMatchdaySync(int seasonId, int matchday);

    @Query("UPDATE fixture SET isSimulated = 1 WHERE id = :fixtureId")
    void markSimulated(int fixtureId);

    @Query("SELECT * FROM match_result WHERE fixtureId = :fixtureId LIMIT 1")
    MatchResult getMatchResultSync(int fixtureId);

    @Query("SELECT * FROM fixture WHERE seasonId = :seasonId AND isSimulated = 1")
    List<Fixture> getSimulatedFixturesSync(int seasonId);

    @Query("SELECT * FROM goal_scorer WHERE fixtureId = :fixtureId ORDER BY minute ASC")
    List<GoalScorer> getGoalScorersByFixture(int fixtureId);

    @Query("SELECT * FROM goal_scorer WHERE seasonId = :seasonId AND playerName = :playerName")
    List<GoalScorer> getGoalsByPlayer(int seasonId, String playerName);

    @Query("SELECT COUNT(*) FROM goal_scorer WHERE seasonId = :seasonId AND playerName = :playerName")
    int getGoalCountByPlayer(int seasonId, String playerName);
}
