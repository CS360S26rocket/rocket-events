package com.example.seprojectpart3;

import androidx.annotation.NonNull;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TicketRepository {

    private static final String EVENTS_COLLECTION = "events";
    private static final String REGISTRATIONS_COLLECTION = "registrations";
    private static final String WAITLIST_COLLECTION = "waitlist";
    private static final String TICKETS_COLLECTION = "tickets";

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public interface TicketCallback {
        void onSuccess(String message);
        void onFailure(String error);
    }

    public interface RegistrationCallback {
        void onConfirmed(String ticketId);
        void onWaitlisted(String waitlistId);
        void onFailure(String error);
    }

    // Story #11
    // Creates/updates ticket type under: events/{eventId}/ticketTypes/{typeId}
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

        // Merge to avoid resetting sold accidentally
        Map<String, Object> data = new HashMap<>();
        data.put("price", price);
        data.put("quantity", quantity);
        data.put("updatedAt", FieldValue.serverTimestamp());

        typeRef.set(data, com.google.firebase.firestore.SetOptions.merge())
                .addOnSuccessListener(v -> callback.onSuccess("Ticket type saved"))
                .addOnFailureListener(e -> callback.onFailure(msg(e)));
    }

    // Story #12 + Option 2 (tickets/{ticketId})
    // Transaction: enforce event capacity + ticket quantity, confirm or waitlist.
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

            Long capacityL = eventSnap.getLong("capacity");
            Long soldCountL = eventSnap.getLong("soldCount");

            long capacity = capacityL == null ? 0 : capacityL;
            long soldCount = soldCountL == null ? 0 : soldCountL;

            DocumentSnapshot typeSnap = transaction.get(ticketTypeRef);
            if (!typeSnap.exists()) throw new IllegalStateException("Ticket type not found");

            Long qtyL = typeSnap.getLong("quantity");
            Long soldL = typeSnap.getLong("sold");

            long quantity = qtyL == null ? 0 : qtyL;
            long ticketSold = soldL == null ? 0 : soldL;

            boolean seatAvailable = soldCount < capacity;
            boolean tierAvailable = ticketSold < quantity;

            if (seatAvailable && tierAvailable) {
                // CONFIRMED
                String registrationId = UUID.randomUUID().toString();
                String ticketId = registrationId; // ticketId == registrationId (QR can encode ticketId only)

                DocumentReference regRef = db.collection(REGISTRATIONS_COLLECTION).document(registrationId);
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
                ticket.put("status", "valid"); // standardized: valid | used | cancelled
                ticket.put("createdAt", FieldValue.serverTimestamp());

                transaction.set(regRef, reg);
                transaction.set(issuedTicketRef, ticket);

                transaction.update(eventRef, "soldCount", soldCount + 1);
                transaction.update(ticketTypeRef, "sold", ticketSold + 1);

                return new Result(ResultType.CONFIRMED, ticketId);

            } else {
                // WAITLISTED
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

    // Story #12
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
                        throw new IllegalStateException("Capacity cannot be less than soldCount (" + soldCount + ")");
                    }

                    transaction.update(eventRef, "capacity", newCapacity);
                    return null;
                }).addOnSuccessListener(v -> callback.onSuccess("Capacity updated"))
                .addOnFailureListener(e -> callback.onFailure(msg(e)));
    }

    private enum ResultType { CONFIRMED, WAITLISTED }

    private static class Result {
        ResultType type;
        String id;
        Result(ResultType type, String id) {
            this.type = type;
            this.id = id;
        }
    }

    private String msg(Exception e) {
        return (e == null || e.getMessage() == null) ? "Unknown error" : e.getMessage();
    }
}