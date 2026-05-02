package com.example.seprojectpart3;

import com.google.firebase.Timestamp;

/**
 * Model: proof_submissions collection document.
 * Represents a user's payment proof screenshot submission.
 *
 * State machine: pending → approved | rejected
 * Used by:
 *   - M2 (USR-B): creates record with status="pending"
 *   - M4 (ORG-B): reads pending submissions for organizer review
 *   - M4 (ORG-C): transitions status to approved/rejected
 *   - M2 (USR-C): reads status for user-facing ticket status screen
 */
public class ProofSubmission {

    // Status constants
    public static final String STATUS_PENDING  = "pending";
    public static final String STATUS_APPROVED = "approved";
    public static final String STATUS_REJECTED = "rejected";

    private String id;               // Firestore document ID
    private String eventId;
    private String userId;
    private String userName;         // Cached for organizer display
    private String userEmail;        // Cached for organizer display
    private String organizerId;
    private String fileId;           // Reference to files collection
    private String proofImageUrl;    // Download URL of proof screenshot
    private String status;           // "pending" | "approved" | "rejected"
    private Timestamp submittedAt;
    private Timestamp reviewedAt;
    private String reviewedBy;
    private String rejectionReason;
    private String idempotencyKey;   // "{eventId}_{userId}" — prevents duplicate submissions
    private String ticketId;         // Set after TKT-A generates ticket on approval

    // Required empty constructor for Firestore deserialization
    public ProofSubmission() {}

    public ProofSubmission(String eventId, String userId, String userName,
                           String userEmail, String organizerId,
                           String fileId, String proofImageUrl) {
        this.eventId = eventId;
        this.userId = userId;
        this.userName = userName;
        this.userEmail = userEmail;
        this.organizerId = organizerId;
        this.fileId = fileId;
        this.proofImageUrl = proofImageUrl;
        this.status = STATUS_PENDING;
        this.idempotencyKey = eventId + "_" + userId;
        this.ticketId = null;
        this.reviewedAt = null;
        this.reviewedBy = null;
        this.rejectionReason = null;
    }

    // ─── Getters ─────────────────────────────────────────────────────

    public String getId() { return id; }
    public String getEventId() { return eventId; }
    public String getUserId() { return userId; }
    public String getUserName() { return userName; }
    public String getUserEmail() { return userEmail; }
    public String getOrganizerId() { return organizerId; }
    public String getFileId() { return fileId; }
    public String getProofImageUrl() { return proofImageUrl; }
    public String getStatus() { return status; }
    public Timestamp getSubmittedAt() { return submittedAt; }
    public Timestamp getReviewedAt() { return reviewedAt; }
    public String getReviewedBy() { return reviewedBy; }
    public String getRejectionReason() { return rejectionReason; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public String getTicketId() { return ticketId; }

    // ─── Setters ─────────────────────────────────────────────────────

    public void setId(String id) { this.id = id; }
    public void setEventId(String eventId) { this.eventId = eventId; }
    public void setUserId(String userId) { this.userId = userId; }
    public void setUserName(String userName) { this.userName = userName; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }
    public void setOrganizerId(String organizerId) { this.organizerId = organizerId; }
    public void setFileId(String fileId) { this.fileId = fileId; }
    public void setProofImageUrl(String proofImageUrl) { this.proofImageUrl = proofImageUrl; }
    public void setStatus(String status) { this.status = status; }
    public void setSubmittedAt(Timestamp submittedAt) { this.submittedAt = submittedAt; }
    public void setReviewedAt(Timestamp reviewedAt) { this.reviewedAt = reviewedAt; }
    public void setReviewedBy(String reviewedBy) { this.reviewedBy = reviewedBy; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
    public void setTicketId(String ticketId) { this.ticketId = ticketId; }

    // ─── Helpers ─────────────────────────────────────────────────────

    public boolean isPending()  { return STATUS_PENDING.equals(status); }
    public boolean isApproved() { return STATUS_APPROVED.equals(status); }
    public boolean isRejected() { return STATUS_REJECTED.equals(status); }
}
