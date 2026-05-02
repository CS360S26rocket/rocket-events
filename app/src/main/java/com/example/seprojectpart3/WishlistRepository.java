package com.example.seprojectpart3;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Story #45 — Save event to wishlist
 * M3 · Sprint 5
 *
 * User-event join table in Firestore:
 *   Collection: "wishlists"
 *   Document:   auto-generated
 *   Fields:     userId, eventId, addedAt
 *
 * Compound key: (userId, eventId) — prevents duplicate wishlist entries.
 * Uses a deterministic document ID: "{userId}_{eventId}" to enforce uniqueness
 * without an extra query.
 */
public class WishlistRepository {

    private static final String COLLECTION_WISHLISTS = "wishlists";
    private static final String COLLECTION_EVENTS = "events";

    private final FirebaseFirestore db;
    private final FirebaseAuth auth;

    public WishlistRepository() {
        this.db = FirebaseFirestore.getInstance();
        this.auth = FirebaseAuth.getInstance();
    }

    public WishlistRepository(FirebaseFirestore db, FirebaseAuth auth) {
        this.db = db;
        this.auth = auth;
    }

    // ── Callbacks ───────────────────────────────────────────────
    public interface OnWishlistActionListener {
        void onSuccess();
        void onFailure(String errorMessage);
    }

    public interface OnWishlistLoadedListener {
        void onSuccess(List<Map<String, Object>> wishlistEvents);
        void onFailure(String errorMessage);
    }

    public interface OnWishlistCheckListener {
        void onResult(boolean isWishlisted);
    }

    /**
     * Get the current authenticated user's ID.
     */
    private String getCurrentUserId() {
        return auth.getCurrentUser() != null
                ? auth.getCurrentUser().getUid() : null;
    }

    /**
     * Generate deterministic document ID for wishlist entry.
     * Ensures one entry per (user, event) pair.
     */
    private String getWishlistDocId(String userId, String eventId) {
        return userId + "_" + eventId;
    }

    /**
     * Add an event to the current user's wishlist.
     */
    public void addToWishlist(String eventId, OnWishlistActionListener listener) {
        String userId = getCurrentUserId();
        if (userId == null) {
            listener.onFailure("User not authenticated");
            return;
        }
        if (eventId == null || eventId.isEmpty()) {
            listener.onFailure("Event ID is required");
            return;
        }

        String docId = getWishlistDocId(userId, eventId);

        Map<String, Object> wishlistEntry = new HashMap<>();
        wishlistEntry.put("userId", userId);
        wishlistEntry.put("eventId", eventId);
        wishlistEntry.put("addedAt", FieldValue.serverTimestamp());

        db.collection(COLLECTION_WISHLISTS)
                .document(docId)
                .set(wishlistEntry)
                .addOnSuccessListener(aVoid -> listener.onSuccess())
                .addOnFailureListener(e ->
                        listener.onFailure(
                                "Failed to add to wishlist: " + e.getMessage()));
    }

    /**
     * Remove an event from the current user's wishlist.
     */
    public void removeFromWishlist(String eventId,
                                   OnWishlistActionListener listener) {
        String userId = getCurrentUserId();
        if (userId == null) {
            listener.onFailure("User not authenticated");
            return;
        }
        if (eventId == null || eventId.isEmpty()) {
            listener.onFailure("Event ID is required");
            return;
        }

        String docId = getWishlistDocId(userId, eventId);

        db.collection(COLLECTION_WISHLISTS)
                .document(docId)
                .delete()
                .addOnSuccessListener(aVoid -> listener.onSuccess())
                .addOnFailureListener(e ->
                        listener.onFailure(
                                "Failed to remove from wishlist: " + e.getMessage()));
    }

    /**
     * Toggle wishlist status: add if not wishlisted, remove if already.
     */
    public void toggleWishlist(String eventId,
                               OnWishlistToggleListener listener) {
        isWishlisted(eventId, isInWishlist -> {
            if (isInWishlist) {
                removeFromWishlist(eventId,
                        new OnWishlistActionListener() {
                            @Override
                            public void onSuccess() {
                                listener.onToggled(false);
                            }

                            @Override
                            public void onFailure(String msg) {
                                listener.onFailure(msg);
                            }
                        });
            } else {
                addToWishlist(eventId,
                        new OnWishlistActionListener() {
                            @Override
                            public void onSuccess() {
                                listener.onToggled(true);
                            }

                            @Override
                            public void onFailure(String msg) {
                                listener.onFailure(msg);
                            }
                        });
            }
        });
    }

    public interface OnWishlistToggleListener {
        void onToggled(boolean nowWishlisted);
        void onFailure(String errorMessage);
    }

    /**
     * Check if an event is in the current user's wishlist.
     */
    public void isWishlisted(String eventId, OnWishlistCheckListener listener) {
        String userId = getCurrentUserId();
        if (userId == null || eventId == null) {
            listener.onResult(false);
            return;
        }

        String docId = getWishlistDocId(userId, eventId);

        db.collection(COLLECTION_WISHLISTS)
                .document(docId)
                .get()
                .addOnSuccessListener(doc -> listener.onResult(doc.exists()))
                .addOnFailureListener(e -> listener.onResult(false));
    }

    /**
     * Get all events in the current user's wishlist.
     * Returns full event data by joining with the events collection.
     */
    public void getWishlistEvents(OnWishlistLoadedListener listener) {
        String userId = getCurrentUserId();
        if (userId == null) {
            listener.onFailure("User not authenticated");
            return;
        }

        db.collection(COLLECTION_WISHLISTS)
                .whereEqualTo("userId", userId)
                .orderBy("addedAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        listener.onSuccess(new ArrayList<>());
                        return;
                    }

                    List<Map<String, Object>> events = new ArrayList<>();
                    int total = querySnapshot.size();
                    final int[] loaded = {0};

                    for (QueryDocumentSnapshot wishlistDoc : querySnapshot) {
                        String eventId = wishlistDoc.getString("eventId");
                        if (eventId == null) {
                            loaded[0]++;
                            if (loaded[0] >= total) {
                                listener.onSuccess(events);
                            }
                            continue;
                        }

                        // Fetch full event details
                        db.collection(COLLECTION_EVENTS).document(eventId)
                                .get()
                                .addOnSuccessListener(eventDoc -> {
                                    if (eventDoc.exists()) {
                                        Map<String, Object> eventData =
                                                eventDoc.getData();
                                        if (eventData != null) {
                                            eventData.put("eventId",
                                                    eventDoc.getId());
                                            eventData.put("wishlistAddedAt",
                                                    wishlistDoc.get("addedAt"));
                                            events.add(eventData);
                                        }
                                    }
                                    loaded[0]++;
                                    if (loaded[0] >= total) {
                                        listener.onSuccess(events);
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    loaded[0]++;
                                    if (loaded[0] >= total) {
                                        listener.onSuccess(events);
                                    }
                                });
                    }
                })
                .addOnFailureListener(e ->
                        listener.onFailure(
                                "Failed to load wishlist: " + e.getMessage()));
    }

    /**
     * Get the count of users who have wishlisted an event.
     * Useful for showing popularity on event detail pages.
     */
    public void getWishlistCount(String eventId,
                                 OnWishlistCountListener listener) {
        db.collection(COLLECTION_WISHLISTS)
                .whereEqualTo("eventId", eventId)
                .get()
                .addOnSuccessListener(querySnapshot ->
                        listener.onResult(querySnapshot.size()))
                .addOnFailureListener(e -> listener.onResult(0));
    }

    public interface OnWishlistCountListener {
        void onResult(int count);
    }
}
