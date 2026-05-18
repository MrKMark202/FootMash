package hr.fipu.footmash;

import android.app.Application;

import hr.fipu.footmash.db.SeedLoader;

public class FootMashApp extends Application {

    private static FootMashApp instance;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        SeedLoader.loadIfNeeded(this);
    }

    public static FootMashApp getInstance() {
        return instance;
    }
}
