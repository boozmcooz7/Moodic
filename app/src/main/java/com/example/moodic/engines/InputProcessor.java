package com.example.moodic.engines;

import android.util.Log;

import com.example.moodic.data.FirebaseManager;

import java.util.HashMap;
import java.util.Map;

public class InputProcessor {
    private static final String TAG = "InputProcessor";

    /**
     * Process mood input from UI, analyze with AIEngine, save to Firebase,
     * and update the user's dynamicListeningProfile with the current mood.
     */
    public static void processMoodInput(String uid, String mood, String genre,
                                        String trackName, long timestamp) {
        Log.d(TAG, "Processing mood input: " + mood);

        try {
            // Step 1: Analyze mood with AIEngine to get a MusicVector
            // analyzeMoodToVector is a blocking call, should be called from background thread.
            AIEngine.MusicVector musicVector =
                    AIEngine.getInstance().analyzeMoodToVector(mood);
            Log.d(TAG, "✅ Music vector created: " + musicVector);

            // Step 2: Save mood entry to moodHistory in Firebase
            // This is the history used for the tracking screen and notifications
            Map<String, Object> moodData = new HashMap<>();
            moodData.put("mood", mood);
            moodData.put("genre", genre);
            moodData.put("trackName", trackName);
            moodData.put("timestamp", timestamp);
            moodData.put("musicVector", musicVector.toMap());

            FirebaseManager.getInstance().saveMoodEntry(uid, moodData);
            Log.d(TAG, "✅ Mood entry saved to history");

            // Step 3: Overwrite dynamicListeningProfile with the current mood vector
            // Always reflects the user's current mood
            double[] currentProfile = new double[]{
                    musicVector.energy,
                    musicVector.tempo,
                    musicVector.valence,
                    musicVector.danceability,
                    musicVector.acousticness
            };

            // Call the manager with the double array as expected by updateDynamicProfile(String, double[])
            FirebaseManager.getInstance().updateDynamicProfile(uid, currentProfile);
            Log.d(TAG, "✅ dynamicListeningProfile updated with current mood");

        } catch (Exception e) {
            Log.e(TAG, "❌ Error processing mood", e);
        }
    }
}
