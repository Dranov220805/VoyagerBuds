package com.example.voyagerbuds.fragments;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;
import com.example.voyagerbuds.R;
import com.example.voyagerbuds.activities.LoginActivity;
import com.example.voyagerbuds.database.DatabaseHelper;
import com.example.voyagerbuds.utils.LocaleHelper;
import com.example.voyagerbuds.utils.ThemeHelper;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.Network;
import android.os.Build;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.GoogleAuthProvider;

/**
 * A simple {@link Fragment} subclass. Use the
 * {@link ProfileFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ProfileFragment extends Fragment {

  private TextView tvCurrentLanguage;
  private TextView tvCurrentTheme;
  private LinearLayout btnLanguage;
  private LinearLayout btnTheme;
  private Button btnLogout;
  private SwitchCompat switchNotifications;
  private TextView tvUserName;
  private TextView tvUserEmail;
  private FirebaseAuth mAuth;
  private GoogleSignInClient mGoogleSignInClient;
  private ActivityResultLauncher<Intent> googleSignInLauncher;

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

    // Configure Google Sign-In
    GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestIdToken(getString(R.string.default_web_client_id))
        .requestEmail()
        .build();
    mGoogleSignInClient = GoogleSignIn.getClient(requireContext(), gso);

    // Register Google Sign-In launcher
    googleSignInLauncher = registerForActivityResult(
        new ActivityResultContracts.StartActivityForResult(),
        result -> {
          if (result.getResultCode() == android.app.Activity.RESULT_OK) {
            Intent data = result.getData();
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
              GoogleSignInAccount account = task.getResult(ApiException.class);
              firebaseAuthWithGoogle(account.getIdToken());
            } catch (ApiException e) {
              android.widget.Toast.makeText(requireContext(),
                  "Google sign-in failed: " + e.getMessage(),
                  android.widget.Toast.LENGTH_SHORT).show();
            }
          }
        });
  }

  @Override
  public View onCreateView(
      LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
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
    switchNotifications = view.findViewById(R.id.switch_notifications);
    LinearLayout btnBackupData = view.findViewById(R.id.btn_backup_data);
    LinearLayout btnRestoreData = view.findViewById(R.id.btn_restore_data);
    LinearLayout btnEmergency = view.findViewById(R.id.btn_emergency);

    // Setup Notifications Switch
    SharedPreferences prefs = requireContext().getSharedPreferences("VoyagerBudsPrefs", Context.MODE_PRIVATE);
    boolean notificationsEnabled = prefs.getBoolean("notifications_enabled", true);
    switchNotifications.setChecked(notificationsEnabled);

    switchNotifications.setOnCheckedChangeListener(
        (buttonView, isChecked) -> {
          prefs.edit().putBoolean("notifications_enabled", isChecked).apply();
        });

    // Set up Emergency Contact Dialog
    btnEmergency.setOnClickListener(v -> showEmergencyContactDialog());

    // Update current language display
    updateLanguageDisplay();

    // Update current theme display
    updateThemeDisplay();

    // Set up language selector
    btnLanguage.setOnClickListener(v -> showLanguageDialog());

    // Set up theme selector
    btnTheme.setOnClickListener(v -> showThemeDialog());

    // Set up logout button
    btnLogout.setOnClickListener(
        v -> {
          // Clear local session values (keep UID -> userId mapping)
          com.example.voyagerbuds.utils.UserSessionManager.clearSession(requireContext());
          // Sign out from Firebase
          if (mAuth == null)
            mAuth = FirebaseAuth.getInstance();
          mAuth.signOut();
          // Redirect to Login screen and clear activity stack
          Intent intent = new Intent(requireContext(), LoginActivity.class);
          intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
          startActivity(intent);
        });

    btnRestoreData.setOnClickListener(
        v -> {
          // Check network connectivity first
          if (!isNetworkAvailable()) {
            showNetworkErrorDialog(false);
            return;
          }

          if (!com.example.voyagerbuds.utils.UserSessionManager.isUserLoggedIn()) {
            android.widget.Toast.makeText(
                getContext(), getString(R.string.restore_login_prompt), android.widget.Toast.LENGTH_SHORT)
                .show();
            return;
          }

          // Check if user has a Firebase account
          if (isLocalAccount()) {
            showLocalAccountDialog(false);
            return;
          }

          // Show a restore preview first (counts of local vs remote data)
          DatabaseHelper dbHelper = new DatabaseHelper(requireContext());
          android.app.AlertDialog progress = new android.app.AlertDialog.Builder(requireContext())
              .setTitle(getString(R.string.fetching_backup_preview_title))
              .setMessage(getString(R.string.fetching_backup_preview_message))
              .setCancelable(false)
              .create();
          progress.show();

          com.example.voyagerbuds.firebase.FirebaseBackupManager.fetchBackupPreview(
              requireContext(),
              mAuth,
              dbHelper,
              new com.example.voyagerbuds.firebase.FirebaseBackupManager.PreviewCallback() {
                @Override
                public void onPreview(
                    com.example.voyagerbuds.firebase.FirebaseBackupManager.BackupPreview preview) {
                  progress.dismiss();
                  // Compute local counts
                  int localUserId = com.example.voyagerbuds.utils.UserSessionManager.getCurrentUserId(
                      requireContext());
                  int localTrips = dbHelper.getAllTrips(localUserId).size();
                  int localSchedules = dbHelper.getTotalSchedulesForUser(localUserId);
                  int localExpenses = dbHelper.getTotalExpensesForUser(localUserId);
                  int localCaptures = dbHelper.getTotalCapturesForUser(localUserId);

                  // Inflate a custom preview dialog with a UX-friendly view
                  View dialogView = LayoutInflater.from(requireContext())
                      .inflate(R.layout.dialog_restore_preview, null);
                  TextView tvLocalCounts = dialogView.findViewById(R.id.tv_local_counts);
                  TextView tvRemoteCounts = dialogView.findViewById(R.id.tv_remote_counts);
                  LinearLayout sampleTripsContainer = dialogView.findViewById(R.id.sampleTripsContainer);
                  MaterialButton btnOverwrite = dialogView.findViewById(R.id.btn_overwrite);
                  MaterialButton btnMerge = dialogView.findViewById(R.id.btn_merge);
                  MaterialButton btnAppend = dialogView.findViewById(R.id.btn_append);

                  // Populate counts
                  String localCounts = " • "
                      + requireContext()
                          .getResources()
                          .getQuantityString(R.plurals.n_trips, localTrips, localTrips)
                      + "\n"
                      + " • "
                      + requireContext()
                          .getResources()
                          .getQuantityString(
                              R.plurals.n_schedules, localSchedules, localSchedules)
                      + "\n"
                      + " • "
                      + requireContext()
                          .getResources()
                          .getQuantityString(R.plurals.n_expenses, localExpenses, localExpenses)
                      + "\n"
                      + " • "
                      + requireContext()
                          .getResources()
                          .getQuantityString(R.plurals.n_captures, localCaptures, localCaptures);
                  String remoteCounts = requireContext()
                      .getResources()
                      .getQuantityString(
                          R.plurals.n_trips, preview.tripCount, preview.tripCount)
                      + "\n"
                      + " • "
                      + requireContext()
                          .getResources()
                          .getQuantityString(
                              R.plurals.n_schedules,
                              preview.scheduleCount,
                              preview.scheduleCount)
                      + "\n"
                      + " • "
                      + requireContext()
                          .getResources()
                          .getQuantityString(
                              R.plurals.n_expenses, preview.expenseCount, preview.expenseCount)
                      + "\n"
                      + " • "
                      + requireContext()
                          .getResources()
                          .getQuantityString(
                              R.plurals.n_captures, preview.captureCount, preview.captureCount);
                  tvLocalCounts.setText(localCounts);
                  tvRemoteCounts.setText(remoteCounts);

                  // Populate all remote trips (no limit)
                  sampleTripsContainer.removeAllViews();
                  for (int i = 0; i < preview.trips.size(); i++) {
                    com.example.voyagerbuds.firebase.FirebaseBackupManager.TripSummary ts = preview.trips.get(i);
                    View item = LayoutInflater.from(requireContext())
                        .inflate(
                            R.layout.item_remote_trip_preview, sampleTripsContainer, false);
                    TextView title = item.findViewById(R.id.tv_remote_trip_title);
                    TextView subtext = item.findViewById(R.id.tv_remote_trip_subtext);
                    title.setText(ts.tripName);
                    String tsCounts = requireContext()
                        .getResources()
                        .getQuantityString(
                            R.plurals.n_schedules, ts.scheduleCount, ts.scheduleCount)
                        + " • "
                        + requireContext()
                            .getResources()
                            .getQuantityString(
                                R.plurals.n_expenses, ts.expenseCount, ts.expenseCount)
                        + " • "
                        + requireContext()
                            .getResources()
                            .getQuantityString(
                                R.plurals.n_captures, ts.captureCount, ts.captureCount);
                    subtext.setText(tsCounts);
                    sampleTripsContainer.addView(item);
                  }

                  // Make the AlertDialog
                  AlertDialog previewDialog = new AlertDialog.Builder(requireContext()).setView(dialogView).create();

                  // Header cancel button
                  android.widget.ImageButton btnCancelDialog = dialogView.findViewById(R.id.btn_dialog_cancel);
                  btnCancelDialog.setOnClickListener(v -> previewDialog.dismiss());

                  // Overwrite button
                  btnOverwrite.setOnClickListener(
                      btn -> {
                        android.app.AlertDialog confirm = new android.app.AlertDialog.Builder(requireContext())
                            .setTitle(getString(R.string.confirm_overwrite_title))
                            .setMessage(getString(R.string.confirm_overwrite_message))
                            .setCancelable(true)
                            .setPositiveButton(
                                android.R.string.yes,
                                (c2, a2) -> {
                                  android.app.AlertDialog pr = new android.app.AlertDialog.Builder(requireContext())
                                      .setTitle(getString(R.string.restoring_title))
                                      .setMessage(
                                          getString(R.string.restoring_message_overwrite))
                                      .setCancelable(false)
                                      .create();
                                  pr.show();
                                  com.example.voyagerbuds.firebase.FirebaseBackupManager
                                      .restoreAllData(
                                          requireContext(),
                                          mAuth,
                                          dbHelper,
                                          com.example.voyagerbuds.firebase.FirebaseBackupManager.RestoreStrategy.OVERWRITE,
                                          new com.example.voyagerbuds.firebase.FirebaseBackupManager.Callback() {
                                            @Override
                                            public void onSuccess() {
                                              pr.dismiss();
                                              previewDialog.dismiss();
                                              android.widget.Toast.makeText(
                                                  requireContext(),
                                                  getString(R.string.restore_completed),
                                                  android.widget.Toast.LENGTH_SHORT)
                                                  .show();
                                            }

                                            @Override
                                            public void onFailure(String error) {
                                              pr.dismiss();
                                              previewDialog.dismiss();
                                              android.util.Log.w(
                                                  "ProfileFragment", "Restore failed: " + error);
                                              new android.app.AlertDialog.Builder(
                                                  requireContext())
                                                  .setTitle(
                                                      getString(R.string.restore_failed_title))
                                                  .setMessage(error)
                                                  .setPositiveButton(
                                                      getString(R.string.copy_details),
                                                      (d1, w1) -> {
                                                        android.content.ClipboardManager clipboard = (android.content.ClipboardManager) requireContext()
                                                            .getSystemService(
                                                                android.content.Context.CLIPBOARD_SERVICE);
                                                        android.content.ClipData clip = android.content.ClipData
                                                            .newPlainText(
                                                                "restore_error", error);
                                                        if (clipboard != null)
                                                          clipboard.setPrimaryClip(clip);
                                                        android.widget.Toast.makeText(
                                                            requireContext(),
                                                            getString(
                                                                R.string.copied_to_clipboard),
                                                            android.widget.Toast.LENGTH_SHORT)
                                                            .show();
                                                      })
                                                  .setNegativeButton(android.R.string.ok, null)
                                                  .show();
                                            }
                                          });
                                })
                            .setNegativeButton(android.R.string.no, null)
                            .show();
                      });

                  // Merge button (confirm then merge)
                  btnMerge.setOnClickListener(
                      btn -> {
                        new AlertDialog.Builder(requireContext())
                            .setTitle(getString(R.string.confirm_merge_title))
                            .setMessage(getString(R.string.confirm_merge_message))
                            .setCancelable(true)
                            .setPositiveButton(
                                android.R.string.yes,
                                (d, w) -> {
                                  android.app.AlertDialog pr = new android.app.AlertDialog.Builder(requireContext())
                                      .setTitle(getString(R.string.restoring_title))
                                      .setMessage(getString(R.string.restoring_message_merge))
                                      .setCancelable(false)
                                      .create();
                                  pr.show();
                                  com.example.voyagerbuds.firebase.FirebaseBackupManager
                                      .restoreAllData(
                                          requireContext(),
                                          mAuth,
                                          dbHelper,
                                          com.example.voyagerbuds.firebase.FirebaseBackupManager.RestoreStrategy.MERGE,
                                          new com.example.voyagerbuds.firebase.FirebaseBackupManager.Callback() {
                                            @Override
                                            public void onSuccess() {
                                              pr.dismiss();
                                              previewDialog.dismiss();
                                              android.widget.Toast.makeText(
                                                  requireContext(),
                                                  getString(R.string.merge_completed),
                                                  android.widget.Toast.LENGTH_SHORT)
                                                  .show();
                                            }

                                            @Override
                                            public void onFailure(String error) {
                                              pr.dismiss();
                                              previewDialog.dismiss();
                                              android.util.Log.w(
                                                  "ProfileFragment", "Merge failed: " + error);
                                              new android.app.AlertDialog.Builder(requireContext())
                                                  .setTitle(getString(R.string.restore_failed_title))
                                                  .setMessage(error)
                                                  .setPositiveButton(
                                                      getString(R.string.copy_details),
                                                      (d1, w1) -> {
                                                        android.content.ClipboardManager clipboard = (android.content.ClipboardManager) requireContext()
                                                            .getSystemService(
                                                                android.content.Context.CLIPBOARD_SERVICE);
                                                        android.content.ClipData clip = android.content.ClipData
                                                            .newPlainText(
                                                                "restore_error", error);
                                                        if (clipboard != null)
                                                          clipboard.setPrimaryClip(clip);
                                                        android.widget.Toast.makeText(
                                                            requireContext(),
                                                            getString(
                                                                R.string.copied_to_clipboard),
                                                            android.widget.Toast.LENGTH_SHORT)
                                                            .show();
                                                      })
                                                  .setNegativeButton(android.R.string.ok, null)
                                                  .show();
                                            }
                                          });
                                })
                            .setNegativeButton(android.R.string.no, null)
                            .show();
                      });

                  // Append button (confirm then append)
                  btnAppend.setOnClickListener(
                      btn -> {
                        new AlertDialog.Builder(requireContext())
                            .setTitle(getString(R.string.confirm_append_title))
                            .setMessage(getString(R.string.confirm_append_message))
                            .setCancelable(true)
                            .setPositiveButton(
                                android.R.string.yes,
                                (d, w) -> {
                                  android.app.AlertDialog pr = new android.app.AlertDialog.Builder(requireContext())
                                      .setTitle(getString(R.string.restoring_title))
                                      .setMessage(getString(R.string.restoring_message_append))
                                      .setCancelable(false)
                                      .create();
                                  pr.show();
                                  com.example.voyagerbuds.firebase.FirebaseBackupManager
                                      .restoreAllData(
                                          requireContext(),
                                          mAuth,
                                          dbHelper,
                                          com.example.voyagerbuds.firebase.FirebaseBackupManager.RestoreStrategy.APPEND,
                                          new com.example.voyagerbuds.firebase.FirebaseBackupManager.Callback() {
                                            @Override
                                            public void onSuccess() {
                                              pr.dismiss();
                                              previewDialog.dismiss();
                                              android.widget.Toast.makeText(
                                                  requireContext(),
                                                  getString(R.string.append_completed),
                                                  android.widget.Toast.LENGTH_SHORT)
                                                  .show();
                                            }

                                            @Override
                                            public void onFailure(String error) {
                                              pr.dismiss();
                                              previewDialog.dismiss();
                                              android.util.Log.w(
                                                  "ProfileFragment",
                                                  "Append restore failed: " + error);
                                              new android.app.AlertDialog.Builder(requireContext())
                                                  .setTitle(getString(R.string.restore_failed_title))
                                                  .setMessage(error)
                                                  .setPositiveButton(
                                                      getString(R.string.copy_details),
                                                      (d1, w1) -> {
                                                        android.content.ClipboardManager clipboard = (android.content.ClipboardManager) requireContext()
                                                            .getSystemService(
                                                                android.content.Context.CLIPBOARD_SERVICE);
                                                        android.content.ClipData clip = android.content.ClipData
                                                            .newPlainText(
                                                                "restore_error", error);
                                                        if (clipboard != null)
                                                          clipboard.setPrimaryClip(clip);
                                                        android.widget.Toast.makeText(
                                                            requireContext(),
                                                            getString(
                                                                R.string.copied_to_clipboard),
                                                            android.widget.Toast.LENGTH_SHORT)
                                                            .show();
                                                      })
                                                  .setNegativeButton(android.R.string.ok, null)
                                                  .show();
                                            }
                                          });
                                })
                            .setNegativeButton(android.R.string.no, null)
                            .show();
                      });

                  // Show the preview dialog
                  previewDialog.show();
                }

                @Override
                public void onFailure(String error) {
                  progress.dismiss();
                  new AlertDialog.Builder(requireContext())
                      .setTitle(getString(R.string.restore_preview_failed_title))
                      .setMessage(error)
                      .setPositiveButton(android.R.string.ok, null)
                      .show();
                }
              });
        });

    // Backup button: upload all local data to Firestore
    btnBackupData.setOnClickListener(
        v -> {
          // Check network connectivity first
          if (!isNetworkAvailable()) {
            showNetworkErrorDialog(true);
            return;
          }

          if (!com.example.voyagerbuds.utils.UserSessionManager.isUserLoggedIn()) {
            android.widget.Toast.makeText(
                getContext(), getString(R.string.backup_login_prompt), android.widget.Toast.LENGTH_SHORT)
                .show();
            return;
          }

          // Check if user has a Firebase account
          if (isLocalAccount()) {
            showLocalAccountDialog(true);
            return;
          }

          DatabaseHelper dbHelper = new DatabaseHelper(requireContext());

          // First, check if there's existing backup data on cloud
          android.app.AlertDialog checkProgress = new android.app.AlertDialog.Builder(requireContext())
              .setTitle(getString(R.string.fetching_backup_preview_title))
              .setMessage(getString(R.string.fetching_backup_preview_message))
              .setCancelable(false)
              .create();
          checkProgress.show();

          com.example.voyagerbuds.firebase.FirebaseBackupManager.fetchBackupPreview(
              requireContext(),
              mAuth,
              dbHelper,
              new com.example.voyagerbuds.firebase.FirebaseBackupManager.PreviewCallback() {
                @Override
                public void onPreview(
                    com.example.voyagerbuds.firebase.FirebaseBackupManager.BackupPreview preview) {
                  checkProgress.dismiss();

                  // Get local data with trip details
                  int localUserId = com.example.voyagerbuds.utils.UserSessionManager.getCurrentUserId(requireContext());
                  java.util.List<com.example.voyagerbuds.models.Trip> localTrips = dbHelper.getAllTrips(localUserId);

                  // Build detailed local trip list
                  StringBuilder localDetails = new StringBuilder();
                  for (com.example.voyagerbuds.models.Trip trip : localTrips) {
                    int schedules = dbHelper.getSchedulesForTrip(trip.getTripId()).size();
                    int expenses = dbHelper.getExpensesForTrip(trip.getTripId()).size();
                    int captures = dbHelper.getCapturesForTrip(trip.getTripId()).size();

                    localDetails.append("• ").append(trip.getDestination()).append("\n  ");
                    localDetails.append(requireContext().getResources().getQuantityString(
                        R.plurals.n_schedules, schedules, schedules)).append(", ");
                    localDetails.append(requireContext().getResources().getQuantityString(
                        R.plurals.n_expenses, expenses, expenses)).append(", ");
                    localDetails.append(requireContext().getResources().getQuantityString(
                        R.plurals.n_captures, captures, captures)).append("\n");
                  }

                  // Build detailed cloud trip list
                  StringBuilder cloudDetails = new StringBuilder();
                  for (com.example.voyagerbuds.firebase.FirebaseBackupManager.TripSummary ts : preview.trips) {
                    cloudDetails.append("• ").append(ts.tripName).append("\n  ");
                    cloudDetails.append(requireContext().getResources().getQuantityString(
                        R.plurals.n_schedules, ts.scheduleCount, ts.scheduleCount)).append(", ");
                    cloudDetails.append(requireContext().getResources().getQuantityString(
                        R.plurals.n_expenses, ts.expenseCount, ts.expenseCount)).append(", ");
                    cloudDetails.append(requireContext().getResources().getQuantityString(
                        R.plurals.n_captures, ts.captureCount, ts.captureCount)).append("\n");
                  }

                  String message = getString(R.string.backup_cloud_check_message_detailed,
                      cloudDetails.toString().trim(),
                      localDetails.toString().trim());

                  new android.app.AlertDialog.Builder(requireContext())
                      .setTitle(getString(R.string.backup_cloud_check_title))
                      .setMessage(message)
                      .setPositiveButton(getString(R.string.action_overwrite), (d, w) -> {
                        performBackup(dbHelper);
                      })
                      .setNegativeButton(getString(R.string.action_cancel), null)
                      .show();
                }

                @Override
                public void onFailure(String error) {
                  checkProgress.dismiss();

                  // No cloud data found or error - show confirmation to create new backup
                  int localUserId = com.example.voyagerbuds.utils.UserSessionManager.getCurrentUserId(requireContext());
                  java.util.List<com.example.voyagerbuds.models.Trip> localTrips = dbHelper.getAllTrips(localUserId);

                  // Build detailed local trip list
                  StringBuilder localDetails = new StringBuilder();
                  for (com.example.voyagerbuds.models.Trip trip : localTrips) {
                    int schedules = dbHelper.getSchedulesForTrip(trip.getTripId()).size();
                    int expenses = dbHelper.getExpensesForTrip(trip.getTripId()).size();
                    int captures = dbHelper.getCapturesForTrip(trip.getTripId()).size();

                    localDetails.append("• ").append(trip.getDestination()).append("\n  ");
                    localDetails.append(requireContext().getResources().getQuantityString(
                        R.plurals.n_schedules, schedules, schedules)).append(", ");
                    localDetails.append(requireContext().getResources().getQuantityString(
                        R.plurals.n_expenses, expenses, expenses)).append(", ");
                    localDetails.append(requireContext().getResources().getQuantityString(
                        R.plurals.n_captures, captures, captures)).append("\n");
                  }

                  new android.app.AlertDialog.Builder(requireContext())
                      .setTitle(getString(R.string.backup_no_cloud_data_title))
                      .setMessage(String.format(getString(R.string.backup_no_cloud_data_message),
                          localDetails.toString().trim()))
                      .setPositiveButton(android.R.string.ok, (d, w) -> {
                        performBackup(dbHelper);
                      })
                      .setNegativeButton(getString(R.string.action_cancel), null)
                      .show();
                }
              });
        });
  }

  private void performBackup(DatabaseHelper dbHelper) {
    android.app.AlertDialog progress = new android.app.AlertDialog.Builder(requireContext())
        .setTitle(getString(R.string.backing_up_title))
        .setMessage(getString(R.string.backing_up_message))
        .setCancelable(false)
        .create();
    progress.show();

    com.example.voyagerbuds.firebase.FirebaseBackupManager.backupAllData(
        requireContext(),
        mAuth,
        dbHelper,
        new com.example.voyagerbuds.firebase.FirebaseBackupManager.Callback() {
          @Override
          public void onSuccess() {
            progress.dismiss();
            android.widget.Toast.makeText(
                requireContext(),
                getString(R.string.backup_completed),
                android.widget.Toast.LENGTH_SHORT)
                .show();
          }

          @Override
          public void onFailure(String error) {
            progress.dismiss();
            android.util.Log.w("ProfileFragment", "Backup failed: " + error);
            new android.app.AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.backup_failed_title))
                .setMessage(error)
                .setPositiveButton(
                    getString(R.string.copy_details),
                    (d, w) -> {
                      android.content.ClipboardManager clipboard = (android.content.ClipboardManager) requireContext()
                          .getSystemService(android.content.Context.CLIPBOARD_SERVICE);
                      android.content.ClipData clip = android.content.ClipData.newPlainText("backup_error",
                          error);
                      if (clipboard != null)
                        clipboard.setPrimaryClip(clip);
                      android.widget.Toast.makeText(
                          requireContext(),
                          getString(R.string.copied_to_clipboard),
                          android.widget.Toast.LENGTH_SHORT)
                          .show();
                    })
                .setNegativeButton(android.R.string.ok, null)
                .show();
          }
        });
  }

  private void setupListeners() {
    // Test Firestore button removed
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
    builder.setSingleChoiceItems(
        languages,
        checkedItem,
        (dialog, which) -> {
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
    builder.setSingleChoiceItems(
        themes,
        checkedItem,
        (dialog, which) -> {
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

  private void showEmergencyContactDialog() {
    AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
    View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_emergency_contact, null);
    builder.setView(dialogView);
    AlertDialog dialog = builder.create();
    dialog.getWindow()
        .setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));

    // Initialize views
    android.widget.Spinner spinnerTimeout = dialogView.findViewById(R.id.spinner_timeout);
    androidx.recyclerview.widget.RecyclerView recyclerContacts = dialogView.findViewById(R.id.recycler_contacts);
    TextView tvEmptyContacts = dialogView.findViewById(R.id.tv_empty_contacts);
    com.google.android.material.button.MaterialButton btnAddContact = dialogView.findViewById(R.id.btn_add_contact);
    Button btnSave = dialogView.findViewById(R.id.btn_save_emergency);
    android.widget.ImageButton btnClose = dialogView.findViewById(R.id.btn_close_dialog);

    // Close button listener
    btnClose.setOnClickListener(v -> dialog.dismiss());

    // Setup Spinner
    android.widget.ArrayAdapter<CharSequence> adapter = android.widget.ArrayAdapter.createFromResource(requireContext(),
        R.array.emergency_timeout_options, android.R.layout.simple_spinner_item);
    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    spinnerTimeout.setAdapter(adapter);

    // Load saved settings
    SharedPreferences prefs = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE);
    int timeoutIndex = prefs.getInt("emergency_timeout_index", 1); // Default to 24h

    if (timeoutIndex < adapter.getCount()) {
      spinnerTimeout.setSelection(timeoutIndex);
    }

    // Load contacts from SharedPreferences
    java.util.List<com.example.voyagerbuds.models.EmergencyContact> contacts = loadEmergencyContacts();

    // Setup RecyclerView
    recyclerContacts.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(requireContext()));
    final EmergencyContactAdapter[] contactAdapterHolder = new EmergencyContactAdapter[1];
    contactAdapterHolder[0] = new EmergencyContactAdapter(contacts,
        // Edit callback
        contact -> showAddEditContactDialog(contact, () -> {
          java.util.List<com.example.voyagerbuds.models.EmergencyContact> updatedContacts = loadEmergencyContacts();
          contactAdapterHolder[0].updateContacts(updatedContacts);
          updateEmptyView(updatedContacts, tvEmptyContacts, recyclerContacts);
        }),
        // Delete callback
        contact -> {
          new AlertDialog.Builder(requireContext())
              .setTitle(getString(R.string.delete))
              .setMessage(getString(R.string.delete_contact_confirm))
              .setPositiveButton(getString(R.string.delete), (d, w) -> {
                deleteEmergencyContact(contact.getId());
                java.util.List<com.example.voyagerbuds.models.EmergencyContact> updatedContacts = loadEmergencyContacts();
                contactAdapterHolder[0].updateContacts(updatedContacts);
                updateEmptyView(updatedContacts, tvEmptyContacts, recyclerContacts);
                android.widget.Toast.makeText(requireContext(), getString(R.string.contact_deleted),
                    android.widget.Toast.LENGTH_SHORT).show();
              })
              .setNegativeButton(getString(R.string.cancel), null)
              .show();
        });
    recyclerContacts.setAdapter(contactAdapterHolder[0]);

    // Update empty view
    updateEmptyView(contacts, tvEmptyContacts, recyclerContacts);

    // Add contact button
    btnAddContact.setOnClickListener(v -> {
      showAddEditContactDialog(null, () -> {
        java.util.List<com.example.voyagerbuds.models.EmergencyContact> updatedContacts = loadEmergencyContacts();
        contactAdapterHolder[0].updateContacts(updatedContacts);
        updateEmptyView(updatedContacts, tvEmptyContacts, recyclerContacts);
      });
    });

    // Save button listener
    btnSave.setOnClickListener(v -> {
      int selectedTimeoutIndex = spinnerTimeout.getSelectedItemPosition();

      SharedPreferences.Editor editor = prefs.edit();
      editor.putInt("emergency_timeout_index", selectedTimeoutIndex);
      editor.apply();

      android.widget.Toast.makeText(requireContext(), getString(R.string.settings_saved),
          android.widget.Toast.LENGTH_SHORT).show();
      dialog.dismiss();
    });

    dialog.show();
  }

  private void updateEmptyView(java.util.List<com.example.voyagerbuds.models.EmergencyContact> contacts,
      TextView emptyView, androidx.recyclerview.widget.RecyclerView recyclerView) {
    if (contacts.isEmpty()) {
      emptyView.setVisibility(View.VISIBLE);
      recyclerView.setVisibility(View.GONE);
    } else {
      emptyView.setVisibility(View.GONE);
      recyclerView.setVisibility(View.VISIBLE);
    }
  }

  private void showAddEditContactDialog(com.example.voyagerbuds.models.EmergencyContact contact,
      Runnable onSaveCallback) {
    AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
    View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_emergency_contact, null);
    builder.setView(dialogView);
    AlertDialog dialog = builder.create();
    dialog.getWindow()
        .setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));

    // Initialize views
    TextView tvTitle = dialogView.findViewById(R.id.tv_dialog_title);
    EditText etName = dialogView.findViewById(R.id.et_contact_name);
    EditText etRelationship = dialogView.findViewById(R.id.et_contact_relationship);
    EditText etEmail = dialogView.findViewById(R.id.et_contact_email);
    EditText etPhone = dialogView.findViewById(R.id.et_contact_phone);
    Button btnSave = dialogView.findViewById(R.id.btn_save_contact);
    android.widget.ImageButton btnClose = dialogView.findViewById(R.id.btn_close_dialog);

    // Set title based on edit/add mode
    if (contact != null) {
      tvTitle.setText(getString(R.string.edit_emergency_contact));
      etName.setText(contact.getName());
      etRelationship.setText(contact.getRelationship());
      etEmail.setText(contact.getEmail());
      etPhone.setText(contact.getPhone());
    }

    btnClose.setOnClickListener(v -> dialog.dismiss());

    btnSave.setOnClickListener(v -> {
      String name = etName.getText().toString().trim();
      String relationship = etRelationship.getText().toString().trim();
      String email = etEmail.getText().toString().trim();
      String phone = etPhone.getText().toString().trim();

      if (name.isEmpty() || relationship.isEmpty() || email.isEmpty()) {
        android.widget.Toast.makeText(requireContext(), getString(R.string.fill_all_fields),
            android.widget.Toast.LENGTH_SHORT).show();
        return;
      }

      String id = contact != null ? contact.getId() : java.util.UUID.randomUUID().toString();
      com.example.voyagerbuds.models.EmergencyContact newContact = new com.example.voyagerbuds.models.EmergencyContact(
          id, name, email, phone, relationship);

      saveEmergencyContact(newContact);
      android.widget.Toast.makeText(requireContext(), getString(R.string.contact_saved),
          android.widget.Toast.LENGTH_SHORT).show();

      if (onSaveCallback != null) {
        onSaveCallback.run();
      }

      dialog.dismiss();
    });

    dialog.show();
  }

  private java.util.List<com.example.voyagerbuds.models.EmergencyContact> loadEmergencyContacts() {
    SharedPreferences prefs = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE);
    String contactsJson = prefs.getString("emergency_contacts", "[]");

    java.util.List<com.example.voyagerbuds.models.EmergencyContact> contacts = new java.util.ArrayList<>();
    try {
      org.json.JSONArray jsonArray = new org.json.JSONArray(contactsJson);
      for (int i = 0; i < jsonArray.length(); i++) {
        org.json.JSONObject jsonObject = jsonArray.getJSONObject(i);
        com.example.voyagerbuds.models.EmergencyContact contact = new com.example.voyagerbuds.models.EmergencyContact();
        contact.setId(jsonObject.getString("id"));
        contact.setName(jsonObject.getString("name"));
        contact.setEmail(jsonObject.getString("email"));
        contact.setPhone(jsonObject.optString("phone", ""));
        contact.setRelationship(jsonObject.getString("relationship"));
        contacts.add(contact);
      }
    } catch (org.json.JSONException e) {
      e.printStackTrace();
    }

    return contacts;
  }

  private void saveEmergencyContact(com.example.voyagerbuds.models.EmergencyContact contact) {
    java.util.List<com.example.voyagerbuds.models.EmergencyContact> contacts = loadEmergencyContacts();

    // Update if exists, add if new
    boolean updated = false;
    for (int i = 0; i < contacts.size(); i++) {
      if (contacts.get(i).getId().equals(contact.getId())) {
        contacts.set(i, contact);
        updated = true;
        break;
      }
    }

    if (!updated) {
      contacts.add(contact);
    }

    saveContactsList(contacts);
  }

  private void deleteEmergencyContact(String contactId) {
    java.util.List<com.example.voyagerbuds.models.EmergencyContact> contacts = loadEmergencyContacts();
    contacts.removeIf(contact -> contact.getId().equals(contactId));
    saveContactsList(contacts);
  }

  private void saveContactsList(java.util.List<com.example.voyagerbuds.models.EmergencyContact> contacts) {
    SharedPreferences prefs = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE);
    org.json.JSONArray jsonArray = new org.json.JSONArray();

    for (com.example.voyagerbuds.models.EmergencyContact contact : contacts) {
      try {
        org.json.JSONObject jsonObject = new org.json.JSONObject();
        jsonObject.put("id", contact.getId());
        jsonObject.put("name", contact.getName());
        jsonObject.put("email", contact.getEmail());
        jsonObject.put("phone", contact.getPhone());
        jsonObject.put("relationship", contact.getRelationship());
        jsonArray.put(jsonObject);
      } catch (org.json.JSONException e) {
        e.printStackTrace();
      }
    }

    SharedPreferences.Editor editor = prefs.edit();
    editor.putString("emergency_contacts", jsonArray.toString());
    editor.apply();
  }

  // Adapter for emergency contacts
  private class EmergencyContactAdapter
      extends androidx.recyclerview.widget.RecyclerView.Adapter<EmergencyContactAdapter.ContactViewHolder> {
    private java.util.List<com.example.voyagerbuds.models.EmergencyContact> contacts;
    private final java.util.function.Consumer<com.example.voyagerbuds.models.EmergencyContact> onEditClick;
    private final java.util.function.Consumer<com.example.voyagerbuds.models.EmergencyContact> onDeleteClick;

    public EmergencyContactAdapter(java.util.List<com.example.voyagerbuds.models.EmergencyContact> contacts,
        java.util.function.Consumer<com.example.voyagerbuds.models.EmergencyContact> onEditClick,
        java.util.function.Consumer<com.example.voyagerbuds.models.EmergencyContact> onDeleteClick) {
      this.contacts = contacts;
      this.onEditClick = onEditClick;
      this.onDeleteClick = onDeleteClick;
    }

    public void updateContacts(java.util.List<com.example.voyagerbuds.models.EmergencyContact> newContacts) {
      this.contacts = newContacts;
      notifyDataSetChanged();
    }

    @androidx.annotation.NonNull
    @Override
    public ContactViewHolder onCreateViewHolder(@androidx.annotation.NonNull ViewGroup parent, int viewType) {
      View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_emergency_contact, parent, false);
      return new ContactViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@androidx.annotation.NonNull ContactViewHolder holder, int position) {
      com.example.voyagerbuds.models.EmergencyContact contact = contacts.get(position);
      holder.bind(contact);
    }

    @Override
    public int getItemCount() {
      return contacts.size();
    }

    class ContactViewHolder extends androidx.recyclerview.widget.RecyclerView.ViewHolder {
      TextView tvName, tvRelationship, tvEmail, tvPhone;
      android.widget.ImageButton btnEdit, btnDelete;

      ContactViewHolder(View itemView) {
        super(itemView);
        tvName = itemView.findViewById(R.id.tv_contact_name);
        tvRelationship = itemView.findViewById(R.id.tv_contact_relationship);
        tvEmail = itemView.findViewById(R.id.tv_contact_email);
        tvPhone = itemView.findViewById(R.id.tv_contact_phone);
        btnEdit = itemView.findViewById(R.id.btn_edit_contact);
        btnDelete = itemView.findViewById(R.id.btn_delete_contact);
      }

      void bind(com.example.voyagerbuds.models.EmergencyContact contact) {
        tvName.setText(contact.getName());
        tvRelationship.setText(contact.getRelationship());
        tvEmail.setText(contact.getEmail());
        tvPhone.setText(contact.getPhone().isEmpty() ? "-" : contact.getPhone());

        btnEdit.setOnClickListener(v -> onEditClick.accept(contact));
        btnDelete.setOnClickListener(v -> onDeleteClick.accept(contact));
      }
    }
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

  /**
   * Check if network connectivity is available
   */
  private boolean isNetworkAvailable() {
    ConnectivityManager connectivityManager = (ConnectivityManager) requireContext()
        .getSystemService(Context.CONNECTIVITY_SERVICE);

    if (connectivityManager == null) {
      return false;
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      Network network = connectivityManager.getActiveNetwork();
      if (network == null) {
        return false;
      }

      NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);
      return capabilities != null && (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
          capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
          capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET));
    } else {
      android.net.NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
      return networkInfo != null && networkInfo.isConnected();
    }
  }

  /**
   * Check if the current user is using a local account (not Firebase
   * authenticated)
   * A local account is one where the user is not logged in via Firebase Auth
   */
  private boolean isLocalAccount() {
    FirebaseUser currentUser = mAuth.getCurrentUser();

    // If no Firebase user, it's a local account
    if (currentUser == null) {
      return true;
    }

    // Check if the user has any provider data (email/password, Google, etc.)
    // If they have providers, they're using Firebase authentication
    return currentUser.getProviderData().isEmpty() ||
        currentUser.getProviderData().size() <= 1; // Size 1 might be just the base provider
  }

  /**
   * Show network error dialog
   * 
   * @param isBackup true if called from backup, false if from restore
   */
  private void showNetworkErrorDialog(boolean isBackup) {
    String message = isBackup ? getString(R.string.no_network_backup_message)
        : getString(R.string.no_network_restore_message);

    new AlertDialog.Builder(requireContext())
        .setTitle(R.string.no_network_connection)
        .setMessage(message)
        .setPositiveButton(android.R.string.ok, null)
        .setIcon(R.drawable.ic_warning)
        .show();
  }

  /**
   * Show local account dialog prompting user to sign in with Google
   * 
   * @param isBackup true if called from backup, false if from restore
   */
  private void showLocalAccountDialog(boolean isBackup) {
    String message = isBackup ? getString(R.string.local_account_backup_message)
        : getString(R.string.local_account_restore_message);

    new AlertDialog.Builder(requireContext())
        .setTitle(R.string.local_account_title)
        .setMessage(message)
        .setPositiveButton(R.string.sign_in_with_google, (dialog, which) -> {
          // Initiate Google Sign-In
          signInWithGoogle();
        })
        .setNegativeButton(R.string.maybe_later, null)
        .setIcon(R.drawable.ic_error)
        .show();
  }

  /**
   * Initiate Google Sign-In flow
   */
  private void signInWithGoogle() {
    Intent signInIntent = mGoogleSignInClient.getSignInIntent();
    googleSignInLauncher.launch(signInIntent);
  }

  /**
   * Authenticate with Firebase using Google credentials
   */
  private void firebaseAuthWithGoogle(String idToken) {
    android.app.AlertDialog progressDialog = new android.app.AlertDialog.Builder(requireContext())
        .setTitle(getString(R.string.checking_connection))
        .setCancelable(false)
        .create();
    progressDialog.show();

    AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
    mAuth.signInWithCredential(credential)
        .addOnCompleteListener(requireActivity(), task -> {
          progressDialog.dismiss();

          if (task.isSuccessful()) {
            FirebaseUser user = mAuth.getCurrentUser();
            if (user != null) {
              // Save user info to SharedPreferences
              android.content.SharedPreferences prefs = requireContext()
                  .getSharedPreferences("VoyagerBudsPrefs", Context.MODE_PRIVATE);
              prefs.edit()
                  .putString("user_email", user.getEmail())
                  .putString("user_display_name", user.getDisplayName())
                  .apply();

              // Update UI
              updateUserDisplay();

              android.widget.Toast.makeText(requireContext(),
                  "Successfully signed in with Google!",
                  android.widget.Toast.LENGTH_SHORT).show();
            }
          } else {
            android.widget.Toast.makeText(requireContext(),
                "Authentication failed: " + task.getException().getMessage(),
                android.widget.Toast.LENGTH_SHORT).show();
          }
        });
  }
}