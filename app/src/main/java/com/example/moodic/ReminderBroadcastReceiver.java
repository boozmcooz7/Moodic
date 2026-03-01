package com.example.moodic;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * ReminderBroadcastReceiver: Handles broadcast events for scheduled mood reminders
 * Triggered by AlarmManager at scheduled times
 */
public class ReminderBroadcastReceiver extends BroadcastReceiver {

    private static final String TAG = "ReminderBroadcastReceiver";

    // Intent action constants
    public static final String ACTION_MOOD_REMINDER = "com.example.moodtracker.ACTION_MOOD_REMINDER";
    public static final String ACTION_AFFIRMATION = "com.example.moodtracker.ACTION_AFFIRMATION";
    public static final String ACTION_TREND_REMINDER = "com.example.moodtracker.ACTION_TREND_REMINDER";

    // Intent extra constants
    public static final String EXTRA_REMINDER_TYPE = "reminder_type";
    public static final String EXTRA_REMINDER_ID = "reminder_id";
    public static final String EXTRA_MESSAGE = "message";
    public static final String EXTRA_TIME = "time";

    // Reminder types
    public static final String REMINDER_TYPE_DAILY_MOOD = "daily_mood";
    public static final String REMINDER_TYPE_AFFIRMATION = "affirmation";
    public static final String REMINDER_TYPE_TREND = "trend";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) {
            Log.w(TAG, "Received null intent");
            return;
        }

        String action = intent.getAction();
        Log.d(TAG, "Broadcast received: " + action);

        if (ACTION_MOOD_REMINDER.equals(action)) {
            handleMoodReminder(context, intent);
        } else if (ACTION_AFFIRMATION.equals(action)) {
            handleAffirmation(context, intent);
        } else if (ACTION_TREND_REMINDER.equals(action)) {
            handleTrendReminder(context, intent);
        } else if (Intent.ACTION_BOOT_COMPLETED.equals(action)) {
            handleBootCompleted(context);
        } else {
            Log.w(TAG, "Unknown action: " + action);
        }
    }

    /**
     * Handle daily mood reminder
     */
    private void handleMoodReminder(Context context, Intent intent) {
        try {
            int reminderId = intent.getIntExtra(EXTRA_REMINDER_ID, -1);
            String message = intent.getStringExtra(EXTRA_MESSAGE);
            if (message == null) {
                message = "How are you feeling today?";
            }
            String time = intent.getStringExtra(EXTRA_TIME);
            if (time == null) {
                time = "";
            }

            Log.d(TAG, "Handling mood reminder: ID=" + reminderId + ", Message=" + message);

            // Show notification
            MoodNotificationManager notificationManager = new MoodNotificationManager(context);
            notificationManager.showMoodReminder("Time to check in", message, time);

            // Log reminder
            logReminder(context, REMINDER_TYPE_DAILY_MOOD, reminderId);

        } catch (Exception e) {
            Log.e(TAG, "Error handling mood reminder: " + e.getMessage(), e);
        }
    }

    /**
     * Handle affirmation reminder
     */
    private void handleAffirmation(Context context, Intent intent) {
        try {
            int reminderId = intent.getIntExtra(EXTRA_REMINDER_ID, -1);
            String message = intent.getStringExtra(EXTRA_MESSAGE);
            if (message == null) {
                message = "You are doing great!";
            }

            Log.d(TAG, "Handling affirmation: ID=" + reminderId + ", Message=" + message);

            // Show notification
            MoodNotificationManager notificationManager = new MoodNotificationManager(context);
            notificationManager.showMotivationNotification(message, "Daily Affirmation");

            // Log reminder
            logReminder(context, REMINDER_TYPE_AFFIRMATION, reminderId);

        } catch (Exception e) {
            Log.e(TAG, "Error handling affirmation: " + e.getMessage(), e);
        }
    }

    /**
     * Handle trend insight reminder
     */
    private void handleTrendReminder(Context context, Intent intent) {
        try {
            int reminderId = intent.getIntExtra(EXTRA_REMINDER_ID, -1);
            String message = intent.getStringExtra(EXTRA_MESSAGE);
            if (message == null) {
                message = "Check your mood trends";
            }

            Log.d(TAG, "Handling trend reminder: ID=" + reminderId + ", Message=" + message);

            // Show notification
            MoodNotificationManager notificationManager = new MoodNotificationManager(context);
            notificationManager.showInsightNotification(message, "Your Mood Insights");

            // Log reminder
            logReminder(context, REMINDER_TYPE_TREND, reminderId);

        } catch (Exception e) {
            Log.e(TAG, "Error handling trend reminder: " + e.getMessage(), e);
        }
    }

    /**
     * Handle device boot completion
     */
    private void handleBootCompleted(Context context) {
        try {
            Log.d(TAG, "Device boot completed, rescheduling alarms");

            ReminderAlarmScheduler alarmScheduler = new ReminderAlarmScheduler(context);
            alarmScheduler.rescheduleAlarms();

            Log.d(TAG, "Alarms rescheduled successfully");

        } catch (Exception e) {
            Log.e(TAG, "Error handling boot completed: " + e.getMessage(), e);
        }
    }

    /**
     * Log reminder event to database/analytics
     */
    private void logReminder(Context context, String reminderType, int reminderId) {
        try {
            Log.d(TAG, "Logging reminder: type=" + reminderType + ", id=" + reminderId);

            // TODO: Implement database logging to Firestore
            // FirebaseFirestore db = FirebaseFirestore.getInstance();
            // Map<String, Object> reminder = new HashMap<>();
            // reminder.put("type", reminderType);
            // reminder.put("id", reminderId);
            // reminder.put("timestamp", System.currentTimeMillis());
            // reminder.put("status", "displayed");
            // db.collection("reminder_logs").add(reminder);

        } catch (Exception e) {
            Log.e(TAG, "Error logging reminder: " + e.getMessage(), e);
        }
    }
}