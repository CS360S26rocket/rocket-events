package com.example.seprojectpart3;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class OrganizerEventUpdateActivity extends AppCompatActivity {

    private EditText etUpdatedDate, etUpdatedTime, etUpdatedVenue, etUpdateMessage;
    private TextView tvEventName, tvStatus, chipVenue, chipTime, chipLive;
    private String eventId, organizerUid, eventTitle, eventDate, eventVenue;
    private String notificationType = "venue_changed";
    private String notificationTitle = "Venue changed";
    private final NotificationRepository notificationRepo = new NotificationRepository();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_organizer_event_update);

        eventId = getIntent().getStringExtra("eventId");
        organizerUid = getIntent().getStringExtra("organizerUid");
        eventTitle = fallback(getIntent().getStringExtra("eventTitle"), "this event");
        eventDate = fallback(getIntent().getStringExtra("eventDate"), "");
        eventVenue = fallback(getIntent().getStringExtra("eventVenue"), "");

        tvEventName = findViewById(R.id.tvUpdateEventName);
        etUpdatedDate = findViewById(R.id.etUpdatedDate);
        etUpdatedTime = findViewById(R.id.etUpdatedTime);
        etUpdatedVenue = findViewById(R.id.etUpdatedVenue);
        etUpdateMessage = findViewById(R.id.etUpdateMessage);
        tvStatus = findViewById(R.id.tvUpdateStatus);
        chipVenue = findViewById(R.id.chipVenueChanged);
        chipTime = findViewById(R.id.chipTimeChanged);
        chipLive = findViewById(R.id.chipLiveUpdate);

        tvEventName.setText(eventTitle);
        etUpdatedDate.setText(eventDate);
        etUpdatedVenue.setText(eventVenue);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnSendUpdate).setOnClickListener(v -> sendUpdate());
        chipVenue.setOnClickListener(v -> selectType("venue_changed", "Venue changed"));
        chipTime.setOnClickListener(v -> selectType("time_changed", "Time changed"));
        chipLive.setOnClickListener(v -> selectType("live_update", "Live update"));
        findViewById(R.id.btnLiveGateOpen).setOnClickListener(v -> quickLive("Gate open"));
        findViewById(R.id.btnLiveVenueShifted).setOnClickListener(v -> {
            etUpdateMessage.setText("Venue shifted. Please follow the latest venue details in this update.");
            selectType("venue_changed", "Venue changed");
        });
        findViewById(R.id.btnLivePerformance).setOnClickListener(v -> quickLive("Performance starting"));
        renderTypeChips();
    }

    private void quickLive(String message) {
        etUpdateMessage.setText(message);
        selectType("live_update", "Live update");
    }

    private void selectType(String type, String title) {
        notificationType = type;
        notificationTitle = title;
        renderTypeChips();
    }

    private void renderTypeChips() {
        setChip(chipVenue, "venue_changed".equals(notificationType));
        setChip(chipTime, "time_changed".equals(notificationType));
        setChip(chipLive, "live_update".equals(notificationType));
    }

    private void setChip(TextView chip, boolean active) {
        chip.setBackgroundResource(active ? R.drawable.bg_button_primary : R.drawable.bg_button_ghost);
        chip.setTextColor(getColor(active ? R.color.button_text_dark : R.color.text_primary));
    }

    private void sendUpdate() {
        if (eventId == null || eventId.trim().isEmpty()) {
            show("Event not found.", true);
            return;
        }

        String date = etUpdatedDate.getText().toString().trim();
        String time = etUpdatedTime.getText().toString().trim();
        String venue = etUpdatedVenue.getText().toString().trim();
        String note = etUpdateMessage.getText().toString().trim();

        StringBuilder message = new StringBuilder();
        message.append(eventTitle).append(" has an update.");
        if (!date.isEmpty()) message.append("\nDate: ").append(date);
        if (!time.isEmpty()) message.append("\nTime: ").append(time);
        if (!venue.isEmpty()) message.append("\nVenue: ").append(venue);
        if (!note.isEmpty()) message.append("\nNote: ").append(note);

        show("Sending update...", false);
        notificationRepo.broadcastUpdateToAttendees(
                eventId,
                fallback(organizerUid, ""),
                notificationType,
                notificationTitle,
                message.toString(),
                new NotificationRepository.NotificationCallback() {
                    @Override public void onSuccess(String result) {
                        openOrganizerSuccess("Update sent",
                                "Attendees have been notified successfully.");
                    }

                    @Override public void onFailure(String error) {
                        show(error, true);
                    }
                });
    }

    private String fallback(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value;
    }

    private void show(String message, boolean error) {
        tvStatus.setVisibility(View.VISIBLE);
        tvStatus.setText(message);
        tvStatus.setTextColor(getColor(error ? R.color.error_red : R.color.accent_lime));
    }

    private void openOrganizerSuccess(String title, String message) {
        Intent intent = new Intent(this, PaymentSuccessActivity.class);
        intent.putExtra("target", "organizer");
        intent.putExtra("uid", organizerUid);
        intent.putExtra("title", title);
        intent.putExtra("message", message);
        startActivity(intent);
        finish();
    }
}
