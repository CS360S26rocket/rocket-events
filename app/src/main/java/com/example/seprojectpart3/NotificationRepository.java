package com.example.seprojectpart3;

import androidx.annotation.NonNull;

import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class NotificationRepository {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public interface NotificationCallback {
        void onSuccess(String message);
        void onFailure(String error);
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
                    }
                })
                .addOnFailureListener(e -> callback.onFailure(message(e)));
    }

    private boolean isTomorrow(String eventDate) {
        if (eventDate == null || eventDate.trim().isEmpty()) return false;

        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        Calendar tomorrow = Calendar.getInstance();
        tomorrow.add(Calendar.DAY_OF_YEAR, 1);

        String tomorrowText = format.format(tomorrow.getTime());
        return tomorrowText.equals(eventDate);
    }

    private String message(Exception e) {
        return e == null || e.getMessage() == null ? "Unknown error" : e.getMessage();
    }
}
