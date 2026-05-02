package com.example.seprojectpart3;

import android.util.Log;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.Transaction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Proof Submission Repository — M4 Sprint 4
 *
 * Contains:
 *   ORG-B (Day 1): Organizer views list of pending proof submissions
 *   ORG-C (Day 2): Organizer approves or rejects a proof submission
 *
 * ORG-C uses a Firestore transaction to handle race conditions:
 *   - Checks status is still "pending" inside transaction
 *   - On approve: updates status → calls M3's TicketRepository.generateTicket()
 *   - On reject: updates status + sets rejection reason
 *
 * ORG-C is HIGH RISK because:
 *   1. Concurrent approvals (organizer double-taps) — handled by transaction
 *   2. Must coordinate with M3's TKT-A (ticket generation) and #34 (idempotency)
 */
public class ProofSubmissionRepository {

    private static final String TAG = "ProofSubmissionRepo";
    private static final String COLLECTION = "proof_submissions";

    private final FirebaseFirestore db;
    private final FirebaseAuth auth;
    private final TicketRepository ticketRepository;  // M3's TKT-A

    public ProofSubmissionRepository() {
        this.db = FirebaseFirestore.getInstance();
        this.auth = FirebaseAuth.getInstance();
        this.ticketRepository = new TicketRepository();
    }

    // Constructor for testing — inject dependencies
    public ProofSubmissionRepository(FirebaseFirestore db, FirebaseAuth auth,
                                     TicketRepository ticketRepository) {
        this.db = db;
        this.auth = auth;
        this.ticketRepository = ticketRepository;
    }

    // ═════════════════════════════════════════════════════════════════
    // ORG-B: View pending proof submissions for an event (Day 1)
    // ═════════════════════════════════════════════════════════════════

    /**
     * Fetch all pending proof submissions for a specific event.
     * Filtered by organizerId to ensure only the event owner sees them.
     * Ordered by submission time (oldest first — FIFO review).
     */
    public void getPendingSubmissions(String eventId,
                                      OnSuccessListener<List<ProofSubmission>> onSuccess,
                                      OnFailureListener onFailure) {

        String organizerId = getCurrentUserId();
        if (organizerId == null) {
            onFailure.onFailure(new Exception("User not authenticated."));
            return;
        }

        db.collection(COLLECTION)
                .whereEqualTo("eventId", eventId)
                .whereEqualTo("organizerId", organizerId)
                .whereEqualTo("status", ProofSubmission.STATUS_PENDING)
                .orderBy("submittedAt", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<ProofSubmission> submissions = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        ProofSubmission submission = doc.toObject(ProofSubmission.class);
                        if (submission != null) {
                            submission.setId(doc.getId());
                            submissions.add(submission);
                        }
                    }
                    onSuccess.onSuccess(submissions);
                })
                .addOnFailureListener(onFailure);
    }

    /**
     * Fetch ALL submissions for an event (all statuses).
     * Used by organizer to see full history — pending, approved, rejected.
     */
    public void getAllSubmissions(String eventId,
                                  OnSuccessListener<List<ProofSubmission>> onSuccess,
                                  OnFailureListener onFailure) {

        String organizerId = getCurrentUserId();
        if (organizerId == null) {
            onFailure.onFailure(new Exception("User not authenticated."));
            return;
        }

        db.collection(COLLECTION)
                .whereEqualTo("eventId", eventId)
                .whereEqualTo("organizerId", organizerId)
                .orderBy("submittedAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<ProofSubmission> submissions = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        ProofSubmission submission = doc.toObject(ProofSubmission.class);
                        if (submission != null) {
                            submission.setId(doc.getId());
                            submissions.add(submission);
                        }
                    }
                    onSuccess.onSuccess(submissions);
                })
                .addOnFailureListener(onFailure);
    }

    /**
     * Get count of pending submissions for an event.
     * Used to show badge count on organizer dashboard.
     */
    public void getPendingCount(String eventId,
                                OnSuccessListener<Integer> onSuccess,
                                OnFailureListener onFailure) {

        String organizerId = getCurrentUserId();
        if (organizerId == null) {
            onFailure.onFailure(new Exception("User not authenticated."));
            return;
        }

        db.collection(COLLECTION)
                .whereEqualTo("eventId", eventId)
                .whereEqualTo("organizerId", organizerId)
                .whereEqualTo("status", ProofSubmission.STATUS_PENDING)
                .get()
                .addOnSuccessListener(querySnapshot ->
                        onSuccess.onSuccess(querySnapshot.size()))
                .addOnFailureListener(onFailure);
    }

    // ═════════════════════════════════════════════════════════════════
    // ORG-C: Approve a proof submission (Day 2) — HIGH RISK
    // ═════════════════════════════════════════════════════════════════

    /**
     * Approve a proof submission using a Firestore transaction.
     *
     * Transaction guarantees:
     *   1. Read current status — must be "pending"
     *   2. Update status to "approved"
     *   3. On success, call M3's TKT-A to generate QR ticket
     *
     * Race condition: if organizer double-taps approve, the second tap
     * finds status != "pending" inside the transaction and returns an error.
     * M3's #34 idempotency key provides a second safety net on ticket generation.
     *
     * @param submissionId  Firestore document ID of the proof_submission
     * @param onSuccess     returns the generated ticketId
     * @param onFailure     returns error (including "already processed" for races)
     */
    public void approveSubmission(String submissionId,
                                  OnSuccessListener<String> onSuccess,
                                  OnFailureListener onFailure) {

        String reviewerId = getCurrentUserId();
        if (reviewerId == null) {
            onFailure.onFailure(new Exception("User not authenticated."));
            return;
        }

        db.runTransaction((Transaction.Function<ProofSubmission>) transaction -> {
            // Step 1: Read current state inside transaction
            DocumentSnapshot snapshot = transaction.get(
                    db.collection(COLLECTION).document(submissionId)
            );

            if (!snapshot.exists()) {
                throw new RuntimeException("Submission not found.");
            }

            String currentStatus = snapshot.getString("status");

            // Step 2: Guard — only transition from "pending"
            if (!ProofSubmission.STATUS_PENDING.equals(currentStatus)) {
                throw new RuntimeException(
                        "This submission has already been " + currentStatus + "."
                );
            }

            // Step 3: Verify reviewer is the organizer for this event
            String organizerId = snapshot.getString("organizerId");
            if (!reviewerId.equals(organizerId)) {
                throw new RuntimeException(
                        "Only the event organizer can approve submissions."
                );
            }

            // Step 4: Update status to approved
            Map<String, Object> updates = new HashMap<>();
            updates.put("status", ProofSubmission.STATUS_APPROVED);
            updates.put("reviewedAt", FieldValue.serverTimestamp());
            updates.put("reviewedBy", reviewerId);

            transaction.update(
                    db.collection(COLLECTION).document(submissionId),
                    updates
            );

            // Return the submission data for ticket generation
            ProofSubmission submission = snapshot.toObject(ProofSubmission.class);
            if (submission != null) {
                submission.setId(submissionId);
                submission.setStatus(ProofSubmission.STATUS_APPROVED);
            }
            return submission;

        }).addOnSuccessListener(approvedSubmission -> {
            Log.d(TAG, "Submission " + submissionId + " approved. Triggering TKT-A...");

            // Step 5: Trigger M3's TKT-A — generate QR ticket
            // This is the agreed interface: generateTicket(submissionId) → ticketId
            ticketRepository.generateTicket(
                    submissionId,
                    ticketId -> {
                        Log.d(TAG, "Ticket generated: " + ticketId);

                        // Save ticketId back to the submission record
                        db.collection(COLLECTION).document(submissionId)
                                .update("ticketId", ticketId);

                        onSuccess.onSuccess(ticketId);
                    },
                    error -> {
                        // Ticket generation failed — log but don't revert approval
                        // M3's idempotency key allows safe retry
                        Log.e(TAG, "Ticket generation failed for " + submissionId, error);
                        onFailure.onFailure(new Exception(
                                "Payment approved but ticket generation failed. "
                                        + "Please try generating the ticket again."
                        ));
                    }
            );

        }).addOnFailureListener(error -> {
            Log.e(TAG, "Approval transaction failed for " + submissionId, error);
            onFailure.onFailure(error);
        });
    }

    // ═════════════════════════════════════════════════════════════════
    // ORG-C: Reject a proof submission (Day 2)
    // ═════════════════════════════════════════════════════════════════

    /**
     * Reject a proof submission with a reason.
     * Also uses a transaction to prevent race conditions with approve.
     *
     * After rejection, the user can submit a new proof (new submission record).
     *
     * @param submissionId    Firestore document ID
     * @param rejectionReason Organizer's reason for rejection
     */
    public void rejectSubmission(String submissionId,
                                 String rejectionReason,
                                 OnSuccessListener<Void> onSuccess,
                                 OnFailureListener onFailure) {

        String reviewerId = getCurrentUserId();
        if (reviewerId == null) {
            onFailure.onFailure(new Exception("User not authenticated."));
            return;
        }

        db.runTransaction((Transaction.Function<Void>) transaction -> {
            DocumentSnapshot snapshot = transaction.get(
                    db.collection(COLLECTION).document(submissionId)
            );

            if (!snapshot.exists()) {
                throw new RuntimeException("Submission not found.");
            }

            String currentStatus = snapshot.getString("status");
            if (!ProofSubmission.STATUS_PENDING.equals(currentStatus)) {
                throw new RuntimeException(
                        "This submission has already been " + currentStatus + "."
                );
            }

            String organizerId = snapshot.getString("organizerId");
            if (!reviewerId.equals(organizerId)) {
                throw new RuntimeException(
                        "Only the event organizer can reject submissions."
                );
            }

            Map<String, Object> updates = new HashMap<>();
            updates.put("status", ProofSubmission.STATUS_REJECTED);
            updates.put("reviewedAt", FieldValue.serverTimestamp());
            updates.put("reviewedBy", reviewerId);
            updates.put("rejectionReason",
                    rejectionReason != null ? rejectionReason : "No reason provided.");

            transaction.update(
                    db.collection(COLLECTION).document(submissionId),
                    updates
            );

            return null;

        }).addOnSuccessListener(result -> {
            Log.d(TAG, "Submission " + submissionId + " rejected.");
            // TODO Sprint 5: fire rejection notification to user here
            onSuccess.onSuccess(null);

        }).addOnFailureListener(error -> {
            Log.e(TAG, "Rejection transaction failed for " + submissionId, error);
            onFailure.onFailure(error);
        });
    }

    // ─── Helper ──────────────────────────────────────────────────────

    private String getCurrentUserId() {
        if (auth.getCurrentUser() != null) {
            return auth.getCurrentUser().getUid();
        }
        return null;
    }
}
