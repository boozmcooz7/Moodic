package com.example.moodic.data;


import com.example.moodic.AuthCompleteListener;
import com.example.moodic.ProfileLoadListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.example.moodic.models.User;

import java.util.List;
import java.util.Map;


public class FirebaseManager {
    private static FirebaseManager instance;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

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
                        saveNewUserProfile(uid, userName);
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
public void saveNewUserProfile(String uid, String userName) {
    // TODO: יצירת אובייקט User ושמירה באמצעות db.collection("Users").document(uid).set(user)
    /// User newUser = new User(
    ///                 uid,
    ///                 userName,
    ///                 Arrays.asList("Pop", "Rock"), // אתחול העדפות ז'אנר בסיסי
    ///                 null, // רשימת מועדפים ריקה
    ///                 new double[]{0.0, 0.0}
    ///         );
    /// db.collection("Users").document(uid).set(newUser)
    ///                 .addOnSuccessListener(aVoid -> {
    ///                   ה
    ///                 })
    ///                 .addOnFailureListener(e -> {
    ///                 });
       }

public void saveMoodEntry(String uid, Map<String, Object> moodData) {
    // pass: שמירת ה-moodVector שהתקבל ל-MoodHistory
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


