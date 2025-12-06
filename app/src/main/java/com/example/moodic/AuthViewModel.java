package com.example.moodic;


import android.net.Uri;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import java.util.HashMap;
import java.util.Map;


public class AuthViewModel extends ViewModel {
    private final FirebaseAuth mAuth = FirebaseAuth.getInstance();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final FirebaseStorage storage = FirebaseStorage.getInstance();
    private final MutableLiveData<AuthResult> authResult = new MutableLiveData<>();

    public LiveData<AuthResult> getAuthResult() {
        return authResult;
    }

    public void loginUser(String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        authResult.setValue(new AuthResult(task.getResult().getUser(), null, false));
                    } else {
                        authResult.setValue(new AuthResult(null, task.getException(), false));
                    }
                });
    }

    public void registerUser(String email, String password) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        authResult.setValue(new AuthResult(task.getResult().getUser(), null, true));
                    } else {
                        // Post a failure result
                        authResult.setValue(new AuthResult(null, task.getException(), true));
                    }
                });
    }

    // Helper class for results
    class AuthResult {
        @Nullable
        public final FirebaseUser user;
        @Nullable
        public final Exception error;
        public final boolean isRegistration;

        public AuthResult(@Nullable FirebaseUser user, @Nullable Exception error, boolean isRegistration) {
            this.user = user;
            this.error = error;
            this.isRegistration = isRegistration;
        }
    }
}

