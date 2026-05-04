/*
 * This file defines TicketGenerationService, a service class used by the Scene app.
 * It contains ticket code, pass, and registration token generation.
 * Its functions include generateTicket, onTicketReady, onFailure, generateQRCodeData to load data, handle user actions, validate input, and save results.
 * It connects this feature to the Scene app's UI, data, navigation, and verification flow.
 */

package com.example.seprojectpart3;

import android.graphics.Bitmap;

import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
















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

    
    public interface OnTicketGeneratedListener {
        void onSuccess(String ticketId, String qrCodeData);
        void onFailure(String errorMessage);
    }

    















    public void generateTicket(String proofSubmissionId,
                               OnTicketGeneratedListener listener) {
        if (proofSubmissionId == null || proofSubmissionId.isEmpty()) {
            listener.onFailure("Proof submission ID is required");
            return;
        }

        
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

                    
                    String qrCodeData = generateQRCodeData(
                            proofSubmissionId, userId, eventId);

                    
                    String finalTicketTier = ticketTier;
                    idempotencyManager.generateTicketIdempotent(
                            proofSubmissionId, userId, eventId,
                            ticketTier, qrCodeData,
                            new IdempotencyManager.OnIdempotentResultListener() {
                                @Override
                                public void onTicketReady(String ticketId,
                                                          boolean wasExisting) {
                                    if (wasExisting) {
                                        
                                        listener.onSuccess(ticketId, qrCodeData);
                                        return;
                                    }

                                    
                                    linkTicketToSubmission(proofSubmissionId,
                                            ticketId);

                                    
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

    






    private String generateQRCodeData(String submissionId, String userId,
                                      String eventId) {
        String raw = submissionId + "|" + userId + "|" + eventId
                + "|" + UUID.randomUUID().toString();

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(raw.getBytes());
            StringBuilder hex = new StringBuilder();
            for (int i = 0; i < 6; i++) { 
                hex.append(String.format("%02x", hash[i]));
            }

            String eventShort = eventId.length() > 6
                    ? eventId.substring(0, 6) : eventId;
            return "CAMPUS-" + eventShort.toUpperCase() + "-"
                    + hex.toString().toUpperCase();

        } catch (NoSuchAlgorithmException e) {
            
            return "CAMPUS-" + UUID.randomUUID().toString()
                    .replace("-", "").substring(0, 16).toUpperCase();
        }
    }

    


    private void linkTicketToSubmission(String submissionId, String ticketId) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("ticketId", ticketId);
        updates.put("ticketGeneratedAt", FieldValue.serverTimestamp());

        db.collection(COLLECTION_SUBMISSIONS).document(submissionId)
                .update(updates);
    }

    


    private void decrementEventCapacity(String eventId) {
        db.collection(COLLECTION_EVENTS).document(eventId)
                .update("availableCapacity", FieldValue.increment(-1));
    }

    







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
