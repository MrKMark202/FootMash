package hr.fipu.footmash.db;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import hr.fipu.footmash.model.CustomPlayer;
import hr.fipu.footmash.model.CustomTeam;

@Database(entities = {CustomPlayer.class, CustomTeam.class}, version = 1, exportSchema = false)
@TypeConverters(Converters.class)
public abstract class AppDatabase extends RoomDatabase {

    private static volatile AppDatabase INSTANCE;

    public abstract CustomPlayerDao customPlayerDao();
    public abstract CustomTeamDao customTeamDao();

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            "footmash_database"
                    ).build();
                }
            }
        }
        return INSTANCE;
    }
}
