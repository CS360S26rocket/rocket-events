package com.example.seprojectpart3;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import java.util.Calendar;
import java.util.Locale;


public class CreateEventActivity extends AppCompatActivity {

    EditText etEventTitle, etDescription, etInstitutionName;
    EditText etStartDate, etStartTime, etEndDate, etEndTime;
    EditText etVenue, etLocation, etCapacity;

    // Radio dot views
    View dotOpenForAll, dotInstitutionOnly, dotInviteOnly, dotHidden;

    // Category chip TextViews
    TextView chipCampusEvents, chipTalks, chipSports, chipWorkshops;

    String selectedEventType = "institution_only"; // default matches mockup
    String selectedCategory  = "campus_events";    // default matches mockup
    String organizerUid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_event);

        organizerUid = getIntent().getStringExtra("organizerUid");

        // Bind fields
        etEventTitle       = findViewById(R.id.etEventTitle);
        etDescription      = findViewById(R.id.etDescription);
        etInstitutionName  = findViewById(R.id.etInstitutionName);
        etStartDate        = findViewById(R.id.etStartDate);
        etStartTime        = findViewById(R.id.etStartTime);
        etEndDate          = findViewById(R.id.etEndDate);
        etEndTime          = findViewById(R.id.etEndTime);
        etVenue            = findViewById(R.id.etVenue);
        etLocation         = findViewById(R.id.etLocation);
        etCapacity         = findViewById(R.id.etCapacity);

        dotOpenForAll      = findViewById(R.id.dotOpenForAll);
        dotInstitutionOnly = findViewById(R.id.dotInstitutionOnly);
        dotInviteOnly      = findViewById(R.id.dotInviteOnly);
        dotHidden          = findViewById(R.id.dotHidden);

        chipCampusEvents   = findViewById(R.id.chipCampusEvents);
        chipTalks          = findViewById(R.id.chipTalks);
        chipSports         = findViewById(R.id.chipSports);
        chipWorkshops      = findViewById(R.id.chipWorkshops);

        setupDateTimePickers();


        // Back
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // Radio button listeners
        findViewById(R.id.radioOpenForAll).setOnClickListener(v ->
                selectEventType("open_for_all"));
        findViewById(R.id.radioInstitutionOnly).setOnClickListener(v ->
                selectEventType("institution_only"));
        findViewById(R.id.radioInviteOnly).setOnClickListener(v ->
                selectEventType("invite_only"));
        findViewById(R.id.radioHidden).setOnClickListener(v ->
                selectEventType("hidden"));

        // Category chip listeners
        chipCampusEvents.setOnClickListener(v -> selectCategory("campus_events"));
        chipTalks.setOnClickListener(v -> selectCategory("talks"));
        chipSports.setOnClickListener(v -> selectCategory("sports"));
        chipWorkshops.setOnClickListener(v -> selectCategory("workshops"));

        // Next: pass all data to TicketSetupActivity
        findViewById(R.id.btnNext).setOnClickListener(v -> {
            String title    = etEventTitle.getText().toString().trim();
            String desc     = etDescription.getText().toString().trim();
            String venue    = etVenue.getText().toString().trim();
            String startDate = etStartDate.getText().toString().trim();
            String capacity = etCapacity.getText().toString().trim();

            if (title.isEmpty() || venue.isEmpty()) {
                // Show inline error (could add a tvError view if needed)
                etEventTitle.setError("Required");
                return;
            }

            Intent intent = new Intent(this, TicketSetupActivity.class);
            intent.putExtra("organizerUid", organizerUid);
            intent.putExtra("title", title);
            intent.putExtra("description", desc);
            intent.putExtra("venue", venue);
            intent.putExtra("startDate", startDate);
            intent.putExtra("startTime", etStartTime.getText().toString().trim());
            intent.putExtra("endDate", etEndDate.getText().toString().trim());
            intent.putExtra("endTime", etEndTime.getText().toString().trim());
            intent.putExtra("location", etLocation.getText().toString().trim());
            intent.putExtra("institutionName", etInstitutionName.getText().toString().trim());
            intent.putExtra("eventType", selectedEventType);
            intent.putExtra("category", selectedCategory);
            intent.putExtra("capacity", capacity);
            startActivity(intent);
        });


    }

    private void selectEventType(String type) {
        selectedEventType = type;
        // Reset all dots to unselected
        dotOpenForAll.setBackgroundResource(R.drawable.bg_radio_unselected);
        dotInstitutionOnly.setBackgroundResource(R.drawable.bg_radio_unselected);
        dotInviteOnly.setBackgroundResource(R.drawable.bg_radio_unselected);
        dotHidden.setBackgroundResource(R.drawable.bg_radio_unselected);
        // Set selected
        switch (type) {
            case "open_for_all":       dotOpenForAll.setBackgroundResource(R.drawable.bg_radio_selected); break;
            case "institution_only":   dotInstitutionOnly.setBackgroundResource(R.drawable.bg_radio_selected); break;
            case "invite_only":        dotInviteOnly.setBackgroundResource(R.drawable.bg_radio_selected); break;
            case "hidden":             dotHidden.setBackgroundResource(R.drawable.bg_radio_selected); break;
        }
    }

    private void selectCategory(String category) {
        selectedCategory = category;
        // Reset all chips
        chipCampusEvents.setBackgroundResource(R.drawable.bg_chip_inactive);
        chipCampusEvents.setTextColor(getColor(R.color.text_primary));
        chipTalks.setBackgroundResource(R.drawable.bg_chip_inactive);
        chipTalks.setTextColor(getColor(R.color.text_primary));
        chipSports.setBackgroundResource(R.drawable.bg_chip_inactive);
        chipSports.setTextColor(getColor(R.color.text_primary));
        chipWorkshops.setBackgroundResource(R.drawable.bg_chip_inactive);
        chipWorkshops.setTextColor(getColor(R.color.text_primary));

        // Highlight selected
        TextView selected = null;
        switch (category) {
            case "campus_events": selected = chipCampusEvents; break;
            case "talks":         selected = chipTalks;        break;
            case "sports":        selected = chipSports;       break;
            case "workshops":     selected = chipWorkshops;    break;
        }
        if (selected != null) {
            selected.setBackgroundResource(R.drawable.bg_chip_active);
            selected.setTextColor(getColor(R.color.button_text_dark));
        }
    }
    private void setupDateTimePickers() {
        etStartDate.setFocusable(false);
        etStartDate.setClickable(true);
        etStartDate.setOnClickListener(v -> showDatePicker(etStartDate));

        etEndDate.setFocusable(false);
        etEndDate.setClickable(true);
        etEndDate.setOnClickListener(v -> showDatePicker(etEndDate));

        etStartTime.setFocusable(false);
        etStartTime.setClickable(true);
        etStartTime.setOnClickListener(v -> showTimePicker(etStartTime));

        etEndTime.setFocusable(false);
        etEndTime.setClickable(true);
        etEndTime.setOnClickListener(v -> showTimePicker(etEndTime));
    }

    private void showDatePicker(EditText target) {
        Calendar calendar = Calendar.getInstance();

        new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    String date = String.format(
                            Locale.US,
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
        ).show();
    }

    private void showTimePicker(EditText target) {
        Calendar calendar = Calendar.getInstance();

        new TimePickerDialog(
                this,
                (view, hourOfDay, minute) -> {
                    String time = String.format(Locale.US, "%02d:%02d", hourOfDay, minute);
                    target.setText(time);
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                true
        ).show();
    }

}
