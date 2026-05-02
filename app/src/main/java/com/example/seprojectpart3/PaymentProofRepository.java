package com.example.seprojectpart3;

import android.net.Uri;

import androidx.annotation.NonNull;

import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.Map;

public class PaymentProofRepository {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final FirebaseStorage storage = FirebaseStorage.getInstance();

    public interface ProofCallback {
        void onSuccess(String proofSubmissionId);
        void onFailure(String error);
    }

    public void uploadPaymentProof(@NonNull Uri proofImageUri,
                                   @NonNull String userId,
                                   @NonNull String eventId,
                                   @NonNull String userEmail,
                                   @NonNull ProofCallback callback) {

        if (userId.trim().isEmpty() || eventId.trim().isEmpty()) {
            callback.onFailure("userId and eventId are required.");
            return;
        }

        String fileName = System.currentTimeMillis() + ".jpg";
        String storagePath = "payment_proofs/" + eventId + "/" + userId + "/" + fileName;

        StorageReference proofRef = storage.getReference().child(storagePath);

        proofRef.putFile(proofImageUri)
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }
                    return proofRef.getDownloadUrl();
                })
                .addOnSuccessListener(downloadUri -> {
                    Map<String, Object> proof = new HashMap<>();
                    proof.put("eventId", eventId);
                    proof.put("userId", userId);
                    proof.put("email", userEmail);
                    proof.put("proofImageUrl", downloadUri.toString());
                    proof.put("storagePath", storagePath);
                    proof.put("status", "pending");
                    proof.put("submittedAt", FieldValue.serverTimestamp());
                    proof.put("updatedAt", FieldValue.serverTimestamp());

                    db.collection("proof_submissions")
                            .add(proof)
                            .addOnSuccessListener(ref -> callback.onSuccess(ref.getId()))
                            .addOnFailureListener(e -> callback.onFailure(message(e)));
                })
                .addOnFailureListener(e -> callback.onFailure(message(e)));
    }

    private String message(Exception e) {
        return e == null || e.getMessage() == null ? "Unknown error" : e.getMessage();
    }
}
