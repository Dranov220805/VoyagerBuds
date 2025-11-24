package com.example.voyagerbuds.receivers;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.preference.PreferenceManager;

import com.example.voyagerbuds.R;
import com.example.voyagerbuds.activities.HomeActivity;

public class NotificationReceiver extends BroadcastReceiver {

    public static final String CHANNEL_ID = "voyager_buds_schedule_channel";
    public static final String EXTRA_TITLE = "extra_title";
    public static final String EXTRA_MESSAGE = "extra_message";
    public static final String EXTRA_SCHEDULE_ID = "extra_schedule_id";

    @Override
    public void onReceive(Context context, Intent intent) {
        // Check if notifications are enabled in settings
        SharedPreferences prefs = context.getSharedPreferences("VoyagerBudsPrefs", Context.MODE_PRIVATE);
        boolean notificationsEnabled = prefs.getBoolean("notifications_enabled", true);

        if (!notificationsEnabled) {
            return;
        }

        String title = intent.getStringExtra(EXTRA_TITLE);
        String message = intent.getStringExtra(EXTRA_MESSAGE);
        int scheduleId = intent.getIntExtra(EXTRA_SCHEDULE_ID, -1);

        createNotificationChannel(context);

        Intent openAppIntent = new Intent(context, HomeActivity.class);
        openAppIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        openAppIntent.putExtra(EXTRA_SCHEDULE_ID, scheduleId);
        // You could pass extra data to navigate to specific schedule
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, openAppIntent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification) // Make sure this icon exists or use a valid one
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setContentIntent(pendingIntent)
                .setFullScreenIntent(pendingIntent, true) // This is key for waking up the screen
                .setAutoCancel(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        try {
            notificationManager.notify(scheduleId, builder.build());
        } catch (SecurityException e) {
            // Handle missing permission
        }
    }

    private void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Schedule Notifications";
            String description = "Notifications for upcoming schedule events";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            channel.enableVibration(true);
            channel.enableLights(true);
            channel.setLockscreenVisibility(android.app.Notification.VISIBILITY_PUBLIC);
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
}
