package com.example.voyagerbuds.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.example.voyagerbuds.R;
import com.example.voyagerbuds.activities.HomeActivity;
import com.example.voyagerbuds.databinding.FragmentLoginBinding;
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

public class LoginFragment extends Fragment {

    private static final String TAG = "LoginFragment";
    private FragmentLoginBinding binding;
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
                        // User cancelled the sign-in
                        hideLoading();
                    }
                });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentLoginBinding.inflate(inflater, container, false);
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

        binding.btnLogin.setOnClickListener(v -> {
            String email = binding.tilEmail.getEditText().getText().toString().trim();
            String password = binding.tilPassword.getEditText().getText().toString().trim();
            if (!email.isEmpty() && !password.isEmpty()) {
                showLoading();
                signInWithEmailPassword(email, password);
            } else {
                Toast.makeText(getContext(), getString(R.string.toast_enter_email_password), Toast.LENGTH_SHORT).show();
            }
        });

        binding.btnGoogleLogin.setOnClickListener(v -> {
            showLoading();
            signInWithGoogle();
        });

        binding.tvGoToRegister.setOnClickListener(v -> {
            NavController navController = Navigation.findNavController(v);
            navController.navigate(R.id.action_loginFragment_to_registerFragment);
        });

        binding.tvForgotPassword.setOnClickListener(v -> {
            Toast.makeText(getContext(), getString(R.string.coming_soon), Toast.LENGTH_SHORT).show();
        });
    }

    private void signInWithEmailPassword(String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(requireActivity(), task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            // Store user data in SharedPreferences
                            android.content.SharedPreferences.Editor editor = requireContext()
                                    .getSharedPreferences("VoyagerBudsPrefs", android.content.Context.MODE_PRIVATE)
                                    .edit();
                            if (user.getEmail() != null) {
                                editor.putString("user_email", user.getEmail());
                            }
                            if (user.getDisplayName() != null) {
                                editor.putString("user_display_name", user.getDisplayName());
                            }
                            editor.apply();

                            // Reload user to get fresh data from server
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
                        Toast.makeText(getContext(), getString(R.string.toast_auth_failed), Toast.LENGTH_SHORT).show();
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

                        Log.d(TAG, "Sign in successful - User email from Firebase: " + userEmail
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
                            // Reload user even if display name exists
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
                            Toast.makeText(getContext(),
                                    getString(R.string.toast_auth_failed_detailed, task.getException().getMessage()),
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    private void promptForPasswordAndLink(String email, AuthCredential credentialToLink) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle(R.string.link_account_title);
        builder.setMessage(R.string.link_account_message);

        final EditText input = new EditText(requireContext());
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        builder.setView(input);

        builder.setPositiveButton(R.string.link, (dialog, which) -> {
            String password = input.getText().toString();
            if (password.isEmpty()) {
                Toast.makeText(getContext(), getString(R.string.toast_password_required), Toast.LENGTH_SHORT).show();
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
                                                Toast.makeText(getContext(),
                                                        getString(R.string.toast_accounts_linked_successful),
                                                        Toast.LENGTH_SHORT).show();
                                                navigateToHome();
                                            } else {
                                                Toast.makeText(getContext(),
                                                        getString(R.string.toast_failed_to_link_accounts),
                                                        Toast.LENGTH_SHORT).show();
                                            }
                                        });
                            }
                        } else {
                            Toast.makeText(getContext(),
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

    private void navigateToHome() {
        Toast.makeText(getContext(), getString(R.string.toast_auth_successful), Toast.LENGTH_SHORT).show();
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
