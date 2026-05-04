/*
 * This file defines CategoryFilterRepository, a data repository used by the Scene app.
 * It contains category-based event filtering and query helpers.
 * Its functions include filterByCategory, filterByTags, filterByCategoryAndTag, getActiveCategories to load data, handle user actions, validate input, and save results.
 * It connects this feature to the Scene app's UI, data, navigation, and verification flow.
 */

package com.example.seprojectpart3;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;








public class CategoryFilterRepository {

    private static final String COLLECTION_EVENTS = "events";
    private static final String FIELD_CATEGORY = "category";
    private static final String FIELD_TAGS = "tags";
    private static final String FIELD_STATUS = "status";
    private static final String FIELD_EVENT_DATE = "eventDate";

    private final FirebaseFirestore db;

    
    public static final List<String> CATEGORIES = Arrays.asList(
            "Academic", "Cultural", "Sports", "Workshop",
            "Social", "Career", "Club", "Other"
    );

    public CategoryFilterRepository() {
        this.db = FirebaseFirestore.getInstance();
    }

    
    public CategoryFilterRepository(FirebaseFirestore db) {
        this.db = db;
    }

    
    public interface OnEventsLoadedListener {
        void onSuccess(List<Map<String, Object>> events);
        void onFailure(String errorMessage);
    }

    



    public void filterByCategory(String category, OnEventsLoadedListener listener) {
        if (category == null || category.isEmpty()) {
            listener.onFailure("Category must not be empty");
            return;
        }

        db.collection(COLLECTION_EVENTS)
                .whereEqualTo(FIELD_CATEGORY, category)
                .whereEqualTo(FIELD_STATUS, "published")
                .orderBy(FIELD_EVENT_DATE, Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Map<String, Object>> results = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        Map<String, Object> event = doc.getData();
                        event.put("eventId", doc.getId());
                        results.add(event);
                    }
                    listener.onSuccess(results);
                })
                .addOnFailureListener(e ->
                        listener.onFailure("Failed to filter by category: " + e.getMessage()));
    }

    





    public void filterByTags(List<String> tags, OnEventsLoadedListener listener) {
        if (tags == null || tags.isEmpty()) {
            listener.onFailure("At least one tag is required");
            return;
        }

        
        List<String> queryTags = tags.size() > 30 ? tags.subList(0, 30) : tags;

        db.collection(COLLECTION_EVENTS)
                .whereArrayContainsAny(FIELD_TAGS, queryTags)
                .whereEqualTo(FIELD_STATUS, "published")
                .orderBy(FIELD_EVENT_DATE, Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Map<String, Object>> results = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        Map<String, Object> event = doc.getData();
                        event.put("eventId", doc.getId());
                        results.add(event);
                    }
                    listener.onSuccess(results);
                })
                .addOnFailureListener(e ->
                        listener.onFailure("Failed to filter by tags: " + e.getMessage()));
    }

    



    public void filterByCategoryAndTag(String category, String tag,
                                       OnEventsLoadedListener listener) {
        if (category == null || tag == null) {
            listener.onFailure("Category and tag are required");
            return;
        }

        db.collection(COLLECTION_EVENTS)
                .whereEqualTo(FIELD_CATEGORY, category)
                .whereArrayContains(FIELD_TAGS, tag)
                .whereEqualTo(FIELD_STATUS, "published")
                .orderBy(FIELD_EVENT_DATE, Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Map<String, Object>> results = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        Map<String, Object> event = doc.getData();
                        event.put("eventId", doc.getId());
                        results.add(event);
                    }
                    listener.onSuccess(results);
                })
                .addOnFailureListener(e ->
                        listener.onFailure("Failed to filter: " + e.getMessage()));
    }

    



    public void getActiveCategories(OnCategoriesLoadedListener listener) {
        db.collection(COLLECTION_EVENTS)
                .whereEqualTo(FIELD_STATUS, "published")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<String> active = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        String cat = doc.getString(FIELD_CATEGORY);
                        if (cat != null && !active.contains(cat)) {
                            active.add(cat);
                        }
                    }
                    listener.onSuccess(active);
                })
                .addOnFailureListener(e ->
                        listener.onFailure("Failed to load categories: " + e.getMessage()));
    }

    public interface OnCategoriesLoadedListener {
        void onSuccess(List<String> categories);
        void onFailure(String errorMessage);
    }
}
