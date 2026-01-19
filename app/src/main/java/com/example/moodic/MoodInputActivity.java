package com.example.moodic;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.moodic.data.FirebaseManager;
import com.google.firebase.auth.FirebaseAuth;

public class MoodInputActivity extends AppCompatActivity {
    private EditText moodInput;
    private Spinner genreSpinner;
    private EditText trackInput;
    private Button analyzeButton;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mood_input);
        mAuth = FirebaseAuth.getInstance();

        EditText moodInput = findViewById(R.id.moodInput);
        Button analyzeButton = findViewById(R.id.analyzeButton);
        moodInput = findViewById(R.id.moodInput);
        genreSpinner = findViewById(R.id.genreSpinner);
        trackInput = findViewById(R.id.trackInput);
        analyzeButton = findViewById(R.id.analyzeButton);

        analyzeButton.setOnClickListener(v -> saveMoodEntry());

    }
    private void saveMoodEntry() {
        String mood = moodInput.getText().toString().trim();
        String genre = genreSpinner.getSelectedItem().toString();
        String trackName = trackInput.getText().toString().trim();

        // Validate input
        if (mood.isEmpty()) {
            Toast.makeText(this, "Please enter a mood", Toast.LENGTH_SHORT).show();
            return;
        }

        // Get current user UID
        String uid = mAuth.getCurrentUser().getUid();

        // Process and save mood entry
        InputProcessor.processMoodInput(uid, mood, genre, trackName, System.currentTimeMillis());

        // Show success message
        Toast.makeText(this, "Mood saved successfully!", Toast.LENGTH_SHORT).show();

        // Clear inputs
        moodInput.setText("");
        trackInput.setText("");
    }

}
