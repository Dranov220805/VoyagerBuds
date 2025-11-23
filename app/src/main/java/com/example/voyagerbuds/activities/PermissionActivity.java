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

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.voyagerbuds.R;
import com.example.voyagerbuds.utils.PermissionUtils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;

public class PermissionActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "VoyagerBudsPrefs";
    private static final String KEY_PERMISSIONS_REQUESTED = "permissions_requested";

    private Button btnContinue;
    private TextView btnSkip;
    private boolean permissionsRequested = false;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAuth = FirebaseAuth.getInstance();

        // Handle the back button press using the recommended OnBackPressedDispatcher
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // Prevent back press on permission screen, show dialog instead
                showSkipDialog();
            }
        });

        // Hide action bar if it exists
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        // Make status bar icons dark/black. Unnecessary SDK check removed as minSdk is
        // 26.
        getWindow().setStatusBarColor(0xFFFFFFFF); // White status bar
        getWindow().getDecorView().setSystemUiVisibility(
                android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR); // Dark icons

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
                .setTitle(R.string.permissions_required_title)
                .setMessage(message.toString())
                .setPositiveButton(R.string.grant_permissions, (dialog, which) -> PermissionUtils.requestPermissions(
                        this,
                        deniedPermissions.toArray(new String[0]),
                        PermissionUtils.PERMISSION_REQUEST_CODE))
                .setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss())
                .setCancelable(false)
                .show();
    }

    private void showSkipDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.skip_permissions_title)
                .setMessage(R.string.skip_permissions_message)
                .setPositiveButton(R.string.skip_anyway, (dialog, which) -> {
                    savePermissionsRequested();
                    proceedToNextActivity();
                })
                .setNegativeButton(R.string.go_back, (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void showSettingsDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.permissions_required_title)
                .setMessage(R.string.permissions_denied_settings_message)
                .setPositiveButton(R.string.open_settings, (dialog, which) -> openAppSettings())
                .setNegativeButton(R.string.cancel, (dialog, which) -> {
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
                        getString(R.string.some_permissions_denied_message),
                        Toast.LENGTH_LONG).show();
                savePermissionsRequested();
            }
        }
    }

    private void onAllPermissionsGranted() {
        Toast.makeText(this, getString(R.string.permission_all_granted_thanks), Toast.LENGTH_SHORT).show();
        savePermissionsRequested();
        proceedToNextActivity();
    }

    private void savePermissionsRequested() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_PERMISSIONS_REQUESTED, true).apply();
    }

    private void proceedToNextActivity() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            // User is signed in
            startActivity(new Intent(this, HomeActivity.class));
        } else {
            // No user is signed in
            startActivity(new Intent(this, LoginActivity.class));
        }
        finish(); // Finish PermissionActivity so user can't go back to it
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Re-check permissions when returning from Settings
        if (permissionsRequested && PermissionUtils.areAllRequiredPermissionsGranted(this)) {
            onAllPermissionsGranted();
        }
    }
}
