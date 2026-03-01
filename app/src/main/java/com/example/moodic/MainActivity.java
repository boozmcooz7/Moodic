package com.example.moodic;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;


import com.example.moodic.activities.MoodInputActivity;
import com.example.moodic.activities.YouTubeSearchActivity;
import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.HashMap;
import java.util.Map;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    private TextView welcomeText;
    private Button moodButton, youtubeButton, favoritesButton, logoutButton;

    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private GenerativeModelFutures geminiModel;

    // Custom Engines
    private AIEngine aiEngine;
    private SpeechToTextEngine speechToTextEngine;
    private TextToSpeechEngine textToSpeechEngine;
    private NotificationManager notificationManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 1. Initialize Firebase & Gemini
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        // 2. Security Check (Move this up so we don't init engines if not logged in)
        if (currentUser == null) {
            navigateToLogin();
            return;
        }

        GenerativeModel gm = new GenerativeModel(
                "gemini-1.5-flash",
                BuildConfig.Gemini_API_KEY
        );
        geminiModel = GenerativeModelFutures.from(gm);

        // 3. Initialize Custom Engines
        aiEngine = AIEngine.getInstance();
        speechToTextEngine = new SpeechToTextEngine(this);
        textToSpeechEngine = new TextToSpeechEngine(this);
        notificationManager = new NotificationManager(this);

        // 4. Setup UI & Listeners
        initUI();
        setupButtonListeners();

        // Start-up tasks
//        notificationManager.scheduleDailyReminder(9, 0);
        Log.d(TAG, "✅ All engines initialized");
    }

    private void initUI() {
        welcomeText = findViewById(R.id.welcomeText);
        moodButton = findViewById(R.id.moodButton);
        youtubeButton = findViewById(R.id.youtubeButton);
        favoritesButton = findViewById(R.id.favoritesButton);
        logoutButton = findViewById(R.id.logoutButton);

        if (currentUser != null && currentUser.getEmail() != null) {
            welcomeText.setText("Welcome, " + currentUser.getEmail() + "!");
        }
    }

    private void analyzeMoodWithGemini(String moodInput) {
        updateUI("Gemini is thinking...");

        String prompt = "The user is feeling: " + moodInput +
                ". Suggest ONE specific song (Title and Artist) that matches this mood.";

        Content content = new Content.Builder().addText(prompt).build();
        ListenableFuture<GenerateContentResponse> response = geminiModel.generateContent(content);

        Futures.addCallback(response, new FutureCallback<GenerateContentResponse>() {
            @Override
            public void onSuccess(GenerateContentResponse result) {
                String aiResponse = result.getText();
                updateUI("AI Suggests: " + aiResponse);
                // Also process the technical vector for the app logic
                analyzeAndPlayMusic(moodInput);
            }

            @Override
            public void onFailure(Throwable t) {
                Log.e(TAG, "Gemini Error: " + t.getMessage());
                updateUI("AI Error. Using local analysis.");
                analyzeAndPlayMusic(moodInput);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void setupButtonListeners() {
        moodButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, MoodInputActivity.class);
            startActivity(intent);
        });

        youtubeButton.setOnClickListener(v -> {
            startActivity(new Intent(this, YouTubeSearchActivity.class));
        });

        favoritesButton.setOnClickListener(v ->
                Toast.makeText(this, "Favorites coming soon!", Toast.LENGTH_SHORT).show());

        logoutButton.setOnClickListener(v -> {
            mAuth.signOut();
            navigateToLogin();
            Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
        });
    }

    private void navigateToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void updateUI(String message) {
        runOnUiThread(() -> {
            if (welcomeText != null) welcomeText.setText(message);
        });
    }

    private void analyzeAndPlayMusic(String moodInput) {
        aiEngine.analyzeMoodToVector(moodInput, new AIEngine.MusicVectorCallback() {
            @Override
            public void onSuccess(AIEngine.MusicVector vector) {
                runOnUiThread(() -> {
                    notificationManager.showMoodResultNotification(vector);
                    textToSpeechEngine.speakMoodAnalysis(vector.getMood());
                    saveAnalysisToFirestore(moodInput, vector);

                    Intent intent = new Intent(MainActivity.this, YouTubeSearchActivity.class);
                    intent.putExtra("SEARCH_QUERY", moodInput);
                    startActivity(intent);
                });
            }
            @Override
            public void onFailure(Throwable t) {
                Log.e(TAG, "Analysis failed: " + t.getMessage());
                updateUI("Error analyzing mood.");
            }
        });
    }

    private void saveAnalysisToFirestore(String mood, AIEngine.MusicVector vector) {
        if (currentUser == null) return;
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Map<String, Object> analysis = new HashMap<>();
        analysis.put("moodInput", mood);
        analysis.put("timestamp", System.currentTimeMillis());
        analysis.put("vector", vector.toMap()); // Use toMap for better Firestore structure

        db.collection("users").document(currentUser.getUid())
                .collection("history").add(analysis)
                .addOnFailureListener(e -> Log.e(TAG, "Firestore error", e));
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mAuth.getCurrentUser() == null) navigateToLogin();
    }
}