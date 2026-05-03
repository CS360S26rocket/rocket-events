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
