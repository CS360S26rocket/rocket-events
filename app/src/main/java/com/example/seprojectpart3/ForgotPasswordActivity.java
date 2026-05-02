package com.example.seprojectpart3;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class ForgotPasswordActivity extends AppCompatActivity {

    private EditText etEmail;
    private EditText etOtp;
    private TextView tvStatus;

    private final OtpManager otpManager = new OtpManager();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        etEmail = findViewById(R.id.etEmail);
        etOtp = findViewById(R.id.etOtp);
        tvStatus = findViewById(R.id.tvStatus);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnSendOtp).setOnClickListener(v -> sendOtp());
        findViewById(R.id.btnVerifyOtp).setOnClickListener(v -> verifyOtpAndSendResetEmail());
    }

    private void sendOtp() {
        String email = etEmail.getText().toString().trim();

        if (email.isEmpty()) {
            show("Enter your email address.");
            return;
        }

        otpManager.sendOtp(email, new OtpManager.OtpCallback() {
            @Override
            public void onSuccess() {
                runOnUiThread(() -> show("OTP sent to " + email + "."));
            }

            @Override
            public void onFailure(String error) {
                runOnUiThread(() -> show(error));
            }
        });
    }

    private void verifyOtpAndSendResetEmail() {
        String email = etEmail.getText().toString().trim();
        String otp = etOtp.getText().toString().trim();

        if (email.isEmpty() || otp.isEmpty()) {
            show("Enter your email and OTP.");
            return;
        }

        otpManager.verifyOtp(email, otp, new OtpManager.OtpCallback() {
            @Override
            public void onSuccess() {
                runOnUiThread(() -> {
                    FirebaseAuth.getInstance()
                            .sendPasswordResetEmail(email)
                            .addOnSuccessListener(v ->
                                    show("OTP verified. Password reset email sent."))
                            .addOnFailureListener(e ->
                                    show(e.getMessage()));
                });
            }

            @Override
            public void onFailure(String error) {
                runOnUiThread(() -> show(error));
            }
        });
    }

    private void show(String message) {
        tvStatus.setVisibility(View.VISIBLE);
        tvStatus.setText(message);
    }
}
