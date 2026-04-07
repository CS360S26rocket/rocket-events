package com.example.seprojectpart3;

import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class OtpManager {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();


    public void sendOtp(String email, OtpCallback callback) {
        String otp = generateOtp();
        long expiryTime = System.currentTimeMillis() + (10 * 60 * 1000);

        Map<String, Object> otpData = new HashMap<>();
        otpData.put("otp", otp);
        otpData.put("email", email);
        otpData.put("expiresAt", expiryTime);
        otpData.put("used", false);

        db.collection("otps").document(email).set(otpData)
                .addOnSuccessListener(v -> {

                    new Thread(() -> {
                        try {
                            EmailSender.sendOtpEmail(email, otp);
                            callback.onSuccess();
                        } catch (Exception e) {
                            callback.onFailure("Failed to send email: " + e.getMessage());
                        }
                    }).start();
                })
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }


    public void verifyOtp(String email, String enteredOtp, OtpCallback callback) {
        db.collection("otps").document(email).get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        callback.onFailure("OTP not found. Please request a new one.");
                        return;
                    }

                    String storedOtp = doc.getString("otp");
                    long expiresAt = doc.getLong("expiresAt");
                    boolean used = Boolean.TRUE.equals(doc.getBoolean("used"));

                    if (used) {
                        callback.onFailure("OTP already used.");
                    } else if (System.currentTimeMillis() > expiresAt) {
                        callback.onFailure("OTP expired. Please request a new one.");
                    } else if (!enteredOtp.equals(storedOtp)) {
                        callback.onFailure("Incorrect OTP.");
                    } else {

                        doc.getReference().update("used", true)
                                .addOnSuccessListener(v -> callback.onSuccess())
                                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
                    }
                })
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    private String generateOtp() {
        return String.format("%06d", new Random().nextInt(999999));
    }

    public interface OtpCallback {
        void onSuccess();
        void onFailure(String error);
    }
}