package com.example.voyagerbuds.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.voyagerbuds.R;
import com.example.voyagerbuds.utils.PermissionUtils;

import java.util.ArrayList;
import java.util.List;

public class PermissionActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "VoyagerBudsPrefs";
    private static final String KEY_PERMISSIONS_REQUESTED = "permissions_requested";

    private Button btnContinue;
    private TextView btnSkip;
    private boolean permissionsRequested = false;

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
            // For Android 5.0-5.1, use a slightly darker status bar since we can't change icon color
            getWindow().setStatusBarColor(0xFFE0E0E0); // Light gray
        }

        // Check if permissions are already granted
        if (checkIfShouldProceed()) {
            proceedToNextActivity();
            return;
        }

        setContentView(R.layout.activity_permission);

        initializeViews();
        setupListeners();
    }

    private void initializeViews() {
        btnContinue = findViewById(R.id.btn_continue);
        btnSkip = findViewById(R.id.btn_skip);
    }

    private void setupListeners() {
        btnContinue.setOnClickListener(v -> requestPermissions());
        btnSkip.setOnClickListener(v -> showSkipDialog());
    }

    private boolean checkIfShouldProceed() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        permissionsRequested = prefs.getBoolean(KEY_PERMISSIONS_REQUESTED, false);

        // If all permissions are granted, proceed
        return PermissionUtils.areAllRequiredPermissionsGranted(this);
    }

    private void requestPermissions() {
        String[] permissions = PermissionUtils.getAllRequiredPermissions();
        List<String> deniedPermissions = PermissionUtils.getDeniedPermissions(this, permissions);

        if (deniedPermissions.isEmpty()) {
            // All permissions already granted
            onAllPermissionsGranted();
        } else {
            // Check if we should show rationale for any permission
            boolean shouldShowRationale = false;
            for (String permission : deniedPermissions) {
                if (PermissionUtils.shouldShowRequestPermissionRationale(this, permission)) {
                    shouldShowRationale = true;
                    break;
                }
            }

            if (shouldShowRationale && permissionsRequested) {
                // User has denied before, show explanation
                showPermissionRationaleDialog(deniedPermissions);
            } else {
                // First time or user hasn't permanently denied
                PermissionUtils.requestPermissions(
                        this,
                        deniedPermissions.toArray(new String[0]),
                        PermissionUtils.PERMISSION_REQUEST_CODE);
                permissionsRequested = true;
                savePermissionsRequested();
            }
        }
    }

    private void showPermissionRationaleDialog(List<String> deniedPermissions) {
        StringBuilder message = new StringBuilder("VoyagerBuds needs the following permissions to work properly:\n\n");

        for (String permission : deniedPermissions) {
            message.append("• ").append(PermissionUtils.getPermissionName(permission))
                    .append(": ").append(PermissionUtils.getPermissionDescription(permission))
                    .append("\n\n");
        }

        new AlertDialog.Builder(this)
                .setTitle("Permissions Required")
                .setMessage(message.toString())
                .setPositiveButton("Grant Permissions", (dialog, which) -> {
                    PermissionUtils.requestPermissions(
                            this,
                            deniedPermissions.toArray(new String[0]),
                            PermissionUtils.PERMISSION_REQUEST_CODE);
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .setCancelable(false)
                .show();
    }

    private void showSkipDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Skip Permissions?")
                .setMessage(
                        "Some features may not work properly without these permissions. You can grant them later in Settings.\n\nAre you sure you want to skip?")
                .setPositiveButton("Skip Anyway", (dialog, which) -> {
                    savePermissionsRequested();
                    proceedToNextActivity();
                })
                .setNegativeButton("Go Back", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void showSettingsDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Permissions Required")
                .setMessage(
                        "Some permissions have been permanently denied. Please enable them in Settings to use all features of the app.")
                .setPositiveButton("Open Settings", (dialog, which) -> openAppSettings())
                .setNegativeButton("Cancel", (dialog, which) -> {
                    savePermissionsRequested();
                    proceedToNextActivity();
                })
                .setCancelable(false)
                .show();
    }

    private void openAppSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", getPackageName(), null);
        intent.setData(uri);
        startActivity(intent);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PermissionUtils.PERMISSION_REQUEST_CODE) {
            List<String> deniedPermissions = new ArrayList<>();
            List<String> permanentlyDeniedPermissions = new ArrayList<>();

            for (int i = 0; i < permissions.length; i++) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    deniedPermissions.add(permissions[i]);

                    // Check if permanently denied
                    if (!PermissionUtils.shouldShowRequestPermissionRationale(this, permissions[i])) {
                        permanentlyDeniedPermissions.add(permissions[i]);
                    }
                }
            }

            if (deniedPermissions.isEmpty()) {
                // All permissions granted
                onAllPermissionsGranted();
            } else if (!permanentlyDeniedPermissions.isEmpty()) {
                // Some permissions permanently denied
                showSettingsDialog();
            } else {
                // Some permissions denied but not permanently
                Toast.makeText(this,
                        "Some permissions were denied. You can continue but some features may be limited.",
                        Toast.LENGTH_LONG).show();
                savePermissionsRequested();
            }
        }
    }

    private void onAllPermissionsGranted() {
        Toast.makeText(this, "All permissions granted! Thank you.", Toast.LENGTH_SHORT).show();
        savePermissionsRequested();
        proceedToNextActivity();
    }

    private void savePermissionsRequested() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_PERMISSIONS_REQUESTED, true).apply();
    }

    private void proceedToNextActivity() {
        // TODO: Check if user is logged in, then navigate accordingly
        // For now, go to HomeActivity
        Intent intent = new Intent(this, HomeActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Re-check permissions when returning from Settings
        if (permissionsRequested && PermissionUtils.areAllRequiredPermissionsGranted(this)) {
            onAllPermissionsGranted();
        }
    }

    @Override
    public void onBackPressed() {
        // Prevent back press on permission screen
        showSkipDialog();
    }
}
