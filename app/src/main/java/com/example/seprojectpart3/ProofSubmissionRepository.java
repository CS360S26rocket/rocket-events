package com.example.seprojectpart3;

import android.util.Log;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
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
import java.util.UUID;

public class ProofSubmissionRepository {

    private static final String TAG = "ProofSubmissionRepo";
    private static final String COLLECTION = "proof_submissions";

    private final FirebaseFirestore db;
    private final FirebaseAuth auth;

    public ProofSubmissionRepository() {
        this.db = FirebaseFirestore.getInstance();
        this.auth = FirebaseAuth.getInstance();
    }

    public ProofSubmissionRepository(FirebaseFirestore db,
                                     FirebaseAuth auth,
                                     TicketRepository ticketRepository) {
        this.db = db;
        this.auth = auth;
    }

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
                .addOnSuccessListener(querySnapshot -> onSuccess.onSuccess(querySnapshot.size()))
                .addOnFailureListener(onFailure);
    }

    public void approveSubmission(String submissionId,
                                  OnSuccessListener<String> onSuccess,
                                  OnFailureListener onFailure) {

        String reviewerId = getCurrentUserId();
        if (reviewerId == null) {
            onFailure.onFailure(new Exception("User not authenticated."));
            return;
        }

        db.runTransaction((Transaction.Function<String>) transaction -> {
            DocumentSnapshot snapshot = transaction.get(
                    db.collection(COLLECTION).document(submissionId)
            );

            if (!snapshot.exists()) {
                throw new RuntimeException("Submission not found.");
            }

            String currentStatus = snapshot.getString("status");
            if (!ProofSubmission.STATUS_PENDING.equals(currentStatus)) {
                throw new RuntimeException("This submission has already been " + currentStatus + ".");
            }

            String organizerId = snapshot.getString("organizerId");
            if (!reviewerId.equals(organizerId)) {
                throw new RuntimeException("Only the event organizer can approve submissions.");
            }

            String existingTicketId = snapshot.getString("ticketId");
            String ticketId = existingTicketId == null || existingTicketId.trim().isEmpty()
                    ? UUID.randomUUID().toString()
                    : existingTicketId;

            String eventId = snapshot.getString("eventId");
            String userId = snapshot.getString("userId");
            String userEmail = snapshot.getString("userEmail");
            String userName = snapshot.getString("userName");
            String fileId = snapshot.getString("fileId");
            String proofImageUrl = snapshot.getString("proofImageUrl");

            Map<String, Object> submissionUpdates = new HashMap<>();
            submissionUpdates.put("status", ProofSubmission.STATUS_APPROVED);
            submissionUpdates.put("reviewedAt", FieldValue.serverTimestamp());
            submissionUpdates.put("reviewedBy", reviewerId);
            submissionUpdates.put("ticketId", ticketId);

            transaction.update(
                    db.collection(COLLECTION).document(submissionId),
                    submissionUpdates
            );

            Map<String, Object> ticket = new HashMap<>();
            ticket.put("ticketId", ticketId);
            ticket.put("proofSubmissionId", submissionId);
            ticket.put("eventId", eventId);
            ticket.put("userId", userId);
            ticket.put("userEmail", userEmail);
            ticket.put("attendeeName", userName);
            ticket.put("status", "valid");
            ticket.put("source", "manual_payment_approval");
            ticket.put("fileId", fileId);
            ticket.put("proofImageUrl", proofImageUrl);
            ticket.put("createdAt", FieldValue.serverTimestamp());

            transaction.set(db.collection("tickets").document(ticketId), ticket);

            return ticketId;

        }).addOnSuccessListener(ticketId -> {
            Log.d(TAG, "Submission " + submissionId + " approved. Ticket generated: " + ticketId);
            onSuccess.onSuccess(ticketId);
        }).addOnFailureListener(error -> {
            Log.e(TAG, "Approval transaction failed for " + submissionId, error);
            onFailure.onFailure(error);
        });
    }

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
                throw new RuntimeException("This submission has already been " + currentStatus + ".");
            }

            String organizerId = snapshot.getString("organizerId");
            if (!reviewerId.equals(organizerId)) {
                throw new RuntimeException("Only the event organizer can reject submissions.");
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
            onSuccess.onSuccess(null);
        }).addOnFailureListener(error -> {
            Log.e(TAG, "Rejection transaction failed for " + submissionId, error);
            onFailure.onFailure(error);
        });
    }

    private String getCurrentUserId() {
        if (auth.getCurrentUser() != null) {
            return auth.getCurrentUser().getUid();
        }
        return null;
    }
}
