package com.example.seprojectpart3;

import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class OrganizerRepository {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public void registerOrganizer(String uid, String societyName,
                                  String societyEmail, String description,
                                  OrganizerCallback callback) {

        // Check for duplicate society
        db.collection("organizers")
                .whereEqualTo("societyEmail", societyEmail)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (!snapshot.isEmpty()) {
                        callback.onFailure("This society is already registered");
                        return;
                    }
                    saveOrganizerProfile(uid, societyName, societyEmail, description, callback);
                })
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    private void saveOrganizerProfile(String uid, String societyName,
                                      String societyEmail, String description,
                                      OrganizerCallback callback) {
        Map<String, Object> organizer = new HashMap<>();
        organizer.put("uid", uid);
        organizer.put("societyName", societyName);
        organizer.put("societyEmail", societyEmail);   // M3 will validate this against whitelist
        organizer.put("description", description);
        organizer.put("status", "pending");             // pending whitelist check by M3
        organizer.put("createdAt", FieldValue.serverTimestamp());

        db.collection("organizers").document(uid).set(organizer)
                .addOnSuccessListener(v -> {
                    // Also update the user's role in the users collection
                    db.collection("users").document(uid)
                            .update("role", "organizer", "organizerStatus", "pending")
                            .addOnSuccessListener(v2 -> callback.onSuccess(uid))
                            .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
                })
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    public interface OrganizerCallback {
        void onSuccess(String uid);
        void onFailure(String error);
    }
}