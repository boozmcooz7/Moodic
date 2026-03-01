package com.example.moodic;

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

import com.example.moodic.activities.SignUpActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.Map;


public class LoginActivity extends AppCompatActivity {
    private static final String TAG = "MoodTunesAuth";
    private static final int PICK_IMAGE_REQUEST = 1;

    // Firebase Components
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseStorage storage;

    // UI Components
    private EditText etEmail, etPassword;
    private Button btnLogin, btnRegister;
    private ImageView ivProfileImage;
    private ProgressBar progressBar;

    // ViewModel
    private Uri imageUri;
    public AuthViewModel authViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();

        // Initialize UI Components
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnRegister = findViewById(R.id.btnRegister);
        ivProfileImage = findViewById(R.id.appLogo);
        progressBar = findViewById(R.id.progressBar);

        // Initialize ViewModel
        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        // Observe ViewModel LiveData for authentication results
        authViewModel.getAuthResult().observe(this, result -> {
            setLoadingState(false);
            if (result.user != null) {
                if (result.isRegistration) {
                    Map<String, Object> userData = new HashMap<>();
                    userData.put("email", result.user.getEmail());
                    uploadImageAndSaveData(result.user, userData);
                } else {
                    // Success: If login was successful, navigate to main screen
                    Toast.makeText(LoginActivity.this, "התחברת בהצלחה! 🎉", Toast.LENGTH_SHORT).show();
                    navigateToMainScreen();
                }
            } else if (result.error != null) {
                // Failure: Show error toast
                Toast.makeText(LoginActivity.this, "Authentication failed: " + result.error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        // Login button click listener
        btnLogin.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();
            if (isValid(email, password)) {
                setLoadingState(true);
                authViewModel.loginUser(email, password);
            }
        });

        // Create Account button - Navigate to SignUpActivity
        btnRegister.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, SignUpActivity.class);
            startActivity(intent);
        });

        ivProfileImage.setOnClickListener(this::chooseImage);
    }

    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in and update UI accordingly
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            navigateToMainScreen();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
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
        startActivityForResult(Intent.createChooser(intent, "בחר תמונת פרופיל"), PICK_IMAGE_REQUEST);
    }

    private void uploadImageAndSaveData(FirebaseUser user, Map<String, Object> userData) {
        if (imageUri != null) {
            StorageReference fileRef = storage.getReference("profile_images/" + user.getUid());
            fileRef.putFile(imageUri)
                    .addOnSuccessListener(taskSnapshot -> fileRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        userData.put("profileImageUrl", uri.toString());
                        saveUserData(user, userData);
                    }))
                    .addOnFailureListener(e -> {
                        Toast.makeText(LoginActivity.this, "שגיאה בהעלאת התמונה. ממשיך ללא תמונה.", Toast.LENGTH_SHORT).show();
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
                    Toast.makeText(LoginActivity.this, "הרשמה מוצלחת ופרטי משתמש נשמרו! 🥳", Toast.LENGTH_SHORT).show();
                    navigateToMainScreen();
                })
                .addOnFailureListener(e -> {
                    setLoadingState(false);
                    Log.w(TAG, "Error adding user data", e);
                    Toast.makeText(LoginActivity.this, "שגיאה בשמירת נתוני המשתמש.", Toast.LENGTH_SHORT).show();
                });
    }

    private void navigateToMainScreen() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);  // ← Change to MainActivity
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish(); }
}