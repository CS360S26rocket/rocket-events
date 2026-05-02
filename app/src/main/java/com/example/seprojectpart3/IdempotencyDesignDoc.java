package com.example.seprojectpart3;

/*
 * ══════════════════════════════════════════════════════════════
 * Story #34 — Idempotency Design Document
 * M3 · Sprint 3 (design) → Sprint 4 (implementation)
 * ══════════════════════════════════════════════════════════════
 *
 * PURPOSE
 * -------
 * Prevent duplicate QR tickets when an organizer taps "Approve"
 * more than once (double-click, slow network, retry).
 *
 * APPROACH — Firestore Transaction + Idempotency Key
 * ---------------------------------------------------
 * 1. Each proof_submission document has a unique ID (submissionId).
 *    This ID doubles as the idempotency key for ticket generation.
 *
 * 2. Before generating a ticket, TKT-A checks if a ticket with
 *    idempotencyKey == submissionId already exists:
 *      - If YES → return the existing ticket (no duplicate).
 *      - If NO  → generate a new ticket inside a Firestore transaction.
 *
 * 3. The approval action (ORG-C) uses a Firestore transaction to
 *    atomically:
 *      a) Read the proof_submission status
 *      b) Verify it is still "pending"
 *      c) Update status to "approved"
 *      d) Call TKT-A to generate the ticket
 *
 *    If the status is already "approved" or "rejected", the
 *    transaction aborts — no duplicate state change.
 *
 * STATE MACHINE
 * -------------
 *   proof_submission.status:
 *     pending ──approve──► approved ──(TKT-A)──► ticket generated
 *     pending ──reject───► rejected
 *     approved ──(no-op)── already processed
 *     rejected ──(no-op)── already processed
 *
 * TICKET GENERATION (TKT-A) IDEMPOTENCY
 * --------------------------------------
 *   tickets collection document structure:
 *     {
 *       ticketId:        auto-generated
 *       idempotencyKey:  submissionId (unique index)
 *       userId:          from submission
 *       eventId:         from submission
 *       qrCodeData:      unique hash
 *       createdAt:       server timestamp
 *     }
 *
 *   Firestore rule:
 *     - tickets collection has a unique constraint on idempotencyKey
 *     - Before writing, query: tickets where idempotencyKey == submissionId
 *     - If found → return existing ticket
 *     - If not found → create inside transaction
 *
 * RACE CONDITION HANDLING
 * -----------------------
 * Scenario: Two near-simultaneous "Approve" taps
 *   T1: reads submission (status=pending), proceeds to approve
 *   T2: reads submission (status=pending), proceeds to approve
 *
 * With Firestore transaction:
 *   T1: transaction reads status=pending → sets approved → generates ticket → COMMITS
 *   T2: transaction reads status=pending → Firestore detects conflict → RETRIES
 *   T2 retry: reads status=approved → aborts (already processed)
 *
 * Result: exactly one ticket generated. ✓
 *
 * IMPLEMENTATION PLAN (Sprint 4)
 * ------------------------------
 * File: IdempotencyManager.java
 *   - generateTicketIdempotent(submissionId, userId, eventId, callback)
 *   - Uses runTransaction() for atomic read-check-write
 *   - Returns existing ticket if idempotencyKey already present
 *
 * Integration points:
 *   - M4's ORG-C (approval endpoint) calls IdempotencyManager
 *   - M3's TKT-A uses IdempotencyManager.generateTicketIdempotent()
 *
 * TESTING STRATEGY
 * ----------------
 * 1. Unit test: call generateTicketIdempotent twice with same submissionId
 *    → assert only one ticket document exists
 * 2. Unit test: attempt approval on already-approved submission
 *    → assert status unchanged, no new ticket
 * 3. Manual test: rapid double-tap approve button on UI
 *    → verify single ticket in Firestore console
 */
