package com.example.moodic.data;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.os.Build;
import android.util.Log;

import com.example.moodic.AuthCompleteListener;
import com.example.moodic.ProfileLoadListener;
import com.example.moodic.models.Track;
import com.example.moodic.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FirebaseManager {
    private static final String TAG = "FirebaseManager";
    private static FirebaseManager instance;

    private final Context appContext;
    private final FirebaseAuth mAuth;
    private final FirebaseFirestore db;

    // Interfaces from Version 2
    public interface FavoritesLoadListener {
        void onFavoritesLoaded(List<Track> tracks);
        void onFavoritesLoadFailed(String error);
    }

    public interface ActionListener {
        void onSuccess();
        void onFailure(String error);
    }

    private FirebaseManager(Context context) {
        this.appContext = context.getApplicationContext();
        this.mAuth = FirebaseAuth.getInstance();
        this.db = FirebaseFirestore.getInstance();

        // Enable Offline Persistence
        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .build();
        try {
            db.setFirestoreSettings(settings);
        } catch (Exception e) {
            Log.w(TAG, "Firestore settings already applied.");
        }
    }

    public static void init(Context context) {
        if (instance == null) {
            instance = new FirebaseManager(context);
        }
    }

    public static FirebaseManager getInstance() {
        if (instance == null) {
            throw new IllegalStateException("Call FirebaseManager.init(context) in Application class first.");
        }
        return instance;
    }

    // -----------------------------------------------------------------------
    // Network Helper
    // -----------------------------------------------------------------------

    public boolean isNetworkAvailable() {
        try {
            ConnectivityManager cm = (ConnectivityManager) appContext.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm == null) return false;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                android.net.Network network = cm.getActiveNetwork();
                if (network == null) return false;
                NetworkCapabilities caps = cm.getNetworkCapabilities(network);
                return caps != null && (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                        || caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR));
            } else {
                return cm.getActiveNetworkInfo() != null && cm.getActiveNetworkInfo().isConnected();
            }
        } catch (Exception e) {
            return false;
        }
    }

    // -----------------------------------------------------------------------
    // Authentication
    // -----------------------------------------------------------------------

    public void registerUser(String email, String password, String userName, AuthCompleteListener listener) {
        if (!isNetworkAvailable()) {
            listener.onFailure("No internet connection.");
            return;
        }
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && mAuth.getCurrentUser() != null) {
                        listener.onSuccess(mAuth.getCurrentUser().getUid());
                    } else {
                        listener.onFailure(task.getException() != null ? task.getException().getMessage() : "Registration failed");
                    }
                });
    }

    public void loginUser(String email, String password, AuthCompleteListener listener) {
        if (!isNetworkAvailable()) {
            listener.onFailure("No internet connection.");
            return;
        }
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && mAuth.getCurrentUser() != null) {
                        listener.onSuccess(mAuth.getCurrentUser().getUid());
                    } else {
                        listener.onFailure(task.getException() != null ? task.getException().getMessage() : "Login failed");
                    }
                });
    }

    // -----------------------------------------------------------------------
    // User Profile
    // -----------------------------------------------------------------------

    public void loadUserProfile(String uid, ProfileLoadListener listener) {
        db.collection("Users").document(uid).get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        listener.onProfileLoaded(snapshot.toObject(User.class));
                    } else {
                        listener.onProfileLoadFailed("Profile not found.");
                    }
                })
                .addOnFailureListener(e -> listener.onProfileLoadFailed(buildNetworkError(e)));
    }

    public void saveUserProfile(String uid, String userName, List<String> favoriteGenres,
                                List<Track> favoriteTracks, double[] dynamicProfile) {
        try {
            User user = new User();
            db.collection("Users").document(uid).set(user, SetOptions.merge())
                    .addOnSuccessListener(v -> Log.d(TAG, "Profile saved"))
                    .addOnFailureListener(e -> Log.e(TAG, "Save failed: " + buildNetworkError(e)));
        } catch (Exception e) {
            Log.e(TAG, "saveUserProfile error", e);
        }
    }

    // -----------------------------------------------------------------------
    // Favorites (Sub-collection Logic)
    // -----------------------------------------------------------------------

    public void saveFavoriteTrack(String uid, Track track, ActionListener listener) {
        db.collection("Users").document(uid).collection("favorites").document(track.getId())
                .set(track, SetOptions.merge())
                .addOnSuccessListener(v -> { if(listener != null) listener.onSuccess(); })
                .addOnFailureListener(e -> { if(listener != null) listener.onFailure(buildNetworkError(e)); });
    }

    public void removeFavoriteTrack(String uid, String trackId, ActionListener listener) {
        db.collection("Users").document(uid).collection("favorites").document(trackId)
                .delete()
                .addOnSuccessListener(v -> { if(listener != null) listener.onSuccess(); })
                .addOnFailureListener(e -> { if(listener != null) listener.onFailure(buildNetworkError(e)); });
    }

    public void loadFavoriteTracks(String uid, FavoritesLoadListener listener) {
        db.collection("Users").document(uid).collection("favorites")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Track> tracks = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        tracks.add(doc.toObject(Track.class));
                    }
                    listener.onFavoritesLoaded(tracks);
                })
                .addOnFailureListener(e -> listener.onFavoritesLoadFailed(buildNetworkError(e)));
    }

    // -----------------------------------------------------------------------
    // Mood History
    // -----------------------------------------------------------------------

    public void saveMoodEntry(String uid, Map<String, Object> moodData) {
        moodData.put("timestamp", FieldValue.serverTimestamp());
        db.collection("Users").document(uid).collection("moodHistory")
                .add(moodData)
                .addOnSuccessListener(ref -> Log.d(TAG, "Mood logged"))
                .addOnFailureListener(e -> Log.e(TAG, "Mood log failed: " + buildNetworkError(e)));
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private String buildNetworkError(Exception e) {
        if (e == null) return "Unknown error";
        String msg = e.getMessage() != null ? e.getMessage() : "Connection error";
        if (!isNetworkAvailable()) return "Offline: Changes will sync later.";
        return msg;
    }
}