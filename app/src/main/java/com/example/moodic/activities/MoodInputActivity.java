package com.example.moodic.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.example.moodic.InputProcessor;
import com.example.moodic.R;
import com.example.moodic.AIEngine;
import com.example.moodic.YouTubeDataSource;
import com.example.moodic.models.Track;
import com.google.firebase.auth.FirebaseAuth;

import java.util.List;

public class MoodInputActivity extends AppCompatActivity {
    private static final String TAG = "MoodInputActivity";

    private EditText moodInput;
    private Spinner genreSpinner;
    private EditText trackInput;
    private Button analyzeButton;
    private ProgressBar progressBar;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mood_input);

        mAuth = FirebaseAuth.getInstance();

        // Initialize UI elements - NO DUPLICATES
        moodInput = findViewById(R.id.moodInput);           // ← Single declaration
        genreSpinner = findViewById(R.id.genreSpinner);
        trackInput = findViewById(R.id.trackInput);
        analyzeButton = findViewById(R.id.analyzeButton);
        progressBar = findViewById(R.id.loadingBar);

        analyzeButton.setOnClickListener(v -> saveMoodEntry());
    }

    private void saveMoodEntry() {
        String mood = moodInput.getText().toString().trim();
        String genre = genreSpinner.getSelectedItem().toString();
        String trackName = trackInput.getText().toString().trim();

        if (mood.isEmpty()) {
            Toast.makeText(this, "Please enter a mood", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        analyzeButton.setEnabled(false);

        String uid = mAuth.getCurrentUser().getUid();

        new Thread(() -> {
            try {
                // Step 1: Save mood entry to Firebase
                InputProcessor.processMoodInput(uid, mood, genre, trackName, System.currentTimeMillis());

                // Step 2: Get music vector from AIEngine (FIXED CALLBACK)
                AIEngine.getInstance().analyzeMoodToVector(mood, new AIEngine.MusicVectorCallback() {
                    @Override
                    public void onSuccess(AIEngine.MusicVector vector) {
                        try {
                            Log.d(TAG, "✅ Music vector created: " + vector);

                            // Step 3: Search YouTube
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
                        } catch (Exception e) {
                            handleError(e);
                        }
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        handleError(t);
                    }
                });
            } catch (Exception e) {
                handleError(e);
            }
        }).start();
    }

    // Helper to keep code clean
    private void handleError(Throwable e) {
        Log.e(TAG, "❌ Error", e);
        runOnUiThread(() -> {
            progressBar.setVisibility(View.GONE);
            analyzeButton.setEnabled(true);
            Toast.makeText(MoodInputActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }
}
