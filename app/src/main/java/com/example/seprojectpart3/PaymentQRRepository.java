/*
 * This file defines PaymentQRRepository, a data repository used by the Scene app.
 * It contains payment QR upload metadata and retrieval for organizer payment settings.
 * Its functions include savePaymentQRUrl, getPaymentQRUrl, removePaymentQR to load data, handle user actions, validate input, and save results.
 * It connects this feature to the Scene app's UI, data, navigation, and verification flow.
 */

package com.example.seprojectpart3;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;








public class PaymentQRRepository {

    private static final String COLLECTION_EVENTS = "events";
    private static final String FIELD_PAYMENT_QR_URL = "paymentQRUrl";
    private static final String FIELD_PAYMENT_QR_UPDATED = "paymentQRUpdatedAt";

    private final FirebaseFirestore db;

    public PaymentQRRepository() {
        this.db = FirebaseFirestore.getInstance();
    }

    public PaymentQRRepository(FirebaseFirestore db) {
        this.db = db;
    }

    
    public interface OnQRSavedListener {
        void onSuccess();
        void onFailure(String errorMessage);
    }

    public interface OnQRUrlLoadedListener {
        void onLoaded(String url);  
    }

    







    public void savePaymentQRUrl(String eventId, String downloadUrl,
                                 OnQRSavedListener listener) {
        if (eventId == null || eventId.isEmpty()) {
            listener.onFailure("Event ID is required");
            return;
        }
        if (downloadUrl == null || downloadUrl.isEmpty()) {
            listener.onFailure("Download URL is required");
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put(FIELD_PAYMENT_QR_URL, downloadUrl);
        updates.put(FIELD_PAYMENT_QR_UPDATED,
                com.google.firebase.firestore.FieldValue.serverTimestamp());

        db.collection(COLLECTION_EVENTS)
                .document(eventId)
                .update(updates)
                .addOnSuccessListener(aVoid -> listener.onSuccess())
                .addOnFailureListener(e ->
                        listener.onFailure("Failed to save QR URL: " + e.getMessage()));
    }

    






    public void getPaymentQRUrl(String eventId, OnQRUrlLoadedListener listener) {
        if (eventId == null || eventId.isEmpty()) {
            listener.onLoaded(null);
            return;
        }

        db.collection(COLLECTION_EVENTS)
                .document(eventId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String url = doc.getString(FIELD_PAYMENT_QR_URL);
                        listener.onLoaded(url);
                    } else {
                        listener.onLoaded(null);
                    }
                })
                .addOnFailureListener(e -> listener.onLoaded(null));
    }

    


    public void removePaymentQR(String eventId, OnQRSavedListener listener) {
        if (eventId == null || eventId.isEmpty()) {
            listener.onFailure("Event ID is required");
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put(FIELD_PAYMENT_QR_URL,
                com.google.firebase.firestore.FieldValue.delete());
        updates.put(FIELD_PAYMENT_QR_UPDATED,
                com.google.firebase.firestore.FieldValue.serverTimestamp());

        db.collection(COLLECTION_EVENTS)
                .document(eventId)
                .update(updates)
                .addOnSuccessListener(aVoid -> listener.onSuccess())
                .addOnFailureListener(e ->
                        listener.onFailure("Failed to remove QR: " + e.getMessage()));
    }
}
