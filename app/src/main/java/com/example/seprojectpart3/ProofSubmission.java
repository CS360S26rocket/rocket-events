/*
 * This file defines ProofSubmission, a supporting Java class used by the Scene app.
 * It contains the legacy payment proof data model used by proof review screens.
 * Its functions include getId, getEventId, getUserId, getUserName to load data, handle user actions, validate input, and save results.
 * It connects this feature to the Scene app's UI, data, navigation, and verification flow.
 */

package com.example.seprojectpart3;

import com.google.firebase.Timestamp;












public class ProofSubmission {

    
    public static final String STATUS_PENDING  = "pending";
    public static final String STATUS_APPROVED = "approved";
    public static final String STATUS_REJECTED = "rejected";

    private String id;               
    private String eventId;
    private String userId;
    private String userName;         
    private String userEmail;        
    private String organizerId;
    private String fileId;           
    private String proofImageUrl;    
    private String status;           
    private Timestamp submittedAt;
    private Timestamp reviewedAt;
    private String reviewedBy;
    private String rejectionReason;
    private String idempotencyKey;   
    private String ticketId;         

    
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

    

    public boolean isPending()  { return STATUS_PENDING.equals(status); }
    public boolean isApproved() { return STATUS_APPROVED.equals(status); }
    public boolean isRejected() { return STATUS_REJECTED.equals(status); }
}
