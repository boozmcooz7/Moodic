package com.example.moodic.engines;

import android.os.AsyncTask;
import android.util.Log;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class MoodAIHelper {

    private static final String TAG = "MoodAIHelper";
    private static final String GEMINI_MODEL = "gemini-2.0-flash";
    private static final String ENDPOINT_TEMPLATE = "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s";

    private final String apiKey;
    private String errorMessage = null;

    public interface Callback {
        void onKeywordsReady(List<String> keywords);
        void onError(String errorMessage);
    }

    public MoodAIHelper(String apiKey, Callback callback) {
        this.apiKey = apiKey;
    }

    // מתודת הרשת הראשית שרצה בתוך ה-Thread ב-MoodInputActivity
    public List<String> doInBackground(String moodInput) {
        try {
            String urlString = String.format(ENDPOINT_TEMPLATE, GEMINI_MODEL, apiKey);
            String prompt = "You are a music expert. The user feels: \"" + moodInput + "\". " +
                    "Provide EXACTLY 10 custom songs that match this vibe. " +
                    "Return ONLY a raw JSON array of strings, where each element is \"Artist Name - Song Title\". " +
                    "Do NOT use markdown, do NOT use ```json blocks, do NOT write explanations. " +
                    "Example format: [\"Taylor Swift - Cruel Summer\", \"Dua Lipa - Levitating\"]";

            // בניית ה-JSON עבור ה-API של Gemini
            JSONObject root = new JSONObject();
            JSONArray contents = new JSONArray();
            JSONObject userContent = new JSONObject();
            userContent.put("role", "user");
            JSONArray parts = new JSONArray();
            JSONObject textPart = new JSONObject();
            textPart.put("text", prompt);
            parts.put(textPart);
            userContent.put("parts", parts);
            contents.put(userContent);
            root.put("contents", contents);

            // ביצוע פניית HTTP POST
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            conn.setDoOutput(true);
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(15000);

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = root.toString().getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int code = conn.getResponseCode();
            if (code == HttpURLConnection.HTTP_OK) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) sb.append(line);

                    // חילוץ הטקסט הנקי מתוך המבנה של גוגל
                    JSONObject responseJson = new JSONObject(sb.toString());
                    String rawText = responseJson.getJSONArray("candidates")
                            .getJSONObject(0)
                            .getJSONObject("content")
                            .getJSONArray("parts")
                            .getJSONObject(0)
                            .getString("text").trim();

                    // ניקוי סימני קוד אם ה-AI הכניס אותם בטעות
                    String cleanJson = rawText.replaceAll("(?i)```json", "").replaceAll("```", "").trim();

                    // המרה לרשימת שירים אמיתית
                    List<String> songs = new ArrayList<>();
                    JSONArray songArray = new JSONArray(cleanJson);
                    for (int i = 0; i < songArray.length(); i++) {
                        songs.add(songArray.getString(i));
                    }
                    return songs;
                }
            } else {
                Log.e(TAG, "Gemini Server Error Code: " + code);
            }
        } catch (Exception e) {
            Log.e(TAG, "Gemini Parsing Exception", e);
        }
        return null;
    }
}