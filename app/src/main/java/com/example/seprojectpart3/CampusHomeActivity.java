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
    private FileStorageRepository fileStorageRepo;

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
    private LinearLayout listTicketTypes;
    private LinearLayout rowQuantity;
    private TextView tvSelectedEvent;
    private TextView tvQuantity;
    private String selectedEventId = null;
    private String selectedTicketTypeId = null;
    private boolean selectedEventIsFree = true;
    private int selectedQuantity = 1;

    private String uid;
    private String email = "";
    private String name = "Campus User";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_campus_home);

        eventRepo = new EventRepository();
        ticketRepo = new TicketRepository();
        fileStorageRepo = new FileStorageRepository();

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
        listTicketTypes = findViewById(R.id.listTicketTypes);
        rowQuantity = findViewById(R.id.rowQuantity);
        tvQuantity = findViewById(R.id.tvQuantity);
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
        findViewById(R.id.btnDecreaseQty).setOnClickListener(v -> changeQuantity(-1));
        findViewById(R.id.btnIncreaseQty).setOnClickListener(v -> changeQuantity(1));

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
        eventRepo.getUpcomingEvents(new EventRepository.EventListCallback() {
            @Override public void onSuccess(List<Map<String, Object>> events) {
                renderEvents(events);
            }

            @Override public void onFailure(String error) {
                selectedEventId = null;
                selectedTicketTypeId = null;
                selectedEventIsFree = true;
                rowQuantity.setVisibility(View.GONE);
                listTicketTypes.removeAllViews();
                tvSelectedEvent.setText("Select an event above");
                tvSelectedEvent.setTextColor(getColor(R.color.text_secondary));
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

        if (!selectedEventIsFree && selectedTicketTypeId == null) {
            showMessage("Select a ticket tier first.");
            return;
        }

        String tierId = selectedTicketTypeId == null ? "free_rsvp" : selectedTicketTypeId;

        ticketRepo.registerTicketSelection(selectedEventId, uid, name, email,
                tierId, selectedQuantity, selectedEventIsFree,
                new TicketRepository.RegistrationCallback() {
                    @Override public void onConfirmed(String registrationId) {
                        if (selectedEventIsFree) {
                            showMessage("RSVP confirmed. Registration ID: " + registrationId);
                        } else {
                            showMessage("Ticket request saved. Upload payment proof when that step is available. Registration ID: " + registrationId);
                        }
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

    private String valueOrDefault(Map<String, Object> map, String key, String fallback) {
        Object value = map.get(key);
        if (value == null) return fallback;
        String asString = String.valueOf(value);
        return asString.trim().isEmpty() || "null".equals(asString) ? fallback : asString;
    }

    private String money(Object value) {
        if (value instanceof Number) {
            double amount = ((Number) value).doubleValue();
            if (amount == Math.floor(amount)) return String.valueOf((long) amount);
            return String.format(java.util.Locale.US, "%.2f", amount);
        }
        return value == null ? "0" : String.valueOf(value);
    }

    private String availableCount(Map<String, Object> tier) {
        long quantity = longValue(tier.get("quantity"));
        long sold = longValue(tier.get("sold"));
        if (quantity <= 0) return "No limit";
        return String.valueOf(Math.max(0, quantity - sold));
    }

    private long longValue(Object value) {
        if (value instanceof Number) return ((Number) value).longValue();
        try {
            return value == null ? 0 : Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void changeQuantity(int delta) {
        selectedQuantity += delta;
        if (selectedQuantity < 1) selectedQuantity = 1;
        if (selectedQuantity > 10) selectedQuantity = 10;
        tvQuantity.setText(String.valueOf(selectedQuantity));
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
            String price = valueOrDefault(event, "priceSummary",
                    Boolean.TRUE.equals(event.get("isFree")) ? "Free RSVP" : "Paid ticket");

            String label = title + "\n" + date + " | " + venue + "\n" + price;

            TextView card = makeEventCard(label);
            card.setOnClickListener(v -> {
                selectedEventId = eventId;
                selectedTicketTypeId = null;
                selectedQuantity = 1;
                tvQuantity.setText(String.valueOf(selectedQuantity));
                listTicketTypes.removeAllViews();
                tvSelectedEvent.setText("Loading event details...");
                tvSelectedEvent.setTextColor(getColor(R.color.text_primary));
                loadEventDetail(eventId);
            });

            listEvents.addView(card);
        }
    }

    private void loadEventDetail(String eventId) {
        eventRepo.getEventDetail(eventId, new EventRepository.EventDetailCallback() {
            @Override public void onSuccess(Map<String, Object> event) {
                selectedEventIsFree = Boolean.TRUE.equals(event.get("isFree"));
                renderEventDetail(event);
                renderTicketTypes(event);
            }

            @Override public void onFailure(String error) {
                showMessage(error);
            }
        });
    }

    @SuppressWarnings("unchecked")
    private void renderTicketTypes(Map<String, Object> event) {
        listTicketTypes.removeAllViews();

        if (selectedEventIsFree) {
            rowQuantity.setVisibility(View.GONE);
            selectedTicketTypeId = "free_rsvp";
            return;
        }

        rowQuantity.setVisibility(View.VISIBLE);
        Object raw = event.get("ticketTypes");
        List<Map<String, Object>> ticketTypes = raw instanceof List
                ? (List<Map<String, Object>>) raw
                : null;

        if (ticketTypes == null || ticketTypes.isEmpty()) {
            TextView empty = makeEventCard("No ticket tiers have been configured yet.");
            listTicketTypes.addView(empty);
            return;
        }

        for (Map<String, Object> tier : ticketTypes) {
            String typeId = value(tier, "typeId");
            String tierName = valueOrDefault(tier, "name", typeId);
            String label = tierName + "\nPKR " + money(tier.get("price"))
                    + " | Available: " + availableCount(tier);

            TextView card = makeEventCard(label);
            card.setOnClickListener(v -> {
                selectedTicketTypeId = typeId;
                highlightSelectedTier();
                card.setBackgroundResource(R.drawable.bg_button_secondary);
                showMessage("Selected tier: " + tierName);
            });

            listTicketTypes.addView(card);

            if (selectedTicketTypeId == null) {
                selectedTicketTypeId = typeId;
                card.setBackgroundResource(R.drawable.bg_button_secondary);
            }
        }
    }

    private void renderEventDetail(Map<String, Object> event) {
        StringBuilder detail = new StringBuilder();
        detail.append(value(event, "title")).append("\n")
                .append("Date: ").append(value(event, "date")).append("\n")
                .append("Time: ").append(valueOrDefault(event, "startTime", "-")).append("\n")
                .append("Venue: ").append(value(event, "venue")).append("\n")
                .append("Price: ").append(valueOrDefault(event, "priceSummary", "-")).append("\n")
                .append("Capacity: ").append(valueOrDefault(event, "capacity", "No limit"));

        String paymentQrFileId = value(event, "paymentQrFileId");
        String paymentQrUrl = value(event, "paymentQrUrl");
        if (!selectedEventIsFree) {
            detail.append("\nPayment QR: ");
            if (!paymentQrFileId.equals("-") && !paymentQrFileId.isEmpty()) {
                String loadingDetail = detail + "Loading...";
                tvSelectedEvent.setText(loadingDetail);
                fileStorageRepo.getFileUrl(paymentQrFileId,
                        url -> tvSelectedEvent.setText(detail + url),
                        error -> tvSelectedEvent.setText(detail
                                + "Organizer has not uploaded it yet"));
                return;
            } else {
                detail.append(paymentQrUrl.equals("-") || paymentQrUrl.isEmpty()
                        ? "Organizer has not uploaded it yet"
                        : paymentQrUrl);
            }
        }

        tvSelectedEvent.setText(detail.toString());
    }

    private void highlightSelectedTier() {
        for (int i = 0; i < listTicketTypes.getChildCount(); i++) {
            View child = listTicketTypes.getChildAt(i);
            child.setBackgroundResource(R.drawable.bg_card_dark);
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
