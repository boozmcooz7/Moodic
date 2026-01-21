package com.example.moodic;

import android.util.Log;

import com.example.moodic.ai.AIEngine;
import com.example.moodic.data.FirebaseManager;

import java.util.HashMap;
import java.util.Map;

public class InputProcessor {
    private static final String TAG = "InputProcessor";

    /**
     * Process mood input from UI, analyze with Gemini, and save to Firebase
     */
    public static void processMoodInput(String uid, String mood, String genre, String trackName, long timestamp, ProcessorCallback callback) {
        Log.d(TAG, "Processing mood input: " + mood);

        // Step 1: Analyze mood with AIEngine (uses Gemini)
        AIEngine.getInstance().analyzeMoodToVector(mood)
                .thenAccept(musicVector -> {
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

                    // Step 4: Callback success
                    if (callback != null) {
                        callback.onSuccess(musicVector);
                    }
                })
                .exceptionally(e -> {
                    Log.e(TAG, "❌ Error processing mood", e);
                    if (callback != null) {
                        callback.onError(e.getMessage());
                    }
                    return null;
                });
    }

    /**
     * Validate mood input
     */
    static boolean isValidMoodInput(String mood) {
        return mood != null && !mood.trim().isEmpty();
    }

    // Legacy methods (kept for compatibility)
    static String processInput(String rawInput, String inputType) {
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

    void onSuccess(AIEngine.MusicVector vector);
    void onError(String errorMessage);
}

