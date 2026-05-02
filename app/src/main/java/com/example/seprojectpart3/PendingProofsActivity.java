package com.example.seprojectpart3;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

/**
 * Pending Proofs Activity — M4 Sprint 4
 *
 * Combines ORG-B (view pending submissions) and ORG-C (approve/reject).
 * Organizer navigates here from OrganizerDashboardActivity for a specific event.
 *
 * Flow:
 *   1. Load all proof submissions for the event (pending shown first)
 *   2. Organizer taps a proof image to view full-screen
 *   3. Organizer taps "Approve" → transaction → TKT-A generates ticket
 *   4. Organizer taps "Reject" → dialog for reason → transaction
 */
public class PendingProofsActivity extends AppCompatActivity
        implements ProofSubmissionAdapter.OnSubmissionActionListener {

    public static final String EXTRA_EVENT_ID = "event_id";
    public static final String EXTRA_EVENT_TITLE = "event_title";

    private RecyclerView rvSubmissions;
    private ProgressBar progressBar;
    private TextView tvEmpty, tvTitle;

    private ProofSubmissionRepository proofRepo;
    private ProofSubmissionAdapter adapter;
    private String eventId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pending_proofs);

        // Get event info from intent
        eventId = getIntent().getStringExtra(EXTRA_EVENT_ID);
        String eventTitle = getIntent().getStringExtra(EXTRA_EVENT_TITLE);

        if (eventId == null) {
            Toast.makeText(this, "Event not found.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Init views
        rvSubmissions = findViewById(R.id.rvProofSubmissions);
        progressBar = findViewById(R.id.progressProofs);
        tvEmpty = findViewById(R.id.tvEmptyProofs);
        tvTitle = findViewById(R.id.tvProofsTitle);

        if (eventTitle != null) {
            tvTitle.setText("Payment Proofs — " + eventTitle);
        }

        // Init repository and adapter
        proofRepo = new ProofSubmissionRepository();
        adapter = new ProofSubmissionAdapter(this);

        rvSubmissions.setLayoutManager(new LinearLayoutManager(this));
        rvSubmissions.setAdapter(adapter);

        // Load submissions
        loadSubmissions();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadSubmissions(); // Refresh on return
    }

    // ─── Data Loading ────────────────────────────────────────────────

    private void loadSubmissions() {
        progressBar.setVisibility(View.VISIBLE);
        tvEmpty.setVisibility(View.GONE);

        proofRepo.getAllSubmissions(eventId,
                submissions -> {
                    progressBar.setVisibility(View.GONE);

                    if (submissions.isEmpty()) {
                        tvEmpty.setVisibility(View.VISIBLE);
                        tvEmpty.setText("No payment proofs submitted yet.");
                    } else {
                        tvEmpty.setVisibility(View.GONE);
                    }
                    adapter.setSubmissions(submissions);
                },
                error -> {
                    progressBar.setVisibility(View.GONE);
                    tvEmpty.setVisibility(View.VISIBLE);
                    tvEmpty.setText("Failed to load submissions.");
                    Toast.makeText(this,
                            "Error: " + error.getMessage(),
                            Toast.LENGTH_LONG).show();
                }
        );
    }

    // ─── ORG-C: Approve ──────────────────────────────────────────────

    @Override
    public void onApprove(ProofSubmission submission) {
        new AlertDialog.Builder(this)
                .setTitle("Approve Payment")
                .setMessage("Approve payment proof from " + submission.getUserName()
                        + "?\n\nA QR ticket will be generated for them automatically.")
                .setPositiveButton("Approve", (dialog, which) -> {
                    executeApproval(submission);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void executeApproval(ProofSubmission submission) {
        progressBar.setVisibility(View.VISIBLE);

        proofRepo.approveSubmission(submission.getId(),
                ticketId -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this,
                            "Approved! Ticket generated for " + submission.getUserName(),
                            Toast.LENGTH_SHORT).show();
                    loadSubmissions(); // Refresh the list
                },
                error -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this,
                            "Error: " + error.getMessage(),
                            Toast.LENGTH_LONG).show();
                }
        );
    }

    // ─── ORG-C: Reject ───────────────────────────────────────────────

    @Override
    public void onReject(ProofSubmission submission) {
        // Show dialog with reason input
        EditText reasonInput = new EditText(this);
        reasonInput.setHint("Reason for rejection (optional)");
        reasonInput.setPadding(48, 32, 48, 16);

        new AlertDialog.Builder(this)
                .setTitle("Reject Payment Proof")
                .setMessage("Reject proof from " + submission.getUserName() + "?")
                .setView(reasonInput)
                .setPositiveButton("Reject", (dialog, which) -> {
                    String reason = reasonInput.getText().toString().trim();
                    if (reason.isEmpty()) {
                        reason = "Payment proof could not be verified.";
                    }
                    executeRejection(submission, reason);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void executeRejection(ProofSubmission submission, String reason) {
        progressBar.setVisibility(View.VISIBLE);

        proofRepo.rejectSubmission(submission.getId(), reason,
                unused -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this,
                            "Rejected. " + submission.getUserName()
                                    + " can submit a new proof.",
                            Toast.LENGTH_SHORT).show();
                    loadSubmissions();
                },
                error -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this,
                            "Error: " + error.getMessage(),
                            Toast.LENGTH_LONG).show();
                }
        );
    }

    // ─── Image Preview ───────────────────────────────────────────────

    @Override
    public void onImageClick(String imageUrl) {
        // Open full-screen image viewer
        Intent intent = new Intent(this, ProofImageViewerActivity.class);
        intent.putExtra(ProofImageViewerActivity.EXTRA_IMAGE_URL, imageUrl);
        startActivity(intent);
    }
}
