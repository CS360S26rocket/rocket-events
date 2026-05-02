package com.example.seprojectpart3;

import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// This class handles event-related operations in Firebase Firestore, including creating events,
// setting ticket sales time, retrieving attendee details, and filtering/searching events.
//
// #10  createEvent       — Add a new event to Firestore
// #13  setSalesTime      — Update ticket sales start/end time
// #14  getAttendees      — Retrieve RSVP count and attendee list
// #26  searchEvents      — Keyword search on event title / description (Low risk, Day 1)
// #24  filterByDateRange — Filter events by date range (Low risk, Day 1)
// #25  filterByPrice     — Filter events by free vs paid (Low risk, Day 1)

public class EventRepository {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    // Story #10 — Create event
    // isFree: true when ticketMode is "free_rsvp", false when "paid_tickets" / "external_link"
    public void createEvent(String organizerUid, String title, String description,
                            String date, String venue, boolean isFree,
                            EventCallback callback) {

        if (title.isEmpty() || date.isEmpty() || venue.isEmpty()) {
            callback.onFailure("Title, date and venue are required");
            return;
        }

        Map<String, Object> event = new HashMap<>();
        event.put("organizerUid", organizerUid);
        event.put("title", title);
        event.put("description", description);
        event.put("date", date);
        event.put("venue", venue);
        event.put("status", "active");
        event.put("rsvpCount", 0);
        event.put("ticketsSold", 0);           // M4 needs this for #50
        event.put("ticketSalesOpen", false);    // M4 needs this for #50
        event.put("ticketSalesStart", null);    // set by story #13
        event.put("ticketSalesEnd", null);      // set by story #13
        event.put("capacity", null);            // set by M3's story #12
        event.put("isFree", isFree);            // used by story #25
        event.put("createdAt", FieldValue.serverTimestamp());

        db.collection("events").add(event)
                .addOnSuccessListener(ref -> callback.onSuccess(ref.getId()))
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    // Overload kept for backward-compat with existing callers that don't pass isFree yet.
    // Defaults to false (paid) so existing behaviour is unchanged.
    public void createEvent(String organizerUid, String title, String description,
                            String date, String venue, EventCallback callback) {
        createEvent(organizerUid, title, description, date, venue, false, callback);
    }

    // Campus discovery home — list all active events.
    public void getActiveEvents(EventListCallback callback) {
        db.collection("events")
                .whereEqualTo("status", "active")
                .get()
                .addOnSuccessListener(snap -> {
                    List<Map<String, Object>> results = new ArrayList<>();

                    for (QueryDocumentSnapshot doc : snap) {
                        Map<String, Object> event = doc.getData();
                        event.put("eventId", doc.getId());
                        results.add(event);
                    }

                    callback.onSuccess(results);
                })
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    // Story #13 — Set ticket sales start and end time
    public void setSalesTime(String eventId, String salesStart,
                             String salesEnd, EventCallback callback) {

        Map<String, Object> updates = new HashMap<>();
        updates.put("ticketSalesStart", salesStart);
        updates.put("ticketSalesEnd", salesEnd);

        db.collection("events").document(eventId)
                .update(updates)
                .addOnSuccessListener(v -> callback.onSuccess(eventId))
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    // Story #14 — View RSVP count and attendee list
    public void getAttendees(String eventId, AttendeesCallback callback) {
        db.collection("events").document(eventId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        callback.onFailure("Event not found");
                        return;
                    }
                    long rsvpCount = doc.getLong("rsvpCount") != null
                            ? doc.getLong("rsvpCount") : 0;
                    // fetch attendees sub-collection
                    db.collection("events").document(eventId)
                            .collection("attendees")
                            .get()
                            .addOnSuccessListener(snap -> {
                                StringBuilder list = new StringBuilder();
                                list.append("Total RSVPs: ").append(rsvpCount).append("\n\n");
                                snap.getDocuments().forEach(a ->
                                        list.append(a.getString("name"))
                                                .append(" — ")
                                                .append(a.getString("email"))
                                                .append("\n")
                                );
                                callback.onSuccess(list.toString());
                            })
                            .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
                })
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    // -------------------------------------------------------------------------
    // Story #26 — Keyword search on event title / description  (Low, Day 1)
    // -------------------------------------------------------------------------
    // Strategy: Firestore does not support full-text search, so we use a
    // prefix-range query on the "title" field (case-sensitive) combined with
    // client-side filtering on "description".  This is the standard lightweight
    // approach before a dedicated search service is wired up.
    //
    // Usage:  searchEvents("hackathon", callback)
    // -------------------------------------------------------------------------
    public void searchEvents(String keyword, EventListCallback callback) {
        if (keyword == null || keyword.trim().isEmpty()) {
            callback.onFailure("Search keyword cannot be empty");
            return;
        }

        String kw = keyword.trim().toLowerCase();

        // Pull all active events then filter client-side.
        // For large datasets this should be replaced by a search index (e.g. Algolia).
        db.collection("events")
                .whereEqualTo("status", "active")
                .get()
                .addOnSuccessListener(snap -> {
                    List<Map<String, Object>> results = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snap) {
                        String title = doc.getString("title");
                        String desc  = doc.getString("description");

                        boolean titleMatch = title != null
                                && title.toLowerCase().contains(kw);
                        boolean descMatch  = desc != null
                                && desc.toLowerCase().contains(kw);

                        if (titleMatch || descMatch) {
                            Map<String, Object> event = doc.getData();
                            event.put("eventId", doc.getId());
                            results.add(event);
                        }
                    }
                    callback.onSuccess(results);
                })
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    // -------------------------------------------------------------------------
    // Story #24 — Filter events by date range  (Low, Day 1)
    // -------------------------------------------------------------------------
    // Expects dates stored in the "date" field as ISO-8601 strings
    // (e.g. "2025-06-01") so lexicographic ordering equals chronological order.
    //
    // Usage:  filterByDateRange("2025-06-01", "2025-06-30", callback)
    // -------------------------------------------------------------------------
    public void filterByDateRange(String startDate, String endDate,
                                  EventListCallback callback) {

        if (startDate == null || startDate.isEmpty()
                || endDate == null || endDate.isEmpty()) {
            callback.onFailure("Both startDate and endDate are required");
            return;
        }

        db.collection("events")
                .whereEqualTo("status", "active")
                .whereGreaterThanOrEqualTo("date", startDate)
                .whereLessThanOrEqualTo("date", endDate)
                .orderBy("date", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(snap -> {
                    List<Map<String, Object>> results = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snap) {
                        Map<String, Object> event = doc.getData();
                        event.put("eventId", doc.getId());
                        results.add(event);
                    }
                    callback.onSuccess(results);
                })
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    // -------------------------------------------------------------------------
    // Story #25 — Filter events by price: free vs paid  (Low, Day 1)
    // -------------------------------------------------------------------------
    // Reads the boolean "isFree" field written by createEvent().
    // Pass isFree = true  → returns free/RSVP events.
    // Pass isFree = false → returns paid-ticket events.
    //
    // Usage:  filterByPrice(true, callback)   // free events only
    //         filterByPrice(false, callback)  // paid events only
    // -------------------------------------------------------------------------
    public void filterByPrice(boolean isFree, EventListCallback callback) {
        db.collection("events")
                .whereEqualTo("status", "active")
                .whereEqualTo("isFree", isFree)
                .get()
                .addOnSuccessListener(snap -> {
                    List<Map<String, Object>> results = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snap) {
                        Map<String, Object> event = doc.getData();
                        event.put("eventId", doc.getId());
                        results.add(event);
                    }
                    callback.onSuccess(results);
                })
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }
    public void getOrganizerEvents(String organizerUid, EventListCallback callback) {
        if (organizerUid == null || organizerUid.trim().isEmpty()) {
            callback.onFailure("Organizer user not found.");
            return;
        }

        db.collection("events")
                .whereEqualTo("organizerUid", organizerUid)
                .whereEqualTo("status", "active")
                .get()
                .addOnSuccessListener(snap -> {
                    List<Map<String, Object>> results = new ArrayList<>();

                    for (QueryDocumentSnapshot doc : snap) {
                        Map<String, Object> event = doc.getData();
                        event.put("eventId", doc.getId());
                        results.add(event);
                    }

                    callback.onSuccess(results);
                })
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }


    // -------------------------------------------------------------------------
    // Callbacks
    // -------------------------------------------------------------------------

    public interface EventCallback {
        void onSuccess(String eventId);
        void onFailure(String error);
    }

    public interface AttendeesCallback {
        void onSuccess(String attendeeList);
        void onFailure(String error);
    }

    /** Callback for query methods that return a list of event maps. */
    public interface EventListCallback {
        void onSuccess(List<Map<String, Object>> events);
        void onFailure(String error);
    }
}