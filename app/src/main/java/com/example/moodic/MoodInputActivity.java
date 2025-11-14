package com.example.moodic;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class MoodInputActivity extends AppCompatActivity {

    private EditText etMood;
    private Button btnSubmitMood;

    private FirebaseAuth mAuth;
    private DatabaseReference database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mood_input); // you will need a minimal XML for this

        etMood = findViewById(R.id.etMood);
        btnSubmitMood = findViewById(R.id.btnSubmitMood);

        mAuth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance().getReference("users");

        btnSubmitMood.setOnClickListener(v -> submitMood());
    }

    private void submitMood(){
        String mood = etMood.getText().toString().trim();
        if(mood.isEmpty()){
            Toast.makeText(this, "Enter your mood", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if(currentUser != null){
            String uid = currentUser.getUid();
            database.child(uid).child("lastMood").setValue(mood)
                    .addOnCompleteListener(task -> {
                        if(task.isSuccessful()){
                            Toast.makeText(this, "Mood saved!", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(MoodInputActivity.this, MainActivity.class));
                            finish();
                        } else {
                            Toast.makeText(this, "Failed to save mood", Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }
}
