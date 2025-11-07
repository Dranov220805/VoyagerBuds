package com.example.voyagerbuds.activities;

import android.os.Build;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.example.voyagerbuds.R;
import com.example.voyagerbuds.fragments.*;

public class HomeActivity extends AppCompatActivity
        implements NavigationBarFragment.OnNavigationItemSelectedListener,
        UpperBarFragment.OnProfileClickListener {

    private String currentFragment = "home";
    private static final String[] FRAGMENT_ORDER = { "home", "trip", "capture", "map", "dashboard" };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Ensure content doesn't go behind system bars - let fitsSystemWindows handle
        // padding
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // On Android 11+, explicitly tell the window to respect system windows
            getWindow().setDecorFitsSystemWindows(true);
        }

        setContentView(R.layout.activity_home);

        if (savedInstanceState == null) {
            // Add upper bar fragment
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.upper_bar_container, new UpperBarFragment())
                    .commit();

            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.content_container, new HomeFragment())
                    .commit();

            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.navigation_container, new NavigationBarFragment())
                    .commit();
        }
    }

    @Override
    public void onNavigationItemSelected(String item) {
        // Don't do anything if we're already on this fragment
        if (currentFragment.equals(item)) {
            return;
        }

        Fragment selected = null;
        switch (item) {
            case "home":
                selected = new HomeFragment();
                break;
            case "trip":
                selected = new TripFragment();
                break;
            case "capture":
                selected = new CaptureFragment();
                break;
            case "map":
                selected = new MapFragment();
                break;
            case "dashboard":
                selected = new DashboardFragment();
                break;
        }

        if (selected != null) {
            // Determine navigation direction
            int currentIndex = getFragmentIndex(currentFragment);
            int newIndex = getFragmentIndex(item);

            // Choose animation based on direction
            int enterAnim, exitAnim;
            if (newIndex > currentIndex) {
                // Moving right
                enterAnim = R.anim.slide_in_right;
                exitAnim = R.anim.slide_out_left;
            } else {
                // Moving left
                enterAnim = R.anim.slide_in_left;
                exitAnim = R.anim.slide_out_right;
            }

            getSupportFragmentManager().beginTransaction()
                    .setCustomAnimations(enterAnim, exitAnim)
                    .replace(R.id.content_container, selected)
                    .commit();

            currentFragment = item;
        }
    }

    private int getFragmentIndex(String fragmentName) {
        for (int i = 0; i < FRAGMENT_ORDER.length; i++) {
            if (FRAGMENT_ORDER[i].equals(fragmentName)) {
                return i;
            }
        }
        return 0;
    }

    @Override
    public void onProfileClicked() {
        // Navigate to profile fragment with fade animation
        Fragment profileFragment = new ProfileFragment();

        getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(R.anim.fade_in, R.anim.fade_out)
                .replace(R.id.content_container, profileFragment)
                .addToBackStack(null) // Add to back stack so user can navigate back
                .commit();

        // Update current fragment tracker
        currentFragment = "profile";
    }
}