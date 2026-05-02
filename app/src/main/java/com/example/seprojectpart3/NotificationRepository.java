package com.example.seprojectpart3;

<<<<<<< HEAD
import androidx.annotation.NonNull;

=======
>>>>>>> origin/Eman
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

<<<<<<< HEAD
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

=======
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

// M1 Sprint 5 notification backend.
// #42: ticket approval confirmation through in-app notification + email.
// #44: event cancellation alert to registered users.
>>>>>>> origin/Eman
public class NotificationRepository {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public interface NotificationCallback {
        void onSuccess(String message);
        void onFailure(String error);
    }

<<<<<<< HEAD
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

                        if (eventId == null || eventId.isEmpty()) {
                            if (remaining.decrementAndGet() == 0) {
                                callback.onSuccess("Queued " + queued.get() + " reminder(s).");
                            }
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
                                                (title == null ? eventId : title));
                                        notification.put("status", "queued");
                                        notification.put("createdAt", FieldValue.serverTimestamp());

                                        db.collection("notifications")
                                                .document(notificationId)
                                                .set(notification)
                                                .addOnSuccessListener(v -> {
                                                    queued.incrementAndGet();
                                                    if (remaining.decrementAndGet() == 0) {
                                                        callback.onSuccess("Queued " + queued.get() + " reminder(s).");
                                                    }
                                                })
                                                .addOnFailureListener(e -> callback.onFailure(message(e)));
                                    } else {
                                        if (remaining.decrementAndGet() == 0) {
                                            callback.onSuccess("Queued " + queued.get() + " reminder(s).");
                                        }
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

        if (eventId.trim().isEmpty() || updateMessage.trim().isEmpty()) {
            callback.onFailure("eventId and message are required.");
=======
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
>>>>>>> origin/Eman
            return;
        }

        db.collection("registrations")
                .whereEqualTo("eventId", eventId)
<<<<<<< HEAD
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

                        if (userId == null || userId.isEmpty()) {
                            if (remaining.decrementAndGet() == 0) {
                                callback.onSuccess("Queued " + sent.get() + " update(s).");
                            }
                            continue;
                        }

                        Map<String, Object> notification = new HashMap<>();
                        notification.put("userId", userId);
                        notification.put("eventId", eventId);
                        notification.put("organizerUid", organizerUid);
                        notification.put("type", "organizer_broadcast");
                        notification.put("title", "Event update");
                        notification.put("message", updateMessage);
                        notification.put("status", "queued");
                        notification.put("createdAt", FieldValue.serverTimestamp());

                        db.collection("notifications")
                                .add(notification)
                                .addOnSuccessListener(ref -> {
                                    sent.incrementAndGet();
                                    if (remaining.decrementAndGet() == 0) {
                                        callback.onSuccess("Queued " + sent.get() + " update(s).");
                                    }
                                })
                                .addOnFailureListener(e -> callback.onFailure(message(e)));
=======
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
>>>>>>> origin/Eman
                    }
                })
                .addOnFailureListener(e -> callback.onFailure(message(e)));
    }

<<<<<<< HEAD
    private boolean isTomorrow(String eventDate) {
        if (eventDate == null || eventDate.trim().isEmpty()) return false;

        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        Calendar tomorrow = Calendar.getInstance();
        tomorrow.add(Calendar.DAY_OF_YEAR, 1);

        String tomorrowText = format.format(tomorrow.getTime());
        return tomorrowText.equals(eventDate);
=======
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
>>>>>>> origin/Eman
    }

    private String message(Exception e) {
        return e == null || e.getMessage() == null ? "Unknown error" : e.getMessage();
    }
}
