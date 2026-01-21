package com.example.moodic;


import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.ai.client.generativeai.type.GenerativeAIException;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class AIEngine {
    private static final String TAG = "AIEngine";
    private static AIEngine instance;
    private GenerativeModel model;

    private AIEngine() {
        // Initialize Gemini model with your API key
        model = new GenerativeModel("gemini-1.5-flash", "YOUR_GEMINI_API_KEY");
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
    public CompletableFuture<MusicVector> analyzeMoodToVector(String moodInput) {
        CompletableFuture<MusicVector> future = new CompletableFuture<>();

        new Thread(() -> {
            try {
                String prompt = buildMoodAnalysisPrompt(moodInput);
                GenerateContentResponse response = model.generateContent(prompt);

                String responseText = response.getText();
                MusicVector vector = parseMusicVector(responseText);

                Log.d(TAG, "✅ Mood analysis successful: " + moodInput);
                future.complete(vector);
            } catch (Exception e) {
                Log.e(TAG, "❌ Error analyzing mood", e);
                future.completeExceptionally(e);
            }
        }).start();

        return future;
    }

    /**
     * Builds a prompt for Gemini to analyze mood and suggest music characteristics
     */
    private String buildMoodAnalysisPrompt(String moodInput) {
        return "Analyze the mood/emotion: '" + moodInput + "'\n\n" +
                "Return ONLY a JSON object with these exact fields (values 0.0-1.0):\n" +
                "{\n" +
                "  \"energy\": <0.0-1.0>,\n" +
                "  \"tempo\": <0.0-1.0>,\n" +
                "  \"valence\": <0.0-1.0>,\n" +
                "  \"danceability\": <0.0-1.0>,\n" +
                "  \"acousticness\": <0.0-1.0>\n" +
                "}\n\n" +
                "Energy: how intense/powerful the music should be\n" +
                "Tempo: how fast the music should be\n" +
                "Valence: how positive/happy the music should be\n" +
                "Danceability: how dance-friendly the music should be\n" +
                "Acousticness: how acoustic vs electronic the music should be\n\n" +
                "Return ONLY the JSON, no other text.";
    }

    /**
     * Parses Gemini's response into a MusicVector object
     */
    private MusicVector parseMusicVector(String jsonResponse) {
        try {
            // Remove any markdown code blocks
            String clean = jsonResponse.replace("```json", "").replace("```", "").trim();

            // Simple JSON parsing (you can use a JSON library for production)
            MusicVector vector = new MusicVector();

            vector.energy = extractJsonValue(clean, "energy");
            vector.tempo = extractJsonValue(clean, "tempo");
            vector.valence = extractJsonValue(clean, "valence");
            vector.danceability = extractJsonValue(clean, "danceability");
            vector.acousticness = extractJsonValue(clean, "acousticness");

            Log.d(TAG, "✅ Music vector parsed: " + vector);
            return vector;
        } catch (Exception e) {
            Log.e(TAG, "❌ Error parsing music vector", e);
            return getDefaultVector(); // Return neutral vector on error
        }
    }

    /**
     * Extracts a double value from JSON string
     */
    private double extractJsonValue(String json, String key) {
        try {
            String searchKey = "\"" + key + "\"";
            int startIndex = json.indexOf(searchKey);
            if (startIndex == -1) return 0.5; // Default neutral value

            int colonIndex = json.indexOf(":", startIndex);
            int commaIndex = json.indexOf(",", colonIndex);
            int braceIndex = json.indexOf("}", colonIndex);

            int endIndex = commaIndex > 0 && commaIndex < braceIndex ? commaIndex : braceIndex;

            String valueStr = json.substring(colonIndex + 1, endIndex).trim();
            return Double.parseDouble(valueStr);
        } catch (Exception e) {
            return 0.5; // Default neutral value
        }
    }

    /**
     * Returns a neutral/default music vector
     */
    private MusicVector getDefaultVector() {
        MusicVector vector = new MusicVector();
        vector.energy = 0.5;
        vector.tempo = 0.5;
        vector.valence = 0.5;
        vector.danceability = 0.5;
        vector.acousticness = 0.5;
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
                    "energy=" + energy +
                    ", tempo=" + tempo +
                    ", valence=" + valence +
                    ", danceability=" + danceability +
                    ", acousticness=" + acousticness +
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