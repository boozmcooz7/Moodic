package com.example.moodic.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.moodic.AuthViewModel;
import com.example.moodic.R;
import com.example.moodic.SharedPreferencesManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {
    private static final String TAG             = "MoodTunesAuth";
    private static final int    PICK_IMAGE_REQUEST = 1;

    // Firebase
    private FirebaseAuth      mAuth;
    private FirebaseFirestore db;
    private FirebaseStorage   storage;

    // UI
    private EditText    etEmail, etPassword;
    private Button      btnLogin, btnRegister;
    private ImageView   ivProfileImage;
    private ProgressBar progressBar;

    // Misc
    private Uri imageUri;
    public AuthViewModel authViewModel;
    private SharedPreferencesManager prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Init Firebase
        mAuth   = FirebaseAuth.getInstance();
        db      = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();

        // Init SharedPreferences
        prefs = SharedPreferencesManager.getInstance(this);

        // Init UI
        etEmail        = findViewById(R.id.etEmail);
        etPassword     = findViewById(R.id.etPassword);
        btnLogin       = findViewById(R.id.btnLogin);
        btnRegister    = findViewById(R.id.btnRegister);
        ivProfileImage = findViewById(R.id.appLogo);
        progressBar    = findViewById(R.id.progressBar);

        // ── Pre-fill email from cache (convenience on re-login) ───────────────
        String cachedEmail = prefs.getUserEmail();
        if (!cachedEmail.isEmpty()) {
            etEmail.setText(cachedEmail);
        }

        // Init ViewModel
        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        // Observe auth results
        authViewModel.getAuthResult().observe(this, result -> {
            setLoadingState(false);

            if (result.user != null) {
                if (result.isRegistration) {
                    Map<String, Object> userData = new HashMap<>();
                    userData.put("email", result.user.getEmail());
                    uploadImageAndSaveData(result.user, userData);
                } else {
                    // ── On successful login, persist session data ──────────────
                    onLoginSuccess(result.user);
                }
            } else if (result.error != null) {
                Toast.makeText(LoginActivity.this,
                        "Authentication failed: " + result.error.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });

        btnLogin.setOnClickListener(v -> {
            String email    = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();
            if (isValid(email, password)) {
                setLoadingState(true);
                authViewModel.loginUser(email, password);
            }
        });

        btnRegister.setOnClickListener(v ->
                startActivity(new Intent(LoginActivity.this, SignUpActivity.class)));

        ivProfileImage.setOnClickListener(this::chooseImage);
    }

    @Override
    public void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            // Already signed in — skip login screen
            onLoginSuccess(currentUser);
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Persists lightweight session data to SharedPreferences, then navigates
     * to MainActivity.  Safe to call whenever a valid FirebaseUser exists.
     */
    private void onLoginSuccess(FirebaseUser user) {
        // Cache email and mark session as active
        if (user.getEmail() != null) {
            prefs.saveUserEmail(user.getEmail());
        }
        prefs.setLoggedIn(true);

        Toast.makeText(LoginActivity.this, "התחברת בהצלחה! 🎉", Toast.LENGTH_SHORT).show();
        navigateToMainScreen();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST
                && resultCode == RESULT_OK
                && data != null
                && data.getData() != null) {
            imageUri = data.getData();
            ivProfileImage.setImageURI(imageUri);
            Toast.makeText(this, "תמונה נבחרה בהצלחה", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean isValid(String email, String password) {
        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "יש למלא כתובת אימייל וסיסמה.", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private void setLoadingState(boolean isLoading) {
        if (progressBar != null) {
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        }
        btnLogin.setEnabled(!isLoading);
        btnRegister.setEnabled(!isLoading);
    }

    public void chooseImage(View view) {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(
                Intent.createChooser(intent, "בחר תמונת פרופיל"),
                PICK_IMAGE_REQUEST);
    }

    private void uploadImageAndSaveData(FirebaseUser user, Map<String, Object> userData) {
        if (imageUri != null) {
            StorageReference fileRef =
                    storage.getReference("profile_images/" + user.getUid());
            fileRef.putFile(imageUri)
                    .addOnSuccessListener(taskSnapshot ->
                            fileRef.getDownloadUrl().addOnSuccessListener(uri -> {
                                userData.put("profileImageUrl", uri.toString());
                                saveUserData(user, userData);
                            }))
                    .addOnFailureListener(e -> {
                        Toast.makeText(LoginActivity.this,
                                "שגיאה בהעלאת התמונה. ממשיך ללא תמונה.",
                                Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Image upload failed", e);
                        saveUserData(user, userData);
                    });
        } else {
            saveUserData(user, userData);
        }
    }

    private void saveUserData(FirebaseUser user, Map<String, Object> userData) {
        db.collection("Users").document(user.getUid())
                .set(userData)
                .addOnSuccessListener(aVoid -> {
                    // ── Also cache in SharedPreferences so the welcome screen works offline ──
                    if (user.getEmail() != null) {
                        prefs.saveUserEmail(user.getEmail());
                    }
                    prefs.setLoggedIn(true);

                    Toast.makeText(LoginActivity.this,
                            "הרשמה מוצלחת ופרטי משתמש נשמרו! 🥳",
                            Toast.LENGTH_SHORT).show();
                    navigateToMainScreen();
                })
                .addOnFailureListener(e -> {
                    setLoadingState(false);
                    Log.w(TAG, "Error adding user data", e);
                    Toast.makeText(LoginActivity.this,
                            "שגיאה בשמירת נתוני המשתמש.",
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void navigateToMainScreen() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
