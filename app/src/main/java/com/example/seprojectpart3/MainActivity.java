package com.example.seprojectpart3;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;

import android.app.DatePickerDialog;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import java.util.Calendar;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
public class MainActivity extends AppCompatActivity {

    EditText etEmail, etPassword, etName;
    TextView tvResult;
    RegistrationRepository regRepo;
    AuthRepository authRepo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etName = findViewById(R.id.etName);
        tvResult = findViewById(R.id.tvResult);

        regRepo = new RegistrationRepository();
        authRepo = new AuthRepository();

        findViewById(R.id.btnRegister).setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();
            String name = etName.getText().toString().trim();

            regRepo.registerUser(email, password, name, "campus_user",
                    new RegistrationRepository.RegistrationCallback() {
                        @Override
                        public void onSuccess(String uid) {
                            tvResult.setText("Registered! UID: " + uid);
                        }
                        @Override
                        public void onFailure(String error) {
                            tvResult.setText("Error: " + error);
                        }
                    });
        });

        findViewById(R.id.btnLogin).setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            authRepo.loginUser(email, password,
                    new AuthRepository.AuthCallback() {
                        @Override
                        public void onSuccess(String token, String uid) {
                            android.content.Intent intent = new android.content.Intent(
                                    MainActivity.this, OrganizerDashboardActivity.class);
                            intent.putExtra("uid", uid);
                            startActivity(intent);
                        }
                        @Override
                        public void onFailure(String error) {
                            tvResult.setText("Error: " + error);
                        }
                    });
        });

        OrganizerRepository orgRepo = new OrganizerRepository();

        findViewById(R.id.btnOrganizer).setOnClickListener(v -> {
            String uid = "NQpNQiGInQhcA9AMsLeTfZjUo053"; // the UID you just got
            orgRepo.registerOrganizer(uid, "LUMS CS Society",
                    "cssociety@lums.edu.pk", "Computer Science Society",
                    new OrganizerRepository.OrganizerCallback() {
                        @Override
                        public void onSuccess(String id) {
                            tvResult.setText("Organizer registered!");
                        }
                        @Override
                        public void onFailure(String error) {
                            tvResult.setText("Error: " + error);
                        }
                    });
        });

        OtpManager otpManager = new OtpManager();
        EditText etOtp = findViewById(R.id.etOtp);

        findViewById(R.id.btnSendOtp).setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            otpManager.sendOtp(email, new OtpManager.OtpCallback() {
                @Override
                public void onSuccess() {
                    tvResult.setText("OTP sent to " + email + "! Check your inbox.");
                }
                @Override
                public void onFailure(String error) {
                    tvResult.setText("Error: " + error);
                }
            });
        });

        findViewById(R.id.btnVerifyOtp).setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String otp = etOtp.getText().toString().trim();
            otpManager.verifyOtp(email, otp, new OtpManager.OtpCallback() {
                @Override
                public void onSuccess() {
                    tvResult.setText("OTP verified! Password reset can proceed.");
                }
                @Override
                public void onFailure(String error) {
                    tvResult.setText("Error: " + error);
                }
            });
        });

        // Sprint 2 — M2 stories

    }
}