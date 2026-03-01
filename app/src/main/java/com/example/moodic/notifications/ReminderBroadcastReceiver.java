package com.example.moodic.notifications;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.example.moodic.notifications.NotificationManager;

public class ReminderBroadcastReceiver extends BroadcastReceiver {
    private static final String TAG = "ReminderReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if ("com.example.moodic.MOOD_REMINDER".equals(intent.getAction())) {
            Log.d(TAG, "🔔 Alarm triggered - showing mood reminder");

            NotificationManager notificationManager = new NotificationManager(context);
            notificationManager.showMoodReminderNotification();

            // Auto-reschedule for next day
            notificationManager.scheduleDailyReminder(9, 0);
        }
    }
}