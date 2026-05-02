package com.example.seprojectpart3;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class InfoSetupActivity extends AppCompatActivity {

    EditText etEntryReqs, etOrganizerContact, etAdditionalInfo;
    Switch switchAnnouncements;
    TextView tvStatus;
    EventRepository eventRepo;

    // All collected data
    String organizerUid, title, description, venue, startDate, startTime,
            endDate, endTime, location, institutionName, eventType, category,
            capacity, ticketMode, salesStart, salesStartTime, salesEnd, salesEndTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_info_setup);

        // Receive all data
        Intent i        = getIntent();
        organizerUid    = i.getStringExtra("organizerUid");
        title           = i.getStringExtra("title");
        description     = i.getStringExtra("description");
        venue           = i.getStringExtra("venue");
        startDate       = i.getStringExtra("startDate");
        startTime       = i.getStringExtra("startTime");
        endDate         = i.getStringExtra("endDate");
        endTime         = i.getStringExtra("endTime");
        location        = i.getStringExtra("location");
        institutionName = i.getStringExtra("institutionName");
        eventType       = i.getStringExtra("eventType");
        category        = i.getStringExtra("category");
        capacity        = i.getStringExtra("capacity");
        ticketMode      = i.getStringExtra("ticketMode");
        salesStart      = i.getStringExtra("salesStart");
        salesStartTime  = i.getStringExtra("salesStartTime");
        salesEnd        = i.getStringExtra("salesEnd");
        salesEndTime    = i.getStringExtra("salesEndTime");

        etEntryReqs          = findViewById(R.id.etEntryReqs);
        etOrganizerContact   = findViewById(R.id.etOrganizerContact);
        etAdditionalInfo     = findViewById(R.id.etAdditionalInfo);
        switchAnnouncements  = findViewById(R.id.switchAnnouncements);
        tvStatus             = findViewById(R.id.tvStatus);
        eventRepo            = new EventRepository();

        // Back
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // Publish Event
        findViewById(R.id.btnPublish).setOnClickListener(v -> publishEvent());
    }

    private void publishEvent() {
        // Use startDate as the event date for createEvent
        String eventDate = startDate + (startTime.isEmpty() ? "" : " " + startTime);

        tvStatus.setVisibility(View.VISIBLE);
        tvStatus.setText("Publishing event...");

        // Step 1: Create the event
        // Story #25: derive isFree from the ticketMode chosen in TicketSetupActivity
        boolean isFree = "free_rsvp".equals(ticketMode);

        eventRepo.createEvent(
                organizerUid, title, description, eventDate, venue, isFree,
                new EventRepository.EventCallback() {
                    @Override
                    public void onSuccess(String eventId) {
                        // Step 2: Set ticket capacity
                        if (capacity != null && !capacity.isEmpty()) {
                            int cap = Integer.parseInt(capacity);
                            TicketRepository ticketRepo = new TicketRepository();
                            ticketRepo.updateCapacity(eventId, cap,
                                    new TicketRepository.TicketCallback() {
                                        @Override public void onSuccess(String id) { /* ok */ }
                                        @Override public void onFailure(String e) { /* non-fatal */ }
                                    });
                        }

                        // Step 3: Set sales timing if provided
                        if (salesStart != null && !salesStart.isEmpty() &&
                                salesEnd   != null && !salesEnd.isEmpty()) {
                            eventRepo.setSalesTime(eventId, salesStart, salesEnd,
                                    new EventRepository.EventCallback() {
                                        @Override public void onSuccess(String id) { /* ok */ }
                                        @Override public void onFailure(String e) { /* non-fatal */ }
                                    });
                        }

                        // Success — go back to dashboard
                        tvStatus.setText("Event published! ID: " + eventId);
                        tvStatus.setTextColor(getColor(R.color.accent_lime));

                        // Navigate back to dashboard after a short delay
                        tvStatus.postDelayed(() -> {
                            Intent intent = new Intent(InfoSetupActivity.this,
                                    OrganizerDashboardActivity.class);
                            intent.putExtra("uid", organizerUid);
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            startActivity(intent);
                            finish();
                        }, 1500);
                    }

                    @Override
                    public void onFailure(String error) {
                        tvStatus.setText("Error: " + error);
                        tvStatus.setTextColor(getColor(R.color.error_red));
                    }
                });
    }
}