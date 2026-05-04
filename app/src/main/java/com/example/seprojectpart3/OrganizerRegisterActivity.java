/*
 * This file defines OrganizerRegisterActivity, an Android activity used by the Scene app.
 * It contains organizer account creation and validation.
 * Its functions include onCreate, onSuccess, onFailure, showError to load data, handle user actions, validate input, and save results.
 * It connects this feature to the Scene app's UI, data, navigation, and verification flow.
 */

package com.example.seprojectpart3;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class OrganizerRegisterActivity extends AppCompatActivity {

    EditText etSocietyName, etEmail, etPassword, etDescription;
    TextView tvError;
    RegistrationRepository regRepo;
    OrganizerRepository orgRepo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_organizer_register);

        etSocietyName = findViewById(R.id.etSocietyName);
        etEmail       = findViewById(R.id.etEmail);
        etPassword    = findViewById(R.id.etPassword);
        etDescription = findViewById(R.id.etDescription);
        tvError       = findViewById(R.id.tvError);
        regRepo       = new RegistrationRepository();
        orgRepo       = new OrganizerRepository();

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        
        findViewById(R.id.tvLogin).setOnClickListener(v -> finish());

        findViewById(R.id.btnRegister).setOnClickListener(v -> {
            String societyName = etSocietyName.getText().toString().trim();
            String email       = etEmail.getText().toString().trim();
            String password    = etPassword.getText().toString().trim();
            String description = etDescription.getText().toString().trim();

            if (societyName.isEmpty() || email.isEmpty() || password.isEmpty()) {
                showError("Please fill in society name, email and password.");
                return;
            }

            showStatus("Registering society...");

            
            regRepo.registerUser(email, password, societyName, "organizer",
                    new RegistrationRepository.RegistrationCallback() {
                        @Override
                        public void onSuccess(String uid) {
                            
                            orgRepo.registerOrganizer(uid, societyName, email,
                                    description.isEmpty() ? "Campus society" : description,
                                    new OrganizerRepository.OrganizerCallback() {
                                        @Override
                                        public void onSuccess(String id) {
                                            showSuccess("Society registered! Awaiting approval. Please log in.");
                                            tvError.postDelayed(() -> finish(), 1500);
                                        }

                                        @Override
                                        public void onFailure(String error) {
                                            showError("Registered but profile save failed: " + error);
                                        }
                                    });
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
        tvError.setTextColor(getColor(R.color.error_red));
        tvError.setVisibility(View.VISIBLE);
    }

    private void showStatus(String msg) {
        tvError.setText(msg);
        tvError.setTextColor(getColor(R.color.text_secondary));
        tvError.setVisibility(View.VISIBLE);
    }

    private void showSuccess(String msg) {
        tvError.setText(msg);
        tvError.setTextColor(getColor(R.color.accent_lime));
        tvError.setVisibility(View.VISIBLE);
    }
}
