package com.example.seprojectpart3;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class CampusRegisterActivity extends AppCompatActivity {

    EditText etName, etEmail, etPassword;
    TextView tvError;
    RegistrationRepository regRepo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_campus_register);

        etName     = findViewById(R.id.etName);
        etEmail    = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        tvError    = findViewById(R.id.tvError);
        regRepo    = new RegistrationRepository();

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // "Log in" link → go back to login
        findViewById(R.id.tvLogin).setOnClickListener(v -> finish());

        findViewById(R.id.btnRegister).setOnClickListener(v -> {
            String name     = etName.getText().toString().trim();
            String email    = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
                showError("Please fill in all fields.");
                return;
            }

            showStatus("Creating account...");

            regRepo.registerUser(email, password, name, "campus_user",
                    new RegistrationRepository.RegistrationCallback() {
                        @Override
                        public void onSuccess(String uid) {
                            // Registration done — go to login
                            showSuccess("Account created! Please log in.");
                            tvError.postDelayed(() -> {
                                finish(); // back to CampusLoginActivity
                            }, 1200);
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
