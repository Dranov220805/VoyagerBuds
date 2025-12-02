package com.example.voyagerbuds.activities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.voyagerbuds.R;
import com.example.voyagerbuds.database.DatabaseHelper;
import com.example.voyagerbuds.databinding.ActivityRegisterBinding;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

import java.util.UUID;

public class RegisterActivity extends BaseActivity {
    private static final String TAG = "RegisterActivity";
    private static final String PREFS_NAME = "VoyagerBudsPrefs";
    private static final String KEY_OFFLINE_UID = "offline_uid";
    private static final String KEY_OFFLINE_EMAIL = "offline_email";
    private static final String KEY_OFFLINE_PASSWORD = "offline_password";
    private static final String KEY_IS_OFFLINE_USER = "is_offline_user";

    private ActivityRegisterBinding binding;
    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;
    private DatabaseHelper databaseHelper;
    private SharedPreferences prefs;

    private final ActivityResultLauncher<Intent> googleSignInLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                    try {
                        GoogleSignInAccount account = task.getResult(ApiException.class);
                        firebaseAuthWithGoogle(account.getIdToken());
                    } catch (ApiException e) {
                        Log.w(TAG, "Google sign in failed", e);
                        hideLoading();
                        Toast.makeText(this, "Google sign in failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    hideLoading();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();
        databaseHelper = new DatabaseHelper(this);
        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        // Configure Google Sign-In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        setupClickListeners();
    }

    private void setupClickListeners() {
        binding.btnCreateAccount.setOnClickListener(v -> handleEmailPasswordRegistration());
        binding.btnGoogleSignup.setOnClickListener(v -> handleGoogleSignUp());
        binding.tvGoToLogin.setOnClickListener(v -> goToLogin());
    }

    private void handleEmailPasswordRegistration() {
        String fullName = binding.etFullName.getText().toString().trim();
        String email = binding.etEmail.getText().toString().trim();
        String password = binding.etPassword.getText().toString().trim();
        String confirmPassword = binding.etConfirmPassword.getText().toString().trim();

        if (!validateInput(fullName, email, password, confirmPassword)) {
            return;
        }

        if (isOnline()) {
            // Online registration with Firebase
            registerUserOnline(fullName, email, password);
        } else {
            // Offline registration
            registerUserOffline(fullName, email, password);
        }
    }

    private void registerUserOnline(String fullName, String email, String password) {
        showLoading();

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    hideLoading();
                    if (task.isSuccessful()) {
                        Log.d(TAG, "createUserWithEmail:success");
                        FirebaseUser firebaseUser = mAuth.getCurrentUser();
                        if (firebaseUser != null) {
                            // Save user to local database
                            saveUserToDatabase(firebaseUser.getUid(), fullName, email);

                            // Clear offline flag
                            prefs.edit().putBoolean(KEY_IS_OFFLINE_USER, false).apply();

                            Toast.makeText(this, "Registration successful!", Toast.LENGTH_SHORT).show();
                            navigateToHome();
                        }
                    } else {
                        Log.w(TAG, "createUserWithEmail:failure", task.getException());
                        Toast.makeText(this, "Registration failed: " +
                                (task.getException() != null ? task.getException().getMessage() : "Unknown error"),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void registerUserOffline(String fullName, String email, String password) {
        // Generate a unique offline UID
        String offlineUid = "offline_" + UUID.randomUUID().toString();

        // Save offline credentials
        prefs.edit()
                .putString(KEY_OFFLINE_UID, offlineUid)
                .putString(KEY_OFFLINE_EMAIL, email)
                .putString(KEY_OFFLINE_PASSWORD, password)
                .putBoolean(KEY_IS_OFFLINE_USER, true)
                .apply();

        // Save user to local database with offline UID
        saveUserToDatabase(offlineUid, fullName, email);

        Toast.makeText(this, "Registered offline. You can sync when internet is available.", Toast.LENGTH_LONG).show();
        navigateToHome();
    }

    private void handleGoogleSignUp() {
        if (!isOnline()) {
            Toast.makeText(this, "Google Sign-In requires internet connection", Toast.LENGTH_SHORT).show();
            return;
        }

        showLoading();
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        googleSignInLauncher.launch(signInIntent);
    }

    private void firebaseAuthWithGoogle(String idToken) {
        showLoading();
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);

        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    hideLoading();
                    if (task.isSuccessful()) {
                        Log.d(TAG, "signInWithCredential:success");
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            // Save user to local database
                            String displayName = user.getDisplayName() != null ? user.getDisplayName() : "User";
                            String email = user.getEmail() != null ? user.getEmail() : "";
                            saveUserToDatabase(user.getUid(), displayName, email);

                            // Clear offline flag
                            prefs.edit().putBoolean(KEY_IS_OFFLINE_USER, false).apply();

                            Toast.makeText(this, "Google sign up successful!", Toast.LENGTH_SHORT).show();
                            navigateToHome();
                        }
                    } else {
                        Log.w(TAG, "signInWithCredential:failure", task.getException());
                        Toast.makeText(this, "Google sign up failed: " +
                                (task.getException() != null ? task.getException().getMessage() : "Unknown error"),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void saveUserToDatabase(String firebaseUid, String fullName, String email) {
        // Store user info in SharedPreferences
        prefs.edit()
                .putString("firebase_uid", firebaseUid)
                .putString("user_name", fullName)
                .putString("user_email", email)
                .apply();

        Log.d(TAG, "User info saved - UID: " + firebaseUid + ", Name: " + fullName + ", Email: " + email);
    }

    private void goToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }

    private void navigateToHome() {
        Intent intent = new Intent(this, HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }

    private boolean validateInput(String fullName, String email, String password, String confirmPassword) {
        if (TextUtils.isEmpty(fullName)) {
            binding.etFullName.setError("Full name is required");
            binding.etFullName.requestFocus();
            return false;
        }

        if (TextUtils.isEmpty(email)) {
            binding.etEmail.setError("Email is required");
            binding.etEmail.requestFocus();
            return false;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.etEmail.setError("Please enter a valid email");
            binding.etEmail.requestFocus();
            return false;
        }

        if (TextUtils.isEmpty(password)) {
            binding.etPassword.setError("Password is required");
            binding.etPassword.requestFocus();
            return false;
        }

        if (password.length() < 6) {
            binding.etPassword.setError("Password must be at least 6 characters");
            binding.etPassword.requestFocus();
            return false;
        }

        if (TextUtils.isEmpty(confirmPassword)) {
            binding.etConfirmPassword.setError("Please confirm your password");
            binding.etConfirmPassword.requestFocus();
            return false;
        }

        if (!password.equals(confirmPassword)) {
            binding.etConfirmPassword.setError("Passwords do not match");
            binding.etConfirmPassword.requestFocus();
            return false;
        }

        return true;
    }

    private boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
        }
        return false;
    }

    private void showLoading() {
        binding.loadingContainer.setVisibility(android.view.View.VISIBLE);
        binding.btnCreateAccount.setEnabled(false);
        binding.btnGoogleSignup.setEnabled(false);
    }

    private void hideLoading() {
        binding.loadingContainer.setVisibility(android.view.View.GONE);
        binding.btnCreateAccount.setEnabled(true);
        binding.btnGoogleSignup.setEnabled(true);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}