package com.example.moodic.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.example.moodic.R;

public class MusicPlayerActivity extends AppCompatActivity {

    private WebView youtubeWebView;
    private MusicInterruptionReceiver interruptionReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_music_player);

        youtubeWebView = findViewById(R.id.youtubeWebView);

        WebSettings webSettings = youtubeWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        youtubeWebView.setWebViewClient(new WebViewClient());

        String videoUrl = getIntent().getStringExtra("videoUrl");

        if (videoUrl == null || videoUrl.isEmpty()) {
            // Fall back to mood-based URL for backwards compatibility
            String mood = getIntent().getStringExtra("mood");
            videoUrl = getVideoUrlForMood(mood);
        } else {
            // Convert watch URL to embed URL if needed
            videoUrl = toEmbedUrl(videoUrl);
        }

        youtubeWebView.loadUrl(videoUrl);
        registerInterruptionReceiver();
    }

    private void registerInterruptionReceiver() {
        interruptionReceiver = new MusicInterruptionReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY); // Headset unplugged
        filter.addAction(TelephonyManager.ACTION_PHONE_STATE_CHANGED); // Phone call
        registerReceiver(interruptionReceiver, filter);
    }

    private void pauseMusic() {
        if (youtubeWebView != null) {
            // This is a common way to pause YouTube in a WebView
            youtubeWebView.loadUrl("javascript:var video = document.getElementsByTagName('video')[0]; if (video) video.pause();");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (interruptionReceiver != null) {
            unregisterReceiver(interruptionReceiver);
        }
    }

    private String toEmbedUrl(String url) {
        // Already an embed URL
        if (url.contains("youtube.com/embed/")) return url;

        // Convert https://www.youtube.com/watch?v=VIDEO_ID to embed
        if (url.contains("watch?v=")) {
            String videoId = url.substring(url.indexOf("watch?v=") + 8);
            // Strip any extra params
            if (videoId.contains("&")) {
                videoId = videoId.substring(0, videoId.indexOf("&"));
            }
            return "https://www.youtube.com/embed/" + videoId;
        }

        return url;
    }

    private String getVideoUrlForMood(String mood){
        if(mood == null) mood = "default";

        switch(mood.toLowerCase()){
            case "happy": return "https://www.youtube.com/embed/d-diB65scQU";
            case "relaxed": return "https://www.youtube.com/embed/2Vv-BfVoq4g";
            case "sad": return "https://www.youtube.com/embed/hLQl3WQQoQ0";
            default: return "https://www.youtube.com/embed/3JZ4pnNtyxQ"; // default mood
        }
    }

    private class MusicInterruptionReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(action)) {
                // Headset unplugged
                pauseMusic();
            } else if (TelephonyManager.ACTION_PHONE_STATE_CHANGED.equals(action)) {
                // Phone call state changed
                String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
                if (TelephonyManager.EXTRA_STATE_RINGING.equals(state) || TelephonyManager.EXTRA_STATE_OFFHOOK.equals(state)) {
                    pauseMusic();
                }
            }
        }
    }
}
