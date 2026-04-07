package com.example.seprojectpart3;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.List;

public class EventCancellationRepository {

    private static final String TAG = "EventCancellation";

    // Firestore collections + fields
    private static final String EVENTS_COL = "events";
    private static final String TICKETS_COL = "tickets";
    private static final String EVENT_FIELD_STATUS = "status";
    private static final String TICKET_FIELD_STATUS = "status";
    private static final String TICKET_FIELD_EVENT = "eventId";
    private static final String STATUS_CANCELLED = "cancelled";

    private final FirebaseFirestore db;

    public EventCancellationRepository() {
        this.db = FirebaseFirestore.getInstance();
    }

    // ── Callback ─────────────────────────────────────────

    public interface CancellationCallback {
        void onSuccess(int attendeesNotified);
        void onSuccessButNotificationFailed(int attendeeCount);
        void onFailure(Exception e);
    }

    // ── Main cancel method ───────────────────────────────

    public void cancelEvent(@NonNull String eventId,
                            @NonNull String eventName,
                            String cancellationReason,
                            @NonNull CancellationCallback callback) {

        db.collection(TICKETS_COL)
                .whereEqualTo(TICKET_FIELD_EVENT, eventId)
                .get()
                .addOnSuccessListener(ticketSnapshot -> {

                    List<String> ticketIds = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : ticketSnapshot) {
                        ticketIds.add(doc.getId());
                    }

                    cancelEventInFirestore(eventId, cancellationReason, ticketIds)
                            .addOnSuccessListener(unused -> {
                                Log.d(TAG, "Event cancelled. Tickets: " + ticketIds.size());

                                sendCancellationNotification(
                                        eventId,
                                        eventName,
                                        cancellationReason,
                                        ticketIds.size(),
                                        callback
                                );
                            })
                            .addOnFailureListener(callback::onFailure);
                })
                .addOnFailureListener(callback::onFailure);
    }

    // ── Firestore batch ──────────────────────────────────

    private Task<Void> cancelEventInFirestore(String eventId,
                                              String reason,
                                              List<String> ticketIds) {

        WriteBatch batch = db.batch();

        // Update event
        batch.update(
                db.collection(EVENTS_COL).document(eventId),
                EVENT_FIELD_STATUS, STATUS_CANCELLED,
                "cancelledAt", FieldValue.serverTimestamp(),
                "cancellationReason", reason != null ? reason : "",
                "notificationFailed", false
        );

        // Update tickets
        for (String ticketId : ticketIds) {
            batch.update(
                    db.collection(TICKETS_COL).document(ticketId),
                    TICKET_FIELD_STATUS, STATUS_CANCELLED,
                    "cancelledAt", FieldValue.serverTimestamp()
            );
        }

        return batch.commit();
    }

    // ── Notification queue (for Cloud Function) ──────────

    private void sendCancellationNotification(String eventId,
                                              String eventName,
                                              String reason,
                                              int attendeeCount,
                                              CancellationCallback callback) {

        String topic = "event_" + eventId;
        String title = eventName + " has been cancelled";
        String body = (reason != null && !reason.isEmpty())
                ? "Reason: " + reason
                : "This event has been cancelled.";

        java.util.Map<String, Object> data = new java.util.HashMap<>();
        data.put("topic", topic);
        data.put("title", title);
        data.put("body", body);
        data.put("eventId", eventId);
        data.put("createdAt", FieldValue.serverTimestamp());
        data.put("sent", false);

        db.collection("notification_queue")
                .add(data)
                .addOnSuccessListener(ref -> {
                    Log.d(TAG, "Notification queued");
                    callback.onSuccess(attendeeCount);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Notification failed", e);

                    db.collection(EVENTS_COL).document(eventId)
                            .update("notificationFailed", true)
                            .addOnCompleteListener(task ->
                                    callback.onSuccessButNotificationFailed(attendeeCount)
                            );
                });
    }

    // ── Retry notification ───────────────────────────────

    public void retryNotification(@NonNull String eventId,
                                  @NonNull String eventName,
                                  String reason,
                                  @NonNull CancellationCallback callback) {

        db.collection(TICKETS_COL)
                .whereEqualTo(TICKET_FIELD_EVENT, eventId)
                .get()
                .addOnSuccessListener(snap ->
                        sendCancellationNotification(
                                eventId,
                                eventName,
                                reason,
                                snap.size(),
                                callback
                        ))
                .addOnFailureListener(callback::onFailure);
    }
}