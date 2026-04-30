package com.example.moodic.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.moodic.BuildConfig;
import com.example.moodic.engines.InputProcessor;
import com.example.moodic.R;
import com.example.moodic.engines.MoodAIHelper;
import com.example.moodic.data.YouTubeDataSource;
import com.example.moodic.models.Track;
import com.google.firebase.auth.FirebaseAuth;

import java.util.List;

/**
 * Updated MoodInputActivity
 *
 * Task 2 integration:
 *   1. User types free-text mood (e.g. "I'm feeling lonely in the rain")
 *   2. MoodAIHelper calls Gemini → returns 3-5 musical keywords
 *   3. Keywords are joined and used as the YouTube search query
 *   4. Results passed to ResultsActivity as usual
 */
public class MoodInputActivity extends AppCompatActivity {

    private static final String TAG = "MoodInputActivity";

    // UI
    private EditText    moodInput;
    private Spinner     genreSpinner;
    private EditText    trackInput;
    private Button      analyzeButton;
    private ProgressBar progressBar;

    // Firebase
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mood_input);

        mAuth = FirebaseAuth.getInstance();

        moodInput     = findViewById(R.id.moodInput);
        genreSpinner  = findViewById(R.id.genreSpinner);
        trackInput    = findViewById(R.id.trackInput);
        analyzeButton = findViewById(R.id.analyzeButton);
        progressBar   = findViewById(R.id.loadingBar);

        analyzeButton.setOnClickListener(v -> startMoodAnalysis());
    }

    // ── Task 2: Mood → AI Keywords → YouTube ─────────────────────────────────

    private void startMoodAnalysis() {
        String rawMood  = moodInput.getText().toString().trim();
        String genre    = genreSpinner.getSelectedItem().toString();
        String trackName = trackInput.getText().toString().trim();

        if (rawMood.isEmpty()) {
            Toast.makeText(this, "Please describe how you're feeling", Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);

        // Step 1: Send free-text mood to Gemini via MoodAIHelper (AsyncTask)
        new MoodAIHelper(BuildConfig.Test_Key, new MoodAIHelper.Callback() {

            @Override
            public void onKeywordsReady(List<String> keywords) {
                // keywords e.g. ["melancholic", "indie folk", "acoustic", "rainy"]
                Log.d(TAG, "AI keywords: " + keywords);

                // Step 2: Build a rich YouTube search query from the keywords + genre
                String keywordQuery = String.join(" ", keywords) + " " + genre;

                // Step 3: Save mood entry and search YouTube (background thread)
                String uid = mAuth.getCurrentUser() != null
                        ? mAuth.getCurrentUser().getUid() : "";

                searchYouTubeAndNavigate(rawMood, genre, trackName, keywordQuery, uid);
            }

            @Override
            public void onError(String errorMessage) {
                Log.e(TAG, "AI keyword extraction failed: " + errorMessage);
                Toast.makeText(MoodInputActivity.this,
                        "AI unavailable, using mood directly: " + rawMood,
                        Toast.LENGTH_SHORT).show();

                // Graceful fallback: search with the raw mood text
                String uid = mAuth.getCurrentUser() != null
                        ? mAuth.getCurrentUser().getUid() : "";
                searchYouTubeAndNavigate(rawMood, genre, trackName, rawMood + " " + genre, uid);
            }

        }).execute(rawMood);  // AsyncTask.execute() kicks off doInBackground
    }

    // ── Background: Firebase save + YouTube search ────────────────────────────

    private void searchYouTubeAndNavigate(String mood, String genre, String trackName,
                                          String youtubeQuery, String uid) {
        new Thread(() -> {
            try {
                // Persist mood entry
                if (!uid.isEmpty()) {
                    InputProcessor.processMoodInput(
                            uid, mood, genre, trackName, System.currentTimeMillis());
                    Log.d(TAG, "✅ Mood saved to Firebase");
                }

                // YouTube search using the AI-enhanced query
                YouTubeDataSource youtube = YouTubeDataSource.getInstance();
                List<Track> tracks = youtube.searchTracks(youtubeQuery, 10);
                Log.d(TAG, "✅ Found " + tracks.size() + " tracks for query: " + youtubeQuery);

                runOnUiThread(() -> {
                    setLoading(false);

                    if (tracks.isEmpty()) {
                        Toast.makeText(this,
                                "No tracks found – try a different mood description",
                                Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this,
                                "Found " + tracks.size() + " tracks!", Toast.LENGTH_SHORT).show();

                        Intent intent = new Intent(MoodInputActivity.this, ResultsActivity.class);
                        intent.putExtra("mood",  mood);
                        intent.putExtra("genre", genre);
                        startActivity(intent);

                        moodInput.setText("");
                        trackInput.setText("");
                    }
                });

            } catch (Exception e) {
                Log.e(TAG, "Error in searchYouTubeAndNavigate", e);
                runOnUiThread(() -> {
                    setLoading(false);
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        analyzeButton.setEnabled(!loading);
    }
}