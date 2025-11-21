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

public class LoginFragment extends Fragment {

    private static final String TAG = "LoginFragment";
    private FragmentLoginBinding binding;

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
                            firebaseAuthWithGoogle(account.getIdToken(), account.getEmail());
                        } catch (ApiException e) {
                            Log.w(TAG, "Google sign in failed", e);
                            Toast.makeText(getContext(), "Google sign in failed.", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentLoginBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.btnLogin.setOnClickListener(v -> {
            String email = binding.tilEmail.getEditText().getText().toString().trim();
            String password = binding.tilPassword.getEditText().getText().toString().trim();
            if (!email.isEmpty() && !password.isEmpty()) {
                signInWithEmailPassword(email, password);
            } else {
                Toast.makeText(getContext(), "Please enter email and password.", Toast.LENGTH_SHORT).show();
            }
        });

        binding.btnGoogleLogin.setOnClickListener(v -> signInWithGoogle());

        binding.tvGoToRegister.setOnClickListener(v -> {
            NavController navController = Navigation.findNavController(v);
            navController.navigate(R.id.action_loginFragment_to_registerFragment);
        });

        binding.tvForgotPassword.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Coming soon!", Toast.LENGTH_SHORT).show();
        });
    }

    private void signInWithEmailPassword(String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(requireActivity(), task -> {
                    if (task.isSuccessful()) {
                        navigateToHome();
                    } else {
                        Toast.makeText(getContext(), "Authentication failed.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void signInWithGoogle() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        googleSignInLauncher.launch(signInIntent);
    }

    private void firebaseAuthWithGoogle(String idToken, String email) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(requireActivity(), task -> {
                    if (task.isSuccessful()) {
                        navigateToHome();
                    } else {
                        if (task.getException() instanceof FirebaseAuthUserCollisionException) {
                            promptForPasswordAndLink(email, credential);
                        } else {
                            Toast.makeText(getContext(), "Authentication Failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    private void promptForPasswordAndLink(String email, AuthCredential credentialToLink) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Link Account");
        builder.setMessage("An account with this email already exists. Please enter your password to link your Google account.");

        final EditText input = new EditText(requireContext());
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        builder.setView(input);

        builder.setPositiveButton("Link", (dialog, which) -> {
            String password = input.getText().toString();
            if (password.isEmpty()) {
                Toast.makeText(getContext(), "Password is required.", Toast.LENGTH_SHORT).show();
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
                                                Toast.makeText(getContext(), "Accounts linked successfully!", Toast.LENGTH_SHORT).show();
                                                navigateToHome();
                                            } else {
                                                Toast.makeText(getContext(), "Failed to link accounts.", Toast.LENGTH_SHORT).show();
                                            }
                                        });
                            }
                        } else {
                            Toast.makeText(getContext(), "Incorrect password. Accounts not linked.", Toast.LENGTH_SHORT).show();
                        }
                    });
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }


    private void navigateToHome() {
        Toast.makeText(getContext(), "Authentication Successful.", Toast.LENGTH_SHORT).show();
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
