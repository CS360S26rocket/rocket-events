/*
 * This file defines NotificationRepository, a data repository used by the Scene app.
 * It contains notification creation, delivery, reading, and event update records.
 * Its functions include getUserNotifications, queue24HourRemindersForUser, broadcastUpdateToAttendees, sendTicketApprovedNotification to load data, handle user actions, validate input, and save results.
 * It connects this feature to the Scene app's UI, data, navigation, and verification flow.
 */

package com.example.seprojectpart3;

import androidx.annotation.NonNull;

import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
/**
 * Handles in-app and email notification workflows for campus users.
 *
 * This repository stores notification records in Firestore and supports event
 * reminders, organizer broadcasts, ticket approval notices, and cancellation
 * notices for registered attendees.
 */
public class NotificationRepository {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public interface NotificationCallback {
        void onSuccess(String message);
        void onFailure(String error);
    }

    /**
     * Callback for notification actions that return a status message.
     */
    public interface NotificationListCallback {
        void onSuccess(List<Map<String, Object>> notifications);
        void onFailure(String error);
    }
    /**
     * Loads all notifications for a user, newest first.
     *
     * @param userId Firebase UID of the campus user
     * @param callback returns notification maps with notificationId included
     */
    public void getUserNotifications(@NonNull String userId,
                                     @NonNull NotificationListCallback callback) {
        if (userId.trim().isEmpty()) {
            callback.onFailure("userId is required.");
            return;
        }

        db.collection("notifications")
                .whereEqualTo("userId", userId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(snap -> {
                    List<Map<String, Object>> results = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snap) {
                        Map<String, Object> notification = doc.getData();
                        notification.put("notificationId", doc.getId());
                        results.add(notification);
                    }
                    callback.onSuccess(results);
                })
                .addOnFailureListener(e -> callback.onFailure(message(e)));
    }

    public void queue24HourRemindersForUser(@NonNull String userId,
                                            @NonNull NotificationCallback callback) {
        if (userId.trim().isEmpty()) {
            callback.onFailure("userId is required.");
            return;
        }

        db.collection("registrations")
                .whereEqualTo("userId", userId)
                .whereEqualTo("status", "confirmed")
                .get()
                .addOnSuccessListener(regSnap -> {
                    if (regSnap.isEmpty()) {
                        callback.onSuccess("No confirmed events need reminders.");
                        return;
                    }

                    AtomicInteger remaining = new AtomicInteger(regSnap.size());
                    AtomicInteger queued = new AtomicInteger(0);

                    for (QueryDocumentSnapshot reg : regSnap) {
                        String eventId = reg.getString("eventId");
                        String registrationId = reg.getId();

                        if (isBlank(eventId)) {
                            finishReminder(callback, remaining, queued);
                            continue;
                        }

                        db.collection("events").document(eventId)
                                .get()
                                .addOnSuccessListener(event -> {
                                    String date = event.getString("date");
                                    String title = event.getString("title");

                                    if (isTomorrow(date)) {
                                        String notificationId = userId + "_" + eventId + "_24h";

                                        Map<String, Object> notification = new HashMap<>();
                                        notification.put("userId", userId);
                                        notification.put("eventId", eventId);
                                        notification.put("registrationId", registrationId);
                                        notification.put("type", "24_hour_reminder");
                                        notification.put("title", "Event reminder");
                                        notification.put("message", "Your event is tomorrow: " +
                                                fallback(title, eventId));
                                        notification.put("status", "queued");
                                        notification.put("createdAt", FieldValue.serverTimestamp());

                                        db.collection("notifications")
                                                .document(notificationId)
                                                .set(notification)
                                                .addOnSuccessListener(v -> {
                                                    queued.incrementAndGet();
                                                    finishReminder(callback, remaining, queued);
                                                })
                                                .addOnFailureListener(e -> callback.onFailure(message(e)));
                                    } else {
                                        finishReminder(callback, remaining, queued);
                                    }
                                })
                                .addOnFailureListener(e -> callback.onFailure(message(e)));
                    }
                })
                .addOnFailureListener(e -> callback.onFailure(message(e)));
    }


    public void broadcastUpdateToAttendees(@NonNull String eventId,
                                           @NonNull String organizerUid,
                                           @NonNull String updateMessage,
                                           @NonNull NotificationCallback callback) {
        broadcastUpdateToAttendees(eventId, organizerUid, "organizer_broadcast",
                "Event update", updateMessage, callback);
    }

    public void broadcastUpdateToAttendees(@NonNull String eventId,
                                           @NonNull String organizerUid,
                                           @NonNull String type,
                                           @NonNull String title,
                                           @NonNull String updateMessage,
                                           @NonNull NotificationCallback callback) {
        if (eventId.trim().isEmpty() || updateMessage.trim().isEmpty()) {
            callback.onFailure("eventId and message are required.");
            return;
        }

        db.collection("registrations")
                .whereEqualTo("eventId", eventId)
                .whereEqualTo("status", "confirmed")
                .get()
                .addOnSuccessListener(regSnap -> {
                    if (regSnap.isEmpty()) {
                        callback.onSuccess("No confirmed attendees to notify.");
                        return;
                    }

                    AtomicInteger remaining = new AtomicInteger(regSnap.size());
                    AtomicInteger sent = new AtomicInteger(0);

                    for (QueryDocumentSnapshot reg : regSnap) {
                        String userId = reg.getString("userId");

                        if (isBlank(userId)) {
                            finishBroadcast(callback, remaining, sent);
                            continue;
                        }

                        Map<String, Object> notification = new HashMap<>();
                        notification.put("userId", userId);
                        notification.put("eventId", eventId);
                        notification.put("organizerUid", organizerUid);
                        notification.put("type", type);
                        notification.put("title", title);
                        notification.put("message", updateMessage);
                        notification.put("status", "queued");
                        notification.put("createdAt", FieldValue.serverTimestamp());

                        db.collection("notifications")
                                .add(notification)
                                .addOnSuccessListener(ref -> {
                                    sent.incrementAndGet();
                                    finishBroadcast(callback, remaining, sent);
                                })
                                .addOnFailureListener(e -> callback.onFailure(message(e)));
                    }
                })
                .addOnFailureListener(e -> callback.onFailure(message(e)));
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
                + " has been approved.";

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

    private void finishReminder(NotificationCallback callback,
                                AtomicInteger remaining,
                                AtomicInteger queued) {
        if (remaining.decrementAndGet() == 0) {
            callback.onSuccess("Queued " + queued.get() + " reminder(s).");
        }
    }

    private void finishBroadcast(NotificationCallback callback,
                                 AtomicInteger remaining,
                                 AtomicInteger sent) {
        if (remaining.decrementAndGet() == 0) {
            callback.onSuccess("Queued " + sent.get() + " update(s).");
        }
    }

    private boolean isTomorrow(String eventDate) {
        if (eventDate == null || eventDate.trim().isEmpty()) return false;

        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        Calendar tomorrow = Calendar.getInstance();
        tomorrow.add(Calendar.DAY_OF_YEAR, 1);

        return format.format(tomorrow.getTime()).equals(eventDate);
    }

    private void sendEmailIfPresent(String email, String subject, String body) {
        if (isBlank(email)) return;

        new Thread(() -> {
            try {
                EmailSender.sendPlainEmail(email, subject, body);
            } catch (Exception ignored) {
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
