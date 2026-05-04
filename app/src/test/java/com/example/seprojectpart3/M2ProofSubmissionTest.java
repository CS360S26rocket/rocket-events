package com.example.seprojectpart3;

import static org.junit.Assert.*;

import org.junit.Test;

public class M2ProofSubmissionTest {

    @Test
    public void newProofSubmissionStartsPending() {
        ProofSubmission submission = new ProofSubmission(
                "event123",
                "user123",
                "Student Name",
                "student@example.com",
                "organizer123",
                "file123",
                "https://example.com/proof.jpg"
        );

        assertEquals(ProofSubmission.STATUS_PENDING, submission.getStatus());
        assertTrue(submission.isPending());
        assertFalse(submission.isApproved());
        assertFalse(submission.isRejected());
    }

    @Test
    public void proofSubmissionCreatesIdempotencyKeyFromEventAndUser() {
        ProofSubmission submission = new ProofSubmission(
                "event123",
                "user123",
                "Student Name",
                "student@example.com",
                "organizer123",
                "file123",
                "https://example.com/proof.jpg"
        );

        assertEquals("event123_user123", submission.getIdempotencyKey());
    }

    @Test
    public void proofSubmissionCanMoveToApproved() {
        ProofSubmission submission = new ProofSubmission();
        submission.setStatus(ProofSubmission.STATUS_APPROVED);

        assertTrue(submission.isApproved());
        assertFalse(submission.isPending());
        assertFalse(submission.isRejected());
    }

    @Test
    public void proofSubmissionCanMoveToRejectedWithReason() {
        ProofSubmission submission = new ProofSubmission();
        submission.setStatus(ProofSubmission.STATUS_REJECTED);
        submission.setRejectionReason("Payment screenshot is unclear.");

        assertTrue(submission.isRejected());
        assertEquals("Payment screenshot is unclear.", submission.getRejectionReason());
    }
}
