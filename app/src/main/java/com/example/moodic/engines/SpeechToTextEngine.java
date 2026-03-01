package com.example.moodic.engines;

import android.content.Context;
import android.content.Intent;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;

import java.util.ArrayList;

/**
 * SpeechToTextEngine: Captures user voice input for mood description
 * Converts speech to text using Android's SpeechRecognizer
 */
public class SpeechToTextEngine implements RecognitionListener {

    private static final String TAG = "SpeechToTextEngine";

    private SpeechRecognizer speechRecognizer;
    private Context context;
    private boolean isListening = false;
    private String recognizedText = "";
    private String partialResults = "";
    private float volume = 0f;
    private String errorMessage = "";

    private OnResultListener onResultListener;
    private OnStateChangeListener onStateChangeListener;

    public interface OnResultListener {
        void onResult(String text);
    }

    public interface OnStateChangeListener {
        void onStateChanged(SpeechRecognitionState state);
    }

    public SpeechToTextEngine(Context context) {
        this.context = context.getApplicationContext();
        initializeSpeechRecognizer();
    }

    /**
     * Initialize SpeechRecognizer
     */
    private void initializeSpeechRecognizer() {
        try {
            if (SpeechRecognizer.isRecognitionAvailable(context)) {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context);
                speechRecognizer.setRecognitionListener(this);
                Log.d(TAG, "SpeechRecognizer initialized successfully");
            } else {
                Log.e(TAG, "Speech recognition not available on this device");
                errorMessage = "Speech recognition not available";
                notifyStateChange();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error initializing SpeechRecognizer: " + e.getMessage(), e);
            errorMessage = "Failed to initialize speech recognition: " + e.getMessage();
            notifyStateChange();
        }
    }

    /**
     * Start listening for voice input
     */
    public void startListening(String language) {
        try {
            recognizedText = "";
            partialResults = "";
            errorMessage = "";
            volume = 0f;
            isListening = true;
            notifyStateChange();

            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, language);
            intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.getPackageName());
            intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5);
            intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
            intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2000);
            intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 2000);

            speechRecognizer.startListening(intent);
            Log.d(TAG, "Started listening for speech input");

        } catch (Exception e) {
            Log.e(TAG, "Error starting listening: " + e.getMessage(), e);
            errorMessage = "Error starting voice input: " + e.getMessage();
            isListening = false;
            notifyStateChange();
        }
    }

    /**
     * Start listening with default language
     */
    public void startListening() {
        startListening("en-US");
    }

    /**
     * Stop listening for voice input
     */
    public void stopListening() {
        try {
            speechRecognizer.stopListening();
            isListening = false;
            Log.d(TAG, "Stopped listening");
            notifyStateChange();
        } catch (Exception e) {
            Log.e(TAG, "Error stopping listening: " + e.getMessage(), e);
        }
    }

    /**
     * Cancel speech recognition
     */
    public void cancel() {
        try {
            speechRecognizer.cancel();
            isListening = false;
            recognizedText = "";
            partialResults = "";
            volume = 0f;
            Log.d(TAG, "Speech recognition cancelled");
            notifyStateChange();
        } catch (Exception e) {
            Log.e(TAG, "Error cancelling: " + e.getMessage(), e);
        }
    }

    /**
     * Set listener for recognition results
     */
    public void setOnResultListener(OnResultListener listener) {
        this.onResultListener = listener;
    }

    /**
     * Set listener for state changes
     */
    public void setOnStateChangeListener(OnStateChangeListener listener) {
        this.onStateChangeListener = listener;
    }

    @Override
    public void onReadyForSpeech(android.os.Bundle params) {
        Log.d(TAG, "Ready for speech");
        errorMessage = "";
        notifyStateChange();
    }

    @Override
    public void onBeginningOfSpeech() {
        Log.d(TAG, "Beginning of speech detected");
    }

    @Override
    public void onRmsChanged(float rmsdB) {
        // Map dB levels to 0-1 scale
        volume = Math.max(0, Math.min(1, (rmsdB + 10) / 20f));
        notifyStateChange();
    }

    @Override
    public void onBufferReceived(byte[] buffer) {
        // Optional: process audio buffer
    }

    @Override
    public void onEndOfSpeech() {
        Log.d(TAG, "End of speech detected");
    }

    @Override
    public void onError(int error) {
        isListening = false;

        String errorMsg;
        switch (error) {
            case SpeechRecognizer.ERROR_AUDIO:
                errorMsg = "Audio recording error";
                break;
            case SpeechRecognizer.ERROR_CLIENT:
                errorMsg = "Client side error";
                break;
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                errorMsg = "Insufficient permissions";
                break;
            case SpeechRecognizer.ERROR_NETWORK:
                errorMsg = "Network error";
                break;
            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                errorMsg = "Network timeout";
                break;
            case SpeechRecognizer.ERROR_NO_MATCH:
                errorMsg = "No speech input detected";
                break;
            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                errorMsg = "Speech recognizer busy";
                break;
            case SpeechRecognizer.ERROR_SERVER:
                errorMsg = "Server error";
                break;
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                errorMsg = "No speech input detected";
                break;
            default:
                errorMsg = "Unknown error (code: " + error + ")";
        }

        Log.e(TAG, "Speech recognition error: " + errorMsg);
        errorMessage = errorMsg;
        notifyStateChange();
    }

    @Override
    public void onResults(android.os.Bundle results) {
        isListening = false;

        try {
            if (results != null) {
                ArrayList<String> matches = results.getStringArrayList(
                        SpeechRecognizer.RESULTS_RECOGNITION);

                if (matches != null && !matches.isEmpty()) {
                    recognizedText = matches.get(0);
                    partialResults = "";
                    volume = 0f;

                    Log.d(TAG, "Final result: " + recognizedText);
                    notifyStateChange();

                    if (onResultListener != null) {
                        onResultListener.onResult(recognizedText);
                    }
                } else {
                    errorMessage = "No speech detected";
                    Log.w(TAG, "No matches found");
                    notifyStateChange();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error processing results: " + e.getMessage(), e);
            errorMessage = "Error processing speech: " + e.getMessage();
            notifyStateChange();
        }
    }

    @Override
    public void onPartialResults(android.os.Bundle partialResults) {
        try {
            if (partialResults != null) {
                ArrayList<String> matches = partialResults.getStringArrayList(
                        SpeechRecognizer.RESULTS_RECOGNITION);

                if (matches != null && !matches.isEmpty()) {
                    this.partialResults = matches.get(0);
                    Log.d(TAG, "Partial result: " + this.partialResults);
                    notifyStateChange();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error processing partial results: " + e.getMessage(), e);
        }
    }

    @Override
    public void onEvent(int eventType, android.os.Bundle params) {
        Log.d(TAG, "onEvent: eventType=" + eventType);
    }

    /**
     * Release resources
     */
    public void release() {
        try {
            if (speechRecognizer != null) {
                speechRecognizer.destroy();
                speechRecognizer = null;
            }
            Log.d(TAG, "SpeechRecognizer resources released");
        } catch (Exception e) {
            Log.e(TAG, "Error releasing resources: " + e.getMessage(), e);
        }
    }

    /**
     * Check if device supports speech recognition
     */
    public boolean isAvailable() {
        return SpeechRecognizer.isRecognitionAvailable(context);
    }

    /**
     * Get current state
     */
    public SpeechRecognitionState getState() {
        return new SpeechRecognitionState(
                isListening,
                recognizedText,
                partialResults,
                volume,
                errorMessage
        );
    }

    /**
     * Notify listeners of state change
     */
    private void notifyStateChange() {
        if (onStateChangeListener != null) {
            onStateChangeListener.onStateChanged(getState());
        }
    }

    public interface SpeechListener {
        void onSpeechRecognized(String text);
        void onListeningStarted();
        void onListeningEnded();
        void onError(String error);
    }
    /**
     * Data class for speech recognition state
     */
    public static class SpeechRecognitionState {
        public boolean isListening;
        public String recognizedText;
        public String partialText;
        public float volume;
        public String error;

        public SpeechRecognitionState(boolean isListening, String recognizedText,
                                      String partialText, float volume, String error) {
            this.isListening = isListening;
            this.recognizedText = recognizedText;
            this.partialText = partialText;
            this.volume = volume;
            this.error = error;
        }
    }
}