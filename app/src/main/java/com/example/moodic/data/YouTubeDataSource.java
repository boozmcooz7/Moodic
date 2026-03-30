package com.example.moodic.data;

import android.util.Log;

import com.example.moodic.BuildConfig;
import com.example.moodic.models.Track;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

public class YouTubeDataSource {
    private static final String TAG = "YouTubeDataSource";
    private static YouTubeDataSource instance;
    private static final String BASE_URL = "https://www.googleapis.com/youtube/v3/search";

    private YouTubeDataSource() {}

    public static synchronized YouTubeDataSource getInstance() {
        if (instance == null) {
            instance = new YouTubeDataSource();
        }
        return instance;
    }

        public List<Track> searchByMoodAndGenre(String mood, String genre) {
            // 🚀 ARCHITECT'S TIP: YouTube loves the word "official" and "song"
            // to filter out garbage results.
            String cleanQuery = mood + " " + genre + " official music song";
            return fetchFromYouTube(cleanQuery);
        }

    public List<Track> searchTracks(String query, int maxResults) {
        return fetchFromYouTube(query);
    }

    private List<Track> fetchFromYouTube(String query) {
        if (query == null || query.trim().isEmpty()) {
            query = "trending music 2026"; // Fallback so the user isn't left with an empty screen
        }
        List<Track> tracks = new ArrayList<>();
        HttpURLConnection conn = null;
        try {
            String encodedQuery = URLEncoder.encode(query, "UTF-8");

            // 🔥 FIX: Simplified URL. Removed videoCategoryId=10 as it often causes 400 errors
            // if the API key doesn't have specific permissions or in certain regions.
            String urlString = BASE_URL + "?part=snippet" +
                    "&type=video" +
                    "&maxResults=10" +
                    "&q=" + encodedQuery +
                    "&key=" + BuildConfig.Test_Key;

            URL url = new URL(urlString);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            int responseCode = conn.getResponseCode();
            Log.d(TAG, "📡 Sending Request: " + urlString);
            Log.d(TAG, "📡 Response Code: " + responseCode);

            if (responseCode == 200) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);

                JSONObject json = new JSONObject(sb.toString());
                JSONArray items = json.optJSONArray("items");

                if (items != null) {
                    for (int i = 0; i < items.length(); i++) {
                        JSONObject obj = items.getJSONObject(i);
                        JSONObject snippet = obj.getJSONObject("snippet");
                        String videoId = obj.getJSONObject("id").getString("videoId");

                        Track track = new Track();
                        track.setId(videoId);
                        track.setTitle(snippet.getString("title"));
                        track.setArtist(snippet.getString("channelTitle"));
                        track.setThumbnailUrl(snippet.getJSONObject("thumbnails").getJSONObject("default").getString("url"));
                        track.setYoutubeUrl("https://www.youtube.com/watch?v=" + videoId);
                        tracks.add(track);
                    }
                }
            } else {
                // 🔍 ERROR DIAGNOSIS: This reads the actual error message from Google
                BufferedReader errorReader = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
                StringBuilder errorSb = new StringBuilder();
                String errorLine;
                while ((errorLine = errorReader.readLine()) != null) errorSb.append(errorLine);
                Log.e(TAG, "❌ YouTube API Error Detail: " + errorSb.toString());
            }

        } catch (Exception e) {
            Log.e(TAG, "❌ YouTube Search Exception: " + e.getMessage());
        } finally {
            if (conn != null) conn.disconnect();
        }
        return tracks;
    }
}