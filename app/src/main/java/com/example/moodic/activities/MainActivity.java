package com.example.moodic.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.moodic.LoginActivity;
import com.example.moodic.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    private TextView welcomeText;
    private Button moodButton;
    private Button youtubeButton;
    private Button favoritesButton;
    private Button logoutButton;

    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        // Check if user is logged in
        if (currentUser == null) {
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            finish();
            return;
        }

        // Initialize UI elements
        welcomeText = findViewById(R.id.welcomeText);
        moodButton = findViewById(R.id.moodButton);
        youtubeButton = findViewById(R.id.youtubeButton);
        favoritesButton = findViewById(R.id.favoritesButton);
        logoutButton = findViewById(R.id.logoutButton);

        // Display welcome message
        String userEmail = currentUser.getEmail();
        welcomeText.setText("Welcome, " + userEmail + "!");

        // Set up button listeners
        setupButtonListeners();
    }

    private void setupButtonListeners() {
        // Navigate to Mood Input
        moodButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, MoodInputActivity.class);
            startActivity(intent);
        });

        // Navigate to YouTube Search
        youtubeButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, YouTubeSearchActivity.class);
            startActivity(intent);
        });

        // Navigate to Favorites (TODO: Create this activity)
        favoritesButton.setOnClickListener(v -> {
            Toast.makeText(this, "Favorites feature coming soon!", Toast.LENGTH_SHORT).show();
            // Intent intent = new Intent(MainActivity.this, FavoritesActivity.class);
            // startActivity(intent);
        });

        // Logout
        logoutButton.setOnClickListener(v -> {
            mAuth.signOut();
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            finish();
            Toast.makeText(MainActivity.this, "Logged out successfully", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Check if user is still logged in
        currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            finish();
        }
    }
}