package com.example.seprojectpart3;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class OrganizerRepository {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    private static final String ALLOWED_DOMAINS_COLLECTION = "allowed_domains";

    public void registerOrganizer(String uid, String societyName,
                                  String societyEmail, String description,
                                  OrganizerCallback callback) {

        final String email = normalizeEmail(societyEmail);

        // M3: Validate email domain against whitelist BEFORE creating organizer
        validateOrganizerDomain(email, new DomainCallback() {
            @Override
            public void onAllowed(String domain) {

                // Check for duplicate society (use normalized email)
                db.collection("organizers")
                        .whereEqualTo("societyEmail", email)
                        .get()
                        .addOnSuccessListener(snapshot -> {
                            if (!snapshot.isEmpty()) {
                                callback.onFailure("This society is already registered");
                                return;
                            }
                            saveOrganizerProfile(uid, societyName, email, description, domain, callback);
                        })
                        .addOnFailureListener(e -> callback.onFailure(safeMsg(e)));
            }

            @Override
            public void onBlocked(String reason) {
                callback.onFailure(reason);
            }
        });
    }

    /**
     * Rules:
     * - valid email format
     * - domain ends with .edu.pk
     * - domain must exist in Firestore collection: allowed_domains (active=true)
     */
    private void validateOrganizerDomain(String normalizedEmail, DomainCallback callback) {
        if (normalizedEmail == null || normalizedEmail.isEmpty()) {
            callback.onBlocked("Society email is required");
            return;
        }

        int at = normalizedEmail.lastIndexOf("@");
        if (at <= 0 || at >= normalizedEmail.length() - 1) {
            callback.onBlocked("Invalid society email format");
            return;
        }

        final String domain = normalizedEmail.substring(at + 1).trim().toLowerCase();

        if (!domain.endsWith(".edu.pk")) {
            callback.onBlocked("Please use a valid university domain email (.edu.pk)");
            return;
        }

        // Preferred: doc id == domain
        DocumentReference docRef = db.collection(ALLOWED_DOMAINS_COLLECTION).document(domain);
        docRef.get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists() && isActive(doc)) {
                        callback.onAllowed(domain);
                    } else {
                        // Fallback: domain stored as a field "domain"
                        db.collection(ALLOWED_DOMAINS_COLLECTION)
                                .whereEqualTo("domain", domain)
                                .limit(1)
                                .get()
                                .addOnSuccessListener(qs -> {
                                    if (qs != null && !qs.isEmpty()) {
                                        DocumentSnapshot d = qs.getDocuments().get(0);
                                        if (isActive(d)) callback.onAllowed(domain);
                                        else callback.onBlocked("Organizer registration blocked: society domain not approved");
                                    } else {
                                        callback.onBlocked("Organizer registration blocked: society domain not approved");
                                    }
                                })
                                .addOnFailureListener(e -> callback.onBlocked(safeMsg(e)));
                    }
                })
                .addOnFailureListener(e -> callback.onBlocked(safeMsg(e)));
    }

    private boolean isActive(DocumentSnapshot doc) {
        // If active missing -> treat as active (for easy initial testing)
        Boolean active = doc.getBoolean("active");
        return active == null || active;
    }

    private void saveOrganizerProfile(String uid, String societyName,
                                      String societyEmail, String description,
                                      String whitelistedDomain,
                                      OrganizerCallback callback) {

        Map<String, Object> organizer = new HashMap<>();
        organizer.put("uid", uid);
        organizer.put("societyName", societyName);
        organizer.put("societyEmail", societyEmail);
        organizer.put("description", description);
        organizer.put("status", "pending");
        organizer.put("whitelistVerified", true);
        organizer.put("whitelistDomain", whitelistedDomain);
        organizer.put("whitelistVerifiedAt", FieldValue.serverTimestamp());
        organizer.put("createdAt", FieldValue.serverTimestamp());

        db.collection("organizers").add(organizer)
                .addOnSuccessListener(docRef -> {
                    db.collection("users").document(uid)
                            .update("role", "organizer", "organizerStatus", "pending")
                            .addOnSuccessListener(v2 -> callback.onSuccess(docRef.getId()))
                            .addOnFailureListener(e -> callback.onFailure(e.getMessage() != null ? e.getMessage() : "Unknown error"));
                })
                .addOnFailureListener(e -> callback.onFailure(e.getMessage() != null ? e.getMessage() : "Unknown error"));
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    private String safeMsg(Exception e) {
        return (e == null || e.getMessage() == null) ? "Unknown error" : e.getMessage();
    }

    private interface DomainCallback {
        void onAllowed(String domain);
        void onBlocked(String reason);
    }

    public interface OrganizerCallback {
        void onSuccess(String uid);
        void onFailure(String error);
    }
}