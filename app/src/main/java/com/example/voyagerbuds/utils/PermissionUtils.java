package com.example.voyagerbuds.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

public class PermissionUtils {

    // Permission groups
    public static final int PERMISSION_REQUEST_CODE = 1001;

    // Essential permissions (required for app to function)
    public static final String[] ESSENTIAL_PERMISSIONS = {
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_NETWORK_STATE
    };

    // Camera permission
    public static final String[] CAMERA_PERMISSIONS = {
            Manifest.permission.CAMERA
    };

    // Location permissions
    public static final String[] LOCATION_PERMISSIONS = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
    };

    // Storage/Media permissions (version dependent)
    public static String[] getStoragePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // Android 13+
            return new String[] {
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_VIDEO
            };
        } else {
            return new String[] {
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            };
        }
    }

    // Notification permission (Android 13+)
    public static String[] getNotificationPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return new String[] { Manifest.permission.POST_NOTIFICATIONS };
        }
        return new String[] {};
    }

    /**
     * Get all required permissions for the app
     */
    public static String[] getAllRequiredPermissions() {
        List<String> permissionsList = new ArrayList<>();

        // Add camera
        for (String permission : CAMERA_PERMISSIONS) {
            permissionsList.add(permission);
        }

        // Add location
        for (String permission : LOCATION_PERMISSIONS) {
            permissionsList.add(permission);
        }

        // Add storage/media
        for (String permission : getStoragePermissions()) {
            permissionsList.add(permission);
        }

        // Add notifications
        for (String permission : getNotificationPermissions()) {
            permissionsList.add(permission);
        }

        return permissionsList.toArray(new String[0]);
    }

    /**
     * Check if a single permission is granted
     */
    public static boolean isPermissionGranted(Context context, String permission) {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Check if all permissions in an array are granted
     */
    public static boolean arePermissionsGranted(Context context, String[] permissions) {
        for (String permission : permissions) {
            if (!isPermissionGranted(context, permission)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Check if all required permissions are granted
     */
    public static boolean areAllRequiredPermissionsGranted(Context context) {
        return arePermissionsGranted(context, getAllRequiredPermissions());
    }

    /**
     * Get list of denied permissions
     */
    public static List<String> getDeniedPermissions(Context context, String[] permissions) {
        List<String> deniedPermissions = new ArrayList<>();
        for (String permission : permissions) {
            if (!isPermissionGranted(context, permission)) {
                deniedPermissions.add(permission);
            }
        }
        return deniedPermissions;
    }

    /**
     * Request permissions
     */
    public static void requestPermissions(Activity activity, String[] permissions, int requestCode) {
        ActivityCompat.requestPermissions(activity, permissions, requestCode);
    }

    /**
     * Request all required permissions
     */
    public static void requestAllRequiredPermissions(Activity activity) {
        String[] permissions = getAllRequiredPermissions();
        List<String> deniedPermissions = getDeniedPermissions(activity, permissions);

        if (!deniedPermissions.isEmpty()) {
            requestPermissions(activity,
                    deniedPermissions.toArray(new String[0]),
                    PERMISSION_REQUEST_CODE);
        }
    }

    /**
     * Check if user has permanently denied permission (should show settings)
     */
    public static boolean shouldShowRequestPermissionRationale(Activity activity, String permission) {
        return ActivityCompat.shouldShowRequestPermissionRationale(activity, permission);
    }

    /**
     * Get user-friendly permission name
     */
    public static String getPermissionName(String permission) {
        switch (permission) {
            case Manifest.permission.CAMERA:
                return "Camera";
            case Manifest.permission.ACCESS_FINE_LOCATION:
            case Manifest.permission.ACCESS_COARSE_LOCATION:
                return "Location";
            case Manifest.permission.READ_EXTERNAL_STORAGE:
            case Manifest.permission.WRITE_EXTERNAL_STORAGE:
                return "Storage";
            case Manifest.permission.READ_MEDIA_IMAGES:
                return "Photos";
            case Manifest.permission.READ_MEDIA_VIDEO:
                return "Videos";
            case Manifest.permission.POST_NOTIFICATIONS:
                return "Notifications";
            case Manifest.permission.INTERNET:
            case Manifest.permission.ACCESS_NETWORK_STATE:
                return "Internet";
            default:
                return "Unknown";
        }
    }

    /**
     * Get permission description for user
     */
    public static String getPermissionDescription(String permission) {
        switch (permission) {
            case Manifest.permission.CAMERA:
                return "Take photos of your travel destinations";
            case Manifest.permission.ACCESS_FINE_LOCATION:
            case Manifest.permission.ACCESS_COARSE_LOCATION:
                return "Track your travel locations and show them on the map";
            case Manifest.permission.READ_EXTERNAL_STORAGE:
            case Manifest.permission.WRITE_EXTERNAL_STORAGE:
            case Manifest.permission.READ_MEDIA_IMAGES:
            case Manifest.permission.READ_MEDIA_VIDEO:
                return "Save and access your travel photos and videos";
            case Manifest.permission.POST_NOTIFICATIONS:
                return "Receive emergency alerts and trip reminders";
            case Manifest.permission.INTERNET:
            case Manifest.permission.ACCESS_NETWORK_STATE:
                return "Connect to the internet for syncing and maps";
            default:
                return "Required for app functionality";
        }
    }

    /**
     * Get permission icon resource name
     */
    public static String getPermissionIcon(String permission) {
        switch (permission) {
            case Manifest.permission.CAMERA:
                return "ic_camera";
            case Manifest.permission.ACCESS_FINE_LOCATION:
            case Manifest.permission.ACCESS_COARSE_LOCATION:
                return "ic_location";
            case Manifest.permission.READ_EXTERNAL_STORAGE:
            case Manifest.permission.WRITE_EXTERNAL_STORAGE:
            case Manifest.permission.READ_MEDIA_IMAGES:
            case Manifest.permission.READ_MEDIA_VIDEO:
                return "ic_photo";
            case Manifest.permission.POST_NOTIFICATIONS:
                return "ic_notification";
            case Manifest.permission.INTERNET:
            case Manifest.permission.ACCESS_NETWORK_STATE:
                return "ic_wifi";
            default:
                return "ic_info";
        }
    }
}
