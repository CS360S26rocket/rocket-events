package com.example.seprojectpart3;

import android.widget.TextView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.AggregateQuerySnapshot;
import com.google.firebase.firestore.AggregateSource;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * Story #47 — Show Attendee Count on Event (M4, Sprint 5, Day 1)
 *
 * COUNT query on registrations collection — reuses the registrations
 * structure from #14 (RegistrationRepository, Sprint 2).
 *
 * Provides:
 *   - getAttendeeCount(): raw count via callback
 *   - bindAttendeeCount(): convenience method that directly sets a TextView
 *
 * Done by day 3. After completion, assist M2 on #43 cron infra
 * or M1 on #42 email edge cases.
 */
public class AttendeeCountHelper {

    private static final String REGISTRATIONS_COLLECTION = "registrations";
    private final FirebaseFirestore db;

    public AttendeeCountHelper() {
        this.db = FirebaseFirestore.getInstance();
    }

    /**
     * Get the number of attendees registered for an event.
     * Uses Firestore count() aggregation — does not download all documents.
     *
     * @param eventId   Firestore document ID of the event
     * @param onSuccess returns the count as a long
     * @param onFailure returns the error
     */
    public void getAttendeeCount(String eventId,
                                 OnSuccessListener<Long> onSuccess,
                                 OnFailureListener onFailure) {

        db.collection(REGISTRATIONS_COLLECTION)
                .whereEqualTo("eventId", eventId)
                .count()
                .get(AggregateSource.SERVER)
                .addOnSuccessListener(snapshot -> {
                    long count = snapshot.getCount();
                    onSuccess.onSuccess(count);
                })
                .addOnFailureListener(onFailure);
    }

    /**
     * Convenience: fetch count and bind it directly to a TextView.
     * Displays "X attending" or "No attendees yet".
     *
     * Usage in any Activity/Fragment:
     *   new AttendeeCountHelper().bindAttendeeCount(eventId, tvAttendeeCount);
     *
     * @param eventId  Firestore document ID of the event
     * @param textView the TextView to update
     */
    public void bindAttendeeCount(String eventId, TextView textView) {
        textView.setText("Loading...");

        getAttendeeCount(eventId,
                count -> {
                    if (count == 0) {
                        textView.setText("No attendees yet");
                    } else if (count == 1) {
                        textView.setText("1 attending");
                    } else {
                        textView.setText(count + " attending");
                    }
                },
                error -> textView.setText("— attending")
        );
    }

    /**
     * Fallback for older Firestore SDKs that don't support count().
     * Downloads documents and counts locally — use only if count() isn't available.
     */
    public void getAttendeeCountFallback(String eventId,
                                         OnSuccessListener<Long> onSuccess,
                                         OnFailureListener onFailure) {

        db.collection(REGISTRATIONS_COLLECTION)
                .whereEqualTo("eventId", eventId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    long count = querySnapshot.size();
                    onSuccess.onSuccess(count);
                })
                .addOnFailureListener(onFailure);
    }
}
