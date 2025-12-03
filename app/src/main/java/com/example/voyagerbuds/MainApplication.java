package com.example.voyagerbuds;

import android.app.Application;
import android.content.Intent;
import android.content.IntentFilter;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import com.example.voyagerbuds.receivers.UserActivityReceiver;
import com.example.voyagerbuds.services.EmergencyCheckWorker;
import java.util.concurrent.TimeUnit;

public class MainApplication extends Application {

    private UserActivityReceiver userActivityReceiver;

    @Override
    public void onCreate() {
        super.onCreate();

        // Register UserActivityReceiver for screen unlock events
        userActivityReceiver = new UserActivityReceiver();
        IntentFilter filter = new IntentFilter(Intent.ACTION_USER_PRESENT);
        registerReceiver(userActivityReceiver, filter);

        // Schedule Emergency Check Worker
        scheduleEmergencyCheck();
    }

    private void scheduleEmergencyCheck() {
        PeriodicWorkRequest emergencyCheckRequest = new PeriodicWorkRequest.Builder(EmergencyCheckWorker.class, 15,
                TimeUnit.MINUTES)
                .build();

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "EmergencyCheckWork",
                ExistingPeriodicWorkPolicy.KEEP, // Keep existing work if already scheduled
                emergencyCheckRequest);
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        if (userActivityReceiver != null) {
            unregisterReceiver(userActivityReceiver);
        }
    }
}
