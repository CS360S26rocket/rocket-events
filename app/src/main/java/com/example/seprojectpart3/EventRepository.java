package com.example.seprojectpart3;

import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import java.util.HashMap;
import java.util.Map;
// This class handles event-related operations in Firebase Firestore, including creating events, setting ticket sales time, and retrieving attendee details.
// The `createEvent` method adds a new event to the Firestore database, validating required fields such as title, date, and venue. 
// The `setSalesTime` method updates the ticket sales start and end time for an event. 
// The `getAttendees` method retrieves the RSVP count and attendee list for a given event. 
// Success and failure for each operation are handled via callback interfaces (`EventCallback` and `AttendeesCallback`).
public class EventRepository {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    // Story #10 — Create event
    public void createEvent(String organizerUid, String title, String description,
                            String date, String venue, EventCallback callback) {

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
        event.put("ticketSalesStart", null);  // set by story #13
        event.put("ticketSalesEnd", null);    // set by story #13
        event.put("capacity", null);          // set by M3's story #12
        event.put("createdAt", FieldValue.serverTimestamp());

        db.collection("events").add(event)
                .addOnSuccessListener(ref -> callback.onSuccess(ref.getId()))
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

    public interface EventCallback {
        void onSuccess(String eventId);
        void onFailure(String error);
    }

    public interface AttendeesCallback {
        void onSuccess(String attendeeList);
        void onFailure(String error);
    }
}
