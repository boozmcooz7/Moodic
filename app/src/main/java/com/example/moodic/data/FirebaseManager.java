package com.example.moodic.data;


import android.util.Log;
import com.example.moodic.models.User;
import com.example.moodic.AuthCompleteListener;
import com.example.moodic.ProfileLoadListener;
import com.example.moodic.models.Track;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;
import java.util.Map;


public class FirebaseManager {
    private static FirebaseManager instance;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private static final String TAG = "FirebaseManager";


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

    private void loginUser(String email, String password, final AuthCompleteListener listener) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String uid = mAuth.getCurrentUser().getUid();
                        loadUserProfile(uid, new ProfileLoadListener() {
                            @Override
                            public void onProfileLoaded(User user) {
                                // אם הטעינה הצליחה, מעבירים הצלחה ל-Activity
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


    public void saveUserProfile(String uid, String userName, List<String> favoriteGenres, List<Track> favoriteTracks, double[] dynamicListeningProfile) {
        // Create or update user profile
        User updatedUser = new User(uid, userName, favoriteGenres, favoriteTracks, dynamicListeningProfile);

        db.collection("Users").document(uid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // If the document exists, update it
                        db.collection("Users").document(uid)
                                .set(updatedUser)  // Overwrites the document
                                .addOnSuccessListener(aVoid -> Log.d(TAG, "✅ User profile updated successfully for UID: " + uid))
                                .addOnFailureListener(e -> Log.e(TAG, "❌ Error updating user profile for UID: " + uid, e));
                    } else {
                        // If the document doesn't exist, create a new one
                        db.collection("Users").document(uid)
                                .set(updatedUser)
                                .addOnSuccessListener(aVoid -> Log.d(TAG, "✅ New user profile created successfully for UID: " + uid))
                                .addOnFailureListener(e -> Log.e(TAG, "❌ Error creating user profile for UID: " + uid, e));
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "❌ Error checking user profile for UID: " + uid, e));
    }


    public void saveMoodEntry(String uid, Map<String, Object> moodData) {
        moodData.put("timestamp", FieldValue.serverTimestamp());
        db.collection("Users").document(uid).collection("moodHistory")
                .add(moodData)
                .addOnSuccessListener(documentReference -> Log.d(TAG, "✅ Mood entry added successfully for UID: " + uid))
                .addOnFailureListener(e -> Log.e(TAG, "❌ Error adding mood entry for UID: " + uid, e));
    }

    public void saveFavoriteTrack(String uid, String trackId, String trackTitle) {
        // pass: עדכון ה-favoriteTracks באוסף Users
    }

    public List<Object> loadFavorites(String uid) {
        // pass: קריאה ושליפת רשימת המועדפים
        return null;
    }

    public void updateDynamicProfile(String uid, double[] newData) {
        // pass: עדכון השדה dynamicListeningProfile במסמך User
    }

    public List<Object> getMoodHistory(String uid) {
        // pass: קריאה לאוסף MoodHistory
        return null;
    }
}


            