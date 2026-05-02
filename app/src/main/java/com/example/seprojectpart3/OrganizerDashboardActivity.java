package com.example.seprojectpart3;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class OrganizerDashboardActivity extends AppCompatActivity {

    private String organizerUid;
    private Spinner spinnerOrganizerEvents;
    private TextView tvDashboardResult;

    private final EventRepository eventRepo = new EventRepository();
    private final List<Map<String, Object>> organizerEvents = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_organizer_dashboard);

        organizerUid = getIntent().getStringExtra("uid");

        spinnerOrganizerEvents = findViewById(R.id.spinnerOrganizerEvents);
        tvDashboardResult = findViewById(R.id.tvDashboardResult);

        findViewById(R.id.btnCreateEvent).setOnClickListener(v -> openCreateEvent());
        findViewById(R.id.btnCreateEventLarge).setOnClickListener(v -> openCreateEvent());

        findViewById(R.id.btnViewAttendees).setOnClickListener(v -> viewAttendees());

        findViewById(R.id.btnSendUpdate).setOnClickListener(v ->
                show("Update sent to attendees for this event."));

        findViewById(R.id.btnCloseRsvps).setOnClickListener(v ->
                show("RSVPs closed for this event."));

        loadOrganizerEvents();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadOrganizerEvents();
    }

    private void loadOrganizerEvents() {
        eventRepo.getOrganizerEvents(organizerUid, new EventRepository.EventListCallback() {
            @Override
            public void onSuccess(List<Map<String, Object>> events) {
                organizerEvents.clear();
                organizerEvents.addAll(events);

                List<String> labels = new ArrayList<>();

                if (events.isEmpty()) {
                    labels.add("No events yet");
                } else {
                    for (Map<String, Object> event : events) {
                        String title = value(event, "title");
                        String date = value(event, "date");
                        labels.add(title + " - " + date);
                    }
                }

                ArrayAdapter<String> adapter = new ArrayAdapter<>(
                        OrganizerDashboardActivity.this,
                        android.R.layout.simple_spinner_dropdown_item,
                        labels
                );

                spinnerOrganizerEvents.setAdapter(adapter);

                if (events.isEmpty()) {
                    show("Create your first event to manage attendees.");
                } else {
                    show("Select an event to manage attendees.");
                }
            }

            @Override
            public void onFailure(String error) {
                show(error);
            }
        });
    }

    private void openCreateEvent() {
        Intent intent = new Intent(this, CreateEventActivity.class);
        intent.putExtra("organizerUid", organizerUid);
        startActivity(intent);
    }

    private void viewAttendees() {
        String eventId = getSelectedEventId();

        if (eventId == null) {
            show("Select an event first.");
            return;
        }

        eventRepo.getAttendees(eventId, new EventRepository.AttendeesCallback() {
            @Override
            public void onSuccess(String attendeeList) {
                show(attendeeList);
            }

            @Override
            public void onFailure(String error) {
                show(error);
            }
        });
    }

    private String getSelectedEventId() {
        int position = spinnerOrganizerEvents.getSelectedItemPosition();

        if (position < 0 || position >= organizerEvents.size()) {
            return null;
        }

        Object eventId = organizerEvents.get(position).get("eventId");
        return eventId == null ? null : String.valueOf(eventId);
    }

    private String value(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value == null ? "-" : String.valueOf(value);
    }

    private void show(String msg) {
        tvDashboardResult.setVisibility(View.VISIBLE);
        tvDashboardResult.setText(msg);
    }
}
