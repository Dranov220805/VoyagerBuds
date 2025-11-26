package com.example.voyagerbuds.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.example.voyagerbuds.R;
import com.example.voyagerbuds.activities.HomeActivity;
import com.example.voyagerbuds.databinding.FragmentRegisterBinding;
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

public class RegisterFragment extends Fragment {

    private static final String TAG = "RegisterFragment";
    private FragmentRegisterBinding binding;
    private View loadingContainer;
    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;
    private ActivityResultLauncher<Intent> googleSignInLauncher;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAuth = FirebaseAuth.getInstance();

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(requireActivity(), gso);

        googleSignInLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == getActivity().RESULT_OK) {
                        Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                        try {
                            GoogleSignInAccount account = task.getResult(ApiException.class);
                            Log.d(TAG, "Google account - Email: " + account.getEmail()
                                    + ", Name: " + account.getDisplayName());
                            firebaseAuthWithGoogle(account.getIdToken(), account.getEmail(), account.getDisplayName());
                        } catch (ApiException e) {
                            Log.w(TAG, "Google sign in failed", e);
                            hideLoading();
                            Toast.makeText(getContext(), getString(R.string.toast_google_sign_in_failed),
                                    Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        // User cancelled the sign-up
                        hideLoading();
                    }
                });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentRegisterBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        loadingContainer = view.findViewById(R.id.loading_container);

        // Apply fade-in animation
        view.setAlpha(0f);
        view.animate()
                .alpha(1f)
                .setDuration(300)
                .setStartDelay(0)
                .start();

        binding.btnCreateAccount.setOnClickListener(v -> validateAndRegister());
        binding.btnGoogleSignup.setOnClickListener(v -> {
            showLoading();
            signInWithGoogle();
        });

        binding.tvGoToLogin.setOnClickListener(v -> {
            NavController navController = Navigation.findNavController(v);
            navController.navigate(R.id.action_registerFragment_to_loginFragment);
        });
    }

    private void validateAndRegister() {
        String fullName = binding.etFullName.getText().toString().trim();
        String email = binding.etEmail.getText().toString().trim();
        String password = binding.etPassword.getText().toString().trim();
        String confirmPassword = binding.etConfirmPassword.getText().toString().trim();

        if (fullName.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(getContext(), getString(R.string.error_fill_all_fields), Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(confirmPassword)) {
            Toast.makeText(getContext(), getString(R.string.password_no_match), Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.length() < 6) {
            Toast.makeText(getContext(), getString(R.string.error_password_min_length), Toast.LENGTH_SHORT).show();
            return;
        }

        showLoading();
        registerUser(email, password, fullName);
    }

    private void registerUser(String email, String password, String fullName) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(requireActivity(), task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            // Store user data in SharedPreferences immediately
                            android.content.SharedPreferences.Editor editor = requireContext()
                                    .getSharedPreferences("VoyagerBudsPrefs", android.content.Context.MODE_PRIVATE)
                                    .edit();
                            editor.putString("user_email", email);
                            editor.putString("user_display_name", fullName);
                            editor.apply();

                            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                    .setDisplayName(fullName)
                                    .build();
                            user.updateProfile(profileUpdates).addOnCompleteListener(profileTask -> {
                                if (profileTask.isSuccessful()) {
                                    // Reload user to sync data
                                    user.reload().addOnCompleteListener(reloadTask -> {
                                        FirebaseUser refreshedUser = mAuth.getCurrentUser();
                                        if (refreshedUser != null) {
                                            Log.d(TAG, "User registered - Email: " + refreshedUser.getEmail()
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
                        } else {
                            hideLoading();
                            navigateToHome();
                        }
                    } else {
                        hideLoading();
                        if (task.getException() instanceof FirebaseAuthUserCollisionException) {
                            Toast.makeText(getContext(), getString(R.string.email_already_registered),
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(getContext(),
                                    getString(R.string.registration_failed_detail, task.getException().getMessage()),
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void signInWithGoogle() {
        // Sign out first to force account selection every time
        mGoogleSignInClient.signOut().addOnCompleteListener(requireActivity(), task -> {
            Intent signInIntent = mGoogleSignInClient.getSignInIntent();
            googleSignInLauncher.launch(signInIntent);
        });
    }

    private void firebaseAuthWithGoogle(String idToken, String email, String displayName) {
        Log.d(TAG, "Authenticating with Google - Email: " + email + ", Name: " + displayName);
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(requireActivity(), task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();

                        // Get email from provider data if user.getEmail() is null
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
                        // If still null, use the email from GoogleSignInAccount
                        if (userEmail == null) {
                            userEmail = email;
                            Log.d(TAG, "Using email from GoogleSignInAccount: " + userEmail);
                        }

                        Log.d(TAG, "Sign in successful - User email: " + userEmail
                                + ", Display name: " + (user != null ? user.getDisplayName() : "null"));

                        if (user != null) {
                            // Store email and display name in SharedPreferences as backup
                            if (userEmail != null || displayName != null) {
                                android.content.SharedPreferences.Editor editor = requireContext()
                                        .getSharedPreferences("VoyagerBudsPrefs", android.content.Context.MODE_PRIVATE)
                                        .edit();
                                if (userEmail != null) {
                                    editor.putString("user_email", userEmail);
                                }
                                if (displayName != null) {
                                    editor.putString("user_display_name", displayName);
                                }
                                editor.apply();
                            }

                            // Update profile with display name from Google account if not already set
                            if (user.getDisplayName() == null || user.getDisplayName().isEmpty()) {
                                UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                        .setDisplayName(displayName != null ? displayName : "User")
                                        .build();
                                user.updateProfile(profileUpdates).addOnCompleteListener(profileTask -> {
                                    if (profileTask.isSuccessful()) {
                                        // Reload to sync the updated profile
                                        user.reload().addOnCompleteListener(reloadTask -> {
                                            FirebaseUser refreshedUser = mAuth.getCurrentUser();
                                            if (refreshedUser != null) {
                                                Log.d(TAG, "Google signup - Email: " + refreshedUser.getEmail()
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
                            // Reload user even if display name exists
                            user.reload().addOnCompleteListener(reloadTask -> {
                                FirebaseUser refreshedUser = mAuth.getCurrentUser();
                                if (refreshedUser != null) {
                                    Log.d(TAG, "Google signup (existing name) - Email: " + refreshedUser.getEmail()
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
                        // Check if it's an account collision (email already exists with different
                        // provider)
                        if (task.getException() instanceof FirebaseAuthUserCollisionException) {
                            Toast.makeText(getContext(),
                                    getString(R.string.error_email_exists_different_provider),
                                    Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(getContext(), getString(R.string.authentication_failed), Toast.LENGTH_SHORT)
                                    .show();
                        }
                    }
                });
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

    private void navigateToHome() {
        Toast.makeText(getContext(), getString(R.string.authentication_successful), Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(getActivity(), HomeActivity.class);
        startActivity(intent);
        requireActivity().finish();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
