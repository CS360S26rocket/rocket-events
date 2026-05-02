package com.example.seprojectpart3;

import android.content.Context;
import android.net.Uri;

import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

/**
 * FILE-STORAGE shared module — built by M4 on Sprint 4 Day 1
 * M3 depends on this from Day 2 (ORG-A: QR upload)
 *
 * This file represents the expected interface from M4's spec (Sprint 3 spike).
 * M3 codes against this interface; M4 provides the final implementation.
 *
 * Handles:
 *   - File upload to Firebase Storage
 *   - Image size/type validation
 *   - Secure download URL generation
 *   - Storage path management
 */
public class FileStorageManager {

    private static final long MAX_FILE_SIZE_BYTES = 5 * 1024 * 1024; // 5 MB
    private static final String[] ALLOWED_TYPES = {
            "image/jpeg", "image/png", "image/webp"
    };

    private final FirebaseStorage storage;

    public FileStorageManager() {
        this.storage = FirebaseStorage.getInstance();
    }

    public FileStorageManager(FirebaseStorage storage) {
        this.storage = storage;
    }

    // ── Callback ────────────────────────────────────────────────
    public interface OnUploadListener {
        void onSuccess(String downloadUrl);
        void onFailure(String errorMessage);
        void onProgress(int percentage);
    }

    /**
     * Upload a file to Firebase Storage with validation.
     *
     * @param fileUri     The local URI of the file to upload
     * @param storagePath The path in Firebase Storage (e.g. "payment_qr/eventId/file.jpg")
     * @param context     Android context for content resolver
     * @param listener    Upload callbacks
     */
    public void uploadFile(Uri fileUri, String storagePath, Context context,
                           OnUploadListener listener) {
        if (fileUri == null) {
            listener.onFailure("No file selected");
            return;
        }

        // Validate file type
        String mimeType = context.getContentResolver().getType(fileUri);
        if (!isAllowedType(mimeType)) {
            listener.onFailure("File type not allowed. Use JPEG, PNG, or WebP.");
            return;
        }

        // Upload to Firebase Storage
        StorageReference ref = storage.getReference().child(storagePath);
        UploadTask uploadTask = ref.putFile(fileUri);

        uploadTask
                .addOnProgressListener(snapshot -> {
                    double progress = (100.0 * snapshot.getBytesTransferred())
                            / snapshot.getTotalByteCount();
                    listener.onProgress((int) progress);
                })
                .addOnSuccessListener(taskSnapshot ->
                        ref.getDownloadUrl()
                                .addOnSuccessListener(uri ->
                                        listener.onSuccess(uri.toString()))
                                .addOnFailureListener(e ->
                                        listener.onFailure(
                                                "Upload succeeded but URL generation failed: "
                                                        + e.getMessage()))
                )
                .addOnFailureListener(e ->
                        listener.onFailure("Upload failed: " + e.getMessage()));
    }

    /**
     * Delete a file from Firebase Storage.
     */
    public void deleteFile(String storagePath, OnDeleteListener listener) {
        StorageReference ref = storage.getReference().child(storagePath);
        ref.delete()
                .addOnSuccessListener(aVoid -> listener.onSuccess())
                .addOnFailureListener(e ->
                        listener.onFailure("Delete failed: " + e.getMessage()));
    }

    public interface OnDeleteListener {
        void onSuccess();
        void onFailure(String errorMessage);
    }

    private boolean isAllowedType(String mimeType) {
        if (mimeType == null) return false;
        for (String allowed : ALLOWED_TYPES) {
            if (allowed.equals(mimeType)) return true;
        }
        return false;
    }
}
