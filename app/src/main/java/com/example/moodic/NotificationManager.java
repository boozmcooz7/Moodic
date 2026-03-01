package com.example.moodic;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;


import java.util.Calendar;

/**
 * ✅ AlarmManager + Notification Manager
 * - Schedules daily mood check reminders
 * - Creates and displays notifications
 * - Handles alarm triggering via BroadcastReceiver
 * - Supports custom time and frequency
 */
public class NotificationManager {
    private static final String TAG = "NotificationManager";
    private static final String CHANNEL_ID = "moodic_reminders";
    private static final String CHANNEL_NAME = "Moodic Reminders";
    private static final int REMINDER_NOTIFICATION_ID = 100;

    private Context context;
    private android.app.NotificationManager notificationManager;
    private AlarmManager alarmManager;

    public NotificationManager(Context context) {
        this.context = context;
        this.notificationManager = (android.app.NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);
        this.alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        createNotificationChannel();
        Log.d(TAG, "✅ NotificationManager initialized");
    }

    /**
     * Create notification channel for Android 8.0+
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            int importance = android.app.NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    importance
            );
            channel.setDescription("Daily mood check reminders");
            channel.enableVibration(true);
            channel.setShowBadge(true);

            notificationManager.createNotificationChannel(channel);
            Log.d(TAG, "✅ Notification channel created");
        }
    }

    /**
     * Schedule daily mood reminder
     * @param hour Hour of day (0-23)
     * @param minute Minute (0-59)
     */
    public void scheduleDailyReminder(int hour, int minute) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);

        // If the time has already passed today, schedule for tomorrow
        if (calendar.before(Calendar.getInstance())) {
            calendar.add(Calendar.DATE, 1);
        }

        Intent intent = new Intent(context, ReminderBroadcastReceiver.class);
        intent.setAction("com.example.moodic.MOOD_REMINDER");

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Schedule repeating alarm
        alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.getTimeInMillis(),
                pendingIntent
        );

        Log.d(TAG, "✅ Daily reminder scheduled for " + hour + ":" +
                String.format("%02d", minute));
    }

    /**
     * Cancel daily mood reminder
     */
    public void cancelReminder() {
        Intent intent = new Intent(context, ReminderBroadcastReceiver.class);
        intent.setAction("com.example.moodic.MOOD_REMINDER");

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        alarmManager.cancel(pendingIntent);
        Log.d(TAG, "❌ Reminder cancelled");
    }

    /**
     * Show mood check reminder notification
     */
    public void showMoodReminderNotification() {
        Notification notification = buildMoodReminderNotification();
        notificationManager.notify(REMINDER_NOTIFICATION_ID, notification);
        Log.d(TAG, "🔔 Mood reminder notification shown");
    }

    /**
     * Show mood analysis result notification
     */
    public void showMoodResultNotification(AIEngine.MusicVector vector) {
        String title = "Mood Analysis Complete";
        String message = buildMoodMessage(vector);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setVibrate(new long[]{0, 500, 250, 500});

        // Add action to open app
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        builder.setContentIntent(pendingIntent);

        notificationManager.notify(REMINDER_NOTIFICATION_ID + 1, builder.build());
        Log.d(TAG, "🔔 Mood result notification shown");
    }

    /**
     * Build mood reminder notification
     */
    private Notification buildMoodReminderNotification() {
        String title = "Time for a Mood Check!";
        String message = "How are you feeling? Let's find the perfect music for your mood.";

        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                1,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        return new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setVibrate(new long[]{0, 500, 250, 500})
                .build();
    }

    /**
     * Build message from mood vector
     */
    private String buildMoodMessage(AIEngine.MusicVector vector) {
        StringBuilder message = new StringBuilder();

        if (vector.valence > 0.7) {
            message.append("😊 You're feeling happy! ");
        } else if (vector.valence > 0.4) {
            message.append("😐 You're feeling neutral. ");
        } else {
            message.append("😢 You're feeling a bit down. ");
        }

        if (vector.energy > 0.7) {
            message.append("Energetic vibes detected!");
        } else if (vector.energy > 0.4) {
            message.append("Moderate energy level.");
        } else {
            message.append("Relaxation mode activated.");
        }

        return message.toString();
    }

    /**
     * Show generic notification
     */
    public void showNotification(String title, String message) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(message)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        notificationManager.notify(
                (int) System.currentTimeMillis(),
                builder.build()
        );

        Log.d(TAG, "🔔 Generic notification shown");
    }

    /**
     * Cancel notification
     */
    public void cancelNotification(int notificationId) {
        notificationManager.cancel(notificationId);
        Log.d(TAG, "❌ Notification cancelled");
    }

    /**
     * Cancel all notifications
     */
    public void cancelAllNotifications() {
        notificationManager.cancelAll();
        Log.d(TAG, "❌ All notifications cancelled");
    }
}

