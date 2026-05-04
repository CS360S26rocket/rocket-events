/*
 * This file defines CampusLoginActivity, an Android activity used by the Scene app.
 * It contains campus user sign-in and navigation into the campus event flow.
 * Its functions include onCreate, onSuccess, onFailure, showError to load data, handle user actions, validate input, and save results.
 * It connects this feature to the Scene app's UI, data, navigation, and verification flow.
 */

package com.example.seprojectpart3;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.firestore.FirebaseFirestore;

public class CampusLoginActivity extends AppCompatActivity {

    EditText etEmail, etPassword;
    TextView tvError;
    AuthRepository authRepo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_campus_login);

        etEmail    = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        tvError    = findViewById(R.id.tvError);
        authRepo   = new AuthRepository();

        
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        
        findViewById(R.id.btnLogin).setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                showError("Please enter your email and password.");
                return;
            }

            authRepo.loginUser(email, password, new AuthRepository.AuthCallback() {
                @Override
                public void onSuccess(String token, String uid) {
                    
                    FirebaseFirestore.getInstance()
                            .collection("users")
                            .document(uid)
                            .get()
                            .addOnSuccessListener(doc -> {
                                String role = doc.getString("role");
                                if ("organizer".equals(role)) {
                                    
                                    Intent intent = new Intent(CampusLoginActivity.this,
                                            OrganizerDashboardActivity.class);
                                    intent.putExtra("uid", uid);
                                    startActivity(intent);
                                    finish();
                                } else {
                                    
                                    Intent intent = new Intent(CampusLoginActivity.this, CampusHomeActivity.class);
                                    intent.putExtra("uid", uid);
                                    startActivity(intent);
                                    finish();
                                }
                            })
                            .addOnFailureListener(e -> showError(e.getMessage()));
                }

                @Override
                public void onFailure(String error) {
                    showError(error);
                }
            });
        });

        
        findViewById(R.id.tvSignUp).setOnClickListener(v ->
                startActivity(new Intent(this, CampusRegisterActivity.class)));

        
        findViewById(R.id.tvForgotPassword).setOnClickListener(v ->
                startActivity(new Intent(this, ForgotPasswordActivity.class)));
    }

    private void showError(String msg) {
        tvError.setText(msg);
        tvError.setVisibility(View.VISIBLE);
    }
}
