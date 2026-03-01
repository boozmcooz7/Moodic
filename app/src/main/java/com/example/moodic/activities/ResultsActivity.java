package com.example.moodic.activities;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.moodic.R;
import com.example.moodic.engines.AIEngine;
import com.example.moodic.data.YouTubeDataSource;
import com.example.moodic.models.Track;


import java.util.List;

public class ResultsActivity extends AppCompatActivity {
    private static final String TAG = "ResultsActivity";

    private TextView moodDisplay;
    private TextView vectorDisplay;
    private RecyclerView tracksRecyclerView;
    private ProgressBar loadingBar;
    private Button retryButton;
    private TrackAdapter adapter;

    private String userMood;
    private String userGenre;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_results);

        // Get intent data
        userMood = getIntent().getStringExtra("mood");
        userGenre = getIntent().getStringExtra("genre");

        // Initialize UI elements
        moodDisplay = findViewById(R.id.moodDisplay);
        vectorDisplay = findViewById(R.id.vectorDisplay);
        tracksRecyclerView = findViewById(R.id.tracksRecyclerView);
        loadingBar = findViewById(R.id.loadingBar);
        retryButton = findViewById(R.id.retryButton);

        // Set up RecyclerView
        tracksRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TrackAdapter(this);
        tracksRecyclerView.setAdapter(adapter);

        // Display mood info
        moodDisplay.setText("Mood: " + userMood + " | Genre: " + userGenre);

        // Start the integration flow
        loadMusicRecommendations();
    }

    /**
     * Main integration flow: Input → AI → YouTube Search → Display
     */
    private void loadMusicRecommendations() {
        showLoading(true);

        // Step 1: Analyze mood with AIEngine using the new Callback
        AIEngine.getInstance().analyzeMoodToVector(userMood, new AIEngine.MusicVectorCallback() {
            @Override
            public void onSuccess(AIEngine.MusicVector vector) {
                // Step 2: Search YouTube in a background thread
                new Thread(() -> {
                    try {
                        Log.d(TAG, "✅ Music vector created: " + vector);
                        Log.d(TAG, "Step 2: Searching YouTube...");

                        YouTubeDataSource youtube = YouTubeDataSource.getInstance();
                        List<Track> tracks = youtube.searchByMoodAndGenre(userMood, userGenre);
                        Log.d(TAG, "✅ Found " + tracks.size() + " tracks");

                        // Step 3: Update UI on main thread
                        runOnUiThread(() -> {
                            showLoading(false);
                            displayResults(vector, tracks);
                        });

                    } catch (Exception e) {
                        handleError("YouTube search failed: " + e.getMessage());
                    }
                }).start();
            }

            @Override
            public void onFailure(Throwable t) {
                handleError("AI Analysis failed: " + t.getMessage());
            }
        });
    }

    /**
     * Display music vector and tracks on UI
     */
    private void displayResults(AIEngine.MusicVector vector, List<Track> tracks) {
        // Display vector info - Fixed to use standard text (no LaTeX needed here)
        String vectorInfo = "Energy: " + String.format("%.2f", vector.energy) +
                " | Tempo: " + String.format("%.2f", vector.tempo) +
                " | Valence: " + String.format("%.2f", vector.valence);

        vectorDisplay.setText(vectorInfo);

        // Update the RecyclerView Adapter
        if (tracks == null || tracks.isEmpty()) {
            Toast.makeText(this, "No tracks found for this mood", Toast.LENGTH_SHORT).show();
            retryButton.setVisibility(android.view.View.VISIBLE);
        } else {
            // This is the critical line that actually shows the music!
            adapter.setTracks(tracks);
            retryButton.setVisibility(android.view.View.GONE);
        }
    }

    private void handleError(String message) {
        Log.e(TAG, message);
        runOnUiThread(() -> {
            showLoading(false);
            showError(message);
        });
    }

    /**
     * Show error message
     */
    private void showError(String errorMessage) {
        Toast.makeText(this, "Error: " + errorMessage, Toast.LENGTH_LONG).show();
        retryButton.setVisibility(android.view.View.VISIBLE);
        retryButton.setOnClickListener(v -> loadMusicRecommendations());
    }

    /**
     * Show/hide loading indicator
     */
    private void showLoading(boolean isLoading) {
        loadingBar.setVisibility(isLoading ? android.view.View.VISIBLE : android.view.View.GONE);
        tracksRecyclerView.setVisibility(isLoading ? android.view.View.GONE : android.view.View.VISIBLE);
    }
}