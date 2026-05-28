package com.example.moodic.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.moodic.BuildConfig;
import com.example.moodic.engines.InputProcessor;
import com.example.moodic.R;
import com.example.moodic.engines.SpeechToTextEngine;
import com.google.firebase.auth.FirebaseAuth;

public class MoodInputActivity extends AppCompatActivity {

    private static final String TAG = "MoodInputActivity";
    private static final int MIC_PERMISSION_CODE = 300;

    // UI Elements
    private EditText    moodInput;
    private Spinner     genreSpinner;
    private Button      analyzeButton;
    private Button      btnHappy, btnSad, btnCalm, btnAngry, btnEnergetic;
    private ImageButton voiceInputButton;
    private ProgressBar progressBar;

    // Frameworks
    private FirebaseAuth mAuth;
    private SpeechToTextEngine speechEngine;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mood_input);

        mAuth = FirebaseAuth.getInstance();

        // קישור משתנים לאלמנטים הגרפיים מה-XML החדש
        moodInput        = findViewById(R.id.moodInput);
        genreSpinner     = findViewById(R.id.genreSpinner);
        analyzeButton    = findViewById(R.id.analyzeButton);
        progressBar      = findViewById(R.id.loadingBar);
        btnHappy         = findViewById(R.id.btnHappy);
        btnSad           = findViewById(R.id.btnSad);
        btnCalm          = findViewById(R.id.btnCalm);
        btnAngry         = findViewById(R.id.btnAngry);
        btnEnergetic     = findViewById(R.id.btnEnergetic);
        voiceInputButton = findViewById(R.id.voiceInputButton);

        // הגדרת האזנה לכפתורי הרגש המהירים
        if (btnHappy != null) btnHappy.setOnClickListener(v -> moodInput.setText("I am feeling incredibly happy, energetic and full of life"));
        if (btnSad != null) btnSad.setOnClickListener(v -> moodInput.setText("I am feeling lonely, down and wish I had more friends around me"));
        if (btnCalm != null) btnCalm.setOnClickListener(v -> moodInput.setText("I am looking for peaceful, tranquil music to help me relax and unwind after a quiet day"));
        if (btnAngry != null) btnAngry.setOnClickListener(v -> moodInput.setText("I feel completely overwhelmed with frustration and anger, looking for intense and heavy sounds"));
        if (btnEnergetic != null) btnEnergetic.setOnClickListener(v -> moodInput.setText("I am feeling incredibly hyped up, motivated, ecstatic and ready to take on the world"));

        // אתחול מנוע הדיבור לטקסט וכפיית הדפסה על ה-UI Thread בזמן אמת
        speechEngine = new SpeechToTextEngine(this);
        speechEngine.setOnResultListener(text -> {
            runOnUiThread(() -> {
                if (moodInput != null && text != null && !text.isEmpty()) {
                    moodInput.setText(text);
                    Log.d(TAG, "Speech matched and typed: " + text);
                }
            });
        });

        // לחיצה על אייקון המיקרופון הצמוד
        if (voiceInputButton != null) {
            voiceInputButton.setOnClickListener(v -> {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                    startListeningVoice();
                } else {
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, MIC_PERMISSION_CODE);
                }
            });
        }

        if (analyzeButton != null) {
            analyzeButton.setOnClickListener(v -> startMoodAnalysis());
        }
    }

    private void startListeningVoice() {
        Toast.makeText(this, "Listening... Speak now", Toast.LENGTH_SHORT).show();
        if (speechEngine != null) {
            speechEngine.startListening("en-US");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == MIC_PERMISSION_CODE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startListeningVoice();
        } else {
            Toast.makeText(this, "Microphone permission required", Toast.LENGTH_SHORT).show();
        }
    }

    private void startMoodAnalysis() {
        String rawMood = moodInput.getText().toString().trim();
        String genre   = genreSpinner.getSelectedItem().toString();

        if (rawMood.isEmpty()) {
            Toast.makeText(this, "Please describe how you're feeling", Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);

        new Thread(() -> {
            try {
                // שמירה מאובטחת של מצב הרוח וההיסטוריה לענן של פיירבייס
                String uid = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : "";
                if (!uid.isEmpty()) {
                    InputProcessor.processMoodInput(uid, rawMood, genre, "", System.currentTimeMillis());
                }

                // מעבר מיידי ומאובטח למסך התוצאות ללא קריסות ומסכים לבנים
                runOnUiThread(() -> {
                    setLoading(false);
                    Intent intent = new Intent(MoodInputActivity.this, ResultsActivity.class);
                    intent.putExtra("mood", rawMood);
                    intent.putExtra("genre", genre);
                    startActivity(intent);
                    moodInput.setText("");
                });

            } catch (Exception e) {
                Log.e(TAG, "Analysis failed to trigger", e);
                runOnUiThread(() -> setLoading(false));
            }
        }).start();
    }

    private void setLoading(boolean loading) {
        if (progressBar != null) progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        if (analyzeButton != null) analyzeButton.setEnabled(!loading);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (speechEngine != null) speechEngine.release();
    }
}