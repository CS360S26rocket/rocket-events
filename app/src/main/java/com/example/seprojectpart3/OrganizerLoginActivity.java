/*
 * This file defines OrganizerLoginActivity, an Android activity used by the Scene app.
 * It contains organizer sign-in and navigation into organizer tools.
 * Its functions include onCreate, onSuccess, onFailure, showError to load data, handle user actions, validate input, and save results.
 * It connects this feature to the Scene app's UI, data, navigation, and verification flow.
 */

package com.example.seprojectpart3;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class OrganizerLoginActivity extends AppCompatActivity {

    EditText etEmail, etPassword;
    TextView tvError;
    AuthRepository authRepo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_organizer_login);

        etEmail    = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        tvError    = findViewById(R.id.tvError);
        authRepo   = new AuthRepository();

        
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        
        findViewById(R.id.tvSignUp).setOnClickListener(v ->
                startActivity(new Intent(this, OrganizerRegisterActivity.class)));

        
        findViewById(R.id.btnLogin).setOnClickListener(v -> {
            String email    = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                showError("Please enter your email and password.");
                return;
            }

            authRepo.loginUser(email, password, new AuthRepository.AuthCallback() {
                @Override
                public void onSuccess(String token, String uid) {
                    Intent intent = new Intent(OrganizerLoginActivity.this,
                            OrganizerDashboardActivity.class);
                    intent.putExtra("uid", uid);
                    startActivity(intent);
                    finish();
                }

                @Override
                public void onFailure(String error) {
                    showError(error);
                }
            });
        });
    }

    private void showError(String msg) {
        tvError.setText(msg);
        tvError.setVisibility(View.VISIBLE);
    }
}
