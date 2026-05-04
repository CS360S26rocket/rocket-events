/*
 * This file defines PaymentVerificationRepository, a data repository used by the Scene app.
 * It contains verified mock payment persistence and payment lookup behavior.
 * Its functions include getPaymentsForEvent to load data, handle user actions, validate input, and save results.
 * It connects this feature to the Scene app's UI, data, navigation, and verification flow.
 */

package com.example.seprojectpart3;

import androidx.annotation.NonNull;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PaymentVerificationRepository {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public interface PaymentListCallback {
        void onSuccess(List<Map<String, Object>> payments);
        void onFailure(String error);
    }

    public void getPaymentsForEvent(@NonNull String eventId,
                                    @NonNull PaymentListCallback callback) {
        if (eventId.trim().isEmpty()) {
            callback.onFailure("Event ID is required.");
            return;
        }

        db.collection("payment_verifications")
                .whereEqualTo("eventId", eventId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<Map<String, Object>> results = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snapshot) {
                        Map<String, Object> payment = doc.getData();
                        payment.put("paymentId", doc.getId());
                        results.add(payment);
                    }
                    callback.onSuccess(results);
                })
                .addOnFailureListener(e -> callback.onFailure(
                        e.getMessage() == null ? "Could not load payments." : e.getMessage()));
    }
}
