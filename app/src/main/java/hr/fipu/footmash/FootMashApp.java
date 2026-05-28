package hr.fipu.footmash;

import android.app.Application;
import android.content.Context;

import hr.fipu.footmash.db.SeedLoader;
import hr.fipu.footmash.di.AppContainer;

public class FootMashApp extends Application {

    private static FootMashApp instance;
    private AppContainer appContainer;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        appContainer = new AppContainer(this);
        SeedLoader.loadIfNeeded(this);
    }

    public static FootMashApp getInstance() {
        return instance;
    }

    public AppContainer getAppContainer() {
        return appContainer;
    }

    /** Convenience: fetch the container from anything that exposes a Context. */
    public static AppContainer container(Context context) {
        return ((FootMashApp) context.getApplicationContext()).appContainer;
    }
}
