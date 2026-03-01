package com.example.moodic.engines;

import android.util.Log;
import com.example.moodic.data.FirebaseManager;

import java.util.HashMap;
import java.util.Map;

public class InputProcessor {
    private static final String TAG = "InputProcessor";

    public static void processMoodInput(String uid, String mood, String genre, String trackName, long timestamp) {
        Log.d(TAG, "Processing mood input: " + mood);

        AIEngine.getInstance().analyzeMoodToVector(mood, new AIEngine.MusicVectorCallback() {
            @Override
            public void onSuccess(AIEngine.MusicVector vector) {
                Log.d(TAG, "✅ Music vector created: " + vector);

                try {
                    Map<String, Object> moodData = new HashMap<>();
                    moodData.put("mood", mood);
                    moodData.put("genre", genre);
                    moodData.put("trackName", trackName);
                    moodData.put("timestamp", timestamp);
                    moodData.put("musicVector", vector.toMap()); // Fixed variable name

                    FirebaseManager.getInstance().saveMoodEntry(uid, moodData);
                    Log.d(TAG, "✅ Mood entry saved successfully");
                } catch (Exception e) {
                    Log.e(TAG, "❌ Error creating mood data", e);
                }
            }

            @Override
            public void onFailure(Throwable t) {
                Log.e(TAG, "❌ Mood analysis error", t);
            }
        });
    }

    public static boolean isValidMoodInput(String mood) {
        return mood != null && !mood.trim().isEmpty();
    }
}