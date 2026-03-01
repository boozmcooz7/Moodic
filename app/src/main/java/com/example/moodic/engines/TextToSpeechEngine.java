package com.example.moodic.engines;

import android.content.Context;
import android.os.Build;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;

import java.util.List;
import java.util.Locale;

/**
 * TextToSpeechEngine: Converts AI analysis results to spoken audio
 * Provides natural language speech output for mood analysis and recommendations
 */
public class TextToSpeechEngine implements TextToSpeech.OnInitListener {

    private static final String TAG = "TextToSpeechEngine";

    private TextToSpeech textToSpeech;
    private Context context;
    private boolean isSpeaking = false;
    private String currentUtterance = "";
    private String errorMessage = "";
    private float speechProgress = 0f;

    private OnCompletionListener onCompletionListener;
    private OnStateChangeListener onStateChangeListener;

    public interface OnCompletionListener {
        void onCompletion();
    }

    public interface OnStateChangeListener {
        void onStateChanged(TextToSpeechState state);
    }

    public TextToSpeechEngine(Context context) {
        this.context = context.getApplicationContext();
        initializeTextToSpeech();
    }

    /**
     * Initialize TextToSpeech engine
     */
    private void initializeTextToSpeech() {
        try {
            textToSpeech = new TextToSpeech(context, this);
            Log.d(TAG, "TextToSpeech initialization started");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing TextToSpeech: " + e.getMessage(), e);
            errorMessage = "Failed to initialize text-to-speech: " + e.getMessage();
        }
    }

    /**
     * Called when TextToSpeech engine is initialized
     */
    @Override
    public void onInit(int status) {
        try {
            if (status == TextToSpeech.SUCCESS) {
                int result = textToSpeech.setLanguage(new Locale("en", "US"));

                if (result == TextToSpeech.LANG_MISSING_DATA ||
                        result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e(TAG, "Language not supported");
                    errorMessage = "Language not supported on this device";
                } else {
                    textToSpeech.setSpeechRate(0.9f);
                    textToSpeech.setPitch(1.0f);
                    textToSpeech.setOnUtteranceProgressListener(new SpeechProgressListener());
                    Log.d(TAG, "TextToSpeech initialized successfully");
                }
            } else {
                Log.e(TAG, "TextToSpeech initialization failed with status: " + status);
                errorMessage = "TextToSpeech initialization failed";
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in onInit: " + e.getMessage(), e);
            errorMessage = "TextToSpeech error: " + e.getMessage();
        }
    }

    /**
     * Speak mood analysis results
     */
    public void speakMoodAnalysis(String analysis) {
        speak(analysis, "mood_analysis");
    }

    /**
     * Speak recommendations
     */
    public void speakRecommendations(List<String> recommendations) {
        StringBuilder sb = new StringBuilder("Here are my recommendations: ");
        for (int i = 0; i < recommendations.size(); i++) {
            sb.append(i + 1).append(". ").append(recommendations.get(i)).append(". ");
        }
        speak(sb.toString(), "recommendations");
    }

    /**
     * Speak affirmation
     */
    public void speakAffirmation(String affirmation) {
        speak(affirmation, "affirmation");
    }

    /**
     * Generic speak method
     */
    public void speak(String text, String utteranceId) {
        try {
            if (textToSpeech == null) {
                errorMessage = "TextToSpeech not initialized";
                Log.e(TAG, "TextToSpeech is not initialized");
                notifyStateChange();
                return;
            }

            if (text.isEmpty()) {
                errorMessage = "Text to speak is empty";
                Log.w(TAG, "Empty text provided to speak");
                notifyStateChange();
                return;
            }

            currentUtterance = text;
            isSpeaking = true;
            errorMessage = "";

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId);
            } else {
                textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null);
            }

            Log.d(TAG, "Speaking: " + text + " (ID: " + utteranceId + ")");
            notifyStateChange();

        } catch (Exception e) {
            Log.e(TAG, "Error speaking text: " + e.getMessage(), e);
            errorMessage = "Error during speech: " + e.getMessage();
            isSpeaking = false;
            notifyStateChange();
        }
    }

    /**
     * Add text to speech queue
     */
    public void queueSpeak(String text, String utteranceId) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                textToSpeech.speak(text, TextToSpeech.QUEUE_ADD, null, utteranceId);
            } else {
                textToSpeech.speak(text, TextToSpeech.QUEUE_ADD, null);
            }
            Log.d(TAG, "Queued speech: " + text);
        } catch (Exception e) {
            Log.e(TAG, "Error queuing speech: " + e.getMessage(), e);
            errorMessage = "Error queuing speech: " + e.getMessage();
            notifyStateChange();
        }
    }

    /**
     * Stop current speech
     */
    public void stop() {
        try {
            if (textToSpeech != null) {
                textToSpeech.stop();
                isSpeaking = false;
                currentUtterance = "";
                speechProgress = 0f;
                Log.d(TAG, "Speech stopped");
                notifyStateChange();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error stopping speech: " + e.getMessage(), e);
        }
    }

    /**
     * Pause speech (only on API 21+)
     */
    public void pause() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                if (textToSpeech != null) {
                    textToSpeech.stop();
                    isSpeaking = false;
                    Log.d(TAG, "Speech paused");
                    notifyStateChange();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error pausing speech: " + e.getMessage(), e);
        }
    }

    /**
     * Set speech rate
     */
    public void setSpeechRate(float rate) {
        try {
            if (textToSpeech != null) {
                rate = Math.max(0.5f, Math.min(2.0f, rate));
                textToSpeech.setSpeechRate(rate);
                Log.d(TAG, "Speech rate set to: " + rate);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error setting speech rate: " + e.getMessage(), e);
        }
    }

    /**
     * Set pitch
     */
    public void setPitch(float pitch) {
        try {
            if (textToSpeech != null) {
                pitch = Math.max(0.5f, Math.min(2.0f, pitch));
                textToSpeech.setPitch(pitch);
                Log.d(TAG, "Pitch set to: " + pitch);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error setting pitch: " + e.getMessage(), e);
        }
    }

    /**
     * Set language
     */
    public void setLanguage(Locale locale) {
        try {
            if (textToSpeech != null) {
                int result = textToSpeech.setLanguage(locale);
                if (result == TextToSpeech.LANG_MISSING_DATA ||
                        result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    errorMessage = "Language not supported: " + locale.getDisplayLanguage();
                    Log.e(TAG, "Language not supported: " + locale.getDisplayLanguage());
                } else {
                    Log.d(TAG, "Language set to: " + locale.getDisplayLanguage());
                }
                notifyStateChange();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error setting language: " + e.getMessage(), e);
        }
    }

    /**
     * Set completion listener
     */
    public void setOnCompletionListener(OnCompletionListener listener) {
        this.onCompletionListener = listener;
    }

    /**
     * Set state change listener
     */
    public void setOnStateChangeListener(OnStateChangeListener listener) {
        this.onStateChangeListener = listener;
    }

    /**
     * Get current state
     */
    public TextToSpeechState getState() {
        return new TextToSpeechState(
                isSpeaking,
                currentUtterance,
                speechProgress,
                errorMessage
        );
    }

    /**
     * Check if TextToSpeech is ready
     */
    public boolean isReady() {
        return textToSpeech != null;
    }

    /**
     * Release resources
     */
    public void release() {
        try {
            if (textToSpeech != null) {
                textToSpeech.stop();
                textToSpeech.shutdown();
                textToSpeech = null;
                Log.d(TAG, "TextToSpeech resources released");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error releasing resources: " + e.getMessage(), e);
        }
    }

    /**
     * Notify listeners of state change
     */
    private void notifyStateChange() {
        if (onStateChangeListener != null) {
            onStateChangeListener.onStateChanged(getState());
        }
    }

    /**
     * Inner class to track speech progress
     */
    private class SpeechProgressListener extends UtteranceProgressListener {
        @Override
        public void onStart(String utteranceId) {
            Log.d(TAG, "Speech started: " + utteranceId);
            isSpeaking = true;
            speechProgress = 0f;
            notifyStateChange();
        }

        @Override
        public void onDone(String utteranceId) {
            Log.d(TAG, "Speech finished: " + utteranceId);
            isSpeaking = false;
            speechProgress = 1f;
            notifyStateChange();

            if (onCompletionListener != null) {
                onCompletionListener.onCompletion();
            }
        }

        @Override
        public void onError(String utteranceId) {
            Log.e(TAG, "Speech error: " + utteranceId);
            isSpeaking = false;
            errorMessage = "Speech error occurred";
            notifyStateChange();
        }
    }

    /**
     * Data class for TextToSpeech state
     */
    public static class TextToSpeechState {
        public boolean isSpeaking;
        public String currentUtterance;
        public float progress;
        public String error;

        public TextToSpeechState(boolean isSpeaking, String currentUtterance,
                                 float progress, String error) {
            this.isSpeaking = isSpeaking;
            this.currentUtterance = currentUtterance;
            this.progress = progress;
            this.error = error;
        }
    }
}