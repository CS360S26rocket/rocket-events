package com.example.seprojectpart3;

import android.util.Log;

import androidx.annotation.NonNull;

import com.example.seprojectpart3.DiscountCode;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.Transaction;

import java.util.Date;
import java.util.List;

/**
 * Story #16 — Separate schema tracking for referral/discount codes.
 *
 * Handles all CRUD and validation for the discountCodes collection.
 * Use validateAndApply() when a user submits a code at checkout —
 * it atomically checks validity and increments usage count.
 */
public class DiscountCodeRepository {

    private static final String TAG = "DiscountCodeRepo";
    private static final String COLLECTION = "discountCodes";

    private final FirebaseFirestore db;

    public DiscountCodeRepository() {
        this.db = FirebaseFirestore.getInstance();
    }

    // ── Create ────────────────────────────────────────────────────────────────

    /**
     * Saves a new discount/referral code to Firestore.
     * Returns the auto-generated document ID via the Task result.
     */
    public Task<DocumentReference> createCode(@NonNull DiscountCode code) {
        return db.collection(COLLECTION).add(code)
                .addOnSuccessListener(ref -> Log.d(TAG, "Code created: " + ref.getId()))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to create code", e));
    }

    // ── Read ──────────────────────────────────────────────────────────────────

    /**
     * Fetches all active codes for a specific event (+ global codes for this organizer).
     * Call from organizer dashboard to list their codes.
     */
    public Task<List<DocumentSnapshot>> getCodesForEvent(@NonNull String eventId,
                                                         @NonNull String orgId) {
        return db.collection(COLLECTION)
                .whereEqualTo("createdByOrgId", orgId)
                .whereEqualTo("active", true)
                .get()
                .continueWith(task -> {
                    if (!task.isSuccessful() || task.getResult() == null) return null;
                    // Filter client-side: either global (null eventId) or matches this event
                    return task.getResult().getDocuments().stream()
                            .filter(doc -> doc.getString("eventId") == null
                                    || eventId.equals(doc.getString("eventId")))
                            .collect(java.util.stream.Collectors.toList());
                });
    }

    // ── Validate & Apply ──────────────────────────────────────────────────────

    public interface ValidationCallback {
        void onValid(DiscountCode code);
        void onInvalid(String reason); // "NOT_FOUND", "EXPIRED", "MAX_USES_REACHED", "WRONG_EVENT"
        void onError(Exception e);
    }

    /**
     * Atomically validates a code and increments its usage count.
     *
     * Call this when the user taps "Apply Code" at ticket checkout.
     * Uses a Firestore transaction so two simultaneous uses can't both succeed
     * if only one use remains.
     *
     * @param rawCode  The code string the user typed (case-insensitive match)
     * @param eventId  The event the user is buying a ticket for
     * @param callback Result callback — always called on the main thread
     */
    public void validateAndApply(@NonNull String rawCode,
                                 @NonNull String eventId,
                                 @NonNull ValidationCallback callback) {

        String normalizedCode = rawCode.trim().toUpperCase();

        // Find the code document by its code string
        db.collection(COLLECTION)
                .whereEqualTo("code", normalizedCode)
                .whereEqualTo("active", true)
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        callback.onInvalid("NOT_FOUND");
                        return;
                    }

                    DocumentSnapshot doc = querySnapshot.getDocuments().get(0);
                    DocumentReference ref = doc.getReference();

                    // Run a transaction to validate and increment atomically
                    db.runTransaction((Transaction.Function<DiscountCode>) transaction -> {
                                DocumentSnapshot fresh = transaction.get(ref);
                                DiscountCode code = fresh.toObject(DiscountCode.class);

                                if (code == null) throw new RuntimeException("NOT_FOUND");

                                // Check expiry
                                if (code.getExpiresAt() != null && code.getExpiresAt().before(new Date())) {
                                    throw new RuntimeException("EXPIRED");
                                }

                                // Check max uses (-1 = unlimited)
                                if (code.getMaxUses() != -1 && code.getCurrentUses() >= code.getMaxUses()) {
                                    throw new RuntimeException("MAX_USES_REACHED");
                                }

                                // Check event scope
                                if (code.getEventId() != null && !code.getEventId().equals(eventId)) {
                                    throw new RuntimeException("WRONG_EVENT");
                                }

                                // All checks passed — increment usage
                                transaction.update(ref, "currentUses", FieldValue.increment(1));
                                return code;

                            }).addOnSuccessListener(callback::onValid)
                            .addOnFailureListener(e -> {
                                String msg = e.getMessage();
                                if ("NOT_FOUND".equals(msg) || "EXPIRED".equals(msg)
                                        || "MAX_USES_REACHED".equals(msg) || "WRONG_EVENT".equals(msg)) {
                                    callback.onInvalid(msg);
                                } else {
                                    callback.onError(e);
                                }
                            });
                })
                .addOnFailureListener(callback::onError);
    }

    // ── Deactivate ────────────────────────────────────────────────────────────

    /**
     * Soft-deletes a code by setting active = false.
     * Never hard-delete codes — you need the history for audit.
     */
    public Task<Void> deactivateCode(@NonNull String codeId) {
        return db.collection(COLLECTION).document(codeId)
                .update("active", false);
    }
}
