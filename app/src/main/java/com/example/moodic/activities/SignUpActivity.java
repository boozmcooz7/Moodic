package com.example.moodic.activities;
import com.example.moodic.AuthCompleteListener;
import com.example.moodic.LoginActivity;
import com.example.moodic.MoodInputActivity;
import com.example.moodic.R;
import com.example.moodic.data.FirebaseManager;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class SignUpActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private Button btnSignUp;
    private TextView goToSignIn;

    private FirebaseManager firebaseManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        firebaseManager = FirebaseManager.getInstance();
        etEmail = findViewById(R.id.signUpEmailEditText);
        etPassword = findViewById(R.id.signUpPasswordEditText);
        btnSignUp = findViewById(R.id.signUpButton);
        goToSignIn = findViewById(R.id.goToSignInTextView);

        btnSignUp.setOnClickListener(v -> registerUser());
        goToSignIn.setOnClickListener(v -> {
            startActivity(new Intent(SignUpActivity.this, LoginActivity.class));
            finish();
        });
    }

    private void registerUser() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if(email.isEmpty() || password.isEmpty()){
            Toast.makeText(this, "Fill in email and password", Toast.LENGTH_SHORT).show();
            return;
        }
        firebaseManager.registerUser(email, password, /* userName */ "New User", new AuthCompleteListener() {
            @Override
            public void onSuccess(String uid) {
                Toast.makeText(SignUpActivity.this, "Account created!", Toast.LENGTH_SHORT).show();
                // אם ההרשמה והכתיבה ל-Firestore הצליחו, עוברים מסך
                startActivity(new Intent(SignUpActivity.this, MoodInputActivity.class));
                finish();
            }

            @Override
            public void onFailure(String error) {
                Toast.makeText(SignUpActivity.this, "Registration failed: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }
}
