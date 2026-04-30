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

/**
 * Task 2 – Requirement #12: Mood-to-Playlist via Generative AI
 *
 * Takes a user's free-text mood description (e.g. "I'm feeling lonely in the rain")
 * and calls the Gemini REST API to retrieve 3-5 musical keywords / genres.
 * Those keywords are then passed back to the UI so a YouTube search can be triggered.
 *
 * Usage (call from an Activity / Fragment):
 *
 *   MoodAIHelper helper = new MoodAIHelper(apiKey, new MoodAIHelper.Callback() {
 *       @Override
 *       public void onKeywordsReady(List<String> keywords) {
 *           // e.g. ["melancholic", "indie folk", "acoustic", "rainy day"]
 *           // trigger YouTubeDataSource.searchTracks(String.join(" ", keywords), 10)
 *       }
 *       @Override
 *       public void onError(String errorMessage) {
 *           Toast.makeText(ctx, "AI error: " + errorMessage, Toast.LENGTH_SHORT).show();
 *       }
 *   });
 *   helper.execute("I'm feeling lonely in the rain");
 */
public class MoodAIHelper extends AsyncTask<String, Void, List<String>> {

    // ── Constants ────────────────────────────────────────────────────────────
    private static final String TAG = "MoodAIHelper";

    /**
     * Gemini 2.0 Flash – fast, cheap, perfect for short keyword generation.
     * Change to "gemini-1.5-pro" if you need longer / richer output.
     */
    private static final String GEMINI_MODEL = "gemini-2.0-flash";

    /**
     * REST endpoint pattern for Gemini generateContent.
     * The API key is appended as a query parameter.
     */
    private static final String ENDPOINT_TEMPLATE =
            "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s";

    // ── State ────────────────────────────────────────────────────────────────
    private final String apiKey;
    private final Callback callback;
    private String errorMessage = null;

    // ── Public interface ─────────────────────────────────────────────────────

    /**
     * Callback delivered on the main thread after the AsyncTask finishes.
     */
    public interface Callback {
        /** Called with 3-5 musical keywords when the API succeeds. */
        void onKeywordsReady(List<String> keywords);

        /** Called when the network request or JSON parsing fails. */
        void onError(String errorMessage);
    }

    /**
     * @param apiKey   Your Gemini API key (from BuildConfig.Gemini_API_KEY).
     * @param callback Result receiver – runs on the main thread.
     */
    public MoodAIHelper(String apiKey, Callback callback) {
        this.apiKey   = apiKey;
        this.callback = callback;
    }

    // ── AsyncTask lifecycle ──────────────────────────────────────────────────

    /**
     * Runs on a background thread.
     * @param params params[0] is the free-text mood input from the user.
     */
    @Override
    protected List<String> doInBackground(String... params) {
        if (params == null || params.length == 0 || params[0] == null) {
            errorMessage = "No mood input provided";
            return null;
        }
        String moodInput = params[0].trim();
        Log.d(TAG, "Analyzing mood: " + moodInput);

        try {
            return callGeminiApi(moodInput);
        } catch (Exception e) {
            Log.e(TAG, "Gemini API call failed", e);
            errorMessage = e.getMessage();
            return null;
        }
    }

    /** Runs on the main thread after doInBackground completes. */
    @Override
    protected void onPostExecute(List<String> keywords) {
        if (callback == null) return;

        if (keywords == null || keywords.isEmpty()) {
            callback.onError(errorMessage != null ? errorMessage : "No keywords returned");
        } else {
            Log.d(TAG, "Keywords received: " + keywords);
            callback.onKeywordsReady(keywords);
        }
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    /**
     * Sends a request to the Gemini REST API and returns the parsed keyword list.
     */
    private List<String> callGeminiApi(String moodInput) throws Exception {
        String url = String.format(ENDPOINT_TEMPLATE, GEMINI_MODEL, apiKey);

        // Build the prompt – instruct Gemini to output only a JSON array.
        String prompt = buildPrompt(moodInput);

        // Build the Gemini request body.
        JSONObject requestBody = buildRequestBody(prompt);

        // Execute HTTP POST.
        String rawResponse = postJson(url, requestBody.toString());
        Log.d(TAG, "Gemini raw response: " + rawResponse);

        // Extract the text content from the response.
        String textContent = extractTextFromResponse(rawResponse);

        // Parse the JSON array of keywords.
        return parseKeywordsFromText(textContent);
    }

    /**
     * Constructs the system + user prompt for keyword extraction.
     * We ask for strict JSON so parsing is reliable.
     */
    private String buildPrompt(String moodInput) {
        return "You are a music recommendation assistant.\n\n" +
                "A user described their current mood as: \"" + moodInput + "\"\n\n" +
                "Based on this mood, return EXACTLY 3 to 5 musical keywords or genres " +
                "that best match it. These keywords will be used as a YouTube music search query.\n\n" +
                "Rules:\n" +
                "- Return ONLY a JSON array of strings. Example: [\"melancholic\", \"indie folk\", \"acoustic\"]\n" +
                "- No extra text, no markdown, no explanation – just the JSON array.\n" +
                "- Keep each keyword concise (1–3 words max).\n" +
                "- Mix genre names with mood descriptors for better search results.";
    }

    /**
     * Wraps the prompt in the Gemini generateContent request schema.
     */
    private JSONObject buildRequestBody(String prompt) throws Exception {
        JSONObject textPart = new JSONObject();
        textPart.put("text", prompt);

        JSONArray parts = new JSONArray();
        parts.put(textPart);

        JSONObject userContent = new JSONObject();
        userContent.put("role", "user");
        userContent.put("parts", parts);

        JSONArray contents = new JSONArray();
        contents.put(userContent);

        // Optional: temperature = 0.7 for slight creative variety
        JSONObject generationConfig = new JSONObject();
        generationConfig.put("temperature", 0.7);
        generationConfig.put("maxOutputTokens", 128);

        JSONObject body = new JSONObject();
        body.put("contents", contents);
        body.put("generationConfig", generationConfig);

        return body;
    }

    /**
     * Performs an HTTP POST with a JSON body and returns the response as a String.
     */
    private String postJson(String urlString, String jsonBody) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        try {
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            conn.setDoOutput(true);
            conn.setConnectTimeout(10_000);
            conn.setReadTimeout(15_000);

            // Write body
            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonBody.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int code = conn.getResponseCode();
            Log.d(TAG, "HTTP response code: " + code);

            if (code == HttpURLConnection.HTTP_OK) {
                try (BufferedReader reader =
                             new BufferedReader(new InputStreamReader(conn.getInputStream(),
                                     StandardCharsets.UTF_8))) {
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) sb.append(line);
                    return sb.toString();
                }
            } else {
                // Read error stream for diagnostic info
                try (BufferedReader reader =
                             new BufferedReader(new InputStreamReader(conn.getErrorStream(),
                                     StandardCharsets.UTF_8))) {
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) sb.append(line);
                    String errBody = sb.toString();
                    Log.e(TAG, "Gemini error body: " + errBody);
                    throw new Exception("HTTP " + code + ": " + errBody);
                }
            }
        } finally {
            conn.disconnect();
        }
    }

    /**
     * Navigates the Gemini response JSON to pull out the model's text output.
     * Response shape: { "candidates": [ { "content": { "parts": [ { "text": "..." } ] } } ] }
     */
    private String extractTextFromResponse(String rawResponse) throws Exception {
        JSONObject root       = new JSONObject(rawResponse);
        JSONArray  candidates = root.getJSONArray("candidates");
        JSONObject candidate  = candidates.getJSONObject(0);
        JSONObject content    = candidate.getJSONObject("content");
        JSONArray  parts      = content.getJSONArray("parts");
        return parts.getJSONObject(0).getString("text").trim();
    }

    /**
     * Parses the model's text output into a List<String>.
     * The model is instructed to return a JSON array, e.g. ["indie", "melancholic", "folk"].
     * We strip markdown fences if the model still includes them.
     */
    private List<String> parseKeywordsFromText(String text) throws Exception {
        // Strip potential markdown code fences
        String clean = text.replaceAll("(?i)```json", "")
                .replaceAll("```", "")
                .trim();

        List<String> keywords = new ArrayList<>();
        JSONArray arr = new JSONArray(clean);
        for (int i = 0; i < arr.length(); i++) {
            String kw = arr.getString(i).trim();
            if (!kw.isEmpty()) keywords.add(kw);
        }
        return keywords;
    }
}