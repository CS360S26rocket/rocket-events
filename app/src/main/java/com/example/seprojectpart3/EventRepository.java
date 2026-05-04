package com.example.seprojectpart3;

import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Repository for event-related Firestore operations.
 *
 * <p>This class owns the app's event creation, discovery queries, organizer event
 * lookup, event metadata updates, cancellation flow, ticket sales timing, and
 * attendee list retrieval. All operations are asynchronous and report results
 * through callback interfaces defined at the bottom of the class.</p>
 *
 * <p>Implemented backlog coverage includes:</p>
 * <ul>
 *     <li>#10 Create event</li>
 *     <li>#13 Set ticket sales start/end time</li>
 *     <li>#14 View RSVP count and attendee list</li>
 *     <li>#23 List upcoming events</li>
 *     <li>#4 View event detail</li>
 *     <li>#26 Keyword search on event title/description</li>
 *     <li>#24 Filter events by date range</li>
 *     <li>#25 Filter events by price</li>
 *     <li>#44 Cancel event and notify registered users</li>
 * </ul>
 */
public class EventRepository {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    /**
     * Creates a new event document in Firestore.
     *
     * @param organizerUid UID of the organizer creating the event.
     * @param title event title; required.
     * @param description event description; optional.
     * @param date event date stored as a {@code yyyy-MM-dd} style string; required.
     * @param venue event venue; required.
     * @param isFree true for free RSVP events, false for paid/manual-payment events.
     * @param callback callback receiving the new event document ID or an error message.
     */
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
        event.put("status", "published");
        event.put("rsvpCount", 0);
        event.put("ticketsSold", 0);
        event.put("ticketSalesOpen", false);
        event.put("ticketSalesStart", null);
        event.put("ticketSalesEnd", null);
        event.put("capacity", null);
        event.put("isFree", isFree);
        event.put("priceSummary", isFree ? "Free RSVP" : "Paid ticket");
        event.put("minTicketPrice", isFree ? 0 : null);
        event.put("paymentQrUrl", "");
        event.put("createdAt", FieldValue.serverTimestamp());

        db.collection("events").add(event)
                .addOnSuccessListener(ref -> callback.onSuccess(ref.getId()))
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    /**
     * Backward-compatible event creation overload for older callers.
     *
     * <p>Defaults {@code isFree} to {@code false}, preserving the previous paid-event behavior.</p>
     *
     * @param organizerUid UID of the organizer creating the event.
     * @param title event title.
     * @param description event description.
     * @param date event date.
     * @param venue event venue.
     * @param callback callback receiving the new event ID or an error.
     */
    public void createEvent(String organizerUid, String title, String description,
                            String date, String venue, EventCallback callback) {
        createEvent(organizerUid, title, description, date, venue, false, callback);
    }

    /**
     * Returns all events visible to campus users.
     *
     * <p>Visibility is intentionally checked client-side so both legacy {@code active}
     * events and current {@code published} events are supported.</p>
     *
     * @param callback callback receiving visible event maps. Each map includes {@code eventId}.
     */
    public void getActiveEvents(EventListCallback callback) {
        db.collection("events")
                .get()
                .addOnSuccessListener(snap -> {
                    List<Map<String, Object>> results = new ArrayList<>();

                    for (QueryDocumentSnapshot doc : snap) {
                        Map<String, Object> event = doc.getData();
                        if (!isVisibleToCampusUsers(event)) continue;
                        event.put("eventId", doc.getId());
                        results.add(event);
                    }

                    callback.onSuccess(results);
                })
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    /**
     * Returns upcoming visible events sorted by date ascending.
     *
     * <p>Dates are stored as strings, so this method compares {@code yyyy-MM-dd}
     * formatted values lexicographically.</p>
     *
     * @param callback callback receiving upcoming event maps. Each map includes {@code eventId}.
     */
    public void getUpcomingEvents(EventListCallback callback) {
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());

        db.collection("events")
                .get()
                .addOnSuccessListener(snap -> {
                    List<Map<String, Object>> results = new ArrayList<>();

                    for (QueryDocumentSnapshot doc : snap) {
                        Map<String, Object> event = doc.getData();
                        if (!isVisibleToCampusUsers(event)) continue;
                        String date = event.get("date") == null
                                ? ""
                                : String.valueOf(event.get("date"));
                        if (!date.isEmpty() && date.compareTo(today) < 0) {
                            continue;
                        }
                        event.put("eventId", doc.getId());
                        results.add(event);
                    }

                    Collections.sort(results, (a, b) ->
                            valueForSort(a, "date").compareTo(valueForSort(b, "date")));
                    callback.onSuccess(results);
                })
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    /**
     * Loads a single event and its configured ticket tiers.
     *
     * @param eventId Firestore document ID from the {@code events} collection.
     * @param callback callback receiving an event map with {@code eventId} and {@code ticketTypes}.
     */
    public void getEventDetail(String eventId, EventDetailCallback callback) {
        if (eventId == null || eventId.trim().isEmpty()) {
            callback.onFailure("Event ID is required");
            return;
        }

        db.collection("events").document(eventId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        callback.onFailure("Event not found");
                        return;
                    }

                    Map<String, Object> event = doc.getData();
                    if (event == null) event = new HashMap<>();
                    event.put("eventId", doc.getId());

                    Map<String, Object> finalEvent = event;
                    db.collection("events").document(eventId)
                            .collection("ticketTypes")
                            .orderBy("price", Query.Direction.ASCENDING)
                            .get()
                            .addOnSuccessListener(ticketSnap -> {
                                List<Map<String, Object>> ticketTypes = new ArrayList<>();
                                for (QueryDocumentSnapshot ticketDoc : ticketSnap) {
                                    Map<String, Object> ticket = ticketDoc.getData();
                                    ticket.put("typeId", ticketDoc.getId());
                                    ticketTypes.add(ticket);
                                }
                                finalEvent.put("ticketTypes", ticketTypes);
                                callback.onSuccess(finalEvent);
                            })
                            .addOnFailureListener(e -> {
                                finalEvent.put("ticketTypes", new ArrayList<Map<String, Object>>());
                                callback.onSuccess(finalEvent);
                            });
                })
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    /**
     * Applies arbitrary metadata updates to an event document.
     *
     * @param eventId event document ID.
     * @param updates map of Firestore fields to update.
     * @param callback callback receiving the event ID on success.
     */
    public void updateEventMetadata(String eventId, Map<String, Object> updates,
                                    EventCallback callback) {
        if (eventId == null || eventId.trim().isEmpty()) {
            callback.onFailure("Event ID is required");
            return;
        }
        if (updates == null || updates.isEmpty()) {
            callback.onSuccess(eventId);
            return;
        }

        db.collection("events").document(eventId)
                .update(updates)
                .addOnSuccessListener(v -> callback.onSuccess(eventId))
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    /**
     * Updates an event status after validating supported status values.
     *
     * <p>Passing {@code cancelled} routes through {@link #cancelEventAndNotify(String, EventCallback)}
     * so registered users are notified.</p>
     *
     * @param eventId event document ID.
     * @param status one of {@code draft}, {@code published}, {@code cancelled}, or {@code sold_out}.
     * @param callback callback receiving the event ID or an error.
     */
    public void updateEventStatus(String eventId, String status, EventCallback callback) {
        if (eventId == null || eventId.trim().isEmpty()) {
            callback.onFailure("Event ID is required");
            return;
        }
        String normalized = status == null ? "" : status.trim().toLowerCase(Locale.US);
        if (!"draft".equals(normalized)
                && !"published".equals(normalized)
                && !"cancelled".equals(normalized)
                && !"sold_out".equals(normalized)) {
            callback.onFailure("Unsupported event status.");
            return;
        }

        if ("cancelled".equals(normalized)) {
            cancelEventAndNotify(eventId, callback);
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("status", normalized);
        updates.put("statusUpdatedAt", FieldValue.serverTimestamp());
        updateEventMetadata(eventId, updates, callback);
    }

    /**
     * Cancels an event and creates notifications for registered users.
     *
     * @param eventId event document ID.
     * @param callback callback receiving the event ID after cancellation and notification dispatch.
     */
    public void cancelEventAndNotify(String eventId, EventCallback callback) {
        if (eventId == null || eventId.trim().isEmpty()) {
            callback.onFailure("Event ID is required");
            return;
        }

        db.collection("events").document(eventId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        callback.onFailure("Event not found");
                        return;
                    }

                    String title = doc.getString("title");
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("status", "cancelled");
                    updates.put("cancelledAt", FieldValue.serverTimestamp());

                    db.collection("events").document(eventId)
                            .update(updates)
                            .addOnSuccessListener(v ->
                                    new NotificationRepository()
                                            .notifyRegisteredUsersEventCancelled(eventId, title,
                                                    new NotificationRepository.NotificationCallback() {
                                                        @Override public void onSuccess(String message) {
                                                            callback.onSuccess(eventId);
                                                        }

                                                        @Override public void onFailure(String error) {
                                                            callback.onFailure(error);
                                                        }
                                                    }))
                            .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
                })
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    private String valueForSort(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value == null ? "" : String.valueOf(value);
    }

    private boolean isVisibleToCampusUsers(Map<String, Object> event) {
        String status = valueForSort(event, "status").toLowerCase(Locale.US);
        return "active".equals(status) || "published".equals(status);
    }

    /**
     * Updates ticket sales start and end timestamps for an event.
     *
     * @param eventId event document ID.
     * @param salesStart sales start value stored by the caller.
     * @param salesEnd sales end value stored by the caller.
     * @param callback callback receiving the event ID or an error.
     */
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

    /**
     * Retrieves RSVP count and attendee names/emails for an event.
     *
     * @param eventId event document ID.
     * @param callback callback receiving a formatted attendee list string.
     */
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

    /**
     * Searches visible events by keyword in title or description.
     *
     * @param keyword non-empty keyword to match case-insensitively.
     * @param callback callback receiving matching event maps.
     */
    public void searchEvents(String keyword, EventListCallback callback) {
        if (keyword == null || keyword.trim().isEmpty()) {
            callback.onFailure("Search keyword cannot be empty");
            return;
        }

        String kw = keyword.trim().toLowerCase();

        db.collection("events")
                .get()
                .addOnSuccessListener(snap -> {
                    List<Map<String, Object>> results = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snap) {
                        Map<String, Object> event = doc.getData();
                        if (!isVisibleToCampusUsers(event)) continue;
                        String title = doc.getString("title");
                        String desc  = doc.getString("description");

                        boolean titleMatch = title != null
                                && title.toLowerCase().contains(kw);
                        boolean descMatch  = desc != null
                                && desc.toLowerCase().contains(kw);

                        if (titleMatch || descMatch) {
                            event.put("eventId", doc.getId());
                            results.add(event);
                        }
                    }
                    callback.onSuccess(results);
                })
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    /**
     * Filters visible events by inclusive date range.
     *
     * @param startDate start date as a sortable string such as {@code yyyy-MM-dd}.
     * @param endDate end date as a sortable string such as {@code yyyy-MM-dd}.
     * @param callback callback receiving matching event maps.
     */
    public void filterByDateRange(String startDate, String endDate,
                                  EventListCallback callback) {

        if (startDate == null || startDate.isEmpty()
                || endDate == null || endDate.isEmpty()) {
            callback.onFailure("Both startDate and endDate are required");
            return;
        }

        db.collection("events")
                .whereGreaterThanOrEqualTo("date", startDate)
                .whereLessThanOrEqualTo("date", endDate)
                .orderBy("date", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(snap -> {
                    List<Map<String, Object>> results = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snap) {
                        Map<String, Object> event = doc.getData();
                        if (!isVisibleToCampusUsers(event)) continue;
                        event.put("eventId", doc.getId());
                        results.add(event);
                    }
                    callback.onSuccess(results);
                })
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    /**
     * Filters visible events by free versus paid status.
     *
     * @param isFree true for free RSVP events, false for paid ticket events.
     * @param callback callback receiving matching event maps.
     */
    public void filterByPrice(boolean isFree, EventListCallback callback) {
        db.collection("events")
                .whereEqualTo("isFree", isFree)
                .get()
                .addOnSuccessListener(snap -> {
                    List<Map<String, Object>> results = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snap) {
                        Map<String, Object> event = doc.getData();
                        if (!isVisibleToCampusUsers(event)) continue;
                        event.put("eventId", doc.getId());
                        results.add(event);
                    }
                    callback.onSuccess(results);
                })
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    /**
     * Returns events owned by a specific organizer.
     *
     * @param organizerUid organizer UID.
     * @param callback callback receiving event maps owned by that organizer.
     */
    public void getOrganizerEvents(String organizerUid, EventListCallback callback) {
        if (organizerUid == null || organizerUid.trim().isEmpty()) {
            callback.onFailure("Organizer user not found.");
            return;
        }

        db.collection("events")
                .whereEqualTo("organizerUid", organizerUid)
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

    /**
     * Callback for operations that return or update a single event ID.
     */
    public interface EventCallback {
        void onSuccess(String eventId);
        void onFailure(String error);
    }

    /**
     * Callback for attendee-list retrieval.
     */
    public interface AttendeesCallback {
        void onSuccess(String attendeeList);
        void onFailure(String error);
    }

    /**
     * Callback for query methods that return a list of event maps.
     */
    public interface EventListCallback {
        void onSuccess(List<Map<String, Object>> events);
        void onFailure(String error);
    }

    /**
     * Callback for event detail retrieval.
     */
    public interface EventDetailCallback {
        void onSuccess(Map<String, Object> event);
        void onFailure(String error);
    }
}
