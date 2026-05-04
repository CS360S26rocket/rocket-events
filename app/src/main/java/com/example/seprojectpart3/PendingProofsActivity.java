/*
 * This file defines PendingProofsActivity, an Android activity used by the Scene app.
 * It contains the legacy pending proof review list for older screenshot-based payment records.
 * Its functions include onCreate, onResume, loadSubmissions, onApprove to load data, handle user actions, validate input, and save results.
 * It connects this feature to the Scene app's UI, data, navigation, and verification flow.
 */

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

        
        eventId = getIntent().getStringExtra(EXTRA_EVENT_ID);
        String eventTitle = getIntent().getStringExtra(EXTRA_EVENT_TITLE);

        if (eventId == null) {
            Toast.makeText(this, "Event not found.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        
        rvSubmissions = findViewById(R.id.rvProofSubmissions);
        progressBar = findViewById(R.id.progressProofs);
        tvEmpty = findViewById(R.id.tvEmptyProofs);
        tvTitle = findViewById(R.id.tvProofsTitle);

        if (eventTitle != null) {
            tvTitle.setText("Payment Proofs — " + eventTitle);
        }

        
        proofRepo = new ProofSubmissionRepository();
        adapter = new ProofSubmissionAdapter(this);

        rvSubmissions.setLayoutManager(new LinearLayoutManager(this));
        rvSubmissions.setAdapter(adapter);

        
        loadSubmissions();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadSubmissions(); 
    }

    

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

    

    @Override
    public void onReject(ProofSubmission submission) {
        
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

    

    @Override
    public void onImageClick(String imageUrl) {
        
        Intent intent = new Intent(this, ProofImageViewerActivity.class);
        intent.putExtra(ProofImageViewerActivity.EXTRA_IMAGE_URL, imageUrl);
        startActivity(intent);
    }
}
