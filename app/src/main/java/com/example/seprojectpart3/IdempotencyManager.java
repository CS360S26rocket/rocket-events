package com.example.seprojectpart3;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.Transaction;

import java.util.HashMap;
import java.util.Map;

/**
 * Story #34 — Duplicate ticket prevention (idempotency implementation)
 * M3 · Sprint 4
 *
 * Implements the design from Sprint 3's #34 design doc.
 *
 * Uses Firestore transactions with an idempotency key
 * (= proof_submission ID) to guarantee exactly-once ticket generation
 * even under concurrent approval taps.
 *
 * Integration:
 *   - Called by TKT-A (TicketGenerationService.generateTicket)
 *   - Called indirectly from M4's ORG-C approval handler
 */
public class IdempotencyManager {

    private static final String COLLECTION_TICKETS = "tickets";
    private static final String FIELD_IDEMPOTENCY_KEY = "idempotencyKey";

    private final FirebaseFirestore db;

    public IdempotencyManager() {
        this.db = FirebaseFirestore.getInstance();
    }

    public IdempotencyManager(FirebaseFirestore db) {
        this.db = db;
    }

    // ── Callback ────────────────────────────────────────────────
    public interface OnIdempotentResultListener {
        void onTicketReady(String ticketId, boolean wasExisting);
        void onFailure(String errorMessage);
    }

    /**
     * Generate a ticket if one does not already exist for this submission.
     *
     * Idempotency guarantee:
     *   - Query tickets where idempotencyKey == submissionId
     *   - If found → return existing ticket (no duplicate)
     *   - If not found → create new ticket document
     *
     * This uses a two-step approach:
     *   1. Check for existing ticket (fast path)
     *   2. If none, use a transaction for atomic create
     *
     * @param submissionId  The proof_submission document ID (idempotency key)
     * @param userId        The user who paid
     * @param eventId       The event being ticketed
     * @param ticketTier    Ticket tier (general, VIP, early_bird)
     * @param qrCodeData    Pre-generated QR data string
     * @param listener      Callback
     */
    public void generateTicketIdempotent(String submissionId, String userId,
                                         String eventId, String ticketTier,
                                         String qrCodeData,
                                         OnIdempotentResultListener listener) {
        if (submissionId == null || submissionId.isEmpty()) {
            listener.onFailure("Submission ID is required for idempotency");
            return;
        }

        // Step 1: Check if ticket already exists (fast path)
        db.collection(COLLECTION_TICKETS)
                .whereEqualTo(FIELD_IDEMPOTENCY_KEY, submissionId)
                .limit(1)
                .get()
                .addOnSuccessListener(existingTickets -> {
                    if (!existingTickets.isEmpty()) {
                        // Ticket already exists — return it (idempotent)
                        String existingId = existingTickets.getDocuments()
                                .get(0).getId();
                        listener.onTicketReady(existingId, true);
                        return;
                    }

                    // Step 2: No existing ticket — create atomically
                    createTicketAtomic(submissionId, userId, eventId,
                            ticketTier, qrCodeData, listener);
                })
                .addOnFailureListener(e ->
                        listener.onFailure("Idempotency check failed: " + e.getMessage()));
    }

    /**
     * Atomic ticket creation using Firestore transaction.
     * Double-checks that no ticket was created between our query and
     * this write (handles the race window from Step 1).
     */
    private void createTicketAtomic(String submissionId, String userId,
                                    String eventId, String ticketTier,
                                    String qrCodeData,
                                    OnIdempotentResultListener listener) {
        // Use a deterministic document ID based on submissionId
        // This provides a secondary idempotency guarantee via Firestore's
        // document-level locking
        String ticketDocId = "ticket_" + submissionId;

        db.runTransaction((Transaction.Function<String>) transaction -> {
            // Read the target document inside the transaction
            var ticketRef = db.collection(COLLECTION_TICKETS).document(ticketDocId);
            var snapshot = transaction.get(ticketRef);

            if (snapshot.exists()) {
                // Another concurrent approval already created this ticket
                return ticketDocId;
            }

            // Create the ticket
            Map<String, Object> ticketData = new HashMap<>();
            ticketData.put(FIELD_IDEMPOTENCY_KEY, submissionId);
            ticketData.put("userId", userId);
            ticketData.put("eventId", eventId);
            ticketData.put("ticketTier", ticketTier);
            ticketData.put("qrCodeData", qrCodeData);
            ticketData.put("status", "active");
            ticketData.put("createdAt",
                    com.google.firebase.firestore.FieldValue.serverTimestamp());

            transaction.set(ticketRef, ticketData);
            return ticketDocId;
        }).addOnSuccessListener(ticketId ->
                listener.onTicketReady(ticketId, false)
        ).addOnFailureListener(e ->
                listener.onFailure("Ticket creation failed: " + e.getMessage()));
    }

    /**
     * Verify that an approval action is valid (status is still "pending").
     * Used by ORG-C before triggering TKT-A.
     *
     * @param submissionId  The proof_submission to verify
     * @param listener      Callback with current status
     */
    public void verifyApprovalEligibility(String submissionId,
                                          OnEligibilityListener listener) {
        db.collection("proof_submissions").document(submissionId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        listener.onResult(false, "Submission not found");
                        return;
                    }
                    String status = doc.getString("status");
                    if ("pending".equals(status)) {
                        listener.onResult(true, "Eligible for approval");
                    } else {
                        listener.onResult(false,
                                "Already processed (status: " + status + ")");
                    }
                })
                .addOnFailureListener(e ->
                        listener.onResult(false,
                                "Failed to verify: " + e.getMessage()));
    }

    public interface OnEligibilityListener {
        void onResult(boolean eligible, String message);
    }
}
