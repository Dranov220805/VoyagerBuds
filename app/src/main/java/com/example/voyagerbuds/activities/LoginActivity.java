package com.example.voyagerbuds.activities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;

import com.example.voyagerbuds.R;
import com.example.voyagerbuds.database.DatabaseHelper;
import com.example.voyagerbuds.databinding.ActivityLoginBinding;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.UserProfileChangeRequest;

public class LoginActivity extends BaseActivity {

    private static final String TAG = "LoginActivity";
    private static final String PREFS_NAME = "VoyagerBudsPrefs";
    private static final String KEY_OFFLINE_UID = "offline_uid";
    private static final String KEY_OFFLINE_EMAIL = "offline_email";
    private static final String KEY_OFFLINE_PASSWORD = "offline_password";
    private static final String KEY_IS_OFFLINE_USER = "is_offline_user";

    private ActivityLoginBinding binding;
    private View loadingContainer;
    private SharedPreferences prefs;

    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;
    private ActivityResultLauncher<Intent> googleSignInLauncher;
    private DatabaseHelper databaseHelper;

    private boolean isRegisterMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Hide action bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        databaseHelper = new DatabaseHelper(this);
        mAuth = FirebaseAuth.getInstance();
        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        googleSignInLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                        try {
                            GoogleSignInAccount account = task.getResult(ApiException.class);
                            Log.d(TAG, "Google account - Email: " + account.getEmail()
                                    + ", Name: " + account.getDisplayName());
                            firebaseAuthWithGoogle(account.getIdToken(), account.getEmail(), account.getDisplayName());
                        } catch (ApiException e) {
                            Log.w(TAG, "Google sign in failed", e);
                            hideLoading();
                            Toast.makeText(this, getString(R.string.toast_google_sign_in_failed),
                                    Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        hideLoading();
                    }
                });

        setupViews();
        setupRegisterViews();
    }

    private void setupViews() {
        loadingContainer = findViewById(R.id.loading_container);

        // Setup register view if it exists
        if (binding.registerContainer != null) {
            setupRegisterViews();
        }

        binding.btnLogin.setOnClickListener(v -> {
            String email = binding.tilEmail.getEditText().getText().toString().trim();
            String password = binding.tilPassword.getEditText().getText().toString().trim();
            if (!email.isEmpty() && !password.isEmpty()) {
                showLoading();
                signInWithEmailPassword(email, password);
            } else {
                Toast.makeText(this, getString(R.string.toast_enter_email_password), Toast.LENGTH_SHORT).show();
            }
        });

        binding.btnGoogleLogin.setOnClickListener(v -> {
            showLoading();
            signInWithGoogle();
        });

        binding.tvGoToRegister.setOnClickListener(v -> {
            switchToRegisterMode();
        });

        binding.tvForgotPassword.setOnClickListener(v -> {
            Toast.makeText(this, getString(R.string.coming_soon), Toast.LENGTH_SHORT).show();
        });
    }

    private void signInWithEmailPassword(String email, String password) {
        // Check if user is offline and has offline credentials
        if (!isOnline()) {
            // Try offline login
            boolean isOfflineUser = prefs.getBoolean(KEY_IS_OFFLINE_USER, false);
            String offlineEmail = prefs.getString(KEY_OFFLINE_EMAIL, "");
            String offlinePassword = prefs.getString(KEY_OFFLINE_PASSWORD, "");

            if (isOfflineUser && email.equals(offlineEmail) && password.equals(offlinePassword)) {
                Log.d(TAG, "Offline login successful");
                hideLoading();
                Toast.makeText(this, "Logged in offline", Toast.LENGTH_SHORT).show();
                navigateToHome();
                return;
            } else {
                hideLoading();
                Toast.makeText(this, "Offline login failed. Check your credentials or connect to internet.",
                        Toast.LENGTH_LONG).show();
                return;
            }
        }

        // Online login with Firebase
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            // Save credentials for offline mode after successful login
                            SharedPreferences.Editor editor = prefs.edit();
                            editor.putString(KEY_OFFLINE_EMAIL, email);
                            editor.putString(KEY_OFFLINE_PASSWORD, password);
                            if (user.getEmail() != null) {
                                editor.putString("user_email", user.getEmail());
                            }
                            if (user.getDisplayName() != null) {
                                editor.putString("user_display_name", user.getDisplayName());
                            }
                            // Clear offline flag when successfully logged in online
                            editor.putBoolean(KEY_IS_OFFLINE_USER, false);
                            editor.apply();

                            user.reload().addOnCompleteListener(reloadTask -> {
                                FirebaseUser refreshedUser = mAuth.getCurrentUser();
                                if (refreshedUser != null) {
                                    Log.d(TAG, "User logged in - Email: " + refreshedUser.getEmail()
                                            + ", Name: " + refreshedUser.getDisplayName());
                                }
                                hideLoading();
                                navigateToHome();
                            });
                        } else {
                            hideLoading();
                            navigateToHome();
                        }
                    } else {
                        hideLoading();
                        Toast.makeText(this, getString(R.string.toast_auth_failed), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void signInWithGoogle() {
        if (!isOnline()) {
            hideLoading();
            Toast.makeText(this, "Google Sign-In requires internet connection", Toast.LENGTH_SHORT).show();
            return;
        }

        mGoogleSignInClient.signOut().addOnCompleteListener(this, task -> {
            Intent signInIntent = mGoogleSignInClient.getSignInIntent();
            googleSignInLauncher.launch(signInIntent);
        });
    }

    private void firebaseAuthWithGoogle(String idToken, String email, String displayName) {
        Log.d(TAG, "Authenticating with Google - Email: " + email + ", Name: " + displayName);
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();

                        String userEmail = user != null ? user.getEmail() : null;
                        if (userEmail == null && user != null && !user.getProviderData().isEmpty()) {
                            for (var profile : user.getProviderData()) {
                                if (profile.getEmail() != null && !profile.getEmail().isEmpty()) {
                                    userEmail = profile.getEmail();
                                    Log.d(TAG, "Email found in provider data: " + userEmail);
                                    break;
                                }
                            }
                        }
                        if (userEmail == null) {
                            userEmail = email;
                            Log.d(TAG, "Using email from GoogleSignInAccount: " + userEmail);
                        }

                        Log.d(TAG, "Sign in successful - User email from Firebase: " + userEmail
                                + ", Display name: " + (user != null ? user.getDisplayName() : "null"));

                        if (user != null) {
                            if (userEmail != null || displayName != null) {
                                SharedPreferences.Editor editor = getSharedPreferences("VoyagerBudsPrefs", MODE_PRIVATE)
                                        .edit();
                                if (userEmail != null) {
                                    editor.putString("user_email", userEmail);
                                }
                                if (displayName != null) {
                                    editor.putString("user_display_name", displayName);
                                }
                                editor.apply();
                            }

                            if (user.getDisplayName() == null || user.getDisplayName().isEmpty()) {
                                UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                        .setDisplayName(displayName != null ? displayName : "User")
                                        .build();
                                user.updateProfile(profileUpdates).addOnCompleteListener(profileTask -> {
                                    if (profileTask.isSuccessful()) {
                                        user.reload().addOnCompleteListener(reloadTask -> {
                                            FirebaseUser refreshedUser = mAuth.getCurrentUser();
                                            if (refreshedUser != null) {
                                                Log.d(TAG, "Google login - Email: " + refreshedUser.getEmail()
                                                        + ", Name: " + refreshedUser.getDisplayName());
                                            }
                                            hideLoading();
                                            navigateToHome();
                                        });
                                    } else {
                                        Log.e(TAG, "Failed to update profile", profileTask.getException());
                                        hideLoading();
                                        navigateToHome();
                                    }
                                });
                                return;
                            }
                            user.reload().addOnCompleteListener(reloadTask -> {
                                FirebaseUser refreshedUser = mAuth.getCurrentUser();
                                if (refreshedUser != null) {
                                    Log.d(TAG, "Google login (existing name) - Email: " + refreshedUser.getEmail()
                                            + ", Name: " + refreshedUser.getDisplayName());
                                }
                                hideLoading();
                                navigateToHome();
                            });
                            return;
                        }
                        hideLoading();
                        navigateToHome();
                    } else {
                        hideLoading();
                        if (task.getException() instanceof FirebaseAuthUserCollisionException) {
                            promptForPasswordAndLink(email, credential);
                        } else {
                            Toast.makeText(this,
                                    getString(R.string.toast_auth_failed_detailed, task.getException().getMessage()),
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    private void promptForPasswordAndLink(String email, AuthCredential credentialToLink) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.link_account_title);
        builder.setMessage(R.string.link_account_message);

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        builder.setView(input);

        builder.setPositiveButton(R.string.link, (dialog, which) -> {
            String password = input.getText().toString();
            if (password.isEmpty()) {
                Toast.makeText(this, getString(R.string.toast_password_required), Toast.LENGTH_SHORT).show();
                return;
            }

            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(signInTask -> {
                        if (signInTask.isSuccessful()) {
                            FirebaseUser user = signInTask.getResult().getUser();
                            if (user != null) {
                                user.linkWithCredential(credentialToLink)
                                        .addOnCompleteListener(linkTask -> {
                                            if (linkTask.isSuccessful()) {
                                                Toast.makeText(this,
                                                        getString(R.string.toast_accounts_linked_successful),
                                                        Toast.LENGTH_SHORT).show();
                                                navigateToHome();
                                            } else {
                                                Toast.makeText(this,
                                                        getString(R.string.toast_failed_to_link_accounts),
                                                        Toast.LENGTH_SHORT).show();
                                            }
                                        });
                            }
                        } else {
                            Toast.makeText(this,
                                    getString(R.string.toast_incorrect_password_accounts_not_linked),
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void showLoading() {
        if (loadingContainer != null) {
            loadingContainer.setVisibility(View.VISIBLE);
        }
    }

    private void hideLoading() {
        if (loadingContainer != null) {
            loadingContainer.setVisibility(View.GONE);
        }
    }

    private void switchToRegisterMode() {
        isRegisterMode = true;
        if (binding.loginContainer != null) {
            binding.loginContainer.setVisibility(View.GONE);
        }
        if (binding.registerContainer != null) {
            binding.registerContainer.setVisibility(View.VISIBLE);
        }
    }

    private void switchToLoginMode() {
        isRegisterMode = false;
        if (binding.registerContainer != null) {
            binding.registerContainer.setVisibility(View.GONE);
        }
        if (binding.loginContainer != null) {
            binding.loginContainer.setVisibility(View.VISIBLE);
        }
    }

    private void setupRegisterViews() {
        // Register button
        binding.btnCreateAccount.setOnClickListener(v -> handleRegistration());

        // Google sign up
        binding.btnGoogleSignup.setOnClickListener(v -> {
            showLoading();
            signInWithGoogle();
        });

        // Back to login
        binding.tvGoToLogin.setOnClickListener(v -> switchToLoginMode());
    }

    private void handleRegistration() {
        String fullName = binding.etFullName.getText().toString().trim();
        String email = binding.etEmailRegister.getText().toString().trim();
        String password = binding.etPasswordRegister.getText().toString().trim();
        String confirmPassword = binding.etConfirmPassword.getText().toString().trim();

        if (!validateRegistrationInput(fullName, email, password, confirmPassword)) {
            return;
        }

        showLoading();

        if (isOnline()) {
            // Online registration
            mAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, task -> {
                        hideLoading();
                        if (task.isSuccessful()) {
                            FirebaseUser user = mAuth.getCurrentUser();
                            if (user != null) {
                                saveUserInfo(user.getUid(), fullName, email, false);
                                Toast.makeText(this, "Registration successful!", Toast.LENGTH_SHORT).show();
                                navigateToHome();
                            }
                        } else {
                            Toast.makeText(this, "Registration failed: " +
                                    (task.getException() != null ? task.getException().getMessage() : "Unknown error"),
                                    Toast.LENGTH_LONG).show();
                        }
                    });
        } else {
            // Offline registration
            String offlineUid = "offline_" + java.util.UUID.randomUUID().toString();
            saveUserInfo(offlineUid, fullName, email, true);
            hideLoading();
            Toast.makeText(this, "Registered offline. You can sync when internet is available.",
                    Toast.LENGTH_LONG).show();
            navigateToHome();
        }
    }

    private boolean validateRegistrationInput(String fullName, String email, String password, String confirmPassword) {
        if (fullName.isEmpty()) {
            binding.etFullName.setError("Full name is required");
            binding.etFullName.requestFocus();
            return false;
        }

        if (email.isEmpty()) {
            binding.etEmail.setError("Email is required");
            binding.etEmail.requestFocus();
            return false;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.etEmail.setError("Please enter a valid email");
            binding.etEmail.requestFocus();
            return false;
        }

        if (password.isEmpty()) {
            binding.etPassword.setError("Password is required");
            binding.etPassword.requestFocus();
            return false;
        }

        if (password.length() < 6) {
            binding.etPassword.setError("Password must be at least 6 characters");
            binding.etPassword.requestFocus();
            return false;
        }

        if (!password.equals(confirmPassword)) {
            binding.etConfirmPassword.setError("Passwords do not match");
            binding.etConfirmPassword.requestFocus();
            return false;
        }

        return true;
    }

    private void saveUserInfo(String uid, String fullName, String email, boolean isOffline) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("firebase_uid", uid);
        editor.putString("user_name", fullName);
        editor.putString("user_email", email);

        if (isOffline) {
            editor.putString(KEY_OFFLINE_UID, uid);
            editor.putString(KEY_OFFLINE_EMAIL, email);
            // Note: password is already saved in handleRegistration for offline mode
            editor.putBoolean(KEY_IS_OFFLINE_USER, true);
        } else {
            editor.putBoolean(KEY_IS_OFFLINE_USER, false);
        }

        editor.apply();
    }

    private void navigateToHome() {
        Toast.makeText(this, getString(R.string.toast_auth_successful), Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(this, HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
        }
        return false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
