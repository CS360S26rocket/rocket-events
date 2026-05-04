/*
 * This file defines TicketSetupActivity, an Android activity used by the Scene app.
 * It contains the organizer ticket setup step for ticket modes, tiers, prices, and sales timing.
 * Its functions include onCreate, selectMode to load data, handle user actions, validate input, and save results.
 * It connects this feature to the Scene app's UI, data, navigation, and verification flow.
 */

package com.example.seprojectpart3;

import java.util.Locale;
import java.util.Calendar;
import android.app.TimePickerDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class TicketSetupActivity extends AppCompatActivity {

    EditText etPayment, etSalesStart, etSalesStartTime, etSalesEnd, etSalesEndTime;
    TextView modeFreeRsvp, modePaidTickets, modeExternalLink, tvPaymentLabel;
    TextView tvStudentPassMeta, tvGeneralPassMeta;
    String selectedMode = "paid_tickets"; 

    
    String organizerUid, title, description, venue, startDate, startTime,
           endDate, endTime, location, institutionName, eventType, category, capacity,
           bannerImageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ticket_setup);

        
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
        bannerImageUri  = i.getStringExtra("bannerImageUri");

        etPayment       = findViewById(R.id.etPayment);
        etSalesStart    = findViewById(R.id.etSalesStart);
        etSalesStartTime = findViewById(R.id.etSalesStartTime);
        etSalesEnd      = findViewById(R.id.etSalesEnd);
        etSalesEndTime  = findViewById(R.id.etSalesEndTime);
        tvPaymentLabel  = findViewById(R.id.tvPaymentLabel);
        tvStudentPassMeta = findViewById(R.id.tvStudentPassMeta);
        tvGeneralPassMeta = findViewById(R.id.tvGeneralPassMeta);

        modeFreeRsvp    = findViewById(R.id.modeFreeRsvp);
        modePaidTickets = findViewById(R.id.modePaidTickets);
        modeExternalLink = findViewById(R.id.modeExternalLink);

        
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        
        modeFreeRsvp.setOnClickListener(v -> selectMode("free_rsvp"));
        modePaidTickets.setOnClickListener(v -> selectMode("paid_tickets"));
        modeExternalLink.setOnClickListener(v -> selectMode("external_link"));

        setupSalesDateTimePickers();
        setupSalesFieldIcons();
        updatePaymentFieldState();
        updateTicketTypeLabels();

        
        findViewById(R.id.btnEditStudentPass).setOnClickListener(v -> {
            
        });
        findViewById(R.id.btnRemoveStudentPass).setOnClickListener(v -> {
            
        });
        findViewById(R.id.btnEditGeneralPass).setOnClickListener(v -> {
            
        });
        findViewById(R.id.btnRemoveGeneralPass).setOnClickListener(v -> {
            
        });
        findViewById(R.id.btnAddTicketType).setOnClickListener(v -> {
            
        });

        
        findViewById(R.id.btnNext).setOnClickListener(v -> {
            if (!validateTicketSetup()) return;
            Intent intent = new Intent(this, InfoSetupActivity.class);
            
            intent.putExtra("organizerUid",    organizerUid);
            intent.putExtra("title",           title);
            intent.putExtra("description",     description);
            intent.putExtra("venue",           venue);
            intent.putExtra("startDate",       startDate);
            intent.putExtra("startTime",       startTime);
            intent.putExtra("endDate",         endDate);
            intent.putExtra("endTime",         endTime);
            intent.putExtra("location",        location);
            intent.putExtra("institutionName", institutionName);
            intent.putExtra("eventType",       eventType);
            intent.putExtra("category",        category);
            intent.putExtra("capacity",        capacity);
            intent.putExtra("bannerImageUri",  bannerImageUri);
            
            intent.putExtra("ticketMode",      selectedMode);
            intent.putExtra("paymentMethod",   etPayment.getText().toString().trim());
            intent.putExtra("externalTicketLink", "external_link".equals(selectedMode)
                    ? etPayment.getText().toString().trim() : "");
            intent.putExtra("salesStart",      etSalesStart.getText().toString().trim());
            intent.putExtra("salesStartTime",  etSalesStartTime.getText().toString().trim());
            intent.putExtra("salesEnd",        etSalesEnd.getText().toString().trim());
            intent.putExtra("salesEndTime",    etSalesEndTime.getText().toString().trim());
            startActivity(intent);
        });
    }


    private void setupSalesDateTimePickers() {
        setupDate(etSalesStart);
        setupDate(etSalesEnd);
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

    private void setupSalesFieldIcons() {
        setEndIcon(etSalesStart, R.drawable.ic_nav_calendar);
        setEndIcon(etSalesEnd, R.drawable.ic_nav_calendar);
        setEndIcon(etSalesStartTime, R.drawable.ic_time_clock);
        setEndIcon(etSalesEndTime, R.drawable.ic_time_clock);
    }

    private void setEndIcon(EditText field, int drawableRes) {
        field.setCompoundDrawablesWithIntrinsicBounds(0, 0, drawableRes, 0);
        field.setCompoundDrawablePadding(10);
    }

    private void updateTicketTypeLabels() {
        if ("free_rsvp".equals(selectedMode)) {
            tvStudentPassMeta.setText("Free RSVP • Qty 100");
            tvGeneralPassMeta.setText("Free RSVP • Qty 200");
        } else {
            tvStudentPassMeta.setText("PKR 300 • Qty 100");
            tvGeneralPassMeta.setText("PKR 500 • Qty 200");
        }
    }

    private void updatePaymentFieldState() {
        boolean freeRsvp = "free_rsvp".equals(selectedMode);
        boolean external = "external_link".equals(selectedMode);
        int visibility = freeRsvp ? View.GONE : View.VISIBLE;
        tvPaymentLabel.setVisibility(visibility);
        etPayment.setVisibility(visibility);
        if (freeRsvp) {
            etPayment.setText("");
            etPayment.setError(null);
            return;
        }
        tvPaymentLabel.setText(external ? "External registration" : "Payment");
        etPayment.setHint(external ? "External ticket link (optional)" : "Paymo (PKR) *");
        etPayment.setInputType(external ? android.text.InputType.TYPE_TEXT_VARIATION_URI
                : android.text.InputType.TYPE_CLASS_TEXT);
    }

    private boolean validateTicketSetup() {
        clearFieldErrors();
        String paymentValue = etPayment.getText().toString().trim();
        if ("paid_tickets".equals(selectedMode) && paymentValue.isEmpty()) {
            etPayment.setError("Payment method is required for paid tickets.");
            etPayment.requestFocus();
            return false;
        }
        if ("external_link".equals(selectedMode) && !paymentValue.isEmpty()
                && !paymentValue.startsWith("https://")) {
            etPayment.setError("External links must start with https://");
            etPayment.requestFocus();
            return false;
        }
        String salesStart = etSalesStart.getText().toString().trim();
        String salesStartTime = etSalesStartTime.getText().toString().trim();
        String salesEnd = etSalesEnd.getText().toString().trim();
        String salesEndTime = etSalesEndTime.getText().toString().trim();
        boolean hasAnySalesTiming = !salesStart.isEmpty() || !salesStartTime.isEmpty()
                || !salesEnd.isEmpty() || !salesEndTime.isEmpty();
        if (!hasAnySalesTiming) return true;
        if (salesStart.isEmpty()) {
            etSalesStart.setError("Choose a sales start date.");
            etSalesStart.requestFocus();
            return false;
        }
        if (salesStartTime.isEmpty()) {
            etSalesStartTime.setError("Choose a sales start time.");
            etSalesStartTime.requestFocus();
            return false;
        }
        if (salesEnd.isEmpty()) {
            etSalesEnd.setError("Choose a sales end date.");
            etSalesEnd.requestFocus();
            return false;
        }
        if (salesEndTime.isEmpty()) {
            etSalesEndTime.setError("Choose a sales end time.");
            etSalesEndTime.requestFocus();
            return false;
        }
        String startValue = salesStart + " " + salesStartTime;
        String endValue = salesEnd + " " + salesEndTime;
        if (endValue.compareTo(startValue) <= 0) {
            etSalesEnd.setError("Sales end must be after sales start.");
            etSalesEndTime.setError("Choose a later end time.");
            etSalesEnd.requestFocus();
            return false;
        }
        return true;
    }

    private void clearFieldErrors() {
        etPayment.setError(null);
        etSalesStart.setError(null);
        etSalesStartTime.setError(null);
        etSalesEnd.setError(null);
        etSalesEndTime.setError(null);
    }

    private void selectMode(String mode) {
        selectedMode = mode;
        
        modeFreeRsvp.setBackgroundResource(R.drawable.bg_ticket_mode_inactive);
        modeFreeRsvp.setTextColor(getColor(R.color.text_secondary));
        modePaidTickets.setBackgroundResource(R.drawable.bg_ticket_mode_inactive);
        modePaidTickets.setTextColor(getColor(R.color.text_secondary));
        modeExternalLink.setBackgroundResource(R.drawable.bg_ticket_mode_inactive);
        modeExternalLink.setTextColor(getColor(R.color.text_secondary));

        
        TextView active = "free_rsvp".equals(mode) ? modeFreeRsvp
                : "paid_tickets".equals(mode) ? modePaidTickets
                : modeExternalLink;
        active.setBackgroundResource(R.drawable.bg_ticket_mode_active);
        active.setTextColor(getColor(R.color.white));
        updatePaymentFieldState();
        updateTicketTypeLabels();
    }
}
