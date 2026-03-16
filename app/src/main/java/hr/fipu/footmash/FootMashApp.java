package hr.fipu.footmash;

import android.app.Application;

/**
 * Application klasa za FootMash.
 * Inicijalizira globalne resurse.
 */
public class FootMashApp extends Application {

    private static FootMashApp instance;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
    }

    public static FootMashApp getInstance() {
        return instance;
    }
}
