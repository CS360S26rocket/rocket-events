package com.example.seprojectpart3;

import androidx.annotation.NonNull;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Repository for ticketing, RSVP, waitlist, ticket-tier, and ticket-status operations.
 *
 * <p>This class coordinates Firestore writes across event capacity, ticket tiers,
 * registrations, waitlists, proof submissions, and issued tickets. Most methods are
 * asynchronous and return results through callback interfaces.</p>
 *
 * <p>Implemented backlog coverage includes:</p>
 * <ul>
 *     <li>#5 Ticket selection</li>
 *     <li>#8 View user's RSVP'd and ticketed events</li>
 *     <li>#11 Create/update ticket type</li>
 *     <li>#12 Update event capacity</li>
 *     <li>#32 Cancel RSVP before deadline and restore capacity</li>
 *     <li>#41 Multiple ticket tiers</li>
 *     <li>USR-C View ticket status</li>
 * </ul>
 */

public class TicketRepository {

    private static final String EVENTS_COLLECTION         = "events";
    private static final String REGISTRATIONS_COLLECTION  = "registrations";
    private static final String WAITLIST_COLLECTION       = "waitlist";
    private static final String TICKETS_COLLECTION        = "tickets";
    private static final String PROOF_SUBMISSIONS         = "proof_submissions";

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    // -------------------------------------------------------------------------
    // Existing callbacks
    // -------------------------------------------------------------------------

    /**
     * Callback for ticket setup and capacity operations that return a status message.
     */

    public interface TicketCallback {
        void onSuccess(String message);
        void onFailure(String error);
    }

    /**
     * Callback for registration operations.
     *
     * <p>A registration may be confirmed immediately or placed on the waitlist if
     * capacity or ticket-tier limits are reached.</p>
     */

    public interface RegistrationCallback {
        void onConfirmed(String ticketId);
        void onWaitlisted(String waitlistId);
        void onFailure(String error);
    }

    // -------------------------------------------------------------------------
    // New callbacks for the three stories below
    // -------------------------------------------------------------------------

    /** #8 — list of event maps for a user's RSVPs and tickets */
    /**
     * Callback for loading a user's registered, ticketed, and waitlisted events.
     */
    public interface UserEventsCallback {
        void onSuccess(List<Map<String, Object>> events);
        void onFailure(String error);
    }

    /** #32 — simple success/failure for RSVP cancellation */
    /**
     * Callback for RSVP cancellation operations.
     */

    public interface CancelCallback {
        void onSuccess(String message);
        void onFailure(String error);
    }

    /** USRC — ticket / proof-submission status */
    /**
     * Callback for retrieving a user's ticket or proof-submission status.
     */

    public interface TicketStatusCallback {
        void onSuccess(String status);   // e.g. "confirmed", "pending", "approved", "rejected", "waitlisted"
        void onFailure(String error);
    }

    /** #5/#41 — available ticket tiers for one event */
    /**
     * Callback for loading ticket tiers configured for a specific event.
     */

    public interface TicketTypesCallback {
        void onSuccess(List<Map<String, Object>> ticketTypes);
        void onFailure(String error);
    }

    /**
     * Loads all registrations and waitlist entries for a user.
     *
     * <p>The returned maps include registration/waitlist fields plus enriched event
     * metadata such as event title, date, venue, banner URL, and status when available.</p>
     *
     * @param attendeeUid Firebase UID of the campus user.
     * @param callback callback receiving user-scoped event records or an error.
     */

    public void getUserEvents(@NonNull String attendeeUid,
                              @NonNull UserEventsCallback callback) {

        if (attendeeUid.trim().isEmpty()) {
            callback.onFailure("attendeeUid required");
            return;
        }

        db.collection(REGISTRATIONS_COLLECTION)
                .whereEqualTo("userId", attendeeUid)
                .get()
                .addOnSuccessListener(snap -> {
                    List<Map<String, Object>> results = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snap) {
                        Map<String, Object> reg = doc.getData();
                        reg.put("registrationId", doc.getId());
                        results.add(reg);
                    }

                    // Also pull any waitlist entries for a complete picture
                    db.collection(WAITLIST_COLLECTION)
                            .whereEqualTo("userId", attendeeUid)
                            .get()
                            .addOnSuccessListener(wSnap -> {
                                for (QueryDocumentSnapshot doc : wSnap) {
                                    Map<String, Object> w = doc.getData();
                                    w.put("registrationId", doc.getId());
                                    results.add(w);
                                }
                                attachEventDetails(results, callback);
                            })
                            .addOnFailureListener(e -> callback.onFailure(msg(e)));
                })
                .addOnFailureListener(e -> callback.onFailure(msg(e)));
    }

    /**
     * Cancels a user's RSVP or ticket registration before the cancellation deadline.
     *
     * <p>If the registration was confirmed, this method restores capacity by decrementing
     * the event sold count and, for paid tiers, the selected ticket tier's sold count.
     * If capacity opens up, the method also attempts a best-effort waitlist promotion.</p>
     *
     * @param registrationId Firestore document ID from the registrations collection.
     * @param callback callback receiving a success message or error.
     */

    public void cancelRsvp(@NonNull String registrationId,
                           @NonNull CancelCallback callback) {

        if (registrationId.trim().isEmpty()) {
            callback.onFailure("registrationId required");
            return;
        }

        DocumentReference regRef = db.collection(REGISTRATIONS_COLLECTION)
                .document(registrationId);

        // Step 1 — read the registration
        regRef.get().addOnSuccessListener(regSnap -> {

            if (!regSnap.exists()) {
                callback.onFailure("Registration not found");
                return;
            }

            String currentStatus = regSnap.getString("status");
            if ("cancelled".equals(currentStatus)) {
                callback.onFailure("Already cancelled");
                return;
            }

            String eventId      = regSnap.getString("eventId");
            String ticketTypeId = regSnap.getString("ticketType");
            boolean wasConfirmed = "confirmed".equals(currentStatus);

            if (eventId == null || eventId.isEmpty()) {
                callback.onFailure("Registration has no eventId");
                return;
            }

            DocumentReference eventRef = db.collection(EVENTS_COLLECTION).document(eventId);

            // Step 2 — check cancellation deadline
            eventRef.get().addOnSuccessListener(eventSnap -> {

                if (!eventSnap.exists()) {
                    callback.onFailure("Event not found");
                    return;
                }

                String cancelDeadline = eventSnap.getString("cancelDeadline");
                if (cancelDeadline != null && !cancelDeadline.isEmpty()) {
                    // Compare lexicographically (ISO-8601 dates sort correctly as strings)
                    String nowIso = new java.text.SimpleDateFormat(
                            "yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US)
                            .format(new java.util.Date());
                    if (nowIso.compareTo(cancelDeadline) > 0) {
                        callback.onFailure("Cancellation deadline has passed (" + cancelDeadline + ")");
                        return;
                    }
                }

                // Step 3 — run the cancellation transaction
                DocumentReference ticketTypeRef = ticketTypeId != null && !ticketTypeId.isEmpty()
                        ? eventRef.collection("ticketTypes").document(ticketTypeId)
                        : null;

                db.runTransaction(tx -> {

                    // Mark registration cancelled
                    tx.update(regRef, "status", "cancelled",
                            "cancelledAt", FieldValue.serverTimestamp());

                    if (wasConfirmed) {
                        // Restore event soldCount
                        DocumentSnapshot esnap = tx.get(eventRef);
                        Long sold = esnap.getLong("soldCount");
                        long newSold = (sold != null && sold > 0) ? sold - 1 : 0;
                        tx.update(eventRef, "soldCount", newSold);

                        // Restore ticket-type sold counter
                        if (ticketTypeRef != null && !"free_rsvp".equals(ticketTypeId)) {
                            DocumentSnapshot tsnap = tx.get(ticketTypeRef);
                            if (tsnap.exists()) {
                                Long tSold = tsnap.getLong("sold");
                                long newTSold = (tSold != null && tSold > 0) ? tSold - 1 : 0;
                                tx.update(ticketTypeRef, "sold", newTSold);
                            }
                        }
                    }

                    return wasConfirmed;

                }).addOnSuccessListener(wasConf -> {

                    // Step 4 (best-effort) — if a confirmed slot just opened up,
                    // promote the oldest waitlisted attendee for this event.
                    if (Boolean.TRUE.equals(wasConf)) {
                        promoteFromWaitlist(eventId, ticketTypeId);
                    }

                    callback.onSuccess("RSVP cancelled" +
                            (Boolean.TRUE.equals(wasConf) ? " — capacity restored" : ""));

                }).addOnFailureListener(e -> callback.onFailure(msg(e)));

            }).addOnFailureListener(e -> callback.onFailure(msg(e)));

        }).addOnFailureListener(e -> callback.onFailure(msg(e)));
    }

    /**
     * Best-effort waitlist promotion after a confirmed cancellation.
     *
     * <p>This promotes the earliest waitlisted user for the event into a confirmed
     * registration and creates a valid ticket document. Failures are intentionally not
     * surfaced because the cancellation itself has already completed.</p>
     *
     * @param eventId event whose waitlist should be checked.
     * @param ticketTypeId ticket tier to restore/promote, if applicable.
     */

    private void promoteFromWaitlist(String eventId, String ticketTypeId) {
        db.collection(WAITLIST_COLLECTION)
                .whereEqualTo("eventId", eventId)
                .whereEqualTo("status", "waitlisted")
                .orderBy("registeredAt", com.google.firebase.firestore.Query.Direction.ASCENDING)
                .limit(1)
                .get()
                .addOnSuccessListener(snap -> {
                    if (snap.isEmpty()) return; // no one waiting

                    QueryDocumentSnapshot first = (QueryDocumentSnapshot) snap.getDocuments().get(0);

                    // Write a confirmed registration for this attendee
                    String newRegId  = UUID.randomUUID().toString();
                    String newTicketId = newRegId;

                    Map<String, Object> reg = new HashMap<>(first.getData());
                    reg.put("status", "confirmed");
                    reg.put("ticketId", newTicketId);
                    reg.put("promotedFromWaitlist", true);
                    reg.put("registeredAt", FieldValue.serverTimestamp());

                    Map<String, Object> ticket = new HashMap<>();
                    ticket.put("eventId", eventId);
                    ticket.put("userId", first.getString("userId"));
                    ticket.put("attendeeName", first.getString("attendeeName"));
                    ticket.put("tier", first.getString("ticketType"));
                    ticket.put("status", "valid");
                    ticket.put("createdAt", FieldValue.serverTimestamp());

                    db.collection(REGISTRATIONS_COLLECTION).document(newRegId).set(reg);
                    db.collection(TICKETS_COLLECTION).document(newTicketId).set(ticket);

                    // Update event soldCount
                    db.collection(EVENTS_COLLECTION).document(eventId)
                            .update("soldCount", FieldValue.increment(1));

                    // Update ticket-type sold counter
                    if (ticketTypeId != null
                            && !ticketTypeId.isEmpty()
                            && !"free_rsvp".equals(ticketTypeId)) {
                        db.collection(EVENTS_COLLECTION).document(eventId)
                                .collection("ticketTypes").document(ticketTypeId)
                                .update("sold", FieldValue.increment(1));
                    }


                    // Remove from waitlist
                    first.getReference().update("status", "promoted");
                });
    }

    /**
     * Returns the user's current status for an event.
     *
     * <p>The lookup checks payment proof submissions first because they provide the
     * pending/approved/rejected state for manual payment. If no proof submission exists,
     * the method falls back to registrations and then waitlist entries.</p>
     *
     * @param userId Firebase UID of the campus user.
     * @param eventId event document ID.
     * @param callback callback receiving status such as pending, approved, rejected,
     *                 confirmed, or waitlisted.
     */

    public void getTicketStatus(@NonNull String userId,
                                @NonNull String eventId,
                                @NonNull TicketStatusCallback callback) {

        if (userId.trim().isEmpty() || eventId.trim().isEmpty()) {
            callback.onFailure("userId and eventId are required");
            return;
        }

        // 1. Check proof_submissions first (most specific status)
        db.collection(PROOF_SUBMISSIONS)
                .whereEqualTo("userId", userId)
                .whereEqualTo("eventId", eventId)
                .limit(1)
                .get()
                .addOnSuccessListener(proofSnap -> {

                    if (!proofSnap.isEmpty()) {
                        // Return the proof-submission status (pending/approved/rejected)
                        String status = proofSnap.getDocuments().get(0).getString("status");
                        callback.onSuccess(status != null ? status : "pending");
                        return;
                    }

                    // 2. No proof submission — fall back to registration record
                    db.collection(REGISTRATIONS_COLLECTION)
                            .whereEqualTo("userId", userId)
                            .whereEqualTo("eventId", eventId)
                            .limit(1)
                            .get()
                            .addOnSuccessListener(regSnap -> {

                                if (!regSnap.isEmpty()) {
                                    String status = regSnap.getDocuments().get(0).getString("status");
                                    callback.onSuccess(status != null ? status : "confirmed");
                                    return;
                                }

                                // 3. Check waitlist
                                db.collection(WAITLIST_COLLECTION)
                                        .whereEqualTo("userId", userId)
                                        .whereEqualTo("eventId", eventId)
                                        .limit(1)
                                        .get()
                                        .addOnSuccessListener(wSnap -> {
                                            if (!wSnap.isEmpty()) {
                                                String status = wSnap.getDocuments()
                                                        .get(0).getString("status");
                                                callback.onSuccess(status != null ? status : "waitlisted");
                                            } else {
                                                callback.onFailure("No registration found for this user/event");
                                            }
                                        })
                                        .addOnFailureListener(e -> callback.onFailure(msg(e)));

                            })
                            .addOnFailureListener(e -> callback.onFailure(msg(e)));

                })
                .addOnFailureListener(e -> callback.onFailure(msg(e)));
    }

    /**
     * Adds event metadata to registration and waitlist records.
     *
     * @param results registration/waitlist maps to enrich.
     * @param callback callback receiving enriched records once all event reads finish.
     */


    private void attachEventDetails(List<Map<String, Object>> results,
                                    UserEventsCallback callback) {
        if (results.isEmpty()) {
            callback.onSuccess(results);
            return;
        }

        AtomicInteger remaining = new AtomicInteger(results.size());

        for (Map<String, Object> reg : results) {
            Object eventIdObj = reg.get("eventId");

            if (eventIdObj == null) {
                reg.put("eventTitle", "Unknown event");
                reg.put("eventDate", "-");

                if (remaining.decrementAndGet() == 0) {
                    callback.onSuccess(results);
                }

                continue;
            }

            db.collection(EVENTS_COLLECTION)
                    .document(String.valueOf(eventIdObj))
                    .get()
                    .addOnSuccessListener(eventSnap -> {
                        reg.put("eventTitle",
                                eventSnap.getString("title") != null
                                        ? eventSnap.getString("title")
                                        : String.valueOf(eventIdObj));

                        reg.put("eventDate",
                                eventSnap.getString("date") != null
                                        ? eventSnap.getString("date")
                                        : "-");

                        reg.put("eventVenue",
                                eventSnap.getString("venue") != null
                                        ? eventSnap.getString("venue")
                                        : "-");

                        reg.put("bannerImageUrl",
                                eventSnap.getString("bannerImageUrl") != null
                                        ? eventSnap.getString("bannerImageUrl")
                                        : "");

                        reg.put("eventStatus",
                                eventSnap.getString("status") != null
                                        ? eventSnap.getString("status")
                                        : "");

                        if (remaining.decrementAndGet() == 0) {
                            callback.onSuccess(results);
                        }
                    })
                    .addOnFailureListener(e -> {
                        reg.put("eventTitle", String.valueOf(eventIdObj));
                        reg.put("eventDate", "-");

                        if (remaining.decrementAndGet() == 0) {
                            callback.onSuccess(results);
                        }
                    });
        }
    }

    /**
     * Registers a user for a free RSVP event.
     *
     * <p>If the event has available capacity, a confirmed registration is created and
     * RSVP/sold counters are incremented. If the event is full, a waitlist record is
     * created instead.</p>
     *
     * @param eventId event document ID.
     * @param attendeeUid Firebase UID of the campus user.
     * @param attendeeName display name of the attendee.
     * @param attendeeEmail email address of the attendee.
     * @param callback callback receiving the registration ID or waitlist ID.
     */

    public void registerFreeRsvp(@NonNull String eventId,
                                 @NonNull String attendeeUid,
                                 @NonNull String attendeeName,
                                 @NonNull String attendeeEmail,
                                 @NonNull RegistrationCallback callback) {

        if (eventId.trim().isEmpty() || attendeeUid.trim().isEmpty()) {
            callback.onFailure("eventId/attendeeUid required");
            return;
        }

        DocumentReference eventRef = db.collection(EVENTS_COLLECTION).document(eventId);

        db.runTransaction(transaction -> {
            DocumentSnapshot eventSnap = transaction.get(eventRef);

            if (!eventSnap.exists()) {
                throw new IllegalStateException("Event not found");
            }

            Long capacityL = eventSnap.getLong("capacity");
            Long soldCountL = eventSnap.getLong("soldCount");
            Long rsvpCountL = eventSnap.getLong("rsvpCount");

            long capacity = capacityL == null ? 0 : capacityL;
            long soldCount = soldCountL == null ? 0 : soldCountL;
            long rsvpCount = rsvpCountL == null ? 0 : rsvpCountL;

            boolean hasCapacityLimit = capacity > 0;

            if (!hasCapacityLimit || soldCount < capacity) {
                String registrationId = UUID.randomUUID().toString();
                DocumentReference regRef = db.collection(REGISTRATIONS_COLLECTION)
                        .document(registrationId);

                Map<String, Object> reg = new HashMap<>();
                reg.put("eventId", eventId);
                reg.put("userId", attendeeUid);
                reg.put("attendeeName", attendeeName);
                reg.put("email", attendeeEmail);
                reg.put("ticketType", "free_rsvp");
                reg.put("status", "confirmed");
                reg.put("registeredAt", FieldValue.serverTimestamp());

                transaction.set(regRef, reg);
                transaction.update(eventRef, "rsvpCount", rsvpCount + 1);
                transaction.update(eventRef, "soldCount", soldCount + 1);

                return new Result(ResultType.CONFIRMED, registrationId);
            }

            String waitlistId = UUID.randomUUID().toString();
            DocumentReference waitRef = db.collection(WAITLIST_COLLECTION)
                    .document(waitlistId);

            Map<String, Object> waitlist = new HashMap<>();
            waitlist.put("eventId", eventId);
            waitlist.put("userId", attendeeUid);
            waitlist.put("attendeeName", attendeeName);
            waitlist.put("email", attendeeEmail);
            waitlist.put("ticketType", "free_rsvp");
            waitlist.put("status", "waitlisted");
            waitlist.put("registeredAt", FieldValue.serverTimestamp());

            transaction.set(waitRef, waitlist);

            return new Result(ResultType.WAITLISTED, waitlistId);

        }).addOnSuccessListener(result -> {
            if (result.type == ResultType.CONFIRMED) {
                callback.onConfirmed(result.id);
            } else {
                callback.onWaitlisted(result.id);
            }
        }).addOnFailureListener(e -> callback.onFailure(msg(e)));
    }

    // Story #5/#41 — Fetch ticket types / tiers for selection.
    public void getTicketTypes(@NonNull String eventId,
                               @NonNull TicketTypesCallback callback) {
        if (eventId.trim().isEmpty()) {
            callback.onFailure("eventId required");
            return;
        }

        db.collection(EVENTS_COLLECTION).document(eventId)
                .collection("ticketTypes")
                .orderBy("price", com.google.firebase.firestore.Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(snap -> {
                    List<Map<String, Object>> ticketTypes = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snap) {
                        Map<String, Object> type = doc.getData();
                        type.put("typeId", doc.getId());
                        ticketTypes.add(type);
                    }
                    callback.onSuccess(ticketTypes);
                })
                .addOnFailureListener(e -> callback.onFailure(msg(e)));
    }

    // Story #5/#41 — Register one or more tickets from a selected tier.
    // Paid tickets remain pending until manual proof approval; free tickets bypass payment.
    public void registerTicketSelection(@NonNull String eventId,
                                        @NonNull String attendeeUid,
                                        @NonNull String attendeeName,
                                        @NonNull String attendeeEmail,
                                        @NonNull String ticketTypeId,
                                        int quantity,
                                        boolean isFreeEvent,
                                        @NonNull RegistrationCallback callback) {

        if (isFreeEvent) {
            registerFreeRsvp(eventId, attendeeUid, attendeeName, attendeeEmail, callback);
            return;
        }

        if (eventId.trim().isEmpty() || attendeeUid.trim().isEmpty()
                || ticketTypeId.trim().isEmpty()) {
            callback.onFailure("eventId, user and ticket type are required");
            return;
        }
        if (quantity < 1 || quantity > 10) {
            callback.onFailure("Quantity must be between 1 and 10");
            return;
        }

        DocumentReference eventRef = db.collection(EVENTS_COLLECTION).document(eventId);
        DocumentReference ticketTypeRef = eventRef.collection("ticketTypes").document(ticketTypeId);

        db.runTransaction(transaction -> {
            DocumentSnapshot eventSnap = transaction.get(eventRef);
            if (!eventSnap.exists()) throw new IllegalStateException("Event not found");

            DocumentSnapshot typeSnap = transaction.get(ticketTypeRef);
            if (!typeSnap.exists()) throw new IllegalStateException("Ticket type not found");

            Long capacityL = eventSnap.getLong("capacity");
            Long soldCountL = eventSnap.getLong("soldCount");
            Long qtyL = typeSnap.getLong("quantity");
            Long soldL = typeSnap.getLong("sold");

            long capacity = capacityL == null ? 0 : capacityL;
            long soldCount = soldCountL == null ? 0 : soldCountL;
            long tierQty = qtyL == null ? 0 : qtyL;
            long tierSold = soldL == null ? 0 : soldL;

            boolean eventHasSpace = capacity <= 0 || soldCount + quantity <= capacity;
            boolean tierHasSpace = tierQty <= 0 || tierSold + quantity <= tierQty;

            if (!eventHasSpace || !tierHasSpace) {
                throw new IllegalStateException("Not enough tickets available for this tier");
            }

            String registrationId = UUID.randomUUID().toString();
            DocumentReference regRef = db.collection(REGISTRATIONS_COLLECTION)
                    .document(registrationId);

            double unitPrice = readDouble(typeSnap, "price");

            Map<String, Object> reg = new HashMap<>();
            reg.put("eventId", eventId);
            reg.put("userId", attendeeUid);
            reg.put("attendeeName", attendeeName);
            reg.put("email", attendeeEmail);
            reg.put("ticketType", ticketTypeId);
            reg.put("quantity", quantity);
            reg.put("unitPrice", unitPrice);
            reg.put("totalPrice", unitPrice * quantity);
            reg.put("status", "pending_payment");
            reg.put("registeredAt", FieldValue.serverTimestamp());

            transaction.set(regRef, reg);
            transaction.update(eventRef, "soldCount", soldCount + quantity);
            transaction.update(ticketTypeRef, "sold", tierSold + quantity);

            return registrationId;
        }).addOnSuccessListener(callback::onConfirmed)
                .addOnFailureListener(e -> callback.onFailure(msg(e)));
    }

    public void saveDefaultTicketTiers(@NonNull String eventId,
                                       boolean isFreeEvent,
                                       @NonNull TicketCallback callback) {
        if (eventId.trim().isEmpty()) {
            callback.onFailure("eventId required");
            return;
        }

        WriteBatch batch = db.batch();
        DocumentReference eventRef = db.collection(EVENTS_COLLECTION).document(eventId);

        if (isFreeEvent) {
            DocumentReference freeRef = eventRef.collection("ticketTypes").document("free_rsvp");
            Map<String, Object> free = new HashMap<>();
            free.put("name", "Free RSVP");
            free.put("price", 0);
            free.put("quantity", 0);
            free.put("sold", 0);
            free.put("tierOrder", 0);
            free.put("updatedAt", FieldValue.serverTimestamp());
            batch.set(freeRef, free, com.google.firebase.firestore.SetOptions.merge());
        } else {
            addTierToBatch(batch, eventRef, "early_bird", "Early Bird", 300, 50, 1);
            addTierToBatch(batch, eventRef, "standard", "Standard", 500, 150, 2);
            addTierToBatch(batch, eventRef, "vip", "VIP", 1000, 25, 3);
        }

        batch.commit()
                .addOnSuccessListener(v -> callback.onSuccess("Ticket tiers saved"))
                .addOnFailureListener(e -> callback.onFailure(msg(e)));
    }

    private void addTierToBatch(WriteBatch batch, DocumentReference eventRef,
                                String id, String name, double price,
                                int quantity, int tierOrder) {
        DocumentReference ref = eventRef.collection("ticketTypes").document(id);
        Map<String, Object> tier = new HashMap<>();
        tier.put("name", name);
        tier.put("price", price);
        tier.put("quantity", quantity);
        tier.put("sold", 0);
        tier.put("tierOrder", tierOrder);
        tier.put("updatedAt", FieldValue.serverTimestamp());
        batch.set(ref, tier, com.google.firebase.firestore.SetOptions.merge());
    }

    // Story #11 — Create/update ticket type
    public void setTicketType(@NonNull String eventId,
                              @NonNull String typeId,
                              double price,
                              int quantity,
                              @NonNull TicketCallback callback) {

        if (eventId.trim().isEmpty() || typeId.trim().isEmpty()) {
            callback.onFailure("eventId/typeId required");
            return;
        }
        if (price < 0 || quantity < 0) {
            callback.onFailure("price/quantity must be >= 0");
            return;
        }

        DocumentReference typeRef = db.collection(EVENTS_COLLECTION)
                .document(eventId)
                .collection("ticketTypes")
                .document(typeId);

        Map<String, Object> data = new HashMap<>();
        data.put("price", price);
        data.put("quantity", quantity);
        data.put("updatedAt", FieldValue.serverTimestamp());

        typeRef.set(data, com.google.firebase.firestore.SetOptions.merge())
                .addOnSuccessListener(v -> callback.onSuccess("Ticket type saved"))
                .addOnFailureListener(e -> callback.onFailure(msg(e)));
    }

    // Story #12 + Option 2 — Register attendee (confirm or waitlist)
    public void registerAttendee(@NonNull String eventId,
                                 @NonNull String attendeeUid,
                                 @NonNull String attendeeName,
                                 @NonNull String attendeeEmail,
                                 @NonNull String ticketTypeId,
                                 @NonNull RegistrationCallback callback) {

        if (eventId.trim().isEmpty() || attendeeUid.trim().isEmpty()) {
            callback.onFailure("eventId/attendeeUid required");
            return;
        }

        DocumentReference eventRef = db.collection(EVENTS_COLLECTION).document(eventId);
        DocumentReference ticketTypeRef = eventRef.collection("ticketTypes").document(ticketTypeId);

        db.runTransaction(transaction -> {

            DocumentSnapshot eventSnap = transaction.get(eventRef);
            if (!eventSnap.exists()) throw new IllegalStateException("Event not found");

            Long capacityL  = eventSnap.getLong("capacity");
            Long soldCountL = eventSnap.getLong("soldCount");
            long capacity   = capacityL  == null ? 0 : capacityL;
            long soldCount  = soldCountL == null ? 0 : soldCountL;

            DocumentSnapshot typeSnap = transaction.get(ticketTypeRef);
            if (!typeSnap.exists()) throw new IllegalStateException("Ticket type not found");

            Long qtyL  = typeSnap.getLong("quantity");
            Long soldL = typeSnap.getLong("sold");
            long quantity   = qtyL  == null ? 0 : qtyL;
            long ticketSold = soldL == null ? 0 : soldL;

            boolean seatAvailable = soldCount < capacity;
            boolean tierAvailable = ticketSold < quantity;

            if (seatAvailable && tierAvailable) {
                String registrationId = UUID.randomUUID().toString();
                String ticketId = registrationId;

                DocumentReference regRef         = db.collection(REGISTRATIONS_COLLECTION).document(registrationId);
                DocumentReference issuedTicketRef = db.collection(TICKETS_COLLECTION).document(ticketId);

                Map<String, Object> reg = new HashMap<>();
                reg.put("eventId", eventId);
                reg.put("userId", attendeeUid);
                reg.put("attendeeName", attendeeName);
                reg.put("email", attendeeEmail);
                reg.put("ticketType", ticketTypeId);
                reg.put("status", "confirmed");
                reg.put("registeredAt", FieldValue.serverTimestamp());
                reg.put("ticketId", ticketId);

                Map<String, Object> ticket = new HashMap<>();
                ticket.put("eventId", eventId);
                ticket.put("userId", attendeeUid);
                ticket.put("attendeeName", attendeeName);
                ticket.put("tier", ticketTypeId);
                ticket.put("status", "valid");
                ticket.put("createdAt", FieldValue.serverTimestamp());

                transaction.set(regRef, reg);
                transaction.set(issuedTicketRef, ticket);
                transaction.update(eventRef, "soldCount", soldCount + 1);
                transaction.update(ticketTypeRef, "sold", ticketSold + 1);

                return new Result(ResultType.CONFIRMED, ticketId);

            } else {
                String waitlistId = UUID.randomUUID().toString();
                DocumentReference waitRef = db.collection(WAITLIST_COLLECTION).document(waitlistId);

                Map<String, Object> w = new HashMap<>();
                w.put("eventId", eventId);
                w.put("userId", attendeeUid);
                w.put("attendeeName", attendeeName);
                w.put("email", attendeeEmail);
                w.put("ticketType", ticketTypeId);
                w.put("status", "waitlisted");
                w.put("registeredAt", FieldValue.serverTimestamp());

                transaction.set(waitRef, w);

                return new Result(ResultType.WAITLISTED, waitlistId);
            }

        }).addOnSuccessListener(result -> {
            if (result.type == ResultType.CONFIRMED) {
                callback.onConfirmed(result.id);
            } else {
                callback.onWaitlisted(result.id);
            }
        }).addOnFailureListener(e -> callback.onFailure(msg(e)));
    }

    // Story #12 — Update event capacity
    public void updateCapacity(@NonNull String eventId,
                               int newCapacity,
                               @NonNull TicketCallback callback) {

        if (eventId.trim().isEmpty()) {
            callback.onFailure("eventId required");
            return;
        }
        if (newCapacity < 0) {
            callback.onFailure("capacity must be >= 0");
            return;
        }

        DocumentReference eventRef = db.collection(EVENTS_COLLECTION).document(eventId);

        db.runTransaction(transaction -> {
                    DocumentSnapshot snap = transaction.get(eventRef);
                    if (!snap.exists()) throw new IllegalStateException("Event not found");

                    Long soldCountL = snap.getLong("soldCount");
                    long soldCount = soldCountL == null ? 0 : soldCountL;

                    if (newCapacity < soldCount) {
                        throw new IllegalStateException(
                                "Capacity cannot be less than soldCount (" + soldCount + ")");
                    }

                    transaction.update(eventRef, "capacity", newCapacity);
                    return null;

                }).addOnSuccessListener(v -> callback.onSuccess("Capacity updated"))
                .addOnFailureListener(e -> callback.onFailure(msg(e)));
    }

    // -------------------------------------------------------------------------
    // Internals
    // -------------------------------------------------------------------------

    private enum ResultType { CONFIRMED, WAITLISTED }

    /**
     * Internal transaction result containing the result type and generated document ID.
     */

    private static class Result {
        ResultType type;
        String id;
        Result(ResultType type, String id) {
            this.type = type;
            this.id   = id;
        }
    }

    /**
     * Safely extracts an exception message for callback errors.
     *
     * @param e exception from Firebase or local validation.
     * @return readable error message.
     */

    private String msg(Exception e) {
        return (e == null || e.getMessage() == null) ? "Unknown error" : e.getMessage();
    }

    /**
     * Reads a numeric Firestore field as a double.
     *
     * @param snap Firestore document snapshot.
     * @param field field name to read.
     * @return numeric value, or 0 if missing.
     */


    private double readDouble(DocumentSnapshot snap, String field) {
        Double asDouble = snap.getDouble(field);
        if (asDouble != null) return asDouble;

        Long asLong = snap.getLong(field);
        return asLong == null ? 0 : asLong;
    }
}
