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

        // "Log in" link → go back to login
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

            // Step 1: Create Firebase Auth account + save to users collection
            regRepo.registerUser(email, password, societyName, "organizer",
                    new RegistrationRepository.RegistrationCallback() {
                        @Override
                        public void onSuccess(String uid) {
                            // Step 2: Save to organizers collection
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
