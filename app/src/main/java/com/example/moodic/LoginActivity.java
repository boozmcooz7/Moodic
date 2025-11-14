package com.example.moodic;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {
    private static final String TAG = "MoodTunesAuth"; //
    private static final int PICK_IMAGE_REQUEST = 1;

    // רכיבי Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseStorage storage;

    // רכיבי UI ונתונים
    private Uri imageUri;
    private EditText etEmail, etPassword;
    private Button btnRegister, btnLogin;
    private ImageView ivProfileImage; // למרות שלא חובה, נשמר את הרעיון לתמונת פרופיל

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login); // ודאי שקובץ ה-Layout קיים

        // 1. אתחול רכיבי Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnRegister = findViewById(R.id.btnRegister);
        btnLogin = findViewById(R.id.btnLogin);
        ivProfileImage = findViewById(R.id.appLogo);

        // 3. הגדרת מאזינים לכפתורים
        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                registerUser(); // קורא לפונקציה של הרשמה
            }
        });

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loginUser(); // קורא לפונקציה של התחברות
            }
        });

        // הגדרת מאזין לתמונת פרופיל (צריך להוסיף OnClickListener ל-ImageView ב-XML)
        // ivProfileImage.setOnClickListener(this::chooseImage);
    }

    // פונקציה לבחירת תמונה (אופציונלי)
    public void chooseImage(View view) {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "בחר תמונת פרופיל"), PICK_IMAGE_REQUEST);
    }

    // טיפול בתוצאת בחירת התמונה
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK
                && data != null && data.getData() != null) {
            imageUri = data.getData();
            ivProfileImage.setImageURI(imageUri);
            Toast.makeText(this, "תמונה נבחרה בהצלחה", Toast.LENGTH_SHORT).show();
        }
    }

    private void registerUser() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "יש למלא כתובת אימייל וסיסמה.", Toast.LENGTH_SHORT).show();
            return;
        }

        // שלב 1: יצירת משתמש ב-Firebase Authentication
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "createUserWithEmail:success");
                        FirebaseUser user = mAuth.getCurrentUser();

                        if (user != null) {
                            // שלב 2: הכנת נתונים לשמירה ב-Firestore
                            Map<String, Object> userData = new HashMap<>();
                            userData.put("email", user.getEmail());
                            userData.put("registrationDate", System.currentTimeMillis());

                            if (imageUri != null) {
                                uploadImage(user, userData); // העלאת תמונה ושמירת ה-URL
                            } else {
                                saveUserData(user, userData); // שמירת נתונים ללא תמונה
                            }
                        }
                    } else {
                        Log.w(TAG, "createUserWithEmail:failure", task.getException());
                        Toast.makeText(LoginActivity.this, "הרשמה נכשלה. " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    // פונקציה אופציונלית להעלאת תמונת פרופיל ל-Firebase Storage
    private void uploadImage(FirebaseUser user, Map<String, Object> userData) {
        if (imageUri != null) {
            StorageReference fileRef = storage.getReference("profile_images/" + user.getUid());

            fileRef.putFile(imageUri)
                    .addOnSuccessListener(taskSnapshot -> {
                        fileRef.getDownloadUrl().addOnSuccessListener(uri -> {
                            // הוספת ה-URL של התמונה לנתוני המשתמש ב-Firestore
                            userData.put("profileImageUrl", uri.toString());
                            saveUserData(user, userData);
                        });
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(LoginActivity.this, "שגיאה בהעלאת התמונה. ממשיך ללא תמונה.", Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Image upload failed", e);
                        saveUserData(user, userData); // ממשיך לשמור את הנתונים גם אם העלאת התמונה נכשלה
                    });
        }
    }

    // פונקציה לשמירת נתוני המשתמש ב-Firebase Firestore
    private void saveUserData(FirebaseUser user, Map<String, Object> userData) {
        db.collection("users").document(user.getUid())
                .set(userData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(LoginActivity.this, "הרשמה מוצלחת ופרטי משתמש נשמרו! 🥳", Toast.LENGTH_SHORT).show();

                    // מעבר למסך הראשי של הפרויקט
                    startActivity(new Intent(LoginActivity.this, MoodInputActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error adding user data", e);
                    Toast.makeText(LoginActivity.this, "שגיאה בשמירת נתוני המשתמש.", Toast.LENGTH_SHORT).show();
                });
    }

    private void loginUser() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "יש למלא כתובת אימייל וסיסמה.", Toast.LENGTH_SHORT).show();
            return;
        }

        // ביצוע התחברות ל-Firebase
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "signInWithEmail:success");
                        Toast.makeText(LoginActivity.this, "התחברת בהצלחה! 🎉", Toast.LENGTH_SHORT).show();

                        // מעבר למסך הראשי של הפרויקט
                        startActivity(new Intent(LoginActivity.this, MoodInputActivity.class));
                        finish();
                    } else {
                        Log.w(TAG, "signInWithEmail:failure", task.getException());
                        Toast.makeText(LoginActivity.this, "התחברות נכשלה. ודא שהפרטים נכונים.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // בדיקה בהפעלה ראשונית אם משתמש כבר מחובר
    @Override
    public void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if(currentUser != null){
            // אם המשתמש כבר מחובר, דלג על מסך ההתחברות
            startActivity(new Intent(LoginActivity.this, MoodInputActivity.class));
            finish();
        }
    }
}