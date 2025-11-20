package com.example.voyagerbuds.fragments;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.voyagerbuds.R;
import com.example.voyagerbuds.utils.LocaleHelper;
import com.example.voyagerbuds.utils.ThemeHelper;

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
    private Button btnEditProfile;
    private Button btnLogout;
    private TextView tvSettings;
    private TextView tvNotifications;
    private TextView tvPrivacy;
    private TextView tvLanguageLabel;
    private TextView tvThemeLabel;
    private TextView tvHelp;

    public ProfileFragment() {
        // Required empty public constructor
    }

    public static ProfileFragment newInstance() {
        return new ProfileFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
        btnEditProfile = view.findViewById(R.id.btn_edit_profile);
        btnLogout = view.findViewById(R.id.btn_logout);
        tvSettings = view.findViewById(R.id.tv_settings);
        tvNotifications = view.findViewById(R.id.tv_notifications);
        tvPrivacy = view.findViewById(R.id.tv_privacy);
        tvLanguageLabel = view.findViewById(R.id.tv_language);
        tvThemeLabel = view.findViewById(R.id.tv_theme);
        tvHelp = view.findViewById(R.id.tv_help);

        // Update current language display
        updateLanguageDisplay();

        // Update current theme display
        updateThemeDisplay();

        // Set up language selector
        btnLanguage.setOnClickListener(v -> showLanguageDialog());

        // Set up theme selector
        btnTheme.setOnClickListener(v -> showThemeDialog());
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
    }
}