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
                Log.d(TAG, "✅ AI Success: " + vector.energy);
                executeYouTubeSearch(userMood + " " + userGenre, vector);            }

            @Override
            public void onFailure(Throwable t) {
                Log.e(TAG, "❌ AI Failed: " + t.getMessage());
                // 🔥 BYPASS: If AI fails, don't show an error Toast. Just fetch music!
                executeYouTubeSearch(userMood + " " + userGenre, null);
            }
        });
    }
    private void executeYouTubeSearch(String query, AIEngine.MusicVector vector) {
        new Thread(() -> {
            try {
                // Clean up the query
                String searchQuery = query + " music song";
                List<Track> tracks = YouTubeDataSource.getInstance().searchTracks(searchQuery, 10);

                runOnUiThread(() -> {
                    showLoading(false);
                    displayResults(vector, tracks);
                    if (vector == null) {
                        Toast.makeText(this, "Showing standard results", Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                handleError("Search failed: " + e.getMessage());
            }
        }).start();
    }

    private void fetchTracks(AIEngine.MusicVector vector, String queryText) {
        new Thread(() -> {
            try {
                // Append "music" to make the query professional
                List<Track> tracks = YouTubeDataSource.getInstance().searchTracks(queryText + " music", 10);
                runOnUiThread(() -> {
                    showLoading(false);
                    displayResults(vector, tracks);
                });
            } catch (Exception e) {
                handleError("Network error: " + e.getMessage());
            }
        }).start();
    }


        private void displayResults(AIEngine.MusicVector vector, List<Track> tracks) {
            runOnUiThread(() -> {
                if (tracks == null || tracks.isEmpty()) {
                    // This is what you see now because of the 403 error
                    Log.e("MOODIC", "No tracks to display");
                } else {
                    // Once you click ENABLE in the console, this will run!
                    adapter.setTracks(tracks);
                    adapter.notifyDataSetChanged();
                    Log.d("MOODIC", "UI Updated with " + tracks.size() + " songs");
                }
            });
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