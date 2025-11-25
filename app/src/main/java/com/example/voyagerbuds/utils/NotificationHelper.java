package com.example.voyagerbuds.utils;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import com.example.voyagerbuds.models.ScheduleItem;
import com.example.voyagerbuds.receivers.NotificationReceiver;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class NotificationHelper {

    public static void scheduleNotification(Context context, ScheduleItem item) {
        if (item.getNotifyBeforeMinutes() <= 0) {
            cancelNotification(context, item.getId());
            return;
        }

        try {
            // Parse date and time
            // Assuming day is "yyyy-MM-dd" and startTime is "HH:mm"
            LocalDate date = LocalDate.parse(item.getDay());
            LocalTime time = LocalTime.parse(item.getStartTime());
            LocalDateTime dateTime = LocalDateTime.of(date, time);

            // Subtract minutes
            LocalDateTime notifyTime = dateTime.minusMinutes(item.getNotifyBeforeMinutes());

            // Convert to millis
            long triggerAtMillis = notifyTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();

            // Check if time is in the past
            if (triggerAtMillis < System.currentTimeMillis()) {
                Log.d("NotificationHelper", "Notification time is in the past, skipping.");
                return;
            }

            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            Intent intent = new Intent(context, NotificationReceiver.class);
            intent.putExtra(NotificationReceiver.EXTRA_TITLE, "Upcoming Event: " + item.getTitle());
            intent.putExtra(NotificationReceiver.EXTRA_MESSAGE, "Event starts at " + item.getStartTime());
            intent.putExtra(NotificationReceiver.EXTRA_SCHEDULE_ID, item.getId());

            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    context,
                    item.getId(),
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            if (alarmManager != null) {
                // Use setAlarmClock for maximum reliability and to bypass battery
                // optimizations.
                // This ensures the notification fires exactly when requested, even in Doze
                // mode.
                // It also doesn't require the SCHEDULE_EXACT_ALARM permission on Android 12+.

                // Create an intent to open the app if the user taps the alarm icon info
                // (optional)
                Intent viewIntent = new Intent(context, com.example.voyagerbuds.activities.HomeActivity.class);
                viewIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                PendingIntent viewPendingIntent = PendingIntent.getActivity(
                        context,
                        item.getId(),
                        viewIntent,
                        PendingIntent.FLAG_IMMUTABLE);

                AlarmManager.AlarmClockInfo info = new AlarmManager.AlarmClockInfo(triggerAtMillis, viewPendingIntent);
                alarmManager.setAlarmClock(info, pendingIntent);

                Log.d("NotificationHelper", "Scheduled alarm for " + notifyTime.toString());
            }

        } catch (SecurityException se) {
            Log.e("NotificationHelper", "Permission denied for alarm", se);
        } catch (Exception e) {
            Log.e("NotificationHelper", "Error scheduling notification", e);
        }
    }

    public static void cancelNotification(Context context, int scheduleId) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, NotificationReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                scheduleId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        if (alarmManager != null) {
            alarmManager.cancel(pendingIntent);
        }
    }
}
