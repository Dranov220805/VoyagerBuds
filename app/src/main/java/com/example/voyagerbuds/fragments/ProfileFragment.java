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
    LinearLayout btnTestFirestore = view.findViewById(R.id.btn_test_firestore);
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
          if (!com.example.voyagerbuds.utils.UserSessionManager.isUserLoggedIn()) {
            android.widget.Toast.makeText(
                getContext(), getString(R.string.restore_login_prompt), android.widget.Toast.LENGTH_SHORT)
                .show();
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

                  // Populate sample remote trips (up to 5)
                  sampleTripsContainer.removeAllViews();
                  int toShow = Math.min(5, preview.trips.size());
                  for (int i = 0; i < toShow; i++) {
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
                  if (preview.trips.size() > toShow) {
                    TextView more = new TextView(requireContext());
                    more.setText(
                        String.format(
                            requireContext().getString(R.string.more_indicator),
                            (preview.trips.size() - toShow)));
                    more.setTextAppearance(requireContext(), android.R.style.TextAppearance_Small);
                    sampleTripsContainer.addView(more);
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
          if (!com.example.voyagerbuds.utils.UserSessionManager.isUserLoggedIn()) {
            android.widget.Toast.makeText(
                getContext(), getString(R.string.backup_login_prompt), android.widget.Toast.LENGTH_SHORT)
                .show();
            return;
          }

          DatabaseHelper dbHelper = new DatabaseHelper(requireContext());
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
        });

    btnTestFirestore.setOnClickListener(
        v -> {
          // Run the preflight test only
          DatabaseHelper dbHelper = new DatabaseHelper(requireContext());
          com.example.voyagerbuds.firebase.FirebaseBackupManager.preflightCheck(
              requireContext(),
              mAuth,
              dbHelper,
              new com.example.voyagerbuds.firebase.FirebaseBackupManager.Callback() {
                @Override
                public void onSuccess() {
                  // Include UID and Project ID in success dialog for diagnostics
                  String uid = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : "(none)";
                  String projectId = com.google.firebase.FirebaseApp.getInstance().getOptions().getProjectId();
                  String message = "Preflight check successful — you can write to Firestore.\nUID: "
                      + uid
                      + "\nProject ID: "
                      + projectId;
                  new AlertDialog.Builder(requireContext())
                      .setTitle(getString(R.string.firestore_test_title))
                      .setMessage(message)
                      .setPositiveButton(
                          getString(R.string.copy),
                          (d, w) -> {
                            android.content.ClipboardManager clipboard = (android.content.ClipboardManager) requireContext()
                                .getSystemService(android.content.Context.CLIPBOARD_SERVICE);
                            android.content.ClipData clip = android.content.ClipData.newPlainText(
                                "firestore_test_success", message);
                            if (clipboard != null)
                              clipboard.setPrimaryClip(clip);
                            android.widget.Toast.makeText(
                                requireContext(),
                                getString(R.string.firestore_test_copied),
                                android.widget.Toast.LENGTH_SHORT)
                                .show();
                          })
                      .setNegativeButton(android.R.string.ok, null)
                      .show();
                }

                @Override
                public void onFailure(String error) {
                  new AlertDialog.Builder(requireContext())
                      .setTitle(getString(R.string.firestore_test_failed_title))
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
    android.widget.EditText etEmergencyEmail = dialogView.findViewById(R.id.et_emergency_email);
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
    String email = prefs.getString("emergency_contact_email", "");

    if (timeoutIndex < adapter.getCount()) {
      spinnerTimeout.setSelection(timeoutIndex);
    }
    etEmergencyEmail.setText(email);

    // Save button listener
    btnSave.setOnClickListener(v -> {
      int selectedTimeoutIndex = spinnerTimeout.getSelectedItemPosition();
      String newEmail = etEmergencyEmail.getText().toString().trim();

      SharedPreferences.Editor editor = prefs.edit();
      editor.putInt("emergency_timeout_index", selectedTimeoutIndex);
      editor.putString("emergency_contact_email", newEmail);
      editor.apply();

      android.widget.Toast.makeText(requireContext(), "Emergency settings saved", android.widget.Toast.LENGTH_SHORT)
          .show();
      dialog.dismiss();
    });

    dialog.show();
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