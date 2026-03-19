package hr.fipu.footmash.ui;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import hr.fipu.footmash.R;
import hr.fipu.footmash.api.ApiClient;

public class MainActivity extends AppCompatActivity {

    private NavController navController;
    private BottomNavigationView bottomNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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
    }

    public NavController getNavController() {
        return navController;
    }
}
