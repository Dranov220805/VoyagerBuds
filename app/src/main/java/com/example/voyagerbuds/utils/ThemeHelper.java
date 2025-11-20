package com.example.voyagerbuds.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import androidx.appcompat.app.AppCompatDelegate;

public class ThemeHelper {
    private static final String SELECTED_THEME = "Theme.Helper.Selected.Mode";
    public static final String LIGHT_MODE = "light";
    public static final String DARK_MODE = "dark";
    public static final String SYSTEM_MODE = "system";

    public static void applyTheme(String themeMode) {
        switch (themeMode) {
            case LIGHT_MODE:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case DARK_MODE:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            case SYSTEM_MODE:
            default:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
        }
    }

    public static void setTheme(Context context, String themeMode) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(SELECTED_THEME, themeMode);
        editor.apply();
        applyTheme(themeMode);
    }

    public static String getTheme(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        return preferences.getString(SELECTED_THEME, LIGHT_MODE);
    }

    public static void initTheme(Context context) {
        String theme = getTheme(context);
        applyTheme(theme);
    }
}
