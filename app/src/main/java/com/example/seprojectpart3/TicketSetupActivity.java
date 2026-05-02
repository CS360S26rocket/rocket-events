package com.example.seprojectpart3;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class TicketSetupActivity extends AppCompatActivity {

    EditText etPayment, etSalesStart, etSalesStartTime, etSalesEnd, etSalesEndTime;
    TextView modeFreeRsvp, modePaidTickets, modeExternalLink;
    String selectedMode = "paid_tickets"; // default matches mockup

    // Data passed from step 1
    String organizerUid, title, description, venue, startDate, startTime,
           endDate, endTime, location, institutionName, eventType, category, capacity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ticket_setup);

        // Receive all data from CreateEventActivity
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

        etPayment       = findViewById(R.id.etPayment);
        etSalesStart    = findViewById(R.id.etSalesStart);
        etSalesStartTime = findViewById(R.id.etSalesStartTime);
        etSalesEnd      = findViewById(R.id.etSalesEnd);
        etSalesEndTime  = findViewById(R.id.etSalesEndTime);

        modeFreeRsvp    = findViewById(R.id.modeFreeRsvp);
        modePaidTickets = findViewById(R.id.modePaidTickets);
        modeExternalLink = findViewById(R.id.modeExternalLink);

        // Back
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // Ticketing mode tabs
        modeFreeRsvp.setOnClickListener(v -> selectMode("free_rsvp"));
        modePaidTickets.setOnClickListener(v -> selectMode("paid_tickets"));
        modeExternalLink.setOnClickListener(v -> selectMode("external_link"));

        // Placeholder: Edit/Remove tickets (these would open dialogs in full impl)
        findViewById(R.id.btnEditStudentPass).setOnClickListener(v -> {
            // TODO: open ticket edit dialog
        });
        findViewById(R.id.btnRemoveStudentPass).setOnClickListener(v -> {
            // TODO: remove ticket type
        });
        findViewById(R.id.btnEditGeneralPass).setOnClickListener(v -> {
            // TODO: open ticket edit dialog
        });
        findViewById(R.id.btnRemoveGeneralPass).setOnClickListener(v -> {
            // TODO: remove ticket type
        });
        findViewById(R.id.btnAddTicketType).setOnClickListener(v -> {
            // TODO: open add ticket dialog
        });

        // Next: pass everything to InfoSetupActivity
        findViewById(R.id.btnNext).setOnClickListener(v -> {
            Intent intent = new Intent(this, InfoSetupActivity.class);
            // Pass step 1 data through
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
            // Step 2 data
            intent.putExtra("ticketMode",      selectedMode);
            intent.putExtra("salesStart",      etSalesStart.getText().toString().trim());
            intent.putExtra("salesStartTime",  etSalesStartTime.getText().toString().trim());
            intent.putExtra("salesEnd",        etSalesEnd.getText().toString().trim());
            intent.putExtra("salesEndTime",    etSalesEndTime.getText().toString().trim());
            startActivity(intent);
        });
    }

    private void selectMode(String mode) {
        selectedMode = mode;
        // Reset all to inactive style
        modeFreeRsvp.setBackgroundResource(R.drawable.bg_ticket_mode_inactive);
        modeFreeRsvp.setTextColor(getColor(R.color.text_secondary));
        modePaidTickets.setBackgroundResource(R.drawable.bg_ticket_mode_inactive);
        modePaidTickets.setTextColor(getColor(R.color.text_secondary));
        modeExternalLink.setBackgroundResource(R.drawable.bg_ticket_mode_inactive);
        modeExternalLink.setTextColor(getColor(R.color.text_secondary));

        // Set active
        TextView active = "free_rsvp".equals(mode) ? modeFreeRsvp
                : "paid_tickets".equals(mode) ? modePaidTickets
                : modeExternalLink;
        active.setBackgroundResource(R.drawable.bg_ticket_mode_active);
        active.setTextColor(getColor(R.color.white));
    }
}
