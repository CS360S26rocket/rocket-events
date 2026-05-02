package com.example.seprojectpart3;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

/**
 * Story ORG-A — Payment QR code Firestore repository
 * M3 · Sprint 4
 *
 * Stores and retrieves the organizer's payment QR code URL
 * on the event document in Firestore.
 */
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

    // ── Callbacks ───────────────────────────────────────────────
    public interface OnQRSavedListener {
        void onSuccess();
        void onFailure(String errorMessage);
    }

    public interface OnQRUrlLoadedListener {
        void onLoaded(String url);  // null if not set
    }

    /**
     * Save the payment QR image URL to the event document.
     * Called after FileStorageManager returns the download URL.
     *
     * @param eventId     The event to attach the QR code to
     * @param downloadUrl The secure download URL from FILE-STORAGE
     * @param listener    Callback
     */
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

    /**
     * Retrieve the payment QR image URL for an event.
     * Used by M1's USR-A to display the QR to users.
     *
     * @param eventId  The event to look up
     * @param listener Callback with URL (null if not set)
     */
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

    /**
     * Remove the payment QR code from an event (organizer action).
     */
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
