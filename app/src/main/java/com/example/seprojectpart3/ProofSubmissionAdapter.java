/*
 * This file defines ProofSubmissionAdapter, a list adapter used by the Scene app.
 * It contains RecyclerView binding for legacy payment proof list items.
 * Its functions include setSubmissions, onCreateViewHolder, onBindViewHolder, getItemCount to load data, handle user actions, validate input, and save results.
 * It connects this feature to the Scene app's UI, data, navigation, and verification flow.
 */

package com.example.seprojectpart3;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;







public class ProofSubmissionAdapter
        extends RecyclerView.Adapter<ProofSubmissionAdapter.ViewHolder> {

    public interface OnSubmissionActionListener {
        void onApprove(ProofSubmission submission);
        void onReject(ProofSubmission submission);
        void onImageClick(String imageUrl);
    }

    private List<ProofSubmission> submissions = new ArrayList<>();
    private final OnSubmissionActionListener listener;
    private final SimpleDateFormat dateFormat =
            new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());

    public ProofSubmissionAdapter(OnSubmissionActionListener listener) {
        this.listener = listener;
    }

    public void setSubmissions(List<ProofSubmission> newSubmissions) {
        this.submissions = newSubmissions;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_proof_submission, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ProofSubmission submission = submissions.get(position);
        Context context = holder.itemView.getContext();

        
        holder.tvUserName.setText(submission.getUserName());
        holder.tvUserEmail.setText(submission.getUserEmail());

        
        if (submission.getSubmittedAt() != null) {
            holder.tvSubmittedAt.setText(
                    "Submitted: " + dateFormat.format(submission.getSubmittedAt().toDate())
            );
        }

        
        if (submission.getProofImageUrl() != null) {
            Glide.with(context)
                    .load(submission.getProofImageUrl())
                    .placeholder(R.drawable.bg_card_dark)
                    .into(holder.ivProofImage);

            holder.ivProofImage.setOnClickListener(v ->
                    listener.onImageClick(submission.getProofImageUrl())
            );
        }

        
        switch (submission.getStatus()) {
            case ProofSubmission.STATUS_PENDING:
                holder.tvStatus.setText("PENDING");
                holder.tvStatus.setTextColor(
                        ContextCompat.getColor(context, android.R.color.holo_orange_dark));
                holder.btnApprove.setVisibility(View.VISIBLE);
                holder.btnReject.setVisibility(View.VISIBLE);
                holder.tvRejectionReason.setVisibility(View.GONE);
                break;

            case ProofSubmission.STATUS_APPROVED:
                holder.tvStatus.setText("APPROVED");
                holder.tvStatus.setTextColor(
                        ContextCompat.getColor(context, android.R.color.holo_green_dark));
                holder.btnApprove.setVisibility(View.GONE);
                holder.btnReject.setVisibility(View.GONE);
                holder.tvRejectionReason.setVisibility(View.GONE);
                break;

            case ProofSubmission.STATUS_REJECTED:
                holder.tvStatus.setText("REJECTED");
                holder.tvStatus.setTextColor(
                        ContextCompat.getColor(context, android.R.color.holo_red_dark));
                holder.btnApprove.setVisibility(View.GONE);
                holder.btnReject.setVisibility(View.GONE);
                if (submission.getRejectionReason() != null) {
                    holder.tvRejectionReason.setVisibility(View.VISIBLE);
                    holder.tvRejectionReason.setText(
                            "Reason: " + submission.getRejectionReason());
                }
                break;
        }

        
        holder.btnApprove.setOnClickListener(v -> listener.onApprove(submission));
        holder.btnReject.setOnClickListener(v -> listener.onReject(submission));
    }

    @Override
    public int getItemCount() {
        return submissions.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvUserName, tvUserEmail, tvSubmittedAt, tvStatus, tvRejectionReason;
        ImageView ivProofImage;
        Button btnApprove, btnReject;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUserName = itemView.findViewById(R.id.tvProofUserName);
            tvUserEmail = itemView.findViewById(R.id.tvProofUserEmail);
            tvSubmittedAt = itemView.findViewById(R.id.tvProofSubmittedAt);
            tvStatus = itemView.findViewById(R.id.tvProofStatus);
            tvRejectionReason = itemView.findViewById(R.id.tvRejectionReason);
            ivProofImage = itemView.findViewById(R.id.ivProofImage);
            btnApprove = itemView.findViewById(R.id.btnApproveProof);
            btnReject = itemView.findViewById(R.id.btnRejectProof);
        }
    }
}
