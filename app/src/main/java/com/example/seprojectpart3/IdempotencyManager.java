/*
 * This file defines IdempotencyManager, a helper manager used by the Scene app.
 * It contains idempotency key tracking so repeated actions do not create duplicate records.
 * Its functions include generateTicketIdempotent, createTicketAtomic, verifyApprovalEligibility to load data, handle user actions, validate input, and save results.
 * It connects this feature to the Scene app's UI, data, navigation, and verification flow.
 */

package com.example.seprojectpart3;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.Transaction;

import java.util.HashMap;
import java.util.Map;















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

    
    public interface OnIdempotentResultListener {
        void onTicketReady(String ticketId, boolean wasExisting);
        void onFailure(String errorMessage);
    }

    


















    public void generateTicketIdempotent(String submissionId, String userId,
                                         String eventId, String ticketTier,
                                         String qrCodeData,
                                         OnIdempotentResultListener listener) {
        if (submissionId == null || submissionId.isEmpty()) {
            listener.onFailure("Submission ID is required for idempotency");
            return;
        }

        
        db.collection(COLLECTION_TICKETS)
                .whereEqualTo(FIELD_IDEMPOTENCY_KEY, submissionId)
                .limit(1)
                .get()
                .addOnSuccessListener(existingTickets -> {
                    if (!existingTickets.isEmpty()) {
                        
                        String existingId = existingTickets.getDocuments()
                                .get(0).getId();
                        listener.onTicketReady(existingId, true);
                        return;
                    }

                    
                    createTicketAtomic(submissionId, userId, eventId,
                            ticketTier, qrCodeData, listener);
                })
                .addOnFailureListener(e ->
                        listener.onFailure("Idempotency check failed: " + e.getMessage()));
    }

    




    private void createTicketAtomic(String submissionId, String userId,
                                    String eventId, String ticketTier,
                                    String qrCodeData,
                                    OnIdempotentResultListener listener) {
        
        
        
        String ticketDocId = "ticket_" + submissionId;

        db.runTransaction((Transaction.Function<String>) transaction -> {
            
            var ticketRef = db.collection(COLLECTION_TICKETS).document(ticketDocId);
            var snapshot = transaction.get(ticketRef);

            if (snapshot.exists()) {
                
                return ticketDocId;
            }

            
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
