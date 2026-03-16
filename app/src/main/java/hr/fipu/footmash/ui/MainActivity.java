package hr.fipu.footmash.ui;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import android.widget.EditText;

import hr.fipu.footmash.R;
import hr.fipu.footmash.api.ApiClient;

public class MainActivity extends AppCompatActivity {

    private NavController navController;
    private BottomNavigationView bottomNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Učitaj API ključ iz SharedPreferences
        loadApiKey();

        // Setup navigation
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        navController = navHostFragment.getNavController();

        bottomNav = findViewById(R.id.bottom_navigation);
        NavigationUI.setupWithNavController(bottomNav, navController);

        // Sakrij bottom nav na detail ekranima
        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            int id = destination.getId();
            if (id == R.id.nav_home || id == R.id.nav_leagues
                    || id == R.id.nav_teams || id == R.id.nav_ai_lab) {
                bottomNav.setVisibility(View.VISIBLE);
            } else {
                bottomNav.setVisibility(View.GONE);
            }
        });

        // Ako nema API ključa, pokaži dijalog
        if (ApiClient.getApiKey().isEmpty()) {
            showApiKeyDialog();
        }
    }

    private void loadApiKey() {
        SharedPreferences prefs = getSharedPreferences("footmash_prefs", MODE_PRIVATE);
        String key = prefs.getString("api_key", "");
        if (!key.isEmpty()) {
            ApiClient.setApiKey(key);
        }
    }

    public void saveApiKey(String key) {
        SharedPreferences prefs = getSharedPreferences("footmash_prefs", MODE_PRIVATE);
        prefs.edit().putString("api_key", key).apply();
        ApiClient.setApiKey(key);
    }

    private void showApiKeyDialog() {
        EditText input = new EditText(this);
        input.setHint("Unesite vaš API-Football ključ");
        input.setPadding(48, 32, 48, 32);

        new MaterialAlertDialogBuilder(this)
                .setTitle("API Ključ")
                .setMessage("Za korištenje aplikacije, unesite API ključ sa api-football.com")
                .setView(input)
                .setPositiveButton("Spremi", (dialog, which) -> {
                    String key = input.getText().toString().trim();
                    if (!key.isEmpty()) {
                        saveApiKey(key);
                    }
                })
                .setNegativeButton("Kasnije", null)
                .setCancelable(true)
                .show();
    }

    public NavController getNavController() {
        return navController;
    }
}
