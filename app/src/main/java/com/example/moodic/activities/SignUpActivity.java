package com.example.moodic.activities;
import com.example.moodic.AuthCompleteListener;
import com.example.moodic.activities.LoginActivity;
import com.example.moodic.R;
import com.example.moodic.data.FirebaseManager;
import com.example.moodic.data.ValidationUtils;

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

        if (!ValidationUtils.isValidEmail(email)) {
            etEmail.setError("Invalid email format");
            return;
        }
        if (!ValidationUtils.isValidPassword(password)) {
            etPassword.setError("Password must be 6+ characters");
            return;
        }

        // Show a ProgressBar here (RC apps don't leave users guessing if it's loading)
        btnSignUp.setEnabled(false);

        firebaseManager.registerUser(email, password, "New User", new AuthCompleteListener() {
            @Override
            public void onSuccess(String uid) {
                btnSignUp.setEnabled(true);
                startActivity(new Intent(SignUpActivity.this, MainActivity.class));
                finish();
            }

            @Override
            public void onFailure(String error) {
                btnSignUp.setEnabled(true);
                // Better error handling:
                if (error.contains("already in use")) {
                    Toast.makeText(SignUpActivity.this, "Email already registered. Try logging in.", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(SignUpActivity.this, error, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}
