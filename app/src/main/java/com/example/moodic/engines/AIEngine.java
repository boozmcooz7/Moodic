package com.example.moodic.engines;

import android.util.Log;
// CORRECT FIREBASE AI IMPORTS
import com.google.firebase.ai.FirebaseAI;
import com.google.firebase.ai.GenerativeModel;
import com.google.firebase.ai.java.GenerativeModelFutures;
import com.google.firebase.ai.type.Content;
import com.google.firebase.ai.type.GenerateContentResponse;
import com.google.firebase.ai.type.GenerativeBackend;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class AIEngine {
    private static final String TAG = "AIEngine";
    private static AIEngine instance;
    private final GenerativeModelFutures modelFutures;
    private final Executor executor = Executors.newSingleThreadExecutor();

    private AIEngine() {
        // 1. Initialize using the Gemini Developer API backend
        GenerativeModel gm = FirebaseAI.getInstance(GenerativeBackend.googleAI())
                .generativeModel("gemini-2.5-flash-lite"); // Use stable 1.5-flash or 2.5-flash-lite

        // 2. Wrap for Java Compatibility
        this.modelFutures = GenerativeModelFutures.from(gm);
    }

    public static synchronized AIEngine getInstance() {
        if (instance == null) {
            instance = new AIEngine();
        }
        return instance;
    }

    public void analyzeMood(String moodInput, MusicVectorCallback callback) {
        // 3. Create Content using the Firebase AI Type
        Content content = new Content.Builder()
                .addText("Analyze mood: " + moodInput + ". Return JSON: {energy: 0-1, valence: 0-1}")
                .build();

        // 4. Execute the call
        ListenableFuture<GenerateContentResponse> future = modelFutures.generateContent(content);

        Futures.addCallback(future, new FutureCallback<GenerateContentResponse>() {
            @Override
            public void onSuccess(GenerateContentResponse response) {
                String resultText = response.getText();
                Log.d(TAG, "AI Response: " + resultText);
                // Handle your MusicVector parsing here
            }

            @Override
            public void onFailure(Throwable t) {
                Log.e(TAG, "AI Error: " + t.getMessage());
                callback.onFailure(t);
            }
        }, executor);
    }

    public interface MusicVectorCallback {
        void onSuccess(String vectorJson);
        void onFailure(Throwable t);
    }
}