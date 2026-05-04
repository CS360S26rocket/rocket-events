/*
 * This file defines AttendeeCountHelper, a supporting Java class used by the Scene app.
 * It contains attendee counting helpers used for event capacity and dashboard totals.
 * Its functions include getAttendeeCount, bindAttendeeCount, getAttendeeCountFallback to load data, handle user actions, validate input, and save results.
 * It connects this feature to the Scene app's UI, data, navigation, and verification flow.
 */

package com.example.seprojectpart3;

import android.widget.TextView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.AggregateQuerySnapshot;
import com.google.firebase.firestore.AggregateSource;
import com.google.firebase.firestore.FirebaseFirestore;














public class AttendeeCountHelper {

    private static final String REGISTRATIONS_COLLECTION = "registrations";
    private final FirebaseFirestore db;

    public AttendeeCountHelper() {
        this.db = FirebaseFirestore.getInstance();
    }

    







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
