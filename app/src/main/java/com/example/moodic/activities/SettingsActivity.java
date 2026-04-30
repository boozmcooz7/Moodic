package com.example.moodic.activities;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.moodic.R;
import com.example.moodic.notifications.NotificationManager;


/**
 * ✅ FIXED SettingsActivity
 * - Better error handling
 * - Null checks for all views
 * - Proper SharedPreferences usage
 */
public class SettingsActivity extends AppCompatActivity {
    private static final String TAG = "SettingsActivity";
    private static final String PREFS_NAME = "MoodicSettings";

    // Preferences
    private SharedPreferences prefs;
    private SharedPreferences.Editor editor;

    // UI Elements
    private TimePicker reminderTimePicker;
    private Switch enableNotificationsSwitch;
    private Spinner languageSpinner;
    private Button saveButton;
    private EditText notificationMessageEditText;
    private NotificationManager notificationManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            setContentView(R.layout.activity_settings);
            Log.d(TAG, "✅ Layout set");
        } catch (Exception e) {
            Log.e(TAG, "❌ Error setting layout: " + e.getMessage());
            Toast.makeText(this, "Error loading settings screen", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        try {
            prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            editor = prefs.edit();
            notificationManager = new NotificationManager(this);
            Log.d(TAG, "✅ SharedPreferences initialized");
        } catch (Exception e) {
            Log.e(TAG, "❌ Error initializing preferences: " + e.getMessage());
            finish();
            return;
        }

        initializeUI();
        loadSettings();
        setupListeners();

        Log.d(TAG, "✅ SettingsActivity initialized successfully");
    }

    /**
     * Initialize UI elements with null checks
     */
    private void initializeUI() {
        try {
            reminderTimePicker = findViewById(R.id.reminderTimePicker);
            enableNotificationsSwitch = findViewById(R.id.enableNotificationsSwitch);
            languageSpinner = findViewById(R.id.languageSpinner);
            saveButton = findViewById(R.id.saveSettingsButton);
            notificationMessageEditText = findViewById(R.id.notificationMessageEditText);

            // Check if all views were found
            if (reminderTimePicker == null) {
                Log.e(TAG, "❌ reminderTimePicker not found");
                Toast.makeText(this, "Error: UI elements not found", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            if (saveButton == null) {
                Log.e(TAG, "❌ saveButton not found");
                Toast.makeText(this, "Error: Save button not found", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            Log.d(TAG, "✅ All UI elements initialized");
        } catch (Exception e) {
            Log.e(TAG, "❌ Error initializing UI: " + e.getMessage());
            Toast.makeText(this, "Error initializing UI: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    /**
     * Load saved settings from SharedPreferences
     */
    private void loadSettings() {
        try {
            // Load notification time (default: 9:00 AM)
            int hour = prefs.getInt("notification_hour", 9);
            int minute = prefs.getInt("notification_minute", 0);

            if (reminderTimePicker != null) {
                reminderTimePicker.setHour(hour);
                reminderTimePicker.setMinute(minute);
            }
            Log.d(TAG, "✅ Loaded notification time: " + hour + ":" + minute);

            // Load notification enabled status (default: true)
            boolean notificationsEnabled = prefs.getBoolean("notifications_enabled", true);
            if (enableNotificationsSwitch != null) {
                enableNotificationsSwitch.setChecked(notificationsEnabled);
            }
            Log.d(TAG, "✅ Notifications enabled: " + notificationsEnabled);

            // Load language (default: English)
            String language = prefs.getString("language", "en");
            if (languageSpinner != null) {
                int languageIndex = getLanguageIndex(language);
                languageSpinner.setSelection(languageIndex);
            }
            Log.d(TAG, "✅ Loaded language: " + language);

            // Load custom notification message
            String message = prefs.getString("notification_message", "");
            if (notificationMessageEditText != null) {
                notificationMessageEditText.setText(message);
            }
        } catch (Exception e) {
            Log.e(TAG, "❌ Error loading settings: " + e.getMessage());
            Toast.makeText(this, "Error loading settings", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Get index of language in spinner
     */
    private int getLanguageIndex(String languageCode) {
        String[] languages = {"en", "es", "fr", "de", "ja", "zh", "pt", "it", "ko", "ar"};
        for (int i = 0; i < languages.length; i++) {
            if (languages[i].equals(languageCode)) {
                return i;
            }
        }
        return 0; // Default to English
    }

    /**
     * Setup button listeners
     */
    private void setupListeners() {
        try {
            // Save button
            if (saveButton != null) {
                saveButton.setOnClickListener(v -> saveSettings());
                Log.d(TAG, "✅ Save button listener set");
            }

            // Enable/disable notifications switch
            if (enableNotificationsSwitch != null) {
                enableNotificationsSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        if (isChecked) {
                            Toast.makeText(SettingsActivity.this, "✅ Notifications enabled", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(SettingsActivity.this, "❌ Notifications disabled", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
                Log.d(TAG, "✅ Switch listener set");
            }
        } catch (Exception e) {
            Log.e(TAG, "❌ Error setting up listeners: " + e.getMessage());
        }
    }

    /**
     * Save all settings
     */
    private void saveSettings() {
        try {
            // Get time from picker
            int hour = 0;
            int minute = 0;

            if (reminderTimePicker != null) {
                hour = reminderTimePicker.getHour();
                minute = reminderTimePicker.getMinute();
            }

            // Get notification enabled status
            boolean notificationsEnabled = false;
            if (enableNotificationsSwitch != null) {
                notificationsEnabled = enableNotificationsSwitch.isChecked();
            }

            // Get language
            String selectedLanguage = "en";
            if (languageSpinner != null) {
                String[] languages = {"en", "es", "fr", "de", "ja", "zh", "pt", "it", "ko", "ar"};
                int position = languageSpinner.getSelectedItemPosition();
                if (position >= 0 && position < languages.length) {
                    selectedLanguage = languages[position];
                }
            }

            // Get custom message
            String customMessage = "";
            if (notificationMessageEditText != null) {
                customMessage = notificationMessageEditText.getText().toString().trim();
            }

            // Save to SharedPreferences
            editor.putInt("notification_hour", hour);
            editor.putInt("notification_minute", minute);
            editor.putBoolean("notifications_enabled", notificationsEnabled);
            editor.putString("language", selectedLanguage);
            editor.putString("notification_message", customMessage);
            editor.apply();

            Log.d(TAG, "✅ Settings saved:");
            Log.d(TAG, "   Hour: " + hour);
            Log.d(TAG, "   Minute: " + minute);
            Log.d(TAG, "   Enabled: " + notificationsEnabled);
            Log.d(TAG, "   Language: " + selectedLanguage);

            // Schedule reminder if enabled
            if (notificationManager != null) {
                if (notificationsEnabled) {
                    notificationManager.scheduleDailyReminder(hour, minute);
                    Toast.makeText(this, "✅ Settings saved! Reminder set for " +
                            String.format("%02d:%02d", hour, minute), Toast.LENGTH_LONG).show();
                } else {
                    notificationManager.cancelReminder();
                    Toast.makeText(this, "✅ Settings saved! Reminders disabled", Toast.LENGTH_LONG).show();
                }
            }

            // Go back to MainActivity
            finish();
        } catch (Exception e) {
            Log.e(TAG, "❌ Error saving settings: " + e.getMessage());
            Toast.makeText(this, "Error saving settings: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Get saved notification hour
     */
    public static int getNotificationHour(SharedPreferences prefs) {
        return prefs.getInt("notification_hour", 9);
    }

    /**
     * Get saved notification minute
     */
    public static int getNotificationMinute(SharedPreferences prefs) {
        return prefs.getInt("notification_minute", 0);
    }

    /**
     * Check if notifications are enabled
     */
    public static boolean areNotificationsEnabled(SharedPreferences prefs) {
        return prefs.getBoolean("notifications_enabled", true);
    }

    /**
     * Get saved language
     */
    public static String getLanguage(SharedPreferences prefs) {
        return prefs.getString("language", "en");
    }

    /**
     * Get custom notification message
     */
    public static String getCustomMessage(SharedPreferences prefs) {
        return prefs.getString("notification_message", "");
    }
}