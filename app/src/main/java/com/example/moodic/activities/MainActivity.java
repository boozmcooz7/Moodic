package com.example.moodic.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.moodic.activities.SettingsActivity;
import com.example.moodic.activities.MoodInputActivity;
import com.example.moodic.activities.YouTubeSearchActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import com.example.moodic.R;
import com.example.moodic.data.YouTubeDataSource;
import com.example.moodic.engines.AIEngine;
import com.example.moodic.engines.InputProcessor;
import com.example.moodic.models.Track;
import com.example.moodic.notifications.NotificationManager;
import com.google.firebase.auth.FirebaseAuth;

/**
 * ✅ Updated MainActivity with Settings
 * - Settings button added to main screen
 * - Notification preferences configurable
 */
public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    // UI Components
    private TextView welcomeText;
    private Button moodButton;
    private Button youtubeButton;
    private Button favoritesButton;
    private Button settingsButton;  // ← NEW!
    private Button logoutButton;

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private NotificationManager notificationManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        // Check authentication
        if (currentUser == null) {
            navigateToLogin();
            return;
        }

        // Initialize managers
        notificationManager = new NotificationManager(this);

        // Setup UI
        initUI();
        setupButtonListeners();

        Log.d(TAG, "✅ MainActivity initialized");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }
    }

    /**
     * Initialize UI components
     */
    private void initUI() {
        welcomeText = findViewById(R.id.welcomeText);
        moodButton = findViewById(R.id.moodButton);
        youtubeButton = findViewById(R.id.youtubeButton);
        favoritesButton = findViewById(R.id.favoritesButton);
        settingsButton = findViewById(R.id.settingsButton);  // ← NEW!
        logoutButton = findViewById(R.id.logoutButton);

        if (currentUser != null && currentUser.getEmail() != null) {
            welcomeText.setText("Welcome, " + currentUser.getEmail() + "!");
        }

        // Load saved notification settings on startup
        loadSavedNotificationSettings();
    }

    /**
     * Load saved notification settings and schedule reminders
     */
    private void loadSavedNotificationSettings() {
        try {
            android.content.SharedPreferences prefs =
                    getSharedPreferences("MoodicSettings", MODE_PRIVATE);

            boolean enabled = SettingsActivity.areNotificationsEnabled(prefs);
            int hour = SettingsActivity.getNotificationHour(prefs);
            int minute = SettingsActivity.getNotificationMinute(prefs);

            if (enabled) {
                notificationManager.scheduleDailyReminder(hour, minute);
                Log.d(TAG, "✅ Reminder scheduled for " + hour + ":" + minute);
            } else {
                notificationManager.cancelReminder();
                Log.d(TAG, "ℹ️ Reminders disabled");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading settings", e);
            // Use default: 9:00 AM reminders enabled
            notificationManager.scheduleDailyReminder(9, 0);
        }
    }

    /**
     * Setup button listeners
     */
    private void setupButtonListeners() {
        // Mood analysis
        moodButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, MoodInputActivity.class);
            startActivity(intent);
        });

        // YouTube search
        youtubeButton.setOnClickListener(v -> {
            startActivity(new Intent(this, YouTubeSearchActivity.class));
        });

        // Favorites
        favoritesButton.setOnClickListener(v ->
                Toast.makeText(this, "Favorites coming soon!", Toast.LENGTH_SHORT).show());


        settingsButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(intent);
        });

        // Logout
        logoutButton.setOnClickListener(v -> {
            mAuth.signOut();
            navigateToLogin();
            Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
        });
    }

    /**
     * Navigate to login screen
     */
    private void navigateToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    /**
     * Reload settings when returning from SettingsActivity
     */
    @Override
    protected void onResume() {
        super.onResume();
        loadSavedNotificationSettings();
    }
}