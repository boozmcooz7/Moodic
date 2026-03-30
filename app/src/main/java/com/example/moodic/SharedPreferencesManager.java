package com.example.moodic;

import android.content.Context;
import android.content.SharedPreferences;

public class SharedPreferencesManager {
    // ── Constants ────────────────────────────────────────────────────────────
    private static final String PREFS_NAME  = "moodic_prefs";

    // Keys
    private static final String KEY_USER_NAME       = "user_name";
    private static final String KEY_USER_EMAIL      = "user_email";
    private static final String KEY_LAST_MOOD       = "last_mood";
    private static final String KEY_LAST_GENRE      = "last_genre";
    private static final String KEY_LAST_TRACK      = "last_track";
    private static final String KEY_LAST_MOOD_TS    = "last_mood_timestamp"; // epoch ms
    private static final String KEY_IS_LOGGED_IN    = "is_logged_in";

    // Defaults
    private static final String DEFAULT_STRING      = "";
    private static final long   DEFAULT_LONG        = 0L;
    private static final boolean DEFAULT_BOOL       = false;

    // ── Singleton ─────────────────────────────────────────────────────────────
    private static SharedPreferencesManager instance;
    private final SharedPreferences prefs;

    private SharedPreferencesManager(Context context) {
        // Use application context to avoid memory leaks
        prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /** Returns the single instance, creating it on first call. */
    public static synchronized SharedPreferencesManager getInstance(Context context) {
        if (instance == null) {
            instance = new SharedPreferencesManager(context);
        }
        return instance;
    }

    // ── User profile ──────────────────────────────────────────────────────────

    /** Save the display name shown in the welcome screen. */
    public void saveUserName(String name) {
        prefs.edit().putString(KEY_USER_NAME, name).apply();
    }

    /** @return Saved display name, or {@code ""} if never saved. */
    public String getUserName() {
        return prefs.getString(KEY_USER_NAME, DEFAULT_STRING);
    }

    /** Save the email address of the logged-in user. */
    public void saveUserEmail(String email) {
        prefs.edit().putString(KEY_USER_EMAIL, email).apply();
    }

    /** @return Saved email, or {@code ""} if never saved. */
    public String getUserEmail() {
        return prefs.getString(KEY_USER_EMAIL, DEFAULT_STRING);
    }

    /** Persist the user's login state so we can show the right screen on cold start. */
    public void setLoggedIn(boolean loggedIn) {
        prefs.edit().putBoolean(KEY_IS_LOGGED_IN, loggedIn).apply();
    }

    /** @return {@code true} if the user was logged in during the last session. */
    public boolean isLoggedIn() {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, DEFAULT_BOOL);
    }

    // ── Last mood entry ───────────────────────────────────────────────────────

    /**
     * Save the most recent mood string entered by the user.
     * Called from MoodInputActivity after the user taps "Get Recommendations".
     */
    public void saveLastMood(String mood) {
        prefs.edit().putString(KEY_LAST_MOOD, mood).apply();
    }

    /** @return Last saved mood, or {@code ""} if the user has never submitted one. */
    public String getLastMood() {
        return prefs.getString(KEY_LAST_MOOD, DEFAULT_STRING);
    }

    /** Save the genre selected alongside the most recent mood. */
    public void saveLastGenre(String genre) {
        prefs.edit().putString(KEY_LAST_GENRE, genre).apply();
    }

    /** @return Last saved genre, or {@code ""} if never saved. */
    public String getLastGenre() {
        return prefs.getString(KEY_LAST_GENRE, DEFAULT_STRING);
    }

    /** Save the optional track name entered by the user. */
    public void saveLastTrack(String trackName) {
        prefs.edit().putString(KEY_LAST_TRACK, trackName).apply();
    }

    /** @return Last saved track name, or {@code ""} if never saved. */
    public String getLastTrack() {
        return prefs.getString(KEY_LAST_TRACK, DEFAULT_STRING);
    }

    /**
     * Save all three mood-related fields atomically in a single edit,
     * together with the current timestamp.
     *
     * @param mood      The mood string (e.g. "happy").
     * @param genre     The selected genre (e.g. "Pop").
     * @param trackName Optional track name; pass {@code ""} if not provided.
     */
    public void saveLastMoodEntry(String mood, String genre, String trackName) {
        prefs.edit()
                .putString(KEY_LAST_MOOD,    mood)
                .putString(KEY_LAST_GENRE,   genre)
                .putString(KEY_LAST_TRACK,   trackName)
                .putLong(KEY_LAST_MOOD_TS,   System.currentTimeMillis())
                .apply();
    }

    /**
     * @return Unix timestamp (ms) of the last saved mood entry,
     *         or {@code 0} if none exists.
     */
    public long getLastMoodTimestamp() {
        return prefs.getLong(KEY_LAST_MOOD_TS, DEFAULT_LONG);
    }

    /**
     * @return {@code true} if the user has submitted at least one mood entry.
     */
    public boolean hasLastMoodEntry() {
        return !prefs.getString(KEY_LAST_MOOD, DEFAULT_STRING).isEmpty();
    }

    // ── Session teardown ──────────────────────────────────────────────────────

    /**
     * Clear only the session-specific data (login state, email).
     * Called on logout. Mood history is intentionally kept so the welcome
     * screen can still show the last mood after re-login.
     */
    public void clearSession() {
        prefs.edit()
                .remove(KEY_IS_LOGGED_IN)
                .remove(KEY_USER_EMAIL)
                .apply();
    }

    /**
     * Wipe every key stored by this manager.
     * Use this only when the user explicitly deletes their account or data.
     */
    public void clearAll() {
        prefs.edit().clear().apply();
    }
}