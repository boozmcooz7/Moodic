package com.example.moodic;

import android.util.Log;
import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.json.JSONObject;
import java.util.HashMap;
import java.util.Map;

public class AIEngine {
    private static final String TAG = "AIEngine";
    private static AIEngine instance;
    private GenerativeModelFutures model;

    private AIEngine() {
        try {
            GenerativeModel gm = new GenerativeModel(
                    "gemini-1.5-flash",
                    BuildConfig.Gemini_API_KEY
            );
            this.model = GenerativeModelFutures.from(gm);
            Log.d(TAG, "✅ Gemini AI initialized");
        } catch (Exception e) {
            Log.e(TAG, "❌ Initialization error", e);
        }
    }

    public static AIEngine getInstance() {
        if (instance == null) {
            instance = new AIEngine();
        }
        return instance;
    }

    public void analyzeMoodToVector(String moodInput, MusicVectorCallback callback) {
        if (moodInput == null || moodInput.trim().isEmpty()) {
            callback.onSuccess(createNeutralVector());
            return;
        }

        try {
            String prompt = "Analyze mood: '" + moodInput + "'. Return ONLY JSON: " +
                    "{\"energy\":0.5, \"tempo\":0.5, \"valence\":0.5, \"danceability\":0.5, \"acousticness\":0.5}";

            Content content = new Content.Builder().addText(prompt).build();
            ListenableFuture<GenerateContentResponse> future = model.generateContent(content);

            Futures.addCallback(future, new FutureCallback<GenerateContentResponse>() {
                @Override
                public void onSuccess(GenerateContentResponse response) {
                    try {
                        String text = response.getText();
                        callback.onSuccess(parseMusicVector(text, moodInput));
                    } catch (Exception e) {
                        callback.onSuccess(mapMoodToVector(moodInput));
                    }
                }

                @Override
                public void onFailure(Throwable t) {
                    callback.onFailure(t);
                }
            }, Runnable::run);
        } catch (Exception e) {
            callback.onSuccess(mapMoodToVector(moodInput));
        }
    }

    private MusicVector parseMusicVector(String jsonResponse, String moodInput) {
        try {
            String clean = jsonResponse.replace("```json", "").replace("```", "").trim();
            JSONObject obj = new JSONObject(clean);
            MusicVector v = new MusicVector();
            v.energy = obj.optDouble("energy", 0.5);
            v.tempo = obj.optDouble("tempo", 0.5);
            v.valence = obj.optDouble("valence", 0.5);
            v.danceability = obj.optDouble("danceability", 0.5);
            v.acousticness = obj.optDouble("acousticness", 0.5);
            return v;
        } catch (Exception e) {
            return mapMoodToVector(moodInput);
        }
    }

    private MusicVector mapMoodToVector(String mood) {
        MusicVector v = new MusicVector();
        String m = mood.toLowerCase();
        if (m.contains("happy")) { v.valence = 0.9; v.energy = 0.8; }
        else if (m.contains("sad")) { v.valence = 0.1; v.energy = 0.2; }
        else { return createNeutralVector(); }
        return v;
    }

    private MusicVector createNeutralVector() {
        MusicVector v = new MusicVector();
        v.energy = 0.5; v.tempo = 0.5; v.valence = 0.5; v.danceability = 0.5; v.acousticness = 0.5;
        return v;
    }

    public interface MusicVectorCallback {
        void onSuccess(MusicVector vector);
        void onFailure(Throwable t);
    }

    public static class MusicVector {
        public double energy, tempo, valence, danceability, acousticness;

        public String getMood() {
            if (valence > 0.6) return "Upbeat and Positive";
            if (valence < 0.4) return "Mellow and Reflective";
            return "Balanced and Calm";
        }

        public Map<String, Double> toMap() {
            Map<String, Double> map = new HashMap<>();
            map.put("energy", energy); map.put("tempo", tempo);
            map.put("valence", valence);
            return map;
        }
    }
}