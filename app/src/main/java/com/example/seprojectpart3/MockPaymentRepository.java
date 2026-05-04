/*
 * This file defines MockPaymentRepository, a data repository used by the Scene app.
 * It contains mock Paymo transaction verification and amount matching.
 * Its functions include verifyTransaction, parse, normalize, value to load data, handle user actions, validate input, and save results.
 * It connects this feature to the Scene app's UI, data, navigation, and verification flow.
 */

package com.example.seprojectpart3;

import androidx.annotation.NonNull;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class MockPaymentRepository {

    private static final String TRANSACTIONS = "mock_payment_transactions";
    private static final String VERIFICATIONS = "payment_verifications";
    private static final String PROOF_SUBMISSIONS = "proof_submissions";

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public interface VerificationCallback {
        void onVerified(String transactionId);
        void onFailure(String error);
    }

    public void verifyTransaction(@NonNull String rawTransactionId,
                                  @NonNull String eventId,
                                  @NonNull String userId,
                                  @NonNull String userName,
                                  @NonNull String userEmail,
                                  double expectedAmount,
                                  @NonNull VerificationCallback callback) {
        String transactionId = normalize(rawTransactionId);
        if (transactionId.isEmpty()) {
            callback.onFailure("Enter the payment code first.");
            return;
        }
        if (eventId.trim().isEmpty() || userId.trim().isEmpty()) {
            callback.onFailure("Event and user are required for payment verification.");
            return;
        }
        if (expectedAmount <= 0) {
            callback.onFailure("Select a paid ticket before verifying payment.");
            return;
        }

        long amount = Math.round(expectedAmount);
        MockTransaction parsed = parse(transactionId);
        if (!parsed.valid) {
            callback.onFailure("This payment code could not be verified.");
            return;
        }
        if (parsed.amount != amount) {
            callback.onFailure("Transaction amount does not match. Expected PKR " + amount + ".");
            return;
        }

        DocumentReference eventRef = db.collection("events").document(eventId);
        DocumentReference txRef = db.collection(TRANSACTIONS).document(transactionId);
        DocumentReference verificationRef = db.collection(VERIFICATIONS).document(transactionId);
        DocumentReference notificationRef = db.collection("notifications").document();
        DocumentReference proofRef = db.collection(PROOF_SUBMISSIONS)
                .document("mock_" + eventId + "_" + userId + "_" + transactionId);

        db.runTransaction(transaction -> {
            DocumentSnapshot eventSnap = transaction.get(eventRef);
            if (!eventSnap.exists()) {
                throw new IllegalStateException("Event not found.");
            }

            DocumentSnapshot txSnap = transaction.get(txRef);
            if (txSnap.exists()) {
                String existingUser = txSnap.getString("userId");
                String existingEvent = txSnap.getString("eventId");
                Long existingAmount = txSnap.getLong("amount");
                if (!userId.equals(existingUser)
                        || !eventId.equals(existingEvent)
                        || existingAmount == null
                        || existingAmount != amount) {
                    throw new IllegalStateException("This payment code has already been used.");
                }
            }

            String organizerId = value(eventSnap, "organizerId");
            String eventTitle = value(eventSnap, "title");

            Map<String, Object> payment = new HashMap<>();
            payment.put("transactionId", transactionId);
            payment.put("provider", parsed.provider);
            payment.put("eventId", eventId);
            payment.put("eventTitle", eventTitle);
            payment.put("userId", userId);
            payment.put("userName", userName);
            payment.put("userEmail", userEmail);
            payment.put("organizerId", organizerId);
            payment.put("amount", amount);
            payment.put("currency", "PKR");
            payment.put("status", "verified");
            payment.put("verifiedAt", FieldValue.serverTimestamp());

            transaction.set(txRef, payment);
            transaction.set(verificationRef, payment);

            Map<String, Object> notification = new HashMap<>();
            notification.put("userId", userId);
            notification.put("eventId", eventId);
            notification.put("type", "payment_verified");
            notification.put("title", "Payment verified");
            notification.put("message", "Your PKR " + amount + " payment for "
                    + eventTitle + " has been verified.");
            notification.put("status", "queued");
            notification.put("createdAt", FieldValue.serverTimestamp());
            transaction.set(notificationRef, notification);

            Map<String, Object> proof = new HashMap<>(payment);
            proof.put("proofImageUrl", "");
            proof.put("source", "mock_payment_api");
            proof.put("status", ProofSubmission.STATUS_APPROVED);
            proof.put("submittedAt", FieldValue.serverTimestamp());
            proof.put("reviewedAt", FieldValue.serverTimestamp());
            proof.put("reviewedBy", "mock_payment_api");
            proof.put("idempotencyKey", eventId + "_" + userId + "_mock_payment");
            transaction.set(proofRef, proof);

            return transactionId;
        }).addOnSuccessListener(callback::onVerified)
                .addOnFailureListener(e -> callback.onFailure(message(e)));
    }

    private MockTransaction parse(String transactionId) {
        String[] parts = transactionId.split("-");
        if (parts.length != 3 || !"OK".equals(parts[2])) {
            return MockTransaction.invalid();
        }
        String provider = parts[0];
        if (!"PAYMO".equals(provider) && !"STRIPE".equals(provider) && !"MOCK".equals(provider)) {
            return MockTransaction.invalid();
        }
        try {
            long amount = Long.parseLong(parts[1]);
            return new MockTransaction(true, provider, amount);
        } catch (NumberFormatException e) {
            return MockTransaction.invalid();
        }
    }

    private String normalize(String raw) {
        return raw == null ? "" : raw.trim().toUpperCase(Locale.US).replace(" ", "");
    }

    private String value(DocumentSnapshot snap, String key) {
        Object value = snap.get(key);
        return value == null ? "" : String.valueOf(value);
    }

    private String message(Exception e) {
        return e == null || e.getMessage() == null ? "Payment verification failed." : e.getMessage();
    }

    private static class MockTransaction {
        final boolean valid;
        final String provider;
        final long amount;

        MockTransaction(boolean valid, String provider, long amount) {
            this.valid = valid;
            this.provider = provider;
            this.amount = amount;
        }

        static MockTransaction invalid() {
            return new MockTransaction(false, "", 0);
        }
    }
}
