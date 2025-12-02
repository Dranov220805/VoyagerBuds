package com.example.voyagerbuds.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.example.voyagerbuds.R;
import com.example.voyagerbuds.fragments.AddScheduleStepFragment;
import com.example.voyagerbuds.fragments.CaptureFragment;
import com.example.voyagerbuds.fragments.CreateTripFragment;
import com.example.voyagerbuds.fragments.DashboardFragment;
import com.example.voyagerbuds.fragments.HomeFragment;
import com.example.voyagerbuds.fragments.MapFragment;
import com.example.voyagerbuds.fragments.NavigationBarFragment;
import com.example.voyagerbuds.fragments.ProfileFragment;
import com.example.voyagerbuds.fragments.ScheduleFragment;
import com.example.voyagerbuds.receivers.NotificationReceiver;
import com.example.voyagerbuds.utils.LocaleHelper;

public class HomeActivity extends BaseActivity
        implements NavigationBarFragment.OnNavigationItemSelectedListener,
        CreateTripFragment.OnTripCreatedListener {

    private String currentFragment = "home";
    private NavigationBarFragment navigationBarFragment;
    private static final String[] FRAGMENT_ORDER = { "home", "schedule", "capture", "map", "dashboard" };
    private CreateTripFragment createTripFragment;
    private String pendingNavigationItem = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Hide action bar if it exists
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        // Make status bar icons dark/black for contrast on light background
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getWindow().setStatusBarColor(0xFFFFFFFF); // White status bar
            getWindow().getDecorView().setSystemUiVisibility(
                    android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR); // Dark icons
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // For Android 5.0-5.1, use a slightly darker status bar since we can't change
            // icon color
            getWindow().setStatusBarColor(0xFFE0E0E0); // Light gray
        }

        // Ensure content doesn't go behind system bars - let fitsSystemWindows handle
        // padding
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // On Android 11+, explicitly tell the window to respect system windows
            getWindow().setDecorFitsSystemWindows(true);
        }

        setContentView(R.layout.activity_home);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.content_container, new HomeFragment())
                    .commit();

            navigationBarFragment = new NavigationBarFragment();
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.navigation_container, navigationBarFragment)
                    .commit();
        } else {
            // Restore state
            currentFragment = savedInstanceState.getString("currentFragment", "home");

            // Restore fragment references
            navigationBarFragment = (NavigationBarFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.navigation_container);

            Fragment contentFragment = getSupportFragmentManager().findFragmentById(R.id.content_container);
            if (contentFragment instanceof CreateTripFragment) {
                createTripFragment = (CreateTripFragment) contentFragment;
                createTripFragment.setListener(this);
            }

            // Update UI based on restored state
        }

        handleNotificationIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleNotificationIntent(intent);
    }

    private void handleNotificationIntent(Intent intent) {
        if (intent != null && intent.hasExtra(NotificationReceiver.EXTRA_SCHEDULE_ID)) {
            int scheduleId = intent.getIntExtra(NotificationReceiver.EXTRA_SCHEDULE_ID, -1);
            if (scheduleId != -1) {
                // Navigate to ScheduleFragment
                ScheduleFragment scheduleFragment = new ScheduleFragment();
                Bundle args = new Bundle();
                args.putInt(ScheduleFragment.ARG_SCHEDULE_ID, scheduleId);
                scheduleFragment.setArguments(args);

                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.content_container, scheduleFragment)
                        .commit();

                // Update current fragment state
                currentFragment = "schedule";

                // Update navigation bar selection if possible
                if (navigationBarFragment != null) {
                    // We need to expose a method in NavigationBarFragment to set selection
                    // For now, we can just rely on the fragment change
                }
            }
        }
    }

    @Override
    public void onNavigationItemSelected(String item) {
        // Check if we're in trip creation mode
        if (currentFragment.equals("create_trip") && createTripFragment != null) {
            if (!createTripFragment.isTripCreationComplete()) {
                pendingNavigationItem = item;
                createTripFragment.showConfirmationDialog();
                return;
            }
        }

        // Don't do anything if we're already on this fragment
        if (currentFragment.equals(item)) {
            return;
        }

        Fragment selected = null;
        switch (item) {
            case "home":
                selected = new HomeFragment();
                break;
            case "schedule":
                selected = new ScheduleFragment();
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
            navigateToFragment(selected, item);
        }
    }

    private void navigateToFragment(Fragment fragment, String fragmentKey) {
        // Determine navigation direction
        int currentIndex = getFragmentIndex(currentFragment);
        int newIndex = getFragmentIndex(fragmentKey);

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
                .replace(R.id.content_container, fragment)
                .commit();

        // Update navigation bar selection
        if (navigationBarFragment != null) {
            navigationBarFragment.updateSelection(fragmentKey);
        }

        currentFragment = fragmentKey;
        createTripFragment = null;
    }

    // Helper to map fragment id to a display title
    private String getTitleForFragment(String fragmentKey) {
        switch (fragmentKey) {
            case "home":
                return getString(R.string.app_name);
            case "schedule":
                return getString(R.string.schedule);
            case "capture":
                return getString(R.string.capture);
            case "map":
                return getString(R.string.map);
            case "dashboard":
                return getString(R.string.dashboard);
            case "create_trip":
                return getString(R.string.create_trip);
            default:
                return getString(R.string.app_name);
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

    public void onProfileClicked() {
        // Check if we're in trip creation mode
        if (currentFragment.equals("create_trip") && createTripFragment != null) {
            if (!createTripFragment.isTripCreationComplete()) {
                pendingNavigationItem = "profile";
                createTripFragment.showConfirmationDialog();
                return;
            }
        }

        // Navigate to profile fragment with fade animation
        Fragment profileFragment = new ProfileFragment();

        getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(R.anim.fade_in, R.anim.fade_out)
                .replace(R.id.content_container, profileFragment)
                .addToBackStack(null) // Add to back stack so user can navigate back
                .commit();

        // Update current fragment tracker
        currentFragment = "profile";
        createTripFragment = null;
    }

    public void showCreateTripFragment() {
        createTripFragment = new CreateTripFragment();
        createTripFragment.setListener(this);

        getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left)
                .replace(R.id.content_container, createTripFragment)
                .commit();

        currentFragment = "create_trip";
    }

    @Override
    public void onTripCreated(long tripId) {
        // After trip creation, show the add-schedule step so user can add schedules now
        // or later
        com.example.voyagerbuds.fragments.AddScheduleStepFragment addScheduleFragment = com.example.voyagerbuds.fragments.AddScheduleStepFragment
                .newInstance(tripId);

        getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left)
                .replace(R.id.content_container, addScheduleFragment)
                .commit();

        currentFragment = "add_schedule";
        createTripFragment = null;
    }

    @Override
    public void onCancelTripCreation() {
        // Navigate to pending destination or home if none
        String destination = (pendingNavigationItem != null) ? pendingNavigationItem : "home";
        pendingNavigationItem = null;

        // Handle profile separately since it's not in the navigation
        if ("profile".equals(destination)) {
            Fragment profileFragment = new ProfileFragment();
            getSupportFragmentManager().beginTransaction()
                    .setCustomAnimations(R.anim.fade_in, R.anim.fade_out)
                    .replace(R.id.content_container, profileFragment)
                    .addToBackStack(null)
                    .commit();
            currentFragment = "profile";
            createTripFragment = null;
            return;
        }

        Fragment selected = null;
        switch (destination) {
            case "home":
                selected = new HomeFragment();
                break;
            case "schedule":
                selected = new ScheduleFragment();
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
            navigateToFragment(selected, destination);
        }
    }

    @Override
    public void onBackPressed() {
        if (currentFragment.equals("create_trip") && createTripFragment != null) {
            if (createTripFragment.handleBackPress()) {
                return; // Back press was handled by the fragment
            }
        }
        super.onBackPressed();
    }

    /**
     * Allow fragments to inform the activity about the current fragment key so
     * the upper bar and navigation selection update accordingly.
     */
    public void setCurrentFragmentKey(String fragmentKey) {
        this.currentFragment = fragmentKey;
        if (navigationBarFragment != null) {
            navigationBarFragment.updateSelection(fragmentKey);
        }
        createTripFragment = null;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("currentFragment", currentFragment);
    }
}