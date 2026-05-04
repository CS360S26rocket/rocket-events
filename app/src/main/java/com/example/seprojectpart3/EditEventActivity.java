/*
 * This file defines EditEventActivity, an Android activity used by the Scene app.
 * It contains the organizer edit-event screen, event status controls, sales timing updates, and attendee update actions.
 * Its functions include onCreate, loadEvent, onSuccess, onFailure to load data, handle user actions, validate input, and save results.
 * It connects this feature to the Scene app's UI, data, navigation, and verification flow.
 */

package com.example.seprojectpart3;

import android.content.Intent;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.HashMap;
import java.util.Calendar;
import java.util.Locale;
import java.util.Map;

public class EditEventActivity extends AppCompatActivity {

    private EditText etTitle, etDescription, etStartDate, etStartTime, etEndDate, etEndTime,
            etVenue, etCapacity, etSalesStart, etSalesStartTime, etSalesEnd, etSalesEndTime;
    private TextView tvStatus, chipDraft, chipPublished, chipSoldOut, chipCancelled;
    private String currentStatus = "draft";
    private String eventId, organizerUid;
    private final EventRepository eventRepo = new EventRepository();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_event);

        eventId = getIntent().getStringExtra("eventId");
        organizerUid = getIntent().getStringExtra("organizerUid");
        etTitle = findViewById(R.id.etEditTitle);
        etDescription = findViewById(R.id.etEditDescription);
        etStartDate = findViewById(R.id.etEditStartDate);
        etStartTime = findViewById(R.id.etEditStartTime);
        etEndDate = findViewById(R.id.etEditEndDate);
        etEndTime = findViewById(R.id.etEditEndTime);
        etVenue = findViewById(R.id.etEditVenue);
        etCapacity = findViewById(R.id.etEditCapacity);
        etSalesStart = findViewById(R.id.etEditSalesStart);
        etSalesStartTime = findViewById(R.id.etEditSalesStartTime);
        etSalesEnd = findViewById(R.id.etEditSalesEnd);
        etSalesEndTime = findViewById(R.id.etEditSalesEndTime);
        tvStatus = findViewById(R.id.tvEditStatus);
        chipDraft = findViewById(R.id.chipStatusDraft);
        chipPublished = findViewById(R.id.chipStatusPublished);
        chipSoldOut = findViewById(R.id.chipStatusSoldOut);
        chipCancelled = findViewById(R.id.chipStatusCancelled);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnSaveEvent).setOnClickListener(v -> saveEvent());
        findViewById(R.id.btnSendEventUpdate).setOnClickListener(v -> openUpdateScreen());
        findViewById(R.id.btnCancelEvent).setOnClickListener(v -> cancelEvent());
        chipDraft.setOnClickListener(v -> updateStatus("draft"));
        chipPublished.setOnClickListener(v -> updateStatus("published"));
        chipSoldOut.setOnClickListener(v -> updateStatus("sold_out"));
        chipCancelled.setOnClickListener(v -> updateStatus("cancelled"));
        setupDateTimePickers();

        loadEvent();
    }

    private void loadEvent() {
        if (eventId == null || eventId.isEmpty()) {
            show("Event not found.", true);
            return;
        }

        show("Loading event...", false);
        eventRepo.getEventDetail(eventId, new EventRepository.EventDetailCallback() {
            @Override public void onSuccess(Map<String, Object> event) {
                etTitle.setText(value(event, "title"));
                etDescription.setText(value(event, "description"));
                etStartDate.setText(firstNonEmpty(value(event, "dateOnly"), datePart(value(event, "date"))));
                etStartTime.setText(firstNonEmpty(value(event, "startTime"), timePart(value(event, "date"))));
                etEndDate.setText(blankDash(value(event, "endDate")));
                etEndTime.setText(blankDash(value(event, "endTime")));
                etVenue.setText(value(event, "venue"));
                etCapacity.setText(value(event, "capacity").equals("-") ? "" : value(event, "capacity"));
                etSalesStart.setText(blankDash(value(event, "ticketSalesStart")));
                etSalesEnd.setText(blankDash(value(event, "ticketSalesEnd")));
                etSalesStartTime.setText(blankDash(value(event, "ticketSalesStartTime")));
                etSalesEndTime.setText(blankDash(value(event, "ticketSalesEndTime")));
                currentStatus = normalizeStatus(value(event, "status"));
                renderStatusChips();
                tvStatus.setVisibility(View.GONE);
            }

            @Override public void onFailure(String error) {
                show(error, true);
            }
        });
    }

    private void saveEvent() {
        String title = etTitle.getText().toString().trim();
        String venue = etVenue.getText().toString().trim();
        if (title.isEmpty() || venue.isEmpty()) {
            show("Title and venue are required.", true);
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("title", title);
        updates.put("description", etDescription.getText().toString().trim());
        String startDate = etStartDate.getText().toString().trim();
        String startTime = etStartTime.getText().toString().trim();
        updates.put("date", startDate + (startTime.isEmpty() ? "" : " " + startTime));
        updates.put("dateOnly", startDate);
        updates.put("startTime", startTime);
        updates.put("endDate", etEndDate.getText().toString().trim());
        updates.put("endTime", etEndTime.getText().toString().trim());
        updates.put("venue", venue);
        updates.put("ticketSalesStart", etSalesStart.getText().toString().trim());
        updates.put("ticketSalesStartTime", etSalesStartTime.getText().toString().trim());
        updates.put("ticketSalesEnd", etSalesEnd.getText().toString().trim());
        updates.put("ticketSalesEndTime", etSalesEndTime.getText().toString().trim());
        updates.put("ticketSalesOpen", !etSalesEnd.getText().toString().trim().isEmpty());

        String capacity = etCapacity.getText().toString().trim();
        if (!capacity.isEmpty()) {
            try {
                updates.put("capacity", Integer.parseInt(capacity));
            } catch (NumberFormatException e) {
                show("Capacity must be a number.", true);
                return;
            }
        }

        show("Saving changes...", false);
        eventRepo.updateEventMetadata(eventId, updates, new EventRepository.EventCallback() {
            @Override public void onSuccess(String id) {
                openOrganizerSuccess("Event updated", "Your event changes were saved successfully.");
            }

            @Override public void onFailure(String error) {
                show(error, true);
            }
        });
    }

    private void cancelEvent() {
        updateStatus("cancelled");
    }

    private void updateStatus(String status) {
        show("Updating status...", false);
        eventRepo.updateEventStatus(eventId, status, new EventRepository.EventCallback() {
            @Override public void onSuccess(String id) {
                currentStatus = normalizeStatus(status);
                renderStatusChips();
                openOrganizerSuccess("Status updated", statusMessage(currentStatus));
            }

            @Override public void onFailure(String error) {
                show(error, true);
            }
        });
    }

    private void renderStatusChips() {
        setChip(chipDraft, "draft".equals(currentStatus));
        setChip(chipPublished, "published".equals(currentStatus) || "active".equals(currentStatus));
        setChip(chipSoldOut, "sold_out".equals(currentStatus));
        setChip(chipCancelled, "cancelled".equals(currentStatus));
    }

    private void setChip(TextView chip, boolean active) {
        chip.setBackgroundResource(active ? R.drawable.bg_button_primary : R.drawable.bg_button_ghost);
        chip.setTextColor(getColor(active ? R.color.button_text_dark : R.color.text_primary));
    }

    private String normalizeStatus(String status) {
        if (status == null || status.equals("-") || status.trim().isEmpty()) return "draft";
        if ("active".equals(status)) return "published";
        return status;
    }

    private String statusMessage(String status) {
        if ("cancelled".equals(status)) return "Event cancelled and attendees notified.";
        if ("sold_out".equals(status)) return "Event marked sold out. It will not appear in campus discovery.";
        if ("draft".equals(status)) return "Event saved as draft. Campus users cannot see it yet.";
        return "Event published. Campus users can now see it.";
    }

    private void openUpdateScreen() {
        if (eventId == null || eventId.isEmpty()) {
            show("Event not found.", true);
            return;
        }

        Intent intent = new Intent(this, OrganizerEventUpdateActivity.class);
        intent.putExtra("eventId", eventId);
        intent.putExtra("organizerUid", organizerUid);
        intent.putExtra("eventTitle", etTitle.getText().toString().trim());
        intent.putExtra("eventDate", etStartDate.getText().toString().trim());
        intent.putExtra("eventVenue", etVenue.getText().toString().trim());
        startActivity(intent);
    }

    private void setupDateTimePickers() {
        setupDate(etStartDate);
        setupDate(etEndDate);
        setupDate(etSalesStart);
        setupDate(etSalesEnd);
        setupTime(etStartTime);
        setupTime(etEndTime);
        setupTime(etSalesStartTime);
        setupTime(etSalesEndTime);
    }

    private void setupDate(EditText target) {
        target.setFocusable(false);
        target.setClickable(true);
        target.setOnClickListener(v -> showDatePicker(target));
    }

    private void setupTime(EditText target) {
        target.setFocusable(false);
        target.setClickable(true);
        target.setOnClickListener(v -> showTimePicker(target));
    }

    private void showDatePicker(EditText target) {
        Calendar calendar = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, dayOfMonth) ->
                target.setText(String.format(Locale.US, "%04d-%02d-%02d",
                        year, month + 1, dayOfMonth)),
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void showTimePicker(EditText target) {
        Calendar calendar = Calendar.getInstance();
        new TimePickerDialog(this, (view, hourOfDay, minute) ->
                target.setText(String.format(Locale.US, "%02d:%02d", hourOfDay, minute)),
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                true).show();
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

    private String value(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value == null ? "-" : String.valueOf(value);
    }

    private String blankDash(String value) {
        return value == null || "-".equals(value) || "null".equals(value) ? "" : value;
    }

    private String firstNonEmpty(String first, String second) {
        String cleanFirst = blankDash(first);
        return cleanFirst.isEmpty() ? blankDash(second) : cleanFirst;
    }

    private String datePart(String raw) {
        if (raw == null || raw.length() < 10 || "-".equals(raw)) return "";
        return raw.substring(0, 10);
    }

    private String timePart(String raw) {
        if (raw == null || raw.length() < 16 || "-".equals(raw)) return "";
        return raw.substring(11, 16);
    }

    private void show(String message, boolean error) {
        tvStatus.setVisibility(View.VISIBLE);
        tvStatus.setText(message);
        tvStatus.setTextColor(getColor(error ? R.color.error_red : R.color.accent_lime));
    }
}
