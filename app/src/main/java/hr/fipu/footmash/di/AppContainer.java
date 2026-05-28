package hr.fipu.footmash.di;

import android.content.Context;

import hr.fipu.footmash.ai.GeminiRepository;
import hr.fipu.footmash.db.AppDatabase;
import hr.fipu.footmash.repository.DraftRepository;
import hr.fipu.footmash.repository.SavedGamesRepository;
import hr.fipu.footmash.repository.SeasonRepository;

/**
 * Production wiring for the app's singletons and repositories.
 *
 * <p>This is a lightweight service locator (Google's recommended
 * "no DI library" pattern). Tests construct repositories directly
 * with fakes — they never reach into this container.
 *
 * <p>Reached via {@code FootMashApp.getAppContainer()}.
 */
public class AppContainer {

    private final Context appContext;

    private volatile AppDatabase database;
    private volatile GeminiRepository gemini;
    private volatile DraftRepository draftRepo;
    private volatile SeasonRepository seasonRepo;
    private volatile SavedGamesRepository savedGamesRepo;

    public AppContainer(Context appContext) {
        this.appContext = appContext.getApplicationContext();
    }

    public AppDatabase database() {
        AppDatabase d = database;
        if (d == null) {
            synchronized (this) {
                d = database;
                if (d == null) d = database = AppDatabase.getInstance(appContext);
            }
        }
        return d;
    }

    public GeminiRepository geminiRepository() {
        GeminiRepository g = gemini;
        if (g == null) {
            synchronized (this) {
                g = gemini;
                if (g == null) g = gemini = new GeminiRepository();
            }
        }
        return g;
    }

    public DraftRepository draftRepository() {
        DraftRepository r = draftRepo;
        if (r == null) {
            synchronized (this) {
                r = draftRepo;
                if (r == null) r = draftRepo = new DraftRepository(database());
            }
        }
        return r;
    }

    public SeasonRepository seasonRepository() {
        SeasonRepository r = seasonRepo;
        if (r == null) {
            synchronized (this) {
                r = seasonRepo;
                if (r == null) r = seasonRepo = new SeasonRepository(database(), geminiRepository());
            }
        }
        return r;
    }

    public SavedGamesRepository savedGamesRepository() {
        SavedGamesRepository r = savedGamesRepo;
        if (r == null) {
            synchronized (this) {
                r = savedGamesRepo;
                if (r == null) r = savedGamesRepo = new SavedGamesRepository(database());
            }
        }
        return r;
    }
}
