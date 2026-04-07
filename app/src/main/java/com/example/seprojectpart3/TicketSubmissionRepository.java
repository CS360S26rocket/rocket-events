package com.example.seprojectpart3;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Transaction;

import java.util.HashMap;
import java.util.Map;

/**
 * Stories #15 and #50.
 *
 * #15 — Prevent duplicate form submissions without payment.
 *       Uses a composite document ID (eventId_userId) as the natural dedup key.
 *       Firestore's set-with-merge won't help here — we need a transaction that
 *       checks existence BEFORE writing.
 *
 * #50 — Auto-close ticket sales when event capacity is reached.
 *       Called after every confirmed ticket sale. Reads current soldCount vs
 *       maxCapacity and flips ticketSalesOpen = false if capacity is hit.
 *
 * IMPORTANT — coordinate field names with M2 before merging:
 *   Event doc fields:  maxCapacity, ticketsSold, ticketSalesOpen
 *   Confirm these match what M2 defined in the events collection (#10).
 */
public class TicketSubmissionRepository {

    private static final String TAG = "TicketSubmissionRepository";
    private static final String SUBMISSIONS_COL = "ticketSubmissions";
    private static final String EVENTS_COL = "events";

    // ── Field name constants — sync these with M2's event schema ──────────────
    // If M2 used different names, change only these constants.
    private static final String FIELD_MAX_CAPACITY   = "capacity"; // confirmed with M2
    private static final String FIELD_TICKETS_SOLD   = "ticketsSold";
    private static final String FIELD_SALES_OPEN     = "ticketSalesOpen";

    private final FirebaseFirestore db;

    public TicketSubmissionRepository() {
        this.db = FirebaseFirestore.getInstance();
    }

    // ── Story #15 ─────────────────────────────────────────────────────────────

    public interface SubmissionCallback {
        void onSuccess(String submissionId);
        void onAlreadySubmitted();   // user already has a pending/confirmed submission
        void onError(Exception e);
    }

    /**
     * Story #15 — Submit a ticket request, preventing duplicates.
     *
     * The document ID is "{eventId}_{userId}" — a deterministic composite key.
     * The Firestore transaction checks if this document already exists before
     * writing, so two rapid taps of the submit button cannot both go through.
     *
     * formData should contain whatever fields the ticket form collects
     * (name, tier, quantity, discountCodeId, etc.).
     *
     * @param eventId  The event being registered for
     * @param userId   The currently logged-in user's UID (from FirebaseAuth)
     * @param formData The form fields to persist
     * @param callback Result delivered on background thread — post to main if updating UI
     */
    public void submitTicketRequest(@NonNull String eventId,
                                    @NonNull String userId,
                                    @NonNull Map<String, Object> formData,
                                    @NonNull SubmissionCallback callback) {

        // Composite ID — naturally prevents one user submitting twice for same event
        String submissionId = eventId + "_" + userId;
        DocumentReference ref = db.collection(SUBMISSIONS_COL).document(submissionId);

        db.runTransaction((Transaction.Function<Void>) transaction -> {
            DocumentSnapshot existing = transaction.get(ref);

            if (existing.exists()) {
                // Document already exists → duplicate submission
                throw new FirebaseFirestoreException(
                        "ALREADY_SUBMITTED",
                        FirebaseFirestoreException.Code.ALREADY_EXISTS
                );
            }

            // Build the submission document
            Map<String, Object> submission = new HashMap<>(formData);
            submission.put("eventId", eventId);
            submission.put("userId", userId);
            submission.put("status", "pending");
            submission.put("createdAt", FieldValue.serverTimestamp());

            transaction.set(ref, submission);
            return null;

        }).addOnSuccessListener(unused -> {
            Log.d(TAG, "Ticket submitted: " + submissionId);
            callback.onSuccess(submissionId);
        }).addOnFailureListener(e -> {
            if (e instanceof FirebaseFirestoreException) {
                FirebaseFirestoreException ffe = (FirebaseFirestoreException) e;
                if (ffe.getCode() == FirebaseFirestoreException.Code.ALREADY_EXISTS) {
                    callback.onAlreadySubmitted();
                    return;
                }
            }
            Log.e(TAG, "Submission failed", e);
            callback.onError(e);
        });
    }

    /**
     * Updates the status of an existing submission.
     * Call this after payment succeeds ("confirmed") or fails ("failed").
     *
     * @param eventId  Same event ID used during submission
     * @param userId   Same user ID used during submission
     * @param status   "confirmed" | "failed" | "cancelled"
     */
    public Task<Void> updateSubmissionStatus(@NonNull String eventId,
                                             @NonNull String userId,
                                             @NonNull String status) {
        String submissionId = eventId + "_" + userId;
        return db.collection(SUBMISSIONS_COL).document(submissionId)
                .update("status", status,
                        "updatedAt", FieldValue.serverTimestamp());
    }

    // ── Story #50 ─────────────────────────────────────────────────────────────

    public interface SalesStatusCallback {
        void onSalesClosed();           // capacity reached — sales just closed
        void onCapacityRemaining(long remaining); // sales still open
        void onNoCapacitySet();         // event has no capacity limit
        void onError(Exception e);
    }

    /**
     * Story #50 — Auto-close ticket sales when capacity is reached.
     *
     * Call this AFTER every confirmed ticket sale (i.e., after your payment
     * confirmation logic sets the submission status to "confirmed").
     *
     * Uses a transaction to atomically read the event state and close sales
     * if the sold count has reached maxCapacity.
     *
     * @param eventId  The event whose capacity to check
     * @param callback Result callback
     */
    public void checkAndCloseTicketSales(@NonNull String eventId,
                                         @NonNull SalesStatusCallback callback) {

        DocumentReference eventRef = db.collection(EVENTS_COL).document(eventId);

        db.runTransaction((Transaction.Function<Long>) transaction -> {
            DocumentSnapshot eventDoc = transaction.get(eventRef);

            if (!eventDoc.exists()) {
                throw new RuntimeException("EVENT_NOT_FOUND");
            }

            // If maxCapacity is not set, there's nothing to close
            if (!eventDoc.contains(FIELD_MAX_CAPACITY) || eventDoc.getLong(FIELD_MAX_CAPACITY) == null) {
                return -1L; // sentinel: no capacity set
            }

            long maxCapacity = eventDoc.getLong(FIELD_MAX_CAPACITY);
            long ticketsSold  = eventDoc.getLong(FIELD_TICKETS_SOLD) != null
                    ? eventDoc.getLong(FIELD_TICKETS_SOLD) : 0L;

            if (ticketsSold >= maxCapacity) {
                // Capacity reached — close sales atomically in the same transaction
                transaction.update(eventRef,
                        FIELD_SALES_OPEN, false,
                        "salesClosedAt", FieldValue.serverTimestamp(),
                        "salesClosedReason", "CAPACITY_REACHED"
                );
                return 0L; // 0 remaining
            }

            return maxCapacity - ticketsSold;

        }).addOnSuccessListener(remaining -> {
            if (remaining == -1L) {
                callback.onNoCapacitySet();
            } else if (remaining == 0L) {
                Log.d(TAG, "Ticket sales closed for event " + eventId + " — capacity reached");
                callback.onSalesClosed();
            } else {
                callback.onCapacityRemaining(remaining);
            }
        }).addOnFailureListener(e -> {
            Log.e(TAG, "checkAndCloseTicketSales failed for event " + eventId, e);
            callback.onError(e);
        });
    }
}