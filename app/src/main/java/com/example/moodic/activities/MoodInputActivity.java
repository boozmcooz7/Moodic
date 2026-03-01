package com.example.moodic.activities;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;
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

/**
 * ✅ MoodInputActivity with Emulator Support
 * - Works on real device WITH speech-to-text
 * - Works on emulator WITHOUT speech-to-text
 * - Manual text input fallback
 */
public class MoodInputActivity extends AppCompatActivity {
    private static final String TAG = "MoodInputActivity";
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private static final int SPEECH_REQUEST_CODE = 100;

    // UI Elements
    private EditText moodInput;
    private Spinner genreSpinner;
    private ImageButton micButton;
    private Button analyzeButton;
    private ProgressBar progressBar;

    // Firebase & Managers
    private FirebaseAuth mAuth;
    private NotificationManager notificationManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mood_input);

        mAuth = FirebaseAuth.getInstance();
        notificationManager = new NotificationManager(this);

        // Initialize UI elements
        initializeUI();

        // Setup listeners
        setupListeners();

        // Show emulator warning if needed
        checkEmulatorAndWarn();

        Log.d(TAG, "✅ MoodInputActivity initialized");
    }

    /**
     * Initialize all UI elements
     */
    private void initializeUI() {
        moodInput = findViewById(R.id.moodInput);
        genreSpinner = findViewById(R.id.genreSpinner);
        micButton = findViewById(R.id.micButton);
        analyzeButton = findViewById(R.id.analyzeButton);
        progressBar = findViewById(R.id.loadingBar);
    }

    /**
     * Setup button listeners
     */
    private void setupListeners() {
        // Microphone button - tries STT, falls back to manual input
        micButton.setOnClickListener(v -> {
            if (isEmulator()) {
                showEmulatorInputDialog();
            } else {
                startSpeechRecognition();
            }
        });

        // Analyze button
        analyzeButton.setOnClickListener(v -> saveMoodEntry());
    }

    /**
     * Check if running on emulator
     */
    private boolean isEmulator() {
        return Build.FINGERPRINT.contains("generic") ||
                Build.FINGERPRINT.contains("unknown") ||
                Build.MODEL.contains("Emulator") ||
                Build.MODEL.contains("Android SDK");
    }

    /**
     * Show warning for emulator users
     */
    private void checkEmulatorAndWarn() {
        if (isEmulator()) {
            Log.w(TAG, "⚠️ Running on emulator - Speech Recognition not available");
            Toast.makeText(this, "💡 Emulator detected: Type mood or click mic for quick input",
                    Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Show input dialog for emulator users
     */
    private void showEmulatorInputDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Quick Mood Input");
        builder.setMessage("Emulator detected. Tap a mood or type your own:");

        // Quick mood buttons
        String[] quickMoods = {"Happy", "Sad", "Energetic", "Calm", "Focused"};
        builder.setItems(quickMoods, (dialog, which) -> {
            moodInput.setText(quickMoods[which].toLowerCase());
        });

        builder.setNegativeButton("Type Custom", (dialog, which) -> {
            // Just let user type in EditText - dialog closes
            moodInput.requestFocus();
        });

        builder.show();
    }

    /**
     * Start speech recognition (device only)
     */
    private void startSpeechRecognition() {
        Log.d(TAG, "🎤 Starting speech recognition...");

        // Check permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO)
                    != PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "⚠️ Requesting RECORD_AUDIO permission");
                ActivityCompat.requestPermissions(
                        this,
                        new String[]{android.Manifest.permission.RECORD_AUDIO},
                        REQUEST_RECORD_AUDIO_PERMISSION
                );
                return;
            }
        }

        startListening();
    }

    /**
     * Actually start the speech recognizer
     */
    private void startListening() {
        try {
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 5000);
            intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5);
            intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Tell me your mood");

            Toast.makeText(this, "🎤 Listening... Speak your mood", Toast.LENGTH_SHORT).show();
            startActivityForResult(intent, SPEECH_REQUEST_CODE);
            Log.d(TAG, "🎤 Speech recognition started");
        } catch (Exception e) {
            Log.e(TAG, "❌ Error starting speech recognizer", e);
            Toast.makeText(this, "Speech not available. Please type your mood.", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Handle speech recognition results
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == SPEECH_REQUEST_CODE) {
            if (resultCode == RESULT_OK && data != null) {
                ArrayList<String> results = data.getStringArrayListExtra(
                        RecognizerIntent.EXTRA_RESULTS);

                if (results != null && !results.isEmpty()) {
                    String recognizedText = results.get(0);
                    Log.d(TAG, "✅ Recognized: " + recognizedText);
                    moodInput.setText(recognizedText);
                    Toast.makeText(this, "Mood: " + recognizedText, Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Speech recognition failed. Please type your mood.",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * Handle permission result
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "✅ RECORD_AUDIO permission granted");
                startListening();
            } else {
                Toast.makeText(this, "Microphone permission required", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * Save mood entry
     */
    private void saveMoodEntry() {
        String mood = moodInput.getText().toString().trim();
        String genre = genreSpinner.getSelectedItem().toString();

        // Validation
        if (mood.isEmpty()) {
            Toast.makeText(this, "Please enter your mood", Toast.LENGTH_SHORT).show();
            return;
        }

        if (mood.length() < 2) {
            Toast.makeText(this, "Please enter a longer mood description", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        analyzeButton.setEnabled(false);

        String uid = mAuth.getCurrentUser().getUid();
        long timestamp = System.currentTimeMillis();

        // Run on background thread
        new Thread(() -> {
            try {
                // Step 1: Save mood entry to Firebase
                Log.d(TAG, "💾 Saving mood to Firebase: " + mood);
                InputProcessor.processMoodInput(uid, mood, genre, "", timestamp);

                // Step 2: Get music vector from AIEngine
                Log.d(TAG, "🤖 Analyzing mood with Gemini AI...");
                AIEngine.getInstance().analyzeMoodToVector(mood, new AIEngine.MusicVectorCallback() {
                    @Override
                    public void onSuccess(AIEngine.MusicVector vector) {
                        handleMoodAnalysisSuccess(mood, genre, vector);
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        handleMoodAnalysisError(t, mood);
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "❌ Error in saveMoodEntry", e);
                handleMoodAnalysisError(e, mood);
            }
        }).start();
    }

    /**
     * Handle successful mood analysis
     */
    private void handleMoodAnalysisSuccess(String mood, String genre, AIEngine.MusicVector vector) {
        try {
            Log.d(TAG, "✅ Analysis successful: " + vector);

            // Show notification
            notificationManager.showSuggestionReadyNotification(mood, vector);

            // Search for music
            YouTubeDataSource youtube = YouTubeDataSource.getInstance();
            List<Track> tracks = youtube.searchByMoodAndGenre(mood, genre);

            runOnUiThread(() -> {
                progressBar.setVisibility(View.GONE);
                analyzeButton.setEnabled(true);

                if (tracks == null || tracks.isEmpty()) {
                    Toast.makeText(MoodInputActivity.this,
                            "No tracks found. Try a different mood!", Toast.LENGTH_SHORT).show();
                } else {
                    Intent intent = new Intent(MoodInputActivity.this, ResultsActivity.class);
                    intent.putExtra("mood", mood);
                    intent.putExtra("genre", genre);
                    intent.putExtra("vector", (Parcelable) vector.toMap());
                    intent.putExtra("tracks", (ArrayList) tracks);
                    startActivity(intent);

                    moodInput.setText("");
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "❌ Error in success handler", e);
            handleMoodAnalysisError(e, mood);
        }
    }

    /**
     * Handle mood analysis error
     */
    private void handleMoodAnalysisError(Throwable t, String mood) {
        Log.e(TAG, "❌ Mood analysis error: " + t.getMessage(), t);

        runOnUiThread(() -> {
            progressBar.setVisibility(View.GONE);
            analyzeButton.setEnabled(true);

            String message = "Analysis failed: ";

            if (t.getMessage() != null && t.getMessage().contains("API")) {
                message += "Check API key in BuildConfig";
            } else if (t.getMessage() != null && t.getMessage().contains("network")) {
                message += "Check internet connection";
            } else {
                message += (t.getMessage() != null ? t.getMessage() : "Unknown error");
            }

            Log.e(TAG, "Full error: " + message);
            Toast.makeText(MoodInputActivity.this, message, Toast.LENGTH_LONG).show();
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "🗑️ MoodInputActivity destroyed");
    }
}