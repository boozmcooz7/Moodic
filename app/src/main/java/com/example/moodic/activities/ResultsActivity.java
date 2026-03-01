package com.example.moodic.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
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

        // 1. Get intent data
        userMood = getIntent().getStringExtra("mood");
        userGenre = getIntent().getStringExtra("genre");

        // Receive the tracks from MoodInputActivity
        List<Track> incomingTracks = (List<Track>) getIntent().getSerializableExtra("tracks");

        // 2. Initialize UI elements
        moodDisplay = findViewById(R.id.moodDisplay);
        vectorDisplay = findViewById(R.id.vectorDisplay);
        tracksRecyclerView = findViewById(R.id.tracksRecyclerView);
        loadingBar = findViewById(R.id.loadingBar);
        retryButton = findViewById(R.id.retryButton);

        // 3. Set up RecyclerView
        tracksRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TrackAdapter(this);
        tracksRecyclerView.setAdapter(adapter);

        // 4. Display mood info
        moodDisplay.setText("Mood: " + userMood + " | Genre: " + userGenre);

        // 5. Setup Navigation
        setupNavigationButtons();

        // 6. DECISION POINT: Show passed tracks or load new ones
        if (incomingTracks != null && !incomingTracks.isEmpty()) {
            showLoading(false);
            adapter.setTracks(incomingTracks);
            vectorDisplay.setText("Mood Vector: Applied from Analysis");
        } else {
            loadMusicRecommendations();
        }
    } // <--- This brace closes onCreate. Now we can start other methods!

    private void setupNavigationButtons() {
        Button homeButton = findViewById(R.id.homeButton);

        // 1. Retry / New Mood Button - simply closes this screen
        retryButton.setVisibility(View.VISIBLE);
        retryButton.setOnClickListener(v -> {
            finish();
        });

        // 2. Home Button - goes to MainActivity
        if (homeButton != null) {
            homeButton.setOnClickListener(v -> {
                Intent intent = new Intent(ResultsActivity.this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish();
            });
        }
    }

    private void loadMusicRecommendations() {
        showLoading(true);
        AIEngine.getInstance().analyzeMood(userMood, new AIEngine.MusicVectorCallback() {
            @Override
            public void onSuccess(AIEngine.MusicVector vector) {
                new Thread(() -> {
                    try {
                        YouTubeDataSource youtube = YouTubeDataSource.getInstance();
                        List<Track> tracks = youtube.searchByMoodAndGenre(userMood, userGenre);
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

    private void displayResults(AIEngine.MusicVector vector, List<Track> tracks) {
        String vectorInfo = "Energy: " + String.format("%.2f", vector.energy) +
                " | Tempo: " + String.format("%.2f", vector.tempo) +
                " | Valence: " + String.format("%.2f", vector.valence);

        vectorDisplay.setText(vectorInfo);

        if (tracks == null || tracks.isEmpty()) {
            Toast.makeText(this, "No tracks found", Toast.LENGTH_SHORT).show();
            retryButton.setVisibility(View.VISIBLE);
        } else {
            adapter.setTracks(tracks);
            retryButton.setVisibility(View.GONE);
        }
    }

    private void handleError(String message) {
        Log.e(TAG, message);
        runOnUiThread(() -> {
            showLoading(false);
            showError(message);
        });
    }

    private void showError(String errorMessage) {
        Toast.makeText(this, "Error: " + errorMessage, Toast.LENGTH_LONG).show();
        retryButton.setVisibility(View.VISIBLE);
        retryButton.setOnClickListener(v -> loadMusicRecommendations());
    }

    private void showLoading(boolean isLoading) {
        loadingBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        tracksRecyclerView.setVisibility(isLoading ? View.GONE : View.VISIBLE);
    }
}