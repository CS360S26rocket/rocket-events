package com.example.seprojectpart3;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import java.util.Calendar;
import java.util.Locale;


public class CreateEventActivity extends AppCompatActivity {

    private static final int PICK_BANNER_REQUEST = 4101;

    EditText etEventTitle, etDescription;
    EditText etStartDate, etStartTime, etEndDate, etEndTime;
    EditText etVenue, etCapacity;
    TextView tvInstitutionName, tvBannerHint;
    ImageView ivBannerPreview;

    // Radio dot views
    View dotOpenForAll, dotInstitutionOnly, dotInviteOnly, dotHidden;

    // Category chip TextViews
    TextView chipCampusEvents, chipTalks, chipSports, chipWorkshops;
    TextView chipLocationInPerson, chipLocationOnline, chipLocationHybrid;

    String selectedEventType = "institution_only"; // default matches mockup
    String selectedCategory  = "entertainment";
    String selectedInstitution = "LUMS";
    String selectedLocationType = "In-person";
    String organizerUid;
    Uri selectedBannerUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_event);

        organizerUid = getIntent().getStringExtra("organizerUid");

        // Bind fields
        etEventTitle       = findViewById(R.id.etEventTitle);
        etDescription      = findViewById(R.id.etDescription);
        tvInstitutionName = findViewById(R.id.tvInstitutionName);
        etStartDate        = findViewById(R.id.etStartDate);
        etStartTime        = findViewById(R.id.etStartTime);
        etEndDate          = findViewById(R.id.etEndDate);
        etEndTime          = findViewById(R.id.etEndTime);
        etVenue            = findViewById(R.id.etVenue);
        etCapacity         = findViewById(R.id.etCapacity);
        tvBannerHint       = findViewById(R.id.tvBannerHint);
        ivBannerPreview    = findViewById(R.id.ivBannerPreview);

        dotOpenForAll      = findViewById(R.id.dotOpenForAll);
        dotInstitutionOnly = findViewById(R.id.dotInstitutionOnly);
        dotInviteOnly      = findViewById(R.id.dotInviteOnly);
        dotHidden          = findViewById(R.id.dotHidden);

        chipCampusEvents   = findViewById(R.id.chipCampusEvents);
        chipTalks          = findViewById(R.id.chipTalks);
        chipSports         = findViewById(R.id.chipSports);
        chipWorkshops      = findViewById(R.id.chipWorkshops);
        chipLocationInPerson = findViewById(R.id.chipLocationInPerson);
        chipLocationOnline   = findViewById(R.id.chipLocationOnline);
        chipLocationHybrid   = findViewById(R.id.chipLocationHybrid);

        setupDateTimePickers();
        tvInstitutionName.setOnClickListener(v -> showUniversityPicker());
        findViewById(R.id.cardBannerPicker).setOnClickListener(v -> pickBannerImage());


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
        chipCampusEvents.setOnClickListener(v -> selectCategory("entertainment"));
        chipTalks.setOnClickListener(v -> selectCategory("seminars"));
        chipSports.setOnClickListener(v -> selectCategory("sports"));
        chipWorkshops.setOnClickListener(v -> selectCategory("workshops"));
        chipLocationInPerson.setOnClickListener(v -> selectLocationType("In-person"));
        chipLocationOnline.setOnClickListener(v -> selectLocationType("Online"));
        chipLocationHybrid.setOnClickListener(v -> selectLocationType("Hybrid"));

        // Next: pass all data to TicketSetupActivity
        findViewById(R.id.btnNext).setOnClickListener(v -> {
            String title    = etEventTitle.getText().toString().trim();
            String desc     = etDescription.getText().toString().trim();
            String venue    = etVenue.getText().toString().trim();
            String startDate = etStartDate.getText().toString().trim();
            String capacity = etCapacity.getText().toString().trim();

            if (title.isEmpty() || venue.isEmpty()) {
                // Show inline error (could add a tvError view if needed)
                if (title.isEmpty()) etEventTitle.setError("Required");
                if (venue.isEmpty()) etVenue.setError("Required");
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
            intent.putExtra("location", selectedLocationType);
            intent.putExtra("institutionName", selectedInstitution);
            intent.putExtra("eventType", selectedEventType);
            intent.putExtra("category", selectedCategory);
            intent.putExtra("capacity", capacity);
            intent.putExtra("bannerImageUri", selectedBannerUri == null ? "" : selectedBannerUri.toString());
            startActivity(intent);
        });


    }

    private void pickBannerImage() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent, "Select event banner"), PICK_BANNER_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_BANNER_REQUEST && resultCode == RESULT_OK && data != null) {
            selectedBannerUri = data.getData();
            ivBannerPreview.setImageURI(selectedBannerUri);
            ivBannerPreview.setVisibility(View.VISIBLE);
            tvBannerHint.setText("Banner selected. Tap to change.");
        }
    }

    private void showUniversityPicker() {
        String[] universities = getResources().getStringArray(R.array.pakistan_universities);
        int checked = 0;
        for (int i = 0; i < universities.length; i++) {
            if (universities[i].equals(selectedInstitution)) {
                checked = i;
                break;
            }
        }

        new AlertDialog.Builder(this)
                .setTitle("Select institution")
                .setSingleChoiceItems(universities, checked, (dialog, which) -> {
                    selectedInstitution = universities[which];
                    tvInstitutionName.setText(selectedInstitution);
                    dialog.dismiss();
                })
                .setNegativeButton("Cancel", null)
                .show();
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
            case "entertainment": selected = chipCampusEvents; break;
            case "seminars":      selected = chipTalks;        break;
            case "sports":        selected = chipSports;       break;
            case "workshops":     selected = chipWorkshops;    break;
        }
        if (selected != null) {
            selected.setBackgroundResource(R.drawable.bg_chip_active);
            selected.setTextColor(getColor(R.color.button_text_dark));
        }
    }

    private void selectLocationType(String type) {
        selectedLocationType = type;
        setLocationChip(chipLocationInPerson, "In-person".equals(type));
        setLocationChip(chipLocationOnline, "Online".equals(type));
        setLocationChip(chipLocationHybrid, "Hybrid".equals(type));
    }

    private void setLocationChip(TextView chip, boolean active) {
        chip.setBackgroundResource(active ? R.drawable.bg_button_primary : R.drawable.bg_button_ghost);
        chip.setTextColor(getColor(active ? R.color.button_text_dark : R.color.text_primary));
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
