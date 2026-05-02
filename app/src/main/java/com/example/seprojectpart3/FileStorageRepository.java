package com.example.seprojectpart3;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.webkit.MimeTypeMap;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * FILE-STORAGE Module — M4 Sprint 4 (Day 1)
 *
 * Shared file storage infrastructure used by:
 *   - M3 (ORG-A): organizer uploads payment QR code
 *   - M2 (USR-B): user uploads payment proof screenshot
 *   - M1 (USR-A): reads file URL to display QR to user
 *
 * Must be complete by end of Day 2 to unblock M2 and M3.
 */
public class FileStorageRepository {

    private static final String TAG = "FileStorageRepo";

    // Upload constraints from Sprint 3 spec
    private static final long MAX_FILE_SIZE_BYTES = 5 * 1024 * 1024; // 5 MB
    private static final List<String> ALLOWED_MIME_TYPES = Arrays.asList(
            "image/png", "image/jpeg", "image/webp"
    );

    // Firebase Storage path prefixes
    private static final String QR_CODE_PATH = "payment_qr";
    private static final String PAYMENT_PROOF_PATH = "payment_proofs";

    // Entity type constants (shared with M2 and M3)
    public static final String ENTITY_TYPE_QR_CODE = "qr_code";
    public static final String ENTITY_TYPE_PAYMENT_PROOF = "payment_proof";

    private final FirebaseStorage storage;
    private final FirebaseFirestore db;
    private final FirebaseAuth auth;

    public FileStorageRepository() {
        this.storage = FirebaseStorage.getInstance();
        this.db = FirebaseFirestore.getInstance();
        this.auth = FirebaseAuth.getInstance();
    }

    // ─── Validation ──────────────────────────────────────────────────

    /**
     * Validate a file before upload. Call this first — reject invalid files
     * before touching Firebase Storage.
     *
     * @return ValidationResult with isValid flag and error message
     */
    public ValidationResult validateFile(Uri fileUri, ContentResolver resolver) {
        // 1. Check MIME type
        String mimeType = resolver.getType(fileUri);
        if (mimeType == null || !ALLOWED_MIME_TYPES.contains(mimeType)) {
            return new ValidationResult(false,
                    "Invalid file type. Only PNG, JPEG, and WEBP images are allowed.");
        }

        // 2. Check file size
        try {
            InputStream inputStream = resolver.openInputStream(fileUri);
            if (inputStream == null) {
                return new ValidationResult(false, "Could not read the selected file.");
            }
            long sizeBytes = inputStream.available();
            inputStream.close();

            if (sizeBytes > MAX_FILE_SIZE_BYTES) {
                return new ValidationResult(false,
                        "File is too large. Maximum size is 5 MB.");
            }
        } catch (Exception e) {
            return new ValidationResult(false, "Could not determine file size.");
        }

        return new ValidationResult(true, null);
    }

    // ─── Upload ──────────────────────────────────────────────────────

    /**
     * Upload a file to Firebase Storage and save metadata in Firestore.
     *
     * Usage by M3 (ORG-A — QR code upload):
     *   uploadFile(qrUri, ENTITY_TYPE_QR_CODE, eventId, ...)
     *
     * Usage by M2 (USR-B — proof screenshot upload):
     *   uploadFile(proofUri, ENTITY_TYPE_PAYMENT_PROOF, submissionId, ...)
     *
     * @param fileUri    URI of the image picked by user
     * @param entityType "qr_code" or "payment_proof"
     * @param entityId   the eventId (for QR) or submissionId (for proof)
     * @param onSuccess  returns the download URL string
     * @param onFailure  returns the error
     */
    public void uploadFile(Uri fileUri, String entityType, String entityId,
                           ContentResolver resolver,
                           OnSuccessListener<UploadResult> onSuccess,
                           OnFailureListener onFailure) {

        String currentUserId = getCurrentUserId();
        if (currentUserId == null) {
            onFailure.onFailure(new Exception("User not authenticated."));
            return;
        }

        // Validate first
        ValidationResult validation = validateFile(fileUri, resolver);
        if (!validation.isValid()) {
            onFailure.onFailure(new Exception(validation.getErrorMessage()));
            return;
        }

        // Build storage path: payment_qr/{eventId}/{uuid}.jpg
        String mimeType = resolver.getType(fileUri);
        String extension = MimeTypeMap.getSingleton()
                .getExtensionFromMimeType(mimeType);
        if (extension == null) extension = "jpg";

        String fileName = UUID.randomUUID().toString() + "." + extension;
        String storagePath = getStoragePath(entityType, entityId, fileName);

        StorageReference fileRef = storage.getReference().child(storagePath);

        // Upload to Firebase Storage
        fileRef.putFile(fileUri)
                .addOnSuccessListener(taskSnapshot -> {
                    // Get download URL after upload completes
                    fileRef.getDownloadUrl()
                            .addOnSuccessListener(downloadUri -> {
                                String downloadUrl = downloadUri.toString();

                                // Save metadata to Firestore 'files' collection
                                saveFileMetadata(
                                        fileName, storagePath, downloadUrl,
                                        mimeType, taskSnapshot.getTotalByteCount(),
                                        entityType, entityId, currentUserId,
                                        fileDocId -> {
                                            UploadResult result = new UploadResult(
                                                    fileDocId, downloadUrl, storagePath
                                            );
                                            onSuccess.onSuccess(result);
                                        },
                                        onFailure
                                );
                            })
                            .addOnFailureListener(onFailure);
                })
                .addOnFailureListener(onFailure);
    }

    /**
     * Save file metadata document in Firestore 'files' collection.
     */
    private void saveFileMetadata(String originalName, String storagePath,
                                  String downloadUrl, String mimeType,
                                  long sizeBytes, String entityType,
                                  String entityId, String uploadedBy,
                                  OnSuccessListener<String> onSuccess,
                                  OnFailureListener onFailure) {

        Map<String, Object> fileData = new HashMap<>();
        fileData.put("originalName", originalName);
        fileData.put("storagePath", storagePath);
        fileData.put("downloadUrl", downloadUrl);
        fileData.put("mimeType", mimeType);
        fileData.put("sizeBytes", sizeBytes);
        fileData.put("entityType", entityType);
        fileData.put("entityId", entityId);
        fileData.put("uploadedBy", uploadedBy);
        fileData.put("createdAt", FieldValue.serverTimestamp());
        fileData.put("deletedAt", null);

        db.collection("files")
                .add(fileData)
                .addOnSuccessListener(docRef -> onSuccess.onSuccess(docRef.getId()))
                .addOnFailureListener(onFailure);
    }

    // ─── Retrieve ────────────────────────────────────────────────────

    /**
     * Get the download URL for a stored file by its Firestore document ID.
     * Used by M1 (USR-A) to serve the QR code image to users.
     */
    public void getFileUrl(String fileId,
                           OnSuccessListener<String> onSuccess,
                           OnFailureListener onFailure) {

        db.collection("files").document(fileId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists() && doc.get("deletedAt") == null) {
                        String url = doc.getString("downloadUrl");
                        if (url != null) {
                            onSuccess.onSuccess(url);
                        } else {
                            onFailure.onFailure(new Exception("File URL not found."));
                        }
                    } else {
                        onFailure.onFailure(new Exception("File not found or deleted."));
                    }
                })
                .addOnFailureListener(onFailure);
    }

    /**
     * Get file download URL by entity type and entity ID.
     * Useful when you know the eventId but not the fileId.
     *
     * Example: get the QR code image for a specific event.
     */
    public void getFileUrlByEntity(String entityType, String entityId,
                                   OnSuccessListener<String> onSuccess,
                                   OnFailureListener onFailure) {

        db.collection("files")
                .whereEqualTo("entityType", entityType)
                .whereEqualTo("entityId", entityId)
                .whereEqualTo("deletedAt", null)
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        String url = querySnapshot.getDocuments()
                                .get(0).getString("downloadUrl");
                        if (url != null) {
                            onSuccess.onSuccess(url);
                        } else {
                            onFailure.onFailure(new Exception("File URL not found."));
                        }
                    } else {
                        onFailure.onFailure(
                                new Exception("No file found for this entity."));
                    }
                })
                .addOnFailureListener(onFailure);
    }

    // ─── Delete ──────────────────────────────────────────────────────

    /**
     * Soft-delete a file. Sets deletedAt timestamp — does not remove from Storage.
     * Only the organizer who owns the event can delete QR codes.
     */
    public void deleteFile(String fileId,
                           OnSuccessListener<Void> onSuccess,
                           OnFailureListener onFailure) {

        Map<String, Object> updates = new HashMap<>();
        updates.put("deletedAt", FieldValue.serverTimestamp());

        db.collection("files").document(fileId)
                .update(updates)
                .addOnSuccessListener(onSuccess)
                .addOnFailureListener(onFailure);
    }

    // ─── Helpers ─────────────────────────────────────────────────────

    /**
     * Build the Firebase Storage path based on entity type.
     */
    private String getStoragePath(String entityType, String entityId,
                                  String fileName) {
        if (ENTITY_TYPE_QR_CODE.equals(entityType)) {
            return QR_CODE_PATH + "/" + entityId + "/" + fileName;
        } else {
            return PAYMENT_PROOF_PATH + "/" + entityId + "/" + fileName;
        }
    }

    private String getCurrentUserId() {
        if (auth.getCurrentUser() != null) {
            return auth.getCurrentUser().getUid();
        }
        return null;
    }

    // ─── Inner Classes ───────────────────────────────────────────────

    /**
     * Result of a file validation check.
     */
    public static class ValidationResult {
        private final boolean valid;
        private final String errorMessage;

        public ValidationResult(boolean valid, String errorMessage) {
            this.valid = valid;
            this.errorMessage = errorMessage;
        }

        public boolean isValid() { return valid; }
        public String getErrorMessage() { return errorMessage; }
    }

    /**
     * Result of a successful file upload.
     * Contains the Firestore doc ID, download URL, and storage path.
     */
    public static class UploadResult {
        private final String fileId;
        private final String downloadUrl;
        private final String storagePath;

        public UploadResult(String fileId, String downloadUrl, String storagePath) {
            this.fileId = fileId;
            this.downloadUrl = downloadUrl;
            this.storagePath = storagePath;
        }

        public String getFileId() { return fileId; }
        public String getDownloadUrl() { return downloadUrl; }
        public String getStoragePath() { return storagePath; }
    }
}
