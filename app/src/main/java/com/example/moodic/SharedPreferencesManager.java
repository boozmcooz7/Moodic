package com.example.moodic;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.example.moodic.models.Track;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class SharedPreferencesManager {
    private static final String TAG = "SPManager";
    private static final String PREFS_NAME = "moodic_prefs";

    private static final String KEY_USER_NAME     = "user_name";
    private static final String KEY_USER_EMAIL    = "user_email";
    private static final String KEY_IS_LOGGED_IN  = "is_logged_in";
    private static final String KEY_FAVORITES     = "favorites_json";
    private static final String KEY_LAST_MOOD      = "last_mood";
    private static final String KEY_LAST_GENRE     = "last_genre";
    private static final String KEY_LAST_TRACK     = "last_track";
    private static final String KEY_LAST_MOOD_TS   = "last_mood_timestamp";

    private static SharedPreferencesManager instance;
    private final SharedPreferences prefs;
    private final Gson gson;

    private SharedPreferencesManager(Context context) {
        this.prefs = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.gson = new Gson();
    }

    public static synchronized void init(Context context) {
        if (instance == null) {
            instance = new SharedPreferencesManager(context);
        }
    }

    public static SharedPreferencesManager getInstance() {
        if (instance == null) {
            throw new IllegalStateException("Call SharedPreferencesManager.init(context) first.");
        }
        return instance;
    }

    /**
     * Overloaded getInstance that ensures initialization if a Context is provided.
     */
    public static synchronized SharedPreferencesManager getInstance(Context context) {
        if (instance == null) {
            init(context);
        }
        return instance;
    }

    // ── User Session ────────────────────────────────────────────────────────

    public void setLoggedIn(boolean loggedIn) {
        prefs.edit().putBoolean(KEY_IS_LOGGED_IN, loggedIn).apply();
    }

    public boolean isLoggedIn() {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    public void saveUserEmail(String email) {
        prefs.edit().putString(KEY_USER_EMAIL, email).apply();
    }

    public String getUserEmail() {
        return prefs.getString(KEY_USER_EMAIL, "");
    }

    public void saveUserDetail(String name, String email) {
        prefs.edit().putString(KEY_USER_NAME, name).putString(KEY_USER_EMAIL, email).apply();
    }

    public String getUserName() { return prefs.getString(KEY_USER_NAME, ""); }

    public void clearSession() {
        prefs.edit().clear().apply();
        Log.d(TAG, "Session cleared.");
    }

    // ── Favorites Logic ─────────────────────────────────────────────────────

    public void saveFavorites(List<Track> tracks) {
        try {
            String json = gson.toJson(tracks);
            prefs.edit().putString(KEY_FAVORITES, json).apply();
        } catch (Exception e) {
            Log.e(TAG, "Error caching favorites", e);
        }
    }

    public List<Track> loadFavorites() {
        try {
            String json = prefs.getString(KEY_FAVORITES, null);
            if (json == null) return new ArrayList<>();
            Type listType = new TypeToken<List<Track>>() {}.getType();
            return gson.fromJson(json, listType);
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    /**
     * Reuses saveFavorites to add a single track safely.
     */
    public void addFavorite(Track track) {
        if (track == null || track.getId() == null) return;

        List<Track> currentList = loadFavorites();

        boolean exists = false;
        for (Track t : currentList) {
            if (track.getId().equals(t.getId())) {
                exists = true;
                break;
            }
        }

        if (!exists) {
            currentList.add(track);
            saveFavorites(currentList);
            Log.d(TAG, "Track added to local favorites.");
        }
    }

    // ── Tracking ────────────────────────────────────────────────────────────

    public void saveLastMoodEntry(String mood, String genre, String trackName) {
        prefs.edit()
                .putString(KEY_LAST_MOOD, mood)
                .putString(KEY_LAST_GENRE, genre)
                .putString(KEY_LAST_TRACK, trackName)
                .putLong(KEY_LAST_MOOD_TS, System.currentTimeMillis())
                .apply();
    }

    public boolean hasLastMoodEntry() {
        return prefs.contains(KEY_LAST_MOOD);
    }

    public String getLastMood() { return prefs.getString(KEY_LAST_MOOD, ""); }

    public String getLastGenre() { return prefs.getString(KEY_LAST_GENRE, ""); }

    public void clearAll() {
        clearSession();
    }
}