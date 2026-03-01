package com.example.moodic.activities;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.moodic.R;
import com.example.moodic.data.YouTubeDataSource;
import com.example.moodic.engines.AIEngine;
import com.example.moodic.engines.InputProcessor;
import com.example.moodic.models.Track;
import com.example.moodic.notifications.NotificationManager;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;

public class MoodInputActivity extends AppCompatActivity {
    private static final String TAG = "MoodInputActivity";
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private static final int SPEECH_REQUEST_CODE = 100;

    private EditText moodInput;
    private Spinner genreSpinner;
    private ImageButton micButton;
    private Button analyzeButton;
    private ProgressBar progressBar;

    private FirebaseAuth mAuth;
    private NotificationManager notificationManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mood_input);

        mAuth = FirebaseAuth.getInstance();
        notificationManager = new NotificationManager(this);

        initializeUI();
        setupListeners();
        checkEmulatorAndWarn();

        Log.d(TAG, "✅ MoodInputActivity initialized");
    }

    private void initializeUI() {
        moodInput = findViewById(R.id.moodInput);
        genreSpinner = findViewById(R.id.genreSpinner);
        micButton = findViewById(R.id.micButton);
        analyzeButton = findViewById(R.id.analyzeButton);
        progressBar = findViewById(R.id.loadingBar);
    }

    private void setupListeners() {
        micButton.setOnClickListener(v -> {
            if (isEmulator()) {
                showEmulatorInputDialog();
            } else {
                startSpeechRecognition();
            }
        });

        analyzeButton.setOnClickListener(v -> saveMoodEntry());
    }

    private boolean isEmulator() {
        return Build.FINGERPRINT.contains("generic") ||
                Build.FINGERPRINT.contains("unknown") ||
                Build.MODEL.contains("Emulator") ||
                Build.MODEL.contains("Android SDK");
    }

    private void checkEmulatorAndWarn() {
        if (isEmulator()) {
            Toast.makeText(this, "💡 Emulator mode: Quick input enabled", Toast.LENGTH_SHORT).show();
        }
    }

    private void showEmulatorInputDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Quick Mood Input");
        String[] quickMoods = {"Happy", "Sad", "Energetic", "Calm", "Focused"};
        builder.setItems(quickMoods, (dialog, which) -> moodInput.setText(quickMoods[which]));
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void startSpeechRecognition() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.RECORD_AUDIO}, REQUEST_RECORD_AUDIO_PERMISSION);
            return;
        }
        startListening();
    }

    private void startListening() {
        try {
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Tell me your mood");
            startActivityForResult(intent, SPEECH_REQUEST_CODE);
        } catch (Exception e) {
            Toast.makeText(this, "Speech not available", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SPEECH_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            ArrayList<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (results != null && !results.isEmpty()) {
                moodInput.setText(results.get(0));
            }
        }
    }

    private void saveMoodEntry() {
        String mood = moodInput.getText().toString().trim();
        String genre = genreSpinner.getSelectedItem().toString();

        if (mood.isEmpty() || mood.length() < 2) {
            Toast.makeText(this, "Please describe your mood", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        analyzeButton.setEnabled(false);

        String uid = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : "anonymous";
        long timestamp = System.currentTimeMillis();

        new Thread(() -> {
            try {
                // 1. Firebase Save (Optional: you can comment this if Firebase quota is also low)
                InputProcessor.processMoodInput(uid, mood, genre, "", timestamp);

                // 2. AI Analysis
                Log.d(TAG, "🤖 Attempting AI Analysis...");
                AIEngine.getInstance().analyzeMood(mood, new AIEngine.MusicVectorCallback() {
                    @Override
                    public void onSuccess(AIEngine.MusicVector vector) {
                        handleMoodAnalysisSuccess(mood, genre, vector);
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        // Check if the error is Quota related
                        if (t.getMessage() != null && t.getMessage().toLowerCase().contains("quota")) {
                            Log.w(TAG, "⚠️ Quota Exceeded! Switching to Mock Mode.");
                            switchToMockMode(mood, genre);
                        } else {
                            handleMoodAnalysisError(t, mood);
                        }
                    }
                });
            } catch (Exception e) {
                switchToMockMode(mood, genre);
            }
        }).start();
    }
    private void switchToMockMode(String mood, String genre) {
        runOnUiThread(() -> {
            Toast.makeText(this, "Quota Reached: Entering Preview Mode", Toast.LENGTH_LONG).show();

            final ArrayList<Track> mockTracks = new ArrayList<>();

            // Add 3 mock tracks so you can test your list scrolling/UI
            String[] titles = {"Golden Hour (Mock)", "Starboy (Mock)", "Levitating (Mock)"};
            String[] artists = {"JVKE", "The Weeknd", "Dua Lipa"};

            for (int i = 0; i < 3; i++) {
                Track t = new Track();
                t.setTitle(titles[i]);
                t.setArtist(artists[i]);
                t.setId("dQw4w9WgXcQ"); // Standard placeholder ID
                mockTracks.add(t);
            }

            progressBar.setVisibility(View.GONE);
            analyzeButton.setEnabled(true);

            Intent intent = new Intent(MoodInputActivity.this, ResultsActivity.class);
            intent.putExtra("mood", mood + " (Preview)");
            intent.putExtra("genre", genre);
            intent.putExtra("tracks", mockTracks);

            Log.d(TAG, "🚀 Mock Mode: Navigating to ResultsActivity");
            startActivity(intent);
        });
    }
    /**
     * ✅ FIXED: Correctly handles the transition to ResultsActivity
     */
    private void handleMoodAnalysisSuccess(String mood, String genre, AIEngine.MusicVector vector) {
        // Fetch results in the background
        List<Track> rawResults = YouTubeDataSource.getInstance().searchByMoodAndGenre(mood, genre);

        // Convert to a final ArrayList to satisfy the Lambda requirement
        final ArrayList<Track> finalTracks = new ArrayList<>();

        if (rawResults != null && !rawResults.isEmpty()) {
            finalTracks.addAll(rawResults);
            Log.d(TAG, "✅ Found " + finalTracks.size() + " real tracks.");
        } else {
            Log.e(TAG, "⚠️ API still blocked or no results. Adding fallback track.");
            Track fallback = new Track();
            fallback.setTitle("API Error 403 (Blocked)");
            fallback.setArtist("Check Cloud Console Project ...388");
            fallback.setId("dQw4w9WgXcQ");
            finalTracks.add(fallback);
        }

        // Switch to UI Thread
        runOnUiThread(() -> {
            progressBar.setVisibility(View.GONE);
            analyzeButton.setEnabled(true);

            Intent intent = new Intent(MoodInputActivity.this, ResultsActivity.class);
            intent.putExtra("mood", mood);
            intent.putExtra("genre", genre);

            // Note: Use putParcelableArrayListExtra if Track implements Parcelable
            // Or just putExtra if it is Serializable
            intent.putExtra("tracks", finalTracks);

            Log.d(TAG, "🚀 Navigating to ResultsActivity");
            startActivity(intent);
        });
    }

    private void handleMoodAnalysisError(Throwable t, String mood) {
        runOnUiThread(() -> {
            progressBar.setVisibility(View.GONE);
            analyzeButton.setEnabled(true);
            Log.e(TAG, "Analysis Error: ", t);
            switchToMockMode(mood, genreSpinner.getSelectedItem().toString());
        });
    }
}