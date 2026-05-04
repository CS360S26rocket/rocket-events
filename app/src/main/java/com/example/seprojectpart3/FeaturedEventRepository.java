/*
 * This file defines FeaturedEventRepository, a data repository used by the Scene app.
 * It contains featured event selection and promoted event lookup behavior.
 * Its functions include getFeaturedEvents, getFeaturedEventsLimited, setFeatured to load data, handle user actions, validate input, and save results.
 * It connects this feature to the Scene app's UI, data, navigation, and verification flow.
 */

package com.example.seprojectpart3;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;









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

    
    public interface OnFeaturedEventsListener {
        void onSuccess(List<Map<String, Object>> events);
        void onFailure(String errorMessage);
    }

    





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
