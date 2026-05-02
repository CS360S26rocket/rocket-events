package com.example.seprojectpart3;

import android.graphics.Bitmap;

import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Story TKT-A — Generate unique QR ticket on organizer approval
 * M3 · Sprint 4
 *
 * Triggered by M4's ORG-C approval handler via:
 *   TicketGenerationService.generateTicket(submissionId, callback)
 *
 * This story absorbs #7 (QR pass) — the QR code is generated here
 * as part of the approval flow instead of being a separate story.
 *
 * Uses IdempotencyManager (#34) to prevent duplicate tickets.
 *
 * Agreed interface with M4 (day 1):
 *   generate_ticket(proof_submission_id) → ticket_id
 */
public class TicketGenerationService {

    private static final String COLLECTION_SUBMISSIONS = "proof_submissions";
    private static final String COLLECTION_TICKETS = "tickets";
    private static final String COLLECTION_EVENTS = "events";

    private final FirebaseFirestore db;
    private final IdempotencyManager idempotencyManager;

    public TicketGenerationService() {
        this.db = FirebaseFirestore.getInstance();
        this.idempotencyManager = new IdempotencyManager();
    }

    public TicketGenerationService(FirebaseFirestore db,
                                   IdempotencyManager idempotencyManager) {
        this.db = db;
        this.idempotencyManager = idempotencyManager;
    }

    // ── Callback ────────────────────────────────────────────────
    public interface OnTicketGeneratedListener {
        void onSuccess(String ticketId, String qrCodeData);
        void onFailure(String errorMessage);
    }

    /**
     * Entry point — called by M4's ORG-C approval handler.
     *
     * Function signature agreed with M4 on Sprint 4 Day 1:
     *   generateTicket(proofSubmissionId) → ticketId
     *
     * Flow:
     *   1. Read the proof_submission to get userId, eventId, tier
     *   2. Generate unique QR code data
     *   3. Use IdempotencyManager to create ticket (exactly-once)
     *   4. Update proof_submission with ticketId reference
     *   5. Return ticketId
     *
     * @param proofSubmissionId  The approved submission ID
     * @param listener           Callback with ticket ID and QR data
     */
    public void generateTicket(String proofSubmissionId,
                               OnTicketGeneratedListener listener) {
        if (proofSubmissionId == null || proofSubmissionId.isEmpty()) {
            listener.onFailure("Proof submission ID is required");
            return;
        }

        // Step 1: Read submission details
        db.collection(COLLECTION_SUBMISSIONS).document(proofSubmissionId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        listener.onFailure("Submission not found");
                        return;
                    }

                    String userId = doc.getString("userId");
                    String eventId = doc.getString("eventId");
                    String ticketTier = doc.getString("ticketTier");

                    if (userId == null || eventId == null) {
                        listener.onFailure("Invalid submission data");
                        return;
                    }

                    if (ticketTier == null) ticketTier = "general";

                    // Step 2: Generate unique QR code data
                    String qrCodeData = generateQRCodeData(
                            proofSubmissionId, userId, eventId);

                    // Step 3: Use IdempotencyManager (exactly-once guarantee)
                    String finalTicketTier = ticketTier;
                    idempotencyManager.generateTicketIdempotent(
                            proofSubmissionId, userId, eventId,
                            ticketTier, qrCodeData,
                            new IdempotencyManager.OnIdempotentResultListener() {
                                @Override
                                public void onTicketReady(String ticketId,
                                                          boolean wasExisting) {
                                    if (wasExisting) {
                                        // Idempotent return — ticket already existed
                                        listener.onSuccess(ticketId, qrCodeData);
                                        return;
                                    }

                                    // Step 4: Link ticket back to submission
                                    linkTicketToSubmission(proofSubmissionId,
                                            ticketId);

                                    // Step 5: Decrement event capacity
                                    decrementEventCapacity(eventId);

                                    listener.onSuccess(ticketId, qrCodeData);
                                }

                                @Override
                                public void onFailure(String errorMessage) {
                                    listener.onFailure(errorMessage);
                                }
                            });
                })
                .addOnFailureListener(e ->
                        listener.onFailure(
                                "Failed to read submission: " + e.getMessage()));
    }

    /**
     * Generate a unique, non-guessable QR code data string.
     *
     * Format: CAMPUS-{eventId_short}-{hash}
     * The hash is a SHA-256 of submissionId + userId + eventId + UUID,
     * truncated to 12 characters for scannability.
     */
    private String generateQRCodeData(String submissionId, String userId,
                                      String eventId) {
        String raw = submissionId + "|" + userId + "|" + eventId
                + "|" + UUID.randomUUID().toString();

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(raw.getBytes());
            StringBuilder hex = new StringBuilder();
            for (int i = 0; i < 6; i++) { // 6 bytes = 12 hex chars
                hex.append(String.format("%02x", hash[i]));
            }

            String eventShort = eventId.length() > 6
                    ? eventId.substring(0, 6) : eventId;
            return "CAMPUS-" + eventShort.toUpperCase() + "-"
                    + hex.toString().toUpperCase();

        } catch (NoSuchAlgorithmException e) {
            // Fallback: UUID-based
            return "CAMPUS-" + UUID.randomUUID().toString()
                    .replace("-", "").substring(0, 16).toUpperCase();
        }
    }

    /**
     * Link the generated ticket back to the proof_submission document.
     */
    private void linkTicketToSubmission(String submissionId, String ticketId) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("ticketId", ticketId);
        updates.put("ticketGeneratedAt", FieldValue.serverTimestamp());

        db.collection(COLLECTION_SUBMISSIONS).document(submissionId)
                .update(updates);
    }

    /**
     * Decrement the event's available capacity by 1 after ticket generation.
     */
    private void decrementEventCapacity(String eventId) {
        db.collection(COLLECTION_EVENTS).document(eventId)
                .update("availableCapacity", FieldValue.increment(-1));
    }

    /**
     * Validate a ticket QR code at the event gate.
     * Reads the ticket document and verifies it is active.
     *
     * @param qrCodeData  The scanned QR data
     * @param eventId     The event being checked into
     * @param listener    Callback with validation result
     */
    public void validateTicketQR(String qrCodeData, String eventId,
                                 OnTicketValidatedListener listener) {
        db.collection(COLLECTION_TICKETS)
                .whereEqualTo("qrCodeData", qrCodeData)
                .whereEqualTo("eventId", eventId)
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        listener.onResult(false, "Invalid ticket");
                        return;
                    }

                    var doc = querySnapshot.getDocuments().get(0);
                    String status = doc.getString("status");

                    if ("active".equals(status)) {
                        // Mark as checked in
                        doc.getReference().update("status", "checked_in",
                                "checkedInAt", FieldValue.serverTimestamp());
                        listener.onResult(true, "Ticket valid — checked in!");
                    } else if ("checked_in".equals(status)) {
                        listener.onResult(false, "Ticket already used");
                    } else {
                        listener.onResult(false,
                                "Ticket status: " + status);
                    }
                })
                .addOnFailureListener(e ->
                        listener.onResult(false,
                                "Validation failed: " + e.getMessage()));
    }

    public interface OnTicketValidatedListener {
        void onResult(boolean valid, String message);
    }
}
