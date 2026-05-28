package com.example.moodic.activities;

import android.content.Context;
import android.media.AudioManager;
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
    private YouTubePlayer activePlayer; // Member variable to handle audio focus
    private AudioManager audioManager;
    private AudioManager.OnAudioFocusChangeListener focusChangeListener;

    private void setupAudioFocus() {
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        focusChangeListener = focusChange -> {
            if (activePlayer == null) return;
            switch (focusChange) {
                case AudioManager.AUDIOFOCUS_LOSS:
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                    // Another app started (Phone call, Spotify, etc.) -> Pause music
                    activePlayer.pause();
                    break;
                case AudioManager.AUDIOFOCUS_GAIN:
                    // You got the focus back -> Resume music
                    activePlayer.play();
                    break;
            }
        };
    }

    private boolean requestAudioFocus() {
        if (audioManager == null) return false;
        int result = audioManager.requestAudioFocus(focusChangeListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN);
        return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
    }

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

        // Initialize Audio Focus
        setupAudioFocus();
        requestAudioFocus();

        // 3. Register the player view with the Activity lifecycle.
        getLifecycle().addObserver(youTubePlayerView);

        // 4. Attach a listener; start playback as soon as the player is ready.
        youTubePlayerView.addYouTubePlayerListener(new AbstractYouTubePlayerListener() {

            @Override
            public void onReady(@NonNull YouTubePlayer youTubePlayer) {
                activePlayer = youTubePlayer;
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
    protected void onStop() {
        super.onStop();
        // Release audio focus when activity is no longer visible
        if (audioManager != null && focusChangeListener != null) {
            audioManager.abandonAudioFocus(focusChangeListener);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (youTubePlayerView != null) {
            youTubePlayerView.release();
        }
    }
}
