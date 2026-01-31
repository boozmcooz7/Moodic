package com.example.moodic;

import android.util.Log;

import com.example.moodic.AIEngine;
import com.example.moodic.data.FirebaseManager;

import java.util.HashMap;
import java.util.Map;

public class InputProcessor {
    private static final String TAG = "InputProcessor";

    /**
     * Process mood input from UI, analyze with AIEngine, and save to Firebase
     */
    public static void processMoodInput(String uid, String mood, String genre, String trackName, long timestamp) {
        Log.d(TAG, "Processing mood input: " + mood);

        try {
            // Step 1: Analyze mood with AIEngine
            AIEngine.MusicVector musicVector = AIEngine.getInstance().analyzeMoodToVector(mood);
            Log.d(TAG, "✅ Music vector created: " + musicVector);

            // Step 2: Create mood entry data
            Map<String, Object> moodData = new HashMap<>();
            moodData.put("mood", mood);
            moodData.put("genre", genre);
            moodData.put("trackName", trackName);
            moodData.put("timestamp", timestamp);
            moodData.put("musicVector", musicVector.toMap());

            // Step 3: Save to Firebase
            FirebaseManager.getInstance().saveMoodEntry(uid, moodData);
            Log.d(TAG, "✅ Mood entry saved successfully");

        } catch (Exception e) {
            Log.e(TAG, "❌ Error processing mood", e);
        }
    }

    /**
     * Validate mood input
     */
    public static boolean isValidMoodInput(String mood) {
        return mood != null && !mood.trim().isEmpty();
    }

    // Legacy methods (kept for compatibility)
    public static String processInput(String rawInput, String inputType) {
        switch (inputType) {
            case "mood":
                return processMoodInput(rawInput);
            case "genre":
                return processGenreInput(rawInput);
            case "track":
                return processTrackInput(rawInput);
            default:
                return "Invalid input type";
        }
    }

    private static String processMoodInput(String rawInput) {
        switch (rawInput.toLowerCase()) {
            case "happy":
                return "Positive mood detected";
            case "sad":
                return "Negative mood detected";
            case "neutral":
                return "Neutral mood detected";
            default:
                return "Unrecognized mood";
        }
    }

    private static String processGenreInput(String rawInput) {
        if (rawInput.equalsIgnoreCase("pop") || rawInput.equalsIgnoreCase("rock")
                || rawInput.equalsIgnoreCase("jazz") || rawInput.equalsIgnoreCase("hip-hop")) {
            return "Favorite genre: " + rawInput;
        } else {
            return "Unknown genre: " + rawInput;
        }
    }

    private static String processTrackInput(String rawInput) {
        if (rawInput != null && !rawInput.isEmpty()) {
            return "Track added: " + rawInput;
        } else {
            return "Invalid track input";
        }
    }
}