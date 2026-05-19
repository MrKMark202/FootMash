package hr.fipu.footmash.db;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import hr.fipu.footmash.model.CustomPlayer;
import hr.fipu.footmash.model.CustomTeam;
import hr.fipu.footmash.model.Fixture;
import hr.fipu.footmash.model.GoalScorer;
import hr.fipu.footmash.model.MatchResult;
import hr.fipu.footmash.model.RealPlayer;
import hr.fipu.footmash.model.RealTeam;
import hr.fipu.footmash.model.SeasonStanding;
import hr.fipu.footmash.model.UserClub;
import hr.fipu.footmash.model.UserSquad;

@Database(
    entities = {CustomPlayer.class, CustomTeam.class, RealPlayer.class, RealTeam.class,
                UserClub.class, UserSquad.class,
                Fixture.class, MatchResult.class, GoalScorer.class, SeasonStanding.class},
    version = 5,
    exportSchema = false
)
@TypeConverters(Converters.class)
public abstract class AppDatabase extends RoomDatabase {

    private static volatile AppDatabase INSTANCE;

    public abstract CustomPlayerDao customPlayerDao();
    public abstract CustomTeamDao customTeamDao();
    public abstract RealPlayerDao realPlayerDao();
    public abstract RealTeamDao realTeamDao();
    public abstract UserClubDao userClubDao();
    public abstract FixtureDao fixtureDao();
    public abstract StandingDao standingDao();

    static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL(
                "CREATE TABLE IF NOT EXISTS `real_teams` (" +
                "`id` INTEGER NOT NULL, " +
                "`name` TEXT, " +
                "`badgeUrl` TEXT, " +
                "`leagueId` INTEGER NOT NULL, " +
                "`leagueName` TEXT, " +
                "`country` TEXT, " +
                "PRIMARY KEY(`id`))"
            );
            database.execSQL(
                "CREATE TABLE IF NOT EXISTS `real_players` (" +
                "`id` INTEGER NOT NULL, " +
                "`name` TEXT, " +
                "`position` TEXT, " +
                "`nationality` TEXT, " +
                "`age` INTEGER NOT NULL, " +
                "`pace` INTEGER NOT NULL, " +
                "`shooting` INTEGER NOT NULL, " +
                "`passing` INTEGER NOT NULL, " +
                "`dribbling` INTEGER NOT NULL, " +
                "`defending` INTEGER NOT NULL, " +
                "`physical` INTEGER NOT NULL, " +
                "`overall` INTEGER NOT NULL, " +
                "`teamId` INTEGER NOT NULL, " +
                "`teamName` TEXT, " +
                "`leagueId` INTEGER NOT NULL, " +
                "`leagueName` TEXT, " +
                "PRIMARY KEY(`id`))"
            );
        }
    };

    static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL(
                "CREATE TABLE IF NOT EXISTS `user_club` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`clubName` TEXT, " +
                "`leagueId` INTEGER NOT NULL, " +
                "`leagueName` TEXT, " +
                "`formation` TEXT, " +
                "`budget` INTEGER NOT NULL, " +
                "`seasonYear` INTEGER NOT NULL, " +
                "`isActive` INTEGER NOT NULL)"
            );
            database.execSQL(
                "CREATE TABLE IF NOT EXISTS `user_squad` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`clubId` INTEGER NOT NULL, " +
                "`playerId` INTEGER NOT NULL, " +
                "`isStartingXI` INTEGER NOT NULL, " +
                "`pitchPosition` TEXT)"
            );
        }
    };

    static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL(
                "CREATE TABLE IF NOT EXISTS `fixture` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`seasonId` INTEGER NOT NULL, " +
                "`matchday` INTEGER NOT NULL, " +
                "`homeTeamId` INTEGER NOT NULL, " +
                "`homeTeamName` TEXT, " +
                "`awayTeamId` INTEGER NOT NULL, " +
                "`awayTeamName` TEXT, " +
                "`isUserTeam` INTEGER NOT NULL, " +
                "`isSimulated` INTEGER NOT NULL)"
            );
            database.execSQL(
                "CREATE TABLE IF NOT EXISTS `match_result` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`fixtureId` INTEGER NOT NULL, " +
                "`homeGoals` INTEGER NOT NULL, " +
                "`awayGoals` INTEGER NOT NULL, " +
                "`matchSummary` TEXT)"
            );
            database.execSQL(
                "CREATE TABLE IF NOT EXISTS `goal_scorer` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`seasonId` INTEGER NOT NULL, " +
                "`fixtureId` INTEGER NOT NULL, " +
                "`playerName` TEXT, " +
                "`teamName` TEXT, " +
                "`minute` INTEGER NOT NULL, " +
                "`isUserTeamPlayer` INTEGER NOT NULL)"
            );
            database.execSQL(
                "CREATE TABLE IF NOT EXISTS `season_standing` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`seasonId` INTEGER NOT NULL, " +
                "`teamId` INTEGER NOT NULL, " +
                "`teamName` TEXT, " +
                "`played` INTEGER NOT NULL, " +
                "`won` INTEGER NOT NULL, " +
                "`drawn` INTEGER NOT NULL, " +
                "`lost` INTEGER NOT NULL, " +
                "`goalsFor` INTEGER NOT NULL, " +
                "`goalsAgainst` INTEGER NOT NULL, " +
                "`points` INTEGER NOT NULL, " +
                "`isUserTeam` INTEGER NOT NULL)"
            );
        }
    };

    static final Migration MIGRATION_4_5 = new Migration(4, 5) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE user_club ADD COLUMN realTeamSourceId INTEGER");
        }
    };

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            "footmash_database"
                    )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                    .build();
                }
            }
        }
        return INSTANCE;
    }
}
