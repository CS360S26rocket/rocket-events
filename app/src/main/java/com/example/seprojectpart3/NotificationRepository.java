package com.example.seprojectpart3;

import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

// M1 Sprint 5 notification backend.
// #42: ticket approval confirmation through in-app notification + email.
// #44: event cancellation alert to registered users.
public class NotificationRepository {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public interface NotificationCallback {
        void onSuccess(String message);
        void onFailure(String error);
    }

    public void sendTicketApprovedNotification(String userId, String email,
                                               String eventId, String eventTitle,
                                               String ticketId,
                                               NotificationCallback callback) {
        if (isBlank(userId) || isBlank(eventId) || isBlank(ticketId)) {
            callback.onFailure("userId, eventId and ticketId are required");
            return;
        }

        String title = "Ticket confirmed";
        String body = "Your ticket for " + fallback(eventTitle, "this event")
                + " has been approved. Ticket ID: " + ticketId;

        createInAppNotification(userId, eventId, "ticket_approved", title, body,
                new NotificationCallback() {
                    @Override public void onSuccess(String message) {
                        sendEmailIfPresent(email, title, body);
                        callback.onSuccess("Ticket confirmation notification sent");
                    }

                    @Override public void onFailure(String error) {
                        callback.onFailure(error);
                    }
                });
    }

    public void notifyRegisteredUsersEventCancelled(String eventId,
                                                   String eventTitle,
                                                   NotificationCallback callback) {
        if (isBlank(eventId)) {
            callback.onFailure("eventId is required");
            return;
        }

        db.collection("registrations")
                .whereEqualTo("eventId", eventId)
                .get()
                .addOnSuccessListener(snap -> {
                    if (snap.isEmpty()) {
                        callback.onSuccess("Event cancelled; no registered users to notify");
                        return;
                    }

                    AtomicInteger remaining = new AtomicInteger(snap.size());
                    AtomicInteger failures = new AtomicInteger(0);
                    String title = "Event cancelled";

                    for (QueryDocumentSnapshot doc : snap) {
                        String userId = doc.getString("userId");
                        String email = doc.getString("email");
                        String body = fallback(eventTitle, "Your event")
                                + " has been cancelled by the organizer.";

                        createInAppNotification(userId, eventId, "event_cancelled",
                                title, body, new NotificationCallback() {
                                    @Override public void onSuccess(String message) {
                                        sendEmailIfPresent(email, title, body);
                                        finishOne();
                                    }

                                    @Override public void onFailure(String error) {
                                        failures.incrementAndGet();
                                        finishOne();
                                    }

                                    private void finishOne() {
                                        if (remaining.decrementAndGet() == 0) {
                                            if (failures.get() == 0) {
                                                callback.onSuccess("Cancellation notifications sent");
                                            } else {
                                                callback.onFailure("Some cancellation notifications failed");
                                            }
                                        }
                                    }
                                });
                    }
                })
                .addOnFailureListener(e -> callback.onFailure(message(e)));
    }

    private void createInAppNotification(String userId, String eventId,
                                         String type, String title, String body,
                                         NotificationCallback callback) {
        if (isBlank(userId)) {
            callback.onFailure("Notification user is missing");
            return;
        }

        Map<String, Object> notification = new HashMap<>();
        notification.put("userId", userId);
        notification.put("eventId", eventId);
        notification.put("type", type);
        notification.put("title", title);
        notification.put("body", body);
        notification.put("read", false);
        notification.put("createdAt", FieldValue.serverTimestamp());

        db.collection("notifications")
                .add(notification)
                .addOnSuccessListener(ref -> callback.onSuccess(ref.getId()))
                .addOnFailureListener(e -> callback.onFailure(message(e)));
    }

    private void sendEmailIfPresent(String email, String subject, String body) {
        if (isBlank(email)) return;

        new Thread(() -> {
            try {
                EmailSender.sendPlainEmail(email, subject, body);
            } catch (Exception ignored) {
                // In-app notification is the durable source; email failure should not
                // roll back the app state.
            }
        }).start();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String fallback(String value, String fallback) {
        return isBlank(value) ? fallback : value;
    }

    private String message(Exception e) {
        return e == null || e.getMessage() == null ? "Unknown error" : e.getMessage();
    }
}
