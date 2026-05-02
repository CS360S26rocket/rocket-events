package com.example.seprojectpart3;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Story #29 — Featured / spotlight flag on home screen
 * M3 · Sprint 3
 *
 * Queries Firestore for events where the "featured" boolean is true.
 * Organizers (or admins) set this flag via CreateEventActivity or
 * the OrganizerDashboard.  This repository only reads the flag.
 */
public class FeaturedEventRepository {

    private static final String COLLECTION_EVENTS = "events";
    private static final String FIELD_FEATURED = "featured";
    private static final String FIELD_STATUS = "status";
    private static final String FIELD_EVENT_DATE = "eventDate";

    private final FirebaseFirestore db;

    public FeaturedEventRepository() {
        this.db = FirebaseFirestore.getInstance();
    }

    public FeaturedEventRepository(FirebaseFirestore db) {
        this.db = db;
    }

    // ── Callback ────────────────────────────────────────────────
    public interface OnFeaturedEventsListener {
        void onSuccess(List<Map<String, Object>> events);
        void onFailure(String errorMessage);
    }

    /**
     * Fetch all published events that are flagged as featured,
     * ordered by event date (soonest first).
     *
     * Firestore index required: (featured ASC, status ASC, eventDate ASC)
     */
    public void getFeaturedEvents(OnFeaturedEventsListener listener) {
        db.collection(COLLECTION_EVENTS)
                .whereEqualTo(FIELD_FEATURED, true)
                .whereEqualTo(FIELD_STATUS, "published")
                .orderBy(FIELD_EVENT_DATE, Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Map<String, Object>> events = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        Map<String, Object> event = doc.getData();
                        event.put("eventId", doc.getId());
                        events.add(event);
                    }
                    listener.onSuccess(events);
                })
                .addOnFailureListener(e ->
                        listener.onFailure("Failed to load featured events: " + e.getMessage()));
    }

    /**
     * Fetch a limited number of featured events (for the home spotlight carousel).
     */
    public void getFeaturedEventsLimited(int limit, OnFeaturedEventsListener listener) {
        db.collection(COLLECTION_EVENTS)
                .whereEqualTo(FIELD_FEATURED, true)
                .whereEqualTo(FIELD_STATUS, "published")
                .orderBy(FIELD_EVENT_DATE, Query.Direction.ASCENDING)
                .limit(limit)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Map<String, Object>> events = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        Map<String, Object> event = doc.getData();
                        event.put("eventId", doc.getId());
                        events.add(event);
                    }
                    listener.onSuccess(events);
                })
                .addOnFailureListener(e ->
                        listener.onFailure("Failed to load featured events: " + e.getMessage()));
    }

    /**
     * Toggle the featured flag on an event (organizer / admin action).
     */
    public void setFeatured(String eventId, boolean featured,
                            OnToggleCompleteListener listener) {
        if (eventId == null || eventId.isEmpty()) {
            listener.onFailure("Event ID is required");
            return;
        }

        db.collection(COLLECTION_EVENTS)
                .document(eventId)
                .update(FIELD_FEATURED, featured)
                .addOnSuccessListener(aVoid -> listener.onSuccess(featured))
                .addOnFailureListener(e ->
                        listener.onFailure("Failed to update featured flag: " + e.getMessage()));
    }

    public interface OnToggleCompleteListener {
        void onSuccess(boolean newState);
        void onFailure(String errorMessage);
    }
}
