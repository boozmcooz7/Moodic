package com.example.moodic.activities;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.moodic.activities.LoginActivity;
import com.example.moodic.R;
import com.example.moodic.SharedPreferencesManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    private TextView welcomeText;
    private TextView lastMoodText;
    private Button moodButton, youtubeButton, favoritesButton, logoutButton;

    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private SharedPreferencesManager prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // PERF: Start timing to measure UI setup performance
        long startTime = System.currentTimeMillis();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        prefs = SharedPreferencesManager.getInstance(this);
        currentUser = mAuth.getCurrentUser();

        // 1. Immediate redirect if not logged in to avoid inflating/initializing views unnecessarily
        if (currentUser == null) {
            navigateToLogin();
            return;
        }

        initializeUI();
        refreshDisplayData();
        setupButtonListeners();

        Log.d(TAG, "MainActivity initialized in " + (System.currentTimeMillis() - startTime) + "ms");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(
                        new String[]{ android.Manifest.permission.POST_NOTIFICATIONS }, 200);
            }
        }
        }

    private void initializeUI() {
        welcomeText = findViewById(R.id.welcomeText);
        lastMoodText = findViewById(R.id.lastMoodText);
        moodButton = findViewById(R.id.moodButton);
        youtubeButton = findViewById(R.id.youtubeButton);
        favoritesButton = findViewById(R.id.favoritesButton);
        logoutButton = findViewById(R.id.logoutButton);
    }

    /**
     * Updates text views with latest data from Firebase or Cache.
     * Called in onCreate and could be called in onResume if data changes elsewhere.
     */
    private void refreshDisplayData() {
        if (isFinishing()) return; // Safety check
        // --- Welcome Message Logic ---
        String displayName = prefs.getUserName();
        if (!displayName.isEmpty()) {
            welcomeText.setText(getString(R.string.welcome_format, displayName));
        } else if (currentUser != null && currentUser.getEmail() != null) {
            String email = currentUser.getEmail();
            // PERF: SharedPreferencesManager should use .apply() internally for non-blocking I/O
            prefs.saveUserEmail(email);
            welcomeText.setText(getString(R.string.welcome_format, email));
        } else {
            String cachedEmail = prefs.getUserEmail();
            if (!cachedEmail.isEmpty()) {
                welcomeText.setText(getString(R.string.welcome_format, cachedEmail));
            } else {
                welcomeText.setText(R.string.welcome_back);
            }
        }


        // --- Last Mood Summary Logic ---
        if (lastMoodText != null) {
            if (!prefs.hasLastMoodEntry()) {
                lastMoodText.setVisibility(View.GONE);
            } else {
                String mood = prefs.getLastMood();
                String genre = prefs.getLastGenre();
                lastMoodText.setVisibility(View.VISIBLE);
                lastMoodText.setText(getString(R.string.last_mood_format, mood, genre));
            }
        }
    }

    private void setupButtonListeners() {
        moodButton.setOnClickListener(v ->
                startActivity(new Intent(this, MoodInputActivity.class)));

        youtubeButton.setOnClickListener(v ->
                startActivity(new Intent(this, YouTubeSearchActivity.class)));

        // Assuming FavoritesActivity is part of your project (not in manifest, but in code)
        // If FavoritesActivity doesn't exist yet, this will cause a compile error.
        favoritesButton.setOnClickListener(v ->
                startActivity(new Intent(this, FavoritesActivity.class)));

        logoutButton.setOnClickListener(v -> {
            prefs.clearSession();
            mAuth.signOut();
            navigateToLogin();
            Toast.makeText(this, R.string.logout_success, Toast.LENGTH_SHORT).show();
        });
    }

private void navigateToLogin() {
    Intent intent = new Intent(this, LoginActivity.class);
    // PERF: Clear the activity backstack so the user can't "Back" into MainActivity
    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
    startActivity(intent);
    finish();
}

    @Override
    protected void onStart() {
        super.onStart();
        // PERF: Only check auth if it was potentially null or session expired.
        // If we are already here and currentUser was valid in onCreate, usually
        // we don't need a heavy check unless the app was in background for a long time.
        if (mAuth.getCurrentUser() == null) {
            navigateToLogin();
        }
    }
}
