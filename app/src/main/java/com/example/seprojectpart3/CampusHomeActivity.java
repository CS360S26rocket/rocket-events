package com.example.seprojectpart3;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.app.DatePickerDialog;
import java.util.Calendar;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.List;
import java.util.Map;

public class CampusHomeActivity extends AppCompatActivity {

    private EventRepository eventRepo;
    private TicketRepository ticketRepo;

    private LinearLayout sectionBrowse;
    private LinearLayout sectionMyEvents;
    private LinearLayout sectionStatus;

    private TextView tabBrowse;
    private TextView tabMyEvents;
    private TextView tabStatus;

    private TextView tvMyEvents;
    private TextView tvMessage;

    private EditText etSearch;
    private EditText etStartDate;
    private EditText etEndDate;
    private EditText etRegistrationId;
    private EditText etStatusEventId;

    private LinearLayout listEvents;
    private TextView tvSelectedEvent;
    private String selectedEventId = null;

    private String uid;
    private String email = "";
    private String name = "Campus User";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_campus_home);

        eventRepo = new EventRepository();
        ticketRepo = new TicketRepository();

        uid = getIntent().getStringExtra("uid");

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (uid == null && user != null) uid = user.getUid();

        if (user != null) {
            email = user.getEmail() == null ? "" : user.getEmail();
            if (user.getDisplayName() != null && !user.getDisplayName().isEmpty()) {
                name = user.getDisplayName();
            }
        }

        sectionBrowse = findViewById(R.id.sectionBrowse);
        sectionMyEvents = findViewById(R.id.sectionMyEvents);
        sectionStatus = findViewById(R.id.sectionStatus);

        tabBrowse = findViewById(R.id.tabBrowse);
        tabMyEvents = findViewById(R.id.tabMyEvents);
        tabStatus = findViewById(R.id.tabStatus);

        listEvents = findViewById(R.id.listEvents);
        tvMyEvents = findViewById(R.id.tvMyEvents);
        tvMessage = findViewById(R.id.tvMessage);

        etSearch = findViewById(R.id.etSearch);
        etStartDate = findViewById(R.id.etStartDate);
        etEndDate = findViewById(R.id.etEndDate);
        etStartDate.setOnClickListener(v -> showDatePicker(etStartDate));
        etEndDate.setOnClickListener(v -> showDatePicker(etEndDate));
        tvSelectedEvent = findViewById(R.id.tvSelectedEvent);
        etRegistrationId = findViewById(R.id.etRegistrationId);
        etStatusEventId = findViewById(R.id.etStatusEventId);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        tabBrowse.setOnClickListener(v -> showTab("browse"));
        tabMyEvents.setOnClickListener(v -> {
            showTab("my");
            loadMyEvents();
        });
        tabStatus.setOnClickListener(v -> showTab("status"));

        findViewById(R.id.btnSearch).setOnClickListener(v -> searchEvents());
        findViewById(R.id.btnFilterDate).setOnClickListener(v -> filterByDate());
        findViewById(R.id.btnFilterPrice).setOnClickListener(v -> filterByPrice());
        findViewById(R.id.btnRsvp).setOnClickListener(v -> rsvpToEvent());
        findViewById(R.id.btnCancelRsvp).setOnClickListener(v -> cancelRsvp());
        findViewById(R.id.btnTicketStatus).setOnClickListener(v -> viewTicketStatus());

        showTab("browse");
        loadAllEvents();
    }

    private void showTab(String tab) {
        sectionBrowse.setVisibility("browse".equals(tab) ? View.VISIBLE : View.GONE);
        sectionMyEvents.setVisibility("my".equals(tab) ? View.VISIBLE : View.GONE);
        sectionStatus.setVisibility("status".equals(tab) ? View.VISIBLE : View.GONE);

        setTabActive(tabBrowse, "browse".equals(tab));
        setTabActive(tabMyEvents, "my".equals(tab));
        setTabActive(tabStatus, "status".equals(tab));
    }

    private void setTabActive(TextView tab, boolean active) {
        tab.setBackgroundResource(active ? R.drawable.bg_button_primary : R.drawable.bg_button_secondary);
        tab.setTextColor(getColor(active ? R.color.button_text_dark : R.color.text_primary));
    }

    private void loadAllEvents() {
        eventRepo.getActiveEvents(new EventRepository.EventListCallback() {
            @Override public void onSuccess(List<Map<String, Object>> events) {
                renderEvents(events);
            }

            @Override public void onFailure(String error) {
                showMessage(error);
            }
        });
    }

    private void searchEvents() {
        String keyword = etSearch.getText().toString().trim();

        if (keyword.isEmpty()) {
            loadAllEvents();
            return;
        }

        eventRepo.searchEvents(keyword, new EventRepository.EventListCallback() {
            @Override public void onSuccess(List<Map<String, Object>> events) {
                renderEvents(events);
            }

            @Override public void onFailure(String error) {
                showMessage(error);
            }
        });
    }

    private void filterByDate() {
        String start = etStartDate.getText().toString().trim();
        String end = etEndDate.getText().toString().trim();

        eventRepo.filterByDateRange(start, end, new EventRepository.EventListCallback() {
            @Override public void onSuccess(List<Map<String, Object>> events) {
                renderEvents(events);
            }

            @Override public void onFailure(String error) {
                showMessage(error);
            }
        });
    }

    private void filterByPrice() {
        RadioGroup group = findViewById(R.id.rgPrice);
        boolean freeOnly = group.getCheckedRadioButtonId() == R.id.rbFree;

        eventRepo.filterByPrice(freeOnly, new EventRepository.EventListCallback() {
            @Override public void onSuccess(List<Map<String, Object>> events) {
                renderEvents(events);
            }

            @Override public void onFailure(String error) {
                showMessage(error);
            }
        });
    }

    private void rsvpToEvent() {
        if (!hasUser() || selectedEventId == null) {
            showMessage("Select an event first.");
            return;
        }

        ticketRepo.registerFreeRsvp(selectedEventId, uid, name, email,
                new TicketRepository.RegistrationCallback() {
                    @Override public void onConfirmed(String registrationId) {
                        showMessage("RSVP confirmed. Registration ID: " + registrationId);
                    }

                    @Override public void onWaitlisted(String waitlistId) {
                        showMessage("Event is full. Added to waitlist: " + waitlistId);
                    }

                    @Override public void onFailure(String error) {
                        showMessage(error);
                    }
                });
    }


    private void loadMyEvents() {
        if (!hasUser()) return;

        ticketRepo.getUserEvents(uid, new TicketRepository.UserEventsCallback() {
            @Override public void onSuccess(List<Map<String, Object>> events) {
                tvMyEvents.setText(formatUserEvents(events));
            }

            @Override public void onFailure(String error) {
                showMessage(error);
            }
        });
    }

    private void cancelRsvp() {
        String registrationId = etRegistrationId.getText().toString().trim();

        if (registrationId.isEmpty()) {
            showMessage("Enter a registration ID from My Events.");
            return;
        }

        ticketRepo.cancelRsvp(registrationId, new TicketRepository.CancelCallback() {
            @Override public void onSuccess(String message) {
                showMessage(message);
                loadMyEvents();
            }

            @Override public void onFailure(String error) {
                showMessage(error);
            }
        });
    }

    private void viewTicketStatus() {
        String eventId = etStatusEventId.getText().toString().trim();

        if (!hasUser() || eventId.isEmpty()) {
            showMessage("Enter an event ID.");
            return;
        }

        ticketRepo.getTicketStatus(uid, eventId, new TicketRepository.TicketStatusCallback() {
            @Override public void onSuccess(String status) {
                showMessage("Ticket status: " + status);
            }

            @Override public void onFailure(String error) {
                showMessage(error);
            }
        });
    }

    private boolean hasUser() {
        if (uid == null || uid.isEmpty()) {
            showMessage("Please log in again.");
            return false;
        }
        return true;
    }


    private String formatUserEvents(List<Map<String, Object>> events) {
        if (events == null || events.isEmpty()) {
            return "You have no RSVPs or tickets yet.";
        }

        StringBuilder out = new StringBuilder();

        for (Map<String, Object> event : events) {
            out.append(value(event, "eventTitle")).append("\n")
                    .append("Status: ").append(value(event, "status")).append("\n")
                    .append("Date: ").append(value(event, "eventDate")).append("\n")
                    .append("Event ID: ").append(value(event, "eventId")).append("\n")
                    .append("Registration ID: ").append(value(event, "registrationId"))
                    .append("\n\n");
        }

        return out.toString().trim();
    }

    private String value(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value == null ? "-" : String.valueOf(value);
    }

    private void showDatePicker(EditText target) {
        Calendar calendar = Calendar.getInstance();

        DatePickerDialog dialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    String date = String.format(
                            java.util.Locale.US,
                            "%04d-%02d-%02d",
                            year,
                            month + 1,
                            dayOfMonth
                    );
                    target.setText(date);
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );

        dialog.show();
    }
    private void renderEvents(List<Map<String, Object>> events) {
        listEvents.removeAllViews();

        if (events == null || events.isEmpty()) {
            TextView empty = makeEventCard("No matching events found.");
            listEvents.addView(empty);
            return;
        }

        for (Map<String, Object> event : events) {
            String eventId = value(event, "eventId");
            String title = value(event, "title");
            String date = value(event, "date");
            String venue = value(event, "venue");
            String price = Boolean.TRUE.equals(event.get("isFree")) ? "Free RSVP" : "Paid ticket";

            String label = title + "\n" + date + " | " + venue + "\n" + price;

            TextView card = makeEventCard(label);
            card.setOnClickListener(v -> {
                selectedEventId = eventId;
                tvSelectedEvent.setText(title + "\n" + date + " | " + venue);
                tvSelectedEvent.setTextColor(getColor(R.color.text_primary));
                showMessage("Selected: " + title);
            });

            listEvents.addView(card);
        }
    }

    private TextView makeEventCard(String text) {
        TextView card = new TextView(this);
        card.setText(text);
        card.setTextColor(getColor(R.color.text_primary));
        card.setTextSize(13);
        card.setPadding(18, 16, 18, 16);
        card.setBackgroundResource(R.drawable.bg_card_dark);
        card.setClickable(true);
        card.setFocusable(true);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, 12);
        card.setLayoutParams(params);

        return card;
    }



    private void showMessage(String message) {
        tvMessage.setVisibility(View.VISIBLE);
        tvMessage.setText(message);
    }
}
