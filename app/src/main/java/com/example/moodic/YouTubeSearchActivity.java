package com.example.moodic;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class YouTubeSearchActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_youtube_search);

        EditText searchInput = findViewById(R.id.searchInput);
        Button searchButton = findViewById(R.id.searchButton);

        searchButton.setOnClickListener(v -> {
            String query = searchInput.getText().toString();
            // TODO: Replace with actual YouTube API call
            Log.d("YouTube", "Searching for: " + query);
            Toast.makeText(this, "Searching for: " + query, Toast.LENGTH_SHORT).show();
        });
    }
}
