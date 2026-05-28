package com.example.moodic.activities;

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

import com.example.moodic.BuildConfig;
import com.example.moodic.R;
import com.example.moodic.engines.AIEngine;
import com.example.moodic.engines.MoodAIHelper;
import com.example.moodic.data.YouTubeDataSource;
import com.example.moodic.models.Track;

import java.util.ArrayList;
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

        userMood = getIntent().getStringExtra("mood");
        userGenre = getIntent().getStringExtra("genre");

        moodDisplay = findViewById(R.id.moodDisplay);
        vectorDisplay = findViewById(R.id.vectorDisplay);
        tracksRecyclerView = findViewById(R.id.tracksRecyclerView);
        loadingBar = findViewById(R.id.loadingBar);
        retryButton = findViewById(R.id.retryButton);

        tracksRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TrackAdapter(this);
        tracksRecyclerView.setAdapter(adapter);

        if (moodDisplay != null) {
            moodDisplay.setText("Mood: " + userMood + " | Genre: " + userGenre);
        }

        if (retryButton != null) {
            retryButton.setVisibility(View.VISIBLE);
            retryButton.setOnClickListener(v -> finish());
        }

        loadMusicRecommendation();
    }

    private void loadMusicRecommendation() {
        if (loadingBar != null) loadingBar.setVisibility(View.VISIBLE);

        // ניסיון חילוץ ווקטור רגשי חזותי
        AIEngine.getInstance().analyzeMood(userMood, new AIEngine.MusicVectorCallback() {
            @Override
            public void onSuccess(AIEngine.MusicVector vector) {
                runOnUiThread(() -> {
                    if (vectorDisplay != null) {
                        String vectorText = String.format("AI Emotional Vector:\n⚡ Energy: %.2f | 🥁 Tempo: %.2f | 😊 Valence: %.2f",
                                vector.energy, vector.tempo, vector.valence);
                        vectorDisplay.setText(vectorText);
                    }
                });
            }
            @Override
            public void onFailure(Throwable t) {
                Log.e(TAG, "AI Vector calculation skipped");
            }
        });

        // שליפת שיר בודד וממוקד ברקע
        new Thread(() -> {
            List<Track> trackContainer = new ArrayList<>();
            YouTubeDataSource youtube = YouTubeDataSource.getInstance();
            boolean networkError = false;

            try {
                // שלב 1: פנייה ל-Gemini לקבלת רשימת המלצות
                MoodAIHelper aiHelper = new MoodAIHelper(BuildConfig.Test_Key, null);
                List<String> suggestedSongs = aiHelper.doInBackground(userMood);

                // שלב 2: במקום לרוץ בלולאה על 10 שירים, לוקחים רק את השיר הראשון (המוביל) ומחפשים אותו ביוטיוב!
                if (suggestedSongs != null && !suggestedSongs.isEmpty()) {
                    String topSongQuery = suggestedSongs.get(0);
                    List<Track> results = youtube.searchTracks(topSongQuery + " " + userGenre, 1);
                    if (results != null && !results.isEmpty()) {
                        trackContainer.add(results.get(0));
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "AI/YouTube route failed, using offline fallback", e);
                networkError = true;
            }

            // שלב 3: רשת ביטחון מיידית - אם החיפוש נכשל או חסום, שמים שיר בנאלי אחד מוכן מראש
            if (trackContainer.isEmpty()) {
                networkError = true;
                trackContainer.add(getSingleFallbackTrack(userGenre));
            }

            List<Track> finalResult = trackContainer;
            boolean showWarning = networkError;

            // עדכון הממשק
            runOnUiThread(() -> {
                if (loadingBar != null) loadingBar.setVisibility(View.GONE);

                if (showWarning && vectorDisplay != null) {
                    vectorDisplay.setText("⚠️ NOTICE: Google API Limit / Offline Mode.\nLoaded focused local backup song.");
                    vectorDisplay.setTextColor(android.graphics.Color.RED);
                } else if (vectorDisplay != null && vectorDisplay.getText().toString().equals("AI Analysis Loading...")) {
                    vectorDisplay.setText("AI Vector Profile Applied Successfully.");
                    vectorDisplay.setTextColor(android.graphics.Color.GREEN);
                }

                if (adapter != null && !finalResult.isEmpty()) {
                    adapter.setTracks(finalResult);
                    adapter.notifyDataSetChanged();
                }
            });

        }).start();
    }

    // שיר יציב אחד קבוע לכל מצב למניעת מסכים ריקים
    private Track getSingleFallbackTrack(String genre) {
        Track fallbackTrack;
        if (genre.equalsIgnoreCase("Pop") || genre.equalsIgnoreCase("Happy")) {
            fallbackTrack = new Track("Ki1uO3N", "Cruel Summer", "Taylor Swift", "Pop");
        } else {
            fallbackTrack = new Track("ZmE3O_a", "Driver's License", "Olivia Rodrigo", "Sad Pop");
        }
        fallbackTrack.setYoutubeUrl("https://www.youtube.com/watch?v=" + fallbackTrack.getId());
        fallbackTrack.setThumbnailUrl("https://img.youtube.com/vi/" + fallbackTrack.getId() + "/0.jpg");
        return fallbackTrack;
    }
}