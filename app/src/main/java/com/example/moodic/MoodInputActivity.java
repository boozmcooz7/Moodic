package com.example.moodic;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class MoodInputActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mood_input);

        EditText moodInput = findViewById(R.id.moodInput);
        Button analyzeButton = findViewById(R.id.analyzeButton);

        analyzeButton.setOnClickListener(v -> {
            String text = moodInput.getText().toString();
            // TODO: Replace with actual Gemini API call
            String fakeAnalysis = "Happy"; // placeholder
            Log.d("Gemini", "Analysis result: " + fakeAnalysis);
            Toast.makeText(this, "Mood: " + fakeAnalysis, Toast.LENGTH_SHORT).show();
        });
    }
}
