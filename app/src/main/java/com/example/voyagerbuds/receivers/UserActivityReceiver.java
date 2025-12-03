package com.example.voyagerbuds.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

public class UserActivityReceiver extends BroadcastReceiver {

    private static final String TAG = "UserActivityReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_USER_PRESENT.equals(intent.getAction())) {
            Log.d(TAG, "User present (screen unlocked). Updating last activity time.");
            updateLastActivityTime(context);
        }
    }

    private void updateLastActivityTime(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE);
        prefs.edit().putLong("last_activity_time", System.currentTimeMillis()).apply();
    }
}
