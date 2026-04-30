package com.example.moodic.activities;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.moodic.R;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView;

/**
 * Task 1 – Requirement #8: Embedded YouTube Player
 *
 * Receives a videoId via Intent extra ("videoId") and plays it immediately
 * inside a lifecycle-aware YouTubePlayerView.
 *
 * How to launch from TrackAdapter / ResultsActivity:
 *   Intent intent = new Intent(context, YouTubePlayerActivity.class);
 *   intent.putExtra("videoId", track.getId());      // YouTube video ID only
 *   intent.putExtra("videoTitle", track.getTitle()); // optional display title
 *   context.startActivity(intent);
 */
public class YouTubePlayerActivity extends AppCompatActivity {

        private static final String TAG = "YouTubePlayerActivity";

        // ── UI ──────────────────────────────────────────────────────────────────
        private YouTubePlayerView youTubePlayerView;
        private ProgressBar loadingBar;
        private TextView titleTextView;

        // ── Data ────────────────────────────────────────────────────────────────
        private String videoId;
        private String videoTitle;

        // ── Lifecycle ───────────────────────────────────────────────────────────

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_youtube_player);

            // 1. Read intent extras
            videoId    = getIntent().getStringExtra("videoId");
            videoTitle = getIntent().getStringExtra("videoTitle");

            if (videoId == null || videoId.isEmpty()) {
                Toast.makeText(this, "No video ID provided", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "videoId is null or empty – cannot play");
                finish();
                return;
            }

            // 2. Bind views
            youTubePlayerView = findViewById(R.id.youTubePlayerView);
            loadingBar        = findViewById(R.id.playerLoadingBar);
            titleTextView     = findViewById(R.id.playerTitleText);

            if (videoTitle != null && !videoTitle.isEmpty()) {
                titleTextView.setText(videoTitle);
            } else {
                titleTextView.setVisibility(View.GONE);
            }

            // 3. Register the player view with the Activity lifecycle.
            //    This is mandatory – it handles pause/resume/destroy automatically.
            getLifecycle().addObserver(youTubePlayerView);

            // 4. Attach a listener; start playback as soon as the player is ready.
            youTubePlayerView.addYouTubePlayerListener(new AbstractYouTubePlayerListener() {

                @Override
                public void onReady(@NonNull YouTubePlayer youTubePlayer) {
                    // Hide spinner, start video immediately
                    loadingBar.setVisibility(View.GONE);
                    youTubePlayer.loadVideo(videoId, 0f);
                    Log.d(TAG, "▶ Playing videoId=" + videoId);
                }

                @Override
                public void onError(@NonNull YouTubePlayer youTubePlayer,
                                    @NonNull com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants.PlayerError error) {
                    Log.e(TAG, "Player error: " + error.name());
                    Toast.makeText(YouTubePlayerActivity.this,
                            "Playback error: " + error.name(), Toast.LENGTH_SHORT).show();
                }
            });
        }

        @Override
        protected void onDestroy() {
            super.onDestroy();
            // The LifecycleObserver handles release automatically,
            // but an explicit release here prevents any edge-case leaks.
            if (youTubePlayerView != null) {
                youTubePlayerView.release();
            }
        }
    }