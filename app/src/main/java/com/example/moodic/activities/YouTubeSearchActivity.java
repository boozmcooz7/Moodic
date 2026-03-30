package com.example.moodic.activities;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.moodic.R;
import com.example.moodic.data.YouTubeDataSource;
import com.example.moodic.models.Track;

import java.util.List;

public class YouTubeSearchActivity extends AppCompatActivity {
    private static final String TAG = "YouTubeSearchActivity";
    private YouTubeDataSource youtubeDataSource;
    private RecyclerView recyclerView;
    private TrackAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_youtube_search);

        // 1. Initialize DataSource
        youtubeDataSource = YouTubeDataSource.getInstance();
// Inside onCreate
        recyclerView = findViewById(R.id.recyclerView); // Ensure this ID exists in XML
        adapter = new TrackAdapter(this);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        // 2. Get UI elements
        EditText searchInput = findViewById(R.id.searchInput);
        Button searchButton = findViewById(R.id.searchButton);
        ProgressBar loadingBar = findViewById(R.id.loadingBar);

        searchButton.setOnClickListener(v -> {
            String query = searchInput.getText().toString().trim();

            if (query.isEmpty()) {
                Toast.makeText(this, "Please enter a search query", Toast.LENGTH_SHORT).show();
                return;
            }

            loadingBar.setVisibility(View.VISIBLE);
            searchButton.setEnabled(false);

            // 3. Search in background thread
            new Thread(() -> {
                Log.d(TAG, "🔍 Starting search for: " + query);

                // IMPORTANT: Ensure your YouTubeDataSource has searchTracks(String, int)
                List<Track> results = youtubeDataSource.searchTracks(query, 10);

                runOnUiThread(() -> {
                    loadingBar.setVisibility(View.GONE);
                    searchButton.setEnabled(true);

                    if (results != null && !results.isEmpty()) {
                        // ✅ THIS IS THE MISSING LINK:
                        adapter.setTracks(results);
                        Log.d(TAG, "Adapter updated with " + results.size() + " tracks");
                    } else {
                        Toast.makeText(this, "No results found.", Toast.LENGTH_SHORT).show();
                    }
                });
            }).start();

        });
    }
}