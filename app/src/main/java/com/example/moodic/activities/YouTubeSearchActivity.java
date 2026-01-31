package com.example.moodic.activities;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.moodic.R;
import com.example.moodic.datasource.YouTubeDataSource;
import com.example.moodic.models.Track;

import java.util.List;

public class YouTubeSearchActivity extends AppCompatActivity {
    private static final String TAG = "YouTubeSearchActivity";
    private YouTubeDataSource youtubeDataSource;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_youtube_search);

        // Initialize YouTubeDataSource
        youtubeDataSource = YouTubeDataSource.getInstance();

        // Get UI elements
        EditText searchInput = findViewById(R.id.searchInput);
        Button searchButton = findViewById(R.id.searchButton);
        ProgressBar loadingBar = findViewById(R.id.loadingBar);

        searchButton.setOnClickListener(v -> {
            String query = searchInput.getText().toString().trim();

            // Validate input
            if (query.isEmpty()) {
                Toast.makeText(this, "Please enter a search query", Toast.LENGTH_SHORT).show();
                return;
            }

            // Show loading indicator
            loadingBar.setVisibility(android.view.View.VISIBLE);
            searchButton.setEnabled(false);

            // Search in background thread
            new Thread(() -> {
                Log.d(TAG, "Searching for: " + query);
                List<Track> results = youtubeDataSource.searchTracks(query, 10);

                runOnUiThread(() -> {
                    loadingBar.setVisibility(android.view.View.GONE);
                    searchButton.setEnabled(true);

                    if (results.isEmpty()) {
                        Toast.makeText(YouTubeSearchActivity.this,
                                "No results found", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(YouTubeSearchActivity.this,
                                "Found " + results.size() + " tracks", Toast.LENGTH_SHORT).show();

                        // Display results (you can use a RecyclerView here)
                        for (Track track : results) {
                            Log.d(TAG, "Track: " + track.getTitle() + " by " + track.getArtist());
                        }
                    }
                });
            }).start();
        });
    }
}