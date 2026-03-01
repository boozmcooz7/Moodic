package com.example.moodic.activities;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.moodic.R;
import com.example.moodic.AIEngine;
import com.example.moodic.YouTubeDataSource;
import com.example.moodic.models.Track;
import com.example.moodic.activities.TrackAdapter;


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

        new Thread(() -> {
            try {
                // Step 1: Analyze mood with AIEngine
                Log.d(TAG, "Step 1: Analyzing mood...");
//                AIEngine.MusicVector vector = AIEngine.getInstance()
//                        .analyzeMoodToVector(userMood);
//                Log.d(TAG, "✅ Music vector created: " + vector);

                // Step 2: Search YouTube for tracks
                Log.d(TAG, "Step 2: Searching YouTube...");
                YouTubeDataSource youtube = YouTubeDataSource.getInstance();
                List<Track> tracks = youtube.searchByMoodAndGenre(userMood, userGenre);
                Log.d(TAG, "✅ Found " + tracks.size() + " tracks");

                // Step 3: Update UI on main thread
                runOnUiThread(() -> {
                    showLoading(false);
                    //displayResults(vector, tracks);
                });

            } catch (Exception e) {
                Log.e(TAG, "❌ Error in recommendation flow", e);
                runOnUiThread(() -> {
                    showLoading(false);
                    showError(e.getMessage());
                });
            }
        }).start();
    }

    /**
     * Display music vector and tracks on UI
     */
    private void displayResults(AIEngine.MusicVector vector, List<Track> tracks) {
        // Display vector info
        String vectorInfo = String.format(
                "Energy: %.2f | Tempo: %.2f | Valence: %.2f | Danceability: %.2f | Acousticness: %.2f",
                vector.energy, vector.tempo, vector.valence, vector.danceability, vector.acousticness
        );
        vectorDisplay.setText(vectorInfo);
        Log.d(TAG, vectorInfo);

        // Display tracks
        if (tracks.isEmpty()) {
            Toast.makeText(this, "No tracks found for this mood", Toast.LENGTH_SHORT).show();
            retryButton.setVisibility(android.view.View.VISIBLE);
        } else {
            adapter.setTracks(tracks);
            Toast.makeText(this, "Found " + tracks.size() + " tracks!", Toast.LENGTH_SHORT).show();
        }
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