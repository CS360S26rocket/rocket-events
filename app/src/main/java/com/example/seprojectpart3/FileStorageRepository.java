package com.example.seprojectpart3;

import android.content.ContentResolver;
import android.net.Uri;
import android.webkit.MimeTypeMap;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class FileStorageRepository {

    private static final long MAX_FILE_SIZE_BYTES = 5L * 1024L * 1024L;

    private static final List<String> ALLOWED_MIME_TYPES = Arrays.asList(
            "image/png",
            "image/jpeg",
            "image/webp"
    );

    private static final String QR_CODE_PATH = "payment_qr";
    private static final String PAYMENT_PROOF_PATH = "payment_proofs";

    public static final String ENTITY_TYPE_QR_CODE = "qr_code";
    public static final String ENTITY_TYPE_PAYMENT_PROOF = "payment_proof";

    private final FirebaseStorage storage = FirebaseStorage.getInstance();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final FirebaseAuth auth = FirebaseAuth.getInstance();

    public ValidationResult validateFile(Uri fileUri, ContentResolver resolver) {
        if (fileUri == null) {
            return ValidationResult.invalid("Select an image first.");
        }

        if (resolver == null) {
            return ValidationResult.invalid("File resolver is unavailable.");
        }

        String mimeType = resolver.getType(fileUri);
        if (mimeType == null || !ALLOWED_MIME_TYPES.contains(mimeType)) {
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

    public void uploadFile(Uri fileUri,
                           String entityType,
                           String entityId,
                           ContentResolver resolver,
                           OnSuccessListener<UploadResult> onSuccess,
                           OnFailureListener onFailure) {

        String currentUserId = getCurrentUserId();
        if (currentUserId == null) {
            onFailure.onFailure(new Exception("User not authenticated."));
            return;
        }

        ValidationResult validation = validateFile(fileUri, resolver);
        if (!validation.isValid()) {
            onFailure.onFailure(new Exception(validation.getErrorMessage()));
            return;
        }

        String mimeType = resolver.getType(fileUri);
        String extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType);
        if (extension == null) extension = "jpg";

        String fileName = UUID.randomUUID().toString() + "." + extension;
        String storagePath = getStoragePath(entityType, entityId, fileName);

        StorageReference fileRef = storage.getReference().child(storagePath);

        fileRef.putFile(fileUri)
                .addOnSuccessListener(taskSnapshot ->
                        fileRef.getDownloadUrl()
                                .addOnSuccessListener(downloadUri -> {
                                    String downloadUrl = downloadUri.toString();

                                    saveFileMetadata(
                                            fileName,
                                            storagePath,
                                            downloadUrl,
                                            mimeType,
                                            taskSnapshot.getTotalByteCount(),
                                            entityType,
                                            entityId,
                                            currentUserId,
                                            fileDocId -> {
                                                UploadResult result = new UploadResult(
                                                        fileDocId,
                                                        downloadUrl,
                                                        storagePath
                                                );
                                                onSuccess.onSuccess(result);
                                            },
                                            onFailure
                                    );
                                })
                                .addOnFailureListener(onFailure))
                .addOnFailureListener(onFailure);
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

    public void getFileUrlByEntity(String entityType,
                                   String entityId,
                                   OnSuccessListener<String> onSuccess,
                                   OnFailureListener onFailure) {
        if (entityType == null || entityType.trim().isEmpty()
                || entityId == null || entityId.trim().isEmpty()) {
            onFailure.onFailure(new IllegalArgumentException("entityType and entityId are required"));
            return;
        }

        db.collection("files")
                .whereEqualTo("entityType", entityType)
                .whereEqualTo("entityId", entityId)
                .whereEqualTo("deletedAt", null)
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        onFailure.onFailure(new IllegalStateException("No file found for this entity"));
                        return;
                    }

                    String downloadUrl = querySnapshot.getDocuments().get(0).getString("downloadUrl");
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

    private void saveFileMetadata(String originalName,
                                  String storagePath,
                                  String downloadUrl,
                                  String mimeType,
                                  long sizeBytes,
                                  String entityType,
                                  String entityId,
                                  String uploadedBy,
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

    private String getStoragePath(String entityType, String entityId, String fileName) {
        if (ENTITY_TYPE_QR_CODE.equals(entityType)) {
            return QR_CODE_PATH + "/" + entityId + "/" + fileName;
        }

        return PAYMENT_PROOF_PATH + "/" + entityId + "/" + fileName;
    }

    private String getCurrentUserId() {
        return auth.getCurrentUser() == null ? null : auth.getCurrentUser().getUid();
    }

    public static class ValidationResult {
        private final boolean valid;
        private final String errorMessage;

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

        public boolean isValid() {
            return valid;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }

    public static class UploadResult {
        private final String fileId;
        private final String downloadUrl;
        private final String storagePath;

        public UploadResult(String fileId, String downloadUrl, String storagePath) {
            this.fileId = fileId;
            this.downloadUrl = downloadUrl;
            this.storagePath = storagePath;
        }

        public String getFileId() {
            return fileId;
        }

        public String getDownloadUrl() {
            return downloadUrl;
        }

        public String getStoragePath() {
            return storagePath;
        }
    }
}
