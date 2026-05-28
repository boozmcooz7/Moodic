package com.example.moodic.engines;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;

import java.util.ArrayList;

public class SpeechToTextEngine implements RecognitionListener {

    private static final String TAG = "SpeechToTextEngine";
    private SpeechRecognizer speechRecognizer;
    private Context context;
    private OnResultListener onResultListener;

    public interface OnResultListener {
        void onResult(String text);
    }

    public SpeechToTextEngine(Context context) {
        this.context = context.getApplicationContext();
        initializeSpeechRecognizer();
    }

    private void initializeSpeechRecognizer() {
        try {
            if (SpeechRecognizer.isRecognitionAvailable(context)) {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context);
                speechRecognizer.setRecognitionListener(this);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error initializing SpeechRecognizer", e);
        }
    }

    public void setOnResultListener(OnResultListener listener) {
        this.onResultListener = listener;
    }

    public void startListening(String language) {
        try {
            if (speechRecognizer == null) return;

            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, language);
            intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);

            speechRecognizer.startListening(intent);
        } catch (Exception e) {
            Log.e(TAG, "Error starting listening", e);
        }
    }

    @Override
    public void onResults(Bundle results) {
        try {
            if (results != null && onResultListener != null) {
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) {
                    String recognizedText = matches.get(0);
                    Log.d(TAG, "Speech success: " + recognizedText);

                    // תיקון: שילוח ישיר ופשוט של הטקסט לתיבה ללא חסימות AI באמצע!
                    onResultListener.onResult(recognizedText);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in onResults", e);
        }
    }

    public void release() {
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
            speechRecognizer = null;
        }
    }

    // מתודות חובה של הממשק - נשארות ריקות כדי לא להעמיס על הקוד
    @Override public void onReadyForSpeech(Bundle params) {}
    @Override public void onBeginningOfSpeech() {}
    @Override public void onRmsChanged(float rmsdB) {}
    @Override public void onBufferReceived(byte[] buffer) {}
    @Override public void onEndOfSpeech() {}
    @Override public void onError(int error) { Log.e(TAG, "Speech error code: " + error); }
    @Override public void onPartialResults(Bundle partialResults) {}
    @Override public void onEvent(int eventType, Bundle params) {}
}