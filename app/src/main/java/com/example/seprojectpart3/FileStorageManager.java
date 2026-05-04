/*
 * This file defines FileStorageManager, a helper manager used by the Scene app.
 * It contains file storage utility behavior for upload path generation and file URL handling.
 * Its functions include uploadFile, deleteFile, isAllowedType to load data, handle user actions, validate input, and save results.
 * It connects this feature to the Scene app's UI, data, navigation, and verification flow.
 */

package com.example.seprojectpart3;

import android.content.Context;
import android.net.Uri;

import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;














public class FileStorageManager {

    private static final long MAX_FILE_SIZE_BYTES = 5 * 1024 * 1024; 
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

    
    public interface OnUploadListener {
        void onSuccess(String downloadUrl);
        void onFailure(String errorMessage);
        void onProgress(int percentage);
    }

    







    public void uploadFile(Uri fileUri, String storagePath, Context context,
                           OnUploadListener listener) {
        if (fileUri == null) {
            listener.onFailure("No file selected");
            return;
        }

        
        String mimeType = context.getContentResolver().getType(fileUri);
        if (!isAllowedType(mimeType)) {
            listener.onFailure("File type not allowed. Use JPEG, PNG, or WebP.");
            return;
        }

        
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
