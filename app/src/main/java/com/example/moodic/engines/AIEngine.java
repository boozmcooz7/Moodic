package com.example.moodic.engines;

import android.util.Log;

import com.google.firebase.ai.FirebaseAI;
import com.google.firebase.ai.java.GenerativeModelFutures;
import com.google.firebase.ai.type.Content;
import com.google.firebase.ai.type.GenerateContentResponse;
import com.google.firebase.ai.type.GenerationConfig;
import com.google.firebase.ai.type.GenerativeBackend;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import org.json.JSONObject;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class AIEngine {
    private static final String TAG = "AIEngine";
    private static AIEngine instance;
    private final GenerativeModelFutures modelFutures;
    private final Executor executor = Executors.newSingleThreadExecutor();

    private AIEngine() {
        GenerationConfig config = new GenerationConfig.Builder()
                .setTemperature(0.7f)
                .setResponseMimeType("application/json")
                .build();

        // 2. Initialize using the Provider Pattern (Satisfies: GenerativeModelProvider)
        // This looks up the model via the "Google AI" backend inside Firebase
        this.modelFutures = GenerativeModelFutures.from(
                FirebaseAI.getInstance(GenerativeBackend.googleAI())
                        .generativeModel("gemini-2.5-flash-lite", config)
        );
    }

    public static synchronized AIEngine getInstance() {
        if (instance == null) {
            instance = new AIEngine();
        }
        return instance;
    }

    /**
     * Synchronous version of mood analysis. 
     * CAUTION: Must be called from a background thread as it blocks until the AI responds.
     */
    public MusicVector analyzeMoodToVector(String moodInput) throws Exception {
        String prompt = "Analyze the mood: '" + moodInput + "'. " +
                "Return ONLY a JSON object with these keys: " +
                "energy, tempo, valence, danceability, acousticness. " +
                "Values must be between 0.0 and 1.0.";

        Content content = new Content.Builder()
                .addText(prompt)
                .build();

        ListenableFuture<GenerateContentResponse> future = modelFutures.generateContent(content);
        // Blocks and waits for result
        GenerateContentResponse response = future.get();
        String resultText = response.getText();
        Log.d(TAG, "AI Sync Raw Response: " + resultText);
        return parseJsonToVector(resultText);
    }

    public void analyzeMood(String moodInput, MusicVectorCallback callback) {
        // 4. Detailed Prompt to ensure the AI doesn't hallucinate
        String prompt = "Analyze the mood: '" + moodInput + "'. " +
                "Return ONLY a JSON object with these keys: " +
                "energy, tempo, valence, danceability, acousticness. " +
                "Values must be between 0.0 and 1.0.";

        Content content = new Content.Builder()
                .addText(prompt)
                .build();

        ListenableFuture<GenerateContentResponse> future = modelFutures.generateContent(content);

        Futures.addCallback(future, new FutureCallback<GenerateContentResponse>() {
            @Override
            public void onSuccess(GenerateContentResponse response) {
                try {
                    String resultText = response.getText();
                    Log.d(TAG, "AI Raw Response: " + resultText);

                    // FIX: We must parse the string into the MusicVector object!
                    MusicVector vector = parseJsonToVector(resultText);
                    callback.onSuccess(vector);
                } catch (Exception e) {
                    Log.e(TAG, "JSON Parsing Error", e);
                    callback.onFailure(e);
                }
            }

            @Override
            public void onFailure(Throwable t) {
                Log.e(TAG, "AI API Error: " + t.getMessage());
                callback.onFailure(t);
            }
        }, executor);
    }

    // NEW: Helper to turn the AI string into your Data Object
    private MusicVector parseJsonToVector(String json) throws Exception {
        String cleanJson = json.replaceAll("```json", "")
                .replaceAll("```", "")
                .trim();
        JSONObject obj = new JSONObject(cleanJson);
        MusicVector v = new MusicVector();
        v.energy = obj.optDouble("energy", 0.5);
        v.tempo = obj.optDouble("tempo", 0.5);
        v.valence = obj.optDouble("valence", 0.5);
        v.danceability = obj.optDouble("danceability", 0.5);
        v.acousticness = obj.optDouble("acousticness", 0.5);
        return v;
    }

    public interface MusicVectorCallback {
        void onSuccess(MusicVector vector);
        void onFailure(Throwable t);
    }

    public static class MusicVector implements Serializable {
        public double energy, tempo, valence, danceability, acousticness;

        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("energy", energy);
            map.put("tempo", tempo);
            map.put("valence", valence);
            map.put("danceability", danceability);
            map.put("acousticness", acousticness);
            return map;
        }
    }
}