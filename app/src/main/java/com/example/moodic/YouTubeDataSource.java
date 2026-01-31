package com.example.moodic.datasource;

import static com.example.moodic.BuildConfig.YouTube_API_KEY;

import android.util.Log;

import com.example.moodic.models.Track;

import org.json.JSONArray;
import org.json.JSONException;
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
    private static final String YOUTUBE_API_KEY = YouTube_API_KEY; // Replace with your key
    private static final String YOUTUBE_API_URL = "https://www.googleapis.com/youtube/v3/search";

    private YouTubeDataSource() {
    }

    public static YouTubeDataSource getInstance() {
        if (instance == null) {
            instance = new YouTubeDataSource();
        }
        return instance;
    }

    /**
     * Search for music tracks on YouTube based on query
     * @param query Search query (e.g., "happy music" or "sad piano")
     * @param maxResults Number of results to return
     * @return List of Track objects
     */
    public List<Track> searchTracks(String query, int maxResults) {
        List<Track> tracks = new ArrayList<>();

        try {
            String searchQuery = buildSearchQuery(query, maxResults);
            String response = makeHttpRequest(searchQuery);
            tracks = parseYouTubeResponse(response);
            Log.d(TAG, "✅ Found " + tracks.size() + " tracks for query: " + query);
        } catch (Exception e) {
            Log.e(TAG, "❌ Error searching tracks", e);
        }

        return tracks;
    }

    /**
     * Search for tracks based on mood and genre
     */
    public List<Track> searchByMoodAndGenre(String mood, String genre) {
        String query = mood + " " + genre + " music";
        return searchTracks(query, 10);
    }

    /**
     * Build the YouTube API search query URL
     */
    private String buildSearchQuery(String query, int maxResults) throws Exception {
        String encodedQuery = URLEncoder.encode(query, "UTF-8");

        return YOUTUBE_API_URL +
                "?part=snippet" +
                "&q=" + encodedQuery +
                "&type=video" +
                "&maxResults=" + maxResults +
                "&key=" + YOUTUBE_API_KEY +
                "&order=relevance";
    }

    /**
     * Make HTTP GET request to YouTube API
     */
    private String makeHttpRequest(String urlString) throws Exception {
        StringBuilder response = new StringBuilder();

        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);

        int responseCode = conn.getResponseCode();
        Log.d(TAG, "Response code: " + responseCode);

        if (responseCode == HttpURLConnection.HTTP_OK) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();
        } else {
            Log.e(TAG, "HTTP Error: " + responseCode);
            return "{}";
        }

        conn.disconnect();
        return response.toString();
    }

    /**
     * Parse YouTube API response and convert to Track objects
     */
    private List<Track> parseYouTubeResponse(String jsonResponse) throws JSONException {
        List<Track> tracks = new ArrayList<>();

        JSONObject json = new JSONObject(jsonResponse);
        JSONArray items = json.optJSONArray("items");

        if (items == null) {
            Log.e(TAG, "No items found in response");
            return tracks;
        }

        for (int i = 0; i < items.length(); i++) {
            try {
                JSONObject item = items.getJSONObject(i);

                // Extract video ID
                String videoId = item.getJSONObject("id").getString("videoId");

                // Extract snippet info
                JSONObject snippet = item.getJSONObject("snippet");
                String title = snippet.getString("title");
                String description = snippet.getString("description");
                String channelTitle = snippet.getString("channelTitle");
                String thumbnail = snippet.getJSONObject("thumbnails")
                        .getJSONObject("default").getString("url");

                // Create Track object
                Track track = new Track();
                track.setId(videoId);
                track.setTitle(title);
                track.setArtist(channelTitle);
                track.setDescription(description);
                track.setThumbnailUrl(thumbnail);
                track.setYoutubeUrl("https://www.youtube.com/watch?v=" + videoId);

                tracks.add(track);

            } catch (JSONException e) {
                Log.e(TAG, "Error parsing item " + i, e);
            }
        }

        return tracks;
    }

    /**
     * Get YouTube embed URL for a video
     */
    public String getEmbedUrl(String videoId) {
        return "https://www.youtube.com/embed/" + videoId;
    }

    /**
     * Get YouTube watch URL for a video
     */
    public String getWatchUrl(String videoId) {
        return "https://www.youtube.com/watch?v=" + videoId;
    }
}