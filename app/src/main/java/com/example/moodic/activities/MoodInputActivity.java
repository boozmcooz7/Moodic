package com.example.moodic.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.moodic.engines.AIEngine;
import com.example.moodic.engines.InputProcessor;
import com.example.moodic.R;
import com.example.moodic.SharedPreferencesManager;
import com.example.moodic.data.YouTubeDataSource;
import com.example.moodic.engines.SpeechToTextEngine;
import com.example.moodic.models.Track;
import com.google.firebase.auth.FirebaseAuth;

import java.util.List;

public class MoodInputActivity extends AppCompatActivity {
    private static final String TAG = "MoodInputActivity";
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;

    private EditText moodInput;
    private Spinner genreSpinner;
    private EditText trackInput;
    private Button analyzeButton;
    private ImageButton voiceInputButton;
    private ProgressBar progressBar;
    private TextView lastMoodHint;

    private FirebaseAuth mAuth;
    private SharedPreferencesManager prefs;
    private SpeechToTextEngine speechEngine;

    private static final String[] GENRES = {
            "Pop", "Rock", "Jazz", "Hip-Hop", "Classical", "Electronic", "RNB"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mood_input);

        mAuth = FirebaseAuth.getInstance();
        prefs = SharedPreferencesManager.getInstance(this);

        // Initialize UI
        moodInput = findViewById(R.id.moodInput);
        genreSpinner = findViewById(R.id.genreSpinner);
        trackInput = findViewById(R.id.trackInput);
        analyzeButton = findViewById(R.id.analyzeButton);
        voiceInputButton = findViewById(R.id.voiceInputButton);
        progressBar = findViewById(R.id.loadingBar);
        lastMoodHint = findViewById(R.id.lastMoodHint);

        // Initialize Quick Mood Buttons
        setupQuickMoodButtons();

        // Initialize Speech Engine
        speechEngine = new SpeechToTextEngine(this);
        speechEngine.setOnResultListener(text -> {
            moodInput.setText(text);
            Toast.makeText(this, "Speech Recognized: " + text, Toast.LENGTH_SHORT).show();
        });

        voiceInputButton.setOnClickListener(v -> toggleVoiceInput());
        analyzeButton.setOnClickListener(v -> saveMoodEntry());

        restoreLastEntry();
    }

    private void setupQuickMoodButtons() {
        View.OnClickListener quickMoodListener = v -> {
            Button b = (Button) v;
            // Extract just the mood name (skip the emoji)
            String text = b.getText().toString();
            String mood = text.substring(text.indexOf(' ') + 1);
            moodInput.setText(mood);
        };

        findViewById(R.id.btnHappy).setOnClickListener(quickMoodListener);
        findViewById(R.id.btnSad).setOnClickListener(quickMoodListener);
        findViewById(R.id.btnEnergetic).setOnClickListener(quickMoodListener);
        findViewById(R.id.btnCalm).setOnClickListener(quickMoodListener);
        findViewById(R.id.btnAngry).setOnClickListener(quickMoodListener);
    }

    private void toggleVoiceInput() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_RECORD_AUDIO_PERMISSION);
            return;
        }

        if (speechEngine.getState().isListening) {
            speechEngine.stopListening();
            voiceInputButton.setImageResource(android.graphics.drawable.Icon.createWithResource(this, android.R.drawable.ic_btn_speak_now).getResId()); // Reset icon
        } else {
            speechEngine.startListening();
            voiceInputButton.setImageResource(android.R.drawable.presence_audio_online); // Use a "recording" indicator icon
            Toast.makeText(this, "Listening...", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            toggleVoiceInput();
        }
    }

    private void restoreLastEntry() {
        if (!prefs.hasLastMoodEntry()) {
            if (lastMoodHint != null) lastMoodHint.setVisibility(View.GONE);
            return;
        }

        String lastMood = prefs.getLastMood();
        String lastGenre = prefs.getLastGenre();
        String lastTrack = prefs.getLastTrack();

        if (lastMoodHint != null) {
            lastMoodHint.setVisibility(View.VISIBLE);
            lastMoodHint.setText("Last time: " + lastMood + " · " + lastGenre);
        }

        if (!lastMood.isEmpty()) moodInput.setHint("Last: " + lastMood);
        if (!lastTrack.isEmpty()) trackInput.setHint("Last: " + lastTrack);

        for (int i = 0; i < GENRES.length; i++) {
            if (GENRES[i].equalsIgnoreCase(lastGenre)) {
                genreSpinner.setSelection(i);
                break;
            }
        }
    }

    private void saveMoodEntry() {
        String mood = moodInput.getText().toString().trim();
        String genre = genreSpinner.getSelectedItem().toString();
        String trackName = trackInput.getText().toString().trim();

        if (mood.isEmpty()) {
            mood = prefs.getLastMood();
        }

        if (mood.isEmpty()) {
            Toast.makeText(this, "Please enter a mood", Toast.LENGTH_SHORT).show();
            return;
        }

        prefs.saveLastMoodEntry(mood, genre, trackName);

        progressBar.setVisibility(View.VISIBLE);
        analyzeButton.setEnabled(false);

        String uid = mAuth.getCurrentUser().getUid();
        final String finalMood = mood;
        final String finalGenre = genre;

        new Thread(() -> {
            try {
                // Save to Firebase and update profile
                InputProcessor.processMoodInput(uid, finalMood, finalGenre, trackName, System.currentTimeMillis());

                // Analyze mood vector
                AIEngine.getInstance().analyzeMood(finalMood, new AIEngine.MusicVectorCallback() {
                    @Override
                    public void onSuccess(AIEngine.MusicVector vector) {
                        searchAndNavigate(finalMood, finalGenre);
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        Log.e(TAG, "AI failed, fallback to direct search", t);
                        searchAndNavigate(finalMood, finalGenre);
                    }
                });

            } catch (Exception e) {
                Log.e(TAG, "Error in mood processing", e);
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    analyzeButton.setEnabled(true);
                    Toast.makeText(MoodInputActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private void searchAndNavigate(String mood, String genre) {
        YouTubeDataSource youtube = YouTubeDataSource.getInstance();
        List<Track> tracks = youtube.searchByMoodAndGenre(mood, genre);

        runOnUiThread(() -> {
            progressBar.setVisibility(View.GONE);
            analyzeButton.setEnabled(true);

            if (tracks.isEmpty()) {
                Toast.makeText(MoodInputActivity.this, "No tracks found.", Toast.LENGTH_SHORT).show();
            } else {
                Intent intent = new Intent(MoodInputActivity.this, ResultsActivity.class);
                intent.putExtra("mood", mood);
                intent.putExtra("genre", genre);
                startActivity(intent);
                moodInput.setText("");
                trackInput.setText("");
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (speechEngine != null) {
            speechEngine.release();
        }
    }
}