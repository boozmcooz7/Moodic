package com.example.moodic.notifications;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.example.moodic.activities.MoodInputActivity;
import com.example.moodic.engines.AIEngine;

import java.util.Calendar;

/**
 * ✅ NotificationManager - COMPLETE IMPLEMENTATION
 * - Notification Channels for Android 8.0+
 * - Mood Reminder notifications
 * - Suggestion Ready notifications
 * - AlarmManager integration for daily reminders
 */
public class NotificationManager {
    private static final String TAG = "NotificationManager";

    // Notification Channel IDs
    private static final String CHANNEL_ID_REMINDERS = "moodic_reminders";
    private static final String CHANNEL_ID_SUGGESTIONS = "moodic_suggestions";
    private static final String CHANNEL_ID_INSIGHTS = "moodic_insights";

    // Notification IDs
    private static final int REMINDER_NOTIFICATION_ID = 100;
    private static final int SUGGESTION_NOTIFICATION_ID = 101;
    private static final int INSIGHT_NOTIFICATION_ID = 102;

    private Context context;
    private android.app.NotificationManager notificationManager;
    private AlarmManager alarmManager;

    public NotificationManager(Context context) {
        this.context = context;
        this.notificationManager = (android.app.NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);
        this.alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        createNotificationChannels();
        Log.d(TAG, "✅ NotificationManager initialized");
    }

    /**
     * Create all notification channels for Android 8.0+
     */
    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Reminders Channel
            NotificationChannel remindersChannel = new NotificationChannel(
                    CHANNEL_ID_REMINDERS,
                    "Mood Reminders",
                    android.app.NotificationManager.IMPORTANCE_DEFAULT
            );
            remindersChannel.setDescription("Daily mood check reminders");
            remindersChannel.enableVibration(true);
            remindersChannel.setShowBadge(true);
            setChannelSound(remindersChannel);

            // Suggestions Channel
            NotificationChannel suggestionsChannel = new NotificationChannel(
                    CHANNEL_ID_SUGGESTIONS,
                    "Music Suggestions",
                    android.app.NotificationManager.IMPORTANCE_HIGH
            );
            suggestionsChannel.setDescription("Ready notifications for your mood suggestions");
            suggestionsChannel.enableVibration(true);
            suggestionsChannel.setShowBadge(true);
            setChannelSound(suggestionsChannel);

            // Insights Channel
            NotificationChannel insightsChannel = new NotificationChannel(
                    CHANNEL_ID_INSIGHTS,
                    "Mood Insights",
                    android.app.NotificationManager.IMPORTANCE_LOW
            );
            insightsChannel.setDescription("Daily affirmations and mood insights");
            insightsChannel.enableVibration(false);
            insightsChannel.setShowBadge(true);

            notificationManager.createNotificationChannel(remindersChannel);
            notificationManager.createNotificationChannel(suggestionsChannel);
            notificationManager.createNotificationChannel(insightsChannel);

            Log.d(TAG, "✅ Notification channels created");
        }
    }

    /**
     * Set custom sound for notification channel
     */
    private void setChannelSound(NotificationChannel channel) {
        Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            android.media.AudioAttributes audioAttributes = new android.media.AudioAttributes.Builder()
                    .setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION)
                    .build();
            channel.setSound(soundUri, audioAttributes);
        }
    }

    /**
     * Show daily mood check reminder notification
     */
    public void showMoodReminderNotification() {
        String title = "Time for a Mood Check! 🎵";
        String message = "How are you feeling right now? Let's find the perfect music!";

        Intent intent = new Intent(context, MoodInputActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID_REMINDERS)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setVibrate(new long[]{0, 500, 250, 500});

        notificationManager.notify(REMINDER_NOTIFICATION_ID, builder.build());
        Log.d(TAG, "🔔 Mood reminder notification shown");
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.getTimeInMillis(),
                    pendingIntent
            );
        } else {
            alarmManager.setInexactRepeating(
                    AlarmManager.RTC_WAKEUP,
                    calendar.getTimeInMillis(),
                    AlarmManager.INTERVAL_DAY,
                    pendingIntent
            );
        }

        Log.d(TAG, "✅ Daily reminder scheduled for " + hour + ":" + String.format("%02d", minute));
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

    // ============================================================================
    // SUGGESTION READY NOTIFICATIONS (NEW)
    // ============================================================================

    /**
     * Show "Suggestion Ready" notification when music recommendations are available
     * @param mood The user's mood
     * @param vector The music vector
     */
    public void showSuggestionReadyNotification(String mood, AIEngine.MusicVector vector) {
        String title = "🎵 Music Suggestions Ready!";
        String message = "We found the perfect songs for your " + mood + " mood!";

        Intent intent = new Intent(context, MoodInputActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                1,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID_SUGGESTIONS)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(message + "\n\nEnjoy your personalized playlist!")
                )
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setVibrate(new long[]{0, 500, 250, 500, 250, 500})
                .setCategory(NotificationCompat.CATEGORY_RECOMMENDATION);

        notificationManager.notify(SUGGESTION_NOTIFICATION_ID, builder.build());
        Log.d(TAG, "🔔 Suggestion ready notification shown for mood: " + mood);
    }

    /**
     * Alternative suggestion notification with just the mood vector
     * (Overloaded version for flexibility)
     */
    public void showMoodResultNotification(AIEngine.MusicVector vector) {
        String title = "✨ Mood Analysis Complete";
        String message = buildMoodDescription(vector);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID_SUGGESTIONS)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setVibrate(new long[]{0, 500, 250, 500});

        Intent intent = new Intent(context, MoodInputActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                1,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        builder.setContentIntent(pendingIntent);

        notificationManager.notify(SUGGESTION_NOTIFICATION_ID, builder.build());
        Log.d(TAG, "🔔 Mood result notification shown");
    }

    // ============================================================================
    // MOOD INSIGHTS NOTIFICATIONS
    // ============================================================================

    /**
     * Show daily affirmation/insight notification
     * @param message The motivational message
     * @param dailyAffirmation The affirmation text
     */
    public void showMotivationNotification(String message, String dailyAffirmation) {
        String title = "💪 Daily Affirmation";
        String fullMessage = message + "\n\n\"" + dailyAffirmation + "\"";

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID_INSIGHTS)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(fullMessage))
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_LOW);

        notificationManager.notify(INSIGHT_NOTIFICATION_ID, builder.build());
        Log.d(TAG, "🔔 Motivation notification shown");
    }

    /**
     * Show mood insight/analytics notification
     * @param message The insight message
     * @param yourMoodInsights Additional insight details
     */
    public void showInsightNotification(String message, String yourMoodInsights) {
        String title = "📊 Your Mood Insights";
        String fullMessage = message + "\n\n" + yourMoodInsights;

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID_INSIGHTS)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(fullMessage))
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_LOW);

        notificationManager.notify(INSIGHT_NOTIFICATION_ID + 1, builder.build());
        Log.d(TAG, "🔔 Insight notification shown");
    }

    // ============================================================================
    // GENERIC NOTIFICATIONS
    // ============================================================================

    /**
     * Show generic notification
     * @param title Notification title
     * @param message Notification message
     */
    public void showNotification(String title, String message) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID_REMINDERS)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(message)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        notificationManager.notify(
                (int) System.currentTimeMillis(),
                builder.build()
        );

        Log.d(TAG, "🔔 Generic notification shown: " + title);
    }

    /**
     * Cancel specific notification
     * @param notificationId The notification ID to cancel
     */
    public void cancelNotification(int notificationId) {
        notificationManager.cancel(notificationId);
        Log.d(TAG, "❌ Notification cancelled: " + notificationId);
    }

    /**
     * Cancel all notifications
     */
    public void cancelAllNotifications() {
        notificationManager.cancelAll();
        Log.d(TAG, "❌ All notifications cancelled");
    }

    // ============================================================================
    // HELPER METHODS
    // ============================================================================

    /**
     * Build mood description from music vector
     */
    private String buildMoodDescription(AIEngine.MusicVector vector) {
        StringBuilder description = new StringBuilder();

        if (vector.valence > 0.7) {
            description.append("😊 You're feeling happy! ");
        } else if (vector.valence > 0.4) {
            description.append("😐 You're feeling neutral. ");
        } else {
            description.append("😢 You're feeling a bit down. ");
        }

        if (vector.energy > 0.7) {
            description.append("Energetic vibes detected!");
        } else if (vector.energy > 0.4) {
            description.append("Moderate energy level.");
        } else {
            description.append("Relaxation mode activated.");
        }

        return description.toString();
    }
}

