package com.example.moodic;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {

    private WebView youtubeWebView;
    private Button btnLogout;
    private TextView tvUserEmail;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        youtubeWebView = findViewById(R.id.youtubeWebView);
        btnLogout = findViewById(R.id.btnLogout);
        tvUserEmail = findViewById(R.id.tvUserEmail);

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if(currentUser != null){
            tvUserEmail.setText("Logged in as: " + currentUser.getEmail());
        }

        // Load a default YouTube video
        WebSettings webSettings = youtubeWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        youtubeWebView.loadUrl("https://www.youtube.com/embed/dQw4w9WgXcQ"); // example video

        btnLogout.setOnClickListener(v -> {
            mAuth.signOut();
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            finish();
        });
    }
}
