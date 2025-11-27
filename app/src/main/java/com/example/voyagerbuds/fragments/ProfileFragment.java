package com.example.voyagerbuds.fragments;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.voyagerbuds.R;
import com.example.voyagerbuds.activities.LoginActivity;
import com.example.voyagerbuds.utils.LocaleHelper;
import com.example.voyagerbuds.utils.ThemeHelper;
import com.example.voyagerbuds.database.DatabaseHelper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link ProfileFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ProfileFragment extends Fragment {

    private TextView tvCurrentLanguage;
    private TextView tvCurrentTheme;
    private LinearLayout btnLanguage;
    private LinearLayout btnTheme;
    private Button btnLogout;
    private TextView tvNotifications;
    private SwitchCompat switchNotifications;
    private TextView tvPrivacy;
    private TextView tvLanguageLabel;
    private TextView tvThemeLabel;
    private TextView tvHelp;
    private LinearLayout btnBackupData;
    private LinearLayout btnRestoreData;
    private LinearLayout btnTestFirestore;
    private TextView tvUserName;
    private TextView tvUserEmail;
    private FirebaseAuth mAuth;

    public ProfileFragment() {
        // Required empty public constructor
    }

    public static ProfileFragment newInstance() {
        return new ProfileFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAuth = FirebaseAuth.getInstance();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize views
        btnLanguage = view.findViewById(R.id.btn_language);
        tvCurrentLanguage = view.findViewById(R.id.tv_current_language);
        btnTheme = view.findViewById(R.id.btn_theme);
        tvCurrentTheme = view.findViewById(R.id.tv_current_theme);
        tvUserName = view.findViewById(R.id.tv_user_name);
        tvUserEmail = view.findViewById(R.id.tv_user_email);
        btnLogout = view.findViewById(R.id.btn_logout);
        tvNotifications = view.findViewById(R.id.tv_notifications);
        switchNotifications = view.findViewById(R.id.switch_notifications);
        tvPrivacy = view.findViewById(R.id.tv_privacy);
        tvLanguageLabel = view.findViewById(R.id.tv_language_label);
        tvThemeLabel = view.findViewById(R.id.tv_theme_label);
        tvHelp = view.findViewById(R.id.tv_help);
        btnBackupData = view.findViewById(R.id.btn_backup_data);
        btnRestoreData = view.findViewById(R.id.btn_restore_data);
        btnTestFirestore = view.findViewById(R.id.btn_test_firestore);

        // Setup Notifications Switch
        SharedPreferences prefs = requireContext().getSharedPreferences("VoyagerBudsPrefs", Context.MODE_PRIVATE);
        boolean notificationsEnabled = prefs.getBoolean("notifications_enabled", true);
        switchNotifications.setChecked(notificationsEnabled);

        switchNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("notifications_enabled", isChecked).apply();
        });

        // Update current language display
        updateLanguageDisplay();

        // Update current theme display
        updateThemeDisplay();

        // Set up language selector
        btnLanguage.setOnClickListener(v -> showLanguageDialog());

        // Set up theme selector
        btnTheme.setOnClickListener(v -> showThemeDialog());

        // Set up logout button
        btnLogout.setOnClickListener(v -> {
            // Clear user session data
            com.example.voyagerbuds.utils.UserSessionManager.clearSession(requireContext());
            mAuth.signOut();
            Intent intent = new Intent(getActivity(), LoginActivity.class);
            // Clear the back stack and start a new task
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            getActivity().finish();
        });

        // Update user name and email display
        updateUserDisplay();

        // Setup backup/restore click handlers
        btnBackupData.setOnClickListener(v -> {
            if (!com.example.voyagerbuds.utils.UserSessionManager.isUserLoggedIn()) {
                android.widget.Toast
                        .makeText(getContext(), "Please log in to back up data", android.widget.Toast.LENGTH_SHORT)
                        .show();
                return;
            }

            android.app.AlertDialog progress = new android.app.AlertDialog.Builder(requireContext())
                    .setTitle("Backing Up")
                    .setMessage("Please wait while we upload data to Firebase...")
                    .setCancelable(false)
                    .create();
            progress.show();

            DatabaseHelper dbHelper = new DatabaseHelper(requireContext());
            com.example.voyagerbuds.firebase.FirebaseBackupManager.backupAllData(requireContext(), mAuth, dbHelper,
                    new com.example.voyagerbuds.firebase.FirebaseBackupManager.Callback() {
                        @Override
                        public void onSuccess() {
                            progress.dismiss();
                            android.widget.Toast.makeText(requireContext(), "Backup completed successfully",
                                    android.widget.Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onFailure(String error) {
                            progress.dismiss();
                            android.util.Log.w("ProfileFragment", "Backup failed: " + error);
                            if (error != null && error.toLowerCase().contains("permission denied")) {
                                // Show troubleshooting dialog with direct action to open Firebase console
                                new android.app.AlertDialog.Builder(requireContext())
                                        .setTitle(getString(R.string.firestore_troubleshoot_title))
                                        .setMessage(getString(R.string.firestore_troubleshoot_message))
                                        .setPositiveButton(getString(R.string.open_firebase_console), (d, w) -> {
                                            android.content.Intent intent = new android.content.Intent(
                                                    android.content.Intent.ACTION_VIEW);
                                            intent.setData(
                                                    android.net.Uri.parse("https://console.firebase.google.com/"));
                                            startActivity(intent);
                                        })
                                        .setNeutralButton("Copy error", (d, w) -> {
                                            android.content.ClipboardManager clipboard = (android.content.ClipboardManager) requireContext()
                                                    .getSystemService(android.content.Context.CLIPBOARD_SERVICE);
                                            android.content.ClipData clip = android.content.ClipData
                                                    .newPlainText("backup_error", error);
                                            if (clipboard != null)
                                                clipboard.setPrimaryClip(clip);
                                            android.widget.Toast.makeText(requireContext(), "Copied to clipboard",
                                                    android.widget.Toast.LENGTH_SHORT).show();
                                        })
                                        .setNegativeButton("OK", null)
                                        .show();
                            } else {
                                // General error dialog with copy option
                                new android.app.AlertDialog.Builder(requireContext())
                                        .setTitle("Backup Failed")
                                        .setMessage(error)
                                        .setPositiveButton("Copy details", (d, w) -> {
                                            android.content.ClipboardManager clipboard = (android.content.ClipboardManager) requireContext()
                                                    .getSystemService(android.content.Context.CLIPBOARD_SERVICE);
                                            android.content.ClipData clip = android.content.ClipData
                                                    .newPlainText("backup_error", error);
                                            if (clipboard != null)
                                                clipboard.setPrimaryClip(clip);
                                            android.widget.Toast.makeText(requireContext(), "Copied to clipboard",
                                                    android.widget.Toast.LENGTH_SHORT).show();
                                        })
                                        .setNegativeButton("OK", null)
                                        .show();
                            }
                        }
                    });
        });

        btnRestoreData.setOnClickListener(v -> {
            if (!com.example.voyagerbuds.utils.UserSessionManager.isUserLoggedIn()) {
                android.widget.Toast
                        .makeText(getContext(), "Please log in to restore data", android.widget.Toast.LENGTH_SHORT)
                        .show();
                return;
            }

                // Show a restore preview first (counts of local vs remote data)
                DatabaseHelper dbHelper = new DatabaseHelper(requireContext());
                android.app.AlertDialog progress = new android.app.AlertDialog.Builder(requireContext())
                    .setTitle("Fetching Backup Preview")
                    .setMessage("Please wait while we check your backup...")
                    .setCancelable(false)
                    .create();
                progress.show();

                com.example.voyagerbuds.firebase.FirebaseBackupManager.fetchBackupPreview(requireContext(), mAuth, dbHelper,
                    new com.example.voyagerbuds.firebase.FirebaseBackupManager.PreviewCallback() {
                    @Override
                    public void onPreview(com.example.voyagerbuds.firebase.FirebaseBackupManager.BackupPreview preview) {
                        progress.dismiss();
                        // Compute local counts
                        int localUserId = com.example.voyagerbuds.utils.UserSessionManager
                            .getCurrentUserId(requireContext());
                        int localTrips = dbHelper.getAllTrips(localUserId).size();
                        int localSchedules = dbHelper.getTotalSchedulesForUser(localUserId);
                        int localExpenses = dbHelper.getTotalExpensesForUser(localUserId);
                        int localCaptures = dbHelper.getTotalCapturesForUser(localUserId);

                        // Build a compare message
                        StringBuilder sb = new StringBuilder();
                        sb.append("Local: " + localTrips + " trips, " + localSchedules + " schedules, " + localExpenses + " expenses, " + localCaptures + " captures\n\n");
                            sb.append("Remote Backup: " + preview.tripCount + " trips, " + preview.scheduleCount + " schedules, " + preview.expenseCount + " expenses, " + preview.captureCount + " captures\n\n");
                            // add sample remote trip names (up to 5)
                            int toShow = Math.min(5, preview.trips.size());
                            if (toShow > 0) {
                                sb.append("Remote Trips (sample): \n");
                                for (int i = 0; i < toShow; i++) {
                                    com.example.voyagerbuds.firebase.FirebaseBackupManager.TripSummary ts = preview.trips.get(i);
                                    sb.append(" - " + ts.tripName + " (" + ts.scheduleCount + "s/" + ts.expenseCount + "e/" + ts.captureCount + "c)\n");
                                }
                                if (preview.trips.size() > toShow) sb.append(" - ... (" + (preview.trips.size() - toShow) + " more)\n");
                                sb.append("\n");
                            }
                        sb.append("What would you like to do?\n\n");
                        sb.append("\u2022 Overwrite: Delete all local data and replace with backup.\n");
                        sb.append("\u2022 Merge: Attempt to merge backup into local data while avoiding duplicates.\n");
                        sb.append("\u2022 Append: Keep existing data and append backup entries.\n");

                        new AlertDialog.Builder(requireContext())
                            .setTitle("Restore Preview")
                            .setMessage(sb.toString())
                            .setPositiveButton("Overwrite", (d, w) -> {
                            android.app.AlertDialog confirm = new android.app.AlertDialog.Builder(requireContext())
                                .setTitle("Confirm Overwrite")
                                .setMessage(
                                    "This will delete all local data and replace it with the remote backup. This cannot be undone. Continue?")
                                .setPositiveButton(android.R.string.yes, (c2, a2) -> {
                                    android.app.AlertDialog pr = new android.app.AlertDialog.Builder(requireContext())
                                        .setTitle("Restoring")
                                        .setMessage("Please wait while we restore and replace your data...")
                                        .setCancelable(false)
                                        .create();
                                    pr.show();
                                    com.example.voyagerbuds.firebase.FirebaseBackupManager.restoreAllData(requireContext(), mAuth,
                                        dbHelper, com.example.voyagerbuds.firebase.FirebaseBackupManager.RestoreStrategy.OVERWRITE,
                                        new com.example.voyagerbuds.firebase.FirebaseBackupManager.Callback() {
                                        @Override
                                        public void onSuccess() {
                                            pr.dismiss();
                                            android.widget.Toast.makeText(requireContext(), "Restore completed",
                                                android.widget.Toast.LENGTH_SHORT).show();
                                        }

                                        @Override
                                        public void onFailure(String error) {
                                            pr.dismiss();
                                            android.util.Log.w("ProfileFragment", "Restore failed: " + error);
                                            new android.app.AlertDialog.Builder(requireContext())
                                                .setTitle("Restore Failed")
                                                .setMessage(error)
                                                .setPositiveButton("Copy details", (d1, w1) -> {
                                                android.content.ClipboardManager clipboard = (android.content.ClipboardManager) requireContext()
                                                    .getSystemService(android.content.Context.CLIPBOARD_SERVICE);
                                                android.content.ClipData clip = android.content.ClipData
                                                    .newPlainText("restore_error", error);
                                                if (clipboard != null)
                                                    clipboard.setPrimaryClip(clip);
                                                android.widget.Toast.makeText(requireContext(), "Copied to clipboard",
                                                    android.widget.Toast.LENGTH_SHORT).show();
                                                })
                                                .setNegativeButton("OK", null).show();
                                        }
                                        });
                                })
                                .setNegativeButton(android.R.string.no, null)
                                .show();
                            })
                            .setNeutralButton("Merge", (d, w) -> {
                            android.app.AlertDialog pr = new android.app.AlertDialog.Builder(requireContext())
                                .setTitle("Restoring (Merge)")
                                .setMessage("Please wait while we merge remote backup into your local data...")
                                .setCancelable(false)
                                .create();
                            pr.show();
                            com.example.voyagerbuds.firebase.FirebaseBackupManager.restoreAllData(requireContext(), mAuth,
                                dbHelper, com.example.voyagerbuds.firebase.FirebaseBackupManager.RestoreStrategy.MERGE,
                                new com.example.voyagerbuds.firebase.FirebaseBackupManager.Callback() {
                                    @Override
                                    public void onSuccess() {
                                    pr.dismiss();
                                    android.widget.Toast.makeText(requireContext(), "Merge completed",
                                        android.widget.Toast.LENGTH_SHORT).show();
                                    }

                                    @Override
                                    public void onFailure(String error) {
                                    pr.dismiss();
                                    android.util.Log.w("ProfileFragment", "Merge failed: " + error);
                                    new android.app.AlertDialog.Builder(requireContext())
                                        .setTitle("Merge Failed")
                                        .setMessage(error)
                                        .setPositiveButton("Copy details", (d1, w1) -> {
                                            android.content.ClipboardManager clipboard = (android.content.ClipboardManager) requireContext()
                                                .getSystemService(android.content.Context.CLIPBOARD_SERVICE);
                                            android.content.ClipData clip = android.content.ClipData
                                                .newPlainText("restore_error", error);
                                            if (clipboard != null)
                                            clipboard.setPrimaryClip(clip);
                                            android.widget.Toast.makeText(requireContext(), "Copied to clipboard",
                                                android.widget.Toast.LENGTH_SHORT).show();
                                        })
                                        .setNegativeButton("OK", null).show();
                                    }
                                });
                            })
                            .setNegativeButton("Append", (d, w) -> {
                            android.app.AlertDialog pr = new android.app.AlertDialog.Builder(requireContext())
                                .setTitle("Restoring (Append)")
                                .setMessage("Please wait while we append remote backup to your local data...")
                                .setCancelable(false)
                                .create();
                            pr.show();
                            com.example.voyagerbuds.firebase.FirebaseBackupManager.restoreAllData(requireContext(), mAuth,
                                dbHelper, new com.example.voyagerbuds.firebase.FirebaseBackupManager.Callback() {
                                    @Override
                                    public void onSuccess() {
                                    pr.dismiss();
                                    android.widget.Toast.makeText(requireContext(), "Append completed",
                                        android.widget.Toast.LENGTH_SHORT).show();
                                    }

                                    @Override
                                    public void onFailure(String error) {
                                    pr.dismiss();
                                    android.util.Log.w("ProfileFragment", "Append restore failed: " + error);
                                    new android.app.AlertDialog.Builder(requireContext())
                                        .setTitle("Restore Failed")
                                        .setMessage(error)
                                        .setPositiveButton("Copy details", (d1, w1) -> {
                                            android.content.ClipboardManager clipboard = (android.content.ClipboardManager) requireContext()
                                                .getSystemService(android.content.Context.CLIPBOARD_SERVICE);
                                            android.content.ClipData clip = android.content.ClipData
                                                .newPlainText("restore_error", error);
                                            if (clipboard != null)
                                            clipboard.setPrimaryClip(clip);
                                            android.widget.Toast.makeText(requireContext(), "Copied to clipboard",
                                                android.widget.Toast.LENGTH_SHORT).show();
                                        })
                                        .setNegativeButton("OK", null).show();
                                    }
                                });
                            })
                            .setNegativeButton(android.R.string.cancel, null)
                            .show();
                    }

                    @Override
                    public void onFailure(String error) {
                        progress.dismiss();
                        new AlertDialog.Builder(requireContext())
                            .setTitle("Restore Preview Failed")
                            .setMessage(error)
                            .setPositiveButton(android.R.string.ok, null).show();
                    }
                    });
        });

        btnTestFirestore.setOnClickListener(v -> {
            // Run the preflight test only
            DatabaseHelper dbHelper = new DatabaseHelper(requireContext());
            com.example.voyagerbuds.firebase.FirebaseBackupManager.preflightCheck(requireContext(), mAuth, dbHelper,
                    new com.example.voyagerbuds.firebase.FirebaseBackupManager.Callback() {
                        @Override
                        public void onSuccess() {
                            // Include UID and Project ID in success dialog for diagnostics
                            String uid = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : "(none)";
                            String projectId = com.google.firebase.FirebaseApp.getInstance().getOptions()
                                    .getProjectId();
                            String message = "Preflight check successful — you can write to Firestore.\nUID: " + uid
                                    + "\nProject ID: " + projectId;
                            new AlertDialog.Builder(requireContext())
                                    .setTitle("Firestore Test")
                                    .setMessage(message)
                                    .setPositiveButton("Copy", (d, w) -> {
                                        android.content.ClipboardManager clipboard = (android.content.ClipboardManager) requireContext()
                                                .getSystemService(android.content.Context.CLIPBOARD_SERVICE);
                                        android.content.ClipData clip = android.content.ClipData
                                                .newPlainText("firestore_test_success", message);
                                        if (clipboard != null)
                                            clipboard.setPrimaryClip(clip);
                                        android.widget.Toast.makeText(requireContext(), "Copied details",
                                                android.widget.Toast.LENGTH_SHORT).show();
                                    })
                                    .setNegativeButton(android.R.string.ok, null)
                                    .show();
                        }

                        @Override
                        public void onFailure(String error) {
                            new AlertDialog.Builder(requireContext())
                                    .setTitle("Firestore Test Failed")
                                    .setMessage(error)
                                    .setPositiveButton(android.R.string.ok, null)
                                    .show();
                        }
                    });
        });
    }

    private void updateLanguageDisplay() {
        String currentLang = LocaleHelper.getLanguage(requireContext());
        if ("vi".equals(currentLang)) {
            tvCurrentLanguage.setText(getString(R.string.vietnamese));
        } else {
            tvCurrentLanguage.setText(getString(R.string.english));
        }
    }

    private void showLanguageDialog() {
        String currentLang = LocaleHelper.getLanguage(requireContext());
        String[] languages = { getString(R.string.english), getString(R.string.vietnamese) };
        int checkedItem = "vi".equals(currentLang) ? 1 : 0;

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle(getString(R.string.select_language));
        builder.setSingleChoiceItems(languages, checkedItem, (dialog, which) -> {
            String selectedLang = (which == 0) ? "en" : "vi";

            // Update locale
            LocaleHelper.setLocale(requireContext(), selectedLang);

            // Recreate the activity to apply changes
            requireActivity().recreate();

            dialog.dismiss();
        });
        builder.setNegativeButton(android.R.string.cancel, null);
        builder.create().show();
    }

    private void updateThemeDisplay() {
        String currentTheme = ThemeHelper.getTheme(requireContext());
        if (ThemeHelper.DARK_MODE.equals(currentTheme)) {
            tvCurrentTheme.setText(getString(R.string.dark_mode));
        } else {
            tvCurrentTheme.setText(getString(R.string.light_mode));
        }
    }

    private void showThemeDialog() {
        String currentTheme = ThemeHelper.getTheme(requireContext());
        String[] themes = { getString(R.string.light_mode), getString(R.string.dark_mode) };
        int checkedItem = ThemeHelper.DARK_MODE.equals(currentTheme) ? 1 : 0;

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle(getString(R.string.select_theme));
        builder.setSingleChoiceItems(themes, checkedItem, (dialog, which) -> {
            String selectedTheme = (which == 0) ? ThemeHelper.LIGHT_MODE : ThemeHelper.DARK_MODE;

            // Update theme
            ThemeHelper.setTheme(requireContext(), selectedTheme);

            // Update display immediately
            updateThemeDisplay();

            dialog.dismiss();
        });
        builder.setNegativeButton(android.R.string.cancel, null);
        builder.create().show();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh UI with current language and theme
        updateLanguageDisplay();
        updateThemeDisplay();
        updateUserDisplay();
    }

    private void updateUserDisplay() {
        if (mAuth == null) {
            mAuth = FirebaseAuth.getInstance();
        }

        // Read from SharedPreferences first (fast, no network call)
        android.content.SharedPreferences prefs = requireContext()
                .getSharedPreferences("VoyagerBudsPrefs", android.content.Context.MODE_PRIVATE);
        String email = prefs.getString("user_email", null);
        String displayName = prefs.getString("user_display_name", null);

        // If not in SharedPreferences, try Firebase (but this should rarely happen now)
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            if (email == null) {
                email = currentUser.getEmail();
                // Try provider data if still null
                if (email == null && !currentUser.getProviderData().isEmpty()) {
                    for (var profile : currentUser.getProviderData()) {
                        if (profile.getEmail() != null && !profile.getEmail().isEmpty()) {
                            email = profile.getEmail();
                            break;
                        }
                    }
                }
            }
            if (displayName == null) {
                displayName = currentUser.getDisplayName();
            }
        }

        android.util.Log.d("ProfileFragment", "Display - Email: " + email + ", Name: " + displayName);

        // Update UI immediately from cached data
        if (displayName != null && !displayName.isEmpty()) {
            tvUserName.setText(displayName);
        } else if (email != null && !email.isEmpty()) {
            // If there's no display name, use the email's local part as a fallback
            int atIndex = email.indexOf('@');
            String fallback = atIndex > 0 ? email.substring(0, atIndex) : email;
            tvUserName.setText(fallback);
        } else {
            tvUserName.setText(R.string.unknown_user);
        }

        if (email != null && !email.isEmpty()) {
            tvUserEmail.setText(email);
        } else {
            tvUserEmail.setText(R.string.email_not_available);
        }
    }
}