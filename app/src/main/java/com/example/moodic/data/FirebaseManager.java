package com.example.moodic.data;


import android.util.Log;
import com.example.moodic.models.User;
import com.example.moodic.AuthCompleteListener;
import com.example.moodic.ProfileLoadListener;
import com.example.moodic.models.Track;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class FirebaseManager {
    private static FirebaseManager instance;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private static final String TAG = "FirebaseManager";

    public interface FavoritesLoadListener {
        void onFavoritesLoaded(List<Track> tracks);
        void onFavoritesLoadFailed(String error);
    }

    public interface ActionListener {
        void onSuccess();
        void onFailure(String error);
    }



    private FirebaseManager() {
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
    }

    public static FirebaseManager getInstance() {
        if (instance == null) {
            instance = new FirebaseManager();
        }
        return instance;
    }

    public void registerUser(String email, String password, String userName, final AuthCompleteListener listener) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String uid = mAuth.getCurrentUser().getUid();
                        //saveUserProfile(uid, userName);
                        listener.onSuccess(uid);
                    } else {
                        listener.onFailure(task.getException().getMessage());
                    }
                });
    }

    private void loginUser(String email, String password,
                           final AuthCompleteListener listener) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String uid = mAuth.getCurrentUser().getUid();
                        loadUserProfile(uid, new ProfileLoadListener() {
                            @Override
                            public void onProfileLoaded(User user) {
                                listener.onSuccess(uid);
                            }

                            @Override
                            public void onProfileLoadFailed(String error) {
                                listener.onFailure("Login successful but profile load failed: " + error);
                            }
                        });
                    } else {
                        listener.onFailure(task.getException().getMessage());
                    }
                });
    }

    public void loadUserProfile(String uid, final ProfileLoadListener listener) {
        db.collection("Users").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        User user = documentSnapshot.toObject(User.class);
                        listener.onProfileLoaded(user);
                    } else {
                        listener.onProfileLoadFailed("User profile document not found.");
                    }
                })
                .addOnFailureListener(e -> {
                    listener.onProfileLoadFailed(e.getMessage());
                });
    }

    public void saveUserProfile(String uid, String userName, List<String> favoriteGenres,
                                List<Track> favoriteTracks, List<Double> dynamicListeningProfile) {

        User updatedUser = new User(uid, userName, favoriteGenres, favoriteTracks, dynamicListeningProfile);

        db.collection("Users").document(uid)
                .set(updatedUser)
                .addOnSuccessListener(aVoid ->
                        Log.d(TAG, "✅ User profile saved for UID: " + uid))
                .addOnFailureListener(e ->
                        Log.e(TAG, "❌ Error saving user profile for UID: " + uid, e));
    }

    public void updateDynamicProfile(String uid, double[] newData) {
        List<Double> dataList = new ArrayList<>();
        if (newData != null) {
            for (double d : newData) dataList.add(d);
        }

        Map<String, Object> update = new HashMap<>();
        update.put("dynamicListeningProfile", dataList);

        // 🔥 Changed .update to .set with merge to prevent NOT_FOUND error
        db.collection("Users").document(uid)
                .set(update, com.google.firebase.firestore.SetOptions.merge())
                .addOnSuccessListener(aVoid -> Log.d(TAG, "✅ Profile synced"))
                .addOnFailureListener(e -> Log.e(TAG, "❌ Sync failed", e));
    }

    // -------------------------------------------------------------------------
    // Mood history
    // -------------------------------------------------------------------------

    public void saveMoodEntry(String uid, Map<String, Object> moodData) {
        moodData.put("timestamp", FieldValue.serverTimestamp());
        db.collection("Users").document(uid).collection("moodHistory")
                .add(moodData)
                .addOnSuccessListener(ref ->
                        Log.d(TAG, "✅ Mood entry added for UID: " + uid))
                .addOnFailureListener(e ->
                        Log.e(TAG, "❌ Error adding mood entry for UID: " + uid, e));
    }

    public List<Object> getMoodHistory(String uid) {
        // TODO: implement for tracking screen
        return null;
    }

    // -------------------------------------------------------------------------
    // Favorites
    // -------------------------------------------------------------------------

    /**
     * Save a track to the user's favorites sub-collection.
     * Uses the YouTube video ID as the document ID so duplicates are avoided.
     */
    public void saveFavoriteTrack(String uid, Track track, ActionListener listener) {
        Map<String, Object> trackData = new HashMap<>();
        trackData.put("id", track.getId());
        trackData.put("title", track.getTitle());
        trackData.put("artist", track.getArtist());
        trackData.put("thumbnailUrl", track.getThumbnailUrl());
        trackData.put("youtubeUrl", track.getYoutubeUrl());
        trackData.put("genre", track.getGenre());
        trackData.put("savedAt", FieldValue.serverTimestamp());

        db.collection("Users").document(uid)
                .collection("favorites").document(track.getId())
                .set(trackData)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "✅ Favorite saved: " + track.getTitle());
                    if (listener != null) listener.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Error saving favorite", e);
                    if (listener != null) listener.onFailure(e.getMessage());
                });
    }

    /**
     * Remove a track from the user's favorites by video ID.
     */
    public void removeFavoriteTrack(String uid, String trackId, ActionListener listener) {
        db.collection("Users").document(uid)
                .collection("favorites").document(trackId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "✅ Favorite removed: " + trackId);
                    if (listener != null) listener.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Error removing favorite", e);
                    if (listener != null) listener.onFailure(e.getMessage());
                });
    }

    /**
     * Load all favorite tracks for a user.
     */
    public void loadFavoriteTracks(String uid, FavoritesLoadListener listener) {
        db.collection("Users").document(uid)
                .collection("favorites")
                .orderBy("savedAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Track> tracks = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        Track track = new Track();
                        track.setId(doc.getString("id"));
                        track.setTitle(doc.getString("title"));
                        track.setArtist(doc.getString("artist"));
                        track.setThumbnailUrl(doc.getString("thumbnailUrl"));
                        track.setYoutubeUrl(doc.getString("youtubeUrl"));
                        track.setGenre(doc.getString("genre"));
                        tracks.add(track);
                    }
                    Log.d(TAG, "✅ Loaded " + tracks.size() + " favorites for UID: " + uid);
                    listener.onFavoritesLoaded(tracks);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Error loading favorites", e);
                    listener.onFavoritesLoadFailed(e.getMessage());
                });
    }
}
