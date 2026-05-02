package com.example.seprojectpart3;

import android.content.ContentResolver;
import android.net.Uri;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.InputStream;

// Shared FILE-STORAGE interface from M4's Sprint 3 spec.
// M1 uses getFileUrl(fileId) for USR-A. Upload wiring remains M2/M3/M4-owned.
public class FileStorageRepository {

    private static final long MAX_FILE_SIZE_BYTES = 5L * 1024L * 1024L;

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public void uploadFile(Uri fileUri, String entityType, String entityId,
                           OnSuccessListener<String> onSuccess,
                           OnFailureListener onFailure) {
        onFailure.onFailure(new UnsupportedOperationException(
                "uploadFile is owned by the M4 file-storage implementation."));
    }

    public void getFileUrl(String fileId,
                           OnSuccessListener<String> onSuccess,
                           OnFailureListener onFailure) {
        if (fileId == null || fileId.trim().isEmpty()) {
            onFailure.onFailure(new IllegalArgumentException("fileId is required"));
            return;
        }

        db.collection("files").document(fileId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        onFailure.onFailure(new IllegalStateException("File not found"));
                        return;
                    }

                    if (doc.get("deletedAt") != null) {
                        onFailure.onFailure(new IllegalStateException("File has been deleted"));
                        return;
                    }

                    String downloadUrl = doc.getString("downloadUrl");
                    if (downloadUrl == null || downloadUrl.trim().isEmpty()) {
                        onFailure.onFailure(new IllegalStateException("File URL is missing"));
                        return;
                    }

                    onSuccess.onSuccess(downloadUrl);
                })
                .addOnFailureListener(onFailure);
    }

    public void deleteFile(String fileId,
                           OnSuccessListener<Void> onSuccess,
                           OnFailureListener onFailure) {
        if (fileId == null || fileId.trim().isEmpty()) {
            onFailure.onFailure(new IllegalArgumentException("fileId is required"));
            return;
        }

        db.collection("files").document(fileId)
                .update("deletedAt", FieldValue.serverTimestamp())
                .addOnSuccessListener(onSuccess)
                .addOnFailureListener(onFailure);
    }

    public ValidationResult validateFile(Uri fileUri, ContentResolver resolver) {
        if (fileUri == null) {
            return ValidationResult.invalid("Select an image first.");
        }
        if (resolver == null) {
            return ValidationResult.invalid("File resolver is unavailable.");
        }

        String mimeType = resolver.getType(fileUri);
        if (!"image/png".equals(mimeType)
                && !"image/jpeg".equals(mimeType)
                && !"image/webp".equals(mimeType)) {
            return ValidationResult.invalid("Only PNG, JPEG and WEBP images are allowed.");
        }

        try (InputStream stream = resolver.openInputStream(fileUri)) {
            if (stream == null) {
                return ValidationResult.invalid("Unable to read selected image.");
            }
            if (stream.available() > MAX_FILE_SIZE_BYTES) {
                return ValidationResult.invalid("Image must be 5 MB or smaller.");
            }
        } catch (Exception e) {
            return ValidationResult.invalid("Unable to read selected image.");
        }

        return ValidationResult.valid();
    }

    public static class ValidationResult {
        public final boolean valid;
        public final String errorMessage;

        private ValidationResult(boolean valid, String errorMessage) {
            this.valid = valid;
            this.errorMessage = errorMessage;
        }

        public static ValidationResult valid() {
            return new ValidationResult(true, null);
        }

        public static ValidationResult invalid(String errorMessage) {
            return new ValidationResult(false, errorMessage);
        }
    }
}
