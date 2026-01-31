package com.example.moodic;

import android.util.Log;

import java.util.HashMap;
import java.util.Map;

public class AIEngine {
    private static final String TAG = "AIEngine";
    private static AIEngine instance;

    private AIEngine() {
    }

    public static AIEngine getInstance() {
        if (instance == null) {
            instance = new AIEngine();
        }
        return instance;
    }

    /**
     * Analyzes mood/emotion and returns a music vector
     * Music vector contains: energy, tempo, valence, danceability, acousticness
     */
    public MusicVector analyzeMoodToVector(String moodInput) {
        Log.d(TAG, "Analyzing mood: " + moodInput);

        String normalizedMood = moodInput.toLowerCase().trim();
        MusicVector vector = mapMoodToVector(normalizedMood);

        Log.d(TAG, "✅ Music vector created: " + vector);
        return vector;
    }

    /**
     * Maps specific moods to music vector values
     */
    private MusicVector mapMoodToVector(String mood) {
        MusicVector vector = new MusicVector();

        switch (mood) {
            case "happy":
            case "joyful":
            case "excited":
                vector.energy = 0.9;
                vector.tempo = 0.85;
                vector.valence = 0.95;
                vector.danceability = 0.8;
                vector.acousticness = 0.3;
                break;

            case "sad":
            case "depressed":
            case "melancholic":
                vector.energy = 0.2;
                vector.tempo = 0.3;
                vector.valence = 0.1;
                vector.danceability = 0.2;
                vector.acousticness = 0.8;
                break;

            case "angry":
            case "frustrated":
            case "aggressive":
                vector.energy = 0.95;
                vector.tempo = 0.9;
                vector.valence = 0.2;
                vector.danceability = 0.7;
                vector.acousticness = 0.1;
                break;

            case "calm":
            case "relaxed":
            case "peaceful":
                vector.energy = 0.3;
                vector.tempo = 0.35;
                vector.valence = 0.6;
                vector.danceability = 0.3;
                vector.acousticness = 0.9;
                break;

            case "energetic":
            case "motivated":
            case "pumped":
                vector.energy = 0.85;
                vector.tempo = 0.8;
                vector.valence = 0.75;
                vector.danceability = 0.85;
                vector.acousticness = 0.2;
                break;

            case "romantic":
            case "love":
            case "affectionate":
                vector.energy = 0.5;
                vector.tempo = 0.55;
                vector.valence = 0.8;
                vector.danceability = 0.4;
                vector.acousticness = 0.7;
                break;

            case "neutral":
            case "normal":
            case "fine":
                vector.energy = 0.5;
                vector.tempo = 0.5;
                vector.valence = 0.5;
                vector.danceability = 0.5;
                vector.acousticness = 0.5;
                break;

            case "anxious":
            case "nervous":
            case "stressed":
                vector.energy = 0.7;
                vector.tempo = 0.75;
                vector.valence = 0.3;
                vector.danceability = 0.5;
                vector.acousticness = 0.6;
                break;

            case "focused":
            case "concentrated":
            case "productive":
                vector.energy = 0.6;
                vector.tempo = 0.65;
                vector.valence = 0.55;
                vector.danceability = 0.4;
                vector.acousticness = 0.3;
                break;

            case "tired":
            case "exhausted":
            case "sleepy":
                vector.energy = 0.1;
                vector.tempo = 0.15;
                vector.valence = 0.4;
                vector.danceability = 0.1;
                vector.acousticness = 0.85;
                break;

            default:
                // Default to neutral if mood not recognized
                vector.energy = 0.5;
                vector.tempo = 0.5;
                vector.valence = 0.5;
                vector.danceability = 0.5;
                vector.acousticness = 0.5;
                break;
        }

        return vector;
    }

    /**
     * Music vector class representing audio characteristics
     */
    public static class MusicVector {
        public double energy;        // 0.0 (calm) to 1.0 (intense)
        public double tempo;         // 0.0 (slow) to 1.0 (fast)
        public double valence;       // 0.0 (sad) to 1.0 (happy)
        public double danceability;  // 0.0 (not danceable) to 1.0 (very danceable)
        public double acousticness;  // 0.0 (electronic) to 1.0 (acoustic)

        @Override
        public String toString() {
            return "MusicVector{" +
                    "energy=" + String.format("%.2f", energy) +
                    ", tempo=" + String.format("%.2f", tempo) +
                    ", valence=" + String.format("%.2f", valence) +
                    ", danceability=" + String.format("%.2f", danceability) +
                    ", acousticness=" + String.format("%.2f", acousticness) +
                    '}';
        }

        public Map<String, Double> toMap() {
            Map<String, Double> map = new HashMap<>();
            map.put("energy", energy);
            map.put("tempo", tempo);
            map.put("valence", valence);
            map.put("danceability", danceability);
            map.put("acousticness", acousticness);
            return map;
        }
    }
}