package com.example.voyagerbuds.services;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.voyagerbuds.utils.EmailSender;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.Tasks;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class EmergencyCheckWorker extends Worker {

    private static final String TAG = "EmergencyCheckWorker";
    private static final String PREFS_NAME = "app_prefs";
    private static final String KEY_LAST_ACTIVITY = "last_activity_time";
    private static final String KEY_ALERT_ENABLED = "emergency_alert_enabled";
    private static final String KEY_TIMEOUT_INDEX = "emergency_timeout_index";
    private static final String KEY_EMERGENCY_CONTACTS = "emergency_contacts";
    private static final String KEY_LAST_ALERT_SENT = "last_alert_sent_time";

    public EmergencyCheckWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        boolean isEnabled = prefs.getBoolean(KEY_ALERT_ENABLED, true);
        if (!isEnabled) {
            return Result.success();
        }

        long lastActivityTime = prefs.getLong(KEY_LAST_ACTIVITY, System.currentTimeMillis());
        int timeoutIndex = prefs.getInt(KEY_TIMEOUT_INDEX, 1); // Default 24h
        String contactsJson = prefs.getString(KEY_EMERGENCY_CONTACTS, "[]");

        java.util.List<String> contactEmails = new java.util.ArrayList<>();
        try {
            org.json.JSONArray jsonArray = new org.json.JSONArray(contactsJson);
            for (int i = 0; i < jsonArray.length(); i++) {
                org.json.JSONObject jsonObject = jsonArray.getJSONObject(i);
                String email = jsonObject.getString("email");
                if (email != null && !email.isEmpty()) {
                    contactEmails.add(email);
                }
            }
        } catch (org.json.JSONException e) {
            Log.e(TAG, "Error parsing emergency contacts", e);
        }

        if (contactEmails.isEmpty()) {
            Log.w(TAG, "No emergency contacts configured.");
            return Result.success();
        }

        long timeoutMillis = getTimeoutMillis(timeoutIndex);
        long currentTime = System.currentTimeMillis();
        long timeSinceActivity = currentTime - lastActivityTime;

        if (timeSinceActivity > timeoutMillis) {
            // Check if we already sent an alert recently (e.g., within the last hour) to
            // avoid spamming
            long lastAlertSent = prefs.getLong(KEY_LAST_ALERT_SENT, 0);
            if (currentTime - lastAlertSent < TimeUnit.HOURS.toMillis(1)) {
                Log.d(TAG, "Alert already sent recently. Skipping.");
                return Result.success();
            }

            Log.i(TAG, "Inactivity detected! Sending emergency alert.");

            // Get Location
            String locationString = "Unknown Location";
            if (ActivityCompat.checkSelfPermission(context,
                    Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                    ActivityCompat.checkSelfPermission(context,
                            Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

                FusedLocationProviderClient fusedLocationClient = LocationServices
                        .getFusedLocationProviderClient(context);
                try {
                    // Wait for location (timeout 5 seconds)
                    Location location = Tasks.await(fusedLocationClient.getLastLocation(), 5, TimeUnit.SECONDS);
                    if (location != null) {
                        locationString = "Lat: " + location.getLatitude() + ", Lon: " + location.getLongitude();
                        // Create a Google Maps link
                        locationString += "\nMap: https://www.google.com/maps/search/?api=1&query="
                                + location.getLatitude() + "," + location.getLongitude();
                    }
                } catch (ExecutionException | InterruptedException | TimeoutException e) {
                    Log.e(TAG, "Failed to get location", e);
                }
            } else {
                locationString = "Location permission not granted.";
            }

            // Send Email to all contacts
            String subject = "EMERGENCY ALERT: VoyagerBuds User Inactivity";
            String body = "This is an automated alert from VoyagerBuds.\n\n" +
                    "The user has been inactive for more than " + (timeoutMillis / (3600 * 1000)) + " hours.\n\n" +
                    "Last Known Location:\n" + locationString + "\n\n" +
                    "Please contact the user immediately.";

            boolean allSent = true;
            for (String email : contactEmails) {
                try {
                    EmailSender.sendEmailSync(email, subject, body);
                    Log.i(TAG, "Emergency email sent to: " + email);
                } catch (Exception e) {
                    Log.e(TAG, "Error sending emergency email to: " + email, e);
                    allSent = false;
                }
            }

            // Update last alert time if at least one email was sent
            if (allSent || contactEmails.size() > 0) {
                prefs.edit().putLong(KEY_LAST_ALERT_SENT, currentTime).apply();
            }
        }

        return Result.success();
    }

    private long getTimeoutMillis(int index) {
        switch (index) {
            case 0:
                return TimeUnit.HOURS.toMillis(12);
            case 1:
                return TimeUnit.HOURS.toMillis(24);
            case 2:
                return TimeUnit.HOURS.toMillis(48);
            case 3:
                return TimeUnit.HOURS.toMillis(72);
            default:
                return TimeUnit.HOURS.toMillis(24);
        }
    }
}
