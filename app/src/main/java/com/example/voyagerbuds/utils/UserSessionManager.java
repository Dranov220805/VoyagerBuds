package com.example.voyagerbuds.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * Manages user session data including user ID mapping between Firebase and
 * local database
 * 
 * IMPORTANT: Firebase UID is a string, but our database uses INTEGER for
 * userId.
 * We convert Firebase UID to a stable integer using hashCode.
 * 
 * For both email/password and Google sign-in:
 * - Firebase automatically assigns a permanent UID when account is created
 * - This UID never changes for that account
 * - We hash it to get a consistent integer for database storage
 */
public class UserSessionManager {
    private static final String TAG = "UserSessionManager";
    private static final String PREF_NAME = "VoyagerBudsPrefs";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_FIREBASE_UID = "firebase_uid";

    /**
     * Get the current logged-in user's local database ID
     * Maps Firebase UID to a consistent local integer user ID
     * 
     * This works for ALL Firebase auth methods:
     * - Email/Password: Firebase creates UID on createUserWithEmailAndPassword()
     * - Google Sign-In: Firebase creates UID on signInWithCredential()
     * - Both get the SAME UID every time they log in
     */
    public static int getCurrentUserId(Context context) {
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser == null) {
            Log.w(TAG, "No Firebase user logged in");
            return -1; // No user logged in
        }

        String firebaseUid = firebaseUser.getUid();
        Log.d(TAG, "Firebase UID: " + firebaseUid);

        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

        // Check if we have a mapping for this Firebase UID
        String savedFirebaseUid = prefs.getString(KEY_FIREBASE_UID, null);
        int userId = prefs.getInt(KEY_USER_ID, -1);

        // If Firebase UID matches and we have a user ID, return it
        if (firebaseUid.equals(savedFirebaseUid) && userId != -1) {
            Log.d(TAG, "Returning cached user ID: " + userId + " for UID: " + firebaseUid);
            return userId;
        }

        // Create a new mapping using hash of Firebase UID
        // This ensures consistent user ID for the same Firebase account
        // Using positive hashCode to avoid negative user IDs
        userId = Math.abs(firebaseUid.hashCode());

        Log.d(TAG, "Created new user ID: " + userId + " for Firebase UID: " + firebaseUid);

        // Save the mapping permanently (survives logout)
        prefs.edit()
                .putString(KEY_FIREBASE_UID, firebaseUid)
                .putInt(KEY_USER_ID, userId)
                .apply();

        return userId;
    }

    /**
     * Clear user session data when logging out
     * NOTE: We keep the Firebase UID to user ID mapping so users get the same
     * local ID when they log back in with the same account
     */
    public static void clearSession(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit()
                // Don't remove KEY_USER_ID and KEY_FIREBASE_UID - keep the mapping!
                // This ensures users get their data back when they re-login
                .remove("user_email")
                .remove("user_display_name")
                .apply();
        Log.d(TAG, "Session cleared (UID mapping preserved)");
    }

    /**
     * Check if user is logged in
     */
    public static boolean isUserLoggedIn() {
        return FirebaseAuth.getInstance().getCurrentUser() != null;
    }
}
