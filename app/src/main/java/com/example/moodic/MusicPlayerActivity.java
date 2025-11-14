package com.example.moodic;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class MusicPlayerActivity extends AppCompatActivity {

    private WebView youtubeWebView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_music_player);

        youtubeWebView = findViewById(R.id.youtubeWebView);

        WebSettings webSettings = youtubeWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);

        youtubeWebView.setWebViewClient(new WebViewClient());

        String mood = getIntent().getStringExtra("mood");
        String videoUrl = getVideoUrlForMood(mood);

        youtubeWebView.loadUrl(videoUrl);
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
}
